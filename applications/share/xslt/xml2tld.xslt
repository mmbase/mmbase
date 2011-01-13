<?xml version="1.0"?>
<xsl:stylesheet
  id="xml2tld"
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  >

  <!-- Tested with:
       ~/mmbase/head/applications$ java org.apache.xalan.xslt.Process -IN taglib/src/org/mmbase/bridge/jsp/taglib/mmbase-taglib.xml -XSL share/xslt/xml2tld.xslt -PARAM version 2.0 | less
   -->


  <xsl:param name="version">1.2</xsl:param>
  <xsl:param name="uri">http://www.mmbase.org/mmbase-taglib-1.0</xsl:param>
  <xsl:variable name="nl"><xsl:text>
</xsl:text></xsl:variable>
  <xsl:output
    omit-xml-declaration="no"
    method="xml"
    indent="yes" />



  <!-- main entry point
       1.1: (tlibversion, jspversion?, shortname, uri?, info?, tag+) >
       2.0: <description>*, <display-name>?, <icon>?, <tlib-version>, <short-name>, <uri>?, <validator>?, <listener>*, <tag>*, <tag-file>*, <function>*, <taglib-extension>*)
  -->
  <xsl:template match="taglib">
    <xsl:choose>
      <xsl:when test="$version &lt; 2.0">
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE taglib  PUBLIC "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN"
        "http://java.sun.com/j2ee/dtds/web-jsptaglibrary_1_1.dtd"&gt;
