<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="java.util.Calendar,
                 java.util.Enumeration"%>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<fmt:bundle basename="nl.didactor.component.email.EmailMessageBundle">
<mm:content postprocessor="none"><%-- no reducespace: it messes with the textarea --%>
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<%@include file="/shared/setImports.jsp"%>

<mm:import externid="back"/>
<mm:present referid="back">
    <mm:redirect page="/email/index.jsp"/>
</mm:present>


  <mm:import id="emaildomain"><mm:treeinclude write="true" page="/email/init/emaildomain.jsp" objectlist="$includePath"/></mm:import>
  
   <mm:import id="to"></mm:import>
   <mm:import id="cc"></mm:import>
   <mm:import id="subject"></mm:import>
   <mm:import id="body"></mm:import>
   <mm:import id="emailok">0</mm:import>
  
  <%-- 
    setup data according to some already existing mail
  --%>
  <mm:import id="reply" externid="reply"/>
  <mm:present referid="reply">
    <mm:import id="loadOld"><mm:write referid="reply"/></mm:import>
  </mm:present>

  <mm:import id="replyAll" externid="replyAll"/>
  <mm:present referid="replyAll">
    <mm:import id="loadOld"><mm:write referid="replyAll"/></mm:import>
  </mm:present>

  <mm:import id="forward" externid="forward"/>
  <mm:present referid="forward">
    <mm:import id="loadOld"><mm:write referid="forward"/></mm:import>
  </mm:present>
  
  <mm:present referid="loadOld">
    <mm:node referid="loadOld">
      <mm:present referid="reply">
        <mm:import id="to" reset="true"><mm:field name="from" escape="none"/></mm:import>
        <mm:import id="subject" reset="true">Re:<mm:field name="subject" escape="none"/></mm:import>
      </mm:present>
      <mm:present referid="replyAll">
        <mm:import id="to" reset="true"><mm:field name="from" escape="none"/></mm:import>
        <mm:import id="cc" reset="true"><mm:field name="cc" escape="none"/></mm:import>
        <mm:import id="subject" reset="true">Re:<mm:field name="subject" escape="none"/></mm:import>
      </mm:present>
      <mm:present referid="forward">
        <mm:import id="subject" reset="true">Fw:<mm:field name="htmlsubject" escape="none"/></mm:import>
      </mm:present>

      <mm:field name="body" jspvar="body" vartype="String" write="false">
      <mm:field name="headers" jspvar="headers" vartype="String" write="false">
        <%
	    
	    body = headers.replaceAll("(^|[\n])","$1> ") + "\n>" +
		    body.replaceAll("(^|[\n])","\n> ");
        %>
	<mm:import id="body" reset="true"><%=body%></mm:import> 
      </mm:field>
      </mm:field>
    </mm:node>
  </mm:present>

  <mm:list nodes="$user" path="people,mailboxes" fields="mailboxes.number" constraints="mailboxes.type=1">
    <mm:field name="mailboxes.number" id="mailboxNumber" write="false"/>
    <mm:node referid="mailboxNumber" id="mailboxNode"/>
  </mm:list>



  
  <%-- edit existing email (not yet sent) --%>
  <mm:import externid="id"/>
  <mm:isnotempty referid="id">
    <mm:node number="$id" id="emailNode">
	<mm:import id="to" reset="true"><mm:field name="to" escape="none"/></mm:import>
	<mm:import id="cc" reset="true"><mm:field name="cc" escape="none"/></mm:import>
	<mm:import id="subject" reset="true"><mm:field name="subject" escape="none"/></mm:import>
	<mm:import id="body" reset="true"><mm:field name="body" escape="none"/></mm:import>
    </mm:node>
  </mm:isnotempty>
   
  <%--
    default: read data from request
  --%>
  <mm:import id="inputto" externid="to" reset="true"/>
  <mm:import id="inputcc" externid="cc" reset="true"/>
  <mm:import id="inputsubject" externid="subject" reset="true"/>
  <mm:import id="inputbody" externid="body" reset="true"/>
  <mm:present referid="inputto">
    <mm:import id="to" reset="true"><mm:write referid="inputto" escape="none"/></mm:import>
  </mm:present>
  <mm:present referid="inputcc">
      <mm:import id="cc" reset="true"><mm:write referid="inputcc" escape="none"/></mm:import>
  </mm:present>
  <mm:present referid="inputsubject">
    <mm:import id="subject" reset="true"><mm:write referid="inputsubject" escape="none"/></mm:import>
  </mm:present>
  <mm:present referid="inputbody">
    <mm:import id="body" reset="true"><mm:write referid="inputbody" escape="none"/></mm:import>
  </mm:present>

    <mm:import externid="field"/>
    <mm:present referid="field"><%-- extra mail addresses from addressbook --%>
	<mm:import externid="ids" vartype="List"/>
	<mm:present referid="ids">

	    <mm:listnodes type="people" constraints="number IN ($ids)">
                <mm:import id="tmp_email" reset="true"><mm:field name="email"/></mm:import>
                <mm:isempty referid="tmp_email">
                    <mm:import id="tmp_email" reset="true"><mm:field name="username"/></mm:import>
                </mm:isempty>
		<mm:import id="$field" reset="true"><mm:isempty referid="$field" inverse="true"><mm:write referid="$field"/>, </mm:isempty><mm:write referid="tmp_email"/></mm:import>
	    </mm:listnodes>
	</mm:present>
    </mm:present>

  <%-- no node yet, if input, create one --%>
    <mm:present referid="emailNode" inverse="true">
	<mm:import id="testforinput"><mm:write referid="to"/><mm:write referid="cc"/><mm:write referid="body"/><mm:write referid="subject"/></mm:import>
	<mm:isnotempty referid="testforinput">
	    <mm:createnode type="emails" id="emailNode"/>
            <mm:node referid="emailNode">
	        <mm:import id="id" reset="true"><mm:field name="number"/></mm:import>
            </mm:node>
	  <mm:createrelation role="related" source="mailboxNode" destination="emailNode"/>
        </mm:isnotempty>
    </mm:present>

  
 
  <mm:node number="$user">
    <mm:import id="from">"<mm:field name="firstname"/> <mm:field name="lastname"/>" <<mm:field name="username"/><mm:treeinclude write="true" page="/email/init/emaildomain.jsp" objectlist="$includePath"/>></mm:import>
  </mm:node>

 
  <mm:isnotempty referid="subject">
  <mm:isnotempty referid="body">
  <mm:import id="ccText" jspvar="ccText"><mm:write referid="cc"/></mm:import>
  <mm:import id="toText" jspvar="toText"><mm:write referid="to"/></mm:import>
  <%
    if ( toText.trim().length() > 0 ) {

  %>	
      <mm:import id="emailok" reset="true">1</mm:import>
  <%
    }
  %>
  </mm:isnotempty>
  </mm:isnotempty>

