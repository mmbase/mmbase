/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.media.filters;

import org.mmbase.applications.media.urlcomposers.URLComposer;
import java.util.ListIterator;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.Element;
import java.util.List;

/**
 * This removes all URLComposers which are not available.
 * @author  Michiel Meeuwissen
 * @version $Id$
 */
public class AvailableFilter implements Filter {

    @Override
    public List<URLComposer> filter(List<URLComposer> urlcomposers) {
        ListIterator<URLComposer> i = urlcomposers.listIterator();
        while (i.hasNext()) {
            URLComposer uc = i.next();
            if (! uc.isAvailable()) {
                i.remove();
            }
        }
        return urlcomposers;

    }
    @Override
    public void configure(DocumentReader reader, Element e) {
        // not needed
    }

}

