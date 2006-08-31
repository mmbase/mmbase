<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml/DTD/transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java" %>
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud name="mmbase" method="http" rank="administrator">
<mm:content type="text/html" encoding="UTF-8" escaper="entities" expires="0">
<html>
  <head>
    <title>MMBob - fix bodies</title>
  </head>
  <body>

    <h1>Checking all postings</h1>
    <mm:listnodescontainer type="postings">
      <p>Er zijn <mm:size/> postings</p>
      <mm:listnodes type="postings">
        Begin met: <mm:field name="number"/><br/>
          <mm:field name="body" jspvar="body" vartype="String" write="false">
            <% 
              body = body.replaceAll("<poster>","<posting>");
              body = body.replaceAll("</poster>","</posting>");
              body = body.replaceAll("<mmxf>","<posting>");
              body = body.replaceAll("</mmxf>","</posting>");
              body = body.replaceAll("<p>","");
              body = body.replaceAll("</p>","<br />");
            %>
            <%=body%><hr />
            <mm:setfield name="body"><%=body%></mm:setfield>
          </mm:field>
        
      </mm:listnodes>
    </mm:listnodescontainer>

  </body>
</html>
</mm:content>
</mm:cloud>