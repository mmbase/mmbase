package org.mmbase.security.implementation.context;

import org.mmbase.security.Rank;
import org.mmbase.security.UserContext;
import org.mmbase.security.SecurityException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *  This UserContext class provides a storage for the authentication
 *  and authorization, so that information can be shared.
 *  This class is NOT a container class for client related stuff, altrough
 *  this is possible.
 */
public class ContextUserContext extends UserContext {
    private static Logger log = Logging.getLoggerInstance(ContextUserContext.class.getName());
    private String  username;
    private Rank    rank;
    private long    key;

    public ContextUserContext(String username, Rank rank, long key) {
    	this.rank = rank;
	this.username = username;
	this.key = key;
    }

    public String getIdentifier() {
    	return username;
    }

    public Rank getRank() {
    	return rank;
    }
    
    long getKey() {
    	return key;
    }
    
}
