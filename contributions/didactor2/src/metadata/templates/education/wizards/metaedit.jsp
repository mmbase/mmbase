<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>

<%@page import = "java.util.Date" %>
<%@page import = "java.util.HashSet" %>
<%@page import = "java.util.ArrayList" %>
<%@page import = "java.util.Enumeration" %>
<%@page import = "java.util.Iterator" %>
<%@page import = "java.text.SimpleDateFormat" %>

<%@page import = "org.mmbase.bridge.*" %>
<%@page import = "nl.didactor.metadata.util.MetaDataHelper" %>

<html>
   <head>
   </head>
   <body style="padding-left:10px">
   <%

      MetaDataHelper mdh = new MetaDataHelper();
      // mdh.log(request,"metaedit.jsp");

      String sNode = request.getParameter("number");     
      String sRequest_Submitted = request.getParameter("submitted");
      String sRequest_DoCloseMetaeditor = request.getParameter("close");
      String sRequest_SetDefaults = request.getParameter("set_defaults"); 

      if (sRequest_Submitted == null) {
         //Empty form
         if (sRequest_SetDefaults != null) {
            %>
            <jsp:include page="metaedit_form.jsp">
               <jsp:param name="number" value="<%= sNode %>" />
               <jsp:param name="set_defaults" value="true" />
            </jsp:include>
            <%
         } else {
            %>
            <jsp:include page="metaedit_form.jsp">
               <jsp:param name="number" value="<%= sNode %>" />
            </jsp:include>
            <%
         }
      } else {
         //Submit has been pressed
         //------------------------------- Check form -------------------------------
         %>
         <mm:content postprocessor="reducespace">
            <mm:cloud jspvar="cloud">      
               <%
                  NodeList nlRequiredMetadefs = mdh.getRequiredMetadefs(cloud);
                  HashSet nlPassedNodes = new HashSet();
                  Enumeration enumParamNames;

                  if( !sRequest_Submitted.equals("add")
                     && !sRequest_Submitted.equals("remove")
                     && sRequest_SetDefaults == null) {
                    
                     // Check for all required metadefinitions
                     // If the metadefinition is present, we remove it from "nlRequiredMetadefs"
                     enumParamNames = request.getParameterNames();
                     ArrayList arliSizeErrors = new ArrayList();
                     while(enumParamNames.hasMoreElements()) {
                        String sParameter = (String) enumParamNames.nextElement();
                        String[] arrstrParameters = request.getParameterValues(sParameter);
                        if(sParameter.charAt(0) == 'm') {                           
                           String sMetadataDefinitionID = sParameter.substring(1);
                           Node thisMetaDef = cloud.getNode(sMetadataDefinitionID);
                           if (nlRequiredMetadefs.contains(thisMetaDef)) {                        
                              if(mdh.hasValidMetadata(cloud,arrstrParameters,sMetadataDefinitionID,arliSizeErrors)) {                        
                                 nlRequiredMetadefs.remove(thisMetaDef);
                              }
                           }
                        }
                     }

                     %>
                     <%@include file="metaedit_header.jsp" %>
                     <br/>
                     <%
                     //Header, if error
                     if((nlRequiredMetadefs.size() > 0) || (arliSizeErrors.size() > 0))
                     {
                        %>
                        <style type="text/css">
                           body{
                              font-family:arial;
                              font-size:12px;
                           }
                        </style>

                        <font style="color:red; font-size:11px; font-weight:bold; text-decoration:none; letter-spacing:1px;"><font style="font-size:15px">O</font>NTBREKENDE VERPLICHTE VELDEN!</font>
                        <br/>
                        <%
                     }
                     //List of errors for empty nodes
                     if(nlRequiredMetadefs.size() > 0) {
                        %>
                        <di:translate key="metadata.please_correct_errors" />:
                        <br/>
                        <ul>
                        <%
                     }
                     for(NodeIterator it = nlRequiredMetadefs.nodeIterator(); it.hasNext();) {
                        Node emptyNode = (Node) it.next();
                        %>
                        <li><%= emptyNode.getStringValue("name") %> <di:translate key="metadata.is_required" /></li>
                        <%
                     }
                     //List of error for errors with "size"
                     for(Iterator it = arliSizeErrors.iterator(); it.hasNext();) {
                        String sMetaDefinitionID = (String) it.next();
                        %>
                        <li>
                           <di:translate key="<%= "metadata." + mdh.getReason(cloud,sMetaDefinitionID) %>" />
                           <mm:node number="<%= sMetaDefinitionID %>">
                              = [ <mm:field name="minvalues"/>, <mm:field name="maxvalues"/> ]
                           </mm:node>
                       </li><%
                     }
                     %></ul><%

                     //Use JS to synchronize values in tree
                     if((arliSizeErrors.size()>0) || (nlRequiredMetadefs.size() > 0)) {
                        %>
                        <a href="javascript:history.go(-1)"><font style="color:red; font-weight:bold; text-decoration:none"><di:translate key="metadata.back_to_metaeditform" /></font></a>
                        <script>
                           try
                           {
                              top.frames['menu'].document.images['img_<%= sNode %>'].src='gfx/metaerror.gif';
                           }
                           catch(err)
                           {
                           }
                        </script>
                        <%
                        
                     } else {
                        
                        if(session.getAttribute("show_metadata_in_list") == null) {
                           //We use metaeditor from content_metadata or not?
                           %>
                           <di:translate key="metadata.metadata_is_saved" />.
                           <script>
                              try
                              {
                                 top.frames['menu'].document.images['img_<%= sNode %>'].src='gfx/metavalid.gif';
                              }
                              catch(err)
                              {
                              }
                              window.setInterval("document.location.href='metaedit.jsp?number=<%= sNode %>&random=<%= (new Date()).getTime()%>;'", 3000);
                           </script>
                           <br/><br/>
                           <a href="javascript:history.go(-1)"><font style="color:red; font-weight:bold; text-decoration:none"><di:translate key="metadata.back_to_metaeditform" /></font></a>
                           <%
                        
                        } else {
                        
                           if ((sRequest_DoCloseMetaeditor != null) && (sRequest_DoCloseMetaeditor.equals("yes"))) {

                              //User has selected "SAVE&CLOSE"
                              response.sendRedirect((String) session.getAttribute("metalist_url"));
                           
                           } else {
                              
                              response.sendRedirect("metaedit.jsp?number=" + sNode + "&random=" + (new Date()).getTime());
                           }
                        }
                     }
                  }

                  //If we set only defaults values, always redirect
                  if((sRequest_SetDefaults != null) 
                        && (!sRequest_Submitted.equals("add"))
                        && (!sRequest_Submitted.equals("remove"))) {
                        %>
                        <%@include file="metaedit_header.jsp" %>
                        <br/>
                        <di:translate key="metadata.default_metadatavalues_are_saved" />.
                        <script>
                           window.setInterval("document.location.href='metaedit.jsp?number=<%= sNode %>&random=<%= (new Date()).getTime()%>&set_defaults=true;'", 3000);
                        </script>
                        <br/><br/>
                        <a href="javascript:history.go(-1)"><font style="color:red; font-weight:bold; text-decoration:none"><di:translate key="metadata.back_to_metaeditform" /></font></a>
                        <%
                  }

                  //---------------- Process parameters and store values ---------------
                  if(sRequest_Submitted.equals("add")) {

                     // We have got "add lang string" command, it creates a new metalangstring
                     
                     Node mNode = mdh.getMetadataNode(cloud,sNode,request.getParameter("add"),false);
                     String sMetadataID = mNode.getStringValue("number");
                     
                     %>
                     <mm:remove referid="lang_id" />
                     <mm:remove referid="metadata_id" />
                     <mm:createnode type="metalangstring" id="lang_id"/>
                     <mm:node number="<%= sMetadataID %>" id="metadata_id" />
                     <mm:createrelation source="metadata_id" destination="lang_id" role="posrel">
                        <mm:setfield name="pos">-1</mm:setfield>
                     </mm:createrelation>
                     <%

                  } else {

                     enumParamNames = request.getParameterNames();
                     while(enumParamNames.hasMoreElements()) {
                        // Parse all parameters from http-request
                        String sParameter = (String) enumParamNames.nextElement();
                        String[] arrstrParameters = request.getParameterValues(sParameter);
   
                        if(sParameter.charAt(0) == 'm') {
                        
                           String sMetadefID = sParameter.substring(1);
                           Node metadataNode = mdh.getMetadataNode(cloud,sNode,sMetadefID,false);
                           
                           // Add this node to the "passed" list, we shouldn't erase values from it in future
                           nlPassedNodes.add(metadataNode);
                           
                           int skipParameter = -1;
                           if(sRequest_Submitted.equals("remove")){
               
                              // if we have got "remove" command, 
                              // we should skip processing of the langstring defined by the add parameter
                              String[] sTarget = request.getParameter("add").split("\\,");
                              if (sMetadefID.equals(sTarget[0])) {
                                 try {
                                    skipParameter = Integer.parseInt(sTarget[1]);
                                 } catch (Exception e ){
                                    
                                 }
                              }
                           }
                           mdh.setMetadataNode(cloud,arrstrParameters,metadataNode,sMetadefID,skipParameter);
                        }
                     }
                     
                     // ------------ Remove nodes which are not passed by this form ---------------

                     %>
                     <mm:node number="<%= sNode %>">
                        <mm:relatednodes type="metadata" jspvar="metadataNode">
                           <%
                           if(!nlPassedNodes.contains(metadataNode)) {
                              %>
                              <mm:related path="posrel,metavocabulary">
                                 <mm:deletenode element="posrel" />
                              </mm:related>
                              <mm:relatednodes type="metadate">
                                 <mm:deletenode deleterelations="true"/>
                              </mm:relatednodes>
                              <mm:relatednodes type="metalangstring">
                                 <mm:deletenode deleterelations="true"/>
                              </mm:relatednodes>
                              <%
                           }
                           %>
                        </mm:relatednodes>
                     </mm:node>
                     <%
                  }
                  if((sRequest_Submitted.equals("add")) || (sRequest_Submitted.equals("remove"))) {
                  %>
                     <jsp:include page="metaedit_form.jsp" flush="true">
                        <jsp:param name="number" value="<%= sNode %>" />
                     </jsp:include>
                  <%
                  }
                  // We have to update metadate.value field (it is handled by metadata builder) 
               %>
               <mm:node number="<%= sNode %>">
                  <mm:relatednodes type="metadata">
                     <mm:setfield name="value">-</mm:setfield>
                  </mm:relatednodes>
               </mm:node>
            </mm:cloud>
         </mm:content>
         <%
      }
   %>
   </body>
</html>
