/* -*- tab-width: 4; -*-

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

/*************************************************************************
 * NOTE This Builder needs significant changes to operate on NON-VPRO
 * machines. Do NOT use before that, also ignore all errors stemming from
 * this builder
 *************************************************************************/
package org.mmbase.module.builders;

import java.util.*;
import java.sql.*;
import java.io.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.util.*;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @author Daniel Ockeloen
 * @author David van Zeventer
 * @$Revision: 1.11 $ $Date: 2001-05-16 15:16:36 $
 *
 */
public class RawAudios extends MMObjectBuilder {

    private static Logger log = Logging.getLoggerInstance(RawAudios.class.getName()); 

 	public boolean replaceCache=true;

	// These contstants are used by the new AudioParts.doGetUrl() method.
	public final static int MP3_FORMAT         = 1;
	public final static int RA_FORMAT          = 2;
	public final static int WAV_FORMAT         = 3;
	public final static int PCM_FORMAT         = 4;
	public final static int MP2_FORMAT         = 5;
	public final static int SURESTREAM_FORMAT  = 6; 
	public final static int EXCERPT_FORMAT     = 7; 
	public final static int GEDAAN = 3;

	public RawAudios() {
	}

	public String getGUIIndicator(MMObjectNode node) {
		String str=node.getStringValue("number");
		if (str.length()>15) {
			return(str.substring(0,12)+"...");
		} else {
			return(str);
		}
	}

	public String getGUIIndicator(String field,MMObjectNode node) {
		if (field.equals("status")) {
			int val=node.getIntValue("status");
			switch(val) {
				case 1: return("Verzoek");
				case 2: return("Onderweg");
				case 3: return("Gedaan");
				case 4: return("Bron");
				default: return("Onbepaald");
			}
		} else if (field.equals("format")) {
			int val=node.getIntValue("format");
			switch(val) {
				case 1: return("mp3");
				case 2: return("ra");
				case 3: return("wav");
				case 4: return("pcm");
				case 5: return("mp2");
				case 6: return("g2/sure");
				case 7: return("excerpt");
				default: return("Onbepaald");
			}
		} else if (field.equals("channels")) {
			int val=node.getIntValue("channels");
			switch(val) {
				case 1: return("mono");
				case 2: return("stereo");
				default: return("Onbepaald");
			}
		}
		return(null);
	}

	/**
	* get new node
	*/
	public MMObjectNode getNewNode(String owner) {
		MMObjectNode node=super.getNewNode(owner);
		// readCDInfo();
		// if (diskid!=null) node.setValue("discId",diskid);
		// if (playtime!=-1) node.setValue("playtime",playtime);
		return(node);
	}


	public Object getValue(MMObjectNode node,String field) {
		if (field.equals("str(status)")) {
			int val=node.getIntValue("status");
			switch(val) {
				case 1: return("Verzoek");
				case 2: return("Onderweg");
				case 3: return("Gedaan");
				case 4: return("Bron");
				default: return("Onbepaald");
			}
		} else if (field.equals("str(channels)")) {
			int val=node.getIntValue("channels");
			switch(val) {
				case 1: return("Mono");
				case 2: return("Stereo");
				default: return("Onbepaald");
			}
		} else if (field.equals("str(format)")) {
			int val=node.getIntValue("format");
			switch(val) {
				case 1: return("mp3");
				case 2: return("ra");
				case 3: return("wav");
				case 6: return("g2/sure");
				default: return("Onbepaald");
			}
		}
		return(super.getValue(node,field));
	}


	public boolean removeAudio(int id) {
		Connection con;
		boolean rtn=false;
		MMObjectNode node;
		Enumeration audios;

		audios=search("WHERE id="+id);
		while(audios.hasMoreElements()) {
			node=(MMObjectNode)audios.nextElement();
            log.service("removeAudio(" + id + "): Zapping " +
                        node.getIntValue("number") + "," + node.getStringValue("url"));
			removeRelations(node);
			removeNode(node);
			zapPhysical(node);
			rtn=true;
		}
		if (true) {
			// For every format check the directory
			// MP3 
			// Nothing yet
			// RA
			removeRA(id);
			// WAV
			// Nothing yet
		}
		return(rtn);
	}

	private void zapPhysical(MMObjectNode node) {
		int id,iformat;
		int speed,channels;
		String path;
		String name;

		id=node.getIntValue("id");
		iformat=node.getIntValue("format");
		speed=node.getIntValue("speed")/1000;
		channels=node.getIntValue("channels");
		switch(iformat) {
			case 1: // mp3
				// Nothing for now
				path="/data/audio/mp3/"+id;
				break;
			case 2: // ra
				// Decode .ra file name
				path="/data/audio/ra/"+id;
				name=speed+"_"+channels+".ra";
				removeFile(path,name);
				break;
			case 3: // wav
				// Nothing for now
				path="/data/audio/wav/"+id;
				break;
			case 6: // G2
				path="/data/audio/ra/"+id;
				name="surestream.rm";
				removeFile(path,name);
				break;
			default: // Unknown
				break;
		}
	}

