/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import java.io.*;
import javax.servlet.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Using MMBaseContext class you can retrieve the servletContext from anywhere
 * using the get method.
 *
 * @author Daniel Ockeloen
 * @author David van Zeventer
 * @author Jaco de Groot
 * @version $Id: MMBaseContext.java,v 1.37 2003-07-18 17:43:02 michiel Exp $
 */
public class MMBaseContext {
    private static final Logger log = Logging.getLoggerInstance(MMBaseContext.class);
    private static boolean initialized = false;
    private static boolean htmlRootInitialized = false;
    private static ServletContext sx;
    private static String userDir;
    private static String configPath;
    private static String htmlRoot;
    private static String htmlRootUrlPath ="/";
    private static boolean htmlRootUrlPathInitialized = false;
    private static String outputFile;

    /**
     * Initialize MMBase using a <code>SevletContext</code>. This method will
     * check the servlet configuration for context parameters mmbase.outputfile
     * and mmbase.config. If not found it will look for system properties.
     *
     * @throws ServletException  if mmbase.configpath is not set or is not a
     *                           directory or doesn't contain the expected
     *                           config files.
     *
     */
    public synchronized static void init(ServletContext servletContext) throws ServletException {
        if (!initialized) {
            // store the current context
            sx = servletContext;

            // Get the current directory using the user.dir property.
            userDir = System.getProperty("user.dir");

            // Init outputfile.
            {
                String outputfile = sx.getInitParameter("mmbase.outputfile");
                if (outputfile == null) {
                    outputfile = System.getProperty("mmbase.outputfile");
                }
                initOutputfile(outputfile);
            }
            // Init configpath.
            {
                String configpath = sx.getInitParameter("mmbase.config");
                if (configpath == null) {
                    configpath = System.getProperty("mmbase.config");
                }
                if (configpath == null) {
                    // desperate looking for a location.. (say we are a war file..)
                    // keeping the value 'null' will always give a failure..
                    configpath =  servletContext.getRealPath("/WEB-INF/config");
                }
                try {
                    initConfigpath(configpath);
                } catch(Exception e) {
                    throw new ServletException(e.getMessage());
                }
            }
            // Init logging.
            initLogging();

            initialized = true;
        }
    }

    /**
     * Initialize MMBase using a config path. Useful when testing
     * MMBase classes with a main. You can also configure to init
     * logging or not.
     *
     * @throws Exception  if mmbase.config is not set or is not a
     *                    directory or doesn't contain the expected
     *                    config files.
     *
     */
    public synchronized static void init(String configPath, boolean initLogging) throws Exception {
        if (!initialized) {
            // Get the current directory using the user.dir property.
            userDir = System.getProperty("user.dir");

            // Init outputfile. // use of mmbase.outputfile  is deprecated!
            initOutputfile(System.getProperty("mmbase.outputfile"));

            // Init configpath.
            initConfigpath(configPath);
            // Init logging.
            if (initLogging) {
                initLogging();
            }
            initialized = true;
       }
    }

    /**
     * Initialize MMBase using system properties only. This may be useful in
     * cases where MMBase is used without a servlet. For example when running
     * JUnit tests.
     *
     * @throws Exception  if mmbase.config is not set or is not a
     *                    directory or doesn't contain the expected
     *                    config files.
     *
     */



    public synchronized static void init() throws Exception {
        init(System.getProperty("mmbase.config"), true);
    }



    private static void initOutputfile(String o) {
        outputFile = o;
        if (outputFile != null) {
            if (!new File(outputFile).isAbsolute()) {
                outputFile = userDir + File.separator + outputFile;
            }
            try {
                 FileOutputStream fos;
                 fos = new FileOutputStream(outputFile, true);
                 PrintStream mystream = new PrintStream(fos);
                 System.setOut(mystream);
                 System.setErr(mystream);
            } catch (IOException e) {
                 outputFile = null;
                 System.err.println("Failed to set mmbase.outputfile to '"
                                    + outputFile + "'.");
                 e.printStackTrace();
            }
        }
    }

