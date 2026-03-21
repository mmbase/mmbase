/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.mmbase.bridge.Node;
import org.mmbase.core.event.*;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 *  Like {@link org.mmbase.util.FileWatcher} but for Resources. If (one of the) file(s) to which the resource resolves
 *  to is added or changed, its onChange will be triggered, if not a 'more important' one was
 *  existing already. If a file is removed, and was the most important one, it will be removed from the filewatcher.
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id$
 * @see    org.mmbase.util.FileWatcher
 * @see    org.mmbase.util.ResourceLoader
 */
public abstract class ResourceWatcher implements NodeEventListener  {
    private static final Logger log = Logging.getLoggerInstance(ResourceWatcher.class);

    /**
     * All instantiated ResourceWatchers.
     */
    static final Map<ResourceWatcher, Object> resourceWatchers = Collections.synchronizedMap(new WeakHashMap<ResourceWatcher, Object>());

    /**
     * Considers all resource-watchers. Perhaps onChange must be called, because there is a node for this resource available now.
     */
    static void setResourceBuilder() {
        synchronized(resourceWatchers) {
            for (ResourceWatcher rw : resourceWatchers.keySet()) {
                if (ResourceLoader.resourceBuilder != null) {

                    if (rw.running) {
                        EventManager.getInstance().addEventListener(rw);
                    }
                    for (String resource : rw.resources) {
                        if (rw.mapNodeNumber(resource)) {
                            log.service("ResourceBuilder is available now. Resource " + resource + " must be reloaded.");
                            rw.onChange(resource);

                        }
                    }
                }
            }
        }
    }

    /**
     * @since MMBase-1.9.2
     */
    static void reinitWatchers() {
        synchronized(resourceWatchers) {
            for (ResourceWatcher rw : new ArrayList<>(resourceWatchers.keySet())) {
                log.debug("Reiniting watcher " + rw);
                rw.readdResources();
            }
        }
    }

    /**
     * Delay setting used for the filewatchers.
     */
   private LongSupplier delay = () -> FileWatcher.getDefaultDelay().get().toMillis();

    /**
     * All resources watched by this ResourceWatcher. A Set of Strings. Often, a ResourceWatcher would watch only one resource.
     */
    protected final SortedSet<String> resources = new TreeSet<String>();

    /**
     * When a resource is loaded from a Node, we must know which Nodes correspond to which
     * resource. You could ask the node itself, but if the node happens to be deleted, then you
     * can't know that anymore. Used in {@link #notify(NodeEvent)}
     */
    protected final Map<Integer, String> nodeNumberToResourceName = new HashMap<Integer, String>();

    /**
     * Whether this ResourceWatcher has been started (see {@link #start})
     */
    private boolean running = false;

    /**
     * Wrapped FileWatcher for watching the file-resources. ResourceName -> FileWatcher.
     */
    protected final Map<String, FileWatcher> fileWatchers = new HashMap<String, FileWatcher>();

    /**
     * The resource-loader associated with this ResourceWatcher.
     */
    protected final ResourceLoader resourceLoader;


    /**
     * Constructor.
     */
    protected ResourceWatcher(ResourceLoader rl) {
        this(rl, true);
    }

    protected ResourceWatcher(ResourceLoader rl, boolean administrate) {
        resourceLoader = rl;
        if (administrate) {
            resourceWatchers.put(this, null);
        }
        if (log.isDebugEnabled()) {
            log.debug(" " + this + " for " + rl);
        }
    }

    /**
     * Constructor, defaulting to the Root ResourceLoader (see {@link ResourceLoader#getConfigurationRoot}).
     */
    protected ResourceWatcher() {
        this(ResourceLoader.getConfigurationRoot());
    }



    /**
     * @return Unmodifiable set of String of watched resources
     */
    public Set<String> getResources() {
        return Collections.unmodifiableSortedSet(resources);
    }

    /**
     * The associated ResourceLoader
     */
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     *
     * @param resourceName The resource to be monitored.
     */
    public synchronized void add(String resourceName) {
        if (resourceName == null || resourceName.equals("")) {
            log.warn("Cannot watch resource '" + resourceName + "' " + Logging.stackTrace());
            return;
        }
        resources.add(resourceName);
        if (log.isDebugEnabled()) {
            log.debug("Started watching '" + resourceName + "' for resource loader " + resourceLoader);
            log.trace("(now watching " + resources + ")");
        }
        if (running) {
            createFileWatcher(resourceName);
            mapNodeNumber(resourceName);
        } else {
            log.debug("Not creating file and and node watchers because not running");
        }
    }

    /**
     * If you resolved a resource already to an URL, you can still add it for watching.
     */
    public synchronized void add(URL url) {
        if (url.getProtocol().equals(ResourceLoader.PROTOCOL)) {
            String path = url.getPath();
            add(path.substring(resourceLoader.getContext().getPath().length()));
        } else {
            throw new UnsupportedOperationException("Don't know how to watch " + url + " (Only URLs produced by ResourceLoader are supported)");
        }
    }

    /**
     * When a resource is added to this ResourceWatcher, this method is called to create a
     * {@link FileWatcher}, and add all files associated with the resource to it.
     */
    protected synchronized void createFileWatcher(String resource) {
        FileWatcher fileWatcher = new ResourceFileWatcher(resource);
        fileWatcher.setDelay(delay);
        fileWatcher.getFiles().addAll(resourceLoader.getFiles(resource));
        fileWatcher.start(); // filewatchers are only created on start, so must always be started themselves.
        FileWatcher old = fileWatchers.put(resource, fileWatcher);
        if (old == null) {
            log.debug("Created " + fileWatcher + " " + fileWatchers);
        } else {
            log.warn("Replaced " + fileWatcher + " " + fileWatchers, new Exception());
            old.exit();
        }
    }

