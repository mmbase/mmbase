/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import java.util.Locale;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;

import org.mmbase.bridge.Node;
import org.mmbase.bridge.Cloud;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
/**
 * A tag which is a 'NodeReferrerTag's can be the child of a
 * NodeProvider tag, which supplies a 'Node' to its child tags. For
 * example the FieldTag, needs the use the Node of the parent
 * NodeProviderTag and therefore would be a NodeReferrerTag.
 *
 * @author Michiel Meeuwissen
 * @version $Id: NodeReferrerTag.java,v 1.26.2.3 2007-07-16 15:55:53 michiel Exp $
 */

public abstract class NodeReferrerTag extends CloudReferrerTag {

    private static final Logger log = Logging.getLoggerInstance(NodeReferrerTag.class);
    protected Attribute parentNodeId = Attribute.NULL;
    private Attribute element  = Attribute.NULL;
    /**
     * The element attribute is used to access elements of
     * clusternodes.
     * @since MMBase-1.7.4
     */
    public void setElement(String e) throws JspTagException {
        element = getAttribute(e);
    }


    /**
     * A NodeReferrer probably wants to supply the attribute 'node',
     * to make it possible to refer to another node than the direct
     * ancestor.
     **/

    public void setNode(String node) throws JspTagException {
        parentNodeId = getAttribute(node);
    }

    /**
    * This method tries to find an ancestor object of type NodeProvider
    * @return the NodeProvider if found else an exception.
    *
    */
    public NodeProvider findNodeProvider() throws JspTagException {
        return (NodeProvider) findParentTag(NodeProvider.class, (String) parentNodeId.getValue(this));
    }
    /**
    * This method tries to find an ancestor object of type NodeProvider
    * @return the NodeProvider or null.
    *
    */
    public NodeProvider findNodeProvider(boolean throwexception) throws JspTagException {
        return (NodeProvider) findParentTag(NodeProvider.class, (String) parentNodeId.getValue(this), throwexception);
    }

    /**
     * Gets the Node variable from the parent NodeProvider.
     * @return a org.mmbase.bridge.Node
     */

    protected Node getNode() throws JspTagException {
        Node node =  parentNodeId == Attribute.NULL ? (Node) pageContext.findAttribute(NodeProviderHelper._NODE) : null;
        // get the node from a parent element.
        if (node == null) {
            node = findNodeProvider().getNodeVar();
        } else {
            node = (Node) org.mmbase.util.Casting.unWrap(node);
        }

        if (node != null && element != Attribute.NULL) {
            node = node.getNodeValue(element.getString(this));
        }
        return node;
    }
    public Cloud getCloudVar() throws JspTagException {
        CloudProvider cp = findCloudProvider(false);
        if (cp != null) {
            return cp.getCloudVar();
        }
        NodeProvider np = findNodeProvider(false);
        if (np != null) {
            Node n = np.getNodeVar();
            if (n != null) return n.getCloud();
        }
        return super.getCloudVar();
    }


    protected void fillStandardParameters(Parameters p) throws JspTagException {
        super.fillStandardParameters(p);
        NodeProvider np = findNodeProvider(false);
        if (np != null) {
            Node node = np.getNodeVar();
            Cloud cloud = node.getCloud();
            p.setIfDefined(Parameter.CLOUD, cloud);
            p.setIfDefined(Parameter.USER, cloud.getUser());

        }
    }

    public Locale getLocale() throws JspTagException {
        LocaleTag localeTag = (LocaleTag)findParentTag(LocaleTag.class, null, false);
        if (localeTag != null) {
            Locale locale = localeTag.getLocale();
            if (locale != null) {
                return locale;
            }
        }
        CloudProvider cp = findCloudProvider(false);
        if (cp != null) {
            return  getCloudVar().getLocale();
        }
        NodeProvider np = findNodeProvider(false);
        if (np != null) {
            return  np.getNodeVar().getCloud().getLocale();
        }
        Locale loc = (Locale) pageContext.getAttribute(LocaleTag.KEY, LocaleTag.SCOPE);
        if (loc != null) return loc;
        return getCloudContext().getDefaultLocale();
    }

}
