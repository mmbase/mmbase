/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/

/*
	$Id: Properties.java,v 1.3 2000-03-24 14:34:00 wwwtech Exp $

	$Log: not supported by cvs2svn $
	Revision 1.2  2000/02/24 14:14:49  wwwtech
	Rico: debug changed
	
*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @version $Id: Properties.java,v 1.3 2000-03-24 14:34:00 wwwtech Exp $
 */
public class Properties extends MMObjectBuilder implements MMBaseObserver {

	private String classname = getClass().getName();
	private boolean debug = true;

	public String getGUIIndicator(MMObjectNode node) {
		String str=node.getStringValue("name");
		if (str.length()>15) {
			return(str.substring(0,12)+"...");
		} else {
			return(str);
		}
	}

	public String getGUIIndicator(String field,MMObjectNode node) {
		/*
		if (field.equals("storage")) {
			int val=node.getIntValue("storage");
			switch(val) {
				case 1: return("Muziek");
				case 2: return("Muziek geen backup");
				case 3: return("Spraak");
				case 4: return("Spraak geen backup");
				default: return("Onbepaald");
			}
		}
		*/
		return(null);
	}
	
	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		super.nodeRemoteChanged(number,builder,ctype);
		return(nodeChanged(number,builder,ctype));
	}

	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		super.nodeLocalChanged(number,builder,ctype);
		return(nodeChanged(number,builder,ctype));
	}

	
	/* Notifies memo:

	* passed ctype:

	* d: node deleted

	* c: node changed

	* n: new node

	* f: node field changed

	* r: node relation changed
	* x: some xml notify?

	*/

	public boolean nodeChanged(String number, String builder, String ctype) {
		if (builder.equals(tableName)) {
			if (debug) debug("nodeChanged(): Property change ! "+number+","+builder+","+ctype);
			if (ctype.equals("d")) {
				// Should zap node prop cache parent, but node already gone...
			}
/*
			 else if (ctype.equals("p")) {
				// The passed node number is node number of parent!
				if (isNodeCached(Integer.parseInt(number))) {
					MMObjectNode pnode=getNode(number);
					if (pnode!=null) {
						if (debug) debug("nodeChanged(): Zapping node prop cache for "+number);
						pnode.delPropertiesCache();
					}
				}
			}
*/
			    else if (ctype.equals("c") || ctype.equals("n") || ctype.equals("f")) { 
				// The passed node number is node of prop node
				MMObjectNode node=getNode(number);
				if (node!=null) {
					int parent=node.getIntValue("parent");
					if (isNodeCached(parent)) {
						if (debug) debug("nodeChanged(): Zapping node properties cache for "+parent);
						MMObjectNode pnode=getNode(parent);	
						if (pnode!=null) pnode.delPropertiesCache();
					}	
				}
			}
		}
		return(true);
	}
}
