/**

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

/**
 * @javadoc
 *
 * @author Pierre van Rooden
 * @version $Id$
 * @since MMBase-1.7
 */
public class VirtualReferrerBuilder extends VirtualBuilder {

    private MMObjectBuilder originalBuilder = null;

    /**
     * Creates an instance of a Virtual builder.
     * A builder instantiated with this constrcutor is not registered in MMBase
     * and should only be used as a temporary parent for virtual nodes which
     * do not have a long life span.
     */
    public VirtualReferrerBuilder(MMObjectBuilder originalBuilder) {
        super(originalBuilder.mmb);
        this.originalBuilder = originalBuilder;
        this.tableName = "virtual_" + originalBuilder.getTableName();
        fields.clear();
        fields.putAll(originalBuilder.fields);
    }

    /**
     * What should a GUI display for this node.
     * @param node The node to display
     * @return either the name field of the node or "no info"
     */
    @Override
    public String getGUIIndicator(MMObjectNode node) {
        return originalBuilder.getGUIIndicator(node);
    }

    /**
     * Provides additional functionality when obtaining field values.
     * @param node the node who setfields are queried
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     */
    @Override
    public Object getValue(MMObjectNode node, String field) {
        return originalBuilder.getValue(node, field);
    }

    /**
     * Returns the original builder
     */
    public Object getOriginalBuilder() {
        return originalBuilder;
    }
}
