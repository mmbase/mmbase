<%@include file="/WEB-INF/templates/portletglobals.jsp"%>
<%@include file="/WEB-INF/templates/edit_defaults/sections/globals.jsp"%>

<div class="portlet-config-canvas">
   <form name="<portlet:namespace />form" method="post" target="_parent"
      action="<cmsc:actionURL><cmsc:param name="action" value="edit"/></cmsc:actionURL>">

<table class="editcontent">
   <%-- Save button --%>
   <c:import url="sections/savebutton.jsp" />
         
   <tr>
      <td colspan="3">
         <h3><fmt:message key="edit_defaults.title" /></h3>
      </td>
   </tr>
   <tr>
      <td><fmt:message key="edit_defaults.channel" />:</td>
      <td align="right">
         <a href="javascript:openRepositoryWithChannel()">
            <img src="<cmsc:staticurl page='/editors/gfx/icons/preview.png'/>" alt="<fmt:message key="edit_defaults.preview"/>"/></a>
         <a href="<c:url value='/editors/repository/select/SelectorChannel.do?channel=${contentchannel}' />"
            target="selectchannel" onclick="openPopupWindow('selectchannel', 340, 400)"> 
               <img src="<cmsc:staticurl page='/editors/gfx/icons/select.png'/>" alt="<fmt:message key="edit_defaults.channelselect" />"/></a>
         <a href="javascript:erase('contentchannel');erase('contentchannelpath')">
            <img src="<cmsc:staticurl page='/editors/gfx/icons/erase.png'/>" alt="<fmt:message key="edit_defaults.erase"/>"/></a>
      </td>
      <td>
      <mm:cloud>
         <mm:node number="${contentchannel}" notfound="skip">
            <mm:field name="path" id="contentchannelpath" write="false" />
         </mm:node>
      </mm:cloud>
      <input type="hidden" name="contentchannel" value="${contentchannel}" />
      <input type="text" name="contentchannelpath" value="${contentchannelpath}" disabled="true" />
      </td>
   </tr>
   <tr>
      <td colspan="2"><fmt:message key="edit_defaults.maxelements" />:</td>
      <td><input type="text" name="maxElements" value="${maxElements}" /></td>
   </tr>
   <tr>
      <td colspan="3">
         <h4><fmt:message key="edit_defaults.clickpage" /></h4>
      </td>
   </tr>
   <tr>
      <td><fmt:message key="edit_defaults.page" />:</td>
      <td align="right">
         <a href="<c:url value='/editors/site/select/SelectorPage.do?channel=${page}' />"
            target="selectpage" onclick="openPopupWindow('selectpage', 340, 400)"> 
               <img src="<cmsc:staticurl page='/editors/gfx/icons/select.png'/>" alt="<fmt:message key="edit_defaults.channelselect" />"/></a>
         <a href="javascript:erase('page');erase('pagepath');eraseList('window')">
            <img src="<cmsc:staticurl page='/editors/gfx/icons/erase.png'/>" alt="<fmt:message key="edit_defaults.erase"/>"/></a>
      </td>
      <td>
      <mm:cloud>
         <mm:node number="${page}" notfound="skip">
            <mm:field name="path" id="pagepath" write="false" />
         </mm:node>
      </mm:cloud>
      <input type="hidden" name="page" value="${page}" />
      <input type="text" name="pagepath" value="${pagepath}" disabled="true" />
   </tr>
   <tr>
      <td colspan="2"><fmt:message key="edit_defaults.window" />:</td>
      <td>
         <cmsc:select var="window">
            <c:forEach var="position" items="${pagepositions}">
               <cmsc:option value="${position}" />
            </c:forEach>
         </cmsc:select>
      </td>
   </tr>
   <tr>
      <td colspan="3">
         <a href="javascript:document.forms['<portlet:namespace />form'].submit()" class="button">
            <img src="<cmsc:staticurl page='/editors/gfx/icons/save.png'/>" alt=""/> <fmt:message key="edit_defaults.save" /></a>
      </td>
   </tr>
</table>
</form>
</div>
