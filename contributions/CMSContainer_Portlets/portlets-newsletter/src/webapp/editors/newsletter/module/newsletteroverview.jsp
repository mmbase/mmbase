<%@include file="globals.jsp" %>

<b><fmt:message key="newsletteroverview.title" /></b>

<table width="50%">
	<tr>	
		<th></th>
		<th align="left"><fmt:message key="newsletteroverview.newsletter" /></tk>
		<th align="left"><fmt:message key="newsletteroverview.numberofthemes" /></tk>
		<th align="left"><fmt:message key="newsletteroverview.numberofpublications" /></tk>
		<th align="left"><fmt:message key="newsletteroverview.numberofsubscriptions" /></tk>
	</tr>
	<c:forEach var="bean" items="${newsletterOverviewBeans}">
	<c:url var="url" value="NewsletterAction.do?action=detail&number=${bean.number}" />
	<tr>
		<td><cmsc:checkbox var="checked" value="${bean.number}" /></td>
		<td><a href="${url}"><jsp:getProperty name="bean" property="title" /></a></td>
		<td><jsp:getProperty name="bean" property="numberOfThemes" /></td>
		<td><jsp:getProperty name="bean" property="numberOfPublications" /></td>
		<td><jsp:getProperty name="bean" property="numberOfSubscriptions" /></td>
	</tr>
	</c:forEach>
</table>

<p><a href="SubscriberAction.do?action=unsubscribe"><fmt:message key="newsletteroverview.link.deleteallsubscriptions" /></a><p>
