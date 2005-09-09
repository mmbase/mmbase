<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@page import="java.util.*" %>
<mm:content postprocessor="reducespace">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<%@ include file="getids.jsp" %>
<% boolean isEmpty = true; %>
<mm:import externid="msg">-1</mm:import>
   <%

      String bundlePOP = null;

   %>

   <mm:write referid="lang_code" jspvar="sLangCode" vartype="String" write="false">

      <%

         bundlePOP = "nl.didactor.component.pop.PopMessageBundle_" + sLangCode;

      %>

   </mm:write>

<fmt:bundle basename="<%= bundlePOP %>">
<%= bundlePOP %>
<div class="contentBody">
  <%@ include file="getmyfeedback.jsp" %>
    <mm:compare referid="msg" value="-1" inverse="true">
      <mm:write referid="msg"/>
    </mm:compare>

  <mm:node number="$currentcomp">
    <form name="editcompform" action="<mm:treefile page="/pop/index.jsp" objectlist="$includePath" 
          referids="$popreferids,currentprofile,currentcomp">
        </mm:treefile>" method="post">
    <table class="font" width="70%">
    <input type="hidden" name="command" value="savecomp">
    <input type="hidden" name="returnto" value="editcomp">
    <input type="hidden" name="todonumber" value="-1">
      <tr style="vertical-align:top;">
        <td width="100" style="vertical-align:top;"><fmt:message key="Competence"/></td>
        <td><b><mm:field name="name"/></b></td>
      </tr>
      <tr style="vertical-align:top;">
        <td width="100" style="vertical-align:top;"><fmt:message key="Description"/></td>
        <td><b><mm:field name="description"/></b></td>
      </tr>
      <tr style="vertical-align:top;">
        <td nowrap><fmt:message key="CompEditFeedback1"/></td>
        <td><input name="myfeedback1" class="popFormInput" type="text" size="50" maxlength="255" value="<mm:write referid="myfeedback1"/>"></td>
      </tr>
      <tr style="vertical-align:top;">
        <td><fmt:message key="CompEditFeedback2"/></td>
        <td><textarea name="myfeedback2" class="popFormInput" cols="50" rows="5"><mm:write referid="myfeedback2"/></textarea></td>
      </tr>
  </table>
  <table class="font" width="80%">
    <tr>
      <td>
        <input type="button" class="formbutton" onClick="editcompform.submit()" value="<fmt:message key="SaveButton"/>">
        <input type="button" class="formbutton" onClick="editcompform.command.value='no';editcompform.submit()" value="<fmt:message key="BackButtonLC"/>">
      </td>

    </tr>
  </table>
<br/>
  <table width="80%" border="0" class="popGreyTableHeader">
    <tr>
      <td colspan="3"><fmt:message key="TodoItems"/></td>
    </tr>
  </table>
          <mm:list nodes="$currentpop" path="pop,todoitems,competencies" orderby="todoitems.number" directions="UP"
              constraints="competencies.number='$currentcomp'">
            <input type="checkbox" name="ids" value="<mm:field name="todoitems.number"/>"><a href="#1"
                onclick="editcompform.command.value='addtodo';editcompform.todonumber.value='<mm:field name="todoitems.number"/>';editcompform.submit();return false;"
              ><mm:field name="todoitems.name" jspvar="todoName" vartype="String"
              ><% if (todoName.length()>0) { %><%= todoName %><% } else { %>...<% } %></mm:field></a><br/>
          </mm:list>
          <br/>
          <a href="#1" onclick="editcompform.command.value='addtodo';editcompform.submit();return false;"
            ><img src="<mm:treefile page="/pop/gfx/icon_add_todo.gif" objectlist="$includePath" referids="$popreferids"/>"
                border="0" alt="<fmt:message key="CompEditMakeNewTodo"/>"/></a>
          <a href="#1" onclick="if (!window.confirm('<fmt:message key="AreYouSureDelTodo"/>'))
                return false;editcompform.command.value='deltodo';editcompform.submit();return false;">
            <img src="<mm:treefile page="/pop/gfx/afspraak verwijderen.gif" objectlist="$includePath" referids="$popreferids"/>"
                border="0" alt="<fmt:message key="CompEditRemoveSelectedTodo"/>"/></a>
