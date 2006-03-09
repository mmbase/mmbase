<%@page session="false" %><% response.setStatus(503); 
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<html>
  <head>
    <title>503 temporary unavailable</title>
    <% String url = (String) request.getAttribute("javax.servlet.forward.request_uri"); %>
    <% response.setHeader("Refresh", "30; url=" + url);%>
    <%@include file="meta.jsp"%>
  </head>
  <body class="basic">
    <h1>503 This web-site is temporary unavailable</h1>
    <h2><%=org.mmbase.Version.get()%></h2>
    <p>
      <em><%=new java.util.Date()%></em> - This web-site is currently unavailable. Please come <a href="<%=url%>">back</a> in a few minutes.
    </p>
    <p>
      If you are the administrator of this site, and this message does not disappear, please check
      the mmbase log, which probably contains an indication of the reason why MMBase is not yet
      successfully started.
    </p>
  </body>
</html>

