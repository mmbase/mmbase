<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"   prefix="mm"
%><%@include file="import.jsp" %><%@include file="settings.jsp"
%>
<mm:content language="$language">
<mm:import externid="user" required="true" />
<mm:cloud loginpage="login.jsp" rank="$rank">

<%  
  try {
%> 
<mm:compare referid="user" value="new">
  <mm:remove referid="user" />
  <mm:import id="wasnew" />  
  <mm:createnode id="user" type="mmbaseusers" />
</mm:compare>

<mm:node id="user" referid="user">
<mm:import id="current">users</mm:import>
<%@include file="navigate.div.jsp" %>
<%@include file="you.div.jsp" %>
<mm:context id="blabla">
  <mm:fieldlist type="edit" fields="owner">
    <mm:fieldinfo type="useinput" />
  </mm:fieldlist>
  <mm:cloudinfo type="user" write="false" id="clouduser" />
  <mm:field name="username">
    <mm:compare referid2="clouduser" inverse="true">
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
    </mm:compare>
  </mm:field>
  
  <%@include file="commitGroupOrUserRights.jsp" %>
</mm:context>


<h1><mm:function name="gui" /> (<%=getPrompt(m, "commited")%>)</h1>
<%@include file="edit_user.form.jsp" %>
</mm:node>


<% } catch (org.mmbase.storage.StorageException se) { %>
<p>
  <%=getPrompt(m, "commitusererror")%>
</p>
<p>
   Storage error <%= se.getMessage() %>.
</p>
<p>
  <a href="<mm:url referids="parameters,$parameters"><mm:param name="url">index_users.jsp</mm:param></mm:url>"><%=getPrompt(m, "back")%></a>
</p>
<% } %>

</mm:cloud>
</mm:content>