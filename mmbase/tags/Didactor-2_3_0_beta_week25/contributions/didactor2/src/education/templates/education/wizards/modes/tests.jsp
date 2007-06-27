<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"%>  
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<jsp:directive.page import="java.util.*,nl.didactor.component.education.utils.*" />
<jsp:scriptlet>
  String imageName = "";
  String sAltText = "";
</jsp:scriptlet>
<mm:cloud method="delegate" authenticate="asis" jspvar="cloud">
  <jsp:directive.include file="/shared/setImports.jsp" />
  <jsp:directive.include file="../mode.include.jsp" />
  <jsp:directive.include file="/education/wizards/roles_defs.jsp" />
   <% //----------------------- Tests come from here ----------------------- %>
   <mm:import id="editcontextname" reset="true">toetsen</mm:import>
   <%@include file="/education/wizards/roles_chk.jsp" %>
   <mm:islessthan inverse="true" referid="rights" referid2="RIGHTS_RW">
      <a href='javascript:clickNode("tests_0")'><img src='gfx/tree_pluslast.gif' width="16" border='0' align='center' valign='middle' id='img_tests_0'/></a>&nbsp;<img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/> <span style='width:100px; white-space: nowrap'><a href='<mm:write referid="listjsp"/>&wizard=config/tests/tests&nodepath=tests&fields=name&searchfields=name&orderby=name<mm:write referid="forbidtemplate" escape="text/plain" />' target="text"><di:translate key="education.tests" /></a></span>
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
               <td><nobr>&nbsp;<a href='<mm:write referid="wizardjsp"/>&wizard=config/tests/tests&objectnumber=new&path= ' title='<di:translate key="education.createnewtestdescription" />' target="text"><di:translate key="education.createnewtest" /></a></nobr></td>
            </tr>
         </table>

         <mm:listnodes type="tests" orderby="tests.name">
         <mm:import id="testname" jspvar="testname" reset="true"><mm:field name="name"/></mm:import>
            <%@include file="whichimage.jsp"%>
            <mm:field name="number" id="tnumber" write="false" />
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

                 <td><nobr>
                     <a href='<mm:write referid="wizardjsp"/>&wizard=config/tests/tests&objectnumber=<mm:field name="number" />&path=<mm:write referid="testname"/>' title='<di:translate key="education.treattest" />' target="text"><mm:field name="name" /></a>

                     <mm:node number="component.metadata" notfound="skip">
                        <a href="metaedit.jsp?number=<mm:write referid="tnumber" />" target="text"><img id="img_<mm:write referid="tnumber" />" src="<%= imageName %>" border="0" title="<%= sAltText %>" alt="<%= sAltText %>"></a>
                     </mm:node>
                     </nobr></td>
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
                           <jsp:param name="testname" value="<%=testname%>" />
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

               <mm:related path="posrel,questions" orderby="posrel.pos">
                  <mm:node element="questions">
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
                        <mm:nodeinfo id="type_of_node" type="type" jspvar="dummyName" vartype="String">

                            <mm:compare referid="type_of_node" value="mcquestions">
                               <mm:import id="mark_error" reset="true">Een multiple-choice vraag moet minstens 1 goed antwoord hebben</mm:import>
                               <mm:relatednodes type="mcanswers" constraints="mcanswers.correct > '0'" max="1">
                                  <mm:import id="mark_error" reset="true"></mm:import>
                               </mm:relatednodes>

                               <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>&wizard=config/question/mcquestions&objectnumber=<mm:field name="number"/>&path=' title='<di:translate key="education.edit" /> <%= dummyName.toLowerCase() %>' target="text"><mm:field name="title" /><mm:isnotempty referid="mark_error"></a> <a style='color: red; font-weight: bold' href='javascript:alert(&quot;<mm:write referid="mark_error"/>&quot;);'>!</mm:isnotempty></a></nobr></td>
                            </mm:compare>
                            <mm:compare referid="type_of_node" valueset="couplingquestions,dropquestions,hotspotquestions,openquestions,rankingquestions,valuequestions,fillquestions,fillselectquestions,essayquestions">
                               <td>&nbsp;<nobr><a href='<mm:write referid="wizardjsp"/>&wizard=config/question/<mm:write referid="type_of_node"/>&objectnumber=<mm:field name="number"/>&path=<mm:write referid="testname"/>' title='<di:translate key="education.edit" /> <%= dummyName.toLowerCase() %>' target="text"><mm:field name="title" /></a></nobr></td>
                            </mm:compare>
                        </mm:nodeinfo>
                     </tr>
                  </table>
                  </mm:node>
               </mm:related>
            </div>
         </mm:listnodes>
      </div>
   </mm:islessthan>
</mm:cloud>

