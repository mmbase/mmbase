/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage;

/**
 * This class defines the attributes names used by the standard storage manager classes.
 * Specific storage managers may ignore or add their own attributes.
 *
 * @author Pierre van Rooden
 * @since MMBase-1.7
 * @version $Id: Attributes.java,v 1.1 2003-08-21 09:59:27 pierre Exp $
 */
public final class Attributes {

    /**
     * Attribute: <code>defaultStorageIdentifierPrefix</code>.
     * When the storage manager encounters a disallowed fieldname for which no replacement is created,
     * it attempts to create a replacement fieldname by prefixing the field with the value set in this property.
     * If this property is not set, the manager will instead issue a StoragException, explaining the fieldname is disallowed
     * By default this attribute is not set.
     */
    public static final String DISALLOWED_FIELD_PREFIX = "defaultStorageIdentifierPrefix";

    /**
     * Option: <code>disallowed-fields-case-sensitive</code>.
     * if <code>true</code>, matching MMBase fieldnames with the disallowed fieldnames list is case-sensitive.
     * By default, this option is <code>false</code>.
     * Note that you can specify this attribute seperately, but the "case-sensitive" attribute
     * of the "disallowedfields" tag overrides this attribute.
     */
    public static final String DISALLOWED_FIELD_CASE_SENSITIVE = "disallowed-fields-case-sensitive";

    /**
     * Option: <code>enforce.disallowed.fields</code>.
     * if <code>true</code>, the storage layer alwyas fails when encountering fieldnames that are reserved sql keywords,
     * and for which no alternate name is available.
     * If <code>false</code>, the layer will ignore the restriction and attempt to use the reserved word (leaving any
     * errors to the underlying implementation). <br />
     * By default, this option is <code>false</code>.
     * Note that you can specify this attribute seperately, but the "enforce" attribute
     * of the "disallowedfields" tag overrides this attribute.
     */
    public static final String ENFORCE_DISALLOWED_FIELDS = "enforce.disallowed.fields";

    /**
     * Attribute: <code>storage-identifier-case</code>.
     * if set, the storage identifiers for builders and fieldnames are converted to lowercase (if the value is 'lower') or
     * uppercase (if the value is 'upper') before they are passed to the storage manager.
     * If you specify another value this attribute is ignored.
     * This ensures that field or builder names that differ only by case will return the same storage identifier
     * (and thus point to the same storage element).
     * You may need to set this value for some specific storage implementations. I.e. some databases expect table or fieldname to be
     * case sensitive, or expect them to be uppercase.
     * By default, this option is not set.
     */
    public static final String STORAGE_IDENTIFIER_CASE = "storage-identifier-case";

    /**
     * Atribute: <code>default-storage-identifier-prefix</code>.
     * A default prefix to place in front of diallowedf fieldnames to make them suitabel for use in a storage layer.
     * By default, this option is not set.
     */
    public static final String DEFAULT_STORAGE_IDENTIFIER_PREFIX = "default-storage-identifier-prefix";

}

