/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.util.*;

import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mmbase.util.XMLEntityResolver;
import org.mmbase.util.XMLErrorHandler;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * The DocumentReader class provides methods for loading a xml document in memory.
 * It serves as the base class for DocumentWriter (which adds ways to write a document), and
 * XMLBasicReader, which adds path-like methods with which to retrieve elements.
 *
 * @author Case Roule
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: DocumentReader.java,v 1.13 2005-08-31 20:51:33 michiel Exp $
 * @since MMBase-1.7
 */
public class DocumentReader  {
    private static Logger log = Logging.getLoggerInstance(DocumentReader.class);

    /** for the document builder of javax.xml. */
    private static Map documentBuilders = Collections.synchronizedMap(new HashMap());

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
        XMLEntityResolver.registerPublicID(PUBLIC_ID_ERROR_1_0, DTD_ERROR_1_0, DocumentReader.class);
    }

    protected Document document;

    private String systemId;

    /**
     * Returns the default setting for validation for DocumentReaders.
     * @todo add a way to configure this value, so validation can be turned off in i.e. production environments
     * @return true if validation is on
     */
    protected static final boolean validate() {
        return true;
    }


    /**
     * Creates an empty document reader.
     */
    protected DocumentReader() {
    }

    /**
     * Constructs the document by reading it from a source.
     * @param source the input source from which to read the document
     */
    public DocumentReader(InputSource source) {
        this(source, validate(), null);
    }

    /**
     * Constructs the document by reading it from a source.
     * @param source the input source from which to read the document
     * @param validating whether to validate the document
     */
    public DocumentReader(InputSource source, boolean validating) {
        this(source, validating, null);
    }

    /**
     * Constructs the document by reading it from a source.
     * You can pass a resolve class to this constructor, allowing you to indicate the package in which the dtd
     * of the document read is to be found. The dtd sould be in the resources package under the package of the class passed.
     * @param source the input source from which to read the document
     * @param resolveBase the base class whose package is used to resolve dtds, set to null if unknown
     */
    public DocumentReader(InputSource source, Class resolveBase) {
        this(source, DocumentReader.validate(), resolveBase);
    }

    /**
     * Constructs the document by reading it from a source.
     * You can pass a resolve class to this constructor, allowing you to indicate the package in which the dtd
     * of the document read is to be found. The dtd sould be in the resources package under the package of the class passed.
     * @param source the input source from which to read the document
     * @param validating whether to validate the document
     * @param resolveBase the base class whose package is used to resolve dtds, set to null if unknown
     */
    public DocumentReader(InputSource source, boolean validating, Class resolveBase) {
        try {
            systemId = source.getSystemId();
            XMLEntityResolver resolver = null;
            if (resolveBase != null) resolver = new XMLEntityResolver(validating, resolveBase);
            DocumentBuilder dbuilder = getDocumentBuilder(validating, null/* no error handler */, resolver);
            if(dbuilder == null) throw new RuntimeException("failure retrieving document builder");
            if (log.isDebugEnabled()) log.debug("Reading " + source.getSystemId());
            document = dbuilder.parse(source);
        } catch(org.xml.sax.SAXException se) {
            throw new RuntimeException("failure reading document: " + source.getSystemId() + "\n" + Logging.stackTrace(se));
        } catch(java.io.IOException ioe) {
            throw new RuntimeException("failure reading document: " + source.getSystemId() + "\n" + ioe, ioe);
        }
    }

    /**
     * @since MMBase-1.8
     */
    public DocumentReader(Document doc) {
        document = doc;
    }


    private static boolean warnedJAXP12 = false;
    /**
     * Creates a DocumentBuilder using SAX.
     * @param validating if true, the documentbuilder will validate documents read
     * @param xsd     Whether to use XSD for validating
     * @param handler a ErrorHandler class to use for catching parsing errors, pass null to use a default handler
     * @param resolver a EntityResolver class used for resolving the document's dtd, pass null to use a default resolver
     * @return a DocumentBuilder instance, or null if none could be created
     */
    private static DocumentBuilder createDocumentBuilder(boolean validating, boolean xsd, ErrorHandler handler, EntityResolver resolver) {
        DocumentBuilder db;
        if (handler == null) handler = new XMLErrorHandler();
        if (resolver == null) resolver = new XMLEntityResolver(validating);
        try {
            // get a new documentbuilder...
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            // get document builder AFTER setting the validation
            dfactory.setValidating(validating);
            if (validating && xsd) {
                try {
                    dfactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", 
                                          "http://www.w3.org/2001/XMLSchema");
                } catch (IllegalArgumentException iae) {
                    if (! warnedJAXP12) {
                        log.warn("The XML parser does not support JAXP 1.2, XSD validation will not work.", iae);
                        warnedJAXP12 = true;
                    }
                }
            }
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

    /**
     * Creates a DocumentBuilder with default settings for handler, resolver, or validation,
     * obtaining it from the cache if available.
     * @return a DocumentBuilder instance, or null if none could be created
     */
    public static DocumentBuilder getDocumentBuilder() {
        return getDocumentBuilder(validate(), null, null);
    }

    /**
     * @see {#getDocumentBuilder(boolean, ErrorHandler, EntityResolver)}
     */
    public static DocumentBuilder getDocumentBuilder(boolean validating, ErrorHandler handler, EntityResolver resolver) {
        return getDocumentBuilder(validating, false, handler, resolver);
    }

    /**
     * Creates a DocumentBuilder.
     * DocumentBuilders that use the default error handler or entity resolver are cached (one for validating,
     * one for non-validating document buidlers).
     * @param validating if true, the documentbuilder will validate documents read
     * @param xsd        if true, validating will be done by an XML schema definiton.
     * @param handler a ErrorHandler class to use for catching parsing errors, pass null to use the default handler
     * @param resolver a EntityResolver class used for resolving the document's dtd, pass null to use the default resolver
     * @return a DocumentBuilder instance, or null if none could be created
     * @since MMBase-1.8.
     */
    public static DocumentBuilder getDocumentBuilder(boolean validating, boolean xsd, ErrorHandler handler, EntityResolver resolver) {
        if (handler == null && resolver == null) {
            String key = "" + validating + xsd;
            DocumentBuilder db = (DocumentBuilder) documentBuilders.get(key);
            if (db == null) {
                db = createDocumentBuilder(validating, xsd, null, null);
                documentBuilders.put(key, db);
            }
            return db;
        } else {
            return createDocumentBuilder(validating, xsd, handler, resolver);
        }
    }

    /**
     * Return the text value of a node.
     * It includes the contents of all child textnodes and CDATA sections, but ignores
     * everything else (such as comments)
     * The code trims excessive whitespace unless it is included in a CDATA section.
     *
     * @param n the Node whose value to determine
     * @return a String representing the node's textual value
     */
    public static String getNodeTextValue(Node n) {
        NodeList nl = n.getChildNodes();
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < nl.getLength(); i++) {
            Node textnode = nl.item(i);
            if (textnode.getNodeType() == Node.TEXT_NODE) {
                res.append(textnode.getNodeValue().trim());
            } else if (textnode.getNodeType() == Node.CDATA_SECTION_NODE) {
                res.append(textnode.getNodeValue());
            }
        }
        return res.toString();
    }

    /**
     * Returns whether an element has a certain attribute, either an unqualified attribute or an attribute that fits in the
     * passed namespace
     */
    static public boolean hasAttribute(Element element, String nameSpace, String localName) {
        return element.hasAttributeNS(nameSpace,localName) || element.hasAttribute(localName);
    }

    /**
     * Returns the value of a certain attribute, either an unqualified attribute or an attribute that fits in the
     * passed namespace
     */
    static public String getAttribute(Element element, String nameSpace, String localName) {
        if (element.hasAttributeNS(nameSpace,localName)) {
            return element.getAttributeNS(nameSpace,localName);
        } else {
            return element.getAttribute(localName);
        }
    }

    /**
     * Returns the systemID of the InputSource used to read the document.
     * This is generally the document's file path.
     * @return the systemID as a String
     *
     * @since MMBase-1.8
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @param e Element
     * @return Tag name of the element
     */
    public String getElementName(Element e) {
        return e.getLocalName();
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
        if (e == null)
            return "";
        else
            return e.getAttribute(attr);
    }

    /**
     * Determine the root element of the contained document
     * @return root element
     */
    public Element getRootElement() {
        if (document == null) {
            log.error("Document is not defined, cannot get root element");
        }
        return document.getDocumentElement();
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
    public Element getElementByPath(Element e, String path) {
        StringTokenizer st = new StringTokenizer(path,".");
        if (!st.hasMoreTokens()) {
            // faulty path
            log.error("No tokens in path");
            return null;
        } else {
            String root = st.nextToken();
            if (e.getLocalName().equals("error")) {
                // path should start with document root element
                log.error("Error occurred : (" + getElementValue(e) + ")");
                return null;
            } else if (!e.getLocalName().equals(root)) {
                // path should start with document root element
                log.error("path ["+path+"] with root ("+root+") doesn't start with root element ("+e.getLocalName()+"): incorrect xml file" +
                          "("+getSystemId()+")");
                return null;
            }
            OUTER:
            while (st.hasMoreTokens()) {
                String tag = st.nextToken();
                NodeList nl = e.getChildNodes();
                for(int i = 0; i < nl.getLength(); i++) {
                    if (! (nl.item(i) instanceof Element)) continue;
                    e = (Element)nl.item(i);
                    if (e.getLocalName().equals(tag)) continue OUTER;
                }
                // Handle error!
                return null;
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
            return getNodeTextValue(e);
        }
    }

    /**
     * @param path Path to the element
     * @return Iterator of child elements
     */
    public Iterator getChildElements(String path) {
        return getChildElements(getElementByPath(path));
    }

    /**
     * @param e Element
     * @return Iterator of child elements
     */
    public Iterator getChildElements(Element e) {
        return getChildElements(e,"*");
    }

    /**
     * @param path Path to the element
     * @param tag tag to match ("*" means all tags")
     * @return Iterator of child elements with the given tag
     */
    public Iterator getChildElements(String path,String tag) {
        return getChildElements(getElementByPath(path),tag);
    }

    /**
     * @param e Element
     * @param tag tag to match ("*" means all tags")
     * @return Iterator of child elements with the given tag
     * @todo XXXX MM: Since we have changed the return type from 1.7 to 1.8 anyway, why don't we return a List then?
     */
    public Iterator getChildElements(Element e,String tag) {
        List v = new ArrayList();
        boolean ignoretag = tag.equals("*");
        if (e!=null) {
            NodeList nl = e.getChildNodes();
            for (int i=0;i<nl.getLength();i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE &&
                    (ignoretag ||
                     ((Element)n).getLocalName().equalsIgnoreCase(tag))) {
                    v.add(n);
                }
            }
        }
        return v.iterator();
    }

}
