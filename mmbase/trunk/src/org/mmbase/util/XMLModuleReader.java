/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @javadoc
 * @move org.mmbase.util.xml
 * @rename ModuleReader
 * @duplicate extend from org.mmbase.util.xml.DocumentReader
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @version $Id: XMLModuleReader.java,v 1.15 2005-07-09 15:29:12 nklasens Exp $
 */
public class XMLModuleReader extends XMLBasicReader {

    /** Public ID of the Module DTD version 1.0 */
    public static final String PUBLIC_ID_MODULE_1_0 = "-//MMBase//DTD module config 1.0//EN";
    private static final String PUBLIC_ID_MODULE_1_0_FAULT = "-//MMBase/DTD module config 1.0//EN";
    private static final String PUBLIC_ID_MODULE_1_0_FAULT2 = "-//MMBase/ DTD module config 1.0//EN";
    /** Public ID of the most recent Module DTD */
    public static final String PUBLIC_ID_MODULE = PUBLIC_ID_MODULE_1_0;

    /** DTD resource filename of the most recent Module DTD */
    public static final String DTD_MODULE_1_0 = "module_1_0.dtd";
    /** DTD resource filename of the most recent Module DTD */
    public static final String DTD_MODULE = DTD_MODULE_1_0;

    /**
     * Register the Public Ids for DTDs used by XMLModuleReader
     * This method is called by XMLEntityResolver.
     * @since MMBase-1.7
     */
    public static void registerPublicIDs() {
        // various builder dtd versions
        XMLEntityResolver.registerPublicID(PUBLIC_ID_MODULE_1_0, DTD_MODULE_1_0, XMLModuleReader.class);

        // legacy public IDs (wrong, don't use these)
        XMLEntityResolver.registerPublicID(PUBLIC_ID_MODULE_1_0_FAULT, DTD_MODULE_1_0, XMLModuleReader.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_MODULE_1_0_FAULT2, DTD_MODULE_1_0, XMLModuleReader.class);
    }

    public XMLModuleReader(String filename) {
        super(filename, XMLModuleReader.class);
    }

    public XMLModuleReader(InputSource is) {
        super(is, XMLModuleReader.class);
    }

    /**
     * get the status of this builder
     */
    public String getStatus() {
        Element e = getElementByPath("module.status");
        return getElementValue(e);
    }

    /**
     * get the version of this application
     */
    public int getModuleVersion() {
        Element e = getElementByPath("module");
        String version = getElementAttributeValue(e, "version");
        int n = 0;
        if (version == null) {
            return n;
        } else {
            try {
                n = Integer.parseInt(version);
            } catch (Exception f) {
                n = 0;
            }
            return n;
        }
    }

    /**
     * get the version of this application
     */
    public String getModuleMaintainer() {
        Element e = getElementByPath("module");

        String tmp = getElementAttributeValue(e, "maintainer");
        if (tmp != null && !tmp.equals("")) {
            return (tmp);
        } else {
            return ("mmbase.org");
        }
    }

    /**
     * get the classfile of this builder
     */
    public String getClassFile() {
        Element e = getElementByPath("module.classfile");
        return getElementValue(e);
    }

    /**
     * get the optional resource url for the module
     * @return the url of the resource or null if no url was defined
     * @since MMBase-1.7
     **/
    public String getURLString(){
        Element e = getElementByPath("module.url");
        if (e != null){
            return getElementValue(e);
        }
        return null;
    }

    /**
     * Get the properties of this builder
     */
    public Hashtable getProperties() {
        Hashtable hash = new Hashtable();
        Element e = getElementByPath("module.properties");
        if (e != null) {
            for (Iterator iter = getChildElements(e, "property"); iter.hasNext();) {
                Element p = (Element) iter.next();
                String name = getElementAttributeValue(p, "name");
                String value = getElementValue(p);
                hash.put(name, value);
            }
        }
        return hash;
    }

}
