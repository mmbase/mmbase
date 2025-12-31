<%@ page language="java" contentType="text/html" session="false"
    import="org.mmbase.security.AuthenticationData,org.mmbase.bridge.ContextProvider"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
  "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<mm:content expires="0" escaper="none">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="nl">
<head>
  <title>MMBase Administration- Login</title>
  <mm:link page="css/login.css">
    <link rel="stylesheet" href="${_}" type="text/css" />
  </mm:link>
  <mm:link page="/mmbase/style/images/favicon.ico">
    <link rel="icon" href="${_}" type="image/x-icon" />
    <link rel="shortcut icon" href="${_}" type="image/x-icon" />
  </mm:link>
</head>
<body class="login">

<mm:import externid="username" from="parameters" />
<mm:import externid="reason">please</mm:import>
<mm:import externid="referrer">index.jsp</mm:import>

<mm:import externid="currentType" jspvar="currentType">name/password</mm:import>
<%
  AuthenticationData authentication = ContextProvider.getDefaultCloudContext().getAuthentication();
  String[] authenticationTypes = authentication.getTypes(authentication.getDefaultMethod(request.getProtocol()));
%>

<div id="wrap">
<form id="loginbox" method="post" action="<mm:url page="$referrer" />">
<mm:compare referid="reason" value="failed">
  <div class="message">You failed to log in. Try again.</div>
</mm:compare>
<fieldset>
<input type="hidden" name="command" value="login" />
<%-- input type="hidden" name="authenticate" value="name/password" / --%>
  <div class="row">
    <label><img src="<mm:url page="/mmbase/style/logo.png" />" alt="MMBase logo" width="40" height="50" /></label>
    <h2>MMBase Administration</h2>
    <h3>Please login</h3>
  </div>
  <div class="row">
    <label for="username">Name</label>
    <input type="text" id="username" name="username" />
  </div>
  <div class="row">
    <label for="password">Password</label>
    <input type="password" id="password" name="password" autocomplete="off" />
  </div>
  <div class="row">
    <label for="authenticate">Type</label>
    <select name="authenticate" id="authenticate">
      <% for (int i = 0 ; i < authenticationTypes.length; i++) { %>
      <option value="<%= authenticationTypes[i] %>" <%= currentType.equals(authenticationTypes[i])? " selected='selected'" : ""%>><%= authenticationTypes[i] %></option>
      <% } %>
      <input type="hidden" name="referrer" value="<mm:write referid="referrer" />" />
    </select>
  </div>
  <div class="lastrow">
    <input type="submit" name="Login" value="login" />
  </div>
</fieldset>
</form>
</div>

</body>
</html>
</mm:content>
