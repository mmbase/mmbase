<xsl:stylesheet version="1.0"
  xmlns:xsl  ="http://www.w3.org/1999/XSL/Transform"
  xmlns:node ="org.mmbase.bridge.util.xml.NodeFunction"
>
  <!--
  wizard.xls

  @since  MMBase-1.6
  @author Kars Veling
  @author Michiel Meeuwissen
  @author Pierre van Rooden
  @version $Id: wizard.xsl,v 1.27 2002-06-28 12:48:21 pierre Exp $
  -->

  <xsl:import href="base.xsl" />

  <xsl:variable name="defaultsearchage">7</xsl:variable>
  <xsl:param name="wizardtitle"><xsl:value-of select="list/object/@type" /></xsl:param>
  <xsl:param name="title"><xsl:value-of select="$wizardtitle" /></xsl:param>

  <xsl:template match="@*">
    <xsl:copy><xsl:value-of select="." /></xsl:copy>
  </xsl:template>

  <xsl:template match="@name"></xsl:template>

  <xsl:template match="/">
    <xsl:apply-templates select="wizard" />
  </xsl:template>


  <xsl:template name="beforeform" />

  <xsl:template match="wizard">
    <html>
      <head>
        <title><xsl:value-of select="title" /></title>
        <link rel="stylesheet" type="text/css" href="../style.css" />
      </head>
      <body leftmargin="0"
        topmargin="0"
        marginwidth="0"
        marginheight="0"
        onload="doOnLoad_ew();" onunload="doOnUnLoad_ew();">
        <script language="javascript" src="{$javascriptdir}tools.js"><xsl:comment>help IE</xsl:comment></script>
        <script language="javascript" src="{$javascriptdir}validator.js"><xsl:comment>help IE</xsl:comment></script>
        <script language="javascript" src="{$javascriptdir}editwizard.jsp{$sessionid}"><xsl:comment>help IE</xsl:comment></script>
        <script language="javascript" src="{$javascriptdir}wysiwyg.js"><xsl:comment>help IE</xsl:comment></script>

        <xsl:call-template name="beforeform" />

        <form name="form" method="post" action="{$wizardpage}" id="{/wizard/curform}"
              message_pattern="{$message_pattern}"
              message_required="{$message_required}"
              message_minlength="{$message_minlength}" message_maxlength="{$message_maxlength}"
              message_min="{$message_min}"  message_max="{$message_max}"
              message_mindate="{$message_mindate}" message_maxdate="{$message_maxdate}"
              message_dateformat="{$message_dateformat}"
              message_thisnotvalid="{$message_thisnotvalid}" message_notvalid="{$message_notvalid}"
        >
          <input type="hidden" name="curform" value="{/wizard/curform}" />
          <input type="hidden" name="cmd" value="" id="hiddencmdfield" />


          <table cellspacing="0" cellpadding="3" border="0" width="100%">
            <tr>
              <td colspan="2" align="center" valign="bottom">
                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                  <tr>
                    <td class="head"><nobr><xsl:value-of select="title" /></nobr></td>
                    <td class="superhead" align="right">
                      <nobr><xsl:if test="$debug='true'"><a href="debug.jsp{$sessionid}" target="_blank">[debug]</a><br /></xsl:if><xsl:value-of select="form[@id=/wizard/curform]/title" /></nobr>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>

            <tr><td colspan="2" class="divider"><span class="head"><nobr><xsl:value-of select="form[@id=/wizard/curform]/subtitle" /><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></nobr></span></td></tr>

            <xsl:apply-templates select="form[@id=/wizard/curform]" />

            <xsl:apply-templates select="/*/steps-validator" />
          </table>

        </form>

      </body>
    </html>

  </xsl:template>

  <xsl:template match="form">
    <xsl:for-each select="fieldset|field|list">
      <tr>
        <xsl:apply-templates select="." />
      </tr>
      <xsl:if test="@minoccurs or @maxoccurs">
        <tr><td><img src="{$mediadir}nix.gif" width="1" height="1" hspace="0" vspace="0" border="0" alt="" /></td></tr>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="field">
    <td align="left" valign="top" width="150">
      <xsl:call-template name="prompt" />
    </td>
    <td align="left">
      <xsl:if test="../../item[count(field|fieldset) &gt; 1]">
        <xsl:attribute name="style">border-width:0 1 0 0; border-style:solid; border-color:#000000; padding-right:3;</xsl:attribute>
      </xsl:if>
      <xsl:call-template name="fieldintern" />
    </td>
  </xsl:template>

  <xsl:template match="fieldset">
    <td align="left" valign="top" width="60">
      <xsl:call-template name="prompt" />
    </td>
    <td align="left">
      <xsl:if test="../../item[count(field|fieldset) &gt; 1]">
        <xsl:attribute name="style">border-width:0 1 0 0; border-style:solid; border-color:#000000; padding-right:3;</xsl:attribute>
      </xsl:if>
      <xsl:for-each select="field">
       <xsl:call-template name="fieldintern" />
       <xsl:text disable-output-escaping="yes"> </xsl:text>
      </xsl:for-each>
    </td>
  </xsl:template>

  <xsl:template name="prompt">
      <xsl:if test="../../item[count(field|fieldset) &gt; 1]">
        <xsl:attribute name="style">border-width:0 0 0 1; border-style:solid; border-color:#000000; padding-left:3;</xsl:attribute>
      </xsl:if>
      <img src="{$mediadir}nix.gif" width="60" height="1" hspace="0" vspace="0" border="0" alt="" /><br />
      <span id="prompt_{@fieldname}" class="valid" prompt="{prompt}">
        <xsl:choose>
          <xsl:when test="description">
            <xsl:attribute name="title"><xsl:value-of select="description" /></xsl:attribute>
            <xsl:attribute name="descriptiion"><xsl:value-of select="description" /></xsl:attribute>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="title"><xsl:value-of select="prompt" /></xsl:attribute>
            <xsl:attribute name="description"><xsl:value-of select="prompt" /></xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="prompt" />
      </span>
   </xsl:template>

   <xsl:template name="fieldintern">
      <xsl:choose>
        <xsl:when test="@ftype='data'">
          <span style="width:400;"><xsl:value-of select="value" /></span>
        </xsl:when>
        <xsl:when test="@ftype='line'">
          <input type="text" size="80" name="{@fieldname}" value="{value}" class="width400" onkeyup="validate_validator(event);" onblur="validate_validator(event);">
            <xsl:apply-templates select="@*" />
          </input>
        </xsl:when>
        <xsl:when test="@ftype='text'">
          <xsl:text disable-output-escaping="yes">&lt;textarea name="</xsl:text><xsl:value-of select="@fieldname" /><xsl:text>" dttype="</xsl:text><xsl:value-of select="@dttype" /><xsl:text>" class="width400" wrap="soft" cols="80" onkeyup="validate_validator(event);" onblur="validate_validator(event);"</xsl:text>
          <xsl:choose>
            <xsl:when test="@rows">
              <xsl:text>rows="</xsl:text><xsl:value-of select="@rows" /><xsl:text>"</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>rows="10"</xsl:text>
            </xsl:otherwise></xsl:choose>
          <xsl:apply-templates select="@*" />
          <xsl:text disable-output-escaping="yes">&gt;</xsl:text><xsl:value-of disable-output-escaping="yes" select="value" />
          <xsl:text disable-output-escaping="yes">&lt;/textarea&gt;</xsl:text>
        </xsl:when>
        <xsl:when test="@ftype='relation' or @ftype='enum'">
          <select name="{@fieldname}" class="width400" onchange="validate_validator(event);" onblur="validate_validator(event);">
            <xsl:apply-templates select="@*" />
            <xsl:choose>
              <xsl:when test="optionlist/option[@selected='true']"></xsl:when>
              <xsl:otherwise>
                <option value="-"><xsl:call-template name="prompt_select" /></option>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="optionlist/option">
              <option value="{@id}">
                <xsl:if test="@selected='true'">
                  <xsl:attribute name="selected">true</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="." />
              </option>
            </xsl:for-each>
          </select>
        </xsl:when>
        <xsl:when test="@ftype='enumdata'">
          <xsl:if test="optionlist/option[@id=current()/value]">
            <span style="width:400;"><xsl:value-of select="optionlist/option[@id=current()/value]" /></span>
          </xsl:if>
        </xsl:when>
        <xsl:when test="@ftype='date'">
          <div>
            <input type="hidden" name="{@fieldname}" value="{value}" id="{@fieldname}">
              <xsl:apply-templates select="@*" />
            </input>

            <xsl:if test="(@dttype='datetime') or (@dttype='date')">
              <select name="internal_{@fieldname}_day" dttype="day" onchange="validate_validator(event);" onblur="validate_validator(event);">
                <option value="1">1</option><option value="2">2</option><option value="3">3</option><option value="4">4</option><option value="5">5</option><option value="6">6</option><option value="7">7</option><option value="8">8</option><option value="9">9</option><option value="10">10</option><option value="11">11</option><option value="12">12</option><option value="13">13</option><option value="14">14</option><option value="15">15</option><option value="16">16</option><option value="17">17</option><option value="18">18</option><option value="19">19</option><option value="20">20</option><option value="21">21</option><option value="22">22</option><option value="23">23</option><option value="24">24</option><option value="25">25</option><option value="26">26</option><option value="27">27</option><option value="28">28</option><option value="29">29</option><option value="30">30</option><option value="31">31</option>
              </select><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
              <select name="internal_{@fieldname}_month" dttype="month" onchange="validate_validator(event);" onblur="validate_validator(event);">
                <xsl:call-template name="optionlist_months" />
              </select><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
              <input name="internal_{@fieldname}_year" dttype="year" type="text" value="" size="5" maxlength="4" onkeyup="validate_validator(event);" onblur="validate_validator(event);" />
            </xsl:if>

            <xsl:if test="@dttype='datetime'">
              <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>at<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
            </xsl:if>

            <xsl:if test="(@dttype='datetime') or (@dttype='time')">
              <input name="internal_{@fieldname}_hours" dttype="hour" type="text" value="" size="2" maxlength="2" onkeyup="validate_validator(event);" onblur="validate_validator(event);" />
              <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>:<xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
              <input name="internal_{@fieldname}_minutes" dttype="minutes" type="text" value="" size="2" maxlength="2" onkeyup="validate_validator(event);" onblur="validate_validator(event);" />
            </xsl:if>
          </div>
        </xsl:when>
        <xsl:when test="@ftype='wizard' or @ftype='startwizard'">
          <nobr>
           <xsl:if test="@inline='true'">
                <a href="javascript:doStartWizard('{../../@fid}','{../../command[@name='add-item']/@value}','{@wizardname}','{@objectnumber}');">
                <xsl:call-template name="prompt_edit_wizard" />
                </a>
           </xsl:if>
           <xsl:if test="not(@inline='true')">
                <a href="{$popuppage}&amp;fid={../../@fid}&amp;did={../../command[@name='add-item']/@value}&amp;sessionkey={@wizardname}|popup_{@objectnumber}&amp;wizard={@wizardname}&amp;objectnumber={@objectnumber}"
                   target="_blank">
                <xsl:call-template name="prompt_edit_wizard" />
                </a>
           </xsl:if>
          </nobr>
        </xsl:when>
        <xsl:when test="@ftype='binary'">
          <xsl:choose>
            <xsl:when test="@dttype='image' and not(upload)">
              <div class="imageupload">
                <div><input type="hidden" name="{@fieldname}" value="YES" />
                  <img src="{$ew_imgdb}{node:function(string(@number), concat('cache(', $imagesize, ')'))}" hspace="0" vspace="0" border="0" /><br />
                  <a href="{$uploadpage}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}" onclick="return doStartUpload(this);">
                  <xsl:call-template name="prompt_image_upload" />
                  </a>
                </div>
              </div>
            </xsl:when>
            <xsl:when test="@dttype='image' and upload">
              <div class="imageupload"><input type="hidden" name="{@fieldname}" value="YES" />
                <img src="{upload/path}" hspace="0" vspace="0" border="0" width="128" height="128" />
                <br />
                <span>
                  <xsl:value-of select="upload/@name" /><xsl:text disable-output-escaping="yes" >&amp;nbsp;</xsl:text> (<xsl:value-of select="round((upload/@size) div 100) div 10" />K)
                </span>
                <br />
                <a href="{$uploadpage}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}" onclick="return doStartUpload(this);"><xsl:call-template name="prompt_image_upload" /></a>
              </div>
            </xsl:when>
            <xsl:otherwise>
              <nobr><input type="hidden" name="{@fieldname}" value="YES" />
                <xsl:choose>
                  <xsl:when test="not(upload)"><xsl:call-template name="prompt_no_file" /><br />
                    <a href="{$ew_context}/attachment.db?{@number}"><xsl:call-template name="prompt_do_download" /></a><br />
                  </xsl:when>
                  <xsl:otherwise><xsl:call-template name="prompt_uploaded" /><xsl:value-of select="upload/@name" /><xsl:text disable-output-escaping="yes" >&amp;nbsp;</xsl:text>(<xsl:value-of select="round((upload/@size) div 100) div 10" />K)<br />
                  </xsl:otherwise>
                </xsl:choose>
                <a href="{$uploadpage}&amp;did={@did}&amp;wizard={/wizard/@instance}&amp;maxsize={@dtmaxsize}" onclick="return doStartUpload(this);"><xsl:call-template name="prompt_do_upload" /></a>
              </nobr>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@ftype='image'">
          <span>
            <img src="{$ew_imgdb}{node:function(string(@number), concat('cache(', $imagesize,')'))}" hspace="0" vspace="0" border="0" title="{field[@name='description']}" /><br />
          </span>

        </xsl:when>
        <xsl:when test="@ftype='realposition'">
       <span style="width:128; height:168;" >
          <input type="text" name="{@fieldname}" value="{value}" class="width400" onkeyaup="validate_validator(event);" onblur="validate_validator(event);">
              <xsl:apply-templates select="@*" />
            </input><input type="button" value="get" onClick="document.forms['form'].{@fieldname}.value = document.embeddedplayer.GetPosition();" />
