/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import org.mmbase.bridge.jsp.taglib.util.Attribute;

import java.io.IOException;
import java.io.File;

import java.util.*;

import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.RequestDispatcher;

import org.mmbase.bridge.*;
import org.mmbase.bridge.ContextProvider;
import org.mmbase.security.*;

import org.mmbase.util.HttpServletRequestUtils;
import org.mmbase.util.functions.*;
import org.mmbase.util.StringSplitter;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Creates a cloud object (pulling it from if session necessary). While
 * creating a cloud object one also has to authenticate itself, so this
 * functionality is also in this tag.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @author Vincent van der Locht
 * @version $Id$
 */

public class CloudTag extends ContextReferrerTag implements CloudProvider, ParamHandler {


    public static final String KEY = "org.mmbase.cloud";
    public static final int SCOPE = PageContext.REQUEST_SCOPE;

    private static String INITIAL_REALM_PREFIX = "initial-";

    /**
     * Constants needed for the loginpage attribute functionality
     */
    private static final String LOGINPAGE_COMMAND_PARAMETER = "command";
    private static final String LOGINPAGE_COMMAND_LOGIN = "login";
    private static final String LOGINPAGE_COMMAND_LOGOUT = "logout";
    private static final String LOGINPAGE_AUTHENTICATE_PARAMETER = "authenticate";
    private static final String LOGINPAGE_CLOUD_PARAMETER = "cloud";

    private static final String LOGINPAGE_DENYREASON_NEED = "please";
    private static final String LOGINPAGE_DENYREASON_FAIL = "failed";
    private static final String LOGINPAGE_DENYREASON_RANKTOOLOW = "rank";

    private static final int DENYREASON_FAIL = 1;
    private static final int DENYREASON_RANKTOOLOW = 2;

    private static final Logger log = Logging.getLoggerInstance(CloudTag.class);

    private static final String REALM = "realm_";


    private String jspVar;

    private Cookie[] cookies;

    private CloudContext cloudContext;

    private Attribute cloudName = Attribute.NULL;
    private String    loginPageCloudName = null;

    private Attribute cloudURI = Attribute.NULL;
    private Cloud cloud;
    private Object prevCloud;
    private Cloud prevCloudThreadLocal;

    /**
     * @since MMBase-1.7
     */
    private boolean sessionCloud = true;

    private Attribute authenticate = Attribute.NULL;

    private Attribute loginpage = Attribute.NULL;

    //private int method = CloudContext.METHOD_UNSET; // how to log on, method can eg be 'http'.
    private Attribute method = Attribute.NULL;
    private Attribute logonatt = Attribute.NULL;
    private List<String> logon;
    private Attribute pwd = Attribute.NULL;
    private Attribute rank = Attribute.NULL;
    private Attribute sessionName = Attribute.NULL;

    private HttpSession session;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Locale locale;

    /**
     * @return the default cloud context
     **/
    public  CloudContext getDefaultCloudContext() throws JspTagException {
        if (cloudContext == null) {
            String uri = cloudURI.getString(this);
            cloudContext = ContextProvider.getCloudContext(cloudURI.getString(this));
            if (cloudContext == null) throw new RuntimeException("ContextProvider gave <code>null</code> for " + cloudURI.getString(this));
        }
        return cloudContext;
    }

    public void setUri(String uri) throws JspTagException {
        cloudURI = getAttribute(uri, true);
    }

    public void setName(String name) throws JspTagException {
        cloudName = getAttribute(name, true);
    }

    protected String getName() throws JspTagException {
        if (loginPageCloudName != null) return loginPageCloudName;
        if (cloudName == Attribute.NULL) {
            List<String> cloudNames = getDefaultCloudContext().getCloudNames();
            if (cloudNames.size() == 0) {
                throw new JspTagException("The cloud context " + getDefaultCloudContext() + " has no clouds");
            }
            return cloudNames.get(0);
        }
        return cloudName.getString(this);
    }

    public void setLogon(String l) throws JspTagException {
        logonatt = getAttribute(l, true);
    }
    public void setPwd(String pwd) throws JspTagException {
        this.pwd = getAttribute(pwd, true);
    }

    /**
     * Synonym for setLogon. Don't mix.
     */
    public void setUsername(String l) throws JspTagException {
        logonatt = getAttribute(l, true);
    }

    /**
     * Synonym for setPwd. Don't mix.
     */
    public void setPassword(String pwd) throws JspTagException {
        this.pwd = getAttribute(pwd, true);
    }

    public void setRank(String r) throws JspTagException {
        rank = getAttribute(r, true);
    }

    /**
     * Gives the configured rank as a Rank object
     */

    protected Rank getRank() throws JspTagException {
        String s = rank.getString(this);
        Rank r = Rank.getRank(s);
        if (r == null) {
            throw new JspTagException("Unknown rank '" + s + "'");
        }
        return r;
    }

    // javadoc inherited (from ParameterHandler)
    public void addParameter(String key, Object value) {
        if (cloud != null) {
            cloud.setProperty(key, value);
        }
    }

    /**
     * If this cloud is 'anonymous' according to rank attribute.
     * @since MMBase-1.7
     */
    private boolean rankAnonymous() throws JspTagException {
        if (rank == Attribute.NULL) {
            return true;
        }
        String rankString = rank.getString(this);
        return rankString.length() == 0 || rankString.equals(Rank.ANONYMOUS.toString());
    }

    public void setJspvar(String jv) {
        jspVar = jv;
    }

    public void setAuthenticate(String authenticate) throws JspTagException {
        this.authenticate = getAttribute(authenticate, true);
    }

    protected String getAuthenticate() throws JspTagException {
        String a = authenticate.getString(this);
        if (a.length() == 0) {
            return cloudContext.getAuthentication().getTypes(getMethodOrDefault())[0];
        }
        return a;
    }

    public void setMethod(String mm) throws JspTagException {
        method = getAttribute(mm, true);
    }

