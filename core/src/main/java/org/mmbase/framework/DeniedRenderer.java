/*

  This software is OSI Certified Open Source Software.
  OSI Certified is a certification mark of the Open Source Initiative.

  The license (Mozilla version 1.0) can be read at the MMBase site.
  See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.net.URI;
import java.util.*;

import javax.servlet.http.*;
import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.GenericResponseWrapper;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * {@link Renderer} to be used in case of access denied (status 401).
 * If rendering of a certain block proved to be not allowed for the current user.
 * Requests get dispatched to a block 'core/401.jspx' with a more or less
 * userfriendly message.
 *
 * @author Michiel Meeuwissen
 * @author Andr&eacute; van Toly
 * @version $Id$
 * @since MMBase-1.9
 */

public class DeniedRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(DeniedRenderer.class);


    public DeniedRenderer(Type t, Block parent) {
        super(t, parent);
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST};
    }

    @Override
    public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        switch(getType()) {
        case BODY:
            try {
                HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
                HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);

                GenericResponseWrapper respw = new GenericResponseWrapper(response);
                String url = JspRenderer.JSP_ROOT + "core/401.jspx";
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(url);

                requestDispatcher.include(request, respw);
                if (log.isDebugEnabled()) {
                    log.debug("Using block.parameters " +  blockParameters.toMap());
                    log.debug("401 denied, rendering: " + url);
                }
                w.write(respw.toString());
            } catch (ServletException se) {
                throw new FrameworkException(se.getMessage(), se);
            } catch (IOException eio) {
                throw new FrameworkException(eio.getMessage(), eio);
            }
            break;
        default:
        }
    }

    @Override
    public URI getUri(Parameters blockParameters, RenderHints hints) {
        return null;
    }

    public String toString() {
        return "DENIED";
    }

}
