<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
    <div class="applicationMenubar" style="white-space: nowrap">
    	<div class="menuItemApplicationMenubar">
			<a title="<fmt:message key="HOME" />" href="<mm:url page="/index.jsp" />" class="menubar"><fmt:message key="HOME" /></a>
    	</div>
    	
		<div class="menuSeperatorApplicationMenubar"></div>

                
        <div class="menuItemApplicationMenubar">
        <mm:node number="$user">
            <a title="<fmt:message key="LOGOUT" />" href="<mm:url page="/logout.jsp" />" class="menubar"><fmt:message key="LOGOUT" /> <mm:field name="firstname"/> <mm:field name="lastname"/></a>
        </mm:node>
    </div>


    	
		<div class="menuSeperatorApplicationMenubar"></div>
		
		<div class="menuItemApplicationMenubar">
			<a title="<fmt:message key="HELP" />" href="<mm:treefile page="/help/index.jsp" objectlist="$includePath" referids="$referids"/>" class="menubar"><fmt:message key="HELP" /></a>
		</div>
		
		<div class="menuSeperatorApplicationMenubar"></div>
		
		<div class="menuItemApplicationMenubar">
			<a title="<fmt:message key="CONFIGURATION" />" href="<mm:url page="/admin/" />" class="menubar"><fmt:message key="CONFIGURATION" /></a>
		</div>

                <div class="menuSeperatorApplicationMenubar"></div>
                <div class="menuItemApplicationMenubar">
                <a title="<fmt:message key="PRINT" />" href="javascript:printThis();"  class="menubar"><fmt:message key="PRINT"/></a>
                </div>
                
	</div>

<script language="JavaScript">
<!--
function printThis() {
    if (frames && frames['content']) {
        if (frames['content'].focus) 
            frames['content'].focus(); 
        if (frames['content'].print)
            frames['content'].print();
    }
    else if (window.print) {
        window.print();
    }
}
//-->
</script>
</mm:cloud>
