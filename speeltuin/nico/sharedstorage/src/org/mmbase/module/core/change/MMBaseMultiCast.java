/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core.change;

import java.util.Map;

import org.mmbase.module.core.MMBase;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.UtilReader;


/**
 * MMBaseMultiCast is a thread object that reads the receive queue
 * and spawns them to call the objects (listeners) who need to know.
 * The MMBaseMultiCast start two threads to handle the sending and receiving of
 * multicast messages.
 * 
 * @author Daniel Ockeloen
 * @author Rico Jansen
 * @author Nico Klasens
 * @version $Id: MMBaseMultiCast.java,v 1.1 2004-09-29 19:34:59 nico Exp $
 */
public class MMBaseMultiCast extends MMBaseSharedStorage {

    private static final Logger log = Logging.getLoggerInstance(MMBaseMultiCast.class);
    
    public static final String CONFIG_FILE = "multicast.xml";

    /**
     * Defines what 'channel' we are talking to when using multicast.
     */
    private String multicastHost = "ALL-SYSTEMS.MCAST.NET";

    /**
     * Determines on what port does this multicast talking between nodes take place.
     * This can be set to any port but check if something else on
     * your network is allready using multicast when you have problems.
     */
    private int multicastPort = 4243;

    /** Determines the Time To Live for a multicast datapacket */
    private int multicastTTL = 1;

    /** Datapacket receive size */
    private int dpsize = 64*1024;

    /** Sender which reads the nodesToSend Queue amd puts the message on the line */
    private MultiCastChangesSender mcs;
    /** Receiver which reads the message from the line and puts message in the nodesToSpawn Queue */
    private MultiCastChangesReceiver mcr;

    /**
     * @see org.mmbase.module.core.MMBaseChangeInterface#init(org.mmbase.module.core.MMBase)
     */
    public void init(MMBase mmb) {
        super.init(mmb);
        
        UtilReader reader = new UtilReader(CONFIG_FILE);
        Map properties = reader.getProperties();
        
        String tmp = (String) properties.get("spawnthreads");
        if (tmp != null && !tmp.equals("")) {
            spawnThreads = "true".equals(tmp);
        }
        
        tmp = (String) properties.get("multicastport");
        if (tmp != null && !tmp.equals("")) {
            try {
                multicastPort = Integer.parseInt(tmp);
            } catch (Exception e) {}
        }

        tmp = (String) properties.get("multicasthost");
        if (tmp != null && !tmp.equals("")) {
            multicastHost = tmp;
        }

        tmp = (String) properties.get("multicastTTL");
        if (tmp != null && !tmp.equals("")) {
            try {
                multicastTTL = Integer.parseInt(tmp);
            } catch (Exception e) {}
        }

        tmp = (String) properties.get("dpsize");
        if (tmp != null && !tmp.equals("")) {
            try {
                dpsize = Integer.parseInt(tmp);
            } catch (Exception e) {}
        }

        log.info("multicasthost: " + multicastHost);
        log.info("multicastport: " + multicastPort);
        log.info("multicastTTL: " + multicastTTL);
        log.info("datapacketsize: " + dpsize);
        start();
    }

    
    protected void startCommunicationThreads() {
        mcs = new MultiCastChangesSender(multicastHost, multicastPort, multicastTTL, nodesToSend);
        mcr = new MultiCastChangesReceiver(multicastHost, multicastPort, dpsize, nodesToSpawn);
    }
    
    protected void stopCommunicationThreads() {
        mcs.stop();
        mcr.stop();
    }

}
