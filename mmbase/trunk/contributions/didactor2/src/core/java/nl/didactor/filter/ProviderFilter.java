/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package nl.didactor.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.util.*;
import org.mmbase.util.functions.*;
import org.mmbase.storage.search.*;

import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseContext;
import org.mmbase.servlet.*;
import org.mmbase.core.event.*;

import org.mmbase.util.logging.*;

/**
 * Redirects request based on information supplied by the jumpers builder.
 *

 * @author Michiel Meeuwissen
 * @version $Id: ProviderFilter.java,v 1.12 2007-07-26 14:35:52 michiel Exp $
 */
public class ProviderFilter implements Filter, MMBaseStarter, NodeEventListener, RelationEventListener {
    private static final Logger log = Logging.getLoggerInstance(ProviderFilter.class);
    private static MMBase mmbase;

    private static Map<String, Map<String, Object>> providerCache = new HashMap<String, Map<String, Object>>();

    public static void clearCache() {
        providerCache.clear();
    }

    /**
     * Initializes the filter
     */
    public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
        log.info("Starting ProviderFilter with " + filterConfig);
        MMBaseContext.init(filterConfig.getServletContext());
        MMBaseContext.initHtmlRoot();
        // stuff that can take indefinite amount of time (database down and so on) is done in separate thread
        Thread initThread = new MMBaseStartThread(this);
        initThread.start();
    }
    public void setInitException(ServletException se) {
        // never mind, simply, ignore
    }


    public MMBase getMMBase() {
        return mmbase;
    }

    public void setMMBase(MMBase mmb) {
        mmbase = mmb;
        EventManager.getInstance().addEventListener(this);        
    }

    public void notify(NodeEvent event) {
        String builder = event.getBuilderName();
        if ("providers".equals(builder) || "educations".equals(builder)) {
            log.info("Clearing provider cache because " + event);
            providerCache.clear();
        }
    }
    public void notify(RelationEvent event) {
        if (event.getRelationDestinationType().equals("urls")) {
            String builder = event.getRelationSourceType();
            if ("providers".equals(builder) || "educations".equals(builder)) {
                log.info("Clearing provider cache because " + event);
                providerCache.clear();
            }
        }
    }

    protected Node selectByRelatedUrl(NodeList nodes, String url) {
        log.debug("Select  for " + url);
        NodeIterator ni = nodes.nodeIterator();
        while (ni.hasNext()) {
            Node suggestion = ni.nextNode();
            NodeList urls = suggestion.getRelatedNodes("urls");
            NodeIterator ui = urls.nodeIterator();
            while (ui.hasNext()) {
                Node urlNode = ui.nextNode();
                String u = urlNode.getStringValue("url");
                if (u.equals(url)) {
                    log.debug("found "  + suggestion.getNumber());
                    return suggestion;
                }
            }
        }
        return null;
    }

    protected Locale findLocale(Node provider, Node education) {
        Locale locale;
        {
            String ls = provider.getStringValue("locale");
            if (ls == null || "".equals(ls)) {
                locale = provider.getCloud().getCloudContext().getDefaultLocale();
            } else {
                locale = LocalizedString.getLocale(ls);
            }
        }

        String variant = provider.getStringValue("path");
        if (variant != null && (! "".equals(variant)) && education != null) {
            String educationPath = education.getStringValue("path");
            if (educationPath != null && (! "".equals(educationPath))) {
                variant += "_" + educationPath;
            }
        }
        if (variant != null && ! "".equals(variant)) {
            return new Locale(locale.getLanguage(), locale.getCountry(), variant);
        } else {
            return locale;
        }

    }

    protected String getSessionName() {
        return "cloud_mmbase";
    }

    protected Cloud getCloud(HttpServletRequest req) {
        HttpSession session = req.getSession(false); // false: do not create a session, only use it
        if (session == null) {
            return ContextProvider.getDefaultCloudContext().getCloud("mmbase"); 
        } else {
            log.debug("from session");
            Object c = session.getAttribute(getSessionName());
            if (c != null) {
                if (c instanceof Cloud) {
                    return (Cloud) c;
                } else {
                    log.warn("" + c + " is not a Cloud, but a " + c.getClass());
                    return ContextProvider.getDefaultCloudContext().getCloud("mmbase"); 
                }
            } else {
                return ContextProvider.getDefaultCloudContext().getCloud("mmbase"); 
            }
        }
    }

    protected Node getUser(Cloud cloud) {
        return nl.didactor.security.Authentication.getCurrentUserNode(cloud);
    }

    protected Node getEducation(Cloud cloud, Node provider, String[] urls, Map<String, Object> attributes) {
        
        Node education = null;
        NodeList educations;
        if (provider != null) {
            educations = provider.getRelatedNodes("educations");
            if (educations.size() == 0) {
                log.warn("Provider " + provider + " has no education");
                educations = cloud.getNodeManager("educations").getList(null, null, null);
            }
        } else {
            // there was no provider found yet, so we try educations only
            educations = cloud.getNodeManager("educations").getList(null, null, null);
        }
        
        for (String u : urls) {
            education = selectByRelatedUrl(educations, u);
            if (education != null) break;
        }                    
        
        
        if (education == null && provider != null) {
            // no url related to the educations, simply take one related to the provider if
            // that was found.
            NodeList eds = provider.getRelatedNodes("educations");
            if (eds.size() > 0) {
                education = eds.nodeIterator().nextNode();
            }   
        }
        
        if (education == null && educations.size() > 0) {
            // Still no education found, if there are education at all, simply guess one.
            education = educations.nodeIterator().nextNode();
        }
        
        if (education != null) {
            log.debug("Found education " + education.getNumber());
            attributes.put("education", education.getNumber()); 
        } else {
            attributes.put("education", null);
        }
        return education;
    }
    

    /**
     * Filters the request: tries to find a jumper and redirects to this url when found, otherwise the 
     * request will be handled somewhere else in the filterchain.
     * @param servletRequest The ServletRequest.
     * @param servletResponse The ServletResponse.
     * @param filterChain The FilterChain.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws java.io.IOException, ServletException {
        if (mmbase == null || ! mmbase.getState()) {
            // if mmbase not yet running. Things not using mmbase can work, otherwise this may give
            // 503.
            log.debug("NO MMBASE member");
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;

        String sp = req.getServletPath();
        if (sp.startsWith("/images/") ||
            sp.startsWith("/attachments/")) {
            // no jsps here, these are blobs. 
            log.debug("No need to filter for " + sp);
            filterChain.doFilter(request, response);
            return;
        }

        boolean useParameters = true;
        if(sp.startsWith("/mmbase/")) {
            // all kind of generic jsp's can be there. reuest parameters can be used for something  else.
            useParameters = false;
        }


        String serverName = req.getServerName();
        String contextPath = req.getContextPath();
        HttpSession session = req.getSession(false);        


        String educationParameter = useParameters && session != null ? (String) session.getAttribute("education") : null;
        if (educationParameter == null && useParameters) educationParameter = req.getParameter("education");

        if (educationParameter != null && useParameters && session != null) {
            // remember some explicit education parameter in the session.
            session.setAttribute("education", educationParameter);
        }

        String providerParameter  = useParameters ? req.getParameter("provider") : null;

        Cloud cloud = getCloud(req);
        Map<String, Object> userAttributes;
        if (session == null) {
            log.debug("no session");
            userAttributes = new HashMap<String, Object>();
            userAttributes.put("user", 0);
        } else {
            userAttributes = (Map<String, Object>) session.getAttribute("nl.didactor.user_attributes");
            if (userAttributes == null) {
                userAttributes = new HashMap<String, Object>();
                session.setAttribute("nl.didactor.user_attributes", userAttributes);
            }
            
            Node user = getUser(cloud);
            userAttributes.put("user", user == null ? 0 : user.getNumber());
        }

        String key = serverName + contextPath + ':' + educationParameter + ':' + providerParameter;
        Map<String, Object> attributes = providerCache.get(key);
        if (attributes == null) {

            String[] urls = {"http://" + serverName + contextPath, 
                             "http://" + serverName, 
                            //serverName cannot alone consitute a valid URL, but this was the only
                            //implemention in previous versions of Didactor.
                             serverName};

            attributes = new HashMap<String, Object>();

            Node provider = null;            
            {
                if (cloud.hasNode(providerParameter)) {
                    // explicitely stated provider on the URL.
                    // Don't know if that should be maintained.
                    provider = cloud.getNode(providerParameter);
                } else {
                    NodeList providers = cloud.getNodeManager("providers").getList(null, null, null);
                    if (providers.size() == 0) {
                        // create one
                        log.info("No provider objects found, should at least be one");                    
                    } else if (providers.size() == 1) {
                        provider = providers.getNode(0);
                    } else {
                        // which are we going to use?
                        for (String u : urls) {
                            provider = selectByRelatedUrl(providers, u);
                            if (provider != null) break;
                        }

                        // no matching URL object directly related to provider.
                        // Try via education object too.
                    }                    
                }
            }
            Node education = getEducation(cloud, provider, urls, attributes);

            // try determining provider if education found, but not yet a provider
            if (provider == null && education != null) {
                NodeList providers = education.getRelatedNodes("providers");
                if (providers.size() > 0) {
                    provider = providers.nodeIterator().nextNode();
                }
            }


            if (provider == null) {
                NodeList providers = cloud.getNodeManager("providers").getList(null, null, null);
                if (providers.size() > 0) {
                    provider = providers.getNode(0);
                    education = getEducation(cloud, provider, urls, attributes);
                }
            }
            Locale locale; 
            if (provider != null) {
                log.debug("Found provider " + provider.getNumber());
                attributes.put("provider", provider.getNumber());
                locale = findLocale(provider, education);
            } else {
                log.warn("No provider found for " + key);
                attributes.put("provider", null);
                locale = cloud.getLocale();
            }
            
            attributes.put("javax.servlet.jsp.jstl.fmt.locale.request", locale);
            attributes.put("language", locale.toString());
            attributes.put("locale", locale);

            if (education == null) {
                log.warn("No education found for " + key);
            }

            if (provider == null) {
                if (req.getServletPath().startsWith("/mmbase")) {
                    filterChain.doFilter(request, response);
                } else {
                    HttpServletResponse res = (HttpServletResponse) response;
                    request.setAttribute("org.mmbase.servlet.error.message", "No provider found for '" + key + "'");
                    res.sendError(HttpServletResponse.SC_NOT_FOUND, "No provider found for '" + key + "'");
                }
                return;
                    
            }

            if (education != null) {
                attributes.put("includePath", provider.getNumber() + "," + education.getNumber());
            } else {
                attributes.put("includePath", "" + provider.getNumber());
            }
            attributes.put("referids", "class?,workgroup?");

            providerCache.put(key, attributes);

            if (log.isDebugEnabled()) {
                log.debug("Found attributes for " + key + " " + attributes);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Found attributes for " + key + " " + attributes);
            }
        }

        
        // Based on the student and the education, try to find the class.
        String c = useParameters ? request.getParameter("class") : null;
        if (c != null) {
            if (cloud.hasNode(c)) {
                userAttributes.put("class", cloud.getNode(c));
            }
        } else {
            Object education = attributes.get("education");
            if (education != null && userAttributes.get("class") == null) {
                try {
                    Node user = cloud.getNode((Integer) userAttributes.get("user"));
                    Function fun = user.getFunction("class");
                    Parameters params = fun.createParameters();
                    params.set("education", education);
                    Node claz = (Node) fun.getFunctionValue(params);
                    userAttributes.put("class", claz);
                } catch (NotFoundException nfe) {
                    // never mind
                    userAttributes.put("class", null);
                }
            }
        }

        // copy all attributes to the request.
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        assert request.getAttribute("provider") != null : "attributes" + attributes; 
        for (Map.Entry<String, Object> entry : userAttributes.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }

        filterChain.doFilter(request, response);
    }



    public void destroy() {
    }


}
