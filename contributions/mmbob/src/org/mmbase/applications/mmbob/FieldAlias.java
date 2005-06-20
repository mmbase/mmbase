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
import org.mmbase.bridge.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 * 
 */
public class FieldAlias implements MMBaseObserver {
 
   // logger
   static private Logger log = Logging.getLoggerInstance(FieldAlias.class); 

   private String id;
   private String object;
   private String extern;
   private String field;
   private String externfield;
   private String key;
   private String externkey;
   private Forum forum;

   public FieldAlias (String id) {
	//log.info("Field alias on : "+id);
	this.id=id;
   }
  
   public void setObject(String object) {
	this.object = object;
   }

   public void setExtern(String extern) {
	this.extern = extern;
   }

   public void setField(String field) {
	this.field = field;
   }

   public void setExternField(String externfield) {
	this.externfield = externfield;
   }


   public void setKey(String key) {
	this.key = key;
   }

   public void setExternKey(String externkey) {
	this.externkey = externkey;
   }

   public String getValue(org.mmbase.bridge.Node node) {
	// is it a key mapping ?
	if (key!=null) {
		String keyvalue=node.getStringValue(key);
		//log.info("key="+key+" keyvalue="+keyvalue+" object="+object);
		Cloud cloud = ForumManager.getCloud();
                NodeManager manager=cloud.getNodeManager(extern);
        	NodeQuery query = manager.createQuery();
                StepField f1=query.getStepField(manager.getField(externkey));
		query.setConstraint(new BasicFieldValueConstraint(f1,keyvalue));		
		org.mmbase.bridge.NodeList result=manager.getList(query);
		NodeIterator i=result.nodeIterator();
		if (i.hasNext()) {
			org.mmbase.bridge.Node n=(org.mmbase.bridge.Node)i.nextNode();
			return n.getStringValue(externfield);
		}
	}
	return null;
   }

   public void init(Forum forum) {
	this.forum = forum;
    	NodeManager nodemanager = ForumManager.getCloud().getNodeManager(extern);
	MMBase.getMMBase().addLocalObserver(extern, this);
        NodeQuery query = nodemanager.createQuery();
        org.mmbase.bridge.NodeList result = nodemanager.getList(query);
        NodeIterator i = result.nodeIterator();
        while (i.hasNext()) {
            org.mmbase.bridge.Node node = (org.mmbase.bridge.Node) i.nextNode();
	    // kind of a weird way but fast result until i think of a better
	    // way to make this more flexible
	    if (object.equals("posters")) {
		// mapping on account field
		if (key.equals("account")) {
			Poster po = forum.getPoster(node.getStringValue(externkey));	
			if (po!=null) {
				if (field.equals("password")) {
					po.setAliasedPassword(node.getStringValue(externfield));
				} else if (field.equals("firstname")) {
					po.setAliasedFirstName(node.getStringValue(externfield));
				} else if (field.equals("lastname")) {
					po.setAliasedLastName(node.getStringValue(externfield));
				}
			}
		}
	    }
	}
   }

    public boolean nodeChanged(String machine, String number, String builder, String ctype) {
	//log.info("c="+ctype+" b="+builder+" number="+number);
	if (builder.equals(extern) && ctype.equals("c")) {
            org.mmbase.bridge.Node node = ForumManager.getCloud().getNode(number);
	    if (object.equals("posters")) {
		// mapping on account field
		if (key.equals("account")) {
			Poster po = forum.getPoster(node.getStringValue(externkey));	
			if (po!=null) {
				if (field.equals("password")) {
					po.setAliasedPassword(node.getStringValue(externfield));
				} else if (field.equals("firstname")) {
					po.setAliasedFirstName(node.getStringValue(externfield));
				} else if (field.equals("lastname")) {
					po.setAliasedLastName(node.getStringValue(externfield));
				}
			}
		}
	    }
        }
	return true;
    }

    public boolean nodeRemoteChanged(String machine, String number, String builder, String ctype) {
	return nodeChanged(machine,number,builder,ctype);
    }

    public boolean nodeLocalChanged(String machine, String number, String builder, String ctype) {
	return nodeChanged(machine,number,builder,ctype);
     }

}
