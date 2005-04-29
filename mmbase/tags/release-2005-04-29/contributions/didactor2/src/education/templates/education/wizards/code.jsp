<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>

<%@page import="org.mmbase.bridge.*,org.mmbase.bridge.util.*,javax.servlet.jsp.JspException"%>

<%
   String imageName = "";
   String sAltText = "";
%>

<%
   if(session.getAttribute("education_topmenu_mode") == null)
   {//Default active element in education top menu
      session.setAttribute("education_topmenu_mode", "components");
   }
%>

<fmt:bundle basename="nl.didactor.component.education.EducationMessageBundle">
<mm:content postprocessor="reducespace">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
   <%@include file="/shared/setImports.jsp"%>

   <mm:import externid="showcode">false</mm:import>
   <mm:import id="wizardjsp"><mm:treefile write="true" page="/editwizards/jsp/wizard.jsp" objectlist="$includePath" /></mm:import>
   <mm:import id="listjsp"><mm:treefile write="true" page="/editwizards/jsp/list.jsp" objectlist="$includePath" /></mm:import>
   <mm:import id="education_top_menu"><%= session.getAttribute("education_topmenu_mode") %></mm:import>

   <mm:compare referid="showcode" value="true" inverse="true">
      <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
         <html>
            <head>
               <title>Javascript menu</title>
               <style type="text/css">
                  a {
                     font-size: 11px;
                  }
               </style>

               <link rel="stylesheet" type="text/css" href='<mm:treefile page="/css/base.css" objectlist="$includePath" referids="$referids" />' />


      <mm:node number="component.pdf" notfound="skip">
         <mm:relatednodes type="providers" constraints="providers.number=$provider">
            <mm:import id="pdfurl"><mm:treefile write="true" page="/pdf/pdfchooser.jsp" objectlist="$includePath" referids="$referids" /></mm:import>
         </mm:relatednodes>
      </mm:node>

      <script type="text/javascript">
         function saveCookie(name,value,days) {
            if (days) {
               var date = new Date();
               date.setTime(date.getTime()+(days*24*60*60*1000))
               var expires = '; expires='+date.toGMTString()
            } else expires = ''
            document.cookie = name+'='+value+expires+'; path=/'
         }
         function readCookie(name) {
            var nameEQ = name + '='
            var ca = document.cookie.split(';')
            for(var i=0;i<ca.length;i++) {
               var c = ca[i];
               while (c.charAt(0)==' ') c = c.substring(1,c.length)
               if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length)
            }
            return null
         }
         function deleteCookie(name) {
            saveCookie(name,'',-1)
         }
         function restoreTree() {
            for(var i=1; i<10; i++) {
               var lastclicknode = readCookie('lastnodepagina'+i);
               if(lastclicknode!=null) { clickNode(lastclicknode); }
            }
         }
         function clickNode(node) {
            var level = node.split('_').length;
            saveCookie('lastnodepagina'+level,node,1);
            el=document.getElementById(node);
            img = document.getElementById('img_' + node);
            img2 = document.getElementById('img2_' + node);
            if (el!=null && img != null)
            {
               if (el.style.display=='none')
               {
                  el.style.display='inline';
                  if (img2 != null) img2.src = 'gfx/folder_open.gif';
                  if (img.src.indexOf('last.gif')!=-1 )
                  {
                     img.src='gfx/tree_minlast.gif';
                  }
                  else
                  {
                     img.src='gfx/tree_min.gif';
                  }
               }
               else
               {
                  el.style.display='none';
                  if (img2 != null) img2.src = 'gfx/folder_closed.gif';
                  if (img.src.indexOf('last.gif')!=-1)
                  {
                     img.src='gfx/tree_pluslast.gif';
                  }
                  else
                  {
                     img.src='gfx/tree_plus.gif';
                  }
               }
            }
         }
      </script>
   </head>
   <body onLoad="top.frames['text'].location.href = '<mm:treefile page="/education/wizards/loaded.jsp" objectlist="$includePath" referids="$referids" />';">
   </mm:compare>
   <mm:compare referid="showcode" value="true">
      <mm:content type="text/plain" />
   </mm:compare>



<% int treeCount = 0; %>
<% int metatreeCount = 0; %>
<% int comptreeCount = 0; %>


