/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */
package org.mmbase.module.tools;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Bootstrap class that wait's in a thead for MMBase to have a running state. Once the state is running
 * it calls MMAdmin.probeCall() and finishes.
 * @author Daniel Ockeloen
 * @version $Id: MMAdminProbe.java,v 1.7 2003-03-26 10:15:21 kees Exp $
 */
public class MMAdminProbe implements Runnable {
    
    private static Logger log = Logging.getLoggerInstance(MMAdminProbe.class.getName());
    
    private Thread kicker = null;
    
    /**
     *  DEFAULT_SLEEP_TIME = 0 ms
     **/
    public final static long DEFAULT_SLEEP_TIME = 0;
    
    long sleeptime = DEFAULT_SLEEP_TIME;
    
    MMAdmin parent=null;
    
    /**
     * DEFAULT_START_DELAY = 2000; ms
     **/
    public final static long DEFAULT_START_DELAY = 2000;
    
    long startdelay=DEFAULT_START_DELAY;
    
    public MMAdminProbe(MMAdmin parent) {
        this.parent=parent;
        init();
    }
    
    public MMAdminProbe(MMAdmin parent,long sleeptime) {
        this.parent=parent;
        
        this.sleeptime=sleeptime;
        
        startdelay=0;
        init();
    }
    
    public void init() {
        if (kicker ==null){
            kicker = new Thread(this,"MMAdminProbe");
            kicker.setDaemon(true);
            kicker.start();
        } else {
            log.error("MMAdminProbe thread was already running");
        }
    }
    
    
    public void run() {
        try {
            while (parent.mmb.getState()==false) {
                try {Thread.sleep(startdelay);} catch (InterruptedException e){ return;}
            }
            try { Thread.sleep(sleeptime); } catch (InterruptedException e){ return;}
            parent.probeCall();
        } catch(Exception e) {
            log.error(e.getMessage());
            log.error(Logging.stackTrace(e));
        }
    }
}