/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.functions;

import org.mmbase.bridge.DataType;

/**
 * If there is not Parameter definition array available you could try it with this specialization, which does not need one.
 * You loose al checking on type and availability. It should only be used as a last fall back and accompanied by warnings.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.7
 * @version $Id: AutodefiningParameters.java,v 1.6 2005-03-16 15:59:51 michiel Exp $
 * @see Parameter
 */

public class AutodefiningParameters extends ParametersImpl {
    //private static Logger log = Logging.getLoggerInstance(Parameters.class);


    public AutodefiningParameters() {
        super(new DataType[0]);
    }
    /**
     * Sets the value of an argument, and grows the definition array.
     */
    public Parameters set(String arg, Object value) {
        DataType[] newDef = new DataType[definition.length + 1];
        for (int i = 0; i < definition.length; i++) {
            newDef[i] = definition[i];
        }
        newDef[newDef.length - 1] = new Parameter(arg, value == null ? Object.class : value.getClass());

        definition = newDef;
        backing.put(arg, value);
        return this;
    }

    public boolean containsParameter(Parameter param) {
        return true;
    }

}
