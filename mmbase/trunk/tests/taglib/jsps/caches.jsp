<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
  %><%@ page import="java.util.*,org.mmbase.util.*,org.mmbase.cache.Cache" %>
<html>
<head><title>MMBase Caches</title></head>
<body>
<mm:import externid="cache" from="parameters" />
<mm:import externid="bytesize" from="parameters" jspvar="bytesize" vartype="string">false</mm:import>

<mm:notpresent referid="cache">

<h1>MMBase Caches</h1>

<table width="100%" border="1" celpadding="1">
<tr><th>Cache</th><th>Size</th><mm:compare referid="bytesize" value="true"><th>Size(bytes)</th></mm:compare><th>Filled</th><th>Hits</th><th>Misses</th><th>Performance</th><th>Content</th></tr>
<% Iterator i = org.mmbase.cache.Cache.getCaches().iterator();

  java.text.NumberFormat form = java.text.NumberFormat.getInstance(); form.setMaximumFractionDigits(1);
   while (i.hasNext()) {     
      Cache cache = Cache.getCache((String) i.next());
      out.println("<tr><td>" + cache.getDescription() + " (" + cache.getName() + ")</td><td align=\"right\">" + cache.getSize() + "</td>" +
      ( "true".equals(bytesize) ? "<td align=\"right\">" +cache.getByteSize() + "</td>" : "") + "<td align=\"right\">" + form.format(cache.size() * 100 /cache.getSize()) + "%</td><td align=\"right\">" + cache.getHits() + " </td><td align=\"right\">" + cache.getMisses() + " </td><td align=\"right\">" + (cache.isActive() ? form.format(cache.getRatio() * 100) + "%" : "not active") + "</td>");
%>
<td align="center"><a href="<mm:url referids="bytesize"><mm:param name="cache"><%= cache.getName() %></mm:param></mm:url>">show</a></td></tr>
<%

  }
%>
</table>
<a href="<mm:url ><mm:param name="bytesize"><%= "true".equals(bytesize) ? "false": "true"%></mm:param></mm:url>">Toggle byte calculation</a>
</mm:notpresent>

<mm:present referid="cache">
<mm:write referid="cache" jspvar="cacheName" vartype="String">
<% Cache cache = Cache.getCache(cacheName); 
  if (cache != null) {
%>
<mm:import externid="removeentry" />
<mm:present referid="removeentry">
 <mm:write referid="removeentry" jspvar="entry" vartype="string">
 <% cache.remove(entry); %>
  </mm:write>
</mm:present>
<h1><%= cache.getDescription() %> Cache</h1>
<table width="100%" border="1" celpadding="1">
<tr><th>#</th><th>Key</th><th>Value</th><mm:compare referid="bytesize" value="true"><th>Size (byte)</th><th></th></mm:compare></tr>
<%
    Iterator i = cache.getOrderedEntries(50).iterator();
    int j = 0;
    while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            Object key = entry.getKey();
        out.println("<tr><td>" + ++j + "</td><td>'" + key + "'" + key.getClass().getName() + "</td><td>" +
                  entry.getValue() + "("+ entry.getValue().getClass() +")</td>" +
                  ("true".equals(bytesize) ? "<td>" + (SizeOf.getByteSize(entry.getValue()) + SizeOf.getByteSize(entry.getKey())) + "</td>" : "")
              + "<td><a href=\"?cache=" + cache.getName() + "&removeentry=" + entry.getKey() + "\">Remove</a></td></tr>");
    }
}
%>
</table>
<hr />
</mm:write>
<a href="<mm:url referids="bytesize" />">Back</a><br />
<a href="<mm:url referids="cache"><mm:param name="bytesize"><%= "true".equals(bytesize) ? "false": "true"%></mm:param></mm:url>">Toggle byte calculation</a><br />
</mm:present>
  <br />
<mm:compare referid="bytesize" value="true">
  Total size: <%= Cache.getTotalByteSize() %><br />
</mm:compare>
 Size of <a href="session.jsp">session:</a> <%= SizeOf.getByteSize(session) %> byte<br />
 <%
      Runtime rt = Runtime.getRuntime();
      out.println("total memory      : " + rt.totalMemory() / (1024 * 1024) + " Mbyte<br />");
      rt.gc();
      out.println("free memory       : " + rt.freeMemory() / (1024 * 1024) + " Mbyte<br />");
%>
<hr />
<a href="<mm:url page="/" />">Home </a>

</body>
</html>
