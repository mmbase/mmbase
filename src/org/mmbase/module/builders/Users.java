/* -*- tab-width: 4; -*-

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import org.mmbase.module.*;
import org.mmbase.module.core.*;
import org.mmbase.module.database.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @author Daniel Ockeloen
 * @version 10 Dec 2000
 */
public class Users extends MMObjectBuilder {

    private static Logger log = Logging.getLoggerInstance(Users.class.getName()); 

	// cache the 100 most active users, enh. is to allow
	// people to set it in users.xml
	LRUHashtable cache = new LRUHashtable(100);

	// rico's funkie password generator
	private PasswordGeneratorInterface pwgen = new PasswordGenerator ();
	
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

			} else if (cmd.equals("email")) {
				// send email field of this user
				return(getEmail(sp.getSessionName()));

			} else if (cmd.equals("password")) {
				// send password field of this user
				return(getPassword(sp.getSessionName()));

			} else if (cmd.equals("account")) {
				// send account field of this user
				return(getAccount(sp.getSessionName()));

			} else if (cmd.equals("newpassword")) {
				// send a _new_ generated password
				return(pwgen.getPassword());
			}
		}
        	return("");
    	}		

	/**
	* get the object number of this user based on the
	* current cookie as defined by the key
	*/
	public int getNumber(String key) {

		// check if we have this key allready in cache
		Integer n=(Integer)cache.get(key);
		if (n!=null) {

			// we have it in the cache so return that
			if (log.isDebugEnabled()) {
                log.debug("user positive cache");
            }
			return(n.intValue());
		}

		// its not in cache so lets check since the current
		// way is only by cookies ask the cookies builder
		// in the future more ways can be added here
		Cookies bul=(Cookies)mmb.getMMObject("cookies");
		if (bul!=null) {

			// ask the cookie builder if he knows this cookie
			int i=bul.getNumber(key);

			if (i!=-1) {
				// lets find a related user, since
				// the logic is that a user has a relation
				// to a cookie object
				MMObjectNode node=getNode(i);
				Enumeration e=node.getRelatedNodes("users").elements();
				if (e.hasMoreElements()) {
					MMObjectNode node2=(MMObjectNode)e.nextElement();
					if (node2!=null) {
						// found a related user so put it in 
						// cache and return it
						int number=node2.getIntValue("number");
						cache.put(key,new Integer(number));
						if (log.isDebugEnabled()) {
                            log.debug("users positive");
                        }
						return(number);
					}
				}
			}
		}
		// no user found send -1 to signal this
		return(-1);
	}


	/**
	* get acount name of active (cookie defined) user
	*/
	private String getAccount(String key) {
		int number=getNumber(key);
		if (number!=-1) {
			MMObjectNode node=getNode(number);
			String value=node.getStringValue("account");
			return(value);
		} 
		return("");	
	}


	/**
	* get email of active (cookie defined) user
	*/
	private String getEmail(String key) {
		int number=getNumber(key);
		if (number!=-1) {
			MMObjectNode node=getNode(number);
			String value=node.getStringValue("email");
			return(value);
		} 
		return("");	
	}


	/**
	* get password of active (cookie defined) user
	*/
	private String getPassword(String key) {
		int number=getNumber(key);
		if (number!=-1) {
			MMObjectNode node=getNode(number);
			String value=node.getStringValue("password");
			return(value);
		} 
		return("");	
	}

	/**
	* flush caches of the (cookie defined) user
	* also signals the session module
	*/
	public void flushCache(String key) {
		// remove from cache
		cache.remove(key);
		// not get module sessions and forget the session
		sessions s=(sessions)mmb.getModule("SESSION");
		if (s!=null) {
			// session module found ask it to forget
			// this sessions
			s.forgetSession(key);
		}
	}	
}
