/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.editwizard;

import org.w3c.dom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.*;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XNodeSet;

import org.mmbase.bridge.Cloud;
import org.mmbase.util.logging.*;

import org.mmbase.cache.xslt.*;
import org.mmbase.util.xml.URIResolver;
import org.mmbase.util.XMLErrorHandler;
import org.mmbase.util.XMLEntityResolver;

/**
 * This class contains static utility methods used by the editwizard.
 * Most methods handle xml functions for you and are just to support ease and lazyness.
 *
 *
 * @javadoc
 * @author  Kars Veling
 * @author  Pierre van Rooden
 * @author  Michiel Meeuwissen
 * @since   MMBase-1.6
 * @version $Id: Utils.java,v 1.31 2003-06-10 09:12:50 pierre Exp $
 */
public class Utils {

    private static Logger log = Logging.getLoggerInstance(Utils.class.getName());
    /**
     * This method returns a new instance of a DocumentBuilder.
     *
     * @return     a DocumentBuilder.
     */
    public static DocumentBuilder getDocumentBuilder(boolean validate) {
	return org.mmbase.util.XMLBasicReader.getDocumentBuilder(validate, new XMLErrorHandler(validate, XMLErrorHandler.ERROR), new XMLEntityResolver(validate, Utils.class));
    }

    /**
     * This method returns an empty XMLDocument.
     *
     * @return     a new empty Document. Returns null if something went wrong.
     */
    public static Document emptyDocument() {
        try {
            DocumentBuilder dBuilder = getDocumentBuilder(false);
            return dBuilder.newDocument();
        } catch (Throwable t) {
            log.error(Logging.stackTrace(t));
        }
        return null;
    }


    /**
     * This method can load a xml file and returns the resulting document. If something went wrong, null is returned.
     *
     * @param      file the file to be loaded.
     * @return     The loaded xml Document
     * @throws     WizardException if the document is invalid
     */
    public static Document loadXMLFile(File file) throws WizardException {
        try {
            try {	  
                DocumentBuilder b = getDocumentBuilder(true);
                return b.parse(file);
            } catch (SAXParseException ex) {
                // try without validating, perhaps no doc-type was specified
                DocumentBuilder b = getDocumentBuilder(false);
                Document d = b.parse(file);
                if (d.getDoctype() == null) {
                    log.warn("No DocumentType speficied in " + file);
                    return d;
                } else {
                    throw new WizardException("Error on line " + ex.getLineNumber() + " (column " + ex.getColumnNumber() + ") of schema xml file " + ex.getSystemId() + ": " + ex.getMessage());
                }
            }        
        } catch (SAXException e) {
            throw new WizardException("Could not load schema xml file '"+file + "' (SAX)\n" + Logging.stackTrace(e));
        } catch (IOException ioe) {
            throw new WizardException("Could not load schema xml file '"+file + "' (IO)\n" + Logging.stackTrace(ioe));
        }
    }

    /**
     * With this method you can parse a xml string and get the resulting Document.
     *
     * @param      xml     The xml string to be parsed. Note that you should supply xml for a valid document (one root node, etc)
     * @return     The newly created xml Document
     * @throws     WizardException if something went wrong
     */
    public static Document parseXML(String xml) throws WizardException {
        try {
            DocumentBuilder b = getDocumentBuilder(false);
            StringReader reader = new StringReader(xml);
            return b.parse(new InputSource(reader));
        } catch (Exception e) {
            throw new WizardException("Could not parse schema xml file. xml:"+xml + "\n" + Logging.stackTrace(e));
        }
    }

    /**
     * Serialize a node to the given writer.
     *
     * @param        node    The node to serialize
     * @param        writer  The writer where the stream should be written to.
     */
    public static void printXML(Node node, Writer writer) {
        try {
            Transformer serializer = FactoryCache.getCache().getDefaultFactory().newTransformer();
            // shouldn't this tranformer be cached?
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            serializer.transform(new DOMSource(node),  new StreamResult(writer));
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
            throw new RuntimeException(Logging.stackTrace(e));
        }
    }

    /**
     * Serialize a node to a string, and return the result.
     *
     * @param        node    The node to serialize
     */
    public static String getSerializedXML(Node node)  {
        StringWriter str=new StringWriter();
        printXML(node, str);
        return str.toString();
    }

    /**
     * Serialize a node and returns the resulting String.
     *
     * @param        node    The node to serialize
     * @return      The resulting string.
     */
    public static String getXML(Node node)  {
        StringWriter writer = new StringWriter();
        printXML(node, writer);
        return writer.toString();
    }

