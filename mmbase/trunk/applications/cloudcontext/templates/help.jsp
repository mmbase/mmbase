<%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0"   prefix="mm"
%><%@page language="java" contentType="text/html; charset=UTF-8"
%><mm:content postprocessor="reducespace">
<%@include file="import.jsp" %><%@include file="settings.jsp" %>
<mm:import id="url">help.jsp</mm:import>
<mm:cloud method="loginpage" loginpage="login.jsp" jspvar="cloud" rank="$rank">
 <%@include file="you.div.jsp" %>
 <mm:import id="current">help</mm:import>
 <%@include file="navigate.div.jsp" %>

  <div class="help">
  <p>
  
  </p>
  <h2>Available rights</h2>
  <p>
   Rights are distributed to 'security contexts'. Every object has a certain security context. The following rights types are distinguished.
  </p>
  <table class="rights">
   <mm:import id="operations" vartype="list">create,read,write,delete,change context</mm:import>
    <tr><mm:stringlist referid="operations"><th><mm:write /></th></mm:stringlist></tr>
    <tr>
    <td class="text">'Create' rights are only relevant to nodes of the type 'object type'. If a user has 'create' rights on a certain 'object type' node, then she may create nodes of that type.</td>
    <td class="text">Whether nodes with this context may be read by user/users from group.</td>
    <td class="text">Whether nodes with this context may be changed by user/users from group.</td>
    <td class="text">Whether nodes with this context may be deleted by user/users from group.</td>
    <td class="text">To every node a 'context' string is assocatiated, and to these 'security contexts' rights are attributed. If you have the 'change context' right to a node, then you may change the security context</td>
  </table>

  <h2>Status of a right</h2>
  <p>
    Rights can be attributes to groups and to users. Users can be in zero or more groups, and groups
    can be in zero or more groups. If a user or or group is in this way 'contained' by a group, then
    this other group is called the 'parent' (there can be zero or more parents). A parent passes all
    rights to its 'children'. Green boxes indicate rights a certain user or group has because of is
    membership of a parent group. Such a rights cannot be revoked directly (should be done on a
    editor for the 'parent' group, or the user/group should be taken out of this parent group). 
  </p>
  <table class="rights">
    <tr><td><input type="checkbox" /></td><td class="text">Disallowed, you can grant</td></tr>
    <tr><td><input type="checkbox" checked="checked" /></td><td class="text">Allowed, you can revoke</td></tr>
    <tr><td class="parent"><input type="checkbox" /></td><td class="text">Allowed by parent group, you can grant (if revoked on parent, right is gone, unless you grant here)</td></tr>
    <tr><td class="parent"><input type="checkbox" checked="checked" /></td><td class="text">Allowed by parent group and here (you can revoke, but right remains because of parent)</td></tr>
    <tr><td></td><td class="text">Disallowed, you may not grant</td></tr>
    <tr><td>X</td><td class="text">Allowed, you may not revoke</td></tr>
    <tr><td class="parent"></td><td class="text">Allowed by parents, you may not grant</td></tr>
    <tr><td class="parent">X</td><td class="text">Allowed by parents and self, you may not revoke</td></tr>
  </table>

</div>
</mm:cloud>
</mm:content>