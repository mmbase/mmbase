/* -*- tab-width: 4 -*- 

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mmbase.util.*;
import org.mmbase.module.core.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * Module , the wrapper for the modules.
 *
 * @author Rico Jansen
 * @author Rob Vermeulen (securitypart)
 *
 * @version $Revision: 1.28 $ $Date: 2001-04-09 14:42:25 $
 */
public abstract class Module {

    static Logger log = Logging.getLoggerInstance(Module.class.getName()); 

	Object SecurityObj;
	String moduleName=null;
	//protected final Hashtable state=new Hashtable();
	Hashtable state=new Hashtable();
	// org.mmbase private UsersInterface users;
	Hashtable mimetypes;

	static Hashtable modules;
	static String mmbaseconfig;
	
	Hashtable properties;
	static ModuleProbe mprobe;
	static boolean debug=false;
	private String className;
	String maintainer;
	int    version;	
		
	/**
 	 * variable to synchronize SecurityObj
 	 */
	private Object synobj=new Object();

	public Module() {
		String StartedAt=(new Date(DateSupport.currentTimeMillis())).toString();
		//String StartedAt=(new Date()).toString();
		state.put("Start Time",StartedAt);
		// org.mmbase mimetypes=Environment.getProperties(this,"mimetypes");

	}
	
	public final void setName(String name) {
		if (moduleName==null) {
			moduleName=name;
		}
	}

	/**
	 * Inits the module (startup final step 2).
	 * This is called second on startup, the module is expected
	 * to read the environment variables it needs. Startup threads,
	 * open connections etc.
	 */
	public abstract void init();

	public abstract void onload();

	/**
	 * state, returns the state hashtable that is/can be used to debug. Should
	 * be overridden when live state should be done.
	 */
	 public Hashtable state() {
		return(state);
	 }

    	public void setInitParameter(String key,String value) {
		if (properties!=null) {
			properties.put(key,value);
		}
	}
	/**
	 * Gets his init-parameters
 	 */
    	public String getInitParameter(String key) {
		if (properties!=null) {
			String value=(String)properties.get(key);
			if (value==null) {
				key=key.toLowerCase();
				value=(String)properties.get(key);
			}
			return(value);
		} else {
            log.error("getInitParameters(" + key + "): No properties found, called before they where loaded");
		}
		return(null);
	}


	/**
	 * Returns the properties to the subclass. 
	 */
   	protected Hashtable getProperties(String propertytable) {
		//String filename="/usr/local/vpro/james/adminopen/modules/";
		//return(results);		
		return(null);
		//return(Environment.getProperties(this,propertytable));
	}
	
	/**
	 * Returns one propertyvalue to the subclass.
	 */
	/* daniel, org.mmbase needs fix
	*/
	protected String getProperty(String name, String var) {
		//return(Environment.getProperty(this,name,var));
		return("");
	} 

	/* daniel, org.mmbase
   	protected Object removeProperty(String propertytable, String key) {
		return(Environment.removeProperty(this,propertytable,key));
	}
	*/
	
	/**
	 * Adds a property to the propertytabel 
	 */
	/* daniel, org.mmbase
	protected String putProperty(String propertytable, String key, String value) {
		return(String)(Environment.putProperty(this,propertytable,key,value));
	}
	*/
 
	/**
	 * Adds a property to the propertytabel 
	 */
	/* daniel, org.mmbase
	protected String putInitProperty(String key, String value) {
		return(String)(Environment.putProperty(this,"module/"+moduleName,key,value));
	} 
	*/

	/**
	 * Gets own modules properties
	 */
   	public Hashtable getInitParameters() {
		return(properties);
	}



	/**
 	 * Returns an iterator of all the modules that are currently active.
 	 * This function <code>null</code> if no attempt has the modules have (not) yet been to loaded.
 	 * Unlike {@link #getModule}, this method does not automatically load modules if this hadn't occurred yet.
	 * @return an <code>Iterator</code> with all active modules
 	 */
	public static final Iterator getModules() {
	    if (modules==null) {
	        return null;
	    } else {
	        return modules.values().iterator();
	    }
	}


	/**
	 *	Get user Module property
	 */
	/* daniel, org.mmbase
	protected final String getUserModuleProperty(String userName,String name,int type) {
		if (users!=null && userName!=null) {
			return(users.getModuleProperty(moduleName,userName,name,type));
		} else {
			return(null);
		}
	}
	*/

	/**
	 *	Get user Module property
	 */
	/* daniel, org.mmbase
	protected final String getUserModuleProperty(String userName,String name) {
		return(getUserModuleProperty(userName,name,1));
	}
	*/

	/**
	 *	Get user Module property
	 */
	/* daniel, org.mmbase
	protected final boolean setUserModuleProperty(String userName,String name,String value, int type) {
		if (users!=null && userName!=null) {
			return(users.setModuleProperty(moduleName,userName,name,value,type));
		} else {
			return(false);
		}
	}
	*/

	/**
	 *	Get user Module property
	 */
	/* daniel, org.mmbase
	protected final boolean setUserModuleProperty(String userName,String name,String value) {
		return(setUserModuleProperty(userName,name,value,1));
	}
	*/
	
	/** 
	 * getName
 	 */
	/* daniel, org.mmbase
	protected final String getName(Object asker) {
		return Environment.getName(asker);
	}
	*/


	/**
	 *  Returns the name of the module
	 * @return the module name
	 */
	public final String getName() {
		return moduleName; // org.mmbase
	}

	/** 
	 * provide some info on the module
 	 */
	 public String getModuleInfo() {
		return("No module info provided");
	 }

