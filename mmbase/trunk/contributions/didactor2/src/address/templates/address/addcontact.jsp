<%--
  This template adds a contact to a addressbook.
--%>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>

<%-- expires is set so renaming a folder does not show the old name --%>
<mm:content postprocessor="reducespace" expires="0">
<mm:cloud loginpage="/login.jsp" jspvar="cloud">

<%@include file="/shared/setImports.jsp" %>
<mm:treeinclude page="/cockpit/cockpit_header.jsp" objectlist="$includePath" referids="$referids">
  <mm:param name="extraheader">
    <title><di:translate key="address.addcontact" /></title>
  </mm:param>
</mm:treeinclude>

<mm:import externid="addressbook"/>
<mm:import externid="callerpage"/>
<mm:import externid="action1"/>
<mm:import externid="action2"/>

<%-- import of input fields --%>
<mm:import id="initials" externid="_initials"/>
<mm:import id="firstname" externid="_firstname"/>
<mm:import id="lastname" externid="_lastname"/>
<mm:import id="email" externid="_email"/>
<mm:import id="address" externid="_address"/>
<mm:import id="zipcode" externid="_zipcode"/>
<mm:import id="city" externid="_city"/>
<mm:import id="telephone" externid="_telephone"/>

<mm:node number="$addressbook" id="myaddressbook"/>

<%-- Check if the create button is pressed --%>
<mm:import id="action1text"><di:translate key="address.create" /></mm:import>
<mm:compare referid="action1" referid2="action1text">

  <%-- check if a firstname is given --%>
  <mm:compare referid="firstname" value="">
    <mm:import id="error">1</mm:import>
  </mm:compare>

  <%-- check if a lastname is given --%>
  <mm:notpresent referid="error">
    <mm:compare referid="lastname" value="">
      <mm:import id="error">2</mm:import>
    </mm:compare>
  </mm:notpresent>

  <mm:notpresent referid="error">
    <mm:node referid="myaddressbook">

      <mm:createnode type="contacts" id="mycontact">

        <mm:fieldlist type="all" fields="initials,firstname,lastname,email,address,zipcode,city,telephone">
          <mm:fieldinfo type="useinput" />
        </mm:fieldlist>

      </mm:createnode>

      <mm:createrelation role="related" source="myaddressbook" destination="mycontact"/>

    </mm:node>

    <mm:redirect referids="$referids,addressbook" page="$callerpage"/>
  </mm:notpresent>

</mm:compare>


<%-- Check if the back button is pressed --%>
<mm:import id="action2text"><di:translate key="address.back" /></mm:import>
<mm:compare referid="action2" referid2="action2text">
  <mm:redirect referids="$referids,addressbook" page="$callerpage"/>
</mm:compare>

<div class="rows">

<div class="navigationbar">
  <div class="titlebar">

    <mm:node referid="myaddressbook">
	  <mm:field name="name"/><br/>
	</mm:node>

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
    <di:translate key="address.addcontact" />
  </div>

  <div class="contentBodywit">

    <%-- Show the form --%>
    <form name="addcontact" class="formInput" method="post" action="<mm:treefile page="/address/addcontact.jsp" objectlist="$includePath" referids="$referids"/>">

      <table class="font">

        <tr><td><di:translate key="address.initials" /></td><td><input type="text" name="_initials" size="80" value="<mm:write referid="initials"/>"/></td></tr>
        <tr><td><di:translate key="address.firstname" /></td><td><input type="text" name="_firstname" size="80" value="<mm:write referid="firstname"/>"/></td></tr>
        <tr><td><di:translate key="address.lastname" /></td><td><input type="text" name="_lastname" size="80" value="<mm:write referid="lastname"/>"/></td></tr>
        <tr><td><di:translate key="address.email" /></td><td><input type="text" name="_email" size="80" value="<mm:write referid="email"/>"/></td></tr>
        <tr><td><di:translate key="address.address" /></td><td><input type="text" name="_address" size="80" value="<mm:write referid="address"/>"/></td></tr>
        <tr><td><di:translate key="address.zipcode" /></td><td><input type="text" name="_zipcode" size="80" value="<mm:write referid="zipcode"/>"/></td></tr>
        <tr><td><di:translate key="address.city" /></td><td><input type="text" name="_city" size="80" value="<mm:write referid="city"/>"/></td></tr>
        <tr><td><di:translate key="address.telephone" /></td><td><input type="text" name="_telephone" size="80" value="<mm:write referid="telephone"/>"/></td></tr>

      </table>

	  <br />
      <input type="hidden" name="callerpage" value="<mm:write referid="callerpage"/>"/>
      <input type="hidden" name="addressbook" value="<mm:write referid="addressbook"/>"/>
      <input type="submit" class="formbutton" name="action1" value="<di:translate key="address.create" />"/>
      <input type="submit" class="formbutton" name="action2" value="<di:translate key="address.back" />"/>

      <mm:present referid="error">
	    <p/>
	    <mm:compare referid="error" value="1">
  	      <h1><di:translate key="address.firstnamenotempty" /></h1>
	    </mm:compare>
	    <mm:compare referid="error" value="2">
  	      <h1><di:translate key="address.lastnamenotempty" /></h1>
	    </mm:compare>
	  </mm:present>

    </form>

  </div>
</div>
</div>

<mm:treeinclude page="/cockpit/cockpit_footer.jsp" objectlist="$includePath" referids="$referids" />

</mm:cloud>
</mm:content>
