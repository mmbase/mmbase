<mm:write referid="startseconds"/> -
<mm:write referid="endseconds"/>

<mm:list nodes="$agenda" path="agendas,eventrel,items" constraints="eventrel.stop > $startseconds AND eventrel.start < $endseconds">
     <mm:import jspvar="itemNumber"><mm:field name="items.number"/></mm:import>
       <%
          linkedlist.add( itemNumber );
      %>
 </mm:list>

