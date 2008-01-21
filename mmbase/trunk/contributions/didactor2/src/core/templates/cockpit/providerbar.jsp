<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <!--
      Show all components that are related to the current provider.
      There are several 'default' components that will be shown in the
      standard layout: as hyperlinks from left to right. All other
      components that are directly related to the provider object will
      be placed in a dropdown box.

  -->
  <mm:cloud method="asis">
    <div class="providerMenubar">

      <di:include page="/cockpit/keepalive.jsp" /> <!-- not necessary when using di:html, but it has a duplicate-include-protection -->

      <mm:hasrank minvalue="didactor user">
        <mm:node referid="provider">
          <mm:functioncontainer>
            <mm:param name="bar">provider</mm:param>
            <mm:listfunction name="components">
              <di:menuitem component="${_}" />
            </mm:listfunction>
          </mm:functioncontainer>
        </mm:node>
      </mm:hasrank>

      <!-- If the user has the rights, then always show the management link. That allows us to enable/disable components after install on an empty database -->
      <di:menuitem component="${di:component('education')}" />

      <mm:hasrank value="anonymous">
        <div class="provideranonymous">
          <mm:node referid="provider">
            <mm:nodeinfo type="gui" />
          </mm:node>
        </div>
      </mm:hasrank>
    </div>
  </mm:cloud>
</jsp:root>
