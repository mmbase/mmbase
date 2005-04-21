/*

VPRO (C)

This source file is part of mmbase and is (c) by VPRO until it is being
placed under opensource. This is a private copy ONLY to be used by the
MMBase partners.

*/
package org.mmbase.module.builders.vwms;

import java.util.*;
import java.sql.*;
import java.io.*;

import javax.servlet.http.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;
import org.mmbase.module.builders.*;


/**
 * @author Daniel Ockeloen
 */

public class ImageMaster extends Vwm implements MMBaseObserver,VwmServiceInterface {

	Hashtable properties;
	boolean first=true;

	public ImageMaster() {
		System.out.println("PageMaster ready for action");
	}


	public boolean probeCall() {
		System.out.println("ImageMaster probe");
		if (first) {
			first=false;
		} else {
			try {
				Netfiles bul=(Netfiles)Vwms.mmb.getMMObject("netfiles");		
				Enumeration e=bul.search("WHERE service='images' AND subservice='mirror' AND status=1 ORDER BY number DESC");
				int i=0;
				while (e.hasMoreElements() && i<10) {
					MMObjectNode node=(MMObjectNode)e.nextElement();
					fileChange(""+node.getIntValue("number"),"c");
					i++;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}	
		}
		return(true);
	}

	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		return(nodeChanged(number,builder,ctype));
	}

	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		return(nodeChanged(number,builder,ctype));
	}

	public boolean nodeChanged(String number,String builder, String ctype) {
		System.out.println("Image sees that : "+number+" has changed type="+ctype);
		return(true);
	}

	public boolean fileChange(String service,String subservice,String filename) {
		filename=URLEscape.unescapeurl(filename);
		System.out.println("ImageMaster frontend change -> "+filename);
		// jump to correct subhandles based on the subservice
		if (subservice.equals("main")) {
			handleMainCheck(service,subservice,filename);
		}	
		return(true);
	}

	public boolean fileChange(String number, String ctype) {
		// System.out.println("ImageMaster -> fileChange="+number+" "+ctype);
		// first get the change node so we can see what is the matter with it.
		Netfiles bul=(Netfiles)Vwms.mmb.getMMObject("netfiles");		
		MMObjectNode filenode=bul.getNode(number);
		if (filenode!=null) {
			// obtain all the basic info on the file.
			String service=filenode.getStringValue("service");
			String subservice=filenode.getStringValue("subservice");
			int status=filenode.getIntValue("status");

			// jump to correct subhandles based on the subservice
			if (subservice.equals("main")) {
				handleMain(filenode,status,ctype);
			} else if (subservice.equals("mirror")) {
				handleMirror(filenode,status,ctype);
			}	
		}
		return(true);
	}

	public boolean handleMirror(MMObjectNode filenode,int status,String ctype) {
		switch(status) {
			case 1:  // Verzoek
				System.out.println("ImageMaster-> mirror verzoek");
				filenode.setValue("status",2);
				filenode.commit();
				// do stuff
				String filename=filenode.getStringValue("filename");
				String dstserver=filenode.getStringValue("mmserver");
				
				// save the image to disk
				ImageCaches bul=(ImageCaches)Vwms.mmb.getMMObject("icaches");		
				
				// get the clear ckey
				String ckey=filename.substring(8);
				int pos=ckey.indexOf(".");
				if (pos!=-1) {
					ckey=ckey.substring(0,pos);
					ckey=path2ckey(ckey);
				}

				System.out.println("ImageMaster -> "+ckey);
				byte[] filebuf=bul.getCkeyNode(ckey);
				System.out.println("ImageMaster -> "+filebuf.length);
				String srcpath=getProperty("test1:path"); // hoe komen we hierachter ?
				saveImageAsisFile(srcpath,filename,filebuf);
				

				// recover teh correct source/dest properties for this mirror
				String sshpath=getProperty("sshpath");
				String dstuser=getProperty(dstserver+":user");
				String dsthost=getProperty(dstserver+":host");
				String dstpath=getProperty(dstserver+":path");

				SCPcopy scpcopy=new SCPcopy(sshpath,dstuser,dsthost,dstpath);

				scpcopy.copy(srcpath,filename);

				// remove the tmp image file

				System.out.println("ImageMaster-> doing mirror stuff");
				filenode.setValue("status",3);
				filenode.commit();
				break;
			case 2:  // Onderweg
				System.out.println("ImageMaster-> Mirror Onderweg");
				break;
			case 3:  // Gedaan
				System.out.println("ImageMaster-> Mirror Done");
				break;
		}
		return(true);
	}


	public boolean handleMain(MMObjectNode filenode,int status,String ctype) {
		switch(status) {
			case 1:  // Verzoek
				System.out.println("ImageMaster-> main verzoek");
				filenode.setValue("status",2);
				filenode.commit();
				// do stuff
				System.out.println("ImageMaster-> doing main stuff");
				doMainRequest(filenode);
				filenode.setValue("status",3);
				filenode.commit();
				break;
			case 2:  // Onderweg
				System.out.println("ImageMaster-> main Onderweg");
				break;
			case 3:  // Gedaan
				System.out.println("ImageMaster-> main Done");
				break;
		}
		return(true);
	}

	public boolean doMainRequest(MMObjectNode filenode) {
		// so this file has changed probably, check if the file is ready on
		// disk and set the mirrors to dirty/request.
		String filename = filenode.getStringValue("filename");
		
		// find and change all the mirror node so they get resend
		Netfiles bul=(Netfiles)Vwms.mmb.getMMObject("netfiles");		
		Enumeration e=bul.search("WHERE filename='"+filename+"' AND service='images' AND subservice='mirror'");
		while (e.hasMoreElements()) {
			MMObjectNode mirrornode=(MMObjectNode)e.nextElement();
			mirrornode.setValue("status",1);
			mirrornode.commit();
		}
		return(true);
	}

	public void handleMainCheck(String service,String subservice,String filename) {
		Netfiles bul=(Netfiles)Vwms.mmb.getMMObject("netfiles");		
		Enumeration e=bul.search("WHERE filename='"+filename+"' AND service='"+service+"' AND subservice='"+subservice+"'");
		if (e.hasMoreElements()) {
			MMObjectNode mainnode=(MMObjectNode)e.nextElement();
			// hier moet een check komen of hij al niet onderweg is !!!
			int currentstatus=mainnode.getIntValue("status");
			if (currentstatus>1) {
				mainnode.setValue("status",1);
				mainnode.commit();
			}
		} else {
			MMObjectNode mainnode=bul.getNewNode("system");
			mainnode.setValue("filename",filename);
			mainnode.setValue("mmserver","test1");
			mainnode.setValue("service",service);
			mainnode.setValue("subservice",subservice);
			mainnode.setValue("status",3);
			mainnode.setValue("filesize",-1);
			bul.insert("system",mainnode);	

			// hack hack moet ook mirror nodes aanmaken !
			mainnode=bul.getNewNode("system");
			mainnode.setValue("filename",filename);
			mainnode.setValue("mmserver","omroep");
			mainnode.setValue("service",service);
			mainnode.setValue("subservice","mirror");
			mainnode.setValue("status",1);
			mainnode.setValue("filesize",-1);
			bul.insert("system",mainnode);	
		}
	}

	public String getProperty(String key) {
		if (properties==null) initProperties();
		return((String)properties.get(key));
	}

	private void initProperties() {
		properties=new Hashtable();
		properties.put("sshpath","/usr/local/bin");
		properties.put("omroep:user","vpro");
		properties.put("omroep:host","vpro.omroep.nl");
		properties.put("omroep:path","/bigdisk/htdocs/");
		properties.put("test1:path","/usr/local/log/james/scancache/PAGE");
	}


	public boolean saveImageAsisFile(String path,String filename,byte[] value) {
		System.out.println("SAVE TO DISK="+path+filename);

		String header="Status: 200 OK";
		header+="\r\nContent-type: image/jpeg";
		header+="\r\nContent-length: "+value.length;
	    header+="\r\n\r\n";

		File sfile = new File(path+filename);
		try {
			DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
			scan.writeBytes(header);
			scan.write(value);
			scan.flush();
			scan.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return(true);
	}


	private String path2ckey(String path) {
		StringTokenizer tok = new StringTokenizer(path,"+\n\r");
		String ckey=tok.nextToken();

		// check if its a number if not check for name and even oalias
		System.out.println("CKEY="+ckey);
		try {
			int numint=Integer.parseInt(ckey);
		} catch(Exception e) {
			MMObjectBuilder imgbul=Vwms.mmb.getMMObject("images");
			if (imgbul!=null) {
				Enumeration g=imgbul.search("MMNODE images.title==*"+ckey+"*");
				while (g.hasMoreElements()) {
					MMObjectNode imgnode=(MMObjectNode)g.nextElement();
					ckey=""+imgnode.getIntValue("number");
				}
			}		
		}	


		while (tok.hasMoreTokens()) {
			String key=tok.nextToken();
			ckey+=key;
		}
		return(ckey);
	}
}
