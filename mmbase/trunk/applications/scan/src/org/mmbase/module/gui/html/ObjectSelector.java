/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.gui.html;

import java.util.*;

import javax.servlet.http.*;

import org.mmbase.util.*;
import org.mmbase.module.core.*;
import org.mmbase.module.builders.*;
import org.mmbase.module.corebuilders.*;

/**
 * The ObjectSelector class offers the functionality to search for objects
 * and select found objects to be edited by the FieldSelector and FieldEditor
 * classes (hitlisted).
 * 
 * @author Daniel Ockeloen
 * @author Hans Speijer
 */
public class ObjectSelector implements CommandHandlerInterface {

	StateManager stateMngr;

	/**
	 * Constructor to setup reference to the StateManager.
	 */
	public ObjectSelector(StateManager manager) {
		stateMngr = manager;
	}

	/**
	 * General List pages coming from MMEdit.
	 */
	public Vector getList(scanpage sp, StringTagger args, StringTokenizer commands) {
		String token;
		String userName=HttpAuth.getRemoteUser(sp);

		EditState state = stateMngr.getEditState(userName);
		Vector result = new Vector();
		
		if (commands.hasMoreTokens()) {
			token = commands.nextToken();
			if (token.equals("GETOBJECTS")) {	
				args.setValue("ITEMS","3");
				return getObjectSelectionList(state);
			} else if (token.equals("GETOBJECTTITLES")) {	
				args.setValue("ITEMS","1");
				return getObjectSelectionTitles(state);
			} else if (token.equals("GETOBJECTFIELDS")) {	
				args.setValue("ITEMS","5");
				return getObjectFields(state);
			} else if (token.equals("GETOBJECTRELATIONS")) {	
				args.setValue("ITEMS","4");
				return getObjectRelations(state);
			} else if (token.equals("GETOBJECTRELATIONS2")) {	
				args.setValue("ITEMS","4");
				return getObjectRelations2(state);
			} else if (token.equals("GETOBJECTRELATIONS3")) {	
				args.setValue("ITEMS","7");
				return getObjectRelations3(state,args);
			}
		}

		result.addElement("List not defined (ObjectSelector)");
		return (result);
	}



