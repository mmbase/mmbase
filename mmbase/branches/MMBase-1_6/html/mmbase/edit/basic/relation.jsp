<%--
 Create tr-'s representing the relations
--%>
 <% RelationManagerIterator relIterator = node.getNodeManager().getAllowedRelations((NodeManager) null, null, source ? "destination" : "source").relationManagerIterator(); 
   while(relIterator.hasNext()) {
      RelationManager relationManager = relIterator.nextRelationManager();
      NodeManager otherManager =  source ? relationManager.getDestinationManager() : relationManager.getSourceManager();
      String      role         =  source ? relationManager.getForwardRole()        : relationManager.getReciprocalRole();
      %>

<mm:context>
<tr>
    <td class="data"> 
        <%=otherManager.getGUIName()%> (<%=role%>)
    </td>
    <th colspan="3"><%=m.getString("relations.relations")%></th>
    <th colspan="3"><%=m.getString("relations.related")%></th>
    <td class="navigate">
        <%-- <%= m.getString("new_relation.new")%> --%>
        <a href='<mm:url page="new_relation.jsp" >
            <mm:param name="node"><mm:field node="this_node" name="number" /></mm:param>
            <mm:param name="node_type"><%= otherManager.getName()%></mm:param>
            <mm:param name="role_name"><%= role %></mm:param>
            <mm:param name="direction"><%= source ? "create_child" : "create_parent" %></mm:param>
            </mm:url>'>
           <span class="create"></span><span class="alt">+</span>
       </a>
    </td>
</tr>

<% RelationIterator relationIterator = node.getRelations(role, otherManager).relationIterator();
   while(relationIterator.hasNext()) {
       Relation relation = relationIterator.nextRelation();
       Node otherNode = source ? relation.getDestination() : relation.getSource();
       // only show on actual type, so, not on parents
       // Not sure that this is what we want
       if (otherNode.getNodeManager().equals(otherManager)) {
%>
<tr>
    <%-- skip first field --%>
    <td>&nbsp;</td>
    <td class="data">
        #<%=relation.getNumber()%>
    </td>
    <td class="data">
        <%= relation.getFunctionValue("gui", null).toString() %>
    </td>
    <td class="navigate">
        <%-- delete the relation node, not sure about the node_type argument! --%>
        <a href='<mm:url page="commit_node.jsp" referids="backpage_cancel,backpage_ok">
            <mm:param name="node_number"><%=relation.getNumber() %></mm:param>
            <mm:param name="node_type"><%=  relationManager.getName() %></mm:param>
            <mm:param name="delete">true</mm:param>
            </mm:url>' >
          <span class="delete"></span><span class="alt">x</span>
        </a>

        <%-- edit the relation --%>
        <a href='<mm:url page="change_node.jsp" referids="backpage_cancel,backpage_ok">
            <mm:param name="node_number"><%=relation.getNumber()%></mm:param>
            </mm:url>' >
          <span class="select"></span><span class="alt">-&gt;</span>
        </a>
    </td>
    <td class="data">
        #<%=otherNode.getNumber()%>
    </td>
    <td class="data">
        <%= otherNode.getFunctionValue("gui", null).toString() %>
    </td>
    <td class="navigate">
        <%-- edit the related node --%>
        <a href='<mm:url page="change_node.jsp" referids="backpage_cancel,backpage_ok">
            <mm:param name="node_number"><%=otherNode.getNumber()%></mm:param>
              </mm:url>'>
          <span class="select"></span><span class="alt">-&gt;</span>
        </a>
    </td>
    <%-- skip last field --%>
    <td>&nbsp;</td>
</tr>
<% } // if
  } // while
%>
</mm:context>
<%
} // while
%>

