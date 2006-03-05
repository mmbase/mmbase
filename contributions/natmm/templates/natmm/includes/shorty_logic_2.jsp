<%
validLink = false;
altTXT = "";
readmoreURL = "";
readmoreTarget = "";

String readmoreTXT = "";
String readmoreID = ""; 
%>
<mm:node number="<%= shortyID[i]  %>">
   <mm:related path="readmore,contentelement" fields="contentelement.number,readmore.readmore" max="1">
   	<mm:field name="readmore.readmore" write="false" jspvar="readmore" vartype="String">
   	<mm:field name="contentelement.titel" write="false" jspvar="contentelement_titel" vartype="String">
   	<mm:field name="contentelement.number" write="false" jspvar="contentelement_number" vartype="String">
   		<% 
   		if(!readmore.equals("")){ 
   		   readmoreTXT = readmore.replaceAll(">>",">");
            readmoreTXT = readmoreTXT.substring(0,1).toUpperCase() + readmoreTXT.substring(1);
   		}
   		altTXT = contentelement_titel;
   		readmoreID = contentelement_number;
   		%>
   	</mm:field>
   	</mm:field>
   	</mm:field>
   </mm:related>
</mm:node>
<%
if(!readmoreID.equals("")){ 
   // treatment for all object types: 
   // - link: special URL
   // - artikel: link to this page with article id
   // - evenement: not related to a page 
   // - natuurgebied: link to natuurgebieden template with n parameter 
   // - provincies: link to natuurgebieden template with prov parameter
   // - pagina: link to the paginatemplate related to this page
   %>
	<mm:node number="<%= readmoreID %>">
		<mm:nodeinfo type="type" write="false" jspvar="nType" vartype="String">
		<% if(nType.equals("link")){ %>
			<mm:field name="url" write="false" jspvar="link_url" vartype="String">
			<mm:field name="titel" write="false" jspvar="link_titel" vartype="String">
			<mm:field name="target" write="false" jspvar="link_target" vartype="String">
			<mm:field name="alt_tekst" write="false" jspvar="alt_tekst" vartype="String">
			<%
			   if(!alt_tekst.equals("")){ 
				   altTXT = alt_tekst;
				} else {
				   altTXT = link_titel;
				}
				readmoreURL = link_url; 
				readmoreTarget = link_target;
			%>
			</mm:field>
			</mm:field>
			</mm:field>
			</mm:field>
		<%	
		} else if(nType.equals("artikel")){
         // find out whether the readmore artikel is linked to a page
         String targetPage = "";
         %>
		   <mm:related path="contentrel,pagina" fields="pagina.number"  max="1">
	      <mm:field name="pagina.number" write="false" jspvar="pagina_number" vartype="String">
	      <% 
	         targetPage = pagina_number;
	      %>
	      </mm:field>
	      </mm:related>
         <%
         if(targetPage.equals("")) {
            %>
            <mm:related path="posrel,dossier,posrel,pagina" fields="pagina.number"  max="1">
   	      <mm:field name="pagina.number" write="false" jspvar="pagina_number" vartype="String">
   	      <% 
   	         targetPage = pagina_number;
   	      %>
   	      </mm:field>
   	      </mm:related>
            <%
         }
         if(!targetPage.equals("")) {
            %>
    	      <mm:list nodes="<%= targetPage %>" path="pagina,gebruikt,paginatemplate" fields="paginatemplate.url"  max="1">
   	      <mm:field name="paginatemplate.url" write="false" jspvar="template_url" vartype="String">
   	      <% 
   	         readmoreURL = template_url + "?p=" + targetPage + "&a="+readmoreID;
   	      %>
   	      </mm:field>
   	      </mm:list>
	         <%
        } else { 
            // artikel is not linked to a page show it in the present page (but with artikel template)
            readmoreURL = "artikel.jsp?p=" + paginaID + "&a="+readmoreID;
        }
		} else if(nType.equals("evenement")){ 
		      readmoreURL = "events.jsp?p=agenda&id="+readmoreID;
		} else if(nType.equals("natuurgebieden")){ 
		      readmoreURL = "natuurgebieden.jsp?n="+readmoreID; 
		} else if(nType.equals("provincies")){ 
		      readmoreURL = "natuurgebieden.jsp?prov="+readmoreID;
		} else if(nType.equals("pagina")){ 
		   %>
		   <mm:related path="gebruikt,paginatemplate" fields="paginatemplate.url"  max="1">
	      <mm:field name="paginatemplate.url" write="false" jspvar="template_url" vartype="String">
	      <% 
	         readmoreURL = template_url + "?p=" + readmoreID;
	      %>
	      </mm:field>
	      </mm:related>
	      <%
	   }						
		validLink = true;
		%>
		</mm:nodeinfo>
	</mm:node>
   <%
}

%>
