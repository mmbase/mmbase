<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@page language="java" contentType="text/html;charset=utf-8" %>
<mm:cloud jspvar="cloud">
<html>
    <head>
	  <title>Codings examples</title>	 
    </head>
	<body>
       <h1>Introduction</h1>
		<p>
		  JSP supports many encodings. Java internally uses
		  unicode. This page is using UTF-8.
		</p>
		<p>
		  There are several sides when presenting non-ascii letters on
		  a MMBase html-page. In the first 'chapter' of this page,
		  there are some things in the page itself, and in the second,
		  data from the MMBase database is presented. It can depend on
		  the value of the 'encoding' property in mmbaseroot.xml what
		  you are seeing there. If you are e.g. using mysql, and you
		  indicate 'UTF-8' in mmbaseroot.xml, while your database is
		  really iso-8859-1, then things will go rather wrong. If the
		  mmbaseroot.xml setting agrees with the actual situation in
		  the database, but it is not UTF-8, then you will see a lost
		  of question marks, but everything should work.
		</p>
		 <h1>In-page text</h1>
		<p>Café tweeëntwintig</p> <p>Ĉu vi ŝatas tion?</p> <mm:import
		externid="node" from="parameters">codings</mm:import>
		<mm:node number="$node" notfound="skip">
		  <h2><mm:field name="title" /></h2>
		  <h3><mm:field name="uppercase(subtitle)" /></h3>
		  <p>
			<mm:field name="intro" />
		  </p>
		  <mm:field name="html(body)" />
		  <mm:import id="articlefound" />
          <hr />
          used node: <mm:write referid="node" /> (<mm:field name="number" />)<br />
          <a href="<mm:url page="index.shtml" />">SCAN version of this page</a><br />
  		  <a href="<mm:url page="default1.jsp" />">ISO-8859-1 version of this page</a>
		</mm:node>
		<mm:notpresent referid="articlefound">
		   <h1>The 'Codings' applications was not deployed. Please do so before going to this page.</h1>
		 </mm:notpresent>
	  </body>
</html>
</mm:cloud>  