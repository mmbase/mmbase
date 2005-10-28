<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>


<mm:cloud loginpage="/login.jsp" jspvar="cloud">
   <%@include file="/shared/setImports.jsp" %>
   <mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
      <mm:param name="extraheader">
         <title>Reload Player</title>
         <link rel="stylesheet" type="text/css" href="css/pop.css" />
      </mm:param>
   </mm:treeinclude>
</mm:cloud>

<iframe id="didactor_frame" name="didactor_frame" src="index.htm" width="100%" height="85%"></iframe>
