<jsp:root version="2.0"
          xmlns:jsp="http://java.sun.com/JSP/Page"
          xmlns:fn="http://java.sun.com/jsp/jstl/functions"
          xmlns:mm="http://www.mmbase.org/mmbase-taglib-2.0"
          xmlns:c="http://java.sun.com/jsp/jstl/core"
          xmlns:di="http://www.didactor.nl/ditaglib_1.0">
  <jsp:output omit-xml-declaration="yes" />
  <mm:import externid="branchPath" />
  <mm:relatednodes role="childppnn" type="portalpagesnodes">
    <di:leaf
        branchPath="${branchPath} "
        click="portal_child_${_node}"
        >
      <mm:link referid="wizardjsp" referids="_node@objectnumber">
        <mm:param name="wizard">config/portalpages/leafportalpagesnodes</mm:param>
        <td><a href="${_}" title="edit" target="text"><mm:field name="name"/></a></td>
      </mm:link>
    </di:leaf>
    <div id="portal_child_${_node}" style="display:none">
      <di:leaf
          branchPath="${branchPath}"
          icon="new_education"
          >
        <mm:link referid="wizardjsp" referids="thischild@origin">
          <mm:param name="wizard">config/portalpages/newsimplecontents</mm:param>
          <mm:param name="objectnumber">new</mm:param>
          <td>
            <a href="${_}" title="nieuwe content" target="text">nieuwe content</a>
          </td>
        </mm:link>
      </di:leaf>
      <mm:relatednodes role="related" path="simplecontents" varStatus="status">
        <di:leaf
            branchPath="${branchPath} ${status.last ? '.' : ' '}"
            icon="learnblock">
          <mm:link referid="wizardjsp" referids="_node@objectnumber">
            <mm:param name="wizard">config/portalpages/simplecontents</mm:param>
            <td>
              <a href="${_}" target="text"><mm:field name="title"/></a>
            </td>
          </mm:link>
        </di:leaf>
      </mm:relatednodes>
    </div>
  </mm:relatednodes>
  <mm:relatednodes role="related" type="simplecontents" varStatus="status">
    <di:leaf
        branchPath="${branchPath}${status.last ? '.' : ' '}"
        icon="learnblock">
      <mm:link referid="wizardjsp" referids="_node@objectnumber">
        <mm:param name="wizard">config/portalpages/simplecontents</mm:param>
        <td>
          <a href="${_}" title="edit" target="text"><mm:field name="title"/></a>
        </td>
      </mm:link>
    </di:leaf>
  </mm:relatednodes>
</jsp:root>
