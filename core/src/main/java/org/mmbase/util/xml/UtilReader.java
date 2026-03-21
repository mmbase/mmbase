/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.io.IOException;
import java.net.URL;

import java.util.*;

import org.mmbase.util.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
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
 * This produces a 'watched map' utilProperties. Every time the
 * underlying config file(s) are changed 'init' is called. Init is
 * called on instantation of the surrounding class too. The map is
 * unmodifiable, and only mirrors the resource(s) "utils/myutil.xml".
 *
 * @since MMBase-1.6.4
 * @author Rob Vermeulen
 * @author Michiel Meeuwissen
 * @version $Id$
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
     * This method is called by EntityResolver.
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_UTIL_1_0, DTD_UTIL_1_0, UtilReader.class);
        // doesnt startup, probably because of cyclic referecnes, if this happens in DocumentReader itself.
        DocumentReader.utilProperties = UtilReader.get("documentreader.xml").getProperties();

    }

    private static final Map<String, UtilReader> utilReaders = new HashMap<String, UtilReader>();     // file-name -> utilreader

    /**
     * Returns a UtilReader for the given fileName. When you use this, the UtilReader instance will be cached.
     *
     * @since MMBase-1.8
     */

    public static UtilReader get(String fileName) {
        UtilReader utilReader = utilReaders.get(fileName);
        if (utilReader == null) {
            synchronized(utilReaders) {
                utilReader = new UtilReader(fileName);
                utilReaders.put(fileName, utilReader);
            }
        }
        return utilReader;
    }

    private class UtilFileWatcher extends ResourceWatcher {
        private final ResourceWatcher wrappedWatcher;
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

    private final Map<String, String> properties = new HashMap<String, String>();
    private final Map<String,Collection<Map.Entry<String, String>>> maps = new HashMap<String, Collection<Map.Entry<String,String>>>();
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

    @Override
    public void finalize() {
        if (watcher != null) watcher.exit();
    }

    /**
     * Get the properties of this utility.
     */
    public PropertiesMap<String> getProperties() {
        return new PropertiesMap<String>(properties);
    }

    /**
     * Get the properties of this utility.
     * @since MMBase-1.8.6
     */
    public PropertiesMap<Collection<Map.Entry<String, String>>> getMaps() {
        return new PropertiesMap<Collection<Map.Entry<String, String>>>(maps);
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

    /**
     * @since MMBase-1.9.1
     */
    protected Map.Entry<String, String> getEntry(DocumentReader reader, String k, String v) {
        return new Entry<String,String>(k, v);
    }



    protected void readProperties(String s) {
        properties.clear();
        maps.clear();

        ResourceLoader configLoader = ResourceLoader.getConfigurationRoot();
        List<URL> configList = configLoader.getResourceList(s);
        for (URL url : configList) {
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
                    for (Element p : DocumentReader.getChildElements(e, "property")) {
                        String name = reader.getElementAttributeValue(p, "name");
                        String type = reader.getElementAttributeValue(p, "type");
                        if (type.equals("mergingmap") ||
                            type.equals("map")) {
                            Collection<Map.Entry<String, String>> entryList = null;
                            if (type.equals("mergingmap")) {
                                entryList = maps.get(name);
                            }

                            if (entryList == null) {
                                entryList = new ArrayList<Map.Entry<String,String>>();
                            }

                            for (Element entry : DocumentReader.getChildElements(p, "entry")) {
                                String key = null;
                                String value = null;

                                for (Element keyorvalue : DocumentReader.getChildElements(entry, "*")) {
                                    if (keyorvalue.getTagName().equals("key")) {
                                        key = DocumentReader.getElementValue(keyorvalue);
                                    } else {
                                        value = DocumentReader.getNodeTextValue(keyorvalue, false);
                                    }
                                }
                                if (key != null) {
                                    entryList.add(getEntry(reader, key, value));
                                }
                            }
                            if (maps.containsKey(name) && ! type.equals("mergingmap")) {
                                log.debug("Property '" + name + "' (" + entryList + ") of " + url + " is shadowed");
                            } else {
                                maps.put(name, entryList);
                            }
                        } else {
                            String value = DocumentReader.getElementValue(p);
                            Map.Entry<String, String> entry = getEntry(reader, name, value);
                            if (properties.containsKey(entry.getKey())) {
                                log.debug("Property '" + entry.getKey() + "' ('" + entry.getValue() + "') of " + url + " is shadowed");
                            } else {
                                properties.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            } else {
                log.debug("Resource " + s + " does not exist");
            }

        }
        if (properties.size() == 0 && maps.size() == 0) {
            log.service("No properties read from " + configList);
        } else {
            log.service("Read " + properties.entrySet() + " from " + configList);
        }
    }

    /**
     * An unmodifiable Map, with extra 'Properties'-like methods. The entries of this Map are
     * typically backed by the resources of an UtilReader (and the Map dynamically changes if the
     * resources change).
     * @since MMBase-1.8
     */

    public static class PropertiesMap<E> extends AbstractMap<String, E> {

        private final Map<String, E> wrappedMap;

        /**
         * Creates an empty Map (not very useful since this Map is unmodifiable).
         */
        public PropertiesMap() {
            wrappedMap = new HashMap<String, E>();
        }

        /**
         * Wrapping the given map.
         */
        public PropertiesMap(Map<String, E> map) {
            wrappedMap = map;
        }
        /**
         * {@inheritDoc}
         */
        public Set<Map.Entry<String, E>> entrySet() {
            return new EntrySet();

        }

        /**
         * Returns the object mapped with 'key', or defaultValue if there is none.
         */
        public E getProperty(String key, E defaultValue) {
            E result = get(key);
            return result == null ? defaultValue : result;
        }

        private class  EntrySet extends AbstractSet<Map.Entry<String, E>> {
            EntrySet() {}
            public int size() {
                return PropertiesMap.this.wrappedMap.size();
            }
            public Iterator<Map.Entry<String, E>> iterator() {
                return new EntrySetIterator();
            }
        }
        private class EntrySetIterator implements Iterator<Map.Entry<String, E>> {
            private Iterator<Map.Entry<String, E>> i;
            EntrySetIterator() {
                i = PropertiesMap.this.wrappedMap.entrySet().iterator();
            }
            public boolean hasNext() {
                return i.hasNext();
            }
            public Map.Entry<String, E> next() {
                return i.next();
            }
            public void remove() {
                throw new UnsupportedOperationException("Unmodifiable");
            }
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + file;
    }

}
