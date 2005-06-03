<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@page import="java.util.*" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<mm:import externid="tests" required="true"/>

<mm:import externid="learnobject" required="true"/>

<mm:import externid="thismadetest" required="true"/>

<mm:import externid="questionsshowed" jspvar="questionsShowed" required="true"/>

<mm:import externid="pagecounter" jspvar="pageCounter" vartype="Integer"/>

<mm:import externid="questionamount" jspvar="questionAmount" vartype="Integer"/>



<%@include file="/shared/setImports.jsp" %>

<%@include file="/education/tests/definitions.jsp" %>

<mm:import externid="student" reset="true"><mm:write referid="user"/></mm:import>

<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">

<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$popreferids">
  <mm:param name="extraheader">
    <title>POP</title>
    <link rel="stylesheet" type="text/css" href="css/pop.css" />
  </mm:param>
</mm:treeinclude>

<!-- TODO where are the different roles described -->
<!-- TODO different things to do with different roles? -->

<% boolean isEmpty = true; 
   String msgString = "";
%>

<%@ include file="getids.jsp" %>

<%@ include file="leftpanel.jsp" %>

<%-- right section --%>
<div class="mainContent"> 
<div class="contentHeader">Voortgangsmonitor
  <di:hasrole referid="user" role="teacher">
    <mm:node number="$student">
      : <mm:field name="firstname"/> <mm:field name="lastname"/>
    </mm:node>
  </di:hasrole>
</div>
  <div class="contentBody">

<mm:node number="$tests" id="my_tests">

   

  <%-- Only the first time a madetests object is created --%>

  <mm:compare referid="thismadetest" value="">



    <%-- Save testresults --%>

    <mm:createnode type="madetests" id="madetest">



      <% long currentDate = System.currentTimeMillis() / 1000; %>

      <mm:setfield name="date"><%=currentDate%></mm:setfield>

      <mm:setfield name="score"><mm:write referid="TESTSCORE_INCOMPLETE"/></mm:setfield>

    </mm:createnode>

    <mm:createrelation role="related" source="my_tests" destination="madetest"/>



    <%-- Make relation between copybooks instance and the madetest --%>

    <mm:node number="$student">

      <mm:relatedcontainer path="classrel,classes">

        <mm:constraint field="classes.number" value="$class"/>

        <mm:related>

          <mm:node element="classrel">

            <mm:relatednodes type="copybooks" id="copybookID">

            </mm:relatednodes>

          </mm:node>

        </mm:related>  

      </mm:relatedcontainer>

    </mm:node>



    <mm:relatednodescontainer path="madetests,copybooks" element="madetests">

        <mm:constraint field="copybooks.number" referid="copybookID"/>

        <mm:relatednodes>

            <mm:relatednodescontainer type="givenanswers">

                <%--Remove Made test with  <mm:size/> answers<br/> --%>

                <mm:relatednodes>

                    <mm:maydelete>

                        <mm:deletenode deleterelations="true"/>

                    </mm:maydelete>

                </mm:relatednodes>

            </mm:relatednodescontainer>

            <mm:maydelete>

                <mm:deletenode deleterelations="true"/>

            </mm:maydelete>

        </mm:relatednodes>

    </mm:relatednodescontainer>



    <%-- Make relation between copybooks instance and the madetest --%>

    <mm:node number="$student">

      <mm:relatedcontainer path="classrel,classes">

        <mm:constraint field="classes.number" value="$class"/>

        <mm:related>

          <mm:node element="classrel">

            <mm:relatednodes type="copybooks" id="my_copybook">

              <mm:createrelation role="related" source="my_copybook" destination="madetest"/>

            </mm:relatednodes>

          </mm:node>

        </mm:related>  

      </mm:relatedcontainer>

    </mm:node>

    

  </mm:compare>

  

  <%-- Reuse the madetests object --%>

  <mm:compare referid="thismadetest" value="" inverse="true">

    <mm:node number="$thismadetest" id="madetest"/>

  </mm:compare>





  <%-- build list of all shown questions until now --%>

  <mm:import id="list" jspvar="list" vartype="List"><mm:write referid="questionsshowed"/></mm:import>

 

  <%



    //

    // iterate over the shown questions in order, so we

    // create the givenanswers objects in the given order too...

    //

    // this is needed because otherwise there is no way to determine

    // the order in which the questions were answered after this point!

    //

    Iterator i = list.iterator();

    while (i.hasNext()) {

        String qNumber = ((String) i.next()).replaceAll("_","");

        %>

  <%-- Examine different questions and save the given answers --%>

  <mm:node number="<%= qNumber %>">



    <%-- Which questions have been answered --%>

    <mm:import id="question" reset="true">shown<mm:field name="number"/></mm:import>

    <mm:import externid="$question" id="shownquestion" reset="true"/>

    

    <mm:import id="possiblequestion" reset="true"><mm:field name="number"/></mm:import>

    <%-- Only rate the answered question --%>



    <mm:compare referid="shownquestion" referid2="possiblequestion">



      <mm:relatednodescontainer path="givenanswers,madetests" element="givenanswers">

        <mm:constraint field="madetests.number" referid="madetest"/>

        <mm:relatednodes>

          <%-- Already an answer given to this question --%>

          <mm:deletenode deleterelations="true"/>

        </mm:relatednodes>

      </mm:relatednodescontainer>



      <mm:import id="page" reset="true">/education/<mm:nodeinfo type="type"/>/rate<mm:nodeinfo type="type"/>.jsp</mm:import>

      <mm:treeinclude page="$page" objectlist="$includePath" referids="$popreferids">

        <mm:param name="question"><mm:field name="number"/></mm:param>

        <mm:param name="madetest"><mm:write referid="madetest"/></mm:param>

      </mm:treeinclude>

    </mm:compare>



    <mm:remove referid="possiblequestion"/>    

    <mm:remove referid="page"/>

  </mm:node>

  <% } %>







  <%-- If all questions are answerd then show the feedback else show next question set --%>

  <% 

     if ( list.size() == questionAmount.intValue() ) {

  %>

       <mm:treeinclude page="/education/tests/totalscore.jsp"  objectlist="$includePath" referids="$popreferids">

         <mm:param name="madetest"><mm:write referid="madetest"/></mm:param>

         <mm:param name="tests"><mm:write referid="tests"/></mm:param>

       </mm:treeinclude>

       <input type="button" class="formbutton" value="<di:translate id="buttontextnext">Volgende</di:translate>" 
	     onClick="top.location.href='<mm:treefile page="/pop/index.jsp" objectlist="$includePath" referids="$popreferids,currentfolder">
             <mm:param name="command">intake</mm:param>
           </mm:treeinclude>'"> 

   <%

     } else {

   %>

         <mm:treeinclude page="/pop/buildtest.jsp"  objectlist="$includePath" referids="$popreferids">

           <mm:param name="learnobject"><mm:write referid="learnobject"/></mm:param>

           <mm:param name="madetest"><mm:write referid="madetest"/></mm:param>

           <mm:param name="questionsshowed"><mm:write referid="questionsshowed"/></mm:param>

           <mm:param name="pagecounter"><mm:write referid="pagecounter"/></mm:param>

         </mm:treeinclude>

   <%

     }

   %>



</mm:node>

  </div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$popreferids" />
</fmt:bundle>
</mm:cloud>
</mm:content>
