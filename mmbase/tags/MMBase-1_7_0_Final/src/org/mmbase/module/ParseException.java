/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import javax.servlet.*;

/**
 * This exception gets thrown when the user hasn't logged in yet.
 *
 * @author Wilbert Hengst
 * @version $Id: ParseException.java,v 1.3 2003-08-29 09:36:50 pierre Exp $
 */
public class ParseException extends ServletException {

    //javadoc is inherited
    public ParseException() {
        super();
    }

    //javadoc is inherited
    public ParseException(String message) {
        super(message);
    }

    //javadoc is inherited
    public ParseException(Throwable cause) {
        super(cause);
    }

    //javadoc is inherited
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
