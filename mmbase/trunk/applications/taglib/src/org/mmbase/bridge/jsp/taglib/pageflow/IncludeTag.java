/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib.pageflow;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.servlet.jsp.JspTagException;

/**
 * Like UrlTag, but does not spit out an URL, but the page itself.
 * 
 * @author Michiel Meeuwissen
 */
public class IncludeTag extends UrlTag {
    public int doAfterBody() throws JspTagException {
        
        if (page == null) {
            throw new JspTagException("Attribute 'page' was not specified");
        }
        try {
            bodyContent.clear(); // newlines and such must be removed            

            String gotUrl = getUrl(false);// false: don't write &amp; tags but real &.
            
            // if not absolute, make it absolute:
            // (how does one check something like that?)
            String urlString;
            if (gotUrl.indexOf(':') == -1) {
                javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest)pageContext.getRequest();
                urlString = 
                    request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + new java.io.File(request.getRequestURI()).getParent().toString() + "/" + gotUrl;
            } else {
                urlString = gotUrl;
            }                
                
	    URL includeURL = new URL(urlString); 
	    HttpURLConnection connection = (HttpURLConnection) includeURL.openConnection();
	    connection.connect();
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader
                (connection.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null ) {
		bodyContent.write(line);
	    }
	    
            if (getId() != null) {
               getContextTag().register(getId(), bodyContent.getString());
            }
            bodyContent.writeOut(bodyContent.getEnclosingWriter());
        } catch (java.io.IOException e) {
            throw new JspTagException (e.toString());            
        }        
        return SKIP_BODY;
    }

}
