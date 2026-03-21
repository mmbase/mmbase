/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.io.*;
import java.text.DateFormat;
import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.mmbase.core.event.EventManager;
import org.mmbase.core.event.SystemEvent;
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
 * @version $Id$
 */
public class MMBaseContext {
    private static final Logger log = Logging.getLoggerInstance(MMBaseContext.class);
    private static boolean initialized = false;
    static boolean htmlRootInitialized = false;
    private static ServletContext sx;
    private static String userDir;

    private static String htmlRoot;
    private static String htmlRootUrlPath = "/";
    private static boolean htmlRootUrlPathInitialized = false;
    private static String outputFile;


    /**
     * Name of the machine used in the mmbase cluster.
     * it is used for the mmservers objects. Make sure that this is different
     * for each node in your cluster. This is not the machines dns name
     * (as defined by host as name or ip number).
     */
    static String machineName = null;


    /**
     * Initialize MMBase using a <code>ServletContext</code>. This method will
     * check the servlet configuration for context parameters mmbase.outputfile
     * and mmbase.config. If not found it will look for system
     * properties.
     *
     * @throws ServletException  if mmbase.config is not set or is not a
     *                           directory or doesn't contain the expected
     *                           config files.
     *
     */
    public synchronized static void init(ServletContext servletContext) {
        if (!initialized ||
            sx == null) { // initialized, but with init(configPath)

            if (servletContext == null) {
                throw new IllegalArgumentException();
            }

            if (initialized) {
                log.info("Reinitializing, this time with ServletContext");
            }

            // store the current context
            sx = servletContext;
            log.service("Found servletContext " + sx);
            EventManager.getInstance().propagateEvent(new SystemEvent.ServletContext(sx), true);

            // Get the user directory using the user.dir property.
            // default set to the startdir of the appserver
            userDir = sx.getInitParameter("user.dir");
            if (userDir == null) {
                try {
                    userDir = System.getProperty("user.dir");
                } catch (SecurityException se) {
                    log.service(se.getMessage());
                }
            }
            // take into account userdir can start at webrootdir
            if (userDir != null && userDir.indexOf("$WEBROOT") == 0) {
                userDir = servletContext.getRealPath(userDir.substring(8));
            }
            // Init outputfile.
            String outputFile = sx.getInitParameter("mmbase.outputfile");
            if (outputFile == null) {
                try {
                    outputFile = System.getProperty("mmbase.outputfile");
                } catch (SecurityException se) {
                    log.debug(se.getMessage());
                }
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
	    try {
		userDir = System.getProperty("user.dir");
	    } catch (SecurityException se) {
		log.info(se.getMessage());
	    }

            // Init outputfile. // use of mmbase.outputfile  is deprecated!
	    try {
		initOutputfile(System.getProperty("mmbase.outputfile"));
	    } catch (SecurityException se) {
		log.info(se.getMessage());
	    }

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
    public static ThreadGroup getThreadGroup() {
        return org.mmbase.util.ThreadPools.threadGroup;
    }

    /**
     * Starts a daemon thread using the MMBase thread group.
     * @param task the task to run as a thread
     * @param name the thread's name
     * @since MMBase-1.8
     */
    public static Thread startThread(Runnable task, String name) {
        Thread kicker = new Thread(getThreadGroup(), task, getMachineName() + ":" + name);
        kicker.setDaemon(true);
        kicker.start();
        return kicker;
    }
    /**
     * Starts a daemon thread using the MMBase thread group.
     * @param task the task to run as a thread
     * @param name the thread's name
     * @deprecated   Use {@link org.mmbase.util.ThreadPools#scheduler}.
     * @since MMBase-1.8
     */
    public static DaemonThread startThread(DaemonTask task, String name) {
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
        try {
            log.info("java.version       : " +  System.getProperty("java.version"));
        } catch (SecurityException se) {
            log.info("java.version       : " +  se.getMessage());
        }

        log.info("user.dir          : " + userDir);
        String configPath = ResourceLoader.getConfigurationRoot().toString();
        log.info("configuration     : " + configPath);
        log.info("webroot           : " + ResourceLoader.getWebRoot());
        String version = org.mmbase.Version.get();
        log.info("version           : " + version);
        Runtime rt = Runtime.getRuntime();
        log.info("total memory      : " + rt.totalMemory() / (1024 * 1024) + " MiB");
        log.info("free memory       : " + rt.freeMemory() / (1024 * 1024) + " MiB");
        log.service("system locale     : " + Locale.getDefault());
        try {
            Locale locale = org.mmbase.util.LocalizedString.getLocale(org.mmbase.module.Module.getInitParameter("mmbaseroot", "language"));
            log.info("MMBase locale     : " + locale);
            org.mmbase.util.LocalizedString.setDefault(locale);
        } catch (IOException ioe) {
            log.error(ioe);
        }
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
                try {
                    htmlRoot = System.getProperty("mmbase.htmlroot");
                } catch (SecurityException se) {
                    log.debug(se);
                }
            }
            if (htmlRoot == null) {
                htmlRoot = sx.getRealPath("");
            }
            if (htmlRoot == null) {
                log.service("Parameter mmbase.htmlroot not set.");
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
     * When using Servlet 2.5, this is the same as {@link #getServletContext()}.getContextPath() +
     * "/", so this <em>also ends in a /</em>
     * @return  the HtmlRootUrlPath
     */
    public synchronized static String getHtmlRootUrlPath() {
        if (! htmlRootUrlPathInitialized) {
            log.debug("Finding root url");
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
                    } else if (sx.getMajorVersion() > 2 || (sx.getMajorVersion() == 2 && sx.getMinorVersion() >= 5)) {
                        try {
                            htmlRootUrlPath = (String) sx.getClass().getMethod("getContextPath").invoke(sx) + "/";
                        } catch(Exception e) {
                            log.error(e);
                        }
                    } else {
                        String url = sx.getResource("/").toString();
                        // MM: simply hope that it is the last part of that URL.
                        // I do not think it is garantueed. Used mmbase.htmlrooturlpath in web.xml if it doesn't work.
                        int length = url.length();
                        int lastSlash = url.substring(0, length - 1).lastIndexOf('/');
                        if (lastSlash > 0) {
                            htmlRootUrlPath = url.substring(lastSlash);
                            log.info("Found " + htmlRootUrlPath + " from " + url);
                        } else {
                            log.warn("Could not determine htmlRootUrlPath. Using default " + htmlRootUrlPath + "(contextUrl     :" + url + ")");
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                try {
                    ServletContext refound = sx.getContext(htmlRootUrlPath);
                    if (refound != null && !sx.equals(refound)) {
                        log.warn("Probably did not succeed in determining htmlRootUrlPath ('" + htmlRootUrlPath + "', because " + sx + "!= " + refound + "). Consider using the mmbase.htmlrooturlpath  context-param in web.xml");
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

    /**
     * Static version of {@link MMBase#getMachineName}
     * @since MMBase-1.8.7
     * @return 'machine name' to identify this web app or <code>null</code> if not yet determined.
     */
    public static String getMachineName() {
        return machineName;
    }
}
