/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import org.mmbase.bridge.Field;
import org.mmbase.datatypes.*;
import org.mmbase.core.CoreField;
import org.mmbase.core.util.Fields;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * VirtualBuilder is a builder which creates 'virtual' nodes.
 * This class is intended to facilitate practical creation of virtual
 * builders by capturing events that migth otherwise lead to unexpected or
 * faulty behavior.
 *
 * @author Pierre van Rooden
 * @version $Id$
 */
public class VirtualBuilder extends MMObjectBuilder {
    private static final Logger log = Logging.getLoggerInstance(VirtualBuilder.class);

    private static int counter = 0;
    /**
     * Creates an instance of a Virtual builder.
     * A builder instantiated with this constrcutor is not registered in MMBase
     * and should only be used as a temporary parent for virtual nodes which
     * do not have a long life span.
     * @param m the MMbase cloud creating the node
     */
    public VirtualBuilder(MMBase m) {
        this.mmb = m;
        this.tableName = "virtualnodes_" + counter++;
        this.description = "";
        virtual = true;
    }

    /**
     * Creates an instance of a Virtual builder and registers it in MMBase.
     * @param m the MMbase cloud creating the node
     * @param tableName the name of the builder as known in the MMbase system
     */
    protected VirtualBuilder(MMBase m, String tableName) {
        this.mmb = m;
        this.tableName = tableName;
        this.description = "";
        virtual = true;
        if (m.addBuilder(tableName, this) != null) {
            log.debug("Replaced virtual builder '" + tableName + "'");
        } else {
            log.debug("Created virtual builder '" + tableName + "'");
        }
    }

    /**
     * Initializes this builder.
     * No specifici cation is performed.
     * This method overrides the default emthod in MMObhjectBuilder, which
     * would otherwise attempt to access the database.
     * @return Always true.
     * @see #create
     */
    public boolean init() {
        return true;
    }

    /**
     * Creates a new builder table in the current database.
     * This method does not perform any action in a virtual builder, as there is
     * no actual table associated with it.
     */
    @Override
    public boolean create() {
        return true;
    }

    /**
     * Insert a new object (content provided) in the cloud, including an entry for the object alias (if provided).
     * This method does not perform any action in a virtual builder.
     * @param owner The administrator creating the node
     * @param node The object to insert
     * @return -1 (the insert failed)
     */
    @Override
    public int insert(String owner, MMObjectNode node) {
        // no insert allowed on this builder, so signal -1
        return -1;
    }

    /**
     * Get a new node, using this builder as its parent.
     * The new node is a virtual node.
     * @param owner The administrator creating the new node.
     * @return A newly initialized <code>VirtualNode</code>.
     */
    @Override
    public MMObjectNode getNewNode(String owner) {
        VirtualNode node = new VirtualNode(this);
        node.setValue("number",-1);
        node.setValue("owner",owner);
        node.setValue("otype",oType);
        setDefaults(node);
        return node;
    }

   /**
     * {@inheritDoc}
     * The default behavior of a virtual node is to display the content of
     * the 'name' field (if present).
     * XXX: should be changed to something better
     * @param node The node to display
     * @return either the name field of the node or "no info"
     */
     @Override
     public String getGUIIndicator(MMObjectNode node) {
        String s= node.getStringValue("name");
        if (s != null) {
            return s;
        } else {
            return GUI_INDICATOR;
        }
    }

    /**
     * Return a field's database state.
     * The default behavior for a virtual node is to return <code>DBSTATE_VIRTUAL</code>.
     * @param fieldName the requested field's name
     * @return <code>DBSTATE_VIRTUAL</code>
     */
    @Override
    public int getDBState(String fieldName) {
        return Field.STATE_VIRTUAL;
    }

    /**
     * {@inheritDoc}
     * Since virtual builders are generally not associated with a database,
     * this method returns null.
     * @param fieldName name of the field
     * @param node
     * @return <code>null</code>
     */
    @Override
    protected String getShortedText(String fieldName, MMObjectNode node) {
        return null;
    }


    /**
     * {@inheritDoc}
     * Since virtual builders are generally not associated with a database,
     * this method returns null.
     * @param fieldName name of the field
     * @param node
     * @return <code>null</code>
     */
    @Override
    protected byte[] getShortedByte(String fieldName, MMObjectNode node) {
        return null;
    }


    /**
     * Get text from a blob field from a database.
     * @since MMBase-1.8
     */
    public Map<String, CoreField> getFields(MMObjectNode node) {
        Map<String, CoreField> res = new HashMap<String, CoreField>();
        // determine fields and field types
        Map<String, Object> values = node.getValues();
        synchronized(values) {
            Iterator<Map.Entry<String, Object>> i = values.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, Object> entry = i.next();
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                if (value == null) value = new Object();
                DataType<? extends Object> fieldDataType = DataTypes.createDataType("field", value.getClass());
                int type = Fields.classToType(value.getClass());
                CoreField fd = Fields.createField(fieldName, type, Field.TYPE_UNKNOWN, Field.STATE_VIRTUAL, fieldDataType);
                fd.finish();
                res.put(fieldName, fd);
            }
        }
        return res;
    }
}
