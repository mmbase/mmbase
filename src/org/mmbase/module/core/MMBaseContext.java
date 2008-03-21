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

import org.mmbase.core.util.DaemonTask;
import org.mmbase.core.util.DaemonThread;
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
 * @version $Id: MMBaseContext.java,v 1.58 2008-03-21 13:42:59 michiel Exp $
 */
public class MMBaseContext {
    private static final Logger log = Logging.getLoggerInstance(MMBaseContext.class);
    private static boolean initialized = false;
    static boolean htmlRootInitialized = false;
    private static ServletContext sx;
    private static String userDir;
    private static String javaVersion;

    private static String htmlRoot;
    private static String htmlRootUrlPath = "/";
    private static boolean htmlRootUrlPathInitialized = false;
    private static String outputFile;
    private static ThreadGroup threadGroup;

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
    public synchronized static void init(ServletContext servletContext) {
        if (!initialized ||
            (initialized && sx == null)) { // initialized, but with init(configPath)

            if (servletContext == null) {
                throw new IllegalArgumentException();
            }

            if (initialized) {
                log.info("Reinitializing, this time with ServletContext");
            }

            // get the java version we are running
            javaVersion = System.getProperty("java.version");
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
            log.service("Initializing with " + configPath);
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
     */
    public synchronized static void init() throws Exception {
        init(System.getProperty("mmbase.config"), true);
    }

    /**
     * Returns the MMBase thread group.
     * @since MMBase-1.8
     */
    public synchronized static ThreadGroup getThreadGroup() {
        if (threadGroup == null) {
            String groupName = org.mmbase.Version.get();// + "" + new Date();
            log.service("Creating threadGroup: " + groupName);
            threadGroup = new ThreadGroup(groupName);
        }
        return threadGroup;
    }

    /**
     * Starts a daemon thread using the MMBase thread group.
     * @param task the task to run as a thread
     * @param name the thread's name
     * @since MMBase-1.8
     */
    public static Thread startThread(Runnable task, String name) {
        DaemonThread kicker = new DaemonThread(task, name);
        kicker.start();
        return kicker;
    }
    /**
     * Starts a daemon thread using the MMBase thread group.
     * @param task the task to run as a thread
     * @param name the thread's name
     * @since MMBase-1.8
     */
    public static Thread startThread(DaemonTask task, String name) {
        DaemonThread kicker = new DaemonThread(name);
        kicker.setTask(task);
        kicker.start();
        return kicker;
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
        log.info("java.version       : " + javaVersion);
        log.info("user.dir          : " + userDir);
        String configPath = ResourceLoader.getConfigurationRoot().toString();
        log.info("configuration     : " + configPath);
        log.info("webroot           : " + ResourceLoader.getWebRoot());
        String version = org.mmbase.Version.get();
        log.info("version           : " + version);
        Runtime rt = Runtime.getRuntime();
        log.info("total memory      : " + rt.totalMemory() / (1024 * 1024) + " Mbyte");
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
        if (!initialized) {
            throw new RuntimeException("The init(ServletContext) method should be called first. (Not initalized)");
        }
        if (sx == null) {
            throw new RuntimeException("The init(ServletContext) method should be called first. (No servlet context was given)");
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
     * Returns the <code>ServletContext</code> used to initialize MMBase.
     * Before calling this method the init method should be called.
     *
     * @return  the <code>ServletContext</code> used to initialize MMBase or
     *          <code>null</code> if MMBase was initialized without
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
        List<File> files =  ResourceLoader.getConfigurationRoot().getFiles("");
        if (files.size() == 0) {
            return null;
        } else {
            return files.get(0).getAbsolutePath();
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
            throw new RuntimeException("The initHtmlRoot method should be called first.");
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
            throw new RuntimeException("The init method should be called first.");
        }
        return outputFile;
    }

    /**
     * Returns a string representing the HtmlRootUrlPath, this is the path under
     * the webserver, what is the root for this instance.
     * this will return '/' or something like '/mmbase/' or so...
     *
     * This information should be requested from the ServletRequest, but if for some reason you
     * don't have one handy, this method can be used.

     * @return  the HtmlRootUrlPath
     */
    public synchronized static String getHtmlRootUrlPath() {
        if (! htmlRootUrlPathInitialized) {
            log.info("Finding root url");
            if (! initialized) {
                throw new RuntimeException("The init method should be called first.");
            }
            if (sx == null) { // no serlvetContext -> no htmlRootUrlPath
                htmlRootUrlPathInitialized = true;
                return htmlRootUrlPath;
            }
            String initPath = sx.getInitParameter("mmbase.htmlrooturlpath");
            if (initPath != null) {
                log.debug("Found mmbase.htmlrooturlpath  explicitely configured");
                htmlRootUrlPath = initPath;
            } else {
                // init the htmlRootUrlPath
                try {
                    log.debug("Autodetecting htmlrooturlpath ");
                    // fetch resource path for the root servletcontext root...
                    // check wether this is root
                    if (sx.equals(sx.getContext("/"))) {
                        htmlRootUrlPath = "/";
                    } else {
                        String url = sx.getResource("/").toString();
                        // MM: simply hope that it is the last part of that URL.
                        // I do not think it is garantueed. Used mmbase.htmlrooturlpath in web.xml if it doesn't work.
                        int length = url.length();
                        int lastSlash = url.substring(0, length - 1).lastIndexOf('/');
                        if (lastSlash > 0) {
                            htmlRootUrlPath = url.substring(lastSlash);
                        } else {
                            log.warn("Could not determine htmlRootUrlPath. Using default " + htmlRootUrlPath + "(contextUrl     :" + url + ")");
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                try {
                    if (!sx.equals(sx.getContext(htmlRootUrlPath))) {
                        log.warn("Probably did not succeed in determining htmlRootUrlPath ('" + htmlRootUrlPath + "'). Consider using the mmbase.htmlrooturlpath  context-param in web.xml");
                    }
                } catch (Exception e2) {
                    log.error(e2);
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
