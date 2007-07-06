/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.*;

import javax.servlet.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;

import org.mmbase.util.*;
import org.mmbase.util.functions.*;
import org.mmbase.servlet.*;
import org.mmbase.module.core.*;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Requestfilter that filters out all URL's looking for virtual 'userfriendly' links that have a 
 * corresponding page (technical URL) within the website. When the recieved URL is not
 * recognized by the framework as an 'userfriendly' one, it just gets forwarded in its original 
 * form.
 * Regular expressions that define URL's to be excluded from filtering should be listed in the
 * 'excludes' parameter in web.xml.
 * The filtering and conversion to an URL pointing to an existing JSP template is 
 * done by implementations of UrlConverter.
 *
 * @author Andr&eacute; van Toly
 * @version $Id: FrameworkFilter.java,v 1.8 2007-07-06 14:22:47 andre Exp $
 */

public class FrameworkFilter implements Filter, MMBaseStarter  {
    
    private static final Logger log = Logging.getLoggerInstance(FrameworkFilter.class);
    
    /**
     * The context this servlet lives in
     */
    protected ServletContext ctx = null;

    /**
     * MMBase needs to be started first to be able to load config
     */
    private static MMBase mmbase;
    private Thread initThread;
    
    /**
     * The pattern being used to determine to exlude an URL
     */
    private static Pattern excludePattern;
        
    /*
     * Methods that need to be overriden form MMBaseStarter
     */
    public MMBase getMMBase() {
        return mmbase;
    }

    public void setMMBase(MMBase mm) {
        mmbase = mm;
    }

    public void setInitException(ServletException se) {
        // never mind, simply ignore
    }

    /**
     * Initialize the filter, called on webapp startup
     *
     * @param config object containing init parameters specified
     * @throws ServletException thrown when an exception occurs in the web.xml
     */
    public void init(FilterConfig config) throws ServletException {
        log.info("Starting UrlFilter");
        ctx = config.getServletContext();
        String excludes = config.getInitParameter("excludes");
        if (excludes != null && excludes.length() > 0) {
            excludePattern = Pattern.compile(excludes);
        }
        
        /* initialize MMBase if its not started yet */
        MMBaseContext.init(ctx);
        MMBaseContext.initHtmlRoot();
        initThread = new MMBaseStartThread(this);
        initThread.start();
        
        log.info("UrlFilter initialized");
    }
    
    /**
     * Destroy method
     */
    public void destroy() {
        // nothing needed here
    }
    

    public static String getPath(HttpServletRequest request) {
        String path = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (path != null) path = path.substring(request.getContextPath().length());
        path = (String) request.getRequestURI();
        if (path != null) path = path.substring(request.getContextPath().length());
        // i think path is always != null now.
        if (path == null) path = request.getServletPath();
        if (path == null) path = request.getPathInfo();
        return path;
    }
    

    /**
     * Filters a request and delegates it to UrlConverter if needed.
     * URL conversion is only done when the URI does not match one of the excludes in web.xml.
     * Waits for MMBase to be up.
     *
     * @param request   incoming request
     * @param response  outgoing response
     * @param chain     a chain object, provided for by the servlet container
     * @throws ServletException thrown when an exception occurs
     * @throws IOException thrown when an exception occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        if (mmbase == null) {
            if (log.isDebugEnabled()) log.debug("Still waiting for MMBase (not initialized)");
            chain.doFilter(request, response);
            return;
        }
        
        if (request instanceof HttpServletRequest) {

        
            HttpServletRequest req = (HttpServletRequest) request;
            if (log.isTraceEnabled()) {
                log.trace("Request URI: " + req.getRequestURI());
                log.trace("Request URL: " + req.getRequestURL());
                Enumeration e = request.getAttributeNames();
                while (e.hasMoreElements()) {
                    String att = (String) e.nextElement();
                    log.trace("attribute " + att + ": " + request.getAttribute(att));
                }
            }

            HttpServletResponse res = (HttpServletResponse) response;
            String path = getPath(req);
            if (log.isDebugEnabled()) log.debug("Processing path: " + path);
            if (path != null) {
                try {
                    if (excludePattern != null && excludePattern.matcher(path).find()) {
                        chain.doFilter(request, response);  // url is excluded from further actions
                        return;
                    }
                } catch (Exception e) {
                    log.fatal("Could not process exclude pattern: " + e);
                }
            }
            
            // URL is not excluded, pass it to UrlConverter to process and forward the request
            Framework fw =  MMBase.getMMBase().getFramework();
            Parameters params = fw.createParameters();
            if (params.containsParameter(Parameter.REQUEST)) {
                params.set(Parameter.REQUEST, req);
            }
            if (params.containsParameter(Parameter.RESPONSE)) {
                params.set(Parameter.RESPONSE, res);
            }
            String forwardUrl = fw.getInternalUrl(path, req.getParameterMap().entrySet(), params).toString();
            
            if (log.isDebugEnabled()) {
                log.debug("Received '" + forwardUrl + "' from framework, forwarding.");
            }
            if (forwardUrl != null && !forwardUrl.equals("")) {
                /* 
                 * RequestDispatcher: If the path begins with a "/" it is interpreted
                 * as relative to the current context root.
                 */
                RequestDispatcher rd = request.getRequestDispatcher(forwardUrl);
                rd.forward(request, response);
            } else {
                if (log.isDebugEnabled()) log.debug("No matching technical URL, just forwarding: " + path);
                chain.doFilter(request, response);
            }
        } else {
            if (log.isDebugEnabled()) log.debug("Request not an instance of HttpServletRequest, therefore no url forwarding");
            chain.doFilter(request, response);
        }
    }
    
}

