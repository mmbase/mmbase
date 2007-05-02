<%@page language="java" contentType="text/html;charset=UTF-8"%>
<%@include file="../../globals.jsp"%>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<head>
	<link href="../../css/compact.css" type="text/css" rel="stylesheet" />
</head>

<body>
<mm:cloud jspvar="cloud" loginpage="../../login.jsp">
	<mm:hasrank minvalue="administrator">
	
		<h1><fmt:message key="admindashboard.system.memory.header" /></h1>
		
		<b><fmt:message key="admindashboard.system.memory.maximum" />:</b> 
		<%=Runtime.getRuntime().maxMemory()/1024/1024%><fmt:message key="admindashboard.system.memory.mb" />
		<br/>
		<b><fmt:message key="admindashboard.system.memory.total" />:</b> 
		<%=Runtime.getRuntime().totalMemory()/1024/1024%><fmt:message key="admindashboard.system.memory.mb" />
		<br/>
		<b><fmt:message key="admindashboard.system.memory.free" />:</b> 
		<%=Runtime.getRuntime().freeMemory()/1024/1024%><fmt:message key="admindashboard.system.memory.mb" />
		
	</mm:hasrank>
</mm:cloud>
</body>
</html:html>
</mm:content>
