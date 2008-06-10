<!--
  This translates a mmbase XML field to enriched ASCII

  @author: Michiel Meeuwissen
  @version: $Id: mmxf2rich.xslt,v 1.10 2008-06-10 12:41:08 michiel Exp $
  @since:  MMBase-1.6
-->
<xsl:stylesheet
  xmlns:xsl = "http://www.w3.org/1999/XSL/Transform"
  xmlns:mmxf="http://www.mmbase.org/xmlns/mmxf"
    version = "1.0" >
  <xsl:output method = "text" />

  <xsl:template match = "mmxf:mmxf" >
    <xsl:apply-templates select="mmxf:p|mmxf:table|mmxf:ol|mmxf:ul" />
    <xsl:apply-templates select="mmxf:section">
      <xsl:with-param name="depth">$</xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match = "mmxf:p" >
    <xsl:apply-templates select="." mode="rels" />
    <xsl:apply-templates select="mmxf:a|mmxf:em|mmxf:strong|text()|mmxf:ul|mmxf:ol|mmxf:br" />
    <xsl:text>&#xA;&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match = "mmxf:section" >
    <xsl:param name="depth" />
    <xsl:value-of select="$depth" />
    <xsl:text> </xsl:text>
    <xsl:apply-templates select="." mode="rels" />
    <xsl:value-of select="mmxf:h" />
    <xsl:text>&#xA;</xsl:text>
    <xsl:apply-templates select="mmxf:p|mmxf:ul|mmxf:ol|mmxf:table" />
    <xsl:apply-templates select="mmxf:section">
      <xsl:with-param name="depth">$<xsl:value-of select="$depth" /></xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>


  <xsl:template match="mmxf:em" >
    <xsl:text>_</xsl:text><xsl:value-of select = "." /><xsl:text>_</xsl:text>
  </xsl:template>

  <xsl:template match="mmxf:strong" >
    <xsl:text>*</xsl:text><xsl:value-of select = "." /><xsl:text>*</xsl:text>
  </xsl:template>

  <xsl:template match="mmxf:a" >
    <xsl:apply-templates select="*" />
  </xsl:template>

  <xsl:template match="mmxf:ul" >
    <xsl:text>&#xA;</xsl:text>
    <xsl:apply-templates select="mmxf:li" mode="ul" />
  </xsl:template>
  <xsl:template match="mmxf:ol" >
    <xsl:text>&#xA;</xsl:text>
    <xsl:apply-templates select="mmxf:li" mode="ol" />
  </xsl:template>

  <xsl:template match="mmxf:li" mode="ul" >
    <xsl:text>- </xsl:text><xsl:apply-templates />
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>
  <xsl:template match="mmxf:li" mode="ol" >
    <xsl:text>* </xsl:text><xsl:apply-templates />
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>


  <xsl:template match="mmxf:table">
    <xsl:text>{|&#xA;</xsl:text>
    <xsl:apply-templates select="mmxf:caption" />
    <xsl:apply-templates select="mmxf:tr" />
    <xsl:apply-templates select="mmxf:tbody" /> <!-- does not exist, but to be on the safe side -->
    <xsl:text>|}&#xA;&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match="mmxf:tbody">
    <xsl:apply-templates select="mmxf:tr" />
  </xsl:template>

  <xsl:template match="mmxf:caption">
    <xsl:text>|+</xsl:text>
    <xsl:apply-templates select="text()|mmxf:p" />
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match="mmxf:tr">
    <xsl:if test="position() != 1">
      <xsl:text>|-&#xA;</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="mmxf:th|mmxf:td" />
    <xsl:text>&#xA;</xsl:text>
  </xsl:template>

  <xsl:template match="mmxf:th">
    <xsl:if test="position() != 1">
      <xsl:text>!</xsl:text>
    </xsl:if>
    <xsl:text>!</xsl:text>
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="mmxf:td">
    <xsl:if test="position() != 1">
      <xsl:text>|</xsl:text>
    </xsl:if>
    <xsl:text>|</xsl:text>
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="mmxf:sub">
    <xsl:text>_</xsl:text><xsl:value-of select="." />
  </xsl:template>
  <xsl:template match="mmxf:sup">
    <xsl:text>^</xsl:text><xsl:value-of select="." />
  </xsl:template>

  <xsl:template match="*" mode="rels">
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="." />
  </xsl:template>

</xsl:stylesheet>
