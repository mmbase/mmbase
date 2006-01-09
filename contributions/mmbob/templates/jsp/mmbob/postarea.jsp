<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<mm:import externid="forumid" />
<%@ include file="thememanager/loadvars.jsp" %>
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBase Forum</title>
</head>
<body>

<mm:import externid="adminmode">false</mm:import>
<mm:import externid="postareaid" />
<mm:import externid="page">1</mm:import>

<!-- login part -->
<%@ include file="getposterid.jsp" %>
<!-- end login part -->

<mm:nodefunction set="mmbob" name="getForumConfig" referids="forumid,posterid">
  <mm:field name="postingsperpage" id="pagesize" write="false"/>
</mm:nodefunction>

<mm:notpresent referid="pagesize">
<mm:import id="pagesize">20</mm:import>
</mm:notpresent>


<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->

<mm:locale language="$lang"> 
<%@ include file="loadtranslations.jsp" %>

<div class="header">
    <mm:import id="headerpath" jspvar="headerpath"><mm:function set="mmbob" name="getForumHeaderPath" referids="forumid"/></mm:import>
    <jsp:include page="<%=headerpath%>"/>
</div>
                                                                                                              
<div class="bodypart">
<mm:nodefunction set="mmbob" name="getForumInfo" referids="forumid,posterid">
<mm:import id="logoutmodetype"><mm:field name="logoutmodetype" /></mm:import>
<mm:import id="navigationmethod"><mm:field name="navigationmethod" /></mm:import>
<mm:import id="active_nick"><mm:field name="active_nick" /></mm:import>
<mm:include page="path.jsp?type=postarea" referids="logoutmodetype,forumid,posterid,active_nick" />
</mm:nodefunction>

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
  		  <mm:nodefunction set="mmbob" name="getPostAreaInfo" referids="forumid,postareaid,posterid,page">
			<mm:import id="guestwritemodetype"><mm:field name="guestwritemodetype" /></mm:import>
			<mm:import id="threadstartlevel"><mm:field name="threadstartlevel" /></mm:import>
			<mm:compare referid="posterid" value="-1" inverse="true">
				<mm:import id="guestwritemodetype" reset="true">open</mm:import>
			</mm:compare>
			<mm:import id="navline"><mm:field name="navline" /></mm:import>
			<mm:import id="pagecount"><mm:field name="pagecount" /></mm:import>
			<tr><th colspan="2" align="left">
					<mm:compare referid="image_logo" value="" inverse="true">
					<center><img src="<mm:write referid="image_logo" />" width="60%" ></center>
					<br />
					</mm:compare>
			<b><mm:write referid="mlg.Area_name"/></b> : <mm:field name="name" /> <b><mm:write referid="mlg.Topics"/></b> : <mm:field name="postthreadcount" /> <b><mm:write referid="mlg.Messages"/></b> : <mm:field name="postcount" /> <b><mm:write referid="mlg.Views"/></b> : <mm:field name="viewcount" /><br />
			<b><mm:write referid="mlg.Last_posting"/></b> : <mm:field name="lastposttime"><mm:compare value="-1" inverse="true"><mm:field name="lastposttime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field> <b><mm:write referid="mlg.by"/></b> <mm:field name="lastposter" /> <b> : '</b><mm:field name="lastsubject" /><b>'</b></mm:compare><mm:compare value="-1"><mm:write referid="mlg.no_messages"/></mm:compare></mm:field><br />
			<mm:import id="isadministrator"><mm:field name="isadministrator" /></mm:import>
                        <mm:import id="ismoderator"><mm:field name="ismoderator" /></mm:import>
		  </mm:nodefunction>
	<br />
	<b><mm:write referid="mlg.Moderators"/></b> :
  		  <mm:nodelistfunction set="mmbob" name="getModerators" referids="forumid,postareaid">
			<mm:field name="nick" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)<br />
		  </mm:nodelistfunction>
	</td>
	</tr>
</table>