<mm:present referid="emailNode">
  <mm:node referid="emailNode">
	<mm:setfield name="from"><mm:write referid="from" escape="none"/></mm:setfield>
        <mm:setfield name="to"><mm:write referid="to"/></mm:setfield>
	<mm:setfield name="cc"><mm:write referid="cc"/></mm:setfield>
        <mm:setfield name="subject"><mm:write referid="subject"/></mm:setfield>
	<mm:setfield name="body"><mm:write referid="body"/></mm:setfield>
        <mm:setfield name="type">0</mm:setfield>
        <mm:setfield name="date"><%=System.currentTimeMillis()/1000%></mm:setfield>
  </mm:node>

    <mm:import id="testattachment" externid="att_handle"/>
    <mm:compare referid="testattachment" value="" inverse="true">
    <mm:import id="attachmentName" externid="att_handle_name" from="multipart"/>
    <mm:compare referid="attachmentName" value="" inverse="true">
      <mm:createnode type="attachments" id="newFile" jspvar="newFile">
        <mm:setfield name="title"><mm:write referid="attachmentName"/></mm:setfield>
        <mm:setfield name="filename"><mm:write referid="attachmentName"/></mm:setfield>

        <mm:fieldlist id="att" nodetype="attachments" fields="handle">
          <mm:fieldinfo type="useinput" />
        </mm:fieldlist>
      </mm:createnode>
      <mm:createrelation role="related" source="emailNode" destination="newFile"/>
      <mm:remove referid="newFile"/>

    </mm:compare>
    </mm:compare>
    <mm:remove referid="attachmentName"/>
    

    <mm:present referid="emailNode">
	<mm:import externid="delete_attachments" vartype="List"/>
	<mm:present referid="delete_attachments">
	    <mm:node number="$emailNode">
		<mm:relatednodes type="attachments" constraints="attachments.number IN ( $delete_attachments )">
		    <mm:deletenode deleterelations="true"/>
		</mm:relatednodes>
	    </mm:node>
	</mm:present>
    </mm:present>



    
  <mm:import externid="send_action"/> <%-- send button pressed --%>
  <mm:present referid="send_action">
    <mm:compare referid="emailok" value="1">
	<mm:node referid="emailNode">
	    <mm:setfield name="type">1</mm:setfield>
	</mm:node>
	<mm:list nodes="$user" path="people,mailboxes" fields="mailboxes.number" max="1">
	    <mm:field id="mailbox" name="mailboxes.number" write="false"/>
	</mm:list>

	<mm:treefile jspvar="forward" write="false" page="/email/index.jsp" objectlist="$includePath" referids="$referids">
	<%--    <mm:param name="page" value="/email/mailboxes.jsp" />--%>
	<mm:param name="provider" value="$provider"/>
	<mm:param name="mailbox" value="$mailbox"/>
	</mm:treefile>
	<%
	    response.sendRedirect(forward);
	%>
    </mm:compare>
  </mm:present>

