<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@page language="java" contentType="text/html;charset=UTF-8"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm"
%><?xml version="1.0" encoding="UTF-8"?>
<html>
<head>
   <title>EditWizard Examples</title>
   <!--
    More complicated example.

    @since    MMBase-1.6
    @author   Michiel Meeuwissen
    @version  $Id: index.jsp,v 1.19 2002-09-24 10:35:35 michiel Exp $

    Showing:
          - use of taglib in this entrance page
          - xml-definitions in subdir of entrance page
          - javascript for search action
          - jump to create directly
    -->

   <link rel="stylesheet" type="text/css" href="../style.css" />
    <script language="javascript"><!--
          function openListImages(el) {
          var href = el.getAttribute("href");
          var zoek = document.forms["searchimage"].elements["imagedesc"].value.toUpperCase();
          if (zoek != '') {
          href += "&constraints=UCASE%28description%29%20LIKE%20%27%25" + zoek + "%25%27%20or%20UCASE%28title%29%20LIKE%20%27%25" + zoek + "%25%27";
          }
          document.location = href;
          return false;
}
--></script>
   <link href="style.css" rel="stylesheet" type="text/css" >
    <!-- will also be used in list, not in wizard, but that would be possible ananlogously. -->
</head>
<body>
   <!-- We are going to set the referrer explicitely, because we don't wont to depend on the 'Referer' header (which is not mandatory) -->
  <mm:import id="referrer"><%=new  java.io.File(request.getServletPath())%></mm:import>
  <mm:import id="templates">/templates</mm:import><!-- unused now -->
  <mm:import id="jsps">/mmapps/editwizard/jsp/</mm:import>
  <mm:import id="pagelength">10</mm:import>
        <h1>Editwizard Examples</h1>
  <p>
   This example overrides some XSL's the editwizard bij placing
   variants in xsl/ relative to this file. It addes a stylesheet by
   overriding base.xsl.  Furthermore it uses it's private XML
   editwizard definitions, which are also placed relativily to this
   file in the 'tasks' directory.
  </p>
  <p>
  <td>
     <a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/xsl/list.xsl</mm:param></mm:url>">view xsl/list.xsl</a>
     <a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/xsl/wizard.xsl</mm:param></mm:url>">view xsl/wizard.xsl</a>
     <a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/xsl/base.xsl</mm:param></mm:url>">view xsl/base.xsl</a>
  </td>
  </p>
  <!-- check if the MyNews application was installed -->
  <mm:cloud method="http">
  <mm:listnodes type="versions" constraints="[type] LIKE '%application%' AND [name] LIKE '%MyNews%'">
      <mm:first><mm:import id="mynews_installed">true</mm:import></mm:first>
  </mm:listnodes>
  </mm:cloud>
        <br />
  <!-- Yes, installed, show the editwizard entry page -->
  <mm:present referid="mynews_installed">

  <table>    
   <tr><td>          
        <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">           
           <mm:param name="wizard">tasks/people</mm:param>
           <mm:param name="nodepath">people</mm:param>
           <mm:param name="fields">number,firstname,lastname</mm:param>
           <mm:param name="orderby">number</mm:param>
           <mm:param name="directions">down</mm:param>
           </mm:url>">Person test</a>
  <!-- show how to jump to wizard.jsp directly -->
  (<a href="<mm:url referids="referrer,pagelength" page="${jsps}wizard.jsp">
            <mm:param name="wizard">tasks/people</mm:param>
            <mm:param name="objectnumber">new</mm:param>
            </mm:url>">Create</a>)
  </td><td>
     This is a '2 step' example. You can create/change the date for a
     person and relate a picture in the first step. In the second
         step then, you can relate articles to the person. We also
           demonstrate here how you can jump directly to the wizard to create a
           new person (without having to go to the list first).
  </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/samples/people.xml</mm:param></mm:url>">view XML</a></td>
  </tr>

   <tr><td>
        <form action="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp" />" method="post">
           <select name="searchfields">
               <option value="firstname">First name</option>
               <option value="lastname">Last name</option>
               <option value="firstname,lastname">First or last name</option>
           </select>
           <input type="text" name="searchvalue" />
           <input type="submit" value="OK" />
           <input type="hidden" name="wizard" value="tasks/people" />
           <input type="hidden" name="nodepath" value="people" />
           <input type="hidden" name="fields" value="number,firstname,lastname" />
           <input type="hidden" name="orderby" value="number" />
           <input type="hidden" name="directions" value="down" />
        </form>
  </td><td>
     This is the same example, however this version first uses a simple search form to search for
     any persons whose name matches the given search term.
  </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/tasks/people.xml</mm:param></mm:url>">view XML</a></td>
  </tr>

  <tr><td>
   <form id="searchimage">
   <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">
           <mm:param name="wizard">tasks/imageupload</mm:param>
           <mm:param name="nodepath">images</mm:param>
           <mm:param name="fields">title,owner</mm:param>
           <mm:param name="orderby">title</mm:param>
           </mm:url>"
           onClick="return openListImages(this);">
           Images</a> (search:  <input type="text" name="imagedesc"	value="" style="width:200px;text-align:left;" />)
   </form>
   </td><td>
    A very simple image uploader. We show here how you could add
   search criteria. We also see that the delete prompt is overridden.
    </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/tasks/imageupload.xml</mm:param></mm:url>">view XML</a></td>
 </tr>
<tr><td>
   <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">
           <mm:param name="wizard">tasks/attachments</mm:param>
           <mm:param name="nodepath">attachments</mm:param>
           <mm:param name="fields">title</mm:param>
           <mm:param name="orderby">title</mm:param>
           </mm:url>" >
           Attachments</a>
   </td><td>
 Use the editwizards to upload and download attachments e.g. PDF files.
    </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/tasks/attachments.xml</mm:param></mm:url>">\
view XML</a></td>
 </tr>
    <tr><td>
    <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">
        	 <mm:param name="wizard">tasks/news</mm:param>
           <mm:param name="nodepath">news</mm:param>
           <mm:param name="fields">number,title</mm:param>
           <mm:param name="orderby">number</mm:param>
           <mm:param name="directions">down</mm:param>
           </mm:url>">News</a>
     </td><td>
       <ul>
         <li> How to use editwizards 'libs'. These are pieces
      of XML stored in the editwizard data directory which you can
      include in you own wizards</li>
       <li>'subwizards'</li>
       <li>fieldset</li>
        </ul>

     </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/tasks/news.xml</mm:param></mm:url>">view XML</a></td>
   </tr>
    <tr><td>
    <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">
		       <mm:param name="title">MyNews Magazine news</mm:param>
		       <mm:param name="startnodes">default.mags</mm:param>
        	 <mm:param name="wizard">tasks/news</mm:param>
           <mm:param name="nodepath">mags,news</mm:param>
           <mm:param name="fields">news.number,news.title</mm:param>
           <mm:param name="orderby">news.number</mm:param>
           <mm:param name="directions">down</mm:param>
           </mm:url>">News</a>
     </td><td>
        Only list news of default magazine (MyNews magazine).
     </td></tr>
    <tr><td>
    <a href="<mm:url referids="referrer,pagelength" page="${jsps}list.jsp">
        	 <mm:param name="wizard">tasks/mags</mm:param>
           <mm:param name="nodepath">mags</mm:param>
           <mm:param name="fields">number,title</mm:param>
           <mm:param name="orderby">number</mm:param>
           <mm:param name="directions">down</mm:param>
           </mm:url>">Magazines</a>
     </td><td>
       Demonstrated is how to use 'posrel', and how to create 'optionlists'.
     </td>
  <td><a target="_new" href="<mm:url page="../citexml.jsp"><mm:param name="page">advanced/tasks/mags.xml</mm:param></mm:url>">view XML</a></td>
      </tr>
     </table>

   </mm:present>

   <!-- MyNews applications was not installed, perhaps builders are missing and so on. Give warning. -->
   <mm:notpresent referid="mynews_installed">
   <h1>The 'MyNews' application was not deployed. Please deploy it before using this example.</h1>
   </mm:notpresent>

<hr />
   <a href="<mm:url page="../../taglib/showanypage.jsp"><mm:param name="page"><%=request.getServletPath()%></mm:param></mm:url>">Source of this page</a><br />
<a href="<mm:url page="../index.html" />">back</a>

</body>
</html>
