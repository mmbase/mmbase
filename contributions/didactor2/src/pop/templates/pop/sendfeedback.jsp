<mm:import externid="nfeedback"/>
<mm:import jspvar="feedback1" externid="feedback1"/>
<mm:import jspvar="feedback2" externid="feedback2"/>
<mm:import jspvar="rating" externid="rating">-1</mm:import>
<% boolean isSuccess = false; %>
<mm:remove referid="thisfeedback"/>
<mm:node number="$nfeedback" id="thisfeedback">
  <mm:field name="status">
    <mm:compare value="0">
      <% isSuccess = true; %><html  >
      <mm:setfield>-1</mm:setfield>
      <mm:setfield name="rank"><mm:write referid="feedback1"/></mm:setfield>
      <mm:setfield name="text"><mm:write referid="feedback2"/></mm:setfield>
      <mm:related path="people">
        <mm:import id="inviteefname"><mm:field name="people.firstname"/> <mm:field name="people.lastname"/></mm:import>
        <mm:import id="from"><mm:field name="people.email"/></mm:import>
      </mm:related>
      <% boolean isNoRating = true; %>
      <mm:related path="related,ratings">
        <mm:node element="related">
          <mm:maydelete>
            <mm:deletenode deleterelations="true"/>
          </mm:maydelete>
        </mm:node>
      </mm:related>
      <mm:compare referid="rating" value="-1" inverse="true">
        <mm:createrelation role="related" source="thisfeedback" destination="rating" />
      </mm:compare>
      <mm:related path="pop,people">
        <mm:import id="userfname"><mm:field name="people.firstname"/> <mm:field name="people.lastname"/></mm:import>
        <mm:import id="to"><mm:field name="people.email"/></mm:import>
      </mm:related>
      <mm:related path="competencies">
        <mm:import id="compname"><mm:field name="competencies.name"/></mm:import>
      </mm:related>
    </mm:compare>
  </mm:field>
</mm:node>

<% if (isSuccess) { %>

<%-- some sending email code--%>
<mm:import id="ratingmsg"></mm:import>
<mm:node number="rating" notfound="skip">
  <mm:import id="ratingmsg" reset="true"><di:translate key="pop.sendfeedbackpart6" /> <mm:field name="name"/><br/></mm:import>
</mm:node>

<mm:import id="subject"><di:translate key="pop.sendfeedbacksubject" /></mm:import>
<mm:import id="body"><HTML>
<di:translate key="pop.sendfeedbackpart1" /> <b><mm:write referid="userfname"/>,<br/>
<br/>
<di:translate key="pop.sendfeedbackpart2" /> <b><mm:write referid="compname"/></b> <di:translate key="pop.sendfeedbackpart3" /> <b><mm:write referid="inviteefname"/></b> 
<di:translate key="pop.sendfeedbackpart4" />
<br/>
<di:translate key="pop.sendfeedbackpart5" /> "<%= feedback1.replaceAll("\\n", "<br/>") %>"<br/>
<%= feedback2.replaceAll("\\n", "<br/>") %><br/>
<br/>
<mm:write referid="ratingmsg"/>
</HTML></mm:import>
<%@include file="sendmail.jsp" %>

<p><di:translate key="pop.msgsendfeedbackdonepart1" /> <b><mm:write referid="compname"/></b> <di:translate key="pop.msgsendfeedbackdonepart2" /> <b><mm:write referid="userfname"/></b><di:translate key="pop.msgsendfeedbackdonepart3" /></p>

<% } %>
