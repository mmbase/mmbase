 /*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.applications.media.filters;

import org.mmbase.applications.media.urlcomposers.URLComposer;

/**
 * This can sort a list of URLComposers with the available ones on top.
 * @author  Michiel Meeuwissen
 * @version $Id: AvailableComparator.java,v 1.2 2003-02-05 14:28:49 michiel Exp $
 */
public class AvailableComparator extends  PreferenceComparator {

    public  AvailableComparator() {
    }
    
    public int getPreference(URLComposer ri) {        
        if (! ri.isAvailable()) {
            return -1; // very bad choice.
        } else {
            return 0;
        }
    }
}

