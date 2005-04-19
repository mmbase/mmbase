<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud>
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<%@ include file="thememanager/loadvars.jsp" %>

<mm:import externid="forumid" />
<mm:import externid="postareaid" />

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
<mm:import externid="disableposterid"/>
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr>
    <th colspan="3" align="center" >
      <mm:write referid="mlg.Disable" /> <mm:write referid="mlg.member"/> 
    </th>
  </tr>

  <form action="<mm:url page="index.jsp">
					<mm:param name="forumid" value="$forumid" />
                                        <mm:param name="disableposterid" value="$disableposterid"/>
					<mm:param name="admincheck" value="true" />
				</mm:url>" method="post">
  <tr>
    <td colspan="2" align="center">
      <mm:write referid="mlg.Disable" /> <mm:write referid="mlg.member"/> : <mm:node number="$disableposterid"><mm:field name="account" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)</mm:node>
    </td>
  </tr>

  <tr>
    <td colspan="2" align="center"><mm:write referid="mlg.Are_you_sure" />?</td>
  </tr>

  <input type="hidden" name="admincheck" value="true">
  <input type="hidden" name="action" value="disableposter">
  
  
  <tr>
    <td align="center" ><input type="submit" value="<mm:write referid="mlg.Disable" />"> </td>
    <td align="center">
      </form> 
      <form action="<mm:url page="index.jsp">
	<mm:param name="forumid" value="$forumid" />
	</mm:url>"
 	method="post">
      <p />
      <input type="submit" value="<mm:write referid="mlg.Cancel" />">
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

