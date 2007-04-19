<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" 
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" 
%><mm:cloud method="delegate" >
<jsp:directive.include file="/shared/setImports.jsp" />
  <%-- Always include the progress component --%>
  <div class="progress">
    <a href="<mm:treefile page="/progress/index.jsp" objectlist="$includePath" referids="$referids" />" class="menubar"> <di:translate key="progress.progress" />:</a>
    <img src="<mm:treefile write="true" page="/gfx/spacer.gif" objectlist="$includePath" />" width="1" height="15" alt="" />
  </div>

  <div class="progressMeter">
    <di:hasrole role="student">
      <mm:timer>
        <mm:import jspvar="progress" id="progress"><mm:treeinclude page="/progress/getprogress.jsp" objectlist="$includePath" referids="$referids"/></mm:import>
      </mm:timer>
      <img src="<mm:treefile write="true" page="/gfx/bar_left.gif" objectlist="$includePath" />" width="4" height="13" alt="" /><img src="<mm:treefile write="true" page="/gfx/bar_center.gif" objectlist="$includePath" />" width="${progress * 92}" height="13" alt="" /><img src="<mm:treefile write="true" page="/gfx/bar_right.gif" objectlist="$includePath" />" width="4" height="13" alt="" />
      </di:hasrole>
  </div>
</mm:cloud>

