/* -*- tab-width: 4; -*-

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.module.core.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *
 * @javadoc
 */

public class Statistics extends MMObjectBuilder {

    private static Logger log = Logging.getLoggerInstance(Statistics.class.getName()); 
    
	LRUHashtable shadowsCache=new LRUHashtable(250);
	int saveDelay=0;
	boolean processingDirty=false;
	LRUHashtable alias2number = new LRUHashtable(250);

	Vector dirty=new Vector(); // Hack vector to signal dirty

	public Statistics() {
		new StatisticsProbe(this);
	}

	private Hashtable vector2hashtable (Vector v) {
		int n = v.size ();

		if (n == 0) return (null);

		Hashtable h = new Hashtable (n);
		MMObjectNode node;

		for (Enumeration e = v.elements (); e.hasMoreElements ();) {
			node = (MMObjectNode)e.nextElement ();	
	
			h.put (new Integer (node.getIntValue ("slicenumber")) ,node);
		}
	
		return (h);
	}


	/**
     */
	public Hashtable getShadows (String number) {
		//log.info("=====> ENTER: getShadows (" + number + ")");

		Hashtable hh = (Hashtable)(shadowsCache.get (number));
	
		if (hh == null) {
			//log.info("=====> Shadows NOT in cache, getting them from database.");
			Vector shadows = ((StatisticsShadow)mmb.getMMObject ("sshadow")).searchVector("parent=E"+number);
			hh = vector2hashtable (shadows);
			//log.info("=====> Got shadows from database: " + hh);
			shadowsCache.put (number, hh);
		} //else log.info("=====> Found shadows in cache: " + hh);
	
		//log.info("=====> LEAVE: getShadows ()");
	
		return (hh);
	}


	/**
     */
	public void delShadows (String number) {
		//log.info("=====> ENTER: delShadows (" + number + ")");
		Hashtable hh = (Hashtable)(shadowsCache.get (number));
	
		if (hh != null) {
			//log.info("=====> Deleting shadows of parent " + number + " from cache.");
			shadowsCache.remove(number);
		}
	}


	public Object getValue(MMObjectNode node,String field) {
		if (field.equals("linked(name)")) {
			//node.prefix="statistics.";
			String val=node.getStringValue("name");
			int pos=val.indexOf("Linked=");
			if (pos!=-1) {
				val=val.substring(pos+7);
			}
			node.prefix="";
			return(val);
		} 
		return(super.getValue(node,field));
	}



	/**
	 *
	 */
	public synchronized String setAliasCount(String alias, int incr) {
		String number=getAliasNumber(alias);
		if (number==null) {
			NewStat(alias,"Autogenerated",0,0,0,"",incr);
			return("");
		} else {
			return(setCount(number,incr));
		}
	}



	public String getAliasNumber(String alias) {
		// try to get the alias from the alias2number table
		String number=(String)alias2number.get(alias);
		if (number!=null) {
			// oke alias fount
			return(number);
		} else {
			// get the number from the database by a search
	
			alias=Encode.encode("ESCAPE_SINGLE_QUOTE", alias);
		    Enumeration w=search("WHERE name='"+alias+"'");
			while (w.hasMoreElements()) {
				MMObjectNode node=(MMObjectNode)w.nextElement();
				number=""+node.getIntValue("number");
                alias2number.put(alias,number);
				return(number);
			}
		}
		return(null);
	}