    /**
     * Sets an attribute of a specific node. If the attribute does not exist, a new attribute is created.
     * If the attribute already exists, the value is overwritten with the new one.
     *
     * @param       node    The node of which the
     */
    public static void setAttribute(Node node, String name, String value) {
        Attr a = node.getOwnerDocument().createAttribute(name);
        a.setNodeValue(value);
        node.getAttributes().setNamedItem(a);
    }

    /**
     * Gets an attribute of an node.
     *
     * @param       node    the node to get the attribute from
     * @param       name    the attributename requested
     * @return     The value of the attribute. Returns "" if none exists.
     */
    public static String getAttribute(Node node, String name) {
        return getAttribute(node, name, "");
    }

    /**
     * Gets an attribute of an node.
     *
     * @param       node    the node to get the attribute from
     * @param       name    the attributename requested
     * @param       defaultvalue    the defaultvalue what should be returned if attribute was not found.
     * @return     The value of the attribute. Returns the defaultvalue if none exists.
     */
    public static String getAttribute(Node node, String name, String defaultvalue) {
        try {
            Node n = node.getAttributes().getNamedItem(name);
            if (n == null) return defaultvalue;
            return n.getNodeValue();
        } catch (Exception e) {
            log.warn(Logging.stackTrace(e));
            return defaultvalue;
        }
    }

    /**
     * Returns the text value of the given node
     *
     * @param       node    the node where you want the text from.
     * @return     The value of the containing textnode. If no textnode present, "" is returned.
     */
    public static String getText(Node node) {
        return getText(node, "");
    }

    /**
     * Returns the text value of the given node. But can do more.
     *
     * @param       node    the node where you want the text from.
     * @param       defaultvalue    of no text is found, this defaultvalue will be returned
     * @param       params  params to be used. eg.: $username will be replaced by the values in the hashtable, if a 'username' key is in the hashtable.
     * @return     The value of the containing textnode. If no textnode present, defaultvalue is returned.
     */
    public static String getText(Node node, String defaultvalue, Map params) {
        return fillInParams(getText(node, defaultvalue), params);
    }

