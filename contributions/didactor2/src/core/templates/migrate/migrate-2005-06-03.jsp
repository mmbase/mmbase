<html>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>

making all titles visible...<br/>
<mm:listnodes type="educations"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="learnblocks"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="pages"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="paragraphs"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
done.
</mm:cloud>
</mm:content>
</html>
