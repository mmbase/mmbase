package org.mmbase.util.functions;

import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.bridge.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A bean can be accessed through the function framework. 
 *
 * @author Michiel Meeuwissen
 * @version $Id: ExampleBean.java,v 1.5 2005-10-18 21:51:30 michiel Exp $
 * @since MMBase-1.8
 */
public final class ExampleBean {

    private static final Logger log = Logging.getLoggerInstance(ExampleBean.class);
    private String parameter1;
    private Integer parameter2 = new Integer(0);
    private String parameter3 = "default";
    private Node node;
    private Cloud cloud;

    public void setParameter1(String hoi) {
        parameter1 = hoi;
    }

    public void setParameter2(Integer j) {
        parameter2 = j;
    }
    public Integer getParameter2() {
        return parameter2;
    }
    public void setAnotherParameter(String a) {
        parameter3 = a;
    }
    public String getAnotherParameter() {
        return parameter3;
    }

    /**
     * Makes this bean useable as a Node function.
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * Makes the functions useable in bridge (so with security)
     */
    public void setCloud(Cloud c) {
        cloud = c;
    }


    /**
     * A function defined by this class
     */
    public String stringFunction() {
        return "[[" + parameter1 + "/" + parameter3 + "]]";
    }

    public Integer integerFunction() {
        return new Integer(parameter2.intValue() * 3);
    }

    /**
     * A real node-function (using the node argument). Returns the next newer node of same type.
     * Also a nice example on the difference between core and bridge.
     */
    public Object successor() {
        if (node == null) throw new IllegalArgumentException("successor is a node-function");
        if (cloud != null) {
            log.debug("Using bridge (security restrictions will be honoured)");
            NodeManager nm = node.getNodeManager();
            NodeQuery q = nm.createQuery();
            StepField field = q.getStepField(nm.getField("number"));
            q.setConstraint(q.createConstraint(field, FieldCompareConstraint.GREATER, new Integer(node.getNumber())));
            q.addSortOrder(field, SortOrder.ORDER_ASCENDING);
            q.setMaxNumber(1);
            NodeIterator i = nm.getList(q).nodeIterator();
            return i.hasNext() ? i.nextNode() : null;
        } else {
            log.debug("Using core.");
            MMObjectBuilder builder = MMBase.getMMBase().getBuilder(node.getNodeManager().getName());
            NodeSearchQuery query = new NodeSearchQuery(builder);
            StepField field = query.getField(builder.getField("number"));
            BasicFieldValueConstraint cons = new BasicFieldValueConstraint(field, new Integer(node.getNumber()));
            cons.setOperator(FieldCompareConstraint.GREATER);
            query.setConstraint(cons);
            query.addSortOrder(field);
            query.setMaxNumber(1);
            try {
                java.util.Iterator i = builder.getNodes(query).iterator();
                return i.hasNext() ? ((MMObjectNode) i.next()) : null;
            } catch (Exception e) {
                return null;
            }
        }
    }

}
