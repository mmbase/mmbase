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
import java.text.DateFormat;

import org.mmbase.util.ResourceLoader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Using MMBaseContext class you can retrieve the servletContext from anywhere
 * using the get method.
 *
 * @author Daniel Ockeloen
 * @author David van Zeventer
 * @author Jaco de Groot
 * @version $Id: MMBaseContext.java,v 1.42 2004-11-11 16:46:02 michiel Exp $
 */
public class MMBaseContext {
    private static final Logger log = Logging.getLoggerInstance(MMBaseContext.class);
    private static boolean initialized = false;
    private static boolean htmlRootInitialized = false;
    private static ServletContext sx;
    private static String userDir;
   
    private static String htmlRoot;
    private static String htmlRootUrlPath = "/";
    private static boolean htmlRootUrlPathInitialized = false;
    private static String outputFile;

    /**
     * Initialize MMBase using a <code>ServletContext</code>. This method will
     * check the servlet configuration for context parameters mmbase.outputfile
     * and mmbase.config. If not found it will look for system properties.
     *
     * @throws ServletException  if mmbase.config is not set or is not a
     *                           directory or doesn't contain the expected
     *                           config files.
     *
     */
    public synchronized static void init(ServletContext servletContext) throws ServletException {
        if (!initialized) {
            // store the current context
            sx = servletContext;
            // Get the user directory using the user.dir property.
            // default set to the startdir of the appserver
            userDir = sx.getInitParameter("user.dir");
            if (userDir == null) {
                userDir = System.getProperty("user.dir");
            }
            // take into account userdir can start at webrootdir
            if (userDir != null && userDir.indexOf("$WEBROOT") == 0) {
                userDir = servletContext.getRealPath(userDir.substring(8));
            }
            // Init outputfile.
            String outputFile = sx.getInitParameter("mmbase.outputfile");
            if (outputFile == null) {
                outputFile = System.getProperty("mmbase.outputfile");
            }
            // take into account configpath can start at webrootdir
            if (outputFile != null && outputFile.indexOf("$WEBROOT") == 0) {
                outputFile = servletContext.getRealPath(outputFile.substring(8));
            }
            initOutputfile(outputFile);

            ResourceLoader.init(sx);

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
                 PrintStream stream = new PrintStream(new FileOutputStream(outputFile, true));
                 System.setOut(stream);
                 System.setErr(stream);
            } catch (IOException e) {
                 outputFile = null;
                 log.error("Failed to set mmbase.outputfile to '" + outputFile + "'.");
                 log.error(Logging.stackTrace(e));
            }
        }
    }

    private static void initLogging() {
        // Starting the logger
        Logging.configure(ResourceLoader.getConfigurationRoot().getChildResourceLoader("log"), "log.xml");
        log.info("===========================");
        log.info("MMBase logging initialized.");
        log.info("===========================");
        log.info("user.dir          : " + userDir);
        log.info("configuration     : " + ResourceLoader.getConfigurationRoot());
        log.info("webroot           : " + ResourceLoader.getWebRoot());

        log.info("version           : " + org.mmbase.Version.get());
        Runtime rt = Runtime.getRuntime();
        log.info("total memory      : " + rt.totalMemory() / (1024 * 1024) + " Mbyte");
        rt.gc();
        log.info("free memory       : " + rt.freeMemory() / (1024 * 1024) + " Mbyte");
        log.info("system locale     : " + Locale.getDefault());
        log.info("start time        : " + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(new Date(1000 * (long) MMBase.startTime)));

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
            log.error(message);
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
                log.error("Parameter mmbase.htmlroot not set.");
            } else {
                if (userDir != null && !new File(htmlRoot).isAbsolute()) {
                    htmlRoot = userDir + File.separator + htmlRoot;
                }
                if (!new File(htmlRoot).isDirectory()) {
                    userDir = null;
                    htmlRoot = null;
                    throw new ServletException("Parameter mmbase.htmlroot is not pointing to a directory.");
                } else {
                    if (htmlRoot.endsWith(File.separator)) {
                        htmlRoot = htmlRoot.substring(0, htmlRoot.length() - 1);
                    }
                }
            }
            htmlRootInitialized = true;
            log.info("mmbase.htmlroot   : " + htmlRoot);
            log.info("context           : " + getHtmlRootUrlPath());
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
            throw new RuntimeException("The init method should be called first.");
        }
        return sx;
    }

    /**
     * Returns a string representing the mmbase.config parameter without a
     * final <code>File.separator</code>. Before calling this method the
     * init method should be called to make sure this parameter is set.
     *
     * @return  the mmbase.config parameter or WEB-INF/config
     * @deprecated use {@link org.mmbase.util.ResourceLoader#getConfigurationRoot} with relative path
     */
    public  synchronized static String getConfigPath() {
        List files =  ResourceLoader.getConfigurationRoot().getFiles("");
        if (files.size() == 0) {
            return null;
        } else {
            return ((File) files.get(0)).getAbsolutePath();
        }
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
            log.error(message);
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
            log.error(message);
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
                log.error(message);
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

                try {
                    contextUrl = convertResourceUrl(sx, "/");
                    rootContext = sx.getContext("/"); // Orion fails here?
                    rootContextUrl = convertResourceUrl(rootContext, "/");
                } catch (Exception e) {
                }

                if(contextUrl != null && rootContextUrl != null) {
                    // the beginning of contextUrl is the same as the string rootContextUrl,
                    // the left part is the current urlPath on the server...
                    if(contextUrl.startsWith(rootContextUrl)) {
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
