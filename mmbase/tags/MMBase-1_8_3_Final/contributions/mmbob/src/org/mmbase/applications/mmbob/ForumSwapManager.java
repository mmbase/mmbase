/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.mmbob;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 *
 */
public class ForumSwapManager implements Runnable {

    // logger
    static private Logger log = Logging.getLoggerInstance(ForumSwapManager.class); 

    // thread
    Thread kicker = null;

    int sleeptime;

    /**
    */
    public ForumSwapManager(int sleeptime) {
        this.sleeptime=sleeptime;
	init();
    }

    /**
    * init()
    */
    public void init() {
        this.start();    
    }


    /**
     * Starts the main Thread.
     */
    public void start() {
        /* Start up the main thread */
        if (kicker == null) {
            kicker = new Thread(this,"forumswapmanager");
            kicker.start();
        }
    }
    
    /**
     * Stops the main Thread.
     */
    public void stop() {
        /* Stop thread */
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
                log.error("run(): ERROR: Exception in forummmbasesyncer thread!");
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
		try {
			expirePosters();
			maintainMemoryCaches();		
	            	Thread.sleep(sleeptime);
		} catch (InterruptedException f2){}
        }
    }

    private void expirePosters() {
	Enumeration e=ForumManager.getForums();
	while (e.hasMoreElements()) {
		Forum f=(Forum)e.nextElement();
		int expiretime=((int)(System.currentTimeMillis()/1000))-(f.getPosterExpireTime());
		Enumeration e2=f.getPostersOnline();
		while (e2.hasMoreElements()) {
			Poster p=(Poster)e2.nextElement();
			if (p.getLastSeen()<expiretime) {
				f.removeOnlinePoster(p);
			}
		}
	}
    }


    private void maintainMemoryCaches() {
	ForumManager.maintainMemoryCaches();
    }
}
