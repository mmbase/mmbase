/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

import java.util.*;
import java.io.File;
import java.sql.*;

import org.mmbase.util.*;
import org.mmbase.util.xml.BuilderWriter;
import org.mmbase.util.platform.setUser;
import org.mmbase.module.*;
import org.mmbase.module.builders.*;  // Vwms, VwmTasks, DayMarkers, Versions
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.database.JDBCInterface;
import org.mmbase.module.database.MultiConnection;
import org.mmbase.module.database.support.MMJdbc2NodeInterface;

import org.mmbase.security.MMBaseCop;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * The module which provides access to the MMBase database defined
 * by the provided name/setup.
 * It holds the overal object cloud made up of builders, objects and relations and
 * all the needed tools to use them.
 * @deprecation contains commented-out code
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @author Johan Verelst
 * @version $Id: MMBase.java,v 1.75 2002-10-10 12:04:05 eduard Exp $
 */
public class MMBase extends ProcessorModule  {

    /**
     * State of MMBase at the beginning of startup
     * @since MMBase-1.6
     */
    private static final int STATE_START_UP  = -1;
    /**
     * State of MMBase before builders are loaded
     * @since MMBase-1.6
     */
    private static final int STATE_LOAD       = 0;
    /**
     * State of MMBase before builders are initialized
     * @since MMBase-1.6
     */
    private static final int STATE_INITIALIZE = 1;
    /**
     * State of MMBase after startup is completed
     * @since MMBase-1.6
     */
    private static final int STATE_UP         = 2;

    // logging
    private static Logger log = Logging.getLoggerInstance(MMBase.class.getName());

    /**
     * Defines what 'channel' we are talking to when using multicast.
     * @scope private, non-static
     */
    public static String multicasthost = null;

    /**
     * Determines on what port does this multicast talking between nodes take place.
     * This can be set to any port but check if something else on
     * your network is allready using multicast when you have problems.
     * @scope private, non-static
     */
    public static int multicastport=-1;

    /**
     * Builds a MultiCast Thread to receive and send
     * changes from other MMBase Servers.
     * @scope private
     */
    public MMBaseChangeInterface mmc;

    /**
     * Base name for the database to be accessed using this instance of MMBase.
     * Retrieved from the mmbaseroot module configuration setUser
     * If not specified the default is "def1"
     * Should be made private and accessed instead using getBaseName()
     * @scope private
     */
    public String baseName="def1";

    /**
     * Reference to the TypeDef builder.
     * Should be made private and accessed instead using getTypeDef()
     * @scope private
     */
    public TypeDef TypeDef;
    /**
     * Reference to the RelDef builder.
     * Should be made private and accessed instead using getRelDef()
     * @scope private
     */
    public RelDef RelDef;
    /**
     * Reference to the OALias builder.
     * Should be made private and accessed instead using getOAlias()
     * @scope private
     */
    public OAlias OAlias;
    /**
     * Reference to the InsRel builder.
     * Should be made private and accessed instead using getInsRel()
     * @scope private
     */
    public InsRel InsRel;
    /**
     * Reference to the TypeRel builder.
     * Should be made private and accessed instead using getTypeRel()
     * @scope private
     */
    public TypeRel TypeRel;

    /**
     * The table that contains all loaded builders. Includes virtual builders such as "multirelations".
     * Should be made private and accessed using getMMObjects()
     * @scope private
     */
    public Hashtable mmobjs=new Hashtable();

    /**
     * The (base)path to the builder configuration files
     * @scope private
     */
    String builderpath = "";

    /**
     * @deprecated-now unused
     * @scope private
     */
    int delay;

    /**
     * @deprecated-now unused
     * @scope private
     */
    boolean	nodecachesdone=false;

    /**
     * A thread object that gets activated by MMbase.
     * It activates every X seconds and takes this signal to call all the
     * builders probeCalls, using the callback {@link #doProbeRun} method in MMBase.
     * @scope private
     */
    MMBaseProbe probe;

    /**
     * A reference to the jdbc driver to use for the current database system.
     * JDBC drivers differ by sytsem (various database systems provide their own drivers).
     * The configuration for the jdbc class to use for your datanse system is set in the configuration files.
     * @scope private
     */
    JDBCInterface jdbc;

    /**
     * MultiRelations virtual builder, used for performing multilevel searches.
     * @scope private
     */
    MultiRelations MultiRelations;

    /**
     * The database to use. Retrieve using getDatabase();
     * @scope private
     */
    MMJdbc2NodeInterface database;

    /**
     * Name of the machine used in the mmbase cluster.
     * it is used for the mmservers objects. Make sure that this is different
     * for each node in your cluster. This is not the machines dns name
     * (as defined by host as name or ip number).
     * @scope private
     */
    String machineName="unknown";

    /**
     * The host or ip number of the machine this module is
     * running on. Its important that this name is set correctly because it is
     * used for communication between mmbase nodes and external devices
     * @scope private
     */
    String host="unknown";

    /**
     * Authorisation type. Access using getAuthType()
     * @scope private
     */
    String authtype="none";

    /**
     * Cookie domain (?). Access using getCookieDomain()
     * @scope private
     */
    String cookieDomain=null;

