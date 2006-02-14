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

/**
 * This class represents a virtual node type information object.
 * It has the same functionality as BasicNodeType, but it's nodes are vitrtual - that is,
 * constructed based on the results of a search over multiple node managers.
 * As such, it is not possible to search on this node type, nor to create new nodes.
 * It's sole function is to provide a type definition for the results of a search.
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @version $Id: VirtualNodeManager.java,v 1.39 2006-02-14 22:34:55 michiel Exp $
 */
public class VirtualNodeManager extends AbstractNodeManager implements NodeManager {
    private static final  Logger log = Logging.getLoggerInstance(AbstractNodeManager.class);

    private static final boolean allowNonQueriedFields = true; // not yet configurable

    // field types
    final protected Map fieldTypes = new HashMap();

    final MMObjectBuilder builder;

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
        }
    }

    /**
     * @since MMBase-1.8
     */
    VirtualNodeManager(Query query, Cloud cloud) {
        super(cloud);
        if (query instanceof NodeQuery) {
            builder = BasicCloudContext.mmb.getBuilder(((NodeQuery) query).getNodeManager().getName());
        } else {
            builder = null;
            if (log.isDebugEnabled()) {
                log.debug("Creating NodeManager for " + query.toSql());
            }
            // code to solve the fields.
            Iterator steps = query.getSteps().iterator();
            while (steps.hasNext()) {
                Step step = (Step) steps.next();
                DataType nodeType  = DataTypes.getDataType("node");
                String name = step.getAlias();
                if (name == null) name = step.getTableName();
                CoreField fd = Fields.createField(name, Field.TYPE_NODE, Field.TYPE_UNKNOWN, Field.STATE_VIRTUAL, nodeType);
                fd.finish();
                Field ft = new BasicField(fd, this);
                fieldTypes.put(name, ft);

                if (allowNonQueriedFields && ! query.isAggregating()) {
                    /// if hasField returns true also for unqueried fields
                    Iterator fields = cloud.getNodeManager(step.getTableName()).getFields().iterator();
                    while (fields.hasNext()) {
                        Field f = (Field) fields.next();
                        final String fieldName = name + "." + f.getName();
                        log.info("adding field " + fieldName);
                        fieldTypes.put(fieldName, new BasicField(((BasicField)f).coreField , this)  { // XXX casting is wrong!!, but I don't have other solution right now
                                public String getName() {
                                    return fieldName;
                                }
                            });
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
                    fieldTypes.put(name, new BasicField(((BasicField)f).coreField , this)  { // XXX casting is wrong!!, but I don't have other solution right now
                            public String getName() {
                                return fieldName;
                            }
                        });

                }
            }
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
        return fieldTypes;
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

}
