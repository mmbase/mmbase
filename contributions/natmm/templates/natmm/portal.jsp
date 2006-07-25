<%@include file="includes/top0.jsp" %>
<mm:cloud jspvar="cloud">
<%@include file="includes/top1_params.jsp" %>
<%@include file="includes/top2_cacheparams.jsp" %>
<cache:cache groups="<%= paginaID %>" key="<%= cacheKey %>" time="<%= expireTime %>" scope="application">
<%@include file="includes/top3_nav.jsp" %>
<%@include file="includes/top4_head.jsp" %>
<%@include file="includes/top5_breadcrumbs_and_pano.jsp" %>
<table width="744" border="0" cellspacing="0" cellpadding="0" align="center" valign="top">
<tr>
  <td style="vertical-align:top;width:165px;padding:2px;">
   <jsp:include page="includes/portal/login.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
      <jsp:param name="sr" value="0" />
    </jsp:include>
    <jsp:include page="includes/teaser.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
      <jsp:param name="sr" value="0" />
    </jsp:include>
    <jsp:include page="includes/portal/navleft.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
      <jsp:param name="sr" value="0" />
    </jsp:include>
    <div style="padding-top:20px;text-align:center;">
      <table><tr><td style="text-align:center; color:#B5B5B5; font-weight:bold;" 
                     onClick="this.style.behavior='url(#default#homepage)'; this.setHomePage('<%= HttpUtils.getRequestURL(request) + "?" + request.getQueryString() %>');"
                     onmouseover="this.style.cursor='pointer'">
        Maak dit <img src="includes/portal/heart.gif" alt="" border="0" /><br/>
        mijn startpagina
      </td></tr></table>
    </div>
  </td>
  <td style="vertical-align:top;width:400px;padding:2px;">
    <jsp:include page="includes/portal/middle_top.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
    <jsp:include page="includes/portal/channels.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
    <jsp:include page="includes/portal/dossiers.jsp">
      <jsp:param name="o" value="<%= paginaID %>"/>
    </jsp:include>
    <%@include file="includes/portal/polls.jsp" %>
    <jsp:include page="includes/portal/nieuwsbrief.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
    <br/>
    <jsp:include page="includes/teaser.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
      <jsp:param name="sr" value="1" />
    </jsp:include>
  </td>
  <td style="vertical-align:top;width:214px;padding:2px;padding-top:1px;">
    <form name="theClock">
      <input type=text name="theTime" class="headerBar" style="width:212px;border:none;font:normal;font-size:90%;text-align:right;padding-right:3px;">
    <form>
    <%
    String embargoLinkConstraint = "(link.embargo < '" + (nowSec+quarterOfAnHour) + "') AND "
                                + "(link.use_verloopdatum='0' OR link.verloopdatum > '" + nowSec + "' )";
    %>
    <mm:list nodes="<%= paginaID %>" path="pagina,contentrel,link" fields="link.number" constraints="<%= embargoLinkConstraint %>">
      <iframe src="<mm:url page="includes/portal/video.jsp">
                     <mm:param name="link"><mm:field name="link.number" /></mm:param>
                  </mm:url>" style="padding:0px;width:214px;height:177px;" id="video<mm:field name="link.number" />" scrolling="no"></iframe>
    </mm:list>
    <jsp:include page="includes/home/shorty_home.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
      <jsp:param name="sr" value="2" />
    </jsp:include>
    <jsp:include page="includes/portal/linklijst.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
    <jsp:include page="includes/portal/fun.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
    <jsp:include page="includes/portal/weblogs.jsp">
      <jsp:param name="s" value="<%= paginaID %>" />
      <jsp:param name="r" value="<%= rubriekID %>" />
      <jsp:param name="rs" value="<%= styleSheet %>" />
    </jsp:include>
  </td>
</tr>
</table>
<%@include file="includes/footer.jsp" %>
</cache:cache>
</mm:cloud>
