<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities">
<%@ include file="thememanager/loadvars.jsp" %>
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:write referid="style_default" />" />
   <title>MMBob</title>
   <script language="JavaScript1.1" type="text/javascript" src="js/smilies.js"></script>
</head>
<body>
<mm:import externid="forumid" />
<mm:import externid="postareaid" />
<mm:import externid="postthreadid" />
<mm:import externid="page">1</mm:import>

<!-- login part -->
<%@ include file="getposterid.jsp" %>
<!-- end login part -->

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="actions.jsp" />
</mm:present>
<!-- end action check -->

<mm:node number="$postthreadid">
  <mm:import id="threadstate"><mm:field name="state" /></mm:import>
  <mm:import id="threadmood"><mm:field name="mood" /></mm:import>
  <mm:import id="threadtype"><mm:field name="type" /></mm:import>
</mm:node>

<%--Check if the poster is an moderator --%>
<mm:nodefunction set="mmbob" name="getPostAreaInfo" referids="forumid,postareaid,posterid,page">
  <mm:import id="ismoderator"><mm:field name="ismoderator" /></mm:import>
</mm:nodefunction>

<%-- reset the threadstate if the poster is a moderator --%>
<mm:compare referid="ismoderator" value="true">
  <mm:import reset="true" id="threadstate">normal</mm:import>
</mm:compare>

<mm:locale language="$lang">
<%@ include file="loadtranslations.jsp" %>

<div class="header">
  <%@ include file="header.jsp" %>
</div>
                                                                                                              
<div class="bodypart">

<mm:include page="path.jsp?type=postthread" />
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="95%">
                        <tr><th colspan="2" align="left">
                                        <mm:compare referid="image_logo" value="" inverse="true">
                                        <center><img src="<mm:write referid="image_logo" />" width="100%" ></center>
                                        </mm:compare>
			</th>
			</tr>
</table>
<table cellpadding="0" cellspacing="0" style="margin-top : 10px;" width="95%">
	<tr><td align="left"><b><mm:write referid="mlg_Pages"/>
   	 	  <mm:nodefunction set="mmbob" name="getPostThreadNavigation" referids="forumid,postareaid,postthreadid,page">
			(<mm:field name="pagecount" />) 
			<mm:field name="navline" />
			<mm:import id="lastpage"><mm:field name="lastpage" /></mm:import>
		  </mm:nodefunction>
	  </b>
	</td></tr>
