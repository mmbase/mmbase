/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mmbase.module.core.*;
import org.mmbase.module.builders.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.module.gui.html.scanparser;

/**
 * File cache system.
 * This system only caches texts (it stores and retrieves strings).
 * Texts are asociated with a key, which is used both as a filename on disk and a
 * key into a memory cache.
 * While in theory it is possible to cache ANY text, this module is mainly used to store pages
 * based on their url.<br>
 * Caching is done in pools. Each pool has its own memory cache and files, and has
 * different ways to handle file caching. The pools currently supported are "PAGE" and "HENK".
 *
 * @author Daniel Ockeloen
 * @author Pierre van Rooden (javadocs)
 * @version 10 Apr 2001
 */
public class scancache extends Module implements scancacheInterface {

    /**
     * Maximum size of a pool's memory cache.
     */
    public static final int MAX_CACHE_POOL_SIZE = 200;

    // logger
    private static Logger log = Logging.getLoggerInstance(scancache.class.getName());

	private scanparser scanparser;

    /**
     * Default expiration time for a cache entry, in seconds.
     * The default is 6 hours.
     * Should be made configurable through a property in scancache.xml
     */
    private static int defaultExpireTime = 21600;

    /**
     * Contains the memory caches for the cache pools.
     * Pools are identified by name. Various pools have different ways of cache-handling.
     * The pools currently supported are 'HENK' and 'PAGE'.
     * The key to retrieve a pool is the poolname. The value returned ia a LRUHashtable,
     * configured to hold a maximum of MAX_CACHE_POOL_SIZE entries.
     */
    Hashtable pools = new Hashtable();

    /**
     * Contains the last date (as an <code>Integer</code>) a file was stored in a pool.
     * The key to retrieve the time is poolname+filekey (where a filekey is generally the file URI).
     * There is a limit to the number of values stored in this pool. This means if you have
	 * more then 4 pooltypes you have to bump that value or suffer performance degredation
     */
    LRUHashtable timepool = new LRUHashtable(MAX_CACHE_POOL_SIZE*4);

    // org.mmbas StatisticsInterface stats;

    /**
     * reference to MMBase module, used to retrieve netfiles and pagemakers builders
     * that support caching.
     */
    MMBase mmbase;
    /**
     * Determines whether the cache module is active.
     * Set by the status field in the scancache.xml configuration file.
     */
    boolean status=false;

    /**
     * The root of the cache filepath.
     * Set by the CacheRoot property in the scancache.xml configuration file.
     */
    private String cachepath="";

    /**
     * Scancache module constructor.
     */
    public scancache() {
    }

    /**
     * Event that should be triggered when the module is loaded.
     * Unused.
     */
    public void onload() {
    }

    /**
     * Event that sh*ould be triggered when the module is shut down.
     * Unused.
     */
    public void shutdown() {
    }

    /**
     * Event that should be triggered when the module is unloaded.
     * Unused.
     */
    public void unload() {
    }

    /**
     * Initializes the module.
     * Reads parameters from the scancache.xml configuration file.
     */
    public void init() {
        String statmode=getInitParameter("statmode");  // comment out

        String tmp=getInitParameter("status");
        log.debug("status "+tmp);
        if (tmp!=null && tmp.equals("active")) status=true;

        cachepath=getInitParameter("CacheRoot");
        if (cachepath==null) {
            // XXX should set cache to inactive?
            log.error("SCANCACHE -> No CacheRoot property set in the scancache.xml configuration file");
        }
        mmbase=(MMBase)getModule("MMBASEROOT");
        /* org.mmbase
        if (statmode!=null && statmode.equals("yes")) {
            stats=(StatisticsInterface)getModule("STATS");
        } else {
            stats=null;
        }
        */
        scanparser=(scanparser)getModule("SCANPARSER");
    }

    /**
     * Retrieve a file from the indicated pool's cache.
     * When using the "HENK" pool, this method performs a check on expiration using
     * the default expiration time (6 hours).
     * For cache "PAGE", this method does not check expiratio, and retrieves the data
     * from the file from disk, NOT from the memory cache.
     * @param poolname name of the cache pool, either "HENK" or "PAGE"
     * @param key URL of the page to retrieve
     * @return the page's content as a string, or <code>null</code> if no entry was found
     *     (i.e. cache was empty or poolname was invalid).
     */
    public String get(String poolName, String key,scanpage sp) {
        if (status==false) return null; // no cache when inactive
        log.debug("poolName="+poolName+" key="+key);
        if (poolName.equals("HENK")) {
            String tmp=get(poolName,key,">",sp);
            return tmp;
        } else if (poolName.equals("PAGE")) {
            return getPage(poolName,key,"");
        }
        log.error("get("+poolName+","+key+"): poolname("+poolName+") is an unknown cachetype!");
        return null;
    }

