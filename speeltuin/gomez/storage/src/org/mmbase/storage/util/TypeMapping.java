/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.util;

import org.mmbase.storage.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The TypeMapping class helps translating MMBase types to storage-specific type descriptions.
 * Examples of type mappings are mappings that convert to database field types.
 * I.e., a STRING with size 0-255 could be configured to translate to 'varchar(${0})', '{0}, in this case,.
 * being the size of the actual field.
 * <br />
 * TypeMapping is a comparable class, which allows it to be used in a sorted map, set or list.
 * However, Typemapping needs fuzzy matching so it is easy to locate the appropriate type-mapping for a field.
 * As such, it's natural ordering is NOT consistent with equals. A typemapping may be considered 'equal' while still having 
 * a different ordering position in the class.
 * This allows for an easy search on a sorted list of TypeMappings: By using 'IndexOf' you can quickly find a TypeMapping in a list,
 * even if the min or max sizes for the type do not completely match.
 * You typically use this if you searxh for a TypeMapping whose size you set with 'getSize()', rather than setting a specific range
 * (using the minSize/maxSize properties).
 *
 * @author Pierre van Rooden
 * @version $Id: TypeMapping.java,v 1.2 2003-07-24 10:11:04 pierre Exp $
 */
public class TypeMapping implements Comparable {

    // logger
    private static Logger log = Logging.getLoggerInstance(TypeMapping.class);

    /**
     * The expression this type should translate to.
     * You can access this property directly, but you can use {@link getType()} to obtain an expanded expression. 
     */
    public String type;
    /**
     * The name of the MMBase type to map
     */
    public String name;
    /**
     * The minimum size of the MMBase type to map. 
     * -1 indicates no mimimum.
     */
    public int minSize;
    /**
     * The maximum size of the MMBase type to map
     * -1 indicates no maximum.
     */
    public int maxSize;

    /**
     * Constructor
     */
    public TypeMapping() {
    }

    /**
     * Sets a fixed size for this TypeMapping.
     * Effectively, this sets the minimimum and maximum size of the type mapping to the specified value, ensuring this TypeMapping object
     * is equal to all TypeMappings whos minimum size is equal to or smaller than the size, and teh maximum size is equal to or greater 
     * that this size. 
     * @param size the size to set
     */
    public void setFixedSize(int size) {
        minSize = minSize;
        maxSize = maxSize;
    }

    public int compareTo(Object o) {
        TypeMapping t = (TypeMapping) o;
        if (!name.equals(t.name)) {
            return name.compareTo(t.name);
        } else if (minSize != t.minSize) {
            return minSize - t.minSize;
        } else if (t.maxSize == -1) {
            if (maxSize == -1) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return maxSize - t.maxSize;
        }
    }

    public boolean equals(Object o) {
        return o instanceof TypeMapping &&
               name.equals(((TypeMapping)o).name) &&
               (( minSize >= ((TypeMapping)o).minSize &&
                  (maxSize <= ((TypeMapping)o).maxSize || (((TypeMapping)o).maxSize == -1)) ) ||
                ( ((TypeMapping)o).minSize >= minSize &&
                  (((TypeMapping)o).maxSize <= maxSize || (maxSize == -1)) ));
    }

}
