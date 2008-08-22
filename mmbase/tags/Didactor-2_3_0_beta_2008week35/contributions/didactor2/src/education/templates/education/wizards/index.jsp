<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <jsp:directive.page buffer="20000kb" />
  <di:html
      type="text/html"
      styleClass="editwizards"
      component="education.wizards"
      title_key="education.editwizards"
      rank="editor" expires="0">

    <!--
    <script type="text/javascript">

      addEventHandler(window, 'load', loadTree);
      addEventHandler(window, 'unload', storeTree);
    </script>
    -->
    <div class="rows" id="rows">

      <di:include debug="html" page="/education/wizards/javascript.jsp" />

      <di:include debug="html" page="/education/wizards/navigation.jspx" />

      <div class="mainContent">
        <table class="layout">
          <tr>
            <td id="left_menu">
              <mm:treeinclude debug="html" page="/education/wizards/code.jsp" objectlist="$includePath" />
            </td>
            <td class="content">
              <mm:treefile id="ok" page="/education/wizards/ok.jsp"
                           objectlist="$includePath" referids="$referids" write="false">
                <iframe id="text" name="text" width="99%" height="100%" marginwidth="0" marginheight="0" border="1" src="${ok}"><jsp:text> </jsp:text></iframe>
              </mm:treefile>
            </td>
          </tr>
        </table>
      </div>
    </div>
  </di:html>
</jsp:root>
