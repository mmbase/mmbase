<%@ page errorPage="exception.jsp" %><%@ include file="settings.jsp" %>
<%@ page import="org.mmbase.applications.editwizard.*" %>
<%@ page import="org.mmbase.applications.editwizard.Config" %>
<%@ page import="java.util.*" %>
<%@ page import="java.io.Writer" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="org.w3c.dom.Node" %>
<%
    /**
     * debug.jsp
     *
     * @since    MMBase-1.6
     * @version  $Id: debug.jsp,v 1.9 2003-12-11 10:41:34 vpro Exp $
     * @author   Kars Veling
     * @author   Michiel Meeuwissen
     */
    String wizard="";
      
    Object con = null;
    if (!ewconfig.subObjects.empty()) {
      con = ewconfig.subObjects.peek();
      if (popup) { 
        Stack stack = (Stack) ((Config.SubConfig)con).popups.get(popupId);
        if ((stack != null) && !stack.empty()) {
            con = stack.peek();
        }
      }
    }

    Document doc = Utils.parseXML("<debugdata/>");
    if (con instanceof Config.WizardConfig) {
        wizard=((Config.WizardConfig)con).wizard;
        add(doc, ((Config.WizardConfig)con).wiz.getData(), wizard);
        add(doc, ((Config.WizardConfig)con).wiz.getSchema(), wizard);
        add(doc, ((Config.WizardConfig)con).wiz.getPreForm(), wizard);
    }
    File template = ewconfig.uriResolver.resolveToFile("xsl/debug.xsl");
    Map map = new HashMap();  
    map.put("session_byte_size", "" + org.mmbase.util.SizeOf.getByteSize(ewconfig));
    Utils.transformNode(doc, template, ewconfig.uriResolver, out,  map);
%>
<%!
    public void add(Document dest, Document src, String name) {

        Node n = dest.importNode(src.getDocumentElement().cloneNode(true), true);
        Utils.setAttribute(n, "debugname", name);
        dest.getDocumentElement().appendChild(n);
    }

%>