<table cellpadding="0" cellspacing="0" style="margin-top : 10px;" width="95%">
	<tr>
	<form action="<mm:url page="postarea.jsp" referids="forumid" />" method="post">
	<td align="left" />
	<a href=""><mm:write referid="mlg.Area_name"/></a> <select name="postareaid" onChange="submit()">
            <mm:nodelistfunction set="mmbob" name="getPostAreas" referids="forumid,posterid">
		<mm:field name="id">
		<option value="<mm:field name="id" />" <mm:compare referid2="postareaid">selected</mm:compare>><mm:field name="name" />
		</mm:field>
	    </mm:nodelistfunction>
	</select>
	<!-- <input type="submit" value="go"> -->
	</td>
	</form>
	<td align="right">
	<a href="<mm:url page="bookmarked.jsp" referids="forumid" />">Bookmarked</a> | <a href="<mm:url page="search.jsp" referids="forumid,postareaid" />"><mm:write referid="mlg.Search" /></a>&nbsp;
	</td></tr>
</table>
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 2px;" width="95%">
  <tr>
    <th width="15" class="state">&nbsp;</th>
    <th width="15">&nbsp;</th>
    <th><mm:write referid="mlg.topic"/></th>
    <th><mm:write referid="mlg.author"/></th>
    <th><mm:write referid="mlg.replies"/></th>
    <th><mm:write referid="mlg.views"/></th>
    <th><mm:write referid="mlg.last_posting"/></th>
    <mm:compare referid="ismoderator" value="true">
      <th><mm:write referid="mlg.moderator"/></th>
    </mm:compare>
  </tr>

  <mm:nodelistfunction set="mmbob" name="getPostThreads" referids="forumid,postareaid,posterid,page,pagesize">
  <tr>
    <td><mm:field name="state"><mm:write referid="image_state_$_" /></mm:field></td>
    <td><mm:field name="mood"><mm:write referid="image_mood_$_" /></mm:field></td>
    <td align="left"><a href="thread.jsp?forumid=<mm:write referid="forumid" />&postareaid=<mm:write referid="postareaid" />&postthreadid=<mm:field name="id" />"><mm:field name="shortname" /></a> <mm:field name="navline" /> <mm:field name="emailonchange"><mm:compare value="true">[email]</mm:compare></mm:field> <mm:field name="bookmarked"><mm:compare value="true">[bookmarked]</mm:compare></mm:field></td>
    <td align="left"><mm:field name="creator" /></td>
    <td align="left"><mm:field name="replycount" /></td>
    <td align="left"><mm:field name="viewcount" /></td>
    <td align="left">
      <mm:field name="lastposttime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field> 
      <mm:write referid="mlg.by"/>
      <mm:field name="lastposternumber">
        <mm:compare value="-1" inverse="true">
          <a href="profile.jsp?forumid=<mm:write referid="forumid" />&posterid=<mm:field name="lastposternumber" />"><mm:field name="lastposter" /></a>
        </mm:compare>
        <mm:compare value="-1" ><mm:field name="lastposter" /></mm:compare>
      </mm:field>
      <a href="thread.jsp?forumid=<mm:write referid="forumid" />&postareaid=<mm:write referid="postareaid" />&postthreadid=<mm:field name="id" />&page=<mm:field name="pagecount" />#p<mm:field name="lastpostnumber" />">></a></td>


    </td>
    <mm:compare referid="ismoderator" value="true">
    <td>
      <a href="<mm:url page="removepostthread.jsp" referids="forumid,postareaid"><mm:param name="postthreadid"><mm:field name="id" /></mm:param></mm:url>">X</a> / <a href="<mm:url page="editpostthread.jsp" referids="forumid,postareaid"><mm:param name="postthreadid"><mm:field name="id" /></mm:param></mm:url>">E</a> / <a href="<mm:url page="movepostthread.jsp" referids="forumid,postareaid"><mm:param name="postthreadid"><mm:field name="id" /></mm:param></mm:url>">M</a>
    </td>
    </mm:compare>
  </tr>
  </mm:nodelistfunction>
</table>

<mm:compare referid="pagecount" value="1" inverse="true">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 5px;" border="0" width="95%">
	<tr>
	<td align="left">
	<mm:write referid="mlg.Pages"/> : <mm:write referid="navline" />
	</td></tr>
