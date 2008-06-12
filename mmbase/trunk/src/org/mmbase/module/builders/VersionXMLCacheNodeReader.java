/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.DocumentReader;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * @javadoc
 * @deprecated is this (cacheversionfile) used? seems obsolete now
 * @author Daniel Ockeloen
 * @version $Id: VersionXMLCacheNodeReader.java,v 1.10 2008-06-12 11:25:10 michiel Exp $
 */
class VersionXMLCacheNodeReader {

    private static Logger log = Logging.getLoggerInstance(VersionCacheNode.class.getName());
    Document document;
    Versions parent;

    public VersionXMLCacheNodeReader(String filename) {
        try {
            DocumentBuilder db = DocumentReader.getDocumentBuilder(false);
            File file = new File(filename);
            if (!file.exists()) {
                log.error("no cache version " + filename + " found)");
            }
            document = db.parse(file);
        }
        catch (SAXException e) {
            log.error("", e);
        }
        catch (IOException e) {
            log.error("", e);
        }
    }

    public void setBuilder(Versions parent) {
        this.parent = parent;
    }

    /**
    */
    public Hashtable<String, Vector<VersionCacheNode>> getCacheVersions(Hashtable<String, Vector<VersionCacheNode>> handlers) {
        Node n1 = document.getFirstChild();
        if (n1.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            n1 = n1.getNextSibling();
        }
        if (n1 != null) {
            Node n2 = n1.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeName().equals("cacheversion")) {

                    // decode all the needed values in the replace itself
                    NamedNodeMap nm = n2.getAttributes();
                    if (nm != null) {
                        Node n3 = nm.getNamedItem("name");
                        if (n3 != null) {
                            String name = n3.getNodeValue();
                            if (log.isDebugEnabled()) log.debug("name = " + name);
                            // find the node
                            MMObjectNode versionnode = null;
                            String query = "name=='" + name + "'+type=='cache'";
                            Enumeration<MMObjectNode> b = parent.search(query);
                            if (b.hasMoreElements()) {
                                versionnode = b.nextElement();
                            }
                            if (log.isDebugEnabled()) log.debug("versionnode=" + versionnode);
                            VersionCacheNode cnode = new VersionCacheNode(parent.getMMBase());
                            cnode.setVersionNode(versionnode);

                            n3 = n2.getFirstChild();
                            while (n3 != null) {

                                if (n3.getNodeName().equals("when")) {
                                    // create new when node
                                    VersionCacheWhenNode wn = new VersionCacheWhenNode();
                                    nm = n3.getAttributes();
                                    if (nm != null) {
                                        Node n5 = nm.getNamedItem("type");
                                        if (n5 != null) {
                                            String types = n5.getNodeValue();
                                            StringTokenizer tok = new StringTokenizer(types, ",\n\r");
                                            while (tok.hasMoreTokens()) {
                                                String type = tok.nextToken();
                                                wn.addType(type);

                                                // place ourselfs in the call hash

                                                Vector<VersionCacheNode> subs = handlers.get(type);
                                                if (subs == null) {
                                                    subs = new Vector<VersionCacheNode>();
                                                    subs.addElement(cnode);
                                                    handlers.put(type, subs);
                                                } else {
                                                    subs.addElement(cnode);
                                                }
                                            }
                                        }
                                        n5 = nm.getNamedItem("node");
                                        if (n5 != null) {
                                            String nodes = n5.getNodeValue();
                                            StringTokenizer tok = new StringTokenizer(nodes, ",\n\r");
                                            while (tok.hasMoreTokens()) {
                                                String type = tok.nextToken();
                                                wn.addNode(type);
                                            }
                                        }
                                    }
                                    cnode.addWhen(wn);
                                }
                                n3 = n3.getNextSibling();
                            }
                        }
                    }
                }
                n2 = n2.getNextSibling();
            }
        }
        return handlers;
    }

    public Vector<Map<String,String>> getDefines() {
        Vector<Map<String,String>> results = new Vector<Map<String,String>>();
        Node n1 = document.getFirstChild();
        if (n1 != null) {
            Node n2 = n1.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeName().equals("define")) {
                    Map<String,String> rep = new Hashtable<String,String>();

                    // decode all the needed values in the replace itself
                    NamedNodeMap nm = n2.getAttributes();
                    if (nm != null) {
                        Node n3 = nm.getNamedItem("type");
                        if (n3 != null) {
                            rep.put("type", n3.getNodeValue());
                        }
                        n3 = nm.getNamedItem("id");
                        if (n3 != null) {
                            rep.put("id", n3.getNodeValue());
                        }

                        n3 = nm.getNamedItem("width");
                        if (n3 != null) {
                            rep.put("width", n3.getNodeValue());
                        }

                        n3 = nm.getNamedItem("height");
                        if (n3 != null) {
                            rep.put("height", n3.getNodeValue());
                        }

                        n3 = nm.getNamedItem("src");
                        if (n3 != null) {
                            rep.put("src", n3.getNodeValue());
                        }

                        n3 = nm.getNamedItem("value");
                        if (n3 != null) {
                            rep.put("value", n3.getNodeValue());
                        }
                        n3 = nm.getNamedItem("file");
                        if (n3 != null) {
                            rep.put("valuefile", n3.getNodeValue());
                        }

                    }

                    results.addElement(rep);
                }
                n2 = n2.getNextSibling();
            }
        }
        return results;
    }

}
