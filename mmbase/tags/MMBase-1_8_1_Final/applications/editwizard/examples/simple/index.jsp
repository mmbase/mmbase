<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1.1-strict.dtd">
<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
%><mm:content type="text/html" expires="0">
<mm:cloud rank="basic user"><!-- make sure user is logged in --%>
<html>
<head>
  <title>EditWizard samples</title>
  <link rel="stylesheet" type="text/css" href="../style.css" />
  <!-- Very straightforward example -->
</head>
<body>
  <mm:import externid="wizards">wizard</mm:import>
  <mm:import id="jsps"><mm:write value="/mmbase/edit/$wizards/jsp" /></mm:import>
  <form>
    <h1>Editwizard - samples (<a href="<mm:url><mm:param name="wizards">wizard.deprecated</mm:param></mm:url>">Deprecated look, if installed</a>)</h1>
    <p>
      This example uses the default editwizard XSL's. It also uses some
      default XML's which can be found in the editwizard directory under
      data/samples/.
    </p>
    <table>
      <mm:import id="referrer"><%=new  java.io.File(request.getServletPath())%></mm:import>
      <tr><td>
      <a href="<mm:url referids="referrer" page="$jsps/list.jsp?wizard=../samples/people&nodepath=people&fields=firstname,lastname,owner" />" >Person-Test</a>
      </td><td>
      A simple one-step person editor. First-name, last-name and related articles.
      </td></tr>
      <tr><td>
      <a href="<mm:url referids="referrer" page="$jsps/list.jsp?wizard=lib/createimage&nodepath=images&fields=title,handle" /> " >Images</a>
      </td><td>
      You can also upload images with an editwizard. Here is shown how this can be done.
      </td></tr>
      <tr><td>
      <a href="<mm:url referids="referrer" page="$jsps/list.jsp?wizard=../samples/news&nodepath=news&fields=title,date,owner&search=yes" />" >News</a>
      </td><td>
      An editor for news articles. In the one step you can create or add a news article and relate people and images to it.
      </td></tr>
    </table>
    <hr />
    <a class="navigate" href="../"><img alt="back" src="<mm:url page="/mmbase/style/images/back.png" />" /></a><br />
</form>
</body>
</html>
</mm:cloud>
</mm:content>
