/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.util.*;
import org.mmbase.module.core.*;

import org.mmbase.module.corebuilders.*;

/**
*/
public class XMLApplicationWriter  {

    public static Vector writeXMLFile(XMLApplicationReader app,String targetpath,String goal,MMBase mmb) {
	Vector resultmsgs=new Vector();
	System.out.println("STARTED XML WRITER ON : "+app);

	// again this is a stupid class generating the xml file
	// the second part called the extractor is kind of neat
	// but very in early beta
	String name=app.getApplicationName();
	String maintainer=app.getApplicationMaintainer();
	int version=app.getApplicationVersion();
	boolean deploy=app.getApplicationAutoDeploy();
	
	String body="<application name=\""+name+"\" maintainer=\""+maintainer+"\" version=\""+version+"\" auto-deploy=\""+deploy+"\">\n";

	// write the needed builders
	body+=getNeededBuilders(app);
	
	// write the needed reldefs
	body+=getNeededRelDefs(app);

	// write the allowed relations
	body+=getAllowedRelations(app);

	// write the datasources
	body+=getDataSources(app);

	// write the datasources
	body+=getDataSources(app);

	// write the relationsources
	body+=getRelationSources(app);

	// write the contextsources
	body+=getContextSources(app);


	// write the description
	body+=getDescription(app);

	// write the install-notice
	body+=getInstallNotice(app);

	
	// close the application file
	body+="</application>\n";
	saveFile(targetpath+"/"+app.getApplicationName()+".xml",body);

	// now the tricky part starts figure out what nodes to write
	writeDateSources(app,targetpath,mmb,resultmsgs);

	// now write the context files itself
	writeContextSources(app,targetpath);

	resultmsgs.addElement("Writing Application file : "+targetpath+"/"+app.getApplicationName()+".xml");
		
	return(resultmsgs);
    }

    static String getNeededBuilders(XMLApplicationReader app) {
	String body="\t<neededbuilderlist>\n";
	Vector builders=app.getNeededBuilders();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String name=(String)bset.get("name");
		String maintainer=(String)bset.get("maintainer");
		String version=(String)bset.get("version");
		body+="\t\t<builder maintainer=\""+maintainer+"\" version=\""+version+"\">"+name+"</builder>\n";
	}
	body+="\t</neededbuilderlist>\n\n";
	return(body);	
    }


    static String getNeededRelDefs(XMLApplicationReader app) {
	String body="\t<neededreldeflist>\n";
	Vector builders=app.getNeededRelDefs();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String source=(String)bset.get("source");
		String target=(String)bset.get("target");
		String dir=(String)bset.get("direction");
		String guisourcename=(String)bset.get("guisourcename");
		String guitargetname=(String)bset.get("guitargetname");
		body+="\t\t<reldef source=\""+source+"\" target=\""+target+"\" direction=\""+dir+"\" guisourcename=\""+guisourcename+"\" guitargetname=\""+guitargetname+"\" />\n";
	}
	body+="\t</neededreldeflist>\n\n";
	return(body);	
    }


    static String getAllowedRelations(XMLApplicationReader app) {
	String body="\t<allowedrelationlist>\n";
	Vector builders=app.getAllowedRelations();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String from=(String)bset.get("from");
		String to=(String)bset.get("to");
		String type=(String)bset.get("type");
		body+="\t\t<relation from=\""+from+"\" to=\""+to+"\" type=\""+type+"\" />\n";
	}
	body+="\t</allowedrelationlist>\n\n";
	return(body);	
    }


    static String getDataSources(XMLApplicationReader app) {
	String body="\t<datasourcelist>\n";
	Vector builders=app.getDataSources();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String path=(String)bset.get("path");
		body+="\t\t<datasource path=\""+path+"\" />\n";
	}
	body+="\t</datasourcelist>\n\n";

	return(body);	
    }


    static String getRelationSources(XMLApplicationReader app) {
	String body="\t<relationsourcelist>\n";
	Vector builders=app.getRelationSources();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String path=(String)bset.get("path");
		body+="\t\t<relationsource path=\""+path+"\" />\n";
	}
	body+="\t</relationsourcelist>\n\n";
	return(body);	
    }

    static String getDescription(XMLApplicationReader app) {
	String body="\t<description>";
	String tmp=app.getDescription();
	if (tmp!=null && !tmp.equals("")) body+=tmp;
	body+="</description>\n\n";
	return(body);
    }


    static String getInstallNotice(XMLApplicationReader app) {
	String body="\t<install-notice>";
	String tmp=app.getInstallNotice();
	if (tmp!=null && !tmp.equals("")) body+=tmp;
	body+="</install-notice>\n";
	return(body);
    }

    static String getContextSources(XMLApplicationReader app) {
	String body="\t<contextsourcelist>\n";
	Vector builders=app.getContextSources();
	for (Enumeration e=builders.elements();e.hasMoreElements();) {
		Hashtable bset=(Hashtable)e.nextElement();
		String path=(String)bset.get("path");
		String type=(String)bset.get("type");
		String goal=(String)bset.get("goal");
		body+="\t\t<contextsource path=\""+path+"\" type=\""+type+"\" goal=\""+goal+"\"/>\n";
	}
	body+="\t</contextsourcelist>\n\n";
	return(body);	
    }


	static boolean saveFile(String filename,String value) {
		File sfile = new File(filename);
		try {
			DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
			scan.writeBytes(value);
			scan.flush();
			scan.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return(true);
	}

	private static void writeDateSources(XMLApplicationReader app,String targetpath,MMBase mmb,Vector resultmsgs) {

		Vector builders=app.getContextSources();
		for (Enumeration e=builders.elements();e.hasMoreElements();) {
			Hashtable bset=(Hashtable)e.nextElement();
			String path=(String)bset.get("path");
			String type=(String)bset.get("type");
			String goal=(String)bset.get("goal");

			path=MMBaseContext.getConfigPath()+("/applications/"+path);
			resultmsgs.addElement("save type : "+type);
			resultmsgs.addElement("save goal : "+goal);

			if (type.equals("depth")) {
				XMLContextDepthReader capp=new XMLContextDepthReader(path);
				XMLContextDepthWriter.writeContext(app,capp,targetpath,mmb,resultmsgs);
			}
		}
	}



	private static void writeContextSources(XMLApplicationReader app,String targetpath) {

		Vector builders=app.getContextSources();
		for (Enumeration e=builders.elements();e.hasMoreElements();) {
			Hashtable bset=(Hashtable)e.nextElement();
			String path=(String)bset.get("path");
			String type=(String)bset.get("type");
			String goal=(String)bset.get("goal");

			path=MMBaseContext.getConfigPath()+("/applications/"+path);
			System.out.println("READ="+path+" type="+type);
			if (type.equals("depth")) {
				XMLContextDepthReader capp=new XMLContextDepthReader(path);
				XMLContextDepthWriter.writeContextXML(capp,targetpath+"/"+(String)bset.get("path"));
			}
		}
	}

}
