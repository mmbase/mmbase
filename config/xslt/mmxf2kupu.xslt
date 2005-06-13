<!--
  This translates mmbase XML, normally containing an objects tag. The XML related to this XSL is generated by
  org.mmbase.bridge.util.Generator, and the XSL is invoked by FormatterTag.

  @author:  Michiel Meeuwissen
  @version: $Id: mmxf2kupu.xslt,v 1.3 2005-06-13 17:07:54 michiel Exp $
  @since:   MMBase-1.6
-->
<xsl:stylesheet  
  xmlns:xsl ="http://www.w3.org/1999/XSL/Transform"
  xmlns:node ="org.mmbase.bridge.util.xml.NodeFunction"
  xmlns:o="http://www.mmbase.org/objects"
  xmlns:mmxf="http://www.mmbase.org/xmlns/mmxf"
  xmlns="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="node o mmxf"
  version = "1.1"
>
  <xsl:import href="mmxf2xhtml.xslt" />   <!-- dealing with mmxf is done there -->
  <xsl:import href="formatteddate.xslt" /><!-- dealing with dates is done there -->


  <xsl:param name="formatter_imgdb" />    <!-- this information is needed to correctly construct img.db urls -->

  <xsl:output method="xml" 
    omit-xml-declaration="yes" /><!-- xhtml is a form of xml -->


   <!-- If objects is the entrance to this XML, then only handle the root child of it -->
  <xsl:template match="o:objects">
    <xsl:apply-templates select="o:object[1]" />
  </xsl:template>

  <xsl:template match="mmxf:h" mode="h1"><xsl:if test=". != ''"><h1><xsl:apply-templates select="node()" /></h1></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h2"><xsl:if test=". != ''"><h2><xsl:apply-templates select="node()" /></h2></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h3"><xsl:if test=". != ''"><h3><xsl:apply-templates select="node()" /></h3></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h4"><xsl:if test=". != ''"><h4><xsl:apply-templates select="node()" /></h4></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h5"><xsl:if test=". != ''"><h5><xsl:apply-templates select="node()" /></h5></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h6"><xsl:if test=". != ''"><h6><xsl:apply-templates select="node()" /></h6></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h7"><xsl:if test=". != ''"><h7><xsl:apply-templates select="node()" /></h7></xsl:if></xsl:template>
  <xsl:template match="mmxf:h" mode="h8"><xsl:if test=". != ''"><h8><xsl:apply-templates select="node()" /></h8></xsl:if></xsl:template>



   <!-- how to present a node -->
   <xsl:template match="o:object">
     <body>
       <xsl:apply-templates select="o:field[@name='body']" />
     </body>     
  </xsl:template>

  <!--
  <xsl:template match="mmxf:p">
    <xsl:choose>
      <xsl:when test="mmxf:ol|mmxf:ul">
        <xsl:apply-templates select="text()|*" />
      </xsl:when>
      <xsl:otherwise>
        <p>
          <xsl:copy-of select="@*" />
          <xsl:apply-templates select="text()|*" />
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
-->

  <xsl:template match="o:field[@o:format='xml']">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match = "mmxf:mmxf" >
    <body>
      <xsl:apply-templates select = "mmxf:p|mmxf:table|mmxf:section" />
    </body>
  </xsl:template>


</xsl:stylesheet>
