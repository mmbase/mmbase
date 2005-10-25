/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.util.Casting;

/**
 *
 * @author Pierre van Rooden
 * @version $Id: NumberDataType.java,v 1.14 2005-10-25 18:33:21 michiel Exp $
 * @since MMBase-1.8
 */
abstract public class NumberDataType extends ComparableDataType {

    /**
     * Constructor for Number field.
     */
    public NumberDataType(String name, Class classType) {
        super(name, classType);
    }


    protected Object castToValidate(Object value, Node node, Field field) {
        if (value == null) return null;
        return new Double(Casting.toDouble(value)); // this makes it e.g. possible to report that 1e20 is too big for an integer.
    }

}
