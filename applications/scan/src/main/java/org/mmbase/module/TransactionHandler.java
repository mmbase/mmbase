/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import org.mmbase.util.*;
import org.mmbase.module.core.*;

import java.util.*;
import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

import org.mmbase.util.logging.*;

/**
 * TransactionHandler Module
 * This class parses the TML code and calls the appropriate methods
 * in TransactionManager TemporarayNodeManager org.mmabse.module.core
 * Furthermore it does some nameserving.
 * @deprecated-now use org.mmbase.applications.xmlimporter.TransactionHandler
 *
 * @author  John Balder: 3MPS
 * @author 	Rob Vermeulen: VPRO
 */

public class TransactionHandler extends Module implements TransactionHandlerInterface {

    private static Logger log = Logging.getLoggerInstance(TransactionHandler.class.getName());
    private static sessionsInterface sessions = null;
    private static MMBase mmbase = null;
    private static Upload upload = null;
    private static String version = "2.3.8";
    //JohnB key test
    private static boolean needs_key = true; //set true for safety
    private static String securityMode = "none";

    // Cashes all transactions belonging to a user.
    private static Hashtable<String, UserTransactionInfo> transactionsOfUser = new Hashtable<String, UserTransactionInfo>();
    // Reference to the transactionManager.
    private static TransactionManager transactionManager;
    // Reference to the temporaryNodeManager
    private static TemporaryNodeManager tmpObjectManager;

    public TransactionHandler() {}

    /**
     * initialize the transactionhandler
     */
    public void init() {
        log.info("Module TransactionHandler (" + version + ") started");
        mmbase = (MMBase)getModule("MMBASEROOT");
        upload = (Upload)getModule("upload");
        sessions = (sessionsInterface)getModule("SESSION");
        transactionManager = TransactionManager.getInstance();
        tmpObjectManager = transactionManager.getTemporaryNodeManager();
        //JB key test initializatioon
        needs_key = (getInitParameter("keycode") != null);
        securityMode = getInitParameter("security");
        if (securityMode == null)
            securityMode = "none";
        log.debug(">> needs key: " + needs_key + " mode: " + securityMode);
        //end JB key test initializatioon
    }

    /**
     * onLoad
     */
    public void onload() {
        log.debug("TransactionHandler onload");
    }

    /**
     * xmlHeader
     */
    private String getXMLHeader() {
        return "<?xml version='1.0'?>\n"
            + "<!DOCTYPE transactions PUBLIC \"-//MMBase//DTD transactions config 1.0//EN\" "
            + "\"https://www.mmbase.org/dtd/transactions_1_0.dtd\">\n";
    }

    /**
     * handleTransaction can be called externally and will execute the TCP commands.
     * @param template The template containing the TCP commands
     * @param session variables of an user
     * @param sp the scanpage
     */
    public void handleTransaction(String template, sessionInfo session, scanpage sp) {

        template = getXMLHeader() + template;
        log.service("TransactionHandler processing TCP");
        log.debug("Received template is:");
        log.debug(template);

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(template));
        // get the 'name' of the user.
        String user = session.getCookie();
        // get all transactions of the user.
        UserTransactionInfo uti = userInfo(user);
        // reset parsetrace....
        uti.trace = new ParseTrace();

