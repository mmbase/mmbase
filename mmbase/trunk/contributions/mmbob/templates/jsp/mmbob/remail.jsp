<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:import id="dac"><mm:function set="mmbob" name="getDefaultAccount" /></mm:import>
<mm:import id="dpw"><mm:function set="mmbob" name="getDefaultPassword" /></mm:import>
</mm:cloud>
<mm:cloud sessionname="forum" username="$dac" password="$dpw">
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">

<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="forumid" />

<!-- login part -->
<%@ include file="getposterid.jsp" %>
<!-- end login part -->
                                                                                                                    
<mm:locale language="$lang">
<%@ include file="loadtranslations.jsp" %>

<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
<mm:compare value="remail" referid="action">
	<mm:import externid="wantedaccount" />
	<mm:node referid="forumid">
		<mm:import id="wforum"><mm:field name="name" /></mm:import>
       		<mm:relatednodes type="posters" constraints="(account='$wantedaccount')" max="1">
		<mm:import id="wemail"><mm:field name="email" /></mm:import>
		<mm:import id="posterid" reset="true"><mm:field name="number" /></mm:import>
		<mm:import id="email"><mm:field name="email" /></mm:import>

		<mm:import id="waccount"><mm:field name="account" /></mm:import>
                
                <mm:import id="firstname"><mm:field name="firstname" /></mm:import>
                <mm:import id="lastname"><mm:field name="lastname" /></mm:import>
                <mm:import id="gender"><mm:field name="gender" /></mm:import>
                <mm:import id="location"><mm:field name="location" /></mm:import>
                <% String newPassword =
                Integer.toHexString(
                    (int)(Math.random() * 0xfffffff)); %>
		<mm:import id="wpassword"><%=newPassword %></mm:import>
                <mm:import id="newpassword"><mm:write referid="wpassword"/></mm:import>
               <mm:import id="newconfirmpassword"><mm:write referid="wpassword"/></mm:import>
                <!--  create the email node -->
                <mm:createnode id="mail1" type="email">
                        <mm:setfield name="from">type.here@your.emailaddress.com</mm:setfield>
                        <mm:setfield name="to"><mm:write referid="wemail" /></mm:setfield>
                        <mm:setfield name="subject"><mm:write referid="mlg_Your_account_information_for_the_MMBob_forum"/></mm:setfield>
                        <mm:setfield name="body"> <mm:write referid="mlg_Your_account_information_for_the_MMBob_forum"/>: <mm:write referid="wforum" /> :


			<mm:write referid="mlg_login_name" />=<mm:write referid="waccount" />
			<mm:write referid="mlg_password" />=<mm:write referid="wpassword" />
			</mm:setfield>
                </mm:createnode>


                <!-- send the email node -->                    <mm:node referid="mail1">
                        <mm:field name="mail(oneshot)" />
                </mm:node>
		<mm:import id="mailed">true</mm:import>
                </mm:relatednodes>
	</mm:node>
 
        <%-- setting new password into poster --%>
	<mm:import id="feedback"><mm:function set="mmbob" name="editPoster" referids="forumid,posterid,firstname,lastname,email,gender,location,newpassword,newconfirmpassword"/></mm:import>






</mm:compare>
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

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="40%" align="center">
<mm:present referid="mailed">
<form action="<mm:url page="index.jsp" referids="forumid" />" method="post">
<tr><th align="left" ><p />
<mm:write referid="mlg_Login_mail_sent_to" /> : <mm:write referid="wemail" />, <br />
<%--<mm:write referid="wpassword" /> - <mm:write referid="feedback"/>--%>
</th></tr>
<tr><td>
<center>
<input type="submit" value="Terug naar het forum">
</form>
</td></tr>
</mm:present>
<mm:notpresent referid="mailed">
<form action="<mm:url page="remail.jsp" referids="forumid" />" method="post">
<tr><th colspan="3" align="left" >
<mm:present referid="action">
<p />
<center><mm:write referid="mlg_Login_name_not_found" /></center>
</mm:present>
<p />
<mm:write referid="mlg_Please_enter_your_login_name" /><p />
<mm:write referid="mlg_login_name"/> : <input name="wantedaccount" size="15">
</th></tr>
<tr><td>
<input type="hidden" name="action" value="remail">
<center>
<input type="submit" value="<mm:write referid="mlg_Ok"/>, <mm:write referid="mlg_send"/>">
</form>
</td>
<td>
<form action="<mm:url page="remail.jsp" referids="forumid" />" method="post">
<p />
<center>
<input type="submit" value="<mm:write referid="mlg_Cancel"/>">
</form>
</td>
</tr>
</mm:notpresent>

</table>

</div>

<div class="footer">

</div>
                                                                                              
</body>
</html>

</mm:locale>
</mm:content>
</mm:cloud>

