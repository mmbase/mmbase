<%@ include file="inc/top.jsp" %><mm:cloud><html><head>	<title>edittag - example 2</title><%@ include file="inc/head.jsp" %></head><body><%@ include file="inc/nav.jsp" %><h4>2de voorbeeld met edittag</h4><p>Een artikel met gerelateerde plaatjes en links. En hier is aan een plaatje ook weer een persoon gerelateerd.</p><mm:edit editor="yammeditor.jsp" icon="/mmbase/edit/my_editors/img/mmbase-edit.gif">	<mm:node number="artikel">	  <h2>[<mm:field name="number" id="nr" />] <mm:field name="title" /></h2>	  <div class="intro"><mm:field name="intro" /></div>	  <mm:list nodes="$nr" path="news,posrel,images">		<mm:node element="images">		  <em><img src="<mm:image template="s(200)" />" width="200" /><br />		  [<mm:field name="number" />] <mm:field name="title" /></em>		  <mm:related path="people" fields="people.firstname">(van: <mm:field name="people.firstname" />)</mm:related><br />		</mm:node>	  </mm:list>	  <div><mm:field name="body" />	  <mm:related path="posrel,urls"	  	  fields="urls.url">	  	  <mm:first><br /><strong>Links:</strong></mm:first>	  	  <a href="<mm:field name="urls.url" />"><mm:field name="urls.name" /></a>	  	  <mm:last inverse="true">,</mm:last>	  </mm:related>	  </div>	</mm:node></mm:edit>	<% 	out.println("<br /><br /><b>Session info</b>");	String[] names  = session.getValueNames();	for (int i = 0; i < names.length; i++) {		out.println("<br /><br />session name : " + names[i]);		out.println("<br />session value : " + session.getValue(names[i]));	}	%></body></html></mm:cloud>