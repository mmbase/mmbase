/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/*
$Id: MMRemoteProbe.java,v 1.9 2001-03-15 14:40:39 vpro Exp $

$Log: not supported by cvs2svn $
Revision 1.8  2001/01/24 14:10:26  vpro
Davzev: Small debug change in doWork() and printStackTrace() added in toXML() catch.

Revision 1.7  2000/12/19 17:10:54  vpro
Davzev: Still managed to add some debug and comment

Revision 1.6  2000/12/19 13:31:03  vpro
Davzev: Added cvs comments

*/
package org.mmbase.remote;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 * 
 * @version $Revision: 1.9 $ $Date: 2001-03-15 14:40:39 $
 * @author Daniel Ockeloen
 */
public class MMRemoteProbe implements Runnable {
	private String classname = getClass().getName();
	private boolean debug = true;
	private void debug(String msg) {System.out.println(classname+":"+msg);}

	private final static int SLEEPTIME = 60 * 1000;
	Thread kicker = null;
	MMProtocolDriver con=null;
	String servicenr;
	Vector runningServices;

	public MMRemoteProbe(Vector runningServices,MMProtocolDriver con,String servicenr) {
		if (debug) debug("MMRemoteProbe(): "+runningServices+","+con+","+servicenr+") Initializing"); 
		this.con=con;
		this.servicenr=servicenr;
		this.runningServices=runningServices;
		init();
	}

	/**
	 * Calls start()
	 */
	public void init() {
		this.start();	
	}

	/**
	 * Starts the admin Thread.
	 */
	public void start() {
		/* Start up the main thread */
		if (kicker == null) {
			if (debug) debug("start(): Creating & starting new MMRemoteProbe thread.");
			kicker = new Thread(this,"MMRemoteProbe");
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

	public void run() {
		while (kicker!=null) {
			try {
				kicker.setPriority(Thread.NORM_PRIORITY+1);  
				doWork();
			} catch(Exception e) {
				debug("run(): ERROR: while doWork(): ");
				e.printStackTrace();
			}
		}
	}

 	/**
	 * Calls routine to commit the mmserver node of this remote builder to mmbase
	 * space and goes to sleep, after sleep it calls a routine which triggers the
	 * maintainance routines of all running services.
	 */
	public void doWork() {
		try {
			if (debug) debug("doWork(): "+SLEEPTIME+" ms are over, calling con.CommitNode and going to sleep.");
			con.commitNode(servicenr,"mmservers",toXML());
			Thread.sleep(SLEEPTIME);
		} catch(Exception e) {
			debug("doWork(): ERROR: while commitNode("+servicenr+",mmservers,toXML()) : ");
			e.printStackTrace();
		}
		if (debug) debug("doWork(): Just awoken...yawn... calling callMaintainances():");
		callMaintainances();
	}

	/**
	 * Writes mmserver node in xml.
	 * @return the mmserver node as xml.
	 */
	public String toXML() {
		String host="";
		try {
			host=InetAddress.getLocalHost().getHostName();
		} catch(Exception e) { 
			debug("toXML(): ERROR: Could not get localhost address!"); 
			e.printStackTrace();
		}
		String body="<?xml version=\"1.0\"?>\n";
		body+="<!DOCTYPE mmnode.mmservers SYSTEM \"http://openbox.vpro.nl/mmnode/mmservers.dtd\">\n";
		body+="<mmservers>\n";
		body+="<name>"+servicenr+"</name>\n";
		body+="<state>1</state>\n";
		body+="<atime>"+(int)(System.currentTimeMillis()/1000)+"</atime>\n";
		body+="<host>"+con.getProtocol()+"://"+con.getLocalHost()+":"+con.getLocalPort()+"</host>\n";
		body+="</mmservers>\n";
		return(body);
	}
	
	/**
	 * Goes through the list of running services and triggers their maintainance routine.
	 */
	void callMaintainances() {
		if (debug) debug("callMaintainances(): Go through list of running services and trigger their maintainance routine");
		Enumeration f=runningServices.elements();
		for (;f.hasMoreElements();) {
			RemoteBuilder serv=(RemoteBuilder)f.nextElement();
			serv.maintainance();
		}
	}
}
