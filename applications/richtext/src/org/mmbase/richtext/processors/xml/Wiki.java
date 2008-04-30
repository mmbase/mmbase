/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.richtext.processors.xml;

import org.mmbase.bridge.*;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.xml.XMLWriter;
import org.mmbase.util.*;
import org.w3c.dom.*;
import javax.xml.transform.dom.*;
import java.util.*;

import org.mmbase.util.logging.*;

/**
 * Setting like wiki. A property of wiki-editing is, that you cannot point more then one idrel to
 * the same anchor. Depending on this, the id's of the idrel can be left unfilled, and in the
 * wiki-text, a user can simple refer to the node-number (or perhaps in later refinement some other
 * id of the node).
 *
 * @author Michiel Meeuwissen
 * @version $Id: Wiki.java,v 1.8 2008-04-30 12:17:03 michiel Exp $
 * @todo something goes wrong if same node relation multiple times.
 */

class Wiki {


    private static final Logger log = Logging.getLoggerInstance(Wiki.class);
    private static final long serialVersionUID = 1L;

    /**
     * Searches in the existsing relations for a relation with the
     * given id
     * @param a The anchor which we are trying to match
     * @param links List of alreayd existing relation objects
     * @param id (of a)
     */
    Node findById(Element a, NodeList links, String id) {
        NodeIterator ni = links.nodeIterator();
        while (ni.hasNext()) {
            Node relation = ni.nextNode();
            String relId = relation.getStringValue("id");
            log.debug("Id in " + relation.getNumber() + " " + relId + " comparing with '" + id);
            if (! "".equals(relId)) {
                String decorId = decorateId(id);
                String decorRelId = decorateId(relId);
                if (relId.equals(decorId) || relId.equals(decorRelId)) {
                    log.debug(relId + "==" + decorId);
                    return relation;
                } else {
                    log.debug(relId + "!=" + decorId);
                }
            } else {
                Node destination = relation.getNodeValue("dnumber");
                log.debug("Found " + destination);
                if (destination == null) {
                    log.warn("dnumber null in " + relation);
                } else {
                    if (destination.getStringValue("number").equals(id)) {
                        log.debug("Setting relation id of " + relation.getNumber() + " to " + destination.getNumber());
                        String decoratedId = decorateId("" + destination.getNumber());
                        relation.setStringValue("id", decoratedId);
                        relation.commit();
                        a.setAttribute("id", decoratedId);
                        log.debug("relation " + relation + " " + relation.getCloud());
                        return relation;
                    }
                }
            }
        }
        return null; // not found
    }

    String cleanId(String id) {
        if (id.startsWith("n_")) {
            return id.substring(2);
        } else {
            return id;
        }
    }
    String decorateId(String id) {
        return "n_" + cleanId(id);
    }

   /**
     * Simply considers the id the node-number, but this could be sophisitcated on.
     */
    Node getNode(Cloud cloud, String id) {
        return cloud.getNode(id);
    }

    /**
     * @param editedNode Node that is edited. Anchors will be either changed, or idrels will be
     * created/modified to be in order
     * @param source
     *
     */
    Document parse(Node editedNode, Document source) {

        Map<Integer, Node> usedLinks = new HashMap<Integer, Node>();
        // reolve anchors. Allow to use nodenumber as anchor.
        if (log.isDebugEnabled()) {
            log.debug("Resolving " + editedNode + " " + XMLWriter.write(source, true));
        }

        Cloud cloud = editedNode.getCloud();
        NodeManager objects = cloud.getNodeManager("object");
        NodeQuery q = Queries.createRelationNodesQuery(editedNode, objects, "idrel", "destination");
        NodeList links = cloud.getNodeManager("idrel").getList(q);

        // search all anchors
        org.w3c.dom.NodeList as = source.getElementsByTagName("a");
        for (int i = 0; i < as.getLength(); i++) {
            Element a = (Element) as.item(i);
            String id = a.getAttribute("id");

            if (log.isDebugEnabled()) {
                log.debug("Found " + XMLWriter.write(a, true));
            }
            Node link = findById(a, links, id);
            if (link == null) {
                log.service("No relation found with id'" + id + "'. Implicitely creating one now.");
                Node node = getNode(cloud, cleanId(id));
                try {
                    Relation newRel = editedNode.createRelation(node, cloud.getRelationManager(editedNode.getNodeManager(), node.getNodeManager(), "idrel"));
                    String decoratedId = decorateId(id);
                    newRel.setStringValue("id", decoratedId);
                    newRel.commit();
                    a.setAttribute("id", decoratedId);
                } catch (Exception e) {
                    log.warn(e);
                }

            }

        }

        return source;
    }


}
