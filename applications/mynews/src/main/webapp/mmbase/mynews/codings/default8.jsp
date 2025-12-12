<%@page session="false" language="java"
       pageEncoding="UTF-8" contentType="text/html;charset=utf-8"
%><%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><mm:import externid="postprocessor">reducespace</mm:import><mm:content type="text/html" postprocessor="$postprocessor">
<mm:import externid="cite">false</mm:import>
<mm:cloud>
<html>
  <head>
    <title>Codings examples</title>
    <link rel="stylesheet" href="<mm:url page="/mmbase/style/css/mmbase.css" />" type="text/css" />
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
    <p>Café tweeëntwintig</p>
    <p>Ĉu vi ŝatas tion?</p>
    <p>CP1252:  <%= org.mmbase.util.transformers.CP1252Surrogator.getTestString() %></p>
    <h1>MMBase data</h1>
    <mm:import externid="node" from="parameters">codings</mm:import>
    <mm:node id="cod" number="$node" notfound="skip">
      <h2><mm:field name="title" /></h2>
      <h3><mm:field name="subtitle" escape="uppercase,text/html" /></h3>

      <mm:field name="intro" escape="p" />
      <mm:field name="body" escape="p" />

      <mm:import id="articlefound" />

      <h1>Image</h1>
      <p>
        If you have a recent version of ImageMagick installed, then you can also use this text in an
        image (well, that is, for me the chinese char where missing in the font). The LC_ALL setting
        must contain UTF-8. You need Arial.ttf in &lt;mmbase config dir&gt;/fonts/ for this
        example:
      </p>
      <%-- Simply search the last image, resize it and make it black/white, and write on it in red --%>
      <mm:listnodes type="images" orderby="number" directions="down" max="1">
        <mm:field node="cod" name="subtitle">
          <img src="<mm:image template="s(600x80!)+f(png)+modulate(200,0)+font(mm:fonts/Arial.ttf)+fill(ff0000)+pointsize(20)+text(20,50,'$_')" />" alt="<mm:field name="title" />" />: <mm:field name="description" />
        </mm:field>
      </mm:listnodes>
<% String thisDir = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() != 80 ? ":" + request.getServerPort() : "") + new java.io.File(request.getRequestURI()).getParent() + "/"; %>
      <h1>Included pages</h1>
      <p><em>With mm:include (utf-8 page):</em></p>
      <% try { %>
      <mm:include referids="node" page="included.jsp" />
      <p><em>With mm:include (external, utf-8 page):</em></p>
      <mm:include referids="node" page='<%=thisDir + "included.jsp"%>' />
      <% } catch (Exception e) { %>
      <p>Did not work (<%=e.toString()%>)</p>
      <% } %>
      <p><em>With mm:include (iso-8859-1 page):</em></p>
      <% try { %>
      <mm:include referids="node" page="included1.jsp" />
      <p><em>With mm:include (external, iso-8859-1 page):</em></p>
      <mm:include referids="node" page='<%=thisDir + "included1.jsp"%>' />
      <% } catch (Exception e) { %>
      <p>Did not work (<%=e.toString()%>)</p>
      <% } %>
      <p><em>With jsp:include (utf-8 page):</em></p>
      <% try { %>
      <jsp:include page="included.jsp" />
      <% } catch (Exception e) { %>
      <p>Did not work (<%=e.toString()%>)</p>
      <% } %>
      <p><em>With jsp:include (iso-8859-1 page):</em></p>
      <% try { %>
      <jsp:include page="included1.jsp" />
      <% } catch (Exception e) { %>
      <p>Did not work (<%=e.toString()%>)</p>
      <% } %>
      <p><em>With &lt;%@:include (utf-8 page)</em></p>
      <%@include file="atincluded.jsp" %>

      <p><em>With &lt;%@:include (iso-8859-1 page):</em></p>
      <%@include file="atincluded1.jsp" %>

<hr />
<h2 id="xmls">
  Including XML's
  <mm:write referid="cite">
    (
    <mm:compare value="true">
      <a href="<mm:url><mm:param name="cite" value="false" /></mm:url>#xmls">No cite</a>
    </mm:compare>
    <mm:compare value="true" inverse="true">
      <a href="<mm:url><mm:param name="cite" value="true" /></mm:url>#xmls">Cite</a>
    </mm:compare>
  </mm:write>
  )
</h2>

<mm:log>mm:include-ing XML's</mm:log>
<p><em>With mm:include (iso-8859-1 xml<mm:compare referid="cite" value="true">, Cite</mm:compare>):</em></p>
<pre>
<mm:include page="included1.xml" escape="text/xml" cite="$cite" />
</pre>
 <p><em>With mm:include (UTF-8 xml<mm:compare referid="cite" value="true">, Cite</mm:compare>):</em></p>
<pre>
<mm:include page="included8.xml"  escape="text/xml" cite="$cite" />
</pre>
 <p><em>With mm:include (iso-8859-1 xml.jsp<mm:compare referid="cite" value="true">, Cite</mm:compare>):</em></p>
