/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage;


/**
 * This interface contains functionality for retrieving a storage identifier - a name or id
 * suitable for storing the object.
 *
 * @author Pierre van Rooden
 * @since MMBase-1.7
 * @version $Id: Storable.java,v 1.2 2003-08-29 12:12:29 keesj Exp $
 */
public interface Storable {

    /**
     * Returns a storage identifier for this object.
     * This should return:
     * <ul>
     *  <li>For MMBase: the object storage element identifier as a String (i.e. fully expanded table name)</li>
     *  <li>For MMObjectBuilder: the builder storage element identifier as a String (i.e. fully expanded table name)</li>
     *  <li>For MMObjectNode: the object number as a Integer</li>
     *  <li>For FieldDefs: a storage-compatible field name as a String (if no such name exists a StorageException is thrown)</li>
     * </ul>
     * A Storable object (except for MMObjectNode) should retrieve its storage identifier using 
     * {@link StorageMagagerFactory.getStorageIdentifier()} when it is first instantiated.
     * @return the identifier
     */
    public Object getStorageIdentifier() throws StorageException;

    /**
     * Returns whether an object is kept in the storage (iow: is persistent).
     * Virtual fields or builders should return <code>false</code>. 
     * @return <code>true</code> if the object is kept in the storage
     */
    public boolean inStorage();

}
