/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.List;
import java.util.Vector;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Provides ErrorHandler methods for checking xml files by Config module
 *
 * @application Config
 * @author cjr@dds.nl
 * @version $Id: XMLCheckErrorHandler.java,v 1.5 2004-10-01 08:41:10 pierre Exp $
 */
public class XMLCheckErrorHandler implements ErrorHandler {

    private static Logger log = Logging.getLoggerInstance(XMLCheckErrorHandler.class.getName());

    private List warninglist,errorlist,fatallist,resultlist;

    public XMLCheckErrorHandler() {
        log.debug("New xmlcheckerrorhandler");
        warninglist = new Vector();
        errorlist   = new Vector();
        fatallist   = new Vector();
        resultlist  = new Vector();
    }

    public void warning(SAXParseException ex) throws SAXException {
        log.debug("warn");
        ErrorStruct err = new ErrorStruct("warning", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage());
        warninglist.add(err);
        resultlist.add(err);

    }

    public void error(SAXParseException ex) throws SAXException{
        log.debug("error");
        ErrorStruct err = new ErrorStruct("error", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage());
        errorlist.add(err);
        resultlist.add(err);
    }

    public void fatalError(SAXParseException ex) throws SAXException {
        log.debug("fatalError");
        ErrorStruct err = new ErrorStruct("fatal error", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage());
        fatallist.add(err);
        resultlist.add(err);
    }

    public List getWarningList() {
        return warninglist;
    }

    public List getErrorList() {
        return errorlist;
    }

    public List getFatalList() {
        return fatallist;
    }

    public List getResultList() {
        return resultlist;
    }
}

