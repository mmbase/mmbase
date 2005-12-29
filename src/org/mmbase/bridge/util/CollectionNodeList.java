/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util;

import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.BasicList;
import java.util.*;
import org.mmbase.util.Casting;
import org.mmbase.util.logging.*;

/**
 * An (unmodifiable) list of nodes, based on a Collection of Nodes. If the collection is a NodeList it will mirror its' changes. Otherwise, no.
 *
 * @author Michiel Meeuwissen
 * @version $Id: CollectionNodeList.java,v 1.2 2005-12-29 22:08:25 michiel Exp $
 * @since MMBase-1.8
 */
public class CollectionNodeList extends AbstractBridgeList implements NodeList {

    private static final Logger log = Logging.getLoggerInstance(CollectionNodeList.class);
    protected Cloud cloud;
    protected NodeManager nodeManager = null;

    protected final NodeList wrappedCollection;



    public CollectionNodeList(Collection c, NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.cloud = nodeManager.getCloud();
        this.wrappedCollection = convertedList(c, cloud);
    }


    public CollectionNodeList(Collection c, Cloud cloud) {
        this.cloud = cloud;
        this.wrappedCollection = convertedList(c, cloud);
    }
    public CollectionNodeList(Collection c) {
        this(c, ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null));
    }

    private static NodeList convertedList(Collection c, Cloud cloud) {
        if (c instanceof NodeList) {
            return (NodeList) c;
        } else {
            NodeList l = cloud.createNodeList();
            l.addAll(c);
            return l;
        }
    }


    public int size() {
        return wrappedCollection.size();
    }
    public Object get(int index) {
        return convert(wrappedCollection.get(index), index);
    }

    /**
     */
    protected Object convert(Object o, int index) {
        if (o instanceof Node || o == null) {
            return o;
        }
        Node node = null;
        if (o instanceof String) { // a string indicates a nodemanager by name
            node = cloud.getNodeManager((String)o);
        } else if (o instanceof Map) {
            if (nodeManager != null) {
                node = new MapNode((Map) o, nodeManager);
            } else {
                node = new MapNode((Map) o, cloud);
            }
        } else {
            // last desperate try
            node = cloud.getNode(Casting.toString(o));
        }
        set(index, node);
        return node;
    }

    /**
     *
     */
    public Node getNode(int index) {
        return (Node)get(index);
    }

    /**
     *
     */
    public NodeList subNodeList(int fromIndex, int toIndex) {
        return new CollectionNodeList(subList(fromIndex, toIndex), cloud);
    }

    /**
     *
     */
    public NodeIterator nodeIterator() {
        return new BasicNodeIterator();
    }


    protected class BasicNodeIterator extends BasicIterator implements NodeIterator {

        public Node nextNode() {
            return (Node)next();
        }

        public Node previousNode() {
            return (Node)previous();
        }
    }
}
