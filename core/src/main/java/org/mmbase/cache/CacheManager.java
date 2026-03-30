/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.cache;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.Pattern;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.mmbase.cache.implementation.LRUCache;
import org.mmbase.util.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.DocumentReader;
import org.w3c.dom.Element;


/**
 * Cache manager manages the static methods of {@link Cache}. If you prefer you can call them on
 * this in stead.
 *
 * Since 1.9.1 this class represents a singleton. Actually most methods should more logically not be
 * static any more.
 *
 * @since MMBase-1.8
 * @version $Id$
 */
public class CacheManager implements CacheManagerMBean {

    private static final Logger log = Logging.getLoggerInstance(CacheManager.class);

    public static String CACHE_NAME = "cacheName";

    /**
     * All registered caches
     */
    //private static final NavigableMap<String, Cache<?,?>> caches = new ConcurrentSkipListMap<String, Cache<?,?>>();
    private final Map<String, Cache<?,?>> caches = new ConcurrentHashMap<String, Cache<?,?>>();

    private static CacheManager instance = null;

    @SuppressWarnings("rawtypes")
    private static BiFunction<Integer, String, CacheImplementationInterface> createCache = (size, name) -> new LRUCache((size));

    private CacheManager() {
        // singleton
    }

    private static  String getMachineName(boolean assertUp) {
        String machineName;
        try {
            if (assertUp) {
                org.mmbase.bridge.ContextProvider.getDefaultCloudContext().assertUp();
            }
            machineName = org.mmbase.module.core.MMBaseContext.getMachineName();
        } catch (NoClassDefFoundError ncfde) {
            //happens when RMMCI
            machineName = "localhost";
        }
        return machineName;
    }

