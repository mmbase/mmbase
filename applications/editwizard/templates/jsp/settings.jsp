<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
%><%@page import="java.io.*,java.util.*, org.mmbase.bridge.Cloud, org.mmbase.util.logging.Logger"
%><%@page import="org.mmbase.util.xml.URIResolver,org.mmbase.applications.editwizard.*"
%><%@ page import="org.mmbase.applications.editwizard.SecurityException,org.mmbase.applications.editwizard.Config"
%><%!
    /**
     * settings.jsp
     *
     * @since    MMBase-1.6
     * @version  $Id: settings.jsp,v 1.33 2003-05-09 07:47:06 pierre Exp $
     * @author   Kars Veling
     * @author   Pierre van Rooden
     * @author   Michiel Meeuwissen
     */

    /**
     * @see org.mmbase.applications.editwizard.Config
     */
    class Configurator extends Config.Configurator {

        Configurator(HttpServletRequest req, HttpServletResponse res, Config c) throws WizardException {
            super(req, res, c);
            c.maxupload = getParam("maxsize", new Integer(4 * 1024 * 1024)).intValue(); // 1 MByte max uploadsize
        }



        // which parameters to use to configure a list page
        public void config(Config.ListConfig c) {
            c.title       = getParam("title", c.title);
            c.pagelength   = getParam("pagelength", new Integer(c.pagelength)).intValue();
            c.maxpagecount   = getParam("maxpagecount", new Integer(c.maxpagecount)).intValue();
            c.wizard      = getParam("wizard", c.wizard);
            if (c.wizard != null && c.wizard.startsWith("/")) {
                c.wizard = "file://" + getRequest().getRealPath(c.wizard);
            }

            c.setAttribute("origin",getParam("origin"));
            c.startNodes  = getParam("startnodes", c.startNodes);
            c.nodePath    = getParam("nodepath", c.nodePath);
            c.fields      = getParam("fields", c.fields);
            c.age         = getParam("age", new Integer(c.age)).intValue();
            c.start       = getParam("start", new Integer(c.start)).intValue();
            c.searchType=getParam("searchtype", c.searchType);
            c.searchFields=getParam("searchfields", c.searchFields);
            c.searchValue=getParam("searchvalue", c.searchValue);
            c.searchDir=getParam("searchdir",c.searchDir);
            c.baseConstraints=getParam("constraints", c.baseConstraints);
            
            if (c.searchFields==null) {
                c.constraints = c.baseConstraints;
            } else {
                // search type: default
                String sType=c.searchType;
                // get the actual field to serach on.
                // this can be 'owner' or 'number' instead of the original list of searchfields,
                // in which case serachtype may change 
                String sFields=getParam("realsearchfield", c.searchFields);
                if (sFields.equals("owner") || sFields.endsWith(".owner")) {
                    sType="string";
                } else if (sFields.equals("number") || sFields.endsWith(".number")) {
                    sType="equals";
                }
                String search=c.searchValue;
                c.constraints=null;
                if (sType.equals("like")) {
                    // actually we should unquote search...
                    search=" LIKE '%"+search.toLowerCase()+"%'";
                } else if (sType.equals("string")) {
                    search=" = '"+search+"'";
                } else {
                    if (search.equals("")) {
                        search="0";
                    }
                    if (sType.equals("greaterthan")) {
                        search=" > "+search;
                    } else if (sType.equals("lessthan")) {
                        search=" < "+search;
                    } else if (sType.equals("notgreaterthan")) {
                        search=" <= "+search;
                    } else if (sType.equals("notlessthan")) {
                        search=" >= "+search;
                    } else if (sType.equals("notequals")) {
                        search=" != "+search;
                    } else { // equals
                        search=" = "+search;
                    }
                }
                StringTokenizer searchtokens= new StringTokenizer(sFields,",");
                while (searchtokens.hasMoreTokens()) {
                    String tok=searchtokens.nextToken();
                    if (c.constraints!=null) {
                        c.constraints+=" OR ";
                    } else {
                        c.constraints="";
                    }
                    if (sType.equals("like")) {
                        c.constraints+="lower(["+tok+"])"+search;
                    } else {
                        c.constraints+="["+tok+"]"+search;
                    }
                }
                if (c.baseConstraints!=null) {
                    c.constraints="("+c.baseConstraints+") and ("+c.constraints+")";
                }
            }
            c.searchDir  = getParam("searchdir", c.searchDir);
            c.directions  = getParam("directions", c.directions);
            c.orderBy     = getParam("orderby", c.orderBy);
            c.distinct    = getParam("distinct", new Boolean(true)).booleanValue();
        }

        // which parameter to use to configure a wizard page
        public void config(Config.WizardConfig c) {
            c.wizard           = getParam("wizard", c.wizard);
            c.popupId          = getParam("popupid",  "");
            c.debug            = getParam("debug",  false);
            if (c.wizard != null && c.wizard.startsWith("/")) {
                c.wizard = "file://" + getRequest().getRealPath(c.wizard);
            }

            c.setAttribute("origin",getParam("origin"));
            c.objectNumber = getParam("objectnumber");
        }
    };

