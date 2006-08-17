<%@page import="nl.leocms.util.tools.SearchUtil" %>
<%@include file="/taglibs.jsp" %>
<mm:cloud logon="admin" pwd="<%= (String) com.finalist.mmbase.util.CloudFactory.getAdminUserCredentials().get("password") %>" method="pagelogon" jspvar="cloud">
<%@include file="includes/templateheader.jsp" %>
<%@include file="includes/cacheparams.jsp" %>
<%@include file="includes/calendar.jsp" %>
<%

String descriptionId = request.getParameter("description"); if(descriptionId==null) { descriptionId=""; }

// smoelenboek update
String firstnameId = request.getParameter("firstname"); if(firstnameId==null) { firstnameId=""; }
String initialsId = request.getParameter("initials"); if(initialsId==null) { initialsId=""; }
String suffixId = request.getParameter("suffix"); if(suffixId==null) { suffixId=""; }
String lastnameId = request.getParameter("lastname"); if(lastnameId==null) { lastnameId=""; }
String companyphoneId = request.getParameter("companyphone"); if(companyphoneId==null) { companyphoneId=""; }
String cellularphoneId = request.getParameter("cellularphone"); if(cellularphoneId==null) { cellularphoneId=""; }
String faxId = request.getParameter("fax"); if(faxId==null) { faxId=""; }
String emailId = request.getParameter("email"); if(emailId==null) { emailId=""; }
String deptdescrId = request.getParameter("deptdescr"); if(deptdescrId==null) { deptdescrId=""; }
String progdescrId = request.getParameter("progdescr"); if(progdescrId==null) { progdescrId=""; }
String posdescrId = request.getParameter("posdescr"); if(posdescrId==null) { posdescrId=""; }
String descrupdateId = request.getParameter("descrupdate"); if(descrupdateId==null) { descrupdateId=""; }
String introupdateId= request.getParameter("introupdate"); if(introupdateId==null) { introupdateId=""; }

postingStr += "|";
String action = getResponseVal("action",postingStr);

String thisPrograms = "";
%><mm:list nodes="<%= paginaID %>" path="pagina,posrel,programs"
    ><mm:field name="programs.number" jspvar="programs_number" vartype="String" write="false"><%
        thisPrograms += "," + programs_number;
    %></mm:field
></mm:list><%
if(!thisPrograms.equals("")) {
    thisPrograms = thisPrograms.substring(1);
} 
%><%@include file="includes/header.jsp" 
%><td><%@include file="includes/pagetitle.jsp" %></td>
<td><% String rightBarTitle = "Zoek een collega"; 
%><%@include file="includes/rightbartitle.jsp" 
%></td>
</tr>
<tr>
<td class="transperant" valign="top">
<div class="<%= infopageClass %>" id="infopage"><%
    if(postingStr.equals("|action=back")) {
        %><div align="right" style="letter-spacing:1px;"><a href="javascript:history.go(-1);">terug</a>&nbsp;</div><% 
    } %>
<table border="0" cellpadding="0" cellspacing="0">
    <tr><td style="padding:10px;padding-top:18px;"><% 
    
    if(nameId.equals(nameEntry)) nameId = "";
    if(!nameId.equals("")) {

        String fname = "";
        String lname = "";

        // *** if possible split name in firstname and lastname ***
        nameId = nameId.trim();
        int sPos = nameId.indexOf(" ");
        if(sPos>-1) {
            fname = nameId.substring(0,sPos);
            sPos = nameId.lastIndexOf(" ");
            lname = nameId.substring(sPos+1);;
        } else {
            fname = nameId;
            lname = nameId;
        }

        if(firstnameId.equals("")&&lastnameId.equals("")) { 
            firstnameId = fname;
            lastnameId = lname;
        } else if(!firstnameId.equals(fname)||!lastnameId.equals(lname)) {
            // *** remove search on name, if this is a repeated search with changes in name ***
            nameId = "";
        }
    }

    if(action.equals("commit")) { // *** commit the changes ***
            %><%@include file="includes/peoplefinder/commit.jsp" %><%
    } else if(!employeeId.equals("-1")) { // *** there is an employee selected ***
        if(action.equals("change")) { // *** show form to update info ***
            %><%@include file="includes/peoplefinder/update.jsp" %><%
        } else { // *** just show the info on the employee ***
            %>
				<jsp:include page="includes/peoplefinder/table.jsp">
					<jsp:param name="e" value="<%= employeeId %>" />
					<jsp:param name="tp" value="<%= thisPrograms %>" />
					<jsp:param name="it" value="<%= imageTemplate %>" />
					<jsp:param name="p" value="<%= paginaID %>" />
					<jsp:param name="ps" value="<%= postingStr %>" />
					<jsp:param name="tqs" value="<%= templateQueryString %>" />
					<jsp:param name="d" value="<%= departmentId %>" />
					<jsp:param name="ps" value="<%= programId %>" />
					<jsp:param name="f" value="<%= firstnameId %>" />
					<jsp:param name="l" value="<%= lastnameId %>" />
				</jsp:include>
				<%
        }
    } else {  // ***  no employee selected, show the intro image and text ***

        %><table border="0" cellpadding="0" cellspacing="0" style="width:100%;padding-top:20px;"> 
            <tr>
                <td><mm:list nodes="<%= paginaID %>" path="pagina,posrel,images" 
                        constraints="posrel.pos='1'"
                        ><% imageTemplate = "s(300)"; 
                        %><div align="center"><img src=<%@include file="includes/imagessource.jsp" %> width="100%" alt="" border="0" ></div>
                    </mm:list
                ></td>
            </tr>
            <tr>
                <td><%@include file="includes/relatedteaser.jsp" %><br><br></td>
            </tr>
        </table><%

} %></td></tr>
</table>
</div>
</td>
<td valign="top"><%
    if(action.equals("")) {
        %><%@include file="includes/peoplefinder/form.jsp" 
        %><%@include file="includes/whiteline.jsp" 
        %><%@include file="includes/peoplefinder/result.jsp" %><%
    } %></td>
<%@include file="includes/footer.jsp" %>
</mm:cloud>