<mm:compare referid="education_top_menu" value="components">
   <% //----------------------- Components come from here ----------------------- %>
   <di:hasrole role="systemadministrator">
      <a href='javascript:clickNode("components_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_components_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/><nobr>&nbsp;<a href='<mm:treefile write="true" page="/components/frame.jsp" objectlist="$includePath" />' title='<fmt:message key="editComponentsDescription"/>' target="text"><fmt:message key="editComponents"/></nobr></a>
      <br>
      <div id='components_0' style='display: none'>
      </div>
   </di:hasrole>
</mm:compare>


<mm:compare referid="education_top_menu" value="roles">
   <% //----------------------- Roles come from here ----------------------- %>
   <di:hasrole role="systemadministrator">
      <a href='javascript:clickNode("components_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_components_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/><nobr>&nbsp;<a href='<mm:write referid="listjsp"/>?wizard=roles&nodepath=roles&fields=name&orderby=name' title='' target="text"><fmt:message key="roles"/></nobr></a>
      <br>
      <div id='components_0' style='display: none'>
         <%// edit people,rolerel, education %>
         <%-- doesn't work properly, so commented it out for the moment
            rolestree.addItem("<fmt:message key="editPeopleRoleRelEducation"/>",
                              "<mm:treefile write="true" page="/education/wizards/roles.jsp" objectlist="$includePath" />",
                              null,
                              "<fmt:message key="editPeopleRoleRelEducationDescription"/>",
                              "<mm:treefile write="true" page="/education/wizards/gfx/new_education.gif" objectlist="$includePath" />");
         --%>
         <%// create new role %>
         <mm:import id="number_of_roles" reset="true">0</mm:import>
         <mm:listnodes type="tests">
            <mm:import id="number_of_roles" reset="true"><mm:size /></mm:import>
         </mm:listnodes>

         <table border="0" cellpadding="0" cellspacing="0">
            <tr>
               <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                  <%// We have to detect the last element %>
                  <mm:isgreaterthan referid="number_of_roles" value="0">
                     <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
                  </mm:isgreaterthan>

                  <mm:islessthan    referid="number_of_roles" value="1">
                     <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
                  </mm:islessthan>

               <td><img src="gfx/new_education.gif" width="16" border="0" align="middle" /></td>
               <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=roles&objectnumber=new' title='<fmt:message key="createNewRolesDescription"/>' target="text"><fmt:message key="createNewRoles"/></a></nobr></td>
            </tr>
         </table>

         <%// edit existing roles %>
         <mm:listnodes type="roles">
            <table border="0" cellpadding="0" cellspacing="0">
               <tr>
                  <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                  <mm:last inverse="true">
                     <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
                  </mm:last>
                  <mm:last>
                     <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
                  </mm:last>
                  <td><img src="gfx/learnblock.gif" border="0" align="middle" /></td>
                  <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=roles&objectnumber=<mm:field name="number" />' title='<fmt:message key="treatRoles"/>' target="text"><mm:field name="name" /></a></nobr></td>
               </tr>
            </table>
         </mm:listnodes>
      </div>
   </di:hasrole>
</mm:compare>


