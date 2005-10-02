/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.fields;

import org.mmbase.util.functions.Parameters;
import org.mmbase.util.transformers.*;


/**
 * Wraps a {@link org.mmbase.util.transformers.ParameterizedTransformerFactory} (it <em>must</em> produces
 * CharTransformer), to be a ParameterizedProcessorFactory. Of course based on {@link
 * org.mmbase.brige.util.fields.CharTransformerProcessor}.
 *
 * @author Michiel Meeuwissen
 * @version $Id: CharTransformerProcessorFactory.java,v 1.2 2005-10-02 16:25:19 michiel Exp $
 * @since MMBase-1.8
 */

public class CharTransformerProcessorFactory implements ParameterizedProcessorFactory {

    private ParameterizedTransformerFactory factory;

    public CharTransformerProcessorFactory(ParameterizedTransformerFactory f) {
        factory = f;
    }

    public Processor createProcessor(Parameters parameters) {
        CharTransformer ct = (CharTransformer) factory.createTransformer(parameters);
        return new CharTransformerProcessor(ct);
    }


    public Parameters createParameters() {
        return factory.createParameters();
    }

}
