/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge.util.fields;

import org.mmbase.bridge.*;
import java.util.*;

/**
 * Chains a bunch of other processors into one new processor.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ChainedProcessor.java,v 1.3 2003-12-23 19:58:18 michiel Exp $
 * @since MMBase-1.7
 */

public class ChainedProcessor implements Processor {

    private List processors = new ArrayList();

    public ChainedProcessor add(Processor proc) {
        processors.add(proc);
        return this;
    }

    public Object process(Node node, Field field, Object value) {
        
        Iterator i = processors.iterator();
        while (i.hasNext()) {
            Processor proc = (Processor) i.next();
            value = proc.process(node, field, value);
        }
        return value;
    }
    

}