    /**
     * Reference to the Root builder (the most basic builder, aka 'object').
     * This can be null (does not exist) in older systems
     */
    private MMObjectBuilder root;

    /**
     * Base url for the location of the DTDs. obtained using getDTDBase()
     * @deprecated
     */
    private String dtdbase="http://www.mmbase.org";

    /**
     * our securityManager (MMBaseCop)
     */
    private MMBaseCop mmbaseCop = null;

    /**
     * Reference to the cluster builder.
     * The cluster builder is a version of the multirelations builder
     * that is used by the MMCI.
     */
    private ClusterBuilder clusterBuilder;

    /**
     * Reference to the sendmail module. Accessible using getSendMail();
     */
    private SendMailInterface sendmail;

    /**
     * Currently used locale. Access using getLanguage()
     */
    private Locale locale = Locale.ENGLISH;

    /**
     * Currently used encoding. Access using getEncoding(). This
     * default to ISO-8859-1 as long as support for other encodings is
     * not thoroughly tested. In the feature we will probably switch
     * to UTF-8.
     *
     * @since MMBase-1.6
     */
    private String encoding = "ISO-8859-1";

    /**
     * MMbase 'up state. Access using getState()
     *
     * @since MMBase-1.6
     */
    private int mmbasestate = STATE_START_UP;

    /**
     * The table that indexes builders being loaded.
     * This map does not actually contian builders - it merely contains
     * a reference that the builder is in the process of being loaded.
     * The map is used to prevent circular references when extending builders.
     *
     * @since MMBase-1.6
     */
    private Map loading = new HashMap();

    /**
     * Constructor to create the MMBase root module.
     */
    public MMBase() {
        log.debug("MMBase constructed");
    }

    /**
     * Initializes the MMBase module. Evaluates the parameters loaded from the configuration file.
     * Sets parameters (authorisation, langauge), loads the builders, and starts MultiCasting.
     */
    public void init() {

        log.service("Init of " + org.mmbase.Version.get());
        // is there a basename defined in MMBASE.properties ?
        String tmp = getInitParameter("BASENAME");
        if (tmp != null) {
            // yes then replace the default name (def1)
            baseName = tmp;
        } else {
            log.info("init(): No name defined for mmbase using default (def1)");
        }

        tmp=getInitParameter("AUTHTYPE");
        if (tmp!=null && !tmp.equals("")) {
            authtype=tmp;
        }

        tmp=getInitParameter("LANGUAGE");
        if (tmp!=null && !tmp.equals("")) {
            locale = new Locale(tmp, "");
        }

        tmp = getInitParameter("ENCODING");
        if (tmp != null && !tmp.equals("")) {
            encoding = tmp;
        }

        tmp=getInitParameter("AUTH401URL");
        if (tmp!=null && !tmp.equals("")) {
            HttpAuth.setLocalCheckUrl(tmp);
        }
        tmp=getInitParameter("DTDBASE");
        if (tmp!=null && !tmp.equals("")) {
            dtdbase=tmp;
        }

        tmp=getInitParameter("HOST");
        if (tmp!=null && !tmp.equals("")) {
            host=tmp;
        }

        tmp=getInitParameter("MULTICASTPORT");
        if (tmp!=null && !tmp.equals("")) {
            try {
                multicastport=Integer.parseInt(tmp);
            } catch(Exception e) {}
        }

        tmp=getInitParameter("MULTICASTHOST");
        if (tmp!=null && !tmp.equals("")) {
            multicasthost=tmp;
        }

        tmp=getInitParameter("COOKIEDOMAIN");
        if (tmp!=null && !tmp.equals("")) {
            cookieDomain=tmp;
        }

        sendmail=(SendMailInterface)getModule("sendmail");
        machineName=getInitParameter("MACHINENAME");

        jdbc= (JDBCInterface) getModule("JDBC");
        

        if (multicasthost != null) {
            mmc = new MMBaseMultiCast(this);
        } else {
            mmc = new MMBaseChangeDummy(this);
        }

        builderpath = getInitParameter("BUILDERFILE");
        if (builderpath==null || builderpath.equals("")) {
            builderpath=MMBaseContext.getConfigPath() + File.separator + "builders" + File.separator;
        }

        mmbasestate=STATE_LOAD;

        if (!checkMMBase()) {
            // there is no base defined yet, create the core objects
            createMMBase();
        }

        log.debug("Init builders:");
        initBuilders();

        mmbasestate=STATE_INITIALIZE;

        log.debug("Objects started");

        // weird place needs to rethink (daniel).
        Vwms bul=(Vwms)getMMObject("vwms");
        if (bul!=null) {
            bul.startVwms();
        }
        Vwmtasks bul2=(Vwmtasks)getMMObject("vwmtasks");
        if (bul2!=null) {
            bul2.start();
        }

        String writerpath=getInitParameter("XMLBUILDERWRITERDIR");
        if (writerpath!=null && !writerpath.equals("")) {
            Enumeration t = mmobjs.elements();
            while (t.hasMoreElements()) {
                MMObjectBuilder fbul=(MMObjectBuilder)t.nextElement();
                if (!fbul.isVirtual()) {
                    String name=fbul.getTableName();
                    log.debug("WRITING BUILDER FILE ="+writerpath+File.separator+name);
                    try {
                        BuilderWriter builderOut=new BuilderWriter(fbul);
                        builderOut.setIncludeComments(false);
                        builderOut.setExpandBuilder(false);
                        builderOut.writeToFile(writerpath+File.separator+fbul.getTableName()+".xml");
                    } catch (Exception ex) {
                        log.error(Logging.stackTrace(ex));
                    }
                }
            }
        }

        // try to load security...
        try {
            mmbaseCop = new
                MMBaseCop(MMBaseContext.getConfigPath() +
                          File.separator + "security" + File.separator + "security.xml");
        } catch(Exception e) {
            log.fatal("error loading the mmbase cop: " + e.toString());
            log.error(Logging.stackTrace(e));
            log.error("MMBase will continue without security.");
            log.error("All future security invocations will fail.");
        }

        // signal that MMBase is up and running
        mmbasestate=STATE_UP;
        log.info("MMBase is up and running");
        checkUserLevel();
    }

