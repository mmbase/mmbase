/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * @author Daniel Ockeloen
 * @version $Id$
 */
public class Cookies extends MMObjectBuilder {

    private static Logger log = Logging.getLoggerInstance(Cookies.class.getName());

    // remember the 250 most used cookies
    LRUHashtable<String, Integer> cache = new LRUHashtable<String, Integer>(250);

    // also remember them the other way around (should be changed)
    LRUHashtable<Integer, String> cache2 = new LRUHashtable<Integer, String>(250);

        /**
        * replace call, when called in format MMBASE-BUILDER-users-xxxxx
        */
    public String replace(scanpage sp, StringTokenizer tok) {
        if (tok.hasMoreTokens()) {
            String cmd=tok.nextToken();
            if (cmd.equals("number")) {
                int i=getNumber(sp.getSessionName());
                if (i!=-1) {
                    return(""+i);
                } else {
                    return("");
                }
            }
        }
            return("");
        }

        /**
        * get the object number of this browser based on the
        * current cookie as defined by the key
        */
    public int getNumber(String key) {

                // check if we have this key allready in cache
        Integer i=cache.get(key);
        if (i!=null) {
                        // we have it in the cache so return that
            if (i.intValue()!=-1) {
                log.debug("cookie positive cache "+key);
                return(i.intValue());
            } else {
                // this branch is only needed for the debug
                log.debug("cookie negative cache "+key);
                return(-1);
            }
        }

        // its not in cache so lets check the database
        Enumeration e=search("WHERE "+mmb.getStorageManagerFactory().getStorageIdentifier("cookiekey")+"='"+key+"'");
        if (e.hasMoreElements()) {
            // found this cookie in the cloud so
            // put it in cache and return it
            MMObjectNode node=(MMObjectNode)e.nextElement();
            int number=node.getIntValue("number");

            // put in cache for positive caching
            cache.put(key,new Integer(number));
            cache2.put(new Integer(number),key);

            log.debug("cookie positive "+key);
            return(number);
        } else {
            // not in the cloud but put it cache
            // to make sure it doesn't keep asking
            // the database and return -1
            log.debug("cookie negative "+key);
            cache.put(key,new Integer(-1));
            cache2.put(new Integer(-1),key);
            return(-1);
        }
    }


    /**
    * cookie nodes has changed, update the different caches
    * in cookies,users,people and even sessions
    */
    public boolean nodeChanged(String machine,String number,String builder,String ctype) {
        // System.out.println("MACHINE="+machine+" NUMBER="+number+" BUILDER="+builder+" TYPE="+ctype);

        // its a new cookie lets put it in the cache
        if (ctype.equals("n")) {
            MMObjectNode node=getNode(number);
            if (node!=null) {
                String key=node.getStringValue("cookiekey");

                // allways put in cache probably overwriting -1
                cache.put(key,new Integer(node.getIntValue("number")));
                cache2.put(new Integer(node.getIntValue("number")),key);
            }
        } else if (ctype.equals("r")) {
            // it has a changed relation (probably linked to a
            // new/different user so clear needed caches
            MMObjectNode node=getNode(number);
            if (node!=null) {
                // node found so lets clear its caches
                flushCache(node.getIntegerValue("number"));
            }
        } else if (ctype.equals("d")) {
                    // The node is deleted so remove it from the cache.
                    flushCache(new Integer(number));
                }

        return(true);
    }

    /**
    * local change on a cookie object detected
    */
    @Override
    public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
        super.nodeLocalChanged(machine,number,builder,ctype);
        return(nodeChanged(machine,number,builder,ctype));
    }

    /**
    * remote  change on a cookie object detected
    */
    @Override
    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
        super.nodeRemoteChanged(machine,number,builder,ctype);
        return(nodeChanged(machine,number,builder,ctype));
    }


    /**
    * flush the cache for this cookie, also signal
    * related builder of the change
    */
    public void flushCache(Integer number) {

        // well first signal the users builder
        Users users=(Users)mmb.getMMObject("users");
        if (users!=null) {
            String key=cache2.get(number);
            users.flushCache(key);
        }

        // now signal the people builder
        People people=(People)mmb.getMMObject("people");
        if (people!=null) {
            String key=cache2.get(number);
            people.flushCache(key);
        }
    }

}
