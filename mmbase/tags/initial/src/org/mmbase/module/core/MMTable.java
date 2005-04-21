/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/
package org.mmbase.module.core;

import java.util.*;
import java.sql.*;

import org.mmbase.module.*;
import org.mmbase.module.database.*;

/**
 * MMTable is the base abstraction of a database table, it provides a starting point
 * only with very basic functions as size and name of the table.
 *
 * @author Daniel Ockeloen
 * @version 12 Mar 1997
 */
public class MMTable {

	public MMBase mmb; 
	public String tableName;

	/**
	*
	*/
	public MMTable() {
	}

	public MMTable(MMBase m) {
		mmb=m; 
	}

	/**
	* return the number of relation types in this mmbase and table
	*/
	public int size() {
		try {
			MultiConnection con=mmb.getConnection();
			Statement stmt=con.createStatement();
			System.out.println("SELECT count(*) FROM "+mmb.getBaseName()+"_"+tableName+";");
			ResultSet rs=stmt.executeQuery("SELECT count(*) FROM "+mmb.getBaseName()+"_"+tableName+";");
			int i=-1;
			while(rs.next()) {
				i=rs.getInt(1);
			}	
			stmt.close();
			con.close();
			return i;
		} catch (Exception e) {
			return(-1);
		}
	}


	/**
	* return the number of relation types in this mmbase and table
	*/
	public boolean created() {
		if (size()==-1) {
			System.out.println("TABLE "+tableName+" NOT FOUND");
			return(false);
		} else {
			System.out.println("TABLE "+tableName+" FOUND");
			return(true);
		}
	}

}
