<%@page language="java" contentType="text/html;charset=UTF-8" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><mm:import externid="language">nl</mm:import><mm:locale language="$language"><mm:cloud jspvar="cloud" loginpage="login.jsp"><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<% java.util.ResourceBundle m = null; // short var-name because we'll need it all over the place
   java.util.Locale locale = null; %>
<mm:write referid="language" jspvar="lang" vartype="string">
<%
  locale  =  new java.util.Locale(lang, "");
  m = java.util.ResourceBundle.getBundle("org.mmbase.util.media.resources.mediaedit", locale);
%>
</mm:write>
<head>
   <title><mm:write id="title" value="<%=m.getString("title")%>" /></title>
   <link href="style/streammanager.css" type="text/css" rel="stylesheet" />
<script src="<mm:url page="style/streammanager.js.jsp?dir=&amp;fragment=&amp;language=$language" />" language="javascript"><!--help IE--></script>
<head>
<mm:import externid="origin">media.myfragments</mm:import>
<mm:import externid="type">mediafragments</mm:import>
<body  onload="init('search');">
  <table>
    <tr><th>Result</th></tr> 

  <tr><th class="kop" colspan="2"><%=m.getString("demo")%></th></tr>
  <tr><td colspan="2"><%=m.getString("demoinfo")%></td></tr>
  <mm:list nodes="$origin" path="pools,$type" max="10" orderby="${type}.number" directions="down">
  <mm:context>
  <mm:node id="base" element="$type">
  <tr><td><mm:field name="title" /> </td>
      <td>
      <mm:nodeinfo id="realtype" type="type" write="false" />
      <img src="<mm:url page="media/${realtype}.gif" />" alt="" />
       <mm:relatednodes id="fragment" type="$realtype">
          <mm:relatednodes type="publishtimes" max="1">
             <mm:field id="begin" name="begin" write="false" />
             <mm:field id="end"   name="end"   write="false" />
             <mm:time  id="now"   time="now"   write="false" />
          </mm:relatednodes>
          <mm:present referid="now">
            <mm:write referid="begin" jspvar="begin" vartype="integer">
            <mm:write referid="end"   jspvar="end"   vartype="integer">
            <mm:write referid="now"   jspvar="now"   vartype="integer">
            <% if (now.intValue() < begin.intValue() || now.intValue() > end.intValue()) { %>
               <mm:import id="notvalid" />
            <% } %>
            </mm:write>
            </mm:write>  
            </mm:write>
          </mm:present>
          <mm:notpresent referid="notvalid">
          <a href="<mm:url referids="fragment,language" page="demo/index.jsp" />" target="new"> 
          </mm:notpresent>
             <mm:field name="title" write="true"><mm:isempty><mm:field node="base" name="title" /></mm:isempty></mm:field>
           <mm:notpresent referid="notvalid">
          </a>
          </mm:notpresent>
          <br />
         <%--
          showurl <mm:field name="showurl()" /><br />
          longurl <mm:field name="longurl()" /><br />
          --%>
       </mm:relatednodes>
      </td>
      <td>
        <mm:field name="owner" />
      </td>
  </tr>
  </mm:node>
  </mm:context>
  </mm:list>
  </table>
  <p id="colofon">
    <img src="images/mmbase.png" />
  </p>
</body>
</html>
</mm:cloud>
</mm:locale>