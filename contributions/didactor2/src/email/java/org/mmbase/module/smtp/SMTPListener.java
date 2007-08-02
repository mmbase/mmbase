package org.mmbase.module.smtp;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;
import org.mmbase.bridge.LocalContext;
import java.util.Map;
import java.net.*;
import java.util.concurrent.*;

/**
 * Listener thread, that accepts connection on port 25 (default) and 
 * delegates all work to its worker threads.
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 * @version $Id: SMTPListener.java,v 1.12 2007-08-02 16:36:23 michiel Exp $
 */
public class SMTPListener extends Thread {

    private static final int THREADS = 10;
    static int number = 1;
    private static final ThreadFactory factory = new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SMTPLISTENER-" + (number++)) {
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
    final ExecutorService socketThreads = new ThreadPoolExecutor(THREADS, THREADS, 5 * 60, TimeUnit.SECONDS, new  LinkedBlockingQueue(), factory);


    private static final Logger log = Logging.getLoggerInstance(SMTPListener.class);
    private boolean running = true;
    private ServerSocket ssocket;
    private final Map<String, String> properties;

    public SMTPListener(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Main run method; it will listen for new connections and spawn
     * new threads for incoming connections.
     */
    public void run() {
        String host = null;
        int port = -1;
        while (running) {

            String portnr = properties.get("port");
            port = Integer.parseInt(portnr);
            
            host = properties.get("hostname");
            if (host == null) host = "localhost";
            
            try {
                ssocket = new ServerSocket();
                SocketAddress address = new InetSocketAddress(host, port);
                ssocket.bind(address);
                log.info("SMTP listening on " + host + ":" + port + " (" + ssocket + ")");
                break;
            } catch (Exception e) {
                running = false;
                log.warn("Cannot listen on " + host + ":"  + port + " because " + e.getMessage());
                try {
                    running = true;
                    SocketAddress address = new InetSocketAddress(port);
                    ssocket.bind(address);
                    log.info("SMTP listening on port " + port + " (" + ssocket + ")");
                    break;
                } catch (Exception f) {
                    log.error("Cannot listen on port "  + port + " because " + f.getMessage());
                    try {
                        // may be someone frees the point, try again later.
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }

        while (running) {
            try {
                final Socket socket = ssocket.accept();
                if (log.isDebugEnabled()) {
                    log.debug("Accepted connection: " + socket);
                }
                final SMTPHandler handler = new SMTPHandler(socket, properties);
                socketThreads.execute(handler);
            } catch (Exception e) {
                if (ssocket != null && ! ssocket.isClosed()) {
                    log.error("Exception while accepting connections: " + e.getMessage(), e);
                } else {
                    log.service(e.getMessage());
                }
            }
        }
        log.service("Stopped SMTP listening on " + host + ":" + port);

    }

    public void interrupt() {
        // Interrupted; this only happens when we are shutting down
        log.debug("Interrupt called");
        running = false;
        if (ssocket != null) {
            try {
                ssocket.close();
            } catch (Exception e) {
                log.info(e);
            }
            ssocket = null;
        }
        socketThreads.shutdown();
    }
}
