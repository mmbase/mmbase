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
public class MMBaseCopConfig {
    private static Logger log=Logging.getLoggerInstance(MMBaseCopConfig.class.getName());

    /** the file from which the config is loaded..*/
    private File configFile;
 
    /** looks if the files have been changed */
    private SecurityFileWatcher watcher;
	
    /** our current authentication class */
    private Authentication authentication;

    /** our current authorization class */
    private Authorization authorization;

    /** if the securitymanager is configured to functionate */
    private boolean active = false;

    /** the shared secret used by this system */
    private String sharedSecret = null;    


    /** the shared secret used by this system */
    private MMBaseCop cop;    

    /** the class that watches if we have to reload...*/
    private class SecurityFileWatcher extends org.mmbase.util.FileWatcher {
//    	private final static Logger log=Logging.getLoggerInstance(SecurityFileWatcher.class.getName());    
    	private MMBaseCop cop;
	
    	public SecurityFileWatcher(MMBaseCop cop) {
	    super();
	    if(cop == null) throw new RuntimeException("MMBase cop was null");
//    	    log.debug("Starting the file watcher");
	    this.cop = cop;
	}
    	
    	public void onChange(File file) {    
	    try {
//	    	log.info("gonna reload the MMBase-cop since the file '" + file.getAbsolutePath() + "' was changed.");	    
	    	cop.reload();
	    }
	    catch(Exception e) {
	    	log.error(e);
    	    	e.printStackTrace();
	    }
	}
    }
    
    /**
     *	The constructor, will load the classes for authorization and authentication
     *	with their config files, as specied in the xml from configUrl
     *	@exception  java.io.IOException When reading the file failed
     *	@exception  java.lang.NoSuchMethodException When a tag was not specified
     *	@exception  org.mmbase.security.SecurityException When the class could not
     *	    be loaded
     */
    public MMBaseCopConfig(MMBaseCop mmbaseCop, File configFile) throws java.io.IOException, java.lang.NoSuchMethodException, SecurityException {
        this.configFile = configFile;
        log.info("using: '" + configFile.getAbsolutePath() + "' as config file for security config");	
	
    	watcher = new SecurityFileWatcher(mmbaseCop);

        cop = mmbaseCop;

    	String configPath = configFile.getAbsolutePath();
    	XMLBasicReader reader = new XMLBasicReader(configPath);

      	// are we active ?
      	String sActive = reader.getElementAttributeValue(reader.getElementByPath("security"),"active");
      	if(sActive.equalsIgnoreCase("true")) {
	    log.debug("SecurityManager will be active");
	    active = true;
	}
      	else if(sActive.equalsIgnoreCase("false")) {
	    log.debug("SecurityManager will NOT be active");
	    active = false;
	}
      	else {
	    log.error("security attibure active must have value of true or false("+configPath+")");
	    throw new SecurityException("security attibure active must have value of true or false");
	}

        // load the sharedSecret
      	sharedSecret = reader.getElementValue(reader.getElementByPath("security.sharedsecret"));
      	if(sharedSecret == null) {
	    String msg = "sharedsecret could not be found in security("+configPath+")";
	    log.error(msg);
    	    throw new java.util.NoSuchElementException(msg);	    
	} 
    	log.debug("Shared Secret retrieved");

      	if(active) {
    	    // load our authentication...
    	    org.w3c.dom.Element entry = reader.getElementByPath("security.authentication");
	    if(entry == null) throw new java.util.NoSuchElementException("security/authentication");
    	    String authClass = reader.getElementAttributeValue(entry,"class");
      	    if(authClass == null) {
	    	String msg = "attribute class could not be found in authentication("+configPath+")";
	    	log.error(msg);
    	    	throw new java.util.NoSuchElementException(msg);
	    }
      	    String authUrl = reader.getElementAttributeValue(entry,"url");
      	    if(authUrl == null) {
	    	String msg = "attribute url could not be found in authentication("+configPath+")";
	    	log.error(msg);
	    	throw new java.util.NoSuchElementException(msg);
	    }
	    // make the url absolute in case it isn't:
            File authFile = new File(authUrl);
            if (! authFile.isAbsolute()) { // so relative to currently
            	// being parsed file. make it absolute, 
            	log.debug("authentication file was not absolutely given (" + authUrl + ")");
            	authFile = new File(configFile.getParent() + File.separator + authUrl);
            	log.debug("will use: " + authFile.getAbsolutePath());            
            }
      	    authentication = getAuthentication(authClass, authFile.getAbsolutePath());
	    log.debug("Authentication retrieved");

      	    // load our authorization...
    	    entry = reader.getElementByPath("security.authorization");
	    if(entry == null) throw new java.util.NoSuchElementException("security.authorization");
      	    String auteClass = reader.getElementAttributeValue(entry,"class");
      	    if(auteClass == null) {
	    	String msg = "attribute class could not be found in auhotization("+configPath+")";
	    	log.error(msg);
	    	throw new java.util.NoSuchElementException(msg);
	    }    	    
      	    String auteUrl = reader.getElementAttributeValue(entry,"url");
      	    if(auteUrl == null) {
	    	String msg ="attribute url could not be found in auhotization("+configPath+")";
	    	log.error(msg);
	    	throw new java.util.NoSuchElementException(msg);
	    }
            // make the url absolute in case it isn't:
            File auteFile = new File(auteUrl); 
            if (! auteFile.isAbsolute()) { // so relative to currently
            	// being parsed file. make it absolute, 
            	log.debug("authorization file was not absolutely given (" + auteUrl + ")");
            	auteFile = new File(configFile.getParent() + File.separator + auteUrl);
            	log.debug("will use: " + auteFile.getAbsolutePath());            
            }
      	    authorization = getAuthorization(auteClass, auteFile.getAbsolutePath());
	    log.debug("Authorization retrieved");
	    	    
	    // add our config file..
	    watcher.add(configFile);    	  
	}
	else {
	    // we dont use security...
    	    authentication = new NoAuthentication();
	    authentication.load(cop, watcher, null);
	    authorization = new NoAuthorization();
	    authorization.load(cop, watcher, null);
	    log.debug("Retrieved dummy security classes");
	}
    }
    
