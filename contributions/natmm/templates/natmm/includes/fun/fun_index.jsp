<%@include file="/taglibs.jsp" %>
<%@include file="../../includes/request_parameters.jsp" %>
<%@include file="../../includes/getstyle.jsp" %>
<mm:cloud jspvar="cloud">
<mm:import id="nodotline" />
<%@include file="../../includes/page_intro.jsp" %>
<div style="margin:7px 0px 7px 0x">Maak een keuze uit de volgende categorie&euml;n:</div>
<%@include file="../../includes/dossier_form.jsp" %>
<br>
<jsp:include page="../includes/shorty.jsp">
   <jsp:param name="s" value="<%= paginaID %>" />
   <jsp:param name="r" value="<%= rubriekID %>" />
   <jsp:param name="rs" value="<%= styleSheet %>" />
   <jsp:param name="sr" value="1" />
</jsp:include>
<table class="dotline"><tr><td height="3"></td></tr></table>
<table border="0" cellspacing="0" cellpadding="0">
	<tr>
   <%
   int imgCnt= 0;
	if(!dossierID.equals("-1")){%>
		<mm:list nodes="<%=dossierID%>" path="dossier,posrel,images" fields="posrel.pos,images.number" orderby="posrel.pos">
			<mm:field name="images.number" jspvar="images_number" vartype="String" write="false">
				<% imgCnt++;%>
				<td align="center">
					<mm:list nodes="<%=paginaID%>" path="pagina,gebruikt,paginatemplate">
						<mm:field name="paginatemplate.url">
						<mm:compare value="ecard.jsp" >
							<a href="<mm:write />?id=<%= images_number %>">
						</mm:compare>
						<mm:compare value="wallpaper.jsp">
							<a href="javascript:OpenWindow('<mm:write />?id=<%= images_number %>','','toolbar=no,menubar=no,location=no,height=450,width=517');">
						</mm:compare>
						</mm:field>
				    </mm:list>
					<mm:node number="<%= images_number %>"><img src="<mm:image template="s(175)" /></mm:node>" alt="<mm:field name="alt_tekst" />" border="0" vspace="5" hspace="5"></a><br>
					<mm:field name="images.titel" jspvar="sTitle" vartype="String" write="false">
                  <%= sTitle.substring(0,1).toUpperCase() + sTitle.substring(1) %>
               </mm:field>  
   			</td>
				<% if(imgCnt % 3 == 0){ %></tr><tr><% } %>
			</mm:field>
			<mm:last>
				<% if(imgCnt % 3 != 0){  if(imgCnt % 2 == 0){%><td>&nbsp;</td><td>&nbsp;</td><%} else if(imgCnt % 1 == 0){%><td>&nbsp;</td><% } %></tr><% } %>
			</mm:last>
		</mm:list>
	<% } %>
</table><br>
<table class="dotline"><tr><td height="3"></td></tr></table>
</mm:cloud>