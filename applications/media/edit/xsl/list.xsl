<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0">
  <!-- Stream manager -->

  <xsl:import href="ew:xsl/list.xsl" /> <!-- extend from standard  editwizard xslt -->

  <xsl:variable name="objectnumber">-1</xsl:variable>

 <xsl:template name="body"> 
    <body onLoad="window.focus(); init(''); ">
      <xsl:call-template name="bodycontent" />
    </body>
  </xsl:template>


</xsl:stylesheet>