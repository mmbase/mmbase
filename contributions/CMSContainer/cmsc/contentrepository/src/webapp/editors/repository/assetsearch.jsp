<%@page language="java" contentType="text/html;charset=utf-8"
%><%@include file="globals.jsp"
%><%@page import="com.finalist.cmsc.repository.AssetElementUtil,
                 com.finalist.cmsc.repository.RepositoryUtil,
                 java.util.ArrayList"
%><%@ page import="com.finalist.cmsc.security.UserRole"
%><%@ page import="com.finalist.cmsc.security.SecurityUtil"
%><mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">

<mm:import externid="action">search</mm:import><%-- either: search, link, of select --%>
<mm:import externid="mode" id="mode">basic</mm:import>
<mm:import externid="returnurl"/>
<mm:import externid="parentchannel" jspvar="parentchannel"/>
<mm:import externid="assettypes" jspvar="assettypes"><%= AssetElementUtil.ASSETELEMENT %></mm:import>
<mm:import externid="results" jspvar="nodeList" vartype="List" />
<mm:import externid="offset" jspvar="offset" vartype="Integer">0</mm:import>
<mm:import externid="resultCount" jspvar="resultCount" vartype="Integer">0</mm:import>

<cmscedit:head title="search.title">
      <script src="../repository/asset.js" language="JavaScript" type="text/javascript"></script>
      <script src="search.js" type="text/javascript"></script>
            <script type="text/javascript">
               function showEditItems(id){
                  document.getElementById('asset-info-'+id).style.display = 'block';
                  document.getElementById('asset-info-'+id).style.display = 2001;
               }
               function hideEditItems(id){
                  document.getElementById('asset-info-'+id).style.display = 'none';
                  document.getElementById('asset-info-'+id).style.display = 2000;
               }
               function changeMode(offset){
                   if(offset==null){offset=0;}
                   var assetsMode = document.getElementsByTagName("option");3
                   for(i = 0; i < assetsMode.length; i++){
                      if(assetsMode[i].selected & assetsMode[i].id=="a_list"){
                         document.location.href = 'AssetSearchAction.do?type=asset&direction=down&offset='+offset;
                      }else if(assetsMode[i].selected & assetsMode[i].id=="a_thumbnail"){
                         document.location.href = 'AssetSearchAction.do?type=asset&direction=down&show=0&offset='+offset;
                      }
                   }
                }
            </script>
    <c:if test="${not empty requestScope.refreshChannels}">
        <script>
        refreshFrame('channels');
        </script>
    </c:if>
</cmscedit:head>
<body>
<mm:import id="assetsearchinit"><c:url value='/editors/repository/AssetSearchInitAction.do'/></mm:import>


<mm:cloud jspvar="cloud" loginpage="../../editors/login.jsp">

   <div class="tabs">
    <!-- active TAB -->
 <div class="${assettypes == 'content'?'tab_active':'tab'}">
      <div class="body">
         <div>
            <a href="SearchInitAction.do"><fmt:message key="content.search.title" /></a>
         </div>
      </div>
   </div>
    <div class="${(assettypes == 'assetelement' || assettypes == null)?'tab_active':'tab'}">
        <div class="body">
            <div>
                <a href="AssetSearchInitAction.do"><fmt:message key="asset.search.title"/></a>
            </div>
        </div>
    </div>

