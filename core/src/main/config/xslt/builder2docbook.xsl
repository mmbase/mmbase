<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <!-- converts a file containing builder sources into docbook -->
  <!-- TODO: add info about types (string/field/etc) -->
  <!-- TODO: add fallback if language doesn't exist in builder.xml -->

  <xsl:output
    method="xml"
    version="1.0"
    encoding="utf-8"
    omit-xml-declaration="no"
    standalone="no"
    doctype-public="-//OASIS//DTD DocBook XML V4.1.2//EN"
    doctype-system="https://www.oasis-open.org/docbook/xml/4.0/docbookx.dtd"
    indent="yes"
    />

  <!-- include file needed for localization -->
  <xsl:include href="common/l10n.xsl"/>

  <!-- default language-setting -->
  <xsl:param name="language" select="'en'"/>

  <!-- general -->
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- rootnode builders -->
  <xsl:template match="builders">
    <!-- only one article needed for all builders -->
    <article>
      <articleinfo>
        <title>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key">Documentation.title</xsl:with-param>
          </xsl:call-template>
        </title>
      </articleinfo>
      <xsl:apply-templates>
        <!-- sort on buildertype-name -->
        <xsl:sort select="@name"/>
      </xsl:apply-templates>
    </article>
  </xsl:template>

  <!-- one section per buildertype -->
  <xsl:template match="buildertype">
    <section id="buildertype-{@name}">
      <title><xsl:value-of select="@name"/></title>
      <xsl:apply-templates>
        <!-- sort on builder-name -->
        <xsl:sort select="@name"/>
      </xsl:apply-templates>
    </section>
  </xsl:template>

  <!-- builder -->
  <xsl:template match="builder">
    <!-- buildername is needed more than once
         and inside parameters, so lets put it into variable -->
    <xsl:variable name="buildername">
      <xsl:value-of select="@name"/>
    </xsl:variable>
    <!-- one section per builder -->
    <section id="{$buildername}">
      <!-- display descriptive name and buildername,
           this way buildername will also be displayed in indexes
           and it will be easier to find the builder you need :-) -->
      <title>
        <xsl:value-of select="names/plural[@xml:lang=$language]"/>
        (<xsl:value-of select="$buildername"/>)
      </title>

      <!-- buildername is used in this id to make it a unique id -->
      <section id="general-{$buildername}">
        <title>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key">General</xsl:with-param>
          </xsl:call-template>
        </title>
        <para>
          <xsl:value-of select="descriptions/description[@xml:lang=$language]"/>
        </para>
        <para>
          <simplelist type="horiz" columns="2">
            <member>
              <xsl:call-template name="gentext">
                <xsl:with-param name="key">version</xsl:with-param>
              </xsl:call-template>
            </member>
            <member><xsl:value-of select="@version"/></member>
            <member>
              <xsl:call-template name="gentext">
                <xsl:with-param name="key">maintainer</xsl:with-param>
              </xsl:call-template>
            </member>
            <member><xsl:value-of select="@maintainer"/></member>
            <xsl:variable name="extends">
              <xsl:value-of select="@extends"/>
            </xsl:variable>
            <xsl:if test="not($extends='')">
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">extends</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <link linkend="{$extends}">
                  <xsl:value-of select="$extends"/>
                </link>
              </member>
            </xsl:if>
            <member>
              <xsl:call-template name="gentext">
                <xsl:with-param name="key">descriptive.name</xsl:with-param>
              </xsl:call-template>
            </member>
            <member><xsl:value-of select="names/singular[@xml:lang=$language]"/></member>
            <member>
              <xsl:call-template name="gentext">
                <xsl:with-param name="key">table.name</xsl:with-param>
              </xsl:call-template>
            </member>
            <member><xsl:value-of select="$buildername"/></member>
          </simplelist>
        </para>
      </section>

      <xsl:apply-templates select="fieldlist"/>

    </section>
  </xsl:template>


  <!-- fields -->
  <xsl:template match="fieldlist">

    <xsl:variable name="buildername">
      <xsl:value-of select="../@name"/>
    </xsl:variable>

    <!-- buildername is used in this id to make it a unique id -->
    <section id="fields-{$buildername}">
      <title>
        <xsl:call-template name="gentext">
          <xsl:with-param name="key">fields</xsl:with-param>
        </xsl:call-template>
      </title>
      <xsl:for-each select="field">
        <formalpara>
          <title><xsl:value-of select="gui/guiname[@xml:lang=$language]"/></title>
          <para>
            <simplelist type="horiz" columns="2">
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">databasename</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="db/name"/>
              </member>
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">description</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="descriptions/description[@xml:lang=$language]"/>
              </member>
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">guitype</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="gui/guitype"/>
              </member>
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">databasetype</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="db/type"/>
              </member>
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">notnull</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="db/type/@notnull"/>
              </member>
              <member>
                <xsl:call-template name="gentext">
                  <xsl:with-param name="key">size</xsl:with-param>
                </xsl:call-template>
              </member>
              <member>
                <xsl:value-of select="db/type/@size"/>
              </member>
            </simplelist>
          </para>
        </formalpara>
      </xsl:for-each>
    </section>
  </xsl:template>

</xsl:stylesheet>