    /**
     * Started when the module is loaded.
     * @deprecated-now unused
     */
    public void onload() {
    }

    /**
     * @deprecated-now unused
     */
    public void unload() {
    }

    /**
     * @deprecated-now unused
     */
    public void shutdown() {
        Logging.shutdown();
        log.service("Shutting down MMBase");
    }

    /**
     * Checks whether the database to be used exists.
     * The system determines whether the object table exists
     * for the baseName provided in the configuration file.
     * @return <code>true</code> if the database exists and is accessible, <code>false</code> otherwise.
     */
    boolean checkMMBase() {
        return getDatabase().created(baseName+"_object");
    }

    /**
     * Create a new database.
     * The database created is based on the baseName provided in the configuration file.
     * This call automatically creates an object table.
     * The fields in the table are either specified in an object builder xml,
     * or from a default setup existing of a number field and a owner field.
     * Note: If specified, the object builder is instantiated and its table created, but
     * the builder is not registered in the TypeDef builder, as this builder does not exist yet.
     * Registration happens when the other builders are registered.
     * @return <code>true</code> if the database was succesfully created, otherwise a runtime exception is thrown
     *   (shouldn't it return <code>false</code> instead?)
     */
    boolean createMMBase() {
        log.debug(" creating new multimedia base : "+baseName);
        Vector v;

	// why are we giving our member variable it's own value here?
        // database=getDatabase();
	getDatabase();

        MMObjectBuilder objekt=null;
        try {
            objekt=loadBuilder("object");
        } catch (BuilderConfigurationException e) {
            // object builder was not defined -
            // builde ris optional, so this is not an error
        }
        if (objekt!=null) {
            objekt.init();
        } else {
            database.createObjectTable(baseName);
        }
        return true;
    }

    /**
     * Determines whether a builder is in the process of being loaded,
     * but not yet finished. Needed to track down circular references.
     * @return true if the builder is being loaded
     *
     * @since MMBase-1.6
     */
    private boolean builderLoading(String name) {
        return loading.get(name)!=null;
    }

    /**
     * Retrieves a specified builder.
     * If the builder is not loaded, but the system is in the 'startup'  state
     * (i.e. it is in the process of loading builders), an attempt is made to
     * directly load the builder.
     * This allows for dependencies between builders to exist (i.e. inheritance).
     * When circular reference occurs between two loading buidlers, an exception is thrown.
     *
     * @since MMBase-1.6
     * @param name The name of the builder to retrieve
     * @return a <code>MMObjectBuilder</code> if found, <code>null</code> otherwise
     * @throws CircularReferenceException when circular reference is detected
     */
    public MMObjectBuilder getBuilder(String name) throws CircularReferenceException {
        MMObjectBuilder builder=getMMObject(name);
        if (builder==null && (mmbasestate==STATE_LOAD)) {
            if (builderLoading(name)) {
                throw new CircularReferenceException("Circular reference to builder with name "+name);
            }
            builder=loadBuilder(name);
        }
        return builder;
    }

    /**
     * Retrieves a specified builder.
     * Note: may get deprecated in the future - use getBuilder instead.
     * @param name The name of the builder to retrieve
     * @return a <code>MMObjectBuilder</code> if found, <code>null</code> otherwise
     */
    public MMObjectBuilder getMMObject(String name) {
        Object o=mmobjs.get(name);
        if (o == null) {
            log.trace("MMObject " + name + " could not be found"); // can happen...
        }
        return (MMObjectBuilder)o;
    }

    /**
     * Retrieves the MMBase module('mmbaseroot').
     * @return the active MMBase module
     */
    static public MMBase getMMBase() {
        return (MMBase) getModule("mmbaseroot");
    }

    /**
     * Retrieves the loaded security manager(MMBaseCop).
     * @return the loaded security manager(MMBaseCop)
     */
    public MMBaseCop getMMBaseCop() {
        return mmbaseCop;
    }

    /**
     * Retrieves the loaded builders.
     * @return an <code>Enumeration</code> listing the loaded builders
     */
    public Enumeration getMMObjects() {
        return mmobjs.elements();
    }

    /**
     * Returns a reference to the InsRel builder.
     * @return the <code>InsRel</code> builder if defined, <code>null</code> otherwise
     */
    public InsRel getInsRel() {
        return InsRel;
    }

