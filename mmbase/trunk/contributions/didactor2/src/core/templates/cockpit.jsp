<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <mm:content postprocessor="none" expires="0">
    <mm:cloud method="asis">
      <jsp:directive.include file="/shared/setImports.jsp" />
      <mm:treeinclude page="/cockpit/cockpit_intro_header.jsp" objectlist="$includePath" referids="$referids">
        <mm:param name="extraheader">

          <!--
               TODO TODO The following is in dutch!

          -->
          <meta name="description" content="Didactor is een open source E-learning omgeving, ontwikkeld door The Mediator Group. Deze elektronische leeromgeving is 100% webbased en gebaseerd op didactische principes. Didactor is ontwikkeld mbv MMbase, Java en XML en maakt blended learning mogelijk" />
          <meta name="keywords" content="didactor, mediator, didactiek, didactisch, webbased, platformonafhankelijk, group, elo, lms, lcms, leeromgeving, on-line, java, sun, mmbase, opleidingen, opleiding, events, e-learning, blended, learning, educatie, training, brakel, becking, hof, puntedu, leren, kennisoverdracht, open, source, standaarden, scorm, eml, cursus, bedrijfsopleiding, universiteit, digitaal, digitale, onderwijs, overheid, zorg, school, congres, bijeenkomst, event, kennis, congres, leeromgeving, didactiek, IEEE-lom, EML, scorm, vraaggestuurd, leerobjecten, netg" />
          <meta name="copyright" content="" />
          <meta name="author" content="The Mediator Group" />
          <meta name="rating" content="General" />
          <meta name="robots" content="all" />
          <title>Didactor</title>
      </mm:param>
      </mm:treeinclude>

      <div class="columns">

        <div class="columnLeft">

          <img src="${mm:treelink('/gfx/logo_didactor.gif', includePath)}"
               width="100%" height="106" border="0" title="Didactor logo" alt="Didactor logo" />

          <div class="titlefield">

            <di:hasrole role="teacher">
              <di:translate key="core.giveneducation" />
            </di:hasrole>
            <di:hasrole role="teacher" inverse="true">
              <di:translate key="core.followededucation" />
            </di:hasrole>
          </div>

          <div class="ListLeft">
            <jsp:directive.include file="listleft.jsp" />
          </div>
        </div>

        <div class="columnMiddle">
          <mm:node number="$provider" notfound="skipbody">
            <mm:treeinclude page="/welcome.jsp" objectlist="$includePath" />
          </mm:node>
          <!-- only show link to public portfolios for guests -->
          <mm:node number="component.portfolio" notfound="skipbody">
            <mm:compare referid="user" value="0">
              <div>
                <a href="${mm:treelink('/portfolio/listall.jsp', includePath)}"><di:translate key="core.listallportfolios" /></a>
              </div>
            </mm:compare>
          </mm:node>
        </div>

        <div class="columnRight">
          <!-- list of all teachers that are online for a specific class -->
          <div class="titlefield2">
            <di:translate key="core.teacherheader" />
          </div>
          <div class="ListTeachers">
            <mm:treeinclude write="true" page="/users/users.jsp" objectlist="$includePath" referids="$referids">
              <mm:param name="mode">teachers</mm:param>
            </mm:treeinclude>
            <mm:treeinclude write="true" page="/users/users.jsp" objectlist="$includePath" referids="$referids">
              <mm:param name="mode">coaches</mm:param>
            </mm:treeinclude>
          </div>
          <!-- list of all students that are online for a specific class -->
          <div class="titlefield">
            <di:translate key="core.studentheader" />
          </div>
          <div class="ListStudents">
            <mm:treeinclude write="true" page="/users/users.jsp" objectlist="$includePath" referids="$referids">
              <mm:param name="mode">students</mm:param>
            </mm:treeinclude>
          </div>
        </div>
      </div>
      <mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath"  referids="$referids" />
    </mm:cloud>
  </mm:content>
</jsp:root>
