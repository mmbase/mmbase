/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import java.util.Map;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This class is used when no authentication is configured.
 * @javadoc
 * @author Eduard Witteveen
 * @version $Id: NoAuthentication.java,v 1.6 2002-06-07 12:56:55 pierre Exp $
 */
public class NoAuthentication extends Authentication {
    private static Logger log=Logging.getLoggerInstance(NoAuthentication.class.getName());

    /**
     *	This method does nothing
     */
    protected void load() {
    }


    /**
     * this method does nothing..
     */
    public UserContext login(String application, Map loginInfo, Object[] parameters) throws org.mmbase.security.SecurityException {
        return new UserContext();
    }

    /**
     * this method does nothing..
     */
    public boolean isValid(UserContext usercontext) throws org.mmbase.security.SecurityException {
        return true;
    }
}
