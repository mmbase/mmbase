package org.mmbase.module.builders.vwms;

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
public class VwmProbe implements Runnable {

	Thread kicker = null;
	VwmProbeInterface parent=null;
	SortedVector tasks= new SortedVector(new MMObjectCompare("wantedtime"));
	// Active Node
	MMObjectNode anode=null;
	PerformProbe pp;

	public VwmProbe(VwmProbeInterface parent) {
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
			kicker = new Thread(this,"Vwmprobe");
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
		System.out.println("VwmProbe -> started probe");
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
						ttime=anode.getIntValue("wantedtime")-ttime;
						if (ttime<3) {
							// time has come handle this task now !
							System.out.println("VwmProbe Handle Task NOW");
							try {
//								parent.performTask(anode);
								pp=new PerformProbe(parent,anode);
							} catch (Exception e) {
								System.out.println("VWMprobe : performTask failed"+anode);
							}
							tasks.removeElement(anode);
						} else {
							System.out.println("VwmProbe wait for "+ttime);
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
			System.out.println("VwmProbe : notify()");
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
