/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.core.util;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.DataTypes;
import org.mmbase.core.CoreField;
import java.util.*;

/**

 * @since MMBase-1.8
 */
public class Fields {
    public final static int STATE_MINVALUE    = 0;
    public final static int STATE_MAXVALUE    = 3;
    private final static String[] STATES = {
        "unknown", "virtual", "unknown", "persistent", "system"
    };

    public final static int TYPE_MINVALUE    = 1;
    public final static int TYPE_MAXVALUE    = 12;
    private final static String[] TYPES = {
        "UNKNOWN", "STRING", "INTEGER", "UNKNOWN", "BINARY" /* BYTE */, "FLOAT", "DOUBLE", "LONG", "XML", "NODE", "DATETIME", "BOOLEAN", "LIST"
    };

    /**
     * Returns an instance of a CoreField based on the type.
     */
    public static CoreField createField(String name, int type) {
        DataType dataType = DataTypes.createDataType(null,type);
        CoreField field = new org.mmbase.module.corebuilders.FieldDefs(name, dataType);
        return field;
    }

    /**
     * Provide a description for the specified type.
     * Useful for debugging, errors or presenting GUI info.
     * @param type the type to get the description of
     * @return the description of the type.
     */
    public static String getTypeDescription(int type) {
       if (type < TYPE_MINVALUE || type > TYPE_MAXVALUE) {
            return TYPES[0];
       }

       return TYPES[type - TYPE_MINVALUE + 1];
    }

    /**
     * Provide a description for the specified state.
     * Useful for debugging, errors or presenting GUI info.
     * @param state the state to get the description of
     * @return the description of the state.
     */
    public static String getStateDescription(int state) {
       if (state < STATE_MINVALUE || state > STATE_MAXVALUE) {
            return STATES[0];
       }
       return STATES[state - STATE_MINVALUE + 1];
    }



    /**
     * Provide an id for the specified mmbase state description.
     * @param type the state description to get the id of
     * @return the id of the state.
     */
    public static int getState(String state) {
        if (state == null) return Field.STATE_UNKNOWN;
        state = state.toLowerCase();
        if (state.equals("persistent"))  return Field.STATE_PERSISTENT;
        if (state.equals("virtual"))     return Field.STATE_VIRTUAL;
        if (state.equals("system"))      return Field.STATE_SYSTEM;
        return Field.STATE_UNKNOWN;
    }

    /**
     * Provide an id for the specified mmbase type description
     * @param type the type description to get the id of
     * @return the id of the type.
     */
    public static int getType(String type) {
        if (type == null) return MMBaseType.TYPE_UNKNOWN;
        // XXX: deprecated VARCHAR
        type = type.toUpperCase();
        if (type.equals("VARCHAR")) return MMBaseType.TYPE_STRING;
        if (type.equals("STRING"))  return MMBaseType.TYPE_STRING;
        if (type.equals("XML"))     return MMBaseType.TYPE_XML;
        if (type.equals("INTEGER")) return MMBaseType.TYPE_INTEGER;
        if (type.equals("BYTE"))    return MMBaseType.TYPE_BINARY;
        if (type.equals("BINARY"))    return MMBaseType.TYPE_BINARY;
        if (type.equals("FLOAT"))   return MMBaseType.TYPE_FLOAT;
        if (type.equals("DOUBLE"))  return MMBaseType.TYPE_DOUBLE;
        if (type.equals("LONG"))    return MMBaseType.TYPE_LONG;
        if (type.equals("NODE"))    return MMBaseType.TYPE_NODE;
        if (type.equals("DATETIME"))return MMBaseType.TYPE_DATETIME;
        if (type.equals("BOOLEAN")) return MMBaseType.TYPE_BOOLEAN;
        if (type.startsWith("LIST"))    return MMBaseType.TYPE_LIST;
        return MMBaseType.TYPE_UNKNOWN;
    }

    public static void sort(List fields, int order) {
        Collections.sort(fields, new FieldComparator(order));
    }


    /**
     * Comparator to sort CoreFields by creation order, or by position
     * specified in one of the GUIPos fields.
     */
    private static class FieldComparator implements Comparator {

        private int order = NodeManager.ORDER_CREATE;

        /**
         * Constrcuts a comparator to sort fields on teh specifie dorder
         * @param order one of NodeManager.ORDER_CREATE, NodeManager.ORDER_EDIT, NodeManager.ORDER_LIST, NodeManager.ORDER_SEARCH
         */
        FieldComparator (int order) {
            this.order = order;
        }

        /**
         * Retrieve the postion of a CoreField object according to the order to sort on
         */
        private int getPos(CoreField o) {
            switch (order) {
            case NodeManager.ORDER_EDIT: {
                return o.getEditPosition();
            }
            case NodeManager.ORDER_LIST: {
                return o.getListPosition();
            }
            case NodeManager.ORDER_SEARCH: {
                return o.getSearchPosition();
            }
            default : {
                return o.getStoragePosition();
            }
            }
        }

        /**
         * Compare two objects (should be CoreFields)
         */
        public int compare(Object o1, Object o2) {
            int pos1 = getPos((CoreField)o1);
            int pos2 = getPos((CoreField)o2);

            if (pos1 < pos2) {
                return -1;
            } else if (pos1 > pos2) {
                return 1;
            } else {
                return 0;
            }
        }
    }



}
