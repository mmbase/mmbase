/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import org.mmbase.module.corebuilders.InsRel;
import org.mmbase.module.core.*;

/**
 */
public class AuthRel extends InsRel {

	/**
	* setDefaults for a node
	*/
	@Override
    public void setDefaults(MMObjectNode node) {
	    super.setDefaults(node);
		node.setValue("creat",1);
		node.setValue("us",1);
		node.setValue("look",1);
	}


	@Override
    public String getGUIIndicator(String field, MMObjectNode node) {
		if (field.equals("creat")) {
			int val=node.getIntValue("creat");
			switch(val) {
				case -1: return("Nee");
				case 1: return("Ja");
				default: return("Unknown");
			}
		} else if (field.equals("look")) {
			int val=node.getIntValue("look");
			switch(val) {
				case -1: return("Nee");
				case 1: return("Ja");
				default: return("Unknown");
			}
		} else if (field.equals("us")) {
			int val=node.getIntValue("us");
			switch(val) {
				case -1: return("Nee");
				case 1: return("Ja");
				default: return("Unknown");
			}
		}
		return(null);
	}


}
