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
import java.lang.*;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.*;


/**
 * TransactionHandler Module
 *
 * @author  John Balder: 3MPS 
 * @author 	Rob Vermeulen: VPRO
 * @version 1.2, 22/10/2000
 * @version 2.3.1 24/11/2000
 *
 * This class parses the TML code and calls the appropriate methods
 * in TransactionManager TemporarayNodeManager org.mmabse.module.core
 * Furthermore is does some nameserving.
 */
 
public class TransactionHandler extends Module implements TransactionHandlerInterface {
	
	private static boolean _debug = true;
 	private static sessionsInterface sessions = null;
	private static MMBase mmbase = null;
	private static String version="2.3.4";

	// Cashes all transactions belonging to a user.
	private static Hashtable transactionsOfUser = new Hashtable();
	// Reference to the transactionManager.
	private static TransactionManagerInterface transactionManager;
	// Reference to the temporaryNodeManager
	private static TemporaryNodeManagerInterface tmpObjectManager;

	/**
	 * prints debug
	 */
	private void debug( String msg, int indent) {
		System.out.print("TR: ");
		for (int i = 1; i < indent; i++) System.out.print("\t");
		System.out.println(msg);
	}
	
	public TransactionHandler() {
	}
	
	/**
	 * initialize the transactionhandler
	 */
	public void init(){
		if (_debug) debug(">> init TransactionHandler Module version " + version, 0);
		mmbase=(MMBase)getModule("MMBASEROOT");
 		sessions = (sessionsInterface)getModule("SESSION");
		tmpObjectManager = new TemporaryNodeManager(mmbase);
		transactionManager = new TransactionManager(mmbase,tmpObjectManager);
	}

	/**	
	 * onLoad
	 */	
	public void onload(){
		if (_debug) debug(">> onload TransactionHandler Module ", 0);
	}

	/**
	 * xmlHeader
	 */
	private final String xmlHeader =
	"<?xml version='1.0'?> <!DOCTYPE TRANSACTION SYSTEM \"Transactions.dtd\">";
	
