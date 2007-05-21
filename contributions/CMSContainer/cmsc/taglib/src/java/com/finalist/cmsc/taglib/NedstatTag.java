/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspTagException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.finalist.cmsc.navigation.ServerUtil;

/**
 * Tag that parses a netstat string with the current location. 
 * 
 * @author Bas Piepers
 */
public class NedstatTag extends CmscTag {
	
	private static Log log = LogFactory.getLog(InsertPortletTag.class);
	
	/*
	 * For nedstat to work, one must also place the sitestat.js script in the root of the site.
	 * The user is responsible for placing the tag in the right location of the document. For instance:
	 * a technical measurement should only be placed at the homepage, as low as possible (just before
	 * the </body> tag). For more information, please read the sitestat manual. 
	 */ 
	
    protected String lable;    
    protected String type;
    protected String externalUrl;
    protected String landCode;
    protected String customerName;
    protected String siteName;
    protected String counterName;
    protected String urlText;
    
    // The types of tags.
    private static final String TYPE_NORMAL = "normal";
    private static final String TYPE_TECHNICAL = "technical";
    private static final String TYPE_LOADTIME1 = "loadtime1";
    private static final String TYPE_LOADTIME2 = "loadtime2";
    private static final String TYPE_CLICKIN = "clickin";
    private static final String TYPE_CLICKOUT = "clickout";
    private static final String TYPE_BOOKMARK = "bookmark";

    private static final String PRODUCTIONVARIABLE = "server/production";
    
