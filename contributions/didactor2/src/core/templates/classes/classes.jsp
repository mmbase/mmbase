<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<mm:cloud jspvar="cloud">
<html>
<head>
   <link href="style/color/wizard.css" type="text/css" rel="stylesheet"/>
   <link href="style/layout/wizard.css" type="text/css" rel="stylesheet"/>
</head>
<body style="overflow:auto;">
<b>Overzicht klassen</b><br/>
<mm:listnodes type="classes" orderby="name">
   <li><mm:field name="name" /><br/>
</mm:listnodes>
</body>
</html>
</mm:cloud>

