/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util.xml;

import org.mmbase.bridge.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.functions.*;

import org.apache.xpath.XPathAPI;

/**
 * Nodes of the bridge can have `virtual fields', which are in fact
 * functions on the node. An example of such a function is "gui()". If
 * you want to use these functions in an XSL transformation it is
 * necessary to use this Xalan extension. The XSLT looks like this then:
 *
 <pre>
  &lt;xsl:stylesheet  version = "1.0"
    xmlns:xsl ="http://www.w3.org/1999/XSL/Transform"
    xmlns:node ="org.mmbase.bridge.util.xml.NodeFunction"
  &gt;
  ..
  </pre>
  You can then use a function like this if in the current template is a DOM Node with a 'field' subnode.
  <pre>
  ..
  &lt;img src="{$formatter_imgdb}{node:function(., 'cache(s(180x180))')}" /&gt;
  </pre>
  And otherwise you can also feed it a number:
  <pre>
  ..
  &lt;img src="{$formatter_imgdb}{node:function(string(@number), 'cache(s(180x180))')}" /&gt;
  </pre>
  Possibly even with the name of MMBase:
  <pre>
  ..
  &lt;img src="{$formatter_imgdb}{node:function('mmbase', string(@number), 'cache(s(180x180))')}" /&gt;
  </pre>
 *
 *
 * @author  Michiel Meeuwissen
 * @version $Id: NodeFunction.java,v 1.14 2005-10-07 18:46:55 michiel Exp $
 * @since   MMBase-1.6
 */

public  class NodeFunction {
    private static final Logger log = Logging.getLoggerInstance(NodeFunction.class);



    /**
     * Supposes the default cloud 'mmbase'.
     * @param  node  The number (or alias) of the Node
     * @param  function The function (with arguments).
     * @return The result of the function (as a String)
     * @see #function(String, String, String)
     */
    public static String function(String node, String function) {
        if (log.isDebugEnabled()) {
            log.debug("calling with string '" + node + "' function: " + function);
        }
        return function("mmbase", node, function);
    }

    /**
     * @param  cloudName The name of the Cloud.
     * @param  number  The number (or alias) of the Node
     * @param  function The function (with arguments).
     * @return The result of the function (as a String)
     */
    public static String function(String cloudName, String number, String function) {
        log.debug("calling base for cloud " + cloudName);
        try {
            Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud(cloudName);
            return function(cloud, number, function);
        } catch (Exception e) {
            return "could not execute '" + function + "' on node '" + number + "' (" + e.toString() + ")";
        }
    }

    
    /**
     * @since MMBase-1.8
     */
    public static org.w3c.dom.Element nodeFunction(org.w3c.dom.NodeList destination, Cloud cloud, String number, String function, String arguments) {
        // it only want to work withh a NodeList. I think my book sais that it should also work with
        // Element, but no..

        try {
            Node node = cloud.getNode(number);
            Generator gen = new Generator(destination.item(0).getOwnerDocument());
            java.util.List args = org.mmbase.util.StringSplitter.splitFunctions(arguments);
            if (log.isDebugEnabled()) {
                log.debug("Executing " + function+ " " + args + " on " + node.getNumber());
            }
            Node resultNode = node.getFunctionValue(function, args).toNode();
            org.w3c.dom.Element element = gen.add(resultNode);
            if (log.isDebugEnabled()) {
                log.debug("Returning " + org.mmbase.util.xml.XMLWriter.write(element, false));
            }
            return element;
        } catch (Exception e) {
            log.error("" + e + " " + Logging.stackTrace(e));
            return null;
        }
    }

    /**
     * It can be handy to supply a whole node, it will search for the field 'number' itself.
     * @param  node  The number (or alias) of the Node
     * @param  function The function (with arguments).
     * @return The result of the function (as a String)
     * @throws javax.xml.transform.TransformerException if xpath fails
     */
    public static String function(org.w3c.dom.Node node, String function) throws javax.xml.transform.TransformerException {
        log.debug("calling with dom node");
        String number = XPathAPI.eval(node, "./field[@name='number']").toString();
        return function(number, function);
    }

    public static String function(Cloud cloud, String number, String function) {
        log.debug("calling base on " + number + " for " + function);
        Node node;
        try {
            node = cloud.getNode(number);
            Function func = node.getFunction(function);
            Parameters params = func.createParameters();
            params.setIfDefined(Parameter.CLOUD, cloud);
            return func.getFunctionValue(params).toString();
        } catch (Throwable e) {
            log.info("could not execute '" + function + "' on node '" + number + "'");
            log.info(Logging.stackTrace(e) + Logging.stackTrace());
            return "could not execute " + function + " on node " + number + "(" + e.getClass() + " " + e.getMessage() + ")";
        }
    }

    /**
     * It can be handy to supply a whole node, it will search for the field 'number' itself.
     * @param cloud cloud to execute in
     * @param  node  The number (or alias) of the Node
     * @param  function The function (with arguments).
     * @return The result of the function (as a String)
     * @throws javax.xml.transform.TransformerException if xpath fails
     */
    public static String function(Cloud cloud, org.w3c.dom.Node node, String function) throws javax.xml.transform.TransformerException {
        log.debug("calling with dom node");
        String number = XPathAPI.eval(node, "./field[@name='number']").toString();
        return function(cloud, number, function);
    }

    /**
     * @since MMBase-1.8
     */
    public static String guiName(Cloud cloud,  String node) {
        return cloud.getNode(node).getNodeManager().getGUIName();
    }

}
