/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.*;


/**
 * <p>
 * Java command server. Especially targeted at executing imagemagick 'convert' in a system call.
 *</p>
 * <p>
 * It can work in two ways. The first way is as a stand-alone server. You start it up with a
 * port-number argument then (or with a host-name and port-number argument, if you don't want it to listen on localhost). A server socket will be
 * created and communication can happen via tcp.
 *
 <pre>
~$ /usr/bin/java -jar mmbase-commandserver.jar michiel.omroep.nl 1679
Started ServerSocket[addr=michiel.omroep.nl/145.58.67.10,port=0,localport=1679]
</pre>
 *</p>
 * <p>
 * If no arguments are provided, it will listen on stdin and return the result on stdout. This means
 * that it can be installed as a inetd daemon (in UNIX systems). This boils down to placing something like
 <pre>
commandserver 1679/tcp
 </pre>
 * In /etc/services. And
 <pre>
commandserver	stream	tcp		nowait	nobody	/usr/bin/java java -jar /home/michiel/mmbase/head/applications/commandserver/build/mmbase-commandserver.jar
 </pre>
 * in /etc/inetd.conf. The result is the same (you can communicate with TCP), but avoids the hassle of having to keep the process alive.
 * </p>
 * <p>
 * The input for this server are 2 serialized String[] arrays (which will be the arguments for
 * {@link Runtime#exec(String[], String[])}), followed by the stdin for the command. It will return stdout of the process.
 * </p>
 *
 * You can use {@link org.mmbase.util.externalprocess.CommandExecutor} to connect to the commandserver.
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.8.2
 * @version $Id$
 */


public class CommandServer {

    private static final int THREADS = 2;
    private static boolean debug = false;

    private static void debug(String m) {
        if (debug) {
            System.out.println(m);
        }
    }