    /**
     * @since MMBase-1.9.1
     */

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
            ThreadPools.jobsExecutor.execute(new Runnable() {
                    public void run() {

                        ObjectName on;
                        final Hashtable<String, String> props = new Hashtable<String, String>();

                        try {
                            props.put("type", "Caches");
                            try {
                                String machineName = getMachineName(true);

                                if (machineName != null) {
                                    props.put("type", machineName);
                                }
                            } catch (Throwable t) {
                                log.error(t.getMessage(), t);
                            }
                            on = new ObjectName("org.mmbase", props);
                        } catch (MalformedObjectNameException mfone) {
                            log.warn(props + " " + mfone);
                            return;
                        }
                        try {
                            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                            mbs.registerMBean(instance, on);
                            log.service("Registered " + on);
                        } catch (JMException jmo) {
                            log.warn(on + " " + jmo.getClass() + " " + jmo.getMessage());
                        } catch (Throwable t) {
                            log.error(on + " " + t.getClass() + " " + t.getMessage());
                        }
                    }
                });

        }
        return instance;
    }




    /**
     * Returns the Cache with a certain name. To be used in combination with getCaches(). If you
     * need a certain cache, you can just as well call the non-static 'getCache' which is normally
     * in cache singletons.
     *
     * @see #getCaches
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) getInstance().caches.get(name);
    }

    /**
     * Returns a cache wrapped in a 'Bean', so it is not a Map any more. This makes it easier
     * accesible by tools which want that (like EL).
     * @since MMBase-1.9
     */
    public static Bean<?, ?> getBean(String name) {
        return new Bean<>(getCache(name));
    }
    public static Set<Bean<?, ?>> getCaches(String className) {
        SortedSet<Bean<?, ?>> result = new TreeSet<>();
        for (Cache<?, ?> c : getInstance().caches.values()) {
            try {
                if (className == null || className.isEmpty() || Class.forName(className).isInstance(c)) {
                    result.add(new Bean<>(c));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * Returns the names of all caches.
     *
     * @return A Set containing the names of all caches.
     */
    public static Set<String> getCaches() {
        return Collections.unmodifiableSet(getInstance().caches.keySet());
    }

    /**
     * @since MMBase-1.8.6
     */
    public static Map<String, Cache<?, ?>> getMap() {
        return Collections.unmodifiableMap(getInstance().caches);
    }


    private static final ThreadPoolExecutor cachePutter = new ThreadPoolExecutor(0, 1, 2 , TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "CachePutter");
            }
        });
/*    static {
        cachePutter.allowCoreThreadTimeOut(true);
    }
*/
    /**
     * Puts a cache in the caches repository. This function will be
     * called in the static of childs, therefore it is protected.
     *
     * @param cache A cache.
     * @return The previous cache of the same type (stored under the same name)
     */
    public static <K,V> Cache<K,V> putCache(final Cache<K,V> cache) {
        Cache old = getInstance().caches.put(cache.getName(), cache);
        try {
            configure(configReader, cache.getName());
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
        Runnable run = new Runnable() {
                public void run() {
                    ObjectName name = getObjectName(cache, true);
                    try {
                        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                        mbs.registerMBean(cache, name);
                    } catch (JMException jmo) {
                        log.warn(name + " " + jmo.getClass() + " " + jmo.getMessage());
                    } catch (Throwable t) {
                        log.error(name + " " + t.getClass() + " " + t.getMessage());
                    }
                }
            };
        if (org.mmbase.bridge.ContextProvider.getDefaultCloudContext().isUp()) {
            run.run();
        } else {
            cachePutter.execute(run);
        }

        return old;
    }


    /**
     * @since MMBase-1.9
     */
    private static ObjectName getObjectName(Cache cache, boolean assertUp) {
        // Not using the Constructor with Hashtable, because you can't influence the order of keys
        // with that. Which is relevant, e.g. when presented in a tree by jconsole.
        StringBuilder buf = new StringBuilder("org.mmbase:");
        try {
            buf.append("type=Caches");
            org.mmbase.util.transformers.CharTransformer identifier = new org.mmbase.util.transformers.Identifier();
            String machineName = getMachineName(assertUp);
            if (machineName != null) {
                buf.append(",mmb=").append(machineName);
            } else {
            }
            if (cache != null) {
                buf.append(",name=").append(identifier.transform(cache.getName()));
            } else {
                //props.put("name", "*"); // WTF, this does not work in java 5.
            }
            return new ObjectName(buf.toString());
        } catch (MalformedObjectNameException mfone) {
            log.warn(buf + " " + mfone);
            return null;
        }
    }

    /**
     * Configures the caches using a config File. There is only one
     * config file now so the argument is a little overdone, but it
     * doesn't harm.
     */

    private static void configure(DocumentReader file) {
        configure(file, null);
    }

    private static DocumentReader configReader = null;

    /**
     * As configure, but it only changes the configuration of the cache 'only'.
     * This is called on first use of a cache.
     */
    private static void configure(DocumentReader xmlReader, String only) {
        if (xmlReader == null) {
            return; // nothing can be done...
        }

        if (only == null) {
            log.service("Configuring caches with " + xmlReader.getSystemId());

            final Element defaultImpl = xmlReader.getElementByPath("caches.defaultImplementation");
            if (defaultImpl != null) {
                final String clazz = DocumentReader.getElementValue(DocumentReader.getElementByPath(defaultImpl, "defaultImplementation.class"));
                final Map<String, String> configValues = new HashMap<>();
                for (Element attrNode : DocumentReader.getChildElements(defaultImpl, "param")) {
                    String paramName = xmlReader.getElementAttributeValue(attrNode, "name");
                    String paramValue = DocumentReader.getElementValue(attrNode);
                    configValues.put(paramName, paramValue);
                }
                log.info("Default cache implementation: " + clazz + " with config " + configValues);
                createCache = (size, name) -> createImplementation(size, clazz, name,  configValues);
            }
        } else {
            if (log.isDebugEnabled()) log.debug("Configuring cache " + only + " with file " + xmlReader.getSystemId());
        }


        for (Element cacheElement: xmlReader.getChildElements("caches", "cache")) {
            String cacheName =  cacheElement.getAttribute("name");
            if (only != null && ! only.equals(cacheName)) {
                continue;
            }
            Cache<?, ?> cache = getCache(cacheName);
            if (cache == null) {
                log.service("No cache " + cacheName + " is present (perhaps not used yet?)");
            } else {
                String clazz = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.implementation.class"));
                if(!"".equals(clazz)) {
                    Element cacheImpl = DocumentReader.getElementByPath(cacheElement, "cache.implementation");
                    Map<String,String> configValues = new HashMap<>();
                    configValues.put(CACHE_NAME, cacheName);;
                    for (Element attrNode: DocumentReader.getChildElements(cacheImpl, "param")) {
                        String paramName = xmlReader.getElementAttributeValue(attrNode, "name");
                        String paramValue = DocumentReader.getElementValue(attrNode);
                        configValues.put(paramName, paramValue);
                    }
                    cache.setImplementation(cache.getMaxSize(), clazz, configValues);
                } else {
                    cache.setDefaultImplementation(cache.getMaxSize());
                }
                String status = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.status"));
                cache.setActive(status.equalsIgnoreCase("active"));
                try {
                    int size = Integer.parseInt(DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.size")));
                    cache.setMaxSize(size);
                    log.service("Setting " + cacheName + " " + status + " with size " + size);
                } catch (NumberFormatException nfe) {
                    log.error("Could not configure cache " + cacheName + " because the size was wrong: " + nfe.toString());
                } catch (Throwable t) {
                    log.error(" " + cacheName + " maxsize " + t.getMessage());
                }
                String maxSize = DocumentReader.getElementValue(DocumentReader.getElementByPath(cacheElement, "cache.maxEntrySize"));
                if (!"".equals(maxSize)) {
                    try {
                        cache.maxEntrySize = Integer.parseInt(maxSize);
                        log.service("Setting maximum entry size on " + cacheName + ": " + cache.maxEntrySize + " bytes ");
                    } catch (NumberFormatException nfe2) {
                        log.error("Could not set max entry size cache  of " + cacheName + " because " + nfe2.toString());
                    } catch (Throwable t) {
                        log.error(" " + cacheName + " maxentrysize " + t.getMessage());
                    }
                } else {
                    if (cache.getDefaultMaxEntrySize() > 0) {
                        log.service("No max entry size specified for this cache taking default " + cache.getDefaultMaxEntrySize() + " bytes");
                    }
                    cache.maxEntrySize = cache.getDefaultMaxEntrySize();
                    //now see if we have to load cache release strategies for this lovely cache...
                    if(cache instanceof QueryResultCache){
                        QueryResultCache queryCache = (QueryResultCache) cache;
                        //first remove all present strategies (this might be a reconfiguration)
                        queryCache.getReleaseStrategy().removeAllStrategies();
                        log.debug("found a SearchQueryCache: " + cacheName);
                        //see if there are globally configured release strategies
                        Element releaseStrategies = xmlReader.getElementByPath("caches.releaseStrategies");
                        if (releaseStrategies != null) {
                            queryCache.getReleaseStrategy().fillFromXml(releaseStrategies);
                        }
                        queryCache.getReleaseStrategy().fillFromXml(cacheElement);

                        if (queryCache.getReleaseStrategy().size() == 0) {
                            log.warn("No release-strategies configured for cache " + queryCache + " (nor globally configured); falling back to basic release strategy");
                            queryCache.addReleaseStrategy(new BasicReleaseStrategy());
                        }
                        log.service("Release strategies for " + queryCache.getName() + ": " + queryCache.getReleaseStrategy());
                    }
                }
            }
        }
    }



    /**
     * The caches can be configured with an XML file, this file can
     * be changed which causes the caches to be reconfigured automaticly.
     */
    private static final ResourceWatcher configWatcher = new ResourceWatcher () {
            public void onChange(String resource) {
                try {
                    org.xml.sax.InputSource is =  ResourceLoader.getConfigurationRoot().getInputSource(resource);
                    log.service("Reading " + is.getSystemId());
                    configReader = new DocumentReader(is, Cache.class);
                } catch (Exception e) {
                    log.warn(e.getClass() + " " + e.getMessage());
                    return;
                }
                configure(configReader);
            }
        };

    static { // configure
        try {
            log.debug("Static init of Caches");
            configWatcher.add("caches.xml");
            configWatcher.onChange("caches.xml");
            configWatcher.start();
        } catch (Throwable t) {
            log.error(t);
        }


    }


    public static int getTotalByteSize() {
        int len = 0;
        SizeOf sizeof = new SizeOf();
        for (Map.Entry<String, Cache<?, ?>> entry : getInstance().caches.entrySet()) {
            len += sizeof.sizeof(entry.getKey()) + sizeof.sizeof(entry.getValue());
        }
        return len;
    }

    /**
     * Shutdown all caches. To be used on shutdown of MMBase.
     * @since MMBase-1.8.1
     */
    public static void shutdown() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        log.info("Clearing and unregistering all caches");
        log.debug(mbs.queryNames(getObjectName(null, false), null));
        for(Cache<?,?> cache : getInstance().caches.values()) {
            cache.shutdown();
            ObjectName name = getObjectName(cache, false);
            if (mbs.isRegistered(name)) {
                try {
                    mbs.unregisterMBean(name);
                } catch (JMException jmo) {
                    log.warn(name + " " + jmo.getClass() + " " + jmo.getMessage() + " " + mbs.queryNames(null, null));
                }
            }
        }
        {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("type", "Caches");
            String machineName = org.mmbase.module.core.MMBaseContext.getMachineName();
            if (machineName != null) {
                props.put("type", machineName);
            }
            try {
                ObjectName name = new ObjectName("org.mmbase", props);
                if (mbs.isRegistered(name)) {
                    mbs.unregisterMBean(name);
                }
            } catch (JMException jmo) {

            }
        }
        if(mbs.queryNames(getObjectName(null, false), null).size() > 0) {
            log.warn("Didn't unregister all caches" + mbs.queryNames(getObjectName(null, false), null));
        }
        getInstance().caches.clear();
        instance = null;
    }


    /**
     * Used in config/functions/caches.xml
     */
    public static Object remove(String name, Object key) {
        Cache cache = getCache(name);
        if (cache == null) {
            throw new IllegalArgumentException();
        }
        log.service("Removing " + key + " from " + cache);
        return cache.remove(key);
    }

    /**
     * @since MMBase-1.9.1
     */
    public String clear(String pattern) {
        if (pattern == null) pattern = ".*";
        StringBuilder buf = new StringBuilder();
        Pattern p = Pattern.compile(pattern);
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            if (p.matcher(entry.getKey()).matches()) {
                buf.append("Clearing " + entry.getValue() + "\n");
                entry.getValue().clear();
            }
        }
        if (buf.length() == 0) buf.append("The regular expression '" + pattern + "' matched no cache at all");
        return buf.toString();
    }
    /**
     * @since MMBase-1.9.1
     */
    public String enable(String pattern) {
        if (pattern == null) pattern = ".*";
        StringBuilder buf = new StringBuilder();
        Pattern p = Pattern.compile(pattern);
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            if (p.matcher(entry.getKey()).matches()) {
                Cache c = entry.getValue();
                if(c.isActive()) {
                    buf.append("Already active " + c + "\n");
                } else {
                    c.setActive(true);
                    buf.append("Making active " + c + "\n");
                }

            }
        }
        if (buf.length() == 0) buf.append("The regular expression '" + pattern + "' matched no cache at all");
        return buf.toString();
    }
    /**
     * @since MMBase-1.9.1
     */
    public String disable(String pattern) {
        if (pattern == null) pattern = ".*";
        StringBuilder buf = new StringBuilder();
        Pattern p = Pattern.compile(pattern);
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            if (p.matcher(entry.getKey()).matches()) {
                Cache c = entry.getValue();
                if(c.isActive()) {
                    c.setActive(false);
                    buf.append("Making inactive " + c + "\n");
                } else {
                    buf.append("Already inactive " + c + "\n");
                }

            }
        }
        if (buf.length() == 0) buf.append("The regular expression '" + pattern + "' matched no cache at all");
        return buf.toString();
    }
    /**
     * @since MMBase-1.9.1
     */
    public String readConfiguration() {
        configWatcher.onChange("caches.xml");
        return "Read " + ResourceLoader.getConfigurationRoot().getResource("caches.xml");
    }

    @SuppressWarnings("unchecked")
    protected <K, V> CacheImplementationInterface<K, V> createDefaultImplementation(String name, int size) {
        return( CacheImplementationInterface<K, V>) createCache.apply(size, name);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> CacheImplementationInterface<K, V>  createImplementation(int size, String clazz, String name, Map<String,String> configValues) {
        try {
            Map<String, String> copy = new HashMap<>(configValues);
            copy.put(CACHE_NAME, name);
            Class<?> clas = Class.forName(clazz);
            CacheImplementationInterface<K, V> implementation;
            try {
                Constructor<?> cons = clas.getDeclaredConstructor(int.class);
                implementation = (CacheImplementationInterface<K, V>) cons.newInstance(size);
            } catch (NoSuchMethodException nsme) {
                log.debug("No constructor with int argument for " + clazz + ", trying default constructor");
                implementation = (CacheImplementationInterface<K,V>) clas.newInstance();
            }
            implementation.config(copy);
            return implementation;


        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException cnfe) {
            log.error("For cache " +  cnfe.getClass().getName() + ": " + cnfe.getMessage());
            throw new RuntimeException(clazz + " " + cnfe);
        }
    }


    public static class Bean<K, V> implements Comparable<Bean<?, ?>> {
        private final Cache<K, V> cache;
        public Bean(Cache<K, V> c) {
            cache = c;
        }
        public String getName() { return cache.getName(); }
        public String getDescription() { return cache.getDescription(); }
        public int getMaxEntrySize() { return cache.getMaxEntrySize(); }
        public Set<Map.Entry<K, V>> getEntrySet() {
            return cache.entrySet();
        }
        public Set<K> getKeySet() {return cache.keySet(); }
        public long getHits() { return cache.getHits(); }
        public long  getMisses() { return cache.getMisses(); }
        public long getPuts() { return cache.getPuts(); }
        public  int getMaxSize() { return cache.maxSize(); }
        public  int getSize() { return cache.size(); }
        public double getRatio() { return cache.getRatio(); }
        public String getStats() { return cache.getStats(); }
        public String toString() { return cache.toString(); }
        public boolean isActive() { return cache.isActive(); }
        public int getByteSize() { return cache.getByteSize(); }
        public int getCheapByteSize() { return cache.getCheapByteSize(); }
        public boolean isEmpty() { return cache.isEmpty(); }
        public ReleaseStrategy getReleaseStrategy() { return cache instanceof QueryResultCache ? ((QueryResultCache) cache).getReleaseStrategy() : null;}
        public Map<K, V> getMap() {  return cache; }
        public Map<K, Integer> getCounts() {
            return new AbstractMap<K, Integer>() {
                public Set<Map.Entry<K, Integer>> entrySet() {
                    return new AbstractSet<Map.Entry<K, Integer>>() {
                        public int size() {
                            return cache.size();
                        }
                        public Iterator<Map.Entry<K, Integer>> iterator() {
                            return new Iterator<Map.Entry<K, Integer>>() {
                                private Iterator<K> iterator = Bean.this.getKeySet().iterator();
                                public boolean hasNext() {
                                    return iterator.hasNext();
                                }
                                public Map.Entry<K, Integer> next() {
                                    K key = iterator.next();
                                    return new org.mmbase.util.Entry<K, Integer>(key, cache.getCount(key));
                                }
                                public void remove() {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }
                    };
                }
            };
        }
        public boolean equals(Object o) {
            return  o instanceof Bean && ((Bean) o).cache.equals(cache);
        }
        public int hashCode() {
            return cache.hashCode();
        }
        public int compareTo(Bean<?, ?> bean) {
            return getName().compareTo(bean.getName());
        }
    }
}
