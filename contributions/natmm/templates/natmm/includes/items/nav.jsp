<table cellspacing="0" cellpadding="0" width="100%">
<tr><td class="internalnav">OVERZICHT VAN DIT PRODUCT</td></tr>
<tr><td style="padding:4px;padding-bottom:14px;">
  <mm:present referid="body">
    <a href="<%= ph.createPaginaUrl(paginaID,request.getContextPath()) %>#body" class="subtitle"><mm:write referid="body" /></a><br/>
  </mm:present>
  <mm:present referid="product">
    <a href="<%= ph.createPaginaUrl(paginaID,request.getContextPath()) %>#product" class="subtitle"><mm:write referid="product" /></a><br/>
  </mm:present>
  <mm:present referid="thumbs">
    <a href="<%= ph.createPaginaUrl(paginaID,request.getContextPath()) %>#thumbs" class="subtitle"><mm:write referid="thumbs" /></a><br/>
  </mm:present>
  <mm:list nodes="<%= shop_itemId %>" path="items,posrel,artikel" orderby="posrel.pos" directions="UP">
    <a href="<%= ph.createPaginaUrl(paginaID,request.getContextPath()) %>#<mm:field name="artikel.number" />" class="subtitle">
      <mm:field name="artikel.titel_fra" />
    </a><br>
  </mm:list>
</td></tr>
<tr><td class="titlebar"><img src="media/trans.gif" height="1" width="1" alt="" border="0" alt=""></td></tr>
</table>
