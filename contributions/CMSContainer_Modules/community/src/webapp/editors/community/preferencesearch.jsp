<%@page language="java" contentType="text/html;charset=utf-8"
%><%@include file="globals.jsp"
%><%@page import="java.util.Iterator,
                 com.finalist.cmsc.mmbase.PropertiesUtil"%>
<%@ taglib prefix="cmsc-ui" uri="http://finalist.com/cmsc-ui" %>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html:html xhtml="true">
<cmscedit:head title="community.preference.title">
<c:url var="actionUrl" value="/editors/community/PreferenceAction.do"/>
<c:url var="userActionUrl" value="/editors/community/userAddInitAction.do"/>
      <script src="../repository/search.js" type="text/javascript"></script>
      <script src="../repository/content.js" type="text/javascript"></script>
      <script src="../../js/prototype.js" type="text/javascript"></script>
		<script type="text/javascript">
			function selectElement(element, title, src) {
				if(window.top.opener != undefined) {
					window.top.opener.selectElement(element, title, src);
					window.top.close();
				}
			}
			
			function showInfo(objectnumber) {
				openPopupWindow('reactioninfo', '500', '500', 'reactioninfo.jsp?objectnumber='+objectnumber);
            }
         function update(number) {
            var myAjax = new Ajax.Request(
             '${actionUrl}',
             {   parameters:"method=modify&id="+number+"&key="+document.getElementById("key_"+number).value+"&value="+document.getElementById("value_"+number).value,
             onComplete: postUpdate
             }
           );
        }

         function deleteInfo(number,offset,resultLength) {
            if(confirm('<fmt:message key="community.preference.delete.conform" />')) {
               if(resultLength == "1") {
                  offset = eval(offset -1);
               }
               var url = "${actionUrl}?method=delete";
               url += "&id="+number+"&offset="+offset;
               document.location = url;
            }
            
         }

        function create() {
            // var url = "${actionUrl}?method=addInit";
           //  document.location = url;
           document.forms[0].method.value = "addInit";
           document.forms[0].offset.value = 0;
           document.forms[0].submit();
        }
        function postUpdate() {
           alert('<fmt:message key="community.preference.update.success" />');
        }

		</script>
</cmscedit:head>
   <body>
      <mm:cloud jspvar="cloud" loginpage="../../editors/login.jsp">

<mm:import externid="action">search</mm:import><%-- either: search of select --%>

      
      <div class="tabs">
         <div class="tab_active">
            <div class="body">
               <div>
                  <a href="#"><fmt:message key="community.preference.title" /></a>
               </div>
            </div>
         </div>
      </div>

     <div class="editor" style="height:500px">
      <div class="body">
         <mm:import id="searchinit"><c:url value='/editors/community/PreferenceAction.do'/></mm:import>
         <html:form action="/editors/community/PreferenceAction" method="post">
			<html:hidden property="method" value="list"/>
            <html:hidden property="offset"/>
            <html:hidden property="order"/>
            <html:hidden property="direction"/>
          <%@include file="preferenceform.jsp" %>
         </html:form>
	</div>

<div class="ruler_green"><div><fmt:message key="community.preference.result" /></div></div>
<div class="body">
<mm:import jspvar="resultCount" vartype="Integer">${totalCount}</mm:import>
<mm:import externid="offset" jspvar="offset" vartype="Integer">${offset}</mm:import>
<c:if test="${resultCount > 0}">
<%@include file="../repository/searchpages.jsp" %>
         <table>
            <tr class="listheader">
               <th> </th>
               <th><fmt:message key="community.preference.user.upper" /></th>
               <th><fmt:message key="community.preference.module.upper" /></th>
               <th><fmt:message key="community.preference.key.upper" /></th>
               <th><fmt:message key="community.preference.value.upper" /></th>
               <th><fmt:message key="community.preference.action.upper" /></th>
            </tr>
            <tbody class="hover">
                <c:set var="useSwapStyle">true</c:set>
                <c:forEach var="preference" items="${results}" >
	                  <tr <c:if test="${useSwapStyle}">class="swap"</c:if>>
	                     <td style="white-space:nowrap;">
						         <mm:hasrank minvalue="administrator">
	                            <a href="javascript:deleteInfo('${preference.id}','${offset}',${fn:length(results)})">
         	                            <img src="../gfx/icons/delete.png" title="<fmt:message key="community.preference.delete" />"/></a>
	                        </mm:hasrank>   
                         </td>
                         <td><a href="${userActionUrl}?authid=${preference.authenticationId}"><c:out  value="${preference.userId}"/></a></td>
	                      <td ><c:out  value="${preference.module}"/></td>
                         <td ><input type="text" name="key" id="key_${preference.id}"  value="<c:out  value="${preference.key}"/>"/></td>
                         <td ><input type="text" name="value" id="value_${preference.id}"  value="<c:out  value="${preference.value}"/>"/></td>
                         <td>
                          <mm:hasrank minvalue="administrator">
                         <a href="javascript:update('${preference.id}')"><fmt:message key="view.submit" /></a>
                          </mm:hasrank>     
                         </td>
	                  </tr>
	               <c:set var="useSwapStyle">${!useSwapStyle}</c:set>
	           </c:forEach>
	         </tbody>
         </table>
</c:if>
<c:if test="${resultCount == 0 && isList != null}">
	<fmt:message key="community.preference.noresult" />
</c:if>
<c:if test="${isAddSuccess != null}">
	<fmt:message key="community.preference.add.success" />
</c:if>
<c:if test="${resultCount > 0}">
	<%@include file="../repository/searchpages.jsp" %>
</c:if>	
</mm:cloud>
   </body>
</html:html>
</mm:content>