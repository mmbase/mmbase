<div class="group">
<div class="environment">
 <p class="current">
   <%=m.getString("current_group")%>: <mm:nodeinfo type="gui" />
 </p>
</div>

<div class="users">
   <mm:relatednodes id="user" searchdir="destination" role="contains" type="mmbaseusers">
     <mm:first>Users: </mm:first>
     <a href="<mm:url page="edit_user.jsp" referids="user" />"><mm:nodeinfo type="gui" /></a>
     <mm:last inverse="true">, </mm:last>
   </mm:relatednodes>
</div>


<div class="rights">
   <form method="post" action="<mm:url referids="parameters,$parameters"><mm:param name="url">commit_group.jsp</mm:param></mm:url>">    
   <input type="hidden" name="group" value="<mm:field name="number" />" />
   <table>
    <mm:fieldlist type="edit">
      <mm:maywrite>
       <tr><td><mm:fieldinfo type="guiname" /></td><td colspan="3"><mm:fieldinfo type="input" /></td></tr>
      </mm:maywrite>
      <mm:maywrite inverse="true">
       <tr><td><mm:fieldinfo type="guiname" /></td><td colspan="3"><mm:fieldinfo type="value" /></td></tr>
      </mm:maywrite>
      </mm:fieldlist>

    <tr>
     <td><%=m.getString("parent_groups")%></td>
     <td>
      <select name="_parentgroups"  size="15" multiple="multiple">
        <mm:relatednodes id="ingroups" type="mmbasegroups" searchdir="source">
         <option selected="selected" value="<mm:field name="number" />"><mm:nodeinfo type="gui" /></option>       
        </mm:relatednodes>
        <mm:unrelatednodes type="mmbasegroups" searchdir="both" excludeself="true">
         <option value="<mm:field name="number" />"><mm:nodeinfo type="gui" /></option>
        </mm:unrelatednodes>
      </select>
      <a href="<mm:url referids="parameters,$parameters"><mm:param name="url">index_groups.jsp</mm:param>
      <mm:relatednodes referid="ingroups">
        <mm:param name="group"><mm:field name="number" /></mm:param>
       </mm:relatednodes>
      </mm:url>"><%=m.getString("view_groups")%></a>
     </td>
     <td><%=m.getString("child_groups")%></td>
     <td>
      <select name="_childgroups"  size="15" multiple="multiple">
        <mm:relatednodes id="fromgroups" type="mmbasegroups" searchdir="destination">
         <option selected="selected" value="<mm:field name="number" />"><mm:nodeinfo type="gui" /></option>       
        </mm:relatednodes>
        <mm:unrelatednodes type="mmbasegroups" searchdir="both" excludeself="true">
         <option value="<mm:field name="number" />"><mm:nodeinfo type="gui" /></option>
        </mm:unrelatednodes>
      </select>
      <a href="<mm:url referids="parameters,$parameters"><mm:param name="url">index_groups.jsp</mm:param>
      <mm:relatednodes referid="fromgroups">
        <mm:param name="group"><mm:field name="number" /></mm:param>
       </mm:relatednodes>
      </mm:url>"><%=m.getString("view_groups")%></a>
     </td>
     </tr>
   </table>
   <mm:import id="back">index_groups.jsp</mm:import>
   <%@include file="groupOrUserRights.table.jsp" %>
   </form>
</div>


</div>