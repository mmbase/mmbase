<%@include file="/WEB-INF/templates/portletglobals.jsp" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://finalist.com/cmsc" prefix="cmsc" %>

<fmt:setBundle basename="portlets-newslettersubscription" scope="request"/>
<form method="POST" name="<portlet:namespace />form_subscribe" action="<cmsc:actionURL/>">
   <div class="content">
      <c:choose>
      <c:when test="${fn:length(subscriptionsToBeTerminate) gt 0}">
      <div><fmt:message key="subscription.subscribe.operation.confirmterminate"/></div>
      <ul>
         <c:forEach items="${subscriptionsToBeTerminate}" var="subscription">
            <li>${subscription.newsletter.title}</li>
            <input type="checkbox" name="subscriptions" value="${subscription.id}" checked="checked"
                   style="visibility:hidden;"/>
         </c:forEach>
      </ul>
      <input type="hidden" name="action" value="terminate"/>
      <input type="hidden" name="confirm_unsubscribe" value="confirm_unsubscribe"/>

      <a href="javascript:document.forms['<portlet:namespace />form_subscribe'].submit()" class="button">
         <fmt:message key="subscription.subscribe.operation.terminate"/>
      </a>
   </div>
   </c:when>
   <c:otherwise>
      <fmt:message key="subscription.subscribe.status.nosubscription"/>
   </c:otherwise>
   </c:choose>
</form>



