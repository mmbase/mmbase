<mm:import externid="max_str" jspvar="max_str" vartype="String">10</mm:import>
<mm:import externid="o" jspvar="ofs_str" vartype="String">0</mm:import>
<mm:import externid="dayofs" jspvar="dayofs" vartype="String">9</mm:import>
<% // Set and get some values
int max = Integer.parseInt(max_str);
int ofs = Integer.parseInt(ofs_str);
%>

<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
	<link rel="stylesheet" href="my_editors.css" type="text/css" />
	<title>my_editors - <%= title %></title>
</head>
<body bgcolor="#FFFFFF">
<table width="100%" class="top-table" border="0" cellspacing="0" cellpadding="3">
  <tr>
    <td width="50"><a href="index.jsp"><img src="img/mmbase-edit-40.gif" alt="my_editors" width="41" height="40" border="0" hspace="4" vspace="4" /></a></td>
    <td>
	  <div class="top-title">my_editors - <%= title %></div>
	  <div class="top-links"><a class="top-links" href="index.jsp">home</a> - 
	  <a class="top-links" href="help.jsp">help</a> -
	  <a class="top-links" href="config.jsp">configure</a> -
	  logged on as:  <%= wolk.getUser().getIdentifier() %> (rank: <%= wolk.getUser().getRank() %>) - 
	  <a href="logout.jsp">log out</a> </div>
	</td>
  </tr><tr bgcolor="#CCCCCC"> 
    <td colspan="2"><div class="top-links">
	  my_editors 
	  <% if (path1 != null) { %>&gt; <a class="top-links" href="index.jsp?type=<%= path1 %>">overview <%= path1 %></a> <% } %>
	</div></td>
  </tr>
</table>
<div style="margin-left: 5px; margin-right: 5px; margin-top: 5px; margin-bottom: 5px;" align="center">
