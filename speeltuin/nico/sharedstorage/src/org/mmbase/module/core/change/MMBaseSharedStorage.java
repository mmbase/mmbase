/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core.change;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseChangeInterface;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.Queue;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * MMBaseSharedStorage is a thread object that reads the receive queue
 * and calls the objects (listeners) who need to know.
 * The MMBaseSharedStorage starts communication threads to handle the sending 
 * and receiving of messages.
 *  
 * @author Nico Klasens
 * @version $Id: MMBaseSharedStorage.java,v 1.1 2004-09-29 19:35:00 nico Exp $
 */
public abstract class MMBaseSharedStorage implements MMBaseChangeInterface, Runnable {

    /** MMbase logging system */
    private static Logger log = Logging.getLoggerInstance(MMBaseSharedStorage.class.getName());
    
    /** Followup number of message */
    protected int follownr = 1;
    /** Number of processed messages */
    protected int spawncount = 0;

    /** Collections of nodes a thread is waiting on for change */
    protected Vector waitingNodes = new Vector();
    /** Queue with messages to send to other MMBase instances */
    protected Queue nodesToSend = new Queue(64);
    /** Queue with received messages from other MMBase instances */
    protected Queue nodesToSpawn = new Queue(64);

    /** Thread which processes the messages */
    private Thread kicker = null;

    /** MMBase instance */
    protected MMBase mmbase = null;

    protected boolean spawnThreads = true; 
    
    /**
     * @see org.mmbase.module.core.MMBaseChangeInterface#init(org.mmbase.module.core.MMBase)
     */
    public void init(MMBase mmb) {
        this.mmbase = mmb;
    }
    