        try {
            parse(null, is, uti);
        } catch (TransactionHandlerException t) {
            // Register the exception
            sessions.setValue(session, "TRANSACTIONTRACE", uti.trace.getTrace());
            sessions.setValue(session, "TRANSACTIONOPERATOR", t.transactionOperator);
            sessions.setValue(session, "TRANSACTIONID", t.transactionId);
            sessions.setValue(session, "OBJECTOPERATOR", t.objectOperator);
            sessions.setValue(session, "OBJECTID", t.objectId);
            sessions.setValue(session, "FIELDOPERATOR", t.fieldOperator);
            sessions.setValue(session, "FIELDNAME", t.fieldId);
            sessions.setValue(session, "TRANSACTIONERROR", t.getClass() + ": " + t.getMessage());
            log.error("Transaction Error:");
            log.error("TransactionTrace " + uti.trace.getTrace());
            log.error("TransactionOperator " + t.transactionOperator);
            log.error("TransactionId " + t.transactionId);
            log.error("ObjectOperator " + t.objectOperator);
            log.error("ObjectId " + t.objectId);
            log.error("FieldOperator " + t.fieldOperator);
            log.error("Fieldname " + t.fieldId);
            log.error("TransActionError " + t.toString());
            log.error("ExceptionPage " + t.exceptionPage + "\n");
            // set jump to exception page
            sp.res.setStatus(302, "OK");
            sp.res.setHeader("Location", t.exceptionPage);
        }
    }

    /**
     * Begin parsing the document
     */
    private void parse(String xFile, InputSource iSource, UserTransactionInfo userTransactionInfo) throws TransactionHandlerException {
        Document document = null;
        Element docRootElement;
        NodeList transactionContextList = null;
        String exceptionPage = "exception.shtml";
        XMLCheckErrorHandler errorHandler = new XMLCheckErrorHandler();

        DocumentBuilder db = XMLBasicReader.getDocumentBuilder(true, errorHandler);

        try {
            if (xFile != null) {
                log.debug("parsing file: " + xFile);
                document = db.parse(xFile);
            } else {
                if (iSource != null) {
                    log.debug("parsing input: " + iSource.toString());

                    document = db.parse(iSource);
                } else {
                    log.error("No xFile and no iSource file received!");
                }
            }
            Iterator<ErrorStruct> i = errorHandler.getResultList().iterator();
            while (i.hasNext()) {
                log.error("" + i.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
            TransactionHandlerException te = new TransactionHandlerException(e.getMessage());
            throw te;
        }

        try {
            // get <Transactions> context
            docRootElement = document.getDocumentElement();

            // get the exceptionPage attribuut
            exceptionPage = docRootElement.getAttribute("exceptionPage");
            if (exceptionPage.equals("")) {
                exceptionPage = "exception.shtml";
            }

            //  JB key test, only if there was a key defined in <module>.xml
            if (needs_key) {
                String key;
                key = docRootElement.getAttribute("key");

                if ((key == null) || (!key.equals(getInitParameter("keycode")))) {
                    if (securityMode.equals("signal")) {
                        log.info("Transaction (TCP) key is incorrect." + " TCP key='" + key + "' Server TCP key='" + getInitParameter("keycode") + "'");
                    }
                    if (securityMode.equals("secure")) {
                        log.error("Transaction (TCP) key is incorrect." + " TCP key='" + key + "' Server TCP key='" + getInitParameter("keycode") + "'");
                        TransactionHandlerException te =
                            new TransactionHandlerException(
                                "Transaction (TCP) key is incorrect." + " TCP key='" + key + "' Server TCP key='" + getInitParameter("keycode") + "'");
                        te.exceptionPage = exceptionPage;
                        throw te;
                    }
                }
            }

            // do for all transaction contexts (create-, open-, commit- and deleteTransaction)
            transactionContextList = docRootElement.getChildNodes();

        } catch (Exception t) {
            log.error("Error in reading transaction.");
        }

        // evaluate all these transactions
        try {
            evaluateTransactions(transactionContextList, userTransactionInfo);
        } catch (TransactionHandlerException te) {
            te.exceptionPage = exceptionPage;
            throw te;
        }

        log.debug("exiting parse method");
    }

    private void evaluateTransactions(NodeList transactionContextList, UserTransactionInfo userTransactionInfo) throws TransactionHandlerException {
        Node currentTransactionArgumentNode;
        String currentTransactionContext;
        boolean anonymousTransaction = true;
        Node transactionContext;
        TransactionInfo transactionInfo = null;

        for (int i = 0; i < transactionContextList.getLength(); i++) {
            // XML Parsing part
            currentTransactionArgumentNode = null;
            currentTransactionContext = null;
            String id = null, commit = null, time = null;

            transactionContext = transactionContextList.item(i);
            String tName = transactionContext.getNodeName();
            if (tName.equals("#text"))
                continue;

            //get attributes for transaction
            NamedNodeMap nm = transactionContext.getAttributes();
            if (nm != null) {
                //id
                currentTransactionArgumentNode = nm.getNamedItem("id");
                if (currentTransactionArgumentNode != null) {
                    id = currentTransactionArgumentNode.getNodeValue();
                }
                //commitOnClose
                currentTransactionArgumentNode = nm.getNamedItem("commit");
                if (currentTransactionArgumentNode != null) {
                    commit = currentTransactionArgumentNode.getNodeValue();
                }
                //timeOut
                currentTransactionArgumentNode = nm.getNamedItem("timeOut");
                if (currentTransactionArgumentNode != null) {
                    time = currentTransactionArgumentNode.getNodeValue();
                }
            }
            // XML Parsing done

            // Execution of XML
            if (id == null) {
                anonymousTransaction = true;
                id = uniqueId();
            } else {
                anonymousTransaction = false;
            }
            if (commit == null)
                commit = "true";
            if (time == null)
                time = "60";

            log.debug("-> " + tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")");
            userTransactionInfo.trace.addTrace(tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")", 1, true);

            try {
                // CREATE TRANSACTION
                if (tName.equals("create")) {
                    // Check if the transaction already exists.
                    if (userTransactionInfo.knownTransactionContexts.get(id) != null) {
                        throw new TransactionHandlerException(tName + " transaction already exists id = " + id);
                    }
                    // actually create transaction
                    transactionManager.createTransaction(id);
                    currentTransactionContext = id;
                    transactionInfo = new TransactionInfo(currentTransactionContext, time, id, userTransactionInfo);
                    // If not anonymous transaction register it in the list of all transaction of the user
                    if (!anonymousTransaction) {
                        userTransactionInfo.knownTransactionContexts.put(id, transactionInfo);
                    }
                } else {

                    if (tName.equals("open")) {
                        // TIMEOUT ADJUSTMENT IS NOT ACCORDING TO THE MANUAL
                        // Check if the transaction exists.
                        if (userTransactionInfo.knownTransactionContexts.get(id) == null) {
                            throw new TransactionHandlerException(tName + " transaction doesn't exists id = " + id);
                        }
                        // actually open transaction
                        transactionInfo = userTransactionInfo.knownTransactionContexts.get(id);
                        currentTransactionContext = transactionInfo.transactionContext;
                    } else {

                        if (tName.equals("commit")) {
                            if (anonymousTransaction == true) {
                                throw new TransactionHandlerException("commit tag needs id attribure");
                            }
                            if (userTransactionInfo.knownTransactionContexts.get(id) == null) {
                                throw new TransactionHandlerException("Transaction '" + id + "' is probably already committed, check attribute commit=false");
                            }
                            // actually open transaction
                            transactionInfo = userTransactionInfo.knownTransactionContexts.get(id);
                            currentTransactionContext = transactionInfo.transactionContext;
                            transactionManager.commit(userTransactionInfo.user, currentTransactionContext);
                            // destroy transaction information
                            transactionInfo.stop();
                            // continue with next transaction command.
                            continue;
                        } else {

                            if (tName.equals("delete")) {
                                // cancel real transaction
                                transactionManager.cancel(userTransactionInfo.user, id);
                                // get transaction information object
                                TransactionInfo ti = userTransactionInfo.knownTransactionContexts.get(id);
                                // destroy transaction information
                                ti.stop();
                                // continue with next transaction command.
                                continue;
                            } else {
                                throw new TransactionHandlerException("transaction operator " + tName + " doesn't exist");
                            }
                        }
                    }
                }

                // DO OBJECTS
                //do for all object contexts (create-, open-, get- and deleteObject)
                NodeList objectContextList = transactionContext.getChildNodes();
                // Evaluate all objects
                evaluateObjects(objectContextList, userTransactionInfo, currentTransactionContext, transactionInfo);

                // ENDING TRANSACTION
                //if (tName.equals("deleteTransaction")) // this is done above
                //if (tName.equals("commitTransaction")) // this is done above
                if (tName.equals("create") || tName.equals("open")) {
                    if (commit.equals("true")) {
                        transactionManager.commit(userTransactionInfo.user, currentTransactionContext);
                        transactionInfo.stop();
                        if (!anonymousTransaction) {
                            userTransactionInfo.knownTransactionContexts.remove(id);
                        }
                    }
                }
                /**
                	This is already done by TransactionInfo.stop()
                	and this code in never reached.
                if (tName.equals("delete") || tName.equals("commit")) {
                		if (!anonymousTransaction) {
                			userTransactionInfo.knownTransactionContexts.remove(id);
                		}
                }
                */
                log.debug("<- " + tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")");
                // End execution of XML
            } catch (Exception e) {
                e.printStackTrace();
                TransactionHandlerException t = null;
                if (e instanceof TransactionHandlerException) {
                    t = (TransactionHandlerException)e;
                } else {
                    t = new TransactionHandlerException("" + e);
                }
                t.transactionOperator = tName;
                t.transactionId = id;
                throw t;
            }
        }
    }

    /**
     * Evaluate and execute object methods
     */
    private void evaluateObjects(
        NodeList objectContextList,
        UserTransactionInfo userTransactionInfo,
        String currentTransactionContext,
        TransactionInfo transactionInfo)
        throws TransactionHandlerException {
        Node currentObjectArgumentNode = null;
        Node objectContext;
        NodeList fieldContextList;
        String currentObjectContext;
        boolean anonymousObject = true;

        for (int j = 0; j < objectContextList.getLength(); j++) {
            String id = null, type = null, oMmbaseId = null;
            String relationSource = null, relationDestination = null;
            String deleteRelations = "false";
            currentObjectContext = null;

            // XML thingies
            objectContext = objectContextList.item(j);
            String oName = objectContext.getNodeName();

            if (oName.equals("#text"))
                continue;

            //get attributes
            NamedNodeMap nm2 = objectContext.getAttributes();
            if (nm2 != null) {
                currentObjectArgumentNode = nm2.getNamedItem("id");
                if (currentObjectArgumentNode != null)
                    id = currentObjectArgumentNode.getNodeValue();
                //type
                currentObjectArgumentNode = nm2.getNamedItem("type");
                if (currentObjectArgumentNode != null)
                    type = currentObjectArgumentNode.getNodeValue();
                //mmbaseId
                currentObjectArgumentNode = nm2.getNamedItem("mmbaseId");
                if (currentObjectArgumentNode != null)
                    oMmbaseId = currentObjectArgumentNode.getNodeValue();
                // source relation
                currentObjectArgumentNode = nm2.getNamedItem("source");
                if (currentObjectArgumentNode != null)
                    relationSource = currentObjectArgumentNode.getNodeValue();
                // destination relation
                currentObjectArgumentNode = nm2.getNamedItem("destination");
                if (currentObjectArgumentNode != null)
                    relationDestination = currentObjectArgumentNode.getNodeValue();
                // have to delete relations?
                currentObjectArgumentNode = nm2.getNamedItem("deleteRelations");
                if (currentObjectArgumentNode != null)
                    deleteRelations = currentObjectArgumentNode.getNodeValue();
            }
            if (id == null) {
                id = uniqueId();
                anonymousObject = true;
            } else {
                anonymousObject = false;
            }

            if (oName.equals("createRelation")) {
                log.debug(oName + " id(" + id + ") source(" + relationSource + ") destination(" + relationDestination + ")");
                userTransactionInfo.trace.addTrace(oName + " id(" + id + ") source(" + relationSource + ") destination(" + relationDestination + ")", 2, true);
            } else {
                log.debug("-> " + oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")");
                userTransactionInfo.trace.addTrace(oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")", 2, true);
            }

            try {
                if (oName.equals("createObject")) {
                    // check for existence
                    if (transactionInfo.knownObjectContexts.get(id) != null) {
                        throw new TransactionHandlerException(oName + " Object id already exists: " + id);
                    }
                    // actually create and administrate if not anonymous
                    currentObjectContext = tmpObjectManager.createTmpNode(type, userTransactionInfo.user.getName(), id);
                    if (!anonymousObject) {
                        transactionInfo.knownObjectContexts.put(id, currentObjectContext);
                    }
                    // add to tmp cloud
                    transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);
                } else {
                    if (oName.equals("createRelation")) {
                        // check for existence
                        if (transactionInfo.knownObjectContexts.get(id) != null) {
                            throw new TransactionHandlerException(oName + " Object id already exists: " + id);
                        }
                        // actually create and administrate if not anonymous
                        currentObjectContext =
                            tmpObjectManager.createTmpRelationNode(type, userTransactionInfo.user.getName(), id, relationSource, relationDestination);
                        if (!anonymousObject) {
                            transactionInfo.knownObjectContexts.put(id, currentObjectContext);
                        }
                        // add to tmp cloud
                        transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);
                    } else {
                        if (oName.equals("accessObject")) {
                            // check for existence
                            if (transactionInfo.knownObjectContexts.get(id) != null) {
                                throw new TransactionHandlerException(oName + " Object id already exists: " + id);
                            }
                            if (oMmbaseId == null) {
                                throw new TransactionHandlerException(oName + " no MMbase id: ");
                            }
                            // actually get presistent object
                            currentObjectContext = tmpObjectManager.getObject(userTransactionInfo.user.getName(), id, oMmbaseId);
                            // add to tmp cloud
                            transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);
                            // if object has a user define handle administrate object in transaction
                            if (!anonymousObject)
                                transactionInfo.knownObjectContexts.put(id, currentObjectContext);
                        } else {
                            if (oName.equals("openObject")) {
                                if (transactionInfo.knownObjectContexts.get(id) == null) {
                                    throw new TransactionHandlerException(oName + " Object id doesn't exists: " + id);
                                }
                                currentObjectContext = transactionInfo.knownObjectContexts.get(id);
                            } else {
                                if (oName.equals("deleteObject")) {
                                    if (id == null) {
                                        throw new TransactionHandlerException(oName + " no id specified");
                                    }
                                    //delete from temp cloud
                                    currentObjectContext = transactionInfo.knownObjectContexts.get(id);
                                    transactionManager.removeNode(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);
                                    // destroy
                                    tmpObjectManager.deleteTmpNode(userTransactionInfo.user.getName(), currentObjectContext);
                                    transactionInfo.knownObjectContexts.remove(id);
                                    continue;
                                } else {
                                    if (oName.equals("markObjectDelete")) {
                                        if (oMmbaseId == null) {
                                            throw new TransactionHandlerException(oName + " no mmbaseId specified");
                                        }
                                        // Mark persistent object deleted.
                                        currentObjectContext = tmpObjectManager.getObject(userTransactionInfo.user.getName(), id, oMmbaseId);
                                        transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);
                                        transactionManager.deleteObject(currentTransactionContext, userTransactionInfo.user.getName(), currentObjectContext);

                                        // destroy
                                        //tmpObjectManager.deleteTmpNode(userTransactionInfo.user.getName(),currentObjectContext);
                                        if (transactionInfo.knownObjectContexts.containsKey(id)) {
                                            transactionInfo.knownObjectContexts.remove(id);
                                        }

                                        // Check relations attached to object to delete
                                        Vector relations = mmbase.getInsRel().getRelations_main(new Integer(oMmbaseId).intValue());
                                        if (relations.size() != 0) {
                                            if (deleteRelations.equals("true")) {
                                                Iterator iterator = relations.iterator();
                                                while (iterator.hasNext()) {
                                                    MMObjectNode node = (MMObjectNode)iterator.next();
                                                    oMmbaseId = "" + node.getValue("number");
                                                    id = uniqueId();
                                                    currentObjectContext = tmpObjectManager.getObject(userTransactionInfo.user.getName(), id, oMmbaseId);
                                                    transactionManager.addNode(
                                                        currentTransactionContext,
                                                        userTransactionInfo.user.getName(),
                                                        currentObjectContext);
                                                    transactionManager.deleteObject(
                                                        currentTransactionContext,
                                                        userTransactionInfo.user.getName(),
                                                        currentObjectContext);
                                                }
                                            } else {
                                                throw new TransactionHandlerException(
                                                    "object has " + relations.size() + " relation(s) attached to it. (use deleteRelations=\"true\")");
                                            }
                                        }
                                        continue;
                                    } else {
                                        throw new TransactionHandlerException("object operator " + oName + " doesn't exist");
                                    }
                                }
                            }
                        }
                    }
                }

                // DO FIELDS
                //do for all field contexts (setField)
                fieldContextList = objectContext.getChildNodes();
                // Evaluate Fields
                evaluateFields(fieldContextList, userTransactionInfo, id, currentObjectContext);

                if (oName.equals("deleteObject")) {}
                if (oName.equals("createObject")) {}
                if (oName.equals("openObject")) {}
                if (oName.equals("accessObject")) {}

                if (oName.equals("createRelation")) {
                    log.debug("<- " + oName + " id(" + id + ") source(" + relationSource + ") destination(" + relationDestination + ")");
                } else {
                    log.debug("<- " + oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")");
                }
            } catch (Exception e) {
                TransactionHandlerException t = null;
                if (e instanceof TransactionHandlerException) {
                    t = (TransactionHandlerException)e;
                } else {
                    t = new TransactionHandlerException("" + e);
                }
                t.objectOperator = oName;
                t.objectId = id;
                throw t;
            }
        }
    }

    private void evaluateFields(NodeList fieldContextList, UserTransactionInfo userTransactionInfo, String oId, String currentObjectContext)
        throws TransactionHandlerException {

        for (int k = 0; k < fieldContextList.getLength(); k++) {
            String fieldName = null;
            Object fieldValue = "";

            Node fieldContext = fieldContextList.item(k);
            String nodeName = fieldContext.getNodeName();

            log.debug("nodeName = " + nodeName);

            if (nodeName.equals("#text")) {
                continue;
            }

            if (!nodeName.equals("setField")) {
                log.error(nodeName + " is not a valid operation on an object");
                throw new TransactionHandlerException(nodeName + " is not a valid operation on an object");
            }

            //get attributes
            NamedNodeMap nm3 = fieldContext.getAttributes();
            if (nm3 != null) {
                Node currentObjectArgumentNode = nm3.getNamedItem("name");
                if (currentObjectArgumentNode != null) {
                    fieldName = currentObjectArgumentNode.getNodeValue();
                }
                if (fieldName == null) {
                    throw new TransactionHandlerException("<setField name=\"fieldname\">value</setField> is missing the NAME attribute!");
                }

                // Is value set by url? Or just between the setField tags?
                currentObjectArgumentNode = nm3.getNamedItem("url");
                if (currentObjectArgumentNode != null) {
                    String url = currentObjectArgumentNode.getNodeValue();
                    fieldValue = upload.getFile(url);
                    upload.deleteFile(url);
                    log.debug("-X Object " + oId + ": [" + fieldName + "] set to: " + url);
                } else {

                    Node setFieldValue = fieldContext.getFirstChild();
                    if (setFieldValue != null) {
                        fieldValue = setFieldValue.getNodeValue();
                    }
                    log.debug("-X Object " + oId + ": [" + fieldName + "] set to: " + fieldValue);
                }
                userTransactionInfo.trace.addTrace("setField " + oId + ": [" + fieldName + "] set to: " + fieldValue, 3, true);

                try {
                    tmpObjectManager.setObjectField(userTransactionInfo.user.getName(), currentObjectContext, fieldName, fieldValue);
                } catch (Exception e) {
                    TransactionHandlerException the = new TransactionHandlerException("cannot set field '" + fieldName + "'");
                    the.fieldId = fieldName;
                    the.fieldOperator = "SETFIELD";
                    throw the;
                }
            }
        }
    }

    private UserTransactionInfo userInfo(String user) {
        if (!transactionsOfUser.containsKey(user)) {
            log.debug("Create UserTransactionInfo for user " + user);
            // make acess to all variables indexed by user;
            UserTransactionInfo uti = new UserTransactionInfo();
            transactionsOfUser.put(user, uti);
            uti.user = new User(user);
        } else {
            log.warn("UserTransactionInfo already known for user " + user);
        }
        return transactionsOfUser.get(user);
    }

    /**
    	 * create unique number
     */
    private synchronized String uniqueId() {
        try {
            Thread.sleep(1); // A bit paranoid, but just to be sure that not two threads steal the same millisecond.
        } catch (InterruptedException e) {
            log.debug("What's the reason I may not sleep?");
        }
        return "ID" + java.lang.System.currentTimeMillis();
    }

    /**
     * Dummy User object, this object needs to be replace by
     * the real User object (when that is finished)
     */
    class User implements org.mmbase.security.UserContext {
        private String name;

        public User(String name) {
            this.name = name;
        }

        String getName() {
            int length = name.length();
            String tempname = "TR" + name.substring(length - 8, length);
            return tempname;
        }
        public String getIdentifier() {
            return name;
        }

        public org.mmbase.security.Rank getRank() {
            return org.mmbase.security.Rank.BASICUSER;
        }


        public boolean isValid() {
            return true;
        }
        public String getOwnerField() {
            return "default";
        }

        public String getAuthenticationType() {
            return "scan";
        }

    }

    /**
     * container class for transactions per user
     */
    class UserTransactionInfo {
        // contains all known transactions of a user
        public Hashtable<String, TransactionInfo> knownTransactionContexts = new Hashtable<String, TransactionInfo>();
        // The user
        public User user = null;
        // the parse trace
        public ParseTrace trace = null;
    }

    /**
     * container class for objects per transaction
     */
    class TransactionInfo implements Runnable {
        // The transactionname
        String transactionContext = null;
        // All objects belonging to a certain transaction
        Hashtable<String, String> knownObjectContexts = new Hashtable<String, String>();
        // Needed to timeout transaction
        long timeout = 0;
        // id of the transaction
        String id = "";
        // thread to monitor timeout
        Thread kicker = null;
        // List of transaction of a user
        UserTransactionInfo uti = null;
        // Is the transaction finished or timedout?
        boolean finished = false;

        TransactionInfo(String t, String timeout, String id, UserTransactionInfo uti) {
            this.transactionContext = t;
            this.timeout = Long.parseLong(timeout) * 1000;
            this.id = id;
            this.uti = uti;
            start();
        }

        /**
        	 * start the TransactionInfo to sleep untill it may timeout
         */
        public void start() {
            if (kicker == null) {
                kicker = new Thread(this, "TR " + transactionContext);
                kicker.start();
            }
        }

        /**
        	 * stop the timeout sleep and cleanup this TransactionInfo
         */
        public synchronized void stop() {
            kicker = null;
            finished = true;
            this.notify();
        }

        /**
        	 * sleep untill the transaction times out.
         * this can be interrupted by invoking the stop method.
        	 */
        public void run() {
            try {
                synchronized (this) {
                    this.wait(timeout * 1000);
                }
            } catch (InterruptedException e) {}
            uti.knownTransactionContexts.remove(id);
            if (!finished) {
                log.warn("Transaction with id=" + id + " is timed out after " + timeout / 1000 + " seconds.");
            }
        }

        public String toString() {
            return "TransactionInfo => transactionContext=" + transactionContext + " id=" + id + " timeout=" + timeout + ".";
        }
    }

    class ParseTrace {
        private String trace = "";

        void addTrace(String s, int indent, boolean new_line) {
            if (new_line)
                trace = trace + "\n<BR>";
            for (int i = 0; i < indent; i++)
                trace = trace + "\t";
            trace = trace + s;
        }

        String getTrace() {
            return trace;
        }
    }
}
