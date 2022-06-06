/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.xmlimporter;

import java.io.*;
import java.util.HashMap;

import javax.xml.parsers.*;

import org.mmbase.module.core.*;
import org.mmbase.util.logging.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class parses the xml file with TML code and calls the appropriate methods
 * in TransactionManager TemporarayNodeManager org.mmabse.module.core
 * Furthermore is does some nameserving.
 *
 * @author John Balder: 3MPS
 * @author Rob Vermeulen: VPRO
 * @author Rob van Maris: Finnalist IT Group
 * @author Erik Visser: Finnalist IT Group
 * @since MMBase-1.5
 * @version $Id$
 */

public class TransactionsParser extends DefaultHandler {

    /** The element and attribute names. */
    private final static String ELEMENT_TRANSACTIONS = "transactions";
    private final static String ATTRIBUTE_EXCEPTION_PAGE = "exceptionPage";
    private final static String ATTRIBUTE_REPORT_FILE = "reportFile";
    private final static String ATTRIBUTE_KEY = "key";
    private final static String ELEMENT_CREATE = "create";
    private final static String ATTRIBUTE_ID = "id";
    private final static String ATTRIBUTE_COMMIT_ON_CLOSE = "commit";
    private final static String ATTRIBUTE_TIME_OUT = "timeOut";
    private final static String ELEMENT_OPEN = "open";
    private final static String ELEMENT_COMMIT = "commit";
    private final static String ELEMENT_DELETE = "delete";
    private final static String ELEMENT_CREATE_OBJECT = "createObject";
    private final static String ATTRIBUTE_TYPE = "type";
    private final static String ATTRIBUTE_DISPOSE = "disposeWhenNotReferenced";
    private final static String ELEMENT_OPEN_OBJECT = "openObject";
    private final static String ELEMENT_DELETE_OBJECT = "deleteObject";
    private final static String ELEMENT_ACCESS_OBJECT = "accessObject";
    private final static String ATTRIBUTE_MMBASE_ID = "mmbaseId";
    private final static String ELEMENT_MARK_OBJECT_DELETE = "markObjectDelete";
    private final static String ATTRIBUTE_NAME = "name";
    private final static String ELEMENT_CREATE_RELATION = "createRelation";
    private final static String ATTRIBUTE_SOURCE = "source";
    private final static String ATTRIBUTE_DESTINATION = "destination";
    private final static String ATTRIBUTE_DELETE_RELATIONS = "deleteRelations";
    private final static String ELEMENT_MERGE_OBJECTS = "mergeObjects";
    private final static String ELEMENT_OBJECT_MERGER = "objectMerger";
    private final static String ELEMENT_OBJECT_MATCHER = "objectMatcher";
    private final static String ATTRIBUTE_CLASS = "class";
    private final static String ELEMENT_PARAM = "param";
    private final static String ATTRIBUTE_VALUE = "value";
    private final static String ELEMENT_SET_FIELD = "setField";
    private final static String ATTRIBUTE_URL = "url";

    /**
     * The character encoding used for the reporfile: ISO-8859-1.
     */
    public final static String ENCODING = "ISO-8859-1";

    /**
     * The XML header used for the reportfile.
     */
    public final String xmlHeader =
        "<?xml version='1.0'  encoding='"
            + ENCODING
            + "'?>\n"
            + "<!DOCTYPE transactions "
            + "PUBLIC '-//MMBase/DTD transactions config 1.0//EN' "
            + "'https://www.mmbase.org/dtd/transactions_1_0.dtd'>\n";

    /** Logger instance. */
    private static Logger log = Logging.getLoggerInstance(TransactionsParser.class.getName());

    /** TransactionHandler module. */
    private static TransactionHandler transactionHandler = null;

    /** Path of the MMBase dtd directory. */
    private String dtdDirectory;

    /** Path of the MMBase report directory. */
    private String reportDirectory;

    /** Transaction information for current user. */
    private UserTransactionInfo uti;

    /** Transaction information for current user. */
    private Consultant consultant;

    /** The error page. */
    private String exceptionPage;

    /** The report file, used to report merging problems to. */
    private File reportFile;

    /** The current transaction context. */
    private Transaction transaction;

    /**  The current object context. */
    private TmpObject tmpObject;

    /** The value of the fieldName attribute when parsing a setField element. */
    private String fieldName;

