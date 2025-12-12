/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import org.mmbase.core.event.NodeEvent;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.functions.*;
import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.bridge.util.Queries;

/**
 * The properties builder can contain key/value pairs for any other node in MMBase.
 *
 * When using bridge, properties can be set and get using the 'nodemanager' function 'get' and 'set'
 * on this builder.
 *
 * In core, the (legacy?) methods on MMObjectNode should still work.
 *
 * Example code for bridge:
 <pre>
      &lt;mm:cloud rank="administrator"&gt;
        &lt;div&gt;
          &lt;mm:import id="key"&gt;test&lt;/mm:import&gt;
          &lt;mm:import id="value"&gt;&lt;mm:time time="now" /&gt;&lt;/mm:import&gt;
          &lt;mm:listnodes type="news" max="1"&gt;
            &lt;p&gt;TEST: &lt;mm:function nodemanager="properties" name="get" referids="_node@node,key" /&gt;&lt;/p&gt;
            &lt;p&gt;TEST: &lt;mm:function nodemanager="properties" name="set" referids="_node@node,key,value" /&gt;&lt;/p&gt;
            &lt;p&gt;TEST: &lt;mm:function nodemanager="properties" name="get" referids="_node@node,key" /&gt;&lt;/p&gt;
          &lt;/mm:listnodes&gt;
        &lt;/div&gt;
      &lt;/mm:cloud&gt;

      &lt;mm:nodelistfunction nodemanager="properties" name="list" referids="_node@node"&gt;
        &lt;mm:field name="key" /&gt;:&lt;mm:field name="value" /&gt;
      &lt;/mm:nodelistfunction&gt;
 </pre>
 *
 * @version $Id$
 */
public class Properties extends MMObjectBuilder {

    private static final Logger log = Logging.getLoggerInstance(Properties.class);

    @Override
    public String getGUIIndicator(MMObjectNode node) {
        String str = node.getStringValue("key");
        if (str.length() > 15) {
            return str.substring(0, 12) + "...";
        } else {
            return str;
        }
    }

    /**
     * @since MMBase-1.8.6
     */
    protected final static Parameter<Node>  NODE   = new Parameter<Node>("node", Node.class, true);
    protected final static Parameter<String> KEY   = new Parameter<String>("key", String.class, true);
    protected final static Parameter<Object> VALUE = new Parameter<Object>("value", Object.class);
    protected final static Parameter<Object>  DEFAULT = new Parameter<Object>("default", Object.class);
    protected final static Parameter[] LIST_PARAMETERS = { NODE };
    protected final static Parameter[] GET_PARAMETERS = { NODE, KEY, DEFAULT };
    protected final static Parameter[] SET_PARAMETERS = { new Parameter.Wrapper(GET_PARAMETERS), VALUE };


    /**
     * @since MMBase-1.8.6
     */
    protected List<Node> getValueNode(Node node, String key) {
        NodeQuery q = node.getCloud().getNodeManager(Properties.this.getTableName()).createQuery();
        Queries.addConstraint(q, Queries.createConstraint(q, "parent", FieldCompareConstraint.EQUAL, node));
        Queries.addConstraint(q, Queries.createConstraint(q, "key", FieldCompareConstraint.EQUAL, key));
        return q.getNodeManager().getList(q);
    }
    /**
     * @since MMBase-1.8.6
     */
    protected Object getValue(List<Node> prop) {
        if (prop.size() == 0) {
            return null;
        } else if (prop.size() == 1) {
            return prop.get(0).getValue("value");
        } else {
            List<Object> result = new ArrayList<Object>();
            for (Node p : prop) {
                result.add(p.getValue("value"));
            }
            return result;
        }
    }
    /**
     * @since MMBase-1.8.6
     */
    protected Object getValue(Node node, String key) {
        return getValue(getValueNode(node, key));

    }

    /**
     * @since MMBase-1.9.1
     */
    protected NodeList getPropertyNodes(Node node) {
        NodeQuery q = node.getCloud().getNodeManager(Properties.this.getTableName()).createQuery();
        Queries.addConstraint(q, Queries.createConstraint(q, "parent", FieldCompareConstraint.EQUAL, node));
        return q.getNodeManager().getList(q);
    }

    {
        addFunction(new AbstractFunction<NodeList>("list", LIST_PARAMETERS) {
                public NodeList getFunctionValue(Parameters parameters) {
                    return Properties.this.getPropertyNodes(parameters.get(NODE));
                }
            });
        addFunction(new AbstractFunction<Object>("get", GET_PARAMETERS) {
                public Object getFunctionValue(Parameters parameters) {
                    Object v = Properties.this.getValue(parameters.get(NODE), parameters.get(KEY));
                    if (v == null) return parameters.get(DEFAULT);
                    return v;
                }
            });
        addFunction(new AbstractFunction<Object>("set", SET_PARAMETERS) {
                public Object getFunctionValue(Parameters parameters) {
                    Node node = parameters.get(NODE);
                    String key = parameters.get(KEY);
                    List<Node> list = new ArrayList<Node>(Properties.this.getValueNode(node, key));
                    Object orgValue = getValue(list);
                    Object newValue = parameters.get(VALUE);
                    if (newValue == null) {
                        for (Node n : list) {
                            n.delete(true);
                        }
                    } else if (newValue instanceof Collection) {
                        Collection c = (Collection) newValue;
                        while (list.size() > c.size()) {
                            list.remove(0).delete(true);
                        }
                        while (list.size() < c.size()) {
                            Node p = node.getCloud().getNodeManager(Properties.this.getTableName()).createNode();
                            p.setStringValue("key", key);
                            p.setNodeValue("parent", node);
                            list.add(p);
                        }
                        int i = 0;
                        for (Object v : c) {
                            Node n = list.get(i++);
                            n.setValue("value", v);
                            n.commit();
                        }

                    } else {
                        while (list.size() > 1) {
                            list.remove(0).delete(true);
                        }
                        while (list.size() < 1) {
                            Node p = node.getCloud().getNodeManager(Properties.this.getTableName()).createNode();
                            p.setStringValue("key", key);
                            p.setNodeValue("parent", node);
                            list.add(p);
                        }
                        Node n = list.get(0);
                        n.setValue("value", newValue);
                        n.commit();
                    }
                    if (orgValue == null) return parameters.get(DEFAULT);
                    return orgValue;
                }
            });
    }

    /* (non-Javadoc)
     * @see org.mmbase.module.core.MMObjectBuilder#notify(org.mmbase.core.event.NodeEvent)
     */
    public void notify(NodeEvent event) {
        if (event.getBuilderName().equals(this.getTableName())) {
            if (log.isDebugEnabled()) {
                log.debug("nodeChanged(): Property change ! "+ event.getMachine() + " " + event.getNodeNumber() +
                          " " + event.getBuilderName() + " "+ NodeEvent.newTypeToOldType(event.getType()));
            }
            if (event.getType() == NodeEvent.TYPE_CHANGE || event.getType() == NodeEvent.TYPE_NEW ) {
                // The passed node number is node of prop node
                MMObjectNode propNode = getNode(event.getNodeNumber());
                if (propNode != null) {
                    int parent = propNode.getIntValue("parent");
                    if (isNodeCached(new Integer(parent))) {
                        log.debug("nodeChanged(): Zapping node properties cache for " + parent);
                        MMObjectNode pnode = getNode(parent);
                        if (pnode != null) pnode.delPropertiesCache();
                    }
                }
            }
        }
        super.notify(event);
    }
}