	/**
	 *
     */
	public synchronized String setCount (String number, int incr) {
		// log.info("Number=" + number + ", increase=" + incr);

		try {
            MMObjectNode stats = getNode (number);
	
            if (stats != null) {
                /* ----------------------------------------------------------
                 * First check if we really have to mess with the shadow-statistics.
                 * 
                 * Parameters for all statistics
                 *  hitTime    = the absolute time of the hit, in seconds.
                 *  statsBegin = the absolute starttime of the statistics, in seconds.
                 *  nrOfSlices = number of time-slices
                 *  interval   = size of a time-interval
                 * 
                 * Parameters for 'current' statistics
                 *  curSliceBegin = the relative starttime of the current statistics (relative w.r.t. statsBegin)
                 *  hits          = number of hits in last interval
                 */
                //int hitTime    = (int)(new java.util.Date().getTime() / 1000); // datefix
                int hitTime=(int)(System.currentTimeMillis()/1000);
	
                int statsStart = stats.getIntValue ("start");
                int nrOfSlices = stats.getIntValue ("timeslices");
                int interval   = stats.getIntValue ("timeinterval");
	
                int curSliceBegin = stats.getIntValue ("timeslice");
                int hits          = stats.getIntValue ("count");
	
                // log.info("Current statistics begin is " + (statsStart + curSliceBegin));
                // log.info("Hit came on time " + hitTime);
	
                if ((nrOfSlices < 1) || ((hitTime >= statsStart + curSliceBegin) && (hitTime < (statsStart + curSliceBegin + interval)))) { // This statistics-node has no shadow-statistics OR hit falls in current slice
                    hits += incr;
                    stats.setValue ("count", hits);
                    if (!dirty.contains(stats)) dirty.addElement(stats);
                    // log.info("Increased node " + number + " to " + hits);
                    return ("");
                }
                int curSliceNr = (curSliceBegin / interval) % nrOfSlices;
			
                /*-------------------------------------------------------------
                 * Time of the current slice has expired, so if it contains hits we should copy it
                 * to a shadow.
                 * 
                 * curSliceNr = slice number of the current statistics
                 */
                if (hits > 0) {
                    MMObjectNode nowSlice = getSlice (number,curSliceNr);
	
                    if (nowSlice == null) { // Node doesn't exist, so we have to make it
                        // log.info("Slice " + curSliceNr + " doesn't exist yet... making it");
                        StatisticsShadow ssbuild = ((StatisticsShadow)mmb.getMMObject ("sshadow"));
                        nowSlice = ssbuild.getNewNode ("logger");
                        nowSlice.setValue ("parent", java.lang.Integer.parseInt (number));
                        nowSlice.setValue ("slicenumber", curSliceNr);
                        nowSlice.setValue ("data", stats.getStringValue ("data"));
                        nowSlice.setValue ("start", curSliceBegin);
                        nowSlice.setValue ("stop", curSliceBegin + interval - 1);
                        nowSlice.setValue ("count", hits);
	
                        int id = ssbuild.insert ("logger", nowSlice); 
                        delShadows(number);
                    } else {
                        // log.info("Changing values of existing slice " + curSliceNr);
                        nowSlice.setValue ("start", curSliceBegin);
                        nowSlice.setValue ("stop", curSliceBegin + interval - 1);
                        nowSlice.setValue ("count", stats.getIntValue ("count"));
                        if (!dirty.contains(nowSlice)) dirty.addElement(nowSlice);
                    }
                } 
                // else log.info("Current stats have no hits, so no need to store it");
	
                /*--------------------------------------------------------------
                 * Update the "current" statistics.
                 *
                 *  hitSliceBegin = relative begin-time of the time-slice in which the hit occured
                 */
                int hitSliceBegin = ((hitTime - statsStart) / interval) * interval;
                stats.setValue ("count", incr); // We still have to count the hit that caused all this mess
                stats.setValue ("timeslice", hitSliceBegin);
	
	
                /*-------------------------------------------------------------------
                 * Update old existing slices in the database. We do not create slices of which the
                 * count is 0 anyway. When searching the database we have to consider this.
                 *
                 *  fillSliceBegin = relative begin-times of the time-slices "before" the slice with the hit
                 *  fillSliceNr    = number of the time-slices before the slice with the hit
                 *  max            = maximum number of time-slices that may be reset
                 */
                int fillSliceBegin = hitSliceBegin - interval;
                int fillSliceNr = (fillSliceBegin / interval) % nrOfSlices;
                int max = nrOfSlices;
	
                for ( ; fillSliceBegin > curSliceBegin && max > 0
                          ; fillSliceBegin -= interval, fillSliceNr--, max--
                      ) {
                    if (fillSliceNr < 0) fillSliceNr = nrOfSlices - 1;
	
                    MMObjectNode s = getSlice (number,fillSliceNr);
	
                    if (s != null) { // Node exists in database, so we have to reset it to new values
                        //log.info("Resetting values of slice " + fillSliceNr + " (new begin = " + fillSliceBegin + ")");
                        resetShadow (s, fillSliceBegin, fillSliceBegin + interval - 1);
                        if (!dirty.contains (s)) dirty.addElement (s);
                    } 
                    // else log.info("Skipping slice " + fillSliceNr + ", begin should have been " + fillSliceBegin);
                }
	
                return ("");		
            }
            /* else */
            //log.info("No Stats node");
            return (null);
		} catch(Exception re) {
			re.printStackTrace();
			return("error");
		}
	}

 
	/**
     * insert a new object, normally not used (only subtables are used)
     */
	public void NewStat(String name,String description,int timeslices,int timeinterval, int timeslice, String data, int inc) {
		MMObjectNode node=getNewNode("system");
		node.setValue("name",name);		
		node.setValue("description",description);		
		node.setValue("start",0);		
		node.setValue("count",inc);		
		node.setValue("timeslices",timeslices);		
		node.setValue("timeinterval",timeinterval);		
		node.setValue("timeslice",timeslice);		
		node.setValue("timesync",0);		
		node.setValue("data",data);		
		insert("system",node);
	}


