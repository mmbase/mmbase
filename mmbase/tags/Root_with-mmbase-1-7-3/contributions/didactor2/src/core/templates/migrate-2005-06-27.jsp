<html>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>

making titles visible...<br/>
<mm:listnodes type="flashpages"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="tests"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="preassessments"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
<mm:listnodes type="postassessments"><mm:setfield name="showtitle">1</mm:setfield></mm:listnodes>
done.
</mm:cloud>
</mm:content>
</html>

