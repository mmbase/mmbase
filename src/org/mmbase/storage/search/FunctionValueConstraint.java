/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search;

import org.mmbase.storage.search.FieldCompareConstraint;

/**
 *
 * @author Marcel Maatkamp
 * @version $Id: FunctionValueConstraint.java,v 1.3 2007-06-12 12:32:47 michiel Exp $
 * @since MMBase-1.8.5
 */
public interface FunctionValueConstraint extends FieldCompareConstraint {
	
    public String getFunction(); 
	
    /**
     * Gets the value to compare with. 
     * Depending on the field type, the value is of type 
     * <code>String</code> or <code>Number</code>.
     * <p>
     * If the associated field type is of string type, when used in 
     * combination with the operator <code>LIKE</code>, this may contain the 
     * following wildcard characters as well:
     * <ul>
     * <li>% for any string
     * <li>_ for a single character
     * </ul>
     */
    Object getValue();

    /**
     * Returns a string representation of this FunctionValueConstraint. 
     * The string representation has the form 
     * "FunctionValueConstraint(inverse:&lt:inverse&gt;, field:&lt;field&gt;, 
     *  casesensitive:&lt;casesensitive&gt;, operator:&lt;operator&gt;,
     *  value:&lt;value&gt;)"
     * where 
     * <ul>
     * <li><em>&lt;inverse&gt;</em>is the value returned by
     *      {@link #isInverse isInverse()}
     * <li><em>&lt;field&gt;</em> is the field alias returned by 
     *     <code>FieldConstraint#getField().getAlias()</code>, or
     *     <code>FieldConstraint#getField().getFieldName()</code>
     *      when the former is <code>null</code>.
     * <li><em>&lt;casesensitive&gt;</em> is the value returned by
     *     {@link FieldConstraint#isCaseSensitive isCaseSensitive()}
     * <li><em>&lt;operator&gt;</em> is the value returned by
     *     (@link FieldCompareConstraint#getOperator getOperator()}
     * <li><em>&lt;value&gt;</em> is the value returned by
     *     {@link #getValue getValue()}
     * </ul>
     *
     * @return A string representation of this FunctionValueConstraint.
     */
    public String toString();

}
