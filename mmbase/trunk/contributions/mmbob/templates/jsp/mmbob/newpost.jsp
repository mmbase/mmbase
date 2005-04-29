<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="forumid" />
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
<mm:compare value="newpost" referid="action">
	<mm:import externid="poster" />
	<mm:import externid="subject" />
	<mm:import externid="body" />
	<mm:import externid="mood">happy</mm:import>
	<mm:nodefunction set="mmbob" name="newPost" referids="forumid,postareaid,poster,subject,body,mood">
		<mm:import id="postresult"><mm:field name="error" /></mm:import>	
	</mm:nodefunction>
</mm:compare>
</mm:present>
<!-- end action check -->
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
   <script language="JavaScript1.1" type="text/javascript" src="js/smilies.js"></script>
</head>
<body>

<div class="header">
    <mm:import id="headerpath" jspvar="headerpath"><mm:function set="mmbob" name="getForumHeaderPath" referids="forumid"/></mm:import>
    <jsp:include page="<%=headerpath%>"/>
</div>
                                                                                              
<div class="bodypart">
<mm:notpresent referid="postresult">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr><th colspan="3"><mm:write referid="mlg.Add_new_topic" /></th></tr>
  <form action="<mm:url page="newpost.jsp">
	<mm:param name="forumid" value="$forumid" />
	<mm:param name="postareaid" value="$postareaid" />
	</mm:url>" method="post" name="posting">
	<tr><th><mm:write referid="mlg.Name" /></th><td colspan="2">
		<mm:compare referid="posterid" value="-1" inverse="true">
		<mm:node number="$posterid">
		<mm:field name="account" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)
		<input name="poster" type="hidden" value="<mm:field name="account" />" >
		</mm:node>
		</mm:compare>
		<mm:compare referid="posterid" value="-1">
		<input name="poster" type="hidden" size="32" value="<mm:write referid="mlg.guest" /> " >
		<mm:write referid="mlg.guest" />	
		</mm:compare>
	</td></tr>
	<tr><th width="150"><mm:write referid="mlg.Topic" /></th><td colspan="2"><input name="subject" style="width: 100%" ></td></th>
	<tr><th width="150"><mm:write referid="mlg.Mood" /></th><td colspan="2">
		<select name="mood">
		<option value="normal"><mm:write referid="mlg.normal"/>
		<option value="mad"><mm:write referid="mlg.mad"/>
		<option value="happy"><mm:write referid="mlg.happy"/>
		<option value="sad"><mm:write referid="mlg.sad"/>
		<option value="question"><mm:write referid="mlg.question"/>
		<option value="warning"><mm:write referid="mlg.warning"/>
		<option value="joke"><mm:write referid="mlg.joke"/>
		<option value="idea"><mm:write referid="mlg.idea"/>
		<option value="suprised"><mm:write referid="mlg.surprised"/>
		</select></td></th>
	<tr><th valign="top"><mm:write referid="mlg.Message" /><center>

<mm:nodefunction set="mmbob" name="getPostAreaInfo" referids="forumid,postareaid,posterid,page">
<mm:field name="smileysenabled"><mm:compare value="true">
<table><tr><th width="100"><%@ include file="includes/smilies.jsp" %></th></tr></table>
</mm:compare></mm:field>
</mm:nodefunction>

</center></th><td colspan="2"><textarea name="body" rows="20" style="width: 100%"></textarea>
</td></tr>
	<tr><th>&nbsp;</th><td>
	<input type="hidden" name="action" value="newpost">
	<center><input type="submit" value="<mm:write referid="mlg.Save" />"></center>
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
	<input type="submit" value="<mm:write referid="mlg.Cancel" />">
        </center>
  	</form>
	</td>
	</tr>
</table>
</mm:notpresent>
<mm:present referid="postresult">
	<mm:compare referid="postresult" value="none">
	  <table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="60%">
	   <tr>
        	<form action="<mm:url page="postarea.jsp" referids="forumid,postareaid" />" method="post">
		<th>feedback
	  </tr>
	  <tr>
		<td align="center">
			Your message has been posted to this area
			<br />
			<br />
			<input type="submit" value="ok" />
		</td>
		</form>
	   </tr>
	  </table>
	</mm:compare>
	<mm:compare referid="postresult" value="none" inverse="true">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr><th colspan="3"><mm:write referid="mlg.Add_new_topic" /></th></tr>
  <tr><th colspan="3" align="center">
	<mm:compare referid="postresult" value="no_subject">
	<font color="red">*** problem missing topic,please enter a topic ***</font>
	</mm:compare>
	<mm:compare referid="postresult" value="no_body">
	<font color="red">*** problem missing text,please enter a text ***</font>
	</mm:compare>
  </th></tr>
  <form action="<mm:url page="newpost.jsp">
	<mm:param name="forumid" value="$forumid" />
	<mm:param name="postareaid" value="$postareaid" />
	</mm:url>" method="post" name="posting">
	<tr><th><mm:write referid="mlg.Name" /></th><td colspan="2">
		<mm:compare referid="posterid" value="-1" inverse="true">
		<mm:node number="$posterid">
		<mm:field name="account" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)
		<input name="poster" type="hidden" value="<mm:field name="account" />" >
		</mm:node>
		</mm:compare>
		<mm:compare referid="posterid" value="-1">
		<input name="poster" type="hidden" size="32" value="<mm:write referid="mlg.guest" /> " >
		<mm:write referid="mlg.guest" />	
		</mm:compare>
	</td></tr>
	<tr><th width="150"><mm:compare referid="postresult" value="no_subject"><font color="red"></mm:compare><mm:write referid="mlg.Topic" /><mm:compare referid="postresult" value="no_subject"></font></mm:compare></th><td colspan="2"><input name="subject" style="width: 100%" value="<mm:write referid="subject" />"></td></th>
	<tr><th width="150"><mm:write referid="mlg.Mood" /></th><td colspan="2">
		<select name="mood">
		<option value="normal"><mm:write referid="mlg.normal"/>
		<option value="mad"><mm:write referid="mlg.mad"/>
		<option value="happy"><mm:write referid="mlg.happy"/>
		<option value="sad"><mm:write referid="mlg.sad"/>
		<option value="question"><mm:write referid="mlg.question"/>
		<option value="warning"><mm:write referid="mlg.warning"/>
		<option value="joke"><mm:write referid="mlg.joke"/>
		<option value="idea"><mm:write referid="mlg.idea"/>
		<option value="suprised"><mm:write referid="mlg.surprised"/>
		</select></td></th>
	<tr><th valign="top"><mm:compare referid="postresult" value="no_body"><font color="red"></mm:compare><mm:write referid="mlg.Message" /><mm:compare referid="postresult" value="no_body"></font></mm:compare><center>

<mm:nodefunction set="mmbob" name="getPostAreaInfo" referids="forumid,postareaid,posterid,page">
<mm:field name="smileysenabled"><mm:compare value="true">
<table><tr><th width="100"><%@ include file="includes/smilies.jsp" %></th></tr></table>
</mm:compare></mm:field>
</mm:nodefunction>

</center></th><td colspan="2"><textarea name="body" rows="20" style="width: 100%"><mm:write referid="body" /></textarea>
</td></tr>
	<tr><th>&nbsp;</th><td>
	<input type="hidden" name="action" value="newpost">
	<center><input type="submit" value="<mm:write referid="mlg.Save" />"></center>
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
	<input type="submit" value="<mm:write referid="mlg.Cancel" />">
        </center>
  	</form>
	</td>
	</tr>
</table>
	</mm:compare>
</mm:present>
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

