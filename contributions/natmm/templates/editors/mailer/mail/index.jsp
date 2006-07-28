<%@include file="/taglibs.jsp" %>
<%@page import="nl.leocms.connectors.UISconnector.input.customers.model.*" %>
<mm:cloud jspvar="cloud">
<mm:log jspvar="log">
   <mm:import externid="action"></mm:import>
   <mm:compare referid="action" value="">
   <%
    String sLogin = request.getParameter("username");
    String sPassword = request.getParameter("password");    
    log.info("Found " + sLogin + " and " + sPassword);
    if((sLogin != null) && (sPassword != null)){
      Object object = nl.leocms.connectors.UISconnector.input.customers.process.Reciever.recieve(nl.leocms.connectors.UISconnector.UISconfig.getCustomersURL(sLogin, sPassword));

      String memberid="-1";    
      if(object instanceof CustomerInformation){
         CustomerInformation customerInformation = (CustomerInformation) object;
         memberid = nl.leocms.connectors.UISconnector.input.customers.process.Updater.update(customerInformation);
    
      }
      if(object instanceof String)
      {
         log.info("Exception " + object);
      }
      log.info("Set memberid " + memberid);
      %>
      <%@include file="/editors/mailer/util/memberid_set.jsp" %>
      <%
    }
   %>
   <%@include file="/editors/mailer/util/memberid_get.jsp" %>
   <%
      if(memberid == null || memberid.equals("-1")){
         log.info("Did not find a memberid");
         response.sendRedirect("login.jsp?reason=failed");
      }
      else{
         log.info("Found memberid");
         response.sendRedirect("dossier.jsp");
      }
   %>
   </mm:compare>
   <mm:compare referid="action" value="logout">
      <% String memberid="-1"; %>
      <%@include file="/editors/mailer/util/memberid_set.jsp" %>
      <% 
         response.sendRedirect("login.jsp");
      %>
   </mm:compare>
</mm:log>
</mm:cloud>
