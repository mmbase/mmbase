<?xml version="1.0"?>
<xsl:stylesheet id="xml2html"
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan/redirect"
  xmlns:xi="http://www.w3.org/2001/XInclude"
  xmlns:rf="org.mmbase.bridge.jsp.taglib.util.ReadFile"
  extension-element-prefixes="xalan rf"
  xmlns:mm="mmbase-taglib"
  >

  <!--
     xalan extension used for xalan:write.

       According to xslt 1.1 you could simply use xsl:document, but xalan does not
       support this. Therefore in this stylesheet xalan:write is used inside each other. This seems
       to work with xsltproc too.
   -->

  <xsl:output method="html"
    version="1.0"
    encoding="UTF-8"
    omit-xml-declaration="yes"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    indent="yes"
    />

  <xsl:param name="basedir"></xsl:param>
  <xsl:param name="exampledir"></xsl:param>
  <xsl:param name="files">reference</xsl:param>
  <xsl:variable name="basedirfiles"><xsl:value-of select="$basedir" />/<xsl:value-of select="$files" /></xsl:variable><!-- make sure this directory exists! -->


  <!-- some configuration -->
  <xsl:variable name="extendscolor">blue</xsl:variable>
  <xsl:variable name="attrcolor">green</xsl:variable>
  <xsl:variable name="reqcolor">red</xsl:variable>

  <xsl:template name="directories">
    <xsl:param name="dir" />
    <xsl:choose>
      <xsl:when test="$dir='examples.taglib'">/mmexamples/taglib</xsl:when>
    </xsl:choose>
  </xsl:template>


  <!-- main entry point -->
  <xsl:template match="taglib">
    <xsl:apply-templates select="tagtypes/type" />

    <!-- create a seperate file for every tag -->
    <xsl:apply-templates select="tag-file|tag|taginterface|function" mode="file" />

    <!-- create a toc file -->
    <!-- xsl:document href="{$basedir}/index.html"-->
    <xalan:write file="{$basedir}/index.html"><!-- xsl:document not supported by xalan.. Sigh..-->
      <html>
        <head>
          <title>MMBase taglib - Table Of Contents</title>
        </head>
        <body bgcolor="#FFFFFF" text="#336699" link="#336699" vlink="#336699" alink="#336699">
          <h1>MMBase taglib <xsl:value-of select="/taglib/tlibversion" /> documentation</h1>
          <xsl:apply-templates select="info" />
          <xsl:for-each select="/taglib/tagtypes/type">
            <a href="{$files}/mmbase-taglib-{@name}.html"><xsl:value-of select="description" /></a><br />
          </xsl:for-each>
          <hr />
          All 'tags' in seperate files (these are small jsp's, probably will be possible to add working examples): <br />
          groups: <br />
          <xsl:apply-templates select="taginterface" mode="toc" >
            <xsl:sort select="name" />
            <xsl:with-param name="file"  select="true()" />
            <xsl:with-param name="subdir"  select="true()" />
          </xsl:apply-templates>
          <br />
          tags: <br />
          <xsl:apply-templates select="tag|tag-file" mode="toc" >
            <xsl:sort select="name" />
            <xsl:with-param name="file"  select="true()" />
            <xsl:with-param name="subdir"  select="true()" />
          </xsl:apply-templates>
          <br />
          functions: <br />
          <xsl:apply-templates select="function" mode="toc" >
            <xsl:sort select="name" />
            <xsl:with-param name="file"  select="true()" />
            <xsl:with-param name="subdir"  select="true()" />
          </xsl:apply-templates>
          <br />
        </body>
      </html>
    </xalan:write>
    <!-- /xsl:document-->
  </xsl:template>

  <!-- The several 'types' of tags -->
  <xsl:template match="type">

    <!-- for backwards compatibility, the 'all' overview is written to stdout -->
    <xsl:if test="@name='all'">
      <xsl:apply-templates select="/taglib" mode="main">
        <xsl:with-param name="info" select="/taglib/info|info" />
        <xsl:with-param name="type" select="@name" />
        <xsl:with-param name="linkexamples" select="false()" />
      </xsl:apply-templates>
    </xsl:if>
    <!-- and overview files are generated for every type -->
    <!-- xsl:document href="{$basedirfiles}/mmbase-taglib-{@name}.html"-->
    <xalan:write file="{$basedirfiles}/mmbase-taglib-{@name}.html">
      <xsl:apply-templates select="/taglib" mode="main">
        <xsl:with-param name="info" select="info" />
        <xsl:with-param name="type" select="@name" />
      </xsl:apply-templates>
    </xalan:write>
    <!-- /xsl:document-->
  </xsl:template>

  <!-- Generate a list of tag documentation, with toc and info on top -->
  <xsl:template match="taglib" mode="main">
    <xsl:param name="info" />
    <xsl:param name="type" select="'all'" />
    <xsl:param name="linkexamples" select="true()" />
    <html>
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>MMBase taglib <xsl:value-of select="tlibversion" /> documentation</title>
        <xsl:if test="@author"><meta name="Author" value="{@author}"/></xsl:if>
      </head>
      <body
          bgcolor="#FFFFFF" text="#336699" link="#336699" vlink="#336699" alink="#336699">
        <h1>MMBase taglib <xsl:value-of select="tlibversion" /> documentation</h1>
        <xsl:if test="not($type='all')"><h2><xsl:value-of select="tagtypes/type[@name=$type]/description" /></h2></xsl:if>
        <table width="100%" cellpadding="5">
          <tr>
            <td width="30"></td>
            <td>
              <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </td>
            <td width="30"></td>
          </tr>
          <tr>
            <td></td>
            <td><xsl:apply-templates select="$info/*"/></td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <a name="toc"/>
              <xsl:apply-templates select="taginterface[contains(type, $type) or $type='all']" mode="toc" >
                <xsl:sort select="name" />
                </xsl:apply-templates><br />
            </td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <a name="toc"/>
              <xsl:apply-templates select="tag[contains(type, $type) or $type='all']|function[$type='all']" mode="toc" >
                <xsl:sort select="name" />
                </xsl:apply-templates><br />
            </td>
            <td></td>
          </tr>
          <xsl:if test="not($type='all')">
            <tr><td /><td><a href="../index.html">complete table of contents</a></td></tr>
          </xsl:if>
          <tr>
            <td></td>
            <td>
              <a href="#docinfo">info about the syntax of this document</a>
            </td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <xsl:apply-templates select="taginterface[contains(type, $type) or $type='all']" mode="full">
                <xsl:with-param name="type" select="$type" />
                <xsl:sort select="name" />
              </xsl:apply-templates>
              <xsl:apply-templates select="tag-file[contains(type, $type) or $type='all']|tag[contains(type, $type) or $type='all']|function[$type='all']" mode="full">
                <xsl:with-param name="type" select="$type" />
                <xsl:with-param name="linkexamples" select="$linkexamples" />
                <xsl:sort select="name" />
              </xsl:apply-templates>
            </td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </td>
            <td></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <a name="docinfo"/>
              <p>
                This document lists the current tags implemented for MMBase (version <xsl:value-of
                select="/taglib/tlibversion" />)
              </p>
              <p>
                Attributes in <font color="{$reqcolor}"><xsl:value-of select="$reqcolor" /></font> are
                required.
              </p>
              <p>
                <font color="{$extendscolor}"><xsl:value-of select="$extendscolor" /></font> entries
                are no tags, but describe a group of tags. Tags can belong to several groups.
              </p>
              <p>
                If a tag definition contains a body section this means that the
                tag might do something with the content of the body.
              </p>
            </td>
            <td></td>
          </tr>
        </table>
      </body>
    </html>
  </xsl:template>


  <!-- To generate the right URL to a certain tag documentation it must be determined if this is not in the same document.
       If not, than the link must be to the seperate file. If so, then simply can be linked with #
       This template only generates the URL.
  -->
  <xsl:template name="tagref">
    <xsl:param name="file" select="false()" />
    <xsl:param name="subdir" select="false()" />
    <xsl:param name="type" select="'all'" />
    <xsl:param name="tag"/>
    <xsl:param name="attribute" select="''" />
    <xsl:param name="anchor"    select="''" />
    <xsl:variable name="usefile" select="$file or (not($type='all') and not(/taglib/*[name = $tag]/type and contains(/taglib/*[name = $tag]/type, $type)))" />
    <xsl:if test="$usefile"><xsl:if test="$subdir"><xsl:value-of select="$files" />/</xsl:if><xsl:value-of select="$tag" />.jsp</xsl:if>
    <xsl:if test="not($attribute='') or not($usefile)">#<xsl:value-of select="$tag" /></xsl:if>
    <xsl:if test="not($anchor='')">#<xsl:value-of select="$anchor" /></xsl:if>
    <xsl:if test="$attribute">.<xsl:value-of select="$attribute" /></xsl:if>
  </xsl:template>
  <xsl:template name="functionref">
    <xsl:param name="file" select="false()" />
    <xsl:param name="subdir" select="false()" />
    <xsl:param name="type" select="'all'" />
    <xsl:param name="function"/>
    <xsl:variable name="usefile" select="$file or (not($type='all') and not(/taglib/function[name = $function]/type and contains(/taglib/function[name = $function]/type, $type)))" />
    <xsl:if test="$usefile"><xsl:if test="$subdir"><xsl:value-of select="$files" />/</xsl:if>function_<xsl:value-of select="$function" />.jsp</xsl:if>
    <xsl:if test="not($usefile)">#function_<xsl:value-of select="$function" /></xsl:if>
  </xsl:template>


  <!-- Generates an entry for table of all tags or taginterfaces -->
  <xsl:template match="tag-file|tag|taginterface|function" mode="toc">
    <xsl:param name="file" select="false()" />
    <xsl:param name="subdir" select="false()" />
    <a>
      <xsl:attribute name="href">
        <xsl:choose>
          <xsl:when test="name()='function'">
            <xsl:call-template name="functionref">
              <xsl:with-param name="file" select="$file" /><xsl:with-param name="subdir" select="$subdir" /><xsl:with-param name="function"  select="name"  />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="tagref">
              <xsl:with-param name="file" select="$file" /><xsl:with-param name="subdir" select="$subdir" /><xsl:with-param name="tag"  select="name"  />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>

      </xsl:attribute>
      <xsl:if test="name()='taginterface'"><font color="{$extendscolor}"><xsl:value-of select="name" /></font></xsl:if>
      <xsl:if test="name()='tag'"><xsl:value-of select="name" /></xsl:if>
      <xsl:if test="name()='tag-file'"><xsl:value-of select="name" /></xsl:if>
      <xsl:if test="name()='function'"><xsl:text disable-output-escaping="yes">&amp;#x24;</xsl:text>{<xsl:value-of select="name" />}</xsl:if>
    </a>
    <xsl:if test="since='MMBase-1.9'">(new)</xsl:if>
    <xsl:if test="position() != last()"> | </xsl:if>
  </xsl:template>

  <!-- -->
  <xsl:template match="tag-file|tag|taginterface" mode="tocext">
    <xsl:param name="testlast" select="false()" />
    <xsl:param name="file" select="false()" />
    <xsl:param name="type" select="'all'" />
    <xsl:apply-templates select="/taglib/*[name()='tag' or name()='taginterface']/extends[.=current()/name]/parent::*" mode="tocext">
      <xsl:with-param name="file" select="$file" />
      <xsl:with-param name="type" select="$type" />
    </xsl:apply-templates>
    <xsl:if test="name()='tag' or name()='tag-file'">
      <a>
        <xsl:attribute name="href">
          <xsl:call-template name="tagref">
            <xsl:with-param name="file" select="$file" />
            <xsl:with-param name="type"  select="$type"  />
            <xsl:with-param name="tag"  select="name"  />
          </xsl:call-template>
        </xsl:attribute>
        <xsl:value-of select="name" />
      </a>
      <xsl:if test="(position() != last()) or not($testlast)"> | </xsl:if>
    </xsl:if>
  </xsl:template>

  <!-- Tags can refer to other tags (and attributes) with a 'see' link. This template handles them. -->
  <xsl:template match="see">
    <xsl:param name="file" select="false()" />
    <xsl:param name="type" select="'all'" />
    <xsl:if test="@type">
      <a>
        <xsl:attribute name="href">mmbase-taglib-<xsl:value-of select="@type" />.html</xsl:attribute>
        <xsl:value-of select="/taglib/tagtypes/type[@name=current()/@type]/description" />
      </a>
    </xsl:if>
    <xsl:if test="not(@type)">
      <a>
        <xsl:attribute name="href">
          <xsl:choose>
            <xsl:when test="@function">
              <xsl:call-template name="functionref">
                <xsl:with-param name="file" select="$file" />
                <xsl:with-param name="type" select="$type" />
                <xsl:with-param name="function"  select="@function"  />
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="tagref">
                <xsl:with-param name="file" select="$file" />
                <xsl:with-param name="type" select="$type" />
                <xsl:with-param name="tag"  select="@tag"  />
                <xsl:with-param name="attribute"  select="@attribute" />
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:if test="@attribute"><xsl:value-of select="@attribute" /> attribute of </xsl:if>
        <xsl:value-of select="@tag" />
        <xsl:value-of select="@function" />

      </a>
    </xsl:if>
    <xsl:if test="position() != last()"> | </xsl:if>
  </xsl:template>

  <!-- Create a file for a tag -->
  <xsl:template match="tag-file|tag|taginterface|function" mode="file">
    <xsl:variable name="filename">
      <xsl:choose>
        <xsl:when test="name()='function'">
          <xsl:text>function_</xsl:text><xsl:value-of select="name" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="name" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xalan:write file="{$basedirfiles}/{$filename}.jsp"><!-- xsl:document not supported by xalan-->
      <html>
        <head>
          <title>MMBase taglib - mm:<xsl:value-of select="name" /></title>
          <xsl:text disable-output-escaping="yes">
            &lt;%@page language="java" contentType="text/html; charset=UTF-8" %&gt;
            &lt;%@taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm" %&gt;
          </xsl:text>
        </head>
        <body
            bgcolor="#FFFFFF" text="#336699" link="#336699" vlink="#336699" alink="#336699">
          <xsl:text disable-output-escaping="yes">
            &lt;mm:cloud&gt;
            &lt;mm:node jspvar="node" number="a.news.node" notfound="providenull"&gt;
          </xsl:text>
          <h1><xsl:value-of select="name" /></h1>
          <xsl:apply-templates select="." mode="full">
            <xsl:with-param name="file" select="true()" />
          </xsl:apply-templates>
          <xsl:text disable-output-escaping="yes">
            &lt;/mm:node&gt;
            &lt;/mm:cloud&gt;
          </xsl:text>
        </body>
      </html>
    </xalan:write>
    <!-- /xsl:document-->
  </xsl:template>

  <!-- A description for one tag -->
  <xsl:template match="tag-file|tag|taginterface" mode="full">
    <xsl:param name="file" select="false()" /><!-- if true, reference to files -->
    <xsl:param name="type" select="'all'" /><!-- if refering to tag of other type, must also reference to file -->
    <xsl:param name="linkexamples" select="true()" />
    <table bgcolor="#eeeeee" width="100%" cellpadding="5">
      <tr>
        <td colspan="2" bgcolor="white" align="right">
          <a name="{name}"/>
          <xsl:if test="$file">
            <a href="../index.html">toc</a>
          </xsl:if>
          <xsl:if test="not($file)">
            <a href="#toc">toc</a>
          </xsl:if>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <xsl:if test="name()='tag'"><b>&lt;mm:<xsl:value-of select="name"/>&gt;</b></xsl:if>
          <xsl:if test="name()='tag-file'"><b>&lt;mm:<xsl:value-of select="name"/>&gt;</b></xsl:if>
          <xsl:if test="name()='taginterface'"><b><font color="{$extendscolor}">`<xsl:value-of select="name"/>' tags</font></b></xsl:if>
          <xsl:apply-templates select="info"/>
          <xsl:if test="since">
            (since: <xsl:value-of select="since" />)
          </xsl:if>
        </td>
      </tr>
      <xsl:if test="see">
        <tr>
          <td width="100" valign="top">see also</td>
          <td>
            <xsl:apply-templates select="see" >
              <xsl:with-param name="file" select="$file" />
              <xsl:with-param name="type" select="$type" />
            </xsl:apply-templates>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="attribute">
        <tr>
          <td width="100" valign="top">attributes</td>
          <td>
            <ul>
              <xsl:apply-templates select="attribute" mode="full">
                <xsl:with-param name="file" select="$file" />
                <xsl:with-param name="type" select="$type" />
              </xsl:apply-templates>
            </ul>
          </td>
        </tr>
      </xsl:if>

      <xsl:apply-templates select="extends">
        <xsl:with-param name="file" select="$file" />
        <xsl:with-param name="type" select="$type" />
      </xsl:apply-templates>
      <xsl:if test="xxbodycontent"><!-- ignore the bodycontent, its no use -->
        <tr>
          <td width="100" valign="top">body</td>
          <td>
            <xsl:value-of select="bodycontent"/>
            <br />
            <xsl:apply-templates select="bodycontentinfo"/>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="name()='taginterface'">
        <tr>
          <td>tags of this type</td><td>
          <xsl:apply-templates
              select="/taglib/*[name()='tag' or name()='taginterface']/extends[.=current()/name]/parent::*"
              mode="tocext" >
            <xsl:with-param name="file" select="$file" />
            <xsl:with-param name="type" select="$type" />
            <xsl:with-param name="testlast" select="true()" />
            <xsl:sort select="name" />
          </xsl:apply-templates>
        </td>
        </tr>
      </xsl:if>
      <xsl:apply-templates select="example[$file or not(include)]">
        <xsl:with-param name="file" select="$file" />
      </xsl:apply-templates>
      <xsl:if test="not($file) and example/include">
        <tr>
          <td>more examples</td><td>
          <xsl:if test="$linkexamples">
            <xsl:for-each select="example/include">
              <a >
                <xsl:attribute name="href">
                  <xsl:call-template name="tagref">
                    <xsl:with-param name="file" select="true()" />
                    <xsl:with-param name="tag"  select="ancestor::tag/name" />
                    <xsl:with-param name="anchor">
                      <xsl:value-of select="ancestor::tag/name" />.example.<xsl:number level="single" count="example"  />
                    </xsl:with-param>
                  </xsl:call-template>
                </xsl:attribute>
                example <xsl:number level="single" count="example"  />
                </a><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </xsl:for-each>
          </xsl:if>
          <xsl:if test="not($linkexamples)">
            <xsl:for-each select="example/include">
              <!-- The following will only work with xalan! -->
              <!--<xsl:apply-templates select="rf:readExample(concat($exampledir, @href))" />-->
              <!--
                  The following will work in XSLT 2.0, but not yet :(
                  <xsl:value-of select="unparsed-text(@href)" />

                  When using 'xsltproc' to generate the documentation, you can use the
                  following, but you have to create a bunch of .xml files from the
                  codesamples:
                  <xsl:apply-templates select="document(concat(@href, '.xml'), example)" />
              -->
            </xsl:for-each>
          </xsl:if>
        </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:template>

  <xsl:template match="function" mode="full">
    <xsl:param name="file" select="false()" /><!-- if true, reference to files -->
    <xsl:param name="type" select="'all'" /><!-- if refering to tag of other type, must also reference to file -->
    <xsl:param name="linkexamples" select="true()" />
    <table bgcolor="#eeeeee" width="100%" cellpadding="5">
      <tr>
        <td colspan="2" bgcolor="white" align="right">
          <a name="function_{name}"/>
          <xsl:if test="$file">
            <a href="../index.html">toc</a>
          </xsl:if>
          <xsl:if test="not($file)">
            <a href="#toc">toc</a>
          </xsl:if>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <b><xsl:text disable-output-escaping="yes">&amp;#x24;</xsl:text>{mm:<xsl:value-of select="name"/>}</b>
          <p><xsl:value-of select="description" /></p>
          <p><em>Signature: </em><xsl:value-of select="function-signature" /></p>
          <xsl:apply-templates select="info"/>
          <xsl:if test="since">
            (since: <xsl:value-of select="since" />)
          </xsl:if>
        </td>
      </tr>
      <xsl:if test="see">
        <tr>
          <td width="100" valign="top">see also</td>
          <td>
            <xsl:apply-templates select="see" >
              <xsl:with-param name="file" select="$file" />
              <xsl:with-param name="type" select="$type" />
            </xsl:apply-templates>
          </td>
        </tr>
      </xsl:if>
      <xsl:apply-templates select="example[$file or not(include)]">
        <xsl:with-param name="file" select="$file" />
      </xsl:apply-templates>
      <xsl:if test="not($file) and example/include">
        <tr>
          <td>more examples</td><td>
          <xsl:if test="$linkexamples">
            <xsl:for-each select="example/include">
              <a >
                <xsl:attribute name="href">
                  <xsl:call-template name="tagref">
                    <xsl:with-param name="file" select="true()" />
                    <xsl:with-param name="tag"  select="ancestor::tag/name" />
                    <xsl:with-param name="anchor">
                      <xsl:value-of select="ancestor::tag/name" />.example.<xsl:number level="single" count="example"  />
                    </xsl:with-param>
                  </xsl:call-template>
                </xsl:attribute>
                example <xsl:number level="single" count="example"  />
                </a><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </xsl:for-each>
          </xsl:if>
          <xsl:if test="not($linkexamples)">
            <xsl:for-each select="example/include">
              <!-- The following will only work with xalan! -->
              <xsl:apply-templates select="rf:readExample(concat($exampledir, @href))" />
              <!--
                  The following will work in XSLT 2.0, but not yet :(
                  <xsl:value-of select="unparsed-text(@href)" />

                  When using 'xsltproc' to generate the documentation, you can use the
                  following, but you have to create a bunch of .xml files from the
                  codesamples:
                  <xsl:apply-templates select="document(concat(@href, '.xml'), example)" />
              -->
            </xsl:for-each>
          </xsl:if>
        </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:template>

  <!-- Examples -->
  <xsl:template match="example">
    <xsl:param name="file" select="false()" />
    <tr>
      <xsl:if test="not(include)">
        <td width="100" valign="top">
          <a>
            <xsl:attribute name="name">
              <xsl:value-of select="parent::tag/name" />.example.<xsl:value-of select="position()" />
            </xsl:attribute>
          </a>
          example <xsl:value-of select="position()" />
        </td>
        <td>
          <pre>
            <xsl:value-of select="."/>
          </pre>
        </td>
      </xsl:if>
      <xsl:if test="include">
        <xsl:if test="$file">
          <xsl:apply-templates select="include" /><!-- should be only one.. -->
        </xsl:if>
      </xsl:if>
    </tr>
  </xsl:template>

  <xsl:template match="include">
    <td width="100" valign="top">
      <a>
        <xsl:attribute name="name">
          <xsl:value-of select="ancestor::tag/name" />.example.<xsl:number level="single" count="example"  />
        </xsl:attribute>
      </a>
      example <xsl:number level="single" count="example" />
    </td>
    <td>
      <table width="100%">
        <tr valign="top">
          <td width="50%"><pre><xsl:text disable-output-escaping="yes">&lt;mm:formatter format="escapexml"&gt;&lt;mm:include cite="true" page="/mmexamples/taglib/</xsl:text>
          <xsl:value-of select="@href" />
          <xsl:text disable-output-escaping="yes">" /&gt;&lt;/mm:formatter&gt;</xsl:text></pre></td>
          <td width="50%">
            <xsl:text disable-output-escaping="yes">&lt;% try { %&gt; &lt;%@include file="/mmexamples/taglib/</xsl:text><xsl:value-of select="@href" /><xsl:text disable-output-escaping="yes">" %&gt; &lt;% } catch(Exception e) { out.println(e.toString()); } %&gt;</xsl:text>
          </td>
        </tr>
      </table>
    </td>
  </xsl:template>

  <xsl:template match="extends">
    <xsl:param name="file" select="false()" />
    <xsl:param name="type" select="'all'" />
    <tr>
      <td width="100" valign="top"><xsl:if test="/taglib/taginterface/name[.=current()]"><a>
        <xsl:attribute name="href">
          <xsl:call-template name="tagref">
            <xsl:with-param name="file" select="$file" />
            <xsl:with-param name="type" select="$type" />
            <xsl:with-param name="tag"  select="current()" />
          </xsl:call-template>
        </xsl:attribute>
      <font color="{$extendscolor}"><xsl:value-of select="." /></font></a></xsl:if> attributes</td>
      <td>
        <xsl:if test="/taglib/*[name()='tag' or name()='taginterface' or name()='tag-file']/name[.=current()]/parent::*/attribute">
          <ul>
            <xsl:apply-templates select="/taglib/*[starts-with(name(), 'tag')]/name[.=current()]/parent::*/attribute"  mode="extends">
              <xsl:with-param name="file" select="$file" />
              <xsl:with-param name="type" select="$type" />
            </xsl:apply-templates>
          </ul>
        </xsl:if>
      </td>
    </tr>
    <xsl:apply-templates select="/taglib/*[name()='tag' or name()='taginterface' or name()='tag-file']/name[.=current()]/parent::*/extends" >
      <xsl:with-param name="file" select="$file" />
      <xsl:with-param name="type" select="$type" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="attribute" mode="extends">
    <xsl:param name="file" select="false()" />
    <xsl:param name="type" select="'all'" />
    <li><a>
      <xsl:attribute name="href">
        <xsl:call-template name="tagref">
          <xsl:with-param name="file" select="$file" />
          <xsl:with-param name="type" select="$type" />
          <xsl:with-param name="tag"  select="parent::*/name" />
          <xsl:with-param name="attribute"  select="name" />
        </xsl:call-template>
      </xsl:attribute>
    <font color="{$attrcolor}"><xsl:value-of select="name" /></font></a></li>
  </xsl:template>

  <xsl:template match="attribute" mode="full">
    <xsl:param name="file" select="false()" />
    <xsl:param name="type" select="'all'" />
    <li>
      <a name="{parent::*/name}.{name}" />
      <xsl:choose>
        <xsl:when test="requirednote">
          <xsl:if test="requirednote">
            <font color="ff9900"><xsl:apply-templates select="name"/></font>
            <font color="black" size="-1"> (<xsl:value-of select="requirednote"/>)</font>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="required='true'">
              <font color="{$reqcolor}"><xsl:apply-templates select="name"/></font>
            </xsl:when>
            <xsl:otherwise>
              <font color="{$attrcolor}"><xsl:apply-templates select="name"/></font>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="since">
        (since: <xsl:value-of select="since" />)
      </xsl:if>
      <br />
      <xsl:if test="info">
        <xsl:apply-templates select="info"/>
        <br />
      </xsl:if>
      <xsl:if test="see">
        see: <xsl:apply-templates select="see">
        <xsl:with-param name="file" select="$file" />
        <xsl:with-param name="type" select="$type" />
      </xsl:apply-templates>
      <br />
      </xsl:if>
      <xsl:if test="possiblevalue">
        <table bgcolor="#99ccff">
          <xsl:apply-templates select="possiblevalue"/>
        </table>
      </xsl:if>
      <xsl:if test="examplevalue">
        <table bgcolor="#99ffff">
          <xsl:apply-templates select="examplevalue"/>
        </table>
      </xsl:if>
    </li>
  </xsl:template>

  <xsl:template match="possiblevalue">
    <tr>
      <td valign="top"><b><xsl:value-of select="value"/></b></td>
      <td valign="top"><xsl:apply-templates select="info"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="examplevalue">
    <tr>
      <td valign="top"><b><xsl:value-of select="value"/></b></td>
      <td valign="top"><xsl:apply-templates select="info"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="info">
    <xsl:apply-templates select="p|text()|em|a|ul|pre|taglibcontent|document" />
  </xsl:template>

  <xsl:template match="document">
    <!-- I hate XSL -->
    <xsl:if test="@mode = 'escapers'">
      <xsl:apply-templates select="document(@file)" mode="escapers"/>
    </xsl:if>

  </xsl:template>

  <xsl:template match="p|text()|a|ul|pre">
    <xsl:copy-of select="." />
  </xsl:template>

  <xsl:template match="em">
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="escaper|postprocessor">
    <tr>
      <td valign="top"><xsl:value-of select="@id" /></td>
      <td valign="top"><xsl:apply-templates select="info"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="parameterizedescaper">
    <tr>
      <td valign="top"><xsl:value-of select="@id" /></td>
      <td valign="top"><xsl:apply-templates select="info"/></td>
      <td valign="top">
        <table>
          <tr>
            <th valign="top">Parameter name</th>
            <th valign="top">Description</th>
          </tr>
          <xsl:for-each select="param">
            <tr>
              <td valign="top"><xsl:value-of select="@name" /></td>
              <td valign="top"><xsl:apply-templates select="info" /></td>
            </tr>
          </xsl:for-each>
        </table>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="taglibcontent" mode="escapers">
    <table bgcolor="#99ccff" width="100%">
      <tr><th valign="top"><a name="escapers"/>Escaper</th><th></th></tr>
      <xsl:apply-templates select="escaper|postprocessor"/>
    </table>
  </xsl:template>
  <xsl:template match="taglibcontent" mode="postprocessors">
    <table bgcolor="#99ccff" width="100%">
      <tr><th valign="top"><a name="postprocessors" />Postprocessor</th><th></th></tr>
      <xsl:apply-templates select="postprocessor"/>
    </table>
  </xsl:template>
  <xsl:template match="taglibcontent" mode="parameterizedescapers">
    <table bgcolor="#99ccff" width="100%">
      <tr>
        <th valign="top" colspan="2"><a name="parameterizedescapers" />Parameterized Escaper</th>
        <th valign="top" colspan="2">Parameters</th>
      </tr>
      <xsl:apply-templates select="parameterizedescaper"/>
    </table>
  </xsl:template>
  <xsl:template match="taglibcontent" mode="content">
    <table bgcolor="#99ffff" width="100%">
      <tr><th>Id</th><th valign="top"><a name="contenttypes" />Content-Type</th><th valign="top">Default escaper</th><th valign="top">Default postprocessor</th><th valign="top">Default encoding</th></tr>
      <xsl:for-each select="content">
        <tr>
          <td valign="top">
            <xsl:if test="@id"><xsl:value-of select="@id" /></xsl:if>
            <xsl:if test="not(@id)"><xsl:value-of select="@type" /></xsl:if>
          </td>
          <td valign="top"><xsl:value-of select="@type" /></td>
          <td valign="top"><xsl:value-of select="@defaultescaper" /></td>
          <td valign="top"><xsl:value-of select="@defaultpostprocessor" /></td>
          <td valign="top"><xsl:value-of select="@defaultencoding" /></td>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>




</xsl:stylesheet>