    /**
     * When a resource is added to this ResourceWatcher, this method is called to check wether a
     * ResourceBuilder node is associated with this resource. If so, this methods maps the number of
     * the node to the resource name. This is needed in {@link #notify(NodeEvent)} in case of a
     * node-deletion.
     * @return Whether a Node as found to map.
     */
    protected synchronized boolean mapNodeNumber(String resource) {
        Node node = resourceLoader.getResourceNode(resource);
        if (node != null) {
            nodeNumberToResourceName.put(node.getNumber(), resource);
            return true;
        } else {
            return false;
        }

    }

    /**
     * If a node (of the type 'resourceBuilder') changes, checks if it is a node belonging to one of the resource of this resource-watcher.
     * If so, {@link #onChange} is called.
     */
    public void notify(NodeEvent event) {
        if (event.getBuilderName().equals("resources")) {
            int number = event.getNodeNumber();
            switch(event.getType()) {
            case NodeEvent.TYPE_DELETE: {
                // hard..
                String name = nodeNumberToResourceName.get(number);
                if (name != null && getResources().contains(name)) {
                    nodeNumberToResourceName.remove(number);
                    log.service("Resource " + name + " changed (node removed)");
                    onChange(name);
                }
                break;
            }
            default: {
                Node node = ResourceLoader.getResourceBuilder().getCloud().getNode(number);
                int contextPrefix = resourceLoader.getContext().getPath().length() - 1;
                String name = node.getStringValue(ResourceLoader.RESOURCENAME_FIELD);
                if (name.length() > contextPrefix && getResources().contains(name.substring(contextPrefix))) {
                    log.service("Resource " + name + " changed (node added or changed)");
                    nodeNumberToResourceName.put(number, name);
                    onChange(name);
                }
            }
            }
        }
    }

    public synchronized void start() {
        if (! running) {
            if (delay.getAsLong() > 0) {
                // create and start all filewatchers.
                for (String resource : getResources()) {
                    //resourceLoader.checkShadowedNewerResources(resource);
                    mapNodeNumber(resource);
                    createFileWatcher(resource);
                }
                if (EventManager.getInstance() != null) {
                    EventManager.getInstance().addEventListener(this);
                }
                running = true;
            } else {
                log.service("Not started watching for  " + resources + " since delay < 0");
            }
        } else {
            log.warn("Already running.", new Exception());
        }
    }

    /**
     * Put here the stuff that has to be executed, when a file has been changed.
     * @param resourceName The resource that was changed.
     */
    abstract public void onChange(String resourceName);

    /**
     * Calls {@link #onChange(String)} for every added resource.
     */
    public final void onChange() {
        for (String resource : getResources()) {
            onChange(resource);
        }
    }

    /**
     * Set the delay to observe between each check of the file changes.
     * @param delay The delay in milliseconds
     */
    public synchronized void setDelay(LongSupplier delay) {
        this.delay = delay;
        for (FileWatcher fw : fileWatchers.values()) {
            fw.setDelay(delay);
        }
    }

    public synchronized void setDelay(Supplier<Duration> delay) {
        setDelay(() -> delay.get().toMillis());
    }

    /**
     */
    public synchronized void remove(String resourceName) {
        boolean wasRunning = running;
        if (running) { // it's simplest like this.
            exit();
        }
        resources.remove(resourceName);
        if (wasRunning) {
            start();
        }
    }

    /**
     * Removes all resources.
     */
    public synchronized  void clear() {
        if (running) {
            exit();
            resources.clear();
            start();
        } else {
            resources.clear();
        }
    }

    /**
     * @since MMBase-1.9.2
     */
    protected synchronized void readdResources() {
        SortedSet<String> copy = new TreeSet<String>();
        copy.addAll(resources);
        clear();
        for (String resource : copy) {
            add(resource);
        }
        log.debug("Readded resources, now " + resources);
        onChange();
    }

    /**
     * Stops watching. Stops all filewatchers, removes observers.
     */
    public synchronized void exit() {
        if (running) {
            Iterator<FileWatcher> i = fileWatchers.values().iterator();
            while (i.hasNext()) {
                FileWatcher fw = i.next();
                fw.exit();
                i.remove();
            }
            if (ResourceLoader.getResourceBuilder() != null) {
                EventManager.getInstance().removeEventListener(this);
            }
            running = false;
        } else {
            log.debug("Not running");
        }
    }

    /**
     * Shows the 'contents' of the filewatcher. It shows a list of files/last modified timestamps.
     */
    public String toString() {
        return resources + " " + fileWatchers;
    }

    /**
     * @since MMBase-1.9.2
     */
    public static Set<ResourceWatcher> getResourceWatchers() {
        return resourceWatchers.keySet();
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
    public Map<String, FileWatcher> getFileWatchers() {
        return Collections.unmodifiableMap(fileWatchers);
    }
    /**
     * @since MMBase-1.9.2
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * A FileWatcher associated with a certain resource of this ResourceWatcher.
     */

    protected class ResourceFileWatcher extends FileWatcher {
        private final String resource;
        ResourceFileWatcher(String resource) {
            super(true);
            this.resource = resource;
        }
        public void onChange(File f) {
            URL shadower = resourceLoader.shadowed(f, resource);
            if (shadower == null) {
                ResourceWatcher.this.onChange(resource);
            } else {
                log.warn("File " + f + " changed, but it is shadowed by " + shadower);
            }
        }
    }

}
