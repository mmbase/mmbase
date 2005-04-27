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
import org.mmbase.cache.*;
import org.mmbase.bridge.implementation.*;
import org.mmbase.storage.search.*;
import org.mmbase.storage.search.implementation.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.*;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @author Daniel Ockeloen
 */
public class PostArea {

    // logger
    static private Logger log = Logging.getLoggerInstance(PostArea.class);

    private int id;
    private Forum parent;
    private Hashtable moderators = new Hashtable();
    private String moderatorsline;
    private Vector postthreads = null;
    private Hashtable namecache = new Hashtable();
    private boolean firstcachecall = true;
    private PostAreaConfig config;

    private int viewcount;
    private int postcount;
    private int postthreadcount;
    private int lastposttime;
    private int lastpostnumber;
    private int lastposternumber;
    private int numberofpinned = 0;
    private String name;
    private String description;
    private String lastposter;
    private String lastpostsubject;

    /**
     * Constructor
     * @param parent forum
     * @param node postarea
     */
    public PostArea(Forum parent, Node node) {
        this.parent = parent;
	this.id = node.getNumber();
        name = node.getStringValue("name");
        description = node.getStringValue("description");

	config  = parent.getPostAreaConfig(getName());

        this.viewcount = node.getIntValue("viewcount");
        if (viewcount == -1) viewcount = 0;
        this.postcount = node.getIntValue("postcount");
        if (postcount == -1) postcount = 0;
        this.postthreadcount = node.getIntValue("postthreadcount");
        if (postthreadcount == -1) postthreadcount = 0;

        this.lastpostsubject = node.getStringValue("c_lastpostsubject");
        this.lastposter = node.getStringValue("c_lastposter");
        this.lastposttime = node.getIntValue("c_lastposttime");
        this.lastpostnumber=node.getIntValue("lastpostnumber");
        this.lastposternumber=node.getIntValue("lastposternumber");

        readRoles();
    }

    /**
     * set the MMBase objectnumber for the postarea
     * @param id MMBase objectnumber
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * set the node for the postarea
     * @param node postarea
     */
	/*
    public void setNode(Node node) {
        this.node = node;
    }
	*/

    /**
     * set the name for the postarea
     * @param name postareaname
     */
    public void setName(String name) {
        this.name = name;
        syncNode(ForumManager.FASTSYNC);

    }

    /**
     * set a description for the postarea
     * @param description postareadescription
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * get the name of the postarea
     * @return postareaname
     */
    public String getName() {
        return name;
    }

    /**
     * get the description of the postarea
     * @return postareadescription
     */
    public String getDescription() {
        return description;
    }

    /**
     * get the MMBase objectnumber of the postarea
     * @return MMBase objectnumber
     */
    public int getId() {
        return id;
    }

    /**
     * get the number of postthreads of the postarea
     * @return number of postthreads
     */
    public int getPostThreadCount() {
        return postthreadcount;
    }

    /**
     * get the average number of posts per postthread
     * @return average number of posts per postthread
     */
    public int getPostThreadCountAvg() {
        if (postthreadcount == 0) return 0;
        return postcount / postthreadcount;
    }

    public int getPostThreadLoadedCount() {
        if (postthreads == null) {
		return 0;
	} else {
		int count = 0;
        	Iterator i = postthreads.iterator();
		while (i.hasNext()) {
			PostThread pt = (PostThread)i.next();
			if (pt.isLoaded()) count++;
		}
		return count;
	}
    }


    public int getPostingsLoadedCount() {
        if (postthreads == null) {
		return 0;
	} else {
		int count = 0;
        	Iterator i = postthreads.iterator();
		while (i.hasNext()) {
			PostThread pt = (PostThread)i.next();
			if (pt.isLoaded()) count+=pt.getPostCount();
		}
		return count;
	}
    }


    public int getMemorySize() {
        if (postthreads == null) {
		return 0;
	} else {
		int size = 0;
        	Iterator i = postthreads.iterator();
		while (i.hasNext()) {
			PostThread pt = (PostThread)i.next();
			size += pt.getMemorySize();
		}
		return size;
	}
    }