	/**
	* maintainance call called by the admin module every x seconds.
	*/
	public void maintainance() {
	}

    /**
    * getMimeType: Returns the mimetype using ServletContext.getServletContext which returns the servlet context
    * which is set when servscan is loaded.
    * Fixed on 22 December 1999 by daniel & davzev.
    * @param ext A String containing the extension.
    * @return The mimetype.
    */
    public String getMimeType(String ext) {
        return getMimeTypeFile("dummy."+ext);
    }

    public String getMimeTypeFile(String filename) {
        ServletContext sx=MMBaseContext.getServletContext();
        String mimetype=sx.getMimeType(filename);
        if (mimetype==null) {
            log.warn("getMimeType(" + filename + "): Can't find mimetype retval=null -> setting mimetype to default text/html");
            mimetype="text/html";
        }
        return(mimetype);
    }



	public static synchronized final void startModules() {
		// call the onload to get properties
		if( log.isDebugEnabled()) {
            log.debug("startModules(): onloading modules(" + modules + ")");
        }
		for (Enumeration e=modules.elements();e.hasMoreElements();) {
			Module mod=(Module)e.nextElement();
			if( log.isDebugEnabled() ) {
                log.debug("startModules(): modules.onload(" + mod + ")");
            }
			try {
				mod.onload();		
			} catch (Exception f) {
				log.warn("startModules(): modules(" + mod + ") not found to 'onload'!");
				f.printStackTrace();
			}
		}
		// so now really give em their init
		if (log.isDebugEnabled()) {
            log.debug("startModules(): init the modules(" + modules + ")");
        }
		for (Enumeration e=modules.elements();e.hasMoreElements();) {
			Module mod=(Module)e.nextElement();
			if ( log.isDebugEnabled()) {
                log.debug("startModules(): mod.init(" + mod + ")");
            }
			try {
				mod.init();		
			} catch (Exception f) {
				log.error("startModules(): module(" + mod + ") not found to 'init'!");
				f.printStackTrace();
			}
		}
	}

	public static Object getModule(String name) {
		// are the modules loaded yet ? if not load them


		if (modules==null) {
				modules=loadModulesFromDisk();
			if (log.isDebugEnabled()) {
                log.debug("getModule(" + name + "): Modules not loaded, loading them..");
            }
			startModules();
			// also start the maintaince thread that calls all modules every x seconds
			mprobe = new ModuleProbe(modules);
		}
		String orgname=name;
		name=name.toLowerCase();


		// try to obtain the ref to the wanted module
		Object obj=modules.get(name);	
		if (obj==null) obj=modules.get(orgname);
	
		if (obj!=null) {
			return(obj);
		} else {
			return(null);
		}
	}	

	public String getMaintainer() {
		return(maintainer);
	}

	public void setMaintainer(String m) {
		maintainer=m;
	}

	public void setVersion(int v) {
		version=v;
	}

	public int getVersion() {
		return(version);
	}


    /**
    * set classname of the builder
    */
    public void setClassName(String d) {
        this.className=d;
    }

    /**
    * return classname of this builder
    */
    public String getClassName() {
        return className;
    }


    public static synchronized Hashtable loadModulesFromDisk() {
        Hashtable results=new Hashtable();
        
        String dtmp=System.getProperty("mmbase.mode");
        if (dtmp!=null && dtmp.equals("demo")) {
            String curdir=System.getProperty("user.dir");
            if (curdir.endsWith("orion")) {
                curdir=curdir.substring(0,curdir.length()-6);
            }
            mmbaseconfig=curdir+"/config";
        } else {
            mmbaseconfig=System.getProperty("mmbase.config");
            if (mmbaseconfig == null) {
                log.error("mmbase.config not defined, use property (-D)mmbase.config=/my/config/dir/");
            }
        }
        MMBaseContext.setConfigPath(mmbaseconfig);

        String dirname=(mmbaseconfig+"/modules/");
        File bdir = new File(dirname);
        if (bdir.isDirectory()) {
            String files[] = bdir.list();
            for (int i=0;i<files.length;i++) {
                String bname=files[i];
                if (bname.endsWith(".xml")) {
                    bname=bname.substring(0,bname.length()-4);
                    XMLModuleReader parser=new XMLModuleReader(dirname+bname+".xml");
                    if (parser!=null) {		
                        if (parser.getStatus().equals("active")) {
                            String cname=parser.getClassFile();
                            // try starting the module and give it its properties
                            try {
                                Class newclass=Class.forName(cname);
                                Object mod = newclass.newInstance();
                                if (mod!=null) {
                                    results.put(bname,mod);
                                    Hashtable modprops = parser.getProperties();
                                    if (modprops!=null) {
                                        ((Module)mod).properties=modprops;
                                    }
                                    // set the module name property using the module's filename
                                    // maybe we need a parser.getModuleName() function to improve on this
                                    ((Module)mod).setName(bname);
                                    ((Module)mod).setMaintainer(parser.getModuleMaintainer());
                                    ((Module)mod).setVersion(parser.getModuleVersion());
                                    ((Module)mod).setClassName(parser.getClassFile());
                                    
                                }
                            } catch (java.lang.ClassNotFoundException cnfe) {
                                log.error("Could not load class with name '" + cname + "', " + 
                                          "which was specified in the module:'" + dirname + bname + ".xml'(" + cnfe + ")" );
                            } catch (Exception e) {
                                log.error("Error while loading module class" + Logging.stackTrace(e));
                            }
                        }
                    }
                }
            }
        }
        return(results);
    }

}
