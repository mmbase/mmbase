/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import javax.servlet.ServletException;

/**
 * This exception gets thrown when the user hasn't logged in yet.
 *
 * @author vpro
 * @version $Id: NotLoggedInException.java,v 1.6 2003-08-29 09:36:55 pierre Exp $
 */
public class NotLoggedInException extends ServletException {

    //javadoc is inherited
    public NotLoggedInException() {
        super();
    }

    //javadoc is inherited
    public NotLoggedInException(String message) {
        super(message);
    }

    //javadoc is inherited
    public NotLoggedInException(Throwable cause) {
        super(cause);
    }

    //javadoc is inherited
    public NotLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }

}
