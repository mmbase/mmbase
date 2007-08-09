<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di"
%><mm:content postprocessor="reducespace" expires="0">
  <mm:cloud rank="didactor user">

    <mm:import externid="tests" required="true"/>
    <mm:import externid="madetest" required="true"/>
    <%@include file="/education/tests/definitions.jsp" %>
    <mm:node referid="madetest">
      <mm:field  id="madetestscore" name="score" write="false"/>
    </mm:node>

    <mm:node number="$tests">
      <mm:field id="requiredscore" name="requiredscore" write="false"/>

      <mm:relatednodes type="questions" role="posrel" orderby="posrel.pos">
        <mm:import id="questionamount"><mm:size/></mm:import>
      </mm:relatednodes>

      <mm:field id="test_name" name="name" write="false"/>
      <mm:field name="showtitle">
        <mm:compare value="1">
          <h1><mm:write referid="test_name"/></h1>
        </mm:compare>
      </mm:field>

      <%-- Which type of feedback: standard page or feedback per question --%>
      <mm:import id="feedback"><mm:field name="feedbackpage"/></mm:import>

      <%-- What is the score of the made test --%>
      <mm:relatednodescontainer type="madetests">
        <mm:constraint field="number" referid="madetest"/>
        <mm:relatednodes id="my_madetest"/>
      </mm:relatednodescontainer>


      <div class="feedback">
        <mm:compare referid="madetestscore" referid2="TESTSCORE_TBS">
          <div class="teacherwillscore">
            <di:translate key="education.feedback_teacherwillscore" />
          </div>
        </mm:compare>

        <mm:compare referid="madetestscore" referid2="TESTSCORE_INCOMPLETE">
          <div class="testincomplete">
            <di:translate key="education.feedback_testincomplete" />
          </div>
        </mm:compare>

        <mm:isgreaterthan referid="madetestscore" value="-1">
          <div class="score">
            <di:translate key="education.feedback_testscore" arg0="${madetestscore}" />
            <mm:compare referid="feedback" value="0">
              <%-- if madestestscore larger or equal than requiredscore --%>
              <mm:islessthan referid="madetestscore" referid2="requiredscore" inverse="true">
                <div class="succeed">
                  <di:translate key="education.feedback_succeed" />
                </div>
              </mm:islessthan>

              <mm:islessthan referid="madetestscore" referid2="requiredscore" >
                <div class="failed">
                  <di:translate key="education.feedback_failed" />
                </div>
              </mm:islessthan>
            </mm:compare>
          </div>
        </div>

        <%-- NODE provider is now "tests" --%>
        <mm:relatednodescontainer type="feedback">
          <mm:constraint field="maximalscore" referid="madetestscore" operator=">="/>
          <mm:constraint field="minimalscore" referid="madetestscore" operator="<="/>

          <mm:relatednodes>
            <h1><mm:field name="name"/></h1>
            <mm:relatednodes type="images">
              <img src="<mm:image template="s(150x150)"/>" title="<mm:field name="title"/>" alt="<mm:field name="title"/>">
            </mm:relatednodes>

            <p><mm:field name="text" escape="none"/></p>
            <mm:field name="number" id="number" write="false">
              <mm:list nodes="$number" path="feedback,descriptionrel,learnobjects">
                <mm:import id="description" reset="true"><mm:field name="descriptionrel.description"/></mm:import>
                <mm:isempty referid="description">
                  <mm:import id="description" reset="true"><mm:field name="learnobjects.name"/></mm:import>
                </mm:isempty>
                <mm:isempty referid="description">
                  <mm:import id="description" reset="true"><di:translate key="education.feedback_moreinfo" /></mm:import>
                </mm:isempty>
                <mm:node element="learnobjects">
                  <mm:import id="page" reset="true">/education/<mm:nodeinfo type="type"/>/index.jsp</mm:import>
                </mm:node>
                <a target="content" href="<mm:treefile page="$page" objectlist="$includePath" referids="$referids"><mm:param name="learnobject"><mm:field name="learnobjects.number" /></mm:param><mm:param name="fb_madetest"><mm:write referid="madetest" /></mm:param></mm:treefile>"><mm:write referid="description"/>
              </a>
              <br/>
            </mm:list>
          </mm:field>
        </mm:relatednodes>
      </mm:relatednodescontainer>
    </mm:isgreaterthan>
  </mm:node>

  <%-- find workgroup --%>
  <mm:node number="$user">
    <mm:relatedcontainer path="workgroups,classes" fields="workgroups.number,classes.number">
      <mm:isnotempty referid="class">
        <mm:constraint field="classes.number" value="$class"/>
      </mm:isnotempty>
      <mm:related>
        <mm:remove referid="workgroup"/>
        <mm:import id="workgroup"><mm:field name="workgroups.number"/></mm:import>
      </mm:related>
    </mm:relatedcontainer>
  </mm:node>

  <%-- get printable string --%>
  <mm:node referid="user">
    <mm:import id="user_string"><di:person /></mm:import>
  </mm:node>

  <mm:present referid="workgroup">
    <mm:node number="$workgroup">
      <mm:relatedcontainer path="people">
        <mm:related>
          <mm:field id="peopleno" name="people.number" write="false"/>

          <di:hasrole referid="peopleno" role="teacher">
            <mm:remove referid="teacherNo"/>
            <mm:field id="teacherNo" name="people.number" write="false"/>
          </di:hasrole>

        </mm:related>
      </mm:relatedcontainer>
    </mm:node>
  </mm:present>

  <mm:present referid="teacherNo">
    <mm:node referid="teacherNo">
      <mm:import jspvar="t_firstname"><mm:field name="html(firstname)" /></mm:import>
      <mm:import jspvar="t_lastname"><mm:field name="html(lastname)" /></mm:import>
      <di:translate key="education.feedback_teachernotify" arg0="<%=t_firstname%>" arg1="<%=t_lastname%>" />
      <mm:import jspvar="test_name"><mm:write referid="test_name" /></mm:import>
      <mm:import jspvar="user_string"><mm:write referid="user_string" /></mm:import>
      <mm:treeinclude page="/email/write/write.jsp" objectlist="$includePath" referids="$referids">
        <mm:param name="nooutput">true</mm:param>
        <mm:param name="to"><mm:field name="username"/></mm:param>
        <mm:param name="subject"><di:translate key="education.feedback_mailsubject" arg0="<%=user_string%>" arg1="<%=test_name%>"/></mm:param>
        <mm:param name="body"><di:translate key="education.feedback_mailbody" arg0="<%=user_string%>" arg1="<%=test_name%>" /></mm:param>
      </mm:treeinclude>
    </mm:node>
  </mm:present>
</mm:cloud>
</mm:content>

