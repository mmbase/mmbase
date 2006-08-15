<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@page import="org.mmbase.bridge.*" %>

<mm:content postprocessor="reducespace">
<mm:cloud method="delegate" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%@include file="includes/geteducation.jsp" %>
<%@include file="includes/getlesson.jsp" %>
<%@include file="includes/variables.jsp" %>
<%@include file="includes/functions.jsp" %>
<%@include file="/education/tests/definitions.jsp" %>

<%-- find users copybook --%>
<mm:import id="class" reset="true">null</mm:import>
<mm:import id="education" reset="true"><mm:node number="$assessment_education"><mm:field name="number"/></mm:node></mm:import>
<mm:node number="$user">
  <%@include file="/education/tests/find_copybook.jsp"%>
</mm:node>

<mm:import externid="step">-1</mm:import>
<mm:import externid="problem_n">-1</mm:import>
<mm:import externid="problemname"/>
<mm:import externid="problemtype">-1</mm:import>
<mm:import externid="problemrating"/>
<mm:import externid="testpos">1</mm:import>

<% int problemrating = -1; // not rated %>

<mm:compare referid="step" value="cancel">
  <mm:redirect page="/assessment/index.jsp" referids="$referids"/>
</mm:compare>

<mm:compare referid="step" value="save">
  <mm:compare referid="problem_n" value="-1">
    <mm:maycreate type="problems">
      <mm:remove referid="problem_n"/>
      <mm:createnode type="problems" id="problem_n">
      </mm:createnode>
      <mm:createrelation role="posrel" source="user" destination="problem_n">
        <mm:setfield name="pos"><%= getMaxPos(cloud,thisUser,"problems")+1 %></mm:setfield>
      </mm:createrelation>
    </mm:maycreate>
  </mm:compare>
  
  <mm:node referid="problem_n" notfound="skip">
    <mm:setfield name="name"><mm:write referid="problemname"/></mm:setfield>
    <mm:node number="<%= currentLesson %>" id="currentlesson"/>
    <mm:related path="posrel,learnblocks" constraints="<%= "learnblocks.number=" + currentLesson %>">
      <mm:node element="posrel">
        <mm:deletenode/>
      </mm:node>
    </mm:related>
    <mm:createrelation role="posrel" source="problem_n" destination="currentlesson"/>
    <mm:related path="posrel,learnblocks" constraints="<%= "learnblocks.number=" + currentLesson %>">
      <mm:node element="posrel">
        <mm:setfield name="pos"><mm:write referid="problemrating"/></mm:setfield>
      </mm:node>
    </mm:related>
    <mm:related path="related,problemtypes">
      <mm:node element="related">
        <mm:deletenode/>
      </mm:node>
    </mm:related>
    <mm:createrelation role="related" source="problem_n" destination="problemtype"/>
  </mm:node>
  <mm:node number="$problemtype">
    <mm:import id="testpos" reset="true"><mm:field name="pos"/></mm:import>
  </mm:node>
  <mm:node number="<%= currentLesson %>" notfound="skip">
    <%@include file="includes/getmadetest.jsp" %>
    <mm:related path="posrel1,tests,posrel2,questions" constraints="posrel1.pos=$testpos">
      <mm:node element="questions">
        <mm:import id="page" reset="true">/education/<mm:nodeinfo type="type"/>/rate<mm:nodeinfo type="type"/>.jsp</mm:import>
        <mm:treeinclude page="$page" objectlist="$includePath" referids="$referids">
          <mm:param name="question"><mm:field name="number"/></mm:param>
          <mm:param name="madetest"><mm:write referid="madetest"/></mm:param>
        </mm:treeinclude>
      </mm:node>
    </mm:related>
  </mm:node>
  
  
  <mm:redirect page="/assessment/index.jsp" referids="$referids"/>
</mm:compare>

  <mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
    <mm:param name="extraheader">
      <title><di:translate key="assessment.assessment_matrix" /></title>
      <link rel="stylesheet" type="text/css" href="css/assessment.css" />
    </mm:param>
  </mm:treeinclude>

  <div class="rows">
    <div class="navigationbar">
      <div class="titlebar">
        <img src="<mm:treefile write="true" page="/gfx/icon_pop.gif" objectlist="$includePath" />" 
            width="25" height="13" border="0" title="<di:translate key="assessment.assessment_matrix" />" alt="<di:translate key="assessment.assessment_matrix" />" /> <di:translate key="assessment.assessment_matrix" />
      </div>		
    </div>

