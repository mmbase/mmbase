/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.xml.Generator;
import org.mmbase.cache.xslt.FactoryCache;
import org.mmbase.cache.xslt.TemplateCache;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


/**
 * Make XSL Transformations
 *
 * @move org.mmbase.util.xml
 * @author Case Roole, cjr@dds.nl
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class XSLTransformer {
    private static final Logger log = Logging.getLoggerInstance(XSLTransformer.class);

    public static final String NS_AWARE = "org.mmbase.util.XSLTransformer.namespaceAware";

    /**
     * Transform an XML document using a certain XSL document.
     *
     * @param xmlPath Path to XML file
     * @param xslPath Path to XSL file
     * @return String with converted XML document
     */
    public static String transform(String xmlPath, String xslPath) {
        return transform(xmlPath, xslPath, false);
    }

    /**
     * Transform an XML document using a certain XSL document, on
     * MMBase specic way (error handling, entitity resolving, uri
     * resolving, logging), and write it to string, which optionally can be
     * 'cut'.
     *
     * @param xmlPath Path to XML file
     * @param xslPath Path to XSL file
     * @param cutXML if <code>true</code>, cuts the &lt;?xml&gt; line that normally starts an
     *               xml document
     * @return String with converted XML document
     *
     *
     */
    public static  String transform(String xmlPath, String xslPath, boolean cutXML) {
        try {
            StringWriter res = new StringWriter();
            transform(new File(xmlPath), new File(xslPath), new StreamResult(res), null, true);
            String s = res.toString();
            int n = s.indexOf("\n");
            if (cutXML && s.length() > n) {
                s = s.substring(n + 1);
            }
            return s;
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error(Logging.stackTrace(e));
            return "Error during XSLT tranformation: "+e.getMessage();
        }
    }

    /**
     * This is the base function which calls the actual XSL
     * transformations. Performs XSL transformation on MMBase specific
     * way (using MMBase cache, and URIResolver).
     * @javadoc
     *
     * @since MMBase-1.6
     */
    public static void transform(Source xml, File xslFile, Result result, Map<String, Object> params) throws TransformerException {
        transform(xml, xslFile, result, params, true);
    }

    /**
     * This is the base function which calls the actual XSL
     * transformations. Performs XSL transformation on MMBase specific
     * way (using MMBase cache, and URIResolver).
     * @param xml The source XML
     * @param xslFile The XSL which must be used for the transformation
     * @param result  The XSL out will be written to this
     * @param params  <code>null</code> or a Map to XSL transformations parameters
     * @param considerDir If <code>true</code>, an URIResolver will be instantiated which can resolve entities relative to the XSL file.
     *
     * @since MMBase-1.6
     */
    public static void transform(Source xml, File xslFile, Result result, Map<String, Object> params, boolean considerDir) throws TransformerException {
        try {
            transform(xml, xslFile.toURL(), result, params, considerDir);
        } catch (java.net.MalformedURLException mfe) {
            throw new TransformerException(mfe.getMessage(), mfe);
        }
    }

    /**
     * @since MMBase-1.8
     */
    public static void transform(Source xml, URL xslFile, Result result, Map<String, Object> params) throws TransformerException {
        transform(xml, xslFile, result, params, true);
    }

    /**
     * @since MMBase-1.8
     */
    public static void transform(Source xml, URL xslFile, Result result, Map<String, Object> params, boolean considerDir) throws TransformerException {
        Source xsl;
        try {
            xsl = new StreamSource(xslFile.openStream());
        } catch (IOException ioe) {
            throw new TransformerException(ioe.getMessage(), ioe);
        }
        try {
            xsl.setSystemId(xslFile.toString());
        } catch (Exception e) {
        }

        URIResolver uriResolver;
        if (considerDir) {
            try {
                uriResolver = new org.mmbase.util.xml.URIResolver(new URL(xslFile, "."));
            } catch (java.net.MalformedURLException mfe) {
                // oddd..
                throw new TransformerException(mfe.getMessage(), mfe);
            }
        } else {
            uriResolver = new org.mmbase.util.xml.URIResolver();
        }
        transform(xml, xsl, uriResolver, result, params);
    }

    /**
     * @since MMBase-1.9.7
     */
    public static void transform(Source xml, Source xsl, Result result, Map<String, Object> params) throws TransformerException {
        transform(xml, xsl, new org.mmbase.util.xml.URIResolver(), result, params );
    }

    /**
     * @since MMBase-1.9.7
     */
    protected static void transform(Source xml, Source xsl, URIResolver uriResolver, Result result, Map<String, Object> params) throws TransformerException {
        TemplateCache cache = TemplateCache.getCache();

        Templates cachedXslt = cache.getTemplates(xsl, uriResolver);
        if (log.isDebugEnabled()) {
            // log.debug("Size of cached XSLT " + SizeOf.getByteSize(cachedXslt) + " bytes");
            log.debug("Size of URIResolver " + SizeOf.getByteSize(uriResolver) + " bytes");
            log.debug("template cache sze " + cache.size() + " entries");
        }
        if (cachedXslt == null) {
            TransformerFactory tf = FactoryCache.getCache().getFactory(uriResolver);
            tf.setURIResolver(uriResolver);
            cachedXslt = tf.newTemplates(xsl);
            if (cachedXslt == null) {
                log.warn("Could not create template for " + uriResolver + ": " + xsl.getSystemId());
            }
            cache.put(xsl, cachedXslt, uriResolver);
        } else {
            if (log.isDebugEnabled()) log.debug("Used xslt from cache with " + xsl.getSystemId());
        }
        Transformer transformer = cachedXslt.newTransformer();
        transformer.setURIResolver(uriResolver);

        //Transformer transformer = TransformerFactory.newInstance().newTransformer();
        if (log.isDebugEnabled()) {
            log.debug("Size of transformer " + SizeOf.getByteSize(transformer) + " bytes");
        }
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (log.isDebugEnabled()) log.debug("setting param " + entry.getKey() + " to " + entry.getValue());
                transformer.setParameter(entry.getKey(), entry.getValue());
            }
        }

        // Register EntityResolver with the XML source
        Source xmlSource = xml;
        if (!(xml instanceof DOMSource)) {
            // For StreamSource and other source types, wrap in SAXSource with EntityResolver
            try {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setNamespaceAware(true);
                XMLReader xmlReader = spf.newSAXParser().getXMLReader();
                xmlReader.setEntityResolver(new org.mmbase.util.xml.EntityResolver(true));
                InputSource inputSource = new InputSource(xml.getSystemId());
                if (xml instanceof StreamSource) {
                    StreamSource ss = (StreamSource) xml;
                    if (ss.getInputStream() != null) {
                        inputSource.setByteStream(ss.getInputStream());
                    } else if (ss.getReader() != null) {
                        inputSource.setCharacterStream(ss.getReader());
                    }
                }
                xmlSource = new SAXSource(xmlReader, inputSource);
            } catch (Exception e) {
                log.warn("Could not set up EntityResolver for XML source: " + e.getMessage());
                // Fall back to original source
                xmlSource = xml;
            }
        }

        transformer.transform(xmlSource, result);
    }

    /**
     * Performs XSL Transformation on XML-file which is parsed MMBase
     * specifically (using MMBasse EntityResolver and Errorhandler).
     * @javadoc
     *
     * @since MMBase-1.6
     */
    public static void transform(File xmlFile, File xslFile, Result result, Map<String,Object> params, boolean considerDir) throws TransformerException, ParserConfigurationException, java.io.IOException, org.xml.sax.SAXException {
        // create the input xml.
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        // turn validating on
        boolean validation = ! "false".equals(System.getProperty("org.mmbase.XSLTransformer.validation"));
        if (! validation) {
            log.service("disabled validation");
        }
        org.xml.sax.EntityResolver resolver = new org.mmbase.util.xml.EntityResolver(validation);
        dfactory.setNamespaceAware(true);
        if (params != null && Boolean.FALSE.equals(params.get(NS_AWARE))) {
            dfactory.setNamespaceAware(false);
        }
        DocumentBuilder db = dfactory.newDocumentBuilder();

        org.xml.sax.ErrorHandler handler = new org.mmbase.util.xml.ErrorHandler();
        db.setErrorHandler(handler);
        db.setEntityResolver(resolver);
        org.w3c.dom.Document xmlDoc = db.parse(xmlFile);
        Source s = new DOMSource(xmlDoc);
        s.setSystemId(xmlFile.toURL().toString());
        transform(s, xslFile, result, params, considerDir);
    }


    /**
     * Can be used to transform a directory of XML-files. Of course the result must be written to files too.
     *
     * The transformations will be called with a paramter "root" which
     * points back to the root directory relatively. You need this
     * when all your transformations results (probably html's) need to
     * refer to the same file which is relative to the root of the transformation.
     * @javadoc
     *
     * @since MMBase-1.6
     */
    public static void transform(File xmlDir, File xslFile, File resultDir, boolean recurse, Map<String,Object> params, boolean considerDir) throws TransformerException, ParserConfigurationException, java.io.IOException, org.xml.sax.SAXException {
        if (! xmlDir.isDirectory()) {
            throw  new TransformerException("" + xmlDir + " is not a directory");
        }
        if (! resultDir.exists()) {
            resultDir.mkdir();
        }
        if (! resultDir.isDirectory()) {
            throw  new TransformerException("" + resultDir + " is not a directory");
        }
        if (params == null) params = new HashMap<String,Object>();

        List<String> exclude = (List<String>) params.get("exclude");

        File[] files = xmlDir.listFiles();
        for (File element : files) {
            if (exclude.contains(element.getName())) continue;

            if (recurse && element.isDirectory()) {
                if ("CVS".equals(element.getName())) continue;
                File resultSubDir = new File(resultDir, element.getName());
                Map<String,Object> myParams;
                if (params == null) {
                    myParams = new HashMap<String,Object>();
                } else {
                    myParams = new HashMap<String,Object>(params);
                }

                if (myParams.get("root") == null) {
                    myParams.put("root", "../");
                } else {
                    if ("./".equals(myParams.get("root"))) {
                        myParams.put("root", "../");
                    } else {
                        myParams.put("root", myParams.get("root") + "../");
                    }
                }
                log.info("Transforming directory " + element + " (root is " + myParams.get("root") + ")");
                transform(element, xslFile, resultSubDir, recurse, myParams, considerDir);
            } else {
                if (! element.getName().endsWith(".xml")) continue;
                String fileName = element.getName();
                fileName = fileName.substring(0, fileName.length() - 4);
                params.put("filename", fileName);
                String extension = (String) params.get("extension");
                if (extension == null) extension = "html";
                File resultFile = new File(resultDir, fileName  + "." + extension);
                if (resultFile.lastModified() > element.lastModified()) {
                    log.info("Not transforming " + element + " because " + resultFile + " is up to date");
                } else {
                    log.info("Transforming file " + element + " to " + resultFile);
                    try {
                        Result res;
                        if ("true".equals(params.get("dontopenfile"))) {
                            res = new StreamResult(System.out);
                        } else {
                            res = new StreamResult(resultFile);
                        }
                        transform(element, xslFile, res, params, considerDir);
                    } catch (Exception e) {
                        log.error(e.toString());
                        log.error(Logging.stackTrace(e));
                    }
                }
            }
        }
    }


    public static Result getResult(String[] argv) throws Exception {
        if (argv.length > 2) {
            FileOutputStream stream = new FileOutputStream(argv[2]);
            Writer f = new OutputStreamWriter(stream,"utf-8");
            return new StreamResult(f);
        } else {
            return new StreamResult(new OutputStreamWriter(System.out, "utf-8"));
        }
    }


    /**
     * Invocation of the class from the commandline for testing/building
     */
    public static void main(String[] argv) {

        // log.setLevel(org.mmbase.util.logging.Level.DEBUG);
        if (argv.length < 2) {
            log.info("Use with two arguments: <xslt-file>|SER <xml-inputfile|node-number> [xml-outputfile]");
            log.info("Use with tree arguments: xslt-file xml-inputdir xml-outputdir [key=value options]");
            log.info("special options can be:");
            log.info("   usecache=true:     Use the Template cache or not (to speed up)");
            log.info("   namespaceaware=true:   ");
            log.info("   exclude=<filename>:  File/directory name to exclude (can be used multiple times");
            log.info("   extension=<file extensions>:  File extensions to use in transformation results (defaults to html)");

            log.info("Other options are passed to XSL-stylesheet as parameters.");

        } else {
            boolean namespaceaware = true;
            Map<String, Object> params = new HashMap<String, Object>();
            if (argv.length > 3) {
                for (int i = 3; i<argv.length; i++) {
                    String key = argv[i];
                    String value = "";
                    int p = key.indexOf("=");
                    if (p > 0) {
                        if (p<key.length()-1) value = key.substring(p+1);
                        key = key.substring(0, p);
                    }
                    if (key.equals("namespaceaware")) {
                        namespaceaware = value.equals("true");
                        params.put(NS_AWARE, namespaceaware);
                    } else if (key.equals("usecache")) {
                        TemplateCache.getCache().setActive(value.equals("true"));
                    } else if (key.equals("exclude")) {
                        if (params.get("exclude") == null) {
                            params.put("exclude", new ArrayList<String>());
                        }
                        List<String> excludes = (List<String>) params.get("exclude");
                        excludes.add(value);
                    } else {
                        params.put(key, value);
                    }
                }
            }
            try {
                File in = new File(argv[1]);
                if (in.exists()) {
                    if (in.isDirectory()) {
                        log.info("Transforming directory " + in);
                        long start = System.currentTimeMillis();
                        try {
                            transform(in, new File(argv[0]), new File(argv[2]), true, params, true);
                        } catch (Exception e) {
                            log.error("Error: " + e.toString());
                        }
                        log.info("Transforming took " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
                    } else {
                        log.info("Transforming file " + argv[1]);
                        transform(new File(argv[1]), new File(argv[0]), getResult(argv), params, true);
                    }
                } else {
                    log.debug("" + in + " does not exist, interpreting it as a node, connecting using RMMCI");
                    Result result = getResult(argv);
                    String nodeNumber = argv[1];
                    CloudContext cc = ContextProvider.getDefaultCloudContext();
                    Cloud cloud = cc.getCloud("mmbase", "anonymous", null);
                    params.put("cloud", cloud);
                    Node node = cloud.getNode(nodeNumber);
                    DocumentBuilder documentBuilder = org.mmbase.util.xml.DocumentReader.getDocumentBuilder();
                    Generator generator = new Generator(documentBuilder, cloud);
                    generator.setNamespaceAware(namespaceaware);
                    generator.add(node, node.getNodeManager().getField("body"));
                    //generator.add(node);
                    //log.info("" + node.getXMLValue("body").getDocumentElement().getNamespaceURI());
                    generator.add(node.getRelations("idrel"));
                    NodeList relatedNodes = node.getRelatedNodes("object", "idrel", "both");
                    generator.add(relatedNodes);
                    log.debug("transforming");
                    transform(new DOMSource(generator.getDocument()),  new File(argv[0]), result, params, true);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                Throwable cause = ex.getCause();
                while (cause != null) {
                    log.error("CAUSE" + cause.getMessage(), cause);
                    cause = cause.getCause();
                }
            }
        }
    }
}
