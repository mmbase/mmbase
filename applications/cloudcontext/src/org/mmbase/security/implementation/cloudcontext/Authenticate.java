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
import org.mmbase.security.classsecurity.ClassAuthentication;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.functions.*;
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
 * @version $Id: Authenticate.java,v 1.19 2007-06-20 14:37:15 michiel Exp $
 */
public class Authenticate extends Authentication {
    private static final Logger log = Logging.getLoggerInstance(Authenticate.class);

    protected static final String ADMINS_PROPS = "admins.properties";

    private int extraAdminsUniqueNumber;

    private boolean allowEncodedPassword = false;

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
            extraAdminsUniqueNumber = extraAdmins.hashCode();
        } catch (IOException ioe) {
            log.error(ioe);
        }
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

    /**
     * {@inheritDoc}
     * @since MMBase-1.9
     */
    public org.mmbase.bridge.Node getNode(org.mmbase.bridge.Cloud cloud) throws SecurityException {
        return cloud.getNode(cloud.getUser().getIdentifier());
    }

    public String getUserBuilder() {
        return "mmbaseusers";
    }


    // javadoc inherited
    public UserContext login(String s, Map map, Object aobj[]) throws SecurityException  {
        if (log.isTraceEnabled()) {
            log.trace("login-module: '" + s + "'");
        }
        MMObjectNode node = null;
        Users users = Users.getBuilder();
        if (users == null) {
            String msg = "builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported)";
            log.fatal(msg);
            throw new SecurityException(msg);
        }
        allowEncodedPassword = org.mmbase.util.Casting.toBoolean(users.getInitParameter("allowencodedpassword"));
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
                    User user = new LocalAdmin(userName, s);
                    loggedInExtraAdmins.put(userName, user);
                    return user;
                }
            }
            node = users.getUser(userName, password);
            if (node != null && ! users.isValid(node)) {
                throw new SecurityException("Logged in an invalid user");
            }
        } else if (allowEncodedPassword && "name/encodedpassword".equals(s)) {
            String userName = (String)map.get("username");
            String password = (String)map.get("encodedpassword");
            if(userName == null || password == null) {
                throw new SecurityException("Expected the property 'username' and 'password' with login. But received " + map);
            }
            if (extraAdmins.containsKey(userName)) {
                if(users.encode((String) extraAdmins.get(userName)).equals(password)) {
                    log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                    User user = new LocalAdmin(userName, s);
                    loggedInExtraAdmins.put(userName, user);
                    return user;
                }
            }
            node = users.getUser(userName, password, false);
            if (node != null && ! users.isValid(node)) {
                throw new SecurityException("Logged in an invalid user");
            }
        } else if ("class".equals(s)) {
            ClassAuthentication.Login li = ClassAuthentication.classCheck("class");
            if (li == null) {
                throw new SecurityException("Class authentication failed  '" + s + "' (class not authorized)");
            }
            String userName = li.getMap().get(PARAMETER_USERNAME.getName());
            String rank     = li.getMap().get(PARAMETER_RANK.getName());
            if (userName != null && (rank == null || (Rank.ADMIN.toString().equals(rank) && extraAdmins.containsKey(userName)))) {
                log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                User user = new LocalAdmin(userName, s);
                loggedInExtraAdmins.put(userName, user);
                return user;
            } else {
                if (userName != null) {
                    node = users.getUser(userName);
                    if (rank != null) {
                    }
                } else if (rank != null) {
                    node = users.getUserByRank(rank, userName);
                    log.debug("Class authentication to rank " + rank + " found node " + node);
                }
            }
        } else {
            throw new UnknownAuthenticationMethodException("login module with name '" + s + "' not found, only 'anonymous', 'name/password' and 'class' are supported");
        }
        if (node == null)  return null;
        return new User(node, getKey(), s);
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
        if (user.node == null) {
            log.debug("No node associated to user object, --> user object is invalid");
            return false;
        } 
        if (! user.isValidNode()) {
            log.debug("Node associated to user object, is invalid");
            return false;
        }
        if ( user.getKey() != getKey()) {
            log.service(user.toString() + "(" + user.getClass().getName() + ") was NOT valid (different unique number)");
            return false;
        }
        log.debug(user.toString() + " was valid");
        return true;
    }


    public String[] getTypes(int method) {
        if (allowEncodedPassword) {
            if (method == METHOD_ASIS) {
                return new String[] {"anonymous", "name/password", "name/encodedpassword", "class"};
            } else {
                return new String[] {"name/password", "name/encodedpassword", "class"};
            }
        } else {
            if (method == METHOD_ASIS) {
                return new String[] {"anonymous", "name/password", "class"};
            } else {
                return new String[] {"name/password", "class"};
            }
        }

    }

    private static final Parameter PARAMETER_ENCODEDPASSWORD = new Parameter("encodedpassword", String.class, true);
    private static final Parameter[] PARAMETERS_NAME_ENCODEDPASSWORD =
        new Parameter[] {
            PARAMETER_USERNAME,
            PARAMETER_ENCODEDPASSWORD,
            new Parameter.Wrapper(PARAMETERS_USERS) };

    public Parameters createParameters(String application) {
        application = application.toLowerCase();
        if ("anonymous".equals(application)) {
            return new Parameters(PARAMETERS_ANONYMOUS);
        } else if ("class".equals(application)) {
            return Parameters.VOID;
        } else if ("name/password".equals(application)) {
            return new Parameters(PARAMETERS_NAME_PASSWORD);
        } else if ("name/encodedpassword".equals(application)) {
            return new Parameters(PARAMETERS_NAME_ENCODEDPASSWORD);
        } else {
            return new AutodefiningParameters();
        }
    }

    protected class LocalAdmin extends User {
        private static final long serialVersionUID = 1;

        private String userName;
        private long   l;
        LocalAdmin(String user, String app) {
            super(new AdminVirtualNode(), Authenticate.this.getKey(), app);
            l = extraAdminsUniqueNumber;
            userName = user;
        }
        public String getIdentifier() { return userName; }
        public String  getOwnerField() { return userName; }
        public Rank getRank() throws SecurityException { return Rank.ADMIN; }
        public boolean isValidNode() { return l == extraAdminsUniqueNumber; }
        private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
            userName = in.readUTF();
            l        = extraAdminsUniqueNumber;
            org.mmbase.util.ThreadPools.jobsExecutor.execute(new Runnable() {
                    public void run() {
                        org.mmbase.bridge.LocalContext.getCloudContext().assertUp();
                        node     = new AdminVirtualNode();
                    }
                });

        }

        private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
            out.writeUTF(userName);
        }

        public boolean equals(Object o) {
            if (o instanceof LocalAdmin) {
                LocalAdmin ou = (LocalAdmin) o;
                return
                    super.equals(o) &&
                    userName.equals(ou.userName) &&
                    l == ou.l;
            } else {
                return false;
            }
        }
    }
    public  class AdminVirtualNode extends VirtualNode {
        AdminVirtualNode() {
            super(Users.getBuilder());
        }
    }

}
