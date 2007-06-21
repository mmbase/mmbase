/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
*/
package org.mmbase.security.implementation.aselect;

/**
 * This Exception class is wrapped around the standard Exception class
 * to make it more obvious that it was an ASelect Exception. 
 *
 * The class does not add any functionality
 * @version $Id: ASelectException.java,v 1.1 2006-01-31 19:53:06 azemskov Exp $
 */

public class ASelectException extends Exception {
    //javadoc is inherited
    public ASelectException() {
        super();
    }

    //javadoc is inherited
    public ASelectException(String message) {
        super(message);
    }

    //javadoc is inherited
    public ASelectException(Throwable cause) {
        super(cause);
    }

    //javadoc is inherited
    public ASelectException(String message, Throwable cause) {
        super(message, cause);
    }
}
