/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.mmbase.core.event.*;
import org.mmbase.util.MMBaseContext;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * ClusterManager is a thread object that reads the receive queue
 * and calls the objects (listeners) who need to know.
 * The ClusterManager starts communication threads to handle the sending
 * and receiving of messages.
 *
 * @author Nico Klasens
 * @author Michiel Meeuwissen
 * @author Ernst Bunders
 * @version $Id$
 */
public abstract class ClusterManager implements AllEventListener, Runnable {

    private static final Logger LOG = Logging.getLoggerInstance(ClusterManager.class);

    protected final Statistics receive = new Statistics();
    protected final Statistics send    = new Statistics();

    /**
     * Queue with messages to send to other MMBase instances
     */
    protected final BlockingQueue<byte[]> nodesToSend = new LinkedBlockingQueue<byte[]>(64);
    /**
     * Queue with received messages from other MMBase instances
     */
    protected final BlockingQueue<byte[]> nodesToSpawn = new LinkedBlockingQueue<byte[]>(64);

    /** Thread which processes the messages */
    protected Thread kicker = null;

    protected boolean spawnThreads = true;

    protected boolean compatible17 = false;

    public final void shutdown(){
        LOG.info("Shutting down clustering");
        stopCommunicationThreads();
        kicker.setPriority(Thread.MIN_PRIORITY);
        kicker = null;
    }

    protected void readConfiguration(Map<String,String> configuration) {
        String tmp = configuration.get("spawnthreads");
        if (tmp != null && !tmp.equals("")) {
            spawnThreads = !"false".equalsIgnoreCase(tmp);
        }
    }

    /**
     * Subclasses should start the communication threads in this method
     */
    protected abstract void startCommunicationThreads();

    /**
     * Subclasses should stop the communication threads in this method
     */
    protected abstract void stopCommunicationThreads();

    @Override
    public void notify(Event event){
        //we only want to propagate the local events into the cluster
        if(event.getMachine().equals(MMBase.getMMBase().getMachineName())){
            byte[] message = createMessage(event);
            if (message != null) {
                if (message.length > 5000) {
                    LOG.warn("Sending large event to the cluster. Serialization of  " + event + " is " + message.length + " long!");
                } else {
                    LOG.debug("Sending an event to the cluster");
                }
                nodesToSend.offer(message);
                LOG.debug("send queue: " + nodesToSend.size());
            } else {
                LOG.debug("Message was null");
            }
        } else {
            LOG.trace("Ignoring remote event from  " + event.getMachine() + " it will not be propagated");
        }
    }