<mm:compare referid="education_top_menu" value="content_metadata">
   <% //----------------------- Metadata for components comes from here ----------------------- %>
   <a href='javascript:clickNode("content_metadata_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_content_metadata_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/>&nbsp;<nobr><a href='#' title="<fmt:message key="filemanagement"/>"><fmt:message key="educationMenuContentMetadata"/></a></nobr>
   <br>
   <div id='content_metadata_0' style='display: none'>
      <%
         String[][] arrstrContentMetadataConfig = new String[5][4];

         arrstrContentMetadataConfig[0][0]  = cloud.getNodeManager("images").getGUIName();
         arrstrContentMetadataConfig[1][0]  = cloud.getNodeManager("attachments").getGUIName();
         arrstrContentMetadataConfig[2][0]  = cloud.getNodeManager("audiotapes").getGUIName();
         arrstrContentMetadataConfig[3][0]  = cloud.getNodeManager("videotapes").getGUIName();
         arrstrContentMetadataConfig[4][0]  = cloud.getNodeManager("urls").getGUIName();


         arrstrContentMetadataConfig[0][1] = "image";
         arrstrContentMetadataConfig[1][1] = "attachment";
         arrstrContentMetadataConfig[2][1] = "audiotapes";
         arrstrContentMetadataConfig[3][1] = "videotapes";
         arrstrContentMetadataConfig[4][1] = "urls";


         arrstrContentMetadataConfig[0][2] = "images";
         arrstrContentMetadataConfig[1][2] = "attachments";
         arrstrContentMetadataConfig[2][2] = "audiotapes";
         arrstrContentMetadataConfig[3][2] = "videotapes";
         arrstrContentMetadataConfig[4][2] = "urls";

         arrstrContentMetadataConfig[0][3] = "title";
         arrstrContentMetadataConfig[1][3] = "title";
         arrstrContentMetadataConfig[2][3] = "title";
         arrstrContentMetadataConfig[3][3] = "title";
         arrstrContentMetadataConfig[4][3] = "name";


         session.setAttribute("content_metadata_names", arrstrContentMetadataConfig);

         for (int f = 0; f < arrstrContentMetadataConfig.length; f++)
         {
            %>
               <table border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     <%
                        if(f == arrstrContentMetadataConfig.length - 1)
                        {
                           %><td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td><%
                        }
                        else
                        {
                           %><td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td><%
                        }
                     %>
                     <td><img src="gfx/learnblock.gif" border="0" align="middle" /></td>
                     <td><nobr>&nbsp;<a href='<mm:write referid="listjsp"/>?wizard=<%= arrstrContentMetadataConfig[f][1] %>&nodepath=<%= arrstrContentMetadataConfig[f][2] %>&searchfields=<%= arrstrContentMetadataConfig[f][3] %>&fields=<%= arrstrContentMetadataConfig[f][3] %>&search=yes&orderby=<%= arrstrContentMetadataConfig[f][3] %>&metadata=yes' title='Bewerk <%= arrstrContentMetadataConfig[f][0] %>' target="text"><%= arrstrContentMetadataConfig[f][0] %></a></nobr></td>
                  </tr>
               </table>
            <%
         }
      %>
               <table border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
                     <td><img src="gfx/learnblock.gif" border="0" align="middle" /></td>
                     <td><nobr>&nbsp;<a href='<mm:write referid="listjsp"/>?wizard=providers&nodepath=providers' target="text">Bewerk welkom pagina</a></nobr></td>
                  </tr>
               </table>

      
      </div>
</mm:compare>



<mm:compare referid="education_top_menu" value="filemanagement">
   <% //----------------------- Filemanagement comes from here ----------------------- %>
   <di:hasrole role="filemanager">
      <a href='javascript:clickNode("filemanagement_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_filemanagement_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/>&nbsp;<nobr><a href='<mm:treefile write="true" page="/education/filemanagement/index.jsp" objectlist="$includePath" />' title="<fmt:message key="filemanagement"/>" target="_top"><fmt:message key="filemanagement"/></a></nobr>
      <br>
      <div id='filemanagement_0' style='display: none'>

      </div>
   </di:hasrole>
</mm:compare>



<mm:compare referid="education_top_menu" value="competence">
   <% //----------------------- Competence comes from here ----------------------- %>
   <%// has to be only for admin I believe %>
   <a href='javascript:clickNode("competence_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_competence_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/><nobr>&nbsp;<a href="#" title=""><fmt:message key="competence"/></nobr></a>
   <br>
   <div id='competence_0' style='display: none'>
      <%
         String[] arrstrNames =    {"Competenties",
                                    "Preassessments",
                                    "Postassessments",
                                    "Profielen",
                                    "P.O.P."};
         String[] arrstrBuilders = {"competencies",
                                    "preassessments",
                                    "postassessments",
                                    "profiles",
                                    "pop"};
         String[] arrstrWizards =  {"competencies",
                                    "preassessments",
                                    "postassessments",
                                    "profiles",
                                    "pop"};

         String[] arrstrTitles =   {"Bewerk competenties",
                                    "Bewerk preassessments",
                                    "Bewerk postassessments",
                                    "Bewerk profielen",
                                    "Bewerk persoonlijk opleidingplan"};


         for(int f = 0; f < arrstrNames.length; f++)
         {
            %>
               <table border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                        <%
                           if(f == arrstrNames.length - 1)
                           {
                              %><td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td><%

                           }
                           else
                           {
                              %><td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td><%
                           }
                        %>
                     <td><img src="gfx/learnblock.gif" border="0" align="middle" /></td>
                     <td><nobr>&nbsp;<a href='<mm:write referid="listjsp"/>?wizard=<%= arrstrWizards[f] %>&nodepath=<%= arrstrBuilders[f] %>&searchfields=name&fields=name' title="<%= arrstrTitles[f] %>" target="text"><%= arrstrNames[f] %></a></nobr></td>
                  </tr>
               </table>
            <%
         }
      %>

   </div>

</mm:compare>