    /**
     * Retrieve a file from the indicated pool's cache.
     * When using the "HENK" pool, this method performs a check on expiration using
     * the default expiration time (6 hours).
     * For cache "PAGE", this method does not check expiration, and retrieves the data
     * from the file from disk, NOT from the memory cache.
     * @param poolname name of the cache pool, either "HENK" or "PAGE"
     * @param key URL of the page to retrieve
     * @param line contains parameters for counters, expiration time, etc, in tag-format.
     *             For this method, these options are currently only used at the mmbase.org site.
     * @return the page's content as a string, or <code>null</code> if no entry was found
     *     (i.e. cache was empty or poolname was invalid).
     */
    public String getNew(String poolName, String key,String line,scanpage sp) {
        if (status==false) return null; // no cache when inactive
        line=line.substring(0,line.indexOf('>'));
        // tagger code should be commented out...
        StringTagger tagger=new StringTagger(line);
        String counter=tagger.Value("COUNTER");
        /* org.mmbase
        log.debug("scancache -> new poolName="+poolName+" key="+key+" line="+line+" tagger="+counter+" stats="+stats);
        if (counter!=null && stats!=null) {
            stats.setCount(counter,1);
        }
        */
        if (poolName.equals("HENK")) {
            String tmp=get(poolName,key,">",sp);
            return tmp;
        } else if (poolName.equals("PAGE")) {
            return getPage(poolName,key,line);
        }
        log.error("getNew("+poolName+","+key+"): poolname("+poolName+") is an unknown cachetype!");
        return null;
    }

    /**
     * Retrieve a file from the indicated pool's cache.
     * It is first retrieved from meory. if that fails, the file is retrieved from disk.
     * This method performs a check on expiration using either the default expiration
     * time (6 hours), or the value in the line parameter.
     * @param poolname name of the cache pool, expected (but not verified) are "HENK" or "PAGE"
     * @param key URL of the page to retrieve
     * @param line the expiration value, either the expiration value in seconds or the word 'NOEXPIRE'. Due to
     *               legacy this value needs to be closed with a '>'.
     *               If the parameter is empty a default value is used.
     * @return the page's content as a string, or <code>null</code> if no entry was found
     */
    public String get(String poolName, String key, String line,scanpage sp) {
        if (status==false) return null; // no cache when inactive
        // get the interval time
        // (nothing, NOEXPIRE, or an int value)
        String tmp=line.substring(0,line.indexOf('>')).trim();
        int interval;
        if (tmp.equals("")) interval = defaultExpireTime;
        else if (tmp.toUpperCase().equals("NOEXPIRE")) interval = Integer.MAX_VALUE;
        else try {
                 interval = Integer.parseInt(tmp);
             }
             catch (NumberFormatException n) {
                 log.error("CACHE "+poolName+": Number format exception for expiration time ("+tmp+")");
                 interval = defaultExpireTime;
             }
        int now=(int)(System.currentTimeMillis()/1000);

        // get pool memory cache
        LRUHashtable pool=(LRUHashtable)pools.get(poolName);
        if (pool!=null) {
            String value=(String)pool.get(key);
            // Check expiration
            // XXX better to check value==null first...
			if (value!=null) {
	            try {
	                // get the time from memory
	                Integer tmp2=(Integer)timepool.get(poolName+key);
	                int then=tmp2.intValue();
	                log.debug("scancache -> file="+then+" now="+now+" interval="+interval);
	                if (((now-then)-interval)<0) {
	                    return value;
	                } else {
	                    log.debug("get("+poolName+","+key+","+line+"): Request is expired, return old version");
						// RICO signal page-processor
						signalProcessor(sp,key);
	                    return value;
	                }
	            } catch(Exception e) {}
			}
        }
        // not in memorycache, get directly from file instead
        fileInfo fileinfo=loadFile(poolName,key);
        if (fileinfo!=null && fileinfo.value!=null) {
            if (((now-fileinfo.time)-interval)>0) {
                log.debug("get("+poolName+","+key+","+line+"): Diskfile expired for file("+fileinfo.time+"), now("+now+") and interval("+interval+") , return old version ");
				// RICO signal page-processor
				signalProcessor(sp,key);
            }
            if (pool==null) {
                // create a new pool
                pool=new LRUHashtable(MAX_CACHE_POOL_SIZE);
                pools.put(poolName,pool);
            }
            pool.put(key,fileinfo.value); // store value in the memory cache
            timepool.put(poolName+key,new Integer(fileinfo.time)); // store expiration time
            return fileinfo.value;
        }
        return null;
    }