	/**
	 * handleTransaction can be called externally and will execute the TCP commands.
	 * @param template The template containing the TCP commands 
	 * @param the session variables of an user
	 * @param sp the scanpage 
	 */
	public void handleTransaction(String template, sessionInfo session, scanpage sp) {
			
		if (_debug) debug("Received template is:", 0);
		if (_debug) debug(template, 0);
		
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
			sessions.setValue (session, "TRANSACTIONTRACE",uti.trace.getTrace()); 
			sessions.setValue (session, "TRANSACTIONOPERATOR",t.transactionOperator); 
			sessions.setValue (session, "TRANSACTIONID",t.transactionId); 
			sessions.setValue (session, "OBJECTOPERATOR",t.objectOperator); 
			sessions.setValue (session, "OBJECTID",t.objectId); 
			sessions.setValue (session, "FIELDOPERATOR",t.fieldOperator); 
			sessions.setValue (session, "FIELDNAME",t.fieldId); 
			sessions.setValue (session, "TRANSACTIONERROR",t.getClass() + ": " + t.getMessage());
			if(_debug) { 
				debug("TRANSACTION ERROR >>>>>>>>>>>>>>>>>>",0);
				debug("TRANSACTIONTRACE " + uti.trace.getTrace(),1);
				debug("TRANSACTIONOPERATOR " + t.transactionOperator,1);
				debug("TRANSACTIONID " + t.transactionId,1);
				debug("OBJECTOPERATOR " + t.objectOperator,2);
				debug("OBJECTID " + t.objectId,2);
				debug("FIELDOPERATOR " + t.fieldOperator,3);
				debug("FIELDNAME " + t.fieldId,3);
				debug("TRANSACTIONERROR " + t.toString(),1);
				debug("EXCEPTIONPAGE " + t.exceptionPage,1);
				debug("TRANSACTION ERROR >>>>>>>>>>>>>>>>>>",0);
			}
			// set jump to exception page
			sp.res.setStatus(302,"OK");
			sp.res.setHeader("Location",t.exceptionPage);
		}
	}

	/**
	 * Begin parsing the document
	 */	
	private void parse(String xFile, InputSource iSource, UserTransactionInfo userTransactionInfo) 
 		throws TransactionHandlerException {
		Document document;
		Element docRootElement;
		NodeList transactionContextList = null;
		String exceptionPage = "exception.shtml";
		
		DOMParser parser = new DOMParser();
		
		try {
			if (xFile !=  null) {
		   		if (_debug) debug("parsing file: " + xFile, 0);
		   		parser.parse(xFile);
		   	} else {
				if (iSource !=  null) {
		   			if (_debug) debug("parsing input: " + iSource.toString(), 0);
		   			parser.parse(iSource);
				} else {
		   			debug("No xFile and no iSource file received!", 0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			TransactionHandlerException te = new TransactionHandlerException(e.getMessage());
			throw te;
		}
		
		try {  
			document = parser.getDocument();
		
			// get <Transactions> context
			docRootElement = document.getDocumentElement();
	
			// get the exceptionPage attribuut
			exceptionPage = docRootElement.getAttribute("exceptionPage");
			if(exceptionPage.equals("")) {
				exceptionPage = "exception.shtml";	
			}
		
			// do for all transaction contexts (create-, open-, commit- and deleteTransaction)
			transactionContextList = docRootElement.getChildNodes();
		
		} catch (Exception t) {
		   if(_debug) debug("Error in reading transaction.", 0);
		}

		// evaluate all these transactions
		try {
			evaluateTransactions(transactionContextList, userTransactionInfo);
		} catch (TransactionHandlerException te) {
			te.exceptionPage=exceptionPage;
			throw te;
		}

		if (_debug) debug("exiting parse method",0);
	}


	private void evaluateTransactions(NodeList transactionContextList, UserTransactionInfo userTransactionInfo) 
		throws TransactionHandlerException {
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
			if (tName.equals("#text")) continue;
			
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
			if (commit==null) commit="true";
			if (time==null) time="60";

			if (_debug) debug("-> " + tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")", 1);
			userTransactionInfo.trace.addTrace(tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")", 1, true);

			try {			
				// CREATE TRANSACTION
				if (tName.equals("createTransaction") || tName.equals("create")) {
					// Check if the transaction already exists.
					if (userTransactionInfo.knownTransactionContexts.get(id) != null) {
						throw new TransactionHandlerException(tName + " transaction already exists id = " + id);
					}
					// actually create transaction
					currentTransactionContext = transactionManager.create(userTransactionInfo.user, id);
					transactionInfo = new TransactionInfo(currentTransactionContext,time,id,userTransactionInfo);
					// If not anonymous transaction register it in the list of all transaction of the user
					if (!anonymousTransaction) {
						userTransactionInfo.knownTransactionContexts.put(id, transactionInfo);
					}
				} else {

				if (tName.equals("openTransaction") || tName.equals("open")) { 
					// TIMEOUT ADJUSTMENT IS NOT ACCORDING TO THE MANUAL
					// Check if the transaction exists.
					if (userTransactionInfo.knownTransactionContexts.get(id) == null) {
						throw new TransactionHandlerException(tName + " transaction doesn't exists id = " + id);
					}
					// actually open transaction
					transactionInfo = (TransactionInfo)userTransactionInfo.knownTransactionContexts.get(id);
					currentTransactionContext = transactionInfo.transactionContext;
				} else {

				if (tName.equals("commitTransaction") || tName.equals("commit")) { 
					transactionManager.commit(userTransactionInfo.user, currentTransactionContext);
					// destroy transaction information
					transactionInfo.stop();	
					// continue with next transaction command.
					continue;
				} else {

				if (tName.equals("deleteTransaction") || tName.equals("delete")) {
					// cancel real transaction
					transactionManager.cancel(userTransactionInfo.user, id);
					// get transaction information object
					TransactionInfo ti = (TransactionInfo)userTransactionInfo.knownTransactionContexts.get(id);
					// destroy transaction information
					ti.stop();
					// continue with next transaction command.
					continue;
 				} else {
                    throw new TransactionHandlerException("tag "+ tName + " doesn't exist");
                } } } }

	
	
				// DO OBJECTS
				//do for all object contexts (create-, open-, get- and deleteObject)
				NodeList objectContextList = transactionContext.getChildNodes();
				// Evaluate all objects
				evaluateObjects(objectContextList, userTransactionInfo, currentTransactionContext, transactionInfo);
	
	
				// ENDING TRANSACTION		
				//if (tName.equals("deleteTransaction")) // this is done above
				//if (tName.equals("commitTransaction")) // this is done above
				if (tName.equals("createTransaction") || tName.equals("openTransaction") ||
						 tName.equals("create") || tName.equals("open")) {
					if(commit.equals("true")) {
						transactionManager.commit(userTransactionInfo.user, currentTransactionContext);
						transactionInfo.stop();	
					}
				} 
				if (_debug) debug("<- " + tName + " id(" + id + ") commit(" + commit + ") time(" + time + ")", 1);
				// End execution of XML
			} catch (Exception e) {
				e.printStackTrace();
				TransactionHandlerException t=null;
				if (e instanceof TransactionHandlerException) {
					t=(TransactionHandlerException)e;
				} else {
					t = new TransactionHandlerException(""+e);
				}
				t.transactionOperator=tName;
				t.transactionId=id;
				throw t; 
			}
		}
	}

	/**
	 * Evaluate and execute object methods
	 */
	private void evaluateObjects(NodeList objectContextList, UserTransactionInfo userTransactionInfo, String currentTransactionContext, TransactionInfo transactionInfo) 
		throws TransactionHandlerException {
		Node currentObjectArgumentNode = null; 
		Node objectContext;
		NodeList fieldContextList;
		String currentObjectContext;
		boolean anonymousObject = true;

		for (int j = 0; j < objectContextList.getLength(); j++) {
			String id = null, type = null, oMmbaseId = null;
			String relationSource = null, relationDestination = null;
			currentObjectContext = null;
			
				
			// XML thingies
			objectContext = objectContextList.item(j);
			String oName = objectContext.getNodeName();

			if (oName.equals("#text")) continue;
				
			//get attributes
			NamedNodeMap nm2 = objectContext.getAttributes();
			if (nm2 != null) {
				currentObjectArgumentNode = nm2.getNamedItem("id");
				if (currentObjectArgumentNode != null) id = currentObjectArgumentNode.getNodeValue();
				//type
				currentObjectArgumentNode = nm2.getNamedItem("type");
				if (currentObjectArgumentNode != null) type = currentObjectArgumentNode.getNodeValue();
				//mmbaseId
				currentObjectArgumentNode = nm2.getNamedItem("mmbaseId");
				if (currentObjectArgumentNode != null) oMmbaseId = currentObjectArgumentNode.getNodeValue();
				// source relation
				currentObjectArgumentNode = nm2.getNamedItem("source");
				if (currentObjectArgumentNode != null) relationSource = currentObjectArgumentNode.getNodeValue();
				// destination relation
				currentObjectArgumentNode = nm2.getNamedItem("destination");
				if (currentObjectArgumentNode != null) relationDestination = currentObjectArgumentNode.getNodeValue();
			}
			if (id == null) {
				id = uniqueId();
				anonymousObject = true;
			} else {
				anonymousObject = false;
			}

			if (_debug) {
				if(oName.equals("createRelation")) {
					debug(oName + " id(" + id + ") source(" +relationSource +") destination("+relationDestination +")", 2);
					userTransactionInfo.trace.addTrace(oName + " id(" + id + ") source(" +relationSource +") destination("+relationDestination +")", 2, true);
				} else {
					debug("-> " + oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")", 2);
					userTransactionInfo.trace.addTrace(oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")", 2, true);
				}
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
					transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
				} else { 
				if (oName.equals("createRelation")) {
					// check for existence
					if (transactionInfo.knownObjectContexts.get(id) != null) {
						throw new TransactionHandlerException(oName + " Object id already exists: " + id);
					}
					// actually create and administrate if not anonymous
					currentObjectContext = tmpObjectManager.createTmpRelationNode(type, userTransactionInfo.user.getName(), id, relationSource, relationDestination);
					if (!anonymousObject) {
						transactionInfo.knownObjectContexts.put(id, currentObjectContext);
					}
					// add to tmp cloud
					transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
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
					debug("#### "+userTransactionInfo.user.getName(),0);
					debug("#### "+id,0);
					debug("#### "+oMmbaseId,0);
					currentObjectContext = tmpObjectManager.getObject(userTransactionInfo.user.getName(),id,oMmbaseId);
					// add to tmp cloud
					transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
					// if object has a user define handle administrate object in transaction 
					if (!anonymousObject)
						transactionInfo.knownObjectContexts.put(id, currentObjectContext);
				} else {
				if (oName.equals("openObject")) {
					if (transactionInfo.knownObjectContexts.get(id) == null) {
						throw new TransactionHandlerException(oName + " Object id doesn't exists: " + id);
					}
					currentObjectContext = (String)transactionInfo.knownObjectContexts.get(id);
				} else {
				if (oName.equals("deleteObject")) {
					if (id==null) { 
						throw new TransactionHandlerException(oName + " no id specified");
					}
					//delete from temp cloud
					currentObjectContext = tmpObjectManager.deleteTmpNode(userTransactionInfo.user.getName(), id);
					transactionManager.removeNode(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
					// destroy
					tmpObjectManager.deleteTmpNode(userTransactionInfo.user.getName(),currentObjectContext);
					transactionInfo.knownObjectContexts.remove(id);
					continue;
				} else {
				if (oName.equals("markObjectDelete")) {
					debug("markObjectDelete , an object may not have any relations",0);
					if (oMmbaseId==null) { 
						throw new TransactionHandlerException(oName + " no mmbaseId specified");
					}
					// Mark persistent object deleted.
					currentObjectContext = tmpObjectManager.getObject(userTransactionInfo.user.getName(),id,oMmbaseId);
					transactionManager.addNode(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
					transactionManager.deleteObject(currentTransactionContext, userTransactionInfo.user.getName(),currentObjectContext);
					// destroy
					tmpObjectManager.deleteTmpNode(userTransactionInfo.user.getName(),currentObjectContext);
					if(transactionInfo.knownObjectContexts.containsKey(id)) {
						transactionInfo.knownObjectContexts.remove(id);
					}
					continue;
				} else {
					throw new TransactionHandlerException("tag "+ oName + " doesn't exist");
				} } } } } } 
			

				// DO FIELDS
				//do for all field contexts (setField)
				fieldContextList = objectContext.getChildNodes();
				// Evaluate Fields
				evaluateFields(fieldContextList, userTransactionInfo, id ,currentObjectContext);
	
				if (oName.equals("deleteObject")) {
				}
				if (oName.equals("createObject")) {
				}
				if (oName.equals("openObject")) {
				}
				if (oName.equals("copyObject")) {
				} 
				
				if (_debug) {
					if(oName.equals("createRelation")) {
						debug("<- " + oName + " id(" + id + ") source(" +relationSource +") destination("+relationDestination +")", 2);
					} else {
						debug("<- " + oName + " id(" + id + ") type(" + type + ") oMmbaseId(" + oMmbaseId + ")", 2);
					}
				}
			} catch (Exception e) {
				TransactionHandlerException t=null;
				if (e instanceof TransactionHandlerException) {
					t=(TransactionHandlerException)e;
				} else {
					t = new TransactionHandlerException(""+e);
				}
				t.objectOperator=oName;
				t.objectId=id;
				throw t; 
			}
		}
	}

	private void evaluateFields(NodeList fieldContextList, UserTransactionInfo userTransactionInfo, String oId, String currentObjectContext)
		throws TransactionHandlerException {

		for (int k = 0; k < fieldContextList.getLength(); k++) {
			String fieldName = null;
			String fieldValue = "";
					
			Node fieldContext = fieldContextList.item(k);
			if (fieldContext.getNodeName().equals("#text")) continue;

			//get attributes
			NamedNodeMap nm3 = fieldContext.getAttributes();
			if (nm3 != null) {
				Node currentObjectArgumentNode = nm3.getNamedItem("name");
				if (currentObjectArgumentNode != null) {
					fieldName = currentObjectArgumentNode.getNodeValue();
				}
				if (fieldName==null) {
					 throw new TransactionHandlerException("<setField name=\"fieldname\">value</setField> is missing the NAME attribute!");
				}
				Node setFieldValue = fieldContext.getFirstChild();
				if(setFieldValue!=null) {
					fieldValue = setFieldValue.getNodeValue();
				}
				if (_debug) debug("-X Object " + oId + ": [" + fieldName + "] set to: " + fieldValue, 3);
				userTransactionInfo.trace.addTrace("setField " + oId + ": [" + fieldName + "] set to: " + fieldValue, 3, true);
		
				//check that we are inside object context
				//if (currentObjectContext == null) {
				//	 throw new TransactionHandlerException(oId + " set field " + fieldName + " to " + fieldValue);
				//}
				try {
					tmpObjectManager.setObjectField(userTransactionInfo.user.getName(),currentObjectContext, fieldName, fieldValue);
				} catch (Exception e) {
					TransactionHandlerException the = new TransactionHandlerException("cannot set field '"+fieldName+"'");
					the.fieldId=fieldName;
					the.fieldOperator="SETFIELD";	
					throw the;
				}
			}
		}
	}

	
	private UserTransactionInfo userInfo(String user) {
		if (!transactionsOfUser.containsKey(user)) {
			if (_debug) debug("Create UserTransactionInfo for user "+user,0);
			// make acess to all variables indexed by user;
			UserTransactionInfo uti = new UserTransactionInfo();
			transactionsOfUser.put(user, uti);
			uti.user = new User(user);
		} else {
			if (_debug) debug("UserTransactionInfo already known for user "+user,0);
		}
		return ((UserTransactionInfo) transactionsOfUser.get(user));
	}
		
		
	/**	
 	 * create unique number
	 */
	private synchronized String uniqueId() {
		try {
			Thread.sleep(1); // A bit paranoid, but just to be sure that not two threads steal the same millisecond.
		} catch (Exception e) {
			debug("What's the reason I may not sleep?",0);
		}
		return "ID"+java.lang.System.currentTimeMillis();
	}


	/**
	 * Dummy User object, this object needs to be replace by
	 * the real User object (when that is finished)
	 */
	class User {
		private String name;

		public User(String name) {
			this.name= name;
		}
		
		String getName() { 
			int length = name.length();
			String tempname = "TR"+ name.substring(length-8,length);
			//debug("Temporary name ="+tempname,0);
			return tempname;
		}
	}


	/**
	 * transactionHandler exception
	 */
	class TransactionHandlerException extends Exception {
		String code = "";
		String fieldId = "";
		String fieldOperator = "";
		String objectOperator = "";
		String objectId = "";
		String transactionOperator = "";
		String transactionId = "";
		String exceptionPage = "";

		TransactionHandlerException(String s) { 
			super(s); 
		}
	}
	
	/** 
	 * container class for transactions per user
	 */
	class UserTransactionInfo {
		// contains all known transactions of a user
		public Hashtable knownTransactionContexts = new Hashtable(); 
		// The user
		public User user = null;
		// the parse trace
		public ParseTrace trace = null;
	}

	/**
	 * container class for objects per transaction
	 */
	class TransactionInfo implements Runnable{
		// The transactionname 
		String transactionContext = null;		
		// All objects belonging to a certain transaction
		Hashtable knownObjectContexts = new Hashtable();
		// Needed to timeout transaction
		long timeout =0;
		// id of the transaction
		String id = "";
		// thread to monitor timeout
		Thread kicker = null;
		// List of transaction of a user
		UserTransactionInfo uti = null;
		// Is the transaction finished or timedout?
		boolean finished=false;

		TransactionInfo (String t, String timeout, String id, UserTransactionInfo uti) {
			this.transactionContext = t;
			this.timeout=Long.parseLong(timeout)*1000;
			this.id=id;
			this.uti=uti;
			start();
		}

		/** 
	 	 * start the TransactionInfo to sleep untill it may timeout
		 */
  		public void start() {
        	if (kicker == null) {
            	kicker = new Thread(this,"TR "+transactionContext);
            	kicker.start();
        	}
    	}

		/**
	 	 * stop the timeout sleep and cleanup this TransactionInfo
		 */
    	public synchronized void stop() {
        	kicker = null;
			finished = true;
			notify();
    	}

		/**
	 	 * sleep untill the transaction times out.
		 * this can be interrupted by invoking the stop method.
	 	 */
    	public void run () {
			try {
				synchronized(this) {
					wait(timeout*1000);
				}
			} catch (InterruptedException e) {
			}		
			uti.knownTransactionContexts.remove(id);
			if (_debug && !finished) {
				debug("Transaction with id="+id +" is timed out after "+timeout/1000+" seconds.",0);
			}
    	}

		public String toString() {
			return "TransactionInfo => transactionContext="+transactionContext+" id="+id+" timeout="+timeout+".";	
		}
	}
	
	class ParseTrace {
		private String trace = "";
		
		void addTrace(String s, int indent, boolean new_line) {
			if (new_line) trace = trace + "\n<BR>";
			for (int i = 0; i < indent; i++) trace = trace + "\t";
			trace = trace + s;
		}
		
		String getTrace() {
			return trace;
		}
	}
}
