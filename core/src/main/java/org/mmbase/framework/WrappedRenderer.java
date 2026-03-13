/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import org.mmbase.util.functions.*;
import java.io.Writer;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * A generic implementation of a Renderer that wraps another Renderer. It defines and implements the
 * parameter 'wraps' which you can point to another block.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9.1
 */
public abstract class WrappedRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(WrappedRenderer.class);


    protected Renderer wrapped;

    public WrappedRenderer(Type t, Block parent) {
        super(t, parent);
    }

    public void setWrapsBlock(String c) {
        setWraps(getBlock().getComponent().getBlock(c).getRenderer(getType()));
    }

    public void setWraps(Renderer r) {
        wrapped = r;
    }

    public Renderer getWraps() {
        return wrapped;
    }

    @Override public  Parameter<?>[] getParameters() {
        return getWraps().getParameters();
    }

    @Override public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        getWraps().render(blockParameters, w, hints);
    }

    @Override public String toString() {
        return "wrapped " + getWraps();
    }

    @Override public java.net.URI getUri(Parameters blockParameters, RenderHints hints) {
        return getWraps().getUri(blockParameters, hints);
    }
}
