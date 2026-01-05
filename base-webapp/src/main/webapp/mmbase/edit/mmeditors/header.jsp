<% response.setContentType("text/html; charset=utf-8");
// as many browsers as possible should not cache:
response.setHeader("Cache-Control", "no-cache");
response.setHeader("Pragma","no-cache");
long now = System.currentTimeMillis();
response.setDateHeader("Expires",  now);
response.setDateHeader("Last-Modified",  now);
response.setDateHeader("Date",  now);
%><%@ page language="java" contentType="text/html; charset=utf-8"
%><%@ page import="java.util.*"
%><%@ page import="org.mmbase.bridge.*"
%><%@ page import="org.mmbase.bridge.Module"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"  prefix="mm"
%><%!
  String translate(Module mmlanguage, String value) {
    if (mmlanguage!=null) {
        return mmlanguage.getInfo("GET-"+value);
    } else {
        return value;
    }
  }
%><mm:import id="rank"><%= org.mmbase.util.xml.UtilReader.get("editors.xml").getProperties().getProperty("rank", "basic user")%></mm:import>
