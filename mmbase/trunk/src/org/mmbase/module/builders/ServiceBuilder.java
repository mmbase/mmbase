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
import java.net.*;

import org.mmbase.module.database.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.module.builders.protocoldrivers.*;
import org.mmbase.util.*;
import org.mmbase.module.sessionsInterface;
import org.mmbase.module.sessionInfo;
import org.mmbase.util.logging.*;

/**
 * @author Daniel Ockeloen
 * @version $Revision: 1.19 $ $Date: 2001-05-02 16:41:01 $
 */
public class ServiceBuilder extends MMObjectBuilder implements MMBaseObserver {

    private static Logger log = Logging.getLoggerInstance(ServiceBuilder.class.getName());

	// Used for retrieving selected cdtrack.
	private sessionsInterface sessions; 

	public boolean init() {
		// Initializing builder.
		super.init();
		MMServers bul=(MMServers)mmb.getMMObject("mmservers");
		if (bul!=null)
			bul.setCheckService(getTableName());
		else
			log.error("Can't get mmservers builder.");

		// Used for retrieving selected cdtrack.
		sessions = (sessionsInterface) org.mmbase.module.Module.getModule("SESSION");
		return true;
	}

	/**
	 * Claims (state=claimed) the service node and fills its info field with owner and selected tracknr
	 * that has to be ripped. The tracknr is retrieved from the session var -> name='serviceobj#TRACKNR'
	 * This is done through the reference variable(either alias or objnumber) stored in tok parameter.
	 * @param sp the scanpage object.
	 * @param tok a StringTokenizer with a servicebuilder type objectnumber (eg. cdplayers) 
	 * and a username of who used the service.
	 * @return an empty String!?
	 */
	public String doClaim(scanpage sp, StringTokenizer tok) {
		log.debug("Getting servicebuildertype node reference and username.");
		if (tok.hasMoreTokens()) {
			String cdplayernumber=tok.nextToken();
			if (tok.hasMoreTokens()) {
				String user=tok.nextToken();
				MMObjectNode node=getNode(cdplayernumber);
				if (node!=null) {
					String name=node.getStringValue("name");
					
					// Retrieve selected track from session (instead from ap.info) 
					// and add it together with user & lease to cdplayers.info.
					String tracknr=null;
					sessionInfo session=sessions.getSession(sp,sp.sname);
					tracknr = sessions.getValue(session,cdplayernumber+"TRACKNR");
					if (tracknr!=null) {
						log.debug("Retrieved tracknr from session value="+tracknr);
						log.debug("Filling info field with: user="+user+" lease=3"+" tracknr="+tracknr);
						node.setValue("info","user="+user+" lease=3"+" tracknr="+tracknr);
						log.debug("Changing "+name+" (obj "+cdplayernumber+") state from '"+node.getStringValue("state")+"' to 'claimed'");
						node.setValue("state","claimed");
					} else {
						log.error("Can't get tracknr from session '"+cdplayernumber+"TRACKNR'");
						node.setValue("state","error");
						node.setValue("info","ERROR: doClaim(): Can't get tracknr from session '"+cdplayernumber+"TRACKNR' value="+tracknr);
					}
					node.commit();
					log.debug("Service "+name+" ("+cdplayernumber+") successfully claimed by "+user);
				} else
					log.error("Can't claim service "+cdplayernumber+" for user:"+user+" because node is null");
			} else
				log.error("No username in StringTokenizer so I won't claim service: "+cdplayernumber);
		} else
			log.error("Empty StringTokenizer so there's nothing to claim!");
		return "";
	}

	/**
	 * Gets the first service node (cdplayers) that was claimed by a certain user. 
	 * @param owner a String with the name of the owner that claimed the service.
	 * @return the service node that was claimed.
	 */
	public MMObjectNode getClaimedService(String owner) {
		if(owner == null) {
			log.error("Owner value = null, returning null");
			return null;
		}
		MMObjectNode node = null;
		String info=null, user=null;
		StringTagger infotagger = null;
		log.info("Searching for the service that was claimed by "+owner);
		Enumeration e=search("WHERE state='claimed' AND number>0");
		while (e.hasMoreElements()) {
			node= (MMObjectNode)e.nextElement();
			info = node.getStringValue("info");
			if (info!=null) { 
				infotagger=new StringTagger(info);
				if (infotagger.containsKey("user")) {
					user=infotagger.Value("user");
					if (user!=null) {
						if (user.equals(owner)) {
							String name = node.getStringValue("name");
							log.info("("+owner+"): Found claimed node "+name+" of type "+node.getName());
							return node;
						} else 
							log.debug("("+owner+"): Wrong user:"+user+", skipping.");
					} else 
						log.error("("+owner+"): User value = null, for node:"+node+", skipping.");
				} else
					log.debug("("+owner+"): Key 'user' not found in info field, info:"+info+", skipping.");
			} else
				log.error("("+owner+"): 'info' field is null for node:"+node+", skipping.");
		}
		log.error("("+owner+"): Can't find any services claimed by "+owner+", returning null.");
		return null;
	}
	
