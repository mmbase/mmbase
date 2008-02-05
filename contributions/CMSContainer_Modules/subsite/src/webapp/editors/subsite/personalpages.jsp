<%@page language="java" contentType="text/html;charset=utf-8"%>
<%@include file="globals.jsp" %>
<%@page import="com.finalist.cmsc.repository.ContentElementUtil,
                 com.finalist.cmsc.repository.RepositoryUtil,
                 java.util.ArrayList"%>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<cmscedit:head title="search.title">
	<script src="<cmsc:staticurl page='/editors/repository/content.js'/>" type="text/javascript"></script>
	<script src="<cmsc:staticurl page='/editors/repository/search.js'/>" type="text/javascript"></script>
</cmscedit:head>
<body>
<script type="text/javascript">
    <c:if test="${not empty param.message}">
    addLoadEvent(alert('${param.message}'));
    </c:if>
    <c:if test="${not empty param.refreshchannel}">
    addLoadEvent(refreshChannels);
    </c:if>
    addLoadEvent(alphaImages);
</script>

<mm:import id="searchinit"><c:url value='/editors/repository/SearchInitAction.do'/></mm:import>
<mm:import externid="action">search</mm:import><%-- either: search, link, of select --%>
<mm:import externid="mode" id="mode">basic</mm:import>
<mm:import externid="results" jspvar="nodeList" vartype="List" />
<mm:import externid="resultCount" jspvar="resultCount" vartype="Integer">0</mm:import>
<mm:import externid="offset" jspvar="offset" vartype="Integer">0</mm:import>
<!--
<mm:import externid="returnurl"/>
<mm:import externid="parentchannel" jspvar="parentchannel"/>
-->

<mm:import externid="subsite" jspvar="subsite" from="parameters" vartype="Integer" />


<mm:cloud jspvar="cloud" loginpage="../../editors/login.jsp">

<div class="content">
   <div class="tabs">
      <div class="tab_active">
         <div class="body">
            <div>
               <a href="#" onclick="selectTab('basic');"><fmt:message key="site.personal.personalpages" /></a>
            </div>
         </div>
      </div>
   </div>
</div><!--


List of pagesNodes:<br><br>
<ul>
<c:forEach var="results" items="${results}">
  <li><b>${results}</b></li>
</c:forEach>
</ul>



--><div class="editor">
<html:form action="/editors/subsite/SubSiteAction" method="post">
	<html:hidden property="action" value="${action}"/>
	<html:hidden property="subsite" value="${subsite}"/>
	<html:hidden property="search" value="true"/>
	<html:hidden property="offset"/>
	<html:hidden property="order"/>
	<html:hidden property="direction"/>
	
	<table>
	   <tr>
	      <td><fmt:message key="subsitedelete.subtitle" /></td>
	      <td colspan="3"><html:text property="title" style="width:200px"/></td><!--
	      </tr>
      <tr>
	      <td><fmt:message key="searchform.keywords" /></td>
	      <td colspan="3"><html:text property="keywords" style="width:200px"/></td>
	      --><td>
	      <input type="submit" class="button" name="submitButton" onclick="setOffset(0);" value="<fmt:message key="site.personal.search" />"/>
	      </td>
	   </tr>
	</table>
</html:form>
</div>





<div class="editor">
<a href="../subsite/PersonalPageCreate.do?parentpage=${subsite}">
<img src="../gfx/icons/new.png" width="16" height="16" title="<fmt:message key="site.personal.new.page" />"
                                                    alt="<fmt:message key="site.personal.new.page" />"/> 
Nieuwe persoonlijke pagina</a>
<br /><br />

<div class="ruler_green"><div><fmt:message key="site.personal.personalpages"/></div></div>
<div class="body">


<c:set var="listSize" value="${fn:length(results)}"/>
<c:set var="resultsPerPage" value="50"/>
<c:set var="offset" value="${param.offset}"/>
<c:set var="extraparams" value="&parentchannel=${param.parentchannel}"/>

<mm:isempty referid="results" inverse="true">
<%@ include file="../pages.jsp" %>
</mm:isempty>

<table>
<thead>
    <tr>
        <th>Icons</th>
        <th><a href="#" class="headerlink" onclick="orderBy('title');" >Title</a></th>
        <th>Edit Elements</th>
        <th><a href="#" class="headerlink" onclick="orderBy('publishdate');" >Publish Date</th>
        <th><a href="#" class="headerlink" onclick="orderBy('creationdate');" >Creation Date</th>
    </tr>
</thead>
<tbody class="hover">

<mm:list referid="results" jspvar="node" max="${resultsPerPage}" offset="${offset*resultsPerPage}">
   <mm:field name="personalpage.number" id="number">
	   <mm:node number="${number}">
		   <tr <mm:even inverse="true">class="swap"</mm:even>>
		   <td style="white-space: nowrap;">
		   
		   <mm:field name="number"  write="false" id="nodenumber">
            <a href="<cmsc:contenturl number="${nodenumber}"/>" 
            title="<fmt:message key="searchform.icon.preview.title" />" target="_blank"><img src="../gfx/icons/preview.png" width="16" height="16"
																             title="<fmt:message key="searchform.icon.preview.title" />"
																             alt="<fmt:message key="searchform.icon.preview.title" />"/></a>
         
      	   <a href="../subsite/SubSiteEdit.do?number=${nodenumber}"
		       title="<fmt:message key="content.edit" />"><img src="../gfx/icons/edit.png" width="16" height="16"
		                                                       title="<fmt:message key="content.edit" />"
		                                                       alt="<fmt:message key="content.edit" />"/></a>
		   <a href="../subsite/SubSiteDelete.do?number=${nodenumber}"
		       title="<fmt:message key="content.delete" />"><img src="../gfx/icons/delete.png" width="16" height="16"
		                                                       title="<fmt:message key="content.delete" />"
		                                                       alt="<fmt:message key="content.delete" />"/></a>
		   </mm:field>
		   </td>
		   <td>
		      <b><mm:field name="title" /></b>
		   </td>
		   <td>
		   <a href="../subsite/PersonalPageElements.do?personalpage=<mm:field name="number" />">Edit Articles</a>
		   </td>
		   <td>
		      <mm:field name="publishdate"><cmsc:dateformat displaytime="true"/></mm:field>
		   </td>
		   <td>
            <mm:field name="creationdate"><cmsc:dateformat displaytime="true"/></mm:field>
         </td>
		   
		   </tr>
	   </mm:node>
   </mm:field>
</mm:list>
</tbody>
</table>

<mm:isempty referid="results" inverse="true">
<%@ include file="../pages.jsp" %>
</mm:isempty>
   
<br />

<%-- Now print if no results --%>
<mm:isempty referid="results">
   <fmt:message key="searchform.searchpages.nonefound" />
</mm:isempty>
</div>
</div>

</mm:cloud>

</body>
</html:html>
</mm:content>