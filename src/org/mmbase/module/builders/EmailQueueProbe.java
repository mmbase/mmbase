/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.module.builders;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 * @version0 $Revision: 1.3 $ $Date: 2003-03-04 14:12:21 $ 
 */
public class EmailQueueProbe implements Runnable {

    static private Logger log = Logging.getLoggerInstance(EmailQueueProbe.class.getName()); 

    Thread kicker = null;
    int sleeptime;
    EmailSendProbe parent=null;

    public EmailQueueProbe(EmailSendProbe parent,int sleeptime) {
        this.parent=parent;
        this.sleeptime=sleeptime;
        init();
    }

    public void init() {
        this.start();    
    }


    /**
     * Starts the main Thread.
     */
    public void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(this,"emailqueueprobe");
            kicker.start();
        }
    }
    
    /**
     * Stops the main Thread.
     */
    public void stop() {
        /* Stop thread */
        kicker.setPriority(Thread.MIN_PRIORITY);  
        kicker.suspend();
        kicker.stop();
        kicker = null;
    }

    /**
     * Main loop, exception protected
     */
    public void run () {
        kicker.setPriority(Thread.MIN_PRIORITY+1);  
        while (kicker!=null) {
            try {
                doWork();
            } catch(Exception e) {
                log.error("run(): ERROR: Exception in emailqueueprobe thread!");
                log.error(Logging.stackTrace(e));
            }
        }
    }

    /**
     * Main work loop
     */
    public void doWork() {
        kicker.setPriority(Thread.MIN_PRIORITY+1);  

        while (kicker!=null) {
            parent.checkQueue();
            try {Thread.sleep(sleeptime*1000);} catch (InterruptedException e){}
        }
    }


}
