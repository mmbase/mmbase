<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud logon="wwwuser" pwd="buggie90">
        <mm:import externid="account" />
        <mm:import externid="password" />

        <mm:listnodes type="users" constraints="(account='$account' AND password='$password')">
                <mm:import id="usernumber" jspvar="usernumber" ><mm:field name="number"/></mm:import>
        </mm:listnodes>
        <mm:present referid="usernumber">
            <mm:write referid="account" cookie="ca" />
            <mm:write referid="password" cookie="cw" />
    		<%response.sendRedirect("/bugtracker/jsp/showMessage.jsp?message=login");%>
        </mm:present>
        <mm:notpresent referid="usernumber">
    		<%response.sendRedirect("/bugtracker/jsp/changeUser.jsp?error=login");%>
        </mm:notpresent>
</mm:cloud>
