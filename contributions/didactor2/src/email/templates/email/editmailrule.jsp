<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud method="delegate" jspvar="cloud">

<%@include file="/shared/setImports.jsp"%>
	<mm:import externid="mailbox">-1</mm:import>

	<mm:compare referid="mailbox" value="-1">
	  <mm:node number="$user">
	    <mm:relatednodes id="inbox" type="mailboxes" max="1" constraints="mailboxes.m_type=0">
	      <mm:remove referid="mailbox"/>
	      <mm:field name="number" id="mailbox" write="false"/>
  	    </mm:relatednodes>
	  </mm:node>
	</mm:compare>

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <img src="<mm:treefile write="true" page="/gfx/icon_email.gif" objectlist="$includePath" />" width="25" height="13" border="0" title="<di:translate key="email.email" />" alt="<di:translate key="email.email" />" /> <di:translate key="email.email" />
  </div>
</div>


<div class="folders">
  <div class="folderHeader">
    <di:translate key="email.mailboxes" />
  </div>
  <div class="folderBody">

  </div>
</div>


    
<div class="mainContent">
  <div class="contentHeader">
    <di:translate key="email.mailrules" />
  </div>
  <div class="contentSubHeader">
  </div>
  <div class="contentBody">
 
    <mm:node number="$mailbox" notfound="skip">
      <table class="listTable">
          <tr>
            <th class="listHeader"><input type="checkbox" onclick="selectAllClicked(this.form,this.checked)"></input></th>
            <th class="listHeader"><di:translate key="email.matchwhat" /></th>
            <th class="listHeader"><di:translate key="email.substring" /></th>
            <th class="listHeader"><di:translate key="email.folder" /></th>
          </tr>
      <mm:list nodes="$mailbox" path="mailboxes1,mailrule,mailboxes2">
	<mm:field name="mailboxes2.name" id="destbox" write="false"/>
	<mm:field name="mailrule.number" id="mailrule" write="false"/>
	<mm:field name="mailrule.rule" id="rule" write="false"/>		  <mm:field name="mailrule.rnumber" id="rulereldef" write="false"/>
	
            <tr>
              <td class="listItem"><input type="checkbox" name="ids" value="<mm:write referid="mailrule"/>"></input></td>
              <td class="listItem">
		<mm:node number="$rulereldef">
		<mm:field id="ruletype" name="sname" write="false"/>
		<mm:compare referid="ruletype" value="subjectmailrule">
		    <di:translate key="email.subject" />
		</mm:compare>
		<mm:compare referid="ruletype" value="sendermailrule">
		    <di:translate key="email.sender" />
		</mm:compare>
		</mm:node>
              </td>
              <td class="listItem"><mm:write referid="rule" /></td>
              <td class="listItem">
	      <mm:write referid="destbox"/>
	      </td>
            </tr>
          </mm:list>
        </table>
    </mm:node>
<script>

      function selectAllClicked(frm, newState) {
	  if (frm.elements['ids'].length) {
	    for(var count =0; count < frm.elements['ids'].length; count++ ) {
		var box = frm.elements['ids'][count];
		box.checked=newState;
	    }
	  }
	  else {
	      frm.elements['ids'].checked=newState;
	  }
      }

</script>
</div>
</div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</mm:cloud>
</mm:content>