%><%

Config ewconfig = null;    // Stores the current configuration for the wizard as whole, so all open lists and wizards are stored in this struct.
Configurator configurator = null; // Fills the ewconfig if necessary.

String popupId = "";  // default means: 'this is not a popup'
boolean popup = false;  
 

 boolean done=false;
     Object closedObject=null;
%><mm:log jspvar="log"><%  // Will log to category: org.mmbase.PAGE.LOGTAG.<context>.<path-to-editwizard>.jsp.<list|wizard>.jsp

log.trace("start of settings.jsp");
// Add some header to make sure these pages are not cached anywhere.
response.addHeader("Cache-Control","no-cache");
response.addHeader("Pragma","no-cache");

// Set session timeout
session.setMaxInactiveInterval(1 * 60 * 60); // 1 hour;

// and make every page expired ASAP.
String now = org.mmbase.util.RFC1123.makeDate(new Date());
response.addHeader("Expires",       now);
response.addHeader("Last-modified", now);

//response.addHeader("Date",          now); // Jetty doesn't like if you set this.
log.trace("done setting headers");

// It is possible to specify an alternatvie 'sessionkey'
// The sessionkey is used as a key for the session.
String sessionKey = request.getParameter("sessionkey");
if (sessionKey == null) sessionKey = "editwizard";


// proceed with the current wizard only if explicitly stated,
// or if this page is a debug page

boolean proceed = "true".equals(request.getParameter("proceed")) || (request.getRequestURI().endsWith("debug.jsp"));


// Look if there is already a configuration in the session.
Object configObject = session.getAttribute(sessionKey);
if (proceed && configObject == null) {
    throw new WizardException("Your data cannot be found anymore, you waited too long (more than an hour), or the server was restarted");
}

if (configObject == null || ! (configObject instanceof Config) || ! (proceed)) { // nothing (ok) in the session
    if (log.isDebugEnabled()) {
        if (proceed) {
            log.debug("creating new configuration (in session is " + configObject + ")");
        } else {
            log.debug("creating new configuration (missing proceed parameter)");
        }
    }
    ewconfig = new Config();
    session.setAttribute(sessionKey, ewconfig);  // put it in the session (if not a search window)

} else {
    log.debug("using configuration from session");
    ewconfig = (Config) configObject;
}


popupId = request.getParameter("popupid");
if (popupId == null) popupId = "";
popup = ! "".equals(popupId);
if (popup) {
    log.debug("this is a popup");
    String replace = request.getParameter("replace");
    if ("true".equals(replace)) { // searchlists (and other?)  popups must be replaced, otherwise they 'inherit' properites of othere searclists on the page...
        if (! ewconfig.subObjects.empty()) {
            Config.SubConfig topObj = (Config.SubConfig) ewconfig.subObjects.peek();
            topObj.popups.remove(popupId);
            if (log.isDebugEnabled()) log.debug("Removing the '" + popupId + "' popup");
        }
     }
} else {
    log.debug("this is not a popup");
}



String refer = ewconfig.backPage;
if (log.isDebugEnabled()) log.trace("backpage in root-config is " + refer);

