/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.util.logging.*;

/**
 * 'Basic' implementation of bridge NodeQuery. Wraps a Query with all and only fields of one
 * Step. If there is only one step, this can wrap NodeSearchQuery of core.
 *
 * Often, queries with more steps are sensible nodequeries, because sorting (e.g. posrel.pos) or
 * filtering can be done on those steps (e.g. publishtimes).
 *
 * Nodes of this type can be used as an argument to function which do return 'real' nodes (so not clusternodes).
 *
 * @todo perhaps it would be nice to have the possibllity to query also two complete steps (also one of the neigbouring 'relation' steps).
 *         this would give nice efficient implementations of things like <mm:relatednode (of mm:listrelations)
 *
 * @todo This kind of functionality should perhaps be present in NodeSearchQuery itself because you can then use it 'under' the bridge too.
 *
 * @author Michiel Meeuwissen
 * @version $Id: BasicNodeQuery.java,v 1.10 2003-09-03 19:36:58 michiel Exp $
 * @since MMBase-1.7
 * @see org.mmbase.storage.search.implementation.NodeSearchQuery
 */
public class BasicNodeQuery extends BasicQuery implements NodeQuery {


    private static final Logger log = Logging.getLoggerInstance(BasicNodeQuery.class);

    protected Step step = null;


    BasicNodeQuery(Cloud c) {
        super(c);
    }

    /**
     * node query.
     */

    BasicNodeQuery(BasicNodeManager nodeManager) {
        super(nodeManager.getCloud());
        query = new NodeSearchQuery(nodeManager.getMMObjectBuilder());
        this.step = (Step) getSteps().get(0); // the only step
    }
    BasicNodeQuery(BasicNodeManager nodeManager, NodeSearchQuery q) {
        super(nodeManager.getCloud());
        query = q;
        this.step = (Step) getSteps().get(0); // the only step
    }

    /**
     * Makes a multi-step node-query, based on a normal query. As a default, all fields of last steps are added (if at least there are steps already)
     *
     */
    BasicNodeQuery(Cloud cloud, Query q) {
        super(cloud);
        query = new BasicSearchQuery(q);
        List steps = query.getSteps();
        if (steps.size() > 0) {
            setNodeStep((Step) (steps.get(steps.size() -1 )));
        }
    }

    public NodeManager getNodeManager() {
        if (step == null) return null;
        return cloud.getNodeManager(step.getTableName());
    }
    public Step getNodeStep() {
        return step;
    }

    // overridden from BasicQuery (a node query does not have '.' in its field names)
    public StepField createStepField(String fieldName) {
        if (fieldName.indexOf('.') == -1) { 
            BasicStepField stepField = (BasicStepField) createStepField(step, fieldName);
            stepField.setAlias(fieldName);
            return stepField;
        } else {
            return super.createStepField(fieldName);
        }
    }

    public StepField getStepField(Field field) {
        if (query instanceof NodeSearchQuery) {
            BasicStepField stepField = ((NodeSearchQuery) query).getField(((BasicField) field).field);
            return stepField;
        } else {
            Iterator fields = query.getFields().iterator();
            while(fields.hasNext()) {
                StepField stepField = (StepField) fields.next();
                if (stepField.getStep().equals(step)) {
                    if (stepField.getFieldName().equals(field.getName())) {
                        return stepField;
                    }
                }

            }
        }
        return null; // hmm.
    }

    public Step setNodeStep(Step step) {
        query.removeFields();
        query.addFields(step);
        Step prevStep = this.step;
        this.step = step;
        return prevStep;
    }



}
