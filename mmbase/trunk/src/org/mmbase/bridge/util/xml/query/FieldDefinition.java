/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.xml.query;

import java.util.Collection;
import org.w3c.dom.*;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;

/**
 * Defines options for a field to index.
 *
 * @author Pierre van Rooden
 * @version $Id: FieldDefinition.java,v 1.4 2005-11-01 18:01:44 michiel Exp $
 * @since MMBase-1.8
 **/
public class FieldDefinition {

    /**
     * Name of the field
     * XXX: How is this different from stepField.getFieldName() ?
     */
    public String fieldName = null;

    /**
     * Reference to (a) definition of this field in the query.
     */
    public StepField stepField = null;

    /**
     * The query definition belonging to this field
     */
    protected QueryDefinition queryDefinition = null;

    /**
     * The Query configurer that instantiated this definition
     * XXX: unused (logically, because it is instantiated by it)
     */
    protected QueryConfigurer configurer = null;

    /**
     * Constructor
     */
    public FieldDefinition(QueryConfigurer configurer, QueryDefinition queryDefinition) {
        this.configurer = configurer;
        this.queryDefinition = queryDefinition;
    }

    /**
     * Configures the field definition, using data from a DOM element
     */
    public void configure(Element fieldElement) {
    }
}