</table>
</mm:compare>
<table cellpadding="0" cellspacing="0" style="margin-top : 5px;" width="95%">
	<mm:compare referid="threadstartlevel" value="">
	<tr><td align="left"><mm:compare referid="guestwritemodetype" value="open"><a href="<mm:url page="newpost.jsp"><mm:param name="forumid" value="$forumid" /><mm:param name="postareaid" value="$postareaid" /></mm:url>"><img src="<mm:write referid="image_newmsg" />" border="0" /></a></mm:compare> 
	</mm:compare>
	<mm:compare referid="threadstartlevel" value="moderator">
	<mm:compare referid="ismoderator" value="true">
	<tr><td align="left"><mm:compare referid="guestwritemodetype" value="open"><a href="<mm:url page="newpost.jsp"><mm:param name="forumid" value="$forumid" /><mm:param name="postareaid" value="$postareaid" /></mm:url>"><img src="<mm:write referid="image_newmsg" />" border="0" /></a></mm:compare> 
	</mm:compare>
	</mm:compare>
	</td></tr>
</table>
<br />
<table>
<tr><td valign="top" width="50%">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 5px; margin-left : 30px" align="left">
	<tr><td align="left">
	<br />
	<mm:write referid="image_state_normal" /> <mm:write referid="mlg.open_topic"/><p />
	<mm:write referid="image_state_normalnew" /> <mm:write referid="mlg.open_topic_unread"/><p />
	<mm:write referid="image_state_hot" /> <mm:write referid="mlg.open_topic_popular"/><p />
	<mm:write referid="image_state_hotnew" /> <mm:write referid="mlg.open_topic_popular_unread"/><p />
	<mm:write referid="image_state_pinned" /> <mm:write referid="mlg.pinned_topic"/><p />
	<mm:write referid="image_state_closed" /> <mm:write referid="mlg.closed_topic"/><p />
	<mm:write referid="image_state_normalme" /> <mm:write referid="mlg.topic_to_which_you_have_contributed"/><p />
	</td></tr>
</table>
</td>
<td valign="top" width="50%">
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 5px; margin-left : 30px" align="left">
	<tr><td align="left">
	<br />
        <mm:compare referid="threadstartlevel" value="">
	<mm:write referid="mlg.You_may_post_new_threads"/> <p />
	</mm:compare>
        <mm:compare referid="threadstartlevel" value="all">
	<mm:write referid="mlg.You_may_post_new_threads"/> <p />
	</mm:compare>
        <mm:compare referid="threadstartlevel" value="moderator">
	<mm:compare referid="ismoderator" value="true">
	<mm:write referid="mlg.You_may_post_new_threads"/> <p />
	</mm:compare>
	<mm:compare referid="ismoderator" value="false">
	<mm:write referid="mlg.You_may_not_post_new_threads"/> <p />
	</mm:compare>
	</mm:compare>
	<mm:write referid="mlg.You_may_post_new_replies"/> <p />
	<mm:write referid="mlg.You_may_edit_your_posts"/> <p />
	<mm:write referid="mlg.You_may_delete_your_posts"/> <p />
	</td></tr>
</table>
</td></tr>
</table>

<mm:compare referid="isadministrator" value="true">
        <table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;margin-bottom : 20px;" width="95%" align="left">
        <tr><th align="left"><mm:write referid="mlg.Admin_tasks"/></th></tr>
        <td align="left">
        <p />
  				<a href="<mm:url page="admin/changepostarea.jsp" referids="forumid,postareaid" />"><mm:write referid="mlg.change_area"/></a><br />

  				<a href="<mm:url page="admin/removepostarea.jsp" referids="forumid,postareaid" />"><mm:write referid="mlg.remove_area"/></a><br />

  				<a href="<mm:url page="admin/newmoderator.jsp">
				<mm:param name="forumid" value="$forumid" />
				<mm:param name="postareaid" value="$postareaid" />
				</mm:url>"><mm:write referid="mlg.add_moderator"/></a><br />
  				<a href="<mm:url page="admin/removemoderator.jsp">
				<mm:param name="forumid" value="$forumid" />
				<mm:param name="postareaid" value="$postareaid" />
				</mm:url>"><mm:write referid="mlg.remove_moderator"/></a><br />
	</td></tr>
</table>
<br />
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
