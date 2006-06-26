/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.implementation;

import javax.servlet.*;
import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.datatypes.*;
import org.mmbase.core.CoreField;
import org.mmbase.core.util.Fields;
import org.mmbase.module.core.*;
import org.mmbase.storage.search.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.LocalizedString;

/**
 * This class represents a virtual node type information object.
 * It has the same functionality as BasicNodeType, but it's nodes are vitrtual - that is,
 * constructed based on the results of a search over multiple node managers.
 * As such, it is not possible to search on this node type, nor to create new nodes.
 * It's sole function is to provide a type definition for the results of a search.
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @version $Id: VirtualNodeManager.java,v 1.43 2006-06-26 09:47:50 michiel Exp $
 */
public class VirtualNodeManager extends AbstractNodeManager implements NodeManager {
    private static final  Logger log = Logging.getLoggerInstance(VirtualNodeManager.class);

    private static final boolean allowNonQueriedFields = true; // not yet configurable

    // field types
    final protected Map fieldTypes = new HashMap();

    final MMObjectBuilder builder;
    private SearchQuery query;

    /**
     * Instantiated a Virtual NodeManager, and tries its best to find reasonable values for the field-types.
     */
    VirtualNodeManager(org.mmbase.module.core.VirtualNode node, Cloud cloud) {
        super(cloud);
        // determine fields and field types
        if (node.getBuilder() instanceof VirtualBuilder) {
            VirtualBuilder virtualBuilder = (VirtualBuilder) node.getBuilder();;
            Map fields = virtualBuilder.getFields(node);
            Iterator i = fields.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String fieldName = (String) entry.getKey();
                CoreField fd = (CoreField) entry.getValue();
                Field ft = new BasicField(fd, this);
                fieldTypes.put(fieldName, ft);
            }
            builder = null;
            setStringValue("name", "virtual builder");
            setStringValue("description", "virtual builder");
        } else {
            builder = node.getBuilder();
            BasicNodeManager.sync(builder, fieldTypes, this);
        }
    }

    /**
     * @since MMBase-1.8
     */
    VirtualNodeManager(Query query, Cloud cloud) {
        super(cloud);
        if (query instanceof NodeQuery) {
            builder = BasicCloudContext.mmb.getBuilder(((NodeQuery) query).getNodeManager().getName());
            BasicNodeManager.sync(builder, fieldTypes, this);
        } else {
            builder = null;
            if (log.isDebugEnabled()) {
                log.debug("Creating NodeManager for " + query.toSql());
            }
            // fieldTypes map will be filled 'lazily' on first call to getFieldTypes.
            this.query = query; // query instanceof BasicQuery ? ((BasicQuery ) query).getQuery() : query;
            setStringValue("name", "cluster builder");
            setStringValue("description", "cluster builder");
        }

    }

    /**

    /**
     * Returns the fieldlist of this nodemanager after making sure the manager is synced with the builder.
     * @since MMBase-1.8
     */
    protected Map getFieldTypes() {
        if (builder == null) {
            return fieldTypes;
        } else {
            if (query != null) { // means not yet called (lazy loading of fields)
                // code to solve the fields.
                Iterator steps = query.getSteps().iterator();
                while (steps.hasNext()) {
                    Step step = (Step) steps.next();
                    DataType nodeType  = DataTypes.getDataType("node");
                    String name = step.getAlias();
                    if (name == null) name = step.getTableName();
                    CoreField fd = Fields.createField(name, Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_VIRTUAL, nodeType);
                    fd.finish();
                    Field ft = new VirtualNodeManagerField(fd, name);
                    fieldTypes.put(name, ft);

                    if (allowNonQueriedFields && ! query.isAggregating()) {
                        /// if hasField returns true also for unqueried fields
                        FieldIterator fields = cloud.getNodeManager(step.getTableName()).getFields().fieldIterator();
                        while (fields.hasNext()) {
                            Field f = fields.nextField();
                            final String fieldName = name + "." + f.getName();
                            fieldTypes.put(fieldName, new VirtualNodeManagerField(f, fieldName));
                        }
                    }
                }
                if (! allowNonQueriedFields || query.isAggregating()) {
                    //hasField only returns true for queried fields
                    Iterator fields = query.getFields().iterator();
                    while(fields.hasNext()) {
                        StepField field = (StepField) fields.next();
                        Step step = field.getStep();
                        Field f = cloud.getNodeManager(step.getTableName()).getField(field.getFieldName());
                        String name = field.getAlias();
                        if (name == null) {
                            name = step.getAlias();
                            if (name == null) name = step.getTableName();
                            name += "." + field.getFieldName();
                        }
                        final String fieldName = name;
                        fieldTypes.put(name, new VirtualNodeManagerField(f, fieldName));

                    }
                }
                query = null;
            }
            return fieldTypes;
        }
    }



    public String getGUIName(int plurality, Locale locale) {
        if (locale == null) locale = cloud.getLocale();
        if (builder != null) {
            if (plurality == NodeManager.GUI_SINGULAR) {
                return builder.getSingularName(locale.getLanguage());
            } else {
                return builder.getPluralName(locale.getLanguage());
            }
        } else {
            return getName();
        }
    }

    public String getName() {
        return builder == null ? getStringValue("name") : builder.getTableName();
    }
    public String getDescription() {
        return getDescription(null);
    }

    public String getDescription(Locale locale) {
        if (builder == null) return getStringValue("description");
        if (locale == null) locale = cloud.getLocale();
        return builder.getDescription(locale.getLanguage());
    }

    /**
     * @todo may be moved to org.mmbase.bridge.util.FieldWrapper
     */
    private class VirtualNodeManagerField implements Field {

        private final Field field;
        private final String name;
        VirtualNodeManagerField(Field field, String name)  {
            this.field = field;
            this.name = name;
        }
        public NodeManager getNodeManager() {
            return VirtualNodeManager.this;
        }

        public int getState() {
            return Field.STATE_VIRTUAL;
        }

        public DataType getDataType() {
            return field.getDataType();
        }
        public boolean isUnique() {
            return field.isUnique();
        }

        public boolean hasIndex() {
            return field.hasIndex();
        }
        public int getType() {
            return field.getType();
        }
        public int getListItemType() {
            return field.getListItemType();
        }
        public int getSearchPosition() {
            return field.getSearchPosition();
        }
        public int getListPosition() {
            return field.getListPosition();
        }
        public int getEditPosition() {
            return field.getEditPosition();
        }
        public int getStoragePosition() {
            return field.getStoragePosition();
        }
        public String getGUIType() {
            return field.getGUIType();
        }
        public boolean isRequired() {
            return field.isRequired();
        }

        public int getMaxLength() {
            return field.getMaxLength();
        }
        public java.util.Collection validate(Object value) {
            return field.validate(value);
        }
        public boolean isVirtual() {
            return true;
        }
        public boolean isReadOnly() {
            return true;
        }
        public String getName() {
            return name;
        }
        public String getGUIName() {
            return field.getGUIName();
        }

        public String getGUIName(Locale locale) {
            return field.getGUIName(locale);
        }

        public LocalizedString getLocalizedGUIName() {
            return field.getLocalizedGUIName();
        }

        public void setGUIName(String g, Locale locale) {
            throw new UnsupportedOperationException();
        }
        public void setGUIName(String g) {
            throw new UnsupportedOperationException();
        }
        public LocalizedString getLocalizedDescription() {
            return field.getLocalizedDescription();
        }

        public String getDescription(Locale locale) {
            return field.getDescription(locale);
        }
        public String getDescription() {
            return field.getDescription();
        }

        public void setDescription(String description, Locale locale) {
            throw new UnsupportedOperationException();
        }

        public void setDescription(String description) {
            throw new UnsupportedOperationException();
        }

    }

}