    /**
     * Retrieve a file from disk.
     * This method loads a file from disk and returns the contents as a string.
     * It does not use the memory cache of a poolname, nor does it check for
     * expiration of the cache.
     * Also, it does not perform updates on the memory cache.
     * @param poolname name of the cache pool (expected, but not verified is "PAGE")
     * @param key URL of the page to retrieve
     * @param line cache line options, unspecified
     * @return the page's content as a string, or <code>null</code> if no entry was found
     */
    public String getPage(String poolName, String key,String line) {
        if (status==false) return null;
        fileInfo fileinfo=loadFile(poolName,key);
        if (fileinfo!=null && fileinfo.value!=null) {
            return fileinfo.value;
        } else {
            // stuff on pagemakers... what does this do?
            /*
            if (mmbase!=null) {
                pagemakers bul=(pagemakers)mmbase.getMMObject("pagemakers");
                if (bul!=null) {
                    MMObjectNode node=bul.getNode(1452549);
                    if (node!=null) {
                        String state=node.getStringValue("state");
                        log.debug("scancache -> PAGE STATE="+state);
                        if (state!=null && state.equals("waiting")) {
                            node.setValue("info","URL=\""+key+"\" "+line);
                            node.setValue("state","newpage");
                            node.commit();
                        }
                    }
                } else {
                    log.error("scancache-> can't get pagemakers");
                }
            }
            return null;
            */
            return null;
        }
    }

    /**
     * Store a file in the indicated pool's cache (both on file and in teh memory cache).
     * Returns the old value if available.
     * When using the "HENK" pool, this method performs a check on expiration using
     * the default expiration time (6 hours).
     * For cache "PAGE", this method does not check expiration, and retrieves the data
     * from the file from disk, NOT from the memory cache.
     * @param poolname name of the cache pool, either "HENK" or "PAGE"
     * @param res reponse object for retrieving headers (used by mmbase.org?)
     *                    only needed for cachepool "PAGE"
     * @param key URL of the page to store
     * @param value the page content to store
     * @param mimeType the page's mime type, only needed for cachepool "PAGE"
     * @return the page's old content as a string, or <code>null</code> if no entry was found
     *     (i.e. cache was empty or poolname was invalid).
     */
    public String newput(String poolName,HttpServletResponse res, String key,String value, String mimeType) {
        if (status==false) return null;  // no caching if inactive
        LRUHashtable pool=(LRUHashtable)pools.get(poolName);
        if (pool==null) {
            // create a new pool
            pool=new LRUHashtable();
            pools.put(poolName,pool);
        }
        // insert the new item and save to disk
        if (poolName.equals("HENK")) {
            saveFile(poolName,key,value);
            return (String)pool.put(key,value);
        } else if (poolName.equals("PAGE")) {
            saveFile(poolName,key,value);
            // new file for asis support
            int pos=key.indexOf('?');
            String filename=key;
            if (pos!=-1) {
                filename=filename.replace('?',':');
                filename+=".asis";
            } else {
                filename+=":.asis";
            }
            // obtain and add headers
            // ----------------------
            // org.mmbase String body="Status:"+(((worker)res).getWriteHeaders()).substring(8);
            String body=getWriteHeaders(value, mimeType);
            body+=value;
            saveFile(poolName,filename,body);
            // signal to start transfer of file to mirrors
            signalNetFileSrv(filename);
            return (String)pool.put(key,value);
        }
        log.error("newPut("+poolName+",HttpServletRequest,"+key+","+value+"): poolname("+poolName+") is not a valid cache name!");
        return null;
    }

