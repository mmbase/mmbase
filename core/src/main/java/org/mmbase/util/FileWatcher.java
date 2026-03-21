/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.UtilReader;

/**
 * Original javadoc.

 *  This will schedule a job after it has been started.
 *  It will check every interval if one of it's files has been changed.
 *  When one of them has been changed, the onChange method will be called, with the file that
 *  was changed. After that the thread will stop.
 *  To stop a running thread, call the method exit();
 *
 *  Example:
 *   <code style="white-space:pre;">
 *   	class FooFileWatcher extends FileWatcher {
 *
 *           public FooFileWatcher() {
 *               super(true); // true: keep reading.
 *           }
 *
 *	    public void onChange(File file) {
 *		System.out.println(file.getAbsolutePath());
 *	    }
 *   	}
 *
 *	// create new instance
 *    	FooFileWatcher watcher = new FooFileWatcher();
 *	// set inteval
 *	watcher.setDelay(10 * 1000);
 *	watcher.add(new File("/tmp/foo.txt"));
 *	watcher.start();
 *	watcher.add(new File("/tmp/foo.txt"));
 *	wait(100*1000);
 *	watcher.exit();
 *    </code>
 *
 * Thanks to contributions by Mathias Bogaert.
 * License was changed from apache 1.1 to Mozilla.
 *
 * MMBase javadoc
 *
 * This code was originally borrowed from the log4j project (as can still be seen from the authors),
 * it was however quite heavily adapted. You are probably better of using a {@link ResourceWatcher}
 * (since MMBase 1.8), because that does not watch only files. Its implementation does of course use
 * FileWatcher, for the 'file' part of the watching.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Eduard Witteveen
 * @author Michiel Meeuwissen
 * @since  MMBase-1.4
 * @version $Id$
 */
public abstract class FileWatcher {
    private static final Logger log = Logging.getLoggerInstance(FileWatcher.class);


    /**
     *	The default delay between every file modification check, set to 60
     *  seconds.
     */
    static  public long DEFAULT_DELAY = 60000;

    /**
     * The one thread doing al the work also needs a delay.
     */
    static public long THREAD_DELAY =  10000;


    static ScheduledFuture<?> future;
    static FileWatcherRunner fileWatchers = new FileWatcherRunner();

    /**
     * @since 1.9.7
     * @return
     */
    public static Supplier<Duration> getDefaultDelay()  {
        return () -> Duration.ofMillis(DEFAULT_DELAY);

    }

    /**
     * @since 1.9.7
     * @return
     */
    public static Duration getThreadDelay()  {
        return Duration.ofMillis(THREAD_DELAY);
    }


    /**
     * @since MMBase-1.9.2
     */
    static void scheduleFileWatcherRunner() {
        if (future != null) {
            future.cancel(true);
        }
        if (THREAD_DELAY > 0) {
            future = org.mmbase.util.ThreadPools.scheduler.scheduleAtFixedRate(fileWatchers, THREAD_DELAY, THREAD_DELAY, TimeUnit.MILLISECONDS);
            org.mmbase.util.ThreadPools.identify(future, "File Watcher");
        } else {
            future = null;
        }
    }

    static {
        try {
            scheduleFileWatcherRunner();
        } catch (Throwable t) {
            log.error(t);
        }
    }


    private static final UtilReader.PropertiesMap<String> props;

