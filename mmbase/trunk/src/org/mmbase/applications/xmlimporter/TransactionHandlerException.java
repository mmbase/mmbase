/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.xmlimporter;

/**
 * Creates a new transactionHandler exception.
 *
 * @author Rob van Maris: Finalist IT Group
 * @since MMBase-1.5
 * @version $Id: TransactionHandlerException.java,v 1.3 2003-03-07 08:50:03 pierre Exp $
 */
public class TransactionHandlerException extends Exception {
        String code = "";
        String fieldId = "";
        String fieldOperator = "";
        String objectOperator = "";
        String objectId = "";
        String transactionOperator = "";
        String transactionId = "";
        String exceptionPage = "";

        /**
         * Creates a new transactionHandler exception.
         * @param s -  Text to serve as message in the exception.
         */
        public TransactionHandlerException(String s) {
                super(s);
        }
}

