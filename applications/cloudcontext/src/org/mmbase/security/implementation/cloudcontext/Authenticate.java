/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import java.util.*;
import java.io.*;
import org.mmbase.security.implementation.cloudcontext.builders.*;
import org.mmbase.security.*;
import org.mmbase.module.core.*;
import org.mmbase.security.SecurityException;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.ResourceWatcher;

/**
 * Cloud-based Authentication. Deploy the application to explore the object-model on which this is based.
 *
 * Besides the cloud also a '<security-config-dir>/admins.properties' file is considered, which can
 * be used by site-admins to give themselves rights if somehow they lost it, without turning of
 * security altogether.
 *
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: Authenticate.java,v 1.10 2005-03-16 15:46:13 michiel Exp $
 */
public class Authenticate extends Authentication {
    private static final Logger log = Logging.getLoggerInstance(Authenticate.class);

    protected static final String ADMINS_PROPS = "admins.properties";

    private long uniqueNumber;
    private long extraAdminsUniqueNumber;

    private static Properties extraAdmins = new Properties();      // Admins to store outside database.
    protected static Map      loggedInExtraAdmins = new HashMap();


    protected void readAdmins(InputStream in) {
        try {
            extraAdmins.clear();
            loggedInExtraAdmins.clear();
            if (in != null) {
                extraAdmins.load(in);
            }
            log.service("Extra admins " + extraAdmins.keySet());
            extraAdminsUniqueNumber = System.currentTimeMillis();
        } catch (IOException ioe) {
            log.error(ioe);
        }
    }


    /**
     * Constructor. Only initializes an 'unique number' for this security instance, which can be used in
     * 'isValid'.
     */
    public Authenticate() {
        uniqueNumber = System.currentTimeMillis();
    }

    // javadoc inherited
    protected void load() throws SecurityException {
        Users users = Users.getBuilder();
        if (users == null) {
            String msg = "builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported)";
            log.fatal(msg);
           throw new SecurityException(msg);
        }
        if (!users.check()) {
           String msg = "builder mmbaseusers was not configured correctly";
            log.error(msg);
            throw new SecurityException(msg);
        }
        
        ResourceWatcher adminsWatcher = new ResourceWatcher(MMBaseCopConfig.securityLoader) {
                public void onChange(String res) {
                    InputStream in = getResourceLoader().getResourceAsStream(res);
                    readAdmins(in);
                }
            };
        adminsWatcher.add(ADMINS_PROPS);
        adminsWatcher.onChange(ADMINS_PROPS);
        adminsWatcher.setDelay(10*1000);
        adminsWatcher.start();

    }

    // javadoc inherited
    public UserContext login(String s, Map map, Object aobj[]) throws SecurityException  {
        if (log.isDebugEnabled()) {
            log.trace("login-module: '" + s + "'");
        }
        MMObjectNode node = null;
        Users users = Users.getBuilder();
        if (users == null) {
            String msg = "builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported)";
            log.fatal(msg);
            throw new SecurityException(msg);
        }
        if ("anonymous".equals(s)) {
            node = users.getAnonymousUser();
        } else if ("name/password".equals(s)) {
            String userName = (String)map.get("username");
            String password = (String)map.get("password");
            if(userName == null || password == null) {
                throw new SecurityException("Expected the property 'username' and 'password' with login. But received " + map);
            }
            if (extraAdmins.containsKey(userName)) {
                if(extraAdmins.get(userName).equals(password)) {
                    log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                    User user = new LocalAdmin(userName);
                    loggedInExtraAdmins.put(userName, user);
                    return user;
                }
            }
            node = users.getUser(userName, password);
            if (node != null && ! users.isValid(node)) {
                throw new SecurityException("Logged in an invalid user");
            }
        } else if ("class".equals(s)) {
            org.mmbase.security.classsecurity.ClassAuthentication.Login li = org.mmbase.security.classsecurity.ClassAuthentication.classCheck("class");
            if (li == null) {
                throw new SecurityException("Class authentication failed  '" + s + "' (class not authorized)");
            }
            String userName = (String) li.getMap().get("username");
            if (extraAdmins.containsKey(userName)) {
                log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                User user = new LocalAdmin(userName);
                loggedInExtraAdmins.put(userName, user);
                return user;
            } else {
                node = users.getUser((String) li.getMap().get("username"));
            }
        } else {
            throw new UnknownAuthenticationMethodException("login module with name '" + s + "' not found, only 'anonymous', 'name/password' and 'class' are supported");
        }
        if (node == null)  return null;
        return new User(node, uniqueNumber, s);
    }

    public static User getLoggedInExtraAdmin(String userName) {
        return (User) loggedInExtraAdmins.get(userName);
    }

    // javadoc inherited
    public boolean isValid(UserContext userContext) throws SecurityException {
        if (! (userContext instanceof User)) {
            log.debug("Changed to other security implementation");
            return false;
        }
        User user = (User) userContext;
        boolean flag = user.isValidNode() && user.getKey() == uniqueNumber;
        if (flag) {
            log.debug(user.toString() + " was valid");
        } else if (user.isValidNode()) {
            log.debug(user.toString() + "(" + user.getClass().getName() + ") was NOT valid (different unique number)");
        } else {
            log.debug(user.toString() + "(" + user.getClass().getName() + ") was NOT valid (node was different)");
        }
        return flag;
    }

    protected class LocalAdmin extends User {
        private String userName;
        private long   l;
        LocalAdmin(String user) {
            super(null, uniqueNumber, "name/password");
            node = new AdminVirtualNode();
            l = extraAdminsUniqueNumber;
            userName = user;
        }
        public String getIdentifier() { return userName; }
        public String  getOwnerField() { return userName; }
        public Rank getRank() throws SecurityException { return Rank.ADMIN; }
        public boolean isValidNode() { return l == extraAdminsUniqueNumber; }
    }
    public  class AdminVirtualNode extends VirtualNode {
        AdminVirtualNode() {
            super(Users.getBuilder());
        }
    }

}
