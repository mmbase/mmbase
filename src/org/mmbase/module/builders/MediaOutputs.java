/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Rico Jansen
 * @version 28 Oct 1998
 */
public class MediaOutputs extends MMObjectBuilder {
public static final int ZILCH=0;
public static final int LWA=1; // Live Web Audio
public static final int LWV=2; // Live Web Video
public static final int AWA=3; // Archief Web Audio
public static final int AWV=4; // Archief Web Video
private MultiRelations multirel=null;
private Vector MTtables=new Vector();
private Vector MTfields=new Vector();
private String MTselectstring,MTtablestring,MTrelstring;
private String MTquery,MTmo,MTmi;


	/**
	* insert a new object, normally not used (only subtables are used)
	*/
	/*
	public int insert(String owner,MMObjectNode node) {
		int outputtype=node.getIntValue("outputtype");
		String machine=node.getStringValue("machine");
		String channel=node.getStringValue("channel");

		if (machine==null) machine="";
		if (channel==null) channel="";
		
		int number=getDBKey();
		if (number==-1) return(-1);
		try {
			MultiConnection con=mmb.getConnection();
			PreparedStatement stmt=con.prepareStatement("insert into "+mmb.baseName+"_"+tableName+" values(?,?,?,?,?,?)");
				stmt.setInt(1,number);
				stmt.setInt(2,oType);
				stmt.setString(3,owner);
				stmt.setInt(4,outputtype);
				stmt.setString(5,machine);
				stmt.setString(6,channel);
				stmt.executeUpdate();
				stmt.close();
				con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error on : "+number+" "+owner+" fake");
			return(-1);
		}
		signalNewObject(tableName,number);
		return(number);	
	}
	*/

	 public String getGUIIndicator(MMObjectNode node) {
        String str=node.getStringValue("channel");
        if (str.length()>15) {
            return(str.substring(0,12)+"...");
        } else {
            return(str);
        }
    }

	public String getGUIIndicator(String field,MMObjectNode node) {
		String rtn=null;
		if (field.equals("outputtype")) {
			int val=node.getIntValue("outputtype");
			switch(val) {
				case ZILCH: rtn="";
					break;
				case LWA: rtn="Live Web Audio";
					break;
				case LWV: rtn="Live Web Video";
					break;
				case AWA: rtn="Archief Web Audio";
					break;
				case AWV: rtn="Archief Web Video";
					break;
				default: 	rtn="Onbekend";
					break;
			}
		}
		return(rtn);
	}


	public Vector getMediaOutputs(int type,int medium,int channel) {
		String twhere,mwhere,cwhere;
		String query;
		Connection conn=null;
		Statement stmt=null;
		Vector res=null;

		if (multirel==null) multirel=multiinit();

		twhere=MTmo+".outputtype="+type;
		mwhere=MTmi+".medium="+medium;
		cwhere=MTmi+".channel="+channel;
		query=MTquery+" AND "+twhere+" AND "+mwhere+" AND "+cwhere;
		System.out.println("MediaOutputs -> query "+query);
		conn=mmb.getConnection();
		try {
			MMObjectNode node;
			int num;
			res=new Vector();

			stmt=conn.createStatement();
			ResultSet rs=stmt.executeQuery(query);
			while(rs.next()) {
				num=rs.getInt(1);
				node=getNode(num);
				if (node!=null) {
					res.addElement(node);
				} else {
					System.out.println("MediaOutputs -> Node "+num+" not found");
				}
			}
			stmt.close();
			conn.close();
		} catch (Exception e) {
			System.out.println("MediaOutputs -> Can't get MediaOutput");
			e.printStackTrace();
			res=null;
		}
		return(res);
	}

	private synchronized MultiRelations multiinit() {
		MultiRelations multi;
		multi=(MultiRelations)mmb.getMMObject("multirelations");
		System.out.println("MultiInit -> "+multi);
		if (multi==null) {
			System.out.println("MediaOutputs -> Didn't get MultiRelations");
		} else {
			// This doesn't change so only in init
			MTtables.addElement("mediain");
			MTtables.addElement("insrel");
			MTtables.addElement("mediaout");
			MTfields.addElement("mediaout.number");
			MTselectstring=multi.getSelectString(MTtables,MTfields);
			MTtablestring=multi.getTableString(MTtables);
			MTrelstring=multi.getRelationString(MTtables);
			MTquery="SELECT "+MTselectstring+" FROM "+MTtablestring+" WHERE "+MTrelstring;
			MTmo=""+multi.idx2char(MTtables.indexOf("mediaout"));
			MTmi=""+multi.idx2char(MTtables.indexOf("mediain"));
		}
		System.out.println("MultiInit -> done");
		return(multi);
	}
}
