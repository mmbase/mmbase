<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="adminmode">false</mm:import>
<mm:import externid="forumid" />
<mm:import externid="boxname">Inbox</mm:import>
<mm:import externid="mailboxid" />
<mm:import externid="pathtype">privatemessages</mm:import>
<mm:import externid="posterid" id="profileid" />

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
   <title>MMBob</title>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
</head>
<body>

<div class="header">
</div>

<div class="bodypart" align="center">

<mm:include page="path.jsp?type=$pathtype" />
<table cellpadding="0" cellspacing="0" style="margin-top : 20px;" width="95%">
 <tr>
   <td width="160" valign="top">
	<table cellpadding="0" width="150">
	<tr><td>
	<table cellpadding="0" class="list" cellspacing="0" width="150">
	<tr><th><mm:write referid="mlg_Folder" /></th></tr>
	<mm:node referid="posterid">
	<mm:related path="posmboxrel,forummessagebox">
		<mm:node element="forummessagebox">
			<mm:field name="name">
			<mm:notpresent referid="mailboxid">
			<mm:compare referid2="boxname">
				<mm:remove referid="mailboxid" />
				<mm:import id="mailboxid"><mm:field name="number" /></mm:import>
			</mm:compare> 
			</mm:notpresent> 
			</mm:field>
			<tr><td><a href="<mm:url page="privatemessages.jsp" referids="forumid,mailboxid"><mm:param name="mailboxid"><mm:field name="number" /></mm:param></mm:url>"><mm:field name="name" /></a> (<mm:relatednodes type="forumprivatemessage"><mm:last><mm:size /></mm:last></mm:relatednodes>)</td></tr>
		</mm:node>
	</mm:related>
	</mm:node>
	</table>
	</td></tr>
	<tr><td>
        <form action="<mm:url page="privatemessages.jsp" referids="forumid,mailboxid" />" METHOD="POST">
	<table cellpadding="0" class="list" style="margin-top : 20px;" cellspacing="0" width="150">
	<tr><th><mm:write referid="mlg_Add_folder" /></th></tr>
	<tr><td><input name="newfolder" style="width: 98%" /></td></tr>
        <input name="action" type="hidden" value="newfolder">
	</table>
	</form>
	</td></tr>
	<tr><td>
	<table cellpadding="0" class="list" style="margin-top : 20px;" cellspacing="0" width="150">
	<mm:import id="barsize">150</mm:import>
        <mm:nodefunction set="mmbob" name="getQuotaInfo" referids="forumid,posterid,barsize">
	<tr><th colspan="3"><mm:write referid="mlg_PM_Quota" /></th></tr>
	<tr><td colspan="3"><mm:write referid="mlg_You_are_using" /> <mm:field name="quotausedpercentage" />% <mm:write referid="mlg_of_your_quota" /></td></tr>
	<tr><td colspan="3"><img src="images/<mm:field name="quotawarning" />.gif" height="7" width="<mm:field name="quotausedbar" />"></td></tr>
	<tr><td align="left" width="33%">0%</td><td align="middle" width="34%">50%</td><td align="right" width="33%">100%</td></tr>
	</mm:nodefunction>
	</table>
	</td></tr>
	</table>
   </td>
   <td valign="top">
	<table cellpadding="0" class="list" style="margin-top : 2px;" cellspacing="0" width="100%" border="1">
	<tr><th></th><th><mm:write referid="mlg_Subject" /></th><th><mm:write referid="mlg_Sender" /></th><th><mm:write referid="mlg_Date" /></th><th></th></tr>
	<mm:present referid="mailboxid">
   	<form action="<mm:url page="privatemessagesconfirmaction.jsp" referids="forumid,mailboxid" />" method="post">
	<mm:node referid="mailboxid">
	<mm:relatednodes type="forumprivatemessage" orderby="createtime" directions="down">
	<mm:first>
		<mm:import id="messagesfound">true</mm:import>
	</mm:first>
	<tr><td><mm:index /></td><td width="50%"><a href="<mm:url page="privatemessage.jsp" referids="forumid,mailboxid"><mm:param name="messageid"><mm:field name="number" /></mm:param></mm:url>"><mm:field name="subject" /></a> <mm:field name="viewstate"><mm:compare value="0">*<mm:write referid="mlg_UNSEEN" />*</mm:compare></mm:field></td><td width="25%" ><mm:field name="poster" /> (<mm:field name="fullname" />)</td><td width="25%"><mm:field name="createtime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field></td><td><input type="checkbox" name="selectedmessages"></td></tr>
	</mm:relatednodes>
	</mm:node>
	<tr>
		<th colspan="4">
		<mm:write referid="mlg_Actions"/> : <input type="submit" name="folderaction" value="<mm:write referid="mlg_new"/>"> -
		<mm:present referid="messagesfound">
		<input type="submit" name="folderaction" value="<mm:write referid="mlg_delete" />"> - 
		<input type="submit" name="folderaction" value="<mm:write referid="mlg_forward" />"> -
		<input type="submit" name="folderaction" value="<mm:write referid="mlg_email" />"> -
		<input type="submit" name="folderaction" value="<mm:write referid="mlg_move" />">
		</mm:present>
		<mm:notpresent referid="messagesfound">
			<input type="submit" name="folderaction" value="<mm:write referid="mlg_delete_mailbox" />">
		</mm:notpresent>
		</th>
	</tr>
	</form>
	</mm:present>
	</table>
   </td>
 </tr>
</table>

</div>
<div class="footer">
</div>

</mm:locale>
</mm:cloud>
</body>
</html>
