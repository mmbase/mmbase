<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud jspvar="cloud" method="asis">
<%@include file="/shared/setImports.jsp" %>
<div class="educationMenubar" style="white-space: nowrap">
<mm:present referid="education">
  <mm:treeinclude page="/progress/cockpit/bar_connector.jsp" objectlist="$includePath" referids="$referids"/>
</mm:present>			
  <div class="educationMenubarNav">
  <mm:present referid="education">
    <mm:node number="$education" notfound="skip">
      <mm:related path="settingrel,components">
        <mm:node element="components">
          <mm:field id="name" name="name" write="false" />
          <mm:treeinclude page="/$name/cockpit/menuitem.jsp" objectlist="$includePath" referids="$referids">
            <mm:param name="name"><mm:field name="name" /></mm:param>
            <mm:param name="number"><mm:field name="number" /></mm:param>
            <mm:param name="type">div</mm:param>
            <mm:param name="scope">education</mm:param>
          </mm:treeinclude>
          <mm:remove referid="name" />
        </mm:node>
      </mm:related>
    </mm:node>
  </mm:present>
  </div>
 
</mm:cloud>
