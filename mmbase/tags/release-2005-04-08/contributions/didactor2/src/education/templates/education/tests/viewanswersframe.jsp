<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<mm:import externid="testNo" required="true"/>
<mm:import externid="madetestNo" required="true"/>
<mm:import externid="userNo" required="true"/>

<%@include file="/shared/setImports.jsp" %>
<%@include file="/education/tests/definitions.jsp" %>

<html>
<head>
   <title>View Answers</title>
   <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" />" />
</head>
<body>

<mm:treeinclude page="/education/tests/viewanswers.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="testNo"><mm:write referid="testNo"/></mm:param>
  <mm:param name="madetestNo"><mm:write referid="madetestNo"/></mm:param>
  <mm:param name="userNo"><mm:write referid="userNo"/></mm:param>
</mm:treeinclude>

</body>
</html>
</mm:cloud>
</mm:content>
