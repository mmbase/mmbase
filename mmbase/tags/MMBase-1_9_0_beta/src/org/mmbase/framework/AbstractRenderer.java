/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.io.IOException;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * Abstract renderer implementation which implements getType and getBlock.
 *
 * @author Michiel Meeuwissen
 * @version $Id: AbstractRenderer.java,v 1.11 2008-02-23 12:15:54 michiel Exp $
 * @since MMBase-1.9
 */
abstract public class AbstractRenderer implements Renderer {

    private static final Logger log = Logging.getLoggerInstance(AbstractRenderer.class);

    protected final Type type;
    private final Block parent;

    public AbstractRenderer(Type t, Block p) {
        type = t;
        parent = p;
    }
    public AbstractRenderer(String t, Block p) {
        type = Type.valueOf(t);
        parent = p;
    }

    public Type getType() {
        return type;
    }

    public Block getBlock() {
        return parent;
    }

    public java.net.URI getUri() {
        return null;
    }

    protected void decorateIntro(HttpServletRequest request, Writer w, String extraClass)  throws IOException {
        w.write("<div id=\"" + request.getAttribute(Framework.COMPONENT_ID_KEY) + "\"");
        w.write(" class=\"");
        if (extraClass != null) {
            w.write(extraClass);
            w.write(' ');
        }
        w.write("mm_c_");
        w.write(getBlock().getComponent().getName());
        w.write(" mm_c_b_");
        w.write(getBlock().getName());
        w.write(" " + request.getAttribute(Framework.COMPONENT_CLASS_KEY));
        w.write("\">");
    }
    protected void decorateOutro(HttpServletRequest request, Writer w) throws IOException {
        w.write("</div>");
    }

}
