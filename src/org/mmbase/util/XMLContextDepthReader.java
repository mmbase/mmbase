/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.traversal.*;

import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.database.support.*;

/**
*/
public class XMLContextDepthReader  {

    Document document;
    DOMParser parser;


    public XMLContextDepthReader(String filename) {
        try {
            parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            //Errors errors = new Errors();
            //parser.setErrorHandler(errors);
            parser.parse(filename);
            document = parser.getDocument();

		/*
	    log.debug("*** START XML APPLICATION READER FOR : "+filename);	
	    log.debug("ExportSource="+getExportSource());	
	    log.debug("TimeStamp="+getTimeStamp());	
	    log.debug("Nodes nodes="+getNodes(mmbase));	
	    log.debug("*** END XML APPLICATION READER FOR : "+filename);	
		*/
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }



    /**
    */
    public int getDepth() {
	Node n1=document.getFirstChild();
	if (n1!=null) {
		Node n2=n1.getFirstChild();
		while (n2!=null) {
			if (n2.getNodeName().equals("depth")) {
				Node n3=n2.getFirstChild();
		    		String tmp=n3.getNodeValue();
				try {
			    		return(Integer.parseInt(tmp));
				} catch(Exception e) {
					return(-1);
				}
			
			}
			n2=n2.getNextSibling();
		}
	}
	return(-1);
    }



    /**
    */
    public String getStartBuilder() {
	Hashtable table=new Hashtable();
	Node n1=document.getFirstChild();
	if (n1!=null) {
		Node n2=n1.getFirstChild();
		while (n2!=null) {
			if (n2.getNodeName().equals("startnode")) {
				Node n3=n2.getFirstChild();
				while (n3!=null) {
					if (n3.getNodeName().equals("builder")) {
						Node n4=n3.getFirstChild();
		    				return(n4.getNodeValue());
					}
					n3=n3.getNextSibling();
				}
			}
			n2=n2.getNextSibling();
		}
	}
	return(null);
    }


    /**
    */
    public String getStartAlias() {
	Hashtable table=new Hashtable();
	Node n1=document.getFirstChild();
	if (n1!=null) {
		Node n2=n1.getFirstChild();
		while (n2!=null) {
			if (n2.getNodeName().equals("startnode")) {
				NamedNodeMap nm=n2.getAttributes();
				if (nm!=null) {
					Node n4=nm.getNamedItem("alias");
					if (n4!=null) return(n4.getNodeValue());
				}

			}
			n2=n2.getNextSibling();
		}
	}
	return(null);
    }


    /**
    */
    public String getStartWhere() {
	Hashtable table=new Hashtable();
	Node n1=document.getFirstChild();
	if (n1!=null) {
		Node n2=n1.getFirstChild();
		while (n2!=null) {
			if (n2.getNodeName().equals("startnode")) {
				Node n3=n2.getFirstChild();
				while (n3!=null) {
					if (n3.getNodeName().equals("where")) {
						Node n4=n3.getFirstChild();
		    				return(n4.getNodeValue());
					}
					n3=n3.getNextSibling();
				}
			}
			n2=n2.getNextSibling();
		}
	}
	return(null);
    }
}
