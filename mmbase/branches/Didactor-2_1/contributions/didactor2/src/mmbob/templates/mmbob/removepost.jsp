<%-- !DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd" --%>
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud method="delegate" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%@ include file="thememanager/loadvars.jsp" %>
<%@ include file="settings.jsp" %>
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" referids="$referids" />" />
   <title>MMBob</title>
</HEAD>
<mm:import externid="forumid" />
<mm:import externid="postareaid" />
<mm:import externid="postthreadid" />
<mm:import externid="postingid" />

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->
<center>
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="80%">
  <tr><th colspan="3" align="left" ><di:translate key="mmbob.wantdeletemessage1" />: <p />
    <mm:node referid="postingid">
    <mm:field name="subject" /> <di:translate key="mmbob.wantdeletemessage2" /> <mm:field name="poster" /><p />
    <mm:field name="html(body)" />
    </mm:node><p /><p />?</th></tr>
  <tr><td>
  <form action="<mm:url page="postarea.jsp">
                    <mm:param name="forumid" value="$forumid" />
                    <mm:param name="postareaid" value="$postareaid" />
                    <mm:param name="postthreadid" value="$postthreadid" />
                    <mm:param name="postingid" value="$postingid" />
                </mm:url>" method="post">
    <input type="hidden" name="admincheck" value="true">
    <input type="hidden" name="action" value="removepost">
    <p />
    <center>
    <input type="submit" value="<di:translate key="mmbob.delete" />">
    </form>
    </td>
    <td>
    <form action="<mm:url page="thread.jsp">
    <mm:param name="forumid" value="$forumid" />
    <mm:param name="postareaid" value="$postareaid" />
    <mm:param name="postthreadid" value="$postthreadid" />
    </mm:url>"
    method="post">
    <p />
    <center>
    <input type="submit" value="<di:translate key="mmbob.cancel" />">
    </form>
    </td>
    </tr>

</table>
</html>
</mm:cloud>