</table>

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 5px;" width="95%">
  		  <mm:nodelistfunction set="mmbob" name="getPostings" referids="forumid,postareaid,postthreadid,posterid,page">
		  <mm:first>
			<tr><th width="25%" align="left"><mm:write referid="mlg_Member"/></th><th align="left"><mm:write referid="mlg_Topic"/>: <mm:field name="subject" /></th></tr>
		  </mm:first>
		  <mm:remove referid="tdvar" />
		  <mm:even><mm:import id="tdvar"></mm:import></mm:even>
		  <mm:odd><mm:import id="tdvar">listpaging</mm:import></mm:odd>
			<tr>
			<td class="<mm:write referid="tdvar" />" align="left">
			<mm:field name="posttime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field>
			</td>
			<td class="<mm:write referid="tdvar" />" align="right">
			<mm:remove referid="postingid" />
			<mm:remove referid="toid" />
			<mm:import id="toid"><mm:field name="posterid" /></mm:import>
			<mm:import id="postingid"><mm:field name="id" /></mm:import>
                                
                           <a href="<mm:url page="newprivatemessage.jsp" referids="forumid,postareaid,postthreadid,postingid,toid" />"><img src="<mm:write referid="image_privatemsg" />"  border="0" /></a>
                           <mm:compare referid="threadstate" value="closed" inverse="true">
                              <a href="<mm:url page="posting.jsp" referids="forumid,postareaid,postthreadid,posterid,postingid" />"><img src="<mm:write referid="image_quotemsg" />"  border="0" /></a>
                           </mm:compare>
		     
			   <mm:field name="ismoderator">
				<mm:compare value="true">
  				<a href="<mm:url page="editpost.jsp">
				<mm:param name="forumid" value="$forumid" />
				<mm:param name="postareaid" value="$postareaid" />
				<mm:param name="postthreadid" value="$postthreadid" />
				<mm:param name="postingid" value="$postingid" />
				</mm:url>"><img src="<mm:write referid="image_medit" />"  border="0" /></a>

  				<a href="<mm:url page="removepost.jsp">
				<mm:param name="forumid" value="$forumid" />
				<mm:param name="postareaid" value="$postareaid" />
				<mm:param name="postthreadid" value="$postthreadid" />
				<mm:param name="postingid" value="$postingid" />
				</mm:url>"><img src="<mm:write referid="image_mdelete" />"  border="0" /></a>

				</mm:compare>
			</mm:field>
			&nbsp;
                        <mm:compare referid="threadstate" value="closed" inverse="true">
			  <mm:field name="isowner">
				<mm:compare value="true">
				<mm:remove referid="postingid" />
				<mm:import id="postingid"><mm:field name="id" /></mm:import>
  				<a href="<mm:url page="editpost.jsp">
				<mm:param name="forumid" value="$forumid" />
				<mm:param name="postareaid" value="$postareaid" />
				<mm:param name="postthreadid" value="$postthreadid" />
				<mm:param name="postingid" value="$postingid" />
				</mm:url>"><img src="<mm:write referid="image_editmsg" />"  border="0" /></a>
				</mm:compare>
			</mm:field>
                        </mm:compare>

			</td>
			</tr>
			<tr>
			<td class="<mm:write referid="tdvar" />" valign="top" align="left">
                        <p>
                        <mm:field name="guest">
                        <mm:compare value="true">
				<b><mm:field name="poster" /></b>
                        </mm:compare>

			<mm:compare value="true" inverse="true">
			
                            <b><a href="profile.jsp?forumid=<mm:write referid="forumid" />&postareaid=<mm:write referid="postareaid" />&type=poster_thread&posterid=<mm:field name="posterid" />&postthreadid=<mm:write referid="postthreadid" />"><mm:field name="poster" /></b>  (<mm:field name="firstname" /> <mm:field name="lastname" />)<br />
                            <mm:field name="avatar">
                              <mm:compare value="-1" inverse="true">
                                <mm:node number="$_">
                                  <img src="<mm:image template="s(80x80)" />" width="80" border="0">
                                </mm:node>
                              </mm:compare>
                            </mm:field>
                        </a>
                        <p />

			<mm:write referid="mlg_Level"/> : <mm:field name="level" /><br />
			<mm:write referid="mlg_Posts"/> : <mm:field name="accountpostcount" /><br />
			<mm:write referid="mlg_Gender"/> : <mm:field name="gender" /><br />
			<mm:write referid="mlg_Location"/> : <mm:field name="location" /><br />
			<mm:write referid="mlg_Member_since"/> : <mm:field name="firstlogin"><mm:time format="d MMMM  yyyy" /></mm:field><br />
			<mm:write referid="mlg_Last_visit"/> : <mm:field name="lastseen"><mm:time format="d/MM/yy HH:mm" /> </mm:field><br />
			</mm:compare>
			</mm:field>
			<br /><br /><br /><br /><br />
                        </p>
			</td>
			<td class="<mm:write referid="tdvar" />" valign="top" align="left">
			<mm:field name="edittime"><mm:compare value="-1" inverse="true"><mm:write referid="mlg_last_time_edited"/> : <mm:field name="edittime"><mm:time format="MMMM d, yyyy, HH:mm:ss" /></mm:field></mm:compare><p /></mm:field>
           
            <mm:node referid="postingid">

            <mm:even> 
              <mm:formatter xslt="xslt/posting2xhtmlDark.xslt">
                <mm:function referids="imagecontext,themeid" name="escapesmilies">
                <mm:write/>
                </mm:function>
              </mm:formatter>
            </mm:even>
            <mm:odd>
               <mm:formatter xslt="xslt/posting2xhtmlLight.xslt">
                <mm:function referids="imagecontext,themeid" name="escapesmilies">
                <mm:write/>
                </mm:function>
              </mm:formatter>
            </mm:odd> 

            </mm:node>

			<br /><br /><br /><br /><br />
			</td>
			</tr>
		  </mm:nodelistfunction>
</table>


<table cellpadding="0" cellspacing="0" style="margin-top : 2px;" width="95%">
	<tr><td align="left"><b><mm:write referid="mlg_Pages"/>
   	 	  <mm:nodefunction set="mmbob" name="getPostThreadNavigation" referids="forumid,postareaid,postthreadid,page">
			<mm:field name="navline" />
		  </mm:nodefunction>
	  </b>
	</td></tr>
</table>


<mm:compare referid="lastpage" value="true">
<mm:compare referid="threadstate" value="closed" inverse="true">

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 10px;" width="85%">
   <a name="reply" />
  <tr><th colspan="3"><mm:write referid="mlg_Fast_reply"/></th></tr>
  <form action="<mm:url page="thread.jsp" referids="forumid,postareaid,postthreadid,page" />#reply" method="post" name="posting">
	<tr><th width="25%"><mm:write referid="mlg_Name"/></th><td>

		<mm:compare referid="posterid" value="-1" inverse="true">
		<mm:node number="$posterid">
		<mm:field name="account" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)
		<input name="poster" type="hidden" value="<mm:field name="account" />" >
		</mm:node>
		</mm:compare>
		<mm:compare referid="posterid" value="-1">
		<input name="poster" style="width: 100%" value="gast" >
		</mm:compare>

		</td></tr>
	<tr><th><mm:write referid="mlg_Reply"/> <center><table width="100"><tr><th><%@ include file="includes/smilies.jsp" %></th></tr></table></center> </th><td><textarea name="body" rows="5" style="width: 100%"></textarea></td></tr>
	<tr><td colspan="3"><input type="hidden" name="action" value="postreply">
	<center><input type="submit" value="<mm:write referid="mlg_Post_reply"/>"/></center>
	</td></tr>
  </form>
</table>
</mm:compare>
</mm:compare>
</div>

<div class="footer">
  <%@ include file="footer.jsp" %>
</div>

</mm:locale>

</body>
</html>
</mm:content>
</mm:cloud>
