/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.context;

import java.util.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.mmbase.bridge.Query;
import org.mmbase.cache.Cache;
import org.mmbase.module.core.MMBase;
import static org.mmbase.module.core.MMObjectBuilder.FIELD_OWNER;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.security.*;
import org.mmbase.security.SecurityException;
import org.mmbase.storage.search.CompositeConstraint;
import org.mmbase.storage.search.Constraint;
import org.mmbase.storage.search.Step;
import org.mmbase.storage.search.StepField;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Authorization based on a XML-configuration file. The XML file contains users, groups and
 * contexts. Contextes provide rights to users and/or groups and are identified by a string (which
 * is stored in the owner field).
 *
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id$
 * @see    ContextAuthentication
 */
public class ContextAuthorization extends Authorization {
    private static final Logger   log = Logging.getLoggerInstance(ContextAuthorization.class);
    private Document 	    document;
    private ContextCache    cache = new ContextCache();

    protected  Cache<String, AllowingContexts> allowingContextsCache
        = new Cache<String, AllowingContexts>(200) { // 200 users.
            public String getName()        { return "CS:AllowingContextsCache"; }
            public String getDescription() { return "Links user id to a set of contexts"; }
        };

    private int            maxContextsInQuery = 50; // must be configurable

    /** contains elements of type = Operation */
    private Set<Operation>            globalAllowedOperations = new HashSet<Operation>();
    private Map<String, String>       replaceNotFound         = new HashMap<String, String>();
    private Map<UserContext, String>  userDefaultContexts     = new HashMap<UserContext, String>();
    private SortedSet<String>         allContexts;

    protected void load() {
        log.debug("using: '" + configResource + "' as config file for authentication");
        try {
            InputSource in = MMBaseCopConfig.securityLoader.getInputSource(configResource);
            // clear the cache of unfound contexts
            replaceNotFound.clear();
            allowingContextsCache.clear();
            // clear the cache of user default contexts
            userDefaultContexts.clear();
            // reload the security xml document
            document = new org.mmbase.util.xml.DocumentReader(in, this.getClass()).getDocument();
            getGlobalAllowedOperations();
            setAllContexts();
        } catch(java.io.IOException ioe) {
            throw new SecurityException("error loading configfile :'" + configResource + "'(" + ioe + ")", ioe);
        }
        log.debug("loaded: '" +  configResource + "' as config file for authorization");
    }

    public String getDefaultContext(UserContext user) throws SecurityException {
        String defaultContext = userDefaultContexts.get(user);
        if (defaultContext == null) {
            String xpath = "/contextconfig/accounts/user[@name='"+user.getIdentifier()+"']";
            Node found;
            try {
                log.debug("going to execute the query:" + xpath + " on file : " + configResource);
                XPath xp = XPathFactory.newInstance().newXPath();
                found = (Node) xp.evaluate(xpath, document, XPathConstants.NODE);
            } catch(XPathExpressionException xe) {
                throw new SecurityException("error executing query: '" + xpath + "' on file: '" + configResource + "'", xe);
            }
            if (found == null) {
                throw new SecurityException("Could not find user " + user.getIdentifier() + " in context security config file (" + configResource + ")") ;
            }

            NamedNodeMap nnm = found.getAttributes();
            Node contextNode = nnm.getNamedItem("context");
            defaultContext = contextNode.getNodeValue();
            userDefaultContexts.put(user,defaultContext);
        }
        if (log.isDebugEnabled()) {
            log.debug("user with name: " + user + " has the default context: " + defaultContext);
        }
        return defaultContext;
    }

    public void create(UserContext user, int nodeNumber) throws SecurityException {
        if (log.isDebugEnabled()) {
            log.debug("create on node #" + nodeNumber + " by user: " + user);
        }
        String defaultContext = getDefaultContext(user);
        setContext(user, nodeNumber, defaultContext);
    }

    public void update(UserContext user, int nodeNumber) throws SecurityException {
        if (log.isDebugEnabled()) {
            log.debug("update on node #" + nodeNumber+" by user: "  + user);
        }
    }

    public void remove(UserContext user, int nodeNumber) throws SecurityException{
        if (log.isDebugEnabled()) {
            log.debug("remove on node #" + nodeNumber + " by user: " + user);
        }
    }