    /**
     * Returns a reference to the RelDef builder.
     * @return the <code>RelDef</code> builder if defined, <code>null</code> otherwise
     */
    public RelDef getRelDef() {
        return RelDef;
    }

    /**
     * Returns a reference to the TypeDef builder.
     * @return the <code>TypeDef</code> builder if defined, <code>null</code> otherwise
     */
    public TypeDef getTypeDef() {
        return TypeDef;
    }

    /**
     * Returns a reference to the TypeRel builder.
     * @return the <code>TypeRel</code> builder if defined, <code>null</code> otherwise
     */
    public TypeRel getTypeRel() {
        return TypeRel;
    }

    /**
     * Returns a reference to the OAlias builder.
     * @return the <code>OAlias</code> builder if defined, <code>null</code> otherwise
     */
    public OAlias getOAlias() {
        return OAlias;
    }

    /**
     * Returns a reference to the Object builder.
     * The Object builder is the builder from which all other builders eventually extend.
     * @return the <code>Object</code> builder if defined, <code>null</code> otherwise
     * @since MMBase1,6
     */
    public MMObjectBuilder getRootBuilder() {
        return root;
    }

    /**
     * Returns the otype of the Object builder, or -1 if it is not known.
     * The Object builder is the builder from which all other builders eventually extend.
     * @since MMBase1,6
     */
    public int getRootType() {
        if (root==null) {
            return -1;
        } else {
            return root.oType;
        }
    }

    /**
     * Returns a reference to the cluster builder.
     * @return an instantiation of the <code>ClusterBuilder</code>
     */
    public ClusterBuilder getClusterBuilder() {
        return clusterBuilder;
    }


    /**
     * Get the JDBC module used by this MMBase.
     */
    public JDBCInterface getJDBC() {
        return jdbc;
    }

    /**
     * Safely close a database connection and/or a database statement.
     * @param con The connection to close. Can be <code>null</code>.
     * @param stmt The statement to close, prior to closing the connection. Can be <code>null</code>.
     */
    public void closeConnection(MultiConnection con, Statement stmt) {
	try {
	    if (stmt!=null) stmt.close();
	} 
	catch(Exception g) {
	}
	try {
	    if (con!=null) con.close();
	} 
	catch(Exception g) {
	}
    }


    /**
     * Get a database connection that is multiplexed and checked.
     * @return a <code>MultiConnection</code> 
     */
    public MultiConnection getConnection() {
	MultiConnection con = null;
        int timeout = 10; //seconds
	
        int tries = 1;
	// always return a connection, maybe database down,... so wait in that situation....
	while(con == null) {
	    try {
		con=database.getConnection(jdbc);
	    } 
	    catch (SQLException sqle){
		log.fatal("Could not get a multi-connection, will try again over " + timeout + " seconds: " + sqle.getMessage());
                if (tries == 1) {
                    log.error(Logging.stackTrace(sqle));
                } else {
                    log.debug(Logging.stackTrace(sqle));
                }
                tries++;
		try {
		    wait(timeout * 1000);
		}
		catch(InterruptedException ie) {
		    String msg = "Wait for connection was canceled:" + Logging.stackTrace(ie);
		    log.warn(msg);
		    throw new RuntimeException(msg);
		}
	    }
	}
	return con;
    }

    /**
     * Get a direct database connection.
     * Should only be used if you want to do database specific things that use non-jdbc
     * interface calls. Use very sparingly.
     */
    public Connection getDirectConnection() {
	Connection con = null;
	int timeout = 10;
	
        int tries = 1;
	// always return a connection, maybe database down,... so wait in that situation....
	while(con == null) {
	    try {
		con=jdbc.getDirectConnection(jdbc.makeUrl());
	    } catch (SQLException sqle){
		log.fatal("Could not get a connection, will try again after " + timeout + " seconds: " + sqle.getMessage());
		if (tries ==1) {
                    log.error(Logging.stackTrace(sqle));
                } else {
                    log.debug(Logging.stackTrace(sqle));
                }
		try {
		    wait(timeout * 1000);
		} catch(InterruptedException ie) {
		    String msg = "Wait for connection was canceled:" + Logging.stackTrace(ie);
		    log.warn(msg);
		    throw new RuntimeException(msg);
		}
	    }
	}
	return con;
    }

    /**
     * Retrieves the database base name
     * @return the base name as a <code>String</code>
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Callback method, called from MMBaseProbe.
     * The probe is a seperate thread that is created every 10 minutes by the module's
     * {@link #maintainance} call.
     * @performance daymarks should be loaded at initialization.
     * @todo Evaluate whether presence of daymarks is required
     */
    public void doProbeRun() {
        DayMarkers bul=(DayMarkers)getMMObject("daymarks");
        if (bul!=null) {
            bul.probe();
        } else {
            log.error("Can't access builder : daymarks");
        }
    }

