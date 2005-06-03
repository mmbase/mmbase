<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@page import="java.util.*" %>
<mm:content postprocessor="reducespace">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
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

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <img src="<mm:treefile write="true" page="/gfx/icon_pop.gif" objectlist="$includePath" />" width="25" height="13" border="0" alt="persoonlijk ontwikkelings plan" /> Persoonlijk ontwikkelings plan
  </div>		
</div>

<%@ include file="leftpanel.jsp" %>

<di:hasrole referid="user" role="teacher" inverse="true">
  <mm:import id="whatselected" reset="true">student</mm:import>
</di:hasrole>
<%-- right section --%>
<div class="mainContent">
<mm:compare referid="command" value="getinvite">
  <mm:import id="currentpop" reset="true">0</mm:import>
</mm:compare>
<mm:compare referid="currentpop" value="-1">
  <div class="contentBody"> 
    <p>Er is geen POP voor jou aangemaakt. Neem contact op met de systeembeheerder om een POP voor je aan te maken.</p>
  </div>
</mm:compare>
<mm:compare referid="currentpop" value="-1" inverse="true">
<mm:compare referid="currentfolder" value="-1">
  <div class="contentHeader">Competenties <mm:compare referid="currentprofile" value="-1" inverse="true"
      ><mm:node number="$currentprofile"><mm:field name="name"/></mm:node></mm:compare>
    <di:hasrole referid="user" role="teacher">
      <mm:node number="$student">
        : <mm:field name="firstname"/> <mm:field name="lastname"/>
      </mm:node>
    </di:hasrole>
  </div>
  <%@ include file="todo.jsp" %>
  <%@ include file="docs.jsp" %>
  <mm:compare referid="command" value="continue">
    <mm:remove referid="command"/>
    <mm:import id="command">editcomp</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="savecomp">
    <%@ include file="savecomp.jsp" %>
    <% msgString = "Uw zelfbeoordeling is aangemaakt"; %>
    <mm:remove referid="command"/>
    <mm:import id="command">no</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="sendinvite">
    <%@ include file="sendinvite.jsp" %>
    <% msgString = "De uitnodiging voor een beoordeling over deze competentie is verstuurd"; %>
    <mm:remove referid="command"/>
    <mm:import id="command">editcomp</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="invite">
    <%@ include file="invite.jsp" %>
    <mm:remove referid="command"/>
    <mm:import id="command">-1</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="getinvite">
    <%@ include file="getinvite.jsp" %>
    <mm:remove referid="command"/>
    <mm:import id="command">-1</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="sendfeedback">
    <%@ include file="sendfeedback.jsp" %>
    <mm:remove referid="command"/>
    <mm:import id="command">no</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="editcomp">
    <jsp:include page="compedit.jsp">
      <jsp:param name="msg" value="<%= msgString %>"/>
    </jsp:include>
  </mm:compare>
  <mm:compare referid="command" value="no">
    <jsp:include page="comptable.jsp">
      <jsp:param name="msg" value="<%= msgString %>"/>
    </jsp:include>
  </mm:compare>
</mm:compare>
<mm:compare referid="currentfolder" value="1">
<di:hasrole referid="user" role="teacher">
  <mm:compare referid="whatselected" value="class">
    <jsp:include page="coachpredetail.jsp"/>
  </mm:compare>
  <mm:compare referid="whatselected" value="wgroup">
    <jsp:include page="coachpredetail.jsp"/>
  </mm:compare>
</di:hasrole>
  <mm:compare referid="whatselected" value="student">
  <div class="contentHeader">Voortgangsmonitor
    <di:hasrole referid="user" role="teacher">
      <mm:node number="$student">
        : <mm:field name="firstname"/> <mm:field name="lastname"/>
      </mm:node>
    </di:hasrole>
  </div>
  <mm:compare referid="command" value="intake">
    <mm:import id="competencies" jspvar="competencies" />
    <jsp:include page="intaketest.jsp">
      <jsp:param name="competencies" value="<%= competencies %>"/>
    </jsp:include>
    <mm:remove referid="command"/>
    <mm:import id="command">-1</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="detail">
    <jsp:include page="progressdetail.jsp"/>

    <mm:remove referid="command"/>
    <mm:import id="command">-1</mm:import>
  </mm:compare>
  <mm:compare referid="command" value="no">
    <jsp:include page="voortgang.jsp">
      <jsp:param name="msg" value="<%= msgString %>"/>
    </jsp:include>
  </mm:compare>
  </mm:compare>
</mm:compare>
<mm:compare referid="currentfolder" value="2">
  <div class="contentHeader">Persoonlijke taken
    <di:hasrole referid="user" role="teacher">
      <mm:node number="$student">
        : <mm:field name="firstname"/> <mm:field name="lastname"/>
      </mm:node>
    </di:hasrole>
  </div>
  <%@ include file="todo.jsp" %>
  <mm:compare referid="command" value="-1" inverse="true">
    <jsp:include page="todolist.jsp">
      <jsp:param name="msg" value="<%= msgString %>"/>
    </jsp:include>
  </mm:compare>
</mm:compare>
  </div>
</div>
</mm:compare>

<di:hasrole referid="user" role="teacher">
<mm:compare referid="whatselected" value="0">
  </div>
</div>
<div class="mainContent">
  <div class="contentHeader">Selecteer een student</div>
  <div class="contentBody">
    <b>Overzicht Persoonlijke Ontwikkelings Plan</b><br/><br/>
    Gebruik de dropdown velden in linkerpaneel om een klas, werkgroep en student te selecteren. Als er een klas of werkgroep 
    geselecteerd is kunt u op de Voortgangsmonitor klikken om de toestresultaten van de klas of werkgroep te bekijken. Als een 
    student geselecteerd is kunt u de POP van de student bekijken.
  </div>
</div>
</mm:compare>
</di:hasrole>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$popreferids" />
</fmt:bundle>
</mm:cloud>
</mm:content>
