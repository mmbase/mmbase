/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import org.mmbase.module.core.MMBase;
import org.mmbase.util.functions.*;
import org.mmbase.util.GenericResponseWrapper;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A Renderer implmentation based on a jsp.
 *
 * @author Michiel Meeuwissen
 * @version $Id: JspRenderer.java,v 1.8 2006-10-14 16:51:03 michiel Exp $
 * @since MMBase-1.9
 */
public class JspRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(JspRenderer.class);

    protected final String path;
    private final Block parent;

    public JspRenderer(String t, String p, Block parent) {
        super(t);
        path = p;
        this.parent = parent;
    }

    public Block getBlock() {
        return parent;
    }

    public String getPath() {
        return path;
    }

    protected Parameter[] getEssentialParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST};
    }

    public void render(Parameters blockParameters, Parameters frameworkParameters, Writer w) throws IOException {
        try {
            HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);
            GenericResponseWrapper respw = new GenericResponseWrapper(response);
            HttpServletRequest request = blockParameters.get(Parameter.REQUEST);
            for (Map.Entry<String, ?> entry : blockParameters.toMap().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            request.setAttribute(Renderer.KEY, this);

            Framework framework = MMBase.getMMBase().getFramework();
            String url = framework.getUrl(path, getBlock().getComponent(), blockParameters, frameworkParameters, false).toString();

            if (log.isDebugEnabled()) {
                log.debug("Block parameters      : [" + blockParameters + "]");
                log.debug("Framework parameters  : [" + frameworkParameters + "]");
                log.debug("Framework returned url: [" + url + "]");
            }
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(url);
            requestDispatcher.include(request, respw);
            w.write(respw.toString());
        } catch (ServletException se) {
            IOException e =  new IOException(se.getMessage());
            e.initCause(se);
            throw e;
        }
    }
}
