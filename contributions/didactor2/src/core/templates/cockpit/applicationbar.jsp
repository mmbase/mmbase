<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" 
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" 
%><mm:cloud method="delegate" authenticate="asis">
<jsp:directive.include file="/shared/setImports.jsp" />
<mm:locale language="${locale}">
<div class="applicationMenubar" style="white-space: nowrap">
  <mm:hasrank value="didactor-anonymous">
    <div class="menuItemApplicationMenubar login">
      <mm:treefile page="education/index.jsp" objectlist="$includePath" referids="$referids"
                   id="startpagepage" write="false" />
      <form method="post" action="${startpage}">
        <p>
          <input type="hidden" name="authenticate"  value="plain"  />
          <input type="hidden" name="command"       value="login" />
          <input type="hidden" name="provider"       value="${provider}" />
          <input type="hidden" name="education"       value="${education}" />
          <mm:fieldlist nodetype="people" fields="username">
            <mm:fieldinfo type="guiname" />
          </mm:fieldlist>
          <input id="loginUsername" type="text" size="20" name="username" value="${newusername}" />
          <mm:fieldlist nodetype="people" fields="password">
            <mm:fieldinfo type="guiname" />
          </mm:fieldlist>
          <input id="loginPassword" type="password" size="20" name="password" value="${newpassword}" />
          <input class="formbutton" id="loginSubmit" type="submit" value="<di:translate key="core.login" />" />
        </p>
      </form>
      <mm:node number="component.register" notfound="skipbody">
        <p class="noaccount">
          <di:translate key="register.noaccountyet" />
          <di:translate key="register.registeryourself" />
          <a href="<mm:treefile page="/register/index.jsp" objectlist="$includePath" referids="$referids" />"><di:translate key="register.here" /></a>
          </p>
      </mm:node>
    </div>
  </mm:hasrank>
  <mm:hasrank minvalue="basic user">
    <div class="menuItemApplicationMenubar">
      <a title="<di:translate key="core.home" />" href="<mm:treefile page="/index.jsp" objectlist="$includePath" referids="provider?,education?"/>" class="menubar"><di:translate key="core.home" /></a>
    </div>

    <div class="menuSeperatorApplicationMenubar"></div>
    <div class="menuItemApplicationMenubar">
      <mm:node number="$user">
        <a title="<di:translate key="core.logout" />" href="<mm:treefile page="/logout.jsp" objectlist="$includePath" referids="$referids"/>" class="menubar">
        <di:translate key="core.logout" />
        <di:person />
      </a>
      </mm:node>
    </div>
     
    <div class="menuSeperatorApplicationMenubar"></div>
    
    <div class="menuItemApplicationMenubar">
      <mm:hasnode number="component.portfolio">
        <a title="<di:translate key="core.configuration" />" href="<mm:treefile page="/portfolio/index.jsp?edit=true" objectlist="$includePath" referids="$referids"/>" class="menubar"><di:translate key="core.configuration" /></a>
      </mm:hasnode>
      <mm:hasnode number="component.portfolio" inverse="true">
        <a title="<di:translate key="core.configuration" />" href="<mm:treefile page="/admin/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar"><di:translate key="core.configuration" /></a>
      </mm:hasnode>
    </div>

    <div class="menuSeperatorApplicationMenubar"></div>
    <div class="menuItemApplicationMenubar">
      <a title="<di:translate key="core.print" />" href="javascript:printThis();"  class="menubar"><di:translate key="core.print" /></a>
    </div>
    
    <!-- region cms help and faq -->
    <mm:node number="$provider" notfound="skipbody">
      <mm:relatednodescontainer path="settingrel,components">
        <mm:constraint field="components.name" value="cmshelp"/>
        <mm:relatednodes>
          <mm:import id="showcmshelp" />
        </mm:relatednodes>
      </mm:relatednodescontainer>
      <mm:relatednodescontainer path="settingrel,components">
        <mm:constraint field="components.name" value="faq"/>
        <mm:relatednodes>
          <mm:import id="showfaq" />
        </mm:relatednodes>
      </mm:relatednodescontainer>
    </mm:node>

    <mm:present referid="education">
      <mm:notpresent referid="showcmshelp">
        <mm:node number="$education" notfound="skip">
          <mm:relatednodescontainer path="settingrel,components">
            <mm:constraint field="components.name" value="cmshelp"/>
            <mm:relatednodes>
              <mm:import id="showcmshelp" />
            </mm:relatednodes>
          </mm:relatednodescontainer>
        </mm:node>      
      </mm:notpresent>
      <mm:notpresent referid="showfaq">
        <mm:node number="$education" notfound="skip">
          <mm:relatednodescontainer path="settingrel,components">
            <mm:constraint field="components.name" value="faq"/>
            <mm:relatednodes>
              <mm:import id="showfaq" />
            </mm:relatednodes>
          </mm:relatednodescontainer>
        </mm:node>      
      </mm:notpresent>
    </mm:present>
     
    <mm:present referid="showcmshelp" >
      <mm:node number="component.cmshelp" notfound="skipbody">
          <mm:treeinclude page="/cmshelp/cockpit/rolerelated.jsp" objectlist="$includePath" referids="$referids" >
             <mm:param name="scope">education</mm:param>
          </mm:treeinclude>
      </mm:node>    
    </mm:present>
	  
    <mm:present referid="showfaq" >
  	  <mm:node number="component.faq" notfound="skipbody">
          <mm:treeinclude page="/faq/cockpit/rolerelated.jsp" objectlist="$includePath" referids="$referids" />
  	  </mm:node> 	   
    </mm:present>
    <!-- end of region cms help and faq -->
                                
  </mm:hasrank>
</div>

<script language="JavaScript" type="text/javascript">
<!--
function printThis() {
    if (frames && frames['content']) {
        if (frames['content'].focus) 
            frames['content'].focus(); 
        if (frames['content'].print)
            frames['content'].print();
    }
    else if (window.print) {
        window.print();
    }
}
//-->
</script>
</mm:locale>
</mm:cloud>
