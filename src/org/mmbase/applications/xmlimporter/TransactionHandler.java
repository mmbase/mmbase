/*
 * ClassName: TransactionHandler.java
 *
 * Date: dec. 1st. 2001
 *
 * Copyright notice:
 * This software is OSI Certified Open Source Software.
 * OSI Certified is a certification mark of the Open Source Initiative.
 *
 * The license (Mozilla version 1.0) can be read at the MMBase site.
 * See http://www.MMBase.org/license
 */
package org.mmbase.applications.xmlimporter;

import java.util.*;
import java.io.*;

import org.mmbase.module.Module;
import org.mmbase.module.sessionInfo;
import org.mmbase.module.sessionsInterface;

import org.mmbase.module.TransactionHandlerInterface;
import org.mmbase.module.Upload;

import org.mmbase.module.core.*;
import org.mmbase.util.scanpage;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import org.mmbase.util.logging.*;

/**
 * This class parses the TML code and calls the appropriate methods
 * in TransactionManager TemporarayNodeManager org.mmabse.module.core
 * Furthermore is does some nameserving.
 *
 * @author John Balder: 3MPS
 * @author Rob Vermeulen: VPRO
 * @author Rob van Maris: Finnalist IT Group
 * @author Erik Visser: Finnalist IT Group
 * @since MMBase-1.5
 * @version $Id: TransactionHandler.java,v 1.6 2003-01-09 14:44:28 robmaris Exp $
 */

public class TransactionHandler extends Module implements TransactionHandlerInterface {
   /** Logger instance. */
   private static Logger log = Logging.getLoggerInstance(TransactionHandler.class.getName());

   /** Current version number. */
   private static String version="1.12.2001";

   /** Sessions module. */
   private static sessionsInterface sessions;

   /** Hashtable to store a UserTransactionInfo object for each user. */
   private static Hashtable transactionsPerUser = new Hashtable();

   /** Hashtable to store a UserTransactionInfo object for each user. */
   private static Consultant consultant;

   /** XML file header, consisting of XML and DOCTYPE declaration. */
   private final String xmlHeader =
   "<?xml version='1.0'?>\n"
      + "<!DOCTYPE transactions "
      + "PUBLIC '-//MMBase/DTD transactions config 1.0//EN' " 
      + "'http://www.mmbase.org/dtd/transactions_1_0.dtd'>\n";

   /**
    * Create new TransactionHandler.
    */
   public TransactionHandler() {}

   /**
    * This method is called on loading of the transactionhandler module
    * and writes a log entry that the TransactionHandler is loaded.
    */
   public void onload(){
      log.info("Module TransactionHandler ("+version+") loaded.");
   }

   /**
    * Initialize the transactionhandler module.
    */
   public void init(){
      log.service("Module TransactionHandler ("+version+") started.");
      sessions = (sessionsInterface)Module.getModule("SESSION");
   }

   /**
    * special version of handleTransaction for SCAN PAGES
    * this version can be removed if scan is not supported anymore
    *
    * parses transactions xml file delivered by the reader and executes the TCP commands.
    * @param template  the template containing the TCP commands
    * @param session   the session variables of a user
    * @param sp        the scanpage
    */
   public void handleTransaction(String template, sessionInfo session, scanpage sp) {
      UserTransactionInfo uti;
      TransactionsParser parser = null;

      // Add header to xml file.
      // The class org.mmbase.module.gui.html.scanparser recognizes the xml file
      // because of the <trtansactions> tag. The header is already down the drain.
      // So it sends the file without header and it has to be added again.
      // The default utf8 encoding header is added.
      String xmlTransactions = xmlHeader + template;

      log.service("TransactionHandler processing TCP from scanpage");
      if (log.isDebugEnabled()) {
         log.trace("Received template (with added xml header) is:");
         log.trace(xmlTransactions);
      }

      // Get user transactions info object.
      if (session == null) {
         uti = new UserTransactionInfo();
         uti.user = new User("automaticUser");
      } else {
         //get user info
         String user = session.getCookie();
         uti = userInfo(user);
      }

      if (log.isDebugEnabled()) {
         log.debug("Transactions started..");
      }

      // Parse template.
      try {
         // Create InputSource.
         StringReader in = new StringReader(xmlTransactions);

         // Parse.
         parser = new TransactionsParser(uti);
         parser.parse(in);
      } catch (Exception e) {
         log.error("TransactionError :" + e.toString());
         log.error("ExceptionPage "+(parser != null? parser.getExceptionPage(): ""));

         if (session != null) {
            // Register the exception
            sessions.setValue(session, "TRANSACTIONERROR", e.toString());
         }

         if (sp != null) {
            // set jump to exception page
            try {
               sp.res.sendError(302,"OK");
            } catch (IOException e2) {
            }
            if (parser != null) {
               sp.res.setHeader("Location",parser.getExceptionPage());
            }
         }
      }
      log.warn("Transaction stopped at : " + getTime());
   }