</div>
   <div class="editor">
   <br />
      <div class="body">

         <html:form action="/editors/repository/AssetSearchAction" method="post">
            <html:hidden property="action" value="${action}"/>
            <html:hidden property="mode"/>
            <html:hidden property="search" value="true"/>
            <html:hidden property="offset"/>
            <html:hidden property="order"/>
            <html:hidden property="direction"/>
            <input type="hidden" name="deleteAssetRequest"/>
            <mm:present referid="returnurl"><input type="hidden" name="returnurl" value="<mm:write referid="returnurl"/>"/></mm:present>
                     <mm:compare referid="mode" value="advanced" >
                        <a href="#" onclick="selectTab('basic');"><input type="button" class="button" value="<fmt:message key="search.simple.search" />"/></a>
                     </mm:compare>
                <mm:compare referid="mode" value="basic" >
                        <a href="#" onclick="selectTab('advanced');"><input type="button" class="button" value="<fmt:message key="search.advanced.search" />"/></a>
                     </mm:compare>
            <table>
               <tr>
                  <td style="width:105px"><fmt:message key="searchform.title" /></td>
                  <td colspan="5"><html:text property="title" style="width:200px"/></td>
               </tr>
             <mm:compare referid="mode" value= "advanced">
               <tr>
                  <td style="width:105px"><fmt:message key="searchform.assettype" /></td>
                  <td>
                     <html:select property="assettypes" onchange="selectAssettype('${searchinit}');" >
                        <html:option value="assetelement">&lt;<fmt:message key="searchform.assettypes.all" />&gt;</html:option>
                        <html:optionsCollection name="typesList" value="value" label="label"/>
                     </html:select>
                  </td>
               </tr>
                  <tr>
                     <td></td>
                     <td><b><fmt:message key="searchform.dates" /></b></td>
                     <td></td>
                     <td><b><fmt:message key="searchform.users" /></b></td>
                     <td></td>
                     <td>
                        <mm:compare referid="assettypes" value="assetelement" inverse="true">
                           <fmt:message key="searchform.searchfor">
                              <fmt:param><mm:nodeinfo nodetype="${assettypes}" type="guitype"/></fmt:param>
                           </fmt:message>
                        </mm:compare>
                     </td>
                  </tr>
                  <tr valign="top">
                     <td><fmt:message key="searchform.creationdate" /></td>
                     <td>
                        <html:select property="creationdate" size="1">
                           <html:option value="0"> - </html:option>
                           <html:option value="-1"><fmt:message key="searchform.pastday" /></html:option>
                           <html:option value="-7"><fmt:message key="searchform.pastweek" /></html:option>
                           <html:option value="-31"><fmt:message key="searchform.pastmonth" /></html:option>
                           <html:option value="-120"><fmt:message key="searchform.pastquarter" /></html:option>
                           <html:option value="-365"><fmt:message key="searchform.pastyear" /></html:option>
                        </html:select>
                     </td>
                     <td><fmt:message key="searchform.personal" /></td>
                     <td>
                        <html:select property="personal" size="1">
                           <html:option value=""> - </html:option>
                           <html:option value="lastmodifier"><fmt:message key="searchform.personal.lastmodifier" /></html:option>
                           <html:option value="author"><fmt:message key="searchform.personal.author" /></html:option>
                        </html:select>
                     </td>
                     <td rowspan="5">
                     <% ArrayList fields = new ArrayList(); %>
                        <mm:compare referid="assettypes" value="assetelement" inverse="true">
                           <table>
                              <mm:fieldlist nodetype="${assettypes}">
                                 <%-- check if the field is from assetelement --%>
                                 <% boolean showField = true; %>
                                 <mm:fieldinfo type="name" id="fname">
                                     <mm:fieldlist nodetype="assetelement">
                                         <mm:fieldinfo type="name" id="cefname">
                                            <mm:compare referid="fname" referid2="cefname">
                                               <% showField=false; %>
                                            </mm:compare>
                                         </mm:fieldinfo>
                                     </mm:fieldlist>
                                 </mm:fieldinfo>
                                 <% if (showField) { %>
                                    <tr rowspan="5">
                                       <td height="22">
                                          <mm:fieldinfo type="guiname" jspvar="guiname"/>:
                                          <mm:fieldinfo type="name" jspvar="name" write="false">
                                             <% fields.add(assettypes + "." + name); %>
                                          </mm:fieldinfo>
                                    </tr>
                                 <% } %>
                              </mm:fieldlist>
                           </table>
                        </mm:compare>
                     </td>
                     <td rowspan="5">
                        <mm:compare referid="assettypes" value="assetelement" inverse="true">
                           <table>
                              <% for (int i = 0; i < fields.size(); i++) {
                                 String field = (String) fields.get(i); %>
                                 <tr>
                                    <td>
                                       <input type="text" name="<%= field %>" value="<%= (request.getParameter(field) == null)? "" :request.getParameter(field) %>" />
                                    </td>
                                 </tr>
                              <% } %>
                           </table>
                        </mm:compare>
                     </td>
                  </tr>
                  <tr>
                     <td><fmt:message key="searchform.lastmodifieddate" /></td>
                     <td>
                        <html:select property="lastmodifieddate" size="1">
                           <html:option value="0"> - </html:option>
                           <html:option value="-1"><fmt:message key="searchform.pastday" /></html:option>
                           <html:option value="-7"><fmt:message key="searchform.pastweek" /></html:option>
                           <html:option value="-31"><fmt:message key="searchform.pastmonth" /></html:option>
                           <html:option value="-120"><fmt:message key="searchform.pastquarter" /></html:option>
                           <html:option value="-365"><fmt:message key="searchform.pastyear" /></html:option>
                        </html:select>
                     </td>
                     <td>
                        <mm:hasrank minvalue="siteadmin">
                           <fmt:message key="searchform.useraccount" />
                        </mm:hasrank>
                     </td>
                     <td>
                        <mm:hasrank minvalue="siteadmin">
                           <html:select property="useraccount" size="1">
                              <html:option value=""> - </html:option>
                               <mm:listnodes type='user' orderby='username'>
                                   <mm:field name="username" id="useraccount" write="false"/>
                                  <html:option value="${useraccount}"> <mm:field name="firstname" /> <mm:field name="prefix" /> <mm:field name="surname" /> </html:option>
                               </mm:listnodes>
                           </html:select>
                        </mm:hasrank>
                     </td>
                  </tr>
                  <tr>
                     <td><fmt:message key="searchform.publishdate" /></td>
                     <td>
                        <html:select property="publishdate" size="1">
                           <html:option value="365"><fmt:message key="searchform.futureyear" /></html:option>
                           <html:option value="120"><fmt:message key="searchform.futurequarter" /></html:option>
                           <html:option value="31"><fmt:message key="searchform.futuremonth" /></html:option>
                           <html:option value="7"><fmt:message key="searchform.futureweek" /></html:option>
                           <html:option value="1"><fmt:message key="searchform.futureday" /></html:option>
                           <html:option value="0"> - </html:option>
                           <html:option value="-1"><fmt:message key="searchform.pastday" /></html:option>
                           <html:option value="-7"><fmt:message key="searchform.pastweek" /></html:option>
                           <html:option value="-31"><fmt:message key="searchform.pastmonth" /></html:option>
                           <html:option value="-120"><fmt:message key="searchform.pastquarter" /></html:option>
                           <html:option value="-365"><fmt:message key="searchform.pastyear" /></html:option>
                        </html:select>
                     </td>
                     <td></td>
                     <td><b><fmt:message key="searchform.other" /></b></td>
                  </tr>
                  <tr>
                     <td><fmt:message key="searchform.expiredate" /></td>
                     <td>
                        <html:select property="expiredate" size="1">
                           <html:option value="365"><fmt:message key="searchform.futureyear" /></html:option>
                           <html:option value="120"><fmt:message key="searchform.futurequarter" /></html:option>
                           <html:option value="31"><fmt:message key="searchform.futuremonth" /></html:option>
                           <html:option value="7"><fmt:message key="searchform.futureweek" /></html:option>
                           <html:option value="1"><fmt:message key="searchform.futureday" /></html:option>
                           <html:option value="0"> - </html:option>
                           <html:option value="-1"><fmt:message key="searchform.pastday" /></html:option>
                           <html:option value="-7"><fmt:message key="searchform.pastweek" /></html:option>
                           <html:option value="-31"><fmt:message key="searchform.pastmonth" /></html:option>
                           <html:option value="-120"><fmt:message key="searchform.pastquarter" /></html:option>
                           <html:option value="-365"><fmt:message key="searchform.pastyear" /></html:option>
                        </html:select>
                     </td>
                     <td><fmt:message key="searchform.number" /></td>
                     <td><html:text property="objectid"/></td>
                  </tr>
                  <tr>
                     <td>
                     </td>
                     <td></td>
                     <td nowrap>
                        <mm:compare referid="action" value="link">
                           <mm:write write="false" id="showTreeOption" value="true" />
                        </mm:compare>

                        <mm:compare referid="action" value="selectforwizard">
                           <mm:write write="false" id="showTreeOption" value="true" />
                        </mm:compare>
                        <mm:present referid="showTreeOption">
                           <fmt:message key="searchform.select.channel" />

                     <a href="<c:url value='/editors/repository/select/SelectorChannel.do' />"
                        target="selectChannel" onclick="openPopupWindow('selectChannel', 340, 400)">
                           <img src="<cmsc:staticurl page='/editors/gfx/icons/select.png'/>" alt="<fmt:message key="searchform.select.channel" />"/></a>
                           <a href="#" onClick="selectChannel('', '');" ><img src="<cmsc:staticurl page='/editors/gfx/icons/erase.png'/>" alt="<fmt:message key="searchform.clear.channel.button" />" /></a>
                        </mm:present>
                     </td>
                     <td>
                        <mm:present referid="showTreeOption">
                        <html:hidden property="parentchannel" />
                          <html:hidden property="parentchannelpath"/>
                        <input type="text" name="parentchannelpathdisplay" disabled value="${SearchForm.parentchannelpath}"/><br />
                        </mm:present>
                     </td>
                  </tr>

               </mm:compare>
            <tr>
                     <td></td>
                     <td>
                        <input type="submit" class="button" name="submitButton" onclick="setOffset(0);" value="<fmt:message key="searchform.submit" />"/>
                     </td>
                  </tr>
            </table>
         </html:form>
      </div>
   </div>

   <div class="editor" style="height:500px">
   <div class="ruler_green"><div><fmt:message key="searchform.results" /></div></div>


   <div>
      <select name="assesMode" onchange="javascript:changeMode(${param.offset})">
         <c:if test="${empty show}">
            <option id="a_list" selected="selected">list</option>
            <option id = "a_thumbnail" >thumbnail</option>
         </c:if>
         <c:if test="${!empty show}">
            <option id="a_list">list</option>
            <option id = "a_thumbnail" selected="selected" >thumbnail</option>
         </c:if>
      </select>
   </div>

