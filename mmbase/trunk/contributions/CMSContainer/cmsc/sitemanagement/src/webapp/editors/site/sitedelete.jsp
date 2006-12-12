<%@page language="java" contentType="text/html;charset=utf-8"%>
<%@include file="globals.jsp" %>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<head>
<link href="../css/main.css" type="text/css" rel="stylesheet" />
<title><fmt:message key="sitedelete.title" /></title>
<style type="text/css">
input { width: 100px;}
</style>
</head>
<mm:import externid="number" required="true" from="parameters"/>
<mm:cloud jspvar="cloud" rank="administrator" loginpage="../login.jsp">
<body>

<div class="side_block_green">
	<!-- bovenste balkje -->
	<div class="header">
		<div class="title"><fmt:message key="sitedelete.title" /></div>
		<div class="header_end"></div>
	</div>
	
	<div class="body"><p>
		<fmt:message key="sitedelete.subtitle" /> <mm:node referid="number"><b><mm:field name="title"/></b></mm:node>
		<br/>
		<br/>
		<fmt:message key="sitedelete.confirm" />
		<br/>
		<form action="?">
	   	<input type="hidden" name="number" value="<mm:write referid="number"/>" />
	   	<input type="submit" name="remove" value="<fmt:message key="sitedelete.yes" />"/>&nbsp;
        <input type="submit" name="cancel" value="<fmt:message key="sitedelete.no" />"/>
		</form>
	</p></div>
	<div class="side_block_end"></div>
</div>
</body>
</mm:cloud>
</html:html>
</mm:content>