/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.net.URI;
import java.util.*;
import java.io.*;
import org.mmbase.util.functions.*;

/**
 * This renderer simply calls the methods of a number of other Renderers sequentially. Only
 * {@link Renderer.Type#HEAD} renderers can be chained, because they don't have to produce a well defined
 * block.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9
 */

public class ChainedRenderer extends AbstractRenderer {

    protected final List<Renderer> chain = new ArrayList<Renderer>();
    protected Parameter[] parameters = Parameter.EMPTY;

    public ChainedRenderer(Type t, Block parent) {
        super(t, parent);
    }

    public void add(Renderer render) {
        chain.add(render);
        List<Parameter> params = new ArrayList<Parameter>(Arrays.asList(parameters));
        params.addAll(Arrays.asList(render.getParameters()));
        parameters = params.toArray(Parameter.EMPTY);
    }

    @Override
    public  Parameter[] getParameters() {
        return parameters;
    }


    @Override
    public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        for (Renderer renderer : chain) {
            renderer.render(blockParameters, w, hints);
        }
    }

    @Override
    public String toString() {
        return chain.toString();
    }

    @Override
    public URI getUri(Parameters blockParameters, RenderHints hints) {
        if (!chain.isEmpty()) {
            return chain.get(0).getUri(blockParameters, hints);
        } else {
            return null;
        }
    }
}
