<%@ page session="false" %><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
   <%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
  </head>
 
    <h1>Cloud method='asis' but session disabled (does this make sense?)</h1>
    <p>
      <mm:cloud method="asis" jspvar="cloud">
        You are logged in as: <%=cloud.getUser().getIdentifier() %>
        (must be anonymous?)
      </mm:cloud>
    </p>
  <hr />
  <a href="cloud5.jsp">Previous</a><br />
  <a href="index.jsp">back</a><br />
    <a href="<mm:url page="/mmexamples/taglib/showanypage.jsp"><mm:param name="page"><%=request.getServletPath()%></mm:param></mm:url>">Source of this page</a><br />
  <hr />
  </body>
</html>
