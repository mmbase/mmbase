<%@ include file="inc/top.jsp" %><mm:cloud><html><head>	<title>edittag - example 5</title><%@ include file="inc/head.jsp" %></head><body><%@ include file="inc/nav.jsp" %><h4>edittag met parameters</h4><p>Een artikel met gerelateerde plaatjes en links. En hier is aan een plaatje ook weer een persoon gerelateerd.</p><mm:edit editor="yammeditor.jsp">  <mm:param name="naam" value="yammeditor" />  <mm:param name="plaatje">/mmbase/edit/my_editors/img/mmbase-edit.gif</mm:param>  <mm:node number="artikel">	<h2>[<mm:field name="number" id="nr" />] <mm:field name="title" /></h2>	<div class="intro"><mm:field name="intro" /></div>	<mm:list nodes="$nr" path="news,posrel,images" 	  fields="posrel.pos"	  orderby="posrel.pos">	  <mm:node element="images">		<em><img src="<mm:image template="s(200)" />" width="200" /><br />		[<mm:field name="number" />] <mm:field name="title" /></em>		<mm:related path="people" fields="people.firstname">(van: <mm:field name="people.firstname" />)</mm:related><br />	  </mm:node>	</mm:list>  </mm:node></mm:edit></body></html></mm:cloud>