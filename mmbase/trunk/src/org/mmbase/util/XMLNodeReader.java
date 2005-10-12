
/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.util.*;

import org.mmbase.bridge.Field;
import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * This class reads a node from an exported application.
 * @application Applications
 * @move org.mmbase.util.xml
 * @rename ContextDepthReader
 * @duplicate extend from org.mmbase.util.xml.DocumentReader
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: XMLNodeReader.java,v 1.38 2005-10-12 00:54:25 michiel Exp $
 */
public class XMLNodeReader extends XMLBasicReader {
    private static final Logger log = Logging.getLoggerInstance(XMLNodeReader.class);


    private ResourceLoader path;

    /**
     * @since MMBase-1.8
     */
    public XMLNodeReader(InputSource is, ResourceLoader path) {
        super(is, false);
        this.path = path;
    }

    /**
     *
     */
    public String getExportSource() {
        Node n1 = document.getFirstChild();
        if (n1.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            n1 = n1.getNextSibling();
        }
        if (n1 != null) {
            NamedNodeMap nm = n1.getAttributes();
            if (nm != null) {
                Node n2 = nm.getNamedItem("exportsource");
                return (n2.getNodeValue());
            }
        }
        else {
            log.warn("exportsource attribute missing");
        }
        return null;
    }

    /**
     *
     */
    public int getTimeStamp() {
        Node n1 = document.getFirstChild();
        if (n1.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            n1 = n1.getNextSibling();
        }
        if (n1 != null) {
            NamedNodeMap nm = n1.getAttributes();
            if (nm != null) {
                Node n2 = nm.getNamedItem("timestamp");
                try {
                    java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddhhmmss", Locale.US);
                    int times = (int) (formatter.parse(n2.getNodeValue()).getTime() / 1000);
                    //int times=DateSupport.parsedatetime(n2.getNodeValue());
                    return times;
                } catch (java.text.ParseException e) {
                    log.warn("error retrieving timestamp: " + Logging.stackTrace(e));
                    return -1;
                }

            }
        }
        else {
            log.warn("timestamp attribute missing");
        }
        return -1;
    }

    /**
     *
     */
    public Vector getNodes(MMBase mmbase) {
        Vector nodes = new Vector();
        Node n1 = document.getFirstChild();
        if (n1.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            n1 = n1.getNextSibling();
        }
        while (n1 != null) {
            MMObjectBuilder bul = mmbase.getMMObject(n1.getNodeName());
            if (bul != null) {
                Node n2 = n1.getFirstChild();
                while (n2 != null) {
                    if (n2.getNodeName().equals("node")) {
                        NamedNodeMap nm = n2.getAttributes();
                        if (nm != null) {
                            Node n4 = nm.getNamedItem("owner");
                            MMObjectNode newNode = null;
                            if (n4 != null) {
                                newNode = bul.getNewNode(n4.getNodeValue());
                            } else {
                                newNode = bul.getNewNode("import");
                            }
                            n4 = nm.getNamedItem("alias");
                            if (n4 != null) {
                                log.info("Setting alias to " + n4.getNodeValue());
                                newNode.setAlias(n4.getNodeValue());
                            }
                            n4 = nm.getNamedItem("number");
                            try {
                                int num = Integer.parseInt(n4.getNodeValue());
                                newNode.setValue("number", num);
                            } catch (Exception e) {}
                            Node n5 = n2.getFirstChild();
                            while (n5 != null) {
                                if (n5.getNodeType() == Node.ELEMENT_NODE) {
                                    String key = n5.getNodeName();
                                    NodeList nl = n5.getChildNodes();
                                    StringBuffer res = new StringBuffer("");
                                    for (int i = 0; i < nl.getLength(); i++) {
                                        Node n = nl.item(i);
                                        if ((n.getNodeType() == Node.TEXT_NODE)
                                            || (n.getNodeType() == Node.CDATA_SECTION_NODE)) {
                                            res.append(n.getNodeValue().trim());
                                        }
                                    }
                                    String value = res.toString();

                                    int type = bul.getDBType(key);
                                    if (type != -1) {
                                        if (type == Field.TYPE_STRING || type == Field.TYPE_XML) {
                                            if (value == null) {
                                                value = "";
                                            }
                                            newNode.setValue(key, value);
                                            if (log.isDebugEnabled()) {
                                                log.debug("After value " + Casting.toString(newNode.getValue(key)));
                                            }
                                        } else if (type == Field.TYPE_NODE) {
                                            // do not really set it, because we need syncnodes later for this.
                                            newNode.storeValue("__" + key, value); // yes, this is hackery, I'm sorry.
                                            newNode.setValue(key, null);
                                        } else if (type == Field.TYPE_INTEGER) {
                                           try {
                                                newNode.setValue(key, Integer.parseInt(value));
                                            } catch (Exception e) {
                                                log.warn("error setting integer-field '" + key + "' to '" + value + "' because " + e);
                                                newNode.setValue(key, -1);
                                            }
                                        } else if (type == Field.TYPE_FLOAT) {
                                            try {
                                                newNode.setValue(key, Float.parseFloat(value));
                                            } catch (Exception e) {
                                                log.warn("error setting float-field '" + key + "' to '" + value + "' because " + e);
                                                newNode.setValue(key, -1);
                                            }
                                        } else if (type == Field.TYPE_DOUBLE) {
                                            try {
                                                newNode.setValue(key, Double.parseDouble(value));
                                            } catch (Exception e) {
                                                log.warn("error setting double-field '" + key + "' to '" + value + "' because " + e);
                                                newNode.setValue(key, -1);
                                            }
                                        } else if (type == Field.TYPE_LONG) {
                                            try {
                                                newNode.setValue(key, Long.parseLong(value));
                                            } catch (Exception e) {
                                                log.warn("error setting long-field '" + key + "' to '" + value + "' because " + e);
                                                newNode.setValue(key, -1);
                                            }
                                        } else if (type == Field.TYPE_DATETIME) {                                            
                                            newNode.setValue(key, Casting.toDate(value));
                                        } else if (type == Field.TYPE_BINARY) {
                                            NamedNodeMap nm2 = n5.getAttributes();
                                            Node n7 = nm2.getNamedItem("file");
                                            try {
                                                newNode.setValue(key, readBytesStream(n7.getNodeValue()));
                                            } catch (IOException ioe) {
                                                log.warn("Could not set field " + key + " " + ioe);
                                            }
                                        } else {
                                            log.error("CoreField not found for #" + type + " was not known for field with name: '" + key + "' and with value: '" + value + "'");
                                        }
                                    }
                                }
                                n5 = n5.getNextSibling();
                            }
                            nodes.add(newNode);
                        }
                    }
                    n2 = n2.getNextSibling();
                }
            } else {
                log.error("Could not find builder with name: " + n1.getNodeName() + "'");
            }
            n1 = n1.getNextSibling();
        }
        return nodes;
    }

    private byte[] readBytesStream(String resourceName) throws IOException {
        InputStream stream = path.getResourceAsStream(resourceName);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c = stream.read();
        while (c != -1) {
            buffer.write(c);
            c = stream.read();
        }
        return buffer.toByteArray();
    }
}
