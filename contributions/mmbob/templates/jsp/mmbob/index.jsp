<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<%@ include file="thememanager/loadvars.jsp" %>
<HTML>
<HEAD>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBase forums</title>
</head>
<mm:import externid="forumid" jspvar="forumid">unknown</mm:import>
<mm:compare referid="forumid" value="unknown">
	<table align="center" cellpadding="0" cellspacing="0" class="list" style="margin-top : 40px;" width="75%">
		<tr><th>MMBob system error</th></tr>
		<tr><td height="40"><b>ERROR : </b> No forum id is provided, if this is a new install try <a href="forums.jsp">forums.jsp</a> instead to create a new forum.</td></tr>
	</table>
</mm:compare>

<mm:compare referid="forumid" value="unknown" inverse="true">
<!-- login part -->
<%@ include file="getposterid.jsp" %>
<!-- end login part -->

<mm:locale language="$lang"> 

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->


<center>
<mm:include page="path.jsp?type=index" />

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
  		  <mm:nodefunction set="mmbob" name="getForumInfo" referids="forumid,posterid">
  			<mm:import id="adminmode"><mm:field name="isadministrator" /></mm:import>
			<tr>
				<mm:compare referid="posterid" value="-1">
				<th width="100"><a href="newposter.jsp?forumid=<mm:write referid="forumid" />"><img src="images/guest.gif" border="0"></a></th>
				<td align="left">
				<form action="login.jsp?forumid=<mm:write referid="forumid" />" method="post">
				<mm:present referid="loginfailed">
				<br />
				<center><h4>** fout loginnaam of wachtwoord, probeer opnieuw **</h4></center>
				<center> <a href="<mm:url page="remail.jsp" referids="forumid" />">Wachtwoord vergeten ?, Druk hier.</a></center>
			
				<p />
				</mm:present>
				<mm:notpresent referid="loginfailed">
				<mm:field name="description" />
				<p />
				<b>inloggen</b><p />
				</mm:notpresent>
				account : <input size="12" name="account">
				wachtwoord : <input size="12" type="password" name="password">
				<input type="submit" value="inloggen" />
				</form><p />
				</mm:compare>
				<mm:compare referid="posterid" value="-1" inverse="true">
				<th width="100">
				<a href="profile.jsp?forumid=<mm:write referid="forumid" />&posterid=<mm:write referid="posterid" />">
				<mm:field name="active_account" /><br />
				<mm:field name="active_avatar"><mm:compare value="-1" inverse="true">
               			<mm:node number="$_">
		                 <img src="<mm:image template="s(80x80)" />" width="80" border="0">
               			</mm:node>
				</mm:compare></mm:field></a>
				<a href="logout.jsp?forumid=<mm:write referid="forumid" />">Logout</a>
				</th>
				<td align="left" valign="top">
					<mm:compare referid="image_logo" value="" inverse="true">
					<br />
					<center><img src="<mm:write referid="image_logo" />" width="98%"></center>
					<br />
					</mm:compare>
					<mm:compare referid="image_logo" value="">
					<h4>Welcome <mm:field name="active_firstname" /> <mm:field name="active_lastname" /> (<mm:field name="active_account" />) <br /> to the <mm:field name="name" /> forum !</h4><p />

					</mm:compare>
					Last time logged in : <mm:field name="active_lastseen"><mm:compare value="" inverse="true"><mm:field name="active_lastseen"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field></mm:compare></mm:field><br />
					Member since : <mm:field name="active_firstlogin"><mm:compare value="" inverse="true"><mm:field name="active_firstlogin"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field></mm:compare></mm:field><br />
					Number of messages : <mm:field name="active_postcount" /> Level : <mm:field name="active_level" />
					<p><br /l
					<b>Je hebt 0 nieuwe en 0 ongelezen <a href="<mm:url page="privatemessages.jsp" referids="forumid" />">prive berichten</a></b><h4>At the moment : <mm:field name="postersonline" /> users online.</h4>
				</mm:compare>
</td>
				<th width="250" align="left" valign="top">
				<b>Areas</b> : <mm:field name="postareacount" /> <b>Topics </b> : <mm:field name="postthreadcount" /><br />
				<b>Messages</b> : <mm:field name="postcount" /> <b>Views </b> : <mm:field name="viewcount" /><br />
				<b>Members</b> : <mm:field name="posterstotal" /> <b>New</b> : <mm:field name="postersnew" /> <b>Online</b> : <mm:field name="postersonline" /><p />
				<b>Last posting</b> : <mm:field name="lastposttime"><mm:compare value="-1" inverse="true"><mm:field name="lastposttime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field> door <mm:field name="lastposter" /> '<mm:field name="lastsubject" />'</mm:compare><mm:compare value="-1">nog geen berichten</mm:compare></mm:field>
				</th>
			</tr>
		  </mm:nodefunction>
</table>

<table cellpadding="0" cellspacing="0" style="margin-top : 10px;" width="95%">
  <tr>
   <td align="right">
	<a href="<mm:url page="moderatorteam.jsp" referids="forumid" />">Het moderator team</a> | <a href="<mm:url page="onlineposters.jsp" referids="forumid" />">Leden nu online | <a href="<mm:url page="allposters.jsp" referids="forumid" />">Alle Leden</a></a>
   </td>
  </tr>
</table>

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 2px;" width="95%">
   <tr><th>area name</th><th>topics</th><th>messages</th><th>views</th><th>last posting</th></tr>
  		  <mm:nodelistfunction set="mmbob" name="getPostAreas" referids="forumid,posterid">
			<tr><td align="left"><a href="postarea.jsp?forumid=<mm:write referid="forumid" />&postareaid=<mm:field name="id" />"><mm:field name="name" /></a>
			<p/>
			<mm:field name="description" />
			<p />
			Moderators : <mm:field name="moderators" />
			<p />
			 </td>
				<td><mm:field name="postthreadcount" /></td>
				<td><mm:field name="postcount" /></td>
				<td><mm:field name="viewcount" /></td>
				<td align="left" valign="top"><mm:field name="lastposttime"><mm:compare value="-1" inverse="true"><mm:field name="lastposttime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field> door <mm:field name="lastposter" /><p /><mm:field name="lastsubject" /></mm:compare><mm:compare value="-1">nog geen berichten</mm:compare></mm:field></td>
			</tr>
		  </mm:nodelistfunction>
</table>
  <mm:compare referid="adminmode" value="true">
	<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
	<tr><th align="lef">Administratie Functies</th></tr>
	<td>
	<p />
	<a href="<mm:url page="changeforum.jsp">
                  <mm:param name="forumid" value="$forumid" />
                 </mm:url>">Forum aanpassen</a><br />
	<a href="<mm:url page="newpostarea.jsp">
                  <mm:param name="forumid" value="$forumid" />
                 </mm:url>">Nieuw gebied toevoegen</a>
	<p />
	</td>
	</tr>
	</table>
  </mm:compare>
</mm:locale>
</mm:compare>
</mm:cloud>
</center>
</html>
