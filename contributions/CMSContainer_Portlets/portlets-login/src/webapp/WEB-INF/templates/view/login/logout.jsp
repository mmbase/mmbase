<%@include file="/WEB-INF/templates/portletglobals.jsp"%>
<fmt:setBundle basename="com.finalist.cmsc.portlets.LoginPortlet" scope="request"/>
<h3><fmt:message key="view.logout" /></h3>

<fmt:message key="view.logged_in_as" />&nbsp;${pageContext.request.remoteUser}.
<br>
<a href="<cmsc:actionURL><cmsc:param name="action" value="logout"/></cmsc:actionURL>">
  <fmt:message key="view.logout" />
</a>
