<%--
  This template creates a new mailbox.
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>

<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.email.EmailMessageBundle">
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="CREATEFOLDER" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="mailbox"/>
<mm:import externid="callerpage"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>

    <%-- detect double clicks on submit form and redirect off the page --%>
    <mm:import externid="detectclicks" from="parameters"/>
    <mm:import externid="oldclicks" from="session"/>
    <mm:present referid="detectclicks">
	<mm:compare referid="detectclicks" value="$oldclicks">
	    <mm:redirect referids="$referids,mailbox" page="$callerpage"/>
        </mm:compare>
	<mm:write session="oldclicks" referid="detectclicks"/>
    </mm:present>




<mm:notpresent referid="action2"> <%-- back was NOT pressed --%>
  <%-- check if a mailboxname is given --%>
  <mm:import id="mailboxname" externid="_name"/>
  <mm:compare referid="mailboxname" value="" inverse="true">

    <mm:node number="$user" id="myuser">
      <mm:createnode type="mailboxes" id="mymailbox">

        <mm:fieldlist type="all" fields="name">
          <mm:fieldinfo type="useinput" />
        </mm:fieldlist>
	    <mm:setfield name="type">3</mm:setfield>

      </mm:createnode>
      <mm:createrelation role="related" source="myuser" destination="mymailbox"/>
    </mm:node>

    <mm:redirect referids="$referids,mailbox" page="$callerpage"/>

  </mm:compare>
  <mm:compare referid="mailboxname" value="">
    <mm:import id="error">1</mm:import>
  </mm:compare>

</mm:notpresent>


<%-- Check if the back button is pressed --%>
<mm:import id="action2text"><fmt:message key="BACK" /></mm:import>
<mm:compare referid="action2" referid2="action2text">
  <mm:redirect referids="$referids,mailbox" page="$callerpage"/>
</mm:compare>

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <img src="<mm:treefile write="true" page="/gfx/icon_email.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<fmt:message key="EMAIL" />"/>
    <fmt:message key="EMAIL" />
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
    <form name="createmailboxform" method="post" action="<mm:treefile page="/email/createmailbox.jsp" objectlist="$includePath" referids="$referids"/>">
	<input type="hidden" name="detectclicks" value="<%= System.currentTimeMillis() %>">

      <table class="Font">
      <mm:fieldlist nodetype="mailboxes" fields="name">
        <tr>
        <td><mm:fieldinfo type="guiname"/></td>
        <td><mm:fieldinfo type="input"/></td>
        </tr>
      </mm:fieldlist>
      </table>
      <script>
      <!--
	document.forms['createmailboxform'].elements['_name'].focus();
	// -->
    </script>

      <input type="hidden" name="mailbox" value="<mm:write referid="mailbox"/>"/>
      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input class="formbutton" type="submit" name="action1" value="<fmt:message key="CREATE" />"/>
      <input class="formbutton" type="submit" name="action2" value="<fmt:message key="BACK" />"/>

      <mm:present referid="error">
	    <p/>
	    <h1><fmt:message key="MAILBOXNAMENOTEMPTY" /></h1>
	  </mm:present>

    </form>

  </div>
</div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</fmt:bundle>
</mm:cloud>
</mm:content>
