<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><%@page language="java" contentType="text/html; charset=utf-8"
%><mm:content expires="0" type="text/html">
<mm:cloud>
<html>
  <head>
    <title>MMBase Bugtracker example</title>
    <link rel="stylesheet" href="<mm:url page="/mmbase/style/css/mmbase.css" />" type="text/css" />
    <link rel="icon" href="<mm:url page="/mmbase/style/images/favicon.ico" />" type="image/x-icon" />
    <link rel="shortcut icon" href="<mm:url page="/mmbase/style/images/favicon.ico" />" type="image/x-icon" />
  </head>
  <body>
    <mm:import externid="template">main.jsp</mm:import>
    
    <%@include file="login.jsp" %>

    <mm:include debug="html" page="$template" />      

  </body>
</html>
</mm:cloud>
</mm:content>