<pre>
<mm:include page="included1.xml.jsp" escape="text/xml"  />
</pre>
 <p><em>With mm:include (UTF-8 xml.jsp<mm:compare referid="cite" value="true">, Cite</mm:compare>):</em></p>
<pre>
<mm:include page="included8.xml.jsp"  escape="text/xml" />
</pre>

<p><em>With mm:include ('external' iso-8859-1 xml): </em></p>
<pre>
<mm:include page='<%= thisDir + "included1.xml"%>' escape="text/xml" />
</pre>
 <p><em>With mm:include ('external' UTF-8 xml):</em></p>
<pre>
<mm:include page='<%= thisDir + "included8.xml"%>' escape="text/xml" />
</pre>
<p><em>With mm:include ('external' iso-8859-1 xml.jsp): </em></p>
<pre>
<mm:include page='<%= thisDir + "included1.xml.jsp"%>' escape="text/xml" />
</pre>
 <p><em>With mm:include ('external' UTF-8 xml.jsp):</em></p>
<pre>
<mm:include page='<%= thisDir + "included8.xml.jsp"%>' escape="text/xml" />
</pre>
<mm:log>jsp:include-ing XML's</mm:log>
      <p><em>With jsp:include (iso-8859-1 xml):</em></p>
<pre>
<mm:log>i1</mm:log>
<% try { %>
<mm:import id="i1"><jsp:include page="included1.xml"  /></mm:import><mm:write referid="i1" escape="text/xml" />
<% } catch (Exception e) { %>
<p>Did not work (<%=e.toString()%>)</p>
<% } %>
</pre>
      <p><em>With jsp:include (UTF-8 xml):</em></p>
<pre>
<mm:log>i8</mm:log>
<% try { %>
<mm:import id="i8"><jsp:include page="included8.xml"  /></mm:import><mm:write referid="i8" escape="text/xml" />
</pre>
      <p><em>With jsp:include (iso-8859-1 xml.jsp):</em></p>
<pre>
<% } catch (Exception e) { %>
<p>Did not work (<%=e.toString()%>)</p>
<% } %>
<% try { %>
<mm:import id="i1j"><jsp:include page="included1.xml.jsp"  /></mm:import><mm:write referid="i1j" escape="text/xml" />
<% } catch (Exception e) { %>
<p>Did not work (<%=e.toString()%>)</p>
<% } %>
</pre>

  <p><em>With jsp:include (UTF-8 xml.jsp):</em></p>
<pre>
<mm:log>i8j</mm:log>
<% try { %>
<mm:import id="i8j"><jsp:include page="included8.xml.jsp"  /></mm:import><mm:write referid="i8j" escape="text/xml" />
<% } catch (Exception e) { %>
<p>Did not work (<%=e.toString()%>)</p>
<% } %>
</pre>
      <p><em>With &lt;%@include (iso-8859-1 xml):</em></p>
<pre>
<mm:log>ai1</mm:log>
<mm:import id="ai1"><%@include file="included1.xml" %></mm:import><mm:write referid="ai1" escape="text/xml" />
</pre>
      <p><em>With &lt;%@include (UTF-8 xml):</em></p>
<pre>
<mm:log>ai8</mm:log>
<mm:import id="ai8"><%@include file="included8.xml" %></mm:import><mm:write referid="ai8" escape="text/xml" />
</pre>
<mm:log>formatter</mm:log>
   <h2>Formatter-tag</h2>
   <p><em>In-page XML, explicitely specifying encoding:</em></p>
<pre>
  <mm:formatter format="escapexmlpretty">
    <?xml version="1.0" encoding="iso-8859-1"?>
    <mmxf version="1" >
      <p>Café tweeëntwintig</p>
      <p>Ĉu vi ŝatas tion?</p>
    </mmxf>

  </mm:formatter>
</pre>
 <p><em>With &lt;%@include (iso-8859-1 xml):</em></p>
<pre>
  <mm:formatter format="escapexmlpretty">
    <%@include file="included1.xml" %>
  </mm:formatter>
</pre>
 <p><em>With &lt;%@include (UTF-8 xml):</em></p>
<pre>
  <mm:formatter format="escapexmlpretty">
    <%@include file="included8.xml" %>
  </mm:formatter>
</pre>
      <hr />
      used node: <mm:write referid="node" /> (<mm:field name="number" />)<br />
      <a href="<mm:url page="." />">back</a><br />
    </mm:node>

    <mm:notpresent referid="articlefound">
      <h1>The 'Codings' applications was not deployed. Please do so before going to this page.</h1>
    </mm:notpresent>

    <a href="<mm:url page="../taglib/showanypage.jsp"><mm:param name="page"><%=request.getServletPath()%></mm:param></mm:url>">Source of this page</a><br />

  </body>
</html>
</mm:cloud>
</mm:content>