    /** The value of the element value when parsing a setField element. */
    private StringBuffer fieldValue;

    /** The value of the url attribute when parsing a setField element. */
    private String url;

    /** The name of the type attribute when parsing a mergeObjects element. */
    private String type;

    /** The objectMerger, when parsing a mergeObjects element. */
    private ObjectMerger objectMerger;

    /** The SimilarObjectFinder, when parsing a mergeObjects element. */
    private SimilarObjectFinder objectFinder;

    /** The name of the class attribute when parsing an objectMerger element. */
    private String className;

    /** The name/value attributes of param elements, when parsing
     *  an objectMerger element. */
    private HashMap<String, String> params;

    /**
     * Creates new TransactionParser object.
     * @param consultant The intermediate import object. Used to set and get status from and set and get objects to and from.
     * @param uti -  UserTransactionInfo object which contains a collection in
     *             which all transactions for a user are listed.
     */
    public TransactionsParser(UserTransactionInfo uti, Consultant consultant) {
        this(uti);
        this.consultant = consultant;
    }

    /**
     * Creates new TransactionParser object.
     * @param uti -  UserTransactionInfo object which contains a collection in
     *              which all transactions for a user are listed.
     */
    public TransactionsParser(UserTransactionInfo uti) {
        this.uti = uti;
        transactionHandler = (TransactionHandler)TransactionHandler.getModule("transactionhandler");
        //upload = (Upload)Module.getModule("upload");
        dtdDirectory = MMBaseContext.getConfigPath() + File.separator + "dtd" + File.separator;
        reportDirectory = MMBaseContext.getConfigPath() + File.separator + "import" + File.separator + "report" + File.separator;
    }

