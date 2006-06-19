/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.util.*;
import java.net.URL;
import java.io.IOException;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.w3c.dom.Element;
/**
 * This class reads configuration files for utilities, that are
 * placed in /config/utils/.
 *
 * A typical way to use it may be like so:
 <pre>
    private UtilReader.PropertiesMap utilProperties = new UtilReader("myutil.xml", new Runnable() { public void run() { init();}}).getProperties();
    private void init() {
      // use utilProperties
    }
    {
      init();
    }
 </pre>
 * This produces a 'watched map' utilProperties. Every time the underlying config file(s) are changed 'init' is called. Init is called on instantation of the surrounding class too.
 *
 * @since MMBase-1.6.4
 * @author Rob Vermeulen
 * @author Michiel Meeuwissen
 * @version $Id: UtilReader.java,v 1.21 2006-06-19 05:55:28 michiel Exp $
 */
public class UtilReader {

    private static final Logger log = Logging.getLoggerInstance(UtilReader.class);

    public static final String CONFIG_UTILS = "utils";

    /** Public ID of the Utilities config DTD version 1.0 */
    public static final String PUBLIC_ID_UTIL_1_0 = "-//MMBase//DTD util config 1.0//EN";
    /** DTD resource filename of the Utilities config DTD version 1.0 */
    public static final String DTD_UTIL_1_0 = "util_1_0.dtd";

    /** Public ID of the most recent Utilities config DTD */
    public static final String PUBLIC_ID_UTIL = PUBLIC_ID_UTIL_1_0;
    /** DTD respource filename of the most recent Utilities config DTD */
    public static final String DTD_UTIL = DTD_UTIL_1_0;

    /**
     * Register the Public Ids for DTDs used by UtilReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_UTIL_1_0, DTD_UTIL_1_0, UtilReader.class);
    }

    private static final Map utilReaders = new HashMap();     // file-name -> utilreader

    /**
     * Returns a UtilReader for the given fileName. When you use this, the UtilReader instance will be cached.
     *
     * @since MMBase-1.8
     */

    public static UtilReader get(String fileName) {
        UtilReader utilReader = (UtilReader) utilReaders.get(fileName);
        if (utilReader == null) {
            synchronized(utilReaders) {
                utilReader = new UtilReader(fileName);
                utilReaders.put(fileName, utilReader);
            }
        }
        return utilReader;
    }

    static {
        // doesnt startup, probably because of cyclic referecnes, if this happens in DocumentReader itself.
        DocumentReader.utilProperties = UtilReader.get("documentreader.xml").getProperties();
    }

    private class UtilFileWatcher extends ResourceWatcher {
        private ResourceWatcher wrappedWatcher;
        public UtilFileWatcher(ResourceWatcher f) {
            super(); // true: keep reading.
            wrappedWatcher = f;
        }

        public void onChange(String f) {
            readProperties(f);
            if (wrappedWatcher != null) {
                wrappedWatcher.onChange(f);
            }
        }
    }

    private final Map properties = new HashMap();
    private final ResourceWatcher watcher;
    private final String file;


    /**
     * Instantiates a UtilReader for a given configuration file in <config>/utils. If the configuration file is used on more spots, then you may consider
     * using the static method {@link #get(String)} in stead.
     *
     * @param fileName The name of the property file (e.g. httppost.xml).
     */
    public UtilReader(String fileName) {
        file = CONFIG_UTILS + "/" + fileName;
        readProperties(file);
        watcher = new UtilFileWatcher(null);
        watcher.add(file);
        watcher.start();

    }
    /**
     * Produces a UtilReader for the given resource name.
     * @param fileName a Resource name relative to config/utils
     * @param w A unstarted ResourceWatcher without files. (It will be only be called from the
     *          filewatcher in this reader). It defines what must happen if something changes in the util's
     *          configuration. Since you probably don't need the resource name for that any more, you
     *          can also simply use {@link #UtilReader(String, Runnable)}
     * @since MMBase-1.8
     */
    public UtilReader(String fileName, ResourceWatcher w) {
        file =  CONFIG_UTILS + "/" + fileName;
        readProperties(file);
        watcher = new UtilFileWatcher(w);
        watcher.add(file);
        watcher.start();

    }


    /**
     * Produces a UtilReader for the given resource name.
     * @param resourceName a Resource name relative to config/utils
     * @param onChange     A Runnable defining what must happen if something changes.
     * @since MMBase-1.8
     */
    public UtilReader(String resourceName, final Runnable onChange) {
        this(resourceName, new ResourceWatcher() {
                public void onChange(String name) {
                    onChange.run();
                }
            }
             );
    }