    /**
     * @since MMBase-1.8
     */
    static  final Runnable watcher = new Runnable() {
            public void run() {

                try {
                    String delay = props.get("delay");
                    if (delay != null) {
                        THREAD_DELAY = Casting.toDuration(delay, () -> Duration.ofSeconds(10)).toMillis();
                        log.service("Set thread delay time to " + THREAD_DELAY);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                try {
                    String delay = props.get("defaultdelay");
                    if (delay != null) {
                        DEFAULT_DELAY = Casting.toDuration(delay, () -> Duration.ofSeconds(10)).toMillis();
                        log.service("Set thread delay time to " + DEFAULT_DELAY);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
                scheduleFileWatcherRunner();
            }
        };


    static {
        props = new UtilReader("resourcewatcher.xml", watcher).getProperties();
        watcher.run();
    }

    /**
     * @since MMBase-1.8
     */
    public static void shutdown() {
        future.cancel(true);
        fileWatchers.cancel();
        fileWatchers = null;
        log.service("Shut down file watcher thread");
    }

    /**
     * The delay to observe between every check. By default set {@link
     * #DEFAULT_DELAY}.
     */
    private LongSupplier delay = () -> DEFAULT_DELAY;

    private final Set<FileEntry> files = new LinkedHashSet<FileEntry>();
    private final Set<File> fileSet = new FileSet(); // (automaticly) wraps 'files'.
    private final Set<File> removeFiles = new HashSet<File>();
    private boolean stop = false;
    private boolean running = false;
    private boolean continueAfterChange = false;
    private long lastCheck = System.currentTimeMillis();

    protected FileWatcher() {
        this(true);
    }

    protected FileWatcher(boolean c) {
        // make it end when parent treath ends..
        continueAfterChange = c;
    }

    public void start() {
        stop = false;
        if (fileWatchers != null) {
            fileWatchers.add(this);
        }
        running = true;
    }
    /**
     * Put here the stuff that has to be executed, when a file has been changed.
     * @param file The file that was changed..
     */
    abstract public void onChange(File file);

    /**
     * Set the delay to observe between each check of the file changes.
     * @param delay The delay in milliseconds
     */

    public void setDelay(long delay) {
        this.delay = () -> delay;
        if (delay < THREAD_DELAY) {
            log.info("Delay of " + this + "  (" + delay + " ms) is smaller than the delay of the watching thread. Will not watch more often then once per " + THREAD_DELAY + " ms. Set to " + THREAD_DELAY);
            this.delay = () -> THREAD_DELAY;
        }
    }
    /**
     * @since MMBase-1.9.7
     */
    public void setDelay(LongSupplier delay) {
        this.delay = delay;
    }
    /**
     * @since MMBase-1.9.7
     */
    public void setDelay(Supplier<Duration> delay) {
        this.delay = () -> delay.get().toMillis();
    }



    /**
     * @since MMBase-1.9.2
     */
    public long getDelay() {
        return delay.getAsLong();
    }

    /**
     * @since MMBase-1.9.2
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @since MMBase-1.9.2
     */
    public boolean isContinueAfterChange() {
        return continueAfterChange;
    }

    /**
     * @since MMBase-1.9.2
     */
    public Date getLastCheck() {
        return new Date(lastCheck);
    }

    /**
     * Add's a file to be checked...
     * @param file The file which has to be monitored..
     * @throws RuntimeException If file is null
     */
    public void add(File file) {
        FileEntry fe = new FileEntry(file);
        synchronized (this) {
            files.add(fe);
            if (removeFiles.remove(fe)) {
                log.service("Canceling removal from filewatcher " + fe);
            }
        }
    }

    /**
     * Whether the file is being watched or not.
     * @param file the file to be checked.
     * @since MMBase-1.6
     */
    public boolean contains(File file) {
        return files.contains(new FileEntry(file));
    }

    /**
     * Remove file from the watch-list
     */
    public void remove(File file) {
        synchronized (this) {
            log.debug("Secheduling " + file + " from removal", new Exception());
            removeFiles.add(file);
        }
    }

    /**
     * Returns a (modifiable) Set of all files (File object) of this FileWatcher. If you change it, you change the
     * FileWatcher. The order of the Set is predictable (backed by a {@link java.util.LinkedHashSet}).
     *
     * @since MMBase-1.8.
     */
    public Set<File> getFiles() {
        return fileSet;
    }

    /**
     * Removes all files, this watcher will end up watching nothing.
     * @since MMBase-1.8
     */
    public void clear() {
        fileSet.clear();
    }

    /**
     * Stops watching.
     */
    public void exit() {
        log.debug("Exiting " + this);
        synchronized (this) {
            stop = true;
        }
    }

    /**
     * Shows the 'contents' of the filewatcher. It shows a list of files/last modified timestamps.
     */
    @Override
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
        synchronized (this) {
            for (FileEntry fe : files) {
                if (fe.changed()) {
                    log.debug("the file :" + fe.getFile().getAbsolutePath() + " has changed.");
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
        synchronized (this) {
            // remove files if necessary
            for (File f : removeFiles) {
                FileEntry found = null;
                // search the file
                for (FileEntry fe : files) {
                    if (fe.getFile().equals(f)) {
                        if (log.isDebugEnabled()) {
                            log.debug("removing file[" + fe.getFile().getName() + "]");
                        }
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
            running = false;
            notifyAll();
        }
    }

    /**
     * Looks if we have to stop
     */
    private boolean mustStop() {
        synchronized (this) {
            return stop;
        }
    }


    /**
     * @since MMBase-1.9.2
     */
    public static Set<FileWatcher> getFileWatchers() {
        return Collections.unmodifiableSet(fileWatchers.watchers);
    }

    /**
     * @javadoc
     */
    public static void main(String[] args) {

        // start some filewatchers
        for (int i = 0; i < 100; i++) {
            FileWatcher w = new TestFileWatcher();
            // add some files
            for (int j = 0; j < 4; j++) {
                try {
                    w.add(File.createTempFile("filewatchertestfile", ".txt"));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            //set a delay and start it.
            w.setDelay(() -> 1 * 1000); // 1 s
            w.start();
            //this.wait(123); // make all watchers out sync
        }

        System.out.println("Starting");
        // ok, a lot of those are running now, let time something else, and see if it suffers.
        long start = System.currentTimeMillis();
        long k;
        for (k = 0; k < 400000000;) {
            k++;
        }
        System.out.println("\ntook " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * The one thread to handle all FileWatchers. In earlier implementation every FileWatcher had
     * it's own thread, but that is avoided now.
     */
    static class FileWatcherRunner implements Runnable {

        /*
         * Set of file-watchers, which are currently active.
         */
        private final Set<FileWatcher> watchers = new CopyOnWriteArraySet<FileWatcher>();


        void add(FileWatcher f) {
            watchers.add(f);
        }

        void remove(FileWatcher f) {
            watchers.remove(f);
        }
        int size() {
            return watchers.size();
        }

        /**
         *  Main loop, will check every watched file every amount of time.
         *  It will never stop, this thread is a daemon.
         */
        public void run() {
            try {
                long now = System.currentTimeMillis();
                List<FileWatcher> removed = new ArrayList<FileWatcher>(); // copyonwritearraylist's iterator  does not support remove
                for (FileWatcher f : watchers) {
                    long staleness = (now - f.lastCheck);
                    if (staleness >= f.delay.getAsLong()) {
                        if (log.isTraceEnabled()) {
                            log.trace("Filewatcher " + f + " with " + f.delay.getAsLong()  + " ms <= " + staleness  + " ms is expired. Currently it's watching: " + f.getClass().getName() + " " + f.toString());
                        }
                        // System.out.print(".");
                        //changed returns true if we can stop watching
                        if (f.changed() || f.mustStop()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Removing filewatcher " + f + " " + f.mustStop());
                            }
                            removed.add(f);
                        }
                        f.lastCheck = now;
                    }
                }
                if (removed.size() > 0) {
                    log.debug("Now removing " + removed);
                    synchronized(this) {
                        int sizeBefore = watchers.size();
                        watchers.removeAll(removed);
                        log.debug("Size " + sizeBefore + " -> " +  watchers.size());
                        for (FileWatcher w : removed) {
                            w.removeFiles();
                        }
                    }
                }
            } catch (Throwable ex) {
                // unexpected exception?? This run method should never interrupt, so we catch everything.
                log.error("Exception: " + ex.getClass().getName() + ": " + ex.getMessage() + Logging.stackTrace(ex));
            }
        }

        public void cancel() {
            watchers.clear();
        }
    }

    /**
     * Performance test
     */
    private static class TestFileWatcher extends FileWatcher {
        int i = 0;
        public void onChange(java.io.File f) {
            // do something..
            i++;
        }

    }

    /**
     * Object used in file-lists of the FileWatcher. It wraps a File object, but adminstrates
     * lastmodified an existence seperately (to compare with the actual values of the File).
     */
    private static class FileEntry {
        // static final Logger log = Logging.getLoggerInstance(FileWatcher.class.getName());
        private long lastModified = -1;
        private boolean exists = false;
        private final File file;

        public FileEntry(File file) {
            if (file == null) {
                throw new IllegalArgumentException();
            }
            exists = file.exists();
            lastModified = getLastModified(file);
            this.file = file;
        }

        /**
         * Returns the last modification time of a file, or -1 if the file does not exists, or the
         * last modification time of the last modified file in it, if it is a directory.
         * @since MMBase-1.9
         */
        protected long getLastModified(File f) {
            long lm;
            if (! f.exists()) {
                // file does not exist. A change will be triggered
                // once the file comes into existence
                if (log.isDebugEnabled()) {
                    log.debug("file :" + f.getAbsolutePath() + " did not exist (yet)");
                }
                lm = -1;
            } else {
                lm = f.lastModified();
                if (f.isDirectory() && f.canRead()) {
                    // in that case, we take the last modified file in it, and return that.
                    // TODO, we may need a flag to also enable _not_ doing this, or at least not recursively
                    for (File child : f.listFiles()) {
                        try {
                            long childLastModified = getLastModified(child);
                            if (childLastModified > lm) {
                                lm = childLastModified;
                            }
                        } catch (SecurityException se) {
                            // never mind
                        }
                    }
                }
            }
            return lm;
        }

        /**
         * Returns true if the file was modified, added, or removed. If the change is handled, then
         * call {@link #updated}.
         * @return <code>true</code> if the file was changed
         */
        public boolean changed() {
            if (file.exists()) {
                if (!exists) {
                    log.info("File " + file.getAbsolutePath() + " added");
                    return true;
                } else {
                    boolean result = lastModified < getLastModified(file);
                    if (result) {
                        log.info("File " + file.getAbsolutePath() + " changed");
                    }
                    return result;
                }
            } else {
                if (exists) {
                    log.info("File " + file.getAbsolutePath() + " removed");
                }
                return exists;
            }
        }

        /**
         * Call this if changes were treated. It resets the state, and after that {@link #changed}
         * will return <code>false</code> again.
         */
        public void updated() {
            exists = file.exists();
            if (exists) {
                lastModified = getLastModified(file);
            } else {
                lastModified = -1;
            }
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.toString() + ":" + lastModified;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FileEntry) {
                FileEntry fe = (FileEntry)o;
                return file.equals(fe.file);
            } else if (o instanceof File) {
                return file.equals(o);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

    }

    /**
     * This FileSet makes the 'files' object of the FileWatcher look like a Set of File rather then Set of FileEntry's.
     * @since MMBase-1.8
     */
    private class FileSet extends AbstractSet<File> {
        public int size() {
            return FileWatcher.this.files.size();
        }
        public  Iterator<File> iterator() {
            return new FileIterator();
        }
        @Override
        public boolean add(File o) {
            int s = size();
            FileWatcher.this.add(o);
            return s != size();
        }
    }
    /**
     * The iterator belonging to FileSet.
     * @since MMBase-1.8
     */
    private class FileIterator implements Iterator<File> {
        Iterator<FileEntry> it;
        File lastFile;
        FileIterator() {
            it = FileWatcher.this.files.iterator();
        }
        public boolean hasNext() {
            return it.hasNext();
        }
        public File next() {
            FileEntry f = it.next();
            lastFile = f.getFile();
            return  lastFile;
        }
        public void remove() {
            FileWatcher.this.remove(lastFile);
        }

    }

}