    /**
     * Overrides a method of org.xml.sax.helpers.DefaultHandler.
     * For further info see javadoc of org.xml.sax.ContentHandler.
     *
     * @param nameSpaceURI -  see org.xml.sax.ContentHandler
     * @param localName -  see org.xml.sax.ContentHandler
     * @param name -  The element type name.
     * @param attributes -  The specified or defaulted attributes.
     * @throws SAXException -  Any SAX exception, possibly wrapping another
     * exception.
     */
    public void startElement(String nameSpaceURI, String localName, String name, Attributes attributes) throws SAXException {

        // Reconstruct the parsed line.
        StringBuffer sb = new StringBuffer("<" + name);
        for (int i = 0; i < attributes.getLength(); i++) {
            sb.append(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
        }
        sb.append(">");
        String parsedLine = sb.toString();

        // Log element read when debugging.
        if (log.isDebugEnabled()) {
            log.debug(parsedLine);
        }

        try {
            if (name.equals(ELEMENT_TRANSACTIONS)) { // transactions
                // Get attributes.
                String attrExceptionPage = attributes.getValue(ATTRIBUTE_EXCEPTION_PAGE);
                String attrReportFile = attributes.getValue(ATTRIBUTE_REPORT_FILE);
                String attrKey = attributes.getValue(ATTRIBUTE_KEY);

                // Check attrKey.
                transactionHandler.checkKey(attrKey);
                // Set exception page.
                if (attrExceptionPage != null) {
                    exceptionPage = attrExceptionPage;
                }

                // Create new reportFile.
                try {
                    if (attrReportFile != null) {
                        reportFile = new File(reportDirectory, attrReportFile);

                        // Delete old file if present.
                        if (reportFile.exists()) {
                            reportFile.delete();
                        } else {
                            // Create parent directory if it does not exist already.
                            reportFile.getParentFile().mkdirs();
                        }

                        appendReportFile(xmlHeader + parsedLine + "\n");
                    }

                } catch (Exception e) {
                    throw new SAXException("Failed to create reportFile " + reportFile + ": " + e);
                }

            } else if (name.equals(ELEMENT_CREATE)) { // create
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id
                    boolean commit = Boolean.valueOf(//commit
    attributes.getValue(ATTRIBUTE_COMMIT_ON_CLOSE)).booleanValue();
                    long timeOut = Long.parseLong(// timeout
    attributes.getValue(ATTRIBUTE_TIME_OUT));

                // Create new transaction.
                transaction = Transaction.createTransaction(uti, id, commit, timeOut, reportFile, consultant);
            } else if (name.equals(ELEMENT_OPEN)) { // open
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id
                    boolean commit = Boolean.valueOf(// commit
    attributes.getValue(ATTRIBUTE_COMMIT_ON_CLOSE)).booleanValue();

                // Open transaction.
                transaction = Transaction.openTransaction(uti, id, commit);

            } else if (name.equals(ELEMENT_COMMIT)) { // commit
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id

                // Commit transaction.
                transaction = Transaction.openTransaction(uti, id, false);
                transaction.commit();

            } else if (name.equals(ELEMENT_DELETE)) { // delete
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID);

                // Delete transaction.
                transaction = Transaction.openTransaction(uti, id, false);
                transaction.delete();

            } else if (name.equals(ELEMENT_CREATE_OBJECT)) { // createObject
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID);
                String type = attributes.getValue(ATTRIBUTE_TYPE);
                boolean dispose // disposeWhenNotReferenced
                = Boolean.valueOf(attributes.getValue(ATTRIBUTE_DISPOSE)).booleanValue();

                // Create new object.
                tmpObject = transaction.createObject(id, type, dispose);

            } else if (name.equals(ELEMENT_CREATE_RELATION)) { // createRelation
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID);
                String type = attributes.getValue(ATTRIBUTE_TYPE);
                String source = attributes.getValue(ATTRIBUTE_SOURCE);
                String destination = attributes.getValue(ATTRIBUTE_DESTINATION);

                // Create relation.
                tmpObject = transaction.createRelation(id, type, source, destination);

            } else if (name.equals(ELEMENT_OPEN_OBJECT)) { // openObject
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id

                // Open object.
                tmpObject = transaction.openObject(id);

            } else if (name.equals(ELEMENT_ACCESS_OBJECT)) { // accessObject
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id
                try {
                    int mmbaseId = Integer.parseInt(attributes.getValue(ATTRIBUTE_MMBASE_ID)); // mmbaseId

                    // Access object.
                    tmpObject = transaction.accessObject(id, mmbaseId);
                } catch (NumberFormatException e) {
                    throw new SAXException("invalid attribute mmbasid=\"" + attributes.getValue(ATTRIBUTE_MMBASE_ID) + "\"");
                }

            } else if (name.equals(ELEMENT_DELETE_OBJECT)) { // deleteObject
                // Get attributes.
                String id = attributes.getValue(ATTRIBUTE_ID); // id

                // Delete object.
                tmpObject = transaction.openObject(id);
                transaction.deleteObject(tmpObject);

            } else if (name.equals(ELEMENT_MARK_OBJECT_DELETE)) { // markObjectDelete
                // Get attributes.
                try {
                    int mmbaseId = Integer.parseInt(attributes.getValue(ATTRIBUTE_MMBASE_ID)); // mmbaseId
                        boolean deleteRelations = Boolean.valueOf(// deleteRelations
    attributes.getValue(ATTRIBUTE_DELETE_RELATIONS)).booleanValue();

                    // Mark object for delete.
                    tmpObject = transaction.accessObject(null, mmbaseId);
                    transaction.markDeleteObject(tmpObject, deleteRelations);
                } catch (NumberFormatException e) {
                    throw new SAXException("invalid attribute mmbasid=\"" + attributes.getValue(ATTRIBUTE_MMBASE_ID) + "\"");
                }

            } else if (name.equals(ELEMENT_MERGE_OBJECTS)) { // mergeObjects
                // Get attributes.
                type = attributes.getValue(ATTRIBUTE_TYPE);

            } else if (name.equals(ELEMENT_OBJECT_MATCHER)) { // objectMatcher
                // Get attributes.
                className = attributes.getValue(ATTRIBUTE_CLASS);

                // Initialize parameters.
                params = new HashMap<String, String>();

            } else if (name.equals(ELEMENT_OBJECT_MERGER)) { // objectMerger
                // Get attributes.
                className = attributes.getValue(ATTRIBUTE_CLASS);

                // Initialize parameters.
                params = new HashMap<String, String>();

            } else if (name.equals(ELEMENT_PARAM)) { // param
                // Get attributes.
                String paramName = attributes.getValue(ATTRIBUTE_NAME);
                String value = attributes.getValue(ATTRIBUTE_VALUE);

                // Add to parameters.
                params.put(paramName, value);

            } else if (name.equals(ELEMENT_SET_FIELD)) {
                // Get attributes.
                fieldName = attributes.getValue(ATTRIBUTE_NAME); // name
                url = attributes.getValue(ATTRIBUTE_URL); // url

                // Url specified, then set value from file.
                /*
                if (url != null) {
                    Object value = upload.getFile(url);
                    tmpObject.setField(fieldName, value);
                }
                */

            } else {
                throw new TransactionHandlerException("transaction operator \"" + name + "\" doesn't exist");
            }

        } catch (TransactionHandlerException e) {
            throw new SAXException(e);
        } finally {
            // Add parsed line of xml to transactions history.
            if (transaction != null) {
                transaction.appendReportBuffer("\n" + parsedLine);
            }
        }
    }

    /**
     * Overrides a method of org.xml.sax.helpers.DefaultHandler.
     * For further info see javadoc of org.xml.sax.ContentHandler.
     *
     * @param nameSpaceURI -  see org.xml.sax.ContentHandler
     * @param localName -  see org.xml.sax.ContentHandler
     * @param name -  The element type name.
     *
     * @throws SAXException -  Any SAX exception, possibly wrapping another exception.
     */
    public void endElement(String nameSpaceURI, String localName, String name) throws org.xml.sax.SAXException {

        // Log this when debugging.
        if (log.isDebugEnabled()) {
            log.debug("</" + name + ">");
        }

        // Add parsed line of xml to transactions history.
        if (transaction != null) {
            transaction.appendReportBuffer("</" + name + ">\n");
        }

        try {
            if (name.equals(ELEMENT_TRANSACTIONS)) { // transactions
                // finish reportFile.
                try {
                    appendReportFile("</" + ELEMENT_TRANSACTIONS + ">");
                } catch (Exception e) {
                    throw new SAXException("Failed to write to reportFile " + reportFile + ": " + e);
                }

                exceptionPage = null;
                reportFile = null;

            } else if (name.equals(ELEMENT_CREATE)) { // create
                transaction.leave();
                transaction = null;

            } else if (name.equals(ELEMENT_OPEN)) { // open
                transaction.leave();
                transaction = null;

            } else if (name.equals(ELEMENT_COMMIT)) { // commit
                transaction = null;

            } else if (name.equals(ELEMENT_DELETE)) { // delete
                transaction = null;

            } else if (name.equals(ELEMENT_CREATE_OBJECT)) { // createObject
                tmpObject = null;

            } else if (name.equals(ELEMENT_CREATE_RELATION)) { // createRelation
                tmpObject = null;

            } else if (name.equals(ELEMENT_OPEN_OBJECT)) { // openObject
                tmpObject = null;

            } else if (name.equals(ELEMENT_ACCESS_OBJECT)) { // accessObject
                tmpObject = null;

            } else if (name.equals(ELEMENT_DELETE_OBJECT)) { // deleteObject
                tmpObject = null;

            } else if (name.equals(ELEMENT_MARK_OBJECT_DELETE)) { // markObjectDelete
                tmpObject = null;

            } else if (name.equals(ELEMENT_MERGE_OBJECTS)) { // mergeObjects
                transaction.mergeObjects(type, objectFinder, objectMerger);

                type = null;
                objectFinder = null;
                objectMerger = null;

            } else if (name.equals(ELEMENT_OBJECT_MATCHER)) { // objectFinder
                // Create and initialize object finder instance.
                try {
                    objectFinder = (SimilarObjectFinder)Class.forName(className).newInstance();
                    objectFinder.init(params);
                } catch (Exception e) {
                    throw new SAXException(e);
                }

                params = null;
                className = null;

            } else if (name.equals(ELEMENT_OBJECT_MERGER)) { // objectMerger
                // Create object merger instance.
                try {
                    objectMerger = (ObjectMerger)Class.forName(className).newInstance();
                    objectMerger.init(params);
                } catch (Exception e) {
                    throw new SAXException(e);
                }

                params = null;
                className = null;

            } else if (name.equals(ELEMENT_SET_FIELD)) {
                if (fieldValue != null) {
                    tmpObject.setField(fieldName, fieldValue.toString());
                }
                fieldName = null;
                fieldValue = null;
                if (url != null) {
                    //upload.deleteFile(url);
                    url = null;
                }
            }

        } catch (TransactionHandlerException e) {
            throw new SAXException(e);
        }

    }

    /**
     * Overrides a method of org.xml.sax.helpers.DefaultHandler.
     * The Parser will call this method to report each chunk of character data.
     * For further info see javadoc of org.xml.sax.ContentHandler.
     * @param ch -  The characters from the XML document.
     * @param start -  The start position in the array.
     * @param length -  The number of characters to read from the array.
     * @throws SAXException -  Any SAX exception, possibly wrapping another
     * exception.
     */
    public void characters(char[] ch, int start, int length) throws org.xml.sax.SAXException {

        // Log characters read when debugging.
        String characters = new String(ch, start, length);
        if (log.isDebugEnabled()) {
            log.debug("\"" + characters + "\"");
        }

        // Add parsed xml to transactions history.
        if (transaction != null) {
            transaction.appendReportBuffer(characters);
        }

        // We are only interested in the text contained in setField elements.
        if (fieldName != null) {
            // Check value not already set by url attribute.
            if (url != null) {
                throw new SAXException("Can not set field value from both url and element value.");
            }
            if (fieldValue == null) {
                fieldValue = new StringBuffer();
            }
            fieldValue.append(characters);
        }
    }

    /**
     * Overrides a method of org.xml.sax.helpers.DefaultHandler.
     * The default implementation (org.xml.sax.ContentHandler) does nothing.
     * This implementation throws a SaxException.
     *
     * @param e c
     * @throws SAXException -  Any SAX exception, possibly wrapping another exception.
     */
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    /**
     * Overrides a method of org.xml.sax.helpers.DefaultHandler.
     * The default implementation (org.xml.sax.ContentHandler) does nothing.
     * This implementation throws a SaxException.
     * @param e -  Any SAX exception, possibly wrapping another exception.
     * @throws SAXException -  Any SAX exception, possibly wrapping another exception.
     */
    public void warning(SAXParseException e) throws SAXException {
        throw e;
    }

    /**
     * Parse xml and execute transactions accordingly.
     * @param input The input source.
     * @throws TransactionHandlerException Except for IO exceptions every exception is wrapped in a TransactionException.
     * @throws IOException If an IO problem occurs while reading the XML data.
     */
    public synchronized void parse(java.io.Reader input) throws IOException, TransactionHandlerException {

        InputSource in = null;
        SAXParser saxParser = null;
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        try {
            saxParser = saxParserFactory.newSAXParser();
        } catch (ParserConfigurationException ex) {
            log.warn(Logging.stackTrace(ex));
        } catch (SAXException ex2) {
            log.warn(Logging.stackTrace(ex2));
        }

        // Create input source.
        in = new InputSource(input);
        // set base directory (only works as reader is not made in this method)
        in.setSystemId(dtdDirectory);
        log.service("Sax parser system id set to \"" + dtdDirectory + "\"");

        // Parse.
        try {

            saxParserFactory.setValidating(true);

            XMLReader reader = saxParser.getXMLReader();
            reader.setContentHandler(this);

            reader.setDTDHandler(this);
            reader.setEntityResolver(this);
            reader.setErrorHandler(this);
            reader.parse(in);
        } catch (SAXException e2) {
            // If a transaction is in progress, delete it.
            if (transaction != null) {
                if (log.isDebugEnabled()) {
                    log.debug("About to roll back transaction " + transaction.getKey() + " because an error occurred.");
                }
                try {
                    transaction.delete();
                } catch (Exception e4) {
                    log.debug("Failed to roll back transaction " + transaction.getKey() + " after an error occurred.");
                }
            }

            // Convert SAXException to TransactionHandlerException.
            Exception e3 = e2.getException();
            if (e3 != null && e3 instanceof TransactionHandlerException) {
                // Use embedded TransactionHandlerException.
                throw (TransactionHandlerException)e3;
            } else {
                // Wrap SAXException in TransactionHandlerException.
                throw new TransactionHandlerException("Parse failed: \n" + e2);
            }
        }
    }

    /**
     * Accessor for exceptionPage.
     * @return value of exceptionPage field.
     */
    String getExceptionPage() {
        return exceptionPage;
    }

    /**
     * Appends text to reportFile (only if reportfile is specified).
     * The file is opened and closed every time this method is called.
     * @param text The text to append.
     * @throws IOException If an IO failure occurred.
     */
    private void appendReportFile(String text) throws IOException {
        Writer out = null;
        if (reportFile != null) {
            try {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reportFile.getPath(), true), ENCODING));
                out.write(text);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }
}
