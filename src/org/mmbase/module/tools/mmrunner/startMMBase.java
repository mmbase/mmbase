/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.util.*;

public class startMMBase extends Object {

	static String runnerVersion="0.2";
	static String appserverVersion="Orion 1.4.5";
	static String cmsVersion="MMBase 1.2.3";
	static String databaseVersion="Hypersonic 2.3";


	public static void main(String[] args) {
		String mode;
		System.out.println("\nStarting MMBase (runner version "+runnerVersion+")");
		if (args.length>0) {
			mode=args[0];
		} else {
			mode="loop";
		}

		// should be moved to own method in time
            	String curdir=System.getProperty("user.dir");
            	if (curdir.endsWith("orion")) {
                	curdir=curdir.substring(0,curdir.length()-6);
           	}

		// detect if this is the first startup
		if (firstContact(curdir)) {
			System.out.println("\nDetecting this is first run, need to ask a few questions.");
		}		

		System.out.println("\n----- starting java with parameters  -----");
		String configdir="-Dmmbase.config="+curdir+"/config/ ";
		System.out.println(configdir);
		String htmlrootdir="-Dmmbase.htmlroot="+curdir+"/html/ ";
		System.out.println(htmlrootdir);
		String logdir="-Dmmbase.outputfile="+curdir+"/log/mmbase.log ";
		System.out.println(logdir);
		String orion="-jar orion/orion.jar -config orion/config/server.xml";
		System.out.println(orion);
		String startupstring="java "+configdir+htmlrootdir+logdir+orion;
		System.out.println("-------------------------------------------\n");
		System.out.println("Starting Application server : "+appserverVersion);
		System.out.println("Loading  CMS : "+cmsVersion);
		System.out.println("Loading  JDBC Database : "+databaseVersion+"\n");
		if (mode.equals("loop")) {
			System.out.println("Logfiles active in the log dir (log/mmbase.log for example)");
			System.out.println("Within seconds server can be found at http://127.0.0.1:4242");
			while (1==1) {
				String reply=execute(startupstring);	
			}
		} else if (mode.equals("bg")) {
			System.out.println("Logfiles active in the log dir (log/mmbase.log for example)");
			System.out.println("Within seconds server can be found at http://127.0.0.1:4242");
			String reply=executeSpawn(startupstring);	
		}
		
	}


	static String execute (String command) {
		Process p=null;
		String s="",tmp="";

		BufferedReader	dip= null;
		BufferedReader	dep= null;
 
		try 
		{
			p = (Runtime.getRuntime()).exec(command,null);
			p.waitFor();
		} 
		catch (Exception e) 
		{
			s+=e.toString();
			return s;
		}

		dip = new BufferedReader( new InputStreamReader(p.getInputStream()));
		dep = new BufferedReader( new InputStreamReader(p.getErrorStream()));

		try 
		{
			while ((tmp = dip.readLine()) != null) 
			{
           		s+=tmp+"\n"; 
			}
			while ((tmp = dep.readLine()) != null) 
			{
				s+=tmp+"\n";
			}
		} 
		catch (Exception e) 
		{
			return s;
		}
		return s;
	}


	static String executeSpawn(String command) {
		Process p=null;
		String s="",tmp="";

		BufferedReader	dip= null;
		BufferedReader	dep= null;
 
		try 
		{
			p = (Runtime.getRuntime()).exec(command,null);
			//p.waitFor();
		} 
		catch (Exception e) 
		{
			s+=e.toString();
			return s;
		}
		return("");

	}

	static private boolean firstContact(String curdir) {
                File f=new File(curdir+"/config/.timestamp");
                return (!f.exists() || !f.isFile());
	}
}
