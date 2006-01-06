<%@page session="false" 
%><%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"  prefix="mm"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<html>
  <head>
    <title>404 The requested resource is unavailable</title>
    <%@include file="meta.jsp" %>
  </head>
  <body class="basic">
    <h1>404 The requested resource is unavailable</h1>
    <h2><%=org.mmbase.Version.get()%></h2>
    <p>
      The current URL (<%=request.getAttribute("javax.servlet.forward.request_uri")%>) does not point to an existing resource in this web-application.
    </p>
  </body>
</html>

