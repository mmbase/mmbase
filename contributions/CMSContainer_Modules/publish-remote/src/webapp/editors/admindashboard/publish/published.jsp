<%@page language="java" contentType="text/html;charset=UTF-8"%>
<%@include file="../../publish-remote/globals.jsp"%>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<head>
	<link href="../../css/compact.css" type="text/css" rel="stylesheet" />
</head>

<body>
<mm:cloud jspvar="cloud" loginpage="../../login.jsp">
	<mm:hasrank minvalue="administrator">
	
		<h1><fmt:message key="admindashboard.publish.published.header" /></h1>
		
		<b><fmt:message key="admindashboard.publish.published.lasthour" />:</b> 
 		<mm:listnodescontainer type="publishqueue">
 			<%
 			java.util.Calendar calendar = java.util.GregorianCalendar.getInstance();
 			calendar.add(java.util.Calendar.HOUR, -1);
 			%>
			<mm:constraint field="status" operator="EQUAL" value="done"/>
			<mm:constraint field="timestamp" operator="GREATER_EQUAL" value='<%=""+calendar.getTimeInMillis()/1000%>'/>
			<mm:size/>
		</mm:listnodescontainer>
		<br/>

		<b><fmt:message key="admindashboard.publish.published.lastday" />:</b> 
 		<mm:listnodescontainer type="publishqueue">
 			<%
 			java.util.Calendar calendar = java.util.GregorianCalendar.getInstance();
 			calendar.add(java.util.Calendar.DAY_OF_MONTH, -1);
 			%>
			<mm:constraint field="status" operator="EQUAL" value="done"/>
			<mm:constraint field="timestamp" operator="GREATER_EQUAL" value='<%=""+calendar.getTimeInMillis()/1000%>'/>
			<mm:size/>
		</mm:listnodescontainer>
		<br/>
	
		<b><fmt:message key="admindashboard.publish.published.lastweek" />:</b> 
 		<mm:listnodescontainer type="publishqueue">
 			<%
 			java.util.Calendar calendar = java.util.GregorianCalendar.getInstance();
 			calendar.add(java.util.Calendar.DAY_OF_MONTH, -7);
 			%>
			<mm:constraint field="status" operator="EQUAL" value="done"/>
			<mm:constraint field="timestamp" operator="GREATER_EQUAL" value='<%=""+calendar.getTimeInMillis()/1000%>'/>
			<mm:size/>
		</mm:listnodescontainer>
		<br/>
	
		
	</mm:hasrank>
</mm:cloud>
</body>
</html:html>
</mm:content>
