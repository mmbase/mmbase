<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>
<mm:import externid="forumid" />
<mm:import externid="postareaid" />

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->

<!-- login part -->
  <%@ include file="getposterid.jsp" %>
<!-- end login part -->                                                                                                                      
<mm:locale language="$lang">
<%@ include file="loadtranslations.jsp" %>

<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
</head>
<body>

<div class="header">
    <mm:import id="headerpath" jspvar="headerpath"><mm:function set="mmbob" name="getForumHeaderPath" referids="forumid"/></mm:import>
    <jsp:include page="<%=headerpath%>"/>
</div>
                                                                                              
<div class="bodypart">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr><th colspan="3"><mm:write referid="mlg.Change_existing_area" /></th></tr>

  <mm:node number="$postareaid">
  <form action="<mm:url page="index.jsp" referids="forumid,postareaid" />" method="post">
	<tr><th><mm:write referid="mlg.Name"/></th><td colspan="2">
	<input name="name" size="70" value="<mm:field name="name" />" style="width: 100%">
	</td></tr>
	<tr><th><mm:write referid="mlg.Description"/></th><td colspan="2">
	<textarea name="description" rows="5" style="width: 100%"><mm:field name="description" /></textarea>
	</td></tr>
        <input type="hidden" name="admincheck" value="true">
	<input type="hidden" name="action" value="changepostarea">
	<tr><th>&nbsp;</th><td align="middle" >
	<input type="submit" value="<mm:write referid="mlg.Save"/>">
  	</form>
	</td>
	</mm:node>
	<td>
  	<form action="<mm:url page="index.jsp">
        <mm:param name="forumid" value="$forumid" />
	</mm:url>"
 	method="post">
	<p />
	<input type="submit" value="<mm:write referid="mlg.Cancel"/>">
  	</form>
	</td>
	</tr>

</table>

</div>

<div class="footer">
    <mm:import id="footerpath" jspvar="footerpath"><mm:function set="mmbob" name="getForumFooterPath" referids="forumid"/></mm:import>
    <jsp:include page="<%=footerpath%>"/>
</div>
                                                                                              
</body>
</html>

</mm:locale>
</mm:content>
</mm:cloud>