    /**
     * get the number of pages for this postarea
     * @param pagesize maximum number of postthreads per page
     * @return the number of pages for this postarea
     */
    public int getPageCount(int pagesize) {
        int pagecount = postthreadcount / pagesize;
        if ((pagecount * pagesize) != postthreadcount) pagecount++;
        return pagecount;
    }

    /**
     * get the number of posts in this postarea
     * @return number of posts
     */
    public int getPostCount() {
        return postcount;
    }

    /**
     * get the number of views in this postarea
     * @return number of views
     */
    public int getViewCount() {
        return viewcount;
    }

    /**
     * get the accountname/nick of the last poster in the postarea
     * @return accountname/nick
     */
    public String getLastPoster() {
        return lastposter;
    }

    /**
     * get the last poster in the postarea
     * @return last poster
     */
   public int getLastPosterNumber() {
        return lastposternumber;
   }

    /**
     * get the last post in the postarea
     * @return postnumber
     */
   public int getLastPostNumber() {
        return lastpostnumber;
   }

    /**
     * get the date/time of the last post in the postarea
     * @return date/time of the last post
     */
    public int getLastPostTime() {
        return lastposttime;
    }

    /**
     * get the subject of the last post in the postarea
     * @return subject of the last post
     */
    public String getLastSubject() {
        return lastpostsubject;
    }

    /**
     * get all the postthreads in the postarea
     * @return all the postthreads
     */
    public Enumeration getPostThreads() {
        if (postthreads == null) readPostThreads();
        return postthreads.elements();
    }

    /**
     * get an iterator of the postthreads in the postarea
     * @param page which page in the sequence
     * @param pagecount maximum number of PostThreads on the page
     * @return postthreads
     */
    public Iterator getPostThreads(int page, int pagecount) {
        if (postthreads == null) readPostThreads();

        // get the range we want
        int start = (page - 1) * pagecount;
        int end = page * pagecount;
        if (end > postthreadcount) {
            end = postthreads.size();
        }
        log.debug("START=" + start + " " + end + " " + postthreads.size());
        List result = postthreads.subList(start, end);

        return result.iterator();
    }

    /**
     * get a postthread by it's MMbase Objectnumber
     * @param id MMbase Objectnumber
     * @return postthread
     */
    public PostThread getPostThread(String id) {
        if (postthreads == null) readPostThreads();
        Object o = namecache.get(id);
        if (o != null) {
            return (PostThread) o;
        }
        return null;
    }

    /**
     * get a (html) navigationline to "walk through" the different pages of PostThreads for a postarea
     * @param baseurl url
     * @param page which page in the sequence
     * @param pagesize maximum number of postthreads to be shown on one page
     * @param cssclass cssclass to be used to display the navigationline
     * @return navigationline in html
     */
    public String getNavigationLine(String baseurl, int page, int pagesize, String cssclass) {
        int f = parent.getId();
        int a = getId();
        if (cssclass!=null && !cssclass.equals("")) {
            cssclass = " class=\"" + cssclass + "\"";
        }

        // weird way must be a better way for pagecount
        int pagecount = postthreadcount / pagesize;
        if ((pagecount * pagesize) != postthreadcount) pagecount++;

        // if only one page no nav line is needed
        if (pagecount == 1) return "";


        int c = page - 1;
        if (c < 1) c = 1;
        int n = page + 1;
        if (n > pagecount) n = pagecount;
        String result = "<a href=\"" + baseurl + "?forumid=" + f + "&postareaid=" + a + "&page=" + c + "\"" + cssclass + ">&lt</a>";
        for (int i = 1; i <= pagecount; i++) {
            result += " <a href=\"" + baseurl + "?forumid=" + f + "&postareaid=" + a + "&page=" + i + "\"" + cssclass + ">";
            if (i == page) {
                result += "[" + i + "]";
            } else {
                result += "" + i;
            }
            result += "</a>";
        }
        result += " <a href=\"" + baseurl + "?forumid=" + f + "&postareaid=" + a + "&page=" + n + "\"" + cssclass + ">&gt</a>";
        return result;
    }

