<%@ page language="java" pageEncoding="utf-8"%>

<%@page import="org.apache.struts.Globals"%>
<%@include file="globals.jsp"%>

<%@taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib uri="http://jsptags.com/tags/navigation/pager" prefix="pg"%>

<fmt:setBundle basename="newsletter" scope="request" />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
   <head>
      <title>newsletters</title>
      <link rel="icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
      <link rel="shortcut icon" href="<c:url value='/favicon.ico'/>" type="image/x-icon" />
      <link href="<c:url value='/editors/css/newsletter.css'/>" type="text/css" rel="stylesheet" />

      <!-- calendar stylesheet -->
      <link rel="stylesheet" type="text/css" media="all"
         href="<c:url value='/editors/newsletter/styles/calendar-win2k-cold-1.css'/>"
         title="win2k-cold-1" />

      <!-- main calendar program -->
      <script type="text/javascript" src="<c:url value='/editors/newsletter/js/calendar.js'/>"></script>


      <!-- language for the calendar -->
      <script type="text/javascript" src="<c:url value='/editors/newsletter/js/lang/calendar-en.js'/>" ></script>

      <!-- the following script defines the Calendar.setup helper function, which makes
       adding a calendar a matter of 1 or 2 lines of code. -->
      <script type="text/javascript" src="<c:url value='/editors/newsletter/js/calendar-setup.js'/>"></script>

      <!-- the format needs -->
      <script src="<c:url value='/editors/utils/rowhover.js'/>" type="text/javascript"></script>
      <script src="<c:url value='/js/window.js'/>" type="text/javascript"></script>
      <script src="<c:url value='/js/transparent_png.js'/>" type="text/javascript"></script>

      <!--the reset button needs  -->
      <script language="javascript"> 
            function resets(){
            document.forms[0].reset();
            var startDate = document.getElementsByName("startDate");
            var endDate = document.getElementsByName("endDate");
                startDate[0].value = "";
                endDate[0].value = "";
            }
            function submits(){
               document.forms[0].submit();
            }
      </script>
   </head>
   <body>
      <script type="text/javascript">
         addLoadEvent(alphaImages);
      </script>

      <div class="tabs">
         <div class="tab_active">
            <div class="body">
               <div>
                  <a name="activetab">
                     <fmt:message key="newsletterlog.newsletter.log" /> 
                  </a>
               </div>
            </div>
         </div>
      </div>

      <div class="editor">
         <div class="body">
            <html:form method="POST" action="/editors/newsletter/NewsletterStatistic">
               <table style="width: 600px">
                  <tr>
                     <td> <fmt:message key="newsletterlog.newsletter" /> </td>
                     <td>
                        <html:select property="newsletters" styleId="newsletters" style="width:150px">
                           <html:optionsCollection name="newsletters" label="title" value="id" />
                        </html:select>
                     </td>
                     <td>&nbsp;</td>
                     <td>&nbsp;</td>
                  </tr>
                  <tr>
                     <td> <fmt:message key="newsletterlog.from" /> </td>
                     <td>
                        <html:text property="startDate" styleId="f_date_b" readonly="true" style="width:125px" />
                        <input type="image" src="/cmsc-community/editors/editwizards_new/media/datepicker/calendar.gif" id="f_trigger_b" border="0">
                           
                        </input>
                        <script type="text/javascript">
                           Calendar.setup({
                              inputField     :    "f_date_b",      // id of the input field
                              ifFormat       :    "%Y-%m-%d",       // format of the input field
                              button         :    "f_trigger_b",   // trigger for the calendar (button ID)
                              step           :    1                // show all years in drop-down boxes (instead of every other year as default)
                           });
                       </script>
                     </td>
                     <td> <fmt:message key="newsletterlog.to" /> </td>
                     <td>
                        <html:text property="endDate" styleId="f_date_be" readonly="true" style="width:125px" />
                        <input type="image" src="/cmsc-community/editors/editwizards_new/media/datepicker/calendar.gif" id="f_trigger_be" border="0">
                           
                        </input>
                        <script type="text/javascript">
                           Calendar.setup({
                              inputField     :    "f_date_be",      // id of the input field
                              ifFormat       :    "%Y-%m-%d",       // format of the input field
                              button         :    "f_trigger_be",   // trigger for the calendar (button ID)
                              step           :    1                // show all years in drop-down boxes (instead of every other year as default)
                           });
                        </script>
                     </td>
                  </tr>
                  <tr>
                     <td>&nbsp;</td>
                     <c:choose>
                        <c:when test="${requestScope.searchForm==null}">
                           <td>
                              <input type="radio" name="detailOrSum" value="1" checked="checked" />
                                 <fmt:message key="newsletterlog.summary" />
                              <input type="radio" name="detailOrSum" value="2" />
                                 <fmt:message key="newsletterlog.detail" />
                           </td>
                        </c:when>
                        <c:when test="${requestScope.searchForm.detailOrSum=='1'}">
                           <td>
                              <input type="radio" name="detailOrSum" value="1" checked="checked" />
                                 <fmt:message key="newsletterlog.summary" />
                              <input type="radio" name="detailOrSum" value="2" />
                                 <fmt:message key="newsletterlog.detail" />
                           </td>
                        </c:when>
                        <c:when test="${requestScope.searchForm.detailOrSum=='2'}">
                           <td>
                              <input type="radio" name="detailOrSum" value="1" />
                                 <fmt:message key="newsletterlog.summary" />
                              <input type="radio" name="detailOrSum" value="2" checked="checked" />
                                 <fmt:message key="newsletterlog.detail" />
                           </td>
                        </c:when>
                     </c:choose>
                     <td colspan="2">&nbsp;</td>
                  </tr>
                  <tr>
                     <td>
                         <input type="button" onclick="javascript:submits()" 
                           value="<fmt:message key="newsletterlog.submit" />" />
                        <input type="button" onclick="javascript:resets()"
                           value="<fmt:message key="newsletterlog.reset" />" />
                     </td>
                     <td colspan="3">&nbsp;</td>
                  </tr>
               </table>
            </html:form>
         </div>
      </div>

      <div class="editor">
         <div class="ruler_green">
            <div>
               <fmt:message key="newsletterlog.showMessages" />
            </div>
         </div>

         <c:if test="${empty requestScope.records}">
            <div class="body">
               <table>
                  <thead>
                     <tr>
                        <th>&nbsp;</th>
                        <th> <fmt:message key="newsletterlog.summary.newsletter" /> </th>
                        <th> <fmt:message key="newsletterlog.summary.removed" /> </th>
                        <th> <fmt:message key="newsletterlog.summary.subscribe" /> </th>
                        <th> <fmt:message key="newsletterlog.summary.unsubscribe" /> </th>
                        <th> <fmt:message key="newsletterlog.summary.bounces" /> </th>
                        <th>&nbsp;</th>
                     </tr>
                  </thead>
                  <tbody class="hover">
                     <tr class="swap">
                        <td>&nbsp;</td>
                        <td onMouseDown="objClick(this);"> 
                           <c:if test="${requestScope.result.name=='newsletter.summary.bydate'}" >
                              <fmt:message key="newsletterlog.summary.statistic" />
                           </c:if>
                           <c:if test="${requestScope.result.name=='newsletter.summary.all'}" >
                              <fmt:message key="newsletterlog.summary.statistic" />
                           </c:if>
                           <c:if test="${requestScope.result.name=='newsletter.summary.all.bydate'}" >
                              <fmt:message key="newsletterlog.summary.statistic" />
                           </c:if>
                           <c:if test="${requestScope.result.name=='newsletter.summary'}" >
                              <fmt:message key="newsletterlog.summary.statistic" />
                           </c:if>
                        </td>
                        <td onMouseDown="objClick(this);"> ${requestScope.result.removed} </td>
                        <td onMouseDown="objClick(this);"> ${requestScope.result.subscribe} </td>
                        <td onMouseDown="objClick(this);"> ${requestScope.result.unsubscribe} </td>
                        <td onMouseDown="objClick(this);"> ${requestScope.result.bounches} </td>
                        <td>&nbsp;</td>
                     </tr>
                  </tbody>
               </table>
            </div>
         </c:if>

         <c:if test="${!empty requestScope.records}">
            <div class="body">
               <pg:pager maxPageItems="${pagesize}" url="NewsletterStatistic.do">
               <pg:param name="action" value="search" />
               <table>
                  <thead>
                     <tr>&nbsp;</tr>
                     <tr>
                        <th>&nbsp;</th>
                        <th> <fmt:message key="newsletterlog.detail.newsletter" /> </th>
                        <th> <fmt:message key="newsletterlog.detail.logdate" /> </th> 
                        <th> <fmt:message key="newsletterlog.detail.removed" /> </th> 
                        <th> <fmt:message key="newsletterlog.detail.subscribe" /> </th> 
                        <th> <fmt:message key="newsletterlog.detail.unsubcribe" /> </th> 
                        <th> <fmt:message key="newsletterlog.detail.bounces" /> </th>
                        <th>&nbsp;</th>
                     </tr>
                  </thead>
                  <tbody class="hover">
                     <c:set var="num" scope="page" value="0" />
                     <c:forEach var="newsletterlog" items="${requestScope.records}">
                        <c:if test="${num%pagesize%2==0}">
                           <pg:item>
                              <tr class="swap">
                                 <td>&nbsp;</td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.name}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.showingdate}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.removed}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.subscribe}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.unsubscribe}" /> </td>
                                 <td onMouseDown="objClick(this);" style="white-space: nowrap;">
                                    <c:out value="${newsletterlog.bounches}" />
                                 </td>
                                 <td>&nbsp;</td>
                               </tr>
                            </pg:item>
                         </c:if>
                         <c:if test="${num%pagesize%2==1}">
                            <pg:item>
                              <tr>
                                 <td>&nbsp;</td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.name}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.showingdate}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.removed}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.subscribe}" /> </td>
                                 <td onMouseDown="objClick(this);"> <c:out value="${newsletterlog.unsubscribe}" /> </td>
                                 <td onMouseDown="objClick(this);" style="white-space: nowrap;">
                                    <c:out value="${newsletterlog.bounches}" />
                                 </td>
                                 <td>&nbsp;</td>
                              </tr>
                            </pg:item>
                         </c:if>
                         <c:set var="num" scope="page" value="${num+1}" />
                     </c:forEach>
                  </tbody>
               </table>
               <%@ include file="pager_index.jsp"%>
               </pg:pager>
            </div>
         </c:if>
      </div>
   </body>
</html>