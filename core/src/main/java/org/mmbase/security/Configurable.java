/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import org.mmbase.util.ResourceWatcher;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Both Authorization and Authentication are configurable.
 * This class provides the shared functionality for that.
 *
 * @author Eduard Witteveen
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public abstract class Configurable {
    private static final Logger log = Logging.getLoggerInstance(Configurable.class);

    /**
     * The SecurityManager, which created this instance
     */
    protected MMBaseCop manager;

    /**
     * This specific security configuration file. The file is absolute. Might be
     * null if the implementation does not have its own configuration file.
     * @since MMBase-1.8
     */
    protected String configResource; // relative to securityLoader



    /**
     * This filewatcher checks the configuration file for changes.
     */
    protected ResourceWatcher configWatcher;


    /**
     * The method which initialized an instance of this class. This method cannot be overridden.
     * This methods sets the member variables of this object and then
     * calls the method load();
     * @param manager The class that created this instance.
     * @param configWatcher checks the files for changes
     * @param configPath The url which contains the config information for the authorization (e.g. context/config.xml). Or null (if configured to be "")
     * @see #load()
     */
    public final void load(MMBaseCop manager, ResourceWatcher configWatcher, String configPath) {
        if (log.isDebugEnabled()) {
            log.debug("Calling load() with as config file:" + configPath);
        }
        this.manager = manager;
        this.configWatcher = configWatcher;

        if (configPath != null && !configPath.equals("")) {
            if (configPath.startsWith("/")) {
                configResource = "file://" + configPath;
            } else {
                configResource = configPath;
            }
            if (! configWatcher.getResources().contains(configResource)) {
                configWatcher.add(configResource);
            }
        }


        load();
    }

    /**
     * This method should be overridden by an extending class.  It should further initialize the
     * class. It can optionally retrieve settings from the general security configuration file
     * (available as the 'configResource' member). Security implementations with complicated
     * configuration would typically retrieve a path to their own configuration file only.
     */
    protected abstract void load();
}
