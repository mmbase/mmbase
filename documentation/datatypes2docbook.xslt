<?xml version="1.0"?>
<xsl:stylesheet id="xml2html"
  version="1.0"
  xmlns="http://docbook.org/ns/docbook"
  xmlns:dt="https://www.mmbase.org/xmlns/datatypes"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="dt"
  >

  <xsl:output method="xml"
    version="1.0"
    omit-xml-declaration="yes"
    indent="no"
    />

  <xsl:template match="/dt:datatypes">
    <xsl:apply-templates select="dt:datatype" />
  </xsl:template>

  <xsl:template match="dt:datatype">
    <section id="section_{@id}">
      <title><xsl:value-of select="@id" /></title>
      <para><xsl:value-of select="dt:description[@xml:lang = 'en']" /></para>
      <xsl:apply-templates select="dt:class" />
      <variablelist>
        <xsl:apply-templates select="dt:datatype|dt:specialization" mode="sub" />
      </variablelist>
    </section>
  </xsl:template>

  <xsl:template match="dt:class">
    <para>
      Class:
      <xsl:apply-templates select="." mode="ulink" />
    </para>
  </xsl:template>

  <xsl:template match="dt:class" mode="ulink">
    <ulink url="http://www.mmbase.org/api/{translate(@name, '.', '/')}.html">
      <xsl:value-of select="@name" />
    </ulink>
  </xsl:template>


  <xsl:template match="dt:setprocessor">
    <para>
      A set-processor is defined  <xsl:apply-templates select="." mode="type" />
      <xsl:for-each select="dt:class">
        <xsl:apply-templates select="." mode="ulink" />
        <xsl:if test="position() != last()">, </xsl:if>
      </xsl:for-each>
    </para>
  </xsl:template>

  <xsl:template match="dt:getprocessor">
    <para>
      A get-processor is defined <xsl:apply-templates select="." mode="type" />
      <xsl:for-each select="dt:class">
        <xsl:apply-templates select="." mode="ulink" />
        <xsl:if test="position() != last()">, </xsl:if>
      </xsl:for-each>
    </para>
  </xsl:template>

  <xsl:template match="dt:commitprocessor">
    <para>
      A commit-processor is defined  <xsl:apply-templates select="." mode="type" />
      <xsl:for-each select="dt:class">
        <xsl:apply-templates select="." mode="ulink" />
        <xsl:if test="position() != last()">, </xsl:if>
      </xsl:for-each>
    </para>
  </xsl:template>

  <xsl:template match="dt:*" mode="type">
    <xsl:choose>
      <xsl:when test="not(@type) or @type = '' or @type = '*'">
        for all types
      </xsl:when>
      <xsl:otherwise>
        for type <xsl:value-of select="@type" />:
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="dt:datatype|dt:specialization" mode="sub">
      <varlistentry>
        <term><xsl:value-of select="@id" /></term>
        <listitem>
          <para><xsl:value-of select="dt:description[@xml:lang = 'en']" /></para>
          <xsl:apply-templates select="dt:class" />
          <xsl:if test="dt:default">
            <para>Default: <xsl:value-of select="dt:default/@value" /></para>
          </xsl:if>
          <xsl:if test="dt:minInclusive">
            <para>Minimal value (inclusive): <xsl:value-of select="dt:minInclusive/@value" /></para>
          </xsl:if>
          <xsl:if test="dt:minExclusive">
            <para>Minimal value (exclusive): <xsl:value-of select="dt:minExclusive/@value" /></para>
          </xsl:if>
          <xsl:if test="dt:maxInclusive">
            <para>Maximal value (inclusive): <xsl:value-of select="dt:maxInclusive/@value" /></para>
          </xsl:if>
          <xsl:if test="dt:maxExclusive">
            <para>Maximal value (exclusive): <xsl:value-of select="dt:maxExclusive/@value" /></para>
          </xsl:if>
          <xsl:if test="dt:password/@value = 'true'">
            <para>This type is designated for passwords</para>
          </xsl:if>
          <xsl:if test="dt:pattern">
            <para>
              Patterns are assigned. 
              <xsl:choose>
                <xsl:when test="dt:pattern[@xml:lang = 'en-US']">
                  Example pattern (for english): 
                  <xsl:value-of select="dt:pattern[@xml:lang = 'en-US']/@value" />
                </xsl:when>
                <xsl:when test="dt:pattern[@xml:lang = 'en']">
                  Example pattern (for english): 
                  <xsl:value-of select="dt:pattern[@xml:lang = 'en']/@value" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="dt:pattern/@value" />
                </xsl:otherwise>
              </xsl:choose>
            </para>
          </xsl:if>
          <xsl:if test="dt:enumeration">
            <para>
              This is an enumeration.
            </para>
          </xsl:if>
          <xsl:apply-templates select="dt:setprocessor|dt:getprocessor|dt:commitprocessor" />
        </listitem>
      </varlistentry>
      <xsl:apply-templates select="dt:datatype|dt:specialization" mode="sub" />
  </xsl:template>
</xsl:stylesheet>

