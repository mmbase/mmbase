<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@page import="org.mmbase.bridge.*" %>
<mm:cloud name="mmbase">
<% String config = request.getParameter("config");
   String target = request.getParameter("target");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<html xmlns="http://www.w3.org/TR/xhtml">
<head>
<title>Configuration File of <%=config%>/<%=target%></title>
<link rel="stylesheet" type="text/css" href="../../css/mmbase.css" />
<meta http-equiv="pragma" value="no-cache" />
<meta http-equiv="expires" value="0" />
</head>
<body class="basic" >
<% String todo = request.getParameter("todo");
   Module configmod=LocalContext.getCloudContext().getModule("config");
%>
<%=configmod.getInfo(todo.toUpperCase()+"-"+config+"-"+target)%>
</body></html>
</mm:cloud>
