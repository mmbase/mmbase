/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/*
$Id: g2encoders.java,v 1.6 2000-11-09 12:03:57 vpro Exp $

$Log: not supported by cvs2svn $
Revision 1.5  2000/03/30 13:11:35  wwwtech
Rico: added license

Revision 1.4  2000/03/29 10:59:26  wwwtech
Rob: Licenses changed

Revision 1.3  2000/03/21 15:39:18  wwwtech
- (marcel) Removed debug (globally declared in MMOBjectNode)

Revision 1.2  2000/02/24 15:07:16  wwwtech
Davzev added debug() methods and calls to all methods.


*/

package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

/**
 * @author Daniel Ockeloen
 * @$Revision: 1.6 $ $Date: 2000-11-09 12:03:57 $
 */
public class g2encoders extends ServiceBuilder implements MMBaseObserver {

	private String classname = getClass().getName();
	private boolean debug = true;
	//  private void debug(String msg){System.out.println(classname+":"+msg);}

	public g2encoders() {
	}

	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		boolean result = false;
		if( debug ) debug("nodeRemoteChanged("+number+","+builder+","+ctype+")");

		super.nodeRemoteChanged(number,builder,ctype);
		result = nodeChanged(number,builder,ctype);

		if( debug ) debug("nodeRemoteChanged("+number+","+builder+","+ctype+"): return("+result+")");
		return result;
	}

	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		boolean result = false;
		if( debug ) debug("nodeLocalChanged("+number+","+builder+","+ctype+")");

		super.nodeLocalChanged(number,builder,ctype);
		result = nodeChanged(number,builder,ctype);

		if( debug ) debug("nodeLocalChanged("+number+","+builder+","+ctype+"): returning("+result+")");
		return result;
	}

	public boolean nodeChanged(String number,String builder,String ctype) {
		boolean result = true;
		if( debug ) debug("nodeLocalChanged("+number+","+builder+","+ctype+"), do nothing, return("+result+")");
		return(result);
	}
}
