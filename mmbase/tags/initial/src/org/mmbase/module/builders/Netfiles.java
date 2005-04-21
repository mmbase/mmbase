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
 * @author Daniel Ockeloen
 * @version 12 Mar 1997
 */
public class Netfiles extends MMObjectBuilder {

	NetFileSrv netfilesrv;

	public String getGUIIndicator(String field,MMObjectNode node) {
		if (field.equals("status")) {
			int val=node.getIntValue("status");
			switch(val) {
				case 1: return("Verzoek");
				case 2: return("Onderweg");
				case 3: return("Gedaan");
				case 4: return("Aangepast");
				case 5: return("CalcPage");
				default: return("Onbepaald");
			}
		}
		return(null);
	}

	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		super.nodeRemoteChanged(number,builder,ctype);
		//System.out.println("CHANGE="+mmb.getMachineName());
		if (mmb.getMachineName().equals("twohigh")) {
		System.out.println("Netfiles-> Change : "+number+" "+builder+" "+ctype);
		if (netfilesrv==null) {
			netfilesrv=(NetFileSrv)mmb.getMMObject("netfilesrv");
			if (netfilesrv!=null) netfilesrv.fileChange(number,ctype);
		} else {
			netfilesrv.fileChange(number,ctype);
		}
		}
		return(true);
	}

	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		super.nodeLocalChanged(number,builder,ctype);
		if (mmb.getMachineName().equals("twohigh")) {
		System.out.println("Netfiles-> Change : "+number+" "+builder+" "+ctype);
		if (netfilesrv==null) {
			netfilesrv=(NetFileSrv)mmb.getMMObject("netfilesrv");
			if (netfilesrv!=null) netfilesrv.fileChange(number,ctype);
		} else {
			netfilesrv.fileChange(number,ctype);
		}
		}
		return(true);
	}


}
