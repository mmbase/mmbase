/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;
import org.mmbase.bridge.jsp.taglib.util.Attribute;
import javax.servlet.jsp.JspTagException;

import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeIterator;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.Node;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * RelatedNodesTag, provides functionality for listing single related nodes in MMBase
 *
 * @author Kees Jongenburger
 * @author Michiel Meeuwissen
 * @author Pierre van Rooden
 * @author Jaco de Groot
 */
public class RelatedNodesTag extends AbstractNodeListTag {
    private static Logger log = Logging.getLoggerInstance(RelatedNodesTag.class.getName());
    protected Attribute type      = Attribute.NULL;
    protected Attribute role      = Attribute.NULL;
    protected Attribute searchDir = Attribute.NULL;

    /**
     * @param type a nodeManager
     */
    public void setType(String type) throws JspTagException {
        this.type = getAttribute(type);
    }
    /**
     * @param role a role
     */
    public void setRole(String role) throws JspTagException {
        this.role = getAttribute(role);
    }

    /**
     * The search parameter, determines how directionality affects the search.
     * Possible values are <code>both</code>, <code>destination</code>,
     * <code>source</code>, and <code>all</code>
     * @param search the swerach value
     */
    public void setSearchdir(String search) throws JspTagException {
        searchDir = getAttribute(search);
    }


    /**
     * Performs the search
     */
    public int doStartTag() throws JspTagException {
        int superresult =  doStartTagHelper(); // the super-tag handles the use of referid...
        if (superresult != NOT_HANDLED) {
            return superresult;
        }
        // obtain a reference to the node through a parent tag
        Node parentNode = getNode();
        if (parentNode == null) {
            throw new JspTagException("Could not find parent node!!");
        }

        
        NodeList nodes;
        if ( (!constraints.getString(this).equals("")) || (!orderby.getString(this).equals("")) ) { 
            log.debug("given orderby or constraints"); // start hacking:
            
            if (type == Attribute.NULL) {
                throw new JspTagException("Contraints attribute can only be given in combination with type attribute");
            }
            NodeManager manager = getCloud().getNodeManager(type.getString(this));
            NodeList initialnodes;

            initialnodes = parentNode.getRelatedNodes(type.getString(this), (String) role.getValue(this), (String) searchDir.getValue(this));

            StringBuffer where = null;
            for (NodeIterator i = initialnodes.nodeIterator(); i.hasNext(); ) {
                Node n = i.nextNode();
                if (where == null) {
                    where = new StringBuffer("" +  n.getNumber());
                } else {
                    where.append(",").append( n.getNumber());
                }
            }
            if (where == null) { // empty list, so use that one.
                nodes = initialnodes;
            } else {
                where.insert(0, "[number] in (").append(")");
                if (! constraints.getString(this).equals("")) where.insert(0, "(" + constraints.getString(this) + ") AND ");
                nodes = manager.getList(where.toString(), orderby.getString(this), directions.getString(this));
            }
        } else {
            log.debug("no orderby or constraints attributes");
            if (type == Attribute.NULL) {
                if (role != Attribute.NULL) {
                    throw new JspTagException("Must specify type attribute when using 'role'");
                }
                log.debug("no nodetype given");
                nodes = parentNode.getRelatedNodes();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Getting relatednodes type: " + type.getString(this) + " role: " + role.getValue(this) + " searchDir: " + searchDir.getValue(this));
                }
                nodes = parentNode.getRelatedNodes(type.getString(this), 
                                                   (String) role.getValue(this), 
                                                   (String) searchDir.getValue(this));
            }
        }
        return setReturnValues(nodes, true);
    }

}

