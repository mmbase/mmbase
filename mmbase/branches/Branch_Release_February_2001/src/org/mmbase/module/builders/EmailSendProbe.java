/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * admin module, keeps track of all the worker pools
 * and adds/kills workers if needed (depending on
 * there load and info from the config module).
 *
 * @version 27 Mar 1997
 * @author Daniel Ockeloen
 */
public class EmailSendProbe implements Runnable {

	Thread kicker = null;
	Email parent=null;
	SortedVector tasks= new SortedVector(new MMObjectCompare("mailtime"));

	// Active Node
	MMObjectNode anode=null;

	public EmailSendProbe(Email parent) {
		this.parent=parent;
		init();
	}

	public void init() {
		this.start();	
	}


	/**
	 * Starts the admin Thread.
	 */
	public void start() {
		/* Start up the main thread */
		if (kicker == null) {
			kicker = new Thread(this,"emailsendprobe");
			kicker.start();
		}
	}
	
	/**
	 * Stops the admin Thread.
	 */
	public void stop() {
		/* Stop thread */
		kicker.setPriority(Thread.MIN_PRIORITY);  
		kicker.suspend();
		kicker.stop();
		kicker = null;
	}

	/**
	 * blocked on the first task in the queue
	 */
	public synchronized void run() {
		kicker.setPriority(Thread.MIN_PRIORITY+1);  
		while (kicker!=null) {
				if (tasks.size()>0) {
					anode=(MMObjectNode)tasks.elementAt(0);
				} else {
					anode=null;
				}
				try {
					if (anode==null) {
						// so no task in the future wait a long time then
						wait(3600*1000);
					} else {
						int ttime=(int)((DateSupport.currentTimeMillis()/1000)); 
						ttime=anode.getIntValue("mailtime")-ttime;
						if (ttime<3) {
							// time has come handle this task now !
							try {
								parent.performTask(anode);
							} catch (Exception e) {
								System.out.println("emailsendprobe : performTask failed"+anode);
							}
							tasks.removeElement(anode);
						} else {
							wait(ttime*1000);
						}
					}
				} catch (InterruptedException e){}
		}
	}

	public synchronized boolean putTask(MMObjectNode node) {
		boolean res;
		if (!containsTask(node)) {
			tasks.addSorted(node);
			res=true;
		} else {
			res=replaceTask(node);
		}
		// is the active node
		if (tasks.size()==0 || node==tasks.elementAt(0)) {
			notify();
		}
		return(true);
	}

	public boolean containsTask(MMObjectNode node) {
		int number=node.getIntValue("number");
		Enumeration e=tasks.elements();
		while (e.hasMoreElements()) {
			MMObjectNode node2=(MMObjectNode)e.nextElement();
			if (node2.getIntValue("number")==number) {
				return(true);
			}
		}
		return(false);
	}


	public boolean replaceTask(MMObjectNode node) {
		int number=node.getIntValue("number");
		Enumeration e=tasks.elements();
		if (e.hasMoreElements()) {
			MMObjectNode node2=(MMObjectNode)e.nextElement();
			if (node2.getIntValue("number")==number) {
				tasks.removeElement(node2);
				tasks.addSorted(node);
				return(true);
			}
		}
		return(false);
	}
}
