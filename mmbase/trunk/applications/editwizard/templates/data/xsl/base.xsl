<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl  ="http://www.w3.org/1999/XSL/Transform">
  <!--
  Basic parameters and settings for all xsl's of the editwizards.
       
  @since  MMBase-1.6
  @author Michiel Meeuwissen
  @version $Id: base.xsl,v 1.15 2002-08-05 14:13:28 michiel Exp $
       -->
  <xsl:import href="xsl/prompts.xsl" />

  <xsl:output
    method="xml"
    version="1.0"
    encoding="utf-8"
    omit-xml-declaration="no"
    standalone="no"
    doctype-public="-//W3C//DTD HTML 4.0 Transitional//"
    indent="no"
    />

  <xsl:param name="ew_context"></xsl:param><!-- The web-application's context -->
  <xsl:param name="ew_path"></xsl:param><!-- The directory in which the editwizards are installed (relative to context root) -->

  <xsl:variable name="rootew_path"><xsl:value-of select="$ew_context" /><xsl:value-of select="$ew_path" /></xsl:variable>
  
  <xsl:param name="username">(unknown)</xsl:param>

  <xsl:param name="language">en</xsl:param>
    
  <xsl:param name="sessionid"></xsl:param>

  <xsl:param name="referrer"></xsl:param><!-- name of the file that called list.jsp or default.jsp, can be used for back-buttons (relative to context-root) --> 
  
  <xsl:variable name="rootreferrer"><xsl:value-of select="$ew_context" /><xsl:value-of select="$referrer" /></xsl:variable><!-- relative to root -->


  <xsl:variable name="referrerdir"><xsl:call-template name="getdirpart"><xsl:with-param name="dir" select="$rootreferrer" /></xsl:call-template></xsl:variable><!-- the directory of that file, needed to refer to resources there (when you override), like e.g. images -->


  <!-- Perhaps you want to refer to stuff not relative to the referrer-page, but to the root of the site where it belongs to. 
       This must be given to the jsp's then with the paremeter 'templates' 
       -->
  <xsl:variable name="templatedir"><xsl:value-of select="$referrerdir" /></xsl:variable>

  <xsl:param name="sessionkey">editwizard</xsl:param>
  <xsl:param name="cloudkey">cloud_mmbase</xsl:param><!-- name of variable in session in which is the cloud -->
  <xsl:param name="wizardparams"><xsl:value-of select="$sessionid" />?proceed=true&amp;sessionkey=<xsl:value-of select="$sessionkey" />&amp;language=<xsl:value-of select="$language" /></xsl:param>
  
  <xsl:variable name="listpage">list.jsp<xsl:value-of select="$wizardparams" /></xsl:variable>
  <xsl:variable name="wizardpage">wizard.jsp<xsl:value-of select="$wizardparams" /></xsl:variable>
  <xsl:variable name="popuppage">wizard.jsp<xsl:value-of select="$sessionid" />?referrer=<xsl:value-of select="$referrer" />&amp;language=<xsl:value-of select="$language" /></xsl:variable>
  <xsl:variable name="deletepage">deletelistitem.jsp<xsl:value-of select="$wizardparams" /></xsl:variable>
  <xsl:variable name="uploadpage">upload.jsp<xsl:value-of select="$wizardparams" /></xsl:variable>
  
  <xsl:param name="debug">false</xsl:param>
  
  <xsl:variable name="javascriptdir">../javascript/</xsl:variable>
  <xsl:variable name="mediadir">../media/</xsl:variable>

  <!-- ================================================================================
       General appearance
       ================================================================================ -->

  <xsl:variable name="imagesize">+s(128x128)</xsl:variable>

  <xsl:template name="extrastyle" />        <!-- If you want to add a cascading stylesheet (to change the appearance), the you can overrride this -->
  <xsl:template name="extrajavascript" />   <!-- If you need extra javascript, then you can override this thing -->





  <!-- ================================================================================ -->

    <!-- utitily function. Takes a file and gets the directory part of it -->
  <xsl:template name="getdirpart">
    <xsl:param name="dir" />
    <xsl:variable name="firstdir" select="substring-before($dir, '/') " />
    <xsl:variable name="restdir" select="substring-after($dir, '/') " />
    <!-- if still a rest then add firstdir to dir -->
    <xsl:if test="$restdir">
      <xsl:value-of select="$firstdir" /><xsl:text>/</xsl:text>
      <xsl:call-template name="getdirpart">
        <xsl:with-param name="dir" select="$restdir" />
      </xsl:call-template>    
    </xsl:if>  
  </xsl:template>

  
</xsl:stylesheet>
  