    /**
     * get the moderators of the postarea
     * @return moderators
     */
    public Enumeration getModerators() {
        return moderators.elements();
    }

    /**
     * get all posters that are no moderator of the postarea
     * @return non-moderators
     */
    public Enumeration getNonModerators() {
        Vector result = new Vector();
        Enumeration e = parent.getPosters();
        while (e.hasMoreElements()) {
            Poster p = (Poster) e.nextElement();
            if (!isModerator(p.getAccount())) {
                result.add(p);
            }
        }
        return result.elements();
    }

    /**
     * determine if the given accountname/nick is a moderator of this postarea
     * @param account accountname/nick to be evaluated
     * @return <code>true</code> if the account is moderator. Also <code>true</code> if the account is administrator of the parent forum.
     */
    public boolean isModerator(String account) {
        // check if he a admin then asign him
        // the moderators role too
        if (parent.isAdministrator(account)) return true;
        return moderators.containsKey(account);
    }

    /**
     * remove the moderator status of the poster regarding this postarea
     * @param mp Poster with moderator status
     * @return Always <code>false</code>
     */
    public boolean removeModerator(Poster mp) {
        if (isModerator(mp.getAccount())) {
            Node node = ForumManager.getCloud().getNode(id);
            RelationIterator i = node.getRelations("rolerel", "posters").relationIterator();
            while (i.hasNext()) {
                Relation rel = i.nextRelation();
                String role = rel.getStringValue("role");
                if (role.equals("moderator")) {
                    Node p = null;
                    if (rel.getSource().getNumber() == node.getNumber()) {
                        p = rel.getDestination();
                    } else {
                        p = rel.getSource();
                    }
                    if (p != null && p.getNumber() == mp.getId()) {
                        rel.delete();
                        moderators.remove(mp.getAccount());
                        moderatorsline = null;
                    }
                }
            }
        }
        return false;
    }

    /**
     * add moderator status to given poster for this postarea
     * @param mp Poster
     * @return <code>true</code> if the action succeeded
     */
    public boolean addModerator(Poster mp) {
        if (isModerator(mp.getAccount())) return true;
        RelationManager rm = ForumManager.getCloud().getRelationManager("postareas", "posters", "rolerel");
        if (rm != null) {
            Node node = ForumManager.getCloud().getNode(id);
            Node rel = rm.createRelation(node, mp.getNode());
            rel.setStringValue("role", "moderator");
            rel.commit();
            moderators.put(mp.getAccount(), mp);
            moderatorsline = null;
        } else {
            log.error("Forum can't load relation nodemanager postareas/posters/rolerel");
            return false;
        }
        return true;
    }

    /**
     * get the moderators line in html
     * @param baseurl
     * @return moderatorsline in html
     */
    public String getModeratorsLine(String baseurl) {
        if (moderatorsline != null) return moderatorsline;
        moderatorsline = "";
        Enumeration e = moderators.elements();
        while (e.hasMoreElements()) {
            Poster p = (Poster) e.nextElement();
            if (!moderatorsline.equals("")) moderatorsline += ",";
            if (baseurl.equals("")) {
                moderatorsline += p.getAccount();
            } else {
                moderatorsline += "<a href=\"" + baseurl + "?forumid=" + parent.getId() + "&postareaid=" + getId() + "&posterid=" + p.getId() + "\">" + p.getAccount() + "</a>";
            }
        }
        return moderatorsline;
    }

