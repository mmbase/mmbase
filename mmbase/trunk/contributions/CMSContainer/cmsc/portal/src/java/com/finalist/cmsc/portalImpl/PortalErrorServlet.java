/*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license

 */
package com.finalist.cmsc.portalImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import net.sf.mmapps.commons.util.HttpUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.finalist.cmsc.beans.om.Site;
import com.finalist.cmsc.portalImpl.registry.PortalRegistry;
import com.finalist.cmsc.services.sitemanagement.SiteManagement;
import com.finalist.pluto.portalImpl.core.*;
import com.finalist.util.version.VersionUtil;

@SuppressWarnings("serial")
public class PortalErrorServlet extends PortalServlet {

    private static Log log = LogFactory.getLog(PortalErrorServlet.class);

    public static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    public static final String ERROR_MESSAGE = "javax.servlet.error.message";
    public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    
    protected ServletConfig config;

    protected static final String[] vars = {
        ERROR_STATUS_CODE,
        ERROR_EXCEPTION_TYPE,
        ERROR_MESSAGE,
        ERROR_EXCEPTION,
        ERROR_REQUEST_URI
    };

    @Override
    public void init(ServletConfig config) {
        // do not start the portal
        this.config = config;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("===>PortalErrorServlet.doGet START!");

        if (PortletContainerFactory.getPortletContainer().isInitialized()) {
            // PortalRegistry reg = PortalRegistry.getPortalRegistry(request);
            PortalEnvironment env = new PortalEnvironment(request, response, config);
            PortalURL currentURL = env.getRequestedPortalURL();
            try {
                String path = extractPath(request, currentURL);
                Integer statusCode = (Integer) request.getAttribute(ERROR_STATUS_CODE);
                log.debug("===>getErrorScreen:'" + path + "'");
                Site site = SiteManagement.getSiteFromPath(path);
                Site errorPageSite = null;
                if (site != null) {
                    if (SiteManagement.isNavigation(site.getUrlfragment() + PATH_SP + statusCode)) {
                        errorPageSite = site;
                    }
                }
                else {
                    List<Site> sites = SiteManagement.getSites();
                    for (Site site2 : sites) {
                        if (SiteManagement.isNavigation(site2.getUrlfragment() + PATH_SP + statusCode)) {
                            errorPageSite = site2;
                            break;
                        }
                    }
                }
                if(errorPageSite != null) {
                    String errorPagePath = errorPageSite.getUrlfragment() + PATH_SP + statusCode;
 /** [FP]                   ScreenFragment screen = getScreen(errorPagePath);
                    if (screen != null) {
                        logError(request);
                        
                        HttpServletRequest errorRequest = new ErrorHttpServletRequest(request, errorPagePath);
                        PortalEnvironment errorEnv = new PortalEnvironment(errorRequest, response, config);
                        
                        PortalRegistry registry = PortalRegistry.getPortalRegistry(request);
                        response.setContentType(CONTENT_TYPE);
                        ScreenFragment oldScreen = registry.getScreen();
                        registry.setScreen(screen);
                        screen.service(request, response);
                        
                        registry.setScreen(oldScreen);
                        
                    }*/
                    
                    PortalRegistry registry = PortalRegistry.getPortalRegistry(request);
                    boolean renderSucceed = doRender(request, response, registry, errorPagePath);
                    if(!renderSucceed) {
                        defaultError(request, response, statusCode);
                    }
                }
                else {
                    defaultError(request, response, statusCode);
                }
            } catch (Throwable t) {
                log.fatal("Error processing", t);
            }
        }
        else {
            basicErrorPage(request, response);
        }
        log.debug("===>PortalErrorServlet.doGet EXIT!");
    }

    private void defaultError(HttpServletRequest request, HttpServletResponse response,
            Integer statusCode) throws ServletException, IOException {
        RequestDispatcher rd = config.getServletContext().getRequestDispatcher("/error/" + statusCode + ".jsp");
        if (rd != null) {
            rd.forward(request, response);
        }
        else {
            logError(request);
            basicErrorPage(request, response);
        }
    }

    private void basicErrorPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>" + request.getAttribute(ERROR_STATUS_CODE)
                + " " + request.getAttribute(ERROR_MESSAGE) + "</title></head><body><table>"); 
        // Print vars
        for (int i = 0; i < vars.length; i++) {
            out.println(
                "<tr><td>" + vars[i] + "</td><td>" +
                request.getAttribute(vars[i]) + 
                "</td></tr>");
        }
        out.println("</table></body></html>");
    }

    public void logError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(ERROR_STATUS_CODE);
        Throwable exception = (Throwable) request.getAttribute(ERROR_EXCEPTION);
        if (statusCode == 500) {
            String version = VersionUtil.getApplicationVersion(config.getServletContext());
            // prepare error ticket
            long ticket = System.currentTimeMillis();

            String msg = HttpUtil.getErrorInfo(request, exception, ticket, version);

            String message = "";
            if (exception != null) {
                message = exception.getMessage();
                if (message == null) {
                    message = exception.toString();
                }
            }
            // write errors to mmbase log
            log.error(ticket + ":\n" + msg);
        }
    }
    
    
    class ErrorHttpServletRequest extends HttpServletRequestWrapper {
        
        private String errorPagePath;

        public ErrorHttpServletRequest(HttpServletRequest request, String errorPagePath) {
            super(request);
            this.errorPagePath = errorPagePath;            
        }
        
        @Override
        public String getServletPath() {
            return errorPagePath;
        }
        
    }
}
