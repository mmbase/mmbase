<mm:cloud rank="basic user">
<!-- action check -->
<mm:import externid="action" />
<mm:present referid="action">
 <mm:include page="principlesets/actions.jsp" />
</mm:present>
<!-- end action check -->

<table cellpadding="0" cellspacing="0" class="list" style="margin-top : 20px;" width="70%">
<form action="<mm:url page="index.jsp" referids="main" />" method="post">
<tr>
  <th><a href="<mm:url page="index.jsp" referids="main" />"><img src="images/mmbase-left.gif" align="left" border="0"></a> PrincipleSet description</th>
</tr>
<tr>
  <td>
    <b>Name</b><br />
    <input name="newname" size="20" value="" /><br /><br />
    <b>Description</b><br />
    <input name="newdescription" size="70" value="" /><br /><br />
    <b>Alias</b><br />
    <input name="newalias" size="40" value="" /><br /><br />
  </td>
</tr>
<tr>
</tr>
<tr>
<td align="right" colspan="2">
<input type="hidden" name="action" value="createprincipleset" />
save <input type="image" src="images/mmbase-ok.gif" />
</form>
&nbsp;&nbsp;&nbsp;cancel <a href="<mm:url page="index.jsp" referids="main" />"><img src="images/mmbase-cancel.gif" border="0"></a>
</td>
</tr>
</table>
</mm:cloud>
