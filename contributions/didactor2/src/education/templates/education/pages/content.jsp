<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di"
%><mm:content postprocessor="reducespace">
<mm:cloud rank="didactor user" >

  <mm:import externid="learnobject" required="true"/>

  <mm:treeinclude page="/education/storebookmarks.jsp" objectlist="$includePath" referids="$referids,learnobject">
    <mm:param name="learnobjecttype">pages</mm:param>
  </mm:treeinclude>

  <mm:import externid="fb_madetest"/>
  <mm:present referid="fb_madetest">
    <mm:node number="$fb_madetest" notfound="skip">
      <mm:relatednodes type="tests">
        <mm:import id="page">/education/tests/feedback.jsp</mm:import>
        <a href="<mm:treefile page="$page" objectlist="$includePath" referids="$referids">
          <mm:param name="tests"><mm:field name="number"/></mm:param>
          <mm:param name="madetest"><mm:write referid="fb_madetest"/></mm:param>
          </mm:treefile>"><di:translate key="education.backtotestresults" /></a><br/>
          <mm:remove referid="page"/>
        </mm:relatednodes>
      </mm:node>
   </mm:present>

   <mm:node number="$learnobject">
     <mm:hasfield name="layout">
       <mm:field id="layout" name="layout" write="false" />
     </mm:hasfield>
     <mm:hasfield name="layout" inverse="true">
       <mm:import id="layout">0</mm:import>
     </mm:hasfield>
     <mm:hasfield name="imagelayout">
       <mm:field id="imagelayout" name="imagelayout" write="false" />
     </mm:hasfield>
     <mm:hasfield name="imagelayout" inverse="true">
       <mm:import id="imagelayout"></mm:import>
     </mm:hasfield>

     <mm:import externid="suppresstitle"/>

     <mm:notpresent referid="suppresstitle">
       <mm:field name="showtitle">
         <mm:compare value="1">
           <h1> <mm:field name="name"/></h1>
         </mm:compare>
       </mm:field>
     </mm:notpresent>


     <mm:import jspvar="text" reset="true"><mm:hasfield name="text"><mm:field name="text" escape="none"/></mm:hasfield> <mm:hasfield name="intro"><mm:field name="intro" escape="none"/></mm:hasfield></mm:import>

     <table width="100%" border="0" class="Font layout${layout}">
       <mm:compare referid="layout" value="0">
         <tr>
           <td width="50%">
             <mm:write referid="text" escape="toxml" />
           </td>
         </tr>
         <tr><td><%@include file="images.jsp"%></td></tr>
       </mm:compare>

       <mm:compare referid="layout" value="1">
         <tr><td  width="50%"><%@include file="images.jsp"%></td></tr>
         <tr><td>
             <mm:write referid="text" escape="toxml" />
         </td></tr>
       </mm:compare>

       <mm:compare referid="layout" value="2">
         <tr>
           <td>
             <mm:write referid="text" escape="toxml" />
           </td>
         <td><%@include file="images.jsp"%></td></tr>
       </mm:compare>

       <mm:compare referid="layout" value="3">
         <tr><td><%@include file="images.jsp"%></td>
         <td><%@include file="/shared/cleanText.jsp"%></td></tr>
       </mm:compare>

     </table>



     <mm:relatednodes type="attachments" role="posrel" orderby="posrel.pos">
       <h3><mm:field name="title"/></h3>
       <p>
         <i><mm:field name="description" escape="inline"/></i><br>
         <a href="<mm:attachment/>"><img src="<mm:treefile page="/education/gfx/attachment.gif" objectlist="$includePath" />" border="0" title="Download <mm:field name="title"/>" alt="Download <mm:field name="title"/>"></a>
       </p>
     </mm:relatednodes>

     <div class="audiotapes">
       <mm:relatednodes type="audiotapes" role="posrel" orderby="posrel.pos">
         <h3><mm:field name="title"/></h3>
         <p>
           <i><mm:field name="subtitle"/></i>
         </p>
         <i><mm:field name="intro" escape="p"/></i>
         <p>
           <mm:field name="body" escape="inline"/><br>
           <a href="<mm:field name="url" />"><img src="<mm:treefile page="/education/gfx/audio.gif" objectlist="$includePath" />" border="0" title="Beluister <mm:field name="title" />" alt="Beluister <mm:field name="title" />"></a></b>
         </p>
       </mm:relatednodes>
     </div>

     <div class="videotapes">
       <mm:relatednodes type="videotapes" role="posrel" orderby="posrel.pos">
         <p>
           <h3><mm:field name="title"/></h3>
           <i><mm:field name="subtitle"/></i>
         </p>
         <i><mm:field name="intro" escape="p"/></i>
         <p>
           <mm:field name="body" escape="inline"/><br>
           <a href="<mm:field name="url" />"><img src="<mm:treefile page="/education/gfx/video.gif" objectlist="$includePath" />" border="0" title="Bekijk <mm:field name="title" />" alt="Bekijk <mm:field name="title" />"></a>

         </p>
       </mm:relatednodes>
     </div>

     <div class="urls">
       <mm:relatednodes type="urls" role="posrel" orderby="posrel.pos">
         <mm:field name="showtitle">
           <mm:compare value="1">
             <h3><mm:field name="name"/></h3>
           </mm:compare>
         </mm:field>
         <p>
           <i><mm:field name="description" escape="inline"/></i><br/>
           <a href="<mm:field name="url"/>" target="_blank"><mm:field name="url"/></a>
         </p>
       </mm:relatednodes>
     </div>
   </mm:node>
 </mm:cloud>

</mm:content>




