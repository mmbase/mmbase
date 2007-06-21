/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.jumpers.strategies;

import org.mmbase.module.core.MMObjectNode;

/**
* UrlStrategy.
* This will return the url-field when the type of the object is 'urls'.
*
* @see org.mmbase.module.builders.Urls#getDefaultUrl(int) 
*
* @author Marcel Maatkamp, VPRO Digitaal
* @version $Id: UrlStrategy.java,v 1.2 2007-06-21 16:04:56 nklasens Exp $
*/

public class UrlStrategy extends JumperStrategy { 

    /** {@inheritDoc} */
    public boolean contains(MMObjectNode node) {
        if(node == null) {
            throw new IllegalArgumentException("node("+node+") is null!");
        }
        if(node.getBuilder().getTableName().equals("urls"))
            return true;
        else
            return false;
    }

    /** {@inheritDoc} */
    public String calculate(MMObjectNode node) { 
        if(node == null) {
            throw new IllegalArgumentException("node(" + node + ") is null!");
        }

        return node.getStringValue("url");
    }
}
