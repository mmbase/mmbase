<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"   prefix="mm"
%><%@include file="settings.jsp"
%><%@include file="import.jsp" %>
<mm:import externid="user" required="true" />

<mm:cloud method="loginpage" loginpage="login.jsp" jspvar="cloud" rank="$rank">
<mm:node id="user" referid="user">
 <%@include file="you.div.jsp" %>
  <mm:context>
    <mm:fieldlist type="edit" fields="owner">
       <mm:fieldinfo type="useinput" />
    </mm:fieldlist>
    <mm:import externid="_groups" vartype="list" jspvar="groups" /> 
     <mm:listrelations type="mmbasegroups" role="contains">
       <mm:relatednode jspvar="group">
        <% if (! groups.contains("" + group.getNumber())) { %>
          <mm:import id="delete" />
        <% } %>
       </mm:relatednode>
       <mm:present referid="delete">
        <mm:deletenode />
       </mm:present>
     </mm:listrelations>
     <mm:unrelatednodes id="unrelated" type="mmbasegroups" />   
     <mm:write referid="unrelated" jspvar="unrelated" vartype="list">
     <mm:stringlist referid="_groups">              
       <mm:node id="group" number="$_" jspvar="group">
         <% if (unrelated.contains(group)) { %>
              <mm:createrelation source="group" destination="user" role="contains" />
         <% } %>
        </mm:node>
     </mm:stringlist>
     </mm:write>
     <mm:import externid="_rank" />
     <mm:isnotempty referid="_rank">      
       <mm:listrelations type="mmbaseranks" role="rank">
          <mm:deletenode />
       </mm:listrelations> 
       <mm:node id="ranknode" number="$_rank" />
       <mm:createrelation source="user" destination="ranknode" role="rank" />
     </mm:isnotempty>

   <%@include file="commitGroupOrUserRights.jsp" %>
   </mm:context>
    <h1><mm:field name="gui()" /> (commited)</h1>
   <%@include file="edit_user.form.jsp" %>
</mm:node>

</mm:cloud>

   