    protected int getMethod() throws JspTagException {
        String m = method.getString(this);
        AuthenticationData data = getDefaultCloudContext().getAuthentication();
        int r =  data == null ? AuthenticationData.METHOD_UNSET : data.getMethod(m);
        if (log.isDebugEnabled()) {
            log.debug("method '" + m + "' -> " + r);
        }
        return r;
    }

    /**
     * @return The login-method, or METHOD_LOGINPAGE if loginpage was specified, or the default method of the authentication implemnetation if also that was not specified.
     * @since MMBase-1.8
     */
    protected int getMethodOrDefault() throws JspTagException {
        int m = getMethod();
        if (m == AuthenticationData.METHOD_UNSET) {
            if (! "".equals(loginpage.getString(this))) {
                log.debug("loginpage attribute given");
                return AuthenticationData.METHOD_LOGINPAGE;
            } else if (logonatt != Attribute.NULL && pwd != Attribute.NULL) {
                log.debug("logon and pwd attributes given");
                return AuthenticationData.METHOD_SESSIONLOGON;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("lp: " + loginpage + " logonatt: " + logonatt + " pwd: " + pwd + " falling back to default for " + request.getProtocol());
                }
                return  cloudContext.getAuthentication().getDefaultMethod(request.getProtocol());
            }
        } else {
            return m;
        }
    }

    public Cloud getCloudVar() {
        return cloud;
    }

    public void setCloudVar(Cloud c) {
        cloud = c;
    }

    public void setSessionname(String s) throws JspTagException {
        if (s.length() != 0) {
            sessionName = getAttribute(s);
        }
    }

    public void setLoginpage(String loginpage) throws JspTagException {
        this.loginpage = getAttribute(loginpage);
    }

    /**
     * @return The cookie with the sessionname.
     */

    private Cookie searchCookie() throws JspTagException {
        String cookie = REALM + getSessionName();
        if (log.isDebugEnabled()) {
            log.debug("Searching cookie " + cookie);
        }
        if (cookies != null) {
            for (Cookie element : cookies) {
                if (element.getName().equals(cookie) && (!"".equals(element.getValue()))) {
                    return element;
                }
            }
        }
        log.debug("Cookie not found");
        return null;
    }

    /**
     * Sets the 'realm' to r, and write it to the session, or if not possible in a cookie.
     * Changing the realm makes it possible to 'logout' when using http-authentication.
     * @return false on failure
     */
    private boolean setRealm(String r) throws JspTagException {
        log.debug("setting realm in cookie");
        Cookie c = searchCookie();
        if (c == null) {
            c = new Cookie(REALM + getSessionName(), r);
        } else {
            c.setValue(r);
        }
        String path = request.getContextPath();
        if (path.length() == 0) path = "/";
        c.setPath(path);
        c.setMaxAge((int) (60 * 60 * 24 * 365.25)); // one year

        if (cookies.length == 0) {
            cookies = new Cookie[1];
        }
        cookies[0] = c;
        response.addCookie(c);

        if (session != null) {
            session.setAttribute(REALM + getSessionName(), r);
            if (session.isNew()) {
                log.debug("New session!? That is very suspicious. Perhaps URL was not encoded, and cookies disabled, sending redirect to make sure the url is encoded.");
                String query = request.getQueryString();
                String thisPage = request.getRequestURI() + (query == null ? "" : "?" + query);
                try {
                    String url = response.encodeRedirectURL(thisPage);
                    log.debug("Redirecting to " + url);
                    if (! response.isCommitted()) {
                        response.sendRedirect(url);
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    throw new TaglibException(e);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Setting realm " + r + " in session " + REALM + getSessionName());
            }
        }
        return true;
    }
    /**
     * Removing the realm will cause a new one to be created, and if
     * you have a good browser, it will pop up a new 'login' screen (when using http-authentication).
     */

    private void removeRealm() throws JspTagException {
        log.debug("Removing realm");
        String currentRealm = getRealm();
        if (currentRealm != null && ! currentRealm.startsWith(INITIAL_REALM_PREFIX)) { // just authenticated, so don't unauthenticate now
            String cookie = REALM + getSessionName();
            log.debug("removing cookie");
            if (cookies != null) {
                for (Cookie element : cookies) {
                    String path = request.getContextPath();
                    if (path.length() == 0) path = "/";
                    if (element.getName().equals(cookie)) {
                        if (log.isDebugEnabled()) {
                            log.debug("removing cookie with value " + element);
                        }
                        element.setValue("");
                        element.setMaxAge(0); // remove
                        element.setPath(path);
                        response.addCookie(element);
                    }
                }
            }
            if (session != null) {
                log.debug("Removing from session too");
                session.removeAttribute(REALM + getSessionName());
            }
        }
    }
    /**
     * Gets the current realm in use for http-authentication.
     */
    private String getRealm() throws JspTagException {
        Cookie c = searchCookie();
        if (c != null) {
            if (log.isDebugEnabled()) {
                log.debug("found cookie on path = " + c.getPath() + " -> " + c.getValue());
            }
            return c.getValue();
        }
        if (session != null) {
            String realm =  (String) session.getAttribute(REALM + getSessionName());
            if (log.isDebugEnabled()) {
                log.debug("Getting realm  from session " + REALM + getSessionName() + " --> " + realm);
            }
            return realm;
        }
        return null;
    }


    private String getRealmName() {
        String contextPath = request.getContextPath().replace('/', '_');
        return "MMBase" + contextPath + "@" + HttpServletRequestUtils.getServerName(request);
    }

    /**
     * Deny access to this page.
     *
     * @param A message to provide to the user if she does not accepts authorization (enters 'cancel')
     *
     * REMARK: Perhaps we should do some i18n on this message
     *
     * @return SKIP_BDDY;
     */
    private int denyHTTP(String message) throws JspTagException {
        log.debug("sending deny");


        if (response.isCommitted()) {
            throw new JspTagException("Response is commited already, cannot send a deny");
        }

        request.setAttribute("org.mmbase.cloudtag.denied_message", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String realm = getRealm();

        if (realm == null) {
            realm = getRealmName();
            if (!setRealm(INITIAL_REALM_PREFIX + realm)) {
                return SKIP_PAGE;
            }
        } else {

        }

        if (log.isDebugEnabled()) {
            log.debug("Setting header WWW-Authenticate: " + realm);
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");

        //res.setHeader("Authorization", logon);   would ne nice...
        //keesj:look at the php3 tutorial for an example

        // if cancel
        // this cannot be done with an exception (which can be a redirect), because the page must
        // flow ahead, to give the popup opportunity to pop up.
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("org.mmbase.bridge.jsp.taglib.resources.messages", getLocale());
            pageContext.getOut().print("<div class=\"mm_cloud\"><h1>" + bundle.getString("cloudtag.fail") + "</h1><p class=\"mm_cloud\">" + message + "</p></div>");
        } catch (IOException ioe) {
            throw new TaglibException(ioe);
        }
        setAnonymousCloud(); // there must also be _some_ cloud, to avoid exception on
        evalBody(); //  register var
        return SKIP_BODY;
    }

    private boolean setAnonymousCloud() throws JspTagException {
        return setAnonymousCloud(null);
    }

    /**
     * Sets the cloud member variable to an anonymous cloud.
     * @return logoutInfo A map containing information for actual logout.
     * @return true on success (cloud is set), false on failure (cloud is set to null)
     */

    private boolean setAnonymousCloud(Parameters logoutInfo) throws JspTagException {
        try {
            // request/response information is not needed for mmbase-only implementation.
            // but when using 'delegated' login-method, you might also want to delegate logout.
            if (log.isDebugEnabled()) {
                log.debug("creating an anonymous cloud for cloud '" + getName() + "' (with " + logoutInfo + ")");
            }
            // removeCloud(); // should not remove existing cloud from session
            if (logoutInfo != null) logoutInfo.checkRequiredParameters();
            cloud = getDefaultCloudContext().getCloud(getName(), "anonymous", logoutInfo == null ? null : logoutInfo.toMap());
            if (locale != null) {
                cloud.setLocale(locale);
            }
            return true;
        } catch (java.lang.SecurityException e) {
            // login failed for anymous ?! That's odd, provide null.
            log.info("Could not create anonymous cloud because " + e.toString());
            cloud = null;
            return false;
        } catch (NotFoundException nfe) {
            try {
                if (!response.isCommitted()) {
                    response.setHeader("Retry-After", "60");
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, nfe.getMessage());
                }
                return false;
            } catch (IOException ioe) {
                throw new TaglibException(ioe);
            }
        } catch (Throwable t) {
            throw new TaglibException("Could not create anonymous cloud because " + t.getClass().getName() + ": " + t.getMessage(), t);
        }
    }


    public String getSessionName() throws JspTagException {
        String sn = sessionName.getString(this);
        if (sn.length() == 0) {
            return "cloud_" + getName();
        } else {
            return sn;
        }
    }

    /**
     * If the 'cloud' member is set, and everything is ok, then this function is called in doStartTag()
     * @return EVAL_BODY or SKIP_BODY if cloud is null (which normally means that no anonymous is present)
     */

    private int evalBody() throws JspTagException {

        if (getId() != null) { // writecloud to context.
            getContextProvider().getContextContainer().register(getId(), cloud);
        }

        prevCloudThreadLocal = org.mmbase.bridge.util.CloudThreadLocal.unbind();
        if (cloud == null) {
            return SKIP_BODY;
        }

        prevCloud = pageContext.getAttribute(KEY, SCOPE);
        if (prevCloud != null) {
            log.debug("Found previous cloud " + prevCloud);
        }
        pageContext.setAttribute(KEY, cloud, SCOPE);
        org.mmbase.bridge.util.CloudThreadLocal.bind(cloud);

        if (cloud.getCloudContext() instanceof LocalContext) {
            cloud.setProperty(Cloud.PROP_REQUEST, request);
            cloud.setProperty(Cloud.PROP_RESPONSE, response);
        }
        cloud.setProperty(LocaleTag.TZ_KEY, getTimeZone());

        if (jspVar != null) {
            log.debug("Setting jspVar " + jspVar);
            Object was = pageContext.getAttribute(jspVar);
            if (was != null && ! was.equals(cloud)) {
                throw new JspTagException("Jsp-var '" + jspVar + "' already in pagecontext! (" + was + "), can't write " + cloud + " in it. This may be a backwards-compatibility issue. This may be a backwards-compatibility issue. Change jspvar name or switch on backwards-compatibility mode (in your web.xml)");
            }
            pageContext.setAttribute(jspVar, cloud);
        }

        if (locale != null) {
            cloud.setLocale(locale);
        }

        // the surround context tag sometimes also want so server information from the cloud context.
        getContextTag().setCloudContext(cloud.getCloudContext());

        ContentTag tag = findParentTag(ContentTag.class, null, false);
        if (tag != null) {
            UserContext user = cloud.getUser();
            if (sessionCloud && ! user.getRank().equals(org.mmbase.security.Rank.ANONYMOUS)) {
                tag.setUser(cloud.getUser());
            }
        }
        return EVAL_BODY;
    }

    /**
     * Checks if this is a 'reuse' of the cloud tag (using the referid
     * attribute), and sets the cloud member variable if this is the
     * cases
     *
     * @return true if this is a reuse (the caller returns eval-body) and false otherwise (the caller continues).
     */

    private final boolean checkReuse() throws JspTagException {
        if (getReferid() != null) {
            int method = getMethod();
            if ((method != AuthenticationData.METHOD_UNSET &&
                 method != AuthenticationData.METHOD_PAGELOGON &&
                 method != AuthenticationData.METHOD_SESSIONLOGON) ||
                logonatt != Attribute.NULL) { // probably add some more
                throw new JspTagException("The 'referid' attribute of cloud cannot be used together with 'method'  or 'logon' attributes");
            }
            log.debug("found cloud with referid");
            cloud = (Cloud) getContextProvider().getContextContainer().getObject(getReferid());
            if (method == AuthenticationData.METHOD_SESSIONLOGON) {
                session = request.getSession(true);
                if (session == null) {
                    throw new JspTagException("No session, cannot store cloud in session");
                }
                String sn = getSessionName();
                cloud.setProperty(Cloud.PROP_SESSIONNAME, sn);
                session.setAttribute(sn, cloud);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if an anonymous cloud is requested. If so, set the cloud variable accordingly.
     * @return true if cloud must be anonymous (the caller returns eval-body) and false otherwise (the caller continues).
     */

    private final boolean checkAnonymous() throws JspTagException {
        try {
            int m = getMethod();
            if ((m == AuthenticationData.METHOD_UNSET && logon == null && rankAnonymous() && loginpage == Attribute.NULL) || m == AuthenticationData.METHOD_ANONYMOUS) { // anonymous cloud:
                log.debug("Implicitely requested anonymous cloud. Not using session");
                setAnonymousCloud();
                return true;
            } else {
                return false;
            }
        } catch (NotFoundException nfe) {
            try {
                if (!response.isCommitted()) {
                    response.setHeader("Retry-After", "60");
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, nfe.getMessage());
                }
                return true;
            } catch (IOException ioe) {
                throw new TaglibException(ioe);
            }
        }
    }

    /**
     * Checks if the cloud of the session is requested to be 'logged out'.
     */
    private final boolean checkLogoutLoginPage() throws JspTagException {
        if (loginpage != Attribute.NULL && LOGINPAGE_COMMAND_LOGOUT.equals(request.getParameter(LOGINPAGE_COMMAND_PARAMETER))) {
            log.debug("request to log out, remove session atributes, give anonymous cloud.");
            if (cloud != null) {
                removeRealm();
                if (session != null) {
                    log.debug("ok. session is not null");
                    session.removeAttribute(getSessionName()); // remove cloud itself
                }
            }
            setAnonymousCloud();
            return true;
        }
        return false;
    }

    /**
     * Checks if the cloud of the session if requested to be 'logged out'.
     */
    private final boolean checkLogoutMethod() throws JspTagException {
        if (getMethod() == AuthenticationData.METHOD_LOGOUT) {
            log.debug("Requested logout");
            removeRealm();
            if (cloud != null) { // if cloud already null, don't do it, otherwise login method="http" on same page doesnt work

                if (session != null) {
                    log.debug("ok. session is not null");
                    session.removeAttribute(getSessionName()); // remove cloud itself
                }
            } else {
                log.debug("No cloud, so no need to log out");
            }
            // add some information for actual logout
            // Logout is loging in with 'anonymous' with some extra info.
            Parameters logoutInfo = cloudContext.getAuthentication().createParameters("anonymous");
            fillStandardParameters(logoutInfo);
            if (cloud != null) {
                String authenticate = cloud.getUser().getAuthenticationType();
                logoutInfo.setIfDefined(AuthenticationData.PARAMETER_AUTHENTICATE, authenticate);
            }
            logoutInfo.setIfDefined(AuthenticationData.PARAMETER_LOGOUT, Boolean.TRUE);
            setAnonymousCloud(logoutInfo);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates the getAumember variables session and cookies variables member
     */
    private final void setupSession() throws JspTagException {
        switch(getMethod()) {
        case AuthenticationData.METHOD_DELEGATE:
            return;
        }
        cookies = request.getCookies();
        if (cookies == null) {
            cookies = new Cookie[0];
        }
        if (log.isDebugEnabled()) {
            log.debug("getting (thus creating) session now: " + session);
        }
        session = request.getSession(false);
        if (session != null) { // some people like to disable their session
            String sessionName = getSessionName();
            Object c = session.getAttribute(sessionName);
            if (c != null && ! (c instanceof Cloud)) {
                throw new TaglibException("The session variable '" + sessionName + "' is not of type Cloud (but it is a '" + c.getClass().getName() + "'), and perhaps is used for another goal. This error could be avoided by use of the 'sessionname' attribute of the cloud-tag.");
            }
            cloud = (Cloud) c;
            if (cloud != null) {
                if (cloud.getUser().isValid()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Created/found a session. Cloud '" + sessionName + "' in it is of: " + cloud.getUser());
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Found invalid cloud in session variable '" + sessionName + "' of '" + cloud.getUser() + "'. Discarding.");
                    }
                    cloud = null;
                }
            } else {
                log.debug("No cloud found in session variable '" + sessionName + "'");
            }

        } else {
            log.debug("Not succeeded creating a session");
        }

    }

    /**
     * Checks if there is a NEW locale set and sets the 'locale' member variable accordingly.
     * The 'locale' member variable will later be assigned to the cloud.
     */
    private final void checkLocale() throws JspTagException {
        Locale l = getLocaleFromContext();
        // only set the locale when found.
        if (l != null) {
            locale = l;
        }
    }


    /**
     * Checks wether the cloud is requested 'as is', meaning that is must be tried to get it from the session.
     *
     */

    private final boolean checkAsis() throws JspTagException {
        if (getMethod() == AuthenticationData.METHOD_ASIS) {
            session = request.getSession(false);
            if (session != null) {
                cloud = (Cloud) session.getAttribute(getSessionName());
            }
            if (cloud == null) {
                setAnonymousCloud();
            }
            if (cloud != null) {
                checkValid();
            }
            checkCloud();
            return true;
        }
        return false;

    }

    /**

     */

    private final void checkValid() {
        if (cloud.getUser() == null || !cloud.getUser().isValid()) {
            log.debug("found a cloud in the session, but is was expired, throwing it away");
            cloud = null;
        }
    }

    /**
     * @since MMBase-1.7
     */
    private final void removeCloud() throws JspTagException {
        cloud = null;
        if (session != null) {
            session.removeAttribute(getSessionName());
        }
    }

    /**
     * Checks if the current cloud satisfies the other conditions set
     * on it (method=anonymous, logon, rank attribute). If not the
     * cloud member variable is made null.
     */
    private final void checkCloud() throws JspTagException {

        if (cloud == null) {
            log.debug("Cloud is null, cannot check it");
            removeCloud();
            return;
        }
        // we have a cloud, check if it is a desired one
        // otherwise make it null.
        if (log.isDebugEnabled()) {
            log.debug("found cloud m: " + method + " l: " + logon + ". Checking it.");
        }
        if (cloud.getUser() == null) {
            log.debug("found a cloud in the session, but is has no user, throwing it away");
            removeCloud();
            return;
        }


        if (!cloud.getUser().isValid()) {
            // Makes the cloud variable null (may not be null already) if it
            // is 'expired'. This means normally that the security
            // configuration has been changed, or MMBase restarted or
            // something like that. This means that the cloud (probably gotten
            // from the session), cannot be used anymore.
            log.debug("found a cloud in the session, but is was expired, throwing it away");
            removeCloud();
            return;
        }
        if (! cloud.getCloudContext().getUri().equals(ContextProvider.getCloudContext(cloudURI.getString(this)).getUri())) {
            log.debug("found a cloud in the session, but is of the wrong cloud context, throwing it away");
            removeCloud();
            return;
        }

        int meth = getMethod();
        if (logon == null && rank == Attribute.NULL && meth != AuthenticationData.METHOD_UNSET  && meth != AuthenticationData.METHOD_ASIS) {
            // authorisation was requested, but not indicated for whom
            if (log.isDebugEnabled()) {
                log.debug("Implicitily requested non-anonymous (by method) cloud. Current user: " + cloud.getUser().getIdentifier());
            }
            if (cloud.getUser().getRank().equals(Rank.ANONYMOUS)) { // so it simply may not be anonymous
                log.debug("there was a cloud, but anonymous. log it on");
                removeCloud();
                return;
            }

        } else if (logon != null && cloud != null) {
            if (log.isDebugEnabled()) {
                log.debug("Explicitily requested non-anonymous cloud (by logon = '" + logon + "'). Current user: " + cloud.getUser().getIdentifier());
            }
            // a logon name was given, check if logged on as the right one
            if (!logon.contains(cloud.getUser().getIdentifier())) { // no!
                log.debug("logged on, but as wrong user. log out first.");
                removeCloud();
                return;
            } else {
                log.debug("Cloud is ok already");
            }
        }
        if (!rankAnonymous()) {
            if (log.isDebugEnabled()) {
                log.debug("Explicitily requested non-anonymous cloud (by rank). Current user: " + cloud.getUser());
            }
            Rank curRank = cloud.getUser().getRank();
            if (curRank.getInt() < getRank().getInt()) {
                if (log.isDebugEnabled()) {
                    log.debug("logged on, but rank of user (" + curRank.toString() + ") is too low (must be " + getRank().toString() + "). log out first.");
                }
                removeCloud();
                return;
            } else {
                log.debug("Cloud is ok already");
            }

        }
        if (meth != AuthenticationData.METHOD_UNSET &&
            meth != AuthenticationData.METHOD_ASIS &&
            cloud != null &&
            authenticate != Attribute.NULL &&
            (!cloud.getUser().getAuthenticationType().equals(getAuthenticate()))) {
            log.debug("Cloud was logged on with different authentication type ('" + cloud.getUser().getAuthenticationType()
                      + "' in stead of the requested '" + getAuthenticate() + "'. Should do procedure again.");
            removeCloud();
            return;
        } else {
            log.debug("Cloud was logged with same authentication type -> ok");
        }

    }

    /**
     * Sets logon and password using http-authentication in the Map argument.
     * @param uses A Map which will be passed to the security system.
     * @return SKIP_BODY if a deny was sent, the caller must not
     * continue then. EVAL_BODY  otherwise (can be ignored).
     */

    private final int doHTTPAuthentication(Parameters user) throws JspTagException {
        log.debug("with http");
        ResourceBundle bundle = ResourceBundle.getBundle("org.mmbase.bridge.jsp.taglib.resources.messages", getLocale());

        String realm = getRealm();
        if (realm == null ) {
            log.debug("no realm found, need to log on again");
            return denyHTTP("<h2 class=\"mm_cloud\">" + bundle.getString("cloudtag.again") + "</h2><p class=\"mm_cloud\">" + bundle.getString("cloudtag.logout") + "</p>");
        }
        if (realm.startsWith(INITIAL_REALM_PREFIX)) {
            realm = realm.substring(INITIAL_REALM_PREFIX.length());
            if (! setRealm(realm)) {
                return SKIP_PAGE;
            }
        }
        String mime_line = request.getHeader("Authorization");
        if (log.isDebugEnabled()) {
            log.debug("authent: " + request.getHeader("WWW-Authenticate") + " realm: " + realm + " authorization " + mime_line);
        }
        // find logon, password with http authentication
        String userName = null;
        String password = null;
        try {
            if (mime_line != null) {
                String user_password = org.mmbase.util.Encode.decode("BASE64", mime_line.substring(6));
                StringTokenizer t = new StringTokenizer(user_password, ":");
                if (t.countTokens() == 2) {
                    userName = t.nextToken();
                    password = t.nextToken();
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        // Authenticate user
        if (log.isDebugEnabled()) {
            log.debug("u " + userName + " p " + password);
        }

        if (logon != null) { // if there was a username specified as well, it must be the same
            log.debug("http with username");
            if (!logon.contains(userName)) {
                log.debug("username not correct");
                return denyHTTP("<h2 class=\"mm_cloud\">" + bundle.getString("cloudtag.wronguser") + "</h2><p class\"mm_cloud\">" + bundle.getString("cloudtag.mustbe") + logon + "</p>");
            } else {
                logon = new ArrayList<String>();
                logon.add(userName);
            }
        } else { // logon == null
            log.debug("http without username");
            if (userName == null) { // there must be at least known a username...
                log.debug("no username known");
                return denyHTTP("<h2 class=\"mm_cloud\">" + bundle.getString("cloudtag.nouser") + "</h2>");
            }
            /*
              logon = new ArrayList();
              logon.add(userName); why is this..
            */
        }
        if (userName != null) {
            user.setIfDefined(AuthenticationData.PARAMETER_USERNAME, userName);
        }
        if (password != null) {
            user.setIfDefined(AuthenticationData.PARAMETER_PASSWORD, password);
        }
        return EVAL_BODY;

    }




    /**
     * Sets logon-variables using the login page (with the loginpage attribute) in the Map argument.
     * @param uses A Map which will be passed to the security system,.
     * @return SKIP_BODY if a redirect was given (to the login page),
     * the caller must not continue then. EVAL_BODY  otherwise
     * (can be ignored).
     */

    private int doLoginPage(Parameters user) throws JspTagException {
        log.debug("login page required to acces this cloud data!");

        // look if we need to login(param command='login') with methods and with the params...
        // otherwise redirect!
        if (LOGINPAGE_COMMAND_LOGIN.equals(request.getParameter(LOGINPAGE_COMMAND_PARAMETER))) {

            if (log.isDebugEnabled()) {
                log.debug("Request to login with loginpage, trying to perform an login");
            }
            // now pass all the attributes to the cloud-login method of the cloudcontext!
            String authenticatePassed = request.getParameter(LOGINPAGE_AUTHENTICATE_PARAMETER);
            if (authenticatePassed != null) {
                setAuthenticate(authenticatePassed); // THIS SEEMS DANGEROUS
            }
            String cloudNamePassed = request.getParameter(LOGINPAGE_CLOUD_PARAMETER);
            if (cloudNamePassed != null) {
                loginPageCloudName = cloudNamePassed;
            }
            user.setAutoCasting(true);
            Enumeration<String> enumeration = request.getParameterNames();
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement();
                String value = (String) org.mmbase.bridge.jsp.taglib.util.ContextContainer.fixEncoding(request.getParameter(key), pageContext);
                if (log.isDebugEnabled()) {
                    log.debug("security info --> key:" + key + " value:" + value);
                }
                user.setIfDefined(key, value);
            }
            fillStandardParameters(user);
            return EVAL_BODY;
        } else {
            // no command give, send redirect to specified login page
            return denyLoginPage(LOGINPAGE_DENYREASON_NEED, "");
        }
    }

    /**
     * Denies access to this page and sends are redirect to the login-page (given by login page attribute).
     * @param reason       The reason for deny, can be LOGINPAGE_DENYREASON_NEED or LOGINPAGE_DENYREASON_FAIL.
     * @param excactReason A  further specification of the reason (any String)
     * @return SKIP_BODY
     */
    private int denyLoginPage(String reason, String exactReason) throws JspTagException {
        log.debug("Denying to login-page");
        try {

            // find this page relative to login-page
            String referrerPage = null;

            String requestURI = request.getRequestURI();
            if (requestURI.endsWith("/")) {
                referrerPage = ".";
            } else {
                referrerPage = new File(requestURI).getName();
            }


            /*
            if (1 == 0) {
                // XXXXX hmm, should test this in freeze
                // making relative urls'.

                String toDir = new File(toFile).getParent();
                if (toDir == null) {
                    toDir = ".";
                }
                File servletPath = new File(request.getServletPath());

                String thisDir = servletPath.getParent();
                if (thisDir == null) {
                    thisDir = ".";
                }

                String thisFile = servletPath.getName();

                if (toDir.startsWith("/")) {
                    referrerPage = org.mmbase.util.UriParser.makeRelative(toDir, thisDir) + "/" + thisFile;
                } else {
                    referrerPage = org.mmbase.util.UriParser.makeRelative(thisDir + "/" + toDir, toDir) + "/" + thisFile;
                }
            }
            */



            String toFile = loginpage.getString(this);

            String referrer = null;

            // if a 'referrer' is explicitely mentioned in the 'loginpage' attribute (e.g. on a 'dologin' page), we try to honour it.
            int existingQueryPosition = toFile.indexOf('?');
            if (existingQueryPosition > 0) {
                String existingQuery = toFile.substring(existingQueryPosition + 1);
                log.debug("Found existing query " + existingQuery);
                String[] parameters = existingQuery.split("&");
                for (String element : parameters) {
                    if (element.startsWith("referrer=")) {
                        referrer = org.mmbase.util.Encode.decode("escape_url", element.substring(9));
                        if (referrer.startsWith("?")) referrer = "." + referrer; // tomcat 5, referrerPage can be "", which is inconvenient, because using it as action for login.jsp whill be empty string, will post to login.jsp again
                        log.debug("Found existing referrer " + referrer);
                        break;
                    }
                }
            }
            if (referrer == null) {
                if (request.getQueryString() != null) {
                    referrer = referrerPage + "?" + request.getQueryString();
                } else {
                    referrer = referrerPage;
                }
            }
            log.debug("Using " + referrer);
            if (! response.isCommitted()) {
                //reference = org.mmbase.util.Encode.encode("ESCAPE_URL_PARAM", reference);
                RequestDispatcher rd = request.getRequestDispatcher(toFile);
                request.setAttribute("referrerpage", referrerPage);
                request.setAttribute("referrer", referrer);
                request.setAttribute("reason", reason);
                request.setAttribute("exactreason", exactReason);
                request.setAttribute("usernames", logon);
                request.setAttribute("uri", getDefaultCloudContext().getUri());
                if (cloudName != Attribute.NULL) {
                    request.setAttribute("cloudname", cloudName.getString(this));
                }
                rd.forward(request, response);
            }
            return SKIP_BODY;
        } catch (javax.servlet.ServletException ioe) {
            throw new TaglibException("error sending redirect " + ioe.getMessage(), ioe);
        } catch (java.io.IOException ioe) {
            throw new TaglibException("error sending redirect " + ioe.getMessage(), ioe);
        }
    }

    private final int deny(int reason, String exactReason) throws JspTagException {
        int meth = getMethodOrDefault();
        // did not succeed, so problably the password was wrong.
        switch(meth){
        case AuthenticationData.METHOD_HTTP:  // give a deny, people can retry the password then.
            ResourceBundle bundle = ResourceBundle.getBundle("org.mmbase.bridge.jsp.taglib.resources.messages", getLocale());
            switch (reason) {
            case DENYREASON_RANKTOOLOW :
                return denyHTTP("<h2 class=\"mm_cloud\">" + bundle.getString("cloudtag.ranktoolow") + " (" + bundle.getString("cloudtag.mustbeatleast") + getRank().toString() + ")</h2>");
            case DENYREASON_FAIL :
            default :
                return denyHTTP("<h2 class=\"mm_cloud\">" + bundle.getString("cloudtag.authentication") + "</h2>");
            }
        case AuthenticationData.METHOD_LOGINPAGE:
            switch (reason) {
            case DENYREASON_RANKTOOLOW :
                return denyLoginPage(LOGINPAGE_DENYREASON_RANKTOOLOW, exactReason);
            case DENYREASON_FAIL :
            default :
                return denyLoginPage(LOGINPAGE_DENYREASON_FAIL, exactReason);
            }
        case AuthenticationData.METHOD_DELEGATE:
        case AuthenticationData.METHOD_SESSIONDELEGATE:
            switch (reason) {
            case DENYREASON_RANKTOOLOW :
                // throw new JspTagException("Rank too low");
            case DENYREASON_FAIL :
            default :
                return SKIP_BODY;
            }
        default:
            // strange, no method given, password wrong (or missing), that's really wrong.
            switch (reason) {
            case DENYREASON_FAIL : {
                if ("name/password".equals(getAuthenticate())) {
                    throw new JspTagException("Logon of with "
                                              + (logon != null && logon.size() > 0 ? "'" + logon.get(0) + "'" : "''")
                                              + " failed."
                                              + "(" + exactReason + ")");
                } else {
                    throw new JspTagException("Authentication ('" + getAuthenticate() + "') failed");
                }
            }
            case DENYREASON_RANKTOOLOW : {
                throw new JspTagException("Rank too low");
            }
            default :
                throw new JspTagException("Denied for unknown reason");
            }
        }
    }

    /**
     * Performs logging in the bridge and sets the cloud variable, using the provided Map argument.
     *
     * @return SKIP_BODY on fail and EVAL_BODY otherwise.
     *
     */
    private final int doLogin(Parameters user) throws JspTagException {
        log.debug("Username found. logging in");
        try {
            user.checkRequiredParameters();
            cloud = getDefaultCloudContext().getCloud(getName(), getAuthenticate(), user == null ? null : user.toMap());
            log.debug("Logged in " );
            if (!cloud.getUser().isValid()) {
                log.warn("Just acquired user " + cloud.getUser().getIdentifier() + " is not valid!");
                return deny(DENYREASON_FAIL, "Just acquired user " + cloud.getUser().getIdentifier() + " is not valid!");
            }
            if (logon != null && pwd != Attribute.NULL) {
                logon.set(0, cloud.getUser().getIdentifier());
            }
            // ok, logging on work, now check rank if necessary
            if (rank != Attribute.NULL) {
                log.debug("Checking for rank");
                Rank curRank = cloud.getUser().getRank();
                if (curRank == null) {
                    throw new RuntimeException ("The user " + cloud.getUser() + " had rank 'null'");
                }
                Rank r = getRank();
                if (curRank.getInt() < r.getInt()) {
                    if (log.isDebugEnabled()) {
                        log.debug("logged on, but rank of user is too low (" + cloud.getUser().getRank() + ". log out first.");
                    }
                    cloud = null;
                    if (session != null) {
                        session.removeAttribute(getSessionName());
                    }
                    log.debug("rank too low");
                    return deny(DENYREASON_RANKTOOLOW, "" + curRank + " < " + r);
                }
            }
            return EVAL_BODY;
        } catch (java.lang.SecurityException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to log in with " + user + " because " + e.toString());
            }
            return deny(DENYREASON_FAIL, e.getMessage());
        }
    }

    /**
     * Makes a logged-in cloud.
     * @return SKIP_BODY if failed to do so. EVAL_BODY  otherwise.
     */

    private final int makeCloud() throws JspTagException {
        Parameters user = null;
        int meth = getMethodOrDefault();
        if (log.isDebugEnabled()) {
            log.debug("Creating the cloud with method " + meth);
        }

        // check how to log on:
        switch(meth) {

        case AuthenticationData.METHOD_SESSIONDELEGATE:
        case AuthenticationData.METHOD_DELEGATE:
            if (log.isDebugEnabled()) {
                log.debug("delegate for " + getAuthenticate());
            }
            user = cloudContext.getAuthentication().createParameters(getAuthenticate());
            if (logon != null) {
                user.setIfDefined(AuthenticationData.PARAMETER_USERNAMES, logon);
            }
            if (rank != Attribute.NULL) {
                user.setIfDefined(AuthenticationData.PARAMETER_RANK, getRank());
            }
            sessionCloud = meth == AuthenticationData.METHOD_SESSIONDELEGATE;
            break;
        case AuthenticationData.METHOD_HTTP:
            log.debug("http");
            user = cloudContext.getAuthentication().createParameters(getAuthenticate());
            sessionCloud = true;
            if (doHTTPAuthentication(user) == SKIP_BODY) {
                return SKIP_BODY;
            }
            break;
        case AuthenticationData.METHOD_LOGINPAGE:
            log.debug("loginpage for " + getAuthenticate());
            user = cloudContext.getAuthentication().createParameters(getAuthenticate());
            sessionCloud = true;
            if (doLoginPage(user) == SKIP_BODY) {
                return SKIP_BODY;
            }
            break;
        default:
            log.debug("default");
            if (logon != null && pwd != Attribute.NULL) {
                user = cloudContext.getAuthentication().createParameters(getAuthenticate());
                user.set(AuthenticationData.PARAMETER_USERNAME, logon.get(0));
                user.set(AuthenticationData.PARAMETER_PASSWORD, pwd.getString(this));
                if (meth == AuthenticationData.METHOD_PAGELOGON) {
                    sessionCloud = false;
                } else {
                    if (meth != AuthenticationData.METHOD_SESSIONLOGON) {
                        log.warn("Using logon/pwd (or username/password) attributes on page '" + request.getRequestURI() + "' without specifying method='[pagelogon|sessionlogon]', defaulting to 'sessionlogon'. Be aware that users of this page now are authenticated in their session!");
                    }
                    sessionCloud = true;
                }
            } else {
                sessionCloud = false;
            }
        }

        // do the MMCI cloud logging on
        if (user != null) {
            fillStandardParameters(user);
            if (doLogin(user) == SKIP_BODY) {
                return SKIP_BODY;
            }
        } else {
            log.debug("no login given, creating anonymous cloud");
            // no logon, create an anonymous cloud.
            setAnonymousCloud();
        }
        checkCloud(); // perhaps the just created cloud does not satifisfy other conditions? (rank)
        if (cloud == null) { // stil null, give it up then...
            /*
            user = new HashMap();
            if (doHTTPAuthentication(user) == SKIP_BODY) {
                return SKIP_BODY;
            }
            */

            log.debug("Could not create Cloud.");
            // throw new JspTagException("Could not create cloud (even not anonymous)");
            return SKIP_BODY;
        } else {
            if (sessionCloud) {
                if (session == null) session = request.getSession(true);
                if (session != null) {
                    String sn = getSessionName();
                    cloud.setProperty(Cloud.PROP_SESSIONNAME, sn);
                    log.debug("Setting cloud in session variable '" + sn + "'");
                    session.setAttribute(sn, cloud);
                }
            } else {
                log.debug("Not storing cloud because session: " + (session != null) + " put cloud " + sessionCloud);
            }
        }
        return EVAL_BODY;
    }

    /**
     * request and response can be determined once a page..
     */

    public void setPageContext(PageContext pc) {
        super.setPageContext(pc);
        request =  (HttpServletRequest) pageContext.getRequest();
        response = (HttpServletResponse) pageContext.getResponse();
        if (log.isDebugEnabled()) {
            log.debug("Got a " + response.getClass().getName() + " (commited: " + response.isCommitted() + ")");
        }
    }


    /**
     *  Sets the cloud variable considering all requirements. SKIP_BODY if this can not be done.
     *
     */
    public int doStartTag() throws JspTagException {

        //log.info("" +  Collections.list(pageContext.setAttributeNamesInScope(PageContext.PAGE_SCOPE)));
        checkLocale();

        {
            String s = logonatt.getString(this);
            logon = s.length() == 0 ? null : StringSplitter.split(s);
        }

        getDefaultCloudContext();
        if (checkReuse()) { // referid
            return evalBody();
        }
        if (checkAnonymous()) { // check if requested, and create
            if (cloud == null) { // could not be created!
                // what can we do now?
                return SKIP_BODY;
            } else {
                // yes, found
                log.debug("Implicitely requested anonymous cloud. Will not use session");
                return evalBody();
            }
        }
        if (checkAsis()) { // checks if request 'asis'
            if (cloud == null) { // could not even make anonymous
                return SKIP_BODY;
            } else {
                return evalBody();
            }
        }
        setupSession(); // might need session now
        if (log.isDebugEnabled()) {
            log.debug("startTag " + cloud);
        }
        if (checkLogoutLoginPage()) {
            // TODO: find a better page to redirect to!
            try {
                String url = request.getRequestURI();
                response.sendRedirect(url);
            } catch (java.io.IOException ioe) {
                throw new TaglibException(ioe);
            }
            return SKIP_BODY;
        }
        if (checkLogoutMethod()) {
            return evalBody();
        }

        if (cloud != null) {
            checkCloud();
        }
        if (cloud == null) {
            if (makeCloud() == SKIP_BODY) { // we did't have a cloud, or it was not a good one:
                return SKIP_BODY;
            }
        }
        //pwd = Attribute.NULL; // could not be right. Using cloud-tag twice in same page might not work then

        return evalBody();
    }

    public int doEndTag() throws JspTagException {
        if (log.isDebugEnabled()) {
            log.debug("Resetting cloud to " + prevCloud + " " + prevCloudThreadLocal);
        }
        org.mmbase.bridge.util.CloudThreadLocal.unbind();
        org.mmbase.bridge.util.CloudThreadLocal.bind(prevCloudThreadLocal);
        pageContext.setAttribute(KEY, prevCloud, SCOPE);
        if (log.isDebugEnabled()) {
            log.debug("Unset " + KEY + " " + prevCloud);
        }
        dereference();
        return super.doEndTag();
    }
    protected void dereference() {
        prevCloud = null;
        prevCloudThreadLocal = null;
        cookies = null;
        cloudContext = null;
        cloud = null;
        logon = null;
        session = null;
        request = null;
        response = null;
        loginPageCloudName = null;
    }

    @Override
    public void doCatch(Throwable e) throws Throwable {
        doEndTag();
        super.doCatch(e);
    }

    @Override
    public void doFinally() {
        // can be cleaned for gc:
        super.doFinally();
        dereference();
    }

    // if EVAL_BODY == EVAL_BODY_BUFFERED
    public int doAfterBody() throws JspTagException {
        if (EVAL_BODY == EVAL_BODY_BUFFERED) {
            try {
                if (bodyContent != null) {
                    bodyContent.writeOut(bodyContent.getEnclosingWriter());
                }
            } catch (IOException ioe) {
                throw new TaglibException(ioe);
            }
        }
        return SKIP_BODY;
    }

}
