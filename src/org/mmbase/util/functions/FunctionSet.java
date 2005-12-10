/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import java.util.*;

/**
 * The implementation of one set ('namespace') of functions. This is actually just a named FunctionProvider.
 *
 * @see    FunctionSets
 * @author Daniel Ockeloen
 * @author Michiel Meeuwissen
 * @version $Id: FunctionSet.java,v 1.8 2005-12-10 11:47:41 michiel Exp $
 * @since MMBase-1.8
 */
public class FunctionSet extends FunctionProvider {

    private String name;
    private String description;

    public FunctionSet(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
