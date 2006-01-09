<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<mm:import externid="forumid" />
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="postareaid" />
<mm:import externid="postthreadid" />
<mm:import externid="page">1</mm:import>

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

<!-- moderator check -->
<%-- Administrative / Moderative functions --%>
<mm:nodefunction set="mmbob" name="getPostAreaInfo" referids="forumid,postareaid,posterid,page">
   <mm:import id="ismoderator"><mm:field name="ismoderator" /></mm:import>
</mm:nodefunction>

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

<mm:compare referid="ismoderator" value="false">
  <table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="45%">
    <tr><th><mm:write referid="mlg.Edit_postthread" /></th></tr>
    <tr><td><font color="red"><b><mm:write referid="mlg.Access_denied" /></font></b></td></tr>
  </table>
</mm:compare>

<mm:compare referid="ismoderator" value="true">

  <table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="45%">
    <mm:node referid="postthreadid">
    <tr><th colspan="3"><mm:write referid="mlg.Move_postthread" /></th></tr>
    <form action="<mm:url page="postarea.jsp" referids="forumid,postareaid,postthreadid" />" method="post">
	<tr><th width="200"><mm:write referid="mlg.Areas" /></th><td colspan="2" align="middle">
		<select name="newpostareaid">
 		<mm:nodelistfunction set="mmbob" name="getPostAreas" referids="forumid,posterid">
		<option value="<mm:field name="id" />"><mm:field name="name" />
		</mm:nodelistfunction>
		</select>
	</td></th>
	<tr><th>&nbsp;</th><td align="center">
	<input type="hidden" name="action" value="movepostthread">
	<input type="submit" value="<mm:write referid="mlg.Move"/>">
	</td>
	<td align="center">
	</mm:node>
        </form>
  	<form action="<mm:url page="postarea.jsp">
	<mm:param name="forumid" value="$forumid" />
	<mm:param name="postareaid" value="$postareaid" />
	</mm:url>"
 	method="post">
	<p />
	<input type="submit" value="<mm:write referid="mlg.Cancel"/>">
  	</form>
	</td>
	</tr>
  </table>
</mm:compare>

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