    /**
     * Performs periodic maintenance.
     * Starts a separate thread that probes the builders by calling {@link #doProbeRun}.
     * The reference to the thread is cleared when it dies (scehduled every 10 minutes), prompting
     * the system to start a new thread.
     * @deprecation contains commented-out code
     * @see MMBaseProbe
     */
    public void maintainance() {
        if (probe==null) probe=new MMBaseProbe(this);

        //LStreams.checkBroadcastState(LStreams.getNode(573949));
        if (2==1 && mmc!=null) {
            // debug for in/out multicast check
            // log.debug("maintenance(): in="+mmc.incount+" out="+mmc.outcount+" spawn="+mmc.spawncount);
        }
    }


    /**
     * Converts a vector containing nodes to a hashmap,
     * using a specified (unique) integer field as the hash key.
     * @param se The vector containing the nodes
     * @param mapper the name of the (integer) field that determines the hash key, i.e. "number"
     * @return the node list mapped to a <code>Hashtable</code>
     */
    public Hashtable getSearchHash(Vector se,String mapper) {
        Hashtable results=new Hashtable();
        Enumeration t = se.elements();
        MMObjectNode node;
        while (t.hasMoreElements()) {
            node=(MMObjectNode)t.nextElement();
            results.put(new Integer(node.getIntValue(mapper)),node);
        }
        return results;
    }

    /**
     * Converts a vector containing nodes to a comma seperated list of values,
     * obtained from a specified integer field.
     * @param se The vector containing the nodes
     * @param mapper the name of the (integer) field whose value to include in the list
     * @return a parenthised, comma-seperated list of values, as a <code>String</code>
     */
    public String getWhereList(Vector se,String mapper) {
        if (se==null) return null;
        StringBuffer inlist = new StringBuffer();
        inlist.append(" (");
        Enumeration t = se.elements();
        MMObjectNode node;
        while (t.hasMoreElements()) {
            node=(MMObjectNode)t.nextElement();
            inlist.append(node.getIntValue(mapper) + ",");
        }
        if (inlist.length() >= 1 ) inlist.setLength(inlist.length()-1);
        inlist.append( ") ");
        return inlist.toString();
    }

    /**
     * Retrieves a reference to the sendmail module.
     * @return a <code>SendMailInterface</code> object if the module was loaded, <code>null</code> otherwise.
     */
    public SendMailInterface getSendMail() {
        return sendmail;
    }

    /**
     * Retrieves the machine name.
     * This value is set using the configuration file.
     * @return the machine name as a <code>String</code>
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * Retrieves the host name or ip number
     * This value is set using the configuration file.
     * @return the host name as a <code>String</code>
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the cookiedomain (whatever that is)
     * This value is set using the configuration file.
     * @return the cookie domain as a <code>String</code>
     */
    public String getCookieDomain() {
        return cookieDomain;
    }

    /**
     * Adds a remote observer to a specified builder.
     * The observer is notified whenever an object of that builder is changed, added, or removed.
     * @return <code>true</code> if adding the observer succeeded, <code>false</code> otherwise.
     */
    public boolean addRemoteObserver(String type,MMBaseObserver obs) {
        MMObjectBuilder bul=getMMObject(type);
        if (bul!=null) {
            return bul.addRemoteObserver(obs);
        } else {
            log.error("addRemoteObserver(): ERROR: Can't find builder : "+type);
            return false;
        }
    }

    /**
     * Adds a local observer to a specified builder.
     * The observer is notified whenever an object of that builder is changed, added, or removed.
     * @return <code>true</code> if adding the observer succeeded, <code>false</code> otherwise.
     */
    public boolean addLocalObserver(String type,MMBaseObserver obs) {
        MMObjectBuilder bul=getMMObject(type);
        if (bul!=null) {
            return bul.addLocalObserver(obs);
        } else {
            log.error("addLocalObserver(): ERROR: Can't find builder : "+type);
            return false;
        }
    }

    /**
     * Returns the number of marked days from a specified daycount (?)
     * @deprecated SCAN related, should not be in this module.
     */
    public String doGetAgeMarker(StringTokenizer tok) {
        if (tok.hasMoreTokens()) {
            String age=tok.nextToken();
            try {
                int agenr=Integer.parseInt(age);
                int agecount=((DayMarkers)getMMObject("daymarks")).getDayCountAge(agenr);
                return ""+agecount;
            } catch (Exception e) {
                log.debug(" Not a valid AGE");
                return "No valid age given";
            }
        } else {
            return "No age given";
        }
    }

    /**
     * Retrieves an unique key to use for a new node's number.
     * Calls the database to request the key. <code>Sychronized</code> so the same number cannot be dealt out to different nodes.
     * Does possibly not work well with multiple mmbase systems that work on the same database.
     * @return the new unique key as an <code>int</code> value
     */
    public synchronized int getDBKey() {
        return database.getDBKey();
    }

    /**
     * Removes functions from fieldnames.
     * private, never called. should be removed.
     */
    private Vector removeFunctions(Vector fields) {
        Vector results=new Vector();
        Enumeration f=fields.elements();
        for (;f.hasMoreElements();) {
            // hack hack this is way silly Strip needs to be fixed
            String fieldname=Strip.DoubleQuote((String)f.nextElement(),Strip.BOTH);
            int pos1=fieldname.indexOf('(');
            if (pos1!=-1) {
                int pos2=fieldname.indexOf(')');
                results.addElement(fieldname.substring(pos1+1,pos2));
            } else {
                results.addElement(fieldname);
            }
        }
        return results;
    }