	/**
	 * adds a service to this builder, name is allways valid and correct
	 * the Service builder _must_ update all the mmbase admins to reflect
	 * new state. Next version will support name with authentication.
	 *
	 * Currently always a service of type cdplayers is expected for insertion.
	 */
	public void addService(String name, String localclass, MMObjectNode mmserver) throws Exception {
		log.info("Adding new "+getTableName()+" service called: "+name+", and connecting it to mmserver "+mmserver.getStringValue("name"));
		MMObjectNode newnode=getNewNode("system");
		newnode.setValue("name",name);
		newnode.setValue("cdtype","C="+localclass); // Warning this will be altered to devtype
		newnode.setValue("state","waiting");
		newnode.setValue("info","");
		int newid=insert("system",newnode);
		if (newid!=-1) {
			log.info(getTableName()+" SERVICE: "+name+" ADDED ON "+(new java.util.Date()).toGMTString());
			InsRel bul=(InsRel)mmb.getMMObject("insrel");
			int relid = bul.insert("system",newid,mmserver.getIntValue("number"),14);	
			if (relid!=-1)
				log.info("RELATION INSERTED between service: "+name+" ("+newid+") and mmserver: "+mmserver.getIntValue("number"));
			else
				log.error("INSERT RELATION FAILED between service: "+name+" ("+newid+") and mmserver: "+mmserver.getIntValue("number"));
		} else
			log.error("INSERT FAILED for "+getTableName()+" service: "+name);
	}

	/**	
	 * adds a service to this builder. name is allways valid and correct
	 * the Service builder _must_ update all the mmbase admins to reflect
	 * new state. Next version will support name with authentication.
	 * adds a init hashtable for use for authentication and other startup
	 * params.
	 */
	public void addService(String name, String localclass, MMObjectNode mmserver, Hashtable initparams) throws Exception {
		log.debug("("+name+","+localclass+","+mmserver+","+initparams+"), called, but we do nothing.");
	}

	/**
	 * remove a service. Does not mean it will be 100% removed from mmbase 
	 * but that its not running at the moment. possible model will be removed 
	 * results in offline state for X hours and removed in X days.
	 */
	public void removeService(String name) throws Exception {
		log.debug("("+name+"), called, but we do nothing.");
	}

	/**
	 * remove a service. does not mean it will be 100% removed from mmbase 
	 * but that its not running at the moment. possible model will be removed 
	 * results in offline state for X hours and removed in X days.
	 */
	public void removeService(String name, Hashtable initparams) throws Exception {
		log.debug("("+name+","+initparams+"), called, but we do nothing.");
	}
	
	/**
	 * Prints that a remote server (other mmbase or remote builder) changed this service node.
	 * @param number a String with the object number of the node that was operated on.
	 * @param builder a String with the buildername of the node that was operated on.
	 * @param ctype a String with the node change type.
	 * @return true, always!?
	 */
	public boolean nodeRemoteChanged(String number,String builder,String ctype) {
		log.debug("("+number+","+builder+","+ctype+"): Calling super.");
		super.nodeRemoteChanged(number,builder,ctype);
		// Don't call sendToRemoteBuilder because nodeLocalChanged already sends a signal to remote side.
		return true;
	}

	/**
	 * Called when an operation is done on a service node eg insert, commit, it calls method
	 * to send the node change to the remote side.
	 * @param number a String with the object number of the node that was operated on.
	 * @param builder a String with the buildername of the node that was operated on.
	 * @param ctype a String with the node change type.
	 * @return true, always!?
	 */
	public boolean nodeLocalChanged(String number,String builder,String ctype) {
		log.debug("("+number+","+builder+","+ctype+"): Calling super.");
		super.nodeLocalChanged(number,builder,ctype);
		log.debug("("+number+","+builder+","+ctype+"): Calling sendToRemoteBuilder, to send node change to remote side.");
		// You can't signal new or delete changes because there's no mmserver related when these changes occur.
		if (!(ctype.equals("n") || ctype.equals("d")) )
			sendToRemoteBuilder(number,builder,ctype);
		return true;
	}

	/**
	 * Sends a signal to indicate that a certain service(builder) node was changed to the remote side.
	 * The information that's send is a service reference name, the builtertype and the mmbase changetype.
	 * Signalling is done using the protocoldriver attached to the first mmserver that in turn is 
	 * attached to the current service(builder) in progress.
	 * @param number a String with the objectnumber of the mmbase service(builder) that was changed.
	 * @param builder a String with the buildername of the node that was changed.
	 * @param ctype a String with the node change type.
	 */
	public void sendToRemoteBuilder(String number,String builder,String ctype) {
		//Get the first mmserver that's attached to the service node number
		Enumeration e=mmb.getInsRel().getRelated(number,"mmservers");
		if  (e.hasMoreElements()) {
			MMObjectNode mmservernode=(MMObjectNode)e.nextElement();	
			String mmservername=mmservernode.getStringValue("name");
			log.debug("("+number+","+builder+","+ctype+"): Found attached mmserver:"+mmservername);
			//Get the protocol driver using the mmserver that's attached to current service. 
			MMServers bul = (MMServers)mmb.getMMObject("mmservers");
			ProtocolDriver pd=bul.getDriverByName(mmservername);
			log.debug("("+number+","+builder+","+ctype+"): Retrieved its protocoldriver: "+pd);
			MMObjectNode node=getNode(number);
			if (node!=null) {
				String servicename = node.getStringValue("name");
				log.debug("("+number+","+builder+","+ctype+"): Sending signal to remote side using remote service reference:"+servicename);
				pd.signalRemoteNode(servicename,builder,ctype);
				log.debug("("+number+","+builder+","+ctype+"): Signal has been send with remote service reference used:"+servicename);
			}else
				log.error("("+number+","+builder+","+ctype+"): Couldn't get node("+number+")!");
		} else
			log.error("("+number+","+builder+","+ctype+"): No related mmserver found attached to this service with number:"+number);
	}
}
