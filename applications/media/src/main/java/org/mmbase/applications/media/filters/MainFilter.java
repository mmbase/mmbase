/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.media.filters;

import java.util.*;

import org.mmbase.applications.media.urlcomposers.URLComposer;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.ResourceWatcher;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.Element;

/**
 * This is the main class for the filter process. It maintains list of
 * Filters (which can be configured in the 'filters.xml' configuration file).
 * It does not do any filtering itself, it is only to access to the actual
 * filters, so filtering is completely configurable.
 * <p>
 * Since there can be only one 'main' filter this class is a
 * Singleton, and its one instance can be gotten by the #getInstance()
 * function (and this is done by Media builders when they need url
 * representations to the stream they describe).
 *
 * @author Rob Vermeulen (VPRO)
 * @author Michiel Meeuwissen
 */
public class MainFilter {
    private static Logger log = Logging.getLoggerInstance(MainFilter.class);

    public static final String MAIN_TAG          = "mainFilter";
    public static final String FILTERCONFIGS_TAG = "filterConfigs";
    public static final String FILTERCONFIG_TAG  = "config";
    public static final String CHAIN_TAG         = "chain";
    public static final String FILTER_TAG        = "filter";
    public static final String FILTER_ATT        = "filter";
    public static final String ID_ATT            = "id";
    public static final String CONFIG_FILE       = "media/filters.xml";

    private ResourceWatcher configWatcher = new ResourceWatcher() {
        public void onChange(String resource) {
            readConfiguration(resource);
        }
    };

    private final List<Filter> filters = new ArrayList<Filter>();

    /**
     * Constructs MainFilter
     */
    private MainFilter() {
        String configFile = CONFIG_FILE;
        configWatcher.add(configFile);
        configWatcher.start();
        configWatcher.onChange();
    }


    private static MainFilter filter = null;

    public static MainFilter getInstance() {
        if (filter == null) {
            filter = new MainFilter();
        }
        return filter;
    }


    /**
     * Read MainFilter configuration
     * @param configFile    configuration file 'config/media/filters.xml'
     */
    private synchronized void readConfiguration(String configFile) {
        if (log.isServiceEnabled()) {
            log.service("Reading " + configFile);
        }
        filters.clear();
        ChainSorter chainComp = new ChainSorter();

        try {
            DocumentReader reader = new DocumentReader(ResourceLoader.getConfigurationRoot().getDocument(configFile, DocumentReader.validate(), getClass()));
            Element filterConfigs = reader.getElementByPath(MAIN_TAG + "." + FILTERCONFIGS_TAG);

            // When chaining 'comparators' they are combined to one comparator.
            // Then only one 'sort' has to be done, which is more efficient.

            for(Element chainElement:reader.getChildElements(MAIN_TAG + "." + CHAIN_TAG, FILTER_TAG)) {
                String  clazz        = DocumentReader.getElementValue(chainElement);
                String  elementId    = chainElement.getAttribute(ID_ATT);
                try {
                    Class<?> newclass = Class.forName(clazz);
                    Filter fil = (Filter) newclass.newInstance();
                    if (fil instanceof Sorter) {
                        chainComp.add((Sorter) fil);
                    } else {
                        filters.add(fil);
                    }
                    log.service("Added filter " + clazz + "(id=" + elementId + ")");
                    if (elementId != null && ! "".equals(elementId)) {
                        // find right configuration
                        // not all filters necessarily have there own configuration
                        boolean found = false;

                        for(Element config : DocumentReader.getChildElements(filterConfigs, FILTERCONFIG_TAG)) {
                            String filterAtt = reader.getElementAttributeValue(config, FILTER_ATT);
                            if (filterAtt.equals(elementId)) {
                                log.service("Configuring " + elementId);
                                fil.configure(reader, config);
                                found = true;
                                break;
                            }
                        }
                        if (! found) log.debug("No configuration found for filter " + elementId);
                    }
                } catch (ClassNotFoundException ex) {
                    log.error("Cannot load filter " + clazz + "\n" + ex);
                } catch (InstantiationException ex1) {
                    log.error("Cannot load filter " + clazz + "\n" + ex1);
                } catch (IllegalAccessException ex2) {
                    log.error("Cannot load filter " + clazz + "\n" + ex2);
                }
            }
        } catch (Exception e) {
            log.warn(e);
        }
        if (chainComp.size() > 0) {
            filters.add(chainComp); // make sure it is at least empty
        }
    }

    /**
     * Perform the actual filter task
     */

    public List<URLComposer> filter(List<URLComposer> urls) {
        for (Filter fil : filters) {
            if (log.isDebugEnabled()) {
                log.debug("Using filter " + fil);
                log.debug("before: " + urls);
            }

            try {
                urls = fil.filter(urls);
            } catch (Exception filterException) {
                log.error("Check filter " + fil + ": " + filterException.getMessage(), filterException);
            }
            if (log.isDebugEnabled()) {
                log.debug("after: " + urls);
            }
        }
        return urls;
    }


    // ====================================================================================
    // Test-code

    private static void addTestData(Collection<String> c) {
        c.add("hoi");
        c.add("hallo");
        c.add("heeej");
        c.add("hello");
        c.add("daag");
        c.add("ajuus");
        c.add("saluton");
        c.add("cao");
        c.add("arriverderci");
        c.add("gxis");
        c.add("hej");
        c.add("komop");
        c.add("1234");
    }
    private static class TestComparator implements Comparator<String> {
        private int i;
        TestComparator(int i) {
            this.i = i;
        }
        public int compare(String o1, String o2) {
            return o1.hashCode() - o2.hashCode();
        }

    }

    public static void main(String[] args) {
        // what is quicker: sorting a list, or creating a new sortedset:
        final int ITERATIONS = 200000;

        List<String> list = new ArrayList<String>();
        addTestData(list);
        long start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            Comparator<String> c = new TestComparator(i);
            Collections.sort(list, c);
        }
        log.debug("list duration: " + (System.currentTimeMillis() - start));
        log.debug(list);
        SortedSet<String>  sortedSet   = new TreeSet<String>();
        addTestData(sortedSet);
        start = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            SortedSet<String> s = new TreeSet<String>(new TestComparator(i));
            s.addAll(sortedSet);
            sortedSet = s;
        }
        log.debug("sortedset duration: " + (System.currentTimeMillis() - start));
        log.debug(sortedSet);
    }


}