    private static void initConfigpath(String c) throws Exception {
        configPath = c;
        if (configPath == null) {
            userDir = null;
            configPath = null;
            String message = "Parameter mmbase.config not set.";
            System.err.println(message);
            throw new Exception(message);
        }
        File fileConfigpath = new File(configPath);
        if (userDir != null && !fileConfigpath.isAbsolute()) {
            configPath = userDir + File.separator + configPath;
            fileConfigpath = new File(configPath);
        }
        // Make it absolute. Needed for servscan and servdb to
        // to startup properly.
        configPath = fileConfigpath.getAbsolutePath();

        if (!fileConfigpath.isDirectory()) {
            userDir = null;
            configPath = null;
            String message = "Parameter mmbase.config is not pointing to "
                             + "a directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if(!new File(configPath + "/security/security.xml").isFile()) {
            userDir = null;
            configPath = null;
            String message = "File 'security/security.xml' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
    /*
        if(!new File(configpath + "/accounts.properties").isFile()) {
            userDir = null;
            configpath = null;
            String message = "File 'accounts.properties' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
    */
        if(!new File(configPath + "/builders").isDirectory()) {
            userDir = null;
            configPath = null;
            String message = "Directory 'builders' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if(!new File(configPath + "/modules").isDirectory()) {
            userDir = null;
            configPath = null;
            String message = "Directory 'modules' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if(!new File(configPath + "/modules/mmbaseroot.xml").isFile()) {
            userDir = null;
            configPath = null;
            String message = "File 'modules/mmbaseroot.xml' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if(!new File(configPath + "/modules/jdbc.xml").isFile()) {
            userDir = null;
            configPath = null;
            String message = "File 'modules/jdbc.xml' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if(!new File(configPath + "/log/log.xml").isFile()) {
            userDir = null;
            configPath = null;
            String message = "File 'log/log.xml' missing in "
                             + "mmbase.config directory("+fileConfigpath.getAbsolutePath()+").";
            System.err.println(message);
            throw new Exception(message);
        }
        if (configPath.endsWith(File.separator)) {
            configPath = configPath.substring(0, configPath.length() - 1);
        }
    }

    private static void initLogging() {
        // Starting the logger
        Logging.configure(configPath + File.separator + "log" + File.separator + "log.xml");
        log.info("===========================");
        log.info("MMBase logging initialized.");
        log.info("===========================");
        log.info("user.dir          : " + userDir);
        log.info("mmbase.config     : " + configPath);
        log.info("mmbase.outputfile : " + outputFile);
        log.info("version           : " + org.mmbase.Version.get());
        Runtime rt = Runtime.getRuntime();
        log.info("total memory      : " + rt.totalMemory() / (1024 * 1024) + " Mbyte");
        rt.gc();
        log.info("free memory       : " + rt.freeMemory() / (1024 * 1024) + " Mbyte");
        log.info("system locale     : " + Locale.getDefault());

    }

    /**
     * Initialize mmbase.htmlroot parameter. This method is only needed for
     * SCAN related servlets and should be called after the init(ServletContext)
     * method. If the mmbase.htmlroot parameter is not found in the servlet
     * context or system properties this method will try to set it to the
     * root directory of the webapp.
     *
     * @throws ServletException  if mmbase.htmlroot is not set or is not a
     *                           directory
     *
     */
    public synchronized static void initHtmlRoot() throws ServletException {
        if (!initialized || sx == null) {
            String message = "The init(ServletContext) method should be called first.";
            System.err.println(message);
            throw new RuntimeException(message);
        }
        if (!htmlRootInitialized) {
            // Init htmlroot.
            htmlRoot = sx.getInitParameter("mmbase.htmlroot");
            if (htmlRoot == null) {
                htmlRoot = System.getProperty("mmbase.htmlroot");
            }
            if (htmlRoot == null) {
                htmlRoot = sx.getRealPath("");
            }
            if (htmlRoot == null){
                String message = "Parameter mmbase.htmlroot not set.";
                System.err.println(message);
                throw new ServletException(message);
            } else {
                if (userDir != null && !new File(htmlRoot).isAbsolute()) {
                    htmlRoot = userDir + File.separator + htmlRoot;
                }
                if (!new File(htmlRoot).isDirectory()) {
                    userDir = null;
                    configPath = null;
                    htmlRoot = null;
                    String message = "Parameter mmbase.htmlroot is not pointing "
                                     + "to a directory.";
                    System.err.println(message);
                    throw new ServletException(message);
                } else {
                    if (htmlRoot.endsWith(File.separator)) {
                        htmlRoot = htmlRoot.substring(0, htmlRoot.length() - 1);
                    }
                    htmlRootInitialized = true;
                    log.info("mmbase.htmlroot   : " + htmlRoot);
                    log.info("context           : " + getHtmlRootUrlPath());
                }
            }
        }
    }

    /**
     * Returns the <code>ServeltContext</code> used to initialize MMBase.
     * Before calling this method the init method should be called.
     *
     * @return  the <code>ServeltContext</code> used to initialize MMBase or
     *          <code>null</code> if MMBase was initilized without
     *          <code>ServletContext</code>
     */
    public synchronized static ServletContext getServletContext() {
        if (!initialized) {
            String message = "The init method should be called first.";
            System.err.println(message);
            throw new RuntimeException(message);
        }
        return sx;
    }

    /**
     * Returns a string representing the mmbase.config parameter without a
     * final <code>File.separator</code>. Before calling this method the
     * init method should be called to make sure this parameter is set.
     *
     * @return  the mmbase.config parameter or WEB-INF/config
     */
    public synchronized static String getConfigPath() {
        if (!initialized) {
            String config = System.getProperty("mmbase.config");
            if (config == null) {
                String message = "The init method should be called first (or start with mmbase.config parameter)";
                System.err.println(message);
                throw new RuntimeException(message);
            } else {
                File configDir = new File(config);
                if(!configDir.exists()) {
                    String message = "Config directory could not be found, does it exist? (" + configDir.getAbsolutePath()  + ")";
                    System.err.println(message);
                    throw new RuntimeException(message);
                }
                if(!configDir.canRead()) {
                    String message = "Config directory could not be read, is it readable? (" + configDir.getAbsolutePath()  + ")";
                    System.err.println(message);
                    throw new RuntimeException(message);
                }
                if(!configDir.isDirectory()) {
                    String message = "Config directory is not a directory (" + configDir.getAbsolutePath()  + ")";
                    System.err.println(message);
                    throw new RuntimeException(message);
                }
                return config;
            }
        }
        return configPath;
    }

    /**
     * Returns a string representing the mmbase.htmlroot parameter without a
     * final <code>File.separator</code>. Before calling this method the
     * initHtmlRoot method should be called to make sure this parameter is set.
     *
     * @return  the mmbase.htmlroot parameter or <code>null</code> if not
     *          initialized
     */
    public synchronized static String getHtmlRoot() {
        if (!htmlRootInitialized) {
            String message = "The initHtmlRoot method should be called first.";
            System.err.println(message);
            throw new RuntimeException();
        }
       return htmlRoot;
    }

    /**
     * Returns a string representing the mmbase.outputfile parameter. If set,
     * this is the file to wich all <code>System.out</code> and
     * <code>System.err</code> output is redirected. Before calling this method
     * the init method should be called.
     *
     * @return  the mmbase.outputFile parameter or <code>null</code> if not set
     * @deprecated use logging system
     */
    public synchronized static String getOutputFile() {
        if (!initialized) {
            String message = "The init method should be called first.";
            System.err.println(message);
            throw new RuntimeException(message);
        }
        return outputFile;
    }


    /**
     * converts a url with a given context, to the resource url.
     * @param servletContext
     * @param url A url to the resource, which must exist
     * @return null on failure, otherwise a resource url.
     */
    private static String convertResourceUrl(ServletContext servletContext, String url) {
        // return null on failure
        if(servletContext == null) return null;

        try {
            java.net.URL transformed = servletContext.getResource(url);
            if(transformed == null){
                log.error("no resource is mapped to the pathname: '"+url+"'");
                return null;
            }
            return transformed.toString();
        } catch (java.net.MalformedURLException e) {
            log.error("could not convert the url: '" + e + "'(error converting)");
        }
        return null;
    }

    private static String CONTEXT_URL_IDENTIFIER = "jndi:/";
    
    /**
     * Returns a string representing the HtmlRootUrlPath, this is the path under
     * the webserver, what is the root for this instance.
     * this will return '/' or something like '/mmbase/' or so...
     * @return  the HtmlRootUrlPath
     * @deprecated  should not be needed, and this information should be requested from the ServletRequest
     */
    public synchronized static String getHtmlRootUrlPath() {
        if (! htmlRootUrlPathInitialized) {
            log.info("Finding root url");
            if (! initialized) {
                String message = "The init method should be called first.";
                System.err.println(message);
                throw new RuntimeException(message);
            }
            if (sx == null) { // no serlvetContext -> not htmlRootUrlPath
                htmlRootUrlPathInitialized = true;
                return htmlRootUrlPath;
            }
            String initPath = sx.getInitParameter("mmbase.htmlrooturlpath");
            if (initPath != null) {
                log.debug("Found mmbase.htmlrooturlpath  explicitely configured");
                htmlRootUrlPath = initPath;
            } else {
                // init the htmlRootUrlPath
                log.debug("Autodetecting htmlrooturlpath ");

                // fetch resource path for the root servletcontext root...
                ServletContext rootContext = null;
                String rootContextUrl = null;
                String contextUrl = null;
            

                if (! sx.getClass().getName().startsWith("com.evermind")) { // Orion horribly fails
                    contextUrl = convertResourceUrl(sx, "/");
                    rootContext = sx.getContext("/"); // Orion fails here
                    rootContextUrl = convertResourceUrl(rootContext, "/");
                } else {
                    log.info("For Orion: Use the parameter mmbase.htmlrooturlpath (in web.xml) if your app is not running on '/' (Cannot detect now)");
                }

                if(contextUrl != null && rootContextUrl != null) {
                    // the beginning of contextUrl is the same as the string rootContextUrl,
                    // the left part is the current urlPath on the server...
                    if(contextUrl.startsWith(rootContextUrl)) {
                        // htmlUrl is gonna be filled
                        htmlRootUrlPath = "/" + contextUrl.substring(rootContextUrl.length(), contextUrl.length());
                    } else {
                        log.warn("the current context:" + contextUrl + " did not begin with the root context :"+rootContextUrl);
                    }
                } else if (rootContextUrl == null && contextUrl != null && contextUrl.startsWith(CONTEXT_URL_IDENTIFIER)) {
                    // This works on my tomcat (4.03),.. this is supposed to be the reference implementation?
                    // so what should be the code?
                    // the String will be typically something like "jndi:/hostname/contextname/", so we are looking for the first '/' after the hostname..
                    int contextStart = contextUrl.substring(CONTEXT_URL_IDENTIFIER.length()).indexOf('/');
                    if(contextStart != -1) {
                        htmlRootUrlPath = contextUrl.substring(CONTEXT_URL_IDENTIFIER.length() + contextStart);
                    } else {
                        log.warn("Could not determine htmlRootUrlPath. Using default " + htmlRootUrlPath
                                 + "\nbut  contextUrl     : '" + contextUrl + "' did start with: '"+CONTEXT_URL_IDENTIFIER + "'");
                    }
                } else {
                    log.warn("Could not determine htmlRootUrlPath. Using default " + htmlRootUrlPath + "(contextUrl     :" + contextUrl + "  rootContextUrl :" + rootContextUrl + ")");
                }
            }
            htmlRootUrlPathInitialized = true;
        }
        return htmlRootUrlPath;
    }

    /**
     * Returns whether this class has been initialized.
     * This can be used to determine whether MMBase specific configuration data is accessible.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
