/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/*
	$Id: JDBC.java,v 1.15 2000-12-30 14:00:59 daniel Exp $

	$Log: not supported by cvs2svn $
	Revision 1.14  2000/12/28 11:37:54  pierre
	pierre: added some debug code
	
	Revision 1.13  2000/07/22 10:52:59  daniel
	Removed some debug
	
	Revision 1.12  2000/06/25 13:09:15  wwwtech
	Daniel.. changed/added method for getConnection per database driver
	
	Revision 1.11  2000/04/30 15:31:48  wwwtech
	Rico: robustified the JDBC.makeUrl code
	
	Revision 1.10  2000/04/25 21:30:47  wwwtech
	daniel: fixed a bug that forgot to return sets with 1 value
	
	Revision 1.9  2000/03/31 13:33:18  wwwtech
	Wilbert: Introduction of ParseException for method getList
	
	Revision 1.8  2000/03/30 13:11:44  wwwtech
	Rico: added license
	
	Revision 1.7  2000/03/29 10:45:02  wwwtech
	Rob: Licenses changed
	
	Revision 1.6  2000/03/06 22:47:25  wwwtech
	Rico: fixed shim reference
	
	Revision 1.5  2000/03/01 16:28:46  wwwtech
	Rico: fixed bug forgetting to init databasesupport
	
	Revision 1.4  2000/02/25 14:06:37  wwwtech
	Rico: added database specific connection init support
	
*/
package org.mmbase.module.database;

import java.util.*;
import java.sql.*;
import java.util.*;

import javax.servlet.http.*;

import org.mmbase.util.*;
import org.mmbase.module.*;

/**
 * JDBC Module the module that provides you access to the loaded JDBC interfaces
 * we use this as the base to get multiplexes/pooled JDBC connects.
 *
 * @see org.mmbase.module.servlets.JDBCServlet
 * @version $Id: JDBC.java,v 1.15 2000-12-30 14:00:59 daniel Exp $
 */
public class JDBC extends ProcessorModule implements JDBCInterface {

 	private String classname = getClass().getName();
 	private boolean debug = false;
 	private void debug( String msg ) { System.out.println( classname+":"+msg ); }

Class  classdriver;
Driver driver;
String JDBCdriver;
String JDBCurl;
String JDBChost;
int JDBCport;
int maxConnections;
int maxQuerys;
String JDBCdatabase;
String databasesupportclass;
DatabaseSupport databasesupport;
MultiPoolHandler poolHandler;
JDBCProbe probe=null;
private String defaultname;
private String defaultpassword;

	public void onload() {
		getprops();
		getdriver();
		loadsupport();
		poolHandler=new MultiPoolHandler(databasesupport,maxConnections,maxQuerys);
	}

	/*
	 * Initialize the properties and get the driver used
	 */
	public void init() {
		// old
		getprops();
		probe=new JDBCProbe(this);
	}

	/**
	 * Reload the properties and driver
	 */
	public void reload() {
		getprops();

		/* This doesn't work, have to figure out why
		try {
			DriverManager.deregisterDriver(driver);
		} catch (SQLException e) {
			debug("reload(): JDBC Module: Can't deregister driver");
		}
		*/
		loadsupport();
		getdriver();
	}

	public void unload() {
	}
	public void shutdown() {
	}

	/**
	 * Get the driver as specified in our properties
	 */
	private void getdriver() {
		Driver d;

		driver=null;
		try {
			classdriver=Class.forName(JDBCdriver);
			if (debug) debug("getDriver(): Loaded load class : "+JDBCdriver);
		} catch (ClassNotFoundException e) {
			debug("getDriver(): Can't load class : "+JDBCdriver);
		}
		/* Also get the instance to unload it later */
		for (Enumeration e=DriverManager.getDrivers();e.hasMoreElements();) {
			d=(Driver)e.nextElement();
			//debug("Driver "+d);
			if (classdriver==d.getClass()) {
				driver=d;
				break;
			}
		}
		if (driver==null) {
			debug("getDriver(): Can't get driver from DriverManager");
		}
	}

