<mm:context id="context_search">
<%-- for selecting next page with listings --%>
<mm:import externid="page" vartype="decimal" from="parameters">0</mm:import>

<mm:import externid="node_type"  required="true" from="parent"/>
<mm:import externid="to_page"    required="true" from="parent"/><!-- where to link to -->

<mm:import externid="node"       from="parent" />

<mm:import externid="role_name"  from="parameters" />
<mm:import externid="direction"  from="parameters" />
<mm:import externid="search"     from="parameters" />
<mm:import externid="maylink"    from="parameters" />

<!-- import search age and store in session -->

<mm:import externid="_search_form_minage" ></mm:import>
<mm:import externid="_search_form_maxage" ><mm:listnodes type="typedef" constraints="[name]='${node_type}'" max="1"><mm:field name="defaultsearchage()" /></mm:listnodes></mm:import>

<%-- you can configure 'hide_search' to hide the search functionality --%>
<%-- mm:compare referid="config.hide_search" value="false" --%>
<mm:context>
  <form name="search" method="post" action='<mm:url referids="node,node_type,role_name,direction" />'>
      <table class="search" align="center" width="100%" border="0" cellspacing="1">
        <%-- search table --%>
        <mm:fieldlist id="search_form" nodetype="$node_type" type="search">
            <tr align="left">
              <td width="20%"><mm:fieldinfo type="guiname" /> <small>(<mm:fieldinfo type="name" />)</small></td>
             <td width="100%"><mm:fieldinfo type="searchinput" /></td>
           </tr>
        </mm:fieldlist>
         <tr align="left">
            <td width="20%"><%=m.getString("search.minage")%></td>
            <td width="100%"><input type ="text" class="small" size="80" name="_search_form_minage" value="<mm:write referid="_search_form_minage" />" /></td>
         </tr>
         <tr align="left">
            <td width="20%"><%=m.getString("search.maxage")%></td>
            <td width="100%"><input type ="text" class="small" size="80" name="_search_form_maxage" value="<mm:write referid="_search_form_maxage" />" /></td>
         </tr>
        <tr>
           <td colspan="2"><input class="search" type ="submit" name="search" value="<%=m.getString("search")%>" /></td>
        </tr>
      </table>
  </form>
  
</mm:context>
<%-- /mm:compare --%>

<%-- ordered to search with form button 'search'. Following are some tricks to get the where right.--%>    
<mm:present referid="search">
  <mm:import id="minage_constraint" />
  <mm:import id="maxage_constraint" />
  <mm:isnotempty referid="_search_form_minage">
     <mm:remove referid="minage_constraint" />     
     <mm:import id="daycount_constraint1"> [daycount] <=  <mm:write referid="_search_form_minage" jspvar="min" vartype="integer"><%=(int)(System.currentTimeMillis()/(1000*60*60*24)) - min.intValue()%></mm:write></mm:import>
     <mm:listnodes type="daymarks" constraints="$daycount_constraint1" max="1" orderby="daycount" directions="DOWN">
        <mm:import id="minage_constraint"> [number] <= <mm:field name="mark" /></mm:import>
     </mm:listnodes>
      <mm:notpresent referid="minage_constraint">
       <mm:import id="minage_constraint"> [number] < 0 </mm:import><!-- will not find a thing -->
     </mm:notpresent>
  </mm:isnotempty>
  <mm:isnotempty referid="_search_form_maxage">
     <mm:remove referid="maxage_constraint" />
     <mm:import id="daycount_constraint2"> [daycount] <=  <mm:write referid="_search_form_maxage" jspvar="min" vartype="integer"><%=(int)(System.currentTimeMillis()/(1000*60*60*24)) - min.intValue()%></mm:write></mm:import>
     <mm:listnodes type="daymarks" constraints="$daycount_constraint2" max="1" orderby="daycount" directions="DOWN">
        <mm:import id="maxage_constraint"> [number] >= <mm:field name="mark" /></mm:import>
     </mm:listnodes>
     <mm:notpresent referid="maxage_constraint">
       <mm:import id="maxage_constraint" /><!-- no constraint -->
     </mm:notpresent>    
  </mm:isnotempty>

  <mm:import id="where"><mm:context
    ><mm:fieldlist id="search_form" nodetype="$node_type" type="search"
      ><mm:fieldinfo type="usesearchinput"><mm:isnotempty
        ><mm:present referid="notfirst"> AND </mm:present><mm:notpresent referid="notfirst"><mm:import id="notfirst">yes</mm:import></mm:notpresent
        ><mm:write 
      /></mm:isnotempty></mm:fieldinfo
    ></mm:fieldlist
    ><mm:write referid="minage_constraint"><mm:isnotempty
      ><mm:present referid="notfirst"> AND </mm:present><mm:notpresent referid="notfirst"><mm:import id="notfirst">yes</mm:import></mm:notpresent
      ><mm:write 
    /></mm:isnotempty></mm:write
    ><mm:write referid="maxage_constraint"><mm:isnotempty
      ><mm:present referid="notfirst"> AND </mm:present
      ><mm:write 
    /></mm:isnotempty></mm:write
  ></mm:context></mm:import>
    <!-- -everything on one line, to avoid pollution of $where with spaces.
         -context is used to avoid pollution of current context with used id's. --> 
             