    /**
     * Store a file in the indicated pool's cache (both in file and in the memory cache).
     * Returns the old value if available.
     * @param poolname name of the cache pool, either "HENK" or "PAGE"
     * @param key URL of the page to store
     * @param value the page content to store
     * @param int cachetype only needed for cachepool "PAGE".
     *        If 0, no file transfer is performed. Otherwise the {@link NetFileSrv} builder is
     *        invoked to start the VWM that handles the transfer.
     * @param mimeType the page's mime type, only needed for cachepool "PAGE"
     * @return the page's old content as a string, or <code>null</code> if no entry was found
     *     (i.e. cache was empty or poolname was invalid).
     * @deprecated Temporary hack for solving asis problems (?). Use {@link #newput} instead.
     */
    public String newput2(String poolName,String key,String value,int cachetype, String mimeType) {
        if (status==false) return null; // no caching when inactive
        LRUHashtable pool=(LRUHashtable)pools.get(poolName);
        if (pool==null) {
            // create a new pool
            pool=new LRUHashtable();
            pools.put(poolName,pool);
        }
        log.debug("newput2("+poolName+","+key+","+value+","+cachetype+"): NEWPUT");
        // insert the new item and save to disk
        // XXX (why not call put ?)
        if (poolName.equals("HENK")) {
            saveFile(poolName,key,value);
            // also add time to timepool??
            return (String)pool.put(key,value);
        } else if (poolName.equals("PAGE")) {
            saveFile(poolName,key,value);
            // new file for asis support
            // -------------------------
            int pos=key.indexOf('?');
            String filename=key;
            if (pos!=-1) {
                filename=filename.replace('?',':');
                filename+=".asis";
            } else {
                filename+=":.asis";
            }
            // obtain and add headers
            String body=getWriteHeaders(value, mimeType);
            body+=value;
            log.debug("newput2("+poolName+","+key+","+value+","+cachetype+"): NEWPUT=SAVE");
            saveFile(poolName,filename,body);
            if (cachetype!=0) signalNetFileSrv(filename);
            return (String)pool.put(key,value);
        }
        log.error("newput2("+poolName+","+key+","+value+","+cachetype+"): poolName("+poolName+") is not a valid cachetype!");
        return null;
    }

    /**
     * Store a file in the indicated pool's cache (both on file and in the memory cache).
     * Returns the old value if available.
     * @param poolname name of the cache pool
     * @param key URL of the page to store
     * @param value the page content to store
     * @return the page's old content as a string, or <code>null</code> if no entry was found
     */
    public String put(String poolName, String key,String value) {
        if (status==false) return null; // no caching if inactive
        LRUHashtable pool=(LRUHashtable)pools.get(poolName);
        if (pool==null) {
            // create a new pool
            pool=new LRUHashtable();
            pools.put(poolName,pool);
        }
        // got pool now insert the new item and save to disk
        saveFile(poolName,key,value);
        return (String)pool.put(key,value);
    }

    public Hashtable state() {
        /*
        state.put("Hits",""+hits);
        state.put("Misses",""+miss);
        */
        return state;
    }

    /**
    * maintainance call, will be called by the admin to perform managment
    * tasks. This can be used instead of its own thread.
    */
    public void maintainance() {

    }

    /*
    void readParams() {
        String tmp=getInitParameter("MaxLines");
        if (tmp!=null) MaxLines=Integer.parseInt(tmp);
        tmp=getInitParameter("MaxSize");
        if (tmp!=null) MaxSize=Integer.parseInt(tmp)*1024;
        tmp=getInitParameter("Active");
        if (tmp!=null && (tmp.equals("yes") || tmp.equals("Yes"))) {
            active=true;
        } else {
            active=false;
        }
    }
    */

    /**
     * Retrieve a description of the module's function.
     */
    public String getModuleInfo() {
        return "This module provides cache functionality for text pages";
    }

