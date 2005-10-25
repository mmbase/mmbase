<%-- !DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd" --%>
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%@ include file="thememanager/loadvars.jsp" %>
<%@ include file="settings.jsp" %>
<HTML>
<HEAD>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
</head>

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->

<center>

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
   <tr><th><di:translate key="mmbob.forumname" /></th><th><di:translate key="mmbob.numberofmessages" /></th><th><di:translate key="mmbob.numberofviews" /></th><th><di:translate key="mmbob.numberofmembers" /></th></tr>
  <mm:nodelistfunction set="mmbob" name="getForums">
            <tr>
            <td><a href="start.jsp?forumid=<mm:field name="id" />"><mm:field name="name" /></a></td>
            <td><mm:field name="postcount" /></td>
            <td><mm:field name="viewcount" /></td>
            <td><mm:field name="posterstotal" /></td>
            </tr>
  </mm:nodelistfunction>
</table>
    <table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
    <tr><th align="left"><di:translate key="mmbob.adminfunctions" /></th></tr>
    <td>
    <p />
    <a href="<mm:url page="newforum.jsp"></mm:url>"><di:translate key="mmbob.addforum" /></a><br />
    <a href="<mm:url page="removeforum.jsp"></mm:url>"><di:translate key="mmbob.removeforum" /></a><br />
    <p />
    </td>
    </tr>
    </table>
</center>
</html>
</mm:cloud>
