/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.lucene;

import java.util.*;
import org.apache.lucene.document.Document;

/**
 * One entry in an index. A bit like a Lucene Document, but takes into account the 'sub indices' and
 * makes it possible for {@link Indexer} to be implemented genericly, because the different
 * implementations of this interface define how index entries are added to a Lucene document.
 *
 * @author Michiel Meeuwissen.
 * @version $Id: IndexEntry.java,v 1.5 2006-09-06 16:47:14 michiel Exp $
 **/
public interface IndexEntry {

    /**
     * Writes this index entry to a lucene {@link org.apache.lucene.document.Document}.
     */
    void index(Document document);

    /**
     * Returns a Collection of 'sub definition', probably copied from the IndexDefinition which produces this entry.
     *
     */
    Collection<IndexDefinition> getSubDefinitions();


    /**
     * The identifier which can be used to retrieve this IndexEntry.
     */
    String  getIdentifier();

}


