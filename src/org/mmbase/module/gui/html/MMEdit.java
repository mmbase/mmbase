/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.gui.html;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import java.lang.String;

import javax.servlet.http.*;
import javax.servlet.*;

import org.mmbase.module.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * The module which provides access to the multimedia database
 * it creates, deletes and gives you methods to keep track of
 * multimedia objects. It does not give you direct methods for
 * inserting and reading them thats done by other objects
 *
 * @author Daniel Ockeloen
 * @author Hans Speijer
 */
public class MMEdit extends ProcessorModule {

	Hashtable commandHandlers; // The objects that handle process, replace and
	                           // list commands
	StateManager stateMngr; 
        
	MMBase mmBase;	

	/**
	 * The central initialisation wich builts up the complete module including
	 * all helper objects
	 */
	public void init() {
		CommandHandlerInterface newHandler;

		mmBase= (MMBase)getModule("MMBASEROOT"); 	 
		commandHandlers = new Hashtable();
		stateMngr = new StateManager(mmBase);
		commandHandlers.put("STATE", stateMngr);
		newHandler = new ObjectSelector(stateMngr); 
		commandHandlers.put("SELECT", newHandler);
		newHandler = new FieldSelector(stateMngr);
		commandHandlers.put("FIELDS", newHandler);
		newHandler = new FieldEditor(stateMngr);
		commandHandlers.put("EDIT", newHandler);
	}

	/**
	 * Only used by the classloader (no functionality
	 */
	public MMEdit() {
	}

	public void reload() {
		//init();
	}

	public void unload() {
	}

	public void shutdown() {
	}


	/*
	*/
        private String leadZero (int m) {
                if (m < 0 || m > 9) return ("" + m);
                return ("0" + m);
        }


	/*
	*/
        private Vector numberList (int begin, int end) {
                Vector v = new Vector ();

                for (int n = begin; n <= end; n++) v.addElement (leadZero (n));
                return (v);
        }


	/*
	*/
        private Vector allMonths () {
                Vector v = new Vector ();

                for (int m = 1; m <= 12; m++) v.addElement (FieldEditor.getMonthString (m));

                return v;
        }


	/*
	*/
        private Vector editDate (String what) {
                Vector v = new Vector ();

                if      (what.equals ("DAYS"))    v = numberList (1, 31);
                else if (what.equals ("MONTHS"))  v = allMonths ();
                else if (what.equals ("YEARS"))   v = numberList (1901, 2005);
                else if (what.equals ("HOURS"))   v = numberList (0, 23);
                else if (what.equals ("MINUTES")) v = numberList (0, 59);
		else if (what.equals ("SECONDS")) v = numberList (0, 59);

                return (v);
        }


	/**
	 * The hook that passes all list related pages to the correct handler
	 */
	public Vector getList(scanpage sp,StringTagger tagger, String command) {
		Vector result = new Vector();
		CommandHandlerInterface handler;

		StringTokenizer tok = new StringTokenizer(Strip.DoubleQuote(command,Strip.BOTH),"-\n\r");
		String token = tok.nextToken();

		if (token.startsWith ("DATE")) {
			result = editDate (tok.nextToken ());
		} else{
			if (token.startsWith ("FILES")) {
				DirectoryLister dirlister = new DirectoryLister();
				String path = tok.nextToken();
				result = dirlister.getDirectories(path);  //Retrieve all filepaths.
			}else{
				if (token.startsWith("TYPE_IMAGES")){
					String comparefield = "modtime";
					DirectoryLister imglister = new DirectoryLister();
					String path = tok.nextToken();
					Vector unsorted = imglister.getDirectories(path);  //Retrieve all filepaths
					Vector sorted = imglister.sortDirectories(unsorted,comparefield);
					result = imglister.createThreeItems(sorted,tagger);
					tagger.setValue("ITEMS", "3");
					//added 27jan1999
					String reverse = tagger.Value("REVERSE");
					if (reverse!=null){
						if(reverse.equals("YES")){
							int items = 3;
							result = imglister.reverse(result,items);
						}
					}
				}else {

					handler = (CommandHandlerInterface)commandHandlers.get(token);
	
					if (handler != null) {
						result = handler.getList(sp, tagger, tok);
					} else {
						result.addElement("List not defined (MMEdit)");
					}
				}
				
			}
		}

		return (result);
	}

	/**
	 * The hook that passes all form related pages to the correct handler
	 */
	public boolean process(scanpage sp, Hashtable cmds, Hashtable vars) {

		CommandHandlerInterface handler;
		String token, cmdline;
		
		for (Enumeration h = cmds.keys();h.hasMoreElements();) {
			cmdline=(String)h.nextElement();	
			StringTokenizer tok = new StringTokenizer(cmdline,"-\n\r");
			token = tok.nextToken();
			handler = (CommandHandlerInterface)commandHandlers.get(token);

			if (handler != null) handler.process(sp,tok, cmds, vars);
		}

		return (true);
	}		

	/**
	 * The hook that passes all replace and trigger related pages to the 
	 * correct handler
	 */
	public String replace(scanpage sp, String command) {
		CommandHandlerInterface handler;
		String token;

		StringTokenizer tok = new StringTokenizer(command,"-\n\r");
		token = tok.nextToken();
		// System.out.println("MMEDIT->"+token+" "+commandHandlers);
		handler =  (CommandHandlerInterface)commandHandlers.get(token);

		if (handler != null) return handler.replace(sp, tok);

		return ("Command not defined (MMEdit)");
	}
}