    /**
     * Fills the postthreads-Vector
     */
    private void readPostThreads() {
        postthreads = new Vector();

       	long start = System.currentTimeMillis();
	log.info("reading threads");
            NodeManager postareasmanager = ForumManager.getCloud().getNodeManager("postareas");
            NodeManager postthreadsmanager = ForumManager.getCloud().getNodeManager("postthreads");
            Query query = ForumManager.getCloud().createQuery();
            Step step1 = query.addStep(postareasmanager);
            RelationStep step2 = query.addRelationStep(postthreadsmanager);
            StepField f1 = query.addField(step1, postareasmanager.getField("number"));
            StepField f3 = query.addField(step2.getNext(), postthreadsmanager.getField("c_lastposttime"));
            query.addField(step2.getNext(), postthreadsmanager.getField("number"));
            query.addField(step2.getNext(), postthreadsmanager.getField("subject"));
            query.addField(step2.getNext(), postthreadsmanager.getField("creator"));
            query.addField(step2.getNext(), postthreadsmanager.getField("viewcount"));
            query.addField(step2.getNext(), postthreadsmanager.getField("postcount"));
            query.addField(step2.getNext(), postthreadsmanager.getField("c_lastpostsubject"));
            query.addField(step2.getNext(), postthreadsmanager.getField("c_lastposter"));
            query.addField(step2.getNext(), postthreadsmanager.getField("c_lastposttime"));
            query.addField(step2.getNext(), postthreadsmanager.getField("lastposternumber"));
            query.addField(step2.getNext(), postthreadsmanager.getField("lastpostnumber"));
            query.addField(step2.getNext(), postthreadsmanager.getField("mood"));
            query.addField(step2.getNext(), postthreadsmanager.getField("state"));
            query.addField(step2.getNext(), postthreadsmanager.getField("ttype"));

            query.addSortOrder(f3, SortOrder.ORDER_DESCENDING);

            query.setConstraint(query.createConstraint(f1, new Integer(id)));
            NodeIterator i2 = ForumManager.getCloud().getList(query).nodeIterator();
	    int newcount = 0;
	    int newthreadcount = 0;
            while (i2.hasNext()) {
                Node n2 = i2.nextNode();
                PostThread postthread = new PostThread(this, n2,true);
		newcount += postthread.getPostCount();
		newthreadcount++;
                if (postthread.getState().equals("pinned")) {
                    postthreads.add(numberofpinned, postthread);
                    numberofpinned++;
                } else {
                    postthreads.add(postthread);
                }
                namecache.put("" + n2.getValue("postthreads.number"), postthread);
            }
       	long end = System.currentTimeMillis();
	log.info("end reading threads time="+(end-start));

         // check the count number
         if (postcount!=newcount) {
                log.info("resync of postareacount : "+postcount+" "+newcount);
                postcount = newcount;
                save();
         }

         // check the threadcount number
         if (postthreadcount!=newthreadcount) {
                log.info("resync of postareathreadcount : "+postthreadcount+" "+newthreadcount);
                postthreadcount = newthreadcount;
                save();
         }


        // very raw way to zap the cache
        Cache cache = RelatedNodesCache.getCache();
        cache.clear();
        cache = NodeCache.getCache();
        cache.clear();
        cache = NodeCache.getCache();
        cache.clear();
        cache = MultilevelCache.getCache();
        cache.clear();
        cache = NodeListCache.getCache();
        cache.clear();
    }

    /**
     * increase the numberofpinned threads in the postarea
     */
    public void incPinnedCount() {
        numberofpinned++;
    }

    /**
     * decrease the numberofpinned threads in the postarea
     */
    public void decPinnedCount() {
        numberofpinned--;
    }

    /**
     * create a new postthread in the postarea
     * @param subject subject of the new postthread
     * @param poster poster that posts the new postthread
     * @param body body of the new postthread
     * @return MMbase objectnumber of the newly created postthread or -1 if the postthread-nodemanager could not be found
     */
    public int newPost(String subject, Poster poster, String body) {
        return (newPost(subject, poster.getAccount(), body));
    }

