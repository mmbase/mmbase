<jsp:root
    version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:di="http://www.didactor.nl/ditaglib_1.0" >
  <mm:content type="application/xml" postprocessor="none" expires="0">
    <mm:cloud rank="didactor user">

      <mm:import externid="learnobject"  required="true"/>
      <mm:import externid="my_questions"  from="session" vartype="list" />

      <mm:import externid="command">next</mm:import>

      <mm:node referid="learnobject" id="my_tests">

        <di:copybook>
          <!--
               update the 'testpath' field of the made test object
          -->
          <mm:node>
            <mm:nodefunction id="madetest" name="madetest" referids="my_tests@test">
              <mm:setfield name="testpath"><mm:field name="testpath"><mm:write /><mm:isnotempty>,</mm:isnotempty>${my_questions}</mm:field></mm:setfield>
              <mm:import id="testpath" vartype="list"><mm:field name="testpath" /></mm:import>
            </mm:nodefunction>
          </mm:node>
        </di:copybook>

        <mm:listnodes referid="my_questions">
          <mm:import id="question">shown<mm:field name="number"/></mm:import>
          <mm:import externid="$question" id="shownquestion" />

          <!-- delete previously given answers -->
          <mm:relatednodescontainer path="givenanswers,madetests" element="givenanswers">
            <mm:constraint field="madetests.number" referid="madetest"/>
            <mm:relatednodes>
              <mm:deletenode deleterelations="true"/>
            </mm:relatednodes>
          </mm:relatednodescontainer>

          <!-- rate the given answers, and lay (new) relations -->
          <mm:import id="ratepage" reset="true">/education/<mm:nodeinfo type="type"/>/rate<mm:nodeinfo type="type"/>.jsp</mm:import>
          <mm:treeinclude page="$ratepage" objectlist="$includePath" referids="$referids,madetest,_node@question" />
        </mm:listnodes>

        <c:choose>
          <c:when test="${command eq 'done'}">
            <!--
                 If "done" pressed then show the feedback
            -->
            <div>
              <mm:field name="feedbackpage">
                <mm:compare value="0">
                  <mm:treeinclude page="/education/tests/totalscore.jsp"  objectlist="$includePath"
                                  referids="$referids,madetest,_node@tests" />
                  <mm:treeinclude page="/education/tests/feedback.jsp" objectlist="$includePath"
                                  referids="$referids,madetest,_node@tests" />
                </mm:compare>

                <mm:compare value="0" inverse="true">
                  <mm:treeinclude page="/education/tests/viewanswersframe.jsp" objectlist="$includePath"
                                  referids="$referids,_node@testNo,madetest@madetestNo,user@userNo" />
                </mm:compare>

              </mm:field>
            </div>
          </c:when>
          <c:otherwise>
            <!--
                 else show next question set
            -->
            <mm:import externid="page" required="true" vartype="integer" />
            <mm:treeinclude page="/education/tests/buildtest.jsp"  objectlist="$includePath" referids="$referids,learnobject,madetest">
              <mm:param name="page">${page + 1}</mm:param>
            </mm:treeinclude>
          </c:otherwise>
        </c:choose>


      </mm:node>
    </mm:cloud>
  </mm:content>
</jsp:root>
