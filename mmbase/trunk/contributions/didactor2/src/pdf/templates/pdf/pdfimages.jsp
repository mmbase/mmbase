<%    String baseUrl = getServletContext().getInitParameter("internalUrl");

    if (baseUrl == null) {
        throw new ServletException("Please set 'internalUrl' in the web.xml!");
    }
%>

<mm:compare referid="imagelayout" value="0">
   <mm:related path="sizerel,images" orderby="sizerel.pos">
   <mm:import id="imwidth" reset="true"><mm:field name="sizerel.width"/></mm:import>
   <mm:import id="imheight" reset="true"><mm:field name="sizerel.height"/></mm:import>
   <mm:import id="imnum" reset="true"><mm:field name="images.number"/></mm:import>
   <mm:node number="$imnum">
   
      <td width="100%">
      <mm:field name="title"/>
      <br/>
       <mm:isgreaterthan referid="imwidth" value="0">
          <mm:isgreaterthan referid="imheight" value="0">
            <mm:import id="template" reset="true">s(<mm:write referid="imwidth"/>x<mm:write referid="imheight"/>)</mm:import>
            <mm:import jspvar="imageUrl" reset="true"><mm:image template="$template"/></mm:import>
            <% imageUrl = baseUrl + imageUrl.substring(imageUrl.indexOf("/img.db")); %>
            <img src="<%= imageUrl %>" border="0"/>
 
          </mm:isgreaterthan>
       </mm:isgreaterthan>
       <mm:islessthan referid="imwidth" value="1">
          <mm:import jspvar="imageUrl" reset="true"><mm:image/></mm:import>
          <% imageUrl = baseUrl + imageUrl.substring(imageUrl.indexOf("/img.db")); %>
          <img src="<%= imageUrl %>" border="0"/>
 
      </mm:islessthan>

      
        <br clear="all"/>
        
      <mm:field name="description"/>
     </td>
     </mm:node>
    </mm:related>
    </mm:compare>
    
    <mm:compare referid="imagelayout" value="1">
    <td width="100%">
   <mm:related path="sizerel,images" orderby="sizerel.pos">
   <mm:import id="imwidth" reset="true"><mm:field name="sizerel.width"/></mm:import>
   <mm:import id="imheight" reset="true"><mm:field name="sizerel.height"/></mm:import>
   <mm:import id="imnum" reset="true"><mm:field name="images.number"/></mm:import>
   <mm:node number="$imnum">
   
      <mm:field name="title"/>
      <br/>
       <mm:isgreaterthan referid="imwidth" value="0">
       <mm:isgreaterthan referid="imheight" value="0">
       <mm:import id="template" reset="true">s(<mm:write referid="imwidth"/>x<mm:write referid="imheight"/>)</mm:import>
       <mm:import jspvar="imageUrl" reset="true"><mm:image template="$template"/></mm:import>
            <% imageUrl = baseUrl + imageUrl.substring(imageUrl.indexOf("/img.db")); %>
            <img src="<%= imageUrl %>" border="0"/>
 
      </mm:isgreaterthan>
      </mm:isgreaterthan>
      <mm:islessthan referid="imwidth" value="1">
       <mm:import jspvar="imageUrl" reset="true"><mm:image/></mm:import>
            <% imageUrl = baseUrl + imageUrl.substring(imageUrl.indexOf("/img.db")); %>
            <img src="<%= imageUrl %>" border="0"/>
 
      </mm:islessthan>
             
       
        <br clear="all"/>
        
      <mm:field name="description"/>
        <br clear="all"/>
     </mm:node>
    </mm:related>
    </td>
    </mm:compare>