	/**
	 * Get the driver as specified in our properties
	 */
	private void loadsupport() {
		Class cl;

		try {
			cl=Class.forName(databasesupportclass);
			databasesupport=(DatabaseSupport)cl.newInstance();
			databasesupport.init();
			if (debug) debug("loadsupport(): Loaded load class : "+databasesupportclass);
		} catch (Exception e) {
			debug("loadsupport(): Can't load class : "+databasesupportclass+" : "+e);
		}
	}

	/**
	 * Get the properties
	 */
	private void getprops() {

		JDBCdriver=getInitParameter("driver");
		JDBCurl=getInitParameter("url");
		JDBChost=getInitParameter("host");
		defaultname=getInitParameter("user");
		defaultpassword=getInitParameter("password");
		databasesupportclass=getInitParameter("supportclass");
	
		if (debug) {	
			debug("JDBCdriver="+JDBCdriver);
			debug("JDBCurl="+JDBCurl);
			debug("JDBChost="+JDBChost);
			debug("defaultname="+defaultname);
			debug("defaultpassword="+defaultpassword);
			debug("databasesupportclass="+databasesupportclass);
		}
		
		if (defaultname==null) {
			defaultname="wwwtech";
		}
		if (defaultpassword==null) {
			defaultpassword="xxxxxx";
		}
		try {
			JDBCport=Integer.parseInt(getInitParameter("port"));
		} catch (NumberFormatException e) {
			JDBCport=0;
		}
		try {
			maxConnections=Integer.parseInt(getInitParameter("connections"));
		} catch (Exception e) {
			maxConnections=8;
		}
		try {
			maxQuerys=Integer.parseInt(getInitParameter("querys"));
		} catch (Exception e) {
			maxQuerys=500;
		}
		JDBCdatabase=getInitParameter("database");
		if (databasesupportclass==null || databasesupportclass.length()==0) {
			databasesupportclass="org.mmbase.module.database.DatabaseSupportShim";
		}
	}

	/**
	 * Routine build the url to give to the DriverManager
	 * to open the connection. This way a servlet/module
	 * doesn't need to care about what database it talks to.
	 * @see java.sql.DriverManager.getConnection
	 */
	public String makeUrl() {
		return(makeUrl(JDBChost,JDBCport,JDBCdatabase));
	}

	/**
	 * Routine build the url to give to the DriverManager
	 * to open the connection. This way a servlet/module
	 * doesn't need to care about what database it talks to.
	 * @see java.sql.DriverManager.getConnection
	 */
	public String makeUrl(String dbm) {
		return(makeUrl(JDBChost,JDBCport,dbm));
	}

	/**
	 * Routine build the url to give to the DriverManager
	 * to open the connection. This way a servlet/module
	 * doesn't need to care about what database it talks to.
	 * @see java.sql.DriverManager.getConnection
	 */
	public String makeUrl(String host,String dbm) {
		return(makeUrl(host,JDBCport,dbm));
	}

	/**
	 * Routine build the url to give to the DriverManager
	 * to open the connection. This way a servlet/module
	 * doesn't need to care about what database it talks to.
	 * @see java.sql.DriverManager.getConnection
	 */
	public String makeUrl(String host,int port,String dbm) {
		String pre,post;
		int pos;
		String end=new String(JDBCurl);
		// $HOST $DBM $PORT

		pos=end.indexOf("$DBM");
		if (pos!=-1) {
			pre=end.substring(0,pos);
			post=end.substring(pos+4);
			end=pre+dbm+post;
		} else {
			debug("Warning: database name is static, can't select other databases within this databaseserver");
		}
		pos=end.indexOf("$HOST");
		if (pos!=-1) {
			pre=end.substring(0,pos);
			post=end.substring(pos+5);
			end=pre+host+post;
		}
		pos=end.indexOf("$PORT");
		if (pos!=-1) {
			pre=end.substring(0,pos);
			post=end.substring(pos+5);
			end=pre+port+post;
		}
		return(end);
	}