    /**
     * Starts the Changer Thread.
     */
    protected void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(MMBaseContext.getThreadGroup(), this, "ClusterManager");
            kicker.setDaemon(true);
            kicker.start();
            try {
                kicker.setPriority(Thread.NORM_PRIORITY + 1);
            } catch (NullPointerException npe) {
                // MM:a NPE is thrown here sometimes if ThreadGroup of kicker is null.
                // which I saw happening on my jvm (ThreadGroep set to null by Thread.start).
                // I don't understand it, but it's not worth failing ClusterManager completely.
                LOG.warn("Could not set thread priority of Cluster Manager");
            }
            startCommunicationThreads();
        }
    }

    /**
     * Format of a message:
     *
     [<17 style message>],0<serialization of event object>
    */
    protected byte[] createMessage(Event event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serializing " + event);
        }
        try {
            long startTime = System.currentTimeMillis();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (compatible17) {
                /// this is odd, it offers events, but this method is not about that.
                if (event instanceof  NodeEvent || event instanceof RelationEvent) {
                    NodeEvent ne;
                    if (event instanceof RelationEvent) {
                        RelationEvent re = (RelationEvent) event;
                        ne = re.getNodeEvent();
                        ByteArrayOutputStream b1 = new ByteArrayOutputStream();
                        byte[] rel1 = createMessage(re.getMachine(), re.getRelationSourceNumber(), re.getRelationSourceType(), "r").getBytes();
                        b1.write(rel1, 0, rel1.length);
                        b1.write(',');
                        b1.write(0);
                        nodesToSend.offer(b1.toByteArray());
                        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
                        byte[] rel2 = createMessage(re.getMachine(), re.getRelationDestinationNumber(), re.getRelationDestinationType(), "r").getBytes();
                        b2.write(rel2, 0, rel2.length);
                        b2.write(',');
                        b2.write(0);
                        nodesToSend.offer(b2.toByteArray());
                    } else {
                        ne = (NodeEvent) event;
                    }
                    byte[] oldStyleEvent = createMessage(ne.getMachine(), ne.getNodeNumber(), ne.getBuilderName(), NodeEvent.newTypeToOldType(ne.getType())).getBytes();
                    bytes.write(oldStyleEvent, 0, oldStyleEvent.length);
                }
            }
            bytes.write(',');
            bytes.write(0);
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(event);
            long cost = System.currentTimeMillis() - startTime;
            send.parseCost += cost;
            send.cost += cost;
            return bytes.toByteArray();
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            return null;
        }

    }

    /** Followup number of message */
    protected int follownr = 1;
    protected int lastReceivedMessage;

    /**
     * Creates MMBase 1.7 parseable message. This is simple String, which is prefixed before the actual 1.8 message.
     *
     * @param machine MMBase 'machine name'.
     * @param nodenr node number
     * @param tableName node type (tablename)
     * @param type command type
     * @return message
     */
    protected String createMessage(String machine, int nodenr, String tableName, String type) {
        return machine + ',' + (follownr++) + ',' + nodenr + ',' + tableName + ',' + type;
    }

    protected Event parseMessage(byte[] message) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(message);
            int c = 1;
            while (c > 0) {
                // ignore backwards compatibility message
                c = stream.read(); // first time read a , then later a 0
                LOG.trace("Found " + c);
            }
            ObjectInputStream in = new ObjectInputStream(stream);
            Event event = (Event) in.readObject();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unserialized " + event);
            }
            return event;
        } catch (StreamCorruptedException scc) {
            // not sure that this can happen, now, because of the while(c>0) trick.
            LOG.debug(scc.getClass() + " " + scc.getMessage() + ". Supposing old style message of " + message.length + " byte ", scc);
            // Possibly, it is a message from an 1.7 system
            String mes;
            try {
                mes = new String(message, "ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException uee) {
                LOG.warn(uee);
                mes = new String(message);
            }

            NodeEvent event = parseMessageBackwardCompatible(mes);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Old style message " + event);
            }
            return event;
        } catch (EOFException eofe) {
            // suppose that this is a 1.7 message
            String mes;
            try {
                mes = new String(message, "ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException uee) {
                LOG.warn(uee);
                mes = new String(message);
            }
            NodeEvent event = parseMessageBackwardCompatible(mes);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Old style message " + event + " of " + message.length + " byte");
            }
            return event;
        } catch (IOException ioe) {
            LOG.error(ioe);
            return null;
        } catch (ClassNotFoundException cnfe) {
            LOG.error(cnfe);
            return null;
        }
    }

    protected NodeEvent parseMessageBackwardCompatible(String message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("RECEIVE=>" + new org.mmbase.util.transformers.UnicodeEscaper().transform(message));
        }
        StringTokenizer tok = new StringTokenizer(message, ",");
        if (tok.hasMoreTokens()) {
            String machine = tok.nextToken();
            if (tok.hasMoreTokens()) {
                String fnr = tok.nextToken();
                int newFollowNr = Integer.valueOf(fnr);
                int expectedFollowNr = lastReceivedMessage + 1;
                if (newFollowNr != expectedFollowNr) {
                    LOG.debug("Expected message " + expectedFollowNr + ", but message " + newFollowNr + " was received ");
                }
                lastReceivedMessage = newFollowNr;
                if (tok.hasMoreTokens()) {
                    String id = tok.nextToken();
                    if (tok.hasMoreTokens()) {
                        String tb = tok.nextToken();
                        if (tok.hasMoreTokens()) {
                            String ctype = tok.nextToken();
                            if (!ctype.equals("s")) {
                                MMBase mmbase = MMBase.getMMBase();
                                MMObjectBuilder builder = mmbase.getBuilder(tb);
                                if (builder == null) builder = mmbase.getBuilder("object");
                                MMObjectNode node = builder.getNode(id);
                                if (node != null) {
                                    return new NodeEvent(machine, tb, node.getNumber(), node.getOldValues(), node.getValues(), NodeEvent.oldTypeToNewType(ctype));
                                } else {
                                    try {
                                        return new NodeEvent(machine, tb, Integer.valueOf(id).intValue(), null, null, NodeEvent.oldTypeToNewType(ctype));
                                    } catch (NumberFormatException nfe) {
                                        LOG.error(message + ": colud not parse " + id + " to a node number.");
                                    }
                                }
                            } else {
                                /// XXXX should we?
                                LOG.error("XML messages not suppported any more");
                            }
                        } else LOG.error(message + ": 'ctype' could not be extracted from this string!");
                    } else LOG.error(message + ": 'tb' could not be extracted from this string!");
                } else LOG.error(message + ": 'id' could not be extracted from this string!");
            } else LOG.error(message + ": 'vnr' could not be extracted from this string!");
        } else LOG.error(message + ": 'machine' could not be extracted from this string!");
        return null;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        while(kicker != null) {
            try {
                byte[] message = nodesToSpawn.take();
                if (message == null) continue;
                long startTime = System.currentTimeMillis();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("RECEIVED =>" + message.length + " bytes, queue: " + nodesToSpawn.size());
                }
                receive.count++;
                receive.bytes += message.length;
                Event event = parseMessage(message);
                receive.parseCost += (System.currentTimeMillis() - startTime);
                if (event != null) {
                    handleEvent(event);
                } else {
                    LOG.warn("Could not handle event, it is null");
                }
                receive.cost += (System.currentTimeMillis() - startTime);
            } catch (InterruptedException e) {
                LOG.debug(Thread.currentThread().getName() +" was interruped.");
                break;
            } catch(Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }
    }

    /**
     * @javadoc
     * @param event
     */
    protected void handleEvent(final Event event) {
        // check if MMBase is 100% up and running, if not eat event
        MMBase mmbase = MMBase.getMMBase();
        if (mmbase == null || !mmbase.getState()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring event " + event + ", mmbase is not up " + mmbase);
            }
            return;
        }
        if (mmbase.getMachineName().equals(event.getMachine())) {
            // ignore changes of ourselves
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring event " + event + " it is from this (" + event.getMachine() + ") mmbase");
            }
            return;
        }

        if (event instanceof NodeEvent) {
            MMObjectBuilder builder = mmbase.getBuilder(((NodeEvent) event).getBuilderName());
            if (builder != null && (! builder.broadcastChanges())) {
                LOG.info("Ignoring node-event for node type " + builder + " because broad cast changes is false");
                return;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Handling event " + event + " for " + event.getMachine());
        }

        if (spawnThreads) {
            Runnable job = new Runnable () {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    EventManager.getInstance().propagateEvent(event);
                    receive.cost += (System.currentTimeMillis() - startTime);
                }
            };
            org.mmbase.util.ThreadPools.jobsExecutor.execute(job);
        } else {
            try {
                EventManager.getInstance().propagateEvent(event);
            } catch (Throwable t) {
                LOG.error("Exception during propagation of event: " + event + ": " + t.getMessage(), t);
            }
        }
    }

}