<div class="folders">
  <div class="folderHeader">
  </div>
  <div class="folderBody">
  </div>
</div>


    <%-- right section --%>
    <div class="mainContent">
      <div class="contentBody">

  <mm:node number="$problem_n" notfound="skip">
    <mm:import id="problemname" reset="true"><mm:field name="name"/></mm:import>
    <mm:related path="posrel,learnblocks" constraints="<%= "learnblocks.number=" + currentLesson %>">
      <mm:field name="posrel.pos" jspvar="problem_weight" vartype="Integer" write="false">
<%
        try {
          problemrating = problem_weight.intValue();
        }
        catch (Exception e) {
        }
%>
      </mm:field>
    </mm:related>
    <mm:compare referid="problemtype" value="-1">
      <mm:relatednodes type="problemtypes">
        <mm:import id="problemtype" reset="true"><mm:field name="number"/></mm:import>
      </mm:relatednodes>
      <mm:node number="$problemtype" notfound="skip">
        <mm:import id="testpos" reset="true"><mm:field name="pos"/></mm:import>
      </mm:node>
    </mm:compare>
  </mm:node>
  
  <form name="questionform" action="<mm:treefile page="/assessment/editproblem.jsp" objectlist="$includePath" 
          referids="$referids"/>" method="post">
    <input type="hidden" name="step" value="save">
    <input type="hidden" name="problem_n" value="<mm:write referid="problem_n"/>">
    <table class="font" width="70%">
      <tr>
        <td width="80"><di:translate key="assessment.problem" />:</td>
        <td align="right"><input name="problemname" class="popFormInput" type="text" size="50" maxlength="255" value="<mm:write referid="problemname"/>"></td>
      </tr>
      <tr>
        <td><di:translate key="assessment.type" /></td>
        <td align="right">
          <select name="problemtype" onchange="questionform.step.value='next';questionform.submit();">
            <mm:listnodes type="problemtypes" orderby="pos">
              <mm:field name="number" jspvar="problemtypeId" vartype="String">
                <option value="<%= problemtypeId %>"<mm:compare referid="problemtype" value="<%= problemtypeId %>"> selected</mm:compare>
                        ><mm:field name="key" jspvar="dummy" vartype="String" write="false"
                           ><di:translate key="<%= "assessment." + dummy %>"
                        /></mm:field></option>
              </mm:field>
            </mm:listnodes>
          </select>
        </td>
      </tr>
      <tr>
        <td><di:translate key="assessment.how_much_trouble" /></td>
        <td align="right">
          <select name="problemrating">
            <% for(int i=2;i<=10;i+=2) { %>
              <option value="<%= i %>"<% if (problemrating == i) {%> selected<% } %>><%= problemWeights[i] %></option>
            <% } %>
          </select>
        </td>
      </tr>
      <mm:node number="<%= currentLesson %>" notfound="skip">
        <tr>
          <td><b><di:translate key="assessment.form" /></b></td><td></td>
        </tr>
        <tr>
          <td colspan="2">
            <%@include file="includes/getmadetest.jsp" %>
            <mm:related path="posrel1,tests,posrel2,questions" constraints="posrel1.pos=$testpos">
              <mm:node element="questions">
                <mm:import id="page" reset="true">/education/<mm:nodeinfo type="type"/>/index.jsp</mm:import>
                <mm:treeinclude page="$page" objectlist="$includePath" referids="$referids">
                  <mm:param name="question"><mm:field name="number"/></mm:param>
                  <mm:param name="testnumber"><mm:write referid="this_test"/></mm:param>
                  <mm:param name="madetest"><mm:write referid="madetest"/></mm:param>
                </mm:treeinclude>
              </mm:node>
            </mm:related>
          </td>
        </tr>
      </mm:node>
    </table>
    <input type="submit" class="formbutton" value="<di:translate key="assessment.save" />">
    <input type="submit" class="formbutton" value="<di:translate key="assessment.cancel" />" onClick="questionform.step.value='cancel'">
  </form>



      </div>
    </div>
  </div>
  <mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</mm:cloud>
</mm:content>
