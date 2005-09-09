<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:import externid="type" />
<mm:cloud loginpage="/login.jsp" jspvar="cloud">
<%@include file="/shared/setImports.jsp" %>
<fmt:bundle basename="nl.didactor.component.email.EmailMessageBundle">
<mm:compare referid="type" value="div">
  <div class="menuSeperator"> </div>
  <div class="menuItem" id="menuEmail">
    <a href="<mm:treefile page="/email/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar">
</mm:compare>

<mm:compare referid="type" value="option">
  <option value="<mm:treefile page="/email/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar">
</mm:compare>
    <% int total = 0; %>
    <fmt:message key="EMAILTITLE" />
    <mm:node number="$user">
      <mm:relatednodescontainer type="mailboxes">
        <mm:relatednodes>
          <mm:relatednodescontainer type="emails">
            <mm:constraint field="type" value="2" operator="=" /> <%-- find new mails --%>
            <mm:import id="size" jspvar="size" vartype="Integer"><mm:size /></mm:import>
            <% total+=size.intValue(); %>
          </mm:relatednodescontainer>
        </mm:relatednodes>
      </mm:relatednodescontainer>  
    </mm:node>
    (<%= total %>)
<mm:compare referid="type" value="div">
    </a>
  </div>
</mm:compare>
<mm:compare referid="type" value="option">
  </option>
</mm:compare>
</fmt:bundle>
</mm:cloud>
