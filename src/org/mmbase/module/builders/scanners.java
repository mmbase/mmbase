/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import javax.servlet.http.*;

import org.mmbase.module.*;
import org.mmbase.module.builders.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * The scanners builder contains scanners that MMBase can use.
 * These scanners will implement the ImageInterface so that the Image builder
 * can access the scanners. The scanner will use a implementation that will
 * also be defined by a builder.
 * 
 * @author Rob Vermeulen
 * @date 12 juli 2000
 */

/**
 */
public class scanners extends MMObjectBuilder implements MMBaseObserver {

	static Logger log =Logging.getLoggerInstance(scanners.class.getName()); 
	public final static String buildername = "scanners";
	public static java.util.Properties driveprops= null;

	public scanners() {
	}
	
	/**
    * Obtains a list of string values by performing the provided command and parameters.
    * This method is SCAN related and may fail if called outside the context of the SCAN servlet.
    * @param sp The scanpage (containing http and user info) that calls the function
    * @param tagger a Hashtable of parameters (name-value pairs) for the command
    * @param tok a list of strings that describe the (sub)command to execute
    * @return a <code>Vector</code> containing the result values as a <code>String</code>
    */
	public Vector getList(scanpage sp, StringTagger tagger, StringTokenizer tok) throws org.mmbase.module.ParseException {
		log.trace("getList");
		String scanner ="";
        String path = "";
        Vector result = new Vector();

		try {
			scanner = tok.nextToken();
		} catch (Exception e) {
			log.error("Syntax of LIST commando = <LIST BUILDER-scanner-[scannername]");					
		}
		log.debug("scanners scanner="+scanner);			
       	String comparefield = "modtime";
       	DirectoryLister imglister = new DirectoryLister(); 
		Enumeration g = search("WHERE name='"+scanner+"'");
        while (g.hasMoreElements()) {
          	MMObjectNode scannernode=(MMObjectNode)g.nextElement();
           	path=scannernode.getStringValue("directory");
        }
		log.debug("scanner '"+scanner+"' its path='"+path+"'");			
		Vector unsorted = null;
		Vector sorted = null;
		try {
           	unsorted = imglister.getDirectories(path);  //Retrieve all filepaths
			debug("unsorted files amount:" + unsorted.size());
            sorted = imglister.sortDirectories(unsorted,comparefield);
			debug("sorted files amount:" + sorted.size());			
        	result = imglister.createThreeItems(sorted,tagger);
			debug("result files amount(after createThreeItems):" + result.size());						
		} catch (Exception e) {
			log.error("Something went wrong in the directory listner, probably "+path+" does not exists needed by "+scanner);			
		}
        tagger.setValue("ITEMS", "3");
        String reverse = tagger.Value("REVERSE");
        if (reverse!=null){
         	if(reverse.equals("YES")){
               	int items = 3;
                result = imglister.reverse(result,items);
				log.debug("reversing vector");			
            }
        }
        return (result);
    }

}
