/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext.builders;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.security.SecurityException;
import java.util.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.cache.Cache;
import org.mmbase.security.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Representation of a 'context', which can be read as a valid value
 * of the 'owner' field of any object in MMBase. Rights are
 * distributed using this thing. This is part of cloud context
 * security, so the 'context' values need to be present in the cloud.
 *
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: Contexts.java,v 1.4 2003-06-16 17:35:34 michiel Exp $
 */
public class Contexts extends MMObjectBuilder {
    private static Logger log = Logging.getLoggerInstance(Contexts.class.getName());
    private boolean readall = true;

    protected static Cache contextCache = new Cache(30) {
            public String getName()        { return "ContextCache"; }
            public String getDescription() { return "Links owner field to Contexts MMObjectNodes"; }
        };


    /**
     * @javadoc
     */
    static final String DEFAULT_CONTEXT = "admin";

    /**
     * @javadoc
     */
    public boolean init() {
        String s = (String) getInitParameters().get("readall");
        readall = "true".equals(s);

        contextCache.putCache();
        CacheInvalidator.getInstance().addCache(contextCache);
        mmb.addLocalObserver(getTableName(), CacheInvalidator.getInstance());
        mmb.addRemoteObserver(getTableName(), CacheInvalidator.getInstance());

        return super.init();
    }

    /**
     * Staticly receives the MMObjectBuilder instance (casted to Contexts). A utility function.
     */
    public static Contexts getBuilder() {
        return (Contexts) MMBase.getMMBase().getBuilder("mmbasecontexts");
    }


    /**
     * Implements check function with same arguments of Authorisation security implementation
     * @see Verify#check(user, nodeId, sourceNodeI, destinationNodeI, operation);
     */

    public boolean mayDo(User user, int nodeId, int sourceNodeId, int destinationNodeId, Operation operation) throws SecurityException {
        // admin bypasses security system
        if (user.getRank().getInt() >= Rank.ADMIN_INT) {
            log.debug("admin may do everything");
            return true;
        }        
        

        // retrieve the nodes
        MMObjectNode source      = getNode(sourceNodeId);
        MMObjectNode destination = getNode(destinationNodeId);

        if  ( (source.parent instanceof Users) && (destination.parent instanceof Ranks)) {
            if (user.getNode().equals(source)) {
                log.debug("Cannot change own rank");
                return false;
            }

            if (Ranks.getBuilder().getRank(destination).getInt() > user.getRank().getInt()) {
                log.debug("May not increase rank of other user to rank higher than own rank");
                return false;
            }
            if (operation == Operation.CREATE) { // may only create such a relation if there are none now.
                List ranks =  source.getRelatedNodes("mmbaseranks", ClusterBuilder.SEARCH_DESTINATION);
                if (ranks.size() > 0) {
                    return false;
                }
            }
        }

        return mayDo(user, nodeId, operation);

    }


