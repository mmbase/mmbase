/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.gui.html;

import java.util.*;
import java.io.*;

import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.module.ParseException;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.gui.html.EditState;

/** 
 * The FieldEditor class is a frontend for the different TypeEditors (hitlisted). 
 *
 * @author Daniel Ockeloen
 * @author Hans Speijer
 * @author Arjan Houtman
 *
 */
public class FieldEditor implements CommandHandlerInterface {
	

	// Logger
	private static Logger log = Logging.getLoggerInstance(FieldEditor.class.getName());
	
	StateManager stateMngr;

	/**
	 * Constructor
	 */
	public FieldEditor(StateManager manager) {
		stateMngr = manager;

		// Deze classe moet alle verschillende TypeEditors gaan ondersteunen
		// kijk in ~/wwwdev/Projects/Editors/classdesign.java voor een eerste 
		// aanzet voor alle types
	}

	/**
	 * List commands
	 */
	public Vector getList(scanpage sp, StringTagger args, StringTokenizer command) throws ParseException {
		String token;
		String userName=HttpAuth.getRemoteUser(sp);

		EditState state = stateMngr.getEditState(userName);
		Vector result = new Vector();
		
		if (command.hasMoreTokens()) {
			token = command.nextToken();
			if (token.equals("DUMMY")) {	
			} else if (token.equals("DUMMY")) {	
			}
		}
		result.addElement("No List command defined (FieldEditor)");
		return result;
	}




	/**
	 * Replace/Trigger commands
	 */
	public String replace(scanpage sp, StringTokenizer command) {
		String token;
		String userName=HttpAuth.getRemoteUser(sp);
		EditState state = stateMngr.getEditState(userName);
		Vector result = new Vector();
		
		if (command.hasMoreTokens()) {
			token = command.nextToken();
			if (token.equals("GETFIELDVALUE")) {	
				return(getEditField(state,command.nextToken()));
			} else
			if (token.equals("SETFIELDVALUE")) {	
				return(setEditField(state,command.nextToken(),command.nextToken()));
			}
			else if (token.equals("GETFIELDDATE")) {
				return (getEditDateField (state, command.nextToken (), command.nextToken ()));
			} else if (token.equals("COMMIT")) {	
				return(""+doCommit(state,userName));
			} else if (token.equals("SETINSSAVE")) {	
				state.setInsSave(true);
			}
		}
		return "Command not defined (FieldEditor)";
	}

	/**
	 * The hook that passes all form related pages to the correct handler
	 */
	public boolean process(scanpage sp, StringTokenizer command,Hashtable cmds, Hashtable vars) {
		String token;
		String userName=HttpAuth.getRemoteUser(sp);
		EditState state = stateMngr.getEditState(userName);
		Vector result = new Vector();
		
		if (command.hasMoreTokens()) {
			token = command.nextToken();
			if (token.equals("SETFIELDVALUE")) {	
				return(setEditField(state,command.nextToken(),cmds));
			}
			else if (token.equals ("SETFIELDDATE")) {
				return (setEditDateField (state, command.nextToken (), cmds));
			} else if (token.equals("SETFIELDFILE_IMG")) {	
				log.info("FieldEditor -> FILE UPLOAD DETECTED");
				setEditIMGField(state,command.nextToken(),cmds,sp);
				return(true);
			} else if (token.equals("SETFIELDFILE_DISK")) {	
				log.info("FieldEditor -> FILE DISK DETECTED");
				setEditDISKField(state,command.nextToken(),cmds,sp);
				return(true);
			} else if (token.equals("DUMMY")) {	
			}
		}
		return false;
	}

	String getEditField(EditState ed, String fieldname) {
		MMObjectBuilder obj=ed.getBuilder(); // This one can be removed...
		MMObjectNode node=ed.getEditNode();
		if (node == null) return "";
		return Encode.encode("ESCAPE_HTML",node.getValueAsString( fieldname ));
	}



