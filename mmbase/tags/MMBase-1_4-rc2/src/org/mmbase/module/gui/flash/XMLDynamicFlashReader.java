/* -*- tab-width: 4; -*-

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.gui.flash;

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.traversal.*;

import org.mmbase.util.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.database.support.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
*/
public class XMLDynamicFlashReader {

    private static Logger log = Logging.getLoggerInstance(XMLDynamicFlashReader.class.getName()); 

    Document document;
    DOMParser parser;


    public XMLDynamicFlashReader(String filename) {
        try {
            parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            //Errors errors = new Errors();
            //parser.setErrorHandler(errors);
			File file = new File(filename);
        	if(!file.exists()) {
  				log.error("xml file "+filename+" does not exist");
           	}
            parser.parse(filename);
            document = parser.getDocument();

		} catch(Exception e) {
		    log.error(Logging.stackTrace(e));
		}
    }


    public XMLDynamicFlashReader(CharArrayReader reader) {
        try {
            parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
            parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            //Errors errors = new Errors();
            //parser.setErrorHandler(errors);
            parser.parse(new InputSource(reader));
            document = parser.getDocument();

		} catch(Exception e) {
		    log.error(Logging.stackTrace(e));
		}
    }


    /**
    * get the name of the src flash
    */
    public String getSrcName() {
		Node n1=document.getFirstChild();
		if (n1!=null) {
			NamedNodeMap nm=n1.getAttributes();
			if (nm!=null) {
				Node n2=nm.getNamedItem("src");
				String name=n2.getNodeValue();
				return(name);
			}
		}
		return(null);
    }

    /**
    * get the caching type
    */
    public String getCaching() {
		Node n1=document.getFirstChild();
		if (n1!=null) {
			NamedNodeMap nm=n1.getAttributes();
			if (nm!=null) {
				Node n2=nm.getNamedItem("caching");
				if (n2==null) return(null);
				String name=n2.getNodeValue();
				return(name);
			}
		}
		return(null);
    }


    /**
    * get the parser
    */
    public String getParser() {
		Node n1=document.getFirstChild();
		if (n1!=null) {
			NamedNodeMap nm=n1.getAttributes();
			if (nm!=null) {
				Node n2=nm.getNamedItem("parser");
				String parser=n2.getNodeValue();
				return(parser);
			}
		}
		return(null);
    }



    /**
    */
    public Vector getReplaces() {
		Vector results=new Vector();
		Node n1=document.getFirstChild();
		if (n1!=null) {
			Node n2=n1.getFirstChild();
			while (n2!=null) {
				if (n2.getNodeName().equals("replace")) {
					Hashtable rep=new Hashtable();

					// decode all the needed values in the replace itself
					NamedNodeMap nm=n2.getAttributes();
					if (nm!=null) {
						Node n3=nm.getNamedItem("type");
						if (n3!=null) {
							String val=n3.getNodeValue();
							rep.put("type",val);
						}
						n3=nm.getNamedItem("id");
						if (n3!=null) {
							String val=n3.getNodeValue();
							rep.put("id",val);
						}

					}

					Node n3=n2.getFirstChild();
					while (n3!=null) {

						// decode all font hints
						if (n3.getNodeName().equals("font")) {
							nm=n3.getAttributes();
							if (nm!=null) {
								Node n5=nm.getNamedItem("type");
								if (n5!=null) rep.put("fonttype",n5.getNodeValue());
								n5=nm.getNamedItem("size");
								if (n5!=null) rep.put("fontsize",n5.getNodeValue());
								n5=nm.getNamedItem("kerning");
								if (n5!=null) rep.put("fontkerning",n5.getNodeValue());
								n5=nm.getNamedItem("color");
								if (n5!=null) rep.put("fontcolor",n5.getNodeValue());
							}
						}
						// decode all font hints
						else if (n3.getNodeName().equals("string")) {
								Node n5=n3.getFirstChild();
								if (n5!=null) rep.put("string",n5.getNodeValue());
								nm=n3.getAttributes();
								if (nm!=null) {
									n5=nm.getNamedItem("file");
									if (n5!=null) rep.put("stringfile",n5.getNodeValue());
								}	
						}
						n3=n3.getNextSibling();
					}


					results.addElement(rep);
				}
				n2=n2.getNextSibling();
			}
		}
		return(results);
    }

    /**
    */
    public Vector getDefines() {
		Vector results=new Vector();
		Node n1=document.getFirstChild();
		if (n1!=null) {
			Node n2=n1.getFirstChild();
			while (n2!=null) {
				if (n2.getNodeName().equals("define")) {
					Hashtable rep=new Hashtable();

					// decode all the needed values in the replace itself
					NamedNodeMap nm=n2.getAttributes();
					if (nm!=null) {
						Node n3=nm.getNamedItem("type");
						if (n3!=null) {
							rep.put("type",n3.getNodeValue());
						}
						n3=nm.getNamedItem("id");
						if (n3!=null) {
							rep.put("id",n3.getNodeValue());
						}

						n3=nm.getNamedItem("width");
						if (n3!=null) {
							rep.put("width",n3.getNodeValue());
						}

						n3=nm.getNamedItem("height");
						if (n3!=null) {
							rep.put("height",n3.getNodeValue());
						}

						n3=nm.getNamedItem("src");
						if (n3!=null) {
							rep.put("src",n3.getNodeValue());
						}

						n3=nm.getNamedItem("value");
						if (n3!=null) {
							rep.put("value",n3.getNodeValue());
						}
						n3=nm.getNamedItem("file");
						if (n3!=null) {
							rep.put("valuefile",n3.getNodeValue());
						}


					}

					results.addElement(rep);
				}
				n2=n2.getNextSibling();
			}
		}
		return(results);
    }

}