    /**
     * Retrieves a (mmbase) module by name.
     * @return the module as an <code>Object</code> if it exists, <code>null</code> otherwise
     * @deprecated-now Use {@link #getModule} instead
     */
    public Object getBaseModule(String name) {
        return getModule(name);
    }

    /**
     * @deprecated-now not used
     */
    public void stop()
    {
    }

    /**
     * Loads a core Builder.
     * If the builder  does not exist, an exception is thrown.
     * @since MMBase-1.6
     * @param name the name of the builder to load
     * @return the builder
     * @throws BuilderConfigurationException if the builder config file does not exist or is inactive
     */
    private MMObjectBuilder loadCoreBuilder(String name) {
        MMObjectBuilder builder=loadBuilder(name);
        if (builder==null) {
            throw new BuilderConfigurationException("The core builder "+name+" is mandatory but inactive.");
        } else {
            return builder;
        }
    }

    /**
     * Initializes the builders, using the builder xml files in the config directory
     * @return Always <code>true</code>
     */
    boolean initBuilders() {

        // first load the builders
        TypeDef=(TypeDef)loadCoreBuilder("typedef");
        TypeDef.init();

        RelDef=(RelDef)loadCoreBuilder("reldef");
        RelDef.init();

        TypeRel=(TypeRel)loadCoreBuilder("typerel");
        TypeRel.init();

        InsRel=(InsRel)loadCoreBuilder("insrel");
        InsRel.init();

        try {
            OAlias=(OAlias)loadBuilder("oalias");
        } catch (BuilderConfigurationException e) {
            // OALias  builder was not defined -
            // builder is optional, so this is not an error
        }

        // new code checks all the *.xml files in builder dir, recursively
        String path = "";
        loadBuilders(path);

        // determine Object builder
        // Note: can be null
        root=getMMObject("object");

        log.debug("Starting MultiRelations Builder");
        MultiRelations = new MultiRelations(this);

        log.debug("Starting Cluster Builder");
        clusterBuilder = new ClusterBuilder(this);

        // first initialize versions, if available
        log.debug("Versions:");
        Versions versions=(Versions)getMMObject("versions");
        if (versions!=null) {
            versions.init();
        }

        log.debug("mmobjects, inits");
        Enumeration t = mmobjs.elements();
        while (t.hasMoreElements()) {
            MMObjectBuilder fbul=(MMObjectBuilder)t.nextElement();
            log.debug("init " + fbul);
            initBuilder(fbul);
        }

        log.debug("**** end of initBuilders");
        return true;
    }


    /**
     * inits a builder
     * @param builder The builder which has to be initialized
     */
    public void initBuilder(MMObjectBuilder builder) {
        if (!builder.isVirtual()) {
            builder.init();
            TypeDef.loadTypeDef(builder.getTableName());
            Versions versions = (Versions) getMMObject("versions");
            if (versions!=null) {
                checkBuilderVersion(builder.getTableName(),versions);
            }
        }
    }

