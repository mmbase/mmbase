/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.gui.html;

import java.util.*;
import java.sql.*;
import org.mmbase.module.core.*;


/**
 * EditState, controls the users edit session, keeps EditStatesNodes
 * (hitlisted)
 *
 *
 * @author Daniel Ockeloen
 * @author Hans Speijer
 */
public class EditStateNode {

	private String classname = getClass().getName();	
	private boolean debug	  = false;
	private void debug( String msg ){ System.out.println( classname +":"+ msg ); }

	private static boolean removeRelations=false; // remover relations on node delete

	Hashtable searchValues = new Hashtable();
	Hashtable htmlValues = new Hashtable();
	String editNode;
	String editor;
	String dutchEditor;
	String selectionQuery="";
	MMObjectBuilder mmObjectBuilder;
	MMObjectNode node;
	MMBase mmBase;
	int nodeEditPos=0;
	boolean insSave=false;
	Vector insSaveList=new Vector();
	
	public EditStateNode(MMBase mmBase) {
		this.mmBase=mmBase;
	}

	public boolean setSearchValue(String fieldname,Object value) {
		searchValues.put(fieldname,value);
		return(true);
	}

	public String getSearchValue(String name) {
		String result = null;

		if( name != null )
			if( !name.equals("") )
				result = ((String)searchValues.get(name));
			else
				debug("getSearchValue("+name+"): ERROR: name is empty!");
		else
			debug("getSearchValue("+name+"): ERROR: name is null!");

		return result;
	}

	public Hashtable getSearchValues() {
		return (searchValues);
	}

	public boolean isChanged() {
		if (node.isChanged() || insSaveList.size()>0) {
			return(true);
		} else {
			return(false);
		}
	}

	public void clearSearchValues() {
		searchValues=new Hashtable();
	}


	public boolean setHtmlValue(String fieldname,Object value) {
		if( fieldname != null )
		{
			if(!fieldname.equals(""))	
			{
				htmlValues.put(fieldname,value);
			}
			else
				debug("setHtmlValue("+fieldname+","+value+"): ERROR: fieldname is empty!");
		}
		else
			debug("setHtmlValue("+fieldname+","+value+"): ERROR: fieldname is null!");

		return(true);
	}

	public String getHtmlValue(String name) {
		return((String)htmlValues.get(name));
	}

	public Hashtable getHtmlValues() {
		return (htmlValues);
	}

	public void clearHtmlValues() {
		htmlValues=new Hashtable();
	}

	public void setEditNode(String number,String userName) {
		node = mmObjectBuilder.getNode(number);
		editNode = number;
	}


	public void getNewNode(String owner) {
		node = mmObjectBuilder.getNewNode(owner);
		editNode="-1";
		delRelationTable();
		//editNode = "-1"; // signal its a new node
	}

	public void removeNode() {
		mmObjectBuilder.removeNode(node);
	}

	public void removeRelations() {
		if (removeRelations) {
			mmObjectBuilder.removeRelations(node);
		}
	}

	public MMObjectNode getEditNode() {
		return(node);
	}

	public MMObjectNode getEditSrcNode() {
		int snum=node.getIntValue("snumber");
		if (snum!=-1) {
			MMObjectNode rnode=mmObjectBuilder.getNode(snum);
			return(rnode);
		} else {
			return(null);
		}
	}


	public MMObjectNode getEditDstNode() {
		int dnum=node.getIntValue("dnumber");
		if (dnum!=-1) {
			MMObjectNode rnode=mmObjectBuilder.getNode(dnum);
			return(rnode);
		} else {
			return(null);
		}
	}



	public int getEditNodeNumber() {
		try {
			int i=Integer.parseInt(editNode);
			return(i);
		} catch(Exception e) {
		}
		return(-1);
	}

	public void setBuilder(String name) {
		if( name != null )
			if( mmBase != null )
			{
				mmObjectBuilder = mmBase.getMMObject(name);
				if( mmObjectBuilder != null )
					dutchEditor= mmObjectBuilder.getDutchSName();
				else
					debug("setBuilder("+name+"): ERROR: No MMObjectBuilder found with this name!");
				editor = name;
			}
			else
				debug("setBuilder("+name+"): ERROR: MMBase is not defined!");	
		else
			debug("setBuilder("+name+"): ERROR: Name is not valid!");	
	}

	public String getBuilderName() {
		return (editor);
	}

	public String getDutchBuilderName() {
		return (dutchEditor);
	}

	public MMObjectBuilder getBuilder() {
		return (mmObjectBuilder);
	}

	public void setSelectionQuery(String query) {
		selectionQuery = query;
	}

	public String getSelectionQuery() {
		return (selectionQuery);
	}

	public boolean getInsSave() {
		return (insSave);
	}

	public void setInsSave(boolean set) {
		insSave=set;
	}

	public void setInsSaveNode(MMObjectNode node) {
		insSaveList.addElement(node);
		debug("setInsSaveNode(): "+insSaveList.toString());
	}

	public Vector getInsSaveList() {
		return(insSaveList);
	}

	public void delInsSaveList() {
		insSaveList=new Vector();
	}

	public void delRelationTable() {
		if (debug) {
			debug("delRelationTable(): Del on relation table, here not implemented!");
		}
	}

	/**
	 * Returns Hashtable with the currently linked items sorted by relation 
	 * type.
	 */
	public Hashtable getRelationTable() {
		Enumeration enum = mmBase.getTypeRel().getAllowedRelations(node);
		MMObjectNode typeRel;
		String typeName;

		// build Hashtable with Vectors for all allowed relations 
		// Key = TypeName for objects that may be linked

		Hashtable relationTable = new Hashtable();
		while (enum.hasMoreElements()) {
			typeRel = (MMObjectNode)enum.nextElement();	
			int j=-1;
			if (typeRel.getIntValue("snumber") == node.getIntValue("otype")) { 
				j=typeRel.getIntValue("dnumber");
			} else {
				j=typeRel.getIntValue("snumber");
			}
			if (j!=-1) {
				typeName = mmBase.getTypeDef().getValue(j);
				relationTable.put(typeName,new Vector());
			} else {
				debug("getRelationTable(): Problem on "+typeRel.toString());
			}
				
		}

		// Hashtable is done now fill it up !!
		// enumeration contains all objectnodes that are linked to the 
		// currently edited Node.

		if (getEditNodeNumber()!=-1) {

			// is this the correct way to get Relations ???? my vote is no !
			// enum = mmBase.getInsRel().getRelations(getEditNodeNumber());
			
			enum = node.getRelations();
	
			MMObjectNode rel;
			MMObjectNode target;
	
			while (enum.hasMoreElements()) {
				try {
					rel = (MMObjectNode)enum.nextElement();	
					if (rel.getIntValue("snumber") == node.getIntValue("number")) 
						target = mmObjectBuilder.getNode(rel.getIntValue("dnumber"));
					else 
						target = mmObjectBuilder.getNode(rel.getIntValue("snumber"));
					typeName = mmBase.getTypeDef().getValue(target.getIntValue("otype"));
					Vector relList = (Vector)relationTable.get(typeName);
					if (relList != null) {
						relList.addElement(target);
						relList.addElement(rel);
					}
				} catch(Exception e) {
					debug("getRelationTable(): Problem with a relation, probably a relation with a non active builder");
				}
			}
		} else {
			if (debug) {
				debug("getRelation(): EditNodeNumber is -1");
			}
		}
		return (relationTable);
	}
}
