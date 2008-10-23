<%@ page import="com.finalist.util.http.BulkUploadUtil" 
%>
   <script src="../repository/search.js" type="text/javascript"></script>
   <script type="text/javascript">
       function upload() {
           var f=document.forms[0];
           f.submit();
           setTimeout('sayWait();',0);
   
       }
   
       function sayWait() {
           document.getElementById("busy").style.visibility="visible";
           document.getElementById("notbusy").style.visibility="hidden";
       }
            
      function showInfo(objectnumber) {
         openPopupWindow('attachmentinfo', '500', '500', 'attachmentinfo.jsp?objectnumber='+objectnumber);
       }
   </script>

<!--////////////////////////////////////-->    

          <form action="" enctype="multipart/form-data" method="post">
                <input type="hidden" name="uploadAction" value="${param.uploadAction}"/>
                <table border="0">
                   <tr>
                      <td><fmt:message key="attachments.upload.explanation" /></td>
                   </tr>
                   <tr>
                      <td><input type="file" name="zipfile"/></td>
                   </tr>
                   <tr>
                      <td><input type="button" name="uploadButton" onclick="upload();" 
                               value="<fmt:message key="attachments.upload.submit" />"/></td>
                   </tr>
                </table>
         </form>
        <div id="busy">
            <fmt:message key="uploading.message.wait"/><br />
        </div>
<%
    // retrieve list op node id's from either the recent upload
    // or from the request url to enable a return url
    // TODO move this to a struts action there are some issue with HttpUpload
    // in combination with struts which have to be investigated first
    
    String uploadedNodes = "";
    int numberOfUploadedNodes = -1;
    if ("post".equalsIgnoreCase(request.getMethod())) {
        NodeManager manager = cloud.getNodeManager("attachments");
        List<Integer> nodes = BulkUploadUtil.uploadAndStore(manager, request);
        uploadedNodes = BulkUploadUtil.convertToCommaSeparated(nodes);
        numberOfUploadedNodes = nodes.size();
    } else {
        if (request.getParameter("uploadedNodes") != null) {
            uploadedNodes = request.getParameter("uploadedNodes");
        }
        if (request.getParameter("numberOfUploadedNodes") != null) {
            numberOfUploadedNodes = Integer.parseInt(request.getParameter("numberOfUploadedNodes"));
        }
    }
%>
<% if (numberOfUploadedNodes == 0) { %>
    <p><fmt:message key="attachments.upload.error"/></p>
<% } else if (numberOfUploadedNodes > 0) { %>
    <p id="notbusy"><fmt:message key="attachments.upload.result">
           <fmt:param value="<%= numberOfUploadedNodes %>"/>
       </fmt:message>
    </p>
         <table>
            <tr class="listheader">
               <th></th>
               <th nowrap="true"><fmt:message key="attachmentsearch.titlecolumn" /></th>
               <th><fmt:message key="attachmentsearch.filenamecolumn" /></th>
            </tr>
            <tbody class="hover">
                <c:set var="useSwapStyle">true</c:set>

                <mm:listnodescontainer path="attachments" nodes="<%= uploadedNodes %>">
                    <mm:listnodes>

               <mm:field name="title" escape="js-single-quotes" jspvar="title">
                  <mm:attachment escape="js-single-quotes" jspvar="attachment">
                     <%
                     title = ((String)title).replaceAll("[\"]","@quot;");
                     attachment = ((String)attachment).replaceAll("[\"]","@quot;");
                     %>
                       <mm:import id="url">javascript:selectElement('<mm:field name="number"/>', '<%=title%>', '<%=attachment%>');</mm:import>
                    </mm:attachment>
                </mm:field>
                    <tr <c:if test="${useSwapStyle}">class="swap"</c:if> href="<mm:write referid="url"/>">
                       <td>
                        <%-- use uploadedNodes and numberOfUploadedNodes in return url --%>
                        
                        <c:set var="returnUrl">/editors/resources/attachmentupload.jsp?uploadedNodes=<%=uploadedNodes%>&numberOfUploadedNodes=<%=numberOfUploadedNodes%>&uploadAction=${param.uploadAction}</c:set>
                   <c:choose>
                      <c:when test="${param.uploadAction == 'select'}">
                              <a href="<mm:url page="SecondaryEditAction.do">
                                           <mm:param name="action" value="init"/>
                                           <mm:param name="number"><mm:field name="number" /></mm:param>
                                           <mm:param name="returnUrl" value="${returnUrl}"/>
                                       </mm:url>" onclick="blockSelect = true">
                          </c:when>
                          <c:otherwise>
                              <a href="<mm:url page="../WizardInitAction.do">
                                           <mm:param name="objectnumber"><mm:field name="number" /></mm:param>
                                           <mm:param name="returnurl" value="${returnUrl}" />
                                       </mm:url>">
                          </c:otherwise>
                       </c:choose>
                       <img src="../gfx/icons/page_edit.png" title="<fmt:message key="images.upload.edit"/>" alt="<fmt:message key="images.upload.edit"/>"/></a>
                    
                         <a href="<mm:url page="DeleteSecondaryContentAction.do" >
                             <mm:param name="objectnumber"><mm:field name="number" /></mm:param>
                             <mm:param name="object_type" value="attachmentsupload" />
                             </mm:url>">
                        <img src="../gfx/icons/delete.png" alt="<fmt:message key="attachmentsearch.icon.delete" />" title="<fmt:message key="attachmentsearch.icon.delete" />"/></a>
                     
                        <a href="javascript:showInfo(<mm:field name="number" />)">
                              <img src="../gfx/icons/info.png" title="<fmt:message key="images.upload.info"/>" alt="<fmt:message key="images.upload.info"/>"/></a>
                       </td>
                       <td onMouseDown="objClick(this);"><mm:field name="title"/></td>
                       <td onMouseDown="objClick(this);"><mm:field name="filename"/></td>
                    </tr>
                    <c:set var="useSwapStyle">${!useSwapStyle}</c:set>
                    </mm:listnodes>
                </mm:listnodescontainer>

            </tbody>
         </table>
<% } %>
<!--//////////////////////////////-->