<%@ include file="page_base.jsp"
%><mm:cloud sessionname="$config.session" method="asis" jspvar="cloud">
<mm:write referid="style" />
 <title>About generic mmbase taglib editors</title>
</head>
<body class="basic">
<table summary="taglib editors">
<tr><th>The MMBase taglib editors</th></tr>
<tr><td class="data">
<p>
These are the <a href="http://www.mmbase.org/" target="_blank">MMBase</a> generic editors, based on 
<a href="http://www.mmbase.org/mmbasenew/index3.shtml?development+452+3747+projects" target="_blank">
MMCI</a> (version 1.2) with usage of 
<a href="http://www.mmbase.org/mmbasenew/index3.shtml?development+452+6176+projects" target="_blank">
MMBase Taglib</a> (version 1.0.1), created by 
<a href="http://www.mmbase.org/mmbasenew/index3.shtml?about+547+427+organization" target="_blank">NOS Internet</a> under the 
<a href="http://www.mmbase.org/mmbasenew/index3.shtml?about+541+3649+documentation" target="_blank">Mozilla License 1.0</a>
</p>
<p>
  version of the editors: 2003-04
</p>
<p>
  These editors were tested with application servers orion 1.6.0 and tomcat 4.1.
</p>
<p>
  The tested browsers are Mozilla 1.0, Netscape 4.7, Opera 5 (all ok)
  and lynx 2.8.4 (not ok because of HttpPost), in Linux. Internet
  Explorer 5.5 was tested on a Windows NT computer. You are using <%=
  request.getHeader("user-agent") %> 
</p>
<p>
  Features:
</p>
  <ul>
  <li>Generic editing of MMBase content, using MMBase taglib and little JSP.</li>
	<li>Relations (with directionality).</li>
	<li>Image upload.</li>
	<li>Aliases.</li>
	<li>Searching with search fields, on alias and on age.</li>
	<li>Configurable (a.o. language and the aspect).</li>
  </ul>
<p>
  Known bugs:
</p>
  <ul>
	<li>Bugs in HttpPost hinder good working in lynx (and in combination Opera/Tomcat?)</li>
	<li>Source code is a little too chaotic</li>
	<li>Heavy use of CSS to define the looks hinder functionallity in some buggy browsers</li>
  </ul>
</td></tr>
</table>
<%@ include file="foot.jsp"  %>
</mm:cloud>