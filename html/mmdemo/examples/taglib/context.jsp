<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><html>
<body>
<h1>context</h1>
<p>
To use a context, you can use a context tag. But there is one implicit
context, named 'context'. This page is using this feature.
</p>
<p>
To start with, a context is empty, you can put things in it e.g. with
the 'import' tag, which gets objects from the outside world (like
parameters).
</p>
<mm:import id="hoi" externid="haj" required="true" jspvar="groet" />
<p>
There are several ways to write something from the context to the
page. Here are a few examples with the 'write' tag. The write tag uses
the 'referid' attribute. Such an attribute expects the <em>name</em>
of the variable:
</p>
<mm:write referid="hoi" />, 
<mm:write referid="context.hoi" />,
<mm:write context="context" referid="hoi" />,
<mm:write referid="hoi" jspvar="greet"><%= greet %></mm:write>,
<%= groet %>
<p>
Sometime you also want to use the value of a variable. In that case a
construction with a dollar sign ($) must be used. Imagine for example
that the <em>value</em> of the command-line variable 'haj' (which
internally in this page is named 'hoi') must be used in an url:
</p>
<mm:url page="${hoi}.jsp">
 <mm:param name="some_variable">value</mm:param>
</mm:url>,
<mm:url page="test.${context.hoi}.jsp">
 <mm:param name="some_variable">another_example</mm:param>
</mm:url>
<br />
<hr />
<a href='<mm:url page="context2.jsp" referids="hoi">
          <mm:param name="hello">saluton</mm:param>
         </mm:url>'>next page</a>	 
</body>
</html>