</xsl:text>
        <taglib >
          <xsl:call-template name="taglib"  />
        </taglib>
      </xsl:when>
      <xsl:otherwise>
        <taglib xmlns="http://java.sun.com/xml/ns/j2ee"                >
          <xsl:attribute name="version">2.0</xsl:attribute>
          <xsl:attribute namespace="http://www.w3.org/2001/XMLSchema-instance" name="schemaLocation">http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd</xsl:attribute>
          <xsl:call-template name="taglib"  />
        </taglib>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="taglib">
      <xsl:comment>
        This is the MMBase tag library descriptor (http://www.mmbase.org). This file is automaticly generated by
        xml2tld.xslt.
        With params:
          version=<xsl:value-of select="$version" />
          uri    =<xsl:value-of select="$uri" />

      </xsl:comment>
      <xsl:if test="$version &gt;= 2.0">
        <xsl:apply-templates select="description | display-name | icon" />
      </xsl:if>

      <xsl:apply-templates select="tlib-version | tlibversion | jspversion" />
      <xsl:if test="not(jspversion) and $version &lt; 2.0">
        <jspversion><xsl:value-of select="$version" /></jspversion>
      </xsl:if>
      <xsl:apply-templates select="shortname | short-name | uri" />
      <xsl:if test="not(uri)">
        <xsl:if test="$version &lt; 2.0">
          <uri><xsl:value-of select="$uri" /></uri>
        </xsl:if>
        <xsl:if test="$version &gt;= 2.0">
          <uri xmlns="http://java.sun.com/xml/ns/j2ee"><xsl:value-of select="$uri" /></uri>
        </xsl:if>
      </xsl:if>
      <xsl:if test="$version &lt; 2.0">
        <xsl:apply-templates select="info"/>
      </xsl:if>
      <xsl:apply-templates select="tag" mode="base" />
      <xsl:if test="$version = '2.0'">
        <!--
            See MMB-1348
            It seems that the present tag-file entries cause the problem.
        -->
        <xsl:apply-templates select="tag-file" />
        <xsl:apply-templates select="function" mode="base" />
      </xsl:if>
  </xsl:template>

  <xsl:template match="info">
    <xsl:apply-templates select="p|text()" />
    <xsl:apply-templates select="since" />
  </xsl:template>

  <xsl:template match="since">
    <xsl:text><![CDATA[<p><em>since</em>  ]]></xsl:text>
    <xsl:value-of select="text()" />
    <xsl:text><![CDATA[</p>]]></xsl:text>
  </xsl:template>

  <xsl:template match="tlibversion">
    <tlib-version><xsl:value-of select="." /></tlib-version>
  </xsl:template>
  <xsl:template match="jspversion">
    <xsl:if test="$version &lt; '2.0'">
      <jspversion><xsl:value-of select="." /></jspversion>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tag" mode="base" >
    <xsl:if test="$version &lt; 2.0">
      <tag>
        <xsl:apply-templates select="name | tagclass | tag-class | teiclass | tei-class | bodycontent | body-content" />
        <xsl:apply-templates select="attribute"/>
        <xsl:apply-templates select="extends" />
      </tag>
    </xsl:if>
    <xsl:if test="$version &gt;= 2.0">
      <tag xmlns="http://java.sun.com/xml/ns/j2ee">
        <xsl:apply-templates select="name | tagclass | tag-class | teiclass | tei-class | bodycontent | body-content" />
        <xsl:if test="not(bodycontent) and not(body-content)">
          <body-content>JSP</body-content>
        </xsl:if>
        <xsl:call-template name="description" />
        <xsl:call-template name="examples" />
        <xsl:apply-templates select="attribute"/>
      </tag>
    </xsl:if>
  </xsl:template>

  <xsl:template name="description">
    <description>
      <xsl:text><![CDATA[<a name="]]></xsl:text>
      <xsl:value-of select="local-name()" />
      <xsl:text>-</xsl:text>
      <xsl:value-of select="name" />
      <xsl:text><![CDATA["</a>]]></xsl:text>
      <xsl:apply-templates select="info|description" />
      <xsl:apply-templates select="see" />
      <xsl:call-template name="showextends" />
      <xsl:call-template name="showtype" />
      <xsl:call-template name="values" />
      <xsl:apply-templates select="since" />
    </description>
  </xsl:template>

  <xsl:template name="examples">
    <example>
      <xsl:apply-templates select="example" />
    </example>
  </xsl:template>

  <xsl:template match="example">
    <xsl:if test="last() &gt; 1">
      <xsl:text>----Example </xsl:text><xsl:value-of select="position()" />
      <xsl:value-of select="$nl" />
    </xsl:if>
    <xsl:value-of select="text()" />
    <xsl:for-each select="include">
      <xsl:text>Can be found in </xsl:text>
      <xsl:value-of select="@href" />
      <xsl:text> (</xsl:text><xsl:value-of select="@base" /><xsl:text>)</xsl:text>
    </xsl:for-each>
    <xsl:if test="last() &gt; 1">
      <xsl:value-of select="$nl" />
      <xsl:value-of select="$nl" />
    </xsl:if>
  </xsl:template>

  <xsl:template match="see">
    <xsl:if test="position() = 1">
      <xsl:text><![CDATA[<h3>See also</h3><p>]]></xsl:text>
    </xsl:if>
    <xsl:variable name="seetag"><xsl:value-of select="@tag" /></xsl:variable>
    <xsl:choose>
      <xsl:when test="//taglib/*[(local-name() = 'tag' or local-name() = 'function') and name = $seetag]">
        <xsl:if test="@attribute">
          <xsl:value-of select="@attribute" />
          <xsl:text> attribute of </xsl:text>
        </xsl:if>
        <xsl:text><![CDATA[<a href="]]></xsl:text>
        <xsl:value-of select="@tag" />
        <xsl:text>.html</xsl:text>
        <xsl:if test="@attribute">
          <xsl:text>#attribute-</xsl:text>
          <xsl:value-of select="@attribute" />
        </xsl:if>
        <xsl:text>">mm:</xsl:text>
        <xsl:value-of select="@tag" />
        <xsl:text><![CDATA[</a>]]></xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@tag" />
      </xsl:otherwise>
    </xsl:choose>
      <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
    <xsl:if test="position() = last()"><![CDATA[</p>]]></xsl:if>
  </xsl:template>

  <xsl:template name="showextends">
    <xsl:variable name="thistag"><xsl:value-of select="name" /></xsl:variable>
    <xsl:for-each select="extends">
      <xsl:variable name="seetag"><xsl:value-of select="text()" /></xsl:variable>
      <xsl:if test="//taglib/*[(local-name() = 'tag' or local-name() = 'function') and name = $seetag]">
        <xsl:text><![CDATA[Extends <a href="]]></xsl:text>
        <xsl:value-of select="text()" />
        <xsl:text><![CDATA[.html">]]></xsl:text>
        <xsl:value-of select="text()" />
        <xsl:text><![CDATA[</a><br /]]></xsl:text>
      </xsl:if>
      <xsl:for-each select="//taglib/*[(local-name() = 'taginterface') and name = $seetag]">
        <xsl:text><![CDATA[<h3>This is a ']]></xsl:text>
        <xsl:value-of select="name" />
        <xsl:text><![CDATA[' tag</h3>]]></xsl:text>
        <xsl:apply-templates select="info" />
        <xsl:text><![CDATA[<p>Other tags like this: ]]></xsl:text>
        <xsl:apply-templates select="//taglib/tag[extends = $seetag and name != $thistag]" mode="othertagoftype">
          <xsl:sort select="name" />
        </xsl:apply-templates>
        <xsl:text><![CDATA[</p>]]></xsl:text>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="tag" mode="othertagoftype">
    <xsl:text><![CDATA[<a href="]]></xsl:text>
    <xsl:value-of select="name" />
    <xsl:text>.html"&gt;</xsl:text>
    <xsl:value-of select="name" />
    <xsl:text><![CDATA[</a>]]></xsl:text>
    <xsl:if test="position() != last()">, </xsl:if>
  </xsl:template>

  <xsl:template name="showtype">
    <xsl:for-each select="type">
      <xsl:variable name="seetype"><xsl:value-of select="text()" /></xsl:variable>
      <xsl:for-each select="//taglib/tagtypes/type[@name = $seetype]">
        <xsl:text><![CDATA[<h3>Part of ']]></xsl:text>
        <xsl:value-of select="description" />
        <xsl:text><![CDATA['</h3>]]></xsl:text>
        <xsl:apply-templates select="info" />
      </xsl:for-each>
    </xsl:for-each>

  </xsl:template>

  <xsl:template name="values">
    <xsl:for-each select="possiblevalue|examplevalue">
      <xsl:if test="position() = 1">
        <xsl:text><![CDATA[<p>values<p><ul>]]></xsl:text>
      </xsl:if>
      <xsl:text><![CDATA[<li><em>]]></xsl:text>
      <xsl:value-of select="value" />
      <xsl:text><![CDATA[</em>  ]]></xsl:text>
      <xsl:apply-templates select="info" />
      <xsl:if test="local-name() = 'examplevalue'">
        <xsl:text><![CDATA[<em> (example)</em>]]></xsl:text>
      </xsl:if>
      <xsl:text><![CDATA[</li>]]></xsl:text>
    </xsl:for-each>
    <xsl:if test="position() = last()">
      <xsl:text><![CDATA[</ul>]]></xsl:text>
    </xsl:if>
  </xsl:template>


  <xsl:template match="function" mode="base" >
    <function xmlns="http://java.sun.com/xml/ns/j2ee">
      <xsl:apply-templates select="name | description | function-class | function-signature" />
      <xsl:apply-templates select="attribute"/>
      <xsl:apply-templates select="extends" />
    </function>
  </xsl:template>

  <xsl:template match="tag|taginterface" mode="extends">
    <xsl:apply-templates select="attribute"/>
    <xsl:apply-templates select="extends" />
  </xsl:template>

  <xsl:template match="attribute">
    <xsl:if test="$version &lt; 2.0">
      <attribute><xsl:apply-templates select="name | required | rtexprvalue" /></attribute>
    </xsl:if>
    <xsl:if test="$version &gt;= 2.0">
      <attribute xmlns="http://java.sun.com/xml/ns/j2ee">
        <xsl:apply-templates select="name | required | rtexprvalue" />
        <xsl:call-template name="description" />
      </attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="extends">
    <xsl:apply-templates select="/taglib/*[name()='tag' or name()='taginterface']/name[.=current()]/parent::*" mode="extends" />
  </xsl:template>

  <xsl:template match="shortname|short-name">
    <xsl:choose>
      <xsl:when test="$version &gt;= 2.0">
        <short-name xmlns="http://java.sun.com/xml/ns/j2ee"><xsl:value-of select="." /></short-name>
      </xsl:when>
      <xsl:otherwise>
        <shortname><xsl:value-of select="." /></shortname>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="tlibversion|tlib-version">
    <xsl:choose>
      <xsl:when test="$version &gt;= 2.0">
        <tlib-version xmlns="http://java.sun.com/xml/ns/j2ee"><xsl:value-of select="." /></tlib-version>
      </xsl:when>
      <xsl:otherwise>
        <tlibversion><xsl:value-of select="." /></tlibversion>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="tagclass|tag-class">
    <xsl:choose>
      <xsl:when test="$version &gt;= 2.0">
        <tag-class xmlns="http://java.sun.com/xml/ns/j2ee"><xsl:value-of select="." /></tag-class>
      </xsl:when>
      <xsl:otherwise>
        <tagclass><xsl:value-of select="." /></tagclass>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="teiclass|tei-class">
    <xsl:choose>
      <xsl:when test="$version &gt;= 2.0">
        <tei-class xmlns="http://java.sun.com/xml/ns/j2ee"><xsl:value-of select="." /></tei-class>
      </xsl:when>
      <xsl:otherwise>
        <teiclass><xsl:value-of select="." /></teiclass>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="bodycontent|body-content">
    <xsl:choose>
      <xsl:when test="$version &gt;= 2.0">
        <body-content xmlns="http://java.sun.com/xml/ns/j2ee">
        <xsl:value-of select="." />
        </body-content>
      </xsl:when>
      <xsl:otherwise>
        <bodycontent><xsl:value-of select="." /></bodycontent>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="tag-file">
    <tag-file xmlns="http://java.sun.com/xml/ns/j2ee">
      <xsl:apply-templates select="name|path" />
    </tag-file>
  </xsl:template>
  <xsl:template match="description|display-name|icon|uri|name|required|rtexprvalue|function-class|function-signature|path">
    <xsl:copy-of select="." />
  </xsl:template>

</xsl:stylesheet>
