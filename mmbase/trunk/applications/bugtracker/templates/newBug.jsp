<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud method="asis">
  <%@include file="parameters.jsp" %>
  <%@include file="login.jsp" %>
<mm:node number="$user">

<form action="<mm:url referids="parameters,$parameters"/>" method="POST">
<table width="99%" cellspacing="0" cellpadding="0" class="list" style="margin-top : 10px;">

<tr>
	<th>
	Type
	<B/th>
	<th>
	Priority
	</th>
	<th>
	Version
	</th>
</tr>
<tr>
		<td>
			<SELECT NAME="newbtype">
				<OPTION VALUE="1">bug
				<OPTION VALUE="2">wish
				<OPTION VALUE="3">docbug
				<OPTION VALUE="4">docwish
			</SELECT>
		</td>
		<td>
			<SELECT NAME="newbpriority">
				<OPTION VALUE="1">high
				<OPTION VALUE="2" SELECTED>medium
				<OPTION VALUE="3">low
			</SELECT>
		</td>
		<td>
			<INPUT NAME="newversion" VALUE="1.7.0" SIZE="10">
		</td>
</tr>
</table>
<table width="99%" cellspacing="0" cellpadding="0" class="list" style="margin-top : 10px;">
<tr>
	<th>
	Area
	</th>
</tr>
<tr>
		<td>
		<mm:import id="noareas" />
		<mm:node number="BugTracker.Start">
		<SELECT NAME="newarea">
   
			<mm:relatednodescontainer type="areas">
        <mm:sortorder field="name" direction="up"/>
			  <mm:relatednodes>
			  <mm:first><mm:remove referid="noareas" /></mm:first>
			    <OPTION VALUE="<mm:field name="number" />"
    			<mm:field name="name">
    			<mm:compare value="Misc">SELECTED</mm:compare>
    			</mm:field>
  			><mm:field name="substring(name,15,.)" />
			</mm:relatednodes>
			</mm:relatednodescontainer>
		</SELECT>
		</mm:node>
		</td>
</tr>
</table>
<table width="99%" cellspacing="0" cellpadding="0" class="list" style="margin-top : 10px;">

<tr>
	<th>
	Issue : give the issue in one line 
	</th>
</tr>
<tr>
		<td>
			<input style="width: 100%" name="newissue">
		</td>
</tr>

</table>
<table width="99%" cellspacing="0" cellpadding="0" class="list" style="margin-top : 10px;">
<tr>
	<th>
	Description : Describe the issue as complete as possible
	</th>
</tr>
<tr>
		<td>
			<textarea name="newdescription" style="width: 100%" rows="15" wrap></textarea>
		</td>
</tr>
</table>
<table width="99%" cellspacing="0" cellpadding="0" class="list" style="margin-top : 10px;">

<tr>
	<th colspan="3">
	Name this bug will be submitted under
	</th>
</tr>
<tr>
		<td>
			<p />
			<input name="submitter" type="hidden" value="<mm:field name="number" />">
			&nbsp;&nbsp;
			<mm:field name="firstname" />
			<mm:field name="lastname" />
			 ( <mm:field name="email" /> )
			
		</td>
		<td>
			<p />
			<CENTER>
			<mm:present referid="noareas" inverse="true">
				<input type="hidden" name="action" value="newbug">
				<INPUT TYPE="submit" VALUE="SUBMIT REPORT">
			</mm:present>
			</form>
			<mm:present referid="noareas">
				No areas defined, admin needs to add areas !
			</mm:present>
		</td>
		<td>
			<p />
      <form action="<mm:url referids="parameters,$parameters"/>" method="GET">
			<CENTER>
				<INPUT TYPE="submit" VALUE="CANCEL">
       </CENTER>
			</form>
		</td>
</tr>

</table>
</mm:node>
</mm:cloud>
