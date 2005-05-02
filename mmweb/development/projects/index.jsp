<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@page language="java" contentType="text/html; charset=iso8859-1" %>
<mm:cloud>
<%@include file="/includes/getids.jsp" %>
<%@include file="/includes/header.jsp" %>
<mm:import id="template">/development/projects/project.jsp</mm:import>
<td class="white" colspan="2" valign="top">
  <mm:listnodescontainer type="project">
    <mm:sortorder field="status" direction="up" />
    <mm:sortorder field="number" direction="down" />
    <mm:composite operator="AND">
      <mm:constraint field="status" value="application" inverse="true" />
      <mm:constraint field="status" value="contribution" inverse="true" />
    </mm:composite>
 
    <mm:listnodes id="projectlist">      
      <mm:changed>
	[<a href="#<mm:field name="number" />"><mm:field name="status" /></a>]&nbsp;
      </mm:changed>
    </mm:listnodes>
  </mm:listnodescontainer>
    
  <mm:listnodes id="project" referid="projectlist">
    <mm:changed>
      <h1><a name="<mm:field name="number" />"><mm:field name="status"/></a></h1>
    </mm:changed>
       <h2><a href="<mm:url referids="portal,page,project,template" />"><mm:field name="title" /></a></h2>
    <mm:field name="intro" />
  </mm:listnodes>
   <br/><br/>
</td>

<%@include file="/includes/footer.jsp" %>
</mm:cloud>
