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

import org.mmbase.module.corebuilders.*;

/**
 * 
 * Writes XML as pretty printed HTML
 *
 * cjr@dds.nl 
 */
public class XMLScreenWriter  {

    Document document;
    DOMParser parser;

    static String tag_color = "#007700";
    static String attribute_color = "#DD0000";
    static String comment_color = "#FF8000";


    public XMLScreenWriter(String filename) {
        try {
            parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            //Errors errors = new Errors();
            //parser.setErrorHandler(errors);
            parser.parse(filename);
            document = parser.getDocument();

	} catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void write(Writer out) throws IOException {
	write(out,document,-1);
    }

    public void write(Writer out, Node node, int level) throws IOException {
	if (node != null) {
	    if (node.getNodeType() == node.COMMENT_NODE) {
		out.write(indent(level));
		out.write("<font color=\""+comment_color+"\">&lt;!--"+node.getNodeValue()+"--&gt;</font><br>\n");
	    } else if (node.getNodeType() == node.DOCUMENT_NODE) {
		NodeList nl = node.getChildNodes();
		for (int i=0; i < nl.getLength(); i++ ) {
		    write(out,nl.item(i),level+1);
		}
	    } else if (node.getNodeType() == node.TEXT_NODE) {
		out.write(node.getNodeValue()+"\n");
	    } else if (node.getNodeType() == node.DOCUMENT_TYPE_NODE) {
		String publicid = ((DocumentType)node).getPublicId();
		String systemid = ((DocumentType)node).getSystemId();
		out.write("<font color=\""+tag_color+"\">&lt;!DOCTYPE "+((DocumentType)node).getName());
		if (publicid != null && !publicid.equals("")) {
		    out.write(" PUBLIC \""+((DocumentType)node).getPublicId()+"\"");
		}
		if (systemid != null && !systemid.equals("")) {
		    out.write(" \""+((DocumentType)node).getSystemId()+"\"");
		}
		out.write("&gt;</font><br>\n");
	    } else {
		boolean is_end_node = isEndNode(node);
		NamedNodeMap nnm = node.getAttributes();
		if (nnm == null || nnm.getLength()==0) {
		    out.write(indent(level));
		    out.write("<font color=\""+tag_color+"\">&lt;"+node.getNodeName()+"&gt;</font>\n");
		} else {
		    out.write(indent(level));
		    out.write("<font color=\""+tag_color+"\">&lt;"+node.getNodeName()+"</font>");
		    for (int i=0; i < nnm.getLength(); i++) {
			Node attribute = nnm.item(i);
			out.write(" <font color=\""+attribute_color+"\">"+attribute.getNodeName()+"=\""+attribute.getNodeValue()+"\"</font>");			
			if (i < nnm.getLength()-1) {
			    out.write(" ");
			}
		    }
		    out.write("<font color=\""+tag_color+"\">&gt;</font>");
		}
		NodeList nl = node.getChildNodes();
		if (!is_end_node) {
		    out.write("<br>\n");
		}
		for (int i=0; i < nl.getLength(); i++ ) {
		    write(out,nl.item(i),level+1);
		}
		if (!is_end_node) {
		    out.write(indent(level));
		}
		out.write("<font color=\""+tag_color+"\">&lt;/"+node.getNodeName()+"&gt;</font><br>\n");
	    }
	}	
    }

    /**
     * @param level Indentation level
     * @return String of hard HTML spaces (&nbsp;) that are multiple of level
     */
    protected String indent(int level) {
	StringBuffer buf = new StringBuffer();
	for (int i=0; i < level; i++) {
	    buf.append("&nbsp;&nbsp;");
	}
	return buf.toString();
    }
    
    /**
     * @param node 
     * @return Whether the node contains only a text node, or possibly also an attribute node
     */
    protected boolean isEndNode(Node node) {
	int countTextNodes = 0;
	NodeList nl = node.getChildNodes();
	for (int i=0; i < nl.getLength(); i++ ) {
	    if (nl.item(i).getNodeType() == node.TEXT_NODE) {
		countTextNodes++;
	    } else if (nl.item(i).getNodeType() == node.ATTRIBUTE_NODE) {
		// do nothing
	    } else {
		return false;
	    }
	}
	return (countTextNodes >0);
    }
}
