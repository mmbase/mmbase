/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
*/
package org.mmbase.bridge.util.fields;

import org.mmbase.bridge.*;
import org.mmbase.util.transformers.*;
import org.mmbase.util.functions.*;
import java.io.StringWriter;

/**
 *
 * @author Michiel Meeuwissen
 * @version $Id: ChecksumProcessorFactory.java,v 1.1 2005-09-09 20:20:35 michiel Exp $
 * @since MMBase-1.8
 */

public class ChecksumProcessorFactory implements ParameterizedProcessorFactory {

    protected static final Parameter[] PARAMS = new Parameter[] {
        new Parameter.Wrapper(ChecksumFactory.PARAMS),
        new Parameter("field", String.class, true)
    };

    private static final ParameterizedTransformerFactory factory = new ChecksumFactory();

    /**
     * Creates a parameterized processor.
     */
    public Processor createProcessor(Parameters parameters) {
        final ByteToCharTransformer transformer = (ByteToCharTransformer) factory.createTransformer(parameters);
        final String  sourceField = (String) parameters.get("field");
        return new CommitProcessor() {

                public Object process(Node node, Field field, Object value) {
                    return value;
                }
                public void commit(Node node, Field field) {
                    StringWriter writer = new StringWriter();
                    transformer.transform(node.getInputStreamValue(sourceField), writer);
                    node.setStringValue(field.getName(), writer.toString());
                }
            };
    }

    /**
     * Create  empty <code>Parameters</code> object for use with {@link #createProcessor}.
     */
    public Parameters createParameters() {
        return new ParametersImpl(PARAMS);
    }

}
