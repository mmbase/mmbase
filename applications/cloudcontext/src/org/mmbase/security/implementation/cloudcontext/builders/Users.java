/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext.builders;

import org.mmbase.security.implementation.cloudcontext.*;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.security.*;
import org.mmbase.security.SecurityException;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.cache.Cache;
import org.mmbase.util.Encode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.functions.*;

/**
 * This MMObjectBuilder implementation belongs to the object type
 * 'mmbaseusers' It contains functionality to MD5 encode passwords,
 * and so on.
 *
 *
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: Users.java,v 1.30 2004-08-10 10:41:09 keesj Exp $
 * @since  MMBase-1.7
 */
public class Users extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(Users.class);


    public final static String FIELD_STATUS      = "status";
    public final static String FIELD_USERNAME    = "username";
    public final static String FIELD_PASSWORD    = "password";
    public final static String FIELD_DEFAULTCONTEXT    = "defaultcontext";
    public final static String FIELD_VALID_FROM    = "validfrom";
    public final static String FIELD_VALID_TO      = "validto";
    public final static String FIELD_LAST_LOGON    = "lastlogon";

    public final static long VALID_TO_DEFAULT      = 4102441200L; // 1-1-2100

    public final static String STATUS_RESOURCE = "org.mmbase.security.status";

    public final static Parameter[] RANK_PARAMETERS = {
    };


    protected static Cache rankCache = new Cache(20) {
            public String getName()        { return "CCS:SecurityRank"; }
            public String getDescription() { return "Caches the rank of users"; }
        };

    protected static Cache userCache = new Cache(20) {
            public String getName()        { return "CCS:SecurityUser"; }
            public String getDescription() { return "Caches the users"; }
        };

    // javadoc inherited
    public boolean init() {
        rankCache.putCache();
		userCache.putCache();
        CacheInvalidator.getInstance().addCache(rankCache);
        CacheInvalidator.getInstance().addCache(userCache);
        mmb.addLocalObserver(getTableName(), CacheInvalidator.getInstance());
        mmb.addRemoteObserver(getTableName(), CacheInvalidator.getInstance());

        // MM: I think this is should not be configured.
        String s = (String)getInitParameters().get("encoding");
        if (s == null) {
            log.debug("no property 'encoding' defined in '" + getTableName() + ".xml' using default encoding");
            encoder = new Encode("MD5");
        } else {
            encoder = new Encode(s);
        }
        log.info("Using " + encoder.getEncoding() + " as our encoding for password");

        return super.init();
    }



    /**
     * The user with rank administrator
     */
    static final String ADMIN_USERNAME = "admin";
    /**
     * The user with rank anonymous
     */
    static final String ANONYMOUS_USERNAME = "anonymous";

    private Encode encoder = null;

    /**
     * @javadoc
     */
    public static Users getBuilder() {
        return (Users) MMBase.getMMBase().getBuilder("mmbaseusers");
    }


    public Rank getRank(MMObjectNode userNode) {
        Rank rank;
        if (userNode != null) {
            rank = (Rank) rankCache.get(userNode);
        } else {
            log.warn("No node given, returning Anonymous.");
            return Rank.ANONYMOUS;
        }

        if (rank == null) {
            if (userNode instanceof Authenticate.AdminVirtualNode) {
                rank = Rank.ADMIN;
            } else {
                List ranks =  userNode.getRelatedNodes("mmbaseranks", RelationStep.DIRECTIONS_DESTINATION);
                if (ranks.size() > 1) {
                    log.warn("More then one rank related to mmbase-user " + userNode.getNumber() + " (but " + ranks.size() + ")");
                }
                rank = Rank.ANONYMOUS;
                if (ranks.size() == 0) {
                    log.debug("No ranks related to this user");
                } else {
                    Iterator i = ranks.iterator();
                    while (i.hasNext()) {
                        Ranks rankBuilder = Ranks.getBuilder();
                        Rank r = rankBuilder.getRank((MMObjectNode) i.next());
                        if (r.compareTo(rank) > 0) rank = r; // choose the highest  one
                    }
                }
            }
            rankCache.put(userNode, rank);
        }
        return rank;
    }

    /**
     * Notify the cache that the rank of user node changed
     * this is fixed by CacheInvalidator alreayd ?
     public void rankChanged(MMObjectNode node) {
        rankCache.remove(node);
    }
    */


    //javadoc inherited
    public boolean setValue(MMObjectNode node, String field, Object originalValue) {
        if (field.equals(FIELD_USERNAME)) {
            Object value = node.values.get(field);
            if (node.getIntValue(FIELD_STATUS) >= 0) {
                if (originalValue != null && ! originalValue.equals(value)) {
                    /*
                    node.values.put(field, value);
                    log.warn("Cannot change username (unless account is blocked)");
                    return false; // hmm?
                    */
                    log.info("Changing account '" + originalValue + "' to '" + value + "'");
                }
            }
        }
        return true;
    }


    /**
     * @javadoc
     */
    public MMObjectNode getAnonymousUser() throws SecurityException {
        return getUser("anonymous", "");
    }

    /**
     * Gets the usernode and check its credential (password only, currently)
     *
     * @return the authenticated user, or null
     * @throws SecurityException
     */
    public MMObjectNode getUser(String userName, String password)  {
        if (log.isDebugEnabled()) {
            log.debug("username: '" + userName + "' password: '" + password + "'");
        }
        MMObjectNode user = getUser(userName);

        if (userName.equals("anonymous")) {
            log.debug("an anonymous username");
            if (user == null) {
                throw new SecurityException("no node for anonymous user"); // odd.
            }
            return user;
        }

        if (user == null) {
            log.debug("username: '" + userName + "' --> USERNAME NOT CORRECT");
            return null;
        }

        if (encode(password).equals(user.getStringValue(FIELD_PASSWORD))) {
            if (log.isDebugEnabled()) {
                log.debug("username: '" + userName + "' password: '" + password + "' found in node #" + user.getNumber());
            }
            Rank userRank = getRank(user);
            if (userRank == null) {
                userRank = Rank.ANONYMOUS;
                log.warn("rank for '" + userName + "' is unknown or not registered, using anonymous.");
            }
            if (userRank.getInt() < Rank.ADMIN.getInt() && getField(FIELD_STATUS) != null) {
                int status = user.getIntValue(FIELD_STATUS);
                if (status == -1) {
                    throw new SecurityException("account for '" + userName + "' is blocked");
                }
            }
            if (userRank.getInt()<Rank.ADMIN_INT && getField(FIELD_VALID_FROM) != null) {
                long validFrom = user.getLongValue(FIELD_VALID_FROM);
                if (validFrom != -1 && validFrom * 1000 > System.currentTimeMillis() ) {
                    throw new SecurityException("account for '" + userName + "' not yet active");
                }
            }
            if (userRank.getInt()<Rank.ADMIN_INT && getField(FIELD_VALID_TO) != null) {
                long validTo = user.getLongValue(FIELD_VALID_TO);
                if (validTo != -1 && validTo * 1000 < System.currentTimeMillis() ) {
                    throw new SecurityException("account for '" + userName + "' is expired");
                }
            }
            if (getField(FIELD_LAST_LOGON) != null) {
                user.setValue(FIELD_LAST_LOGON,System.currentTimeMillis() / 1000);
                user.commit();
            }
            return user;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("username: '" + userName + "' found in node #" + user.getNumber() + " --> PASSWORDS NOT EQUAL");
            }
            return null;
        }

    }
    /**
     * Gets the usernode by userName (the 'identifier'). Or 'null' if not found.
     */
    public MMObjectNode getUser(String userName)  {
        MMObjectNode user = (MMObjectNode) userCache.get(userName);
        if (user == null) {
            NodeSearchQuery nsq = new NodeSearchQuery(this);
            StepField sf        = nsq.getField(getField("username"));
            Constraint cons = new BasicFieldValueConstraint(sf, userName);
            nsq.setConstraint(cons);
            nsq.addSortOrder(nsq.getField(getField("number")));
            SearchQueryException e = null;
            try {
                Iterator i = getNodes(nsq).iterator();
                if(i.hasNext()) {
                    user = (MMObjectNode) i.next();
                }

                if(i.hasNext()) {
                    log.warn("Found more users with username '" + userName + "'");
                }
            } catch (SearchQueryException sqe) {
                e = sqe; // even if database down 'extra admins' can log on.
            }
            if (user == null) {
                User admin =  Authenticate.getLoggedInExtraAdmin(userName);
                if (admin != null) {
                    user = admin.getNode();
                }
            }
            if (user == null && e != null) {
                throw new SecurityException(e);
            }
            userCache.put(userName, user);
        }
        return user;
    }

    /**
     * UserName must be unique, check it also here (to throw nicer exceptions)
     */
    public int insert(String owner, MMObjectNode node) {
        int res = super.insert(owner, node);
        String userName = node.getStringValue("username");
        NodeSearchQuery nsq = new NodeSearchQuery(this);
        StepField sf        = nsq.getField(getField("username"));
        Constraint cons = new BasicFieldValueConstraint(sf, userName);
        nsq.setConstraint(cons);
        try {
            Iterator i = getNodes(nsq).iterator();
            while(i.hasNext()) {
                MMObjectNode n = (MMObjectNode) i.next();
                if (n.getNumber() == node.getNumber()) continue;
                removeNode(node);
                throw new SecurityException("Cannot insert user '" + userName + "', because there is already is a user with that name");
            }
        } catch (SearchQueryException sqe) {
            throw new SecurityException("Cannot insert user '" + userName + "', because check-query failed:" + sqe.getMessage() ,sqe );
        }
        userCache.clear();
        return res;
    }



    /**
     * @see org.mmbase.security.implementation.cloudcontext.User#getOwnerField
     */

    public String getDefaultContext(MMObjectNode node)  {
        return node.getNodeValue(FIELD_DEFAULTCONTEXT).getStringValue("name");
    }

    /**
     * @return The string representation the username of the User node.
     */
    public String getUserName(MMObjectNode node) {
         return node.getStringValue(FIELD_USERNAME);
    }

    /**
     * Encodes a password for storage (to avoid plain text passwords).
     */
    protected String encode(String s)  {
        return encoder.encode(s);
    }

    /**
     * @javadoc
     */
    public boolean isValid(MMObjectNode node)  {
        return true;
        /*
        if (node == null) { // if this is the case in the previous line you would have had a nullpointerexception.
            log.debug("node was null!");
            return false;
        }
        log.debug("original node #" + mmobjectnode.getNumber() + ": " + mmobjectnode.hashCode() + " current node #" + mmobjectnode1.getNumber() + " : " + mmobjectnode1.hashCode());


        if (mmobjectnode1 == mmobjectnode) {
            return true;
        } else {
            log.debug("hashcode's were different, comparing the number fields");
            return mmobjectnode.getNumber() == mmobjectnode1.getNumber();
       }
        */
    }

    /**
     * Makes sure unique values and not-null's are filed
     */
    public void setDefaults(MMObjectNode node) {
        MMObjectNode defaultDefaultContext = Contexts.getBuilder().getContextNode(node.getStringValue("owner"));
        node.setValue(FIELD_DEFAULTCONTEXT, defaultDefaultContext);
        node.setValue(FIELD_PASSWORD, "");
        setUniqueValue(node, FIELD_USERNAME, "user");
        if (getField(FIELD_VALID_FROM) != null) {
            node.setValue(FIELD_VALID_FROM,System.currentTimeMillis()/1000);
        }
        if (getField(FIELD_VALID_TO) != null) {
            node.setValue(FIELD_VALID_TO, VALID_TO_DEFAULT);
        }
     }

    public Parameter[] getParameterDefinition(String function) {
        Parameter[] params = org.mmbase.util.functions.NodeFunction.getParametersByReflection(Users.class, function);
        if (params == null) return super.getParameterDefinition(function);
        return params;
    }

    /**
     * @javadoc
     */
    public boolean check() {
        return true;
    }

   protected Object executeFunction(MMObjectNode node, String function, List args) {
        if (function.equals("info")) {
            List empty = new ArrayList();
            java.util.Map info = (java.util.Map) super.executeFunction(node, function, empty);
            info.put("gui", "(status..) Gui representation of this object.");
            if (args == null || args.size() == 0) {
                return info;
            } else {
                return info.get(args.get(0));
            }
        }  else if (function.equals("rank")) {
            return getRank(node);
        } else if (args != null && args.size() > 0) {
            if (function.equals("gui")) {
                String field = (String) args.get(0);

                if (FIELD_STATUS.equals(field)) {
                    // THIS KIND OF STUFF SHOULD BE AVAILEBLE IN MMOBJECTBUILDER.
                    String val = node.getStringValue(field);
                    ResourceBundle bundle;
                    Parameters pars = Parameters.get(GUI_PARAMETERS, args);
                    Locale locale = (Locale) pars.get(Parameter.LOCALE);
                    if (locale == null) {
                        String lang = (String) pars.get(Parameter.LANGUAGE);
                        if (lang != null){
                            locale = new Locale(lang, "");
                        }
                    }
                    if (locale == null) {
                        locale = mmb.getLocale();
                    }
                    bundle = ResourceBundle.getBundle(STATUS_RESOURCE,  locale, getClass().getClassLoader());

                    try {
                        return bundle.getString(val);
                    } catch (MissingResourceException e) {
                        return val;
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Function '" + function + "'  not matched in users");
        }
        return super.executeFunction(node, function, args);
    }

    public boolean equals(MMObjectNode o1, MMObjectNode o2) {
        return o1.getNumber() == o2.getNumber();
    }

    public String toString(MMObjectNode n) {
        return n.getStringValue("username");
    }



}