	boolean setEditField(EditState ed, String fieldname,Hashtable cmds) {
		MMObjectBuilder obj=ed.getBuilder();
		FieldDefs def=obj.getField(fieldname);
		int type=def.getDBType();
		String value=(String)cmds.get("EDIT-SETFIELDVALUE-"+fieldname);
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			node.setValue( fieldname, type, value );
			replaceOwner(ed,node);
		}
		return(true);
	}



	String setEditField(EditState ed, String fieldname,String value) {
		MMObjectBuilder obj=ed.getBuilder();
		FieldDefs def=obj.getField(fieldname);
		int type=def.getDBType();
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			node.setValue( fieldname, type, value );
			replaceOwner(ed,node);
		}
		return("");
	}



	/*
	*/
        static private String monthNames[] = 
		{ "januari", "februari", "maart", "april", "mei", "juni", "juli",
		  "augustus", "september", "oktober", "november", "december" };


	/*
	*/
        static public String getMonthString (int m) {
                return (monthNames[m - 1]);
        }


	/*
	*/
        private int getMonthNumber (String m) {
                int r;

                for (r = 0; r < 12; r++) if (m.equals (monthNames[r])) break;

                return (r + 1);
        }

        private String leadZero (int m) {
                if (m < 0 || m > 9) return ("" + m);
                return ("0"+m);
        }


	String getEditDateField (EditState ed, String what, String fieldname) {
		MMObjectNode node=ed.getEditNode();
                String res = new String ();

                if (node != null) {
                        int time = node.getIntValue(fieldname);
			Date d;

			if (time == -1) { // User current date and time
				d = new Date ();
			}
			else {
				d = new Date (((long)time * 1000)+DateSupport.getMilliOffset());
			}

                        if (what.equals ("DAYS")) res = leadZero (d.getDate ());
                        else if (what.equals ("MONTHS")) res = getMonthString (d.getMonth () + 1);
                        else if (what.equals ("YEARS")) res = "" + (1900 + d.getYear ());
                        else if (what.equals ("HOURS")) res = leadZero (d.getHours ());
                        else if (what.equals ("MINUTES")) res = leadZero (d.getMinutes ());
			else if (what.equals ("SECONDS")) res = leadZero (d.getSeconds ());
                }

                return (res);
	
	}



	boolean setEditDateField (EditState ed, String fieldname, Hashtable cmds) {
		MMObjectNode node = ed.getEditNode();
                String days    = (String)cmds.get ("EDIT-DAY");
                String months  = new Integer (getMonthNumber ((String)cmds.get ("EDIT-MONTH"))).toString ();
                String years   = (String)cmds.get ("EDIT-YEAR");
                String hours   = (String)cmds.get ("EDIT-HOUR");
                String minutes = (String)cmds.get ("EDIT-MINUTE");
		String seconds = (String)cmds.get ("EDIT-SECOND");
                String value   = hours + ":" + minutes + ":" + seconds + " " + days + "/" + months + "/" + years;

                try { // to use all separate data in create a complete date
                        int time = (int)(DateSupport.convertDateToLong (value) / 1000);

                        Date d = new Date ((long)time * 1000);
                        log.debug("FieldEdit -> Storing: " + d.toString () + " in field " + fieldname);

                        node.setValue (fieldname, new Integer (time));
                }
                catch (Exception e) {
                        e.printStackTrace ();
                        return (false);
                }

                return (true);

	}


	boolean setEditIMGField(EditState ed, String fieldname,Hashtable cmds,scanpage sp) {
		MMObjectBuilder obj=ed.getBuilder();
		FieldDefs def=obj.getField(fieldname);
		try {
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			byte[] bytes=sp.poster.getPostParameterBytes("file");
			node.setValue(fieldname,bytes);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return(true);
	}


	boolean setEditDISKField(EditState ed, String fieldname,Hashtable cmds,scanpage sp) {
		MMObjectBuilder obj=ed.getBuilder();
		FieldDefs def=obj.getField(fieldname);
		try {
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			String filename=(String)cmds.get("EDIT-SETFIELDFILE_DISK-"+fieldname);
			byte[] bytes=getFile(filename);
			if (bytes==null) {
				log.warn("FieldEditor-> Empty file !!");
			} else {
				node.setValue(fieldname,bytes);
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return(true);
	}


	int doCommit(EditState ed,String userName) {
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
				int id=node.getIntValue("number");
				if (id==-1) {
				//	log.debug("FieldEditor -> COMMIT ON A NEW OBJECT="+node);
					node.preEdit(ed);
					id=node.insert(userName);
					node.insertDone(ed);
					node.changed.removeAllElements();
				} else {
					node.preEdit(ed);
					node.commit();
				}
			
			
				if (node.getIntValue("snumber")!=-1 && node.getIntValue("dnumber")!=-1) {		
					EditStateNode prenode=ed.getEditStateNode(1);
					if (prenode!=null) {
						prenode.delRelationTable();
					}
				}

			return(id);
		}
		return(-1);
	}


 	private void replaceOwner(EditState ed,MMObjectNode node) {
 		// fix pop name to overide to username
 		if (node!=null) {
 
 			String ow=node.getStringValue("owner");
 			String userName=ed.getUser();
 			if (ow.equals("pop")) {
 				node.setValue("owner",userName);
 				log.debug("FieldEditor -> replaceOwner("+node.getValue("number")+","+userName+"): Replaced owner 'pop' with owner '"+userName+"'");
 			}	
 		}
 	}

	/**
	 * getFile: This method creates a byte array using the specified filepath argument.
	 */
	public byte[] getFile(String filepath){
		try {
			File file = new File(filepath);	
			FileInputStream fis = new FileInputStream(filepath);
			byte[] ba = new byte[(int)file.length()];	//Create a bytearray with a length the size of the filelength.
			fis.read(ba);	//Read up to ba.length bytes of data from this inputstream into the bytearray ba.
			fis.close();
			return(ba);
		}catch (IOException ioe){
			return(null);
		}
	}

}