   private String getTime() {
      return new Date().toString();
   }

   /**
    * Get transaction info for this user.
    * A UserTransactionInfo object is stored for each user,
    * holding information on the transactions held by that user.
    * Use this method to access it.
    * @param user the user.
    */
   private UserTransactionInfo userInfo(String user) {
      if (!transactionsPerUser.containsKey(user)) {
         if (log.isDebugEnabled()) {
            log.debug("Create UserTransactionInfo for user "+user);
         }
         // make acess to all variables indexed by user;
         UserTransactionInfo uti = new UserTransactionInfo();
         transactionsPerUser.put(user, uti);
         uti.user = new User(user);
      } else {
         log.warn("UserTransactionInfo already known for user "+user);
      }
      return ((UserTransactionInfo) transactionsPerUser.get(user));
   }

   /**
    * starts handleTransaction(java.io.Reader input) in a seperate thread
    * @param input input
    * @param consultant The intermediate import object. Used to set and get status from and set and get objects to and from.
    */
   public void handleTransactionAsynchronously(final java.io.Reader input,
   final org.mmbase.applications.xmlimporter.Consultant consultant) {
      //anonymous inner class
      Thread t = new Thread() {
         public void run() {
            handleTransaction(input, consultant);
         }
      };
      t.start();
   }

   /**
    * parses transactions xml file delivered by the reader and executes the TCP commands.
    * @param input the connection to the input source
    * @param consultant The intermediate import object. Used to set and get status from and set and get objects to and from.
    */
   public void handleTransaction(Reader input, Consultant consultant) {
      UserTransactionInfo uti;
      TransactionsParser parser = null;

      log.service("TransactionHandler processing TCP");
      if (log.isDebugEnabled()) {
         try{
            StringBuffer sb = new StringBuffer();
            String s;
            BufferedReader in = new BufferedReader(input);
            while((s=in.readLine()) != null) {
               sb.append(s);
            }
            String template = sb.toString();
            log.debug("Received template is:");
            log.debug(template);
            input = new StringReader(template);
         } catch (IOException e) {
            log.error("TransactionError " + e.toString());
         }
      }

      // Get user transactions info object.
      uti = new UserTransactionInfo();
      uti.user = new User("automaticUser");

      log.debug("Transactions started..");

      // Parse template.
      try {
         // Parse.
         consultant.setImportStatus(Consultant.IMPORT_RUNNING);
         parser = new TransactionsParser(uti, consultant);
         parser.parse(input);
         consultant.setImportStatus(Consultant.IMPORT_FINISHED);
      } catch (Exception e) {
         log.error("TransactionError :" + e.toString());
         log.error("ExceptionPage "+(parser!=null?parser.getExceptionPage():""));
         if (consultant.getImportStatus() != Consultant.IMPORT_TIMED_OUT) {
            consultant.setImportStatus(Consultant.IMPORT_EXCEPTION);
         }
      }
      log.warn("Transaction stopped at : " + getTime());
   }

   /**
    * Performs JB key test. Compares key with TransactionHandler keycode,
    * logs message and throws exception when key rejected, depending
    * on TransactionHandler security mode.
    * @param key The key provided with the transactions.
    * @throws TransactionHandlerException When the key is rejected,
    * while in secure mode.
    */
   void checkKey(String key) throws TransactionHandlerException {
       //  JB key test, only if there was a key defined in transactionhandler.xml
       String keycode = getInitParameter("keycode");
       String mode = getInitParameter("security");
       if (keycode != null) {

           if (key == null || !key.equals(keycode)) {
               // Keycode rejected.
               String message = "Transaction (TCP) key is incorrect."
               + " TCP key='" + key + "' Server TCP key='"
               + getInitParameter("keycode") + "'";

               // No mode specified: do nothing.
               if (mode == null) {
                   return;

               // Signal mode: just note in log.
               } else if (mode.equals("signal")) {
                   log.info(message);

               // Secure mode: throw exception
               } else if (mode.equals("secure")) {
                   log.error(message);
                   TransactionHandlerException te
                   = new TransactionHandlerException(message);
                   throw te;
               }
           }
       }
   }

}