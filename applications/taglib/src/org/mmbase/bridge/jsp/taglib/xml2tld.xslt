<?xml version="1.0"?>
<xsl:stylesheet id="xml2tld"  version="1.0"	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" indent="yes"/>
  
  <!-- main entry point -->
  <xsl:template match="taglib">
    <xsl:text disable-output-escaping="yes">
&lt;!DOCTYPE taglib  PUBLIC "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN" 
    "http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd"&gt;
</xsl:text>
<xsl:comment>
      This is the MMBase tag library descriptor (http://www.mmbase.org). This file was generated by
      `xml2tld.xslt' which can be called with `ant web' in org/mmbase/scripts.
      
      If you need to edit it, then it is prudent to edit mmbase-taglib.xml (by hand or other xmlt),
      and rerun the conversion.
</xsl:comment><xsl:text>
</xsl:text>
    <taglib>
      <xsl:apply-templates select="tlibversion | shortname | jspversion | uri | info" />
      <xsl:apply-templates select="tag" mode="base" />
    </taglib>
  </xsl:template>
  
  
  <xsl:template match="info">
    <info><xsl:value-of select="." /></info>
  </xsl:template>
  
  <xsl:template match="tag" mode="base" >
    <tag>
      <xsl:apply-templates select="name | tagclass | teiclass | bodycontent | info" />
      <xsl:apply-templates select="extends" />
      <xsl:apply-templates select="attribute"/> 
    </tag>
  </xsl:template>
  
  <xsl:template match="tag|taginterface" mode="extends">
    <xsl:apply-templates select="extends" />
    <xsl:apply-templates select="attribute"/> 
  </xsl:template>
  
  <xsl:template match="attribute">
    <attribute><xsl:apply-templates select="name | required | rtexprvalue" /></attribute>
  </xsl:template>
  
  <xsl:template match="extends">
    <xsl:variable name="e" select="." />
    <xsl:apply-templates select="/taglib/taginterface/name[.=$e]/parent::*" mode="extends" />
  </xsl:template>
  
  <xsl:template match="tlibversion|shortname|jspversion|uri|name|tagclass|teiclass|bodycontent|required|rtexprvalue">
    <xsl:copy-of select="." />
  </xsl:template>
  
</xsl:stylesheet>
