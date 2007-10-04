<%@ page import="org.mmbase.module.core.MMBase,org.mmbase.bridge.*,java.util.*"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:cloud rank="administrator" loginpage="login.jsp" jspvar="cloud">
<div
	class="mm_c mm_c_core ${requestScope.componentClassName}"
	id="${requestScope.componentId}">
  <mm:import externid="builder" jspvar="builder" />
  <mm:import externid="path" jspvar="path" />
  <mm:import externid="cmd" jspvar="cmd" />

  <h3>Administrate builder ${builder}</h3>
  <table summary="description of <mm:write referid="builder" />" border="0" cellspacing="0" cellpadding="3">
  <caption>
    <mm:function referids="builder" module="mmadmin" name="BUILDERDESCRIPTION" />
     
    <mm:present referid="cmd">
      <mm:functioncontainer module="mmadmin">
         <mm:nodefunction name="$cmd" referids="builder,path" >
           <mm:field name="RESULT" escape="p" />
         </mm:nodefunction>
       </mm:functioncontainer>
    </mm:present>      
    
    </caption>
    <tr>
      <th>Setting</th>
      <th colspan="2">Value</th>
      <th class="center" colspan="2">Change</th>
    </tr>
    <tr>
      <td>Class</td>
      <td colspan="2"><mm:function referids="builder" module="mmadmin" name="BUILDERCLASSFILE" /></td>
      <td class="center" colspan="2">
        Not Available
      </td>
    </tr>
    <tr><td colspan="5"> </td></tr>
    <tr>
      <th>Field</th>
      <th>Name</th>
      <th>Type</th>
      <th>Size</th>
      <th class="center">View</th>
    </tr>
    <mm:nodelistfunction referids="builder" module="mmadmin" name="FIELDS">
      <tr>
        <td><mm:field name="item1" /></td>
        <td>
          <mm:import id="field" reset="true"><mm:field name="item2" /></mm:import>
          <mm:link page="builders-field" referids="builder,field">
            <a href="${_}">${field}</a>
          </mm:link>
        </td>
        <td><mm:field name="item3" /></td>
        <td><mm:field name="item4" /></td>
        <td class="center">
          <mm:link page="builders-field" referids="builder,field">
            <a href="${_}"><img src="<mm:url page="/mmbase/style/images/search.png" />" alt="view properties" /></a>
          </mm:link>
        </td>
      </tr>
    </mm:nodelistfunction>
  </table>

  <mm:link page="builders-actions" referids="builder">
    <mm:param name="cmd">BUILDERSAVE</mm:param>
    <form action="${_}" method="post">
  </mm:link>
  <table summary="save <mm:write referid="builder" />" border="0" cellspacing="0" cellpadding="3">
    <tr>
      <th>Action</th>
      <th>Path</th>
      <th class="center">Confirm</th>
    </tr>
    <tr>
     <td>Save</td>
     <td><input name="path" value="/tmp/${builder}.xml" size="80" /></td>
     <td class="center">
       <input type="image" src="<mm:url page="/mmbase/style/images/ok.png" />" alt="OK" />
     </td>
    </tr>
  </table>
  </form>
  
  <p>
    <mm:link page="builders">
      <a href="${_}"><img src="<mm:url page="/mmbase/style/images/back.png" />" alt="back" /></a>
      <a href="${_}">Return to Builder Overview</a>
    </mm:link>
  </p>
  
</div>
</mm:cloud>
