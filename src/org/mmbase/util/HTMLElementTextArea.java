/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;

/**
 * Generates a HTML Element: INPUT PASSWORD, uses this variables which are set in the<br>
 * super class (HTMLElement) to generate HTML:
 * <ul>
 * <li>boolean moreValues : if true it will take the first value of a list of items.</li>
 * <li>Vector valuesList  : The list of items. </li>
 * <li>String cols        : if not null the HTML tag COLS=cols is added </li>
 * <li>String rows        : if not null the HTML tag ROWS=rows is added </li>
 * </ul>
 *
 * @version 26-Sep-1996
 * @author Jan van Oosterom
 */
public class HTMLElementTextArea  extends HTMLElement {
    // Note: more appropriate would be to extend from HTMLElementText

    /**
     * Creates a HTMLElementTextArea
     */
    public HTMLElementTextArea() {
    }

    /**
     * Generates the HTML code.
     */
    protected String generate() {
        String html = "";
        if (moreValues) {
            if (valuesList != null) {
                String val = null;
                Enumeration e = valuesList.elements();
                if (e.hasMoreElements()) {
                    val = (String) e.nextElement();
                }
                html += "<textarea name=\"" + name+"\" ";
                if (cols != null) html += "cols=\"" + cols+"\" ";
                if (rows != null) html += " rows=\"" + rows+"\" ";
                html += ">";
                if (val != null) html += val;
                html += "</textarea>";
            }
        } else {
            html += "<textarea name=\"" + name+"\" ";
            if (cols != null) html += "cols=\"" + cols+"\" ";
            if (rows != null) html += " rows=\"" + rows+"\" ";
            html += ">";
            if (values != null) {
                if (values.charAt(0) == '\"') {
                    html += values.substring(1,values.length()-1);
                } else {
                    html +=    values;
                }
            }
            html += "</textarea>";
        }
        return html;
    }
}
