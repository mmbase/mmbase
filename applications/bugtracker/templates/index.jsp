<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><%@page language="java" contentType="text/html; charset=utf-8"

%><mm:cloud
><%@include file="/includes/getids.jsp" 
%><%@include file="/includes/header.jsp"

%><td colspan="2">
<mm:import externid="base">/development/bugtracker</mm:import>

<mm:include referids="base"  page="main.jsp" />

</td>
<%@include file="/includes/footer.jsp"
%></mm:cloud>