	/**
     * Given the number of a statistics node and a slice-index number, returns the 
     * corresponding shadow-statistics node.
     */
	private MMObjectNode getSlice(String number,int slice) {
		// Retreive a hashtable with the shadow-nodes belonging to the statistics node
		Hashtable h = getShadows (number);
		if (h!=null) {
			// Hashtable exists, so get the slice from the index corresponding with the slice-number
			MMObjectNode node = (MMObjectNode)h.get (new Integer (slice));
			int tmp = -1;
			// If node exists, check its slice-number of the node found with the requested slice-number
			if (node != null) tmp = node.getIntValue ("slicenumber");
	
			//if (tmp == -1) log.debug("Slice " + slice + " is not available");
			//else log.debug("Slice " + slice + " Tmp " + tmp + " (hashed)");
	
			return node;
		} else {
			//log.debug("getSlice didn't get a Hashtable from getShadows");
			return(null);
		}
	}

	/**
	 * Given an existing ShadowStatistics, reset its values
	 */
	private void resetShadow (MMObjectNode n, int start, int stop) {
		n.setValue ("start", start);
		n.setValue ("stop", stop);
		n.setValue ("count", 0);
	}

	public void checkDirty() {
		// Check against check when processing
		if (!processingDirty) {
			Vector proc=null;
			processingDirty=true;
			// Clone the dirty vector
			synchronized(dirty) {
				if (dirty.size()>0) {
					proc=(Vector)dirty.clone();
					dirty.removeAllElements();
				}
			}
			// Process the (copied) dirty elements
			if (proc!=null) {
				MMObjectNode node;
				// log.info("Stats -> Updating "+proc.size()+" nodes");
				while (proc.size()>0) {
					node=(MMObjectNode)proc.elementAt(0);
					node.commit();
					proc.removeElementAt(0);		
					try {Thread.sleep(250);} catch (InterruptedException e){}
				}
			}
			processingDirty=false;
		} else {
			log.info("CheckDirty while Processing");
		}
	}


	public int getSliceNr(String number) {
		MMObjectNode node=getNode(number);
		if (node!=null) {
			int nrOfSlices = node.getIntValue ("timeslices");
			int interval = node.getIntValue ("timeinterval");
			int curSliceBegin = node.getIntValue ("timeslice");
			int curSliceNr = (curSliceBegin / interval) % nrOfSlices;
			return(curSliceNr);
		}
		return(-1);
	}
}