    // thread pool.
    static int number = 0;
    static final ThreadFactory FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "POOL-" + (number++)) {
                /**
                 * Overrides run of Thread to catch and log all exceptions. Otherwise they go through to app-server.
                 */
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        System.err.println("Error during job: " + t.getClass().getName() + " " + t.getMessage());
                    }
                        }
            };
            t.setDaemon(true);
            return t;
        }
    };

    // direct-hand-of-executor. Resources are limited by the acceptQueue
    final static BlockingQueue<Runnable> threadQueue = new  SynchronousQueue<Runnable>(true);
    final static ThreadPoolExecutor threads = new ThreadPoolExecutor(6 * THREADS, Integer.MAX_VALUE, 5 * 60, TimeUnit.SECONDS, threadQueue, FACTORY);

    public static String stackTrace(Throwable e, int max) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        String message = e.getMessage();
        StringBuilder buf = new StringBuilder(e.getClass().getName());
        buf.append(": ");
        if (message == null) {

        }  else {
            buf.append(message);
        }
        for (int i = 0; i < stackTrace.length; i++) {
            if (i == max) break;
            buf.append("\n        at ").append(stackTrace[i]);
        }
        Throwable t = e.getCause();
        if (t != null) {
            buf.append("\n").append(stackTrace(t, max));
        }
        return buf.toString();
    }
    // copy job
    public static class Copier implements Runnable {
        private boolean ready;
        private int count = 0;
        private final InputStream in;
        private final OutputStream out;
        private final String name;
        public  boolean debug = false;

        public Copier(InputStream i, OutputStream o, String n) {
            in = i; out = o; name = n;
        }
        public void run() {
            debug("Started " + this);
            int size = 0;
            try {
                byte[] buffer = new byte[10];
                while ((size = in.read(buffer)) != -1) {
                    out.write(buffer, 0, size);
                    count+= size;
                }
            } catch (Throwable t) {
                System.err.println("Connector " + toString() +  ": " + t.getClass() + " " + t.getMessage() + " " + in + " " + stackTrace(t, 10));
            }
            debug("Read " + toString() + " "  + count);
            synchronized(this) {
                ready = true;
                notifyAll();
            }
        }
        public  boolean ready() {
            return ready;
        }
        public void  waitFor() throws InterruptedException {
            debug("Waiting for " + this);
            synchronized(this) {
                while (! ready) wait();
            }
            debug("Written " + toString() + " "  + count);
        }
        public String toString() {
            return name;
        }

    }

    private static long seq = 1;
    private static ScheduledExecutorService scheduler;

    public static class Command implements Runnable {

        private final InputStream input;
        private final OutputStream output;
        private final OutputStream errors;
        private final String desc;
        private final Runnable close;
        protected final long number = seq++;
        private String[] params;
        private int maxDuration = -1;
        private final List<Future<?>> futures = new ArrayList<Future<?>>();
        private int exitValue = -1;
        private Process process;
        public Command(InputStream in, OutputStream out, OutputStream err, String d, Runnable c) {
            input = in;
            output = out;
            errors = err;
            desc   = d;
            close = c;

        }
        public void setMaxDuration(int max, Future<Command> future) {
            maxDuration = max;
            futures.add(future);
        }
        protected void killAfterDuration() {
            if (futures.size() > 0 ) {
                if (scheduler == null) {
                    scheduler = Executors.newScheduledThreadPool(1);
                }
                scheduler.schedule(new Runnable() {
                        public void run() {
                            Process p = Command.this.process;
                            if (p != null) {
                                System.out.println("Killing " + Command.this);
                                try {
                                    errors.write(("Killing " + Command.this + " (took over " + maxDuration + " ms)\n").getBytes());
                                } catch (IOException e) {
                                    System.err.println(e.getMessage());
                                }
                                p.destroy();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    System.err.println(e.getMessage());
                                }
                                for (Future<?> f : futures) {
                                    if (f.cancel(true)) {
                                        System.err.println("Canceled " + f);
                                    }
                                }
                            }
                        }
                    }, maxDuration, TimeUnit.MILLISECONDS);
            }
        }
        public void run() {
            try {
                killAfterDuration();
                int version = input.read();
                ObjectInputStream stream = new ObjectInputStream(input);
                params = (String[]) stream.readObject();
                String[] env    = (String[]) stream.readObject();
                System.out.println(number + " Executing " + Arrays.asList(params) + " (queue " + threadQueue.size() + ")");
                process = Runtime.getRuntime().exec(params, env);
                PipedInputStream pi  = new PipedInputStream();
                PipedOutputStream po = new PipedOutputStream(pi);

                Copier connector = new Copier(input, po, number + ".input -> piped output");
                futures.add(threads.submit(connector));

                Copier connector2 = new Copier(pi, process.getOutputStream(), number + ",piped input -> process input");
                futures.add(threads.submit(connector2));


                PipedInputStream pi2 = new PipedInputStream();
                PipedOutputStream po2 = new PipedOutputStream(pi2);

                InputStream  inputStream = process.getInputStream();
                OutputStream outputStream = process.getOutputStream();

                Copier connector3 = new Copier(inputStream, po2, number + ";process output -> piped output 2");
                futures.add(threads.submit(connector3));


                InputStream  errorStream = process.getErrorStream();
                PipedInputStream piErr = new PipedInputStream();
                PipedOutputStream poErr = new PipedOutputStream(piErr);

                Copier connectorErr = new Copier(errorStream, poErr, number +  ";process err -> piped err");
                futures.add(threads.submit(connectorErr));

                Copier connector4 = new Copier(pi2, output, number + ";piped input2 -> output");
                futures.add(threads.submit(connector4));


                Copier connectorErr2 = new Copier(piErr, errors, number +  ";piped err -> errors");
                futures.add(threads.submit(connectorErr2));

                connector.waitFor();
                po.close();

                connector2.waitFor();

                outputStream.close();

                connector3.waitFor();
                po2.close();

                debug("Waiting for process to end");
                process.waitFor();


                debug("Closing");
                connectorErr.waitFor();
                poErr.close();

                connector4.waitFor();
                pi2.close();
                connectorErr2.waitFor();
                piErr.close();

                output.close();

                if (errors != output) {
                    errors.close();
                }
                if (close != null) {
                    close.run();
                }
                exitValue = process.exitValue();
                debug(number + " ready. Exit value: " + exitValue);

            } catch (Exception ie) {
                System.err.println("" + number + " " + ie.getClass().getName() + " " + ie.getMessage() + " for " + desc + " " + Arrays.asList(ie.getStackTrace()));
            } finally {
                process = null;
                debug("End");
            }


        }
        public int getExitValue() {
            return exitValue;
        }
        @Override
        public String toString() {
            return number + ":" + (params == null ?  "" : Arrays.asList(params) + " exit value:" + exitValue);
        }

    }
    /**
     */
    public static void main(String[] a) throws IOException, InterruptedException {
        List<String> args = new ArrayList<String>();
        int numberOfThreads = THREADS;
        int maxDuration     = -1;
        for (int i = 0 ; i < a.length; i++) {
            String arg = a[i];
            if (arg.equals("--help") || args.equals("-h") || args.equals("-?") || args.equals("--?")) {
                System.out.println("Usage:\n");
                System.out.println(" java -jar mmbase-commandserver.jar [[--threads <number>] [<hostname>] <portnumber>]\n");
                System.out.println("If both arguments missing, it will listen on stdin, and produce output on stdout.");
                return;
            } else if (arg.equals("--threads")) {
                numberOfThreads = Integer.parseInt(a[++i]);
            } else if (arg.equals("--maxDuration")) {
                maxDuration = (int) (Float.parseFloat(a[++i]) * 1000);
            } else {
                args.add(arg);
            }
        }
        if (args.size() == 0) {
            debug = false;
            Runnable run = new Command(System.in, System.out, System.err, "stdin/stdout", null);
            run.run();
        } else {
            String host = args.size() > 1 ? args.get(0) : null;
            if ("*".equals(host) || host == null) {
                host = null;
                System.out.println("Binding to all interfaces");
            }
            int port = args.size() == 1 ? Integer.parseInt(args.get(0)) : Integer.parseInt(args.get(1));
            System.out.println("Starting command server on " + (host == null ? "*" : host) + ":" + port);

            final BlockingQueue<Runnable> socketQueue = new LinkedBlockingQueue<Runnable>();
            threads.setCorePoolSize(numberOfThreads * 7);
            final ExecutorService socketThreads = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 5 * 60, TimeUnit.SECONDS, socketQueue, FACTORY) {
                @Override
                protected void afterExecute(Runnable r, Throwable t) {
                    super.afterExecute(r, t);
                    Command res;
                    try {
                        res = ((FutureTask<Command>) r).get();
                    } catch (Exception ie) {
                        System.err.println(ie.getMessage());
                        res = null;
                    }
                    System.out.println(res + " READY " + (t != null ? "(" + t + ")" : "") + " (query " + socketQueue.size() + ")");
                }
            };
            try (ServerSocket server = new ServerSocket()) {
                if (host == null) {
                    server.bind(new InetSocketAddress(port));
                } else {
                    SocketAddress address = new InetSocketAddress(host, port);
                    server.bind(address);
                }
                System.out.println("Started " + server + " (using " + numberOfThreads + " threads, " + maxDuration + " ms)");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while (true) {

                    final Socket accept = server.accept();
                    accept.setSoTimeout(10000);
                    accept.setKeepAlive(false);
                    accept.setReceiveBufferSize(1024);
                    final Command command = new Command(accept.getInputStream(),
                        accept.getOutputStream(),
                        accept.getOutputStream(),
                        accept.toString(),
                        new Runnable() {
                            public void run() {
                                try {
                                    accept.shutdownInput();
                                } catch (Exception e) {
                                }
                            }
                        });
                    System.out.println(command.number + " " + format.format(new Date()) + " " + " Connection " + accept + " (queue " + socketQueue.size() + ")");
                    Future<Command> future = socketThreads.submit(command, command);
                    if (maxDuration > 0) {
                        command.setMaxDuration(maxDuration, future);
                    }

                }
            }
        }

    }

}
