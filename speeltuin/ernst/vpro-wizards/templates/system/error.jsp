<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%--
<%@ taglib uri="/WEB-INF/taglibs-mailer.tld" prefix="mt" %>
--%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <title>3V12 Redactie Error</title>
    <link rel="stylesheet" type="text/css" href="/edit/stylesheets/edit.css"/>
</head>
<body class="error">
<div class="message">
    <h4>FOUT</h4>
    <mm:import id="text">
        <p>TODO: How about a really meaningfull error page? Maybe not. Maybe i want
        to see the error in the context of the page that coused it. Dunno yet.
    </mm:import>
    <mm:write referid="text"/>
    <a href="javascript:location=document.referrer;">terug</a>
</div>
<mm:cloud jspvar="cloud" method="loginpage" loginpage="/edit/login.jsp">
    <mm:import id="user"><%= "" + cloud.getUser().getIdentifier() %></mm:import>
</mm:cloud>

</body>
</html>