<br/>
<br/>
    <mm:relatedcontainer path="popfeedback,pop">
      <mm:constraint field="pop.number" referid="currentpop" operator="EQUAL"/>
      <table width="80%" border="0" class="popSpecialTableHeader">
        <tr>
          <td colspan="3"><fmt:message key="CompEditGrades"/></td>
        </tr>
      </table>
      <div><table class="poplistTable">
        <tr>
          <th class="listHeader"><fmt:message key="CompEditBy"/></th>
          <th class="listHeader"><fmt:message key="CompEditWorkTogetherEtc"/></th>
          <th class="listHeader"><fmt:message key="Score"/></th>
          <th class="listHeader"><fmt:message key="CompEditGrade"/></th>
        </tr>
        <mm:related>
          <mm:node element="popfeedback">
            <mm:relatedcontainer path="people">
              <mm:constraint field="people.number" referid="student" operator="EQUAL" inverse="true"/>
              <mm:related>
                <tr>
                  <td class="listItem"><mm:field name="people.firstname"/> <mm:field name="people.lastname"/></td>
                  <mm:node element="popfeedback">
                    <mm:field name="status" jspvar="isAnswered" vartype="String">
                      <% if (!isAnswered.equals("0")) { %>
                        <td class="listItem"><mm:field name="rank"/></td>
                        <td class="listItem">
                          <mm:related path="ratings">
                            <mm:field name="ratings.name" id="rating" write="true"/>
                          </mm:related>
                        </td>
                        <td class="listItem"><mm:field name="text"/></td>
                      <% } else { %>
                        <td class="listItem"><i><fmt:message key="NotAnswered"/></i></td>
                        <td class="listItem">&nbsp;</td>
                        <td class="listItem">&nbsp;</td>
                      <% } %>
                    </mm:field>
                  </mm:node>
                </tr>
              </mm:related>
            </mm:relatedcontainer>
          </mm:node>
        </mm:related>
      </table></div>
    </mm:relatedcontainer>
        <a href="#1" onclick="editcompform.command.value='invite';editcompform.submit();return false;">
          <img src="<mm:treefile page="/pop/gfx/icon_invitation.gif" objectlist="$includePath" referids="$popreferids"/>"
            border="0" alt="<fmt:message key="CompEditInviteColleague"/>"/></a>
    <br/><br/><br/>
    <table width="80%" border="0" class="popGreyTableHeader">
      <tr>
        <td colspan="3"><fmt:message key="Portfolio" /></td>
      </tr>
    </table>
    <mm:compare referid="thisfeedback" value="-1" inverse="true">
      <mm:node number="$thisfeedback">
        <mm:relatednodes type="attachments">
          <input type="checkbox" name="portfolio_items_ids" value="<mm:field name="number"/>">
          <mm:related path="folders,portfolios,people" constraints="people.number='$student'">
            <mm:import id="foldername" reset="true"><mm:field name="folders.name"/></mm:import>
            <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$popreferids">
                <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
              </mm:treefile>"
            ></mm:related><mm:write referid="foldername"/> > <mm:field name="title"/></a><br/>
        </mm:relatednodes>
        <mm:relatednodes type="folders">
          <mm:import id="foldernumber" reset="true"><mm:field name="number"/></mm:import>
          <input type="checkbox" name="portfolio_items_ids" value="<mm:field name="number"/>">
          <mm:related path="portfolios,people" constraints="people.number='$student'">
            <mm:import id="foldername" reset="true"><mm:field name="folders.name"/></mm:import>
            <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$popreferids">
                <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                <mm:param name="currentfolder"><mm:write referid="foldernumber"/></mm:param>
              </mm:treefile>"
            ></mm:related><mm:field name="name"/></a><br/>
        </mm:relatednodes>
        <mm:relatednodes type="urls">
          <input type="checkbox" name="portfolio_items_ids" value="<mm:field name="number"/>">
          <mm:related path="folders,portfolios,people" constraints="people.number='$student'">
            <mm:import id="foldername" reset="true"><mm:field name="folders.name"/></mm:import>
            <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$popreferids">
                <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
              </mm:treefile>"
            ></mm:related><mm:write referid="foldername"/> > <mm:field name="name"/></a><br/>
        </mm:relatednodes>
        <mm:relatednodes type="pages">
          <input type="checkbox" name="portfolio_items_ids" value="<mm:field name="number"/>">
          <mm:related path="folders,portfolios,people" constraints="people.number='$student'">
            <mm:import id="foldername" reset="true"><mm:field name="folders.name"/></mm:import>
            <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$popreferids">
                <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
              </mm:treefile>"
            ></mm:related><mm:write referid="foldername"/> > <mm:field name="name"/></a><br/>
        </mm:relatednodes>
        <mm:relatednodes type="chatlogs">
          <input type="checkbox" name="portfolio_items_ids" value="<mm:field name="number"/>">
          <mm:related path="folders,portfolios,people" constraints="people.number='$student'">
            <mm:import id="foldername" reset="true"><mm:field name="folders.name"/></mm:import>
            <a href="<mm:treefile page="/portfolio/index.jsp" objectlist="$includePath" referids="$popreferids">
                <mm:param name="typeof"><mm:field name="portfolios.type"/></mm:param>
                <mm:param name="currentfolder"><mm:field name="folders.number"/></mm:param>
              </mm:treefile>"
            ></mm:related><mm:write referid="foldername"/> > <mm:field name="date"/></a><br/>
        </mm:relatednodes>
      </mm:node>
    </mm:compare>
    <br/>
    <a href="#1" onclick="if (!window.confirm('<fmt:message key="AreYouSureDelDoc"/>'))
        return false;editcompform.command.value='deldocs';editcompform.submit();return false;">
      <img src="<mm:treefile page="/pop/gfx/afspraak verwijderen.gif" objectlist="$includePath" referids="$popreferids"/>"
        border="0" alt="<fmt:message key="CompEditRemoveSelectedDoc"/>"/></a>
    <a href="#1" onclick="editcompform.command.value='adddoc';editcompform.submit();return false;">
      <img src="<mm:treefile page="/portfolio/gfx/document plaatsen.gif" objectlist="$includePath" referids="$popreferids"/>" 
        border="0" alt="<fmt:message key="PortfolioAddDoc"/>"/></a>
  </form>
  </mm:node>
</div>
</fmt:bundle>
</mm:cloud>
</mm:content>
