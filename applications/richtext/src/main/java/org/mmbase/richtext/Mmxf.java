/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.richtext;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import org.mmbase.bridge.*;
import org.mmbase.util.ResourceLoader;
import java.io.*;

import org.mmbase.util.logging.*;

/**
 * Utilities related to the 'mmxf' rich field format of MMBase and bridge.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @see    org.mmbase.util.transformers.XmlField
 * @since  MMBase-1.8
 */
public class Mmxf {

    private static final Logger log = Logging.getLoggerInstance(Mmxf.class);

    public final static String NAMESPACE = "http://www.mmbase.org/xmlns/mmxf";
    public final static String DOCUMENTTYPE_PUBLIC =  "-//MMBase//DTD mmxf 1.1//EN";
    public final static String DOCUMENTTYPE_SYSTEM = "https://www.mmbase.org/dtd/mmxf_1_1.dtd";


    /**
     * Defaulting version of {@link #createTree(org.w3c.dom.Node, org.mmbase.bridge.Node, RelationManager, int, String, String, Writer)}.
     */
    public static org.mmbase.bridge.Node createTree(org.w3c.dom.Node node, RelationManager relationManager, int depth, Writer buf) {
        return createTree(node, null, relationManager, depth, "title", "body", buf);
    }


    protected static void exception(Writer buf, String message) {
        if (buf == null) {
            throw new IllegalArgumentException(message);
        } else {
            try {
                buf.write("ERROR: " + message + '\n');
            } catch (IOException ioe) {
                IllegalArgumentException e = new IllegalArgumentException(ioe.getMessage() + message);
                e.initCause(ioe);
                throw e;
            }
        }
    }

    protected static void debug(Writer buf, String message) {
        if (buf == null) {
            log.debug(message);
        } else {
            try {
                buf.write(message + '\n');
            } catch (IOException ioe) {
                IllegalArgumentException e = new IllegalArgumentException(ioe.getMessage() + message);
                e.initCause(ioe);
                throw e;
            }
        }
    }
    /**
     * Creates a a tree of Nodes from an mmxf DOM-Node. The (mmxf) document can of course be stored
     * in one MMXF field, but if it is large, you want to split it up in a tree of nodes. This can
     * be necessary in an import, where you would e.g. receive a complete book (or chapter) in one
     * XML document. You could e.g. (XSL) transform it to one huge MMXF and then feed it into this function. See also
     * {@link org.mmbase.util.transformers.XmlField} for creating MMXF from ASCII.
     *
     * If your MMXF is a Document, you must feed {@link org.w3c.dom.Document#getDocumentElement()}
     *
     * @param node Source DOM-Node (it's node-name must be 'mmxf' or 'section').
     * @param root Root node (to be used in index relations) Can be <code>null</code> in which case it will be set equal to the first node created.
     * @param relationManager Describes the object model in which it must be dispatched. Source and
     *        Destination must be of the same type, the relation must have a field 'pos'.
     * @param depth How far to dispatch. If -1 then it will be split up until no sections are
     *        remaining.
     * @param titleField The h-tag of the sections are written to this field.
     * @param xmlField   A new mmxf-document is created for the test and written to this field.
     * @param feedBack   A string buffer for feedback (can be e.g. used for logging or presenting on import-jsp).
     */
    public static org.mmbase.bridge.Node createTree(org.w3c.dom.Node node,
                                                    org.mmbase.bridge.Node root,  RelationManager relationManager, int depth, String titleField, String xmlField, Writer feedBack) {
        String nodeName = node.getNodeName();
        if (! (nodeName.equals("section") || nodeName.equals("mmxf"))) {
            exception(feedBack, "dom-Node must be a 'section' or 'mmxf' (but is a " + node.getNodeName() + ")");
            return null;
        }
        NodeManager nm = relationManager.getDestinationManager();
        org.w3c.dom.NodeList childs = node.getChildNodes();
        debug(feedBack, "Found " + childs.getLength() + " childs");
        int i = 0;

        debug(feedBack, "Importing " + nodeName);
        String title;
        if (nodeName.equals("section")) {
            if (childs.getLength() < 1) {
                exception(feedBack, "No child nodes! (should at least be a h-child)");
            }
            org.w3c.dom.Node h = childs.item(i);
            if (! h.getNodeName().equals("h")) {
                exception(feedBack, "No h-tag");
            }
            title = org.mmbase.util.xml.DocumentReader.getNodeTextValue(h);
            i++;
        } else {
            title = "Imported MMXF";
        }
        debug(feedBack, "Creating node with title '" + title + "'");
        Document mmxf = createMmxfDocument();
        while (i < childs.getLength()) {
            org.w3c.dom.Node next = childs.item(i);
            String name = next.getNodeName();
            if (name.equals("p") || name.equals("table") ||
                name.equals("ul") || name.equals("ol") ||
                (depth == 0 && name.equals("section"))) {
                org.w3c.dom.Node n = mmxf.importNode(next, true);
                debug(feedBack, "appending " + name);
                mmxf.getDocumentElement().appendChild(n);
            } else {
                debug(feedBack, "name is not p or table, but '" + name + "' breaking");
                break;
            }
            i++;
        }
        debug(feedBack, "# handled childs:" + i);
        // create the node.
        org.mmbase.bridge.Node newNode = nm.createNode();
        newNode.setStringValue(titleField, title);
        newNode.setXMLValue(xmlField, mmxf);
        newNode.commit();
        debug(feedBack, "created node " + newNode.getNumber());

        if (root == null) root = newNode;

        int pos = 1;
        while (i < childs.getLength()) {
            org.w3c.dom.Node next = childs.item(i);
            String name = next.getNodeName();
            debug(feedBack, "found  for " + i + " " + name);
            if (name.equals("section")) {
                org.mmbase.bridge.Node  destination = createTree(next, root, relationManager, depth > 0 ? depth -1 : depth, titleField, xmlField, feedBack);
                Relation relation = relationManager.createRelation(newNode, destination);
                relation.setIntValue("pos", pos);
                relation.setNodeValue("root", root);
                relation.commit();
                debug(feedBack, "Created relation " + newNode.getNumber() + " --" + pos + " -->" + destination.getNumber());
                pos++;
            } else {
                exception(feedBack, "Not a section, but a " + name);
            }
            i++;

        }
        debug(feedBack, "found " + pos + " subsections on node " + newNode.getNumber());
        return newNode;

    }

