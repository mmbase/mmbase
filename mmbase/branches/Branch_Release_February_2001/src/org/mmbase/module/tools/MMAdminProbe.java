/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/*

$Id: MMAdminProbe.java,v 1.2 2000-08-27 21:00:41 daniel Exp $

$Log: not supported by cvs2svn $
Revision 1.1  2000/08/27 19:04:46  daniel
changed from examples to admin module

*/

package org.mmbase.module.tools;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Daniel Ockeloen
 * @version0 $Revision: 1.2 $ $Date: 2000-08-27 21:00:41 $ 
 */
public class MMAdminProbe implements Runnable {

	private String classname = getClass().getName();
	private boolean debug = false;
	private void debug( String msg ) { System.out.println( classname+":"+msg ); }

	Thread kicker = null;
	MMAdmin parent=null;
	long sleeptime=0;
	long startdelay=2000;

	public MMAdminProbe() {
	}

	public MMAdminProbe(MMAdmin parent) {
		this.parent=parent;
		startdelay=2000;
		init();
	}

	public MMAdminProbe(MMAdmin parent,long sleeptime) {
		this.sleeptime=sleeptime;
		this.parent=parent;
		startdelay=0;
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
			kicker = new Thread(this,"mmexamplesprobe");
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
		try {
			doWork();
		} catch(Exception e) {
			debug("run(): ERROR: Exception in mmexamples thread!");
			e.printStackTrace();
		}
	}

	/**
	 * Main work loop
	 */
	public void doWork() {
		kicker.setPriority(Thread.MIN_PRIORITY+1);  
		MMObjectNode node,node2;
		boolean needbreak=false;
		int id;

		// ugly pre up polling
		while (parent.mmb.getState()==false) {
			try {
				Thread.sleep(startdelay);
			} catch (InterruptedException e){
			}
		}

		try {
			Thread.sleep(sleeptime);
		} catch (InterruptedException e){
		}

		parent.probeCall();
	}


}
