/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;

import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import java.util.concurrent.*;

/**
 * @javadoc
 * @application Tools
 * @author Daniel Ockeloen
 * @version $Id$
 */
public class MMEvents extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(MMEvents.class);

    private int notifyWindow = 3600;
    private ScheduledFuture future = null;

    public boolean init() {
        super.init();
        String tmp = getInitParameter("NotifyWindow");
        if (tmp != null) {
            try {
                int nw = Integer.parseInt(tmp);
                notifyWindow = nw;
            } catch (NumberFormatException xx) {}
        }
        boolean enableNotify = true;

        tmp = getInitParameter("EnableNotify");
        if (tmp != null && (tmp.equals("false") || tmp.equals("no"))) {
            enableNotify = false;
        }
        if (enableNotify) {
            future =  ThreadPools.scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    MMEvents.this.probeCall();
                }
                },
                100,  // shortly after
                300, TimeUnit.SECONDS);
            ThreadPools.identify(future, "MMEvents Probe");
        }
        return true;
    }

    public void shutdown() {
        if (future != null) {
            future.cancel(true);
        }
        super.shutdown();
    }


    private void probeCall() {
        // the queue is really a bad idea have to make up
        // a better way.
        final List<MMObjectNode> also = new ArrayList<MMObjectNode>();
        log.debug("MMEvent probe CALL");
        int now = (int)(System.currentTimeMillis()/1000);
        log.debug("The currenttime in seconds NOW="+now);
        MMObjectNode snode = null, enode = null;


        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            StepField startField = query.getField(getField("start"));
            query.addSortOrder(startField);
            query.setConstraint(new BasicFieldValueBetweenConstraint(startField, now, now + notifyWindow));
            if (log.isDebugEnabled()) log.debug("Executing query " + query);
            also.addAll(getNodes(query));
            if (also.size() > 0) {
                snode = also.get(also.size() - 1);
            }
        } catch (SearchQueryException e) {
            log.error(e);
        } catch (IllegalArgumentException iea) {
            if (mmb.getState()) {
                throw iea;
            }
        }

        try {
            NodeSearchQuery query = new NodeSearchQuery(this);
            StepField stopField = query.getField(getField("stop"));
            query.addSortOrder(stopField);
            query.setConstraint(new BasicFieldValueBetweenConstraint(stopField, now, now + notifyWindow));
            if (log.isDebugEnabled()) log.debug("Executing query " + query);
            also.addAll(getNodes(query));
            if (also.size() > 0 ) {
                enode =  also.get(also.size() - 1);
            }
        } catch (SearchQueryException e) {
            log.error(e);
        }
        MMObjectNode wnode = null;
        int sleeptime = -1;
        if (snode != null && enode == null) {
            sleeptime = snode.getIntValue("start");
            wnode = snode;
        }
        if (snode == null && enode != null) {
            sleeptime = enode.getIntValue("stop");
            wnode = enode;
        }
        if (snode != null && enode != null) {
            if (snode.getIntValue("start") < enode.getIntValue("stop")) {
                sleeptime = snode.getIntValue("start");
                wnode = snode;
            } else {
                sleeptime = enode.getIntValue("stop");
                wnode = enode;
            }
        }

        if (sleeptime != -1) {
            // WTF?

            if (log.isDebugEnabled()) {
                log.debug("SLEEPTIME=" + (sleeptime - now) + " wnode=" + wnode + " also=" + also);
            }
            final MMObjectNode waitNode = wnode;
            final int sleep = sleeptime;
            ThreadPools.scheduler.schedule(new Runnable() {
                    public void run() {
                        log.debug("Node local change " + waitNode.getNumber());
                        MMEvents.super.nodeLocalChanged(mmb.getMachineName(), "" + waitNode.getNumber(), tableName, "c");
                        for (MMObjectNode a : also) {
                            if ((a.getIntValue("start") == sleep) || (a.getIntValue("stop") == sleep)) {
                                log.debug("Node local change " + a.getIntValue("number"));
                                MMEvents.super.nodeLocalChanged(mmb.getMachineName(),"" + a.getNumber(), tableName,"c");
                            }
                        }
                    }
                }, (sleeptime - now), TimeUnit.SECONDS);
        }
    }

    @Override
    public int insert(String owner, MMObjectNode node) {
        int val = node.getIntValue("start");
        int newval = (int)(System.currentTimeMillis()/1000);
        if (val == -1) {
            node.setValue("start", newval);

        }
        val = node.getIntValue("stop");
        if (val == -1) {
            node.setValue("stop", newval);

        }
        return super.insert(owner, node);
    }

    @Override
    public boolean commit(MMObjectNode node) {
        int val = node.getIntValue("start");
        int newval= ( int)(System.currentTimeMillis()/1000);
        if (val == -1) {
            node.setValue("start", newval);

        }
        val = node.getIntValue("stop");
        if (val == -1) {
            node.setValue("stop", newval);

        }
        return super.commit(node);
    }
}
