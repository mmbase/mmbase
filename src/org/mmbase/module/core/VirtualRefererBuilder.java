/**

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * VirtualBuilder is a builder which creates 'virtual' nodes.
 * This class is intended to facilitate practical creation of virtual
 * builders by capturing events that migth otherwise lead to unexpected or
 * faulty behavior.
 *
 * @author Pierre van Rooden
 * @version 11 May 2001
 */
public class VirtualRefererBuilder extends VirtualBuilder {

    // logging variable
    private static Logger log = Logging.getLoggerInstance(VirtualRefererBuilder.class.getName());

    private MMObjectBuilder originalBuilder=null;

    /**
     * Creates an instance of a Virtual builder.
     * A builder instantiated with this constrcutor is not registered in MMBase
     * and should only be used as a temporary parent for virtual nodes which
     * do not have a long life span.
     * @param m the MMbase cloud creating the node
     */
    public VirtualRefererBuilder(MMObjectBuilder originalBuilder) {
        super(originalBuilder.mmb);
        this.originalBuilder=originalBuilder;
        this.tableName="virtual_"+originalBuilder.getTableName();
        fields=new Hashtable(originalBuilder.fields);
    }

    /**
     * What should a GUI display for this node.
     * @param node The node to display
     * @return either the name field of the node or "no info"
     */
    public String getGUIIndicator(MMObjectNode node) {
        return originalBuilder.getGUIIndicator(node);
    }

    /**
     * Provides additional functionality when obtaining field values.
     * @param node the node who setfields are queried
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     */
    public Object getValue(MMObjectNode node,String field) {
        return originalBuilder.getValue(node,field);
    }

    /**
     * Returns the original builder
     * @param node the node who setfields are queried
     * @param field the fieldname that is requested
     * @return the result of the 'function', or null if no valid functions could be determined.
     */
    public Object getOriginalBuilder() {
        return originalBuilder;
    }
}
