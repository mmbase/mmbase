<%@include file="/WEB-INF/templates/portletglobals.jsp" %>

<div class="portlet-config-canvas">
<h3><fmt:message key="edit_defaults.title" /></h3>

<form method="POST" name="<portlet:namespace />form" action="<cmsc:actionURL><cmsc:param name="action" value="edit"/></cmsc:actionURL>" target="_parent">

<table class="editcontent">
   <tr>
      <td><fmt:message key="edit_defaults.view" />:</td>
      <td>
         <cmsc:select var="view">
            <c:forEach var="v" items="${views}">
               <cmsc:option value="${v.id}" name="${v.title}" />
            </c:forEach>
         </cmsc:select><br />
      </td>
   </tr>
   <tr>
      <td><fmt:message key="edit_defaults.maxelements" />:</td>
      <td>
         <input type="text" name="userName" value="${user}" />
         <input type="text" name="passWord" value="${pass}" />
      </td>
   </tr>
   <tr>
      <td><fmt:message key="edit_defaults.indexname" />:</td>
      <td><lm:listindexes var="indexes"/>
         <cmsc:select var="indexName">
            <c:forEach var="idx" items="${indexes}">
               <cmsc:option value="${idx}" name="${idx}" />
            </c:forEach>
         </cmsc:select>
      </td>
   </tr>
   <tr>
		<td colspan="2">
			<input type="submit" value="<fmt:message key="edit_defaults.save" />" class="button" />
		</td>
	</tr>
</table>
</div>

