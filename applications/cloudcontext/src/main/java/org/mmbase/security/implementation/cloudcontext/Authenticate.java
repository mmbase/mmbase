/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import java.io.IOException;
import java.io.InputStream;

import java.util.*;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.VirtualNode;
import org.mmbase.security.*;
import org.mmbase.security.SecurityException;
import org.mmbase.security.classsecurity.ClassAuthentication;
import org.mmbase.util.ResourceWatcher;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

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
 * @version $Id$
 */
public class Authenticate extends CloudContextAuthentication {
    private static final Logger log = Logging.getLoggerInstance(Authenticate.class);


    protected static final String ADMINS_PROPS = "admins.properties";

    private static int extraAdminsUniqueNumber;

    private boolean allowEncodedPassword = false;

    private   static Properties extraAdmins = new Properties();      // Admins to store outside database.
    protected static Map<String, User>      loggedInExtraAdmins = new HashMap<String, User>();


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


    /**
     * {@inheritDoc}
     */
    @Override
    protected void load() throws SecurityException {
        attributes.put(STORES_CONTEXT_IN_OWNER, Boolean.TRUE);
        UserProvider users = getUserProvider();
        if (users == null) {
            throw new SecurityException("builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported)");
        }

        ResourceWatcher adminsWatcher = new ResourceWatcher(MMBaseCopConfig.securityLoader) {
                public void onChange(String res) {
                    InputStream in = getResourceLoader().getResourceAsStream(res);
                    readAdmins(in);
                }
            };
        adminsWatcher.add(ADMINS_PROPS);
        adminsWatcher.onChange(ADMINS_PROPS);
        adminsWatcher.start();

    }

    /**
     * @since MMBase-1.9
     */
    @Override
    public int getNode(UserContext user) throws SecurityException {
        return ((User) user).getNode().getNumber();
    }


    private boolean warnedNoAnonymousUser = false;