    /**
     * Implements check function with same arguments of Authorisation security implementation
     * @see Verify#check(user, nodeId, operation);
     */
    public boolean mayDo(User user, int nodeId, Operation operation) throws SecurityException {

        // retrieve the node
        MMObjectNode node       = getNode(nodeId);
        MMObjectBuilder builder = node.getBuilder();

        // may never unlink relation with own rank
        if (operation == Operation.DELETE && builder instanceof InsRel) {
            MMObjectNode source      = getNode(node.getIntValue("snumber"));
            MMObjectNode destination = getNode(node.getIntValue("dnumber"));
            if (source.parent instanceof Users && destination.parent instanceof Ranks) {
                if (user.getNode().equals(source)) {
                    log.debug("May not unlink rank with own user object");
                    return false;
                }
                
            }                                          
        }

        // admin bypasses security system
        if (user.getRank().getInt() >= Rank.ADMIN_INT) {
            log.debug("admin may do everything");
            return true;
        }


        if (node == null) {
            throw new SecurityException("node #" + nodeId + " not found");
        }

        if (readall && operation == Operation.READ) {
            log.debug("Allowing all read operiations, so read on " + nodeId + " is permitted");
            return true;
        }

        // if this is a group node, then you may do anything on it, if you are member of the group.
        // should that be?
        if (builder instanceof Groups) {
            return Groups.getBuilder().contains(node, user);
        }

        // when it is our user node, and you are this user, you may do anything on it (change password)
        if (builder instanceof Users) {
            if (user.equals(node) && (operation == Operation.READ || operation == Operation.WRITE)) {
                if (log.isDebugEnabled()) {
                    log.debug("May always " + operation + " on own user node: " + nodeId);
                }
                return true;
            }
        }

        // retrieve all the groups in which the context of this node is.

        MMObjectNode contextNode = getContextNode(node); // the mmbasecontext node associated with this node
        Iterator iter = RightsRel.getBuilder().getGroups(contextNode, operation).iterator();        

        // now checking if this user is in one of these groups.
        while (iter.hasNext()) {
            MMObjectNode group = (MMObjectNode) iter.next();            
            log.trace("checking group " + group);
            if(Groups.getBuilder().contains(group, user)) { 
                if (log.isDebugEnabled()) {
                    log.debug("May " + operation + " on node: " + nodeId + " of context " + contextNode);
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("May not " + operation + " on node: " + nodeId + " of context " + contextNode);
        }
        return false;
    }

    /**
     * @return The MMObjectNode presenting the context of the given node.
     */
    private final MMObjectNode getContextNode(MMObjectNode node)  {
        String s = node.getStringValue("owner");
        
        MMObjectNode context = (MMObjectNode) contextCache.get(s);
        if (context == null) {

            Enumeration enumeration = searchWithWhere(" name = '" + s + "' ");
            if (enumeration.hasMoreElements()) {
                context =  (MMObjectNode) enumeration.nextElement();
            } else {
                // log.warn("context with name '" + s + "' not found, using default: '" + "admin" + "'");
                enumeration = searchWithWhere(" name = 'admin' ");
                if (!enumeration.hasMoreElements()) {
                    throw new SecurityException("no context with name 'admin' defined! This one is needed as default");
                }
                context = (MMObjectNode) enumeration.nextElement();
            }
            contextCache.put(s, context);
        }
        return context;
    }

    /**
     * @javadoc
     */
    public String getContext(User user, int i)  throws SecurityException {
        MMObjectNode node = getNode(i);
        if (node == null) {
            throw new SecurityException("node #" + i + " not found");
        }
        if (node.getBuilder() instanceof Groups) {
            return "unused";
        }
        return getContextNode(node).getStringValue("name");
    }

    /**
     * @javadoc
     */
    public MMObjectNode setContext(User user, int i, String s) throws SecurityException {
        MMObjectNode node = getNode(i);
        if (node == null) {
            throw new SecurityException("node #" + i + " not found");
        }
        if (node.getBuilder() instanceof Groups) { 
            node.setValue("owner", "system");
            node.commit();
            return node;
        }
        if (!getPossibleContexts(user, i).contains(s)) {
            throw new SecurityException("could not set the context to " + s + " for node #" + i + "(context name:" + s + " is not a valid context)");
        }
        node.setValue("owner", s);
        node.commit();
        return node;
    }

    /**
     * Wraps getPossibleContexts of Authorisation implementaiton Verify.
     * @see Verify#getPossibleContexts
     */
    public Set getPossibleContexts(User user, int nodeId) throws SecurityException {
        MMObjectNode node = getNode(nodeId);
        if (node == null) {
            throw new SecurityException("node #" + nodeId + " not found");
        }
        if (node.getBuilder() instanceof Groups) { 
            return new HashSet();  // why?
        }
        Enumeration enumeration = search(null);  // list all (readable) Contextes simply..

        Set hashSet = new HashSet();
        while (enumeration.hasMoreElements()) {
            MMObjectNode context = (MMObjectNode) enumeration.nextElement();
            if (mayDo(user, context.getNumber(), Operation.READ )) {
                hashSet.add(context.getStringValue("name"));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("context with name:" + context.getStringValue("name") + " could not be added to possible contexes, since we had no read rights");
                }
            }
        }
        return hashSet;
    }

    public String toString(MMObjectNode n) {
        return n.getStringValue("name");
    }

}
