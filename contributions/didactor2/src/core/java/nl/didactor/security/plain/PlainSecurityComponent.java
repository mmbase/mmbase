package nl.didactor.security.plain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.*;
import org.mmbase.util.*;
import java.io.InputStream;

import org.mmbase.module.core.MMObjectNode;
import org.mmbase.security.classsecurity.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.security.*;
import nl.didactor.builders.PeopleBuilder;
import nl.didactor.security.AuthenticationComponent;

import nl.didactor.security.UserContext;

/**
 * Default AuthenticationComponent for Didactor.
 * @javadoc
 * @version $Id: PlainSecurityComponent.java,v 1.8 2006-12-13 09:21:36 mmeeuwissen Exp $
 */

public class PlainSecurityComponent implements AuthenticationComponent {
    private static final Logger log = Logging.getLoggerInstance(PlainSecurityComponent.class);

    private PeopleBuilder users;
    private final Map properties = new HashMap();

    private void checkBuilder() throws org.mmbase.security.SecurityException {
        if (users == null) {
            org.mmbase.module.core.MMBase mmb = org.mmbase.module.core.MMBase.getMMBase();
            users = (PeopleBuilder) mmb.getBuilder("people");
            if (users == null) {
                throw new org.mmbase.security.SecurityException("builder people not found");
            }
        }
    }

    public PlainSecurityComponent() {
        ResourceWatcher fileWatcher = new ResourceWatcher() {
                public void onChange(String file) {
                    configure(file);
                }
            };
        fileWatcher.add("security/login.properties");
        fileWatcher.setDelay(10 * 1000);
        fileWatcher.start();
        fileWatcher.onChange();
    }

    protected void configure(String file) {
        properties.clear();
        Properties props = new Properties();
        try {
            InputStream is = ResourceLoader.getConfigurationRoot().getResource(file).openStream();
            props.load(is);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        properties.putAll(props);

    }

    public UserContext processLogin(HttpServletRequest request, HttpServletResponse response, String application) {
        checkBuilder();

        if ("class".equals(application)) {
            ClassAuthentication.Login li = ClassAuthentication.classCheck("class");
            if (li == null) {
                throw new org.mmbase.security.SecurityException("Class authentication failed  '" + application + "' (class not authorized)");
            }
            String userName = (String) li.getMap().get(AuthenticationData.PARAMETER_USERNAME.getName());
            String rank     = (String) li.getMap().get(AuthenticationData.PARAMETER_RANK.getName());
            if (userName != null) {
                MMObjectNode user = users.getUser(userName);
                UserContext uc = new UserContext(user, "class");
                if (rank != null) {
                    if (uc.getRank().getInt() < Rank.getRank(rank).getInt()) {
                        return null;
                    }
                }
            } else {
                if (rank == null) rank = "basic user";
                UserContext uc = new UserContext("classuser", "classuser", Rank.getRank(rank), "class");
                return uc;
            }
        }

        String sLogin = request.getParameter("username");
        String sPassword = request.getParameter("password");

        if (sLogin == null || sPassword == null) {
            log.debug("Did not find matching credentials");
            return null;
        }

        MMObjectNode user = users.getUser(sLogin, sPassword);
        if (user == null) {
            log.debug("Found credentials, but no matching user. Returning null");
            return null;
        }

        log.debug("Found matching credentials, so user is now logged in.");
        HttpSession session = request.getSession(true);
        session.setAttribute("didactor-plainlogin-userid", "" + user.getNumber());
        session.setAttribute("didactor-plainlogin-application", application);
        return new UserContext(user, application);
    }

    public UserContext isLoggedIn(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String onum = (String) session.getAttribute("didactor-plainlogin-userid");
            String app  = (String) session.getAttribute("didactor-plainlogin-application");
            if (onum != null) {
                checkBuilder();
                MMObjectNode user = users.getNode(onum);
                log.debug("Found 'didactor-plainlogin-userid' in session");
                return new UserContext(user, app == null ? "name/password" : app);
            } else {
                log.debug("There is a session, but no 'didactor-plainlogin-userid' in it");
            }
        } else {
            log.debug("No session, so the user is not logged in");
        }
        return null;
    }

    protected String getLoginPage(HttpServletRequest request) {
        String page = (String) properties.get(request.getServerName() + request.getContextPath() + ".plain.login_page");
        if (page == null) {
            page = (String) properties.get(request.getServerName() + ".plain.login_page");
        }
        if (page == null) {
            page = (String) properties.get("plain.login_page");
        }
        if (page == null) {
            org.mmbase.module.core.MMBase mmb = org.mmbase.module.core.MMBase.getMMBase();
            if (mmb.getRootBuilder().getNode("component.portal") != null) {
                page = "/portal";
            }
        }
        return page == null ? "/login_plain.jsp" : page;
    }

    public String getLoginPage(HttpServletRequest request, HttpServletResponse response) {
        String sLogin    = request.getParameter("username");
        String sPassword = request.getParameter("password");
        if (sLogin != null && sPassword != null) {
            return getLoginPage(request) + "?reason=failed";
        } else {
            return getLoginPage(request);
        }
    }
    
    public String getName() {
        return "didactor-plainlogin";
    }
    
    public void logout(HttpServletRequest request, HttpServletResponse respose) {
        log.debug("logout() called");
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("didactor-plainlogin-userid");
        }
    }
}
