<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%>
<mm:content type="text/html" expires="0">
<mm:cloud>
<mm:import externid="bugreport" />
<mm:import externid="portal" jspvar="portal" />
<mm:import externid="page" jspvar="page2" />
<mm:import externid="flap" jspvar="flap">overview</mm:import>
<mm:import externid="cw" from="cookie" />
<mm:import externid="ca" from="cookie" />

<mm:import externid="base" />

<mm:present referid="ca">
    <mm:present referid="cw">
	   <mm:listnodes type="users" constraints="account='$ca' and password='$cw'" max="1">
	       <mm:import id="user"><mm:field name="number" /></mm:import>
	    </mm:listnodes>
    </mm:present>
</mm:present>
<mm:present referid="user">
    <mm:list path="users,groups" nodes="$user" constraints="groups.name='BugTrackerCommitors'" max="1">
      <mm:field id="commitor" write="false" name="users.number" />
   </mm:list>
</mm:present>
<%@include file="actions.jsp" %>

<mm:node number="$bugreport">
<mm:related path="rolerel,users" constraints="rolerel.role='maintainer'" max="1">
   <mm:import id="hasmaintainers">yes</mm:import>
</mm:related>


<mm:related path="rolerel,users" constraints="rolerel.role='submitter'">
	<mm:import id="submitter"><mm:field name="users.number" /></mm:import>
</mm:related>

<%@ include file="parts/flaps.jsp" %>

		<mm:compare referid="flap" value="overview"><%@ include file="parts/overview.jsp" %> 
		<%@ include file="parts/comments.jsp" %> </mm:compare>
		<mm:compare referid="flap" value="history"><%@ include file="parts/history.jsp" %> </mm:compare>
		<mm:compare referid="flap" value="change"><%@ include file="parts/change.jsp" %> </mm:compare>
	     <%--
		<mm:compare referid="flap" value="mybug"><%@ include file="parts/mybug.jsp" %> </mm:compare>
         --%>

</mm:node>
</mm:cloud>
</mm:content>