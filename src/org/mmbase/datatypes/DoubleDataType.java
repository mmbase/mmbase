/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

/**
 * A NumberDataType, but provides getMin and getMax as double.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: DoubleDataType.java,v 1.4 2005-10-21 09:40:13 michiel Exp $
 * @since MMBase-1.8
 */

public class DoubleDataType extends NumberDataType {

    /**
     */
    public DoubleDataType(String name) {
        super(name, Double.class);
        setMin(new Double(Double.NEGATIVE_INFINITY), false);
        setMax(new Double(Double.POSITIVE_INFINITY), false);
    }

    /**
     * @return the minimum value as a <code>double</code>, or {@link Double#NEGATIVE_INFINITY} if there is no minimum.
     */
    public double getMin() {
        Number min = (Number) getMinRestriction().getValue();
        return min == null ? Double.NEGATIVE_INFINITY : min.doubleValue();
    }

    /**
     * @return the maximum value as a <code>double</code>, or {@link Double#POSITIVE_INFINITY} if there is no maximum.
     */
    public double getMax() {
        Number max = (Number) getMaxRestriction().getValue();
        return max == null ? Double.POSITIVE_INFINITY : max.doubleValue();
    }

}