    /**
     * Saves a file to disk.
     * The file is stored under the cache root directory, with a unique name,
     * composed of the pool name and the 'original' file name.
     * @param pool The name of the pool
     * @param filename the name of the file
     * @param value the value to store in the file
     */
    public boolean saveFile(String pool,String filename,String value) {
        log.debug("saveFile("+pool+","+filename+",length("+value.length()+" bytes): saving!");
        File sfile = new File(cachepath+pool+filename);
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(value);
            scan.flush();
            scan.close();
        } catch(Exception e) {
            // make dirs only when an exception occurs... argh
            // e.printStackTrace();
            String dname=cachepath+pool+filename;
            int pos=dname.lastIndexOf('/');
            String dirname=dname.substring(0,pos);
            File file = new File(dirname);
            try {
                if (file.mkdirs()) {
                    DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
                    scan.writeBytes(value);
                    scan.flush();
                    scan.close();
                } else {
                    log.error("scandisk cache -> making "+dirname+" failed ");
                }
            } catch (Exception f) {
                    log.error("scandisk cache -> Saving file "+filename+" failed "+f);
            }
            return false;
        }
        return true;
    }

    /**
     * loads a file to disk.
     * The file retrieved should be stored under the cache root directory, with a unique name,
     * composed of the pool name and the 'original' file name.
     * @param pool The name of the pool
     * @param filename the name of the file
     * @return the content of the file in a {@link fileInfo} object.
     */
    public fileInfo loadFile(String pool,String filename) {
        fileInfo fileinfo=new fileInfo();
        try {
            File sfile = new File(cachepath+pool+filename);
            FileInputStream scan =new FileInputStream(sfile);
            int filesize = (int)sfile.length();
            byte[] buffer=new byte[filesize];
            int len=scan.read(buffer,0,filesize);
            if (len!=-1) {
                String value=new String(buffer,0);
                fileinfo.value=value;
                fileinfo.time=(int)(sfile.lastModified()/1000);
                return fileinfo;
            }
            scan.close();
        } catch(Exception e) {
            // e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Signal the NetFileServ builder that the .asis file for a page has changed.
     * The builder then searches the responsible VWM that handles the mirrorring of the pages,
     * and activates it.
     * @param filename the .asis filename that changed
     */
    public void signalNetFileSrv(String filename) {
        log.debug("signalNetFileSrv("+filename+"): SIGNAL");
        if (mmbase!=null) {
            NetFileSrv bul=(NetFileSrv)mmbase.getMMObject("netfilesrv");
            if (bul!=null) {
                ((NetFileSrv)bul).fileChange("pages","main",filename);
            }
        } else {
            log.error("signalNetFileSrv("+filename+"): can't use NetFileSrv builder");
        }
    }

    /**
     * Returns the headers for a .asis file to be stored for a "PAGE" cache.
     * @param value page content, used to set the Content-length header.
     * @param mimeType the mimetype of the page. default (if unspecified) is text/html; iso-8859-1
     * @return the page headers as a <code>String</code>
     */
    String getWriteHeaders(String value, String mimeType) {
        if ((mimeType==null) || mimeType.equals("") || mimeType.equals("text/html"))
            mimeType = "text/html; iso-8859-1";
        String now = RFC1123.makeDate(new Date(DateSupport.currentTimeMillis()));
        String expireTime = RFC1123.makeDate(new Date(DateSupport.currentTimeMillis()+15000));
        String body="Status: 200 OK\n";
        body+="Server: OrionCache\n";  // orion cache ???
        body+="Content-type: "+mimeType+"\n";
        body+="Content-length: "+value.length()+"\n";
        body+="Expires: "+expireTime+"\n";
        body+="Date: "+now+"\n";
        body+="Cache-Control: no-cache\n";
        body+="Pragma: no-cache\n";
        body+="Last-Modified: "+now+"\n\n";
        return body;
    }

    /**
     * Removes an entry from the cache pool (both the file on disk and in the memory cache).
     * If the pool is "PAGE", the file will only be removed from the local cache,
     * not from any mirror servers.
     * @param pool name of cache pool, expected (but not verified) "HENK" or "PAGE"
     * @param key URL of the page to remove
     */
    public void remove(String poolName, String key) {
            File file = new File(cachepath + poolName + key);
            file.delete();
            LRUHashtable pool=(LRUHashtable)pools.get(poolName);
            if (pool!=null) pool.remove(key);
            timepool.remove(poolName + key);
    }

    /**
     * Returns the status of this module.
     * @return <code>true</code> if the module is active, <code>false</code> otherwise
     */
    public boolean getStatus() {
        return status;
    }

	private void signalProcessor(scanpage sp, String uri) {
		scanpage fakesp=new scanpage();
		scanparser.processPage(fakesp,uri);
	}
}
