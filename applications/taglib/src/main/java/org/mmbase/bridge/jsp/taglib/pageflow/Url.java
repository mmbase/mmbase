/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.pageflow;


import java.util.*;
import java.util.regex.Pattern;
import java.net.*;
import javax.servlet.jsp.JspTagException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mmbase.bridge.jsp.taglib.TaglibException;
import org.mmbase.bridge.jsp.taglib.ContextReferrerTag;
import org.mmbase.util.Casting;
import org.mmbase.util.functions.*;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.State;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.BasicUrlConverter;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * <p>
 * A lazy 'URL' creator. A container object that contains all necessary information to construct an URL, but
 * will only do it on actual request (by the {@link #get}) method. This is also what is stored by an
 * url-tag with an id attribute.
 * </p>
 * <p>
 * The creation of the URL is delegated to the MMBase framework.
 * </p>
 * @version $Id$;
 * @since MMBase-1.9
 */
public class Url implements Comparable, CharSequence, Casting.Unwrappable {
    private static final Logger log = Logging.getLoggerInstance(Url.class);
    private static  final Logger pageLog = Logging.getLoggerInstance(Logging.PAGE_CATEGORY);

    private final ContextReferrerTag tag;
    private CharSequence page;
    //private final Component component;
    protected final Map<String, Object> params;
    protected final Map<String, Object> frameworkParams;
    private final String abs;
    private final boolean encodeUrl;
    private final boolean escapeAmps;

    private String cacheEscapeAmp = null;
    private String cacheNoEscapeAmp = null;
    private String string = null;
    private final boolean internal;
    private boolean process = false;

    private boolean legacy = false;

    public Url(UrlTag t,
               CharSequence p,
               Map<String, Object> framework,
               Map<String, Object> pars,
               boolean intern) throws JspTagException {
        tag = t;
        abs = t.getAbsolute();
        encodeUrl = t.encode();
        escapeAmps = t.escapeAmps();
        page = p;
        params = pars;
        frameworkParams = framework;
        internal = intern;
    }

    public Url(UrlTag t,
               Url u,
               Map<String, Object> framework,
               Map<String, Object> pars,
               boolean intern) throws JspTagException {
        tag = t;
        abs = t.getAbsolute();
        encodeUrl = t.encode();
        escapeAmps = t.escapeAmps();
        page = u.page;
        params = pars;
        frameworkParams = framework;
        internal = intern;
    }


    /**
     * Used e.g. by mm:form
     */
    public Url(ContextReferrerTag t, CharSequence p, String a) throws JspTagException {
        tag = t;
        abs = a;
        encodeUrl = true;
        escapeAmps = true;
        page = p;
        params = new HashMap<String, Object>();
        frameworkParams = null;
        internal = false;
    }


    /**
     * @since MMBase-1.9.1
     */
    Url(Url u, boolean escapeAmps, boolean encode) {
        this.tag = u.tag;
        this.abs = u.abs;
        this.encodeUrl = encode;
        this.escapeAmps = escapeAmps;
        this.page = u.page;
        this.params = u.params;
        this.frameworkParams = u.frameworkParams;
        this.internal = u.internal;
    }


    public static Component getComponent(ContextReferrerTag tag) {
        HttpServletRequest req = (HttpServletRequest) tag.getPageContext().getRequest();
        State state =  State.getState(req);
        if (state.isRendering()) {
            return state.getBlock().getComponent();
        } else {
            return null;
        }
    }

    public void setProcess() {
        process = true;
    }

    public void setLegacy() {
        this.legacy = true;
    }

    public String getLegacy(boolean escapeamp) throws JspTagException {
        Map<String, Object> m = new HashMap<String, Object>();
        if (params != null) {
            m.putAll(params);
        }
        if (log.isDebugEnabled()) {
            log.debug("legacy url " + page + m);
        }
        String res = BasicUrlConverter.getUrl(page.toString(), m, (HttpServletRequest)tag.getPageContext().getRequest(), escapeamp).toString();
        pageLog.service("getting legacy: " + page + " -> " + res);
        return res;
      }



    private static final Pattern EXTERNAL = Pattern.compile("[a-z]{2,}:.*");

    /**
     * Returns the URL as a String, always without the application context. Never <code>null</code>
     */
    public String get(boolean escapeamp) throws JspTagException, FrameworkException {

        String result = escapeamp ? cacheEscapeAmp : cacheNoEscapeAmp;
        if (result != null) {
            log.debug("found cached " + result);
            return result;
        }
        if (legacy) {
            result =  getLegacy(escapeamp);
            if (escapeamp) {
                cacheEscapeAmp = result;
            } else {
                cacheNoEscapeAmp = result;
            }
            return result;
        }
        if (page != null && page.length() > 0) {
            if (EXTERNAL.matcher(page).matches()) {
                result = getLegacy(escapeamp);
                if (escapeamp) {
                    cacheEscapeAmp = result;
                } else {
                    cacheNoEscapeAmp = result;
                }
                return result;
            }
        }

        Framework framework = Framework.getInstance();

        Parameters frameworkParameters;

        {
            final HttpServletRequest req = (HttpServletRequest) tag.getPageContext().getRequest();
            final Parameters filterFrameworkParameters = (Parameters) req.getAttribute(FrameworkFilter.PARAMS_KEY);

            if (filterFrameworkParameters == null) {
                frameworkParameters = framework.createParameters();
                log.debug("Not found from filter, created " + frameworkParameters);
            } else {
                frameworkParameters = new Parameters(filterFrameworkParameters);
                log.debug("Found fw parameter from filter " + frameworkParameters);

                // clone because should not  _change_ the actual parameters
            }
            frameworkParameters.setAutoCasting(true);
            tag.fillStandardParameters(frameworkParameters);
        }


        if (page != null && page.length() > 0) {
            String curBlock = frameworkParameters.get(Framework.BLOCK);
            if (page.equals(curBlock)) {
                frameworkParameters.setIfDefined(Framework.BLOCK, null);
            }
            //            frameworkParameters.setIfDefined(Framework.COMPONENT, null);
        }

        if (frameworkParams != null) {
            for (Map.Entry<String, Object> entry : frameworkParams.entrySet()) {
                frameworkParameters.set(entry.getKey(), entry.getValue());
            }
        }

        if (internal) {
            if (log.isTraceEnabled()) {
                log.trace("Creating internal url link to page: " + page + frameworkParameters, new Exception());
            } else if (log.isDebugEnabled()) {
                log.debug("Creating internal url link to page: " + page + frameworkParameters);
            }
            result = framework.getInternalUrl(page.toString(), params, frameworkParameters);
            if (result == null) {
                // Shouldn't this happen in the framework instead ?
                BasicUrlConverter converter = new BasicUrlConverter((BasicFramework)framework);
                result = converter.getInternalUrl(page.toString(), params, frameworkParameters).getUrl();
                log.debug("No internal url gotten from framework for, falled back to " + result);
            } else  {
                log.debug("Internal url gotten from framework " + result);
            }
        } else {
            if (process) {
                result = framework.getProcessUrl(page.toString(), params, frameworkParameters, escapeamp);
                if (log.isDebugEnabled()) {
                    log.debug("Created normal process url link to page: " + page + " " + params + " fw: " + frameworkParameters + " -> " + result);
                }
            } else {
                result = framework.getUrl(page.toString(), params, frameworkParameters, escapeamp);
                if (log.isDebugEnabled()) {
                    log.debug("Created normal url link to page: " + page + " " + params + " fw: " + frameworkParameters + " -> " + result);
                }
            }
        }
        {
            String amp = escapeamp ? "&amp;" : "&";
            String connector = result.indexOf("?") > 0 ? amp : "?";
            StringBuffer a = new StringBuffer(result);
            tag.appendMoreParameters(connector , amp, a);
            result = a.toString();
        }

        if (escapeamp) {
            cacheEscapeAmp = result;
        } else {
            cacheNoEscapeAmp = result;
        }

        return result;
    }

    final static Pattern ABSOLUTE_URLS = Pattern.compile("(?i)[a-z]+\\:.*");

    /**
     *
     */
    protected boolean useAbsoluteAttribute(StringBuilder show, String p) throws JspTagException {

        if ("".equals(abs) || "false".equals(abs)) {
            return false;
        }

        HttpServletRequest req = (HttpServletRequest) tag.getPageContext().getRequest();

        if (abs.equals("true")) {
            if (ABSOLUTE_URLS.matcher(p).matches()) {
                show.append(p);
                return true;
            } else {
                String scheme = req.getHeader("x-forwarded-proto");
                if (scheme == null) {
                    scheme = req.getScheme();
                }
                show.append(scheme).append("://");
                String host = req.getHeader("x-forwarded-host");
                if (host == null) {
                    host = req.getServerName();
                }
                show.append(host);
                int port = req.getIntHeader("x-forwarded-port");
                if (port == -1) {
                    port = req.getServerPort();
                }
                show.append((port == 80 && "http".equals(scheme)) ||
                            (port == 443 && "https".equals(scheme))
                            ? "" : ":" + port);
            }
        } else if (abs.equals("server")) {
            if (ABSOLUTE_URLS.matcher(page).matches()) {
                // The redirect tag giv
                show.append(page);
                return true;
            }
            //show.append("/");
        } else if (abs.equals("context")) {

        } else {
            throw new JspTagException("Unknown value for 'absolute' attribute '" + abs + "' (must be either 'true', 'false', 'server' or 'context')");
        }
        if (! abs.equals("context")) {
            show.append(req.getContextPath());
        }
        char firstChar = p.charAt(0);
        try {
            URI uri;
            if (firstChar != '/') {
                uri = new URI("servlet", req.getServletPath() + "/../" + p, null);
            } else {
                uri = new URI("servlet", p, null);
            }
            uri = uri.normalize(); // resolves .. and so one
            show.append(uri.getSchemeSpecificPart());
        } catch (URISyntaxException  use) {
            throw new TaglibException(use.getMessage(), use);
        }
        return true;
    }


    /**
     * Returns the actual URL, considering all options of the url-tag, like 'absolute' and 'encode'.
     */
    public String get() {
        if (string != null) {
            return string;
        }
        try {
            String u = get(escapeAmps);
            log.debug("Produced " + u);
            if (u == null) {
                log.warn("Produces an url which is null! With " + page + " " + params + " " + frameworkParams);
                return "NULL_URL";
            }
            StringBuilder show = new StringBuilder();
            if (! useAbsoluteAttribute(show, u)) {
                log.debug("Absolute attribute not applied on " + u);
                if (u.length() > 0 && u.charAt(0) == '/') {
                    HttpServletRequest req =  (HttpServletRequest) tag.getPageContext().getRequest();
                    if (req != null) {
                        show.insert(0, req.getContextPath());
                    }
                }
                show.append(u);
            }
            if (encodeUrl) {
                HttpServletResponse response = (HttpServletResponse)tag.getPageContext().getResponse();
                if (response != null) {
                    string = response.encodeURL(show.toString());
                } else {
                    string = show.toString();
                }
            } else {
                string = show.toString();
            }
        } catch (FrameworkException fe) {
            throw new RuntimeException(fe.getMessage(), fe);
        } catch (Throwable e) {
            string =  e.toString();
            if (log.isServiceEnabled()) {
                log.service(e.getMessage(), e);
            }
        }
        return string;
    }

    public boolean getEncodeUrl() {
        return encodeUrl;
    }


    public boolean getEscapeAmps() {
        return escapeAmps;
    }

    protected void invalidate() {
        log.debug("invalidating");
        if (params instanceof UrlParameters) {
            ((UrlParameters) params).invalidate();
        }
        string = cacheEscapeAmp = cacheNoEscapeAmp = null;
    }

    public char charAt(int index) {
        return get().charAt(index);
    }
    public int length() {
        return get().length();
    }
    public CharSequence subSequence(int start, int end) {
        return get().subSequence(start, end);
    }

    public String toString() {
        // this means that it is written to page by ${_} and that consequently there _must_ be a body.
        // this is needed when body is not buffered.
        tag.haveBody();
        return get();
    }
    public int compareTo(Object o) {
        return toString().compareTo(Casting.toString(o));
    }

    void doEndTag() {
        // The page CharSequence may be dependent on members of the UrlTag extension
        // This converts it to a String, which is certainly not dynamic any more.
        page = page.toString();
    }
    /**
     * Add a key/value pair to a map, but does not replace the already exsiting mapping.
     * In stead, the already mapped value is converted to a list, contain both values.
     */
    public static Map<String, Object> addParameter(Map<String, Object> map, String key, Object value) {
        if (map.containsKey(key)) {
            log.debug("map already contains mapping for " + key);
            Object existingValue = map.get(key);
            if (existingValue instanceof ImplicitList) {
                log.trace("Adding to existing");
                ((ImplicitList) existingValue).add(value);
            } else {
                List listValue = new ImplicitList();
                listValue.add(existingValue);
                listValue.add(value);
                map.put(key, listValue);
            }
        } else {
            map.put(key, value);
        }
        return map;
    }
    static class ImplicitList extends ArrayList {
        ImplicitList() {
        }
    }

}
