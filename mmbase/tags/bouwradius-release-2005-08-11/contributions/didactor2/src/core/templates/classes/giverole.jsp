<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<mm:cloud jspvar="cloud">
<mm:node number="studentrole" id="studentrole" />
<mm:listnodes type="people" id="student">
   <mm:remove referid="hasrole" />
   <mm:relatednodes type="roles" max="1"><mm:import id="hasrole" /></mm:relatednodes>
   <mm:notpresent referid="hasrole">
      <mm:createrelation source="student" destination="studentrole" role="related" />
   </mm:notpresent>
</mm:listnodes>
</mm:cloud>

