/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.util.LRUHashtable;

/**
 * Simple file cache system that can be used by any servlet
 *
 * @author  $Author: wwwtech $ 
 * @version $Revision: 1.7 $ $Date: 2000-06-20 14:56:13 $
 */
public class cache extends Module implements cacheInterface {

	private String 	classname 	= getClass().getName();
	private boolean debug	 	= false;	
	private void	debug(String msg ){ System.out.println(classname+":"+msg); }

	boolean state_up = false;
	int hits,miss;
	private int MaxLines=1000;
	private int MaxSize=100*1024;
	private boolean active=true;
	LRUHashtable lines = new LRUHashtable( MaxLines );

	public void onload() {
	}

	public void reload() {
		readParams();
		if( MaxLines > 0 ) lines = new LRUHashtable( MaxLines );
	}

	public void shutdown() {
	}


	/**
	 * Simple file cache system that can be used by any servlet
	 */
	public cache() {
	}

	/**
	* old interface to the inner table, will be removed soon
	*/
	public LRUHashtable lines() {
		return(lines);
	}

	/**
	* try to get a cacheline from cache, returns null if not found
	*/
	public cacheline get(Object wanted) {
		if (!active) return(null);
		cacheline o=(cacheline)lines.get(wanted);
		if (o==null) {
			//if( debug ) debug("WOW CACHE MIS = "+wanted);
			miss++;
		} else {
			//if( debug ) debug("WOW CACHE HIT = "+wanted);
			hits++;
		}
		return(o);
	}

	/**
	* try to put a cacheline in cache, returns old one if available
	* in all other cases returns null.
	*/
	public cacheline put(Object key,Object value) {
		if (!active) return(null);
		// check if there is still room in the cache
		// there is room so look at the cacheline and check size
		cacheline line=(cacheline)value;
		// if size is to big ignore the entry
		if (line.filesize<MaxSize) {	
			// cacheline is oke place it in cache
			return((cacheline)lines.put(key,value));
		} else {
			// cacheline to big
			return(null);
		}
	}
	
	/**
	* Clear the whole cache in one go
	*/
	public boolean clear() {
		lines.clear();
		return(false);
	}

	/**
	* Remove the entry identified by key from the cache
	*/
	public cacheline remove(Object key) {
		return (cacheline)lines.remove(key);
	}

	public void init() {
		if (!state_up) {
			state_up=true;
		}
		readParams();
		if( MaxLines > 0 ) lines = new LRUHashtable( MaxLines );
	}

	public void unload() {
	}

	public Hashtable state() {
		state.put("Hits",""+hits);
		state.put("Misses",""+miss);
		if (hits!=0 && miss!=0) {
			state.put("Cache hits %",""+((hits+miss)*100)/hits);
			state.put("Cache misses %",""+((hits+miss)*100)/miss);
		}
		state.put("Number cachelines",""+lines.size());
		cacheline line;
		int size=0;
		for (Enumeration t=lines.elements();t.hasMoreElements();) {
			line=(cacheline)t.nextElement();
			size+=line.filesize;
		}
		state.put("Cache Size (in kb)",""+(size+1)/1024);
		return(state);
	}
	
	/** 
	* maintainance call, will be called by the admin to perform managment
	* tasks. This can be used instead of its own thread.
	*/
	public void maintainance() {
		// do nothing, LRUHashtable does the trick now.
	}

	void readParams() {
		String tmp = null;
		try
		{
			tmp=getInitParameter("MaxLines");
			if (tmp!=null) MaxLines=Integer.parseInt(tmp);
			tmp=getInitParameter("MaxSize");
			if (tmp!=null) MaxSize=Integer.parseInt(tmp)*1024;
			tmp=getInitParameter("Active");
		} catch (NumberFormatException e ) {
			debug("readParams(): ERROR: " + e ) ;
		}

		if (tmp!=null && (tmp.equals("yes") || tmp.equals("Yes"))) {
			active=true;
		} else {
			active=false;
	
		}
	}	
	public String getModuleInfo() {
		return("this module provides cache function for http requests");	
	}
}
