<%@include file="../demo/includes/taglibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<cmsc:location var="cur" sitevar="site" />
<c:set var="page" value="${cur.id}" />

<mm:content type="text/html" encoding="UTF-8">
	<html xmlns="http://www.w3.org/1999/xhtml" lang="${site.language}" xml:lang="${site.language}">
	<cmsc:screen>
		<head>
		<title>${cur.title}</title>
		</head>
		<body>
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td><cmsc:insert-portlet layoutid="column1_1" /></td>
			</tr>
			<tr>
				<td><cmsc:insert-portlet layoutid="column1_2" /></td>
			</tr>
			<tr>
				<td><cmsc:insert-portlet layoutid="column1_3" /></td>
			</tr>
		</table>
		</body>
	</cmsc:screen>
	</html>
</mm:content>
