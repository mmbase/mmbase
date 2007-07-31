<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" 
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" 
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" 
%><mm:content postprocessor="reducespace" expires="0">
<mm:cloud rank="didactor user" jspvar="cloud">
  
  <mm:import externid="learnobject" required="true"/>
  <mm:import externid="madetest" />
  
  <mm:import externid="page" vartype="integer">0</mm:import>
  
  <mm:node id="learnobject" referid="learnobject" />
  <di:copybook><mm:node id="copybookNo" /></di:copybook>

  <mm:present referid="copybookNo">
    
    <mm:notpresent referid="madetest" >
      <mm:remove referid="madetest" />     
      <mm:import externid="clearmadetest">false</mm:import>
      <mm:node referid="copybookNo">
        <mm:nodefunction id="madetest" name="madetest" referids="learnobject@test,clearmadetest@clear" />
      </mm:node>
    </mm:notpresent>
  </mm:present>


  <div class="learnenvironment">
    <!-- Take care: form name is used in JavaScript of the specific question jsp pages! -->
    <mm:treefile id="post" page="/education/tests/rate.jsp" objectlist="$includePath" referids="$referids,madetest@thismadetest" write="false"/>
    <form name="questionform"           
          action="${post}"
          method="POST">
      
      <mm:node number="$learnobject" id="test">
        <mm:field name="showtitle">
          <mm:compare value="1">
            <h1><mm:field name="name"/></h1>
          </mm:compare>
        </mm:field>

        <mm:hasfield name="text"><mm:field name="text" escape="toxml"/></mm:hasfield>

        <mm:present referid="copybookNo">
          <mm:node referid="madetest">
            <mm:compare referid="page" value="0">
              <mm:setfield name="testpath" />
            </mm:compare>
            <mm:field name="testpath" write="false" vartype="list">
              <c:choose>
                <c:when test="${empty _}">
                  <mm:nodelistfunction node="test" name="questions" id="questions" referids="copybookNo@seed" />
                </c:when>
                <c:otherwise>
                  <mm:listnodes referid="_" id="questions" />
                </c:otherwise>
              </c:choose>
            </mm:field>
            <mm:relatednodes role="related"  type="givenanswers" id="givenanswers" />
          </mm:node>
        </mm:present>

        <mm:nodelistfunction name="questions" id="my_questions" referids="copybookNo?@seed,page" />

        <mm:write session="my_questions" referid="my_questions" />
                
        <mm:listnodes referid="my_questions">

          <mm:present referid="copybookNo">
            <mm:nodeinfo type="type">
              <mm:treeinclude 
                  debug="xml"
                  page="/education/${_}/index.jsp" 
                  objectlist="$includePath" referids="$referids,_node@question,learnobject@testnumber,madetest" />
            </mm:nodeinfo>
          </mm:present>

          <mm:notpresent referid="copybookNo">
            <mm:nodeinfo type="type">
              <div class="${_}">
                <h1 ><mm:field name="title" /></h1>
                <mm:field name="text" escape="toxml"/>
              </div>
            </mm:nodeinfo>
          </mm:notpresent>
          <input type="hidden" name="shown${_node}" value="${_node}" />
        </mm:listnodes>
          

        <!-- Arguments for rating -->
        <input type="hidden" name="learnobject" value="${learnobject}" />
        <input type="hidden" name="thismadetest" value="${madetest}" />
        <input type="hidden" name="page" value="${page}" />
        <input type="hidden" name="command" value="next" />
        <mm:nodeinfo type="type">
          <input type="hidden" name="${_}" value="${_node}" />
        </mm:nodeinfo>
        <input type="hidden" name="testpath" value="${questions}"/>

        <c:if test="${fn:length(my_questions) lt 1}">
          <di:translate key="education.testwithoutquestions" />
        </c:if>
        
        <mm:present referid="copybookNo">
          <!-- Determine if all questions are showed -->
          <c:choose>
            <c:when test="${fn:length(my_questions) + fn:length(givenanswers) ge fn:length(questions)}">
              <input type="submit" value="${di:translate(pageContext, 'education.buttontextdone')}" class="formbutton" 
                     onClick="questionform.command.value='done'; postContent('${post}', questionform);"/>
            </c:when>
            <c:otherwise>
              <c:if test="${page gt 0}">
                <input type="button" 
                       value="${di:translate(pageContext, 'education.buttontextprev')}" 
                       class="formbutton" 
                       onClick="questionform.command.value='back'; postContent('${post}', questionform);" />
              </c:if>
              <c:if test="${learnobject.questionsperpage gt 0 or page * learnobject.questionsperpage lt fn:length(questions)}">
                <input type="button" 
                       value="${di:translate(pageContext, 'education.buttontextnext')}" 
                       class="formbutton"
                       onClick="postContent('${post}', questionform);" />
              </c:if>
              <c:if test="${learnobject.questionsperpage lt 1 or page * learnobject.questionsperpage ge fn:length(questions)}">
                <input type="button"
                       value="${di:translate(pageContext, 'education.buttontextdone')}" 
                       class="formbutton" 
                       onClick="questionform.command.value='done'; postContent('${post}', questionform);" />
              </c:if>
            </c:otherwise>
          </c:choose>
        </mm:present>
      </mm:node>
    </form>
    <mm:notpresent referid="copybookNo">
      <di:translate key="education.nocopybookfound" />
    </mm:notpresent>
  </div>
</mm:cloud>
</mm:content>