</span>
        </xsl:when>
        <xsl:otherwise>
          <input type="text" name="{@fieldname}" value="{value}" class="width400" onkeyaup="validate_validator(event);" onblur="validate_validator(event);">
            <xsl:apply-templates select="@*" />
          </input>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <xsl:template match="item">
    <span class="listitem" style="display:inline; ">
      <!-- here we figure out how to draw this repeated item. It depends on the displaytype -->
      <xsl:choose>
        <xsl:when test="@displaytype='link'">
          <span style="width:600;"><a href="{$wizardpage}&amp;wizard={@wizardname}&amp;objectnumber={field[@name='number']/value}">- <xsl:value-of select="field[@name='title']/value" /></a></span>
        </xsl:when>
        <xsl:when test="@displaytype='image'">
          <span style="width:128; height:168;" >
            <table border="0" cellspacing="0" cellpadding="0" width="128" style="display:inline; width:128;">
              <tr>
                <td>
                  <xsl:call-template name="itembuttons" />
                </td>
              </tr>
              <tr>
                <td>
                  <span>
                    <img src="{$ew_imgdb}{node:function(string(@destination), concat('cache(',$imagesize,')'))}" hspace="0" vspace="0" border="0" title="{field[@name='description']}" /><br />
                  </span>
                </td>
                <td width="20"><img src="{$mediadir}nix.gif" width="20" height="1" /></td>
                <xsl:if test="field[not(@ftype='data')]">
                  <!-- another field, not just data, eg. a position editor -->
                  <td colspan="10" valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" width="200" style="width:200">
                      <xsl:for-each select="field[not(@ftype='data')]">
                        <tr>
                          <xsl:apply-templates select="." />
                        </tr>
                      </xsl:for-each>
                    </table>
                  </td>
                </xsl:if>
              </tr>
              <tr>
                <td width="128">
                  <span style="width:128; height:26; overflow:hidden; font-size:10px;"><xsl:value-of select="field[@ftype='data']/value" /></span>
                </td>
              </tr>
            </table>
          </span>
        </xsl:when>
        <xsl:when test="count(field|fieldset) &lt; 2">
          <table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr>
              <td align="left" valign="top">
                <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                     <xsl:for-each select="field|fieldset">
                      <xsl:apply-templates select="." />
                     </xsl:for-each>
                  </tr>
                </table>
              </td>
              <td align="right" valign="top">
                <nobr>
                  <xsl:call-template name="itembuttons" />
                </nobr>
              </td>
            </tr>
          </table>
        </xsl:when>
        <xsl:otherwise>
          <table border="0" cellspacing="0" cellpadding="0" width="100%" >
            <tr>
              <td align="right" valign="top" xstyle="position:relative; top:0; left:0;">
                  <xsl:call-template name="itembuttons" />
              </td>
            </tr>
            <!-- draw all fields, if there are any for this item -->
            <tr><td colspan="2" style="border-width:1 1 0 1; border-style:solid; border-color:#000000;"><img src="{$mediadir}nix.gif" width="1" height="3" hspace="0" vspace="0" border="0" alt="" /></td></tr>
            <xsl:for-each select="field|fieldset">
              <tr>
                <xsl:apply-templates select="." />
              </tr>
            </xsl:for-each>
            <tr><td colspan="2" style="border-width:0 1 1 1; border-style:solid; border-color:#000000;"><img src="{$mediadir}nix.gif" width="1" height="3" hspace="0" vspace="0" border="0" alt="" /></td></tr>
          </table>
        </xsl:otherwise>
      </xsl:choose>
    </span><wbr />
  </xsl:template>

  <xsl:template name="itembuttons">
    <xsl:if test="@displaytype='audio'">
        <a href="{$ew_context}/rastreams.db?{@destination}" title="{$tooltip_audio}"><xsl:call-template name="prompt_audio" /></a>
    </xsl:if>
    <xsl:if test="@displaytype='video'">
        <a href="{$ew_context}/rmstreams.db?{@destination}" title="{$tooltip_video}"><xsl:call-template name="prompt_video" /></a>
    </xsl:if>
    <xsl:if test="command[@name='delete-item']">
        <span class="imagebutton" title="{$tooltip_remove}" onclick="doSendCommand('{command[@name='delete-item']/@cmd}');">
          <xsl:call-template name="prompt_remove" />
        </span>
    </xsl:if>

    <xsl:if test="command[@name='move-up']">
          <span class="imagebutton" title="{$tooltip_up}" onclick="doSendCommand('{command[@name='move-up']/@cmd}');"><xsl:call-template name="prompt_up" /></span>
    </xsl:if>
    <xsl:if test="command[@name='move-down']">
          <span class="imagebutton" title="{$tooltip_down}" onclick="doSendCommand('{command[@name='move-down']/@cmd}');"><xsl:call-template name="prompt_down" /></span>
    </xsl:if>
  </xsl:template>

  <xsl:template match="list">
    <td align="left" valign="top" colspan="2" class="listcanvas" width="558">
      <div class="subhead" title="{description}">
        <nobr><xsl:value-of select="title" /></nobr>
      </div>

      <xsl:if test="item">
        <br />
        <div style="position:relative; top:0; left:0;">
          <xsl:apply-templates select="item" />
        </div><br />
      </xsl:if>

      <xsl:if test="command[@name='add-item']">
        <xsl:for-each select="command[@name='search']">
          <table border="0" cellspacing="0" cellpadding="0" style="display:inline;" width="616">
            <tr>
              <td align="right" valign="top" class="search" width="100%">
                <nobr>
                  <xsl:value-of select="prompt" /><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
                  <select name="searchage_{../command[@name='add-item']/@cmd}" style="width:80">
                    <option value="0"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'0'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_now" /></option>
                    <option value="1"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'1'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_day" /></option>
                    <option value="7"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'7'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_week" /></option>
                    <option value="31"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'31'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_month" /></option>
                    <option value="365"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'365'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_year" /></option>
                    <option value="-1"><xsl:call-template name="searchage"><xsl:with-param name="real" select="'-1'" /><xsl:with-param name="pref" select="@age" /></xsl:call-template><xsl:call-template name="age_any" /></option>
                  </select>
                  <select name="searchfields_{../command[@name='add-item']/@cmd}" style="width:125;">
                    <xsl:choose>
                      <xsl:when test="search-filter">
                        <xsl:for-each select="search-filter">
                          <option value="{search-fields}"><xsl:value-of select="name" /></option>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <option value="title"><xsl:call-template name="prompt_search_title" /></option>
                      </xsl:otherwise>
                    </xsl:choose>
                    <option value="owner"><xsl:call-template name="prompt_search_owner" /></option>
                  </select>
                  <img src="{$mediadir}nix.gif" width="2" height="1" hspace="0" vspace="0" border="0" alt="" />
                  <input type="text" name="searchterm_{../command[@name='add-item']/@cmd}" value="" style="width:175;" />
                  <img src="{$mediadir}nix.gif" width="2" height="1" hspace="0" vspace="0" border="0" alt="" />
                  <span title="{$tooltip_search}" class="imagebutton" onclick="doSearch(this,'{../command[@name='add-item']/@cmd}','{$sessionkey}');"  >
                    <xsl:for-each select="@*"><xsl:copy /></xsl:for-each>
                    <xsl:call-template name="prompt_search" />
                  </span>
                  <img src="{$mediadir}nix.gif" width="5" height="1" hspace="0" vspace="0" border="0" alt="" />
                </nobr>
              </td>
            </tr>
          </table>
        </xsl:for-each>
      </xsl:if>

      <xsl:for-each select="command[@name='add-item']">
        <table border="0" cellspacing="0" cellpadding="0" width="616">
          <xsl:choose>
            <xsl:when test="../command[@name='search']">
              <xsl:attribute name="style">display:inline; visibility:hidden; position:absolute; top:0; left:0;</xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="style">display:inline;</xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
          <tr>
            <td align="right" valign="top" class="search" width="100%">
              <nobr>
                <span class="imagebutton" onclick="doSendCommand('{@cmd}');"><xsl:call-template name="prompt_new" /></span>
                <img src="{$mediadir}nix.gif" width="5" height="1" hspace="0" vspace="0" border="0" alt="" />
              </nobr>
            </td>
          </tr>
        </table>
      </xsl:for-each>
      <xsl:for-each select="command[@name='startwizard']">
        <table border="0" cellspacing="0" cellpadding="0" style="display:inline;" width="616">
          <tr>
            <td align="right" valign="top" class="search" width="100%">
              <nobr>
           <xsl:if test="@inline='true'">
                <a href="javascript:doStartWizard('{../@fid}','{../command[@name='add-item']/@value}','{@wizardname}','{@objectnumber}');">
                <xsl:call-template name="prompt_add_wizard" />
                </a>
           </xsl:if>
           <xsl:if test="not(@inline='true')">
                <a href="{$popuppage}&amp;fid={../@fid}&amp;did={../command[@name='add-item']/@value}&amp;sessionkey={@wizardname}|popup_{@objectnumber}&amp;wizard={@wizardname}&amp;objectnumber={@objectnumber}"
                   target="_blank">
                <xsl:call-template name="prompt_add_wizard" />
                </a>
           </xsl:if>
                <img src="{$mediadir}nix.gif" width="5" height="1" hspace="0" vspace="0" border="0" alt="" />
              </nobr>
            </td>
          </tr>
        </table>
      </xsl:for-each>
    </td>
  </xsl:template>

  <xsl:template match="steps-validator">
    <!-- when multiple steps -->
    <xsl:if test="count(step) &gt; 1">
      <tr>
        <td colspan="2" align="center">
          <hr color="#005A4A" size="1" noshade="true" />
        </td>
      </tr>
      <tr>
        <td>
        </td>
        <td>
          <!-- all steps -->
          <xsl:for-each select="step">
            <xsl:call-template name="stepbutton" />
            <br />
          </xsl:for-each>
        </td>
      </tr>
      <tr>
        <td colspan="2" align="center">
          <!-- previous -->
          <xsl:call-template name="previousbutton" />
           - -
          <!-- next -->
          <xsl:call-template name="nextbutton" />
        </td>
      </tr>
    </xsl:if>

    <!-- our buttons -->
    <tr><td colspan="2" align="center"><hr color="#005A4A" size="1" noshade="true" /><p>
          <!-- cancel -->
          <xsl:call-template name="cancelbutton" />
           -
          <!-- commit  -->
          <xsl:call-template name="savebutton" />
        </p><hr color="#005A4A" size="1" noshade="true" /></td></tr>
  </xsl:template>

  <xsl:template name="savebutton">
          <a href="javascript:doSave();" id="bottombutton-save" unselectable="on"
           titlesave="{$tooltip_save}" titlenosave="{$tooltip_no_save}"
          >
            <xsl:if test="@allowsave='true'">
              <xsl:attribute name="class">bottombutton</xsl:attribute>
              <xsl:attribute name="title"><xsl:value-of select="$tooltip_save" /></xsl:attribute>
            </xsl:if>
            <xsl:if test="@allowsave='false'">
              <xsl:attribute name="class">bottombutton-disabled</xsl:attribute>
              <xsl:attribute name="title"><xsl:value-of select="$tooltip_no_save" /></xsl:attribute>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="step[@valid='false'][not(@form-schema=/wizard/curform)]">
                <xsl:attribute name="otherforms">invalid</xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="otherforms">valid</xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="prompt_save" />
          </a>
  </xsl:template>

  <xsl:template name="cancelbutton">
            <a href="javascript:doCancel();"><span id="bottombutton-cancel" class="bottombutton" title="{$tooltip_cancel}">
            <xsl:call-template name="prompt_cancel" />
          </span></a>
  </xsl:template>

  <xsl:template name="previousbutton">
          <xsl:choose>
            <xsl:when test="/wizard/form[@id=/wizard/prevform]">
              <a class="step" align="left" width="100%" href="javascript:doGotoForm('{/wizard/prevform}')" title="{$tooltip_previous} '{/wizard/form[@id=/wizard/prevform]/title}'"><xsl:call-template name="prompt_previous" /></a>
            </xsl:when>
            <xsl:otherwise>
              <span class="step-disabled" align="left" width="100%" title="{$tooltip_no_previous}"><xsl:call-template name="prompt_previous" /></span>
            </xsl:otherwise>
          </xsl:choose>
  </xsl:template>

  <xsl:template name="nextbutton">
          <xsl:choose>
            <xsl:when test="/wizard/form[@id=/wizard/nextform]">
              <a class="step" align="left" width="100%" href="javascript:doGotoForm('{/wizard/nextform}')" title="{$tooltip_next} '{/wizard/form[@id=/wizard/nextform]/title}'"><xsl:call-template name="prompt_next" /></a>
            </xsl:when>
            <xsl:otherwise>
              <span class="step-disabled" align="left" width="100%" title="{$tooltip_no_next}"><xsl:call-template name="prompt_next" /></span>
            </xsl:otherwise>
          </xsl:choose>
  </xsl:template>


  <xsl:template name="stepbutton">
            <xsl:variable name="schemaid" select="@form-schema" />
            <a href="javascript:doGotoForm('{@form-schema}');" id="bottombutton-step-{$schemaid}" class="stepicon"
             titlevalid="{$tooltip_valid}" titlenotvalid="{$tooltip_not_valid}"
            > <xsl:attribute name="class"><xsl:if test="$schemaid=/wizard/curform">current</xsl:if>stepicon<xsl:if test="@valid='true'">-valid</xsl:if></xsl:attribute>
              <xsl:attribute name="title"><xsl:value-of select="/*/form[@id=$schemaid]/title" /><xsl:if test="@valid='false'"><xsl:value-of select="$tooltip_step_not_valid" /></xsl:if></xsl:attribute>
              <xsl:call-template name="prompt_step" />
            </a>
            <span class="step-info" ><xsl:value-of select="/*/form[@id=$schemaid]/title" /></span>
  </xsl:template>

  <xsl:template name="searchage">
    <xsl:param name="real" />
    <xsl:param name="pref" />
    <xsl:if test="($real=$pref) or (not($pref) and $defaultsearchage=$real)"><xsl:attribute name="selected">true</xsl:attribute></xsl:if>
  </xsl:template>

</xsl:stylesheet>
