package nl.didactor.taglib;

import java.io.IOException;
import java.util.*;
import java.text.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import javax.servlet.Servlet;
import org.mmbase.bridge.jsp.taglib.*;
import nl.didactor.component.Component;

/**
 * GetSettingTag: retrieve a setting for a component
 * @author Johannes Verelst &lt;johannes.verelst@eo.nl&gt;
 */
public class GetSettingTag extends CloudReferrerTag { 
    private String component;
    private String setting;
    private String[] arguments = new String[0];

    /**
     * Set the value for the 'component' argument of the GetSetting tag
     * @param component Component value
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * Set the value for the 'setting' argument of the GetSetting tag
     * @param setting Setting name
     */
    public void setSetting(String setting) {
        this.setting = setting;
    }

    /**
     * Set the value for the 'arguments' argument of the GetSetting tag
     * @param arguments The optional arguments
     */
    public void setArguments(String arguments) {
        StringTokenizer st = new StringTokenizer(arguments, ",");
        if (st.countTokens() > 0) {
            this.arguments = new String[st.countTokens()];
            for (int i=0; i<st.countTokens(); i++) {
                this.arguments[i] = st.nextToken();
            }
        }
    }

    /**
     * Retrieve the value for the setting based on:
     * <ul>
     *   <li>Given component name (is used to find the correct class to handle the setting)</li>
     *   <li>Given setting name</li>
     *   <li>Context in which this tag is called (values are passed to the component)</li>
     *   <li>Cloud in which this setting tag is used</li>
     * </ul>
     */
    public int doStartTag() throws JspTagException {
        Component comp = Component.getComponent(component);
        if (comp == null) {
            return SKIP_BODY;
        }

        String value = null;
        
        try {
            value = comp.getSetting(setting, getCloudVar(), getContextProvider().getContextContainer(), arguments);
        } catch (IllegalArgumentException e) {
            throw new JspTagException(e.getMessage());
        }
    
        try {
            pageContext.getOut().print(value);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
        return SKIP_BODY;
    }
}
