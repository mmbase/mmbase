/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.clustering;


import java.io.*;
import java.util.*;

import org.mmbase.core.event.*;
import org.mmbase.module.core.*;
import org.mmbase.util.Queue;
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
 * @version $Id: ClusterManager.java,v 1.18 2006-01-13 15:44:30 pierre Exp $
 */
public abstract class ClusterManager implements AllEventListener, Runnable{

    private static final Logger log = Logging.getLoggerInstance(ClusterManager.class);

    /** 
     * Number of processed messages 
     */
    protected int spawncount = 0;

    /** Queue with messages to send to other MMBase instances */
    protected Queue nodesToSend = new Queue(64);
    /** Queue with received messages from other MMBase instances */
    protected Queue nodesToSpawn = new Queue(64);

    /** Thread which processes the messages */
    private Thread kicker = null;

    protected boolean spawnThreads = true;


    public void shutdown(){
        stopCommunicationThreads();
        kicker.setPriority(Thread.MIN_PRIORITY);
        kicker = null;
    }

    /**
     * Subclasses should start the communication threads in this method
     */
    protected abstract void startCommunicationThreads();

    /**
     * Subclasses should stop the communication threads in this method
     */
    protected abstract void stopCommunicationThreads();

    public void notify(Event event){
        //we only want to propagate the local events into the cluster
        if(event.getMachine().equals(MMBase.getMMBase().getMachineName())){
            byte[] message = createMessage(event);
            nodesToSend.append(message);
        }
    }

    /**
     * Starts the Changer Thread.
     */
    protected void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = MMBaseContext.startThread(this, "ClusterManager");
            kicker.setPriority(Thread.NORM_PRIORITY + 1);
            startCommunicationThreads();
        }
    }

    protected byte[] createMessage(Event event) {
        if (log.isDebugEnabled()) {
            log.debug("Serializing " + event);
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(event);
            return bytes.toByteArray();
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            return null;
        }

    }

    protected Event parseMessage(byte[] message) {
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(message));
            Event event = (Event) in.readObject();
            if (log.isDebugEnabled()) {
                log.debug("Unserialized " + event);
            }
            return event;
        } catch (StreamCorruptedException scc) {
            log.debug(scc.getMessage() + ". Supposing old style message.");
            // Possibly, it is a message from an 1.7 system
            String mes = new String(message);
            NodeEvent event = parseMessageBackwardCompatible(mes);
            if (log.isDebugEnabled()) {
                log.debug("Old style message " + event);
            }
            return event;
        } catch (IOException ioe) {
            log.error(ioe);
            return null;
        } catch (ClassNotFoundException cnfe) {
            log.error(cnfe);
            return null;
        }
    }

    protected NodeEvent parseMessageBackwardCompatible(String message) {
        if (log.isDebugEnabled()) {
            log.debug("RECEIVE=>" + message);
        }
        StringTokenizer tok = new StringTokenizer(message,",");
        if (tok.hasMoreTokens()) {
            String machine = tok.nextToken();
            if (tok.hasMoreTokens()) {
                String vnr = tok.nextToken();
                if (tok.hasMoreTokens()) {
                    String id = tok.nextToken();
                    if (tok.hasMoreTokens()) {
                        String tb = tok.nextToken();
                        if (tok.hasMoreTokens()) {
                            String ctype=tok.nextToken();
                            if (!ctype.equals("s")) {
                                MMBase mmbase = MMBase.getMMBase();
                                MMObjectBuilder builder = mmbase.getBuilder(tb);
                                MMObjectNode    node    = builder.getNode(id);
                                return new NodeEvent(machine, tb, node.getNumber(), node.getOldValues(), node.getValues(), NodeEvent.oldTypeToNewType(ctype));
                            } else {
                                /// XXXX should we?
                                log.error("XML messages not suppported any more");
                            }
                        } else log.error(message + ": 'ctype' could not be extracted from this string!");
                    } else log.error(message + ": 'tb' could not be extracted from this string!");
                } else log.error(message + ": 'id' could not be extracted from this string!");
            } else log.error(message + ": 'vnr' could not be extracted from this string!");
        } else log.error(message + ": 'machine' could not be extracted from this string!");
        return null;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while(kicker != null) {
            try {
                byte[] message = (byte[]) nodesToSpawn.get();
                if (log.isDebugEnabled()) {
                    log.trace("RECEIVED =>" + message.length + " bytes");
                }
                spawncount++;
                Event event = parseMessage(message);
                if (event != null) {
                    handleEvent(event);
                } else {
                    log.warn("Could not handle message, it is null");
                }
            } catch (InterruptedException e) {
                log.debug(Thread.currentThread().getName() +" was interruped.");
                break;
            } catch(Throwable t) {
                log.error(t.getMessage(), t);
            }
        }

    }

    /**
     * Handle message
     *
     * @param event NodeEvent
     */
    protected void handleEvent(Event event) {
        // check if MMBase is 100% up and running, if not eat event
        MMBase mmbase = MMBase.getMMBase();
        if (mmbase == null || !mmbase.getState()) {
            log.debug("Ignoring event " + event + ", mmbase is not up " + mmbase);
            return;
        }
        if(mmbase.getMachineName().equals(event.getMachine())) {
            // ignore changes of ourselves
            log.debug("Ignoring event " + event + " it is from this mmbase");
            return;
        }
        MessageProbe probe = new MessageProbe(event);
        if (spawnThreads) {
            probe.run();
        } else{
            org.mmbase.util.ThreadPools.jobsExecutor.execute(probe);
        }
    }

}