	public void doTag() throws JspException, IOException {				
		/*
		 *  Only construct nedstat tag if this is the live production environment. The production environment
		 *  to be added in the context file is 'server/production' set to 'true' or 'false'.
		 */					
		if (ServerUtil.isLive() && Boolean.valueOf(ServerUtil.getEnvironmentVariableValue(PRODUCTIONVARIABLE))) {			
			String nedstat = "";
			PageContext ctx = (PageContext) getJspContext();
			if (getCustomerName() == null) {
				throw new JspTagException("No customername provided.");
			}
			
			if (getSiteName() == null) {
				throw new JspTagException("No sitename provided.");
			}
			
            if (StringUtils.isBlank(counterName)) {
                /*
                 *  Generate countername by taking the current page location (path).
                 * If the current location is the site (http://[sitename]/) then the 
                 * countername should be the sitename only. Futhermore, if the last character is a
                 * '/', then this character is ommitted from the countername.
                */
                String url = super.getPath();
                if (StringUtils.isBlank(url)) {
                    throw new JspException("CounterName is empty and url is empty");
                }
                else {
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    // Site is provided seperately present. Strip site urlfragment from url
                    int siteIndex = url.indexOf("/");
                    if (siteIndex > -1) {
                        url = url.substring(siteIndex + 1);
                    }
                    else {
                        url = "homepage";
                    }
                    
                    setCounterName(url.replace('/', '.'));  
                }
            }
            
			if (getLandCode() == null) {
				// 	Assuming NL.
				setLandCode("nl");
			}

			// Construct the string according to the type.			
			String type = getType().toLowerCase();
			if (type.equals(TYPE_NORMAL)){
				nedstat = "<!-- Begin Sitestat4 code -->\n<script type=\'text/javascript\'>\n";
				nedstat += "<!--\nfunction sitestat(ns_l){ns_l+=\'&amp;ns__t=\'+(new Date()).getTime();ns_pixelUrl=ns_l;\n";
				nedstat += "ns_0=document.referrer;\n";
				nedstat += "ns_0=(ns_0.lastIndexOf(\'/\')==ns_0.length-1)?ns_0.substring(ns_0.lastIndexOf(\'/\'),0):ns_0;\n";
				nedstat += "if(ns_0.length>0)ns_l+='&amp;ns_referrer='+escape(ns_0);\n";
				nedstat += "if(document.images){ns_1=new Image();ns_1.src=ns_l;}else\n";
				nedstat += "document.write(\'<img src=\"\'+ns_l+\'\" width=\"1\" height=\"1\" alt=\"\">\');}";
				nedstat += "sitestat(\"http://" + getLandCode() + ".sitestat.com/" + getCustomerName() + "/" +
				            getSiteName() + "/s?" + getCounterName() + "\");//-->\n</script>"; 			
				nedstat += "<noscript>\n" +
						"<img src=\"http://" + getLandCode()+ ".sitestat.com/" + getCustomerName() + "/" +
						    getSiteName() + "/s?" + getCounterName() + "\" width=\"1\" height=\"1\" alt=\"\" />" +
						"</noscript>\n";
				nedstat += "<!-- End Sitestat4 code -->";
			}
			else if (type.equals(TYPE_TECHNICAL)){
				nedstat = "<!-- Begin Sitestat4 Technical code -->\n" +
						"<script type=\'text/javascript\' src=\'" + getJavascriptFile(ctx) + "\'></script>\n" +
						"<!-- End Sitestat4 Technical Code -->";			
			}
			else if (type.equals(TYPE_LOADTIME1)){
				// Place just underneath the <head> tag.
				nedstat = "<!-- Begin Sitestat4 Loadingtime1 code -->\n" +
						"<script type=\'text/javascript\'>ns_loadingtime1=(new Date()).getTime()</script>\n" +
						"<!-- End Sitestat4 Loadingtime1 code -->";
			}
			else if (type.equals(TYPE_LOADTIME2)){
				// Place just before the </body> tag, after the technical measurement.
                nedstat = "<!-- Begin Sitestat4 Loadingtime2 code -->\n" +
                		"<script type=\'text/javascript\' src=\'" + getJavascriptFile(ctx) + "\'></script>\n" +
                		"<script type=\'text/javascript\'>ns_loadingtime2=(new Date()).getTime()</script>\n" +
                		"<!-- End Sitestat4 Loadingtime2 code -->";				
			}
			else if (type.equals(TYPE_CLICKIN)){
				if (getExternalUrl() == null) {
					throw new JspTagException("No external url provided for type " + getType() + ".");
				}
				nedstat = "<!-- Begin Sitestat4 Clickin code -->\n" +
						"<a href=\"http://" + getLandCode() + ".sitestat.com/" + getCustomerName() + "/" +
						getSiteName() + "/s?" + getCounterName() + "&amp;ns_type=clickin&amp;ns_url=" +
						getExternalUrl() + "\">" + getUrlText() + "</a>\n" +
						"<!-- End Sitestat4 Clickin code -->";			
			}
			else if (type.equals(TYPE_CLICKOUT)){
				if (getExternalUrl() == null){
					throw new JspTagException("No external url provided for type " + getType() + ".");
				}
				nedstat = "<!-- Begin Sitestat4 Clickout code -->\n" +
						"<a href=\"http://" + getLandCode() + ".sitestat.com/" + getCustomerName() + "/" +
						getSiteName() + "/s?" + getCounterName() + "&amp;ns_type=clickout&amp;ns_url=" +
						getExternalUrl() + "\">" + getUrlText() + "</a>\n" +
						"<!-- End Sitestat4 Clickout code -->";
			}		
			else if (type.equals(TYPE_BOOKMARK)){
				// Place in head section.
				nedstat = "<link rel=\"shortcut icon\" href=\"http://" + getLandCode() + ".sitestat.com/" +
				        getCustomerName() + "/" + getSiteName() + "/s?" + getCounterName() +
				        "&amp;ns_class=bookmark\" />";
				log.debug("nedstat is: " + nedstat + ".");
			}
			else {
				// Wrong/unsupported type.
				throw new JspTagException("Unsupported type: " + type);
			}
		
			ctx.getOut().print(nedstat);
		}
	}
    /**
     * @param ctx
     * @return
     */
    private String getJavascriptFile(PageContext ctx) {
        HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
        String javascriptFile = request.getContextPath() + "/js/sitestat.js";
        return javascriptFile;
    }
	public String getCounterName() {
		return counterName;
	}

	public void setCounterName(String counterName) {
		this.counterName = counterName;
	}

	public String getExternalUrl() {
		return externalUrl;
	}

	public void setExternalUrl(String externalUrl) {
		this.externalUrl = externalUrl;
	}

	public String getLable() {
		return lable;
	}

	public void setLable(String lable) {
		if (lable.toLowerCase().startsWith("category=")){
			this.lable = lable.toLowerCase();
		}
		else {
			this.lable = "category=" + lable.toLowerCase();
		}		
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getLandCode() {
		return landCode;
	}

	public void setLandCode(String landCode) {
		this.landCode = landCode;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getUrlText() {
		return urlText;
	}

	public void setUrlText(String urlText) {
		this.urlText = urlText;
	}
}
