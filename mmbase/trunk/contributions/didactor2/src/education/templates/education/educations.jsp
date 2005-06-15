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
<div class="learnenvironment">

<%-- remember this page --%>
<mm:treeinclude page="/education/storebookmarks.jsp" objectlist="$includePath" referids="$referids">
    <mm:param name="learnobject"><mm:write referid="edu"/></mm:param>
    <mm:param name="learnobjecttype">educations</mm:param>
</mm:treeinclude>


<mm:node number="$edu">

   <mm:treeinclude page="/education/pages/content.jsp" objectlist="$includePath" referids="$referids">
        <mm:param name="learnobject"><mm:field name="number"/></mm:param>
    </mm:treeinclude>



   <mm:treeinclude page="/education/paragraph/paragraph.jsp" objectlist="$includePath" referids="$referids">
      <mm:param name="node_id"><mm:write referid="edu"/></mm:param>
      <mm:param name="path_segment"></mm:param>
   </mm:treeinclude>
</mm:node>
</div>
</body>

</html>



</mm:cloud>

</mm:content>

