<%@include file="/taglibs.jsp" %>
<%@include file="../../request_parameters.jsp" %>
<mm:cloud jspvar="cloud">
  <%
  extendedHref = ph.createPaginaUrl("bel",request.getContextPath());
	%>
  <table width="180" cellspacing="0" cellpadding="0">
	<tr>
		<td width="180">
		<table width="180" cellspacing="0" cellpadding="0">
			<tr>
			<td class="maincolor" width="0%"><a href="<%@include file="../includes/extendedhref.jsp" 
				%>"><img src="media/telefoon.gif" border="0" alt=""></a></td>
			<td class="maincolor" style="vertical-align:middle;padding-right:2px;" width="100%"><nowrap><a href="<%@include file="../includes/extendedhref.jsp" 
				%>" class="white"><bean:message bundle="LEOCMS" key="shop.phonelink.order_by_phone" /></a></nowrap></td>
			<td class="maincolor" style="padding:2px;" width="100%"><a href="<%@include file="../includes/extendedhref.jsp" 
				%>"><img src="media/pijl_wit_op_oranje.gif" border="0" alt=""></a></td>
			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td width="180" colspan="2"><img src="media/trans.gif" height="1" width="180" border="0" alt=""></td>
	</tr>
	<tr>
		<td class="subtitlebar" width="180" colspan="2"><bean:message bundle="LEOCMS" key="shop.phonelink.phonenumber" /></td>
	</tr>
</table>