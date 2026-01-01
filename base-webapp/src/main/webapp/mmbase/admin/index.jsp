<jsp:root
    version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:fn="http://java.sun.com/jsp/jstl/functions"
    xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0">

  <!-- actually just to help some browsers: -->
  <jsp:output doctype-root-element="html"
              doctype-public="-//W3C//DTD XHTML 1.1//EN"
              doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"/>

  <mm:import externid="mmbase_adminpages_language" from="parameters,cookie">client</mm:import>
  <mm:write cookie="mmbase_adminpages_language" referid="mmbase_adminpages_language" />
  <mm:content expires="0"
              type="application/xhtml+xml"
              unacceptable="CRIPPLE"
              postprocessor="none" language="${mmbase_adminpages_language}">


    <c:if test="${! empty param.logout}">
      <mm:cloud method="logout" />
    </c:if>
    <mm:cloud rank="basic user">
      <mm:import externid="category">about</mm:import>
      <mm:import externid="subcategory"></mm:import>
      <mm:import externid="component" />
      <mm:import externid="block" />
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
        <mm:formatter xslt="xslt/framework/head.xslt" escape="none">
          <head>
            <title>MMBase<mm:present referid="category"> - <mm:write referid="category" /></mm:present><mm:present referid="block"> : <mm:write referid="block" /></mm:present></title>
            <mm:link page="/mmbase/admin/css/admin.css">
              <link rel="stylesheet" href="${_}" type="text/css" />
            </mm:link>
            <mm:link page="/mmbase/style/images/favicon.ico">
              <link rel="icon" href="${_}" type="image/x-icon" />
              <link rel="shortcut icon" href="${_}" type="image/x-icon" />
            </mm:link>
            <mm:present referid="block">
              <mm:component name="$component" block="$block" render="head" />
            </mm:present>
            <mm:notpresent referid="block">
              <mm:functioncontainer>
                <mm:param name="id">mmbase.${category}</mm:param>
                <mm:function set="components" name="blockClassification">
                  <c:if test="${fn:length(_[0].blocks) gt 0}">
                    <c:set var="component" value="${_[0].blocks[0].component.name}" />
                    <c:set var="block" value="${_[0].blocks[0].name}" />
                    <mm:component name="$component" block="${block}" render="head" />
                  </c:if>
                </mm:function>
              </mm:functioncontainer>
            </mm:notpresent>
            <mm:link page="/mmbase/admin/admin.js">
              <script src="${_}" type="text/javascript"><jsp:text> </jsp:text></script>
            </mm:link>
          </head>
        </mm:formatter>
        <mm:function id="blockObject" set="components" name="block" referids="component,block" write="false" />
        <body>
          <div id="header">
            <div id="logo">
              <a href="${mm:link('/mmbase')}"><mm:link page="/mmbase/style/logo_trans.png"><img src="${_}" alt="MMBase" width="40" height="50" /></mm:link></a>
            </div>
            <div id="head">
              <h1>MMBase</h1>
              <p>
                You are logged in as: <mm:cloudinfo type="user" /> (rank: <mm:cloudinfo type="rank" />) |
                <mm:link>
                  <mm:param name="logout" value="ok" />
                  <a href="${_}">logout</a>
                </mm:link>
                <mm:import externid="languages" vartype="list">nl_NL,en,zh,fr,eo</mm:import>
                <form method="post">
                  <select name="mmbase_adminpages_language" onchange="this.form.submit()">
                    <mm:option value="client" compare="${mmbase_adminpages_language}">-AUTO-</mm:option>
                    <mm:stringlist referid="languages">
                      <mm:option value="${_}" compare="${mmbase_adminpages_language}">
                        <mm:locale language="${_}" jspvar="loc">${loc.displayLanguage}</mm:locale>
                      </mm:option>
                    </mm:stringlist>
                  </select>
                </form>
              </p>
            </div>
           </div>
          <div id="wrap">
             <div id="navigation">
              <ul>
                <mm:functioncontainer>
                  <mm:param name="id">mmbase</mm:param>
                  <mm:listfunction set="components" name="blockClassification">
                    <mm:stringlist referid="_.subTypes" id="cat">
                      <c:if test="${fn:length(cat.blocks) gt 0}">
                        <mm:link id="link">
                          <mm:frameworkparam name="category">${cat.name}</mm:frameworkparam>
                        </mm:link>
                        <li class="weight_${cat.weight}">
                          <a class="${category eq cat.name ? 'selected' : ''}" href="${link}">${mm:string(cat.title)}</a>
                          <mm:compare referid="category" value="${cat.name}">
                            <ul>
                              <c:forEach var="b" items="${cat.blocks}">
                                <mm:link>
                                  <mm:frameworkparam name="urlconverter">org.mmbase.framework.basic.MMBaseUrlConverter</mm:frameworkparam>
                                  <mm:frameworkparam name="category">${category}</mm:frameworkparam>
                                  <mm:frameworkparam name="component">${b.component.name}</mm:frameworkparam>
                                  <mm:frameworkparam name="block">${b.name}</mm:frameworkparam>
                                  <li class="${b.name eq block and subcat.component.name eq component ? 'current' : ''}">
                                    <a title="${mm:string(b.description)}" href="${_}">
                                      <mm:escape escape="text/xml">${mm:string(b.title)}</mm:escape>
                                      <span class="component"> (${b.component.name})</span>
                                    </a>
                                  </li>
                                </mm:link>
                              </c:forEach>
                            </ul>
                          </mm:compare>
                        </li>
                      </c:if>
                    </mm:stringlist>
                  </mm:listfunction>
                </mm:functioncontainer>
              </ul>
            </div>
             <div id="content">
              <c:catch var="exception">
                <h2 class="top">${mm:escape('text/xml', mm:string(blockObject.title))}</h2>
                <c:forEach items="${blockObject.classification}" var="clazz">
                  <c:if test="${clazz.name eq 'unfinished'}">
                    <h2 class="top unfinished">Warning. This block is marked 'unfinished', so it probably is not working correctly yet.</h2>
                  </c:if>
                </c:forEach>
                <mm:component debug="xml" name="$component" block="${block}">
                  <mm:frameworkparam name="category">${category}</mm:frameworkparam>
                </mm:component>
                <!--
                <mm:may component="core" action="view_source">
                  <mm:url page="${block}" internal="true" absolute="context">
                    <mm:frameworkparam name="component">${component}</mm:frameworkparam>
                  </mm:url>
                </mm:may>
                -->
              </c:catch>
              <c:if test="${! empty exception}">
                <pre>
                  ${mm:escape('text/xml', exception)}
                </pre>
                <pre>
                  ${mm:escape('text/xml', exception.cause.cause.cause)}
                  ${mm:escape('text/xml', exception.cause.cause)}
                  ${mm:escape('text/xml', exception.cause)}
                </pre>
              </c:if>
            </div>
            <div id="footer">
              <ul>
                <li><a href="http://www.mmbase.org">www.mmbase.org</a></li>
                <li><a href="http://www.mmbase.org/license">license</a></li>
                <li><a href="http://www.mmbase.org/mmdocs">documentation</a></li>
                <li><a href="http://www.mmbase.org/bugs">bugs</a></li>
                <li><a href="http://www.mmbase.org/contact">contact</a></li>
                <li><a href="http://www.mmbase.org/principles">principles</a></li>
              </ul>
            </div>
          </div><!-- /#wrap -->
        </body>
      </html>
    </mm:cloud>
  </mm:content>
</jsp:root>
