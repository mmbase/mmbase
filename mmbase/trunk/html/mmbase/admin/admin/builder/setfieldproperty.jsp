<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@page import="org.mmbase.bridge.*" %>
<%@page import="java.util.*" %>
<mm:cloud name="mmbase">
<% String builder = request.getParameter("builder");
   String field = request.getParameter("field");
   String name=request.getParameter("name");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<html xmlns="http://www.w3.org/TR/xhtml">
<head>
<title>Administrate Builder <%=builder%>, <%=name%> of Field <%=field%></title>
<link rel="stylesheet" type="text/css" href="../../css/mmbase.css" />
<meta http-equiv="pragma" value="no-cache" />
<meta http-equiv="expires" value="0" />
</head>
<body class="basic" >
<% Module mmAdmin=LocalContext.getCloudContext().getModule("mmadmin");
   String cmd=request.getParameter("cmd");
   String country=request.getParameter("country");
   String value=null;
   if (country!=null) {
       value= mmAdmin.getInfo("GETGUINAMEVALUE-"+builder+"-"+field+"-"+country,request,response);
   } else {
       value= mmAdmin.getInfo("GETBUILDERFIELD-"+builder+"-"+field+"-"+cmd,request,response);
   }
   String property="value";
%>
<table summary="builder field property data" width="93%" cellspacing="1" cellpadding="3">
<tr align="left">
  <th class="header">Property</td>
  <% if (cmd.equals("newguiname")) { %>
    <th class="header">Country Code / Value</td>
  <% } else { %>
    <th class="header">Value</td>
  <% } %>
  <th class="header">Change</td>
</tr>

<form action="field.jsp" method="POST">
<tr>
  <td class="data"><%=name%></td>
 <td class="data">
  <% if (cmd.equals("dbmmbasetype")) {%>
<%@include file="properties/dbmmbasetype.jsp" %>
  <% } else if (cmd.startsWith("editor")) {%>
<%@include file="properties/editorpos.jsp" %>
  <% } else if (cmd.equals("guitype")) {%>
<%@include file="properties/guitype.jsp" %>
  <% } else if (cmd.equals("dbstate")) {%>
<%@include file="properties/dbstate.jsp" %>
  <% } else if (cmd.equals("dbkey") || cmd.equals("dbnotnull")) {%>
<%@include file="properties/truefalse.jsp" %>
  <% } else if (cmd.equals("newguiname")) {
        cmd="guiname";
        value=null;
  %>
<% property="country"; %>
<%@include file="properties/iso639.jsp" %>
/ <input type="text" name="value" value="<%=value%>" />
  <% } else { %>
<input type="text" name="value" value="<%=value%>" />
  <% } %>

</td>
<td class="linkdata">
    <input type="hidden" name="builder" value="<%=builder%>" />
    <input type="hidden" name="field" value="<%=field%>" />
<% if (country!=null) { %>
    <input type="hidden" name="country" value="<%=country%>" />
<% } %>
    <input type="hidden" name="cmd" value="BUILDER-SET<%=cmd.toUpperCase()%>" />
    <input type="submit" value="Change" />
</td>
</tr>
</form>

<tr><td>&nbsp;</td></tr>

<tr>
<td class="navigate"><a href="field.jsp?builder=<%=builder%>&field=<%=field%>"><img src="../../images/pijl2.gif" alt="back" border="0" align="left" /></td>
<td class="data" colspan="3">Return to Builder Field Administration</td>
</tr>
</table>
</body></html>
</mm:cloud>