    /**
     * Subclasses should start the communication threads in this method
     */
    protected abstract void startCommunicationThreads();
    /**
     * Subclasses should stop the communication threads in this method
     */
    protected abstract void stopCommunicationThreads();

    
    /**
     * Starts the Changer Thread.
     */
    public void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(this, "MMBaseSharedStorage");
            kicker.setDaemon(true);
            kicker.start();
            startCommunicationThreads();
        }
    }

    /**
     * Stops the MMBaseSharedStorage.
     */
    public void stop() {
        stopCommunicationThreads();
        /* Stop thread */
        kicker.setPriority(Thread.MIN_PRIORITY);
        kicker = null;
    }
    
    /**
     * @see org.mmbase.module.core.MMBaseChangeInterface#changedNode(int, java.lang.String, java.lang.String)
     */
    public boolean changedNode(int nodenr, String tableName, String type) {
        String message = createMessage(nodenr, tableName, type);
        nodesToSend.append(message);
        return true;
    }

    /**
     * Create message to send to other servers
     * 
     * @param nodenr node number
     * @param tableName node type (tablename)
     * @param node Node with xml info
     * @return message with node xml
     */
    protected String createXmlMessage(String nodenr, String tableName, MMObjectNode node) {
        return createMessage(nodenr, tableName, "x", node.toXML());
    }
    
    /**
     * Create message to send to other servers
     * 
     * @param nodenr node number
     * @param tableName node type (tablename)
     * @param type command type
     * @return message
     */
    protected String createMessage(int nodenr, String tableName, String type) {
        return createMessage("" + nodenr, tableName, type, null);
    }
    
    /**
     * Create message to send to other servers
     * 
     * @param nodenr node number
     * @param tableName node type (tablename)
     * @param type command type
     * @param xml node xml
     * @return message
     */
    protected String createMessage(String nodenr,String tableName,String type, String xml) {
        String message = mmbase.getMachineName()+","+(follownr++)+","+nodenr+","+tableName+","+type;
        if (xml != null) {
            message += "," + xml +"\n";
        }
        return message;
    }
    
    /**
     * @see org.mmbase.module.core.MMBaseChangeInterface#waitUntilNodeChanged(org.mmbase.module.core.MMObjectNode)
     */
    public boolean waitUntilNodeChanged(MMObjectNode node) {
        try {
            MMBaseWaitNode wnode = new MMBaseWaitNode(node);
            waitingNodes.addElement(wnode);
            wnode.doWait(60*1000);
            waitingNodes.removeElement(wnode);
        } catch(Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            kicker.setPriority(Thread.NORM_PRIORITY+1);
            doWork();
        } catch(Exception e) {
            log.error(Logging.stackTrace(e));
        }
    }
    
    /**
     * Let the thread do his work
     */
    private void doWork() {
        String chars;
        String machine,vnr,id,tb,ctype;
        StringTokenizer tok;
        while(kicker!=null) {
            chars=(String)nodesToSpawn.get();
            if (log.isDebugEnabled()) {
                log.debug("RECEIVE=>" + new String(chars));
            }
            spawncount++;
            tok=new StringTokenizer(chars,",");
            if (tok.hasMoreTokens()) {
                machine=tok.nextToken();
                if (tok.hasMoreTokens()) {
                    vnr=tok.nextToken();
                    if (tok.hasMoreTokens()) {
                        id=tok.nextToken();
                        if (tok.hasMoreTokens()) {
                            tb=tok.nextToken();
                            if (tok.hasMoreTokens()) {
                                ctype=tok.nextToken();
                                if (!ctype.equals("s")) {
                                    handleMsg(machine,vnr,id,tb,ctype);
                                } else {
                                    if (tok.hasMoreTokens()) {
                                        String xml=tok.nextToken("");
                                        commitXML(machine,vnr,id,tb,ctype,xml);
                                    } else log.error("doWork("+chars+"): 'xml' could not be extracted from this string!");
                                }
                            } else log.error("doWork("+chars+"): 'ctype' could not be extracted from this string!");
                        } else log.error("doWork("+chars+"): 'tb' could not be extracted from this string!");
                    } else log.error("doWork("+chars+"): 'id' could not be extracted from this string!");
                } else log.error("doWork("+chars+"): 'vnr' could not be extracted from this string!");
            } else log.error("doWork("+chars+"): 'machine' could not be extracted from this string!");
        }
    }

    /**
     * Handle message
     * 
     * @param machine machine name
     * @param fnr follow up number
     * @param id node number
     * @param tb tablename
     * @param ctype command type
     * @return <code>true</code> when message is handled
     */
    public boolean handleMsg(String machine, String fnr, String id, String tb, String ctype) {
        // check if MMBase is 100% up and running, if not eat event
        if (!mmbase.getState()) return true;

        MMObjectBuilder bul = mmbase.getMMObject(tb);
        if (bul == null) {
            log.error("Unknown builder=" + tb);
            return false;
        }
        if (machine.equals(mmbase.getMachineName())) {
            if (bul != null) {
                nodeChanged(bul, machine, id, tb, ctype, false);
            }
        }
        else {
            try {
                if (ctype.equals("g")) {
                    if (bul!=null) {
                        MMObjectNode node = bul.getNode(id);
                        if (node!=null) {
                            // well send it back !
                            String chars = createXmlMessage(id, tb, node);
                            nodesToSend.append(chars);
                        } else {
                            log.error("can't get node " + id);
                        }
                    } else {
                        log.error("can't find builder " + bul);
                    }
                }
                else {
                    nodeChanged(bul, machine, id, tb, ctype, true);
                }
            } catch(Exception e) {
                log.error(Logging.stackTrace(e));
            }
        }
        return true;
    }

    /**
     * Process node changed
     * 
     * @param bul builder
     * @param machine machine name
     * @param id node number
     * @param tb tablename
     * @param ctype command type
     * @param remote update from remote
     */
    private void nodeChanged(MMObjectBuilder bul,String machine,
                            String id, String tb, String ctype, boolean remote) {
        
        if (spawnThreads) {
            new MessageProbe(this,bul,machine,id,tb,ctype,remote);
        }
        else {
            try {
                if (remote) {
                    bul.nodeRemoteChanged(machine,id,tb,ctype);
                    checkWaitingNodes(id);
                } else {
                    bul.nodeLocalChanged(machine,id,tb,ctype);
                    checkWaitingNodes(id);
                }
            } catch(Throwable t) {
                log.equals(Logging.stackTrace(t));
            }
            
        }
    }
    
    /**
     * Check collection of waiting nodes fir the changed node
     * @param snumber changed node number
     */
    public void checkWaitingNodes(String snumber) {
        try {
            int number = Integer.parseInt(snumber);
            for (Enumeration e=waitingNodes.elements();e.hasMoreElements();) {
                MMBaseWaitNode n = (MMBaseWaitNode) e.nextElement();
                if (n.doNotifyCheck(number)) {
                    waitingNodes.removeElement(n);
                    log.debug("waitingNodes size=" + waitingNodes.size());
                }
            }
        } catch(Exception e) {
            log.error("not a valid number " + snumber);
        }
    }
    
    /**
     * @javadoc
     */
    public boolean commitXML(String machine,String fnr,String id,String tb,String ctype,String xml) {
        try {
            MMObjectBuilder bul = mmbase.getMMObject(tb);
            if (bul==null) {
                log.error("Unknown builder=" + tb);
                return false;
            }
            if (!machine.equals(mmbase.getMachineName())) {
                MMObjectNode node=bul.getNode(id);
                if (node!=null) {
                    // well send it back !
                    mergeXMLNode(node,xml);
                    node.commit();
                } else {
                    log.error("can't get node "+id);
                }
            }
        }
        catch(Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

    /**
     * @javadoc
     */
    private void mergeXMLNode(MMObjectNode node,String body) {
        StringTokenizer tok = new StringTokenizer(body, "\n\r");
        String xmlline = tok.nextToken();
        String docline = tok.nextToken();

        String builderline = tok.nextToken();
        String endtoken = "</" + builderline.substring(1);

        // weird way
        String nodedata = body.substring(body.indexOf(builderline)+builderline.length());
        nodedata = nodedata.substring(0,nodedata.indexOf(endtoken));

        int bpos=nodedata.indexOf("<");
        while (bpos != -1) {
            String key = nodedata.substring(bpos + 1);
            key = key.substring(0, key.indexOf(">"));
            String begintoken = "<" + key + ">";
            endtoken = "</" + key + ">";

            String value = nodedata.substring(nodedata.indexOf(begintoken) + begintoken.length());
            value = value.substring(0, value.indexOf(endtoken));

            // set the node
            int dbtype = node.getDBType(key);
            if (!key.equals("number") && !key.equals("otype") && !key.equals("owner"))
                    node.setValue(key, dbtype, value);

            nodedata = nodedata.substring(nodedata.indexOf(endtoken) + endtoken.length());
            bpos = nodedata.indexOf("<");
        }
    }
    
}
