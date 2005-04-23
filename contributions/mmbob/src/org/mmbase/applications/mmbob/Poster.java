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

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 */
public class Poster {

    // logger
    static private Logger log = Logging.getLoggerInstance(Poster.class);

    private int id, postcount, sessionstart, lastsessionend, state;
    private int quotanumber,quotaused;
    private int avatar = 0;
    private int lastseen = 0;
    private int firstlogin = -1;
    private String account,firstname, lastname, email, level, location, gender, password;
    //private Node node;
    private Forum parent;
    private HashMap mailboxes;
    private HashMap seenthreads = new HashMap();

    private static final int STATE_ACTIVE = 0;
    private static final int STATE_DISABLED = 1;

    /**
     * Contructor
     *
     * @param node   poster Node
     * @param parent Forum that the poster belongs to
     */
    public Poster(Node node, Forum parent,boolean prefixwanted) {
	String prefix="";
	if (prefixwanted) prefix = "posters.";
	this.quotanumber=10;
	this.quotaused=-1;
        this.parent = parent;
        //this.node = node;
	id = node.getIntValue(prefix+"number");
	state = -1;
	account = node.getStringValue(prefix+"account");
	firstname = node.getStringValue(prefix+"firstname");
	lastname = node.getStringValue(prefix+"lastname");
	postcount = node.getIntValue(prefix+"postcount");
	level = node.getStringValue(prefix+"level");
	location = node.getStringValue(prefix+"location");
	gender = node.getStringValue(prefix+"gender");
	firstlogin = node.getIntValue(prefix+"firstlogin");
	lastsessionend = node.getIntValue(prefix+"lastseen");

	/*
        if (firstlogin == -1) {
            node.setIntValue("firstlogin", ((int) (System.currentTimeMillis() / 1000)));
            syncNode(ForumManager.FASTSYNC);
        }
	*/
	lastseen = lastsessionend;
        sessionstart = (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * increases postcount for this poster
     */
    public void addPostCount() {
        postcount++;
        syncNode(ForumManager.FASTSYNC);
    }

    /**
     * set the id (MMBase object number) for this poster
     *
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * set the firstname for this poster
     *
     * @param firstname
     */
    public void setFirstName(String firstname) {
        this.firstname = firstname;
    }

    /**
     * set the lastname for this poster
     *
     * @param lastname
     */
    public void setLastName(String lastname) {
        this.lastname = lastname;
    }

    /**
     * set the email-address for this poster
     *
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * set the gender for this poster
     *
     * @param gender
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * set the location for this poster
     *
     * @param location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * set the number of posts for this poster
     *
     * @param postcount Number of posts
     */
    public void setPostCount(int postcount) {
        this.postcount = postcount;
    }

    /**
     * get accountname / nick from this poster
     *
     * @return accountname / nick
     */
    public String getAccount() {
        //return getAliased("account");
	return account;
    }

    /**
     * get the MMBase objectnumber of the avatar (images-object)
     *
     * @return
     */
    public int getAvatar() {
        if (avatar == 0) readImages();
        return avatar;
    }

    /**
     * get the Emailaddress of the poster
     *
     * @return Emailaddress
     */
    public String getEmail() {
        return email;
    }

    /**
     * determine if the poster has viewed this thread
     *
     * @param id           MMBase object number of the thread to
     * @param lastposttime Date/time (Epoch) of the last post in the thread
     * @return <code>true</code> if the thread was viewed by this poster, <code>false</code> if it wasn't. Also returns <code>false</code> if the postthread contains new post(s) since the poster last viewed this thread.
     */
    public boolean viewedThread(int id, Integer lastposttime) {
        Integer time = (Integer) seenthreads.get(new Integer(id));
        if (time != null) {
            if (lastposttime.equals(time)) {
                return true;
            }
        }
        return false;
    }

    /**
     * add the MMbase threadid to the HashMap of seen threads.
     *
     * @param t MMBase objectnumber of the thread
     */
    public void seenThread(PostThread t) {
        Integer tid = new Integer(t.getId());
        seenthreads.put(tid, new Integer(t.getLastPostTime()));
    }

    /**
     * get the firstname of the poster
     *
     * @return firstname
     */
    public String getFirstName() {
        return getAliased("firstname");
    }

   public String getPassword() {
        return getAliased("password");
   }

   public void setPassword(String password) {
       org.mmbase.util.transformers.MD5 md5 = new org.mmbase.util.transformers.MD5();
       log.debug("going to md5 this password: " + password);
       this.password = md5.transform(password);
       log.debug("result of the md5: " + this.password);
   }


    /**
     * get the lastname of the poster
     *
     * @return lastname
     */
    public String getLastName() {
        return getAliased("lastname");
    }

    /**
     * get the number of posts of this poster
     *
     * @return number of posts
     */
    public int getPostCount() {
        return postcount;
    }

    /**
     * get the level of the poster
     *
     * @return level
     */
    public String getLevel() {
        return level;
    }

    /**
     * get the gender of the poster
     *
     * @return gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * get the location of the poster
     *
     * @return location
     */
    public String getLocation() {
        return location;
    }

    /**
     * get the date/time (epoch) of the first login of the poster
     *
     * @return date/time (epoch)
     */
    public int getFirstLogin() {
        return firstlogin;
    }

    /**
     * get the date/time (epoch) when the poster was last seen
     *
     * @return date/time (epoch)
     */
    public int getLastSeen() {
        return lastseen;
    }

    /**
     * get the date/time (epoch) when the poster started this session
     *
     * @return date/time (epoch)
     */
    public int getSessionStart() {
        return sessionstart;
    }

    /**
     * get the date/time (epoch) when the poster ended the last session
     *
     * @return date/time (epoch)
     */
    public int getLastSessionEnd() {
        return lastsessionend;
    }

    /**
     * get the MMBase objectnumber of the poster
     *
     * @return MMBase objectnumber of the poster
     */
    public int getId() {
        return id;
    }

    /**
     * set the node for the poster
     *
     * @param node
     */
	/*
    public void setNode(Node node) {
        this.node = node;
    }
	*/

    /**
     * get a node for a poster
     *
     * @return poster
     */
    public Node getNode() {
        Node node = ForumManager.getCloud().getNode(id);
        return node;
    }

    /**
     * Add the poster-node to the given syncQueue
     *
     * @param queue syncQueue that must be used
     */
    private void syncNode(int queue) {
        Node node = ForumManager.getCloud().getNode(id);
        node.setIntValue("postcount", postcount);
        node.setStringValue("level", level);
        node.setStringValue("gender", gender);
        node.setStringValue("location", location);
        ForumManager.syncNode(node, queue);
    }

    /**
     * update "lastseen" for the poster, and add the posternode to the syncQueue
     */
    public void signalSeen() {
        int oldtime = lastseen;
        int onlinetime = ((int) (System.currentTimeMillis() / 1000)) - (parent.getPosterExpireTime());

        if (oldtime < onlinetime) {
            parent.newPosterOnline(this);
        }
        Node node = ForumManager.getCloud().getNode(id);
        lastseen =  (int) ((System.currentTimeMillis() / 1000));
        node.setIntValue("lastseen", lastseen);
        ForumManager.syncNode(node, ForumManager.SLOWSYNC);
    }

    /**
     * Save the poster, and add the node to the syncQueue
     */
    public void savePoster() {
        Node node = ForumManager.getCloud().getNode(id);	
        node.setValue("firstname", firstname);
        node.setValue("lastname", lastname);
        node.setValue("email", email);
        node.setValue("gender", gender);
        node.setValue("location", location);
        node.setValue("password",password);
        readImages();
        ForumManager.syncNode(node, ForumManager.FASTSYNC);
    }

    /**
     * Set the most recently related image as avatar
     */
    private void readImages() {
        if (avatar == 0) avatar = -1;
        Node node = ForumManager.getCloud().getNode(id);
        if (node != null) {
            int oldrelnumber = -1;
            RelationIterator i = node.getRelations("rolerel", "images").relationIterator();
            while (i.hasNext()) {
                Relation rel = i.nextRelation();
                Node p = null;
                if (rel.getSource().getNumber() == node.getNumber()) {
                    p = rel.getDestination();
                } else {
                    p = rel.getSource();
                }
                if (rel.getNumber() > oldrelnumber) {
                    oldrelnumber = rel.getNumber();
                    avatar = p.getNumber();
                }
            }
        }
    }

    /**
     * Signal that the profile of the poster has been updated and re-read the (avatar) images
     *
     * @return <code>true</code> if this method is called
     */
    public boolean profileUpdated() {
        readImages();
        return true;
    }

    /**
     * remove the poster
     *
     * @return <code>true</code> if this method is called
     */
    public boolean remove() {
        Node node = ForumManager.getCloud().getNode(id);
        log.debug("going to remove poster: " + node.getNumber());
        removeForeignKeys(ForumManager.getCloud().getNodeManager("postareas"),"lastposternumber");
        removeForeignKeys(ForumManager.getCloud().getNodeManager("postthreads"),"lastposternumber");
        removeForeignKeys(ForumManager.getCloud().getNodeManager("forums"),"lastposternumber");
        removeForeignKeys(ForumManager.getCloud().getNodeManager("postings"),"posternumber");
        node.delete(true);
        parent.childRemoved(this);
        return true;
    }

    private void removeForeignKeys(NodeManager nodeManager, String fieldname) {
        //check if nodenumber is somewhere referenced as a foreignkey
        Node node = ForumManager.getCloud().getNode(id);
        NodeList nodeList = nodeManager.getList(fieldname +"="+node.getNumber(),null,null);
        log.debug("found: " + nodeList);
        NodeIterator it = nodeList.nodeIterator();
        Node tempNode;
        while (it.hasNext()) {
            tempNode = (Node)it.next();
            tempNode.setValue(fieldname,"");
            tempNode.commit();
        }
    }

    /**
     * disable the poster
     *
     * @return <code>true</code> if this method is called
     */
    public boolean disable() {
        this.state = STATE_DISABLED;
        Node node = ForumManager.getCloud().getNode(id);
        node.setIntValue("state",this.state);
        ForumManager.syncNode(node, ForumManager.FASTSYNC);
        return true;
    }

   /**
     * enable the poster
     *
     * @return <code>true</code> if this method is called
     */
    public boolean enable() {
        this.state = STATE_ACTIVE;
        Node node = ForumManager.getCloud().getNode(id);
        node.setIntValue("state",this.state);
        ForumManager.syncNode(node, ForumManager.FASTSYNC);
        return true;
    }

   /**
     * Check if the poster is blocked
     *
     * @return <code>true</code> if this method is called
     */
    public boolean isBlocked() {
       if (this.state == STATE_DISABLED) return true;
       return false;
    }


    /**
     * get the poster's mailbox by the  name
     *
     * @param name
     * @return the mailbox
     */
    public Mailbox getMailbox(String name) {
        if (mailboxes == null) readMailboxes();
        Object o = mailboxes.get(name);
        return (Mailbox) o;
    }

    /**
     * remove the poster's mailbox by the name
     *
     * @param name
     * @return <code>true</code> if the remove action was successfull, <code>false</code> if it wasn't.
     */
    public boolean removeMailbox(String name) {
        Mailbox m = getMailbox(name);
        if (!m.remove()) return false;
        mailboxes.remove(name);
        return true;
    }

    /**
     * Get the poster's mailboxes
     *
     * @ return All the poster's mailboxes
     */
    private void readMailboxes() {
        mailboxes = new HashMap();
        Node node = ForumManager.getCloud().getNode(id);
        if (node != null) {
            RelationIterator i = node.getRelations("posmboxrel", "forummessagebox").relationIterator();
            while (i.hasNext()) {
                Relation rel = i.nextRelation();
                Node p = null;
                if (rel.getSource().getNumber() == node.getNumber()) {
                    p = rel.getDestination();
                } else {
                    p = rel.getSource();
                }
                Mailbox mailbox = new Mailbox(p, this);
                mailboxes.put(mailbox.getName(), mailbox);
            }
        }
       calcMailboxQuota();
    }

    /**
     * Add a new mailbox for the poster
     *
     * @param name           name of the mailbox
     * @param description    description of the mailbox
     * @param editstate      ToDo: doc
     * @param maxmessages    maximum number of messages for the mailbox
     * @param maxsize        maximum size of the mailbox in
     * @param carboncopymode ToDo: doc
     * @param pos            ToDo: doc
     * @return The newly created MailBox. <code>null</code> if the creation of the mailbox didn't succeed.
     */
    public Mailbox addMailbox(String name, String description, int editstate, int maxmessages, int maxsize, int carboncopymode, int pos) {
        NodeManager nm = ForumManager.getCloud().getNodeManager("forummessagebox");
        if (nm != null) {
            Node mnode = nm.createNode();
            mnode.setStringValue("name", name);
            mnode.setStringValue("description", description);
            mnode.setIntValue("editstate", editstate);
            mnode.setIntValue("maxmessages", maxmessages);
            mnode.setIntValue("maxsize", maxsize);
            mnode.setIntValue("carboncopymode", carboncopymode);
            mnode.commit();

            Node node = ForumManager.getCloud().getNode(id);
            RelationManager rm = ForumManager.getCloud().getRelationManager("posters", "forummessagebox", "posmboxrel");
            if (rm != null) {
                Node rel = rm.createRelation(node, mnode);
                // rel.setIntValue("pos", 1); // little weird, daniel
                rel.commit();
                Mailbox newbox = new Mailbox(mnode, this);
                mailboxes.put(name, newbox);
                return newbox;
            } else {
                log.error("Forum can't load relation nodemanager posters/forummessagebox/posmboxrel");
            }
        } else {
            log.error("Forum can't load forummessagebox nodemanager");
        }
        return null;
    }

   /** 
   * get aliases version of this field
   */
   public String getAliased(String key) {
        Node node = ForumManager.getCloud().getNode(id);
        //long start = System.currentTimeMillis();
        String value = parent.getAliased(node,"posters."+key);
        if (value==null)  {
                value=node.getStringValue(key);
        }
        //long end = System.currentTimeMillis();
        //log.info("getAlias Speed = "+(end-start)+"ms");
        return value;
   }

   public boolean isQuotaReached() {
	if (quotaused<quotanumber) {
		return true;
	}
	return false;
   }

   public int getQuotaUsedNumber() {
	return quotaused;
   }

   public int getQuotaNumber() {
	return quotanumber;
   }

   public void mailboxChanged(Mailbox mb) {
       // check on quota
       calcMailboxQuota();
   }

   private void calcMailboxQuota() {
       quotaused = 0;
       Iterator i=mailboxes.values().iterator();
       while (i.hasNext()) {
                Mailbox m = (Mailbox)i.next();
                quotaused +=m.getMessageCount();
       }
  }
   	

}