    /**
     * Unloads a builders from MMBase. After this, the builder is gone
     * @param builder the builder which has to be unloaded
     */
    public void unloadBuilder(MMObjectBuilder builder) {
        if(mmobjs.remove(builder.getTableName()) == null) {
            String msg = "builder with name: " + builder.getTableName() + " could not be unloaded, since it was not loaded.";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        if (!builder.isVirtual()) {
            TypeDef.unloadTypeDef(builder.getTableName());
            log.info("unloaded builder with name:" + builder.getTableName());
        }
        else {
            log.info("unloaded virtual builder with name:" + builder.getTableName());
        }
    }

    /**
     * Loads all builders within a given path relative to the main builder config path, including builders in sub-paths
     * @param ipath the path to start searching. The path need be closed with a File.seperator character.
     */
    void loadBuilders(String ipath) {
        String path = builderpath + ipath;
        // new code checks all the *.xml files in builder dir
        File bdir = new File(path);
        if (bdir.isDirectory()) {
            String files[] = bdir.list();
            if (files!=null) {
                for (int i=0;i<files.length;i++) {
                    String bname=files[i];
                    if (bname.endsWith(".xml")) {
            bname=bname.substring(0,bname.length()-4);
            loadBuilderFromXML(bname,ipath);
                    } else {
            loadBuilders(ipath +  bname + File.separator);
                    }
                }
            } else {
                log.error("Cannot find modules in "+path);
            }
        }
    }

    /**
     * Locate one specific builder withing the main builder config path, including sub-paths.
     * If the builder already exists, the existing object is returned instead.
     * If the builder cannot be found in this path, a BuilderConfigurationException is thrown.
     * @since MMBase-1.6
     * @param builder name of the builder to initialize
     * @return the initialized builder object, or null if no builder could be created..
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    MMObjectBuilder loadBuilder(String builder) {
        return loadBuilder(builder,"");
    }

    /**
     * Locate one specific builder within a given path, relative to the main builder config path, including sub-paths.
     * Return the actual path.
     * @param builder name of the builder to find
     * @param ipath the path to start searching. The path need be closed with a File.seperator character.
     * @return the file path to the builder xml, or null if no builder could be found.
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    public String getBuilderPath(String builder, String path) {
        if ((new File(builderpath+path+builder+".xml")).exists()) {
            return path;
        } else {
            // not in the builders path, so we need to search recursively
            File dirList = new File(builderpath + path);
            String[] files = dirList.list();
            if (files!=null) {
                for (int i=0; i<files.length;i++) {
                    String lPath = path + files[i] + File.separator;
                    if ((new File(builderpath + lPath)).isDirectory()) {
                        String resultpath = getBuilderPath(builder, lPath);
                        if (resultpath!=null) {
                            return resultpath;
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Locate one specific builder within a given path, relative to the main builder config path, including sub-paths.
     * If the builder already exists, the existing object is returned instead.
     * @param builder name of the builder to initialize
     * @param ipath the path to start searching. The path need be closed with a File.seperator character.
     * @return the initialized builder object, or null if no builder could be created..
     * @throws BuilderConfigurationException if the builder config file does not exist
     */
    MMObjectBuilder loadBuilder(String builder, String ipath) {
        MMObjectBuilder bul=getMMObject(builder);
        if (bul != null) {
            log.debug("Builder '" + builder + "' is already loaded");
            return bul;
        }
        String path = getBuilderPath(builder, ipath);
        if (path != null) {
            return loadBuilderFromXML(builder, path);
        } else {
            log.error("Cannot find specified builder " + builder);
            throw new BuilderConfigurationException("Cannot find specified builder " + builder);
        }
    }


    /**
     * Create a new builder object using a xml configfile located in a given path relative to the main builder config path,
     * and return the builder object.
     * If the builder already exists, the existing object is returned instead.
     * Note that the builder's init() method is NOT called (since some builders need other builders in memory when their init() is called,
     * this method is called seperately after all builders are loaded).
     * @deprecation uses deprecated buidedr methods, contains commented-out code
     * @param builder name of the builder to initialize
     * @param ipath the path to start searching. The path need be closed with a File.seperator character.
     * @return the loaded builder object.
     */
    public MMObjectBuilder loadBuilderFromXML(String builder, String ipath) {
        MMObjectBuilder bul=getMMObject(builder);
        if (bul != null) {
            log.debug("Builder '"+builder+"' is already loaded");
            return bul;
        }

        String path = builderpath + ipath;
        String objectname=builder; // should this allow override in file ?
        try {
            // register the loading of this builder
            loading.put(objectname,"TRUE");
            XMLBuilderReader parser=new XMLBuilderReader(path+builder+".xml", this);
            String status=parser.getStatus();
            if (status.equals("active")) {
                String classname=parser.getClassFile();
                log.info("Starting builder : "+objectname);
                Class newclass=Class.forName(classname);

                bul = (MMObjectBuilder)newclass.newInstance();
                bul.setXMLPath(ipath);
                bul.setMMBase(this);
                bul.setTableName(objectname);

                // register the parent builder, if applicable
                MMObjectBuilder parent=parser.getParentBuilder();
                if (parent!=null) {
                    bul.setParentBuilder(parent);
                }

                Hashtable descriptions=parser.getDescriptions();
                bul.setDescriptions(descriptions);
                String desc=(String)descriptions.get(locale.getLanguage());
                // XXX" set description by builder?
                bul.setDescription(desc);
                bul.setSingularNames(parser.getSingularNames());
                bul.setPluralNames(parser.getPluralNames());
                bul.setVersion(parser.getBuilderVersion());
                bul.setMaintainer(parser.getBuilderMaintainer());
                bul.setSearchAge(""+parser.getSearchAge());
                bul.setInitParameters(parser.getProperties());
                bul.setXMLValues(parser.getFieldDefs()); // temp  ?

                mmobjs.put(objectname,bul);

                // oke set the huge hack for insert layout
                // XXX: setDBLayout is deprecated
                //bul.setDBLayout(fields);

            }
        } catch (Exception e) {
            loading.remove(objectname);
            log.error(Logging.stackTrace(e));
            return null;
        }
        loading.remove(objectname);
        return bul;
    }

    /**
     * Retrieves the DTD base url.
     * This value is set using the configuration file.
     * @deprecated keesj: This method is not used. Document type definitions
     * should contain a fully qualified url.
     * I think the author was thinking of a dtdpath wich makes sence
     * @return the dtd base as a <code>String</code>
     */
    public String getDTDBase() {
        return dtdbase;
    }

    /**
     * Returns a reference to the database used.
     * If needed, it loads the appropriate support class using the configuration parameters.
     * @return a <code>MMJdbc2NodeInterface</code> which holds the appropriate database class
     */
    public MMJdbc2NodeInterface getDatabase() {
        if (database==null) {
	    File databaseConfig = null;
	    String databaseConfigDir = MMBaseContext.getConfigPath()+ File.separator + "databases" + File.separator;
	    String databasename = getInitParameter("DATABASE");
	    if(databasename == null){
		DatabaseLookup lookup = new DatabaseLookup(new File(databaseConfigDir + "lookup.xml"), new File(databaseConfigDir));
		if(jdbc == null) throw new RuntimeException("Could not retrieve jdbc module, is it loaded?");
		// How do we know for sure that the JDBC.init() has been called first?
		// the only way is by using the startModule, (little bit scary to use here)	       
                Module m = (Module) jdbc;
                if(!m.hasStarted()) {
                    // STUPID
		    log.error("jdbc module not started, trying to do an explicit start of jdbc module");
                    m.startModule();
                }
		try {
		    // dont use the getDirectConnection, upon failure, it will loop,....
		    databaseConfig = lookup.getDatabaseConfig(jdbc.getDirectConnection(jdbc.makeUrl()));
		}
		catch(java.sql.SQLException sqle) {
		    log.error(sqle);
		    log.error(Logging.stackTrace(sqle));
		    throw new RuntimeException("error retrieving an connection to the database:" + sqle);
		}
	    }
	    else {
		// use the correct databas-xml
		databaseConfig = new File(databaseConfigDir + databasename + ".xml");
	    }
	    // get our config...
	    XMLDatabaseReader dbdriver = new XMLDatabaseReader(databaseConfig.getPath());

	    try {                
                Class newclass = Class.forName(dbdriver.getMMBaseDatabaseDriver());
                database = (MMJdbc2NodeInterface) newclass.newInstance();
	    }
	    catch(ClassNotFoundException cnfe){
	        String msg = "class not found:\n" + Logging.stackTrace(cnfe);
		log.error(msg);
		throw new RuntimeException(msg);
	    }
	    catch(InstantiationException ie) {
	        String msg = "error instanciating class:\n" + Logging.stackTrace(ie);
		log.error(msg);
		throw new RuntimeException(msg);
	    }
	    catch(IllegalAccessException iae) {
	        String msg = "illegal acces on class:\n" + Logging.stackTrace(iae);
		log.error(msg);
		throw new RuntimeException(msg);
	    }
	    // print information about our database connection..	    
	    log.info("Using class: '"+database.getClass().getName()+"' with config: '"+databaseConfig+"'." );
	    // init the database..
	    database.init(this, dbdriver);
        }
        return database;
    }


    /**
     * Loads a Node again, using its 'right' parent.
     * Reloading may retrieve extra fields if the original node was not loaded accurately.
     * @deprecated Not necessary in most cases, with the possible exception of lists obtained from InsRel.
     *   However, in the later case using this method is probably too costly.
     */
    public MMObjectNode castNode(MMObjectNode node) {
    /* fake because solved
     */
    int otype=node.getOType();
    String ename=TypeDef.getValue(otype);
    if (ename==null) return null;
    MMObjectBuilder res=getMMObject(ename);
    MMObjectNode node2=res.getNode(node.getNumber());
    return node2;
    //return node;
    }

    /**
     * Retrieves the autorisation type.
     * This value is set using the configuration file.
     * Examples are 'none' or 'basic'.
     * @return a <code>String</code> identifying the type
     */
    public String getAuthType() {
        return authtype;
    }

    /**
     * Retrieves the current language.
     * This value is set using the configuration file.
     * Examples are 'us' or 'nl'.
     * @return the language as a <code>String</code>
     */
    public String getLanguage() {
        return locale.getLanguage();
    }

    /**
     * Retrieves the encoding.
     * This value is set using the configuration file.
     * Examples are 'UTF-8' (default) or 'ISO-8859-1'.
     *
     * @return the coding as a <code>String</code>
     * @since  MMBase-1.6
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Retrieves whether this mmbase module is running.
     * @return <code>true</code> if the module has been initialized and all builders loaded, <code>false</code> otherwise.
     */
    public boolean getState() {
        return mmbasestate==STATE_UP;
    }

    /**
     * Checks and switches the user/grouplevel in which MMBase runs.
     * The userlevel is set using the -Dmmbase:userlevel=user:group commandline parameter.
     * Should probably be changed to <code>private</code>.
     */
    public void checkUserLevel() {
        String level=System.getProperty("mmbase.userlevel");
        if (level!=null) {
            log.info("CheckUserLevel ->  mmmbase.userlevel="+System.getProperty("mmbase.userlevel"));
            int pos=level.indexOf(':');
            if (pos!=-1) {
                String user=level.substring(0,pos);
                String group=level.substring(pos+1);
                setUser setuser=new setUser();
                setuser.setUserGroup(user,group);
            } else {
                log.info("CheckUserLevel ->  mmmbase.userlevel= not defined as user:group");
            }
        }
    }

    /**
     * Checks the builder version and, if needed, updates the version table.
     * Queries the xml files instead of the builder itself (?)
     * @return always <code>true</code>.
     */
    private boolean checkBuilderVersion(String buildername,Versions ver) {

        MMObjectBuilder tmp = (MMObjectBuilder)mmobjs.get(buildername);
        String builderfile = builderpath + tmp.getXMLPath() + buildername + ".xml";
        XMLBuilderReader bapp=new XMLBuilderReader(builderfile,this);
        if (bapp!=null) {
            int version=bapp.getBuilderVersion();
            String maintainer=bapp.getBuilderMaintainer();
            int installedversion=ver.getInstalledVersion(buildername,"builder");
            if (installedversion==-1 || version>installedversion) {
                if (installedversion==-1) {
                    ver.setInstalledVersion(buildername,"builder",maintainer,version);
                } else {
                    ver.updateInstalledVersion(buildername,"builder",maintainer,version);
                }
            }
        }
        return true;
    }

}