<c:if test="${empty show}">
   <div class="body">
<!-- we check to see if we have workflow, this is done by looking if the editors for the workflow are on the HD -->
<c:set var="hasWorkflow" value="false"/>
<mm:haspage page="/editors/workflow">
   <c:set var="hasWorkflow" value="true"/>
</mm:haspage>



   <%-- Now print if no results --%>
   <mm:isempty referid="results">
      <fmt:message key="searchform.searchpages.nonefound" />
   </mm:isempty>

   <%-- Now print the results --%>
   <mm:node number="<%= RepositoryUtil.ALIAS_TRASH %>">
      <mm:field id="trashnumber" name="number" write="false"/>
   </mm:node>

   <mm:list referid="results">
      <mm:first>
         <c:set var="extraparams" value="&direction=${param.direction}" />
         <%@include file="searchpages.jsp" %>
<mm:hasrank minvalue="siteadmin">
               <c:if test="${fn:length(results) >1}">
               <div align="left">
                  <input type="submit" class="button" name="massdelete" 
                        onclick="javascript:deleteAsset('massdelete','<fmt:message key="recyclebin.massremoveconfirm"/>')"
                        value="<fmt:message key="asset.delete.massdelete" />"/>
               </div>
               </c:if>
            </mm:hasrank>
         <form action="" name="linkForm" method="post">
            
            <table>
            <thead>
               <tr>
                  <th>
                     <mm:present referid="returnurl"><input type="hidden" name="returnurl" value="<mm:write referid="returnurl"/>"/></mm:present>  
                      <input type="checkbox" onclick="selectAll(this.checked, 'linkForm', 'chk_');" value="on" name="selectall" />
                  </th>
                  <th></th>
                  <th><a href="javascript:orderBy('otype')" class="headerlink" ><fmt:message key="locate.typecolumn" /></a></th>
                  <th><a href="javascript:orderBy('title')" class="headerlink" ><fmt:message key="locate.titlecolumn" /></a></th>
                  <th><fmt:message key="locate.creationchannelcolumn" /></th>
                  <th><a href="javascript:orderBy('creator')" class="headerlink" ><fmt:message key="locate.authorcolumn" /></th>
                  <th><a href="javascript:orderBy('lastmodifieddate')" class="headerlink" ><fmt:message key="locate.lastmodifiedcolumn" /></th>
                  <th><a href="javascript:orderBy('number')" class="headerlink" ><fmt:message key="locate.numbercolumn" /></th>
               </tr>
            </thead>

            <tbody class="hover">
      </mm:first>

   <mm:field name="${assettypes}.number" id="number" write="false">
      <mm:node number="${number}">
         <mm:relatednodes role="creationrel" type="contentchannel">
            <c:set var="creationRelNumber"><mm:field name="number" id="creationnumber"/></c:set>
            <mm:compare referid="trashnumber" referid2="creationnumber">
                <c:set var="channelName"><fmt:message key="search.trash" /></c:set>
                <c:set var="channelIcon" value="/editors/gfx/icons/trashbin.png"/>
                <c:set var="channelIconMessage"><fmt:message key="search.trash" /></c:set>
                <c:set var="channelUrl" value="../recyclebin/index.jsp"/>
            </mm:compare>
            <mm:field name="number" jspvar="channelNumber" write="false"/>
            <cmsc:rights nodeNumber="${channelNumber}" var="rights"/>
            <mm:compare referid="trashnumber" referid2="creationnumber" inverse="true">
                <mm:field name="name" jspvar="channelName" write="false"/>
                <c:set var="channelIcon" value="/editors/gfx/icons/type/contentchannel_${rights}.png"/>
                <c:set var="channelIconMessage"><fmt:bundle basename="cmsc-security"><fmt:message key="role.${rights}" /></fmt:bundle></c:set>
                <c:set var="channelUrl" value="Asset.do?parentchannel=${channelNumber}"/>
            </mm:compare>
         </mm:relatednodes>
         <tr <mm:even inverse="true">class="swap"</mm:even>>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);">
         
               <mm:compare referid="action" value="link">
                   <input type="checkbox" value="<mm:field name="number" />" name="chk_<mm:field name="number" />" onClick="document.forms['linkForm'].elements.selectall.checked=false;"/>
               </mm:compare>
               <mm:compare referid="action" value="link" inverse="true">
                  <c:if test="${creationRelNumber == trashnumber && rights == 'webmaster' && fn:length(results) >1}">
                      <input type="checkbox" value="permanentDelete:<mm:field name="number" />" name="chk_<mm:field name="number" />" onClick="document.forms['linkForm'].elements.selectall.checked=false;"/>
                  </c:if>
                  <c:if test="${creationRelNumber != trashnumber && (rights == 'writer' || rights == 'chiefeditor' || rights == 'editor' || rights == 'webmaster') && fn:length(results) >1}">
                    <input type="checkbox" value="moveToRecyclebin:<mm:field name="number" />" name="chk_<mm:field name="number" />" onClick="document.forms['linkForm'].elements.selectall.checked=false;"/>
                  </c:if>
               </mm:compare>
            </td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);">
               <%@ include file="searchIconsBar.jspf" %>
            </td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);">
               <mm:nodeinfo type="guitype"/>
            </td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);">
               <c:set var="assettype" ><mm:nodeinfo type="guitype"/></c:set>
               <mm:field id="title" write="false" name="title"/>
               <c:if test="${assettype == 'URL'}">
                  <c:set var="title" ><mm:field name="name"/></c:set>
               </c:if>
               <c:if test="${fn:length(title) > 50}">
                  <c:set var="title">${fn:substring(title,0,49)}...</c:set>
               </c:if>
               ${title}
            </td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);">
                <img src="<cmsc:staticurl page="${channelIcon}"/>" align="top" alt="${channelIconMessage}" />
                  <mm:compare referid="action" value="search">
                     <a href="${channelUrl}">${channelName}</a>
                  </mm:compare>
                  <mm:compare referid="action" value="search" inverse="true">
                     ${channelName}
                  </mm:compare>
            </td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);"><mm:field name="creator" /></td>
            <td style="white-space: nowrap;" onMouseDown="objClick(this);"><mm:field name="lastmodifieddate"><cmsc:dateformat displaytime="true" /></mm:field></td>
            <td  style="white-space: nowrap;" onMouseDown="objClick(this);"><mm:field name="number"/></td>
            <c:if test="${hasWorkflow}">
               <td width="10" onMouseDown="objClick(this);">
                  <c:set var="status" value="waiting"/>
                  <mm:relatednodes type="workflowitem" constraints="type='asset'">
                     <c:set var="status"><mm:field name="status"/></c:set>
                  </mm:relatednodes>
                  <c:if test="${status == 'waiting'}">
                     <mm:listnodes type="remotenodes" constraints="sourcenumber=${number}">
                        <c:set var="status" value="onlive"/>
                     </mm:listnodes>
                  </c:if>
                     <img src="../gfx/icons/status_${status}.png"
                        alt="<fmt:message key="asset.status" />: <fmt:message key="asset.status.${status}" />"
                        title="<fmt:message key="asset.status" />: <fmt:message key="asset.status.${status}" />"/>
               </td>
            </c:if>
         </tr>
      </mm:node>
   </mm:field>
   <mm:last>
   </tbody>
   </table>
   </form>
   <mm:hasrank minvalue="siteadmin">
               <c:if test="${fn:length(results) >1}">
               <div align="left">
                  <input type="submit" class="button" name="massdelete" 
                        onclick="javascript:deleteAsset('massdelete','<fmt:message key="recyclebin.massremoveconfirm"/>')"
                        value="<fmt:message key="asset.delete.massdelete" />"/>
               </div>
               </c:if>
            </mm:hasrank>
      <%@include file="searchpages.jsp" %>
   </mm:last>
   </mm:list>
      </div>
