<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud logon="wwwuser" pwd="buggie90">
<mm:import externid="bugreport" jspvar="bugreport" />
<mm:import externid="newstatus" />

	<mm:node id="bugnode" referid="bugreport">
		<mm:setfield name="bstatus"><mm:write referid="newstatus" /></mm:setfield>
	</mm:node>

	<%response.sendRedirect("/bugtracker/jsp/fullview.jsp?bugreport="+bugreport);%>
</mm:cloud>
