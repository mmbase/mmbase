<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="forumid" />
<mm:import externid="postareaid" />
<mm:import externid="postthreadid" />
<mm:import externid="postingid" />

<!-- login part -->
<%@ include file="getposterid.jsp" %>
<!-- end login part -->

<mm:locale language="$lang">
<%@ include file="loadtranslations.jsp" %>

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->

<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
</head>
<body>

<div class="header">
</div>

<div class="bodypart">

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr><th colspan="3"><mm:write referid="mlg.send"/> <mm:write referid="mlg.private_message" /></th></tr>
  <form action="<mm:url page="thread.jsp" referids="forumid,postareaid,postthreadid,postingid" />" method="post">
	<tr><th><mm:write referid="mlg.To"/></th><td colspan="2">
		<mm:node number="$postingid">
		<mm:field name="c_poster" />
		<input name="to" type="hidden" value="<mm:field name="c_poster" />">
		<input name="poster" type="hidden" value="<mm:node referid="posterid"><mm:field name="account" /></mm:node>">
	</td></tr>
	<tr><th><mm:write referid="mlg.Subject"/></th><td colspan="2"><input name="subject" style="width: 100%" value="Re: <mm:field name="subject" />"></td></th>
	</mm:node>
	<tr><th><mm:write referid="mlg.Message" /></th><td colspan="2"><textarea name="body" rows="20" style="width: 100%"></textarea></td></tr>
	<tr><th>&nbsp;</th><td>
	<input type="hidden" name="action" value="newprivatemessage">
	<center><input type="submit" value="<mm:write referid="mlg.Send"/> <mm:write referid="mlg.message"/>"></center>
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
	<input type="submit" value="<mm:write referid="mlg.Cancel"/>">
        </center>
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
