/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import org.mmbase.module.Module;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

import org.w3c.dom.*;
import org.mmbase.util.XMLBasicReader;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;


/**
 * MMBaseServlet is a base class for other MMBase servlets (like ImageServlet). Its main goal is to
 * store a MMBase instance for all its descendants, but it can also be used as a serlvet itself, to
 * show MMBase version information.
 *
 * @version $Id: MMBaseServlet.java,v 1.19 2003-03-18 16:44:25 michiel Exp $
 * @author Michiel Meeuwissen
 * @since  MMBase-1.6
 */
public class MMBaseServlet extends  HttpServlet {

    private   static Logger log;
    protected static MMBase mmbase;
    // private   static String context;


    // ----------------
    // members needed for refcount functionality.

    /**
     * To keep track of the currently running servlets
     * switch the following boolean to true.
     *
     * @bad-constant
     */
    private static final boolean logServlets = true;
    private static int servletCount; // Number of running servlets
    /**
     *  Lock to sync add and remove of threads
     */
    private static Object servletCountLock = new Object();
    /**
     * Hashtable containing currently running servlets
     */
    private static Hashtable runningServlets = new Hashtable();
    /**
     * Toggle to print running servlets to log.
     * @javadoc Not clear, I don't understand it.
     */
    private static int printCount;

    private static int servletInstanceCount = 0;
    private static Map servletMappings    = new Hashtable();;
    private static Map associatedServlets = new Hashtable();

    /**
     * On default, servlets are not associated with any function.
     *
     * This function is called in the init method.
     *
     * @return A map of Strings (function) -> Integer (priority). Never null.
     */

    protected Map getAssociations() {
        return new Hashtable();
    }

    private static class ServletEntry {
        ServletEntry(String n) {
            this(n, null);
        }
        ServletEntry(String n, Integer p) {
            name = n;
            if (p == null) {
                priority = 0;
            } else {
                priority = p.intValue();
            }
        }
        String name;
        int    priority;
    }

    /**
     * The init of an MMBaseServlet checks if MMBase is running. It not then it is started.
     */

