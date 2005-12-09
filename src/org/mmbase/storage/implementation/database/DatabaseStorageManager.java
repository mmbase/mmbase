/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.implementation.database;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;


import org.mmbase.bridge.Field;
import org.mmbase.bridge.NodeManager;
import org.mmbase.cache.Cache;
import org.mmbase.core.CoreField;
import org.mmbase.core.util.Fields;
import org.mmbase.datatypes.DataTypes;
import org.mmbase.datatypes.DataType;
import org.mmbase.datatypes.BasicDataType;
import org.mmbase.module.core.*;
import org.mmbase.storage.*;
import org.mmbase.storage.util.*;
import org.mmbase.util.Casting;
import org.mmbase.util.transformers.*;

import org.mmbase.util.logging.*;

/**
 * A JDBC implementation of an object related storage manager.
 * @javadoc
 *
 * @author Pierre van Rooden
 * @since MMBase-1.7
 * @version $Id: DatabaseStorageManager.java,v 1.138 2005-12-09 14:25:32 nklasens Exp $
 */
public class DatabaseStorageManager implements StorageManager {

    /** Max size of the object type cache */
    public static final int OBJ2TYPE_MAX_SIZE = 20000;

    // contains a list of buffered keys
    protected static final List sequenceKeys = new LinkedList();

    private static final Logger log = Logging.getLoggerInstance(DatabaseStorageManager.class);

    private static final Blob BLOB_SHORTED = new InputStreamBlob(null, -1);

    private static final CharTransformer UNICODE_ESCAPER = new org.mmbase.util.transformers.UnicodeEscaper();

    // maximum size of the key buffer
    protected static Integer bufferSize = null;

    /**
     * This sets contains all existing tables which could by associated with MMBase builders. This
     * is because they are queried all at once, but requested for existance only one at a time.
     * @since MMBase-1.7.4
     */
    protected static Set tableNameCache = null;

    /**
     * Whether the warning about blob on legacy location was given.
     */
    private static boolean legacyWarned = false;

    /**
     * The cache that contains the last X types of all requested objects
     * @since 1.7
     */
    private static Cache typeCache;

    static {
        typeCache = new Cache(OBJ2TYPE_MAX_SIZE) {
            public String getName()        { return "TypeCache"; }
            public String getDescription() { return "Cache for node types";}
        };
        typeCache.putCache();
    }

    /**
     * The factory that created this manager
     */
    protected DatabaseStorageManagerFactory factory;

    /**
     * The datasource through which to access the database.
     */
    protected DataSource dataSource;

    /**
     * The currently active Connection.
     * This member is set by {!link #getActiveConnection()} and unset by {@link #releaseActiveConnection()}
     */
    protected Connection activeConnection;

    /**
     * <code>true</code> if a transaction has been started.
     * This member is for state maitenance and may be true even if the storage does not support transactions
     */
    protected boolean inTransaction = false;

    /**
     * The transaction issolation level to use when starting a transaction.
     * This value is retrieved from the factory's {@link Attributes#TRANSACTION_ISOLATION_LEVEL} attribute, which is commonly set
     * to the highest (most secure) transaction isolation level available.
     */
    protected int transactionIsolation = Connection.TRANSACTION_NONE;

    /**
     * Pool of changed nodes in a transaction
     */
    protected Map changes;

    /**
     * Constructor
     */
    public DatabaseStorageManager() {}

    protected long getLogStartTime() {
        return System.currentTimeMillis();
    }
    
    // for debug purposes
    protected final void logQuery(String msg, long startTime) {
        if (log.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            log.debug("Time:" + (now - startTime) + " Query :" + msg);
            log.trace(Logging.stackTrace());
        }
    }

    // javadoc is inherited
    public double getVersion() {
        return 1.0;
    }

    // javadoc is inherited
    public void init(StorageManagerFactory factory) throws StorageException {
        this.factory = (DatabaseStorageManagerFactory)factory;
        dataSource = (DataSource)factory.getAttribute(Attributes.DATA_SOURCE);
        if (factory.supportsTransactions()) {
            transactionIsolation = ((Integer)factory.getAttribute(Attributes.TRANSACTION_ISOLATION_LEVEL)).intValue();
        }
        // determine generated key buffer size
        if (bufferSize==null) {
            bufferSize = new Integer(1);
            Object bufferSizeAttribute = factory.getAttribute(Attributes.SEQUENCE_BUFFER_SIZE);
            if (bufferSizeAttribute != null) {
                try {
                    bufferSize = Integer.valueOf(bufferSizeAttribute.toString());
                } catch (NumberFormatException nfe) {
                    // remove the SEQUENCE_BUFFER_SIZE attribute (invalid value)
                    factory.setAttribute(Attributes.SEQUENCE_BUFFER_SIZE,null);
                    log.error("The attribute 'SEQUENCE_BUFFER_SIZE' has an invalid value(" +
                        bufferSizeAttribute + "), will be ignored.");
                }
            }
        }

    }

    /**
     * Obtains an active connection, opening a new one if needed.
     * This method sets and then returns the {@link #activeConnection} member.
     * If an active connection was allready open, and the manager is in a database transaction, that connection is returned instead.
     * Otherwise, the connection is closed before a new one is opened.
     * @throws SQLException if opening the connection failed
     */
    protected Connection getActiveConnection() throws SQLException {
        if (activeConnection != null) {
            if (factory.supportsTransactions() && inTransaction) {
                return activeConnection;
            } else {
                releaseActiveConnection();
            }
        }
        activeConnection = dataSource.getConnection();
        // set autocommit to true
        activeConnection.setAutoCommit(true);
        return activeConnection;
    }

    /**
     * Safely closes the active connection.
     * If a transaction has been started, the connection is not closed.
     */
    protected void releaseActiveConnection() {
        if (!(inTransaction && factory.supportsTransactions()) && activeConnection != null) {
            try {
                // ensure that future attempts to obtain a connection (i.e.e if it came from a pool)
                // start with autocommit set to true
                // needed because Query interface does not use storage layer to obtain transactions
                activeConnection.setAutoCommit(true);
                activeConnection.close();
            } catch (SQLException se) {
                // if something went wrong, log, but do not throw exceptions
                log.error("Failure when closing connection: " + se.getMessage());
            }
            activeConnection = null;
        }
    }

    // javadoc is inherited
    public void beginTransaction() throws StorageException {
        if (inTransaction) {
            throw new StorageException("Cannot start Transaction when one is already active.");
        } else {
            if (factory.supportsTransactions()) {
                try {
                    getActiveConnection();
                    activeConnection.setTransactionIsolation(transactionIsolation);
                    activeConnection.setAutoCommit(false);
                } catch (SQLException se) {
                    releaseActiveConnection();
                    inTransaction = false;
                    throw new StorageException(se);
                }
            }
            inTransaction = true;
            changes = new HashMap();
        }

    }

    // javadoc is inherited
    public void commit() throws StorageException {
        if (!inTransaction) {
            throw new StorageException("No transaction started.");
        } else {
            inTransaction = false;
            if (factory.supportsTransactions()) {
                if (activeConnection == null) {
                    throw new StorageException("No active connection");
                }

                try {
                    activeConnection.commit();
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                    factory.getChangeManager().commit(changes);
                }
            }
        }
    }

    // javadoc is inherited
    public boolean rollback() throws StorageException {
        if (!inTransaction) {
            throw new StorageException("No transaction started.");
        } else {
            inTransaction = false;
            if (factory.supportsTransactions()) {
                try {
                    activeConnection.rollback();
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                    changes.clear();
                }
            }
            return factory.supportsTransactions();
        }
    }

    /**
     * Commits the change to a node.
     * If the manager is in a transaction (and supports it), the change is stored in a
     * {@link #changes} object (to be committed after the transaction ends).
     * Otherwise it directly commits and broadcasts the changes
     * @param node the node to register
     * @param change the type of change: "n": new, "c": commit, "d": delete, "r" : relation changed
     */
    protected void commitChange(MMObjectNode node, String change) {
        if (inTransaction && factory.supportsTransactions()) {
            changes.put(node, change);
        } else {
            factory.getChangeManager().commit(node, change);
            log.debug("Commited node");
        }
    }

