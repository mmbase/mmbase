/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.mmbase.security.Operation;
import org.mmbase.security.SecurityException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * ContextCache class
 * @javadoc
 *
 * @author Eduard Witteveen
 * @version $Id: ContextCache.java,v 1.6 2002-07-05 14:18:41 michiel Exp $
 */
public class ContextCache  {
    private static Logger log = Logging.getLoggerInstance(ContextCache.class.getName());

    private org.mmbase.cache.Cache globalRightCache = new org.mmbase.cache.Cache(50) {
        public String getName()        { return "ContextRight"; }
        public String getDescription() { return "Context Security Implementation Rights Cache"; }
        };

    private long    rightTries = 0;
    private long    rightSucces = 0;
    private long    rightSize = 0;

    public void rightAdd(String operation, String context, String user, boolean value) {
        Map operationCache = (Map) globalRightCache.get(operation);
    // when operation not known, create
    if(operationCache == null) {
        operationCache = new HashMap();
        globalRightCache.put(operation, operationCache);
    }
    Map contextCache = (Map) operationCache.get(context);
    // when context not known, create
    if(contextCache == null) {
        contextCache = new HashMap();
        operationCache.put(context, contextCache);
    }
        if(contextCache.containsKey(user)) {
        log.warn("rights context cache already contained this entry");
    }
        contextCache.put(user, new Boolean(value));
        log.debug("added to cache the operation: " + operation + " for context: " + context + " with user: " + user + " with value: " + value );
        rightSize++;
    }

    public Boolean rightGet(String operation, String context, String user) {
        HashMap operationCache = (HashMap)globalRightCache.get(operation);
        rightTries ++;
    if(operationCache == null) {
        log.debug("operation not found in cache ("+info(rightTries, rightSucces, rightSize)+")");
        return null;
    }

        HashMap contextCache = (HashMap)operationCache.get(context);

    if(contextCache == null) {
        log.debug("rights context not found in cache ("+info(rightTries, rightSucces, rightSize)+")");
        return null;
    }

        if(contextCache.containsKey(user)) {
        rightSucces ++;
        log.debug("user found in cache ("+info(rightTries, rightSucces, rightSize)+")");
        log.debug("the operation: " + operation + " for context: " + context + " with user: " + user + " returned: " + contextCache.get(user) );
        }
    return (Boolean)contextCache.get(user);
    }

    private org.mmbase.cache.Cache globalContextCache = new org.mmbase.cache.Cache(50) {
        public String getName()        { return "ContextContext"; }
        public String getDescription() { return "Context Security Implementation Context Cache"; }
        };

    private long    contextTries = 0;
    private long    contextSucces = 0;
    private long    contextSize = 0;

    public void contextAdd(String context, Set possible) {
    // when context was already known....
        if(globalContextCache.containsKey(context)) {
        log.warn("context cache already contained this entry");
    }
        globalContextCache.put(context, possible);
        log.debug("added possible list to context with name : " + context);
        contextSize++;
    }

    public Set contextGet(String context) {
        contextTries++;

        if(globalContextCache.containsKey(context)) {
        contextSucces++;
        log.debug("context found in cache ("+info(contextTries, contextSucces, contextSize)+")");
        }
    return (Set)globalContextCache.get(context);
    }

    private String info(long tries, long succes, long size) {
        return "hit of #"+succes+ " access of #"+tries+" ("+ succes/(tries/100.0)+" %) with a number of entries #"+size;
    }

    ContextCache() {
        globalContextCache.putCache();
        globalRightCache.putCache();
    }
}
