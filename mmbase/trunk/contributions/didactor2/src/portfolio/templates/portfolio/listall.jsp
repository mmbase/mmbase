<%--
  This template shows the personal workspace (in dutch: persoonlijke werkruimte).
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud jspvar="cloud" method="asis">
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="MYDOCUMENTS" /></title>
  </mm:param>
</mm:treeinclude>

<div class="rows">
<div class="navigationbar">
<div class="titlebar">
  <img src="<mm:treefile write="true" page="/gfx/icon_shareddocs.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<fmt:message key="PORTFOLIO" />"/>
      <fmt:message key="PORTFOLIO" />
</div>
</div>

<div class="folders">

<div class="folderHeader">
    <fmt:message key="PORTFOLIO" />
</div>
  <div class="folderBody">
  </div>
</div>


<div class="mainContent">

 <div class="contentHeader">
  </div>

  <div class="contentSubHeader">
  </div>

  <div class="contentBodywit">
  <mm:import id="startChar" externid="c" jspvar="startChar"/>
  <% 
  boolean cvalid = false;
  for (char c = 'a'; c <= 'z' ; c++) { 
      if (String.valueOf(c).equals(startChar)) {
          cvalid = true;
      }
      %>
      <a href="?c=<%= c %>"><%= String.valueOf(c).toUpperCase() %></a>
      <% if (c != 'z') { %> | <% } %>
  <% } %><p/><% 
  if (cvalid) { %>
  <mm:listnodes type="people" orderby="lastname,firstname" constraints="lastname LIKE '${startChar}%'">
    <mm:import id="nodetype" reset="true"><mm:nodeinfo type="type"/></mm:import>
    <mm:compare referid="nodetype" value="contacts" inverse="true">
       <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$referids">
            <mm:param name="contact"><mm:field name="number"/></mm:param>
        </mm:treefile>"><mm:field name="firstname"/> <mm:field name="lastname"/></a><br>
    </mm:compare>
   </mm:listnodes>
  <% } %>
  </div>
</div>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</fmt:bundle>
</mm:cloud>
</mm:content>
