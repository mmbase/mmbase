<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page language="java" contentType="text/html;charset=utf-8" session="false"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><mm:content type="text/html">
<html>
  <head>
    <title>Welcome to MMBase</title>
    <link rel="stylesheet" href="mmbase/style/css/mmbase.css" type="text/css" />
    <link rel="icon" href="<mm:url page="/mmbase/style/images/favicon.ico" />" type="image/x-icon" />
    <link rel="shortcut icon" href="<mm:url page="/mmbase/style/images/favicon.ico" />" type="image/x-icon" />

  </head>
<body >
<table>
  <tr>
    <th class="main" colspan="3">Welcome to MMBase</th>
  </tr>
  <tr>
    <td colspan="3">
      <p>
        You are running <a href="<mm:url page="/version" />"><%=org.mmbase.Version.get() %></a>
      </p>
      <p>
        This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the Open Source Initiative.
      </p>
      <p>
        MMBase has been build under the <a href="mmbase/mpl-1.0.jsp">Mozilla Public License, Version 1.0</a>
      </p>
      <p>
        <b>IMPORTANT:</b> Several pages in the Administrative console require you to enter a username and password. <br />
        Please use the following credentials: <br />
        <b>Username:</b> <tt>admin</tt><br />
        <b>Password:</b> <tt>admin2k</tt><br />
        We strongly advise you to change this administrator password as soon as possible, read the documentation for more details.
      </p>  
      <p>
        Consicely, for further upgrading this demo-installation of MMBase to a producation environment we advice to do the following
        <ul>
          <li>Evaluate all builder xmls from config/builders (and below). Remove all which you don't
          need (the builders in 'core' should remain). You need to start with empty database then.</li>
          <li>Evaluate all applications from config/applications. Remove all which you don't need.</li>
          <li>Configure another database (hsql is not really fit for production), e.g. mysql or postgresql</li>
          <li>Remove /mmexamples (it is a bit dangerous, and superfluous in production)</li>
          <li>Remove /index.jsp (this file), you want to put the index.jsp of your site here of course.</li>
          <li>Remove /mmdocs</li>
        </ul>
      </p>
    </td>
  </tr>
  <tr>
    <th>Section</th>
    <th colspan="2">Description</th>
  </tr>  
  
  <tr>
    <td>MMBase Demos</td>
    <td>
      <p>
        Examples of MMBase functionality. Includes among other things a news application, an alternate editor,
        and the MMBase editwizards.
      </p>
    </td>
    <td class="link" >
      <a href="<mm:url page="mmexamples/" />"><img alt="&gt;" src="mmbase/style/images/next.gif"  /></a>
    </td>
  </tr>

  <tr>
    <td>Manage your MMBase installation</td>
    <td>
      <p>
        Your MMBase installation comes with a configuration panel that allows you to perform certain
        administrative tasks, and has links to several editors that allow you to edit information stored
        in your MMBase instance.
      </p>
    </td>
    <td class="link">
      <a href="<mm:url page="mmbase/admin/" />"><img alt="&gt;" src="mmbase/style/images/next.gif"  /></a>
    </td>
  </tr>  
  
  <tr>
    <td>MMBase Documentation</td>
    <td>
      An overview of available MMBase documentation, listed by target group.<br />
    </td>
    <td class="link" >
      <a href="<mm:url page="mmdocs/" />"><img alt="&gt;" src="mmbase/style/images/next.gif" /></a>
    </td>
  </tr>
  
  <tr>
    <td>www.mmbase.org</td>
    <td>
      Link to the MMBase website.
    </td>
    <td class="link" >
      <a href="http://www.mmbase.org"><img alt="&gt;" src="mmbase/style/images/next.gif" /></a>
    </td>
  </tr> 
</table>
</body>
</html>
</mm:content>
