    <meta name="MMBase-SessionName"     content="${config.session}" />

    <link rel="icon" href="images/favicon.ico" type="image/x-icon" />
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon" /><mm:write value="" request="org.mmbase.validation" />
    <link rel="StyleSheet" type="text/css" href="css/<mm:write referid="config.style_sheet" />"/>
    <jsp:directive.include file="/mmbase/validation/javascript.jspxf" />
    <meta name="MMBase-Language"     content="${config.lang}" />
    <mm:link page="/mmbase/validation/Widgets.js">
      <script type="text/javascript" src="${_}"> </script>
    </mm:link>
    <script type="text/javascript" src="javascript.js"> </script>
