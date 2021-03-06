/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.util;
import java.util.*;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.BasicFieldList;
import org.mmbase.util.functions.Function;

/**
 * Abstract implementation of NodeManager, to minimalize the implementation of a virtual one. Most
 * methods throw UnsupportOperationException (like in {@link
 * org.mmbase.bridge.implementation.VirtualNodeManager}).
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @see org.mmbase.bridge.NodeManager
 * @since MMBase-1.8
 */
public abstract class AbstractNodeManager extends AbstractNode implements NodeManager {

    protected Map<String, Object> values = new HashMap<String, Object>();
    protected final Cloud cloud;
    protected AbstractNodeManager(Cloud c) {
        cloud = c;
    }

    @Override
    protected void setValueWithoutChecks(String fieldName, Object value) {
        values.put(fieldName, value);
    }
    public Object getValueWithoutProcess(String fieldName) {
        return values.get(fieldName);
    }
    protected void edit(int action) {
        // go ahead
    }

    @Override
    protected void setSize(String fieldName, long size) {
        // never mind
    }
    public long getSize(String fieldName) {
        // never mind
        return 2;
    }
    public NodeManager getNodeManager() {
        return cloud.getNodeManager("typedef");
    }

    public void setNodeManager(NodeManager nm) {
        if (! nm.getName().equals("typedef")) {
            throw new IllegalArgumentException("Cannot change the node manager of node managers");
        }
    }

    public Cloud getCloud() {
        return cloud;
    }


    @Override
    public boolean isNodeManager() {
        return true;
    }
    @Override
    public NodeManager toNodeManager() {
        return this;
    }
    public Node createNode() { throw new UnsupportedOperationException();}


    public NodeList getList(String constraints, String sorted, String directions) {
        NodeQuery query = createQuery();
        Queries.addConstraints(query, constraints);
        Queries.addSortOrders(query, sorted, directions);
        NodeList list = getList(query);
        list.setProperty("constraints", constraints);
        list.setProperty("orderby",     sorted);
        list.setProperty("directions",  directions);
        return list;
    }


    public FieldList createFieldList() {
        return new BasicFieldList(Collections.emptyList(), this);
    }

    public NodeList createNodeList() {
        return new CollectionNodeList(BridgeCollections.EMPTY_NODELIST, this);
    }

    public RelationList createRelationList() {
        return new CollectionRelationList(BridgeCollections.EMPTY_RELATIONLIST, this);
    }

    public boolean mayCreateNode() {
        return false;
    }

    public NodeList getList(NodeQuery query) {
        if (query == null) query = createQuery();
        return getCloud().getList(query);
    }

    public NodeQuery createQuery() {
        return new org.mmbase.bridge.implementation.BasicNodeQuery(this);
    }
    public NodeList getList(String command, Map parameters, ServletRequest req, ServletResponse resp){ throw new UnsupportedOperationException();}

    public NodeList getList(String command, Map parameters){ throw new UnsupportedOperationException();}


    public RelationManagerList getAllowedRelations() { return BridgeCollections.EMPTY_RELATIONMANAGERLIST; }
    public RelationManagerList getAllowedRelations(String nodeManager, String role, String direction) { return BridgeCollections.EMPTY_RELATIONMANAGERLIST; }

    public RelationManagerList getAllowedRelations(NodeManager nodeManager, String role, String direction) { return BridgeCollections.EMPTY_RELATIONMANAGERLIST; }

    public String getInfo(String command) { return getInfo(command, null,null);}

    public String getInfo(String command, ServletRequest req,  ServletResponse resp){ throw new UnsupportedOperationException();}


    protected abstract Map<String, Field> getFieldTypes();


    public boolean hasField(String fieldName) {
        Map<String, Field> fieldTypes = getFieldTypes();
        return fieldTypes.isEmpty() || fieldTypes.containsKey(fieldName);
    }

    public final FieldList getFields() {
        return getFields(NodeManager.ORDER_NONE);
    }

    public final FieldList getFields(int sortOrder) {
        if (sortOrder == ORDER_NONE) {
            return new BasicFieldList(getFieldTypes().values(), this);
        } else {
            List<Field> orderedFields = new ArrayList<Field>();
            for (Field field : getFieldTypes().values()) {
                // include only fields which have been assigned a valid position, and are
                if (
                    ((sortOrder == ORDER_CREATE) && (field.getStoragePosition() > -1)) ||
                    ((sortOrder == ORDER_EDIT) && (field.getEditPosition() > -1)) ||
                    ((sortOrder == ORDER_SEARCH) && (field.getSearchPosition() > -1)) ||
                    ((sortOrder == ORDER_LIST) && (field.getListPosition() > -1))
                    ) {
                    orderedFields.add(field);
                }
            }
            org.mmbase.core.util.Fields.sort(orderedFields, sortOrder);

            return new BasicFieldList(orderedFields, this);
        }
    }

    public Field getField(String fieldName) throws NotFoundException {
        Field f = getFieldTypes().get(fieldName);
        if (f == null) throw new NotFoundException("Field '" + fieldName + "' does not exist in NodeManager '" + getName() + "'.(" + getFieldTypes() + ")");
        return f;
    }

    public String getGUIName() {
        return getGUIName(NodeManager.GUI_SINGULAR);
    }

    public String getGUIName(int plurality) {
        return getGUIName(plurality, null);
    }

    public String getGUIName(int plurality, Locale locale) {
        return getName();
    }

    public String getName() {
        return "virtual_manager";
    }
    public String getDescription() {
        return getDescription(null);
    }

    public String getDescription(Locale locale) {
        return "";
    }

    public NodeManager getParent() {
        return null;
    }


    public String getProperty(String name) {
        return getProperties().get(name);
    }
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }


    public NodeManagerList getDescendants() {
        NodeManagerList descendants = getCloud().createNodeManagerList();
        String name = getName();
        for (NodeManager nm  : getCloud().getNodeManagers()) {
            try {
                NodeManager parent = nm.getParent();
                if (parent != null && name.equals(parent.getName())) {
                    if (! descendants.contains(nm)) {
                        descendants.add(nm);
                        for (NodeManager sub : nm.getDescendants()) {
                            descendants.add(sub);
                        }
                    }
                }
            } catch (NotFoundException nfe) {
                // never mind, getParent may do that, it simply means that it is object or so.
            }
        }
        return descendants;
    }

    @Override
    public Collection< Function<?>>  getFunctions() {
        return Collections.emptyList();
    }


    @Override
    public String toString() {
        return getName() + " " +  getFieldTypes().keySet();
    }

}