<% boolean mayLink = false; %><mm:present referid="maylink"><% mayLink = true; %></mm:present>

<a name="searchresult" />
<table width="100%" class="list"><!-- list table -->      
  <tr align="left"><!-- header -->
         <th>Gui()</th>
    <mm:fieldlist nodetype="$node_type" type="list">
         <th><mm:fieldinfo type="guiname" /> <small>(<mm:fieldinfo type="name" />)</small></th>
    </mm:fieldlist>
    <th>&nbsp;</th><!-- X collum -->
    <th>&nbsp;</th><!-- -> collum -->
  </tr>
  <mm:listnodes id="node_number" type="$node_type" directions="DOWN"   orderby="number"
                offset="${+$page*$config.page_size}"  max="$config.page_size"
                jspvar="sn"
                constraints="$where">
  <tr>
        <td class="listdata"><mm:nodeinfo type="gui" />&nbsp;</td>
   <mm:fieldlist nodetype="$node_type" type="list">
        <td class="listdata"><mm:fieldinfo type="guivalue" />&nbsp;</td>
   </mm:fieldlist>
    <td class="navigate">
        <mm:maydelete>
          <mm:countrelations>
            <mm:compare value="0">
              <a href="<mm:url referids="node_type,node_number,page" page="commit_node.jsp" ><mm:param name="delete">true</mm:param></mm:url>">
                  <span class="delete"></span><span class="alt">[delete]</span>
              </a>
            </mm:compare>
          </mm:countrelations>
        </mm:maydelete>
        &nbsp;
     </td>    
     <td class="navigate">  
    <% if(sn.mayWrite() || sn.mayDelete() || sn.mayChangeContext() || (mayLink)) { %>
            <a href="<mm:url page="$to_page" referids="node_number" />">
                  <span class="change"></span><span class="alt">[change]</span>
            </a>
     <% } else { %>&nbsp;<% } %>
     </td>
 </tr>  
 <mm:last>
   <mm:index>
      <mm:compare referid2="config.page_size">
         <mm:import id="next_page">jes</mm:import>
      </mm:compare>
  </mm:index>
</mm:last>
</mm:listnodes>
</table>
</mm:present>

<table><!-- pager -->
  <tr>
    <mm:isgreaterthan referid="page" value="0.5">
      <td class="navigate">
            <a href='<mm:url referids="node,node_type,role_name,direction,search">
                <mm:param name="page" value="${+$page-1}" />
                <!--pass all search field values -->
                <mm:context>
                  <mm:fieldlist id="search_form" nodetype="$node_type" type="search">
                    <mm:fieldinfo type="name">
                      <mm:import externid="search_form_$_" />
                      <mm:param name="search_form_$_" value="${search_form_$_}" />
                    </mm:fieldinfo>
                  </mm:fieldlist>
                </mm:context>
              </mm:url>' >
                 <span class="previous"></span><span class="alt">[<-previous page]</span>
      </a>
        </td> 
    </mm:isgreaterthan>
    <mm:present referid="next_page">
      <td class="navigate">      
            <a href='<mm:url referids="node,node_type,role_name,direction,search,_search_form_minage,_search_form_maxage">
                <mm:param name="page"  value="${+$page+1}" />
                <!--pass all search field values -->
                <mm:context>
                  <mm:fieldlist id="search_form" nodetype="$node_type" type="search">
                    <mm:fieldinfo type="name">
                      <mm:import externid="search_form_$_" />
                      <mm:param name="search_form_$_" value="${search_form_$_}" />
                    </mm:fieldinfo>
                  </mm:fieldlist>
                </mm:context>
              </mm:url>' >
              <span class="next"></span><span class="alt">[next page ->]</span>
            </a>
      </td>
    </mm:present>
   </tr>
</table>
</mm:context>
