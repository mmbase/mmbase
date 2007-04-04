<%@page language="java" contentType="text/html;charset=utf-8"%>
<%@include file="globals.jsp" %>
<%@page import="com.finalist.cmsc.repository.RepositoryUtil" %>
<%@page import="com.finalist.cmsc.security.*" %>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<html:html xhtml="true">
	<head>
		<title><fmt:message key="content.title" /></title>
		<link rel="stylesheet" type="text/css" href="../css/main.css" />
		<script src="content.js" type="text/javascript"></script>
		<script src="../utils/window.js" type="text/javascript"></script>
		<script src="../utils/rowhover.js" type="text/javascript"></script>
	    <script type="text/javascript" src="../utils/transparent_png.js" ></script>
	    <script type="text/javascript">
			function refreshChannels() {
				refreshFrame('channels');
				if (window.opener) {
					window.close();
				}
			}
		</script>
	</head>
	<body onload="refreshChannels();alphaImages();"	>
		<c:if test="${not empty param.message}">      
			<script type="text/javascript">
				addLoadEvent(alert('${param.message}'));
			</script>
		</c:if>
	
<mm:cloud jspvar="cloud" rank="basic user" loginpage="../login.jsp">
	  <mm:import externid="parentchannel" jspvar="parentchannel" vartype="Integer" from="parameters" required="true"/>
      <mm:import jspvar="returnurl" id="returnurl">/editors/repository/Content.do?parentchannel=<mm:write referid="parentchannel"/>&direction=down</mm:import>

      <div class="tabs">
         <!-- actieve TAB -->
         <div class="tab_active">
            <div class="body">
               <div>
                  <a name="activetab"><fmt:message key="content.title" /></a>
               </div>
            </div>
         </div>
      </div>
      

    <div class="editor">
      <div class="body">


<mm:node number="$parentchannel" jspvar="parentchannelnode">
<% UserRole role = RepositoryUtil.getRole(cloud, parentchannelnode, false); %>
   <p>
   <fmt:message key="content.channel" >
      <fmt:param ><mm:field name="path"/></fmt:param>
   </fmt:message>
   </p>
		<% if (role != null && SecurityUtil.isWriter(role)) { %>
			<ul class="shortcuts">
			   <li class="new" style="text-decoration: none;"><fmt:message key="content.new" />
				  <form action="../WizardInitAction.do" method="post" style="display:inline;text-decoration:none">
					 <input type="hidden" name="action" value="create" />
					 <input type="hidden" name="creation" value="<mm:write referid="parentchannel" />" />
					 <input type="hidden" name="returnurl" value="<%= returnurl %>" />


					 <select name="contenttype">
						<c:forEach var="type" items="${typesList}">
	                        <option value="${type.value}">${type.label}</option>
						</c:forEach>
					 </select>
					 <input type="submit" name="submitButton" value="<fmt:message key="content.create" />" class="button" />
				  </form>
			   </li>
			   <li class="link">
				  <a href="<mm:url page="SearchInitAction.do">
							  <mm:param name="linktochannel" value="$parentchannel" />
							  <mm:param name="returnurl" value="${returnurl}" />
                       <mm:param name="mode" value="advanced" />
                       <mm:param name="action" value="link" />
						   </mm:url>">
					 <fmt:message key="content.existing" />
				  </a>
			   </li>
				<% if (SecurityUtil.isEditor(role)) { %>
			   <li class="reorder">
				  <a href="<mm:url page="ReorderAction.do">
							  <mm:param name="parent" value="$parentchannel" />
						   </mm:url>">
					 <fmt:message key="content.reorder" />
				  </a>
			   </li>
				<% } %>
			</ul>
			<% } %>
	</div>
   <div class="ruler_green"><div><fmt:message key="content.content" /></div></div>
	<div class="body">
