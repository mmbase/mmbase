/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.richtext.processors.xml;
import org.mmbase.datatypes.processors.xml.Modes;
import org.mmbase.datatypes.processors.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.util.xml.Generator;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.richtext.transformers.XmlField;
import org.mmbase.util.xml.XMLWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.*;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;


/**
 * This class implements the `get' for `mmxf' fields.
 *
 * @author Michiel Meeuwissen
 * @version $Id: MmxfGetString.java,v 1.8 2008-02-14 15:32:22 michiel Exp $
 * @since MMBase-1.8
 */

public class MmxfGetString implements  Processor {
    private static final Logger log = Logging.getLoggerInstance(MmxfGetString.class);

    private static final int serialVersionUID = 1;

    static DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();
            org.xml.sax.ErrorHandler handler = new XMLErrorHandler();
            documentBuilder.setErrorHandler(handler);
            documentBuilder.setEntityResolver( new XMLEntityResolver());
            return documentBuilder;
        } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce.getMessage(), pce);
        }
    }

    static protected Generator getGenerator(final Cloud cloud) {
        return new Generator(getDocumentBuilder(), cloud) {
            // in 1.8 this is @Override, in 1.9, this is ignored. So this can be dropped if no 1.8
            // compatibility needed any more.
            protected void setIdAttribute(Element object, String name) {
                object.setIdAttribute(name, true);
            }

        };
    }
    /**
     * Returns a 'objects' Document containing the node with the mmxf field plus all idrelated objects
     */
    public static Document getDocument(final Node node, final Field field)  {

        log.debug("Getting document for node " + node.getNumber());
        Generator generator = getGenerator(node.getCloud());
        generator.setNamespaceAware(true);
        generator.add(node, field);
        org.mmbase.bridge.NodeList relatedNodes = node.getRelatedNodes("object", "idrel", "destination");
        if (log.isDebugEnabled()) {
            log.debug("Idrelated " + relatedNodes);
        }
        generator.add(relatedNodes);
        org.mmbase.bridge.RelationList relationsNodes = node.getRelations("idrel", node.getCloud().getNodeManager("object"), "destination");
        if (log.isDebugEnabled()) {
            log.debug("Idrelations " + relationsNodes);
        }
        generator.add(relationsNodes);

        // TODO. With advent of 'blocks' one deeper level must be queried here (see node.body.jspx)
        return generator.getDocument();
    }


    public Object process(Node node, Field field, Object value) {
        if (log.isDebugEnabled()) {
            log.debug("Getting " + field + " from " + node + " as a String");
            log.debug("Received value as " + value.getClass());
        }

        try {
            String stringMode = (String) node.getCloud().getProperty(Cloud.PROP_XMLMODE);
            int mode = stringMode == null ? Modes.XML : Modes.getMode(stringMode);
            switch(mode) {
            case Modes.KUPU: {
                // this is actually not really used, at the moment is done on node.body.jspx
                log.debug("Generating kupu-compatible XML for " + value);
                Document xml = getDocument(node, field);
                java.net.URL u = ResourceLoader.getConfigurationRoot().getResource("xslt/mmxf2kupu.xslt");
                StringWriter res = new StringWriter();
                // TODO: XSL transformation parameter stuff must be generalized (not only cloud, but only e.g. request specific stuff must be dealt with).
                Map params = new HashMap();
                params.put("cloud", node.getCloud());
                XSLTransformer.transform(new DOMSource(xml), u, new StreamResult(res), params);
                return res.toString();
            }
            case Modes.WIKI: {
                log.debug("Generating 'wiki'  for " + value);
                Document xml = getDocument(node, field);
                java.net.URL u = ResourceLoader.getConfigurationRoot().getResource("xslt/2rich.xslt");
                StringWriter res = new StringWriter();
                Map params = new HashMap();
                params.put("cloud", node.getCloud());
                XSLTransformer.transform(new DOMSource(xml), u, new StreamResult(res), params);
                return res.toString();
            }
            case Modes.FLAT: {
                Generator generator = getGenerator(node.getCloud());
                generator.setNamespaceAware(true);
                generator.add(node, field);
                Document xml = generator.getDocument();
                java.net.URL u = ResourceLoader.getConfigurationRoot().getResource("xslt/mmxf2rich.xslt");
                StringWriter res = new StringWriter();
                Map params = new HashMap();
                params.put("cloud", node.getCloud());
                XSLTransformer.transform(new DOMSource(xml), u, new StreamResult(res), params);
                return res.toString();
            }
            case Modes.PRETTYXML: {
                // get the XML from this thing....
                // javax.xml.parsers.DocumentBuilderFactory dfactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                // javax.xml.parsers.DocumentBuilder dBuilder = dfactory.newDocumentBuilder();
                // org.w3c.dom.Element xml = node.getXMLValue(field.getName(), dBuilder.newDocument());
                Document xml = node.getXMLValue(field.getName());

                if(xml != null) {
                    return XMLWriter.write(xml, true, true);
                }
                return "";

            }
            case Modes.DOCBOOK:
                // not implemented..
                return value;
            case Modes.XML:
            default:
                Document xml = node.getXMLValue(field.getName());

                if(xml != null) {
                    // make a string from the XML
                    return XMLWriter.write(xml, false, true);
                }
                return "";
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return value;
        }
    }

    public String toString() {
        return "get_MMXF";
    }

}
