<%--
  This template shows a existing folderitem.
--%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="none" expires="0">
<!-- get cloud -->
<mm:cloud method="asis" jspvar="cloud">
<!-- get imports -->
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.workspace.WorkspaceMessageBundle">
<!-- get cockpit header -->
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><fmt:message key="OPENITEM" /></title>
  </mm:param>
</mm:treeinclude>
<!-- header done -->

<mm:import externid="contact"/>
<mm:import externid="currentitem"/>
<mm:import externid="currentfolder"/>
<mm:import externid="typeof"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>
<mm:notpresent referid="currentitem">
    <mm:import externid="contact" from="multipart" required="true"/>
    <mm:import externid="currentitem" from="multipart" required="true"/>
    <mm:import externid="currentfolder" from="multipart" required="true"/>
    <mm:import externid="typeof" from="multipart"/>
    <mm:import externid="action1" from="multipart"/>
    <mm:import externid="action2" from="multipart"/>


</mm:notpresent>
<!-- testing stuff -->
<mm:import id="mayread">false</mm:import>
<mm:isgreaterthan referid="user" value="0">
<mm:node number="$currentitem">
    <mm:relatednodes type="portfoliopermissions" max="1">
        <mm:field name="readrights">
            <mm:compare value="2">
                <mm:list nodes="$user" path="people1,classes,people2,portfolios,folders"  constraints="folders.number=$currentfolder" max="1">
                    <mm:import id="mayread" reset="true">true</mm:import>
                </mm:list>
            </mm:compare>
             <mm:compare value="3">
                <di:hasrole role="teacher">
                    <mm:list nodes="$user" path="people1,classes,people2,portfolios,folders" constraints="folders.number=$currentfolder" max="1">
                        <mm:import id="mayread" reset="true">true</mm:import>
                    </mm:list>
                </di:hasrole>
            </mm:compare>
            <mm:compare value="3">
                <mm:import id="mayread" reset="true">true</mm:import>
            </mm:compare>
            <mm:compare value="4">
                <mm:import id="mayread" reset="true">true</mm:import>
            </mm:compare>
        </mm:field>
    </mm:relatednodes>

        <mm:list nodes="$user" path="people,portfolios,folders" constraints="folders.number=$currentfolder" max="1">
            <mm:import id="mayread" reset="true">true</mm:import>
        </mm:list>

</mm:node>
</mm:isgreaterthan>

<mm:compare referid="user" value="0">
 <mm:node number="$currentitem">
    <mm:relatednodes type="portfoliopermissions" max="1">
        <mm:field name="readrights">
           <mm:compare value="4">
                <mm:import id="mayread" reset="true">true</mm:import>
            </mm:compare>
        </mm:field>
    </mm:relatednodes>
 </mm:node>
</mm:compare>

<%--<h1>mayread: <mm:write referid="mayread"/>, user: <mm:write referid="user"/></h1>--%>

<mm:compare referid="mayread" value="true">

<%-- Check if the back button is pressed --%>
<mm:present referid="action2">
    <mm:redirect referids="$referids,contact,currentfolder,typeof" page="index.jsp"/>
</mm:present>

<mm:notpresent referid="action2">




<%-- check if the use may edit this entry --%>
<mm:import id="mayeditthis">false</mm:import>

<%-- user may edit if he's a teacher of this portfolio's owner--%>
<mm:isgreaterthan referid="user" value="0">
    <di:hasrole role="teacher">
        <mm:list nodes="$user" path="people1,classes,people2,portfolios,folders" constraints="folders.number=$currentfolder" max="1">
             <mm:import id="mayeditthis" reset="true">true</mm:import>
        </mm:list>
    </di:hasrole>

    <%-- user may edit if he's the owner and this is not the assessment portfolio --%>
    <mm:list nodes="$user" path="people,portfolios,folders" constraints="folders.number=$currentfolder and portfolios.m_type != 1" max="1">
        <mm:import id="mayeditthis" reset="true">true</mm:import>
    </mm:list>

    <mm:compare referid="mayeditthis" value="true">
    <%-- edit the content of the object --%>
    <mm:node number="$currentitem">
    <mm:present referid="action1">
          <mm:fieldlist fields="title?,name?,description?,url?,text?,filename?,handle?">
            <mm:fieldinfo type="useinput" />
          </mm:fieldlist>
    </mm:present>

    <mm:import id="numrelations"><mm:countrelations type="portfoliopermissions"/></mm:import>
    <mm:compare referid="numrelations" value="0">
        <mm:createnode type="portfoliopermissions" id="permissions"/>
        <mm:createrelation source="currentitem" destination="permissions" role="related"/>
    </mm:compare>

    <mm:relatednodes type="portfoliopermissions" max="1">
        <mm:fieldlist fields="readrights,allowreactions">
            <mm:fieldinfo type="useinput" />
        </mm:fieldlist>
    </mm:relatednodes>

    </mm:node>
    </mm:compare>

