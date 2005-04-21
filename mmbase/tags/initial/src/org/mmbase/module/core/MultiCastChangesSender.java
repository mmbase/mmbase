/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/
package org.mmbase.module.core;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.util.*;

/**
 * MultiCastChangesSender is a thread object sending the nodes found in the
 * sending queue over the multicast 'channel'
 *
 * @version 12-May-1999
 * @author Rico Jansen
 */
public class MultiCastChangesSender implements Runnable {

	Thread kicker = null;
	MMBaseMultiCast parent=null;
	Queue nodesTosend;
	InetAddress ia;
	MulticastSocket ms;
	int mport;
	int dpsize;

	public MultiCastChangesSender(MMBaseMultiCast parent,Queue nodesTosend) {
		this.parent=parent;
		this.nodesTosend=nodesTosend;
		init();
	}

	public void init() {
		this.start();	
	}

	public void start() {
		/* Start up the main thread */
		if (kicker == null) {
			kicker = new Thread(this,"MulticastSender");
			kicker.start();
		}
	}
	
	public void stop() {
		/* Stop thread */
		try {
			ms.leaveGroup(ia);
			ms.close();		
		} catch (Exception e) {
		}
		kicker.setPriority(Thread.MIN_PRIORITY);  
		kicker.suspend();
		kicker.stop();
		kicker = null;
	}

	/**
	 * admin probe, try's to make a call to all the maintainance calls.
	 */
	public void run() {
		try {
			try {
				mport=parent.mport;
				dpsize=parent.dpsize;
				ia = InetAddress.getByName(parent.multicastaddress);
				ms = new MulticastSocket();
				ms.joinGroup(ia);
			} catch(Exception e) {
				System.out.println("MMBaseMultiCast -> ");
				e.printStackTrace();
			}
			doWork();
		} catch (Exception e) {
			System.out.println("MultiCastChangesSender -> ");
			e.printStackTrace();
		}
	}

	private void doWork() {
		byte[] data;
		DatagramPacket dp;
		String chars;
		while(kicker!=null) {
			chars=(String)nodesTosend.get();
			parent.incount++;
			data = new byte[chars.length()];
			chars.getBytes(0,chars.length(), data, 0);		
			dp = new DatagramPacket(data, data.length, ia,mport);
			try {
				ms.send(dp, (byte)1);
			} catch (IOException e) {
				System.out.println("MMBaseMulticast -> can't send message");
				e.printStackTrace();
			}
			parent.outcount++;
		}
	}
}
