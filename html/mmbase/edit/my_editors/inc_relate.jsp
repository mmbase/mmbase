<mm:import externid="verander_ok" />

<!-- ### Create relation with node nr: we have nr, ntype, kind, rnr ### -->
<mm:present referid="rnr">

	<table border="0" cellspacing="0" cellpadding="4" class="table-form">
	<tr bgcolor="#CCCCCC">
	  <td>&nbsp;</td><td class="title-m">Objects related</td>
	</tr>

	<mm:compare referid="dir" value="nwparent">
		<tr valign="top">
		  <td class="name" align="right">Source</td>
		  <td>
			<mm:node number="$rnr" id="source_node">
				<b><mm:field name="gui()" /></b><br /><mm:nodeinfo type="guinodemanager" />
			</mm:node> 
		  </td>
		</tr><tr valign="top">
		  <td class="name" align="right">Destination</td>
		  <td>
			<mm:node referid="nr" id="dest_node">
				<b><mm:field name="gui()" /></b><br /><mm:nodeinfo type="guinodemanager" />
			</mm:node>
		  </td>
		</tr>
	</mm:compare>
	<mm:compare referid="dir" value="nwchild">
		<tr valign="top">
		  <td class="name" align="right">Source</td>
		  <td>
			<mm:node referid="nr" id="source_node">
				<b><mm:field name="gui()" /></b><br /><mm:nodeinfo type="guinodemanager" />
			</mm:node> 
		  </td>
		</tr><tr valign="top">
		  <td class="name" align="right">Destination</td>
		  <td>
			<mm:node number="$rnr" id="dest_node">
				<b><mm:field name="gui()" /></b><br /><mm:nodeinfo type="guinodemanager" />
			</mm:node>
		  </td>
		</tr>
	</mm:compare>

	<tr valign="top">
	  <td class="name" align="right">Relation kind</td>
	  <td><b><mm:write referid="kind" /></b></td>
	</tr>
	
	<mm:notpresent referid="verander_ok">
		<%-- Create relation: go to edit relation when it has > 1 editable fields (then it is a relation with a value) --%>
		<mm:createrelation source="source_node" destination="dest_node" role="$kind" id="the_relation">
			<mm:fieldlist type="edit"><mm:first><mm:import id="verander">ok</mm:import></mm:first></mm:fieldlist>
		</mm:createrelation>
	</mm:notpresent>	<%-- end notpresent verander_ok --%>

	<mm:present referid="verander">
		<%-- Edit relation: needed when a relation has a value (like position f.e.) --%>
		<mm:node referid="the_relation">
			<tr bgcolor="#CCCCCC"><td>&nbsp;</td><td class="title-s">Edit the relation</td></tr>
			<form method="post" action="<mm:url referids="ntype,nr,rnr,kind,dir">
				<mm:param name="the_relation"><mm:field name="number" /></mm:param>
				<mm:param name="save_it">didthat</mm:param>
			</mm:url>">
			<mm:fieldlist type="edit">
			<tr>
			  <td class="name" align="right"><mm:fieldinfo type="guiname" /></td>
			  <td><mm:fieldinfo type="input" /></td>
			</tr>
			</mm:fieldlist>
			<tr><td>&nbsp;</td><td><input type="submit" name="verander_ok" value="Save" /></td></tr>
			</form>
		</mm:node>
	</mm:present>		<%-- end verander --%>
	
	<mm:present referid="verander_ok">
		<%-- Save relation --%>
		<mm:import externid="the_relation" required="true" />
		<tr><td colspan="2"><p class="message">The nodes have been related and your input is saved.</p></td></tr>
		<mm:node referid="the_relation">
			<mm:fieldlist type="edit">
				<mm:context><mm:fieldinfo type="useinput" /></mm:context>
				<mm:first><tr bgcolor="#CCCCCC"><td>&nbsp;</td><td class="title-s">Value of the relation</td></tr></mm:first>
				<mm:context>
				<tr valign="top">
				  <td align="right"><span class="name"><mm:fieldinfo type="guiname" /></span><br /><mm:fieldinfo type="name" /></td>
				  <td><mm:fieldinfo type="guivalue" /></td>
				</tr>
				</mm:context>
			</mm:fieldlist>
		</mm:node>
	</mm:present>		<%-- end verander_ok --%>

	</table>

</mm:present>	<%-- end rnr --%>
