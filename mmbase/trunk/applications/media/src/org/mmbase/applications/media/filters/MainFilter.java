/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.applications.media.filters;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.applications.media.builders.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.*;

import org.w3c.dom.Element;

import java.util.*;
import java.io.File;
import java.lang.Integer;

/**
 * This is the main class for the filter process. It maintains list of
 * Filters (which content can be configured by the 'filters.xml'
 * configuration file. It does not do any filtering itself, it is only
 * the access to the actual filters, so filtering is completably
 * configurable.
 *
 * Since there can be only one 'main' filter this class is a
 * Singleton, and its one instance can be gotten by the getInstance
 * function (and this is done by the Media builders when they need url
 * representations to the stream they describe).
 *
 *
 * @author Rob Vermeulen (VPRO)
 * @author Michiel Meeuwissen
 */
public class MainFilter {
    private static Logger log = Logging.getLoggerInstance(MainFilter.class.getName());

    public static final String MAIN_TAG          = "mainFilter";
    public static final String FILTERCONFIGS_TAG = "filterConfigs";
    public static final String FILTERCONFIG_TAG  = "config";
    public static final String CHAIN_TAG         = "chain";
    public static final String FILTER_TAG        = "filter";
    public static final String FILTER_ATT        = "filter";
    public static final String ID_ATT            = "id";
    public static final String CONFIG_FILE       = "media" + File.separator + "filters.xml";
        
    private FileWatcher configWatcher = new FileWatcher(true) {
        protected void onChange(File file) {
            readConfiguration(file);
        }
    };
    
    private List filters = new ArrayList();

    /**
     * Construct the MainFilter
     */
    private MainFilter() {
        File configFile = new File(org.mmbase.module.core.MMBaseContext.getConfigPath(), CONFIG_FILE);
        if (! configFile.exists()) {
            log.error("Configuration file for mediasourcefilter " + configFile + " does not exist");
            return;
        }
        readConfiguration(configFile);
        configWatcher.add(configFile);
        configWatcher.setDelay(10 * 1000); // check every 10 secs if config changed
        configWatcher.start();
    }

    
    private static MainFilter filter = null;

    public static MainFilter getInstance() {
        if (filter == null) filter = new MainFilter();
        return filter;
    }

    
    /**
     * read the MainFilter configuration
     */
    private synchronized void readConfiguration(File configFile) {
        if (log.isServiceEnabled()) {
            log.service("Reading " + configFile);
        }
        filters.clear();

        XMLBasicReader reader = new XMLBasicReader(configFile.toString(), getClass());
        Element filterConfigs = reader.getElementByPath(MAIN_TAG + "." + FILTERCONFIGS_TAG);

        ChainSorter chainComp = new ChainSorter();
        // When chaining 'comparators' then they are combined to one comparator
        // Then only one 'sort' has to be done, which is more efficient.

        for(Enumeration e = reader.getChildElements(MAIN_TAG + "." + CHAIN_TAG, FILTER_TAG); 
            e.hasMoreElements();) {
            Element chainElement =(Element)e.nextElement();
            String  clazz        = reader.getElementValue(chainElement);
            String  elementId    = chainElement.getAttribute(ID_ATT);
            try {
                Class newclass = Class.forName(clazz);
                Filter filter = (Filter) newclass.newInstance();
                if (filter instanceof SorterFilter) {
                    chainComp.add((SorterFilter) filter);
                } else {
                    if (chainComp.size() > 0) { 
                        filters.add(chainComp);
                        chainComp = new ChainSorter();                        
                    }
                    filters.add(filter);
                }
                log.service("Added filter " + clazz + "(id=" + elementId + ")");
                if (elementId != null && ! "".equals(elementId)) {                    
                    // find right configuration
                    // not all filters necessarily have there own configuration
                    boolean found = false;
                    Enumeration f = reader.getChildElements(filterConfigs, FILTERCONFIG_TAG);
                    while (f.hasMoreElements()) {
                        Element config = (Element) f.nextElement();
                        String filterAtt = reader.getElementAttributeValue(config, FILTER_ATT);
                        if (filterAtt.equals(elementId)) {
                            log.service("Configuring " + elementId);
                            filter.configure(reader, config);
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
        if (chainComp.size() > 0) filters.add(chainComp); // make sure it is at least empty
    }

    /**
     * Perform the actual filter task
     */

    public List filter(List urls) {
        Iterator i = filters.iterator();
        while (i.hasNext()) {
            Filter filter = (Filter) i.next();
            if (log.isDebugEnabled()) {
                log.debug("Using filter " + filter);
                log.debug("before: " + urls);
            }
            
            urls = filter.filter(urls);
            if (log.isDebugEnabled()) {
                log.debug("after: " + urls);
            }
        }
        return urls;
    }
    
}