	/**
	 *
	 */
	Vector getObjectFields(EditState ed) {
		Vector results=new Vector();
		MMObjectBuilder obj=ed.getBuilder();
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			FieldDefs def;
			String DBName,val;
			Object o;
			for (Enumeration h=obj.getSortedFields().elements();h.hasMoreElements();) 			{
				def=(FieldDefs)h.nextElement();
				DBName=def.getDBName();
				if (!DBName.equals("owner") && !DBName.equals("number") && !DBName.equals("otype")) {
					val=obj.getGUIIndicator(DBName,node);
					if (val==null) val = node.getValueAsString( DBName );
					else if (val.equals("null")) val="";
					results.addElement(DBName);
					results.addElement(val);
					if (val.length()>14 && val.indexOf("<")==-1) {
						results.addElement(val.substring(0,14)+"...");
					} else {
						results.addElement(val);
					}
					results.addElement(def.getGUIName());
					results.addElement(def.getGUIType());
				}
			}
		}
		return(results);
	}


	/**
	 */
	Vector getObjectRelations2(EditState ed) {
		Vector results=new Vector();
		MMObjectBuilder obj=ed.getBuilder();
		MMObjectNode node=ed.getEditNode();


		if (node!=null && node.getIntValue("number")!=-1) {
			FieldDefs def;
			String DBName,val;
			Object o;
			Enumeration e=stateMngr.mmBase.getInsRel().getRelations(ed.getEditNodeNumber());
			MMObjectNode rel;
			for (;e.hasMoreElements();) {
				rel=(MMObjectNode)e.nextElement();	
				MMObjectNode rdn=stateMngr.mmBase.getRelDef().getNode(rel.getIntValue("rnumber"));
				// am i the source of the desitination of the relation ?
				if (rel.getIntValue("snumber")==ed.getEditNodeNumber()) {
					MMObjectNode other=obj.getNode(rel.getIntValue("dnumber"));
					other=stateMngr.mmBase.castNode(other);
					other=stateMngr.mmBase.castNode(other);
					results.addElement(""+rel.getIntValue("number"));
					results.addElement(stateMngr.mmBase.getTypeDef().getValue(rel.getIntValue("otype")));
					results.addElement(stateMngr.mmBase.getTypeDef().getValue(other.getIntValue("otype")));
					results.addElement(""+other.getGUIIndicator());
				} else {
					MMObjectNode other=obj.getNode(rel.getIntValue("snumber"));
					other=stateMngr.mmBase.castNode(other);
					results.addElement(""+rel.getIntValue("number"));
					results.addElement(stateMngr.mmBase.getTypeDef().getValue(rel.getIntValue("otype")));
					results.addElement(stateMngr.mmBase.getTypeDef().getValue(other.getIntValue("otype")));
					results.addElement(""+other.getGUIIndicator());
				}
			}
		}
		return(results);
	}

	Vector getAllowedBuilders(String user) {
		Vector allowed=null;
		if (stateMngr.mmBase.getAuthType().equals("basic")) {
		allowed=new Vector();
		MultiRelations multirel=(MultiRelations)stateMngr.mmBase.getMMObject("multirelations");
		Vector tables=new Vector();
		tables.addElement("typedef");
		tables.addElement("authrel");
		tables.addElement("people");
		Vector fields=new Vector();
		fields.addElement("typedef.name");
		fields.addElement("people.account");
		Vector ordervec=new Vector();
		Vector dirvec=new Vector();
		dirvec.addElement("UP");

		Vector vec=multirel.searchMultiLevelVector(-1,fields,"NO",tables,"people.account=E'"+user+"'",ordervec,dirvec);
		for (Enumeration h=vec.elements();h.hasMoreElements();)	{
			MMObjectNode node=(MMObjectNode)h.nextElement();
			String builder=node.getStringValue("typedef.name");
			allowed.addElement(builder);
		}
		//System.out.println("TEST="+allowed);
		}
		return(allowed);
	}	

	/**
	 */
	Vector getObjectRelations3(EditState ed,StringTagger args) {
		Vector results=new Vector();
		MMObjectBuilder obj=ed.getBuilder();
		MMObjectNode node=ed.getEditNode();

		String user=args.Value("USER");
		//System.out.println("EDITOR USER="+user);
		Vector allowed=null;
		if (user!=null && !user.equals("")) {
			allowed=getAllowedBuilders(user);	
		}

		if (node!=null) {
			FieldDefs def;
			String DBName,val,name;
			Object o;
			Hashtable res=ed.getRelationTable();
			Enumeration e=res.keys();
			MMObjectNode rel;
			MMObjectNode other;
			Vector qw;
			for (;e.hasMoreElements();) {
				name=(String)e.nextElement();	
				//System.out.println("EDITOR="+name);
				if (allowed==null || allowed.contains(name)) {
					qw=(Vector)res.get(name);
					for (Enumeration h=qw.elements();h.hasMoreElements();) {
						other=(MMObjectNode)h.nextElement();
						rel=(MMObjectNode)h.nextElement();
						results.addElement(name);
						results.addElement(stateMngr.mmBase.getTypeDef().getValue(rel.getIntValue("otype")));
						results.addElement(""+rel.getIntValue("number"));
						results.addElement("");
						results.addElement(other.getGUIIndicator());
						results.addElement("insEditor");
						results.addElement(other.getDutchSName());
					}
					// the empty dummy
					results.addElement(name);
					results.addElement(name);
					results.addElement(""+node.getIntValue("number"));

					// startnewfix
					int reltype=stateMngr.mmBase.getTypeRel().getAllowedRelationType(stateMngr.mmBase.getTypeDef().getIntValue(name),node.getIntValue("otype"));
					if (reltype!=-1) {
						MMObjectNode rdn=stateMngr.mmBase.getRelDef().getNode(reltype);
						results.addElement(rdn.getStringValue("sname"));
					} else {
						results.addElement("multiple");
					}
					// end new fix


					results.addElement("");
					results.addElement("addEditor");
					results.addElement(stateMngr.mmBase.getTypeDef().getDutchSName(name));
				}
			}
		}
		return(results);
	}


	/**
	 */
	Vector getObjectRelations(EditState ed) {
		Vector results=new Vector();
		MMObjectBuilder obj=ed.getBuilder();
		MMObjectNode node=ed.getEditNode();
		if (node!=null) {
			FieldDefs def;
			String DBName,val;
			Object o;

			// find all the typeRel that are allowed

			Enumeration e=stateMngr.mmBase.getTypeRel().getAllowedRelations(node);
			MMObjectNode trn;
			MMObjectNode rdn;
			MMObjectNode tdn;
			for (;e.hasMoreElements();) {
				trn=(MMObjectNode)e.nextElement();	
				rdn=stateMngr.mmBase.getRelDef().getNode(trn.getIntValue("rnumber"));
				if (trn.getIntValue("snumber")==node.getIntValue("otype")) {
					tdn=stateMngr.mmBase.getTypeDef().getNode(trn.getIntValue("dnumber"));
					results.addElement(""+trn.getIntValue("snumber"));
					results.addElement(tdn.getStringValue("name"));
					results.addElement(rdn.getStringValue("sGUIName"));
					results.addElement(rdn.getStringValue("sname"));
				} else {
					tdn=stateMngr.mmBase.getTypeDef().getNode(trn.getIntValue("snumber"));
					results.addElement(""+trn.getIntValue("dnumber"));
					results.addElement(tdn.getStringValue("name"));
					results.addElement(rdn.getStringValue("dGUIName"));
					results.addElement(rdn.getStringValue("sname"));
				}	
			}
		}
		return(results);
	}


	/**
	 * Builds a list of HTML Table rows to display a table of found values.
	 * It gets the values out of the database using the selectionQuery of 
	 * the current EditState
	 * Item1 = Object ID for selectable object
	 * Item2 = HTML String met record waardes
	 */
	Vector getObjectSelectionList(EditState state) {
		Vector result = new Vector();
		String key,guival;
		MMObjectBuilder builder = state.getBuilder();
		MMObjectNode node;

		String conditions = state.getSelectionQuery();
		Enumeration searchResult;
	   
		String HTMLString = "";

		if (builder != null) {
			Vector vals = builder.getSortedListFields();
			//searchResult = builder.search(conditions); 
			//searchResult = builder.search(conditions,"number"); 
			searchResult = builder.search(conditions,"number",false); 
			
			while (searchResult.hasMoreElements()) {
				node = (MMObjectNode)searchResult.nextElement();	
				result.addElement(node.getValue("number").toString());
				HTMLString = "";
		
				for (Enumeration enum = vals.elements(); enum.hasMoreElements();) {
					key = ((FieldDefs)enum.nextElement()).getDBName();
					guival=builder.getGUIIndicator(key,node);
					if (guival!=null) {
					   	HTMLString += "<TD BGCOLOR=\"#FFFFFF\" WIDTH=\"500\"> "+guival+"&nbsp;</TD> ";
					} else {
					   		Object o=node.getValue(key);
							if (o==null || o.toString().equals("null")) {
					   			HTMLString += "<TD BGCOLOR=\"#FFFFFF\" WIDTH=\"500\">&nbsp;</TD> ";
							} else {
								// should be replaced soon mapping is weird
								if (o.toString().equals("$SHORTED")) {
									o=node.getStringValue(key);
								}			
					   			HTMLString += "<TD BGCOLOR=\"#FFFFFF\" WIDTH=\"500\"> "+o.toString()+"&nbsp; </TD> ";
							}
					}
				}
				result.addElement(HTMLString);
				result.addElement(""+vals.size());
			}
		}

		return (result);
	}	

	/**
	 * Builds a list of title strings containing the fields to be displayed
	 * Item1 = Name of the field (GUI Name)
	 */
	Vector getObjectSelectionTitles(EditState state) {
		Vector result = new Vector();
		MMObjectBuilder builder = state.getBuilder();
		Vector fieldDefs;
		
		if (builder != null) {
			fieldDefs = builder.getSortedListFields();
			for (Enumeration enum = fieldDefs.elements(); enum.hasMoreElements();) {
				result.addElement(((FieldDefs)enum.nextElement()).getGUIName());
			}
		}

 		return (result);
	}

	/**
	 * General proces pages coming from MMEdit.
	 */
	public boolean process(scanpage sp, StringTokenizer command, Hashtable cmds, Hashtable vars) {		
		return (false);
	}

	/**
	 * Sets the selection query for this user in this editor.
	 */
	boolean setObjectSelectionConditions(String user, Hashtable vars) {
		EditState state = stateMngr.getState(user);

		// Waardes uit de values lezen en met setQueryString() aan
		// de userstate geven

		return (true);
	}

	/**
	 * General replace/trigger pages coming from MMEdit.
	 */
	public String replace(scanpage sp, StringTokenizer cmds) {
		return ("Command not defined (ObjectSelector)");
		// bedoeld voor het clearen van de serachvalues
	}

	/**
	 * Clears the search fields for the searchfields
	 */
	void clearSearchFields(String user) {
	}

}




