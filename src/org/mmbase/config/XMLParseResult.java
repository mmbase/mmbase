/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.config;

import java.util.*;

import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;

import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
				    
/**
 * Class XMLParseResult
 * 
 * @javadoc
 * @duplicate Same code appears in module/Config
 */

class XMLParseResult {

    private static Logger log = Logging.getLoggerInstance(XMLParseResult.class.getName()); 
    
    List warningList, errorList, fatalList,resultList;
    boolean hasDTD;
    String dtdpath;
    
    public XMLParseResult(String path) {
	hasDTD = false;
	dtdpath = null;
	try {
	    
	    DOMParser parser = new DOMParser();
	    parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
	    parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
	    
	    XMLCheckErrorHandler errorHandler = new XMLCheckErrorHandler();
	    parser.setErrorHandler(errorHandler);
	    
	    //use own entityResolver for converting the dtd's url to a local file
	    XMLEntityResolver resolver = new XMLEntityResolver();
	    parser.setEntityResolver((EntityResolver)resolver);
	    
	    parser.parse(path);
	    hasDTD = resolver.hasDTD();
	    dtdpath = resolver.getDTDPath();
	    
	    Document document = parser.getDocument();
	    
	    warningList = errorHandler.getWarningList();
	    errorList = errorHandler.getErrorList();
	    fatalList = errorHandler.getFatalList();
	    
	    resultList = errorHandler.getResultList();
	    
	} catch (Exception e) {
	    warningList = new Vector();
	    errorList = new Vector();
	    
	    ErrorStruct err = new ErrorStruct("fatal error",0,0,e.getMessage());
	    
	    fatalList = new Vector();
	    fatalList.add(err);
	    resultList = new Vector();
	    resultList.add(err);
	    
	    if (log.isDebugEnabled()) {
		log.debug("ParseResult error: "+e.getMessage());
	    }
	}
    }
    
    public List getResultList() {
	return resultList;
    }
    
    public List getWarningList() {
	return warningList;
    }
    
    public List getErrorList() {
	return errorList;
    }
    
    public List getFatalList() {
	return fatalList;
    }
    
    public boolean hasDTD() {
	return hasDTD;
    }
    
    public String getDTDPath() {
	return dtdpath;
    }

}
