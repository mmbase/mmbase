<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
   <%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
  </head>
  <body>
    <h1>Cloud method='asis' attribute</h1>
    <p>
      Of course, 'asis' must port over pages.
      <mm:cloud method="asis" jspvar="cloud">
        You are logged in as: <%=cloud.getUser().getIdentifier() %>
        (must not be anonymous, but 'foo')
      </mm:cloud>
    </p>
    <p>
      Logging out for next page: <mm:cloud method="logout" />
    </p>
  <hr />
  <a href="cloud3.jsp">Previous</a><br />
  <a href="cloud5.jsp">Next</a><br />
    <a href="<mm:url page="/mmexamples/taglib/showanypage.jsp"><mm:param name="page"><%=request.getServletPath()%></mm:param></mm:url>">Source of this page</a><br />
  <hr />
  </body>
</html>
