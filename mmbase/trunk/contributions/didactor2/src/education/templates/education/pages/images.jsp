<div class="images">
   <mm:compare referid="imagelayout" value="0">
   <table border="0" class="Font">
   <tr>
   <mm:related path="sizerel,images" orderby="sizerel.pos">
   <mm:import id="imwidth" reset="true"><mm:field name="sizerel.width"/></mm:import>
   <mm:import id="imheight" reset="true"><mm:field name="sizerel.height"/></mm:import>
   <mm:import id="imnum" reset="true"><mm:field name="images.number"/></mm:import>
   <mm:node number="$imnum">
   
      <td>
      <mm:field name="title"/>
      <br/>
       <mm:isgreaterthan referid="imwidth" value="0">
       <mm:isgreaterthan referid="imheight" value="0">
       <mm:import id="template" reset="true">s(<mm:write referid="imwidth"/>x<mm:write referid="imheight"/>)</mm:import>
        <img src="<mm:image template="$template"/>" border="0"/>
      </mm:isgreaterthan>
      </mm:isgreaterthan>
      <mm:islessthan referid="imwidth" value="1">
          <img src="<mm:image/>" border="0"/>
      </mm:islessthan>
       
        <br clear="all"/>
        
      <mm:field name="description"/>
     </td>
     </mm:node>
    </mm:related>
    </tr>
    </table>
    </mm:compare>
    <mm:compare referid="imagelayout" value="1">

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
        <img src="<mm:image template="$template"/>" border="0"/>
      </mm:isgreaterthan>
      </mm:isgreaterthan>
      <mm:islessthan referid="imwidth" value="1">
          <img src="<mm:image/>" border="0"/>
      </mm:islessthan>
       
        <br clear="all"/>
        
      <mm:field name="description"/>
      <br clear="all"/>
     </mm:node>
    </mm:related>
    </mm:compare>
 </div>

