<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@page import="java.util.*"%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<%@include file="/shared/setImports.jsp" %>
<%@include file="/education/tests/definitions.jsp" %>

<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title>Voortgang</title>
  </mm:param>
</mm:treeinclude>

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    Voortgang
  </div>		
</div>

<div class="folders">
  <div class="folderHeader">
    &nbsp;
  </div>
  <div class="folderBody">
    &nbsp;
  </div>
</div>

<mm:import id="student" reset="true"><mm:write referid="user"/></mm:import>
<di:hasrole role="teacher"><mm:import id="student" externid="student" reset="true"/></di:hasrole>
<mm:isempty referid="student">
    <mm:import id="student" reset="true"><mm:write referid="user"/></mm:import>
</mm:isempty>

<div class="mainContent">
  <div class="contentHeader">
<%--    Some buttons working on this folder--%>
    <mm:node referid="student">
        <mm:field name="firstname"/> <mm:field name="lastname"/>
    </mm:node>
  </div>
  <div class="contentBodywit">


<mm:import externid="showfeedback"/>
<mm:present referid="showfeedback">
    <mm:import externid="madetest" required="true"/>
    <mm:import externid="tests" required="true"/>
    <mm:node number="$tests">
    <b>Uitslag van test <mm:field name="name"/></b><br>
    </mm:node>
    <mm:treeinclude page="/education/tests/feedback.jsp" objectlist="$includePath" referids="$referids" >
            <mm:param name="madetes"><mm:write referid="madetest"/></mm:param>
            <mm:param name="tests"><mm:write referid="tests"/></mm:param>
    </mm:treeinclude>
</mm:present>
<mm:notpresent referid="showfeedback">
 

<di:hasrole role="teacher">
<div style="float: right">
<a href="<mm:treefile page="/progress/index.jsp" objectlist="$includePath" referids="$referids"/>">Terug naar overzicht</a>
</div>
</di:hasrole>
 
<table class="Font">
<tr>
    <td>Percentage doorlopen:</td><td>
        <mm:import jspvar="progress" id="progress" vartype="Double"><mm:treeinclude page="/progress/getprogress.jsp" objectlist="$includePath" referids="$referids">
            <mm:param name="student"><mm:write referid="student"/></mm:param>
        </mm:treeinclude></mm:import>
        <%= (int)(progress.doubleValue()*100.0)%>%
    </td>
</tr>


  <mm:list fields="classrel.number" path="people,classrel,classes" constraints="people.number=$student and classes.number=$class">
	<mm:field name="classrel.number" id="classrel" write="false"/>
    </mm:list>
    <mm:node referid="classrel">

<tr>
    <td>Aantal maal ingelogd:</td>
    <td>
        <mm:field name="logincount"/>
    </td>
</tr>
<tr>
    <td>Duur inloggen</td>
    <td>
        <mm:field name="onlinetime" jspvar="onlinetime" vartype="Integer" write="false">
        <%
        int hour = onlinetime.intValue() / 3600;
        int min = (onlinetime.intValue() % 3600) / 60;
        %>
        <%=hour%>:<%=min%>
        </mm:field>
        </mm:node>
    </td>
</tr>
</table>
<p>
<b>Educatie traject:</b>
</p>
<table class="Font">
<mm:node number="$education">

  <% boolean first = true; %>
  <mm:relatednodescontainer type="learnblocks" role="posrel">
    <mm:sortorder field="posrel.pos" direction="up"/>
    <mm:tree type="learnblocks" role="posrel" searchdir="destination" orderby="posrel.pos" direction="up">
        <mm:import jspvar="depth" vartype="Integer"><mm:depth/></mm:import>
        <% if (depth.intValue() == 2) { %><mm:first inverse="true"></td></tr></mm:first><tr><td><mm:field name="name"/></td><td><% 
            first = true;
        } else if (depth.intValue() > 2 ) { 
            if (!first) { 
                %>, <%
            } else { 
                first = false; 
            }
            %><mm:field name="name"/><%
        } %>
    <mm:last></td></tr></mm:last>
   </mm:tree>
  </mm:relatednodescontainer>
</mm:node>
</table>


    
<mm:node referid="student">
<p>
  <b>Toetsen van <mm:field name="firstname"/> <mm:field name="lastname"/></b>
</p>

  <table class="listTable">

    <tr>
        <th class="listHeader">Leerblok</th>
        <th class="listHeader">Toets</th>
        <th class="listHeader">Vragen</th>
        <th class="listHeader">Score</th>
        <th class="listHeader">Nodig</th>
        <th class="listHeader">Geslaagd</th>
    </tr>
