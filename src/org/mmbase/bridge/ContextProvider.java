/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge;

/**
 * Main class to aquire CloudContexts
 * @author Kees Jongenburger
 * @version $Id: ContextProvider.java,v 1.4 2003-02-25 12:38:16 kees Exp $
 * @since MMBase-1.5
 */
public class ContextProvider{
    private final static String DEFAULT_CLOUD_CONTEXT="local";
    private static String defaultCloudContextName ;
    /**
     * Factory method to get an instance of a CloudContext. Depending
     * on the uri parameter given the CloudContext might be a local context
     * or a remote context (rmi)
     * @param uri an identifier for the context<BR>
     * possible values:
     * <UL>
     *   <LI>local : will return a local context</LI>
     *   <LI>rmi://hostname:port/contextname : will return a remote context</LI>
     *   <LI>a null parameter: will return a local context
     * </UL>
     * @return a cloud context
     * @throws BridgeException if the cloudcontext was not found
     */
    public static CloudContext getCloudContext(String uri) {
        if (uri == null || (uri != null && uri.trim().length() == 0)){
		uri = getDefaultCloudContextName();
	}

        if (uri.startsWith("rmi")){
            return RemoteContext.getCloudContext(uri);
        } else if (uri.startsWith("local")){
            return LocalContext.getCloudContext();
        }
	throw new BridgeException("cloudcontext with name {"+ uri +"} is not known to MMBase");
    }

     public static String getDefaultCloudContextName(){
	    //first choice.. set the cloud context using system properties
	    if (defaultCloudContextName == null){
		    defaultCloudContextName = System.getProperty("mmbase.defaultcloudcontext");
	    }
	    if (defaultCloudContextName == null){
		    defaultCloudContextName = DEFAULT_CLOUD_CONTEXT;
	    }
	    return defaultCloudContextName;
    }

    public static CloudContext getDefaultCloudContext(){
	    return getCloudContext(getDefaultCloudContextName());
    }
}
