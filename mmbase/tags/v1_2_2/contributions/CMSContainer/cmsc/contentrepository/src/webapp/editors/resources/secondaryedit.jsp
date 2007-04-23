<%@page language="java" contentType="text/html;charset=UTF-8"%>
<%@include file="globals.jsp" %>
<mm:content type="text/html" encoding="UTF-8" expires="0">
<html:html xhtml="true">
<head>
	<title><fmt:message key="secondaryedit.title" /></title>
	<link href="../css/main.css" type="text/css" rel="stylesheet" />
	<script>
		function save() {
			var form = document.forms["SecondaryEditForm"];
			if(form["title"].value.length > 0) {
				form["action"].value = "save";
				return true;
			}
			else {
				alert("<fmt:message key="secondaryedit.error.title" />");
				return false;
			}
		}

		function cancel() {
			document.forms["SecondaryEditForm"]["action"].value = "cancel";
		}
	</script>
</head>
<body>
    <div class="tabs">
        <div class="tab_active">
            <div class="body">
                <div>
                    <a href="#"><fmt:message key="secondaryedit.title" /></a>
                </div>
            </div>
        </div>
    </div>

	<div class="editor">
		<div class="body">
		  <html:form action="/editors/resources/SecondaryEditAction">
		  	<html:hidden property="action"/>
		  	<html:hidden property="number"/>
		  	<html:hidden property="returnUrl"/>
			<table border="0">
			   <tr>
			      <td style="width: 150px"><fmt:message key="secondaryedit.titlefield" /></td>
			      <td><html:text property="title" style="width: 350px"/></td>
			   </tr>		  	
			   <tr>
			      <td style="width: 150px"><fmt:message key="secondaryedit.description" /></td>
			      <td><html:textarea property="description" style="width: 350px; height:75px"/></td>
			   </tr>		  	
		  		<tr>
		  			<td colspan="2">
					  	<html:submit onclick="return save();"><fmt:message key="secondaryedit.save" /></html:submit>
					  	<html:submit onclick="cancel();"><fmt:message key="secondaryedit.cancel" /></html:submit>
					</td>
				</tr>
			  	<mm:cloud>
				  	<mm:node number="${SecondaryEditForm.number}">
					  	<mm:nodeinfo type="type" jspvar="nodetype" write="false"/>
					  	<c:if test="${nodetype == 'images'}">
					  		<tr>
					  			<td colspan="2">
							  		<img src="<mm:image template="s(600x300)"/>" alt="<mm:field name="description" />"/>
							  	</td>
							</tr>
					  	</c:if>
					</mm:node>
				</mm:cloud>
			</table>
		  </html:form>
		</div>
		<div class="side_block_end"></div>
	</div>	
</body>
</html:html>
</mm:content>	            