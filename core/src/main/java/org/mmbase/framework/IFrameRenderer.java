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
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The IFrameRenderer renders a page (in the current web-app) in an iframe. This makes it possible
 * to effortlessly use any jsp as a block (though not a very nice one, because it is rendered in an
 * iframe).
 * It supports generic properties, which are set as parameters on the iframe url.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9.1
 */
public class IFrameRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(IFrameRenderer.class);

    protected final Map<String, Object> properties = new TreeMap<String, Object>();

    private String url;

    public IFrameRenderer(Type t, Block parent) {
        super(t, parent);
    }

    public void setProperty(String k, Object v) {
        properties.put(k, v);
    }

    public void setUrl(String u) {
        url = u;
    }

    protected String getStyleClass() {
        return null;
    }

    protected String getIFrameUrl(Parameters blockParameters) {
        HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
        return org.mmbase.framework.basic.BasicUrlConverter.getUrl(url, properties, request, true);
    }

    public Parameter[] getParameters() {
        return new Parameter[] {Parameter.RESPONSE, Parameter.REQUEST};
    }

    /**
     */
    public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        try {
            HttpServletRequest request   = blockParameters.get(Parameter.REQUEST);
            HttpServletResponse response = blockParameters.get(Parameter.RESPONSE);
            String sc = getStyleClass();
            decorateIntro(hints, w, "iframe" + (sc == null ? "" : " " + sc));
            String u = getIFrameUrl(blockParameters);
            if (u.startsWith("/")) {
                u = response.encodeUrl(request.getContextPath() + u);
            }
            w.write("<iframe src=\"" + u + "\" />");
            decorateOutro(hints, w);
        } catch (IOException eio) {
            throw new FrameworkException(eio.getMessage(), eio);
        }
    }



}
