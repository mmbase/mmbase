/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

/**
 * The data associated with 'XML' values ({@link org.w3c.dom.Document}). At the moment this class is
 * empty, but of course we forsee the possibility for  restrictions on doc-type.
 *
 * @author Michiel Meeuwissen
 * @version $Id: XmlDataType.java,v 1.4 2006-01-06 17:19:21 michiel Exp $
 * @since MMBase-1.8
 */
public class XmlDataType extends BasicDataType {

    private static final long serialVersionUID = 1L; // increase this if object serialization changes (which we shouldn't do!)

    /**
     * Constructor for xml data type.
     * @param name the name of the data type
     */
    public XmlDataType(String name) {
        super(name, org.w3c.dom.Document.class);
    }

}
