<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html;charset=utf-8" language="java" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><html>
<head>
   <title>MMBase Bugtracker</title>
   <link rel="stylesheet" type="text/css" href="css/mmbase.css" />
</head>
<mm:cloud>
<mm:import externid="sbugid" jspvar="sbugid" />
<mm:import externid="sissue" jspvar="sissue" />
<mm:import externid="sstatus" jspvar="sstatus" />
<mm:import externid="stype" jspvar="stype" />
<mm:import externid="sarea" jspvar="sarea" />
<mm:import externid="sversion" jspvar="sversion" />
<mm:import externid="spriority" jspvar="spriority" />
<mm:import externid="offset" jspvar="offset">0</mm:import>
<mm:import externid="where" jspvar="where" />
<mm:import externid="flap">search</mm:import>

<mm:import externid="cw" from="cookie" />
<mm:import externid="ca" from="cookie" />
<mm:present referid="ca">
   <mm:present referid="cw">
			<mm:listnodes type="users" constraints="account='$ca' and password='$cw'" max="1">
			 	<mm:import id="user"><mm:field name="number" /></mm:import>
			</mm:listnodes>
   </mm:present>
</mm:present>


<body class="basic"> 
<%-- first the selection part --%>

<table><%-- a table only for a header? --%>
<form action="index.jsp" method="POST">
<tr>

		<td width="50"><img src="images/trans.gif" width="50" height="1"></td> 
		<td>
		 <center>BugTracker 1.2imnotfinishedyet - Daniel Ockeloen
		</td>
</tr>
</table>

<%@ include file="mainparts/flaps_index.jsp" %>


<mm:compare referid="flap" value="search">
   <%@ include file="mainparts/search.jsp" %>
</mm:compare>
<mm:compare referid="flap" value="lastchanges">
   <%@ include file="mainparts/lastchanges.jsp" %>
</mm:compare>
<mm:compare referid="flap" value="stats">
   <%@ include file="mainparts/statistics.jsp" %>
</mm:compare>
<mm:compare referid="flap" value="mysettings">
   <%@ include file="mainparts/mysettings.jsp" %>
</mm:compare>
<mm:compare referid="flap" value="mybug">
   <%@ include file="mainparts/mybug.jsp" %>
</mm:compare>


</mm:cloud>
</body>
</html>
