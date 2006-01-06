<%--
  This template deletes a existing folder.
--%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><di:translate key="portfolio.deletefolder" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="currentfolder"/>
<mm:import externid="callerpage"/>
<mm:import externid="typeof"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>

<mm:import externid="contact">-1</mm:import>
<mm:compare referid="contact" value="-1" inverse="true">
  <mm:import id="user" reset="true"><mm:write referid="contact"/></mm:import>
</mm:compare>

<%-- Check if the yes button is pressed --%>
<mm:present referid="action1">
  <mm:import id="action1text"><di:translate key="portfolio.deleteyes" /></mm:import>
  <mm:compare referid="action1" referid2="action1text">

    <%-- Retrieve the currentfolder, delete the node with relations and delete folderitems --%>
    <mm:node number="$currentfolder">
      <mm:relatednodes type="attachments">
        <mm:deletenode deleterelations="true"/>
      </mm:relatednodes>
      <mm:relatednodes type="urls">
        <mm:deletenode deleterelations="true"/>
      </mm:relatednodes>
      <mm:relatednodes type="pages">
        <mm:deletenode deleterelations="true"/>
      </mm:relatednodes>
      <mm:relatednodes type="chatlogs">
        <mm:deletenode deleterelations="true"/>
      </mm:relatednodes>
      <mm:deletenode deleterelations="true"/>
    </mm:node>

    <%-- Remove the reference to the currentfolder --%>
    <mm:remove referid="currentfolder"/>
    <mm:import id="currentfolder">-1</mm:import>

    <mm:redirect referids="$referids,currentfolder,typeof,contact?" page="$callerpage"/>

  </mm:compare>
</mm:present>


<%-- Check if the no button is pressed --%>
<mm:present referid="action2">
  <mm:import id="action2text"><di:translate key="portfolio.deleteno" /></mm:import>
  <mm:compare referid="action2" referid2="action2text">
    <mm:redirect referids="$referids,currentfolder,typeof,contact?" page="$callerpage"/>
  </mm:compare>
</mm:present>

<div class="rows">

<div class="navigationbar">
<div class="titlebar">
<img src="<mm:treefile write="true" page="/gfx/icon_shareddocs.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" title="<di:translate key="portfolio.portfolio" />" alt="<di:translate key="portfolio.portfolio" />"/>
<di:translate key="portfolio.portfolio" />
</div>
</div>

<div class="folders">

<div class="folderHeader">
<di:translate key="portfolio.portfolio" />
</div>
<div class="folderBody"></div>
</div>


<div class="mainContent">

  <div class="contentHeader">
  	<di:translate key="portfolio.deletefolder" />
  </div>

  <div class="contentBodywit">

    <%-- Show the form --%>
    <form name="deletefolder" method="post" action="<mm:treefile page="/portfolio/deletefolder.jsp" objectlist="$includePath" referids="$referids"/>">

      <di:translate key="portfolio.deletethisfolderyesno" />
      <p/>

      <table class="Font">
      <mm:node referid="currentfolder">
        <mm:fieldlist fields="name">
          <tr>
          <td><mm:fieldinfo type="guiname"/></td>
          <td><mm:fieldinfo type="value"/></td>
          </tr>
        </mm:fieldlist>
      </mm:node>
      </table>
      <p/>
      <input type="hidden" name="currentfolder" value="<mm:write referid="currentfolder"/>"/>
      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input type="hidden" name="typeof" value="<mm:write referid="typeof"/>"/>
      <mm:compare referid="contact" value="-1" inverse="true">
        <input type="hidden" name="contact" value="<mm:write referid="contact"/>"/>
      </mm:compare>
      <input class="formbutton" type="submit" name="action1" value="<di:translate key="portfolio.deleteyes" />" />
      <input class="formbutton" type="submit" name="action2" value="<di:translate key="portfolio.deleteno" />" />
    </form>
  </div>
</div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />
</mm:cloud>
</mm:content>
