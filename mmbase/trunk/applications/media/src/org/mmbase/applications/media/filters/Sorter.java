 /*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.applications.media.filters;

import org.mmbase.applications.media.urlcomposers.URLComposer;
import java.util.*;
import org.mmbase.util.XMLBasicReader;
import org.w3c.dom.Element;

/**
 * Implements a Filter as a Sorter. That means that it only sorts,
 * and the implementation is done by implementating it as an
 * Sorter.
 *
 * @author  Michiel Meeuwissen
 */
abstract public class Sorter implements Comparator, Filter {

    /**
     * Implement this.
     */

    abstract  protected int compareURLComposer(URLComposer o1, URLComposer o2); 
    
    public void configure(XMLBasicReader reader, Element e) {
        // nothing to be configured on default.
    }
         
    final public int compare(Object o1, Object o2) {
        URLComposer ri1  = (URLComposer) o1;
        URLComposer ri2  = (URLComposer) o2;
        return compareURLComposer(ri1, ri2);
    }

    public List filter(List urlcomposers) {
        Collections.sort(urlcomposers, this);
        return urlcomposers;
    }
}