    public void init() throws ServletException {
        if (! MMBaseContext.isInitialized()) {
            ServletContext servletContext = getServletConfig().getServletContext();
            MMBaseContext.init(servletContext);
            MMBaseContext.initHtmlRoot();
        }
        if (log == null) {
            // Initializing log here because log4j has to be initialized first.
            log = Logging.getLoggerInstance(MMBaseServlet.class.getName());
        }

        log.info("Init of servlet " + getServletName() + ".");
        boolean initialize=false;
        // for retrieving servletmappings, determine status
        synchronized (servletMappings) {
            initialize = (servletInstanceCount == 0);
            servletInstanceCount++;
        }
        if (initialize) {
            // used to determine the accurate way to access a servlet
            try {
                // get config and do stuff.
                String path = MMBaseContext.getHtmlRoot() + "/WEB-INF/web.xml";
                log.service("Reading servlet mappings from " + path);
                XMLBasicReader webDotXml = new XMLBasicReader(path, false);
                Enumeration mappings = webDotXml.getChildElements("web-app","servlet-mapping");
                while (mappings.hasMoreElements()) {
                    Element mapping = (Element) mappings.nextElement();
                    Element servName = webDotXml.getElementByPath(mapping, "servlet-mapping.servlet-name");
                    String name = webDotXml.getElementValue(servName);
                    if (!(name.equals(""))) {
                        Element urlPattern=webDotXml.getElementByPath(mapping, "servlet-mapping.url-pattern");
                        String pattern=webDotXml.getElementValue(urlPattern);
                        if (!(pattern.equals(""))) {
                            List ls = (List) servletMappings.get(servName);
                            if (ls == null) {
                                ls = new Vector();
                                servletMappings.put(name, ls);
                            }
                            ls.add(pattern);
                        }
                    }
                }
                webDotXml = null;
            } catch (Exception e) {
                log.error(Logging.stackTrace(e));
            }
            log.debug("Loaded servlet mappings");
        }
        if (mmbase == null) {
            log.service("Creating MMBase module in " + getServletName());
            mmbase = MMBase.getMMBase();
            if (mmbase == null) {
                log.error("Could not find module with name 'MMBASEROOT'!");
            }
        }
        log.debug("Associating this servlet with functions");
        Iterator i = getAssociations().entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            associate((String) e.getKey(), getServletName(), (Integer) e.getValue());
        }

    }

    /**
     * Gets all the mappings for a given servlet. So, this is a method to obtain info from web.xml.
     *
     * @param servletName the name of the servlet
     * @return an unmodifiable list of servlet mappings for this servlet
     */
    public static List getServletMappings(String servletName) {
        List ls = (List) servletMappings.get(servletName);
        if (ls==null) {
            return Collections.EMPTY_LIST;
        } else {
            return Collections.unmodifiableList(ls);
        }
    }

    /**
     * Gets all the mappings for a given association.
     *
     * Use this to find out how to call a servlet to handle a certain
     * type of operation or data (i.e 'images', 'attachments').
     *
     *
     * @param topic the topic that deidentifies the type of association
     * @return an unmodifiable list of servlet mappings associated with the topic
     */
    public static List getServletMappingsByAssociation(String topic) {
        String name = getServletByAssociation(topic);
        if (name != null) {
            return getServletMappings(name);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets the name of the servlet that performs actions associated with the
     * the given keyword.
     *
     * Use this to find a servlet to handle a certain type of
     * operation or data (i.e 'imageservlet', 'myimageservlet',
     * 'images');
     *
     * @param topic the topic that deidentifies the type of association
     * @return the name of the servlet associated with the topic, or null if there is none
     */
    public static String getServletByAssociation(String topic) {
        ServletEntry e = ((ServletEntry) associatedServlets.get(topic));
        if (e != null) {
            return e.name;
        } else {
            return null;
        }
    }


    /**
     * Associate a given servlet with the the given topic.
     * Use this to set a servlet to handle a certain type of operation or data (i.e 'image-processing');
     * For now, only one servlet can be registered.
     * @param topic the topic that deidentifies the type of association
     * @param servletname name of the servlet to associate with the topic
     * @param  priority    priority of this association, the association only occurs if no servlet
     *                    with higher priority for the same topic is present already
     */
    private static synchronized void associate(String function, String servletName, Integer priority) {
        if (priority == null) priority = new Integer(0);
        ServletEntry e = (ServletEntry) associatedServlets.get(function);
        if (e == null || (priority.intValue() >= e.priority)) {
            log.service("Associating function '" + function + "' with servlet " + servletName + (e == null ? ""  : " (removing " + e.name +")"));
            associatedServlets.put(function, new ServletEntry(servletName, priority));
        }
    }

    /**
     * Serves MMBase version information. This doesn't do much usefull
     * yet, but one could image lots of cool stuff here. Any other
     * MMBase servlet will probably override this method.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        PrintWriter pw = res.getWriter();
        pw.print(org.mmbase.Version.get());
        pw.close();
    }

    /**
     * The service method is extended with calls for the refCount
     * functionality (for performance related debugging).  So you can
     * simply override doGet in extension classes, and this stays
     * working, without having to think about it.
     */
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException,IOException {
        incRefCount(req);
        try {
            super.service(req, res);
        } finally { // whatever happens, decrease the refcount:
            decRefCount(req);
        }
    }

    /**
     * Returns information about this servlet. Don't forget to override it.
     */

    public String getServletInfo()  {
        return "Serves MMBase version information";
    }



    // ----------------------
    // functions needed for refcount functionality.

    /**
     * Return URI with QueryString appended
     * @param req The HttpServletRequest.
     */
    protected static String getRequestURL(HttpServletRequest req) {
        String result = req.getRequestURI();
        String queryString = req.getQueryString();
        if (queryString!=null) result += "?" + queryString;
        return result;
    }


    /**
     * Decrease the reference count of the servlet
     * @param req The HttpServletRequest.
     */

    protected void decRefCount(HttpServletRequest req) {
        if (logServlets) {
            String url = getRequestURL(req);
            url += " " + req.getMethod();
            synchronized (servletCountLock) {
                servletCount--;
                ReferenceCountServlet s = (ReferenceCountServlet) runningServlets.get(this);
                if (s!=null) {
                    if (s.refCount==0) runningServlets.remove(this);
                    else {
                        s.refCount--;
                        int i = s.uris.indexOf(url);
                        if (i>=0) s.uris.removeElementAt(i);
                    }
                }// s!=null
            }//sync
        }// if (logServlets)
    }

    /**
     * Increase the reference count of the servlet (for debugging)
     * and send running servlets to log once every 32 requests
     * @param req The HttpServletRequest.
     * @scope private
     * @bad-constant  31 should be configurable.
     */

    protected void incRefCount(HttpServletRequest req) {
        if (logServlets) {
            String url = getRequestURL(req);
            url += " " + req.getMethod();
            int curCount;
            synchronized (servletCountLock) {
                servletCount++; curCount=servletCount; printCount++;
                ReferenceCountServlet s = (ReferenceCountServlet) runningServlets.get(this);
                if (s==null) runningServlets.put(this, new ReferenceCountServlet(this, url, 0));
                else { s.refCount++; s.uris.addElement(url); }
            }// sync

            if ((printCount & 31) == 0) { // Why not (printCount % <configurable number>) == 0?
                if (curCount > 0) {
                    log.info("Running servlets: " + curCount);
                    for(Enumeration e=runningServlets.elements(); e.hasMoreElements();)
                        log.info(e.nextElement());
                }// curCount>0
            }
        }
    }

    public void destroy() {
        log.info("Servlet " + getServletName() + " is taken out of service");
        super.destroy();
         // for retrieving servletmappings, determine status
        synchronized (servletMappings) {
           servletInstanceCount--;
            if (servletInstanceCount == 0) {
                log.info("Unloaded servlet mappings");
                associatedServlets.clear();
                servletMappings.clear();
                log.info("No MMBase servlets left; modules can be shut down");
                Module.shutdownModules();
                Logging.shutdown();
                mmbase = null;
                log    = null;
            }
        }
   }

    /**
     * This class maintains current state information for a running servlet.
     * It contains a reference count, as well as a list of URI's being handled by the servlet.
     */
    private class ReferenceCountServlet {
        /**
         * The servlet do debug
         * @scope private
         */
        MMBaseServlet servlet;
        /**
         * List of URIs that call the servlet
         * @scope private
         */
        Vector uris = new Vector();
        /**
         * Nr. of references
         * @scope private
         */
        int refCount;

        /**
         * Create a new ReferenceCountServlet using the jamesServlet
         */
        ReferenceCountServlet(MMBaseServlet servlet, String uri, int refCount) {
            this.servlet = servlet;
            uris.add(uri);
            this.refCount = refCount;
        }

        /**
         * Return a description containing servlet info and URI's
         */
        public String toString() {
            return "servlet("+servlet+"), refcount("+(refCount+1)+"), uri's("+uris+")";
        }
    }


}