    public int createKey() throws StorageException {
        log.debug("Creating key");
        synchronized (sequenceKeys) {
            log.debug("Acquired lock");
            // if sequenceKeys conatins (buffered) keys, return this
            if (sequenceKeys.size() > 0) {
                return ((Integer)sequenceKeys.remove(0)).intValue();
            } else try {
                getActiveConnection();
                Statement s;
                String query;
                Scheme scheme = factory.getScheme(Schemes.UPDATE_SEQUENCE, Schemes.UPDATE_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), bufferSize });
                    long startTime = getLogStartTime();
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                }
                scheme = factory.getScheme(Schemes.READ_SEQUENCE, Schemes.READ_SEQUENCE_DEFAULT);
                query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), bufferSize });
                s = activeConnection.createStatement();
                try {
                    long startTime = getLogStartTime();
                    ResultSet result = s.executeQuery(query);
                    logQuery(query, startTime);
                    try {
                        if (result.next()) {
                            int keynr = result.getInt(1);
                            // add remaining keys to sequenceKeys
                            for (int i = 1; i < bufferSize.intValue(); i++) {
                                sequenceKeys.add(new Integer(keynr+i));
                            }
                            return keynr;
                        } else {
                            throw new StorageException("The sequence table is empty.");
                        }
                    } finally {
                        result.close();
                    }
                } finally {
                    s.close();
                }
            } catch (SQLException se) {
                log.error(Logging.stackTrace(se));
                // wait 2 seconds, so any locks that were claimed are released.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException re) {}
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    // javadoc is inherited
    public String getStringValue(MMObjectNode node, CoreField field) throws StorageException {
        try {
            MMObjectBuilder builder = node.getBuilder();
            Scheme scheme = factory.getScheme(Schemes.GET_TEXT_DATA, Schemes.GET_TEXT_DATA_DEFAULT);
            String query = scheme.format(new Object[] { this, builder, field, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            ResultSet result = s.executeQuery(query);
            try {
                if ((result != null) && result.next()) {
                    String rvalue = (String) getStringValue(result, 1, field, false);
                    result.close();
                    s.close();
                    return rvalue;
                } else {
                    if (result != null) result.close();
                    s.close();
                    throw new StorageException("Node with number " + node.getNumber() + " not found.");
                }
            } finally {
                result.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Retrieve a text for a specified object field.
     * The default method uses {@link ResultSet#getString(int)} to obtain text.
     * Override this method if you want to optimize retrieving large texts,
     * i.e by using clobs or streams.
     * @param result the resultset to retrieve the text from
     * @param index the index of the text in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved text, <code>null</code> if no text was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected Object getStringValue(ResultSet result, int index, CoreField field, boolean mayShorten) throws StorageException, SQLException {
        String untrimmedResult = null;
        if (field != null && (field.getStorageType() == Types.CLOB || field.getStorageType() == Types.BLOB || factory.hasOption(Attributes.FORCE_ENCODE_TEXT))) {
            InputStream inStream = result.getBinaryStream(index);
            if (result.wasNull()) {
                return null;
            }
            if (mayShorten && shorten(field)) {
                return MMObjectNode.VALUE_SHORTED;
            }
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int c = inStream.read();
                while (c != -1) {
                    bytes.write(c);
                    c = inStream.read();
                }
                inStream.close();
                String encoding = factory.getMMBase().getEncoding();
                if (encoding.equalsIgnoreCase("ISO-8859-1")) {
                    // CP 1252 only fills in the 'blanks' of ISO-8859-1,
                    // so it is save to upgrade the encoding, in case accidentily those bytes occur
                    encoding = "CP1252";
                }
                untrimmedResult = new String(bytes.toByteArray(), encoding);
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        } else {
            untrimmedResult = result.getString(index);
            if (factory.hasOption(Attributes.LIE_CP1252)) {
                try {
                    String encoding = factory.getMMBase().getEncoding();
                    if (encoding.equalsIgnoreCase("ISO-8859-1")) {
                        untrimmedResult = new String(untrimmedResult.getBytes("ISO-8859-1"), "CP1252");
                    }
                 } catch(java.io.UnsupportedEncodingException uee) {
                     // cannot happen
                 }
            }
        }


        if(untrimmedResult != null) {
            if (factory.hasOption(Attributes.TRIM_STRINGS)) {
                untrimmedResult = untrimmedResult.trim();
            }
            if (factory.getGetSurrogator() != null) {
                untrimmedResult = factory.getGetSurrogator().transform(untrimmedResult);
            }
        }

        return untrimmedResult;
    }

    /**
     * Retrieve the XML (as a string) for a specified object field.
     * The default method uses {@link ResultSet#getString(int)} to obtain text.
     * Unlike
     * Override this method if you want to optimize retrieving large texts,
     * i.e by using clobs or streams.
     * @param result the resultset to retrieve the xml from
     * @param index the index of the xml in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved xml as text, <code>null</code> if nothing was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
     protected Object getXMLValue(ResultSet result, int index, CoreField field, boolean mayShorten) throws StorageException, SQLException {
         return getStringValue(result, index, field, mayShorten);
     }

    /**
     * Retrieve a date for a specified object field.
     * The default method uses {@link ResultSet#getTimestamp(int)} to obtain the date.
     * @param result the resultset to retrieve the value from
     * @param index the index of the value in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved java.util.Date value, <code>null</code> if no text was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected java.util.Date getDateTimeValue(ResultSet result, int index, CoreField field) throws StorageException, SQLException {
        Timestamp ts = null;
        try {
            ts = result.getTimestamp(index);
        }
        catch (SQLException sqle) {
            // deal with all-zero datetimes when reading them
            if ("S1009".equals(sqle.getSQLState())) {
                return null;
            }
            else {
                throw sqle;
            }
        }
        if (ts == null) {
            return null;
        } else {
            return new java.util.Date(ts.getTime());
        }
    }

    /**
     * Retrieve a boolean value for a specified object field.
     * The default method uses {@link ResultSet#getBoolean(int)} to obtain the date.
     * @param result the resultset to retrieve the value from
     * @param index the index of the value in the resultset
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved Boolean value, <code>null</code> if no text was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected Boolean getBooleanValue(ResultSet result, int index, CoreField field) throws StorageException, SQLException {
        boolean value = result.getBoolean(index);
        if (result.wasNull()) {
            return null;
        } else {
            return Boolean.valueOf(value);
        }
    }

    /**
     * Determine whether a field (such as a large text or a blob) should be shortened or not.
     * A 'shortened' field contains a placeholder text ('$SHORTED') to indicate that the field is expected to be of large size
     * and should be retrieved by an explicit call to {@link #getStringValue(MMObjectNode, CoreField)} or.
     * {@link #getBinaryValue(MMObjectNode, CoreField)}.
     * The default implementation returns <code>true</code> for binaries, and <code>false</code> for other
     * types.
     * Override this method if you want to be able to change the placeholder strategy.
     * @param field the (MMBase) fieldtype
     * @return <code>true</code> if the field should be shortened
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected boolean shorten(CoreField field) {
        return field.getType() == Field.TYPE_BINARY;
    }

    /**
     * Read a binary (blob) from a field in the database
     * @param node the node the binary data belongs to
     * @param field the binary field
     * @return An InputStream representing the binary data, <code>null</code> if no binary data was stored, or VALUE_SHORTED, if mayShorten
     */
    protected Blob getBlobFromDatabase(MMObjectNode node, CoreField field, boolean mayShorten) {
        try {
            MMObjectBuilder builder = node.getBuilder();
            Scheme scheme = factory.getScheme(Schemes.GET_BINARY_DATA, Schemes.GET_BINARY_DATA_DEFAULT);
            String query = scheme.format(new Object[] { this, builder, field, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            ResultSet result = s.executeQuery(query);
            try {
                if ((result != null) && result.next()) {
                    Blob blob = getBlobValue(result, 1, field, mayShorten);
                    if (blob != null) {
                        node.setSize(field.getName(), blob.length());
                    }
                    return blob;
                } else {
                    if (result != null) result.close();
                    s.close();
                    throw new StorageException("Node with number " + node.getNumber() + " of type " + builder + " not found.");
                }
            } finally {
                result.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public byte[] getBinaryValue(MMObjectNode node, CoreField field) throws StorageException {
        try {
            Blob b = getBlobValue(node, field);;
            return b.getBytes(1, (int) b.length());
        } catch (SQLException sqe) {
            throw new StorageException(sqe);
        }
    }
    // javadoc is inherited
    public InputStream getInputStreamValue(MMObjectNode node, CoreField field) throws StorageException {
        try {
            return getBlobValue(node, field).getBinaryStream();
        } catch (SQLException sqe) {
            throw new StorageException(sqe);
        }
    }

    public Blob getBlobValue(MMObjectNode node, CoreField field) throws StorageException {
        return getBlobValue(node, field, false);
    }

    public Blob getBlobValue(MMObjectNode node, CoreField field, boolean mayShorten) throws StorageException {
        if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
            return getBlobFromFile(node, field, mayShorten);
        } else {
            return getBlobFromDatabase(node, field, mayShorten);
        }
    }


    /**
     * Retrieve a large binary object (byte array) for a specified object field.
     * The default method uses {@link ResultSet#getBytes(int)} to obtain text.
     * Override this method if you want to optimize retrieving large objects,
     * i.e by using clobs or streams.
     * @param result the resultset to retrieve the text from
     * @param index the index of the text in the resultset, or -1 to retireiv from file (blobs).
     * @param field the (MMBase) fieldtype. This value can be null
     * @return the retrieved data, <code>null</code> if no binary data was stored
     * @throws SQLException when a database error occurs
     * @throws StorageException when data is incompatible or the function is not supported
     */
    protected Blob getBlobValue(ResultSet result, int index, CoreField field, boolean mayShorten) throws StorageException, SQLException {
        if (factory.hasOption(Attributes.SUPPORTS_BLOB)) {
            Blob blob = result.getBlob(index);
            if (result.wasNull()) {
                return null;
            }
            if (mayShorten && shorten(field)) {
                return BLOB_SHORTED;
            }

            return blob;
        } else {
            try {
                InputStream inStream = result.getBinaryStream(index);
                if (result.wasNull()) {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        }
                        catch (RuntimeException e) {
                            log.debug("", e);
                        }
                    }
                    return null;
                }
                if (mayShorten && shorten(field)) {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        }
                        catch (RuntimeException e) {
                            log.debug("", e);
                        }
                    }
                    return BLOB_SHORTED;
                }
                return new InputStreamBlob(inStream);
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        }
    }

    /**
     * Defines how binary (blob) data files must look like.
     * @param node the node the binary data belongs to
     * @param fieldName the name of the binary field
     * @return The File where to store or read the binary data
     */
    protected File getBinaryFile(MMObjectNode node, String fieldName) {
        String basePath = factory.getBinaryFileBasePath();
        StringBuffer pathBuffer = new StringBuffer();
        int number = node.getNumber() / 1000;
        while (number > 0) {
            int num = number % 100;
            pathBuffer.insert(0, num);
            if (num < 10) {
                pathBuffer.insert(0, 0);
            }
            pathBuffer.insert(0, File.separator);
            number /= 100;
        }
        pathBuffer.insert(0, basePath + File.separator + factory.getDatabaseName() + File.separator + node.getBuilder().getFullTableName());
        return new File(pathBuffer.toString(), "" + node.getNumber() + '.' + fieldName);
    }

    /**
     * Tries legacy paths
     * @returns such a File if found and readable, 'null' otherwise.
     */
    private File getLegacyBinaryFile(MMObjectNode node, String fieldName) {
        // the same basePath, so you so need to set that up right.
        String basePath = factory.getBinaryFileBasePath();

        File f = new File(basePath, node.getBuilder().getTableName() + File.separator + node.getNumber() + '.' + fieldName);
        if (f.exists()) { // 1.6 storage or 'support' blobdatadir
            if (!f.canRead()) {
                log.warn("Found '" + f + "' but it cannot be read");
            } else {
                return f;
            }
        }

        f = new File(basePath + File.separator + factory.getCatalog() + File.separator + node.getBuilder().getFullTableName() + File.separator + node.getNumber() + '.' + fieldName);
        if (f.exists()) { // 1.7.0.rc1 blob data dir
            if (!f.canRead()) {
                log.warn("Found '" + f + "' but it cannot be read");
            } else {
                return f;
            }
        }

        // don't know..
        return null;

    }

    /**
     * Store a binary (blob) data file
     * @todo how to do this in a transaction???
     * @param node the node the binary data belongs to
     * @param field the binary field
     */
    protected void storeBinaryAsFile(MMObjectNode node, CoreField field) throws StorageException {
        try {
            String fieldName = field.getName();
            File binaryFile = getBinaryFile(node, fieldName);
            binaryFile.getParentFile().mkdirs(); // make sure all directory exist.
            if (node.isNull(fieldName)) {
                if (field.isNotNull()) {
                    node.storeValue(field.getName(), new ByteArrayInputStream(new byte[0]));
                } else {
                    if (binaryFile.exists()) {
                        binaryFile.delete();
                    }
                    return;
                }
            }
            //log.warn("Storing " + field + " for " + node.getNumber());
            InputStream in = node.getInputStreamValue(fieldName);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(binaryFile));
            long size = 0;
            int c = in.read();
            while (c > -1) {
                out.write(c);
                c = in.read();
                size ++;
            }
            out.close();
            in.close();
            // unload the input-stream, it is of no use any more.
            node.setSize(fieldName, size);
            node.storeValue(fieldName, MMObjectNode.VALUE_SHORTED);
        } catch (IOException ie) {
            throw new StorageException(ie);
        }
    }

    /**
     * Checks whether file is readable and existing. Warns if not.
     * If non-existing it checks older locations.
     * @return the file to be used, or <code>null</code> if no existing readable file could be found, also no 'legacy' one.
     */

    protected File checkFile(File binaryFile, MMObjectNode node, CoreField field) {
        String fieldName = field.getName();
        if (!binaryFile.canRead()) {
            String desc = "while it should contain the byte data for node '" + node.getNumber() + "' field '" + fieldName + "'. Returning null.";
            if (!binaryFile.exists()) {
                // try legacy
                File legacy = getLegacyBinaryFile(node, fieldName);
                if (legacy == null) {
                    if (!binaryFile.getParentFile().exists()) {
                        log.warn("The file '" + binaryFile + "' does not exist, " + desc, new Exception());
                        log.info("If you upgraded from older MMBase version, it might be that the blobs were stored on a different location. Make sure your blobs are in '"
                                 + factory.getBinaryFileBasePath()
                                 + "' (perhaps use symlinks?). If you changed configuration to 'blobs-on-disk' while it was blobs-in-database. Go to admin-pages.");

                    } else if (log.isDebugEnabled()) {
                        log.debug("The file '" + binaryFile + "' does not exist. Probably the blob field is simply 'null'");
                    }
                } else {
                    if (!legacyWarned) {
                        log.warn("Using the legacy location '" + legacy + "' rather then '" + binaryFile + "'. You might want to convert this dir.");
                        legacyWarned = true;
                    }
                    return legacy;
                }
            } else {
                log.error("The file '" + binaryFile + "' can not be read, " + desc);
            }
            return null;
        } else {
            return binaryFile;
        }
    }

    /**
     * Read a binary (blob) data file
     * @todo how to do this in a transaction???
     * @param node the node the binary data belongs to
     * @param field the binary field
     * @return the byte array containing the binary data, <code>null</code> if no binary data was stored
     */
    protected Blob getBlobFromFile(MMObjectNode node, CoreField field, boolean mayShorten) throws StorageException {
        String fieldName = field.getName();
        File binaryFile = checkFile(getBinaryFile(node, fieldName), node, field);
        if (binaryFile == null) {
            return null;
        }
        try {
            node.setSize(field.getName(), binaryFile.length());
            if (mayShorten && shorten(field)) {
                return BLOB_SHORTED;
            }
            return new InputStreamBlob(new FileInputStream(binaryFile), binaryFile.length());
        } catch (FileNotFoundException fnfe) {
            throw new StorageException(fnfe);
        }
    }

    // javadoc is inherited
    public int create(MMObjectNode node) throws StorageException {
        // assign a new number if the node has not yet been assigned one
        int nodeNumber = node.getNumber();
        if (nodeNumber == -1) {
            nodeNumber = createKey();
            node.setValue(MMObjectBuilder.FIELD_NUMBER, nodeNumber);
        }
        MMObjectBuilder builder = node.getBuilder();
        // precommit call, needed to convert or add things before a save
        // Should be done in MMObjectBuilder
        builder.preCommit(node);
        create(node, builder);
        commitChange(node, "n");
        unloadShortedFields(node, builder);
//        refresh(node);
        return nodeNumber;
    }

    /**
     * This method inserts a new object in a specific builder, and registers the change.
     * This method makes it easier to implement relational databases, where you may need to update the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to insert. The node already needs to have a (new) number assigned
     * @param builder the builder to store the node
     * @throws StorageException if an error occurred during creation
     */
    protected void create(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        // get a builders fields
        List createFields = new ArrayList();
        List builderFields = builder.getFields(NodeManager.ORDER_CREATE);
        for (Iterator f = builderFields.iterator(); f.hasNext();) {
            CoreField field = (CoreField)f.next();
            if (field.inStorage()) {
                createFields.add(field);
            }
        }
        String tablename = (String) factory.getStorageIdentifier(builder);
        create(node, createFields, tablename);
    }

    protected void create(MMObjectNode node, List createFields, String tablename) {
        // Create a String that represents the fields and values to be used in the insert.
        StringBuffer fieldNames = null;
        StringBuffer fieldValues = null;

        List fields = new ArrayList();
        for (Iterator f = createFields.iterator(); f.hasNext();) {
            CoreField field = (CoreField)f.next();
            // skip bytevalues that are written to file
            if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getType() == Field.TYPE_BINARY)) {
                storeBinaryAsFile(node, field);
                // do not handle this field further
            } else {
                // store the fieldname and the value parameter
                fields.add(field);
                String fieldName = (String)factory.getStorageIdentifier(field);
                if (fieldNames == null) {
                    fieldNames = new StringBuffer(fieldName);
                    fieldValues = new StringBuffer("?");
                } else {
                    fieldNames.append(',').append(fieldName);
                    fieldValues.append(",?");
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("insert field values " + fieldNames + " " + fieldValues);
        }
        if (fields.size() > 0) {
            Scheme scheme = factory.getScheme(Schemes.INSERT_NODE, Schemes.INSERT_NODE_DEFAULT);
            try {
                String query = scheme.format(new Object[] { this, tablename, fieldNames.toString(), fieldValues.toString()});
                getActiveConnection();
                executeUpdateCheckConnection(query, node, fields);
            } catch (SQLException se) {
                throw new StorageException(se.getMessage() + " during creation of " + UNICODE_ESCAPER.transform(node.toString()), se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    protected void unloadShortedFields(MMObjectNode node, MMObjectBuilder builder) {
        for (Iterator f = builder.getFields().iterator(); f.hasNext();) {
            CoreField field = (CoreField)f.next();
            if (field.inStorage() && shorten(field)) {
                String fieldName = field.getName();
                if (! node.isNull(fieldName)) {
                    node.storeValue(fieldName, MMObjectNode.VALUE_SHORTED);
                }
            }
        }
    }

    /**
     * Executes an update query for given node and fields. It will close the connections which are no
     * good, which it determines by trying "SELECT 1 FROM <OBJECT TABLE>" after failure. If that happens, the connection
     * is explicitely closed (in case the driver has not done that), which will render is unusable
     * and at least GenericDataSource will automaticly try to get new ones.
     *
     * @throws SQLException If something wrong with the query, or the database is down or could not be contacted.
     * @since MMBase-1.7.1
     */
    protected void executeUpdateCheckConnection(String query, MMObjectNode node,  List fields) throws SQLException {
        try {
            executeUpdate(query, node, fields);
        } catch (SQLException sqe) {
            while (true) {
                Statement s = null;
                ResultSet rs = null;
                try {
                    s = activeConnection.createStatement();
                    rs = s.executeQuery("SELECT 1 FROM " + factory.getMMBase().getBuilder("object").getFullTableName() + " WHERE 1 = 0"); // if this goes wrong too it can't be the query
                } catch (SQLException isqe) {
                    // so, connection must be broken.
                    log.service("Found broken connection, closing it");
                    if (activeConnection instanceof org.mmbase.module.database.MultiConnection) {
                        ((org.mmbase.module.database.MultiConnection) activeConnection).realclose();
                    } else {
                        activeConnection.close();
                    }
                    getActiveConnection();
                    if (activeConnection.isClosed()) {
                        // don't know if that can happen, but if it happens, this would perhaps avoid an infinite loop (and exception will get thrown in stead)
                        break;
                    }
                    continue;
                 } finally {
                     if (s != null) s.close();
                     if (rs != null) rs.close();
                 }
                break;
            }
            executeUpdate(query, node, fields);
        }
    }

    /**
     * Executes an update query for given node and fields.  This is wrapped in a function because it
     * is repeatedly called in {@link #executeUpdateCheckConnection} which in turn is called from
     * several spots in this class.
     *
     * @since MMBase-1.7.1
     */
    protected void executeUpdate(String query, MMObjectNode node, List fields) throws SQLException {
        PreparedStatement ps = activeConnection.prepareStatement(query);
        for (int fieldNumber = 0; fieldNumber < fields.size(); fieldNumber++) {
            CoreField field = (CoreField) fields.get(fieldNumber);
            setValue(ps, fieldNumber + 1, node, field);
        }
        long startTime = getLogStartTime();
        ps.executeUpdate();
        ps.close();
        logQuery(query, startTime);

    }

    // javadoc is inherited
    public void change(MMObjectNode node) throws StorageException {
        MMObjectBuilder builder = node.getBuilder();
        // precommit call, needed to convert or add things before a save
        // Should be done in MMObjectBuilder
        builder.preCommit(node);
        change(node, builder);
        commitChange(node, "c");
        unloadShortedFields(node, builder);
//        refresh(node);
    }

    /**
     * Change this node in the specified builder.
     * This method makes it easier to implement relational databses, where you may need to update the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to change
     * @param builder the builder to store the node
     * @throws StorageException if an error occurred during change
     */
    protected void change(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        List changeFields = new ArrayList();
        // obtain the node's changed fields
        Collection fieldNames = node.getChanged();
        synchronized(fieldNames) { // make sure the set is not changed during this loop
            for (Iterator f = fieldNames.iterator(); f.hasNext();) {
                String key = (String)f.next();
                CoreField field = builder.getField(key);
                if ((field != null) && field.inStorage()) {
                    changeFields.add(field);
                }
            }
        }
        String tablename = (String) factory.getStorageIdentifier(builder);
        change(node, builder, tablename, changeFields);
    }

    protected void change(MMObjectNode node, MMObjectBuilder builder, String tableName, Collection changeFields) {
        // Create a String that represents the fields to be used in the commit
        StringBuffer setFields = null;
        List fields = new ArrayList();
        for (Iterator iter = changeFields.iterator(); iter.hasNext();) {
            CoreField field = (CoreField) iter.next();
            // changing number is not allowed
            if ("number".equals(field.getName()) || "otype".equals(field.getName())) {
                throw new StorageException("trying to change the '" + field.getName() + "' field of " + node + ". Changed fields " + node.getChanged());
            }
            // skip bytevalues that are written to file
            if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getType() == Field.TYPE_BINARY)) {
                storeBinaryAsFile(node, field);
            } else {
                // handle this field - store it in fields
                fields.add(field);
                // store the fieldname and the value parameter
                String fieldName = (String)factory.getStorageIdentifier(field);
                if (setFields == null) {
                    setFields = new StringBuffer(fieldName + "=?");
                } else {
                    setFields.append(',').append(fieldName).append("=?");
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("change field values " + node);
        }
        if (fields.size() > 0) {
            Scheme scheme = factory.getScheme(Schemes.UPDATE_NODE, Schemes.UPDATE_NODE_DEFAULT);
            try {
                String query = scheme.format(new Object[] { this, tableName , setFields.toString(), builder.getField("number"), node });
                getActiveConnection();
                executeUpdateCheckConnection(query, node, fields);
            } catch (SQLException se) {
                throw new StorageException(se.getMessage() + " for node " + node, se);
            } finally {
                releaseActiveConnection();
            }
        }
    }


    /**
     * Store the value of a field in a prepared statement
     * @todo Note that this code contains some code that should really be implemented in CoreField.
     * In particular, casting should be done in CoreField, IMO.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param node the node from which to retrieve the value
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the fieldtype is invalid, or data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setValue(PreparedStatement statement, int index, MMObjectNode node, CoreField field) throws StorageException, SQLException {
        String fieldName = field.getName();
        Object value = node.getValue(fieldName);
        switch (field.getType()) {
            // Store numeric values
        case Field.TYPE_INTEGER :
        case Field.TYPE_FLOAT :
        case Field.TYPE_DOUBLE :
        case Field.TYPE_LONG :
            setNumericValue(statement, index, value, field, node);
            break;
        case Field.TYPE_BOOLEAN :
            setBooleanValue(statement, index, value, field, node);
            break;
        case Field.TYPE_DATETIME :
            setDateTimeValue(statement, index, value, field, node);
            break;
            // Store nodes
        case Field.TYPE_NODE :
            // cannot do getNodeValue here because that might cause a new connection to be needed -> deadlocks
            setNodeValue(statement, index, value, field, node);
            break;
            // Store strings
        case Field.TYPE_XML :
            setXMLValue(statement, index, value, field, node);
            break;
        case Field.TYPE_STRING :
            // note: do not use getStringValue, as this may attempt to
            // retrieve a (old, or nonexistent) value from the storage
            node.storeValue(fieldName, setStringValue(statement, index, value, field, node));
            break;
            // Store binary data
        case Field.TYPE_BINARY : {
            // note: do not use getByteValue, as this may attempt to
            // retrieve a (old, or nonexistent) value from the storage
            setBinaryValue(statement, index, value, field, node);
            break;
        }
        case Field.TYPE_LIST : {
            setListValue(statement, index, value, field, node);
            break;
        }
        default :    // unknown field type - error
            throw new StorageException("unknown fieldtype");
        }
    }


    /**
     * Stores the 'null' value in the statement if appopriate (the value is null or unset, and the
     * value may indeed be NULL, according to the configuration). If the value is null or unset,
     * but the value may not be NULL, then -1 is stored.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the numeric value to store, which will be checked for null.
     * @param field the MMBase field, containing meta-information
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     * @return true if a null value was set, false otherwise
     * @since MMBase-1.7.1
     */
    protected boolean setNullValue(PreparedStatement statement, int index, Object value, CoreField field, int type) throws StorageException, SQLException {
        boolean mayBeNull = ! field.isNotNull();
        if (value == null) { // value unset
            if (mayBeNull) {
                statement.setNull(index, type);
                return true;
            }
            /*
        } else if (value == MMObjectNode.VALUE_NULL) { // value explicitely set to 'null'
            if (mayBeNull) {
                statement.setNull(index, type);
                return true;
            } else {
                log.debug("Tried to set 'null' in field '" + field.getName() + "' but the field is 'NOT NULL', it will be casted.");
            }
            */
        }

        return false;
    }

    /**
     * Store a numeric value of a field in a prepared statement
     * The method uses the Casting class to convert to the appropriate value.
     * Null values are stored as NULL if possible, otherwise they are stored as -1.
     * Override this method if you want to override this behavior.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the numeric value to store. This may be a String, MMObjectNode, Numeric, or other value - the
     *        method will convert it to the appropriate value.
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data. Used to update this node if the database layer makes changes
     *             to the data (i.e. creating a default value for a non-null field that had a null value)
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setNumericValue(PreparedStatement statement, int index, Object value, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        // Store integers, floats, doubles and longs
        if (!setNullValue(statement, index, value, field, field.getType())) {
            switch (field.getType()) { // it does this switch part twice now?
            case Field.TYPE_INTEGER : {
                int storeValue = Casting.toInt(value);
                statement.setInt(index, storeValue);
                node.storeValue(field.getName(), new Integer(storeValue));
                break;
            }
            case Field.TYPE_FLOAT : {
                float storeValue = Casting.toFloat(value);
                statement.setFloat(index, storeValue);
                node.storeValue(field.getName(), new Float(storeValue));
                break;
            }
            case Field.TYPE_DOUBLE : {
                double storeValue = Casting.toDouble(value);
                statement.setDouble(index, storeValue);
                node.storeValue(field.getName(), new Double(storeValue));
                break;
            }
            case Field.TYPE_LONG : {
                long storeValue = Casting.toLong(value);
                statement.setLong(index, storeValue);
                node.storeValue(field.getName(), new Long(storeValue));
                break;
            }
            default:
                break;
            }
        }
    }


    /**
     * Store a node value of a field in a prepared statement
     * Nodes are stored in the database as numeric values.
     * Since a node value can be a (referential) key (depending on implementation),
     * Null values should be stored as NULL, not -1. If a field cannot be null when a
     * value is not given, an exception is thrown.
     * Override this method if you want to override this behavior.
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param nodeValue the node to store
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data.
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setNodeValue(PreparedStatement statement, int index, Object nodeValue, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        if (!setNullValue(statement, index, nodeValue, field, java.sql.Types.INTEGER)) {
            if (nodeValue == null && field.isNotNull()) {
                throw new StorageException("The NODE field with name " + field.getClass() + " " + field.getName() + " of type " + field.getParent().getTableName() + " can not be NULL.");
            }
            int nodeNumber;
            if (nodeValue instanceof MMObjectNode) {
                nodeNumber = ((MMObjectNode) nodeValue).getNumber();
            } else {
                nodeNumber = Casting.toInt(nodeValue);
            }
            // retrieve node as a numeric value
            statement.setInt(index, nodeNumber);
        }
    }

    /**
     * Store a boolean value of a field in a prepared statement.
     * The method uses the Casting class to convert to the appropriate value.
     * Null values are stored as NULL if possible, otherwise they are stored as <code>false</code>
     * Override this method if you use another way to store booleans
     * @since MMBase-1.8
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the data (boolean) to store
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data. Used to update this node if the database layer makes changes
     *             to the data (i.e. creating a default value for a non-null field that had a null value)
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setBooleanValue(PreparedStatement statement, int index, Object value, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        if (!setNullValue(statement, index, value, field, java.sql.Types.BOOLEAN)) {
            boolean bool = Casting.toBoolean(value);
            statement.setBoolean(index, bool);
            node.storeValue(field.getName(),Boolean.valueOf(bool));
        }
    }

    /**
     * Store a Date value of a field in a prepared statement.
     * The method uses the Casting class to convert to the appropriate value.
     * Null values are stored as NULL if possible, otherwise they are stored as the date 31/12/1969 23:59:59 (-1)
     * Override this method if you use another way to store dates
     * @since MMBase-1.8
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the data (date) to store
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data. Used to update this node if the database layer makes changes
     *             to the data (i.e. creating a default value for a non-null field that had a null value)
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setDateTimeValue(PreparedStatement statement, int index, Object value, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        if (!setNullValue(statement, index, value, field, java.sql.Types.TIMESTAMP)) {
            java.util.Date date = Casting.toDate(value);
            statement.setTimestamp(index, new Timestamp(date.getTime()));
            node.storeValue(field.getName(), date);
        }
    }

    /**
     * Store a List value of a field in a prepared statement.
     * The method uses the Casting class to convert to the appropriate value.
     * Null values are stored as NULL if possible, otherwise they are stored as an empty list.
     * Override this method if you use another way to store lists
     * @since MMBase-1.8
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param value the data (List) to store
     * @param field the MMBase field, containing meta-information. This value can be null
     * @param node the node that contains the data. Used to update this node if the database layer makes changes
     *             to the data (i.e. creating a default value for a non-null field that had a null value)
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setListValue(PreparedStatement statement, int index, Object value, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        if (!setNullValue(statement, index, value, field, java.sql.Types.ARRAY)) {
            List list = Casting.toList(value);
            statement.setObject(index, list);
            node.storeValue(field.getName(), list);
        }
    }

    /**
     * Store binary data of a field in a prepared statement.
     * This basic implementation uses a binary stream to set the data.
     * Null values are stored as NULL if possible, otherwise they are stored as an empty byte-array.
     * Override this method if you use another way to store binaries (i.e. Blobs).
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param objectValue the data (byte array) to store
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data. Used to update this node if the database layer makes changes
     *             to the data (i.e. creating a default value for a non-null field that had a null value)
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected void setBinaryValue(PreparedStatement statement, int index, Object objectValue, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        log.warn("Setting inputstream bytes into field " + field);
        if (!setNullValue(statement, index, objectValue, field, java.sql.Types.VARBINARY)) {
            log.warn("Didn't set null");
            InputStream stream = Casting.toInputStream(objectValue);
            long size = node.getSize(field.getName());
            try {
                statement.setBinaryStream(index, stream, (int) size);
                stream.close();
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
            if (node != null) {
                node.storeValue(field.getName(), MMObjectNode.VALUE_SHORTED);
            }
        }
    }

    /**
     * Store the text value of a field in a prepared statement.
     * Null values are stored as NULL if possible, otherwise they are stored as an empty string.
     * If the FORCE_ENCODE_TEXT option is set, text is encoded (using the MMBase encoding) to a byte array
     * and stored as a binary stream.
     * Otherwise it uses {@link PreparedStatement#setString(int, String)} to set the data.
     * Override this method if you use another way to store large texts (i.e. Clobs).
     * @param statement the prepared statement
     * @param index the index of the field in the prepared statement
     * @param objectValue the text to store
     * @param field the MMBase field, containing meta-information
     * @param node the node that contains the data.
     * @throws StorageException if the data is invalid or missing
     * @throws SQLException if an error occurred while filling in the fields
     */
    protected Object setStringValue(PreparedStatement statement, int index, Object objectValue, CoreField field, MMObjectNode node) throws StorageException, SQLException {

        if (setNullValue(statement, index, objectValue, field, java.sql.Types.VARCHAR)) return objectValue;
        String value = Casting.toString(objectValue);
        if (factory.getSetSurrogator() != null) {
            value = factory.getSetSurrogator().transform(value);
        }
        String encoding = factory.getMMBase().getEncoding();
        // Store data as a binary stream when the code is a clob or blob, or
        // when database-force-encode-text is true.
        if (field.getStorageType() == Types.CLOB || field.getStorageType() == Types.BLOB || factory.hasOption(Attributes.FORCE_ENCODE_TEXT)) {
            byte[] rawchars = null;
            try {
                if (encoding.equalsIgnoreCase("ISO-8859-1") && factory.hasOption(Attributes.LIE_CP1252)) {
                    encoding = "CP1252";
                } else {
                }
                rawchars = value.getBytes(encoding);
                ByteArrayInputStream stream = new ByteArrayInputStream(rawchars);
                statement.setBinaryStream(index, stream, rawchars.length);
                stream.close();
            } catch (IOException ie) {
                throw new StorageException(ie);
            }
        } else {
            String setValue = value;
            if (factory.hasOption(Attributes.LIE_CP1252)) {
                try {
                    if (encoding.equalsIgnoreCase("ISO-8859-1")) {
                        log.info("Lying CP-1252");
                        encoding = "CP1252";
                        setValue = new String(value.getBytes("CP1252"), "ISO-8859-1");
                    } else {
                    }
                } catch(java.io.UnsupportedEncodingException uee) {
                    // cannot happen
                }
            } else {
            }
            statement.setString(index, setValue);

        }
        if (value != null) {
            if (! encoding.equalsIgnoreCase("UTF-8")) {
                try {
                    value = new String(value.getBytes(encoding), encoding);
                } catch(java.io.UnsupportedEncodingException uee) {
                    log.error(uee);
                    // cannot happen
                }
            }

            // execute also getSurrogator, to make sure that it does not confuse, and the node contains what it would contain if fetched from database.
            if (factory.getGetSurrogator() != null) {
                value = factory.getGetSurrogator().transform(value);
            }
            if (factory.hasOption(Attributes.TRIM_STRINGS)) {
                value = value.trim();
            }
        }


        if (objectValue == null) node.storeValue(field.getName(), value);

        return value;
    }

    /**
     * This default implementation calls {@link #setStringValue}.
     * Override this method if you want to override this behavior.
     * @since MMBase-1.7.1
     */
    protected void setXMLValue(PreparedStatement statement, int index, Object objectValue, CoreField field, MMObjectNode node) throws StorageException, SQLException {
        if (objectValue == null) {
            if(field.isNotNull()) {
                objectValue = "<p/>";
            }
        }
        objectValue = Casting.toXML(objectValue);
        if (objectValue != null) {
            objectValue = org.mmbase.util.xml.XMLWriter.write((org.w3c.dom.Document) objectValue, false, true);
        }
        node.storeValue(field.getName(), objectValue);
        setStringValue(statement, index, objectValue, field, node);
    }

    // javadoc is inherited
    public void delete(MMObjectNode node) throws StorageException {
        // determine parent
        if (node.hasRelations()) {
            throw new StorageException("cannot delete node " + node.getNumber() + ", it still has relations");
        }
        delete(node, node.getBuilder());
        commitChange(node, "d");
    }

    /**
     * Delete a node from a specific builder
     * This method makes it easier to implement relational databses, where you may need to remove the node
     * in more than one builder.
     * Call this method for all involved builders if you use a relational database.
     * @param node The node to delete
     * @throws StorageException if an error occurred during delete
     */
    protected void delete(MMObjectNode node, MMObjectBuilder builder) throws StorageException {
        List blobFileField = new ArrayList();
        List builderFields = builder.getFields(NodeManager.ORDER_CREATE);
        for (Iterator f = builderFields.iterator(); f.hasNext();) {
            CoreField field = (CoreField)f.next();
            if (field.inStorage()) {
                if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getType() == Field.TYPE_BINARY)) {
                    blobFileField.add(field);
                }
            }
        }
        String tablename = (String) factory.getStorageIdentifier(builder);
        delete(node, builder, blobFileField, tablename);
    }

    protected void delete(MMObjectNode node, MMObjectBuilder builder, List blobFileField, String tablename) {
        try {
            Scheme scheme = factory.getScheme(Schemes.DELETE_NODE, Schemes.DELETE_NODE_DEFAULT);
            String query = scheme.format(new Object[] { this, tablename, builder.getField("number"), node });
            getActiveConnection();
            Statement s = activeConnection.createStatement();
            long startTime = getLogStartTime();
            s.executeUpdate(query);
            s.close();
            logQuery(query, startTime);
            // delete blob files too
            for (Iterator f = blobFileField.iterator(); f.hasNext();) {
                CoreField field = (CoreField)f.next();
                String fieldName = field.getName();
                File binaryFile = getBinaryFile(node, fieldName);
                File checkedFile = checkFile(binaryFile, node, field);
                if (checkedFile == null) {
                    if (field.isNotNull()) {
                        log.warn("Could not find blob for field to delete '" + fieldName + "' of node " + node.getNumber() + ": " + binaryFile);
                    } else {
                        // ok, value was probably simply 'null'.
                    }
                } else if (! checkedFile.delete()) {
                    log.warn("Could not delete '" + checkedFile + "'");
                } else {
                    log.debug("Deleted '" + checkedFile + "'");
                }
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public MMObjectNode getNode(final MMObjectBuilder builder, final int number) throws StorageException {
        Scheme scheme = factory.getScheme(Schemes.SELECT_NODE, Schemes.SELECT_NODE_DEFAULT);
        try {
            // create a new node (must be done before acquiring the connection, because this code might need a connection)
            MMObjectNode node = builder.getNewNode("system");

            getActiveConnection();
            // get a builders fields
            List builderFields = builder.getFields(NodeManager.ORDER_CREATE);
            StringBuffer fieldNames = null;
            for (Iterator f = builderFields.iterator(); f.hasNext();) {
                CoreField field = (CoreField)f.next();
                if (field.inStorage()) {
                    if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE) && (field.getType() == Field.TYPE_BINARY)) {
                        continue;
                    }
                    // store the fieldname and the value parameter
                    String fieldName = (String)factory.getStorageIdentifier(field);
                    if (fieldNames == null) {
                        fieldNames = new StringBuffer(fieldName);
                    } else {
                        fieldNames.append(',').append(fieldName);
                    }
                }
            }
            String query = scheme.format(new Object[] { this, builder, fieldNames.toString(), builder.getField("number"), new Integer(number)});
            Statement s = activeConnection.createStatement();
            ResultSet result = null;
            try {
                result = s.executeQuery(query);
                fillNode(node, result, builder);
            } finally {
                if (result != null) result.close();
                s.close();
            }
            return node;
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }


    /**
     * Reloads the data from a node from the adtabase.
     * Use this after a create or change action, so the data in memory is consistent with
     * any data stored in the database.
     * @param node the node to refresh
     */
    protected void refresh(MMObjectNode node) throws StorageException {
        Scheme scheme = factory.getScheme(Schemes.SELECT_NODE, Schemes.SELECT_NODE_DEFAULT);
        try {
            getActiveConnection();
            MMObjectBuilder builder = node.getBuilder();
            // get a builders fields
            List builderFields = builder.getFields(NodeManager.ORDER_CREATE);
            StringBuffer fieldNames = null;
            for (Iterator f = builderFields.iterator(); f.hasNext();) {
                CoreField field = (CoreField)f.next();
                if (field.inStorage()) {
                    // store the fieldname and the value parameter
                    String fieldName = (String)factory.getStorageIdentifier(field);
                    if (fieldNames == null) {
                        fieldNames = new StringBuffer(fieldName);
                    } else {
                        fieldNames.append(',').append(fieldName);
                    }
                }
            }
            String query = scheme.format(new Object[] { this, builder, fieldNames.toString(), builder.getField("number"), new Integer(node.getNumber())});
            Statement s = activeConnection.createStatement();
            ResultSet result = null;
            try {
                result = s.executeQuery(query);
                fillNode(node, result, builder);
            } finally {
                if (result != null) result.close();
                s.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Fills a single Node from the resultset of a query.
     * You can use this method to iterate through a query, creating multiple nodes, provided the resultset still contains
     * members (that is, <code>result.isAfterLast</code> returns <code>false</code>)
     * @param node The MMObjectNode to be filled
     * @param result the resultset
     * @param builder the builder to use for creating the node
     * @throws StorageException if the resultset is exhausted or a database error occurred
     */
    protected void fillNode(MMObjectNode node, ResultSet result, MMObjectBuilder builder) throws StorageException {
        try {
            if ((result != null) && result.next()) {

                // iterate through all a builder's fields, and retrieve the value for that field
                // Note that if we would do it the other way around (iterate through the recordset's fields)
                // we might get inconsistencies if we 'remap' fieldnames that need not be mapped.
                // this also guarantees the number field is set first, which we  may need when retrieving blobs
                // from disk
                for (Iterator i = builder.getFields(NodeManager.ORDER_CREATE).iterator(); i.hasNext();) {
                    CoreField field = (CoreField)i.next();
                    if (field.inStorage()) {
                        Object value;
                        if (field.getType() == Field.TYPE_BINARY && factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
                            value =  getBlobFromFile(node, field, true);
                            if (value == BLOB_SHORTED) value = MMObjectNode.VALUE_SHORTED;
                        } else {
                            String id = (String)factory.getStorageIdentifier(field);
                            value = getValue(result, result.findColumn(id), field, true);
                        }
                        if (value == null) {
                            node.storeValue(field.getName(), null);
                        } else {
                            node.storeValue(field.getName(), value);
                        }
                    }
                }
                // clear the changed signal on the node
                node.clearChanged();
                return;
            } else {
                throw new StorageNotFoundException("Statement " + result.getStatement() + " (to fetch a Node) did not result anything");
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        }
    }

    /**
     * Attempts to return a single field value from the resultset of a query.
     * @todo This method is called from the search query code and therefor needs to be public.
     *       Perhaps code from searchquery should be moved to storage.
     * @param result the resultset
     * @param index the index of the field in the resultset
     * @param field the expected MMBase field type. This can be null
     * @param mayShorten Whether it would suffice to return only a 'shorted' version of the value.
     * @return the value
     * @throws StorageException if the value cannot be retrieved from the resultset
     */

    public Object getValue(ResultSet result, int index, CoreField field, boolean mayShorten) throws StorageException {
        try {
            int dbtype = Field.TYPE_UNKNOWN;
            if (field != null) {
                dbtype = field.getType();
            } else { // use database type.as
                dbtype = getJDBCtoField(result.getMetaData().getColumnType(index), dbtype);
            }

            switch (dbtype) {
                // string-type fields
            case Field.TYPE_XML :
                return getXMLValue(result, index, field, mayShorten);
            case Field.TYPE_STRING :
                return getStringValue(result, index, field, mayShorten);
            case Field.TYPE_BINARY :
                Blob b =  getBlobValue(result, index, field, mayShorten);
                if (b == BLOB_SHORTED) return MMObjectNode.VALUE_SHORTED;
                if (b == null) return null;
                return b.getBytes(1L, (int) b.length());
            case Field.TYPE_DATETIME :
                return getDateTimeValue(result, index, field);
            case Field.TYPE_BOOLEAN :
                return getBooleanValue(result, index, field);
            case Field.TYPE_INTEGER :
            case Field.TYPE_NODE :
                Object o = result.getObject(index);
                if (o instanceof Integer) {
                    return o;
                } else if (o instanceof Number) {
                    return new Integer(((Number)o).intValue());
                } else {
                    return o;
                }
            default :
                return result.getObject(index);
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        }
    }

    // javadoc is inherited
    public int getNodeType(int number) throws StorageException {
        Integer numberValue = new Integer(number);
        Integer otypeValue = (Integer)typeCache.get(numberValue);
        if (otypeValue != null) {
            return otypeValue.intValue();
        } else {
            Scheme scheme = factory.getScheme(Schemes.SELECT_NODE_TYPE, Schemes.SELECT_NODE_TYPE_DEFAULT);
            try {
                getActiveConnection();
                MMBase mmbase = factory.getMMBase();
                String query = scheme.format(new Object[] { this, mmbase, mmbase.getTypeDef().getField("number"), numberValue });
                Statement s = activeConnection.createStatement();
                try {
                    ResultSet result = s.executeQuery(query);
                    if (result != null) {
                        try {
                            if (result.next()) {
                                int retval = result.getInt(1);
                                typeCache.put(numberValue, new Integer(retval));
                                return retval;
                            } else {
                                return -1;
                            }
                        } finally {
                            result.close();
                        }
                    } else {
                        return -1;
                    }
                } finally {
                    s.close();
                }
            } catch (SQLException se) {
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    /**
     * Returns whether tables inherit fields form parent tables.
     * this determines whether fields that are inherited in mmbase builders
     * are redefined in the database tables.
     */
    protected boolean tablesInheritFields() {
        return true;
    }

    /**
     * Determines whether the storage should make a field definition in a builder table for a
     * specified field.
     */
    protected boolean isPartOfBuilderDefinition(CoreField field) {
        // persistent field?
        // skip binary fields when values are written to file
        boolean isPart = field.inStorage() && (field.getType() != Field.TYPE_BINARY || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE));
        // also, if the database is OO, and the builder has a parent,
        // skip fields that are in the parent builder
        MMObjectBuilder parentBuilder = field.getParent().getParentBuilder();
        if (isPart && parentBuilder != null) {
            isPart = !tablesInheritFields() || parentBuilder.getField(field.getName()) == null;
        }
        return isPart;
    }

    // javadoc is inherited
    public void create(MMObjectBuilder builder) throws StorageException {
        log.debug("Creating a table for " + builder);
        // use the builder to get the fields and create a
        // valid create SQL string
        // for backward compatibility, fields are to be created in the order defined
        List fields = builder.getFields(NodeManager.ORDER_CREATE);
        log.debug("found fields " + fields);

        List tableFields = new ArrayList();
        for (Iterator f = fields.iterator(); f.hasNext();) {
            CoreField field = (CoreField)f.next();
            if (isPartOfBuilderDefinition(field)) {
                tableFields.add(field);
            }
        }
        String tableName = (String) factory.getStorageIdentifier(builder);
        createTable(builder, tableFields, tableName);
        verify(builder);
    }

    protected void createTable(MMObjectBuilder builder, List tableFields, String tableName) {
        StringBuffer createFields = new StringBuffer();
        StringBuffer createIndices = new StringBuffer();
        StringBuffer createFieldsAndIndices = new StringBuffer();
        StringBuffer createConstraints = new StringBuffer();
        // obtain the parentBuilder
        MMObjectBuilder parentBuilder = builder.getParentBuilder();
        Scheme rowtypeScheme;
        Scheme tableScheme;
        // if the builder has no parent, it is an object table,
        // so use CREATE_OBJECT_ROW_TYPE and CREATE_OBJECT_TABLE schemes.
        // Otherwise use CREATE_ROW_TYPE and CREATE_TABLE schemes.
        //
        if (parentBuilder == null) {
            rowtypeScheme = factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE);
            tableScheme = factory.getScheme(Schemes.CREATE_OBJECT_TABLE, Schemes.CREATE_OBJECT_TABLE_DEFAULT);
        } else {
            rowtypeScheme = factory.getScheme(Schemes.CREATE_ROW_TYPE);
            tableScheme = factory.getScheme(Schemes.CREATE_TABLE, Schemes.CREATE_TABLE_DEFAULT);
        }

        for (Iterator f = tableFields.iterator(); f.hasNext();) {
            try {
                CoreField field = (CoreField)f.next();
                // convert a fielddef to a field SQL createdefinition
                String fieldDef = getFieldDefinition(field);
                if (createFields.length() > 0) {
                    createFields.append(", ");
                }
                createFields.append(fieldDef);
                // test on other indices
                String constraintDef = getConstraintDefinition(field);
                if (constraintDef != null) {
                    // note: the indices are prefixed with a comma, as they generally follow the fieldlist.
                    // if the database uses rowtypes, however, fields are not included in the CREATE TABLE statement,
                    // and the comma should not be prefixed.
                    if (rowtypeScheme == null || createIndices.length() > 0) {
                        createIndices.append(", ");
                    }

                    createIndices.append(constraintDef);
                    if (createFieldsAndIndices.length() > 0) {
                        createFieldsAndIndices.append(", ");
                    }
                    createFieldsAndIndices.append(fieldDef + ", " + constraintDef);
                } else {
                    if (createFieldsAndIndices.length() > 0) {
                        createFieldsAndIndices.append(", ");
                    }
                    createFieldsAndIndices.append(fieldDef);
                }
            } catch (StorageException se) {
                // if something wrong with one field, don't fail the complete table.
                log.error("" + se.getMessage(), se);
            }
        }
        String query = "";
        try {
            getActiveConnection();
            // create a rowtype, if a scheme has been given
            // Note that creating a rowtype is optional
            if (rowtypeScheme != null) {
                query = rowtypeScheme.format(new Object[] { this, tableName, createFields.toString(), parentBuilder });
                // remove parenthesis with empty field definitions -
                // unfortunately Schems don't take this into account
                if (factory.hasOption(Attributes.REMOVE_EMPTY_DEFINITIONS)) {
                    query = query.replaceAll("\\(\\s*\\)", "");
                }
                Statement s = activeConnection.createStatement();
                long startTime = getLogStartTime();
                s.executeUpdate(query);
                s.close();
                logQuery(query, startTime);
            }
            // create the table
            query = tableScheme.format(new Object[] { this, tableName, createFields.toString(), createIndices.toString(), createFieldsAndIndices.toString(), createConstraints.toString(), parentBuilder, factory.getDatabaseName() });
            // remove parenthesis with empty field definitions -
            // unfortunately Schemes don't take this into account
            if (factory.hasOption(Attributes.REMOVE_EMPTY_DEFINITIONS)) {
                query = query.replaceAll("\\(\\s*\\)", "");
            }

            Statement s = activeConnection.createStatement();
            long startTime = getLogStartTime();
            s.executeUpdate(query);
            s.close();
            logQuery(query, startTime);

            tableNameCache.add(tableName.toUpperCase());

            // create indices and unique constraints
            for (Iterator i = builder.getStorageConnector().getIndices().values().iterator(); i.hasNext();) {
                Index index = (Index)i.next();
                create(index);
            }

        } catch (SQLException se) {
            throw new StorageException(se.getMessage() + " in query:" + query, se);
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Creates a fielddefinition, of the format '[fieldname] [fieldtype] NULL' or
     * '[fieldname] [fieldtype] NOT NULL' (depending on whether the field is nullable).
     * The fieldtype is taken from the type mapping in the factory.
     * @param field the field
     * @return the typedefiniton as a String
     * @throws StorageException if the field type cannot be mapped
     */
    protected String getFieldDefinition(CoreField field) throws StorageException {
        // create the type mapping to search for
        String typeName = Fields.getTypeDescription(field.getType());
        int size = field.getMaxLength();
        TypeMapping mapping = new TypeMapping();
        mapping.name = typeName;
        mapping.setFixedSize(size);
        // search type mapping
        List typeMappings = factory.getTypeMappings();
        int found = typeMappings.indexOf(mapping);
        if (found > -1) {
            String fieldDef = factory.getStorageIdentifier(field) + " " + ((TypeMapping)typeMappings.get(found)).getType(size);
            if (field.isNotNull()) {
                fieldDef += " NOT NULL";
            }
            return fieldDef;
        } else {
            throw new StorageException("Type for field " + field.getName() + ": " + typeName + " (" + size + ") undefined.");
        }
    }

    /**
     * Creates an index definition string for a field to be passed when creating a table.
     * @param field the field for which to make the index definition
     * @return the index definition as a String, or <code>null</code> if no definition is available
     */
    protected String getConstraintDefinition(CoreField field) throws StorageException {
        String definitions = null;
        Scheme scheme = null;
        if (field.getName().equals("number")) {
            scheme = factory.getScheme(Schemes.CREATE_PRIMARY_KEY, Schemes.CREATE_PRIMARY_KEY_DEFAULT);
            if (scheme != null) {
                definitions = scheme.format(new Object[] { this, field.getParent(), field, factory.getMMBase() });
            }
        } else {
            // the field is unique: create a unique key for it
            if (field.isUnique()) {
                scheme = factory.getScheme(Schemes.CREATE_UNIQUE_KEY, Schemes.CREATE_UNIQUE_KEY_DEFAULT);
                if (scheme != null) {
                    definitions = scheme.format(new Object[] { this, field.getParent(), field, field });
                }
            }
            if (field.getType() == Field.TYPE_NODE) {
                scheme = factory.getScheme(Schemes.CREATE_FOREIGN_KEY, Schemes.CREATE_FOREIGN_KEY_DEFAULT);
                if (scheme != null) {
                    Object keyname = factory.getStorageIdentifier("" + field.getParent().getTableName() + "_" + field.getName() + "_FOREIGN");
                    String definition = scheme.format(new Object[] { this, field.getParent(), field, factory.getMMBase(), factory.getStorageIdentifier("number"), keyname});
                    if (definitions != null) {
                        definitions += ", " + definition;
                    } else {
                        definitions = definition;
                    }
                }
            }
        }
        return definitions;
    }

    // javadoc is inherited
    public void change(MMObjectBuilder builder) throws StorageException {
        // test if you can make changes
        // iterate through the fields,
        // use metadata.getColumns(...)  to select fields
        //      (incl. name, datatype, size, null)
        // use metadata.getImportedKeys(...) to get foreign keys
        // use metadata.getIndexInfo(...) to get composite and other indices
        // determine changes and run them
        throw new StorageException("Operation not supported");
    }

    // javadoc is inherited
    public synchronized void delete(MMObjectBuilder builder) throws StorageException {
        int size = size(builder);
        if (size != 0) {
            throw new StorageException("Can not drop builder, it still contains " + size + " node(s)");
        }
        try {
            getActiveConnection();
            Scheme scheme = factory.getScheme(Schemes.DROP_TABLE, Schemes.DROP_TABLE_DEFAULT);
            String query = scheme.format(new Object[] { this, builder });
            Statement s = activeConnection.createStatement();
            long startTime = getLogStartTime();
            s.executeUpdate(query);
            s.close();
            logQuery(query, startTime);
            scheme = factory.getScheme(Schemes.DROP_ROW_TYPE);
            if (scheme != null) {
                query = scheme.format(new Object[] { this, builder });
                s = activeConnection.createStatement();
                long startTime2 = getLogStartTime();
                s.executeUpdate(query);
                s.close();
                logQuery(query, startTime2);

                String tableName = factory.getStorageIdentifier(builder).toString().toUpperCase();
                if(tableNameCache.contains(tableName)) {
                    tableNameCache.remove(tableName);
                }
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public void create() throws StorageException {
        create(factory.getMMBase().getRootBuilder());
        createSequence();
    }

    /**
     * Creates a means for the database to pre-create keys with increasing numbers.
     * A sequence can be a database routine, a number table, or anything else that can be used to create unique numbers.
     * Keys can be obtained from the sequence by calling {@link #createKey()}.
     * @throws StorageException when the sequence can not be created
     */
    protected void createSequence() throws StorageException {
        synchronized (sequenceKeys) {
            try {
                getActiveConnection();
                // create the type mapping to search for
                String typeName = Fields.getTypeDescription(Field.TYPE_INTEGER);
                TypeMapping mapping = new TypeMapping();
                mapping.name = typeName;
                // search type mapping
                List typeMappings = factory.getTypeMappings();
                int found = typeMappings.indexOf(mapping);
                if (found == -1) {
                    throw new StorageException("Type " + typeName + " undefined.");
                }
                String fieldName = (String)factory.getStorageIdentifier("number");
                String fieldDef = fieldName + " " + ((TypeMapping)typeMappings.get(found)).type + " NOT NULL, PRIMARY KEY(" + fieldName + ")";
                String query;
                Statement s;
                Scheme scheme = factory.getScheme(Schemes.CREATE_SEQUENCE, Schemes.CREATE_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, fieldDef, factory.getDatabaseName()});
                    long startTime = getLogStartTime();
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                }
                scheme = factory.getScheme(Schemes.INIT_SEQUENCE, Schemes.INIT_SEQUENCE_DEFAULT);
                if (scheme != null) {
                    query = scheme.format(new Object[] { this, factory.getStorageIdentifier("number"), new Integer(1), bufferSize });
                    long startTime = getLogStartTime();
                    s = activeConnection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                }
            } catch (SQLException se) {
                throw new StorageException(se);
            } finally {
                releaseActiveConnection();
            }
        }
    }

    // javadoc is inherited
    public boolean exists(MMObjectBuilder builder) throws StorageException {
        boolean result = exists((String)factory.getStorageIdentifier(builder));
        if (result) {
            verify(builder);
        }
        return result;
    }

    /**
     * Queries the database metadata to test whether a given table exists.
     *
     * @param tableName name of the table to look for
     * @throws StorageException when the metadata could not be retrieved
     * @return <code>true</code> if the table exists
     */
    protected synchronized boolean exists(String tableName) throws StorageException {
        if(tableNameCache == null) {
            try {
                tableNameCache = new HashSet();
                getActiveConnection();
                DatabaseMetaData metaData = activeConnection.getMetaData();
                String prefixTablename = factory.getMMBase().getBaseName();
                if (metaData.storesLowerCaseIdentifiers()) {
                    prefixTablename = prefixTablename.toLowerCase();
                }
                if (metaData.storesUpperCaseIdentifiers()) {
                    prefixTablename = prefixTablename.toUpperCase();
                }
                ResultSet res = metaData.getTables(factory.getCatalog(), null, prefixTablename+"_%", new String[] { "TABLE", "VIEW", "SEQUENCE" });
                try {
                    while(res.next()) {
                        if(! tableNameCache.add(res.getString(3).toUpperCase())) {
                            log.warn("builder already in cache(" + res.getString(3) + ")!");
                        }
                    }
                } finally {
                    res.close();
                }

            } catch(Exception e) {
                throw new StorageException(e.getMessage());
            } finally {
                releaseActiveConnection();
             }
        }

        return tableNameCache.contains(tableName.toUpperCase());
    }

    // javadoc is inherited
    public boolean exists() throws StorageException {
        return exists(factory.getMMBase().getRootBuilder());
    }

    // javadoc is inherited
    public int size(MMObjectBuilder builder) throws StorageException {
        try {
            getActiveConnection();
            Scheme scheme = factory.getScheme(Schemes.GET_TABLE_SIZE, Schemes.GET_TABLE_SIZE_DEFAULT);
            String query = scheme.format(new Object[] { this, builder });
            Statement s = activeConnection.createStatement();
            ResultSet res = s.executeQuery(query);
            int retval;
            try {
                res.next();
                retval = res.getInt(1);
            } finally {
                res.close();
            }
            s.close();
            return retval;
        } catch (Exception e) {
            throw new StorageException(e);
        } finally {
            releaseActiveConnection();
        }
    }

    // javadoc is inherited
    public int size() throws StorageException {
        return size(factory.getMMBase().getRootBuilder());
    }

    /**
     * Guess the (mmbase) type in storage using the JDBC type.
     * Because a JDBC type can represent more than one mmbase Type,
     * the current type is also passed - if the current type matches, that type
     * is returned, otherwise the method returns the closest matching MMBase type.
     */
    protected int getJDBCtoField(int jdbcType, int mmbaseType) {
        switch (jdbcType) {
        case Types.INTEGER :
        case Types.SMALLINT :
        case Types.TINYINT :
        case Types.BIGINT :
            if (mmbaseType == Field.TYPE_INTEGER || mmbaseType == Field.TYPE_LONG || mmbaseType == Field.TYPE_NODE) {
                return mmbaseType;
            } else {
                return Field.TYPE_LONG;
            }
        case Types.FLOAT :
        case Types.REAL :
        case Types.DOUBLE :
        case Types.NUMERIC :
        case Types.DECIMAL :
            if (mmbaseType == Field.TYPE_FLOAT || mmbaseType == Field.TYPE_DOUBLE) {
                return mmbaseType;
            } else {
                return Field.TYPE_DOUBLE;
            }
        case Types.BINARY :
        case Types.LONGVARBINARY :
        case Types.VARBINARY :
        case Types.BLOB :
            if (mmbaseType == Field.TYPE_BINARY || mmbaseType == Field.TYPE_STRING || mmbaseType == Field.TYPE_XML) {
                return mmbaseType;
            } else {
                return Field.TYPE_BINARY;
            }
        case Types.CHAR :
        case Types.CLOB :
        case Types.LONGVARCHAR :
        case Types.VARCHAR :
            if (mmbaseType == Field.TYPE_STRING || mmbaseType == Field.TYPE_XML) {
                return mmbaseType;
            } else {
                return Field.TYPE_STRING;
            }
        case Types.BIT :
        case Types.BOOLEAN :
            return Field.TYPE_BOOLEAN;
        case Types.DATE :
        case Types.TIME :
        case Types.TIMESTAMP :
            return Field.TYPE_DATETIME;
        case Types.ARRAY :
            return Field.TYPE_LIST;
        case Types.JAVA_OBJECT :
        case Types.OTHER :
            if (mmbaseType == Field.TYPE_LIST) {
                return mmbaseType;
            }  else {
                return Field.TYPE_UNKNOWN;
            }
        default :
            return Field.TYPE_UNKNOWN;
        }
    }

    /**
     * Tests whether a builder and the table present in the database match.
     */
    public void verify(MMObjectBuilder builder) throws StorageException {
        try {
            getActiveConnection();
            String tableName = (String)factory.getStorageIdentifier(builder);
            DatabaseMetaData metaData = activeConnection.getMetaData();
            if (metaData.storesUpperCaseIdentifiers()) {
                tableName = tableName.toUpperCase();
            }
            // skip if does not support inheritance, or if this is the object table
            if (tablesInheritFields()) {
                MMObjectBuilder parent = builder.getParentBuilder();
                try {
                    ResultSet superTablesSet = metaData.getSuperTables(null, null, tableName);
                    try {
                        if (superTablesSet.next()) {
                            String parentName = superTablesSet.getString("SUPERTABLE_NAME");
                            if (parent == null || !parentName.equalsIgnoreCase((String)factory.getStorageIdentifier(parent))) {
                                log.error("VERIFY: parent builder in storage for builder " + builder.getTableName() + " should be " + parent.getTableName() + " but defined as " + parentName);
                            } else {
                                log.debug("VERIFY: parent builder in storage for builder " + builder.getTableName() + " defined as " + parentName);
                            }
                        } else if (parent != null) {
                            log.error("VERIFY: no parent builder defined in storage for builder " + builder.getTableName());
                        }
                    } finally {
                        superTablesSet.close();
                    }
                } catch (AbstractMethodError ae) {
                    // ignore: the method is not implemented by the JDBC Driver
                    log.debug("VERIFY: Driver does not fully implement the JDBC 3.0 API, skipping inheritance consistency tests for " + tableName);
                } catch (UnsupportedOperationException uoe) {
                    // ignore: the operation is not supported by the JDBC Driver
                    log.debug("VERIFY: Driver does not support all JDBC 3.0 methods, skipping inheritance consistency tests for " + tableName);
                } catch (SQLException se) {
                    // ignore: the method is likely not implemented by the JDBC Driver
                    // (should be one of the above errors, but postgresql returns this as an SQLException. Tsk.)
                    log.debug("VERIFY: determining super tables failed, skipping inheritance consistency tests for " + tableName);
                }
            }
            Map columns = new HashMap();
            ResultSet columnsSet = metaData.getColumns(null, null, tableName, null);
            try {
                // get column information
                while (columnsSet.next()) {
                    Map colInfo = new HashMap();
                    colInfo.put("DATA_TYPE", new Integer(columnsSet.getInt("DATA_TYPE")));
                    colInfo.put("TYPE_NAME", columnsSet.getString("TYPE_NAME"));
                    colInfo.put("COLUMN_SIZE", new Integer(columnsSet.getInt("COLUMN_SIZE")));
                    colInfo.put("NULLABLE", Boolean.valueOf(columnsSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
                    columns.put(columnsSet.getString("COLUMN_NAME"), colInfo);
                }
            } finally {
                columnsSet.close();
            }
            // iterate through fields and check all fields present
            int pos = 0;
            List builderFields = builder.getFields(NodeManager.ORDER_CREATE);
            for (Iterator i = builderFields.iterator(); i.hasNext();) {
                CoreField field = (CoreField)i.next();
                if (field.inStorage() && (field.getType() != Field.TYPE_BINARY || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
                    field.rewrite();
                    pos++;
                    Object id = field.getStorageIdentifier();
                    Map colInfo = (Map)columns.get(id);
                    if ((colInfo == null)) {

                        // bug #6609, marcel
                        if((field!=null) && (field.getName()!=null) && (field.getName().equals(id))) {
                            log.error("VERIFY: Field '" + field.getName() + "' of builder '" + builder.getTableName() + "' does NOT exist in storage! Field will be considered virtual");
                        } else {
                            log.error("VERIFY: Field '" + field.getName() + "' (mapped to field '"+id+"') of builder '" + builder.getTableName() + "' does NOT exist in storage! Field will be considered virtual");
                        }

                        // set field to virtual so it will not be stored -
                        // prevents future queries or statements from failing
                        field.setState(Field.STATE_VIRTUAL);
                    } else {
                        // compare type
                        int curtype = field.getType();
                        int storageType = ((Integer)colInfo.get("DATA_TYPE")).intValue();
                        field.setStorageType(storageType);
                        int type = getJDBCtoField(storageType, curtype);
                        if (type != curtype) {
                            log.warn("VERIFY: Field '" + field.getName() + "' of builder '"
                                      + builder.getTableName() + "' mismatch : type defined as "
                                      + Fields.getTypeDescription(curtype)
                                      + ", but in storage " + Fields.getTypeDescription(type)
                                      + " (" + colInfo.get("TYPE_NAME") + "). Storage type will be used.");
                            DataType originalDataType = field.getDataType();
                            DataType newDataType      = (DataType)DataTypes.getDataType(type).clone(originalDataType.getName());
                            field.setType(Fields.classToType(newDataType.getTypeAsClass()));
                            if (originalDataType instanceof BasicDataType) {
                                newDataType.inherit((BasicDataType) originalDataType); // takes as much possible...
                            } else {
                                // cannot happen, I think
                                log.warn("original data type is no BasicDataType, but a " + originalDataType.getClass());
                            }
                            field.setDataType(newDataType);
                            log.info("" + originalDataType + " -> " + field.getDataType());

                        }
                        boolean nullable = ((Boolean)colInfo.get("NULLABLE")).booleanValue();
                        if (nullable == field.isNotNull()) {
                            // only correct if storage is more restrictive
                            if (!nullable) {
                                field.setNotNull(!nullable);
                                log.warn("VERIFY: Field '" + field.getName() + "' of builder '" + builder.getTableName() + "' mismatch : notnull in storage is " + !nullable + " (value corrected for this session)");
                            } else {
                                log.debug("VERIFY: Field '" + field.getName() + "' of builder '" + builder.getTableName() + "' mismatch : notnull in storage is " + !nullable);
                            }
                        }
                        // compare size
                        int size = ((Integer)colInfo.get("COLUMN_SIZE")).intValue();
                        int cursize = field.getMaxLength();
                        // ignore the size difference for large fields (generally blobs or memo texts)
                        // since most databases do not return accurate sizes for these fields
                        if (cursize != -1 && size != -1 && size != cursize && cursize <= 255) {
                            if (size < cursize) {
                                // only correct if storage is more restrictive
                                field.setMaxLength(size);
                                log.warn("VERIFY: Field '" + field.getName() + "' of builder '" + builder.getTableName() + "' mismatch : size defined as " + cursize + ", but in storage " + size + " (value corrected for this session)");
                            } else {
                                log.debug("VERIFY: Field '" + field.getName() + "' of builder '" + builder.getTableName() + "' mismatch : size defined as " + cursize + ", but in storage " + size);
                            }
                        }
                        columns.remove(id);
                    }
                    // lock the field now that it has been checked
                    // this prevents any accidental changes to the field.
                    field.finish();
                }
            }
            // if any are left, these fields were removed!
            for (Iterator i = columns.keySet().iterator(); i.hasNext();) {
                String column = (String)i.next();
                log.warn("VERIFY: Column '" + column + "' for builder '" + builder.getTableName() + "' in Storage but not defined!");
            }
        } catch (Exception e) {
            log.error("Error during check of table (Assume table is correct.):" + e.getMessage());
            log.error(Logging.stackTrace(e));
        } finally {
            releaseActiveConnection();
        }
    }

    /**
     * Determines if an index exists.
     * You should have an active connection before calling this method.
     * @param index the index to test
     * @param tablename the tablename to test the index against
     * @throws StorageException when a database error occurs
     */
    protected boolean exists(Index index, String tablename) {
        boolean result = false;
        try {
            DatabaseMetaData metaData = activeConnection.getMetaData();
            ResultSet indexSet = metaData.getIndexInfo(null, null, tablename, index.isUnique(), false);
            try {
                String indexName = (String)factory.getStorageIdentifier(index);
                while (!result && indexSet.next()) {
                    int indexType = indexSet.getInt("TYPE");
                    if (indexType != DatabaseMetaData.tableIndexStatistic) {
                        result = indexName.equalsIgnoreCase(indexSet.getString("INDEX_NAME"));
                    }
                }
            } finally {
                indexSet.close();
            }
        } catch (SQLException se) {
            throw new StorageException(se);
        }
        return result;
    }

    /**
     * Determines if an index exists.
     * You should have an active connection before calling this method.
     * @param index the index to test
     * @throws StorageException when a database error occurs
     */
    protected boolean exists(Index index) throws StorageException {
        return exists(index, index.getParent().getTableName());
    }


    /**
     * Drop all constraints and indices that contain a specific field.
     * You should have an active connection before calling this method.
     * @param field the field for which to drop indices
     * @throws StorageException when a database error occurs
     */
    protected void deleteIndices(CoreField field) throws StorageException {
        for (Iterator i = field.getParent().getStorageConnector().getIndices().values().iterator(); i.hasNext();) {
            Index index = (Index)i.next();
            if (index.contains(field)) {
                delete(index);
            }
        }
    }

    /**
     * Drop a constraint or index.
     * You should have an active connection before calling this method.
     * @param index the index to drop
     * @throws StorageException when a database error occurs
     */
    protected void delete(Index index) throws StorageException {
        Scheme deleteIndexScheme;
        if (index.isUnique()) {
            //  Scheme: DELETE_CONSTRAINT
            deleteIndexScheme = factory.getScheme(Schemes.DELETE_UNIQUE_INDEX, Schemes.DELETE_UNIQUE_INDEX_DEFAULT);
        } else {
            //  Scheme: DELETE_INDEX
            deleteIndexScheme = factory.getScheme(Schemes.DELETE_INDEX, Schemes.DELETE_INDEX_DEFAULT);
        }
        if (deleteIndexScheme != null && exists(index)) {
            // remove index
            String query = null;
            try {
                Statement s = activeConnection.createStatement();
                query = deleteIndexScheme.format(new Object[] { this, index.getParent(), index });
                long startTime = getLogStartTime();
                try {
                    s.executeUpdate(query);
                } finally {
                    s.close();
                }
                logQuery(query, startTime);
            } catch (SQLException se) {
                throw new StorageException(se.getMessage() + " in query:" + query, se);
            }
        }
    }

    /**
     * Returns a comma seperated list of fieldnames for an index.
     * @param index the index to create it for
     * @return the field list definition as a String, or <code>null</code> if the index was empty, or
     *         if it consists of a composite index and composite indices are not supported.
     */
    protected String getFieldList(Index index) {
        String result = null;
        if (index.size() == 1 || factory.hasOption(Attributes.SUPPORTS_COMPOSITE_INDEX)) {
            StringBuffer indexFields = new StringBuffer();
            for (Iterator f = index.iterator(); f.hasNext();) {
                CoreField field = (CoreField)f.next();
                if (indexFields.length() > 0) {
                    indexFields.append(", ");
                }
                indexFields.append(factory.getStorageIdentifier(field));
            }
            if (indexFields.length() > 0) {
                result = indexFields.toString();
            }
        }
        return result;
    }

    /**
     * (Re)create all constraints and indices that contain a specific field.
     * You should have an active connection before calling this method.
     * @param field the field for which to create indices
     * @throws StorageException when a database error occurs
     */
    protected void createIndices(CoreField field) throws StorageException {
        for (Iterator i = field.getParent().getStorageConnector().getIndices().values().iterator(); i.hasNext();) {
            Index index = (Index)i.next();
            if (index.contains(field)) {
                create(index);
            }
        }
    }

    /**
     * Create an index or a unique constraint.
     * @param index the index to create
     */
    protected void create(Index index) throws StorageException {
        String tablename = (String) factory.getStorageIdentifier(index.getParent());
        createIndex(index, tablename);
    }

    /**
     * Create an index or a unique constraint.
     * @param index the index to create
     * @param tablename name of the table
     */
    protected void createIndex(Index index, String tablename) {
        Scheme createIndexScheme;
        if (index.isUnique()) {
            //  Scheme: CREATE_UNIQUE_INDEX
            createIndexScheme = factory.getScheme(Schemes.CREATE_UNIQUE_INDEX, Schemes.CREATE_UNIQUE_INDEX_DEFAULT);
        } else {
            //  Scheme: CREATE_INDEX
            createIndexScheme = factory.getScheme(Schemes.CREATE_INDEX, Schemes.CREATE_INDEX_DEFAULT);
        }
        // note: do not attempt to create an index if it already exists.
        if (createIndexScheme != null && !exists(index, tablename)) {
            String fieldlist = getFieldList(index);
            if (fieldlist != null) {
                String query = null;
                try {
                    Statement s = activeConnection.createStatement();
                    query = createIndexScheme.format(new Object[] { this, tablename, fieldlist, index });
                    long startTime = getLogStartTime();
                    try {
                        s.executeUpdate(query);
                    } finally {
                        s.close();
                    }
                    logQuery(query, startTime);
                } catch (SQLException se) {
                    throw new StorageException(se.getMessage() + " in query:" + query, se);
                }
            }
        }
    }

    // javadoc is inherited
    public void create(CoreField field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (create new field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (create new field) on row types.");
        }
        if (field.inStorage() && (field.getType() != Field.TYPE_BINARY || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.CREATE_FIELD, Schemes.CREATE_FIELD_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic creation of fields");
            } else {
                try {
                    getActiveConnection();
                    // add field
                    String fieldDef = getFieldDefinition(field);
                    String query = scheme.format(new Object[] { this, field.getParent(), fieldDef });
                    Statement s = activeConnection.createStatement();
                    long startTime = getLogStartTime();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                    // add constraints
                    String constraintDef = getConstraintDefinition(field);
                    if (constraintDef != null) {
                        scheme = factory.getScheme(Schemes.CREATE_CONSTRAINT, Schemes.CREATE_CONSTRAINT_DEFAULT);
                        if (scheme != null) {
                            query = scheme.format(new Object[] { this, field.getParent(), constraintDef });
                            s = activeConnection.createStatement();
                            s.executeUpdate(query);
                            s.close();
                            logQuery(query, startTime);
                        }
                    }
                    deleteIndices(field);
                    createIndices(field);
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }

    // javadoc is inherited
    public void change(CoreField field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (change field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (change field) on row types.");
        }
        if (field.inStorage() && (field.getType() != Field.TYPE_BINARY || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.CHANGE_FIELD, Schemes.CHANGE_FIELD_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic changing of fields");
            } else {
                try {
                    getActiveConnection();
                    deleteIndices(field);
                    String fieldDef = getFieldDefinition(field);
                    String query = scheme.format(new Object[] { this, field.getParent(), fieldDef });
                    Statement s = activeConnection.createStatement();
                    long startTime = getLogStartTime();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                    // add constraints
                    String constraintDef = getConstraintDefinition(field);
                    if (constraintDef != null) {
                        scheme = factory.getScheme(Schemes.CREATE_CONSTRAINT, Schemes.CREATE_CONSTRAINT_DEFAULT);
                        if (scheme != null) {
                            query = scheme.format(new Object[] { this, field.getParent(), constraintDef });
                            s = activeConnection.createStatement();
                            long startTime2 = getLogStartTime();
                            s.executeUpdate(query);
                            s.close();
                            logQuery(query, startTime2);
                        }
                    }
                    createIndices(field);
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }

    // javadoc is inherited
    public void delete(CoreField field) throws StorageException {
        if (!factory.hasOption(Attributes.SUPPORTS_DATA_DEFINITION)) {
            throw new StorageException("Data definiton statements (delete field) are not supported.");
        }
        if (factory.getScheme(Schemes.CREATE_OBJECT_ROW_TYPE) != null) {
            throw new StorageException("Can not use data definiton statements (delete field) on row types.");
        }
        if (field.inStorage() && (field.getType() != Field.TYPE_BINARY || !factory.hasOption(Attributes.STORES_BINARY_AS_FILE))) {
            Scheme scheme = factory.getScheme(Schemes.DELETE_FIELD, Schemes.DELETE_FIELD_DEFAULT);
            if (scheme == null) {
                throw new StorageException("Storage layer does not support the dynamic deleting of fields");
            } else {
                try {
                    getActiveConnection();
                    deleteIndices(field);
                    String query = scheme.format(new Object[] { this, field.getParent(), field });
                    Statement s = activeConnection.createStatement();
                    long startTime = getLogStartTime();
                    s.executeUpdate(query);
                    s.close();
                    logQuery(query, startTime);
                    createIndices(field);
                } catch (SQLException se) {
                    throw new StorageException(se);
                } finally {
                    releaseActiveConnection();
                }
            }
        }
    }


    /**
     * Convert legacy file
     * @return Number of converted fields. Or -1 if not storing binaries as files
     */

    public int convertLegacyBinaryFiles() throws org.mmbase.storage.search.SearchQueryException, SQLException {
        if (factory.hasOption(Attributes.STORES_BINARY_AS_FILE)) {
            synchronized(factory) { // there is only on factory. This makes sure that there is only one conversion running
                int result = 0;
                int fromDatabase = 0;
                Iterator builders = factory.getMMBase().getBuilders().iterator();
                while (builders.hasNext()) {
                    MMObjectBuilder builder = (MMObjectBuilder)builders.next();
                    Iterator fields = builder.getFields().iterator();
                    while (fields.hasNext()) {
                        CoreField field = (CoreField)fields.next();
                        String fieldName = field.getName();
                        if (field.getType() == Field.TYPE_BINARY) { // check all binaries

                            // check whether it might be in a column
                            boolean foundColumn = false;
                            {
                                try {
                                    getActiveConnection();
                                    String tableName = (String)factory.getStorageIdentifier(builder);
                                    DatabaseMetaData metaData = activeConnection.getMetaData();
                                    ResultSet columnsSet = metaData.getColumns(null, null, tableName, null);
                                    try {
                                        while (columnsSet.next()) {
                                            if (columnsSet.getString("COLUMN_NAME").equals(fieldName)) {
                                                foundColumn = true;
                                                break;
                                            }
                                        }
                                    } finally {
                                        columnsSet.close();
                                    }
                                } catch (java.sql.SQLException sqe) {
                                    log.error(sqe.getMessage());
                                } finally {
                                    releaseActiveConnection();
                                }
                            }

                            List nodes = builder.getNodes(new org.mmbase.storage.search.implementation.NodeSearchQuery(builder));
                            log.service("Checking all " + nodes.size() + " nodes of '" + builder.getTableName() + "'");
                            Iterator i = nodes.iterator();
                            while (i.hasNext()) {
                                MMObjectNode node = (MMObjectNode)i.next();
                                File storeFile = getBinaryFile(node, fieldName);
                                if (!storeFile.exists()) { // not found!
                                    File legacyFile = getLegacyBinaryFile(node, fieldName);
                                    if (legacyFile != null) {
                                        storeFile.getParentFile().mkdirs();
                                        if (legacyFile.renameTo(storeFile)) {
                                            log.service("Renamed " + legacyFile + " to " + storeFile);
                                            result++;
                                        } else {
                                            log.warn("Could not rename " + legacyFile + " to " + storeFile);
                                        }
                                    } else {
                                        if (foundColumn) {

                                            Blob b = getBlobFromDatabase(node, field, false);
                                            byte[] bytes = b.getBytes(0L, (int) b.length());
                                            node.setValue(fieldName, bytes);
                                            storeBinaryAsFile(node, field);

                                            node.storeValue(fieldName, MMObjectNode.VALUE_SHORTED); // remove to avoid filling node-cache with lots of handles and cause out-of-memory
                                            // node.commit(); no need, because we only changed blob (so no database updates are done)
                                            result++;
                                            fromDatabase++;
                                            log.service("( " + result + ") Found bytes in database while configured to be on disk. Stored to " + storeFile);
                                        }
                                    }
                                }
                            } // nodes
                        } // if type = byte
                    } // fields
                } // builders
                if (result > 0) {
                    log.info("Converted " + result + " fields " + ((fromDatabase > 0 && fromDatabase < result) ? " of wich  " + fromDatabase + " from database" : ""));
                    if (fromDatabase > 0) {
                        log.info("You may drop byte array columns from the database now. See the the VERIFY warning during initialisation.");
                    }

                } else {
                    log.service("Converted no fields");
                }
                return result;
            } // synchronized
        } else {
            // not configured to store blobs as file
            return -1;
        }
    }

    protected static class InputStreamBlob implements Blob {
        private InputStream inputStream;
        private byte[] bytes = null;
        private long size;
        public InputStreamBlob(InputStream is, long s) {
            inputStream = is;
            size = s;
        }
        public InputStreamBlob(InputStream is) {
            inputStream = is;
            size = -1;
        }

        public InputStream getBinaryStream() {
            if (bytes != null) {
                return new ByteArrayInputStream(bytes);
            } else {
                return inputStream;
            }
        }
        public byte[] getBytes(long pos, int length) {
            if (pos == 1 && size == length && bytes != null) return bytes;

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            long p = 1;
            int c;
            InputStream stream = getBinaryStream();
            try {
                while((c = stream.read()) > -1) {
                    if (p >= pos) {
                        b.write(c);
                    }
                    p++;
                    if (p > pos + length) break;
                }
            } catch (IOException ioe) {
                log.error(ioe);
            }
            return b.toByteArray();
        }

        protected void getBytes() {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            int c;
            try {
                while((c = inputStream.read()) > -1) {
                    b.write(c);
                }
            } catch (IOException ioe) {
                log.error(ioe);
            }
            bytes = b.toByteArray();
            size = bytes.length;

        }

        public long length() {
            if (size < 0 && inputStream != null) {
                getBytes();
            }
            return size;
        }

        public long position(Blob pattern, long start) {
            throw new UnsupportedOperationException("");
        }

        public long  position(byte[] pattern, long start) {
            throw new UnsupportedOperationException("");
        }

        public OutputStream setBinaryStream(long pos) {
            throw new UnsupportedOperationException("");
        }
        public int setBytes(long pos, byte[] bytes) {
            throw new UnsupportedOperationException("");
        }
        public int setBytes(long pos, byte[] bytes, int offset, int len) {
            throw new UnsupportedOperationException("");
        }

        public void truncate(long len) {
            throw new UnsupportedOperationException("");
        }
    }
}
