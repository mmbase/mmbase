/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.database;

import java.sql.*;
import java.util.*;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
/**
 * MultiPoolHandler handles multi pools so we can have more than one database
 * open and they can all have a multipool.
 *
 */
 
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

public class MultiPoolHandler {
    private static final Logger log = Logging.getLoggerInstance(MultiPoolHandler.class);
    private int maxConnections;
    private int maxQueries;
    private Map pools = new ConcurrentHashMap();

    private DatabaseSupport databaseSupport;

    public MultiPoolHandler(DatabaseSupport databaseSupport, int maxConnections) {
        this(databaseSupport, maxConnections, 500);
    }

    public MultiPoolHandler(DatabaseSupport databaseSupport, int maxConnections,int maxQueries) {
	this.maxConnections = maxConnections;
	this.maxQueries     = maxQueries;
	this.databaseSupport= databaseSupport;
    }

    public MultiConnection getConnection(String url, String name, String password) throws SQLException {
	MultiPool pool = (MultiPool) pools.get(url + "," + name + "," + password);
	if (pool != null) {
	    return pool.getFree();
	} else {
            log.service("No multipool present, creating one now");
            pool = new MultiPool(databaseSupport, url, name, password, maxConnections, maxQueries);
            if (pools.put(url + "," + name + "," + password, pool) != null) {
                log.error("Replaced an old MultiPool!? " + Logging.stackTrace());
            }
            return pool.getFree();
	}
    }

    /**
     * Calls shutdown of all registered MultiPools
     * @since MMBase-1.6.2
     */
    public void shutdown() {
        for (Iterator i = pools.values().iterator(); i.hasNext();) {
            MultiPool pool = (MultiPool) i.next();
	    pool.shutdown();
	}
    }

    public void checkTime() {
	for (Iterator i = pools.values().iterator(); i.hasNext();) {
	    MultiPool pool = (MultiPool) i.next();
	    pool.checkTime();
	}
    }

    public Set keySet() {
	return pools.keySet();
    }

    /*
    public Enumeration keys() {
	return pools.keys();
    }
    */

    public MultiPool get(String id) {
        return (MultiPool) pools.get(id);
    }

    public void setMaxConnections(int max) {
        maxConnections = max;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxQuerys(int max) {
	maxQueries = max;
    }

    public int getMaxQuerys() {
	return maxQueries;
    }
}
