/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import java.io.File;

import org.mmbase.util.XMLBasicReader;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *  This class is the main class of the security system. It loads the authentication
 *  and authorization classes if needed, and they can be requested from this manager.
 */
public class MMBaseCop extends java.lang.SecurityManager  {
    private static Logger log=Logging.getLoggerInstance(MMBaseCop.class.getName());
 
    /** the configuration used by our system */
    private MMBaseCopConfig config;
    
    /** the file from which the config is loaded..*/
    private File configFile;
    
    /**
     *	The constructor, will load the classes for authorization and authentication
     *	with their config files, as specied in the xml from configUrl
     *	@exception  java.io.IOException When reading the file failed
     *	@exception  java.lang.NoSuchMethodException When a tag was not specified
     *	@exception  org.mmbase.security.SecurityException When the class could not
     *	    be loaded
     */
    public MMBaseCop(String configPath) throws java.io.IOException, java.lang.NoSuchMethodException, SecurityException {
        super();

        configFile = new File(configPath);
    	if (! configFile.isAbsolute()) { // so relative to currently
    	    // being parsed file. make it absolute, 
            log.debug("config file was not absolutely given (" + configPath + ")");
            configFile = new File(configFile.getParent() + File.separator + configPath);
            log.debug("will use: " + configFile.getAbsolutePath());
	}
        log.info("using: '" + configFile.getAbsolutePath() + "' as config file for security");	

	config = new MMBaseCopConfig(this, configFile);
	if(config.getActive()) {
    	    // only start watching the files, when we have a active security system...s
	    config.startWatching();
	}
		
        log.info("done loading security configuration");
    }

    
    /**
     *	reload, will load the classes for authorization and authentication
     *	with their config files, as specied in the xml from configUrl
     *	@exception  java.io.IOException When reading the file failed
     *	@exception  java.lang.NoSuchMethodException When a tag was not specified
     *	@exception  org.mmbase.security.SecurityException When the class could not
     *	    be loaded
     */
    public void reload() throws java.io.IOException, java.lang.NoSuchMethodException, SecurityException {
        log.info("gonna retrieve a new security configuration...");
    	MMBaseCopConfig newConfig = new MMBaseCopConfig(this, configFile);
	
        log.info("gonna change the security configration now");	
	synchronized(this) {
	    // first stop watching the config file change-es
	    config.stopWatching();
	    // replace the old with the new one..
	    config = newConfig;
	    
	    if(config.getActive()) {
	    	// only start watching the files, when we have a active security system...s
	    	config.startWatching();
	    }
	}
        log.info("done changing security configuration");
    }
    
    private MMBaseCopConfig getConfig() {
	synchronized(this) {
	    return config;
	}    	
    }
    
    /**
     *	Returns the authentication class, which should be used.
     *	@return The authentication class which should be used.
     */
    public Authentication getAuthentication() {
    	return getConfig().getAuthentication();
    }

    /**
     *	Returns the authorization class, which should be used.
     *	@return The authorization class which should be used.
     */
    public Authorization getAuthorization() {
    	return getConfig().getAuthorization();
    }

    /**
     *	Returns the authorization class, which should be used(for optimizations)
     *	@return <code>true</code>When the SecurityManager should
     *	    be used.
     *	    	<code>false</code>When not.
     */
    public boolean getActive() {
    	return getConfig().getActive();
    }

    /**
     * checks if the received shared secret is equals to your own shared secret
     * @param receive shared secret
     * @return true if received shared secret equals your own shared secret
     * @return false if received shared secret not equals your own shared secret
     */
    public boolean checkSharedSecret(String key) {
    	return getConfig().checkSharedSecret(key);    
    }

    /**
     * get the shared Secret
     * @return the shared Secret
     */
    public String getSharedSecret() {
    	return getConfig().getSharedSecret();
    }    
}
