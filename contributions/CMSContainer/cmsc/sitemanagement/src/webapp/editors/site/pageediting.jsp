<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <link href="../css/toolbar.css" rel="stylesheet" type="text/css">
</head>
<script type="text/javascript">
    function preview() {
        var contentLocation = this.frames["pcontent"].location.href;
        if (contentLocation.indexOf("mode=preview") > -1) {
            setPortletMode(false, false, false);
            setElementStyleByClassName('portlet-mode-spacer', 'display', 'none');
            setElementStyleByClassName('portlet-header-canvas', 'display', 'none');
            setElementStyleByClassName('portlet-canvas', 'borderWidth', '0px');
            setElementStyleByClassName('portlet-mode-canvas portlet-mode-type-view', 'display', 'none');
        }
    }

    function setPortletMode(admin, edit, view) {
        setElementStyleByClassName('portlet-mode-type-admin', 'display', admin ? '' : 'none');
        setElementStyleByClassName('portlet-mode-type-edit', 'display', edit ? '' : 'none');
        setElementStyleByClassName('portlet-mode-type-view', 'display', view ? '' : 'none');
    }

    function setElementStyleByClassName(cl, propertyName, propertyValue) {
        if (!pcontent.document.getElementsByTagName) return;
        var re = new RegExp("(^| )" + cl + "( |$)");
        var el = pcontent.document.all ? pcontent.document.all : pcontent.document.getElementsByTagName("body")[0].getElementsByTagName("*"); // fix for IE5.x
        for (var i = 0; i < el.length; i++) {
            if (el[i].className && el[i].className.match(re)) {
                el[i].style[propertyName] = propertyValue;
            }
        }
    }
</script>

<c:set var="str">${requestScope.toolbar}?number=${requestScope.nodeId}&pagepath=${requestScope.pathofpage}</c:set>
<frameset rows="35,*" framespacing="0" border="0">
    <frame frameborder="0" src="${str}" name="toolbar" scrolling="no"/>
    <frame frameborder="0" src="${requestScope.pathofpage}" onload="preview()" name="pcontent"/>
</frameset>

</html>