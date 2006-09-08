/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import java.util.*;
import org.mmbase.module.core.MMObjectNode;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.bridge.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A NodeFunction represents a function on a node instances of this builder. This means
 * that it always has one implicit node argument. This node-argument needs not be mentioned in
 * the Parameter array of the constructor.
 *
 * @author Michiel Meeuwissen
 * @version $Id: NodeFunction.java,v 1.25 2006-09-08 18:34:12 michiel Exp $
 * @see org.mmbase.module.core.MMObjectBuilder#executeFunction
 * @see org.mmbase.bridge.Node#getFunctionValue
 * @see org.mmbase.util.functions.BeanFunction
 * @since MMBase-1.8
 */

public abstract class NodeFunction<R> extends AbstractFunction<R, Object> {

    private static final Logger log = Logging.getLoggerInstance(NodeFunction.class);

    /**
     * Utility function, for easy call of function on node by one string.
     */
    public static FieldValue getFunctionValue(Node node, String function) {
        if(node == null) {
            log.warn("Tried to execute node-function on null!");
            return null;
        }
        List args = new ArrayList();
        String functionName = getFunctionNameAndFillArgs(function, args);
        if (log.isDebugEnabled()) {
            log.debug("Executing " + functionName + " " + args + " on " + node.getNumber());
        }

        return node.getFunctionValue(functionName, args);
    }

    public static String getFunctionNameAndFillArgs(String function, java.util.List args) {
        String functionName = function;
        int pos1 = function.indexOf('(');
        if (pos1 != -1) {
            int pos2 = function.lastIndexOf(')');
            if (pos2 != -1) {
                functionName = function.substring(0, pos1);
                java.util.List args2 = org.mmbase.util.StringSplitter.splitFunctions(function.subSequence(pos1 + 1, pos2));
                args.addAll(args2);
            }
        }
        return functionName;
    }

    public NodeFunction(String name, Parameter[] def, ReturnType returnType) {
        super(name, getNodeParameterDef(def), returnType);
    }
    /**
     * @since MMBase-1.9
     */
    public NodeFunction(String name, Parameter... def) {
        super(name, getNodeParameterDef(def));
    }

    protected static Parameter[] getNodeParameterDef(Parameter... def) {
        List<Parameter> defList = new ArrayList(Arrays.asList(def));
        if (! defList.contains(Parameter.NODE)) defList.add(Parameter.NODE);
        if (! defList.contains(Parameter.CLOUD)) defList.add(Parameter.CLOUD);
        if (! defList.contains(Parameter.CORENODE)) defList.add(Parameter.CORENODE);
        return defList.toArray(Parameter.EMPTY);
    }

    /**
     * Returns a new instance of NodeInstanceFunction, which represents an actual Function.
     */
    final public Function<R, Object> newInstance(MMObjectNode node) {
        return new NodeInstanceFunction(node);
    }