    public void setContext(UserContext user, int nodeNumber, String context) throws SecurityException {
        // notify the log
        if (log.isDebugEnabled()) {
            log.debug("set context on node #"+nodeNumber+" by user: " + user + " to " + context );
        }
        // don't even bother if the context was already set.
        MMObjectNode node = getMMNode(nodeNumber);
        if (node.getStringValue(FIELD_OWNER).equals(context)) return;

        // check if is a valid context for us..
        Set<String> possible = getPossibleContexts(user, nodeNumber);
        if(!possible.contains(context)) {
            throw new SecurityException("could not set the context to "+context+" for node #"+nodeNumber+" by user: " +user);
        }

        // check if this operation is allowed? (should also be done somewhere else, but we can never be sure enough)
        verify(user, nodeNumber, Operation.CHANGE_CONTEXT);

        // well now really set it...
        node.setValue(FIELD_OWNER, context);
        node.commit();
        if (log.isDebugEnabled()) {
            log.debug("changed context settings of node #"+nodeNumber+" to context: "+context+ " by user: " +user);
        }
    }

    public String getContext(UserContext user, int nodeNumber) throws SecurityException {
        // notify the log
        if (log.isDebugEnabled()) {
            log.debug("get context on node #" + nodeNumber + " by user: " + user);
        }

        // check if this operation is allowed? (should also be done somewhere else, but we can never be sure enough)
        verify(user, nodeNumber, Operation.READ);

        // and get the value...
        return getContext(nodeNumber);
    }