<mm:import externid="elements" from="request" required="true"/>

 	<c:set var="listSize" value="${fn:length(elements)}"/>
	<c:set var="resultsPerPage" value="50"/>
	<c:set var="offset" value="${param.offset}"/>
    <c:set var="extraparams" value="&parentchannel=${param.parentchannel}"/>

   <%@include file="../pages.jsp" %>


   <table>
   <thead>
      <tr>
         <th></th>
         <th><fmt:message key="content.typecolumn" /></th>
         <th><fmt:message key="content.titlecolumn" /></th>
         <th><fmt:message key="content.authorcolumn" /></th>
         <th><fmt:message key="content.lastmodifiedcolumn" /></th>
         <th><fmt:message key="content.numbercolumn" /></th>
         <th><fmt:message key="content.creationchannelcolumn" /></th>
         <th></th>
      </tr>
   </thead>
   <tbody class="hover">
   <mm:listnodes referid="elements" jspvar="node"  max="${resultsPerPage}" offset="${offset*resultsPerPage}">
		<mm:field name="number" write="false" id="number" vartype="String"/>
		<mm:field name="number" write="false" id="relnumber"/>

		<mm:url page="../WizardInitAction.do" id="url" write="false" >
		   <mm:param name="objectnumber" value="$number"/>
		   <mm:param name="returnurl" value="$returnurl" />
		</mm:url>
      <tr <mm:even inverse="true">class="swap"</mm:even> href="<mm:write referid="url"/>">
		<td style="white-space: nowrap;">
        	<a href="javascript:info('<mm:field name="number" />')"><img src="../gfx/icons/info.png" width="16" height="16" title="<fmt:message key="content.info" />" alt="<fmt:message key="content.info" />"/></a>
            <a href="<cmsc:contenturl number="${number}"/>" target="_blanc"><img src="../gfx/icons/preview.png" alt="<fmt:message key="content.preview.title" />" title="<fmt:message key="content.preview.title" />" /></a>
			<a href="javascript:callEditWizard('<mm:field name="number" />');"  title="<fmt:message key="content.edit" />"><img src="../gfx/icons/edit.png" width="16" height="16" title="<fmt:message key="content.edit" />" alt="<fmt:message key="content.edit" />"/></a>
			<% if (role != null && SecurityUtil.isWriter(role)) { %>
				<a href="<c:url value='/editors/repository/select/SelectorChannel.do?role=writer' />"
					target="selectchannel" onclick="moveContent(<mm:field name="number" />, ${parentchannel} )"> 
	                   <img src="../gfx/icons/page_move.png" title="<fmt:message key="searchform.icon.move.title" />" /></a>

				<a href="javascript:unpublish('<mm:write referid="parentchannel" />','<mm:field name="number" />');" title="<fmt:message key="content.unlink" />"><img src="../gfx/icons/delete.png" width="16" height="16" title="<fmt:message key="content.unlink" />" alt="<fmt:message key="content.unlink" />"/></a>
			<% } %>
         <mm:haspage page="/editors/versioning">
            <c:url value="/editors/versioning/ShowVersions.do" var="showVersions">
               <c:param name="nodenumber"><mm:field name="number" /></c:param>
            </c:url>
            <a href="#" onclick="openPopupWindow('versioning', 750, 550, '${showVersions}')"><img src="../gfx/icons/versioning.png" title="<fmt:message key="content.icon.versioning.title" />" alt="<fmt:message key="content.icon.versioning.title" />"/></a>
         </mm:haspage>
			<% if (role != null && SecurityUtil.isWriter(role)) { %>
	         <mm:last inverse="true">
	            <a href="javascript:moveDown('<mm:field name="number" />','<mm:write referid="parentchannel" />')"><img src="../gfx/icons/down.png" width="16" height="16" title="<fmt:message key="content.move.down" />" alt="<fmt:message key="content.move.down" />"/></a>
	         </mm:last>
	         <mm:first inverse="true">
	            <mm:last><img src="../gfx/icons/spacer.png" width="16" height="16" alt=""/></mm:last>
	            <a href="javascript:moveUp('<mm:field name="number" />','<mm:write referid="parentchannel" />')"><img src="../gfx/icons/up.png" width="16" height="16" title="<fmt:message key="content.move.up" />" alt="<fmt:message key="content.move.up" />"/></a>
	         </mm:first> 
	      <% } %>
 	  	   <cmsc:hasfeature name="savedformmodule">
			<c:set var="typeval">
      			<mm:nodeinfo type="type" />          	
      		</c:set> 
      		<c:if test="${typeval == 'responseform'}">         
	       		<mm:url page="/editors/savedform/ShowSavedForm.do" id="showSavedForms" write="false">
	            	<mm:param name="nodenumber"><mm:field name="number" /></mm:param>
	            	<mm:param name="initreturnurl" value="${returnurl}" />
	       		</mm:url>                   
	       		<a href="<mm:write referid="showSavedForms"/>"><img src="../gfx/icons/application_form_magnify.png" title="<fmt:message key="content.icon.savedform.title" />" alt="<fmt:message key="content.icon.savedform.title" />"/></a>          
	       	</c:if>
	 	</cmsc:hasfeature>          	
      </td>
      <td onMouseDown="objClick(this);">
		   <mm:nodeinfo type="guitype"/>
		</td>
		<td  onMouseDown="objClick(this);">
         <mm:field jspvar="title" write="false" name="title" />
			<c:if test="${fn:length(title) > 50}">
				<c:set var="title">${fn:substring(title,0,49)}...</c:set>
			</c:if>
			${title}
		</td>
		<td onMouseDown="objClick(this);" style="white-space: nowrap;">
       	<mm:field name="lastmodifier" jspvar="lastmodifier" write="false"/>
      	<mm:listnodes type="user" constraints="username = '${lastmodifier}'">
      		<c:set var="lastmodifierFull"><mm:field name="firstname" /> <mm:field name="prefix" /> <mm:field name="surname" /></c:set>
      		<c:if test="${lastmodifierFull != ''}"><c:set var="lastmodifier" value="${lastmodifierFull}"/></c:if>
      	</mm:listnodes>
      	${lastmodifier}
      </td>
        <td style="white-space: nowrap;"><mm:field name="lastmodifieddate"><cmsc:dateformat displaytime="true" /></mm:field></td>
        <td><mm:field name="number"/></td>
		<td width="50" onMouseDown="objClick(this);" style="white-space: nowrap;">
			<c:choose>
				<c:when test="${not empty createdNumbers[number]}">
					<fmt:message key="content.yes" />
				</c:when>
				<c:otherwise>
			      <mm:relatednodes role="creationrel" type="contentchannel">
			          <mm:field name="number" jspvar="channelNumber" write="false"/>
			          <cmsc:rights nodeNumber="${channelNumber}" var="rights"/>

			          <mm:field name="name" jspvar="channelName" write="false"/>
						 <c:set var="channelIcon" value="/editors/gfx/icons/type/contentchannel_${rights}.png"/>
						 <c:set var="channelIconMessage"><fmt:message key="role.${rights}" /></c:set>
						 <c:set var="channelUrl" value="Content.do?parentchannel=${channelNumber}"/>
						 
						 <img src="<cmsc:staticurl page="${channelIcon}"/>" align="top" alt="${channelIconMessage}" />
                 	<a href="${channelUrl}">${channelName}</a>
               </td>
			      </mm:relatednodes>
			   </c:otherwise>
			</c:choose>
		</td>
		<td width="10" onMouseDown="objClick(this);">
			<c:set var="status" value="waiting"/>
			<mm:relatednodes type="workflowitem">
				<c:set var="status"><mm:field name="status"/></c:set>
			</mm:relatednodes>
			<c:if test="${status == 'waiting'}">
				<mm:listnodes type="remotenodes" constraints="sourcenumber=${number}">
					<c:set var="status" value="onlive"/>
				</mm:listnodes>
			</c:if>
			<img src="../gfx/icons/status_${status}.png" alt="<fmt:message key="content.status" />: <fmt:message key="content.status.${status}" />" title="<fmt:message key="content.status" />: <fmt:message key="content.status.${status}" />" />
		</td>

         </tr>
   </mm:listnodes>
      </tbody>
   </table>
   <%@include file="../pages.jsp" %>
      </div>
   </div>
   </mm:node>
</mm:cloud>
	</body>
</html:html>
</mm:content>