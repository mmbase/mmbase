/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import org.mmbase.bridge.*;
import org.mmbase.util.Casting;

/**
 * A DataType representing some kind of numeric value, like a floating point number or an integer number.
 *
 * @author Pierre van Rooden
 * @version $Id: NumberDataType.java,v 1.24 2007-02-25 18:12:16 nklasens Exp $
 * @since MMBase-1.8
 */
abstract public class NumberDataType<E extends Number&Comparable<E>> extends ComparableDataType<E> {

    private static final long serialVersionUID = 1L;
    /**
     * Constructor for Number field.
     */
    public NumberDataType(String name, Class<E> classType) {
        super(name, classType);
    }



    /**
     * @since MMBase-1.9
     */
    protected Number castString(Object preCast) throws CastException {
        if (preCast instanceof String) {
            if (! StringDataType.DOUBLE_PATTERN.matcher((String) preCast).matches()) {
                throw new CastException("Not a number: " + preCast);
            }
        }
        return Casting.toDouble(preCast); // this makes it e.g. possible to report that 1e20 is too big for an integer.
    }


    protected Object castToValidate(Object value, Node node, Field field) throws CastException {
        if (value == null) return null;
        Object preCast = preCast(value, node, field); // resolves enumerations
        return castString(preCast);

    }
}
