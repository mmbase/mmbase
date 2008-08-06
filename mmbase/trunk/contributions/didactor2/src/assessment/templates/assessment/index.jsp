<jsp:root
    xmlns:jsp="http://java.sun.com/JSP/Page"
    version="2.0">

  <di:html
      expires="0"
      styleClass="assessment"
      title_key="assessment.assessment_matrix"
      xmlns:jsp="http://java.sun.com/JSP/Page"
      xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
      xmlns:di="http://www.didactor.nl/ditaglib_1.0"
      css="/assessment/css/base.css"
      component="assessmment" >

    <mm:import externid="coachmode">
      <di:hasrole role="teacher">true</di:hasrole>
      <di:hasrole role="teacher" inverse="true">false</di:hasrole>
    </mm:import>

    <div class="rows">

      <mm:include page="navigation.jspx" />

      <div class="mainContent">
        <div class="contentBody">
          <di:hasrole role="student">
            <di:hasrole role="teacher">
              <form name="coachform" method="get">
                <select name="coachmode"
                        onchange="$('form[name =coachform]').submit();">
                  <mm:option value="true" compare="${coachmode}">
                    <di:translate key="assessment.overview_students" />
                  </mm:option>
                  <mm:option value="false" compare="${coachmode}">
                    <di:translate key="assessment.personal_assessment" />
                  </mm:option>
                </select>
              </form>
            </di:hasrole>
          </di:hasrole>
          <mm:compare referid="coachmode" value="false">
            <di:include debug="html" page="/assessment/student.jspx" />
          </mm:compare>
          <mm:compare referid="coachmode" value="true">
            <di:include debug="html" page="/assessment/coach.jspx" />
          </mm:compare>
        </div>
      </div>
    </div>
  </di:html>
</jsp:root>
