<%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@taglib uri="http://www.didactor.nl/ditaglib_1.0" prefix="di" 
%><mm:cloud method="asis">
<div class="prev_next">
<a href="javascript:parent.previousContent();">
  <img src="${mm:treefile('/gfx/icon_arrow_last.gif', pageContext, includePath)}" width="14" height="14" border="0" 
       title="${di:translate(pageContext, 'education.previous')}"
       alt="${di:translate(pageContext, 'education.previous')}" />
</a>
<a href="javascript:parent.previousContent();" class="path"><di:translate key="education.previous" /></a>
<mm:link page="/education/show.jspx">
  <a class="popup" 
     href="#" 
     onmouseover="this.href='${_}?block=' + document.href_frame;"
     onclick="var el = document.getElementById('contentBodywit'); 
              var w = window.open(this.href, '_blank', 'menubar=no,toolbar=no,scrollbars=yes,height=' + el.clientHeight + ',width=' + el.clientWidth);
              return false;
              ">
    <di:translate key="education.pop" />
  </a>
</mm:link>
<a href="javascript:parent.nextContent();" class="path"><di:translate key="education.next" /></a>
<a href="javascript:parent.nextContent();">
  <img src="${mm:treefile('/gfx/icon_arrow_next.gif', pageContext, includePath)}" width="14" height="14" border="0"
       title="${di:translate(pageContext, 'education.next')}" 
       alt="${di:translate(pageContext, 'education.next')}"  />
</a>
</div>
</mm:cloud>
