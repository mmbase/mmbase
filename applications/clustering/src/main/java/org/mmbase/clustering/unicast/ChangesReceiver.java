/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering.unicast;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Queue;

import org.mmbase.util.ThreadPools;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * ChangesReceiver is a running object that (can) build(s) a Unicast Thread  to receive changes from other MMBase Servers.
 *
 * @author Nico Klasens
 * @version $Id$
 * @see Unicast
 */
public class ChangesReceiver implements Runnable {

    private static final Logger log = Logging.getLoggerInstance(ChangesReceiver.class);

    private Thread kicker = null;

    /** Queue with messages received from other MMBase instances */
    private final BlockingQueue<byte[]> nodesToSpawn;

    private final ServerSocket serverSocket;

    private final int version;

    private int maxMessageSize = 5 * 1024 * 1024;

    /**
     * Construct UniCast Receiver
     * @param unicastHost host of unicast connection
     * @param unicastPort port of the unicast connections
     * @param nodesToSpawn Queue of received messages
     * @throws IOException when server socket failrf to listen
     */
    public ChangesReceiver(final String unicastHost, int unicastPort, BlockingQueue<byte[]> nodesToSpawn, int version) throws IOException {
        this.nodesToSpawn = nodesToSpawn;
        this.serverSocket = new ServerSocket();
        final InetAddress ia;
        if ("*".equals(unicastHost)) {
            ia = null;
        } else {
            ia = InetAddress.getByName(unicastHost);
        }
        if (unicastPort > 0) {
            SocketAddress address = new InetSocketAddress(ia, unicastPort);
            serverSocket.bind(address);
        }
        this.version = version;
    }

    public void setMaxMessageSize(int m) {
        maxMessageSize = m;
    }

    public void start() {
        if (kicker == null) {
            kicker = new Thread(ThreadPools.threadGroup, this, "UnicastReceiver");
            kicker.setDaemon(true);
            kicker.start();
        }
    }

    void stop() {
        if (kicker != null) {
            try {
                kicker.interrupt();
                kicker.setPriority(Thread.MIN_PRIORITY);
                kicker = null;
            } catch (Throwable t) {
            }
            try {
                serverSocket.close();
            } catch (IOException ioe) {
                log.warn(ioe.getMessage(), ioe);
            }
        } else {
            log.service("Cannot stop thread, because it is null");
        }
    }

    /**
     * Reads a number of 'messages' from a stream, according to the 'version 2' protocol (i.e. supporting multiple
     * messages in one session).
     * When ready, closes the stream.
     * @param in The stream to read from
     * @param queue The collection of messages to add the newly read messages to
     * @since MMBase-2.0
     */
    protected void readStreamVersion2(InputStream in, Queue<byte[]> queue) throws IOException {
        DataInputStream reader = null;
        try {
            reader = new DataInputStream(in);
            int listSize = reader.readInt();
            log.trace("Will read " + listSize + " events");

            for (int i = 0; i < listSize; i++) {
                int arraySize = reader.readInt();
                if (arraySize > maxMessageSize) {
                    log.warn("Size of event " + i + ": " + arraySize + " too big, ignoring the rest (" + (listSize - i) + " messages remaining).");
                    break;
                } else {
                    log.trace("Size of event " + i + ": " + arraySize);
                }
                ByteArrayOutputStream writer = new ByteArrayOutputStream();
                //this buffer has nothing to do with the OS buffer

                byte[] buffer = new byte[arraySize];
                reader.readFully(buffer);
                if (writer != null) {
                    writer.write(buffer, 0, arraySize);
                    writer.flush();
                }
                // maybe we should use encoding here?
                byte[] message = writer.toByteArray();
                queue.offer(message);
                if (log.isDebugEnabled()) {
                    log.debug("unicast(v" + version + ") RECEIVED=>" + message + " queue: " + queue.size());
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Reads one  'message' from a stream, according to the 'version 1' protocol (i.e. supporting only one
     * message in one session)
     * When ready, closes the stream.
     * @param in The stream to read from
     * @param queue The collection of messages to add the newly read message to
     * @since MMBase-2.0
     */
    protected void readStreamVersion1(InputStream in, Queue<byte[]> queue) throws IOException {
        DataInputStream reader = null;
        try {
            reader = new DataInputStream(in);
            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            int size = 0;
            //this buffer has nothing to do with the OS buffer
            byte[] buffer = new byte[1024];
            while ((size = reader.read(buffer)) != -1) {
                if (writer != null) {
                    writer.write(buffer, 0, size);
                    writer.flush();
                }
            }
            byte[] message = writer.toByteArray();
            nodesToSpawn.offer(message);
            if (log.isDebugEnabled()) {
                log.debug("unicast(v1) RECEIVED=>" + message + " queue: " + nodesToSpawn.size());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void run() {
        log.info("Unicast listening started on " + serverSocket + " (v:" + version + ")");
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) break;
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    if (log.isTraceEnabled()) {
                        log.trace("" + socket);
                    }

                    if (version > 1) {
                        readStreamVersion2(socket.getInputStream(), nodesToSpawn);
                    } else {
                        readStreamVersion1(socket.getInputStream(), nodesToSpawn);
                    }
                } catch (SocketException e) {
                    log.warn(e.getMessage(), e);
                    continue;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
