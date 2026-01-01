<xsl:stylesheet
    xmlns:a="http://www.w3.org/2005/Atom"
    xmlns:xsl = "http://www.w3.org/1999/XSL/Transform" version = "1.0" >

  <xsl:output method="xml"
              version="1.0"
              encoding="utf-8"
              omit-xml-declaration="yes"
              indent="yes"
              />

  <xsl:template match="rss">
    <ul>
      <xsl:for-each select="*/item">
        <li>
          <em><xsl:value-of select="pubDate" /></em>
          <a href="{link}"><xsl:value-of select="title" /></a>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <xsl:template match="a:feed">
    <ul>
      <xsl:for-each select="a:entry">
        <li>
          <xsl:if test="a:link[@type='image/png']">
            <img>
              <xsl:attribute name="src">
                <xsl:value-of select="a:link[@type='image/png']/@href" />
              </xsl:attribute>
            </img>
          </xsl:if>
          <em>
            <xsl:value-of select="a:published" />
          </em>
          <xsl:text> </xsl:text>
          <span class="message">
            <xsl:if test="not(a:author/a:uri = 'http://twitter.com/mmbase')">
              <xsl:text>[</xsl:text><xsl:value-of select="a:author/a:name" /><xsl:text>] </xsl:text>
            </xsl:if>
            <xsl:text> </xsl:text>
            <a href="{a:link[@type='text/html']/@href}"><xsl:value-of select="a:title" /></a>
          </span>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>


</xsl:stylesheet>