<mm:compare referid="education_top_menu" value="metadata">
   <% //----------------------- Metadata comes from here ----------------------- %>
   <a href='javascript:clickNode("metadata_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_metadata_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/><nobr>&nbsp;<a href='<mm:write referid="listjsp"/>?wizard=metastandard&nodepath=metastandard&fields=name&orderby=name' title='' target="text"><fmt:message key="metadata"/></nobr></a>
   <br>

   <mm:import id="number_of_metadata" reset="true">0</mm:import>
   <mm:listnodes type="tests">
      <mm:import id="number_of_metadata" reset="true"><mm:size /></mm:import>
   </mm:listnodes>

   <div id='metadata_0' style='display: none'>
      <%// create new metadata standard %>
      <table border="0" cellpadding="0" cellspacing="0">
         <tr>
            <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
               <%// We have to detect the last element %>
               <mm:isgreaterthan referid="number_of_metadata" value="0">
                  <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
               </mm:isgreaterthan>

               <mm:islessthan    referid="number_of_metadata" value="1">
                  <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
               </mm:islessthan>

            <td><img src="gfx/new_education.gif" width="16" border="0" align="middle" /></td>
            <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=metastandard&objectnumber=new' title='<fmt:message key="createNewMetadatastandardDescription"/>' target="text"><fmt:message key="createNewMetadatastandard"/></a></nobr></td>
         </tr>
      </table>


      <%// edit existing metadata standards %>
      <mm:listnodes type="metastandard">
         <mm:remove referid="metastandardNumber"/>
         <mm:field id="metastandardNumber" name="number" write="false"/>

         <table border="0" cellpadding="0" cellspacing="0">
            <tr>
               <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                  <%// We have to detect the last element %>

                  <mm:last inverse="true">
                     <td><a href='javascript:clickNode("<mm:field name="number"/>")'><img src="gfx/tree_plus.gif" border="0" align="middle" id='img_<mm:field name="number"/>'/></a></td>
                  </mm:last>

                  <mm:last>
                     <td><a href='javascript:clickNode("<mm:field name="number"/>")'><img src="gfx/tree_pluslast.gif" border="0" align="middle" id='img_<mm:field name="number"/>'/></a></td>
                  </mm:last>

               <td><img src="gfx/folder_closed.gif" border="0" align="middle" id='img2_<mm:field name="number"/>'/></td>
               <td><nobr><a href='<mm:write referid="wizardjsp"/>?wizard=metastandard&objectnumber=<mm:field name="number" />' title='<fmt:message key="treatMetastandard"/>' target="text"><mm:field name="name" /><mm:field name="name" /></a>&nbsp;<a href='metaedit.jsp?number=<mm:field name="number"/>&set_defaults=true' target='text'><img src='gfx/metavalid.gif' border='0' alt='Bewerk standaard waarden voor metadatastandaard'></a></nobr></td>
            </tr>
         </table>

         <mm:import id="the_last_parent" reset="true">false</mm:import>
         <mm:last>
            <mm:import id="the_last_parent" reset="true">true</mm:import>
         </mm:last>


         <div id='<mm:field name="number"/>' style="display:none">
            <%// Create new metadefinition %>

            <mm:import id="the_last_element" reset="true">true</mm:import>
            <mm:relatednodes type="metadefinition" max="1">
               <mm:import id="the_last_element" reset="true">false</mm:import>
            </mm:relatednodes>


            <table border="0" cellpadding="0" cellspacing="0">
               <tr>
                  <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     <%// We have to detect the last element %>
                     <mm:compare referid="the_last_parent" value="true" inverse="true">
                        <td><img src="gfx/tree_vertline.gif" border="0" align="center" valign="middle"/></td>
                     </mm:compare>
                     <mm:compare referid="the_last_parent" value="true">
                        <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     </mm:compare>

                     <mm:compare referid="the_last_element" value="true" inverse="true">
                        <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
                     </mm:compare>

                     <mm:compare referid="the_last_element" value="true">
                        <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
                     </mm:compare>

                  <td><img src="gfx/new_education.gif" width="16" border="0" align="middle" /></td>
                  <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=metadefinition&objectnumber=new&origin=<mm:write referid="metastandardNumber" />' title='<fmt:message key="createNewMetadefinitionDescription"/>' target="text"><fmt:message key="createNewMetadefinition"/></a></nobr></td>
               </tr>
            </table>

            <mm:relatednodes type="metadefinition" orderby="metadefinition.name">
               <table border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                        <%// We have to detect the last element %>
                        <mm:compare referid="the_last_parent" value="true" inverse="true">
                           <td><img src="gfx/tree_vertline.gif" border="0" align="center" valign="middle"/></td>
                        </mm:compare>
                        <mm:compare referid="the_last_parent" value="true">
                           <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                        </mm:compare>

                        <mm:last inverse="true">
                           <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
                        </mm:last>

                        <mm:last>
                           <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
                        </mm:last>

                     <td><img src="gfx/learnblock.gif" border="0" align="middle" /></td>
                     <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=<mm:nodeinfo type="type" />&objectnumber=<mm:field name="number" />' title='<fmt:message key="treatMetadefinition"/> <mm:nodeinfo type="type" />' target="text"><mm:field name="name" /></a></nobr></td>
                  </tr>
               </table>
            </mm:relatednodes>
         </div>
      </mm:listnodes>
   </div>
