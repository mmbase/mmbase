<html>
<head>
	<title>EditWizard samples</title>
	<link rel="stylesheet" type="text/css" href="../style.css" />
   <%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
   <!-- Very straightforward example -->
</head>
<body>
<form>
	<h1>Editwizard - samples</h1>
  <p>
   This example uses the default editwizard XSL's. It also uses some
   default XML's which can be found in the editwizard directory under
   data/samples/.
  </p>
	<table>
  <tr><td>
	<a href="<mm:url page="/mmapps/editwizard/jsp/list.jsp?wizard=samples/people&nodepath=people&fields=firstname,lastname,owner" />" >Person-Test</a>
  </td><td> 
  A simple one-step person editor. First-name, last-name and related articles.
  </td></tr>
  <tr><td>
	  <a href="<mm:url page="/mmapps/editwizard/jsp/list.jsp?wizard=samples/imageupload&nodepath=images&fields=title" /> " >Images</a>
  </td><td> 
   You can also upload images with an editwizard. Here is shown how this can be done.
  </td></tr>
  <tr><td>
	<a href="<mm:url page="/mmapps/editwizard/jsp/list.jsp?wizard=samples/news&nodepath=news&fields=title,owner" />" >News</a>
    </td><td> 
   An editor for news articles. In the one step you can create or add a news article and relate people and images to it.
  </td></tr>
  </table>
  <hr />

  <a href="../index.html">back</a>
 
</form>
</body>
</html>