    /**
     * Creates an (empty) Mmxf Document
     */
    public static Document createMmxfDocument() {
        DocumentBuilder documentBuilder = org.mmbase.util.xml.DocumentReader.getDocumentBuilder();
        DOMImplementation impl = documentBuilder.getDOMImplementation();
        Document document = impl.createDocument(NAMESPACE,
                                                "mmxf",
                                                impl.createDocumentType("mmxf", DOCUMENTTYPE_PUBLIC, DOCUMENTTYPE_SYSTEM)
                                                );
        document.getDocumentElement().setAttribute("version", "1.1");
        return document;
    }


    /**
     * main for testing purposes.
     */
    public static void main(String[] argv) {
        try {
            CloudContext cc = ContextProvider.getDefaultCloudContext();
            Cloud cloud = cc.getCloud("mmbase", "class", null);

            if (argv.length == 0) {
                System.out.println("Usage:\n java " + Mmxf.class.getName() + " <-Dmmbase.defaultcloudcontext=rmi://...>  <fileName>");
                return;
            }
            ResourceLoader rc = ResourceLoader.getSystemRoot();

            System.out.println("" + rc);
            Document doc = rc.getDocument(argv[0]);

            System.out.println("Found cloud " + cloud.getUser().getIdentifier());
            RelationManager relationManager = cloud.getRelationManager("segments", "segments", "index");
            Writer writer = new BufferedWriter(new OutputStreamWriter(System.out));
            org.mmbase.bridge.Node node = Mmxf.createTree(doc.getDocumentElement(), relationManager, 3, writer);
            writer.flush();
            System.out.println("Created node " + node.getNumber());


        } catch (Exception e) {
            System.err.println("" + e);
        }
    }
}
