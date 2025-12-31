<%@ include file="inc/top.jsp" %>
<mm:content>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nl">
<head>
<mm:import id="pagetitle">Login</mm:import>
<%@ include file="inc/head.jsp" %>
  <link rel="stylesheet" href="<mm:url absolute="server" page="css/login.css" />" type="text/css" />
</head>
<body class="login" onload="document.loginbox.username.focus()">

<mm:import externid="username" from="parameters" />
<mm:import externid="reason">please</mm:import>
<mm:import externid="referrer">index.jsp</mm:import>

<form id="loginbox" name="loginbox" method="post" action="<mm:url page="$referrer" />">
<mm:compare referid="reason" value="failed">
  <div class="message">You failed to log in. Try again.</div>
</mm:compare>
<fieldset>
<input type="hidden" name="command" value="login" />
<input type="hidden" name="authenticate" value="name/password" />
  <div class="first">
    <h2>my_editors</h2>
    <p>Generic editors for MMBase</p>
    <h4>Please login</h4>
  </div>
  <div class="row">
    <label for="username">Name</label>
    <input type="text" id="username" name="username" />
  </div>
  <div class="row">
    <label for="password">Password</label>
    <input type="password" id="password" name="password" autocomplete="off" />
  </div>
  <div class="lastrow">
    <input type="submit" name="Login" value="login" />
  </div>
</fieldset>
</form>

</body>
</html>
</mm:content>
