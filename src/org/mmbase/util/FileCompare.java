/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;
import java.io.File;

/**
 * Class to compare two Files on their modification time, used by SortedVector.
 * @see SortedVector
 * @see CompareInterface
 *
 * @author David V van Zeventer
 * @version $Id: FileCompare.java,v 1.7 2004-05-03 11:25:27 michiel Exp $
 * @todo   Should be named FileLastModifiedComparator and implement java.util.Comparator
 */
public class FileCompare implements CompareInterface {

    /**
     * Make the comparison.
     * The result is a negative value if the time of the first file is 'smaller' than the second,
     * a positive value if it is 'larger', and 0 if both times are 'equal'.
     * @param thisOne the first object to compare. should be a <code>File</code>.
     * @param other the second object to compare. should be a <code>File</code>.
     * @return the result of the comparison
     */
    public int compare(Object thisone,Object other) {
        return (int) (((File)thisone).lastModified()-((File)other).lastModified());
    }
}
