/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
*/
package org.mmbase.config;

import java.util.*;
import java.io.*;
import java.sql.*;

import org.mmbase.util.*;

/**
 * @author Case Roole, cjr@dds.nl
 * @version $Id: JavaReport.java,v 1.1 2000-09-11 20:26:53 case Exp $
 *
 * $Log: not supported by cvs2svn $
 */
public class JavaReport extends AbstractReport {
    
    // --- public methods ---------------------------------------
    public String label() {
	return "JVM and classpath";
    }

    /**
     * @return String with java and classpath configuration
     */
    public String report() {
	String res = "";
	String eol = (String)specialChars.get("eol");
	res = res + "JVM = "+System.getProperty("java.vendor") + ", version " + System.getProperty("java.version") + 
	    "(API java " + System.getProperty("java.class.version") + ")" + eol;
	//res = res + "classpath = " + System.getProperty("java.class.path").replace(System.getProperty("path.separator").charAt(0),'\n') + eol;

	boolean notfound = false;
	// Check for servlet API
	if (!checkServletAPILoadable()) {
	    res = res + "Couldn't find Servlet API in classpath" + eol;
	    notfound = true;
	} else {
	    res = res + "Found Servlet API" + eol;
	}
	
	// Check for xerces xml parser
	if (!checkXercesLoadable()) {
	    res = res + "Couldn't find Xerces XML parser in classpath" + eol;
	    notfound = true;
	} else {
	    res = res + "Found Xerces" + eol;
	}

	// Check for xalan xsl transformations
	if (!checkXalanLoadable()) {
	    res = res + "Couldn't find Xalan XSL transformation code in classpath" + eol;
	    notfound = true;
	} else {
	    res = res + "Found Xalan" + eol;
	}

	if (notfound) {
	    res = res + "classpath = "+System.getProperty("java.class.path") + eol;
	}


	return res;
    }

    // --- private methods ---------------------------------------
    private boolean checkServletAPILoadable() {
	try {
	    Class c = Class.forName("java.util.Vector");
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }
    
    private boolean checkXercesLoadable() {
	try {
	    Class c = Class.forName("org.apache.xerces.parsers.DOMParser");
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

    private boolean checkXalanLoadable() {
	try {
	    Class c = Class.forName("org.apache.xalan.xslt.XSLTProcessor");
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

}
