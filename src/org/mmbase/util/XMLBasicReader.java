/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;

import java.io.FileInputStream;
import java.io.StringReader;

import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * XMLBasicReader has two goals.
 <ul>
   <li>
   It provides a way for parsing XML
   </li>
   <li>
   It provides a way for searching in this XML, without the need for an XPath implementation, and without the hassle of org.w3c.dom alone.
   It uses dots to lay a path in the XML (XPath uses slashes).
   </li>
 </ul>
 *
 * @author Case Roule
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: XMLBasicReader.java,v 1.36 2003-07-18 14:57:50 michiel Exp $
 */
public class XMLBasicReader  {
    private static Logger log = Logging.getLoggerInstance(XMLBasicReader.class);

    /** for the document builder of javax.xml. */
    private static DocumentBuilder documentBuilder = null;
    private static DocumentBuilder altDocumentBuilder = null;

    protected static final String FILENOTFOUND = "FILENOTFOUND://";

    /** Public ID of the Error DTD version 1.0 */
    public static final String PUBLIC_ID_ERROR_1_0 = "-//MMBase//DTD error 1.0//EN";
    /** DTD resource filename of the Error DTD version 1.0 */
    public static final String DTD_ERROR_1_0 = "error_1_0.dtd";

    /** Public ID of the most recent Error DTD */
    public static final String PUBLIC_ID_ERROR = PUBLIC_ID_ERROR_1_0;
    /** DTD respource filename of the most recent Error DTD */
    public static final String DTD_ERROR = DTD_ERROR_1_0;

    /**
     * Register the Public Ids for DTDs used by XMLBasicReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_ERROR_1_0, DTD_ERROR_1_0, XMLBasicReader.class);
    }

    /** set this one to true, and parser will be loaded...  */

    /** set this one to true, when all document pars */
    private static final boolean VALIDATE = true;

    protected Document document;

    private String xmlFilePath;

    private static InputSource getInputSource(String path) {
        try {
            // remove file protocol if present to avoid errors in accessing file
            if (path.startsWith("file://")) path= path.substring(7);
            InputSource is = new InputSource(new FileInputStream(path));
            is.setSystemId("file://" + path);
            return is;
        } catch (java.io.FileNotFoundException e) {
            log.error("Error reading " + path + ": " + e.toString());
            log.service("Using empty source");
            // try to handle more or less gracefully
            InputSource is = new InputSource();
            is.setSystemId(FILENOTFOUND + path);
            is.setCharacterStream(new StringReader("<?xml version=\"1.0\"?>\n" +
                                                   "<!DOCTYPE error PUBLIC \"" + PUBLIC_ID_ERROR + "\"" +
                                                   " \"http://www.mmbase.org/dtd/error_1_0.dtd\">\n" +
                                                   "<error>" + e.toString() + "</error>"));
            return is;
        }
    }

    public XMLBasicReader(String path) {
        this(getInputSource(path), VALIDATE);
    }
    public XMLBasicReader(InputSource source, Class resolveBase) {
        this(source, VALIDATE, resolveBase);
    }
    public XMLBasicReader(String path, Class resolveBase) {
        this(getInputSource(path), VALIDATE, resolveBase);
    }

    public XMLBasicReader(InputSource source) {
        this(source, VALIDATE);
    }

    public XMLBasicReader(String path, boolean validating) {
        this(getInputSource(path), validating);
    }

    public XMLBasicReader(InputSource source, boolean validating) {
        this(source, validating, null);
    }


    public XMLBasicReader(InputSource source, boolean validating, Class resolveBase) {
        try {
            xmlFilePath = source.getSystemId();
            DocumentBuilder dbuilder = getDocumentBuilder(validating, new XMLEntityResolver(validating, resolveBase));

            if(dbuilder == null) throw new RuntimeException("failure retrieving document builder");
            if (log.isDebugEnabled()) log.debug("Reading " + source.getSystemId());
            document = dbuilder.parse(source);
        } catch(org.xml.sax.SAXException se) {
            throw new RuntimeException("failure reading document: " + source.getSystemId() + "\n" + Logging.stackTrace(se));
        } catch(java.io.IOException ioe) {
            throw new RuntimeException("failure reading document: " + source.getSystemId() + "\n" + ioe);
        }
    }
    /*
    public XMLBasicReader(Document doc) {
        document = doc;
        xmlFilePath = doc.getDoctype().getSystemId();
    }
    */
    private static DocumentBuilder createDocumentBuilder(boolean validating, ErrorHandler handler, EntityResolver resolver) {
        DocumentBuilder db;
        try {
            // get a new documentbuilder...
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            // get docuemtn builder AFTER setting the validation
            dfactory.setValidating(validating);
            dfactory.setNamespaceAware(true);

            db = dfactory.newDocumentBuilder();

            db.setErrorHandler(handler);

            // set the entity resolver... which tell us where to find the dtd's
            db.setEntityResolver(resolver);

        } catch(ParserConfigurationException pce) {
            log.error("a DocumentBuilder cannot be created which satisfies the configuration requested");
            log.error(Logging.stackTrace(pce));
            return null;
        }
        return db;
    }

    private static DocumentBuilder createDocumentBuilder(boolean validating, ErrorHandler handler) {
        return createDocumentBuilder(validating, handler, new XMLEntityResolver(validating));
    }
    private static DocumentBuilder createDocumentBuilder(boolean validating, EntityResolver resolver) {
        return createDocumentBuilder(validating, new XMLErrorHandler(), resolver);
    }
    private static DocumentBuilder createDocumentBuilder(boolean validating) {
        return createDocumentBuilder(validating, new XMLErrorHandler(), new XMLEntityResolver(validating));

    }


