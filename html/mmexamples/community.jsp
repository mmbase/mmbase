<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud name="mmbase">
<HTML>
<HEAD>
   <TITLE>MMExamples - Community</TITLE>
</HEAD>

<BODY TEXT="#42BDAD" BGCOLOR="#00425B" LINK="#FFFFFF" ALINK="#555555" VLINK="#FFFFF">
<BR>
<TABLE width="90%" cellspacing=1 cellpadding=3 border=0>
<TR>
	<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="#44BDAD" COLSPAN=3>
	<FONT COLOR="#000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Description of the Community examples</B>
	</TD>
</TR>
<TR>
		<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
		<TD BGCOLOR="#00425A" COLSPAN=3 VALIGN="top">
			<BR> The community example shows all the features of the community functionality in MMBase. 
            It displays a forum and a chat, it also shows the admin-possibilities. 	
			<BR>
		</TD>
</TR>
<TR>
	<TD>
	<BR>
	</TD>
</TR>


<TR>
	<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="#44BDAD" COLSPAN=3>
	<FONT COLOR="#000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Location of the Community example</B>
	</TD>
</TR>
<TR>
		<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
		<TD BGCOLOR="#00425A" COLSPAN=3 >
			<BR>

            <mm:list path="versions" fields="name,type" constraints="versions.name='Community' AND versions.type='application'">
              <mm:first>
                <mm:import id="mynewsIsPresent">true</mm:import>   
              </mm:first>
            </mm:list>
            <mm:notpresent referid="mynewsIsPresent">
              Community application NOT installed please install before using it.<BR>
You can install the Community application by going to ADMIN -> APPLICATIONS
            </mm:notpresent>
            <mm:present referid="mynewsIsPresent">              
              <mm:url id="url" page="/mmexamples/jsp/community/community.jsp" write="false" />
              This url will show the Community: <A HREF="<mm:write referid="url" />" TARGET="community"><mm:write referid="url" /></A>
            </mm:present>

			<BR><BR>
		</TD>
</TR>
<TR>
	<TD>
	<BR>
	</TD>
</TR>

<TR>
	<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="#44BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Cloud Design</B>
	</TD>
	<TD BGCOLOR="#44BDAD" COLSPAN=2>
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Picture</B>
	</TD>
</TR>
<TR>	
		<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
		<TD BGCOLOR="#00425A" VALIGN="top">
The image on the right shows the basic-design of the community-application. 
Community is the toplevel node which can consists of multiple channels. 
Each channel exists of one or more mesages.
If people must login before they can participate in a channel, there must be
a relation between the people node and the channel. 
A community can be a forum or a chat.
		</TD>
		<TD BGCOLOR="#00425A" COLSPAN=2>
		<A HREF="../share/images/community_cloud.jpg" TARGET="img">
		<IMG SRC="../share/images/community_cloud.jpg" WIDTH="220">
		</A>
		</TD>
</TR>

<TR>
	<TD>
	<BR>
	</TD>
</TR>


<TR>
	<TD><IMG SRC="../../mmadmin/jsp/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
	<TD BGCOLOR="#44BDAD">
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Manual</B>
	</TD>
	<TD BGCOLOR="#44BDAD" COLSPAN=1>
	<FONT COLOR="000000" FACE=helvetica,arial,geneva SIZE=2>
	<B>Picture</B>
	</TD>
</TR>
<TR>	
		<TD><IMG SRC="../../mmadmin/images/trans.gif" WIDTH="50" HEIGHT="1"></TD>
		<TD BGCOLOR="#00425A" VALIGN="top">
			The image on the right shows the homepage of the community-example. 
This example shows you the basic features of the community application.
		</TD>
		<TD BGCOLOR="#00425A" COLSPAN=1>
		<A HREF="../share/images/community_manual.jpg" TARGET="img">
		<IMG SRC="../share/images/community_manual.jpg" WIDTH="220">
		</A>
		</TD>
</TR>

</FORM>

</TABLE>


</BODY>
</HTML>
</mm:cloud>





