<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" 
%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<mm:content type="text/html">
<html>
<head>
<%
    String category=request.getParameter("category");
    String subcategory=request.getParameter("subcategory");
%>
<link rel="stylesheet" href="<mm:url page="/mmbase/style/css/mmbase.css" />" type="text/css">
<title>Navigation Bar <%=category%>/<%=subcategory%></title>
</head>
<body class="navigationbar">
<table summary="navigation">
<tr>
<td width="50">
<img src="<mm:url page="/mmbase/style/logo.gif" />" border="0" alt="MMBase">
</td>
<td width="850" border="0">
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=about&subcategory=about" />" target="_top">
      <span class="<%=("about".equals(category)) ? "current" : ""%>menuitem">ABOUT</span>
    </a>
    <% if (pageContext.getServletContext().getResource("/mmexamples") != null) { %>
     &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=examples" />" target="_top">
      <span class="<%=("examples".equals(category)) ? "current" : ""%>menuitem">EXAMPLES</span>
    </a>
    <% } %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=editors&subcategory=basic" />" target="_top"
    ><span class="<%=("editors".equals(category)) ? "current" : ""%>menuitem">EDITORS</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=servers" />" target="_top"
    ><span class="<%=("admin".equals(category)) ? "current" : ""%>menuitem">ADMIN</span></a>
    <% if (pageContext.getServletContext().getResource("/mmdocs") != null) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=documentation&subcategory=overview" />" target="_top">
      <span class="<%=("documentation".equals(category)) ? "current" : ""%>menuitem">DOCUMENTATION</span>
    </a>
    <% } %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=tools&subcategory=cache" />" target="_top">
    <span class="<%=("tools".equals(category)) ? "current" : ""%>menuitem">TOOLS</span></a>
        <hr />
    <% if("about".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=about&subcategory=about" />" target="_top"
    ><span class="<%=("about".equals(subcategory)) ? "current" : ""%>menuitem">ABOUT</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=about&subcategory=license" />" target="_top"
    ><span class="<%=("license".equals(subcategory)) ? "current" : ""%>menuitem">LICENSE</span></a>
        <% } else if("editors".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=editors&subcategory=basic" />" target="_top"
    ><span class="currentmenuitem">BASIC</span></a>
    <mm:haspage page="/mmbase/security">
      &nbsp;&nbsp;
      <a href="<mm:url page="default.jsp?category=editors&subcategory=security" />" target="_top"
      ><span class="currentmenuitem">SECURITY</span></a>
    </mm:haspage>
        <% } else if("examples".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=examples" />" target="_top"
    ><span class="currentmenuitem">MMBASE DEMOS</span></a>
        <% } else if("admin".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=resourceedit" />" target="_top"
    ><span class="<%=("resourceedit".equals(subcategory)) ? "current" : ""%>menuitem">RESOURCES</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=servers" />" target="_top"
    ><span class="<%=("servers".equals(subcategory)) ? "current" : ""%>menuitem">SERVERS</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=builders" />" target="_top"
    ><span class="<%=("builders".equals(subcategory)) ? "current" : ""%>menuitem">BUILDERS</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=applications" />" target="_top"
    ><span class="<%=("applications".equals(subcategory)) ? "current" : ""%>menuitem">APPLICATIONS</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=modules" />" target="_top"
    ><span class="<%=("modules".equals(subcategory)) ? "current" : ""%>menuitem">MODULES</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=databases" />" target="_top"
    ><span class="<%=("databases".equals(subcategory)) ? "current" : ""%>menuitem">DATABASES</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=admin&subcategory=blobs" />" target="_top" >
    <span class="<%=("blobs".equals(subcategory)) ? "current" : ""%>menuitem">BLOBS</span></a>
        <% } else if("documentation".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=documentation&subcategory=overview" />" target="_top"
    ><span class="currentmenuitem">OVERVIEW</span></a>
        <% } else if("tools".equals(category)) { %>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=tools&subcategory=cache" />" target="_top"
    ><span class="<%=("cache".equals(subcategory)) ? "current" : ""%>menuitem">CACHE</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=tools&subcategory=querytool" />" target="_top"
    ><span class="<%=("querytool".equals(subcategory)) ? "current" : ""%>menuitem">SQL</span></a>
    &nbsp;&nbsp;
    <a href="<mm:url page="default.jsp?category=tools&subcategory=email" />" target="_top" >
    <span class="<%=("email".equals(subcategory)) ? "current" : ""%>menuitem">EMAIL</span></a>
    &nbsp;&nbsp;
    <mm:haspage page="/mmbase/packagemanager/index.jsp">
      <a href="<mm:url page="/mmbase/packagemanager/index.jsp" />" target="_top" >
    </mm:haspage>    
      <span class="<%=("packagemanager".equals(subcategory)) ? "current" : ""%>menuitem">PACKAGEMANAGER</span>
     <mm:haspage page="/mmbase/packagemanager/index.jsp">
        </a>
    </mm:haspage>    
    &nbsp;&nbsp;
    <mm:haspage page="/mmbase/packagebuilder/index.jsp">
      <a href="<mm:url page="/mmbase/packagebuilder/index.jsp" />" target="_top" >
    </mm:haspage>
    <span class="<%=("packagebuilder".equals(subcategory)) ? "current" : ""%>menuitem">PACKAGEBUILDER</span>
    <mm:haspage page="/mmbase/packagebuilder/index.jsp">
      </a>
    </mm:haspage>
        <% } %>
</td>
</tr>
</table>
</body>
</html>
</mm:content>