    /**
     * Get the properties of this utility.
     */
    public PropertiesMap getProperties() {
        return new PropertiesMap(properties);
    }

    /**
     * Reports whether the configured resource (in the constructor) is actually backed. If not,
     * getProperties will certainly return an empty Map.
     * @since MMBase-1.8.1
     */
    public boolean resourceAvailable() {
        try {
            return ResourceLoader.getConfigurationRoot().getResource(file).openConnection().getDoInput();
        } catch (IOException io) {
            return false;
        }
    }

    protected void readProperties(String s) {
        properties.clear();

        ResourceLoader configLoader = ResourceLoader.getConfigurationRoot();
        List configList = configLoader.getResourceList(s);
        log.service("Reading " + configList);
        Iterator configs = configList.iterator();
        while (configs.hasNext()) {
            URL url = (URL) configs.next();
            org.xml.sax.InputSource is;
            try {
                is = ResourceLoader.getInputSource(url);
            } catch (IOException ioe) {
                // input source does not exist
                log.debug(ioe.getMessage() + " for " + url);
                continue;
            }
            if (is != null) {
                log.debug("Reading " + url);
                DocumentReader reader = new DocumentReader(is, UtilReader.class);
                Element e = reader.getElementByPath("util.properties");
                if (e != null) {
                    for (Iterator iter = reader.getChildElements(e, "property"); iter.hasNext();) {
                        Element p = (Element) iter.next();
                        String name = reader.getElementAttributeValue(p, "name");
                        String type = reader.getElementAttributeValue(p, "type");
                        if (type.equals("map")) {
                            Collection entryList = new ArrayList();

                            for (Iterator entriesIter = reader.getChildElements(p, "entry"); entriesIter.hasNext();) {
                                Element entry = (Element) entriesIter.next();
                                String key = null;
                                String value = null;

                                for (Iterator en = reader.getChildElements(entry, "*"); en.hasNext();) {
                                    Element keyorvalue = (Element) en.next();
                                    if (keyorvalue.getTagName().equals("key")) {
                                        key = reader.getElementValue(keyorvalue);
                                    } else {
                                        value = reader.getElementValue(keyorvalue);
                                    }
                                }
                                if (key != null && value != null) {
                                    entryList.add(new Entry(key, value));
                                }
                            }
                            if (properties.containsKey(name)) {
                                log.service("Property '" + name + "'(" + entryList + "') of " + url + " is shadowed");
                            } else {
                                properties.put(name, entryList);
                            }
                        } else {
                            String value = reader.getElementValue(p);
                            if (properties.containsKey(name)) {
                                log.service("Property '" + name + "'(" + value + "') of " + url + " is shadowed");
                            } else {
                                properties.put(name, value);
                            }
                        }
                    }
                }
            } else {
                log.debug("Resource " + s + " does not exist");
            }
        }
        if (properties.size() == 0) {
            log.warn("No properties read from " + configList);
        }
    }

    /**
     * A unmodifiable Map, with extra 'Properties'-like methods. The entries of this Map are
     * typically backed by the resources of an UtilReader (and the Map dynamically changes if the
     * resources change).
     * @since MMBase-1.8
     */

    public static class PropertiesMap extends AbstractMap {

        private final Map wrappedMap;

        /**
         * Creates an empty Map (not very useful since this Map is unmodifiable).
         */
        public PropertiesMap() {
            wrappedMap = new HashMap();
        }

        /**
         * Wrapping the given map.
         */
        public PropertiesMap(Map map) {
            wrappedMap = map;
        }
        /**
         * {@inheritDoc}
         */
        public Set entrySet() {
            return new EntrySet();

        }

        /**
         * Returns the object mapped with 'key', or defaultValue if there is none.
         */
        public Object getProperty(Object key, Object defaultValue) {
            Object result = get(key);
            return result == null ? defaultValue : result;
        }

        private class  EntrySet extends AbstractSet {
            EntrySet() {}
            public int size() {
                return PropertiesMap.this.wrappedMap.size();
            }
            public Iterator iterator() {
                return new EntrySetIterator();
            }
        }
        private class EntrySetIterator implements Iterator {
            private Iterator i;
            EntrySetIterator() {
                i = PropertiesMap.this.wrappedMap.entrySet().iterator();
            }
            public boolean hasNext() {
                return i.hasNext();
            }
            public Object next() {
                return i.next();
            }
            public void remove() {
                throw new UnsupportedOperationException("Unmodifiable");
            }
        }
    }

}