</mm:present>


    <mm:import externid="lookup_to_action"/>
    <mm:present referid="lookup_to_action">
	<mm:import id="redirect_url" jspvar="redirect_url"><mm:treefile page="/address/index.jsp" objectlist="$includePath" referids="$referids"/>&mailid=<mm:present referid="emailNode"><mm:write referid="emailNode"/></mm:present>&field=to</mm:import>
    	<%    response.sendRedirect(redirect_url); %>
    </mm:present>

    <mm:import externid="lookup_cc_action"/>
    <mm:present referid="lookup_cc_action">
	<mm:import id="redirect_url" jspvar="redirect_url"><mm:treefile page="/address/index.jsp" objectlist="$includePath" referids="$referids"/>&mailid=<mm:present referid="emailNode"><mm:write referid="emailNode"/></mm:present>&field=cc</mm:import>
    	<%    response.sendRedirect(redirect_url); %>
    </mm:present>



	
<mm:import externid="nooutput"/>
<mm:notpresent referid="nooutput">

<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title>Send mail</title>
  </mm:param>
</mm:treeinclude>


<div class="rows">

<div class="navigationbar">
  <div class="titlebar">
    <img src="<mm:treefile write="true" page="/gfx/icon_email.gif" objectlist="$includePath" />" width="25" height="13" border="0" alt="e-mail" /> E-mail
  </div>
</div>

<div class="folders">
  <div class="folderHeader">

  </div>
  <div class="folderBody">

  
  <script>
    
    function checkFields(frm) {
      if(frm.elements['to'].value.length == 0) {
        alert('<fmt:message key="TOEMPTY" />');
        return false;
      }

      if(frm.elements['subject'].value.length == 0) {
		alert('<fmt:message key="SUBJECTEMPTY" />');
        return false;
      }

      if(frm.elements['body'].value.length == 0) {
        alert('<fmt:message key="BODYEMPTY" />');
        return false;
      }
	return true;
    }


  </script>

  <mm:treeinclude write="true" page="/email/cockpit/menuitem.jsp" objectlist="$includePath">
    <mm:param name="icon">write message</mm:param>
    <mm:param name="text"><fmt:message key="WRITEMESSAGE" /></mm:param>
  </mm:treeinclude>

  </div>
</div>

<div class="mainContent">
  <div class="contentHeader">

  </div>
  <div class="contentBodywit">
<br><br><br>
  <form action="<mm:treefile write="true" page="/email/write/write.jsp" objectlist="$includePath">
                <mm:notpresent referid="course"><mm:param name="provider" value="$provider"/></mm:notpresent>
                </mm:treefile>" method="post" enctype="multipart/form-data" name="webmailForm">
		<mm:present referid="id">
		<input type="hidden" name="id" value="<mm:write referid="id"/>">
		</mm:present>
    <table class="font">
            <tr>
              <td><fmt:message key="TO" /> :&nbsp;</td>
              <td>
	      <input type="text" class="formInput" name="to" value="<mm:write referid="to"/>"> 
	      <input type="submit" name="lookup_to_action" value="<di:translate id="lookup_to">Uit adresboek</di:translate>" class="formbutton"></td>
              </tr>
 
            </tr>
            <tr>
              <td><fmt:message key="CC" /> :&nbsp;</td>
              <td>
	      <input type="text" class="formInput" name="cc" value="<mm:write referid="cc"/>">
	      <input type="submit" name="lookup_cc_action" value="<di:translate id="lookup_cc">Uit adresboek</di:translate>" class="formbutton"></td>
	      </tr>
           <tr>
              <td><fmt:message key="SUBJECT" /> :&nbsp;</td>
              <td><input type="text" class="formInput" name="subject" value="<mm:write referid="subject"/>">
              </td>
            </tr>
            <tr>
              <td>
              </td>
              <td>
                <br>
                <textarea name="body" class="formInput"><mm:write referid="body"/></textarea>
                <br>
              </td>
            </tr>
            <tr>
              <td><fmt:message key="ATTACHMENTS" /> :&nbsp;</td>
              <td>
	      <mm:present referid="emailNode">
	      <mm:node number="$emailNode">
	      <mm:relatednodes type="attachments">
		<div class="attachment">
		<input type="checkbox" name="delete_attachments" value="<mm:field name="number"/>">
		<a href="<mm:attachment/>"><mm:field name="title"/></a></div>
	      </mm:relatednodes>
	      </mm:node>
	      </mm:present>
		  <div class="attachment">
                    <input type="file" class="formInput" name="att_handle" class="formbutton">
		    <input type="submit" name="att_attachment_action" value="<di:translate id="updateattachments">bijwerken</di:translate>" class="formbutton">
                  </div>
              </td>
            </tr>
            <tr>
              <td>
		    <input type="submit" name="send_action" value="<di:translate id="send">Verstuur</di:translate>" onclick="return checkFields(this.form)" class="formbutton">
              </td>
              <td>
				<input type="submit" name="back" value="<di:translate id="back">Terug</di:translate>" class="formbutton">
				</div>
              </td>
            </tr>
    </table>
  </form>

  </div>
</div>
</div>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</mm:notpresent>

</mm:cloud>
</mm:content>
</fmt:bundle>


