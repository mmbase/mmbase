<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<mm:content postprocessor="reducespace">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<mm:import externid="edu" required="true"/>

<%@include file="/shared/setImports.jsp" %>
<html>
<head>
   <title>Learnblock content</title>
   <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" />" />
</head>
<body>

<mm:node number="$edu">
  <h1><mm:field name="name"/></h1>
  <mm:field id="intro" name="intro" write="false"/>
  <mm:write referid="intro" escape="none"/>
</mm:node>

</body>
</html>

</mm:cloud>
</mm:content>
