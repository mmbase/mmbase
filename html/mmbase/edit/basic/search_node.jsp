<%@ include file="page_base.jsp"
%><mm:import externid="username" from="parameters" />
<mm:locale language="$config.lang">
<mm:cloud method="http" logon="$username" sessionname="${SESSION}" jspvar="cloud">
<mm:write referid="style" />

<title><%=m.getString("search_node.search")%></title>
</head>
<mm:context id="edit">
<mm:import externid="node_type"  jspvar="node_type" from="parameters"/>

<body class="basic" <mm:present referid="node_type"><mm:compare referid="config.hide_search" value="false"> onLoad="document.search.elements[0].focus();"</mm:compare></mm:present>>
    <table summary="node editors" width="100%" cellspacing="1" cellpadding="0" border="0">
    	<tr align="left">
    	    <th width="20%"><%=m.getString("search_node.search")%></th>
    	    <mm:present referid="node_type">
    	    	<th width="80%"><%=m.getString("search_node.type")%> <mm:nodeinfo nodetype="$node_type" type="guitype" />
                                              (<mm:nodeinfo nodetype="$node_type" type="type" />)
				</th>
    	    </mm:present>		
    	    <mm:notpresent referid="node_type">
    	    	<th width="80%"><%=m.getString("search_node.nonodes")%></th>
    	    </mm:notpresent>		
    	</tr>
    	<tr valign="top">
    	    <td><!-- node manager overview -->
    	    	<table summary="node managers" width="100%" cellspacing="1" cellpadding="3" border="0">
		    <% // functionality for listing nodemanagers is not (yet?) in taglib, using MMCI.
                     NodeManagerList l = cloud.getNodeManagers();
                    java.util.Collections.sort(l); // MMCI doesn't sort, do it ourselves.
                    for (int i=0; i<l.size(); i++) {
			NodeManager nt = l.getNodeManager(i);
		    %>
      	    	    <tr valign="top">
      	    	    	<td class="data" width="100%" ><%=nt.getGUIName()%></td>
      	    	    	<td class="navigate">
                        <% if (nt.mayCreateNode()) { %>
			    <a href="<mm:url page="create_node.jsp"><mm:param name="node_type"><%=nt.getName()%></mm:param></mm:url>" >
                    	       <img src="images/create.gif" alt="[create]" border="0" width="20" height="20" align="right" />
             	    	    </a>
                       <% } else { %>&nbsp;<% } %>
      	    	    	</td>												
      	    	    	<td class="navigate">
			    <% if (! nt.getName().equals(node_type)) { %>
            	    	    <a href='<mm:url><mm:param name="node_type"><%=nt.getName()%></mm:param></mm:url>' >
			    	<img src="images/select.gif" alt="[list]" width="20" height="20" border="0" />
			    </a>
			    <% } else { %>
			    &nbsp;
			    <% } %>
      	    	    	</td>
		    </tr>
		    <%
    	    	    } 
		    %>
                    <!-- quick search by number/alias: -->
    	    	    <form method="post" action="<mm:url page="change_node.jsp"/>">
    	    	    	<tr>
			    <td class="data"><%=m.getString("alias")%>: <input type="text" size="5" name="node_number" /></td>
			    <td colspan="2" class="navigate"><input type="submit"  name="change" value="&gt;" /></td>
			</tr>
    	    	    </form>
    	    	</table>
    	    </td>
    	    <td><!-- right collum, present search result (if clicked on node manager)-->  
    	    	<mm:present referid="node_type">		   
		    <!-- following page needs the param 'to_page' -->
		    	<mm:import id="to_page"><mm:url page="change_node.jsp"/></mm:import>			
			 <%@include file="search_node_with_type.jsp" %>
		    <!-- end import -->	    	   
               <mm:maycreate type="$node_type">
              	    	<table summary="nodes" width="100%" cellspacing="1" cellpadding="3" border="0">
	      	    	    <tr>
	      	    	    	<td class="data"><%= m.getString("search_node.create")%> <mm:nodeinfo nodetype="${node_type}" type="guitype" /> (<mm:write referid="node_type" />)</td>
	      	    	    	<td class="navigate">
	      	    	    	    <a href="<mm:url referids="node_type" page="create_node.jsp" />" >
                    	    		<img src="images/create.gif" alt="[create]" border="0" width="20" height="20" align="right" />
             	    	    	</a>
              	    	    </td>
	      	    	    </tr>
	      	    	</table>
              </mm:maycreate>
              <mm:maycreate type="${node_type}" inverse="true">
              	    	<table width="100%">
	      	    	    <tr>
			    	<td class="data"><%= m.getString("search_node.maynotcreate")%> <mm:nodeinfo nodetype="${node_type}" type="guitype" /> (<mm:write referid="node_type" />)</td>
	      	    	    	<td class="navigate">&nbsp;</td>        
	      	    	    </tr>
              	    	</table> 
              </mm:maycreate>
		    &nbsp;
    	    	</mm:present>
	    </td>
    	</tr>
    </table>
</mm:context>
<%@ include file="foot.jsp"  %>
</mm:cloud>
</mm:locale>
