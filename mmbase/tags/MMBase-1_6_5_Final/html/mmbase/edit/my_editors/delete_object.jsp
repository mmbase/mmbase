<% String title = "Delete object"; %>
<%@ include file="inc_top.jsp" %>
<mm:cloud name="mmbase" jspvar="wolk" method="loginpage" loginpage="login.jsp" rank="basic user">
<mm:import externid="nr" jspvar="nr" required="true" />
<mm:import externid="delete" />
<%
String node_type = "";
String node_gui = "";
%>
<mm:present referid="nr">
<mm:node number="$nr" id="the_node">

	<mm:nodeinfo type="type" jspvar="n_type" vartype="String" write="false"><% node_type = n_type; %></mm:nodeinfo>
	<mm:nodeinfo type="guinodemanager" jspvar="n_gui" vartype="String" write="false"><% node_gui = n_gui; %></mm:nodeinfo>
	<% String path1 = node_type;		// Eerst stukje van kruimelpad %>
	<%@ include file="inc_head.jsp" %> 

<!-- main table -->
<table border="0" cellspacing="0" cellpadding="3">
<tr>
  <td valign="top" width="20%">

	<!-- table with back button -->
	<table width="230" border="0" cellspacing="0" cellpadding="3" class="table-left">	
	<tr>
	  <td width="24" align="right"><a href="index.jsp?ntype=<mm:nodeinfo type="type" />"><img src="img/mmbase-left.gif" alt="overview" width="21" height="20" border="0" /></a></td>
	  <td>Overview <mm:nodeinfo type="type" /></td>
	</tr><tr>
	  <td width="24" align="right"><mm:maywrite><a href="edit_object.jsp?nr=<mm:field name="number" />" title="edit node"><img src="img/mmbase-edit.gif" alt="edit object" width="21" height="20" border="0" /></a></mm:maywrite></td>
	  <td>Edit the node</td>
	</tr>
	</table>
	<!-- end table back button -->

  	


  </td>
  <td valign="top" width="80%">

	<%-- Check: are you sure --%>
	<mm:notpresent referid="delete">

		<form method="post" action="<mm:url referids="nr" />">
		<table border="0" cellspacing="0" cellpadding="4" class="table-form">
		<tr>
		  <td bgcolor="#CCCCCC" align="right" class="title-s">Delete node</td>
		  <td bgcolor="#CCCCCC" class="title-s"><b><%= node_gui %></b> (<%= node_type %>)</td>
		</tr>
		<mm:fieldlist type="list">
			<tr valign="top">
			  <td align="right"><span class="name"><mm:fieldinfo type="guiname" /></span><br /><mm:fieldinfo type="name" /></td>
			  <td><mm:fieldinfo type="guivalue" /> &nbsp;</td>
			</tr>
		</mm:fieldlist>
    	<tr valign="top">
    	  <td align="right" class="name">Alias</td>
    	  <td><mm:aliaslist node="the_node" id="aliasses"><mm:write /></mm:aliaslist></td>
    	</tr><tr>
		  <td colspan="2">
			<mm:import id="nr_relations" jspvar="nr_rels" vartype="String"><mm:countrelations /></mm:import>
			<% 
			int nr_rel = Integer.parseInt(nr_rels);
			if (nr_rel > 0) { 
			%>
				<p class="message">This node has <%= nr_rel %> relation(s) with other node(s).<br />
				<input type="submit" name="delete" value="Delete_with_relations" /></p>
			<% } else { %>
				<input type="submit" name="delete" value="Delete" />
			<% } %>
		  </td>
		</tr>
		</table>
		</form>

	</mm:notpresent>	<%-- end notpresent delete --%>
</mm:node>

	<%-- Delete the node --%>
	<mm:present referid="delete">
		<mm:deletenode number="<%= nr %>" deleterelations="true" />
		<p>&nbsp;</p>
		<table border="0" cellspacing="0" cellpadding="4" class="table-form">
		<tr>
		  <td bgcolor="#CCCCCC" align="right" nowrap>
		    <a href="new_object.jsp?ntype=<%= node_type %>" title="a new node of this type"><img src="img/mmbase-new.gif" alt="new" width="21" height="20" border="0" /></a>
		  </td>
		  <td bgcolor="#CCCCCC" class="title-m"><b><%= node_gui %></b> (<%= node_type %>) is deleted</td>
		<tr>
		  <td colspan="2">
		  	<p class="message">The node of type <b><%= node_gui %></b> (<%= node_type %>) is deleted.<br />
		  	<a href="index.jsp?ntype=<%= node_type %>" title="back to the overview of <%= node_type %>"><img src="img/mmbase-left.gif" alt="go back" width="21" height="20" border="0" /></a>
		  	Back to the <a href="index.jsp?ntype=<%= node_type %>">overview of <%= node_type %></a>.</p>
		  </td>
		</tr>
		</table>	
		<p>&nbsp;</p>
	</mm:present>

</td>
</tr></table>
<!-- end of main table -->

</mm:present>	<%-- end present nr --%>

<%@ include file="inc_foot.jsp" %>
</mm:cloud>
