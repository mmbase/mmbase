<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <mm:cloud method="asis">
    <div class="applicationMenubar">
      <mm:hasrank value="anonymous">

        <div class="menuItemApplicationMenubar login">
          <mm:treefile page="/education/index.jsp" objectlist="$includePath" referids="$referids"
                       id="startpage" write="false" />
          <form method="post" action="${startpage}">
            <p>
              <input type="hidden" name="authenticate"  value="plain"  />
              <input type="hidden" name="command"       value="login" />
              <input type="hidden" name="provider"       value="${provider}" />
              <input type="hidden" name="education"       value="${education}" />
              <mm:fieldlist nodetype="people" fields="username">
                <mm:fieldinfo type="guiname" />
              </mm:fieldlist>
              <input id="loginUsername" type="text" size="20" name="username" value="${sessionScope.registerPerson.username}" />
              <mm:fieldlist nodetype="people" fields="password">
                <mm:fieldinfo type="guiname" />
              </mm:fieldlist>
              <input id="loginPassword" type="password" size="20" name="password" value="${sessionScope.registerPassword}" />
              <input class="formbutton" id="loginSubmit" type="submit" value="${di:translate('core.login')}" />
              ${sessionScope["nl.didactor.security.reason"]}
            </p>
          </form>
          <!-- WTF WTF WTF WTF, all this explicit mentioning of 'components' is a bit silly -->
          <mm:node number="component.register" notfound="skipbody">
            <p class="noaccount">
              <di:translate key="register.noaccountyet" />
              <di:translate key="register.registeryourself" />
              <mm:treefile page="/register/index.jsp" objectlist="$includePath" referids="$referids" write="false">
                <a href="${_}"><di:translate key="register.here" /></a>
              </mm:treefile>
            </p>
          </mm:node>
        </div>
      </mm:hasrank>
      <mm:hasrank minvalue="didactor user">
        <div class="menuItemApplicationMenubar start">
          <mm:import externid="reset" />
          <mm:treefile page="/education/index.jsp" objectlist="$includePath" referids="reset?" write="false">
            <a title="${di:translate('core.home')}" href="${_}" class="menubar"><di:translate key="core.home" /></a>
          </mm:treefile>
        </div>

        <div class="menuSeperatorApplicationMenubar"><jsp:text> </jsp:text></div>
        <div class="menuItemApplicationMenubar">
          <mm:node number="${user}">
            <mm:treefile page="/logout.jsp" objectlist="$includePath" referids="$referids" write="false">
              <a title="${di:translate('core.logout')}"
                 href="${_}" class="menubar">
                <di:translate key="core.logout" />
                <jsp:text> </jsp:text>
                <di:person />
              </a>
            </mm:treefile>
          </mm:node>
        </div>

        <!--
            WTF WTF, specific code for yet another certain component
            This must be gone!
        -->

        <div class="menuSeperatorApplicationMenubar"><jsp:text> </jsp:text></div>

        <div class="menuItemApplicationMenubar">
          <mm:hasnode number="component.portfolio">
            <mm:treefile page="/portfolio/index.jsp?edit=true" objectlist="$includePath" referids="$referids" write="false"/>
            <a title="${di:translate('core.configuration')}"
               href="${_}" class="menubar"><di:translate key="core.configuration" /></a>
          </mm:hasnode>
          <mm:hasnode number="component.portfolio" inverse="true">
            <mm:treefile page="/admin/index.jsp"
                         objectlist="$includePath" referids="$referids" write="false">
              <a title="${di:translate('core.configuration')}"
                 href="${_}" class="menubar"><di:translate key="core.configuration" /></a>
            </mm:treefile>
          </mm:hasnode>
        </div>


        <div class="menuSeperatorApplicationMenubar"><jsp:text> </jsp:text></div>
        <div class="menuItemApplicationMenubar">
          <a title="${di:translate('core.print')}"
             href="javascript:printThis();"  class="menubar"><di:translate key="core.print" /></a>
        </div>

        <!--
            region cms help and faq
            WTF, why it this present in a generic JSP??
            Can people _please_ not hack their way?
        -->
        <mm:node number="$provider">
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

        <mm:notpresent referid="showcmshelp"><!-- WTF -->
          <mm:node number="$education">
            <mm:relatednodescontainer path="settingrel,components">
              <mm:constraint field="components.name" value="cmshelp"/>
              <mm:relatednodes>
                <mm:import id="showcmshelp" />
              </mm:relatednodes>
            </mm:relatednodescontainer>
          </mm:node>
        </mm:notpresent>

        <mm:notpresent referid="showfaq"> <!-- WTF -->
          <mm:node number="$education">
            <mm:relatednodescontainer path="settingrel,components">
              <mm:constraint field="components.name" value="faq"/>
              <mm:relatednodes>
                <mm:import id="showfaq" />
              </mm:relatednodes>
            </mm:relatednodescontainer>
          </mm:node>
        </mm:notpresent>

        <mm:present referid="showcmshelp" >
          <mm:node number="component.cmshelp">
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


  </mm:cloud>
</jsp:root>
