<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="forumid" />
<mm:import externid="postareaid" />
<mm:import externid="postthreadid" />

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
   <TITLE>MMBob</TITLE>
</HEAD>
<body>

<div class="header">
</div>
                                                                                              
<div class="bodypart">

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="40%">
  <tr><th colspan="3"><mm:write referid="mlg_Delete"/> <mm:write referid="mlg_topic"/>: <mm:node referid="postthreadid">'<mm:field name="subject" />'</mm:node> </th></tr>
  <tr><td colspan="3"><mm:write referid="mlg_Are_you_sure"/></td></tr>
  <tr><td>
  <form action="<mm:url page="postarea.jsp" referids="forumid,postareaid,postthreadid" />" method="post">
	<input type="hidden" name="moderatorcheck" value="true">
	<input type="hidden" name="action" value="removepostthread">
	<p />
	<center>
	<input type="submit" value="<mm:write referid="mlg_Yes_delete"/>">
  	</form>
	</td>
	<td>
  	<form action="<mm:url page="postarea.jsp">
	<mm:param name="forumid" value="$forumid" />
	<mm:param name="postareaid" value="$postareaid" />
	</mm:url>"
 	method="post">
	<p />
	<center>
	<input type="submit" value="<mm:write referid="mlg_Cancel"/>">
  	</form>
	</td>
	</tr>

</table>

</div>                                                                                               

<div class="footer">
</div>

</body>
</html>

</mm:locale>
</mm:content>
</mm:cloud>