</mm:isgreaterthan>


<mm:import externid="add_message"/>
<mm:present referid="add_message">
    <mm:import externid="title" reset="true"/>
    <mm:import externid="body" reset="true"/>
    <mm:isnotempty referid="title"><mm:isnotempty referid="body">
        <mm:createnode type="forummessages" id="newmessage">
            <mm:setfield name="title"><mm:write referid="title"/></mm:setfield>
            <mm:setfield name="body"><mm:write referid="body"/></mm:setfield>
            <mm:setfield name="date"><%= System.currentTimeMillis()/1000 %></mm:setfield>
        </mm:createnode>
        <mm:createrelation role="related" destination="currentitem" source="newmessage"/>
    </mm:isnotempty></mm:isnotempty>
</mm:present>

<div class="rows">


<div class="navigationbar">
  <div class="titlebar">
  <img src="<mm:treefile write="true" page="/gfx/icon_portfolio.gif" objectlist="$includePath" referids="$referids"/>" width="25" height="13" border="0" alt="<fmt:message key="MYDOCUMENTS" />" />
  <fmt:message key="MYDOCUMENTS" />
  </div>
</div>

<div class="folders">
  <div class="folderHeader">
  </div>
  <div class="folderBody">
  </div>
</div>

<div class="mainContent">

  <div class="contentHeader">
    <mm:node number="$currentitem">
      <fmt:message key="OPENITEM" />:
      <mm:import id="nodetype"><mm:nodeinfo type="type"/></mm:import>
      <mm:compare referid="nodetype" value="attachments">
        <fmt:message key="FOLDERITEMTYPEDOCUMENT" />
      </mm:compare>
      <mm:compare referid="nodetype" value="urls">
        <fmt:message key="FOLDERITEMTYPEURL" />
      </mm:compare>
      <mm:compare referid="nodetype" value="pages">
        <fmt:message key="FOLDERITEMTYPEPAGE" />
      </mm:compare>
      <mm:compare referid="nodetype" value="chatlogs">
        <fmt:message key="FOLDERITEMTYPECHATLOG" />
      </mm:compare>
    </mm:node>
  </div>

  <div class="contentBodywit">
  <br><br><br>
      <table class="Font">
    <%-- Show the form --%>
    <%-- 
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
        DO NOT USE the referid="..." construct in the form's 
        action="<treefile...>"  construct here. - MMBase gets 
        completely confused if it's more than a couple of bytes long and
        you're trying to post a multipart/form-data encoded form

        If you need to pass parameters use hidden fields, as below
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    --%>
    <form name="showitem" method="post" action="<mm:treefile page="/portfolio/showitem.jsp" objectlist="$includePath"/>" enctype="multipart/form-data">
    <mm:present referid="provider">
        <input type="hidden" name="provider" value="<mm:write referid="provider"/>">
    </mm:present>
      <mm:present referid="class">
        <input type="hidden" name="class" value="<mm:write referid="class"/>">
    </mm:present>
    <mm:present referid="education">
        <input type="hidden" name="education" value="<mm:write referid="education"/>">
    </mm:present>
    
    
      <mm:node number="$currentitem">
        <mm:fieldlist fields="title?,name?,filename?,url?,handle?,text?,description?">
          <tr>
          <td valign="top"><mm:fieldinfo type="guiname"/></td>
          <td>
            <mm:compare referid="mayeditthis" value="true"><mm:fieldinfo type="input"/></mm:compare>
            <mm:fieldinfo type="name">
                <mm:compare value="handle">
                  <mm:field name="mimetype" jspvar="mimetype" vartype="String">
                    <% if (mimetype.startsWith("image/")) { %>
                        <p>
                          <img src="<mm:image/>"/>
                        </p>
                    <% } else { %>
                        <mm:compare referid="mayeditthis" value="false"><mm:fieldinfo type="guivalue"/></mm:compare>
                    <% } %>
                  </mm:field>
                </mm:compare>
                <mm:compare value="handle" inverse="true">
                    <mm:compare referid="mayeditthis" value="false">
                    
                        <mm:compare value="url">
                            <mm:fieldinfo type="guivalue"/>
                        </mm:compare>
                        <mm:compare value="url" inverse="true">
                            <mm:fieldinfo type="guivalue" escape="p"/>
                        </mm:compare>
                    </mm:compare>
                </mm:compare>
            </mm:fieldinfo>
          </td>
          </tr>
        </mm:fieldlist>
        
	<mm:compare referid="mayeditthis" value="true">
        <mm:relatednodes type="portfoliopermissions" max="1">
        <mm:field name="readrights">
        <tr>
            <td>Leesrechten</td>
            <td><select name="_readrights">
                <option value="0" <mm:compare value="0">selected</mm:compare> <mm:compare value="1">selected</mm:compare>>Niet zichtbaar</option>
                <option value="1" <mm:compare value="1">selected</mm:compare>>Zichtbaar voor studenten uit mijn klassen</option>
                <option value="2" <mm:compare value="2">selected</mm:compare>>Zichtbaar voor mijn docenten</option>
                <option value="3" <mm:compare value="3">selected</mm:compare>>Zichtbaar voor iedereen.</option>
                <option value="4" <mm:compare value="4">selected</mm:compare>>Zichtbaar voor niet-ingelogde (anonieme) gebruikers.</option>
            </select>
            </td>
        </tr>
        </mm:field>
        <mm:field name="allowreactions">
        <tr>
            <td>Reacties</td>
            <td><select name="_allowreactions">
                <option value="0" <mm:compare value="0">selected</mm:compare> <mm:compare value="1">selected</mm:compare>>Geen reacties toestaan</option>
                <option value="1" <mm:compare value="1">selected</mm:compare>>Reacties toestaan</option>
                </select>
            </td>
        </tr>
        </mm:field>
        </mm:relatednodes>
        </mm:compare>

      </mm:node>
      <input type="hidden" name="currentfolder" value="<mm:write referid="currentfolder"/>"/>
      <input type="hidden" name="currentitem" value="<mm:write referid="currentitem"/>"/>
      <input type="hidden" name="contact" value="<mm:write referid="contact"/>"/>
     <input type="hidden" name="typeof" value="<mm:write referid="typeof"/>"/>
      <tr><td>
      <mm:compare referid="mayeditthis" value="true"><input class="formbutton" type="submit" name="action1" value="<fmt:message key="SAVE" />" /></mm:compare>
      <input class="formbutton" type="submit" name="action2" value="<fmt:message key="BACK" />" />
      </td></tr>
    </form>


    
