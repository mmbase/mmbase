/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.mmbob;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.*;
import org.mmbase.util.xml.EntityResolver;
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * @javadoc
 * @author Daniel Ockeloen
 * @version $Id$
 */
public class ProfileInfo {

    static private final Logger log = Logging.getLoggerInstance(ProfileInfo.class);

    private int id = -1;
    private Poster parent;
    private Forum forum;
    private String xml;
    private String external;
    private int synced;
    private HashMap entries = new HashMap();

    public static final String DTD_PROFILEINFO_1_0 = "profileinfo_1_0.dtd";
    public static final String PUBLIC_ID_PROFILEINFO_1_0 = "-//MMBase//DTD mmbob profileinfo 1.0//EN";

    /**
     * Register the Public Ids for DTDs used by DatabaseReader
     */
    public static void registerPublicIDs() {
        EntityResolver.registerPublicID(PUBLIC_ID_PROFILEINFO_1_0, DTD_PROFILEINFO_1_0, ProfileInfo.class);
    }

    public ProfileInfo(Poster parent) {
	this.parent  = parent;
	this.forum = parent.getParent();
	//syncExternals();
    }

    public ProfileInfo(Poster parent,int id,String xml,String external,int synced) {
	this.parent  = parent;
	this.id=id;
	this.xml=xml;
	this.external=external;
	this.synced=synced;
	this.parent=parent;
	decodeXML();
	this.forum = parent.getParent();
	//syncExternals();
    }


    public int getId() {
	return id;
    }

    public boolean save() {
	if (id!=-1) {
            org.mmbase.bridge.Node node = ForumManager.getCloud().getNode(id);
            node.setValue("xml",encodeXML());
            node.setIntValue("synced",synced);
            node.commit();
	} else {
            NodeManager man = ForumManager.getCloud().getNodeManager("profileinfo");
            org.mmbase.bridge.Node node = man.createNode();
            node.setValue("xml",encodeXML());
            node.setIntValue("synced",synced);
            node.commit();

            RelationManager rm = ForumManager.getCloud().getRelationManager("posters", "profileinfo", "related");
            if (rm != null) {
                org.mmbase.bridge.Node rel = rm.createRelation(ForumManager.getCloud().getNode(parent.getId()), node);
                rel.commit();
            }
            id = node.getNumber();
	}
	return true;
    }

    private void decodeXML() {
        if (xml!=null && !xml.equals("")) {
            try {
                DocumentReader reader = new DocumentReader(new InputSource(new StringReader(xml)),ProfileInfo.class);
                if(reader != null) {
                    for(Element n : ForumsConfig.list(reader.getChildElements("profileinfo", "entry"))) {
                        NamedNodeMap nm = n.getAttributes();
                        if (nm != null) {
                            String name = null;
                            boolean synced = false;
                            boolean edit = false;

                            // decode name
                            org.w3c.dom.Node n2 = nm.getNamedItem("name");
                            if (n2 != null) {
                                name = n2.getNodeValue();
                            }

                            // decode synced
                            n2 = nm.getNamedItem("synced");
                            if (n2 != null) {
                                if (n2.getNodeValue().equals("true")) synced = true;
                            }
                            if (name!=null) {
				ProfileEntry pe = new ProfileEntry();
				pe.setName(name);
				org.w3c.dom.Node n4 = n.getFirstChild();
				if (n4!=null) {
                                    pe.setValue(n4.getNodeValue());
				} else {
                                    pe.setValue("");
				}
				pe.setSynced(synced);
				entries.put(name,pe);
                            }
                        }
                    }
                }
            } catch(Exception e) {
		log.error("Decode problem with : "+xml);
            }
	}
    }

    public Iterator getValues() {
	return entries.values().iterator();
    }

    public ProfileEntry getValue(String name) {
	Object o = entries.get(name);
	if (o!=null) return (ProfileEntry)o;
	return null;
    }

    public String setValue(String name,String value) {
	ProfileEntry pe = (ProfileEntry) entries.get(name);
	if (pe==null) {
            pe = new ProfileEntry();
            entries.put(name,pe);
	}
	pe.setName(name);
	String oldvalue = getValue(name).getValue();
	if (oldvalue==null || !oldvalue.equals(value)) {
            pe.setValue(value);
            pe.setSynced(false);
            setSynced(false);
            save();
            ProfileEntryDef pd=forum.getProfileDef(name);
            if (pd!=null) {
                String external = pd.getExternal();
                String externalname = pd.getExternalName();

                if (external!=null && !external.equals("")) {
                    ExternalProfilesManager.addToSyncQueue(this);
                }
            }
	}
	return null;
    }

    private String encodeXML() {
	String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body += "<!DOCTYPE profileinfo PUBLIC \"-//MMBase/DTD mmbob profileinfo 1.0//EN\" \"https://www.mmbase.org/dtd/mmbobprofileinfo_1_0.dtd\">\n";
	body += "<profileinfo>\n";

	Iterator pi=entries.values().iterator();
        while (pi.hasNext()) {
            ProfileEntry pe = (ProfileEntry)pi.next();
            body += "\t<entry name=\""+pe.getName()+"\"><![CDATA["+pe.getValue()+"]]></entry>\n";
	}
	body += "</profileinfo>\n";
	return body;
    }

    private void syncExternals() {
	Iterator pdi=forum.getProfileDefs();
	if (pdi!=null) {
            while (pdi.hasNext()) {
		ProfileEntryDef pd = (ProfileEntryDef)pdi.next();
		String external = pd.getExternal();
		String externalname = pd.getExternalName();
		if (external!=null && !external.equals("")) {
                    ExternalProfileInterface ci = ExternalProfilesManager.getHandler(external);
                    if (ci!=null) {
                        String name = pd.getName();
                        String account = parent.getAccount();
                        if (externalname!=null && !externalname.equals("")) {
                            String rvalue = ci.getValue(account,externalname);
                            if (rvalue!=null) setValue(name,rvalue);
                        } else {
                            String rvalue = ci.getValue(account,name);
                            if (rvalue!=null) setValue(name,rvalue);
                        }
                    }
		}
            }
	}
    }

    public ProfileEntryDef getProfileDef(String name) {
	return forum.getProfileDef(name);
    }

    public String getAccount() {
	return parent.getAccount();
    }

    public void loginTrigger() {
	ExternalProfilesManager.addToCheckQueue(this);
    }

    public void setSynced(boolean value) {
	if (value) {
            synced = 1;
	} else {
            synced = -1;
	}
    }

}