    /**
     * create a new postthread in the postarea
     * @param subject subject of the new postthread
     * @param poster accountname/nick of the poster that posts the new postthread
     * @param body body of the new postthread
     * @return MMbase objectnumber of the newly created postthread or -1 if the postthread-nodemanager could not be found
     */
    public int newPost(String subject, String poster, String body) {
        if (postthreads == null) readPostThreads();
        NodeManager nm = ForumManager.getCloud().getNodeManager("postthreads");
        if (nm != null) {
            Node ptnode = nm.createNode();
            ptnode.setStringValue("subject", subject);
            ptnode.setStringValue("creator", poster);
            ptnode.setStringValue("state", "normal");
            ptnode.setStringValue("mood", "normal");
            ptnode.setStringValue("ttype", "post");
            ptnode.setIntValue("createtime", (int) (System.currentTimeMillis() / 1000));
            ptnode.commit();
            RelationManager rm = ForumManager.getCloud().getRelationManager("postareas", "postthreads", "areathreadrel");
            if (rm != null) {
	        Node node = ForumManager.getCloud().getNode(id);
                Node rel = rm.createRelation(node, ptnode);
                rel.commit();
                PostThread postthread = new PostThread(this, ptnode,false);
		postthread.setLoaded(true);

                // now add the first 'reply' (wrong name since its not a reply)
                postthread.postReply(subject, poster, body);
                if (postthread.getState().equals("pinned")) {
                    postthreads.add(0, postthread);
                } else {
                    postthreads.add(numberofpinned, postthread);
                }
                namecache.put("" + ptnode.getNumber(), postthread);

                postthreadcount++;
                syncNode(ForumManager.FASTSYNC);
                parent.signalNewPost(this);
                return ptnode.getNumber();
            } else {
                log.error("Forum can't load relation nodemanager postareas/postthreads/areathreadrel");
            }
        } else {
            log.error("Forum can't load postthreads nodemanager");
        }
        return -1;
    }

    /**
     * signal the postarea that there is a new reply
     * @param child postthread
     */
    public void signalNewReply(PostThread child) {
        postcount++;
        lastposttime = child.getLastPostTime();
        lastposter = child.getLastPoster();
        lastpostsubject = child.getLastSubject();
        lastpostnumber=child.getLastPostNumber();
        lastposternumber=child.getLastPosterNumber();
        syncNode(ForumManager.FASTSYNC);

        resort(child);

        parent.signalNewReply(this);
    }

    /**
     * signal the postarea that a reply was removed so it cat update the postcount etc ..
     * @param child postthread
     */
    public void signalRemovedReply(PostThread child) {
        // todo: Make this configurable.
        //       uncomment this if you want to decrease the stats if a thread was removed
        //postcount--;

        if (lastposttime==child.getLastPostTime() && lastposter.equals(child.getLastPoster())) {
            lastpostsubject="removed";
        }

        lastposttime = child.getLastPostTime();
        lastposter = child.getLastPoster();

        syncNode(ForumManager.FASTSYNC);

        // signal the parent Forum that it's postcount needs to be updated
        parent.signalRemovedReply(this);
    }

    /**
     * re-add the given PostThread to the postthreads-Vector
     * @param child postthread
     */
    public void resort(PostThread child) {
        // move to the top of the queue
        if (postthreads.remove(child)) {
            if (child.getState().equals("pinned")) {
                postthreads.add(0, child);
            } else {
                postthreads.add(numberofpinned, child);
            }
        }
    }

    /**
     * get the parent forum of this postarea
     * @return Forum
     */
    public Forum getParent() {
        return parent;
    }

    /**
     * signal the postarea that the number of views has been changed
     * @param child
     */
    public void signalViewsChanged(PostThread child) {
        viewcount++;
        syncNode(ForumManager.SLOWSYNC);
        parent.signalViewsChanged(this);
    }

    /**
     * save the postarea (add it to the SyncQueue)
     * @return
     */
    public boolean save() {
        syncNode(ForumManager.FASTSYNC);
        return true;
    }

    /**
     * add the postarea-node to the given syncQueue
     *
     * @param queue syncQueue that must be used
     */
    private void syncNode(int queue) {
        Node node = ForumManager.getCloud().getNode(id);
        node.setStringValue("name", name);
        node.setStringValue("description", description);
        node.setIntValue("postthreadcount", postthreadcount);
        node.setIntValue("postcount", postcount);
        node.setIntValue("viewcount", viewcount);
        node.setIntValue("c_lastposttime", lastposttime);
        node.setStringValue("c_lastposter", lastposter);
        node.setStringValue("c_lastpostsubject", lastpostsubject);
        node.setIntValue("lastposternumber",lastposternumber);
        node.setIntValue("lastpostnumber",lastpostnumber);
        ForumManager.syncNode(node, queue);
    }