<mm:node number="$currentitem">
   <mm:import id="allowreactions">0</mm:import>
   <mm:relatednodes type="portfoliopermissions" max="1">
       <mm:import id="allowreactions" reset="true"><mm:field name="allowreactions"/></mm:import>
   </mm:relatednodes>
   <mm:compare referid="allowreactions" value="1">
    <tr><td colspan="2">
    <br><br>
    <p>
    <b><fmt:message key="REACTIONS"/></b>
    </p>
    </td></tr>
    <tr><td></td><td>
       
        <mm:relatednodes type="forummessages" orderby="number" directions="UP">
          <div style="border: solid black 1px; width: 500px; margin-bottom: 0.5em; padding: 0.25em 0.5em 0.25em 0.5em">
            <b><mm:field name="title"/> (<mm:field name="date"><mm:time format="d/M/yyyy"/></mm:field>)</b><br>
            <mm:field name="body" escape="p"/>
          </div>
        </mm:relatednodes>
    </td></tr>
        <form name="showitem" method="post" action="<mm:treefile page="/portfolio/showitem.jsp" objectlist="$includePath" referids="$referids"/>" >
      <input type="hidden" name="currentfolder" value="<mm:write referid="currentfolder"/>"/>
      <input type="hidden" name="currentitem" value="<mm:write referid="currentitem"/>"/>
      <input type="hidden" name="typeof" value="<mm:write referid="typeof"/>"/>
      <input type="hidden" name="contact" value="<mm:write referid="contact"/>"/>
 
        
            <input type="hidden" name="add_message" value="1">
            <tr><td>Titel</td>
            <td><input type="text" name="title" size="80"></td>
            </tr>
            <tr>
            <td>Tekst</td>
            <td><textarea name="body" cols="80" rows="10"></textarea></td>
            </tr>
            <tr>
            <td>
            <input type="submit" value="<fmt:message key="REACT"/>" class="formbutton">
            </td></tr>
        </form>
    </mm:compare>
</mm:node>
    </table>
  </div>
</div>
</div>
<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</mm:notpresent>
</mm:compare>
</fmt:bundle>
</mm:cloud>
</mm:content>
