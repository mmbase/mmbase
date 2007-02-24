/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search;

import java.util.*;
import org.mmbase.bridge.Field;
import org.mmbase.module.core.*;

/**
 * A <code>ResultBuilder</code> is a builder for
 * {@link ResultNode ResultNodes}, that represent the results of executing
 * an arbitrary search query.
 * <p>
 * This builder contains info on the fields of the resultnodes.
 *
 * @author  Rob van Maris
 * @version $Id: ResultBuilder.java,v 1.9 2007-02-24 21:57:50 nklasens Exp $
 * @since MMBase-1.7
 */
public class ResultBuilder extends VirtualBuilder {

    /**
     * Creator.
     * Creates new <code>ResultBuilder</code> instance, used to represent
     * the results of executing a search query.
     *
     * @param mmbase MMBase instance.
     * @param query The search query that defines the search.
     */
    public ResultBuilder(MMBase mmbase, SearchQuery query) {
        super(mmbase);

        // Create fieldsByAlias map.
        List<StepField> queryFields = query.getFields();
        Iterator<StepField> i = queryFields.iterator();
        while (i.hasNext()) {
            StepField field = i.next();
            String fieldAlias = field.getAlias();
            if (fieldAlias == null) {
                fieldAlias = field.getFieldName();
            }
            fields.put(fieldAlias, org.mmbase.core.util.Fields.createField(fieldAlias, field.getType(), -1, Field.STATE_VIRTUAL, null));;
        }
    }

    // javadoc is inherited
    public MMObjectNode getNewNode(String owner) {
        return new ResultNode(this);
    }

}
