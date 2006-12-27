<%--
  This template delete existing mail from a mailbox.
--%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>

<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud method="delegate">

<jsp:directive.include file="/shared/setImports.jsp" />
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><di:translate key="email.deletefolderitems" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="mailbox"/>
<mm:import externid="so"/>
<mm:import externid="sf"/>
<mm:import externid="callerpage"/>

<mm:import externid="idCount"/>


<!-- wtf -->
<mm:import externid="ids" />
<mm:import id="list" jspvar="list" vartype="list"><mm:write referid="ids"/></mm:import>

<mm:import externid="action1"/>
<mm:import externid="action2"/>


<%-- Check if the yes button is pressed --%>
<mm:import id="action1text"><di:translate key="email.deleteyes" /></mm:import>

<mm:log>AAAA ${action1} ${action1text}  ${action2} ${action2text}</mm:log>

<mm:compare referid="action1" referid2="action1text">

  <%-- Determine the items to be deleted --%>
  <mm:node number="$mailbox">

    <mm:field id="type" name="type" write="false" />

    <mm:log>TYPE ${type}</mm:log>
    <%-- from deleted items mailbox: do a real delete --%>
    <mm:compare referid="type" value="2">
      <mm:relatednodescontainer type="object">
        <mm:constraint field="number" referid="list" operator="IN"/>
        <mm:relatednodes>
          <mm:deletenode deleterelations="true"/>
        </mm:relatednodes>
      </mm:relatednodescontainer>

      <%-- Show the previous page --%>
      <mm:log>deleted, now redirecting</mm:log>
      <mm:redirect referids="$referids,mailbox,so?,sf?" page="$callerpage"/>

    </mm:compare>

    <mm:compare referid="type" value="2" inverse="true">

      <%-- Get the name of the deleted items mailbox --%>
      <mm:node number="$user">
        <mm:relatednodescontainer type="mailboxes">
          <mm:constraint field="type" value="2"/>
          <mm:relatednodes>
            <mm:import id="mailboxname"><mm:field name="name"/></mm:import>
          </mm:relatednodes>
        </mm:relatednodescontainer>
      </mm:node>

      <mm:log>Redirectin to moveitems.jsp</mm:log>
      <%-- from any other mailbox: do a move items to deleted items mailbox --%>
      <mm:redirect page="/email/moveitems.jsp" referids="$referids,so?,sf?">
        <mm:param name="submitted" value="true"/>
        <mm:param name="action1"><di:translate key="email.move" /></mm:param>
        <mm:param name="callerpage"><mm:write referid="callerpage"/></mm:param>
        <mm:param name="mailbox"><mm:write referid="mailbox"/></mm:param>
        <mm:param name="mailboxname"><mm:write referid="mailboxname"/></mm:param>
        <mm:param name="idCount"><mm:write referid="idCount"/></mm:param>
        <mm:param name="ids"><mm:write referid="ids"/></mm:param>
      </mm:redirect>
    </mm:compare>

  </mm:node>

</mm:compare>


<mm:log>AAAA ${action1} ${action2} ${action2text}</mm:log>

<%-- Check if the no button is pressed --%>
<mm:import id="action2text"><di:translate key="email.deleteno" /></mm:import>
<mm:compare referid="action2" referid2="action2text">
  <mm:log>redirecting</mm:log>
  <mm:redirect referids="$referids,mailbox,so?,sf?" page="$callerpage"/>
</mm:compare>


<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <img src="<mm:treefile write="true" page="/gfx/icon_email.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<di:translate key="email.email" />"/>
    <di:translate key="email.email" />
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
    <di:translate key="email.deletefolderitems" />
  </div>

  <div class="contentBodywit">

    <%-- Show the form --%>
    <form name="deletemailboxitemform" method="post" >
      <di:translate key="email.deletefolderitemsyesno" />
      <br/><br/>
      <div><table class="listTable">
        <tr>
          <th class="listHeader"><di:translate key="email.sender" /></td>
          <th class="listHeader"><di:translate key="email.recipient" /></td>
          <th class="listHeader"><di:translate key="email.subject" /></td>
          <th class="listHeader"><di:translate key="email.date" /></td>
        </tr>
        <% for (int i=0;i<list.size();i++) {%>
          <mm:node number="<%= (String) list.get(i)%>">
            <tr>
              <td class="listItem"><mm:field name="from" /></td>
              <td class="listItem"><mm:field name="to" /></td>
              <td class="listItem"><mm:field name="subject" /></td>
              <td class="listItem"><mm:field name="gui(date)" /></td>
            </tr>
          </mm:node>
        <%} %> 
      </table></div>
      <br/><br/>

      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input type="hidden" name="mailbox" value="${mailbox}" />
      <input type="hidden" name="ids" value="${ids}" />
      <mm:present referid="so">
        <input type="hidden" name="so" value="${so}" />
      </mm:present>
      <mm:present referid="sf">
        <input type="hidden" name="sf" value="${sf}" />
      </mm:present>
      <input class="formbutton" type="submit" name="action1" value="<di:translate key="email.deleteyes" />"/>
      <input class="formbutton" type="submit" name="action2" value="<di:translate key="email.deleteno" />"/>
    </form>

  </div>
</div>
</div>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</mm:cloud>
</mm:content>
