<mm:edit type="basic" >
  <%--
      optional parameters

  <mm:param name="url" value="/yammeditor/yammeditor.jsp" />
  <mm:param name="icon">/mmbase/edit/my_editors/img/mmbase-edit.gif</mm:param>
  --%>
  <mm:node number="default.mags" notfound="skip">
    <h2><mm:field name="title" /></h2>
    <mm:relatednodes role="posrel" orderby="posrel.pos"  type="news">
      <h3><mm:field name="title" /></h3>
      <mm:relatednodes type="people">
        by <mm:field name="email" />
      </mm:relatednodes>
    </mm:relatednodes>
  </mm:node>
</mm:edit>
