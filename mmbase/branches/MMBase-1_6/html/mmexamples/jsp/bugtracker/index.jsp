<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<HTML>
<HEAD>
   <TITLE>MMBase Bugtracker</TITLE>
   <%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
</HEAD>
<mm:cloud>
<mm:import externid="sbugid" jspvar="sbugid" />
<mm:import externid="sissue" jspvar="sissue" />
<mm:import externid="sstatus" jspvar="sstatus" />
<mm:import externid="stype" jspvar="stype" />
<mm:import externid="sversion" jspvar="sversion" />
<mm:import externid="spriority" jspvar="spriority" />

<mm:import externid="cw" from="cookie" />
<mm:import externid="ca" from="cookie" />
<mm:present referid="ca">
        <mm:present referid="cw">
			<mm:listnodes type="users" constraints="account='$ca' and password='$cw'" max="1">
				<mm:import id="user"><mm:field name="number" /></mm:import>
			</mm:listnodes>
        </mm:present>
</mm:present>


<BODY BACKGROUND="images/back.gif" TEXT="#42BDAD" BGCOLOR="#00425B" LINK="#42BDAD" ALINK="#42BDAD" VLINK="#42BDAD">
<!-- first the selection part -->

<TABLE width="100%" cellspacing=1 cellpadding=3 border=0>
<FORM ACTION="index.jsp" METHOD="POST">

<TR>

		<TD WIDTH="30"></TD>
		<TD BGCOLOR="#00425A" COLSPAN="8">
		 <center>BugTracker 1.1 - Daniel Ockeloen
		</TD>
</TR>
<TR>
	<TD>
	<BR>
	</TD>
</TR>
<TR>
	<TD WIDTH="50"><IMG SRC="images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<INPUT NAME="sbugid" SIZE="4">
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<SELECT NAME="sstatus">
		<OPTION VALUE="">
		<OPTION VALUE="1">open
		<OPTION VALUE="2">accepted
		<OPTION VALUE="3">rejected
		<OPTION VALUE="4">pending
		<OPTION VALUE="5">integrated
		<OPTION VALUE="6">closed
	</SELECT>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<SELECT NAME="stype">
		<OPTION VALUE="">
		<OPTION VALUE="1">bug
		<OPTION VALUE="2">enhan.
		<OPTION VALUE="3">doc
		<OPTION VALUE="4">docenh.
	</SELECT>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<SELECT NAME="spriority">
		<OPTION VALUE="">
		<OPTION VALUE="1">high
		<OPTION VALUE="2">medium
		<OPTION VALUE="3">low
	</SELECT>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<INPUT NAME="sversion" SIZE="3">
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<SELECT NAME="sarea">
		<OPTION VALUE="">
		<mm:listnodes type="areas">
		<OPTION VALUE="<mm:field name="number" />"><mm:field name="substring(name,15,.)" />
		</mm:listnodes>
	</SELECT>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<INPUT NAME="sissue" SIZE="20">
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=1>
	<INPUT TYPE="SUBMIT" VALUE="search">
	</TD>
</TR>
</FORM>
<TR>
	<TD>
	</TD>
</IR>
<TR>
	<TD WIDTH="50"><IMG SRC="images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Bug #</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Status</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Type</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Priority</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Version</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Area</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Issue</B>
	</TD>
	<TD BGCOLOR="42BDAD" WIDTH="14">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=1>
	&nbsp;
	</TD>
</TR>
<!-- the real searchpart -->

<% 	String where="";
	if (sissue!=null && !sissue.equals(""))  where+="issue like '%"+sissue+"%'";
	if (sstatus!=null && !sstatus.equals("")) { if (!where.equals("")) where+=" and ";where+="bstatus="+sstatus; }
	if (stype!=null && !stype.equals("")) { if (!where.equals("")) where+=" and ";where+="btype="+stype; }
	if (sversion!=null && !sversion.equals("")) { if (!where.equals("")) where+=" and ";where+="version like '%"+sversion+"%'"; }
	if (sbugid!=null && !sbugid.equals("")) { if (!where.equals("")) where+=" and ";where+="bugreports.number="+sbugid; }
	if (spriority!=null && !spriority.equals("")) { if (!where.equals("")) where+=" and ";where+="bugreports.bpriority="+spriority; }
%>

<mm:list path="pools,bugreports,areas" nodes="BugTracker.Start" orderby="bugreports.number" directions="down" constraints="<%=where%>">
<TR>
		<TD WIDTH="30"></TD>
		<TD BGCOLOR="#00425A">
			#<mm:field name="bugreports.number" />
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="bugreports.bstatus">
				<mm:compare value="1">Open</mm:compare>
				<mm:compare value="2">Accepted</mm:compare>
				<mm:compare value="3">Rejected</mm:compare>
				<mm:compare value="4">Pending</mm:compare>
				<mm:compare value="5">Integrated</mm:compare>
				<mm:compare value="6">Closed</mm:compare>
			 </mm:field>
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="bugreports.btype">
				<mm:compare value="1">Bug</mm:compare>
				<mm:compare value="2">Enhanchement</mm:compare>
				<mm:compare value="3">DocBug</mm:compare>
				<mm:compare value="4">DocEnhanchement</mm:compare>
			 </mm:field>
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="bugreports.bpriority">
				<mm:compare value="1">High</mm:compare>
				<mm:compare value="2">Medium</mm:compare>
				<mm:compare value="3">Low</mm:compare>
			 </mm:field>
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="bugreports.version" />&nbsp;
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="areas.name" />&nbsp;
		</TD>
		<TD BGCOLOR="#00425A">
			 <mm:field name="bugreports.issue" />&nbsp;
		</TD>
		<TD BGCOLOR="#44BDAD" WIDTH="14">
			<A HREF="fullview.jsp?bugreport=<mm:field name="bugreports.number" />"><IMG SRC="images/arrow.gif" BORDER="0" ALIGN="left"></A>
		</TD>
</TR>
</mm:list>





<!-- end of the searchpart -->
<TR>
	<TD>
		&nbsp;
	</TD>
</TR>

<TR>
	<TD WIDTH="50"><IMG SRC="images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="42BDAD" COLSPAN="6">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>New bug report</B>
	</TD>
	<TD BGCOLOR="42BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>&nbsp;</B>
	</TD>
</TR>
<TR>

		<TD WIDTH="30"></TD>
		<mm:present referid="user" inverse="true" >
			<TD BGCOLOR="#00425A" COLSPAN="6">
			 <center>We have no idea who you are please login !
			</TD>
			<TD BGCOLOR="#44BDAD" WIDTH="14">
				<A HREF="changeUser.jsp"><IMG SRC="images/arrow.gif" BORDER="0" ALIGN="left"></A>
			</TD>
		</mm:present>
		<mm:present referid="user">
			<TD BGCOLOR="#00425A" cOLSPAN="6">
			<mm:node number="$user">
			<CENTER> I am <mm:field name="firstname" /> <mm:field name="lastname" /> ( its not me , <A HREF="changeUser.jsp">change name</A> ) i have a new bug and want to report it
			</TD>
			<TD BGCOLOR="#44BDAD" WIDTH="14">
				<A HREF="newBug.jsp?user=<mm:write referid="user" />"><IMG SRC="images/arrow.gif" BORDER="0" ALIGN="left"></A>
			</TD>
			</mm:node>
		</mm:present>
</TR>


</TABLE>

</mm:cloud>
</BODY>
</HTML>
