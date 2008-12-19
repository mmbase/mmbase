<%@ page language="java" contentType="text/html;charset=utf-8"
%><%@ include file="globals.jsp"
%><%@ page import="com.finalist.cmsc.repository.RepositoryUtil"
%><%@ page import="com.finalist.cmsc.security.*"
%><mm:content type="text/html" encoding="UTF-8" expires="0">

   <mm:cloud jspvar="cloud" rank="basic user" loginpage="../login.jsp">
      <html:html xhtml="true">
         <mm:import externid="parentchannel" jspvar="parentchannel" vartype="Integer" from="parameters" required="true"/>
         <mm:import jspvar="returnurl" id="returnurl">/editors/repository/Asset.do?type=asset&parentchannel=<mm:write
         referid="parentchannel"/>&direction=down</mm:import>

         <cmscedit:head title="images.title">
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
         var assetsMode = document.getElementsByTagName("option");
         for(i = 0; i < assetsMode.length; i++){
            if(assetsMode[i].selected & assetsMode[i].id=="a_list"){
                if(document.getElementById("chk_showImageOnly").checked){
                    document.getElementById("chk_showImageOnly").checked=false;
                    offset=0;
                }
                document.location.href = 'Asset.do?type=asset&parentchannel=<mm:write referid="parentchannel"/>&direction=down&offset='+offset+'&imageOnly=no';
            }else if(assetsMode[i].selected & assetsMode[i].id=="a_thumbnail"){
               if(document.getElementById("chk_showImageOnly").checked==false){
                   document.location.href = 'Asset.do?type=asset&parentchannel=<mm:write referid="parentchannel"/>&direction=down&show=0&offset='+offset+'&imageOnly=no';
                    }
                    if(document.getElementById("chk_showImageOnly").checked==true){
                        document.location.href = 'Asset.do?type=asset&parentchannel=<mm:write referid="parentchannel"/>&direction=down&show=0&offset='+offset+'&imageOnly=yes';
                    }
            }
         }
      }
      function showImageOnly(){
          var offset=0;
          var assetsMode = document.getElementById("assetMode");
          assetsMode.selectedIndex=1;
         if(document.getElementById("chk_showImageOnly").checked == true){
	         document.location.href = 'Asset.do?type=asset&parentchannel=<mm:write referid="parentchannel"/>&direction=down&show=0&offset='+offset+'&imageOnly=yes'
         }else{
            document.location.href = 'Asset.do?type=asset&parentchannel=<mm:write referid="parentchannel"/>&direction=down&show=0&offset='+offset+'&imageOnly=no';
          }
      }
   </script>
   <script src="../repository/asset.js" language="JavaScript" type="text/javascript"></script>
         </cmscedit:head>

         <div class="editor">
            <div class="body">
               <!-- check to see if we have workflow, this is done by looking if the editors for the workflow are on the HD -->
               <c:set var="hasWorkflow" value="false"/>
               <mm:haspage page="/editors/workflow">
                  <c:set var="hasWorkflow" value="true"/>
               </mm:haspage>

               <mm:node number="$parentchannel" jspvar="parentchannelnode">
               <% UserRole role = RepositoryUtil.getRole(cloud, parentchannelnode, false); %>
               <%@ include file="assetupload.jsp" %>
               <% if (role != null && SecurityUtil.isWriter(role)) { %>
                  <ul class="shortcuts">
                     <li class="new" style="text-decoration: none;"><fmt:message key="asset.new"/>
                        <form name="initForm" action="../WizardInitAction.do" method="post" style="display:inline;text-decoration:none">
                           <input type="hidden" name="action" value="create"/>
                           <input type="hidden" name="creation" value="<mm:write referid="parentchannel" />"/>
                           <input type="hidden" name="returnurl" value="<%= returnurl %>"/>
                           <input type="hidden" name="order" value="${orderby}" />
                           <input type="hidden" name="direction" value="${direction}"/>
                           <input type="hidden" name="offset" value="${param.offset}"/>
                           <input type="hidden" name="assettype" value="urls"/>
                           <input type="submit" name="submitButton" value="<fmt:message key="asset.create" />" class="button"/>
                        </form>
                     </li>
                  </ul>
               <% } %>
            </div>

            <div class="ruler_green">
               <div>
                  <fmt:message key="asset.asset">
                     <fmt:param><mm:field name="path"/></fmt:param>
                  </fmt:message>
               </div>
            </div>

            <div style="padding-left:11px">
               <select name="assesMode" id="assetMode" onchange="javascript:changeMode(${param.offset})">
                  <c:if test="${empty show}">
                     <option id="a_list" selected="selected">list</option>
                     <option id = "a_thumbnail" >thumbnail</option>
                  </c:if>
                  <c:if test="${!empty show}">
                     <option id="a_list">list</option>
                     <option id = "a_thumbnail" selected="selected" >thumbnail</option>
                  </c:if>
               </select>
            <div style="padding-left:100px;display:inline;font-size:12px;">
               <input type="checkbox" name="showImageOnly" id="chk_showImageOnly" <c:if test="${imageOnly eq 'yes'}">checked="checked"</c:if> onclick="javascript:showImageOnly()"/><fmt:message key="asset.image.show"/>
            </div>
            </div>

            <c:if test="${empty show}">
               <div class="body" >
                  <mm:import externid="elements" from="request" required="true"/>
                  <mm:import externid="elementCount" from="request" vartype="Integer">0</mm:import>
                  <mm:import externid="resultsPerPage" from="request" vartype="Integer">25</mm:import>
                  <c:set var="listSize" value="${elementCount}"/>
                  <c:set var="offset" value="${param.offset}"/>
                  <c:if test="${imageOnly eq 'no'}">
                  <c:set var="extraparams" value="&direction=${param.direction}&parentchannel=${param.parentchannel}&imageOnly=no"/>
                  </c:if>
                  <c:if test="${imageOnly eq 'yes'}">
                  <c:set var="extraparams" value="&direction=${param.direction}&parentchannel=${param.parentchannel}&imageOnly=yes"/>
                  </c:if>
                  <c:set var="orderby" value="${param.orderby}" scope="page" />
                  <c:set var="type" value="asset" scope="page" />
                  <%@ include file="../pages.jsp" %>

                  <form action="AssetMassDeleteAction.do" name="assetForm">
                     <input type="hidden" name="offset" value="${param.offset}"/>
                     <input type="hidden" name="orderby" value="${orderby}" />
                     <input type="hidden" name="direction" value="${direction}"/>
                     <input type="hidden" name="channelnumber" value="<mm:write referid="parentchannel" />"/>
                     <% if (role != null && SecurityUtil.isWriter(role)) { %>
                        <c:if test="${fn:length(elements) >1}">
                           <input type="submit" class="button" value="<fmt:message key="asset.delete.massdelete" />"/>
                           <input type="button" class="button" value="<fmt:message key="content.delete.massmove" />" 
                                 onclick="massMove('${parentchannel}','<c:url value='/editors/repository/select/SelectorChannel.do?role=writer' />')"/>
                        </c:if>
                     <% } %>
                     <table>
                        <thead>
                           <tr>
                              <th><% if (role != null && SecurityUtil.isWriter(role)) { %>
                                    <c:if test="${fn:length(elements) >1}">
                                     <input type="checkbox"  name="selectall"  onclick="selectAll(this.checked, 'assetForm', 'chk_');" value="on"/></c:if> <% } %></th>
                              <th><a href="javascript:sortBy('Asset', 'otype','<mm:write referid="parentchannel" />')" class="headerlink">
                                    <fmt:message key="asset.typecolumn"/></a></th>
                              <th><a href="javascript:sortBy('Asset', 'title','<mm:write referid="parentchannel" />')" class="headerlink">
                                    <fmt:message key="asset.titlecolumn"/></a></th>
                              <th><a href="javascript:sortBy('Asset', 'creator','<mm:write referid="parentchannel" />')" class="headerlink">
                                    <fmt:message key="asset.authorcolumn"/></a> </th>
                              <th><a href="javascript:sortBy('Asset', 'lastmodifieddate','<mm:write referid="parentchannel" />')" class="headerlink">
                                    <fmt:message key="asset.lastmodifiedcolumn"/></a></th>
                              <th><a href="javascript:sortBy('Asset', 'number','<mm:write referid="parentchannel" />')" class="headerlink">
                                    <fmt:message key="asset.numbercolumn"/></a></th>
                              <th>&nbsp;</th>
                           </tr>
                        </thead>

                        <tbody class="hover">
                           <mm:listnodes referid="elements" jspvar="node">
                              <mm:field name="number" write="false" id="number" vartype="String"/>
                              <mm:field name="number" write="false" id="relnumber"/>

                              <mm:url page="../WizardInitAction.do" id="url" write="false">
                                 <mm:param name="objectnumber" value="$number"/>
                                 <mm:param name="returnurl" value="$returnurl"/>
                              </mm:url>
                              <tr <mm:even inverse="true">class="swap"</mm:even> href="<mm:write referid="url"/>">
                              <td style="white-space: nowrap;">
                                 <% if (role != null && SecurityUtil.isWriter(role)) { %>
                                    <c:if test="${fn:length(elements) >1}">
                                       <input type="checkbox"  name="chk_<mm:field name="number" />" value="<mm:field name="number" />" onClick="document.forms['contentForm'].elements.selectall.checked=false;"/>
                                    </c:if>
                                 <% } %>
                                 <%@ include file="icons.jspf"%>
                              </td>
                              <td onMouseDown="objClick(this);">
                                 <mm:nodeinfo type="guitype"/></td>
                              <td onMouseDown="objClick(this);">
                              <c:set var="assettype" ><mm:nodeinfo type="type"/></c:set>
                                 <mm:field id="title" write="false" name="title"/>
                                 <c:if test="${assettype == 'urls'}">
                                    <c:set var="title" ><mm:field name="name"/></c:set>
                                 </c:if>
                                 <c:if test="${fn:length(title) > 50}">
                                    <c:set var="title">${fn:substring(title,0,49)}...</c:set>
                                 </c:if>${title}</td>
                              <td onMouseDown="objClick(this);" style="white-space: nowrap;">
                                 <mm:field name="lastmodifier" id="lastmodifier" write="false"/>
                                 <mm:listnodes type="user" constraints="username = '${lastmodifier}'">
                                    <c:set var="lastmodifierFull"><mm:field name="firstname"/> <mm:field name="prefix"/> <mm:field
                                    name="surname"/></c:set>
                                    <c:if test="${lastmodifierFull != ''}"><c:set var="lastmodifier" value="${lastmodifierFull}"/></c:if>
                                 </mm:listnodes>
                                 ${lastmodifier}</td>
                              <td style="white-space: nowrap;"><mm:field name="lastmodifieddate"><cmsc:dateformat
                                 displaytime="true"/></mm:field></td>
                              <td><mm:field name="number"/></td>
                              </td>
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
                           </mm:listnodes>
                        </tbody>
                     </table>
                     <% if (role != null && SecurityUtil.isWriter(role)) { %>
                     <c:if test="${fn:length(elements) >1}">
                        <input type="submit" class="button" value="<fmt:message key="asset.delete.massdelete" />"/>
                        <input type="button" class="button" value="<fmt:message key="content.delete.massmove" />" 
                              onclick="massMove('${parentchannel}','<c:url value='/editors/repository/select/SelectorChannel.do?role=writer' />')"/>
                     </c:if>
                     <% } %>
                  </form>
                  <%@ include file="../pages.jsp" %>
               </div>
            </c:if>

            <c:if test="${!empty show}">
               <div class="body">
                  <mm:import externid="elements" from="request" required="true"/>
                  <mm:import externid="elementCount" from="request" vartype="Integer">0</mm:import>
                  <mm:import externid="resultsPerPage" from="request" vartype="Integer">25</mm:import>
                  <c:set var="listSize" value="${elementCount}"/>
                  <c:set var="offset" value="${param.offset}"/>
                  <c:set var="orderby" value="${param.orderby}" scope="page" />
                  <c:set var="type" value="asset" scope="page" />
                  <c:if test="${imageOnly eq 'no'}">
                  <c:set var="extraparams" value="&direction=${param.direction}&parentchannel=${param.parentchannel}&show=0&imageOnly=no"/>
                  </c:if>
                  <c:if test="${imageOnly eq 'yes'}">
                  <c:set var="extraparams" value="&direction=${param.direction}&parentchannel=${param.parentchannel}&show=0&imageOnly=yes"/>
                  </c:if>
                  <%@ include file="../pages.jsp" %>
                  <form>
                     <input type="hidden" name="offset" value="${param.offset}"/>
                     <input type="hidden" name="orderby" value="${orderby}" />
                     <input type="hidden" name="direction" value="${direction}"/>

                     <div width="100%;float:left;">
                        <mm:listnodes referid="elements">
                           <div style="width:160px; height:180px;float:left;text-align:center;" onMouseOut="javascript:hideEditItems(<mm:field name='number'/>)" onMouseOver="showEditItems(<mm:field name='number'/>)">
                           <div style="clear:both;float:left;width:77%;height:33px;">
                              <div class="asset-info" id="asset-info-<mm:field name='number'/>" style="display: none; position: relative; border: 1px solid #eaedff" >
                                 <%@ include file="icons.jspf" %>
                              </div>
                           </div>
                              <div style="width:100%;height:100px;text-align:left;padding:0px 10px 5px 5px;vertical-align:middle;display:block;float:left;" >
                                 <a href="javascript:showInfo(<mm:field name="number" />)">
                                    <c:set var="typedef" ><mm:nodeinfo type="type"/></c:set>
                                    <c:if test="${typedef eq 'images'}">
                                       <img src="<mm:image template="s(120x100)"/>" alt=""/>
                                    </c:if> 
                                    <c:if test="${typedef eq 'attachments'}">
                                       <img src="../gfx/alert_green_left.gif" alt=""/>change
                                    </c:if>
                                     <c:if test="${typedef eq 'urls'}">
                                       need to add
                                    </c:if>

                                 </a>
                              </div>
                              <div style="width:80%;text-align:center;margin:0px 0px 0px 0px;padding-top:0px;padding-right:0px;overflow-x:hidden;">
                                 <c:set var="typedef" ><mm:nodeinfo type="type"/></c:set>
                                 <c:if test="${typedef eq 'images'}">
                                    <mm:field name="title"/><br/><mm:field name="itype"/>
                                 </c:if>
                                 <c:if test="${typedef eq 'attachments'}">
                                    need to change
                                 </c:if>
                                  <c:if test="${typedef eq 'urls'}">
                                    need to add
                                 </c:if>
                              </div>
                           </div>
                        </mm:listnodes>
                        <div style="clear:both;"></div>
                     </div>
                  </form>
                  <%@ include file="../pages.jsp" %>
               </div>
            </c:if>
         </mm:node>
         </div>

      </html:html>
   </mm:cloud>
</mm:content>