/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge;

/**
 * This exception gets thrown when something goes wrong on the MMCI.
 *
 * @author Pierre van Rooden
 * @version $Id: BridgeException.java,v 1.14 2003-08-29 09:36:50 pierre Exp $
 */
public class BridgeException extends RuntimeException {

    //javadoc is inherited
    public BridgeException() {
        super();
    }

    //javadoc is inherited
    public BridgeException(String message) {
        super(message);
    }

    //javadoc is inherited
    public BridgeException(Throwable cause) {
        super(cause);
    }

    //javadoc is inherited
    public BridgeException(String message, Throwable cause) {
        super(message,cause);
    }

}
