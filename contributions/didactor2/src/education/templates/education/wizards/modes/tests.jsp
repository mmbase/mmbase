<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:fn="http://java.sun.com/jsp/jstl/functions"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <jsp:output omit-xml-declaration="yes" />
  <mm:import id="imageName" />
  <mm:import id="sAltText" />
  <mm:cloud rank="basic user">
    <jsp:directive.include file="../mode.include.jsp" />
    <di:has editcontext="toetsen"> <!-- WTF, this is dutch, toetsen means 'tests'  -->

      <a href="javascript:clickNode('tests_0')">
        <img src='gfx/tree_minlast.gif' width="16" border='0' align='center' valign='middle' id='img_tests_0'/>
      </a>
      <img src='gfx/menu_root.gif' border='0' align='center' valign='middle'/> <span style='width:100px; white-space: nowrap' />
      <mm:link referid="listjsp">
        <mm:param name="wizard">config/tests/tests</mm:param>
        <mm:param name="nodepath">tests</mm:param>
        <mm:param name="fields">name</mm:param>
        <mm:param name="orderby">name</mm:param>
        <mm:param name="searchfields">name</mm:param>
        <!-- ${forbidtemplate} ??-->
        <a href="${_}" target="text"><di:translate key="education.tests" /></a>
      </mm:link>
      <br />
      <div id="tests_0" >

        <mm:listnodescontainer type="tests">
          <mm:size id="number_of_tests" write="false" />
        </mm:listnodescontainer>

        <di:leaf icon="new_education"
                 branchPath=".">
          <mm:link referid="wizardjsp">
            <mm:param name="wizard">config/tests/tests</mm:param>
            <mm:param name="objectnumber">new</mm:param>
            <a href="${_}" title="${di:translate('education.createnewtestdescription')}"
               target="text"><di:translate key="education.createnewtest" /></a>
          </mm:link>
        </di:leaf>

        <mm:listnodes type="tests" orderby="tests.name">
          <mm:import id="testname" jspvar="testname" reset="true"><mm:field name="name"/></mm:import>
          <jsp:directive.include file="../whichimage.jsp" /> <!-- WTF -->

          <di:leaf
              branchPath=". "
              click="${_node}">
            <mm:link referid="wizardjsp" referids="_node@objectnumber">
              <mm:param name="wizard">config/tests/tests</mm:param>
              <mm:param name="path">${testname}</mm:param>
              <a href="${_}"
                 title="${di:translate('education.treattest')}" target="text"><mm:field name="name" /></a>
            </mm:link>
            <mm:hasnode number="component.metadata">
              <mm:link page="metaedit.jsp" referids="_node@number">
                <a href="${_}" target="text"><img id="img_${_node}"
                src="${imageName}" border="0" title="${sAltText}" alt="${sAltTex}" /></a>
              </mm:link>
            </mm:hasnode>
          </di:leaf>
          <div id="${_node}" style="display:none">

            <mm:include page="../newfromtree_tests.jsp" referids="wizardjsp,testname">
              <!--
                  <jsp:param name="the_last_parent" value="${sTheLastParent}" />
              -->
            </mm:include>

            <!-- WTF WTF -->
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

            <mm:relatednodes role="posrel" type="questions" orderby="posrel.pos">
              <di:leaf
                  icon="edit_learnobject"
                  branchPath=". ">
                <mm:nodeinfo type="type" id="type_of_node">
                  <mm:compare value="mcquestions">
                    <mm:import id="mark_error" reset="true">Een multiple-choice vraag moet minstens 1 goed antwoord hebben</mm:import>
                    <mm:relatednodes type="mcanswers" constraints="mcanswers.correct > '0'" max="1">
                      <mm:import id="mark_error" reset="true"></mm:import>
                    </mm:relatednodes>
                    <mm:link referid="wizardjsp" referids="_node@objectnumber">
                      <mm:param name="wizard">config/question/mcquestions</mm:param>
                      <a href="${_}"
                         title="${di:translate('education.edit')}"
                         target="text">
                        <mm:field name="title" />
                        <mm:isnotempty referid="mark_error">
                          <span style="color: red; font-weight: bold" onclick="alert('${mark_error}')">!</span>
                        </mm:isnotempty>
                      </a>
                    </mm:link>
                  </mm:compare>
                  <mm:compare  valueset="couplingquestions,dropquestions,hotspotquestions,openquestions,rankingquestions,valuequestions,fillquestions,fillselectquestions,essayquestions">
                    <mm:link referid="wizardjsp" referids="_node@objectnumber">
                      <mm:param name="wizard">config/question${type_of_node}</mm:param>
                      <mm:param name="path">${testname}</mm:param>
                      <a href="${_}"
                         title="${di:translate('education.edit')}"
                         target="text"><mm:field name="title" /></a>
                    </mm:link>
                  </mm:compare>
                </mm:nodeinfo>
              </di:leaf>
            </mm:relatednodes>
          </div>
        </mm:listnodes>
      </div>
    </di:has>
  </mm:cloud>
</jsp:root>