    /**
     * Implements the function on a certain node. Override this method <em>or</em> it's bridge
     * counter-part {@link #getFunctionValue(org.mmbase.bridge.Node, Parameters)}.  Overriding the
     * bridge version has two advantages. It's easier, and mmbase security will be honoured. That
     * last thing is of course not necesary if you are not going to use other nodes.
     *
     * XXX: made final because it does not work well if you don't implement a bridge version
     */
    protected final R getFunctionValue(final MMObjectNode coreNode, final Parameters parameters) {
        if (coreNode == null) throw new RuntimeException("No node argument given for " + this + "(" + parameters + ")!");
        Node node = (Node) parameters.get(Parameter.NODE);
        if (node == null) {
            Cloud cloud   = (Cloud)  parameters.get(Parameter.CLOUD);
            if (cloud == null) {
                // lets try this
                try {
                    cloud = org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
                } catch (org.mmbase.security.SecurityException se) {
                    // perhaps class-security not implemented by security implementation.
                    log.warn("" + se.getMessage());
                    cloud = org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase");
                }
                if (cloud == null) {
                    throw new RuntimeException("No cloud argument given"  + this + "(" + parameters + ")!" + Logging.stackTrace());
                }
            }
            if (coreNode instanceof org.mmbase.module.core.VirtualNode) {
                node = new org.mmbase.bridge.implementation.VirtualNode((org.mmbase.module.core.VirtualNode) coreNode, cloud);
            } else {
                int number = coreNode.getNumber();
                if (number == -1) {
                    // must be in transaction or uncommited node
                    String tmpNumber = coreNode.getStringValue(MMObjectBuilder.TMP_FIELD_NUMBER);
                    if (cloud.hasNode(tmpNumber)) {
                        node = cloud.getNode(tmpNumber);
                    } else {
                        // last resort..., we're really desperate now.
                        // This happens when calling gui() in transaction.
                        // Perhaps we need something like a public new BasicNode(MMobjectNode, Cloud). Abusing VirtualNode for similar purpose now.
                        org.mmbase.module.core.VirtualNode virtual = new org.mmbase.module.core.VirtualNode(coreNode.getBuilder());
                        Iterator<Map.Entry<String, Object>> i = coreNode.getValues().entrySet().iterator();
                        while (i.hasNext()) {
                            Map.Entry<String, Object> entry =  i.next();
                            virtual.storeValue(entry.getKey(), entry.getValue());
                        }
                        node = new org.mmbase.bridge.implementation.VirtualNode(virtual, cloud);
                    }
                } else {
                    if (cloud.mayRead(number)) {
                        node = cloud.getNode(number);
                    } else {
                        log.warn("Could not produce Bridge Node for '" + number + "', cannot execute node function.");
                        return null;
                    }
                }
            }
            parameters.set(Parameter.NODE, node);
        }
        return getFunctionValue(node, parameters);

    }

    /**
     * Utility method to convert a {@link org.mmbase.bridge.Node} to a a {@link org.mmbase.module.core.MMObjectNode}.
     */
    protected final MMObjectNode getCoreNode(final MMObjectBuilder builder, final Node node) {
        if (node instanceof org.mmbase.bridge.implementation.VirtualNode) {
            return ((org.mmbase.bridge.implementation.VirtualNode) node).getNodeRef();
        } else {
            return builder.getNode(node.getNumber());
        }

    }

    /**
     */
    protected abstract R getFunctionValue(Node node, Parameters parameters);

    protected Node getNode(Parameters parameters) {
        if (! parameters.containsParameter(Parameter.NODE)) {
            throw new IllegalArgumentException("The function " + toString() + " requires a node argument");
        }
        Node node = (Node) parameters.get(Parameter.NODE);
        if (node == null) {
            throw new IllegalArgumentException("The '" + Parameter.NODE + "' argument of  " + getClass() + " " + toString() + " must not be null ");
        }
        return node;
    }

    /**
     * To implement a NodeFunction, you must override {@link #getFunctionValue(Node, Parameters)}.
     * This one can be overriden if the same function must <em>also</em> be a builder function.
     */
    public R getFunctionValue(Parameters parameters) {
        return  getFunctionValue(getNode(parameters), parameters);
    }

    /**
     * This represents the function on one specific Node. This is instantiated when new Istance
     * if called on a NodeFunction.
     */
    private class NodeInstanceFunction extends WrappedFunction<R, Object> {

        protected MMObjectNode node;

        public NodeInstanceFunction(MMObjectNode node) {
            super(NodeFunction.this);
            this.node = node;
        }
        //javadoc inherited
        public final R getFunctionValue(Parameters parameters) {
            parameters.set(Parameter.CORENODE, node);
            return NodeFunction.this.getFunctionValue(node, parameters);

        }

        public String toString() {
            return NodeFunction.this.toString() + " for node " + node.getNumber();
        }
    }

}


