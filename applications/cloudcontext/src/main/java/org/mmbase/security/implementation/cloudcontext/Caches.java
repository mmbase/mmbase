/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext;

import java.util.*;
import org.mmbase.cache.Cache;
import org.mmbase.core.event.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.security.Operation;
import org.mmbase.security.Rank;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This class is responsible for the Cloud Context Security caches. It contains them, and sets up a
 * listener to arrange invalidation of (entries of) them  when necessary.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since  MMBase-1.9.1
 */
public abstract class Caches {

    private static final Logger log = Logging.getLoggerInstance(Caches.class);


    protected static final Cache<Integer, Rank> rankCache = new Cache<Integer,Rank>(20) {
        @Override
        public String getName() {
            return "CCS:SecurityRank";
        }
        @Override
        public String getDescription() {
            return "Caches the rank of users. User node --> Rank";
        }
    };

    protected static final Cache<String, MMObjectNode> userCache = new Cache<String, MMObjectNode>(20) {
        @Override
        public String getName() {
            return "CCS:SecurityUser";
        }
        @Override
        public String getDescription() {
            return "Caches the users. UserName --> User Node";
        }
    };




    protected static final Cache<String, MMObjectNode>contextCache = new Cache<String, MMObjectNode>(30) { // 30 'contexts' (organisations or so)

        @Override
        public String getName() {
            return "CCS:ContextCache";
        }
        @Override
        public String getDescription() {
            return "Links owner field to Contexts MMObjectNodes";
        }
    };


    protected static final Cache<String, ContextProvider.AllowingContexts> allowingContextsCache = new Cache<String, ContextProvider.AllowingContexts>(200) { // 200 users.

        @Override
        public String getName() {
            return "CCS:AllowingContextsCache";
        }
        @Override
        public String getDescription() {
            return "Links user id to a set of contexts";
        }
    };



    protected static final Cache<String, Boolean> containsCache = new Cache<String, Boolean>(500) {
        @Override
        public String getName() {
            return "CCS:ContainedBy";
        }
        @Override
        public String getDescription() {
            return "group + group/user --> boolean";
        }
    };

    protected static final class OperationsCache extends Cache<String, Set<MMObjectNode>> {
        OperationsCache() {
            super(100);
        }
        @Override
        public String getName()        { return "CCS:SecurityOperations"; }
        @Override
        public String getDescription() { return "The groups associated with a security operation";}

        public Object put(MMObjectNode context, Operation op, Set<MMObjectNode> groups) {
            return super.put(op.toString() + context.getNumber(), groups);
        }
        public Set<MMObjectNode> get(MMObjectNode context, Operation op) {
            return super.get(op.toString() + context.getNumber());
        }

    }


    protected static final OperationsCache operationsCache = new OperationsCache();



    static {
        rankCache.putCache();
        userCache.putCache();

        contextCache.putCache();
        allowingContextsCache.putCache();
        operationsCache.putCache();
        containsCache.putCache();

        EventManager.getInstance().addEventListener(new NodeEventListener() {
                public void notify(NodeEvent event) {
                    Caches.notify(event);
                }
            });
        EventManager.getInstance().addEventListener(new RelationEventListener() {
                public void notify(RelationEvent event) {
                    Caches.notify(event);
                }
            });
    }

    public static Cache<Integer, Rank> getRankCache() {
        return rankCache;
    }

    public static Cache<String, MMObjectNode> getUserCache() {
        return userCache;
    }
    public static Cache<String, MMObjectNode> getContextCache() {
        return contextCache;
    }
    public static OperationsCache getOperationsCache() {
        return operationsCache;
    }
    public static Cache<String, ContextProvider.AllowingContexts> getAllowingContextsCache() {
        return allowingContextsCache;
    }

    public static Cache<String, Boolean> getContainsCache() {
        return containsCache;
    }


    static void notify(NodeEvent event) {

        try {
            Collection<String> contextBuilders = new BuilderNames(Verify.getInstance().getContextProvider().getContextQueries());

            if (log.isDebugEnabled()) {
                log.debug("Received " + event + " context builders are " + contextBuilders);
            }

            String builder = event.getBuilderName();
            if (builder.equals(Authenticate.getInstance().getUserProvider().getUserBuilder().getTableName())) {
                invalidateCaches(event.getNodeNumber());
            }
            if (contextBuilders.contains((builder))
                || builder.equals("rightsrel")
                || builder.equals("mmbasegroups")
                || builder.equals("mmbaseranks")
                ) {
                invalidateCaches();
            }
        } catch (ClassCastException cce) {
            log.warn(cce + " (Different security implementations?)");
        }
    }
    static void notify(RelationEvent event) {
        try {
            String sourceType = event.getRelationSourceType();
            String destType = event.getRelationDestinationType();
            String userBuilder = Authenticate.getInstance().getUserProvider().getUserBuilder().getTableName();
            Collection<String> contextBuilders = new BuilderNames(Verify.getInstance().getContextProvider().getContextQueries());
            if (log.isDebugEnabled()) {
                log.debug("Received " + event + " context builders are " + contextBuilders);
            }

            if (sourceType.equals(userBuilder)) {
                invalidateCaches(event.getRelationSourceNumber());
            }
            if (destType.equals(userBuilder)) {
                invalidateCaches(event.getRelationDestinationNumber());
            }
            if (contextBuilders.contains(sourceType) || contextBuilders.contains(destType)) {
                invalidateCaches();
            }
            if (sourceType.equals("mmbasegroups") || destType.equals("mmbasegroups")) {
                invalidateCaches();
            }
            if (sourceType.equals("mmbaseranks") || destType.equals("mmbaseranks")) {
                invalidateCaches();
            }
        } catch (ClassCastException cce) {
            log.warn(cce + " (Different security implementations?)");
        }
    }


    protected static boolean invalidateScheduled = false;
    private static final Timer timer = new Timer(true);
    protected static void invalidateCaches() {
        if (! invalidateScheduled) {
            invalidateScheduled = true;
            timer.schedule(new TimerTask() {
                    public void run() {
                        log.service("Invalidating all security caches now.");
                        synchronized(Caches.class) {
                            invalidateScheduled = false;
                            operationsCache.clear();
                            contextCache.clear();
                            allowingContextsCache.clear();
                            containsCache.clear();
                            Caches.class.notify();
                        }
                    }
                }, 1000);
        }
    }
    public static void waitForCacheInvalidation() throws InterruptedException {
        while (invalidateScheduled) {
            log.service("waiting for cache invalidation");
            synchronized(Caches.class) {
                Caches.class.wait();
            }
        }
    }

    protected static void invalidateCaches(int nodeNumber) {
        rankCache.remove(Integer.valueOf(nodeNumber));
        Iterator<Map.Entry<String, MMObjectNode>> i =  userCache.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, MMObjectNode> entry = i.next();
            MMObjectNode node = entry.getValue();
            if (node == null) {
                i.remove();
            } else {
                if (node.getNumber() == nodeNumber) {
                    i.remove();
                }
            }
        }
    }


}
