<DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page language="java" contentType="text/html;charset=UTF-8" 
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><%@include file="config/read.jsp" %><mm:content language="$config.lang" postprocessor="reducespace">
<mm:cloud jspvar="cloud" method="asis">
<html>
<head>
  <title><mm:write id="title" value="<%=m.getString("title")%>" /></title>
  <link href="style/streammanager.css" type="text/css" rel="stylesheet" />
  <script src="<mm:url page="style/streammanager.js.jsp?dir=&amp;fragment=" />" language="javascript"><!--help IE--></script>  
<head>

<body class="content">
   <h1><%=m.getString("categories")%></h1>

   <%-- search form --%>
   
   <mm:context>
     <form action="<mm:url />" method="post">
       <table summary="pools">
         <mm:fieldlist nodetype="pools" type="search" id="searchform">
           <tr><td><mm:fieldinfo type="guiname" /></td><td><mm:fieldinfo type="searchinput" /></td</tr>
         </mm:fieldlist>
         <tr><td /><td><input type="submit" value="<%=m.getString("search")%>" name="search" /></td></tr>
       </table>
     </form>
   </mm:context>

   <%-- handle submit node --%>
  <mm:import externid="submitnode" />

  <mm:present referid="submitnode">
    <mm:import externid="number" required="true" />

    <%-- new node --%>
    <mm:compare referid="number" value="new">
      <mm:createnode id="newpool" type="pools">
        <mm:fieldlist fields="name,description">
          <mm:fieldinfo type="useinput" />
        </mm:fieldlist>
      </mm:createnode>
      <mm:node id="media_node" number="media.streams" />
      <mm:createrelation source="media_node" destination="newpool" role="parent" />
    </mm:compare>

    <%-- submit node --%>
    <mm:compare referid="number" value="new" inverse="true">
      <mm:node number="$number">            
        <mm:fieldlist fields="name,description,owner">
          <mm:fieldinfo type="useinput" />
        </mm:fieldlist>
        <mm:field name="owner" id="owner" write="false" />
        <%-- make sure all relations are of the same owner --%>
        <mm:listrelations role="parent" searchdir="destination">
          <mm:maychangecontext>
            <mm:setcontext><mm:write referid="owner" /></mm:setcontext>
          </mm:maychangecontext>
        </mm:listrelations>
      </mm:node>
    </mm:compare>
  </mm:present>

   <%-- handle delete node --%>
  <mm:import externid="deletenode" />
  <mm:present referid="deletenode">
    <mm:import externid="number" required="true" />
    <mm:deletenode number="$number" deleterelations="true" />
  </mm:present>

  <%-- search result --%>


  <mm:import externid="search" />

  <mm:import id="pagelength">10</mm:import>

  <table class="searchresult">      
  <mm:node number="media.streams">
    <mm:relatednodescontainer type="pools" role="parent">

      <mm:sortorder field="number" direction="down" />
      
      <mm:present referid="search">
        <%-- handle search form --%>
        <mm:fieldlist nodetype="pools" type="search" id="searchform">
          <mm:fieldinfo type="usesearchinput" />
        </mm:fieldlist>
      </mm:present>

      <mm:import externid="offset">0</mm:import>
      <mm:offset    value="$offset" />

      <tr>
        <mm:fieldlist nodetype="pools" fields="name,description,owner">
          <th><mm:fieldinfo type="guiname" /></th>
        </mm:fieldlist>
        <th colspan="2">#: <mm:size /></th>
      </tr>

      <mm:maycreate type="pools">

        <form method="post" action="<mm:url />">
        <mm:fieldlist node="" nodetype="pools" fields="name,description">
          <td><mm:fieldinfo type="input" /></td>
        </mm:fieldlist>
        <td />
        <td colspan="2">
          <input type="hidden" name="number" value="new" />
          <input type="submit" name="submitnode" value="<%=m.getString("new")%>" /></td>
        </form>
      </mm:maycreate>

      <mm:maxnumber value="$pagelength" />

      <%-- show results --%>
      <mm:relatednodes>
        <tr>
          <mm:maywrite>
            <form method="post" action="<mm:url />">
            <mm:fieldlist fields="name,description,owner">
              <td><mm:fieldinfo type="input" /></td>
            </mm:fieldlist>
            <td>
              <input type="hidden" name="number" value="<mm:field name="number" />" />
              <input type="submit" name="submitnode" value="submit" /></td>
            </form>
          </mm:maywrite>
          <mm:maywrite inverse="true">
            <td><mm:field name="name" /></td>
            <td><mm:field name="description" /></td>
            <td><mm:field name="owner" /></td>
            <td></td>
          </mm:maywrite>
          <td>
          <mm:maydelete>
            <mm:countrelations>
              <mm:compare value="1">
                <form method="post" action="<mm:url />">
                <input type="hidden" name="number" value="<mm:field name="number" />" />
                <input type="submit" name="deletenode" value="delete" /></td>
                </form>
              </mm:compare>
            </mm:countrelations>
          </mm:maydelete>
          </td>
        </tr>
      </mm:relatednodes>
      
      <%-- paging --%>
      <tr>
        <td colspan="2">
          <mm:previousbatches max="1">
            <a href="<mm:url referids="search">
              <mm:param name="offset"><mm:write /></mm:param>
              <mm:fieldlist nodetype="pools" type="search">
                <mm:fieldinfo type="reusesearchinput" />
              </mm:fieldlist>
              </mm:url>">&lt;</a>
           </mm:previousbatches>
         </td>

        <td colspan="3">
          <mm:nextbatches max="1">
            <a href="<mm:url referids="search">
              <mm:param name="offset"><mm:write /></mm:param>
              <mm:fieldlist nodetype="pools" type="search">
                <mm:fieldinfo type="reusesearchinput" />
              </mm:fieldlist>
              </mm:url>">&gt;</a>
           </mm:nextbatches>
         </td>
       </tr>
    </mm:relatednodescontainer>
  </mm:node>
  </table>
  
  
</body>
</html>
</mm:cloud>
</mm:content>
