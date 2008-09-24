/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

/**
 * Element in a {@link Queue}.
 *
 * @author vpro
 * @version $Id: QueueElement.java,v 1.5 2004-09-30 14:07:12 pierre Exp $
 */
public class QueueElement {

    /**
     * The actual object stored in the queue
     */
    public Object obj;
    /**
     * The next element in the queue (null indicates no next element).
     */
    public QueueElement next;
}
