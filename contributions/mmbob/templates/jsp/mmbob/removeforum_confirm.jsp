<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
</head>
<body>
<mm:import externid="remforum" />

<div class="header">
    <mm:import id="headerpath" jspvar="headerpath"><mm:function set="mmbob" name="getForumHeaderPath" referids="forumid"/></mm:import>
    <jsp:include page="<%=headerpath%>"/>
</div>
                                                                                              
<div class="bodypart">

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="40%">
  <tr><th colspan="3" align="left">Echt forum <mm:node number="$remforum"><mm:field name="name" /></mm:node> verwijderen ?
	<p />
	Dit houd in dat alle gebieden, onderwerpen, reacties en posters worden weg gegooit !!!
  </th></tr>
  </td></tr>
  <tr><td>
  <form action="<mm:url page="forums.jsp"></mm:url>" method="post">
	<p />
	<center>
	<input type="hidden" name="remforum" value="<mm:write referid="remforum" />" /> <input type="hidden" name="action" value="removeforum" />
	<input type="submit" value="Ja, Echt Verwijderen">
  	</form>
	</td>
	<td>
  	<form action="<mm:url page="forums.jsp">
	</mm:url>"
 	method="post">
	<p />
	<center>
	<input type="submit" value="Oops, Nee toch niet">
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
</mm:content>
</mm:cloud>

