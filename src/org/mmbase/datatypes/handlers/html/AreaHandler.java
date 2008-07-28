/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.datatypes.handlers.html;

import org.mmbase.datatypes.handlers.*;
import org.mmbase.bridge.*;
import org.mmbase.util.Casting;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id: AreaHandler.java,v 1.1 2008-07-28 16:47:31 michiel Exp $
 * @since MMBase-1.9.1
 */

public class AreaHandler extends TextHandler {

    private int cols = 80;
    private int rows = -1;

    public void setCols(int c) {
        cols = c;
    }

    public void setRows(int r) {
        rows = r;
    }

    protected int getCols(Field field) {
        return cols;
    }
    protected int getRows(Field field) {
        return rows == -1 ? (field.getMaxLength() > 2048 ? 10 : 5) : rows;
    }

    protected void appendClasses(StringBuilder buf, Node node, Field field) {
        buf.append(field.getMaxLength() > 2048 ? "big " : "small ");
        super.appendClasses(buf, node, field);
    }


    public String input(Request request, Node node, Field field, boolean search) {
        if (search) {
            return super.input(request, node, field, search);
        } else {
            StringBuilder buffer =  new StringBuilder();
            buffer.append("<textarea class=\"big");
            appendClasses(buffer, node, field);
            buffer.append("\" ");
            appendNameId(buffer, request, field);
            buffer.append(">");
            Object value = request.getValue(field);
            if ("".equals(value)) {
                // This can be needed because:
                // If included, e.g. with xmlhttprequest,
                // the textarea can collaps: <textarea />
                // This does not work in either FF or IE if the contenttype is text/html
                // The more logical contenttype application/xml or text/xml would make it behave normally in FF,
                // but that is absolutely not supported by IE. IE sucks. FF too, but less so.
                //
                // Any how, in short, sometimes you _must_ output one space here if empty otherwise.
                // I _reall_ cannot think of anything more sane then this.
                // e.g. <!-- empty --> would simply produce a textarea containing that...
                // also <![CDATA[]]> produces a textarea containing that...
                //
                // HTML is broken.

                buffer.append(' ');
            } else {
                buffer.append(XML.transform(Casting.toString(value)));
            }
            buffer.append("</textarea>");
            return buffer.toString();
        }
    }
}
