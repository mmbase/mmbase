package org.mmbase.applications.friendlylink;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.mmbase.cache.oscache.OSCacheImplementation;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Originally copied from LeoCMS. The cache contains two Maps: one maps JSP urls
 * to 'userfriendly' urls and the other the other way around.
 * Added methods to check if a certain cache entry exists.
 * It depends on the MMBase OSCache application.
 *
 * @author Finalist IT Group
 * @author Andr&eacute; vanToly &lt;andre@toly.nl&gt;
 * @version $Id: UrlCache.java,v 1.3 2007-03-04 21:06:07 andre Exp $
 */

public class UrlCache {

    private static final Logger log = Logging.getLoggerInstance(UrlCache.class);

    private final Map cacheJSPToURL;
    private final Map cacheURLToJSP;
  
    public UrlCache() {
        cacheJSPToURL = new OSCacheImplementation();
        cacheURLToJSP = new OSCacheImplementation();
        // set path explicitly, otherwise java.lang.NullPointerException
        // at org.mmbase.cache.oscache.OSCacheImplementation.get(OSCacheImplementation.java:135)
        String tempdir = System.getProperty("java.io.tmpdir");
        if ( !(tempdir.endsWith("/") || tempdir.endsWith("\\")) ) {
            tempdir += tempdir + System.getProperty("file.separator");
        }
        Map config = new HashMap(); 
        config.put("path", tempdir);
        ((OSCacheImplementation)cacheJSPToURL).config(config);
        ((OSCacheImplementation)cacheURLToJSP).config(config);
        
    }
  
    public void flushAll() {
        cacheJSPToURL.clear();
        cacheURLToJSP.clear();
    }
  
    /** 
     * Checks for the technical URL (jsp) in cache. 
     * The processed URL (friendlylink) is the key of the Map cacheURLToJSP,
     * the technical URL is the value.
     *
     * @param jsp  a technical url like 'index.jps?nr=34'
     * @return        <code>true</code> if present,  
     *                <code>false</code> if not.
     */
    public boolean hasJSPEntry(String jsp) {
        return cacheURLToJSP.containsValue(jsp);
    }
    
    public void putJSPEntry(String flink, String jsp) {
        if (!hasJSPEntry(jsp)) {
            cacheURLToJSP.put(flink, jsp);
            if (log.isDebugEnabled()) log.debug("Added '" + flink + "' / '" + jsp + "'");
        }
    }
  
    public String getJSPEntry(String processedURL) {
        return (String)cacheURLToJSP.get(processedURL);
    }
  
    /** 
     * Checks for a friendlylink in cache. The technical URL (jsp) is the key of 
     * the Map cacheJSPToURL, the friendlylink is the value.
     *
     * @param flink a 'userfriendly' link
     * @return        <code>true</code> if present,  
     *                <code>false</code> if not.
     */
    public boolean hasURLEntry(String flink) {
        return cacheJSPToURL.containsValue(flink);
    }
    
    public void putURLEntry(String jsp, String flink) {
        if (!hasURLEntry(flink)) {
             cacheJSPToURL.put(jsp, flink);
             if (log.isDebugEnabled()) log.debug("Added '" + jsp + "' / '" + flink + "'");
        }
    }
    
    public String getURLEntry(String jsp) {
        return (String)cacheJSPToURL.get(jsp);
    }

    /** 
     * Common toString() method to return all cache entries
     *
     * @return  String with all present key/value pairs of both Maps
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<h4>JSP to URL Cache</h4>");
        for (Iterator it=cacheJSPToURL.keySet().iterator();it.hasNext();) {
          String key = (String)it.next();
          String value = (String)cacheJSPToURL.get(key);
          sb.append(key).append(" - ").append(value).append("<br />");
        }
        sb.append("<h4>URL to JSP Cache</h4>");
        for (Iterator it=cacheURLToJSP.keySet().iterator();it.hasNext();) {
          String key = (String)it.next();
          String value = (String)cacheURLToJSP.get(key);
          sb.append(key).append(" - ").append(value).append("<br />");
        }
        return sb.toString();
    }
 
}