    public static DocumentBuilder getDocumentBuilder(boolean validating) {
        if (validating == VALIDATE) {
            return getDocumentBuilder();
        } else {
            if (altDocumentBuilder == null) {
                altDocumentBuilder = createDocumentBuilder(validating);
            }
            return altDocumentBuilder;
        }
    }

    public static DocumentBuilder getDocumentBuilder(boolean validating, ErrorHandler handler) {
        return createDocumentBuilder(validating, handler);
    }

    public static DocumentBuilder getDocumentBuilder(boolean validating, EntityResolver resolver) {
        return createDocumentBuilder(validating, resolver);
    }

    public static DocumentBuilder getDocumentBuilder(boolean validating, ErrorHandler handler, EntityResolver resolver) {
        return createDocumentBuilder(validating, handler, resolver);
    }

    public static DocumentBuilder getDocumentBuilder() {
        if(documentBuilder == null)  {
            documentBuilder = createDocumentBuilder(VALIDATE);
        }
        return documentBuilder;
    }

    public static DocumentBuilder getDocumentBuilder(Class refer) {
        return createDocumentBuilder(VALIDATE, new XMLErrorHandler(), new XMLEntityResolver(VALIDATE, refer));
    }

    /**
     * @param path Dot-separated list of tags describing path from root element to requested element.
     *             NB the path starts with the name of the root element.
     * @return Leaf element of the path
     */
    public Element getElementByPath(String path) {
        if (document == null) {
            log.error("Document is not defined, cannot get " + path);
        }
        return getElementByPath(document.getDocumentElement(),path);
    }

    /**
     * @param e Element from which the "relative" path is starting.
     *          NB the path starts with the name of the root element.
     * @param path Dot-separated list of tags describing path from root element to requested element
     * @return Leaf element of the path
     */
    public Element getElementByPath(Element e,String path) {
        StringTokenizer st = new StringTokenizer(path,".");
        if (!st.hasMoreTokens()) {
            // faulty path
            log.error("No tokens in path");
            return null;
        } else {
            String root = st.nextToken();
            if (e.getNodeName().equals("error")) {
                // path should start with document root element
                log.error("Error occurred : (" + getElementValue(e) + ")");
                return null;
            } else if (!e.getNodeName().equals(root)) {
                // path should start with document root element
                log.error("path ["+path+"] with root ("+root+") doesn't start with root element ("+e.getNodeName()+"): incorrect xml file" +
                          "("+xmlFilePath+")");
                return null;
            }
            while (st.hasMoreTokens()) {
                String tag = st.nextToken();
                NodeList nl = e.getElementsByTagName(tag);
                if (nl.getLength()>0) {
                    e = (Element)nl.item(0);
                } else {
                    // Handle error!
                    return null;
                }
            }
            return e;
        }
    }

    /**
     * @param path Path to the element
     * @return Text value of element
     */
    public String getElementValue(String path) {
        return getElementValue(getElementByPath(path));
    }

    /**
     * @param e Element
     * @return Text value of element
     */
    public String getElementValue(Element e) {
        if (e == null) {
            return "";
        } else {
            NodeList nl = e.getChildNodes();
            StringBuffer res = new StringBuffer();
            for (int i=0;i<nl.getLength();i++) {
                Node n = nl.item(i);
                if ((n.getNodeType() == Node.TEXT_NODE) ||
                    (n.getNodeType() == Node.CDATA_SECTION_NODE)) {
                    res.append(n.getNodeValue().trim());
                }
            }
            return res.toString();
        }
    }

    /**
     * @param e Element
     * @return Tag name of the element
     */
    public String getElementName(Element e) {
        return e.getTagName();
    }

    /**
     * @param path Path to the element
     * @param attr Attribute name
     * @return Value of attribute
     */
    public String getElementAttributeValue(String path, String attr) {
        return getElementAttributeValue(getElementByPath(path),attr);
    }


    /**
     * @param e Element
     * @param attr Attribute name
     * @return Value of attribute
     */
    public String getElementAttributeValue(Element e, String attr) {
        if (e==null)
            return "";
        else
            return e.getAttribute(attr);
    }

    /**
     * @param path Path to the element
     * @return Enumeration of child elements
     */
    public Enumeration getChildElements(String path) {
        return getChildElements(getElementByPath(path));
    }

    /**
     * @param e Element
     * @return Enumeration of child elements
     */
    public Enumeration getChildElements(Element e) {
        return getChildElements(e,"*");
    }

    /**
     * @param path Path to the element
     * @param tag tag to match ("*" means all tags")
     * @return Enumeration of child elements with the given tag
     */
    public Enumeration getChildElements(String path,String tag) {
        return getChildElements(getElementByPath(path),tag);
    }

    /**
     * @param e Element
     * @param tag tag to match ("*" means all tags")
     * @return Enumeration of child elements with the given tag
     */
    public Enumeration getChildElements(Element e,String tag) {
        Vector v = new Vector();
        boolean ignoretag=tag.equals("*");
        if (e!=null) {
            NodeList nl = e.getChildNodes();
            for (int i=0;i<nl.getLength();i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE &&
                    (ignoretag ||
                     ((Element)n).getTagName().equalsIgnoreCase(tag))) {
                    v.addElement(n);
                }
            }
        }
        return v.elements();
    }
    /**
     * Returns the file name associated with this reader (if there is one)
     */

    public String getFileName() {
        return xmlFilePath;
    }
}