	public MultiConnection getConnection(String url, String name, String password) throws SQLException {
		return(poolHandler.getConnection(url,name,password));
	}

	public MultiConnection getConnection(String url) throws SQLException {
		return(poolHandler.getConnection(url,defaultname,defaultpassword));
	}

	public Connection getDirectConnection(String url,String name,String password) throws SQLException {

		return(DriverManager.getConnection(url,name,password));
	}

	public Connection getDirectConnection(String url) throws SQLException {

		return(DriverManager.getConnection(url,defaultname,defaultpassword));
	}

	public synchronized void checkTime() {
		try {
			if (poolHandler!=null) poolHandler.checkTime();
		} catch(Exception e) {
			debug("checkTime(): Exception");
			e.printStackTrace();
		}
	}

	/*
	 * User interface stuff
	 */

	public Vector getList(HttpServletRequest requestInfo,StringTagger tagger, String value) throws ParseException {
    	String line = Strip.DoubleQuote(value,Strip.BOTH);
		StringTokenizer tok = new StringTokenizer(line,"-\n\r");
		if (tok.hasMoreTokens()) {
			String cmd=tok.nextToken();	
			if (cmd.equals("POOLS")) return(listPools(tagger));
			if (cmd.equals("CONNECTIONS")) return(listConnections(tagger));
		}
		return(null);
	}

	public Vector listPools(StringTagger tagger) {
		Vector results=new Vector();
		int i;
		for (Enumeration e=poolHandler.keys();e.hasMoreElements();) {
			String name=(String)e.nextElement();
			MultiPool pool=poolHandler.get(name);
			i=name.indexOf(',');
			if (i!=-1) {
				results.addElement(name.substring(0,i));
			} else {
				results.addElement(name);
			}
			results.addElement(""+pool.getSize());
			results.addElement(""+pool.getTotalConnectionsCreated());
		}
		tagger.setValue("ITEMS","3");
		return(results);
	}


	public Vector listConnections(StringTagger tagger) {
		int i;
		Vector results=new Vector();
		for (Enumeration e=poolHandler.keys();e.hasMoreElements();) {
			String name=(String)e.nextElement();
			MultiPool pool=poolHandler.get(name);
			for (Enumeration f=pool.busyelements();f.hasMoreElements();) {
				MultiConnection realcon=(MultiConnection)f.nextElement();
				i=name.indexOf(',');
				if (i!=-1) {
					results.addElement(name.substring(name.lastIndexOf('/')+1,i));
				} else {
					results.addElement(name.substring(name.lastIndexOf('/')+1));
				}
				results.addElement(realcon.toString());
				results.addElement(""+realcon.getLastSQL());
				results.addElement(""+realcon.getUsage());
				//results.addElement(""+pool.getStatementsCreated(realcon));
			}
			for (Enumeration f=pool.elements();f.hasMoreElements();) {
				MultiConnection realcon=(MultiConnection)f.nextElement();
				i=name.indexOf(',');
				if (i!=-1) {
					results.addElement(name.substring(name.lastIndexOf('/')+1,i));
				} else {
					results.addElement(name.substring(name.lastIndexOf('/')+1));
				}
				results.addElement(realcon.toString());
				results.addElement("&nbsp;");
				results.addElement(""+realcon.getUsage());
				//results.addElement(""+pool.getStatementsCreated(realcon));
			}
		}
		tagger.setValue("ITEMS","4");
		return(results);
	}

	public String getUser() {
		return(defaultname);
	}

	public String getPassword() {
		return(defaultpassword);
	}
	
	public String getDatabaseName() {
		return(getInitParameter("database"));
	}
}
