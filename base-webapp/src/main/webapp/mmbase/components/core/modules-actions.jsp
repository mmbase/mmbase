<%@ page import="org.mmbase.bridge.*,java.util.*"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %>
<mm:content>
<mm:cloud rank="administrator" loginpage="login.jsp" jspvar="cloud">
<mm:import externid="module"   jspvar="module" />
<mm:import externid="property" jspvar="property" />
<mm:import externid="value"    jspvar="value" />
<mm:import externid="path"     jspvar="path" />
<mm:import externid="cmd"      jspvar="cmd" />
<mm:import externid="action" />
<div
  class="mm_c c_core b_modules ${requestScope['org.mmbase.componentClassName']}"
  id="${requestScope['org.mmbase.componentId']}">

<%
org.mmbase.bridge.Module mmAdmin=ContextProvider.getDefaultCloudContext().getModule("mmadmin");
    String msg="";
%>

<mm:present referid="cmd">
  <mm:compare referid="cmd" value="MODULE-SETPROPERTY">
    <%
    if (cmd != null) {
        try {
            Hashtable params = new Hashtable();
            params.put("MODULE", module);
            params.put("CLOUD", cloud);
            if (cmd.equals("MODULE-SETPROPERTY")) {
                if (property.length()==0) {
                    throw new Exception("Property name should be specified");
                }

                params.put("PROPERTYNAME", property);
                params.put("VALUE", value);
            }
            mmAdmin.process(cmd,module,params,request,response);
            msg="<p>"+mmAdmin.getInfo("LASTMSG",request,response)+"</p>";
        } catch (Exception e ) {
            msg="<p> Error: "+e.getMessage()+"</p>";
        }
    }
    %>
  </mm:compare>
  <mm:compare referid="cmd" value="MODULESAVE">
    <%
    try {
        Hashtable params=new Hashtable();
        params.put("module",module);
        params.put("PATH", path);
        mmAdmin.process(cmd,module,params,request,response);
        msg="<p>"+mmAdmin.getInfo("LASTMSG",request,response)+"</p>";
    } catch (Exception e ) {
        msg="<p> Error: "+e.getMessage()+"</p>";
    }
    %>
  </mm:compare>
</mm:present><%-- /cmd --%>

<mm:present referid="action">
  <mm:compare referid="action" value="alter">
    <h3>Administrate Module ${module}, Property ${property}</h3>

    <%
    //Module mmAdmin=ContextProvider.getDefaultCloudContext().getModule("mmadmin");
    String val = mmAdmin.getInfo("GETMODULEPROPERTY-"+module+"-"+property,request,response);
    %>
    <mm:link page="modules-actions" referids="module,property"><form action="${_}" method="post"></mm:link>
    <table summary="module property data" border="0" cellspacing="0" cellpadding="3">
    <tr>
      <th>Property</th>
      <th>Value</th>
      <th class="center">Change</th>
    </tr>
    <tr>
      <td><mm:escape><%= property %></mm:escape></td>
      <td><input type="text" name="value" size="62" value="<mm:escape><%= val %></mm:escape>" /></td>
      <td class="center">
        <input type="hidden" name="cmd" value="MODULE-SETPROPERTY" />
        <input type="image" src="<mm:url page="/mmbase/style/images/edit.png" />" alt="Change" />
      </td>
    </tr>
    </table>
    </form>
  </mm:compare><%-- /action = alter --%>

</mm:present><%-- /action --%>

<mm:notpresent referid="action">
  <h3>Administrate Module ${module}</h3>
  <table summary="module actions" border="0" cellspacing="0" cellpadding="3">
  <caption>
    Description of <%= module %><br />
    <%= mmAdmin.getInfo("MODULEDESCRIPTION-"+module,request,response) %>
    <%= msg %>
  </caption>
  <tr>
    <th>Setting</th>
    <th>Value</th>
    <th class="center">Change</th>
  </tr><tr>
    <td>Class</td>
    <td><%= mmAdmin.getInfo("MODULECLASSFILE-"+module,request,response) %></td>
    <td class="center">Not Available</td>
  </tr><tr>
    <th>Property</th>
    <th>Value</th>
    <th class="center">Change</th>
</tr>
<%
   java.util.Map params = new java.util.Hashtable();
   params.put("CLOUD", cloud);
    NodeList props=mmAdmin.getList("MODULEPROPERTIES-"+module, params,request,response);
    for (int i=0; i<props.size(); i++) {
        Node prop=props.getNode(i);
%>
<tr>
   <td><mm:escape><%= prop.getStringValue("item1") %></mm:escape></td>
   <td><mm:escape><%= prop.getStringValue("item2") %></mm:escape></td>
   <td class="center">
     <mm:link page="modules-actions" referids="module">
       <mm:param name="property"><%= prop.getStringValue("item1") %></mm:param>
       <mm:param name="action">alter</mm:param>
       <a href="${_}"><img src="<mm:url page="/mmbase/style/images/edit.png" />" alt="change" width="21" height="20" /></a>
     </mm:link>
   </td>
  </tr>
  <%
  }
  %>
  </table>

  <mm:link page="modules-actions" referids="module"><form action="${_}" method="post"></mm:link>
  <table border="0" cellspacing="0" cellpadding="3">
  <caption>
    Save configuration of <%= module %>
  </caption>
  <tr>
    <th>Action</th>
    <th>Path</th>
    <th class="center">Confirm</th>
  </tr><tr>
   <td>Save</td>
   <td><input name="path" value="/tmp/${module}.xml" size="62" /></td>
   <td class="center">
     <input type="hidden" name="cmd" value="MODULESAVE" />
     <input type="image" src="<mm:url page="/mmbase/style/images/ok.png" />" alt="OK" width="21" height="20" />
   </td>
  </tr>
  </table>
  </form>
</mm:notpresent>

<p>
  <mm:link page="modules">
    <a href="${_}"><img src="<mm:url page="/mmbase/style/images/back.png" />" alt="back" width="21" height="20" /></a>
    <a href="${_}">Return to Module Overview</a>
  </mm:link>
</p>

</div>
</mm:cloud>
</mm:content>