</mm:compare>







<mm:compare referid="education_top_menu" value="tests">
   <% //----------------------- Tests come from here ----------------------- %>
   <a href='javascript:clickNode("tests_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_tests_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/> <span style='width:100px; white-space: nowrap'><a href='<mm:write referid="listjsp"/>?wizard=tests&nodepath=tests&orderby=name' target="text"><fmt:message key="tests"/></a></span>
   <br>
   <div id='tests_0' style='display: none'>

      <mm:import id="number_of_tests" reset="true">0</mm:import>
      <mm:listnodes type="tests">
         <mm:import id="number_of_tests" reset="true"><mm:size /></mm:import>
      </mm:listnodes>

      <table border="0" cellpadding="0" cellspacing="0">
         <tr>
            <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
               <%// We have to detect the last element %>
               <mm:isgreaterthan referid="number_of_tests" value="0">
                  <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
               </mm:isgreaterthan>

               <mm:islessthan    referid="number_of_tests" value="1">
                  <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
               </mm:islessthan>

            <td><img src="gfx/new_education.gif" width="16" border="0" align="middle" /></td>
            <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>?wizard=tests-origin&objectnumber=new' title='<fmt:message key="createNewTestDescription"/>' target="text"><fmt:message key="createNewTest"/></a></nobr></td>
         </tr>
      </table>

      <mm:listnodes type="tests" orderby="tests.name">

         <table border="0" cellpadding="0" cellspacing="0">
            <tr>
               <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                  <%// We have to detect the last element %>
                  <mm:last inverse="true">
                     <td><a href='javascript:clickNode("<mm:field name="number"/>")'><img src="gfx/tree_plus.gif" border="0" align="middle" id='img_<mm:field name="number"/>'/></a></td>
                  </mm:last>

                  <mm:last>
                     <td><a href='javascript:clickNode("<mm:field name="number"/>")'><img src="gfx/tree_pluslast.gif" border="0" align="middle" id='img_<mm:field name="number"/>'/></a></td>
                  </mm:last>

               <td><img src="gfx/folder_closed.gif" border="0" align="middle" id='img2_<mm:field name="number"/>'/></td>
               <td><nobr><a href='<mm:write referid="wizardjsp"/>?wizard=tests&objectnumber=<mm:field name="number" />' title='<fmt:message key="treatTest"/>' target="text"><mm:field name="name" /></a></nobr></td>
            </tr>
         </table>

         <mm:import id="the_last_parent" reset="true">false</mm:import>
         <mm:last>
            <mm:import id="the_last_parent" reset="true">true</mm:import>
         </mm:last>

         <div id='<mm:field name="number"/>' style="display:none">

            <mm:field name="number" jspvar="sID" vartype="String">
               <mm:write referid="wizardjsp" jspvar="sWizardjsp" vartype="String" write="false">
                  <mm:write referid="the_last_parent" jspvar="sTheLastParent" vartype="String">
                     <jsp:include page="newfromtree_tests.jsp">
                        <jsp:param name="node" value="<%= sID %>" />
                        <jsp:param name="wizardjsp" value="<%= sWizardjsp %>" />
                        <jsp:param name="the_last_parent" value="<%= sTheLastParent %>" />
                     </jsp:include>
                  </mm:write>
               </mm:write>
            </mm:field>



            <mm:remove referid="questionamount" />
            <mm:import id="mark_error" reset="true"></mm:import>
            <mm:field name="questionamount" id="questionamount">
                <mm:isgreaterthan value="0">
                    <mm:countrelations type="questions">
                        <mm:islessthan value="$questionamount">
                            <mm:import id="mark_error" reset="true">Er zijn minder vragen ingevoerd dan er gesteld moeten worden.</mm:import>
                        </mm:islessthan>
                    </mm:countrelations>
                </mm:isgreaterthan>
                <mm:remove referid="requiredscore" />
                <mm:field name="requiredscore" id="requiredscore">
                  <mm:countrelations type="questions">
                      <mm:islessthan value="$requiredscore">
                          <mm:import id="mark_error" reset="true">Er zijn minder vragen ingevoerd dan er goed beantwoord moeten worden.</mm:import>
                      </mm:islessthan>
                  </mm:countrelations>
                  <mm:isgreaterthan referid="questionamount" value="0">
                      <mm:islessthan referid="questionamount" value="$requiredscore">
                        <mm:import id="mark_error" reset="true">Er worden minder vragen gesteld dan er goed beantwoord moeten worden.</mm:import>
                      </mm:islessthan>
                  </mm:isgreaterthan>
                </mm:field>
            </mm:field>

            <mm:relatednodes type="questions" orderby="title">
               <table border="0" cellpadding="0" cellspacing="0">
                  <tr>
                     <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     <mm:compare referid="the_last_parent" value="true" inverse="true">
                        <td><img src="gfx/tree_vertline.gif" border="0" align="center" valign="middle"/></td>
                     </mm:compare>
                     <mm:compare referid="the_last_parent" value="true">
                        <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                     </mm:compare>
                     <mm:last inverse="true">
                        <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="center" valign="middle"/></td>
                     </mm:last>
                     <mm:last>
                        <td><img src="gfx/tree_leaflast.gif" border="0" align="center" valign="middle"/></td>
                     </mm:last>
                     <td><img src="gfx/edit_learnobject.gif" width="16" border="0" align="middle" /></td>


                     <mm:remove referid="type_of_node"/>
                     <mm:nodeinfo id="type_of_node" type="type">

                         <mm:compare referid="type_of_node" value="mcquestions">
                            <mm:import id="mark_error" reset="true">Een multiple-choice vraag moet minstens 1 goed antwoord hebben</mm:import>
                            <mm:relatednodes type="mcanswers" constraints="mcanswers.correct > '0'" max="1">
                               <mm:import id="mark_error" reset="true"></mm:import>
                            </mm:relatednodes>

                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=mcquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /><mm:isnotempty referid="mark_error"></a> <a style='color: red; font-weight: bold' href='javascript:alert(&quot;<mm:write referid="mark_error"/>&quot;);'>!</mm:isnotempty></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="couplingquestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=couplingquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="dropquestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=dropquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="hotspotquestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=hotspotquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="openquestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=openquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="rankingquestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=rankingquestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>

                         <mm:compare referid="type_of_node" value="valuequestions">
                            <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=valuequestions&objectnumber=<mm:field name="number"/>' title='bewerk object' target="text"><mm:field name="title" /></a></nobr></td>
                         </mm:compare>
                     </mm:nodeinfo>
                  </tr>
               </table>
            </mm:relatednodes>
         </div>
      </mm:listnodes>
   </div>
