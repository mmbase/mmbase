<%@taglib uri="http://www.mmbase.org/mmbase-taglib-1.1" prefix="mm" %>
<%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" %>
<mm:import externid="type" />
<mm:import externid="scope">none</mm:import>
<%-- 'progress' is only valid in the 'education' scope --%>
<%-- TODO use translate tag for use of more languages --%>
<mm:compare referid="scope" value="education">
  <mm:cloud loginpage="/login.jsp" jspvar="cloud">
  <%@include file="/shared/setImports.jsp" %>
  <mm:compare referid="type" value="div">
    <div class="menuSeperator"> </div>
    <div class="menuItem" id="menuProgress">
      <a href="<mm:treefile page="/progress/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar"><di:translate key="progress.progress" /></a>
    </div>
  </mm:compare>
  
  <mm:compare referid="type" value="option">
    <option value="<mm:treefile page="/progress/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar">
      <di:translate key="progress.progress" />
    </option>
  </mm:compare>
  </mm:cloud>
</mm:compare>
