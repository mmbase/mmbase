/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.jsp.taglib;

import javax.servlet.jsp.tagext.VariableInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;


/**
* The Export tag exports one jsp variable. Some other tags, such as
* CloudTag and NodeProviders can also export jsp variables by
* themselves.
*
* @author Michiel Meeuwissen
*/
public class ExportTEI extends TagExtraInfo {
    
    public ExportTEI() { 
        super(); 
    }

    public VariableInfo[] getVariableInfo(TagData data) {
        VariableInfo[] variableInfo =  new VariableInfo[1];
        
        String typeAttribute    = (String) data.getAttribute("type"); 
        if (typeAttribute == null) typeAttribute = "java.lang.Object";

        String type = "java.lang.Object";

        if ("Object".equals(typeAttribute)) {
            type = "java.lang.Object";
        } else if ("String".equals(typeAttribute)) {
            type = "java.lang.String";
        } else if ("Node".equals(typeAttribute)) {
            type = "org.mmbase.bridge.Node";
        } else {
            //type = "org.lang.Object"; 
            throw new RuntimeException("Unknown type '" + typeAttribute + "'");
        }

        String jspvarAttribute  = (String) data.getAttribute("jspvar"); 

        variableInfo[0] =  new VariableInfo(jspvarAttribute,
                                            type,
                                            true,
                                            VariableInfo.AT_END);
        return variableInfo;
    }        
}
