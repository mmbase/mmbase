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
    <%@ include file="header.jsp" %>
</div>
                                                                                              
<div class="bodypart">
<mm:import externid="enableposterid"/>
<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 50px;" width="75%">
  <tr>
    <th colspan="3" align="center" >
      <mm:write referid="mlg_Enable" /> <mm:write referid="mlg_member"/> 
    </th>
  </tr>

  <form action="<mm:url page="index.jsp">
					<mm:param name="forumid" value="$forumid" />
                                        <mm:param name="enableposterid" value="$enableposterid"/>
					<mm:param name="admincheck" value="true" />
				</mm:url>" method="post">
  <tr>
    <td colspan="2" align="center">
      <mm:write referid="mlg_Enable" /> <mm:write referid="mlg_member"/> : <mm:node number="$enableposterid"><mm:field name="account" /> (<mm:field name="firstname" /> <mm:field name="lastname" />)</mm:node>
    </td>
  </tr>

  <tr>
    <td colspan="2" align="center"><mm:write referid="mlg_Are_you_sure" />?</td>
  </tr>

  <input type="hidden" name="admincheck" value="true">
  <input type="hidden" name="action" value="enableposter">
  
  
  <tr>
    <td align="center" ><input type="submit" value="<mm:write referid="mlg_Enable" />"> </td>
    <td>
      </form> 
      <form action="<mm:url page="index.jsp">
	<mm:param name="forumid" value="$forumid" />
	</mm:url>"
 	method="post">
      <p />
      <center>
      <input type="submit" value="<mm:write referid="mlg_Cancel" />">
      </form>
    </td>
  </tr>
</table>
</div>

<div class="footer">
  <%@ include file="footer.jsp" %>
</div>
                                                                                              
</body>
</html>

</mm:locale>
</mm:content>
</mm:cloud>