    private void setAllContexts() throws SecurityException {
        allContexts = new TreeSet<String>();
        String xpath = "/contextconfig/contexts/context";
        log.trace("going to execute the query:" + xpath );
        NodeList found;
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            found = (NodeList) xp.evaluate(xpath, document, XPathConstants.NODESET);
        } catch(XPathExpressionException xe) {
            throw new SecurityException("error executing query: '" + xpath  +"' ", xe);
        }
        for (int i = 0; i < found.getLength(); i++) {
            Node context = found.item(i);
            NamedNodeMap nnm = context.getAttributes();
            Node contextNameNode = nnm.getNamedItem("name");
            allContexts.add(contextNameNode.getNodeValue());
        }
    }

    public Set<String> getPossibleContexts(UserContext user, int nodeNumber) throws SecurityException {
        if (log.isDebugEnabled()) {
            log.debug("get possible context on node #" + nodeNumber + " by user: " + user);
        }

        // check if this operation is allowed? (should also be done somewhere else, but we can never be sure enough)
        // TODO: research if we maybe better could use WRITE or CHANGE_CONTEXT as rights for this operation...
        verify(user, nodeNumber, Operation.READ);

        // retrieve the current context..
        String currentContext = getContext(user, nodeNumber);
        synchronized(replaceNotFound) {
            if(replaceNotFound.containsKey(currentContext)) {
                currentContext = replaceNotFound.get(currentContext);
            }
        }

        Set<String> list;
        synchronized(cache) {
            list = cache.contextGet(currentContext);
            if(list != null) {
                log.debug("cache hit");
                return list;
            }
            list = new HashSet<String>();
        }

        // possible contextes are dependeding of the context they're in...
        String xpath = "/contextconfig/contexts/context[@name='"+currentContext+"']/possible";
        log.debug("going to execute the query:" + xpath );
        NodeList found;
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            found = (NodeList) xp.evaluate(xpath, document, XPathConstants.NODESET);
        } catch(XPathExpressionException xe) {
            throw new SecurityException("error executing query: '" + xpath + "' ", xe);
        }

        for(int i = 0; i < found.getLength(); i++) {
            Node context = found.item(i);
            NamedNodeMap nnm = context.getAttributes();
            Node contextNameNode = nnm.getNamedItem("context");
            list.add(contextNameNode.getNodeValue());
            if (log.isDebugEnabled()) {
                log.debug("the context: " + contextNameNode.getNodeValue() + " is possible context for node #" +
                          nodeNumber + " by user: "  + user);
            }
        }
        synchronized(cache) {
            cache.contextAdd(currentContext, list);
        }
        return list;
    }

    /**
     * @since MMBase-1.9
     */
    protected String getContext(int nodeNumber) {
        MMObjectNode node = getMMNode(nodeNumber);
        return node.getStringValue("owner");
    }

    public boolean check(UserContext user, int nodeNumber, Operation operation) throws SecurityException{
        if (log.isDebugEnabled()) {
            log.debug("check on node #" + nodeNumber + " by user: " + user + " for operation " + operation);
        }

        // is our usercontext still valid?
        if(!manager.getAuthentication().isValid(user)) {
            throw new java.lang.SecurityException("the usercontext was expired");
        }
        // operations can be granted for the whole system...
        if(globalAllowedOperations.contains(operation)) {
            log.debug("not retrieving the node, since operation:" + operation + " is granted to everyone");
            return true;
        }

        //  look which groups belong to this,...
        String context = getContext(nodeNumber);
        return check(user, context, operation.toString());
    }

    private boolean check(UserContext user, int nodeNumber, String operation) throws SecurityException {
        return check(user, getContext(user, nodeNumber), operation);
    }

    private boolean check(UserContext user, String context, Operation operation) throws SecurityException {
        return check(user, context, operation.toString());
    }

    private boolean check(UserContext user, String context, String operation) throws SecurityException {
        // look if we have this one already inside the positive cache...
        synchronized(cache) {
            Boolean result = cache.rightGet(operation, context, user.getIdentifier());
            if(result != null) {
                log.trace("cache hit");
                return result.booleanValue();
            }
        }

        String xpath;
        xpath = "/contextconfig/contexts/context[@name='"+context+"']";
        Node found;
        try {
            if (log.isDebugEnabled()) {
                log.trace("going to execute the query:" + xpath );
            }
            XPathFactory xf = XPathFactory.newInstance();
            found = (Node) xf.newXPath().evaluate(xpath, document, XPathConstants.NODE);

            if (found == null) { // fall back to default
                log.warn("context with name :'" + context + "' was not found in the configuration " + configResource );

                // retrieve the default context...
                xpath = "/contextconfig/contexts/context[@name = ancestor::contexts/@default]";

                if (log.isDebugEnabled()) {
                    log.trace("going to execute the query:" + xpath + " on file : " + configResource);
                }

                found = (Node) xf.newXPath().evaluate(xpath, document, XPathConstants.NODE);

                if (found == null) {
                    throw new SecurityException("Configuration error: Context " + context + " not found and no default context found either (change " + configResource + ")");
                }

                // put it in the cache
                NamedNodeMap nnm = found.getAttributes();
                Node defaultContextNode = nnm.getNamedItem("name");
                String defaultContext = defaultContextNode.getNodeValue();

                synchronized(replaceNotFound) {
                    replaceNotFound.put(context, defaultContext);
                }
            }

            // found is not null now.
            // now get the requested operation

            // now do the same query with the default context...
            xpath = "operation[@type='" + operation + "']/grant";
            if (log.isDebugEnabled()) {
                log.debug("going to execute the query:" + xpath + " On " + found.toString());
            }
            NodeList grants =  (NodeList) xf.newXPath().evaluate(xpath, found, XPathConstants.NODESET);

            if (log.isDebugEnabled()) {
                log.debug("Found " + grants.getLength() + " grants on " + operation + " for context " + context) ;
            }

            Set<String> allowedGroups = new HashSet<String>();
            for(int currentNode = 0; currentNode < grants.getLength(); currentNode++) {
                Node contains = grants.item(currentNode);
                NamedNodeMap nnm = contains.getAttributes();
                Node groupNameNode = nnm.getNamedItem("group");
                if (groupNameNode == null) {
                    throw new SecurityException("Configuration error: 'grant' element must contain attribute 'group'");
                }
                allowedGroups.add(groupNameNode.getNodeValue());
                if (log.isDebugEnabled()) {
                    log.debug("the group "+groupNameNode.getNodeValue() +" is granted for context " + context);
                }
            }

            boolean allowed = userInGroups(user.getIdentifier(), allowedGroups, new HashSet<String>());
            if (log.isDebugEnabled()) {
                if (allowed) {
                    log.debug("operation " + operation + " was permitted for user with id " + user);
                } else {
                    log.debug("operation " + operation + " was NOT permitted for user with id " + user);
                }
            }

            // put it in the cache
            synchronized(cache) {
                cache.rightAdd(operation, context, user.getIdentifier(), allowed);
            }

            return allowed;

        } catch(XPathExpressionException xe) {
            throw new java.lang.SecurityException("error executing query: '"+ xpath + "' ", xe);
        }

    }



    private boolean userInGroups(String user, Set<String> groups, Set<String> done) {
        // look if we have something to do...
        if(groups.size() == 0) {
            log.debug("entering userInGroups(recursive) with username: '"+user+"' without any groups, so user was not found..");
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("entering userInGroups(recursive) with username: '" + user + "' and look if the user is in the following groups:");

            Iterator<String> di = groups.iterator();
            while (di.hasNext()) {
                log.debug("\t -> group : " + di.next());
            }
        }

        Set<String> fetchedGroups = new HashSet<String>();
        for (String groupname : groups) {
            // get the group we are researching....
            // well, since we are already exploring ourselve, no need to do it again....
            done.add(groupname);
            // do the xpath query...
            String xpath = "/contextconfig/groups/group[@name='" + groupname + "']/contains";

            if (log.isDebugEnabled()) {
                log.debug("\tresearching group with name : " + groupname);
                log.debug("\tgoing to execute the query:" + xpath );
            }

            NodeList found;
            XPathFactory xf = XPathFactory.newInstance();
            try {
                found = (NodeList) xf.newXPath().evaluate(xpath, document, XPathConstants.NODESET);
            } catch(XPathExpressionException xe) {
                throw new java.lang.SecurityException("error executing query: '" + xpath + "' ", xe);
            }
            // research the result...
            for(int i = 0; i < found.getLength(); i++) {
                Node contains = found.item(i);
                NamedNodeMap nnm = contains.getAttributes();
                String type = nnm.getNamedItem("type").getNodeValue();
                String named = nnm.getNamedItem("named").getNodeValue();
                if (log.isDebugEnabled()) {
                    log.debug("\t<contains type=\"" + type + "\" named=\"" + named + "\" />");
                }
                if(type.equals("group")) {
                    // this is a group...
                    // when not already known, add it to our to fetch-list
                    if(!done.contains(named)) {
                        log.debug("\tfound a new group with name "+named+", which could contain our user, adding it to the to fetch list");
                        fetchedGroups.add(named);
                    }
                } else if(type.equals("user")) {
                    // oh, maybe its me !!
                    if(named.equals(user)){
                        log.debug("found the user with name " + named + " thus allowed." );
                        return true;
                    }
                    log.debug("\tdid found the user with name " + named + " but is not we are looking for.");
                } else {
                    throw new SecurityException("dont know the type:" + type);
                }
            }
        }
        return userInGroups(user, fetchedGroups, done);
    }

    public void verify(UserContext user, int nodeNumber, Operation operation) throws SecurityException {
        if (log.isDebugEnabled()) {
            if (operation.getInt() > Operation.READ_INT ) {
                log.debug("assert on node #" + nodeNumber + " by user: "  + user + " for operation " + operation);
            } else {
                log.trace("assert on node #" + nodeNumber +" by user: " + user + " for operation " + operation);
            }
        }
        if (!check(user, nodeNumber, operation) ) {
            throw new SecurityException("Operation '" + operation + "' on " + nodeNumber + " (" + getContext(nodeNumber) + ") was NOT permitted to " + user.getIdentifier());
        }
    }


    public boolean check(UserContext user, int nodeNumber, int srcNodeNumber, int dstNodeNumber, Operation operation) throws SecurityException {
        if (operation == Operation.CREATE) {
            // may link on both nodes
            return check(user, srcNodeNumber, "link") && check(user, dstNodeNumber, "link");
        } else if (operation == Operation.CHANGE_RELATION) {
            return check(user, nodeNumber, Operation.WRITE.toString()) &&
                check(user, srcNodeNumber, "link") && check(user, dstNodeNumber, "link");
        } else {
            throw new RuntimeException("Called check with wrong operation " + operation);
        }
    }

    public void verify(UserContext user, int nodeNumber, int srcNodeNumber, int dstNodeNumber, Operation operation) throws SecurityException {
        if (operation == Operation.CREATE) {
            // may link on both nodes
            if(!check(user, srcNodeNumber, "link")) {
                throw new SecurityException("Operation 'link' on " + srcNodeNumber + " was NOT permitted to " + user.getIdentifier());
            }
            if (! check(user, dstNodeNumber, "link")) {
                throw new SecurityException("Operation 'link' on " + dstNodeNumber + " was NOT permitted to " + user.getIdentifier());
            }
        } else if (operation == Operation.CHANGE_RELATION) {
            if(!check(user, srcNodeNumber, "link")) {
                throw new SecurityException("Operation 'link' on " + srcNodeNumber + " was NOT permitted to " + user.getIdentifier());
            }
            if (! check(user, dstNodeNumber, "link")) {
                throw new SecurityException("Operation 'link' on " + dstNodeNumber + " was NOT permitted to " + user.getIdentifier());
            }
            verify(user, nodeNumber, Operation.WRITE);
        } else {
            throw new RuntimeException("Called check with wrong operation " + operation);
        }
    }

    private void getGlobalAllowedOperations() {
        // get all the Operations and add them to the globalAllowedOperations set..
        String xpath = "/contextconfig/global/allowed";
        log.debug("going to execute the query:" + xpath );
        NodeList found;
        XPathFactory xf = XPathFactory.newInstance();
        try {
            found = (NodeList) xf.newXPath().evaluate(xpath, document, XPathConstants.NODESET);
        } catch(XPathExpressionException xe) {
            throw new SecurityException("error executing query: '" + xpath + "' ", xe);
        }

        for (int i = 0 ; i < found.getLength(); i++) {
            Node allowed = found.item(i);
            NamedNodeMap nnm = allowed.getAttributes();
            Node contextNameNode = nnm.getNamedItem("operation");
            Operation operation = Operation.getOperation(contextNameNode.getNodeValue());
            log.info("Everyone may do operation:" + operation);
            if(globalAllowedOperations.contains(operation)) throw new SecurityException("operation:" + operation + " already in allowed list");
            globalAllowedOperations.add(operation);
        }
    }

    private static org.mmbase.module.core.MMObjectBuilder builder = null;

    private MMObjectNode getMMNode(int n) {
        if(builder == null) {
            MMBase mmb = MMBase.getMMBase();
            builder =  mmb.getMMObject("typedef");
            if(builder == null) {
                throw new SecurityException("builder 'typedef' not found");
            }
        }
        MMObjectNode node = builder.getNode(n);
        if(node == null) {
            throw new SecurityException("node " + n + " not found");
        }
        return node;
    }


    protected SortedSet<String> getAllContexts() {
        return allContexts;
    }

    protected SortedSet<String> getDisallowingContexts(UserContext user, Operation operation) {
        if (operation != Operation.READ) throw new UnsupportedOperationException("Currently only implemented for READ");
        SortedSet<String> set = new TreeSet<String>();
        for (String context : getAllContexts()) {
            if (! check(user, context, operation)) {
                set.add(context);
            }
        }
        return set;
    }


    public QueryCheck check(UserContext userContext, Query query, Operation operation) {
        if(globalAllowedOperations.contains(operation)) {
            return COMPLETE_CHECK;
        } else {
            if (operation == Operation.READ) {

                AllowingContexts ac = allowingContextsCache.get(userContext.getIdentifier());
                if (ac == null) {
                    // smart stuff for query-modification
                    SortedSet<String> disallowing = getDisallowingContexts(userContext, operation);
                    SortedSet<String> contexts;
                    boolean   inverse;
                    if (log.isDebugEnabled()) {
                        log.debug("disallowing: " + disallowing + " all " + getAllContexts());
                    }

                    // searching which is 'smallest' disallowing contexts, or allowing contexts.
                    if (disallowing.size() < (getAllContexts().size() / 2)) {
                        contexts = disallowing;
                        inverse = true;
                    } else {
                        contexts  = new TreeSet<String>(getAllContexts());
                        contexts.removeAll(disallowing);
                        inverse = false;
                    }
                    ac = new AllowingContexts(contexts, inverse);
                    allowingContextsCache.put(userContext.getIdentifier(), ac);
                }

                if (ac.contexts.size() == 0) {
                    if (ac.inverse) {
                        return COMPLETE_CHECK;
                    } else {
                        // may read nothing
                        Constraint mayNothing = query.createConstraint(query.createStepField(query.getSteps().get(0), "number"), -1);
                        return new Authorization.QueryCheck(true, mayNothing);
                    }
                }

                List<Step> steps = query.getSteps();
                if (steps.size() * ac.contexts.size() < maxContextsInQuery) {
                    Iterator<Step> i = steps.iterator();
                    Constraint constraint = null;
                    while (i.hasNext()) {
                        Step step = i.next();
                        StepField field = query.createStepField(step, "owner");
                        Constraint newConstraint = query.createConstraint(field, ac.contexts);
                        if (ac.inverse) query.setInverse(newConstraint, true);
                        if (constraint == null) {
                            constraint = newConstraint;
                        } else {
                            constraint = query.createConstraint(constraint, CompositeConstraint.LOGICAL_AND, newConstraint);
                        }
                    }
                    return new Authorization.QueryCheck(true, constraint);
                } else { // query would grow too large
                    return Authorization.NO_CHECK;
                }

            } else {
                //not checking for READ: never mind, this is only used for read checks any way
                return Authorization.NO_CHECK;
            }
        }
    }

    private static class AllowingContexts {
        private final SortedSet<String> contexts;
        private final boolean inverse;
        AllowingContexts(SortedSet<String>  c, boolean i) {
            contexts = c;
            inverse = i;
        }
    }
}
