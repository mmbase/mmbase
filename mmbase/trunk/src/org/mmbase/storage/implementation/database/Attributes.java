/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.implementation.database;

/**
 * This class defines the attributes names used by the default database storage manager classes.
 * Specific storage managers may ignore or add their own attributes.
 *
 * @author Pierre van Rooden
 * @since MMBase-1.7
 * @version $Id: Attributes.java,v 1.6 2003-09-18 12:20:26 pierre Exp $
 */
public final class Attributes {

    /**
     * Attribute: <code>database-data-source</code>.
     * The data source object used by the storage layer.
     * This attribute is set by the storagelayer and returns a javax.sql.DataSource object.
     * You should not set or configure this attribute (but you can retrieve it).
     */
    public static final String DATA_SOURCE = "database-data-source";

    /**
     * Option: <code>database-supports-transactions</code>.
     * When true, the database supports transactions.
     * The default is determined form the database, but you can override it.
     */
    public static final String SUPPORTS_TRANSACTIONS = "database-supports-transactions";

    /**
     * Option: <code>database-stores-binary-as-file</code>.
     * When true, binary data is stored on disk, rather than in the database.
     * If you set this option ou should also set the attribute {@link #BINARY_FILE_PATH}
     * The default is <code>false</code>
     */
    public static final String STORES_BINARY_AS_FILE = "database-stores-binary-as-file";

    /**
     * Attribute: <code>database-binary-file-path</code>.
     * The path to the directyory where binary files are to be stored if {@link #STORES_BINARY_AS_FILE} is true.
     * The default is <code>false</code>
     */
    public static final String BINARY_FILE_PATH = "database-binary-file-path";

    /**
     * Option: <code>database-force-encode-text</code>.
     * If true, the database layer will explicitly decode/encode strings using the MMBase encoding when
     * storing and retrieving text from the database.
     * The default is <code>false</code>
     */
    public static final String FORCE_ENCODE_TEXT = "database-force-encode-text";

    /**
     * Option: <code>database-supports-blob</code>.
     * When true, the driver/database used supports the JDBC getBlob() method.
     * The default is <code>false</code>
     */
    public static final String SUPPORTS_BLOB = "database-supports-blob";

    /**
     * Option: <code>database-supports-composite-index</code>.
     * When true, the database uses composite indices for 'key' fields.
     * When false, it uses single indices (a separate index for each field)
     * The default is <code>true</code>
     */
    public static final String SUPPORTS_COMPOSITE_INDEX = "database-supports-composite-index";

    /**
     * Attribute: <code>database-transaction-isolation-level</code>.
     * The transaction isolation level used for connections to the database.
     * This determines the level of transaction support.
     * The default is determined from the database metadata.
     */
    public static final String TRANSACTION_ISOLATION_LEVEL = "database.transaction-isolation-level";

    /**
     * Option: <code>database-supports-data-definition</code>.
     * If true, the data definiton (table structure) can be changed using ALTER TABLE statements.
     * Some databses (such as Informix) may have trouble with ALTER TABLE statements on OO-tables.
     * Turn this option false for tehse databses.
     * The default is <code>true</code>
     */
    public static final String SUPPORTS_DATA_DEFINITION = "database-supports-data-definition";



}
