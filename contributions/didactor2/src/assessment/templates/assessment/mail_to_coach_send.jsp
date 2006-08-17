<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>

<mm:cloud method="delegate" jspvar="cloud">

<%@include file="/shared/setImports.jsp" %>

<mm:remove referid="provider"/>
<mm:remove referid="education"/>
<mm:remove referid="class"/>

<mm:import externid="provider" />
<mm:import externid="education" />
<mm:import externid="class" />
<mm:import externid="message" />


<head>
   <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" referids="$referids" />" />
   <link rel="stylesheet" type="text/css" href="css/assessment.css" />
</head>
<mm:node number="$user">
   <mm:import id="from"><mm:field name="email"/></mm:import>
</mm:node>
<mm:import id="subject"><di:translate key="assessment.mail_to_coach_letter___subject" /></mm:import>
<mm:import id="body"><mm:write referid="message" /></mm:import>

<mm:node number="$education" notfound="skip">
   <mm:related path="classrel,people">
      <mm:node element="people">
         <mm:remove referid="to" />
         <mm:import id="to"><mm:field name="email"/></mm:import>

         <mm:related path="related,roles" constraints="roles.name='teacher'">
            <mm:write referid="to"/>
            <br/>
            <%@ include file="includes/sendmail.jsp" %>
         </mm:related>
      </mm:node>
   </mm:related>

   <mm:related path="classrel,classes,classrel,people">
      <mm:node element="people">
         <mm:remove referid="to" />
         <mm:import id="to"><mm:field name="email"/></mm:import>

         <mm:related path="related,roles" constraints="roles.name='teacher'">
            <mm:write referid="to"/>
            <br/>
            <%@ include file="includes/sendmail.jsp" %>
         </mm:related>
      </mm:node>
   </mm:related>


</mm:node>


<di:translate key="assessment.mail_to_coach_sent___done_message" />

</mm:cloud>
