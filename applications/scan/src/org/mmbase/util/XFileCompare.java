/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

/**
 * Class to compare XFile object depending on their comparefield.
 * @see SortedVector
 * @see CompareInterface
 * @see XFile
 *
 * @author David V van Zeventer
 * @version 16 November 1998
 */
public class XFileCompare implements CompareInterface {

    /**
     * Attribute to compare on ('filepath' or 'modtime')
     */
    String compareField;

    /**
     * Create a XFuileCompare.
     * @param compareField Attribute to compare on ('filepath' or 'modtime')
     */
    public XFileCompare(String compareField) {
        this.compareField = compareField;
    }

    /**
     * Make the comparison.
     * The result is a negative value if the comparefield of the first file is 'smaller' than the second,
     * a positive value if it is 'larger', and 0 if they are 'equal'.
     * @param thisOne the first object to compare. should be a <code>XFile</code>.
     * @param other the second object to compare. should be a <code>XFile</code>.
     * @return the result of the comparison
     */
    public int compare(Object thisOne, Object other) {
        int result = 0;
        long longresult=0;
        XFile xfileobj1 = (XFile)thisOne;
        XFile xfileobj2 = (XFile)other;

        if (compareField.equals("filepath")) {       //Compare objects using their filepathname.
            result = (xfileobj1.getFilePath()).compareTo(xfileobj2.getFilePath());
            //log.debug("XFileCompare: Filepath compare result= "+result);

        } else {
            if (compareField.equals("modtime")) {   //Compare objects using their modification time.
                longresult =  xfileobj1.getModTime() - xfileobj2.getModTime();
                result = (int)(longresult/1000);
                //log.debug("XFileCompare: Modificationtime compare result= "+result);
            }
        }
        return result;
    }
}
