/* -*- tab-width: 4; -*-

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/*

$Id: MMServersProbe.java,v 1.10 2003-03-04 14:12:22 nico Exp $

$Log: not supported by cvs2svn $
Revision 1.9  2002/06/19 19:17:30  michiel
michiel: cleaning project: made the interval between probings configurable. Removed some unused variables.

Revision 1.8  2001/04/10 12:20:38  michiel
michiel: new logging system.

Revision 1.7  2000/07/22 21:38:35  daniel
needed a or not a and :)

Revision 1.6  2000/07/22 21:30:20  daniel
small startup fix

Revision 1.5  2000/07/22 10:47:46  daniel
Now uses the MMbase up signal

Revision 1.4  2000/03/30 13:11:32  wwwtech
Rico: added license

Revision 1.3  2000/03/29 10:59:23  wwwtech
Rob: Licenses changed

Revision 1.2  2000/02/24 14:08:20  wwwtech
- (marcel) Changed System.out into debug and added headers

*/
package org.mmbase.module.builders;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @author Daniel Ockeloen
 * @version0 $Revision: 1.10 $ $Date: 2003-03-04 14:12:22 $ 
 */
public class MMServersProbe implements Runnable {

    private static Logger log = Logging.getLoggerInstance(MMServersProbe.class.getName()); 

	Thread kicker = null;
	MMServers parent=null;

	public MMServersProbe(MMServers parent) {
		this.parent=parent;
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
			kicker = new Thread(this,"mmserversprobe");
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
				log.error("Exception in mmservers thread!" + Logging.stackTrace(e));
			}
		}
	}

	/**
	 * Main work loop
	 */
	public void doWork() {
		kicker.setPriority(Thread.MIN_PRIORITY+1);  

        int probeInterval = 60 * 1000;

		// ugly pre up polling
		while (parent.mmb==null || parent.mmb.getState()==false) {
			try {
				Thread.sleep(2*1000);
			} catch (InterruptedException e){
                log.debug(e.toString());
			}
		}

        String tmp = parent.getInitParameter("ProbeInterval"); 
        if (tmp != null) {
            if (log.isDebugEnabled()) log.debug("ProbeInterval was configured to be " + tmp + " seconds");
            probeInterval = Integer.parseInt(tmp) * 1000;
        }

		while (kicker != null) {
			parent.probeCall();
			try {
                Thread.sleep(probeInterval);
            } catch (InterruptedException e) {
                log.debug(e.toString());
            }
		}
	}


}
