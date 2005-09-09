<%--
  This template moves a folder item from one folder to another.
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="MOVEITEMS" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="currentfolder"/>
<mm:import externid="callerpage"/>
<mm:import externid="typeof"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>
<mm:import externid="foldernumber"/>

<mm:import externid="ids"/>


<mm:import id="list" jspvar="list" vartype="List"><mm:write referid="ids"/></mm:import>

<mm:node number="$currentfolder" id="mycurrentfolder"/>

<mm:import externid="contact">-1</mm:import>
<mm:compare referid="contact" value="-1" inverse="true">
  <mm:import id="user" reset="true"><mm:write referid="contact"/></mm:import>
</mm:compare>

<%-- Check if the move button is pressed --%>
<mm:present referid="action1">
  <mm:import id="action1text"><fmt:message key="MOVE" /></mm:import>
  <mm:compare referid="action1" referid2="action1text">


    <%-- Delete the content from the current folder --%>
    <mm:node referid="mycurrentfolder">

      <!-- TODO determine in the future if there is a shorter way to code this -->
      <mm:relatednodes type="attachments" id="myattachements">
        <mm:remove referid="itemno"/>
        <mm:import id="itemno" jspvar="itemNo"><mm:field name="number"/></mm:import>
        <%
           int pos = list.indexOf( itemNo );
           if ( pos >= 0 ) {
        %>
          <mm:listrelations>
            <mm:deletenode/>
          </mm:listrelations>

        <%
           }
        %>
      </mm:relatednodes>

      <mm:relatednodes type="urls" id="myurls">
        <mm:remove referid="itemno"/>
        <mm:import id="itemno" jspvar="itemNo"><mm:field name="number"/></mm:import>
        <%
           int pos = list.indexOf( itemNo );
           if ( pos >= 0 ) {
        %>
          <mm:listrelations>
            <mm:deletenode/>
          </mm:listrelations>

        <%
           }
        %>
      </mm:relatednodes>

      <mm:relatednodes type="pages" id="mypages">
        <mm:remove referid="itemno"/>
        <mm:import id="itemno" jspvar="itemNo"><mm:field name="number"/></mm:import>
        <%
           int pos = list.indexOf( itemNo );
           if ( pos >= 0 ) {
        %>
          <mm:listrelations>
            <mm:deletenode/>
          </mm:listrelations>

        <%
           }
        %>
      </mm:relatednodes>

      <mm:relatednodes type="chatlogs" id="mychatlogs">
        <mm:remove referid="itemno"/>
        <mm:import id="itemno" jspvar="itemNo"><mm:field name="number"/></mm:import>
        <%
           int pos = list.indexOf( itemNo );
           if ( pos >= 0 ) {
        %>
          <mm:listrelations>
            <mm:deletenode/>
          </mm:listrelations>

        <%
           }
        %>
      </mm:relatednodes>

    </mm:node>

    <%-- Move the content to the new folder --%>
	<mm:node number="$foldernumber" id="mynewfolder">
              <%
                 java.util.Iterator it = list.iterator();
                 while ( it.hasNext() ) {
              %>
                 <mm:remove referid="itemno"/>
                 <mm:remove referid="mynewitems"/>
                 <mm:import id="itemno"><%=it.next()%></mm:import>
                 <mm:node number="$itemno" id="mynewitems"/>

                 <mm:createrelation role="related" source="mynewfolder" destination="mynewitems"/>
              <%
                 }
	          %>
	  </mm:node>

    <mm:redirect referids="$referids,currentfolder,typeof,contact?" page="$callerpage"/>

  </mm:compare>
</mm:present>


<%-- Check if the back button is pressed --%>
<mm:present referid="action2">
  <mm:import id="action2text"><fmt:message key="BACK" /></mm:import>
  <mm:compare referid="action2" referid2="action2text">
    <mm:redirect referids="$referids,currentfolder,typeof,contact?" page="$callerpage"/>
  </mm:compare>
