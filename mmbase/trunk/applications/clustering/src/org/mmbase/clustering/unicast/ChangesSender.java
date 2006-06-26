/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering.unicast;

import org.mmbase.clustering.Statistics;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

import org.mmbase.module.builders.MMServers;
import org.mmbase.module.core.*;

import org.mmbase.util.Queue;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * ChangesSender is a thread object sending the nodes found in the
 * sending queue over unicast connections
 *
 * @author Nico Klasens
 * @version $Id: ChangesSender.java,v 1.11 2006-06-26 13:02:20 michiel Exp $
 */
public class ChangesSender implements Runnable {

    private static final Logger log = Logging.getLoggerInstance(ChangesSender.class);


    private final Statistics send;

    /** Thread which sends the messages */
    private Thread kicker = null;

    /** Queue with messages to send to other MMBase instances */
    private final Queue nodesToSend;

    /** For the port on which the talking between nodes take place.*/
    private final Map configuration;
    private final int defaultUnicastPort;

    /** Timeout of the connection.*/
    private final int unicastTimeout;

    /** last time the mmservers table was checked for active servers */
    private long lastServerChecked = -1;
    private List activeServers = new ArrayList();

    /** Interval of servers change their state */
    private long serverInterval;

    /**
     * Construct UniCast Sender
     * @param unicastPort port of the unicast connections
     * @param unicastTimeout timeout on the connections
     * @param nodesToSend Queue of messages to send
     * @param mmbase MMBase instance
     */
    ChangesSender(Map configuration, int unicastPort, int unicastTimeout, Queue nodesToSend, Statistics send) {
        this.nodesToSend = nodesToSend;
        this.configuration = configuration;
        this.defaultUnicastPort = unicastPort;
        this.unicastTimeout = unicastTimeout;
        this.send = send;
        this.start();
    }

    private  void start() {
        if (kicker == null) {
            kicker = MMBaseContext.startThread(this, "UnicastSender");
            log.debug("UnicastSender started");
        }
    }
    void stop() {
        if (kicker != null) {
            kicker.interrupt();
            kicker.setPriority(Thread.MIN_PRIORITY);
            kicker = null;
        } else {
            log.service("Cannot stop thread, because it is null");
        }
    }


    // javadoc inherited
    public void run() {
        while(kicker != null) {
            try {
                byte[] data = (byte[]) nodesToSend.get();
                long startTime = System.currentTimeMillis();
                List servers = getActiveServers();
                for (int i = 0; i < servers.size(); i++) {
                    MMObjectNode node = (MMObjectNode) servers.get(i);
                    if (node != null) {
                        String hostname = node.getStringValue("host");
                        String machinename = node.getStringValue("name");

                        int unicastPort = defaultUnicastPort;
                        String specificPort = (String) configuration.get(machinename + ".unicastport");
                        if (specificPort != null) {
                            unicastPort = Integer.parseInt(specificPort);
                        }
                        Socket socket = null;
                        DataOutputStream os = null;
                        try {
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(hostname, unicastPort), unicastTimeout);
                            os = new DataOutputStream(socket.getOutputStream());
                            os.write(data, 0, data.length);
                            os.flush();
                            if (log.isDebugEnabled()) {
                                log.debug("SEND=>" + hostname + ":" + unicastPort);
                            }
                        } catch(SocketTimeoutException ste) {
                            servers.remove(i);
                            log.warn("Server timeout: " + hostname + ":" + unicastPort + " " + ste + ". Removed " + node + " from active server list.");
                        } catch (ConnectException ce) {
                            log.warn("Connect exception: " + hostname + ":" + unicastPort + " " + ce + ".");
                        } catch (IOException e) {
                            log.error("can't send message " + e.getMessage() , e);
                        } finally {
                            if (os != null) {
                                try {
                                    os.close();
                                } catch (IOException e1) {
                                }
                            }
                            if (socket != null) {
                                try {
                                    socket.close();
                                } catch (IOException e1) {
                                }
                            }
                        }
                    }
                }
                send.count++;
                send.bytes += data.length;
                send.cost += (System.currentTimeMillis() - startTime);

            } catch (InterruptedException e) {
                log.debug(Thread.currentThread().getName() +" was interruped.");
                break;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Get Active server list
     * @return server list
     */
    private List getActiveServers() {
        List prevActiveServers = activeServers;
        if (serverInterval < 0) {
            MMBase mmbase = MMBase.getMMBase();
            MMServers mmservers = (MMServers) mmbase.getBuilder("mmservers");
            serverInterval = mmservers.getIntervalTime();
            activeServers = mmservers.getActiveServers();
            lastServerChecked = System.currentTimeMillis();
            log.info("Active servers: " + activeServers );
        } else {
            if (lastServerChecked + serverInterval < System.currentTimeMillis()) {
                MMBase mmbase = MMBase.getMMBase();
                MMServers mmservers = (MMServers) mmbase.getBuilder("mmservers");
                activeServers = mmservers.getActiveServers();
                lastServerChecked = System.currentTimeMillis();
                if (! activeServers.equals(prevActiveServers)) {
                    log.info("Active servers: " + activeServers + " " + prevActiveServers.size() + " -> " + activeServers.size());
                } else {
                    log.debug("Active servers: " + activeServers);
                }
            }
        }
        return activeServers;
    }


}
