/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.cloudcontext.builders;

import org.mmbase.security.implementation.cloudcontext.*;
import org.mmbase.module.core.*;
import org.mmbase.security.*;
import java.util.*;
import org.mmbase.cache.Cache;
import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The rightsrel relation, connects a 'context' with a 'group'. The
 * 'operation' field then indicates which operation is allowed because
 * of this relation.
 *
 * @author Eduard Witteveen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: RightsRel.java,v 1.6 2003-08-13 10:39:18 michiel Exp $
 */
public class RightsRel extends InsRel {


    public boolean init() {
        mmb.addLocalObserver(getTableName(), CacheInvalidator.getInstance());
        mmb.addRemoteObserver(getTableName(), CacheInvalidator.getInstance());
        return super.init();
    }


    /**
     * The field of this relations which present the operation.
     */
    public static String OPERATION_FIELD = "operation";

    private static Logger log = Logging.getLoggerInstance(RightsRel.class);

    /**
     * Util method to get this Builder.
     *
     * @return The RightsRel MMObjectBuilder
     */
    public static RightsRel getBuilder() {
        return (RightsRel) MMBase.getMMBase().getBuilder("rightsrel");
    }

    // inherited
    public String getGUIIndicator(MMObjectNode node) {
        return node.getStringValue(OPERATION_FIELD) + " " + super.getGUIIndicator(node);
    }

    /**
     * Operation defaults to 'read'.
     */
    public void setDefaults(MMObjectNode node) {
        // default -> read
        node.setValue(OPERATION_FIELD, Operation.READ.toString());
        super.setDefaults(node);
    }



    MMObjectNode getNewNode(String owner, int snumber, int dnumber, Operation operation) {
        MMObjectNode rel = getNewNode(owner);
        rel.setValue("snumber", snumber);
        rel.setValue("dnumber", dnumber);
        rel.setValue("rnumber", mmb.getRelDef().getNumberByName("grants"));
        rel.setValue("operation", operation.toString());
        return rel;
    }

    /**
     * Check on possible values for operation.
     */
    public boolean setValue(MMObjectNode node, String fieldName) {
        // most situations, handle in inherited class
        if (!fieldName.equals(OPERATION_FIELD)) super.setValue(node, fieldName);

        // mm: not sure I like this.
        String value = (String) node.values.get(OPERATION_FIELD);
        if (value == null)        return true;
        if (value.equals("all"))  return true;
        if (value.equals(Operation.READ.toString())) return true;
        if (value.equals(Operation.WRITE.toString())) return true;
        if (value.equals(Operation.CREATE.toString())) return true;
        if (value.equals(Operation.CHANGE_RELATION.toString())) return true;
        if (value.equals(Operation.DELETE.toString())) return true;
        if (value.equals(Operation.CHANGECONTEXT.toString())) return true;
        String msg = 
            "field with name operation must contain a valid opertion( value was: '" + value + "')\n" +
            "valid operations are: all, " + Operation.READ + ", " + Operation.WRITE + ", " + Operation.CREATE +
            ", " + Operation.CHANGE_RELATION + ", " + Operation.DELETE + ", " + Operation.CHANGECONTEXT + ", ";
        throw new RuntimeException(msg);
    }
}
