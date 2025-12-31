<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
%><%@include file="config/read.jsp"
%><mm:content language="$config.lang" type="text/html" expires="0">
<html>
  <link href="style/streammanager.css" type="text/css" rel="stylesheet" />
  <title>Login</title>
</head>
<body class="left">
  <mm:cloud jspvar="cloud" method="asis">
  <mm:import id="current">login</mm:import>
  <%@include file="submenu.jsp" %>
  <hr />
  <span class="kop">Login</span>
  <mm:import externid="reason">please</mm:import>
  <mm:import externid="referrer">search.jsp</mm:import>
  <mm:compare referid="reason" value="failed">
    <p class="failed">
      Failed to log in. Try again.
    </p>
  </mm:compare>
  <table>
    <form method="post" action="dologin.jsp" >
      <tr><td>Name:</td><td><input type="text" name="username"></td></tr>
      <tr><td>Password</td><td><input type="password" name="password" autocomplete="off"></td></tr>
      <!-- tr><td>Authenticate:</td><td><input type="text" name="authenticate" value="name/password"></td></tr-->
      <tr><td /><td><input type="submit" name="command" value="login"></td></tr>
    </form>
  </table>
  </mm:cloud>
</body>
</html>
</mm:content>