if (request.getParameter("logout") != null) {
    %><mm:cloud method="logout" /><%
    // what to do if 'logout' is requested?
    // return to the deeped backpage and clear the session.
    log.debug("logout parameter given, clearing session");
    session.removeAttribute(sessionKey);
    log.debug("Redirecting to " + refer);
    if (! refer.startsWith("http:")) {
        refer = response.encodeRedirectURL(request.getContextPath() + refer);
    }
    response.sendRedirect(refer);
    return;
}
ewconfig.sessionKey = sessionKey;
configurator = new Configurator(request, response, ewconfig);

// removing top page from the session
if (request.getParameter("remove") != null) {

    if (log.isDebugEnabled()) log.debug("Removing top object requested from " + configurator.getBackPage());
    if(! ewconfig.subObjects.empty()) {    
        if (! popup) { // remove inline             
            log.debug("popping one of subObjects " + ewconfig.subObjects);
            closedObject = ewconfig.subObjects.pop();
        } else { //popup
            log.debug("a separate running popup, so remove sessiondata for " + popupId);
            Config.SubConfig top = (Config.SubConfig) ewconfig.subObjects.peek();
            Stack stack =  (Stack) top.popups.get(popupId);
            closedObject = stack.pop();
            if (stack.size() == 0) {
                top.popups.remove(popupId);        
                log.debug("going to close this window"); 
%>
<html>
<script language="javascript">
 try { // Mac IE doesn't always support window.opener.
<%
 if (closedObject instanceof Config.WizardConfig && ((Config.WizardConfig) closedObject).wiz.committed()) {
   // XXXX I find all this stuff in wizard.jsp too. Why??
   
   
   log.debug("A popup was closed (commited)");
   String sendCmd = "";
   String objnr = "";
   Config.WizardConfig popupWiz= (Config.WizardConfig) closedObject;
   // we move from a popup sub-wizard to a parent wizard...
   // with an inline popupwizard we should like to pass the newly created or updated
   // item to the 'lower' wizard.
   objnr=popupWiz.objectNumber;
   if ("new".equals(objnr)) {
     // obtain new object number
     objnr=popupWiz.wiz.getObjectNumber();
     if (log.isDebugEnabled()) log.debug("Objectnumber was 'new', now " + objnr);
     String parentFid = popupWiz.parentFid;
     if ((parentFid!=null) && (!parentFid.equals(""))) {
       log.debug("Settings. Sending an add-item command ");
       String parentDid = popupWiz.parentDid;
       sendCmd="cmd/add-item/"+parentFid+"/"+parentDid+"//";
     }
   } else {
     if (log.isDebugEnabled()) log.debug("Aha, this was existing, send an 'update-item' cmd for object " + objnr);
     sendCmd="cmd/update-item////";
   }
   if (log.isDebugEnabled()) log.debug("Sending command " + sendCmd + " , " + objnr);
   %>
   window.opener.doSendCommand("<%=sendCmd%>","<%=objnr%>");
<%          } %>
 } catch (e) {}
 window.close();
</script>
</html>
<%
            done = true;
            }
        } // popup
    } // not subObject empty

    if (ewconfig.subObjects.empty()) { // it _is_ empty? Then we are ready.
        log.debug("last object cleared, redirecting");
        if (! refer.startsWith("http:")) {
            refer = response.encodeRedirectURL(request.getContextPath() + refer);
        }
        log.debug("Redirecting to " + refer);
        response.sendRedirect(refer);
        done = true;
    } else if (ewconfig.subObjects.peek() instanceof Config.ListConfig) {
        log.debug("Redirecting to list");
        response.sendRedirect(response.encodeRedirectURL("list.jsp?proceed=true&sessionkey="+sessionKey));
        done = true;
    }
}


if (!done) {
    if (log.isDebugEnabled()) {
        log.debug("Stack "            + ewconfig.subObjects);
        if (! ewconfig.subObjects.empty()) {
            Config.SubConfig topObj = (Config.SubConfig) ewconfig.subObjects.peek();
            log.debug("Popups "           + topObj.popups  );
        }
        log.debug("URIResolver "      + ewconfig.uriResolver.getPrefixPath());
    }
    log.service("end of settings.jsp");// meaning that the rest of the list/wizard page will be done (those include setting.jsp).
}
%></mm:log><%
    if (done) return;
%><mm:import externid="loginmethod" from="parameters">loginpage</mm:import>