	public String getFullName(MMObjectNode node) {
		int id,iformat;
		int speed,channels;
		String path;
		String name;

		id=node.getIntValue("id");
		iformat=node.getIntValue("format");
		speed=node.getIntValue("speed")/1000;
		channels=node.getIntValue("channels");
		switch(iformat) {
			case 1: // mp3
				path="/data/audio/mp3/"+id+"/"+speed+"_"+channels+".mp3";
				break;
			case 2: // ra
				path="/data/audio/ra/"+id+"/"+speed+"_"+channels+".ra";
				break;
			case 3: // wav
				path="/data/audio/wav/"+id+".wav";
				break;
			case 6: // G2
				path="/data/audio/ra/"+id+"/"+"surestream.rm";
				break;
			default: // Unknown
				path=null;
				break;
		}
		return(path);
	}

	private void removeRA(int id) {
		String path="/data/audio/ra";
		String name="real.txt";
		removeFile(path,id+"/"+name);
		removeFile(path,""+id);
	}

	private void removeFile(String path,String name) {
		File f;

		f=new File(path,name);
		if (f.isDirectory()) {
            if (log.isServiceEnabled()) {
                log.service("removeFile(" + path + "/" + name + "): Removing dir " + f.getPath());
            }
			if (!f.delete()) {
				log.error("removeFile(" + path + "/" + name + "): Can't delete directory " + f.getPath());
			}
		} else {
            if (log.isServiceEnabled()) {
                log.service("Removing file " + f.getPath());
            }
			if (!f.delete()) {
				log.error("removeFile("+path+"/"+name+"): Can't delete file "+f.getPath());
			}
		}
	}

	/**
	 * getFileName: Gets the right audio filename using the format speed and channels values.
	 * @param format The audio format used.
	 * @param speed The speed value.
	 * @param channels The channels value.
	 * @return The audio fileName
	 */
	public static String getFileName(int format, int speed, int channels) {	
		String fileName = new String();
		String SURESTREAM_FILENAME = "surestream.rm"; 

		if (format == RA_FORMAT) {
			fileName = ""+(speed/1000)+"_"+channels+".ra";
		} else if (format == WAV_FORMAT) {
            if (log.isDebugEnabled()) {
                log.debug("getFileName(" + format + "," + speed + "," + channels + 
                          "): Yeah right!! I'm NOT giving you the wav filename!");
            }
		} else if (format == SURESTREAM_FORMAT) {
			fileName = SURESTREAM_FILENAME;
		}
        return fileName;
	}

	/**
	 * getHostName: Gets the right hostname and using String containing a rawaudios.url field.
	 * This method contains a lot of if-then-else constructs, since the RawAudios.url field uses
	 * such a StrangE! format.
	 * the url format is either: F=/audiopart#/16_1.ra H1=hostA.bla.nl H2=hostB.bla.nl 
	 * the url format is or:     http://hostA.vpro.nl/audio/ra/audiopart#/40_1.ra 
	 * @param url A String containing the contents of the rawaudios.url field.
	 * @return The hostName
	 */
	public static String getHostName(String url) {
		String FLIPSYMBOL = "F";
		String HOSTSYMBOL = "H";
		String DEFAULTHOST = "station.vpro.nl";
		String hostName = new String();

		try {
			if (url.startsWith(FLIPSYMBOL)) {
				// If H2 exists, then return H2 else return H1. If H1 Also doesn't exist, return DEFAULTHOST.
				if (url.indexOf(HOSTSYMBOL+"2") != -1) {
					// Get everything starting at H2=here ,thus 3 chars further.
					hostName = url.substring(url.indexOf(HOSTSYMBOL+"2") + 3);
					// Only use String up until the first space character  
					if (hostName.indexOf(" ") != -1) 
						hostName = hostName.substring(0,hostName.indexOf(" "));
				} else if (url.indexOf(HOSTSYMBOL+"1") != -1) {
					// Get everything starting at H1=here ,thus 3 chars further.
					hostName = url.substring(url.indexOf(HOSTSYMBOL+"1") + 3);
					// Only use String up until the first space character  
					if (hostName.indexOf(" ") != -1) 
						hostName = hostName.substring(0,hostName.indexOf(" "));
				} else {
					log.error("getHostName(" + url + "): Url field contains " + FLIPSYMBOL + 
                              " symbol but NO " + HOSTSYMBOL + " symbol -> returning defaulthost:" + DEFAULTHOST);
					hostName = DEFAULTHOST;
				}
			} else {
				// Get the hostname after the protocolname up until the first slash. (probably station.vpro.nl)
				int fromIndex = url.indexOf("://") + 3;
				hostName = url.substring(fromIndex, url.indexOf("/",fromIndex));
			}
			return hostName;
		} catch (Exception e) {
			// If the format of the Url field is changed, an exception could be thrown during manipulation,
			// then return the defaulthost.
			log.error("getHostName(" + url + "): Url field format is unknown to me, returning defaulthost(" + 
                      DEFAULTHOST + "), exception was: " + e.toString());
			return DEFAULTHOST;
		}
	}

	/**
	 * getProtocolName: Gets the protocol name used for this audiofile.
	 * Currently only SURESTREAM and RA is supported and the method returns HTTP if another.
	 * @param format The audio format used.
	 * @return A String containing the protocolName.
	 */
	public static String getProtocolName(int format) {
		String protName = new String();

		if (format == SURESTREAM_FORMAT) {
			protName = "rtsp";
		} else if (format == RA_FORMAT) {
			protName = "pnm";
		} else {
			protName = "http";
		}
		return protName;
	}
}