    /**
     *	Returns the authentication class, which should be used.
     *	@return The authentication class which should be used.
     */
    public Authentication getAuthentication() {
    	return authentication;
    }

    /**
     *	Returns the authorization class, which should be used.
     *	@return The authorization class which should be used.
     */
    public Authorization getAuthorization() {
    	return authorization;
    }

    /**
     *	Returns the authorization class, which should be used(for optimizations)
     *	@return <code>true</code>When the SecurityManager should
     *	    be used.
     *	    	<code>false</code>When not.
     */
    public boolean getActive() {
    	return active;
    }

    /**
     * checks if the received shared secret is equals to your own shared secret
     * @param receive shared secret
     * @return true if received shared secret equals your own shared secret
     * @return false if received shared secret not equals your own shared secret
     */
    public boolean checkSharedSecret(String key) {
    	if (sharedSecret!=null) {
    	    if(sharedSecret.equals(key)) return true;
    	    else log.error("the shared "+sharedSecret+"!="+key+" secrets don't match.");
      	}
    	return false;
    }


    /**
     * stops tracking changes on the files...
     */
    public void stopWatching() {
    	watcher.exit();
    }


    /**
     * starts tracking changes on the files...
     */
    public void startWatching() {
    	watcher.start();
    }

    /**
     * get the shared Secret
     * @return the shared Secret
     */
    public String getSharedSecret() {
    	return sharedSecret;
    }
      
    private Authentication getAuthentication(String className, String configUrl) throws SecurityException {
    	log.debug("Using class:"+className+" with config:"+configUrl+" for Authentication");
	Authentication result;
      	try {
            Class classType = Class.forName(className);
            Object o = classType.newInstance();
            result = (Authentication) o;
            result.load(cop, watcher, configUrl);
      	}
      	catch(java.lang.ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw new SecurityException(cnfe.toString());
      	}
      	catch(java.lang.IllegalAccessException iae) {
            iae.printStackTrace();
            throw new SecurityException(iae.toString());
      	}
      	catch(java.lang.InstantiationException ie) {
            ie.printStackTrace();
            throw new SecurityException(ie.toString());
      	}
	return result;
    }

    private Authorization getAuthorization(String className, String configUrl) throws SecurityException {
    	log.debug("Using class:"+className+" with config:"+configUrl+" for Authorization");
	Authorization result;
      	try {
            Class classType = Class.forName(className);
            Object o = classType.newInstance();
            result = (Authorization) o;
            result.load(cop, watcher, configUrl);
      	}
      	catch(java.lang.ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw new SecurityException(cnfe.toString());
      	}
      	catch(java.lang.IllegalAccessException iae) {
            iae.printStackTrace();
            throw new SecurityException(iae.toString());
      	}
      	catch(java.lang.InstantiationException ie) {
            ie.printStackTrace();
            throw new SecurityException(ie.toString());
      	}
	return result;	
    }    
}
