/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
/* 
CREDITS:
    Contributors:  Mathias Bogaert
    @author Ceki G&uuml;lc&uuml;    
    changed licence from apache 1.1 to current licence    
*/

package org.mmbase.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import org.mmbase.util.logging.*;

/**
 *  This will run as a thread afer it has been started, it will check every interval if one
 *  of it's files has been changed.
 *  When one of them has been changed, the OnChange method will be called, with the file witch 
 *  was changed. After that the thread will stop.
 *  To stop a running thread, call the method exit();
 *
 *  Example:
    <code>
    	class FooFileWatcher extends FileWatcher {

            public FooFileWatcher() {
                super(true); // true: keep reading.
            }

	    public void onChange(File file) {
		System.out.println(file.getAbsolutePath());
	    }
    	}
	    
	// create new instance
    	FooFileWatcher watcher = new FooFileWatcher();
	// set inteval
	watcher.setDelay(10 * 1000);
	watcher.add(new File("/tmp/foo.txt"));
	watcher.start();
	watcher.add(new File("/tmp/foo.txt"));
	wait(100*1000);
	watcher.exit();
    </code>
 *
 * @author Ceki G&uuml;lc&uuml;    
 * @author Eduard Witteveen
 * @author Michiel Meeuwissen
 * @since  MMBase-1.4
 */
public abstract class FileWatcher extends Thread {
    static Logger log = Logging.getLoggerInstance(FileWatcher.class.getName());

    /**
     * This class is used by init of logging system.
     * After configuration of logging, logging must be reinitialized.
     */
    public static void reinitLogger() {
        log = Logging.getLoggerInstance(FileWatcher.class.getName());
    }

    private class FileEntry {
    	// static final Logger log = Logging.getLoggerInstance(FileWatcher.class.getName());
    	private long lastModified; 
    	private File file;
        	
	public FileEntry(File file) {
	    if(file == null) {
	    	String msg = "file was null";
	    	// log.error(msg);
	    	throw new RuntimeException(msg);	    
	    }
	    
    	    if(!file.exists()) {
	    	String msg = "file :"  + file.getAbsolutePath() + " did not exist";
	    	// log.error(msg);
	    	throw new RuntimeException(msg);	    
	    }
	    this.file = file;
	    lastModified = file.lastModified();
	}
	
	public boolean changed() {
	    return (lastModified < file.lastModified());
	}

        /**
         * Call if changes were treated.
         */
        public void updated() {
            lastModified = file.lastModified();
        }

    	public File getFile() {
	    return file;
	}	
        public String toString() {
            return file.toString() + ":" + lastModified;
        }
        public boolean equals(Object o) {
            if (o instanceof FileEntry) {
                FileEntry fe = (FileEntry) o;
                return file.equals(fe.file);
            } else if (o instanceof File) {
                return file.equals((File) o);
            }
            return false;
        }
        public int hashCode() {
            return file.hashCode();
        }
        
    }

    private Set files       = new HashSet();
    private Set removeFiles = new HashSet();

    /**
     *	The default delay between every file modification check, set to 60
     *  seconds.  
     */
    static final public long DEFAULT_DELAY = 60000; 
    
    /**
     * The delay to observe between every check. By default set {@link
     * #DEFAULT_DELAY}. 
     */
    private long delay = DEFAULT_DELAY; 
    private boolean stop = false;
    private boolean continueAfterChange = false;
  
    protected FileWatcher() {
    	// make it end when parent treath ends..
    	setDaemon(true);
    }

    protected FileWatcher(boolean c) {
    	// make it end when parent treath ends..
        continueAfterChange = c;
    	setDaemon(true);
    }

    /**
     * Put here the stuff that has to be executed, when a file has been changed.
     * @param file The file that was changed..
     */
    abstract protected void onChange(File file);

    /**
     * Set the delay to observe between each check of the file changes.
     */
    public void setDelay(long delay) {
    	this.delay = delay;
    }
    
    /**
     * Add's a file to be checked...
     * @param file The file which has to be monitored..
     * @throws RuntimeException If file is null or does not exist.
     */
    public void add(File file) {
    	FileEntry fe = new FileEntry(file);
    	synchronized(this) {
	    files.add(fe);
            if (removeFiles.remove(fe)) log.service("Canceling removal from filewatcher " + fe); 
	}
    }

    /**
     * Wether the file is being watched or not.
     * @param the file to be checked.
     * @since MMBase-1.6
     */
    public boolean contains(File file) {
        return files.contains(new FileEntry(file));
    }

    /**
     * Remove file from the watch-list
     */
    public void remove(File file) {
        synchronized(this) {
            removeFiles.add(file);
        }
    }

    /**
     * Add's a file to be checked...
     *@param file The file which has to be monitored..
     */
    public void exit() {
    	synchronized(this) {
    	    stop = true;
	}
    }

    /**
     * Shows the 'contents' of the filewatcher. It shows a list of files/last modified timestamps.
     */
    public String toString() {
        return files.toString();
    }
    
    /**
     * Looks if a file has changed. If it is, the 'onChance' for this file is called.
     *
     * Before doing so, it removes the files which were requested for
     * removal from the watchlist.
     *
     */
    private boolean changed() {
    	synchronized(this) {
	    Iterator i = files.iterator();
	    while(i.hasNext()) {
	    	FileEntry fe = (FileEntry) i.next();       
		if(fe.changed()) {
		    log.service("the file :" + fe.getFile().getAbsolutePath() + " has changed.");
                    try {
                        onChange(fe.getFile());
                    } catch (Throwable e) {
                        log.warn("onChange of " + fe.getFile().getName() + " lead to exception:");
                        log.warn(Logging.stackTrace(e));
                    }
                    if (continueAfterChange) {                        
                        fe.updated(); // onChange was called now, it can be marked up-to-date again
                    } else { // 
                        return true; // stop watching
                    }
		}
	    }
	}
	return false;
    }
    
    private void removeFiles() {
        log.debug("Start");
        synchronized(this) {
            // remove files if necessary
            Iterator ri = removeFiles.iterator();
            while (ri.hasNext()) {    
                File f = (File) ri.next();
                FileEntry found = null;
                // search the file
                Iterator i = files.iterator();
                while (i.hasNext()) {
                    FileEntry fe = (FileEntry) i.next();
                    if (fe.getFile().equals(f)) {
                        found = fe;
                        break;
                    }
                }
                if (found != null) {
                    files.remove(found);
                    log.service("Removed " + found + " from watchlist");
                }
            }
            removeFiles.clear();
        }
        log.debug("End");
    }

    /**
     * Looks if we have to stop
     */
    private boolean mustStop(){
    	synchronized(this) {
	    return stop;
	}
    }

    /**
     *  Main loop, will repeat every amount of time.
     *	It will stop, when either a file has been changed, or exit() has been called
     */
    public void run() {
    	do {
    	    try {
    	    	if(log.isDebugEnabled()) { 
                    log.trace("Filewatcher will sleep for : " + delay / 1000 + " s. " + 
                              "Currently watching: " + toString());
                }
	    	Thread.currentThread().sleep(delay);
                removeFiles();
      	    } catch(InterruptedException e) {
	    	// no interruption expected
      	    }
	// when we found a change, we exit..
	} while (!mustStop() && !changed());
    }
}
