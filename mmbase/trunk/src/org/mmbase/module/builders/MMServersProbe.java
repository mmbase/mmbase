/*

$Id: MMServersProbe.java,v 1.3 2000-03-29 10:59:23 wwwtech Exp $

$Log: not supported by cvs2svn $
Revision 1.2  2000/02/24 14:08:20  wwwtech
- (marcel) Changed System.out into debug and added headers

*/
package org.mmbase.module.builders;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Daniel Ockeloen
 * @version0 $Revision: 1.3 $ $Date: 2000-03-29 10:59:23 $ 
 */
public class MMServersProbe implements Runnable {

	private String classname = getClass().getName();
	private boolean debug = false;
	private void debug( String msg ) { System.out.println( classname+":"+msg ); }

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
				debug("run(): ERROR: Exception in mmservers thread!");
				e.printStackTrace();
			}
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

		try {Thread.sleep(30*1000);} catch (InterruptedException e){}
		while (kicker!=null) {
			parent.probeCall();
			try {Thread.sleep(60*1000);} catch (InterruptedException e){}
		}
	}


}
