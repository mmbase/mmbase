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

import org.mmbase.util.functions.*;
import org.mmbase.util.transformers.UrlEscaper;
import org.mmbase.servlet.*;
import org.mmbase.module.core.*;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Requestfilter that filters out all URL's looking for virtual 'userfriendly' links that have a
 * corresponding page (technical URL) within the website. When the recieved URL is not
 * recognized by the framework as an 'userfriendly' one, it just gets forwarded in its original
 * form. The filtering and conversion to an URL pointing to an existing JSP template is done by
 * an {@link org.mmbase.framework.basic.UrlConverter}.
 * <p>
 * Regular expressions that define URL's to be excluded from filtering should be listed in the
 * 'excludes' parameter in web.xml.
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 * @since MMBase-1.9
 */

public class FrameworkFilter implements Filter, MMBaseStarter  {

    /**
     * Access to the implicit framework parameters for the current thread. This incluses e.g. the current request
     * @since 1.9.7
     */
    public static final ThreadLocal<Parameters> FRAMEWORK_PARAMETERS = ThreadLocal.withInitial(() -> null);

    public static final String PARAMS_KEY = "org.mmbase.framework.filter.parameters";

    private static Logger log = Logging.getLoggerInstance(FrameworkFilter.class);

    /**
     * MMBase needs to be started first to be able to load config
     */
    private MMBase mmbase;
    private Thread initThread;

    /**
     * The pattern being used to determine to exclude an URL
     */
    private static Pattern excludePattern;


    private static long forwarded = 0;
    private static long included  = 0;
    private static long chained   = 0;
    private static long errors    = 0;

    public static long getForwardedRequests() {
        return forwarded;
    }
    public static long getIncludedRequests() {
        return included;
    }
    public static long getChainedRequests() {
        return chained;
    }
    public static long getErrorRequests() {
        return errors;
    }

    /*
     * Methods that need to be overriden form MMBaseStarter
     */
    public MMBase getMMBase() {
        return mmbase;
    }

    public void setMMBase(MMBase mm) {
        mmbase = mm;
        // logging is not coxmpletey initialized, replace logger instance too
        log = Logging.getLoggerInstance(FrameworkFilter.class);
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
        ServletContext ctx = config.getServletContext();
        String excludes = config.getInitParameter("excludes");
        if (excludes != null && excludes.length() > 0) {
            excludePattern = Pattern.compile(excludes);
        }

        /* initialize MMBase if its not started yet */
        if (! MMBaseContext.isInitialized()) {
            MMBaseContext.init(ctx);
            MMBaseContext.initHtmlRoot();
        }

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


    private static final UrlEscaper URL_ESCAPER = new UrlEscaper();
    public static String getPath(HttpServletRequest request) {
        String path = (String) request.getAttribute("javax.servlet.forward.servlet");
        if (path == null) {
            path = (String) request.getAttribute("javax.servlet.forward.request_uri");
            if (path != null) path = path.substring(request.getContextPath().length());
        }
        if (path == null) {
            path = request.getRequestURI();
            if (path != null) path = path.substring(request.getContextPath().length());
        }
        // i think path is always != null now.
        if (path == null) path = request.getPathInfo();

        return URL_ESCAPER.transformBack(path);
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
        Object prevIp = Logging.getMDC().get("IP");
        try {

            if (request instanceof HttpServletRequest) {

                if (mmbase == null) {
                    if (log.isDebugEnabled()) log.debug("Still waiting for MMBase (not initialized)");
                    chained++;
                    chain.doFilter(request, response);
                    return;
                }
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || "".equals(ip)) {
                    ip = req.getRemoteAddr();
                }
                Logging.getMDC().put("IP", ip);


                if (log.isTraceEnabled()) {
                    log.trace("Request URI: " + req.getRequestURI());
                    log.trace("Request URL: " + req.getRequestURL());
                    Enumeration e = request.getAttributeNames();
                    while (e.hasMoreElements()) {
                        String att = (String) e.nextElement();
                        log.trace("attribute " + att + ": " + request.getAttribute(att));
                    }
                }


                String path = getPath(req);
                Logging.getMDC().put("path", path);
                if (log.isDebugEnabled()) log.debug("Processing path: " + path);
                if (path != null) {
                    if (excludePattern != null && excludePattern.matcher(path).find()) {
                        chained++;
                        chain.doFilter(request, response);  // url is excluded from further actions
                        return;
                    }
                }

                // URL is not excluded, pass it to UrlConverter to process and forward the request
                Framework fw =  Framework.getInstance();
                if (fw == null) {
                    log.error("No MMBase framework found");
                    chained++;
                    chain.doFilter(request, response);
                    return;
                }
                Parameters frameworkParameters = fw.createParameters();
                if (frameworkParameters.containsParameter(Parameter.REQUEST)) {
                    frameworkParameters.set(Parameter.REQUEST, req);
                }
                if (frameworkParameters.containsParameter(Parameter.RESPONSE)) {
                    frameworkParameters.set(Parameter.RESPONSE, res);
                }
                if (frameworkParameters.containsParameter(Parameter.CLOUD)) {
                    frameworkParameters.set(Parameter.CLOUD, org.mmbase.bridge.ContextProvider.getDefaultCloudContext().getCloud("mmbase"));
                }
                FRAMEWORK_PARAMETERS.set(frameworkParameters);
                try {
                    @SuppressWarnings("unchecked")
                    String forwardUrl = fw.getInternalUrl(path, req.getParameterMap(), frameworkParameters);

                    if (log.isDebugEnabled()) {
                        log.debug("Received '" + forwardUrl + "' from framework, forwarding. rp:" + req.getParameterMap() + " fwp:" + frameworkParameters);
                    }

                    if (forwardUrl != null && !forwardUrl.equals("")) {
                        res.setHeader("X-MMBase-forward", forwardUrl);
                        req.setAttribute(PARAMS_KEY, frameworkParameters);
                        if (req.getAttribute(org.mmbase.Version.class.getName()) == null) {
                            req.setAttribute(org.mmbase.Version.class.getName(), org.mmbase.Version.get());
                            res.addHeader("X-Powered-By", org.mmbase.Version.get());
                        }
                        /*
                         * RequestDispatcher: If the path begins with a "/" it is interpreted
                         * as relative to the current context root.
                         */
                        RequestDispatcher rd = request.getRequestDispatcher(forwardUrl);
                        if(response.isCommitted()){
                            log.debug("** response committed, including");
                            included++;
                            rd.include(request, response);
                        }else{
                            log.debug("** response not committed, forwarding");
                            forwarded++;
                            rd.forward(request, response);
                        }
                        log.debug("Ready");
                    } else {
                        if (log.isDebugEnabled()) log.debug("No matching technical URL, just forwarding: " + path);
                        chained++;
                        chain.doFilter(request, response);
                    }
                } catch (FrameworkException fe) {
                    errors++;
                    throw new ServletException(fe);
                } catch (ServletException se) {
                    errors++;
                    throw se;
                } catch (IOException ioe) {
                    errors++;
                    throw ioe;
                } catch (RuntimeException re) {
                    errors++;
                    throw re;
                }
            } else {
                if (log.isDebugEnabled()) log.debug("Request not an instance of HttpServletRequest, therefore no url forwarding");
                chained++;
                chain.doFilter(request, response);
            }
        } finally {
            Logging.getMDC().put("IP", prevIp);
            Logging.getMDC().put("path", null);
            FRAMEWORK_PARAMETERS.remove();

        }
    }

}