    /**
     * Returns the text value of the given node.
     *
     * @param       node    the node where you want the text from.
     * @param       defaultvalue  if no text is found, this defaultvalue will be returned
     * @return     The value of the containing textnode. If no textnode present, defaultvalue is returned.
     */
    public static String getText(Node node, String defaultvalue) {
        try {
            if ((node.getNodeType()==Node.TEXT_NODE) ||
                (node.getNodeType()==Node.ATTRIBUTE_NODE)) {
                return node.getNodeValue();
            }
            Node childnode=node.getFirstChild();
            if ((childnode!=null) &&(childnode.getNodeType()==Node.TEXT_NODE)) {
                return childnode.getNodeValue();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return defaultvalue;
    }

    /**
     * Selects a single node using the given xpath and uses the given node a a starting context and returns the textnode found. If no text is found, the default value is given.
     *
     * @param       node  the contextnode to start the xpath from.
     * @param       xpath   the xpath which should be fired.
     * @param       defaultvalue    this value will be returned when no node is found using the xpath.
     * @return     The text string.
     */
    public static String selectSingleNodeText(Node node, String xpath, String defaultvalue) {
        return selectSingleNodeText(node, xpath, defaultvalue);
    }
    
    /**
     * Selects a single node using the given xpath and uses the given node a a starting context and returns the textnode found. 
     * If no text is found, the default value is given.
     * If a cloud argument is passed, it is used to select the most approprite text (using xml:lang attributes) depending on 
     * the cloud's properties.
     *
     * @param  node  the contextnode to start the xpath from.
     * @param  xpath the xpath which should be fired.
     * @param  defaultvalue  this value will be returned when no node is found using the xpath.
     * @param  cloud the cloud whose locale is to be used for selecting language-specific texts
     * @return The text string.
     */
    public static String selectSingleNodeText(Node node, String xpath, String defaultvalue, Cloud cloud) {
        try {
            XObject x = null;
            // select based on cloud locale setting
            if (cloud != null) {
                x=XPathAPI.eval(node, xpath + "[lang('"+cloud.getLocale().getLanguage()+"')]");
            }
            // if not found or n.a., just grab the first you can find 
            if (x == null || (x instanceof XNodeSet && x.nodelist().getLength() < 1)) {
                x = XPathAPI.eval(node, xpath);
            }
            if (x == null || (x instanceof XNodeSet && x.nodelist().getLength() < 1)) {
                return defaultvalue;
            }
            if (x instanceof XNodeSet) {
                try {
                    return getText(x.nodelist().item(0));
                } catch (Throwable ignore) {
                    //Error will occur if older Xalan/Xerces is used. If so, we'll just return the string value. Will work in most cases.
                    return x.toString();
                }
            } else {
                return x.toString();
            }
        } catch (Exception e) {
            log.error(Logging.stackTrace(e) + ", evaluating xpath:" + xpath);
        }
        return defaultvalue;
    }

    /**
     * This method stores text in a node. If needed, a new text node is created.
     *
     * @param       node    the parentnode on which a textnode should be created or overwritten.
     * @param       text    The text what should be placed in the textnode.
     * @param       params  optional params which should be used in a replace action.
     */
    public static void storeText(Node node, String text, Map params) {
        storeText(node, fillInParams(text, params));
    }

    /**
     * Same as above, but without the params.
     */
    public static void storeText(Node node, String text) {
        Node t = node.getFirstChild();
        if (t!=null) {
            if (t.getNodeType()==Node.TEXT_NODE) {
                t.setNodeValue(text);
                return;
            }
        }
        t = node.getOwnerDocument().createTextNode(text);
        node.appendChild(t);
    }

    /**
     * This method clones, imports and places all nodes in the list and places it
     */
    public static void appendNodeList(NodeList list, Node dest) {
        if (list==null) return;
        Document ownerdoc = dest.getOwnerDocument();
        for (int i=0; i<list.getLength(); i++) {
            Node n = list.item(i).cloneNode(true);
            dest.appendChild(ownerdoc.importNode(n, true));
        }
    }


    /**
     * This method creates a new node, places text and attaches it to the parent.
     *
     * @param       parentnode      Place where new node should be appended
     * @param       nodename        the name of the new node
     * @param       nodevalue       the new nodevalue
     * @return     the newly created node
     */
    public static Node createAndAppendNode(Node parentnode, String nodename, String nodevalue) {
        Node n = parentnode.getOwnerDocument().createElement(nodename);
        storeText(n, nodevalue);
        parentnode.appendChild(n);
        return n;
    }

    /**
     * Following routines are strange but handy for the editwizard
     */

    /**
     * This method tags all nodes in the nodelist. Id-counter starts counting with 1.
     *
     * @param       list    the nodelist
     * @param       name    the name of the tags
     * @param       pre     the prefix what should be used in the tag-values
     */


    public static int tagNodeList(NodeList list, String name, String pre) {
        return tagNodeList(list, name, pre, 1);
    }

    /**
     * Same as above, but now you can supply a startnumber.
     */
    public static int tagNodeList(NodeList list, String name, String pre, int start) {
        for (int i=0; i<list.getLength(); i++) {
            Node n = list.item(i);
            Utils.setAttribute(n, name, pre + "_" + (start++) );
        }
        return start;
    }


    /**
     * Copies all attributes from one node to the other.
     * @param       source  One node
     * @param       dest    The other node
     */
    public static void copyAllAttributes(Node source, Node dest) {
        copyAllAttributes(source,dest,null);
    }

    /**
     * Same as above, but now you can supply a Vector with names which should NOT be copied.
     */

    public static void copyAllAttributes(Node source, Node dest, List except) {
        NamedNodeMap attrs = source.getAttributes();
        for (int i=0; i<attrs.getLength(); i++) {
            String attrname = attrs.item(i).getNodeName();
            if (except==null || (!except.contains(attrname))) setAttribute(dest, attrname, attrs.item(i).getNodeValue());
        }
    }



    /**
     * Below are handy XSL(T) Utils
     */

    /**
     * This method can set the stylesheetparams for a transformer.
     *
     * @param       transformer     The transformer.
     * @param       params          The params to be placed. Standard name/value pairs.
     */
    protected static void setStylesheetParams(Transformer transformer, Map params){
        if (params==null) return;

        Iterator i = params.entrySet().iterator();
        while (i.hasNext()){
            Map.Entry entry = (Map.Entry) i.next();
            log.debug("setting param " + entry.getKey() + " to " + entry.getValue());
            transformer.setParameter((String) entry.getKey(), entry.getValue());
        }
    }



    /**
     * This method does a standard XSL(T) transform on a node as a base context node and sends it to the given Result result.
     *
     * @param       node    the base context node to run the xsl(t) against.
     * @param       xslFile the xsl file
     * @param       result  The place where to put the result of the transformation
     * @param       params  Optional params.
     */
    public static void transformNode(Node node, File xslFile, URIResolver uri, Result result, Map params) throws TransformerException {
        TemplateCache cache= TemplateCache.getCache();
        Source xsl = new StreamSource(xslFile);
        Templates cachedXslt = cache.getTemplates(xsl, uri);
        if (cachedXslt == null) {
            cachedXslt = FactoryCache.getCache().getFactory(uri).newTemplates(xsl);
            cache.put(xsl, cachedXslt, uri);
        } else {
            if (log.isDebugEnabled()) log.debug("Used xslt from cache with " + xsl.getSystemId());
        }
        Transformer transformer = cachedXslt.newTransformer();
        // Set any stylesheet parameters.
        if (params != null) {
            setStylesheetParams(transformer, params);
        }
        if (log.isDebugEnabled()) log.trace("transforming: \n" + stringFormatted(node));
        transformer.transform(new DOMSource(node), result);
    }


    /**
     * For debugging purposes. Return the constructed document as a String.
     */

    public static String stringFormatted(Node node) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("node " +node);
                log.debug("doc " + node.getOwnerDocument());
            }
            Document doc;
            if (node instanceof Document) {
                doc = (Document) node;
            } else {
                doc = node.getOwnerDocument();
            }

            Source domSource = new DOMSource(doc);
            StringWriter result = new StringWriter();
            StreamResult streamResult = new StreamResult(result);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT,"yes"); 
            // Indenting not very nice int all xslt-engines, but well, its better then depending 
            // on a real xslt or lots of code.
            serializer.transform(domSource, streamResult); 
            return result.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }



    /**
     * same as above, but now the result is returned in a new Node and some less params.
     *
     * @param       node    the base context node.
     * @param       xslFile     the xslFile.
     * @return     the documentelement of the resulting xml (of the transformation)
     */

    public static Node transformNode(Node node, File xslFile, URIResolver uri) throws TransformerException {
        DOMResult res = new DOMResult();
        transformNode(node, xslFile, uri, res, null);
        return res.getNode();
    }

    /**
     * same as above, but now you can supply a params hashtable.
     */
    public static Node transformNode(Node node, File xslFile, URIResolver uri, Map params) throws TransformerException {
        DOMResult res = new DOMResult();
        transformNode(node, xslFile, uri, res, params);
        return res.getNode();
    }


    /**
     * same as above, but now the result is written to the writer.
     */
    public static void transformNode(Node node, File xslFile, URIResolver uri, Writer out) throws TransformerException {
        transformNode(node, xslFile, uri, out, null);
    }
    /**
     * same as above, but now the result is written to the writer and you can use params.
     */
    public static void transformNode(Node node, File xslFile, URIResolver uri, Writer out, Map params) throws TransformerException {
        if (log.isDebugEnabled()) log.trace("transforming: " + node.toString() + " " + params);
        // UNICODE works like this...
        StringWriter res = new StringWriter();
        transformNode(node, xslFile, uri, new javax.xml.transform.stream.StreamResult(res),  params);
        if (log.isDebugEnabled()) log.trace("transformation result " + res.toString());
        try {
            out.write(res.toString());
        } catch (java.io.IOException e) {
            log.error(e.toString());
        }
        //new StreamResult(out), null);
    }

    public static Node transformNode(Node node, String xslFile, URIResolver uri, Writer out, Map params) throws TransformerException {
        DOMResult res = new DOMResult();
        transformNode(node, uri.resolveToFile(xslFile), uri, res, params);
        return res.getNode();
    }


    /**
     * transforms an attribute. A attributeTemplate is used to place values. with { and } people can place simple xpaths to calculate
     * values.
     * @param       context         the contextnode
     * @param       attributeTemplate       the template to evaluate.
     * @return     a string with the result.
     */
    public static String transformAttribute(Node context, String attributeTemplate) {
        return transformAttribute(context,attributeTemplate,false,null);
    }

    /**
     * same as above, but now you can supply if the given attributeTemplate is already a xpath or not. (Default should be false).
     */
    public static String transformAttribute(Node context, String attributeTemplate, boolean plainTextIsPath) {
        return transformAttribute(context,attributeTemplate,plainTextIsPath,null);
    }

    /**
       Executes an attribute template.
       example node:
       <person state='bad'><name>Johnny</name></person>
       example template:
       "hi there {person/name}, you're looking {person/@state}"

       @param context the Node on which any xpaths are fired.
       @param attributeTemplate the String containting an attribute template.
       @param plainTextIsXpath true means that if the template doesn't contain
       any curly braces, the template is assumed to be a valid xpath (instead
       of plain data). Else the template is assumed to be a valid attribute template.
    */
    public static String transformAttribute(Node context, String attributeTemplate, boolean plainTextIsXpath, Map params) {
        if (attributeTemplate==null) return null;
        StringBuffer result = new StringBuffer();
        String template = fillInParams(attributeTemplate, params);
        if (plainTextIsXpath && template.indexOf("{") == -1){template = "{" + template + "}";}
        java.util.StringTokenizer templateParts = new java.util.StringTokenizer(template,"{}",true);
        while (templateParts.hasMoreElements()){
            String part = templateParts.nextToken();
            if (part.equals("{") && templateParts.hasMoreElements()){
                part = templateParts.nextToken();
                part = selectSingleNodeText(context, part,"");
                result.append(part);
            } else if (part.equals("}")){
                // Nothing, go to the next part.
            } else{
                result.append(part);
            }
        }
        return result.toString();
    }

    /**
     * This method selects a single node using the given contextnode and xpath.
     * @param contextnode
     * @param xpath
     * @return    The found node.
     */
    public static Node selectSingleNode(Node contextnode, String xpath) {
        if (contextnode==null) throw new RuntimeException("Cannot execute xpath '" + xpath + "' on dom.Node that is null");
        try {
            return XPathAPI.selectSingleNode(contextnode, xpath);
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
            throw new RuntimeException(Logging.stackTrace(e));
        }
    }

    /**
     * This method selects a multiple nodes using the given contextnode and xpath.
     * @param contextnode
     * @param xpath
     * @return    The found nodes in a NodeList.
     */
    public static NodeList selectNodeList(Node contextnode, String xpath) {
        if (contextnode==null) throw new RuntimeException("Cannot execute xpath '" + xpath + "' on dom.Node that is null");
        try {
            return XPathAPI.selectNodeList(contextnode, xpath);
        } catch (Exception e) {
            log.error(Logging.stackTrace(e));
            throw new RuntimeException(Logging.stackTrace(e));
        }
    }


    /**
     * Below some handy string utils
     */

    /**
     * This method fills in params in a string. It uses the params hashtable to do a string replace.
     * if the name/value: username --> kars is in the hashtable, all $username occurences will be replaced with kars.
     * Note: #multipleReplace method is used for replacements.
     *
     * @param       text    the source text to be used
     * @param       params  the table with params (name/value pairs)
     * @return     The resulting string
     */
    public static String fillInParams(String text, Map params) {
        if (params==null) return text;
        Iterator i = params.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            // accept both $xxx and {$xxx}
            text = multipleReplace(text,"{$"+entry.getKey()+"}", (String) entry.getValue());
            text = multipleReplace(text,"$"+entry.getKey(), (String) entry.getValue());
        }
        return text;
    }

    /**
     * replaces single or multiple occurences of a string in a given source string.
     *
     * Note: if the searchfor-string contains the replacewith-string, no replacement is made. It would result in an inifinite loop!
     *
     * @param       text  the source text (the haystack)
     * @param       searchfor     the needle. the this we're looking for
     * @param       replacewith     the string which should be placed.
     */
    public static String multipleReplace(String text, String searchfor, String replacewith) {
        if (text==null || searchfor==null || replacewith==null) return null;
        if (searchfor.indexOf(replacewith)>-1) return text; // cannot replace, would create an infinite loop!
        int pos=-1;
        int len=searchfor.length();
        while ((pos=text.indexOf(searchfor))>-1) {
            text = text.substring(0,pos) + replacewith + text.substring(pos+len);
        }
        return text;
    }


    /**
     * (Not used) method to post (http-post) xml to a url.
     *
     * Since it is 'not used' I made i private for the moment.
     *
     * @param       xml     The main node which should be posted.
     * @param       url     The destination url
     * @return     The resulting string sent from the destination after sending.
     */
    private static String postXml(Node xml, String url) throws Exception {
        String inputString = getXML(xml);

        URL downeyjrURL = new URL(url);
        HttpURLConnection c = (HttpURLConnection)(downeyjrURL.openConnection());
        c.setDoOutput(true);
        PrintWriter out = new PrintWriter(c.getOutputStream());
        // Here's whether the parameter is set.
        // TODO: replace in 1.4 with:
        //        out.println("xml=" + URLEncoder.encode(inputString,"UTF-8"));
        out.println("xml=" + URLEncoder.encode(inputString));
        out.close();

        BufferedReader in2 = new BufferedReader(new
                                                InputStreamReader(c.getInputStream()));

        String outputstr = "";
        String inputLine;
        while((inputLine = in2.readLine()) != null)
            outputstr += inputLine;
        in2.close();
        return outputstr;
    }
}