    /**
     * Called by constructor
     * fills the HashTable "moderators" with all known moderators for this postarea
     */
    private void readRoles() {
	    Node node = ForumManager.getCloud().getNode(id);
            RelationIterator i = node.getRelations("rolerel", "posters").relationIterator();
            while (i.hasNext()) {
                Relation rel = i.nextRelation();
                Node p = null;
                if (rel.getSource().getNumber() == node.getNumber()) {
                    p = rel.getDestination();
                } else {
                    p = rel.getSource();
                }
                String role = rel.getStringValue("role");
                if (role.equals("moderator")) {
                    Poster po = parent.getPoster(p.getNumber());
                    moderators.put(po.getAccount(), po);
                }
            }
    }

    /**
     * remove the postarea
     * @return  <code>true</code> if it succeeds, <code>false</code> if it doesn't
     */
    public boolean remove() {
        if (postthreads == null) readPostThreads();
        if (getPostThreadCount() != 0) {
            Enumeration e = postthreads.elements();
            while (e.hasMoreElements()) {
                PostThread t = (PostThread) e.nextElement();
                if (!t.remove()) {
                    log.error("Can't remove PostThread : " + t.getId());
                    return false;
                }
                postthreads.remove("" + t.getId());
            }
        }
        Node node = ForumManager.getCloud().getNode(id);
        node.delete(true);
        return true;
    }

    /**
     * remove a postthread by it's MMbase objectnumber
     * @param postthreadid MMbase objectnumber of the postthread
     * @return <code>true</code> if it succeeds, <code>false</code> if it doesn't
     */
    public boolean removePostThread(String postthreadid) {
        PostThread t = getPostThread(postthreadid);
        if (t != null) {
            postthreads.remove("" + t.getId());
            if (!t.remove()) {
                log.error("Can't remove PostThread : " + t.getId());
                return false;
            }
        }
        return true;
    }

    /**
     * remove a postthread from the postthreads and signal the
     * parent forum that Thread was removed
     *
     * @param t postthread
     */
    public void childRemoved(PostThread t) {
        // todo: Make this configurable.
        //       uncomment this if you want to decrease the stats if a thread was removed
        //postthreadcount--;

        postthreads.remove(t);
        syncNode(ForumManager.FASTSYNC);
        parent.signalRemovedPost(this);
    }

    /**
     * called to maintain the memorycaches
     */
    public void maintainMemoryCaches() {
	int ptime = ForumManager.getPreloadChangedThreadsTime();
        if (ptime!=0 && postthreads != null && firstcachecall) {
            firstcachecall = false;
            int time = (int) (System.currentTimeMillis() / 1000) - ptime;
            Enumeration e = postthreads.elements();
            while (e.hasMoreElements()) {
                PostThread t = (PostThread) e.nextElement();
                int time2 = t.getLastPostTime();
                if (time2==-1 || time2 > time) {
                    t.readPostings();
                }
            }
        }

        if (postthreads != null) {
	    int etime = ForumManager.getSwapoutUnusedThreadsTime();
	    if (etime!=0) {
            int time = (int) (System.currentTimeMillis() / 1000) - etime;
            int time4 = (int) (System.currentTimeMillis() / 1000) - ptime;
            Enumeration e = postthreads.elements();
            while (e.hasMoreElements()) {
                PostThread t = (PostThread) e.nextElement();
		if (t.isLoaded()) {
                	int time2 = t.getLastUsed();
                	int time3 = t.getLastPostTime();
	                if (time2 < time && time4 > time3) {
				t.swapOut();
                	}
		}
            }
	    }
        }
    }


   public String getGuestReadModeType() {
	if (config != null) {
		String tmp = config.getGuestReadModeType();
        	if (tmp != null) {
               	 	return tmp;
        	}
	}
        return parent.getGuestReadModeType();
   }


   public String getGuestWriteModeType() {
	if (config != null) {
		String tmp = config.getGuestWriteModeType();
        	if (tmp != null) {
               	 	return tmp;
        	}
	}
        return parent.getGuestWriteModeType();
   }
}
