/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.net.*;
import java.io.*;

import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * MultiCastChangesSender is a thread object sending the nodes found in the
 * sending queue over the multicast 'channel'
 * @javadoc
 *
 * @author Rico Jansen
 * @version $Id: MultiCastChangesSender.java,v 1.10 2004-07-16 11:23:58 pierre Exp $
 */
public class MultiCastChangesSender implements Runnable {

    // logging
    private static Logger log = Logging.getLoggerInstance(MultiCastChangesSender.class.getName());

    /**
     * @javadoc
     * @scope private
     */
    Thread kicker = null;
    /**
     * @javadoc
     * @scope private
     */
    MMBaseMultiCast parent=null;
    /**
     * @javadoc
     * @scope private
     */
    Queue nodesTosend;
    /**
     * @javadoc
     * @scope private
     */
    InetAddress ia;
    /**
     * @javadoc
     * @scope private
     */
    MulticastSocket ms;
    /**
     * Port for sending datapackets send by Multicast
     */
    int mport=4243;
    /**
     * Time To Live for datapackets send by Multicast
     */
    int mTTL=1;
    /**
     * @javadoc
     * @scope private
     */
    int dpsize;

    /**
     * @javadoc
     */
    public MultiCastChangesSender(MMBaseMultiCast parent,Queue nodesTosend) {
        this.parent=parent;
        this.nodesTosend=nodesTosend;
        init();
    }

    /**
     * @javadoc
     */
    public void init() {
        this.start();
    }

    /**
     * @javadoc
     */
    public void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(this,"MulticastSender");
            kicker.setDaemon(true);
            kicker.start();
        }
    }

    /**
     * @javadoc
     */
    public void stop() {
        /* Stop thread */
        try {
            ms.leaveGroup(ia);
            ms.close();
        } catch (Exception e) {
        }
        kicker.setPriority(Thread.MIN_PRIORITY);
        kicker = null;
    }

    /**
     * admin probe, try's to make a call to all the maintainance calls.
     */
    public void run() {
        try {
            try {
                mport=parent.mport;
                dpsize=parent.dpsize;
                mTTL=parent.mTTL;
                ia = InetAddress.getByName(parent.multicastaddress);
                ms = new MulticastSocket();
                ms.joinGroup(ia);
                ms.setTimeToLive(mTTL);
            } catch(Exception e) {
                log.error(Logging.stackTrace(e));
            }
            doWork();
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
        }
    }

    /**
     * @javadoc
     * @todo check what encoding to sue for getBytes()
     */
    private void doWork() {
        while(kicker != null) {
            String chars = (String)nodesTosend.get();
            parent.incount++;
            byte[] data = chars.getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, ia, mport);
            try {
                if (log.isDebugEnabled()) {
                    log.debug("SEND=>" + new String(chars));
                }
                ms.send(dp);
            } catch (IOException e) {
                log.error("can't send message");
                log.error(Logging.stackTrace(e));
            }
            parent.outcount++;
        }
    }
}
