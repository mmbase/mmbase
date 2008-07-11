/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.util.*;


/**
 * See http://java.sun.com/docs/books/tutorial/jmx/mbeans/standard.html
 * @author Michiel Meeuwissen
 * @version $Id: CacheMBean.java,v 1.1 2008-07-11 12:46:44 michiel Exp $
 * @since MMBase-1.9
 */
public interface CacheMBean {


    String getName();
    String getDescription();

    /**
     * @see Cache#clear()
     */
    void clear();

    /**
     * @see Cache#getSize()
     */
    int getSize();
    /**
     * @see Cache#getHits()
     */
    long getHits();
    /**
     * @see Cache#getMisses()
     */
    long getMisses();
    /**
     * @see Cache#getPuts()
     */
    long getPuts();
    /**
     * @see Cache#getMaxSize()
     */
    int getMaxSize();
    void setMaxSize(int i);
    /**
     * @see Cache#isActive
     */
    void setActive(boolean b);
    boolean isActive();
    /**
     * @see Cache#getMaxEntrySize
     */
    int getMaxEntrySize();
    /**
     * @see Cache#getRatio
     */
    double getRatio();
    //Class getImplementation();




}
