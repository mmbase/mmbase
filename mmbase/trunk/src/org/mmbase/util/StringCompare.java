/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

/**
 * Class to compare two strings, used by SortedVector.
 * @see org.mmbase.util.SortedVector
 * @see org.mmbase.util.CompareInterface
 *
 * @author Rico Jansen
 * @version $Id: StringCompare.java,v 1.6 2004-05-03 11:25:26 michiel Exp $
 * @deprecated Should implement java.util.Comparator, or should not exist, because this is java.lang.String's 'natural' order.
 */
public class StringCompare implements CompareInterface {

    /**
     * The compare function called by SortedVector to sort things
     * @see org.mmbase.util.SortedVector
     * @see org.mmbase.util.CompareInterface
     */
    public int compare(Object thisone,Object other) {
        return(((String)thisone).compareTo((String)other));
    }
}