</mm:compare>





<mm:compare referid="education_top_menu" value="educations">
   <% //----------------------- Educations come from here ----------------------- %>
   <a href='javascript:clickNode("node_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_node_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/> <span style='width:100px; white-space: nowrap'><a href="<mm:write referid="listjsp"/>?wizard=educations&nodepath=educations&fields=name&orderby=name" target="text"><fmt:message key="educationMenuEducations"/></a></span>
   <br>
   <div id='node_0' style='display: none'>

      <% //We go throw all educations for CURRENT USER%>
      <%
         int iNumberOfRelations = 0;
      %>
      <mm:node number="$user">
         <mm:related path="classrel,classes,related,educations">
            <mm:size jspvar="NumberOfRelations" vartype="Integer" write="false">
               <%
                  iNumberOfRelations = NumberOfRelations.intValue();
               %>
            </mm:size>
         </mm:related>
      </mm:node>
      <%
         String sEducationConstraints = new String();
         if (iNumberOfRelations > 1) sEducationConstraints = "educations.number=" + session.getAttribute("education_topmenu_course");
      %>
      <%//Number of educations accroding to constraints %>
      <mm:import id="number_of_educations" reset="true">0</mm:import>
      <mm:node number="$user">
         <mm:related path="classrel,classes,related,educations" constraints="<%=sEducationConstraints%>">
            <mm:import id="number_of_educations" reset="true"><mm:size /></mm:import>
         </mm:related>
      </mm:node>



      <% //new education item %>
      <di:hasrole role="courseeditor">
         <table border="0" cellpadding="0" cellspacing="0">
            <tr>
               <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
               <%// We have to detect the last element %>
               <mm:isgreaterthan referid="number_of_educations" value="0">
                  <td><img src="gfx/tree_vertline-leaf.gif" border="0" align="middle"/></td>
               </mm:isgreaterthan>

               <mm:islessthan    referid="number_of_educations" value="1">
                  <td><img src="gfx/tree_leaflast.gif" border="0" align="middle"/></td>
               </mm:islessthan>

               <td><img src="gfx/new_education.gif" width="16" border="0" align="middle" /></td>
               <td><nobr>&nbsp;<a href="<mm:write referid="wizardjsp"/>?wizard=educations&objectnumber=new" title="<fmt:message key="createNewEducationDescription"/>" target="text"><fmt:message key="createNewEducation"/></a></nobr></td>
            </tr>
         </table>
      </di:hasrole>

      <mm:node number="$user">
         <mm:related path="classrel,classes">
            <mm:node element="classes">
               <mm:related path="related,educations" constraints="<%=sEducationConstraints%>">
                  <% //Show current education from here %>
                  <mm:node element="educations">
                        <%@include file="whichimage.jsp"%>
                        <table border="0" cellpadding="0" cellspacing="0">
                           <tr>
                              <td><img src="gfx/tree_spacer.gif" width="16px" height="16px" border="0" align="center" valign="middle"/></td>
                              <td>
                                 <a href='javascript:clickNode("education_0")'><img src="gfx/tree_pluslast.gif" border="0" align="center" valign="middle" id="img_education_0"/></a>
                              </td>
                              <td><img src="gfx/folder_closed.gif" border="0" align="middle" id="img2_education_0"/></td>
                              <td><nobr><a href="<mm:write referid="wizardjsp"/>?wizard=educations&objectnumber=<mm:field name="number" />" title="<fmt:message key="editEducation"/>" target="text"><mm:field name="name" /><mm:present referid="pdfurl"></a> <a href="<mm:write referid="pdfurl"/>&number=<mm:field name="number"/>" target="text"><img src='gfx/icpdf.gif' border='0' alt='(PDF)'/></mm:present></a> <a href="metaedit.jsp?number=<mm:field name="number"/>" target="text"><img id="img_<mm:field name="number"/>" src="<%= imageName %>" border="0" alt="<%= sAltText %>"></a></nobr></td>
                           </tr>
                        </table>

                        <% // We have to count all learnblocks %>
                        <mm:remove referid="number_of_learnblocks"/>
                        <mm:import id="number_of_learnblocks">0</mm:import>
                        <mm:related path="posrel,learnobjects" orderby="posrel.pos" directions="up" searchdir="destination">
                           <mm:remove referid="number_of_learnblocks"/>
                           <mm:import id="number_of_learnblocks"><mm:size /></mm:import>
                        </mm:related>


                        <div id="education_0" style='display: none'>
                           <%// create new learnblock item %>
                           <table border="0" cellpadding="0" cellspacing="0">
                              <tr>
                                 <td><img src="gfx/tree_spacer.gif" width="32px" height="16px" border="0" align="center" valign="middle"/></td>
                                 <mm:isgreaterthan referid="number_of_learnblocks" value="0">
                                    <td><img src='gfx/tree_vertline-leaf.gif' border='0' align='center' valign='middle' id='img_node_0_1_2'/></td>
                                 </mm:isgreaterthan>
                                 <mm:islessthan    referid="number_of_learnblocks" value="1">
                                    <td><img src='gfx/tree_leaflast.gif' border='0' align='center' valign='middle' id='img_node_0_1_2'/></td>
                                 </mm:islessthan>
                                 <td><img src='gfx/new_education.gif' width="16" border='0' align='middle' /></td>
                                 <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>?wizard=learnblocks-origin&objectnumber=new&origin=<mm:field name="number"/>' title="<fmt:message key="createNewLearnblockDescription"/>" target="text"><fmt:message key="createNewLearnblock"/></a></nobr></td>
                              </tr>
                           </table>

                        <% //All learnblocks for current education %>
                        <%
                           int iLearnblockCounter = 0;
                        %>
                        <mm:related path="posrel,learnobjects" orderby="posrel.pos" directions="up" searchdir="destination">
                           <mm:node element="learnobjects">
                              <%@include file="whichimage.jsp"%>
                              <mm:nodeinfo type="type" id="this_node_type">
                                 <mm:import id="mark_error" reset="true"></mm:import>
                                 <mm:compare referid="this_node_type" value="tests">
                                    <mm:field name="questionamount" id="questionamount">
                                       <mm:isgreaterthan value="0">
                                          <mm:countrelations type="questions">
                                             <mm:islessthan value="$questionamount">
                                                <mm:import id="mark_error" reset="true">Er zijn minder vragen ingevoerd dan er gesteld moeten worden.</mm:import>
                                             </mm:islessthan>
                                          </mm:countrelations>
                                       </mm:isgreaterthan>

                                       <mm:field name="requiredscore" id="requiredscore">
                                          <mm:countrelations type="questions">
                                             <mm:islessthan value="$requiredscore">
                                                <mm:import id="mark_error" reset="true">Er zijn minder vragen ingevoerd dan er goed beantwoord moeten worden.</mm:import>
                                             </mm:islessthan>
                                          </mm:countrelations>
                                          <mm:isgreaterthan referid="questionamount" value="0">
                                             <mm:islessthan referid="questionamount" value="$requiredscore">
                                                <mm:import id="mark_error" reset="true">Er worden minder vragen gesteld dan er goed beantwoord moeten worden.</mm:import>
                                             </mm:islessthan>
                                          </mm:isgreaterthan>
                                       </mm:field>
                                    </mm:field>
                                 </mm:compare>

                                 <table border="0" cellpadding="0" cellspacing="0">
                                    <tr>
                                       <td><img src="gfx/tree_spacer.gif" width="32px" height="16px" border="0" align="center" valign="middle"/></td>
                                       <td><mm:last inverse="true"><a href='javascript:clickNode("node_0_0_<%= iLearnblockCounter %>")'><img src="gfx/tree_plus.gif" border="0" align="center" valign="middle" id="img_node_0_0_<%= iLearnblockCounter %>"/></a></mm:last><mm:last><a href='javascript:clickNode("node_0_0_<%= iLearnblockCounter %>")'><img src="gfx/tree_pluslast.gif" border="0" align="center" valign="middle" id="img_node_0_0_<%= iLearnblockCounter %>"/></a></mm:last></td>
                                       <td><img src="gfx/folder_closed.gif" border="0" align="middle" id='img2_node_0_0_<%= iLearnblockCounter %>'/></td>
                                       <td><nobr><a href="<mm:write referid="wizardjsp"/>?wizard=<mm:nodeinfo type="type" />&objectnumber=<mm:field name="number" />" title="<fmt:message key="treatLearnobject"/> <mm:nodeinfo type="type" />" target="text"><mm:field name="name" /><mm:present referid="pdfurl"><mm:compare referid="this_node_type" value="pages"></a> <a href="<mm:write referid="pdfurl" />&number=<mm:field name="number"/>" target="text"><img src='gfx/icpdf.gif' border='0' alt='(PDF)'/></mm:compare><mm:compare referid="this_node_type" value="learnblocks"></a> <a href="<mm:write referid="pdfurl"/>&number=<mm:field name="number"/>" target="text"><img src='gfx/icpdf.gif' border='0' alt='(PDF)'/></mm:compare></mm:present></a> <a href="metaedit.jsp?number=<mm:field name="number"/>" target="text"><img id="img_<mm:field name="number"/>" src="<%= imageName %>" border="0" alt="<%= sAltText %>"></a></nobr></td>
                                    </tr>
                                 </table>
                              </mm:nodeinfo>

                              <div id="node_0_0_<%= iLearnblockCounter %>" style="display:none">
                                 <mm:treeinclude write="true" page="/education/wizards/learnobject.jsp" objectlist="$includePath" referids="wizardjsp">
                                    <mm:param name="startnode"><mm:field name="number" /></mm:param>
                                    <mm:param name="depth">10</mm:param>
                                    <mm:last>
                                       <mm:param name="the_last_parent">true</mm:param>
                                    </mm:last>
                                    <mm:last inverse="true">
                                       <mm:param name="the_last_parent">false</mm:param>
                                    </mm:last>
                                 </mm:treeinclude>
                              </div>
                           </mm:node>
                           <%
                              iLearnblockCounter++;
                           %>
                        </mm:related>
                        </div>
                  </mm:node>
               </mm:related>
            </mm:node>
         </mm:related>
      </mm:node>
   </div>
</mm:compare>



<mm:compare referid="showcode" value="true" inverse="true">
   </body>

   </html>
</mm:compare>

</mm:cloud>

</mm:content>
</fmt:bundle>

