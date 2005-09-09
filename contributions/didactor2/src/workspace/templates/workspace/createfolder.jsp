<%--
  This template creates a new folder.
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="CREATEFOLDER" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="currentfolder"/>
<mm:import externid="callerpage"/>
<mm:import externid="typeof"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>
<mm:import externid="detectclicks" from="parameters"/>
<mm:import externid="oldclicks" from="session"/>
<mm:import externid="workspace" required="true"/>

<mm:present referid="detectclicks">
    <mm:compare referid="detectclicks" value="$oldclicks">
	<mm:redirect referids="$referids,currentfolder,typeof" page="$callerpage"/>
    </mm:compare>
    <mm:write session="oldclicks" referid="detectclicks"/>
</mm:present>

<%-- Check if the back button is not pressed --%>
<mm:notpresent referid="action2">
    <%-- check if a foldername is given --%>
    <mm:import id="foldername" externid="_name"/>
    <mm:compare referid="foldername" value="" inverse="true">
        <mm:createnode type="folders" id="myfolders">
           <mm:fieldlist type="all" fields="name,type">
               <mm:fieldinfo type="useinput" />
            </mm:fieldlist>
        </mm:createnode>

        <mm:createrelation role="related" source="workspace" destination="myfolders"/>
        <mm:redirect referids="$referids,currentfolder,typeof" page="$callerpage"/>
    </mm:compare>

    
    <mm:compare referid="foldername" value="">
	  <mm:import id="error">1</mm:import>
    </mm:compare>


</mm:notpresent>


<%-- Check if the back button is pressed --%>
<mm:present referid="action2">
  <mm:import id="action2text"><fmt:message key="BACK" /></mm:import>
  <mm:compare referid="action2" referid2="action2text">
    <mm:redirect referids="$referids,currentfolder,typeof" page="$callerpage"/>
  </mm:compare>
</mm:present>

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <mm:compare referid="typeof" value="1">
      <img src="<mm:treefile write="true" page="/gfx/icon_mydocs.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<fmt:message key="MYDOCUMENTS" />" />
      <fmt:message key="MYDOCUMENTS" />
    </mm:compare>
    <mm:compare referid="typeof" value="2">
      <img src="<mm:treefile write="true" page="/gfx/icon_shareddocs.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<fmt:message key="SHAREDDOCUMENTS" />" />
      <fmt:message key="SHAREDDOCUMENTS" />
    </mm:compare>
  </div>
</div>

<div class="folders">
  <div class="folderHeader">
  </div>
  <div class="folderBody">
  </div>
</div>

<div class="mainContent">

  <div class="contentHeader">
    <fmt:message key="CREATEFOLDER" />
  </div>

  <div class="contentBodywit">

    <%-- Show the form --%>
    <form name="createfolder" method="post" action="<mm:treefile page="/workspace/createfolder.jsp" objectlist="$includePath" referids="$referids"/>">

      <table class="Font">
      <mm:fieldlist nodetype="folders" fields="name">
        <tr>
        <td><mm:fieldinfo type="guiname"/></td>
        <td><mm:fieldinfo type="input"/></td>
        </tr>
      </mm:fieldlist>
      </table>
    <script>
	document.forms['createfolder'].elements['_name'].focus();
    </script>
      <input type="hidden" name="detectclicks" value="<%= System.currentTimeMillis() %>">
      <input type="hidden" name="workspace" value="<mm:write referid="workspace"/>">
      <input type="hidden" name="currentfolder" value="<mm:write referid="currentfolder"/>"/>
      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input type="hidden" name="typeof" value="<mm:write referid="typeof"/>"/>
      <input class="formbutton" type="submit" name="action1" value="<fmt:message key="CREATE" />" />
      <input class="formbutton" type="submit" name="action2" value="<fmt:message key="BACK" />" />
      <mm:present referid="error">
	    <p/>
	    <h1><fmt:message key="FOLDERNAMENOTEMPTY" /></h1>
	  </mm:present>
    </form>
  </div>
</div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</fmt:bundle>
</mm:cloud>
</mm:content>