</c:if>

<c:if test="${!empty show}">
<div class="body">

<!-- we check to see if we have workflow, this is done by looking if the editors for the workflow are on the HD -->
<c:set var="hasWorkflow" value="false"/>
<mm:haspage page="/editors/workflow">
   <c:set var="hasWorkflow" value="true"/>
</mm:haspage>

   <%-- Now print if no results --%>
   <mm:isempty referid="results">
      <fmt:message key="searchform.searchpages.nonefound" />
   </mm:isempty>

   <%-- Now print the results --%>
   <mm:node number="<%= RepositoryUtil.ALIAS_TRASH %>">
      <mm:field id="trashnumber" name="number" write="false"/>
   </mm:node>

   <mm:list referid="results">
      <mm:first>
      <c:set var="extraparams" value="&direction=${param.direction}&show=0"/>
         <%@include file="searchpages.jsp" %>
         <form name="linkForm">
      </mm:first>

   <mm:field name="${assettypes}.number" id="number" write="false">
      <mm:node number="${number}">

         <mm:relatednodes role="creationrel" type="contentchannel">
            <c:set var="creationRelNumber"><mm:field name="number" id="creationnumber"/></c:set>
            <mm:compare referid="trashnumber" referid2="creationnumber">
                <c:set var="channelName"><fmt:message key="search.trash" /></c:set>
                <c:set var="channelIcon" value="/editors/gfx/icons/trashbin.png"/>
                <c:set var="channelIconMessage"><fmt:message key="search.trash" /></c:set>
                <c:set var="channelUrl" value="../recyclebin/index.jsp"/>
            </mm:compare>
            <mm:field name="number" jspvar="channelNumber" write="false"/>
            <cmsc:rights nodeNumber="${channelNumber}" var="rights"/>
            <mm:compare referid="trashnumber" referid2="creationnumber" inverse="true">
                <mm:field name="name" jspvar="channelName" write="false"/>
                <c:set var="channelIcon" value="/editors/gfx/icons/type/contentchannel_${rights}.png"/>
                <c:set var="channelIconMessage"><fmt:bundle basename="cmsc-security"><fmt:message key="role.${rights}" /></fmt:bundle></c:set>
                <c:set var="channelUrl" value="Asset.do?parentchannel=${channelNumber}"/>
            </mm:compare>
         </mm:relatednodes>

         <div style="width:160px; height:180px;float:left;text-align:center;" onMouseOut="javascript:hideEditItems(<mm:field name='number'/>)" onMouseOver="showEditItems(<mm:field name='number'/>)">
            <div style="clear:both;float:left;width:77%;height:33px;">
               <div class="asset-info" id="asset-info-<mm:field name='number'/>" style="display: none; position: relative; border: 1px solid #eaedff" >
               <%@ include file="searchIconsBar.jspf" %>
               </div>
            </div>
            <div style="width:100%;height:100px;text-align:left;padding:0px 10px 5px 5px;vertical-align:middle;display:block;float:left;" >
               <a href="javascript:showInfo(<mm:field name="number" />)">
                  <c:set var="typedef" ><mm:nodeinfo type="guitype"/></c:set>
                  <c:if test="${typedef eq 'Image'}">
                     <img src="<mm:image template="s(120x100)"/>" alt=""/>
                  </c:if>
                  <c:if test="${typedef eq 'Attachment'}">
                     <img src="../gfx/alert_green_left.gif" alt=""/>change
                  </c:if>
                   <c:if test="${typedef eq 'URL'}">
                     need to add
                  </c:if>
               </a>
               <div style="width:80%;text-align:center;margin:0px 0px 0px 0px;padding-top:0px;padding-right:0px;overflow-x:hidden;">
                  <c:set var="typedef" ><mm:nodeinfo type="guitype"/></c:set>
                  <c:if test="${typedef eq 'Image'}">
                     <mm:field name="title"/><br/><mm:field name="itype"/>
                  </c:if>
                  <c:if test="${typedef eq 'Attachment'}">
                     <mm:field name="title"/>
                  </c:if>
                   <c:if test="${typedef eq 'URL'}">
                     <mm:field name="name"/>
                  </c:if>
               </div>
            </div>
            </div>
      </mm:node>
   </mm:field>
   <mm:last>
   </form>
      <%@include file="searchpages.jsp" %>
   </mm:last>
   </mm:list>
</div>
</c:if>
   </div>
</mm:cloud>

   </body>
</html:html>
</mm:content>