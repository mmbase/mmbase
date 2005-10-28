<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm"%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:content postprocessor="reducespace">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">


<%@include file="/shared/setImports.jsp" %>
<html>
<head>
   <link rel="stylesheet" type="text/css" href="<mm:treefile page="/css/base.css" objectlist="$includePath" />" />
</head>
<body>
<div class="learnenvironment">
<mm:import externid="learnobject" required="true"/>

<!-- TODO show the flash animation -->


<%-- remember this page --%>
<mm:treeinclude page="/education/storebookmarks.jsp" objectlist="$includePath" referids="$referids">
    <mm:param name="learnobject"><mm:write referid="learnobject"/></mm:param>
    <mm:param name="learnobjecttype">flashpages</mm:param>
</mm:treeinclude>


<mm:node number="$learnobject" jspvar="flash">
    <% 
        int layout = flash.getIntValue("layout");
        int width = 770;
        int height= 440;
        if (layout >= 0) {
            if (layout >= 2) {
                width = 385;
                height = 220;
                %><table class="Font"><tr><td><%
            }
            if (layout == 2 || layout == 0) {
                %><mm:field name="text" escape="none"/><%
                if (layout == 2) {
                    %></td><td><% 
                }
            }
        }
    %>
   <mm:relatednodes type="attachments">
<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" codebase="http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,47,0" width="760" height="440" id="flashpage">
              <param name="movie" value="<mm:attachment/>">
              <param name="quality" value="high">
              <embed src="<mm:attachment/>" quality="high" pluginspage="http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash" type="application/x-shockwave-flash" width="<%= width %>" height="<%= height %>" name="flashpage" swLiveConnect="true">
              </embed> 			  
          </object>
  </mm:relatednodes>
    <%
        if (layout >= 2) {
            if (layout == 2) {
                %></td><%
            }
            if (layout == 3) {
                %></td><td><mm:field name="text" escape="none"/></td><%
            }
            %></tr></table><%
        }
        else if (layout == 1) {
            %><mm:field name="text" escape="none"/><%
        }
    %>
  
</mm:node>
</div>
</body>
</html>
</mm:cloud>
</mm:content>
 