</mm:present>

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
    <fmt:message key="MOVESELECTED" />
  </div>
  <div class="contentBodywit">
  <br><br><br>
    <%-- Show the form --%>
    <form name="moveitems" method="post" action="<mm:treefile page="/portfolio/moveitems.jsp" objectlist="$includePath" referids="$referids"/>">
      <table class="Font">

      <tr>
      <td>
      <fmt:message key="MOVESINGLE" /> <fmt:message key="FILESTO" />
      </td>
      </tr>

      <tr>
      <td>
      <select name="foldernumber">
        <mm:compare referid="contact" value="-1">
          <mm:node number="$user">
            <mm:relatednodes type="workspaces">
              <mm:relatednodescontainer type="folders">
                <mm:relatednodes>
                  <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                  <option value="<mm:field name="number"/>"><fmt:message key="MYDOCUMENTS" /> &gt; <mm:field name="name"/></option>
                </mm:relatednodes>
              </mm:relatednodescontainer>
            </mm:relatednodes>
          </mm:node>
        </mm:compare>
        <mm:present referid="class">
          <mm:node number="$class">
            <mm:relatednodes type="workspaces">
              <mm:relatednodescontainer type="folders">
                <mm:relatednodes>
                  <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                  <option value="<mm:field name="number"/>"><fmt:message key="SHAREDDOCUMENTS" /> &gt; <mm:field name="name"/></option>
                </mm:relatednodes>
              </mm:relatednodescontainer>
            </mm:relatednodes>
          </mm:node>
        </mm:present>
        <fmt:bundle basename="nl.didactor.component.portfolio.PortfolioMessageBundle">
          <mm:node number="$user">
            <mm:relatednodes type="portfolios" constraints="m_type = 0">
              <mm:relatednodescontainer type="folders">
                <mm:relatednodes>
                  <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                  <option value="<mm:field name="number"/>"><fmt:message key="DEVELOPMENTPORTFOLIO" /> &gt; <mm:field name="name"/></option>
                </mm:relatednodes>
              </mm:relatednodescontainer>
           </mm:relatednodes>
              <mm:relatednodes type="workgroups">
                <mm:field name="name" id="workgroupname">
                    <mm:relatednodes type="workspaces">
                        <mm:relatednodes type="folders">
                            <option value="<mm:field name="number"/>"><mm:write referid="workgroupname"/> &gt; <mm:field name="name"/></option>
                        </mm:relatednodes>
                    </mm:relatednodes>
                </mm:field>
              </mm:relatednodes>
 
            <di:hasrole role="teacher">
                <mm:relatednodes type="portfolios" constraints="m_type = 1">
                  <mm:relatednodescontainer type="folders">
                    <mm:relatednodes>
                      <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                      <option value="<mm:field name="number"/>"><fmt:message key="ASSESSMENTPORTFOLIO" /> &gt; <mm:field name="name"/></option>
                    </mm:relatednodes>
                  </mm:relatednodescontainer>
                </mm:relatednodes>
            </di:hasrole>
             <mm:relatednodes type="portfolios" constraints="m_type = 2">
              <mm:relatednodescontainer type="folders">
                <mm:relatednodes>
                  <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
                  <option value="<mm:field name="number"/>"><fmt:message key="SHOWCASEPORTFOLIO" /> &gt; <mm:field name="name"/></option>
                </mm:relatednodes>
              </mm:relatednodescontainer>
            </mm:relatednodes>
          </mm:node>
        </fmt:bundle>
      </td>
      </tr>

      <table>

      <input type="hidden" name="ids" value="<mm:write referid="ids"/>"/>
      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input type="hidden" name="currentfolder" value="<mm:write referid="currentfolder"/>"/>
      <input type="hidden" name="typeof" value="<mm:write referid="typeof"/>"/>
      <mm:compare referid="contact" value="-1" inverse="true">
        <input type="hidden" name="contact" value="<mm:write referid="contact"/>"/>
      </mm:compare>
      <input class="formbutton" type="submit" name="action1" value="<fmt:message key="MOVE" />" />
      <input class="formbutton" type="submit" name="action2" value="<fmt:message key="BACK" />" />
    </form>

  </div>
</div>
</div>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</fmt:bundle>
</mm:cloud>
</mm:content>