<%-- find copybook --%>
  <mm:import id="copybookNo"/>
  <mm:relatedcontainer path="classrel,classes">
    <mm:constraint field="classes.number" value="$class"/>
    <mm:related>
      <mm:node element="classrel">
        <mm:relatednodes type="copybooks">
          <mm:remove referid="copybookNo"/>
          <mm:field id="copybookNo" name="number" write="false"/>
        </mm:relatednodes>
      </mm:node>
    </mm:related>  
  </mm:relatedcontainer>

<mm:node number="$education">
  <% List blockName = new ArrayList(); %>
  <mm:relatednodescontainer type="learnblocks" role="posrel">
    <mm:sortorder field="posrel.pos" direction="up"/>
    <mm:tree type="learnblocks" role="posrel" searchdir="destination" orderby="posrel.pos" direction="up">
        <mm:import jspvar="depth" vartype="Integer"><mm:depth/></mm:import>
        <% 
            for (int i = blockName.size() - 1; i < depth.intValue() -1; i++) {
                blockName.add(null);
            }
            while (depth.intValue() -1 < blockName.size()) {
                blockName.remove( blockName.size() -1);
            }
 
        %>
      <mm:field name="name" jspvar="thisBlockName" vartype="String">
        <% blockName.set(blockName.size()-1,thisBlockName); %>
      </mm:field>

        <mm:relatednodescontainer type="tests" role="posrel">
          <mm:sortorder field="posrel.pos" direction="up"/>
            <mm:relatednodes>
        <mm:import id="testNo" reset="true"><mm:field  name="number" /></mm:import>
        <mm:field id="feedback" name="feedbackpage" write="false"/>
        <tr> 


        <mm:relatednodescontainer path="madetests,copybooks" element="madetests">
            <mm:constraint field="copybooks.number" referid="copybookNo"/>
            <mm:relatednodes>
                <mm:field id="madetestNo" name="number" write="false"/>
            </mm:relatednodes>
        </mm:relatednodescontainer>

        
        <td class="listItem"><% for (int i=0; i<blockName.size(); i++) {%><%= (String) blockName.get(i) %><% if (i < blockName.size()-1 ) { %> &gt; <% } } %></td>

        <%@include file="teststatus.jsp"%>

        
        <td class="listItem">
        <mm:compare referid="teststatus" value="incomplete">
            <mm:field name="name"/>
        </mm:compare>
        <mm:compare  referid="teststatus" value="incomplete" inverse="true">
        <a href="<mm:treefile page="/progress/student.jsp" objectlist="$includePath" referids="$referids" >
            <mm:param name="madetest"><mm:write referid="madetestNo"/></mm:param>
            <mm:param name="tests"><mm:write referid="testNo"/></mm:param>
            <mm:param name="showfeedback">true</mm:param>
        </mm:treefile>"/><mm:field name="name"/></a>
        </mm:compare>
        
        </td>

        

        <td class="listItem">
            <mm:field name="questionamount" write="false">
                <mm:islessthan value="1">
                    <mm:countrelations type="questions" write="true"/>
                </mm:islessthan>
                <mm:isgreaterthan value="0">
                    <mm:write/>
                </mm:isgreaterthan>
            </mm:field>
       </td> 

        <td class="listItem"><mm:write referid="save_madetestscore"/></td>
 
        <td class="listItem"><mm:write referid="requiredscore"/></td>
       
             <mm:compare referid="teststatus" value="toberated">
             <td class="listItem">Nog niet nagekeken</td>
             </mm:compare>
            
             <mm:compare referid="teststatus" value="passed">
       	     <td class="listItem">Ja</td>
             </mm:compare>
             
             <mm:compare referid="teststatus" value="failed">
             <td class="listItem">Nee</td>
            </mm:compare>
            <mm:compare referid="teststatus" value="incomplete" >
             <td class="listItem">Niet afgemaakt</td>
             </mm:compare>
        </tr>
        <mm:remove referid="madetestscore"/>
         <mm:remove referid="save_madetestscore"/>
         <mm:remove referid="testNo"/>
        </mm:relatednodes> <%-- tests --%>
      </mm:relatednodescontainer>
    </mm:tree>
  </mm:relatednodescontainer> <%-- learnblocks --%>
</mm:node> <%-- education --%>
<mm:remove referid="copybookNo"/>
</mm:node>

</mm:notpresent>
</table>
 </div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</mm:cloud>
</mm:content>
