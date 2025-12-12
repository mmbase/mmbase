
package org.mmbase.mynews;


import java.util.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.InsRel;

import org.mmbase.util.logging.*;


public class MyNewsBuilder extends org.mmbase.util.functions.ExampleBuilder {
    private static final Logger log = Logging.getLoggerInstance(MyNewsBuilder.class);

    // Adds a few 'virtual' fields.

    @Override
    public Object getValue(MMObjectNode node, String field) {
        if ("magazine".equals(field)) {
            log.trace("Getting magazine virtual field");
            BasicSearchQuery query = new BasicSearchQuery();
            query.addStep(this);
            MMObjectBuilder mags = getMMBase().getBuilder("mags");
            BasicRelationStep relation = query.addRelationStep((InsRel) getMMBase().getBuilder("posrel"), mags);
            query.addField(relation.getNext(), mags.getField("number"));
            try {
                List<MMObjectNode> nodes = getMMBase().getClusterBuilder().getClusterNodesFromQueryHandler(query);
                if (nodes.size() > 0) {
                    String magNumber = nodes.get(0).getStringValue("mags.number");
                    if (log.isDebugEnabled()) {
                        log.debug("Returning mag " + magNumber + mags.getNode(magNumber).getValues());
                    }
                    return mags.getNode(magNumber).getValues();
                } else {
                    log.service("No magazine node found for " + node + " query: " + org.mmbase.util.Casting.toString(query));
                    return null;
                }
            } catch (SearchQueryException sqe) {
                log.warn(sqe);
                return null;
            }
        } else if ("utitle".equals(field)) {
            String v = ("" + node.getValue("title")).toUpperCase();
            log.debug("Return " + v + " for utitle of node " + node.getNumber());
            return v;
        } else {
            return super.getValue(node, field);
        }

    }
}
