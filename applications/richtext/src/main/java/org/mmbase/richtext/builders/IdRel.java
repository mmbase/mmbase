/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.richtext.builders;

import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.module.core.MMObjectNode;

/**
 * Like InsRel, only with a GUIIndicator tailored for a relation with an 'id' field.
 *
 * @author Michiel Meeuwissen.
 * @version $Id$
 * @since MMBase-1.8
 */
public class IdRel extends InsRel {

    public static final String ID           = "id";
    public static final String VERSION_FROM = "version_from";
    public static final String VERSION_TO   = "version_to";

    @Override
    public String getGUIIndicator(MMObjectNode node) {
        return super.getGUIIndicator(node) + ":" + node.getStringValue(ID);
    }


}