    /**
     * {@inheritDoc}
     *
     <table>
       <caption>Password comparison strategies</caption>
       <tr><th>client provided</th><th>database</th><th>application to use</th><th>comments</th><tr>
       <tr><td>plain</td><th>encoded</td><td>name/password</td><td>this is the default and most
       sensible case</td></tr>
       <tr><td>encoded</td><th>encoded</td><td>name/encodedpassword</td><td>The 'allowencodedpassword' property must be true for this to work.</td></tr>

       <tr><td>plain</td><th>plain</td><td>name/password</td><td>this is the default if the password
       set-processor is empty.</td></tr>

       <tr><td>encoded</td><th>plain</td><td>name/encodedpassword</td><td></td></tr>
     </table>
     */
    @Override
    public UserContext login(String type, Map<String, ?> map, Object[] parameters) throws SecurityException {
        if (log.isTraceEnabled()) {
            log.trace("login-module: '" + type + "'");
        }
        MMObjectNode node = null;
        UserProvider users = getUserProvider();
        if (users == null) {
            throw new SecurityException("builders for security not installed, if you are trying to install the application belonging to this security, please restart the application after all data has been imported");
        }
        allowEncodedPassword = org.mmbase.util.Casting.toBoolean(users.getUserBuilder().getInitParameter("allowencodedpassword"));
        if ("anonymous".equals(type)) {
            node = users.getAnonymousUser();
            if (node == null) {
                if (! warnedNoAnonymousUser) {
                    log.warn("No user node for anonymous found");
                    warnedNoAnonymousUser = true;
                }
                return new LocalAdmin("anonymous", type, Rank.getRank("anonymous"));
            }
        } else if ("name/password".equals(type)) {
            String userName = (String) map.get("username");
            String password = (String) map.get("password");
            if(userName == null || password == null) {
                throw new SecurityException("Expected the property 'username' and 'password' with login. But received " + map);
            }
            if (extraAdmins.containsKey(userName)) {
                if(extraAdmins.get(userName).equals(password)) {
                    log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                    User user = new LocalAdmin(users.getUser(userName), userName, type);
                    loggedInExtraAdmins.put(userName, user);
                    return user;
                }
            }
            node = users.getUser(userName, password, false);
            if (node != null && ! users.isValid(node)) {
                throw new SecurityException("Logged in an invalid user");
            }
        } else if (allowEncodedPassword && "name/encodedpassword".equals(type)) {
            String userName = (String)map.get("username");
            String password = (String)map.get("encodedpassword");
            if(userName == null || password == null) {
                throw new SecurityException("Expected the property 'username' and 'password' with login. But received " + map);
            }
            if (extraAdmins.containsKey(userName)) {
                if(users.encode((String) extraAdmins.get(userName)).equals(password)) {
                    log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                    User user = new LocalAdmin(users.getUser(userName), userName, type);
                    loggedInExtraAdmins.put(userName, user);
                    return user;
                }
            }
            node = users.getUser(userName, password, true);
            if (node != null && ! users.isValid(node)) {
                throw new SecurityException("Logged in an invalid user");
            }
        } else if ("class".equals(type)) {
            ClassAuthentication.Login li = ClassAuthentication.classCheck("class", map);
            if (li == null) {
                throw new SecurityException("Class authentication failed  '" + type + "' (class not authorized)");
            }
            String userName = li.getMap().get(PARAMETER_USERNAME.getName());
            String rank     = li.getMap().get(PARAMETER_RANK.getName());
            if (userName != null && (rank == null || (Rank.ADMIN.toString().equals(rank) && extraAdmins.containsKey(userName)))) {
                log.service("Logged in an 'extra' admin '" + userName + "'. (from admins.properties)");
                User user = new LocalAdmin(users.getUser(userName), userName, type);
                loggedInExtraAdmins.put(userName, user);
                return user;
            } else {
                if (userName != null) {
                    try {
                        node = users.getUser(userName);
                    } catch (SecurityException se) {
                        log.service(se);
                        return new LocalAdmin(userName, type, rank == null ? Rank.ADMIN : Rank.getRank(rank));
                    }
                } else if (rank != null) {
                    node = users.getUserByRank(rank, userName);
                    log.debug("Class authentication to rank " + rank + " found node " + node);
                    if (node == null) {
                        return new LocalAdmin(rank, type, Rank.getRank(rank));
                    }
                }
            }
        } else {
            throw new UnknownAuthenticationMethodException("login method with name '" + type + "' not found, only 'anonymous', 'name/password', 'name/encodedpassword' " + (allowEncodedPassword ? " (DISALLOWED) " : "")
                                                           + "  and 'class' are supported");
        }
        if (node == null)  return null;
        return new User(node, getKey(), type);
    }

    public static User getLoggedInExtraAdmin(String userName) {
        return loggedInExtraAdmins.get(userName);
    }

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


    @Override
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

    @Override
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
        private Rank   r = Rank.ADMIN;
        LocalAdmin(MMObjectNode userNode, String user, String app) {
            super(userNode == null ? new AdminVirtualNode() : userNode, Authenticate.this.getKey(), app);
            l = extraAdminsUniqueNumber;
            userName = user;
        }
        LocalAdmin(String user, String app) {
            this(null, user, app);
        }
        LocalAdmin(String user, String app, Rank r) {
            this(user, app);
            this.r = r;
        }
        public String getIdentifier() { return userName; }
        public String  getOwnerField() { return userName; }
        public Rank getRank() throws SecurityException { return r; }
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
                    (userName == null ? ou.userName == null : userName.equals(ou.userName)) &&
                    l == ou.l;
            } else {
                return false;
            }
        }
    }
    public  class AdminVirtualNode extends VirtualNode {
        AdminVirtualNode() {
            super(Authenticate.this.getUserProvider().getUserBuilder());
        }
    }

}
