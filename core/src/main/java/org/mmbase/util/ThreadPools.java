/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.UtilReader;
import org.mmbase.util.xml.Instantiator;

/**
 * Generic MMBase Thread Pools
 *
 * @since MMBase 1.8
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public abstract class ThreadPools {
    private static final Logger log = Logging.getLoggerInstance(ThreadPools.class);

    public static final ThreadGroup threadGroup =  new ThreadGroup("MMBase Thread Pool");

    private static final Map<Future<?>, String> identifiers =
        Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * There is no way to identify the FutureTask objects returned in
     * the getQueue methods of the executors.  This works around that.
     * Used by admin pages.
     * @since MMBase-1.9
     */
    public static String identify(Future<?> r, String s) {
        return identifiers.put(r, s);
    }

    /**
     * Wrapper around Thread.scheduler.scheduleAtFixedRate.
     * @deprecated Used ThreadPools.scheduler#scheduleAtFixedRate  This method is only provided to
     * use this in both 1.8 (concurrecy backport) and 1.9 (java 1.5).
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable pub, int time1, int time2) {
        return scheduler.scheduleAtFixedRate(pub,
                                             time1,
                                             time2, TimeUnit.SECONDS);
    }
    /**
     * returns a identifier string for the given task.
     * @since MMBase-1.9
     */
    public static String getString(Future<?> r) {
        String s = identifiers.get(r);
        if (s == null) return "" + r;
        return s;
    }

    /**
     * Generic Thread Pools which can be used by 'filters'. Filters
     * are short living tasks. This is mainly used by {@link
     * org.mmbase.util.transformers.ChainedCharTransformer} (and only
     * when transforming a Reader).
     * <p>
     * Code performing a similar task could also use this thread pool.
     */
    public static final ExecutorService filterExecutor = Executors.newCachedThreadPool();


    private static final List<WeakReference<Thread>> nameLess = new CopyOnWriteArrayList<>();


    public static Thread newThread(final Runnable r, final String id) {
        String mn = getMachineName();
        log.service("Found mn " + mn + "(" + (mn == null) + ")");
        Thread t = new Thread(threadGroup, r,
                              (mn == null ? "" : mn) + ":" + id) {
                /**
                 * Overrides run of Thread to catch and log all exceptions. Otherwise they go through to app-server.
                 */
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (org.mmbase.bridge.NotFoundException nf) {
                        log.debug("Error during job: " + r + ":" + id + " " + nf.getClass().getName() + " " + nf.getMessage(), nf);
                    } catch (Throwable e) {
                        log.error("Error during job: " + r + ":" + id + " " + e.getClass().getName() + " " + e.getMessage(), e);
                    }
                }
            };
        t.setDaemon(true);
        if (mn == null) {
            nameLess.add(new WeakReference<>(t));
        }
        return t;
    }


    private static long jobsSeq = 0;
    /**
     * All kind of jobs that should happen in a seperate Thread can be
     * executed by this executor. E.g. sending mail could be done by a
     * job of this type.
     *
     */
    public static final ThreadPoolExecutor jobsExecutor = new ThreadPoolExecutor(2, 2000, 60 , TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "JobsThread-" + (jobsSeq++));
            }
        }) {
            @Override
            public void execute(Runnable r) {
                if (log.isDebugEnabled()) {
                    log.debug("Executing " + r + " because ", new Exception());
                }
                super.execute(r);
            }
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                log.debug("Now executing " + r + " in thread " + t);


            }
        };


    private static String getMachineName() {
        String machineName;
        try {
            machineName = org.mmbase.module.core.MMBaseContext.getMachineName();
        } catch (org.mmbase.bridge.BridgeException be) {
            machineName = "localhost";
        } catch (NoClassDefFoundError cnfe) {
            // happens if no MMBaseContext, because this is used with the
            // rmmci-client jar.
            machineName = "localhost";
        }
        return machineName;
    }


    private static long schedSeq = 0;
    /**
     * This executor is for repeating tasks. E.g. every running
     * {@link org.mmbase.module.Module}  has a  {@link
     * org.mmbase.module.Module#maintainance} which is scheduled to
     * run every hour.
     *
     * @since MMBase-1.9
     */
    public static final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "SchedulerThread-" + (schedSeq++));
            }
        });
    static {
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        // after some time the machine name will be known, use it for the 'nameless' threads.
        // Actually, getMachineName is starting to wait too, so I think the scheduled delay here is
        // a bit silly, but otherwise I in some cases encountered an exception ('cannot be started
        // by this class').
        scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    String machineName = getMachineName();
                    for (WeakReference<Thread> tr : nameLess) {
                        Thread t = tr.get();
                        if (t != null) {
                            String stringBefore = "" + t;
                            t.setName(machineName + t.getName());
                            log.debug("Fixed name of " + stringBefore + " -> " + t);
                        }
                    }
                    nameLess.clear();
                }
            }, 60, TimeUnit.SECONDS);
    }

    private static final Map<String, ExecutorService> threadPools = new ConcurrentHashMap<>();
    static {
        threadPools.put("jobs", jobsExecutor);
        threadPools.put("filters", filterExecutor);
        threadPools.put("schedules", scheduler);

    }

    public static Map<String, ExecutorService> getThreadPools() {
        return threadPools;
    }


    static final UtilReader properties = new UtilReader("threadpools.xml", new Runnable() {
            @Override
            public void run() {
                configure();
            }
        });


    /**
     * @since MMBase-1.9.2
     */
    static protected void setProperty(ThreadPoolExecutor object, String key, String value) {
        if (key.equals("maxsize")) {
            int newSize = Integer.parseInt(value);
            if (object.getMaximumPoolSize() !=  newSize) {
                log.info("Setting max pool size from " + object.getMaximumPoolSize() + " to " + newSize);
                object.setMaximumPoolSize(newSize);
            }
        } else if (key.equals("coresize")) {
            int newSize = Integer.parseInt(value);
            if (object.getCorePoolSize() != newSize) {
                log.info("Setting core pool size from " + object.getCorePoolSize() + " to " + newSize);
                object.setCorePoolSize(newSize);
            }
        } else if (key.equals("keepAliveTime")) {
            int newTime = Integer.parseInt(value);
            if (object.getKeepAliveTime(TimeUnit.SECONDS) != newTime) {
                log.info("Setting keep alive time  from " + object.getKeepAliveTime(TimeUnit.SECONDS) + " to " + newTime);
                object.setKeepAliveTime(newTime, TimeUnit.SECONDS);
            }
        } else {
            Instantiator.setProperty(key, object.getClass(), object, value);
        }
    }


    /**
     * @since MMBase-1.9
     */
    static void configure() {

        Map<String,String> props = properties.getProperties();

        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (entry.getKey().startsWith("jobs.")) {
                setProperty(jobsExecutor, entry.getKey().substring("jobs.".length()), entry.getValue());;
            } else if (entry.getKey().startsWith("scheduler.")) {
                setProperty(scheduler, entry.getKey().substring("scheduler.".length()), entry.getValue());;
            } else if (entry.getKey().startsWith("filters.")) {
                setProperty(scheduler, entry.getKey().substring("filters.".length()), entry.getValue());;
            }
        }
    }

    /**
     * @since MMBase-1.8.4
     */
    public static void shutdown() {
        {
            List<Runnable> run = scheduler.shutdownNow();
            if (!run.isEmpty()) {
                log.info("Interrupted " + run);
            }
        }
        {

            List<Runnable> run = filterExecutor.shutdownNow();
            if (!run.isEmpty()) {
                log.info("Interrupted " + run);
            }

        }
        {
            List<Runnable> run = jobsExecutor.shutdownNow();
            if (!run.isEmpty()) {
                log.info("Interrupted " + run);
            }
        }
    }

    private ThreadPools() {
    }

}
