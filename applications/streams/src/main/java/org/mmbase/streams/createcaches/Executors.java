/*

This file is part of the MMBase Streams application,
which is part of MMBase - an open source content management system.
    Copyright (C) 2009 André van Toly, Michiel Meeuwissen

MMBase Streams is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MMBase Streams is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MMBase. If not, see <http://www.gnu.org/licenses/>.

*/

package org.mmbase.streams.createcaches;

import java.util.*;
import java.util.concurrent.*;
import org.mmbase.util.externalprocess.CommandExecutor;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This maintains a list of reusable {@link CommandExecutor.Method}s. You can obtain on unused one with {@link
 * #getFreeExecutor}. Supposing that you are going to want to use it in a seperate thread, it also maintains ThreadPoolExecuters.
 * @author Michiel Meeuwissen
 * @since MMBase-1.9.6
 */
public class Executors {

    private static final Logger LOG = Logging.getLoggerInstance(Processor.class);

    private Executors() {
        // no instances;
    }

    private static int transSeq = 0;

    private static final Map<Stage, ThreadPoolExecutor> threadPools = new EnumMap<Stage, ThreadPoolExecutor>(Stage.class);
    private static final ConcurrentHashMap<CommandExecutor.Method, Stage> executorsMap = new ConcurrentHashMap<CommandExecutor.Method, Stage>();
    public static Map<CommandExecutor.Method, Stage> getExecutors() {
        return executorsMap;
    }

    protected static final ResourceWatcher watcher = new ResourceWatcher() {
            @Override
            public void onChange(String resource) {
                try {
                    LOG.debug("Reading " + resource);

                    HashMap<CommandExecutor.Method, Stage> newexecutorsMap = new HashMap<CommandExecutor.Method, Stage>();
                    Document document = getResourceLoader().getDocument(resource);
                    Map<Stage, Integer> totals = new EnumMap<Stage, Integer>(Stage.class);

                    if (document != null) {
                        org.w3c.dom.NodeList ellist = document.getDocumentElement().getChildNodes();

                        Stage prevStage = Stage.RECOGNIZER;
                        for (int i = 0; i <= ellist.getLength(); i++) {
                            if (ellist.item(i) instanceof Element) {
                                Element el = (Element) ellist.item(i);
                                if (el.getTagName().equals("localhost")) {
                                    int max = Integer.parseInt(el.getAttribute("max_simultaneous_transcoders"));
                                    Stage s = Stage.valueOf(el.getAttribute("stage").toUpperCase());
                                    Integer t = totals.get(s);
                                    if (t == null) t = 0;
                                    t += max;
                                    totals.put(s, t);
                                    for (int j = 1; j <= max; j++) {
                                        newexecutorsMap.put(new CommandExecutor.Method(), s);
                                    }
                                } else if (el.getTagName().equals("server")) {
                                    int max = Integer.parseInt(el.getAttribute("max_simultaneous_transcoders"));
                                    Stage s = Stage.valueOf(el.getAttribute("stage").toUpperCase());
                                    Integer t = totals.get(s);
                                    if (t == null) t = 0;
                                    t += max;
                                    totals.put(s, t);
                                    String host = el.getAttribute("host");
                                    int    port = Integer.parseInt(el.getAttribute("port"));
                                    for (int j = 1; j <= max; j++) {
                                        newexecutorsMap.put(new CommandExecutor.Method(host, port), s);
                                    }
                                }
                            }
                        }
                        for (Map.Entry<Stage, Integer> e : totals.entrySet()) {
                            threadPools.get(e.getKey()).setCorePoolSize(e.getValue());
                            threadPools.get(e.getKey()).setMaximumPoolSize(e.getValue());
                        }
                    } else {
                        LOG.warn("No " + resource);
                    }
                    synchronized (executorsMap) {
                        executorsMap.clear();
                        executorsMap.putAll(newexecutorsMap);
                    }
                    LOG.service("Reading of configuration file " + resource + " successfull. Executors " + executorsMap + ". Max simultaneous transcoders: " + totals);
                } catch (Exception e)  {
                    LOG.error(e.getClass() + " " + e.getMessage() + " In " + resource + " Executors now " + executorsMap + " (not changed)", e);
                }
            }
        };


    static {
        threadPools.put(Stage.TRANSCODER, new ThreadPoolExecutor(3, 3, 5 * 60 , TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return ThreadPools.newThread(r, "TranscoderThread-" + Stage.TRANSCODER + "-" + (transSeq++));
                }
            }));
        threadPools.put(Stage.RECOGNIZER, new ThreadPoolExecutor(3, 3, 5 * 60 , TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return ThreadPools.newThread(r, "TranscoderThread-" + Stage.RECOGNIZER + "-" + (transSeq++));
            }
            }));

        // register them too
        ThreadPools.getThreadPools().put(Executors.class.getName() + "." + Stage.TRANSCODER, threadPools.get(Stage.TRANSCODER));
        ThreadPools.getThreadPools().put(Executors.class.getName() + "." + Stage.RECOGNIZER, threadPools.get(Stage.RECOGNIZER));


        // fill the rest of the map too, so we don't have to think about it any more later on.
        for (Stage s : Stage.values()) {
            if (!threadPools.containsKey(s)) {
                threadPools.put(s, ThreadPools.jobsExecutor);
            }
        }
        // default configuration, 5 + 1 executors.
        for (int i = 0; i < 5; i++) {
            executorsMap.put(new CommandExecutor.Method(), Stage.TRANSCODER);
        }
        executorsMap.put(new CommandExecutor.Method(), Stage.RECOGNIZER);
        readConfiguration();
    }



    protected static void readConfiguration() {
        watcher.exit();
        watcher.add("streams/createcaches.xml");
        watcher.onChange();
        watcher.start();
    }

    public static CommandExecutor.Method getFreeExecutor() {
        // for backwards compatability, be sure to return one
        return getFreeExecutor(Stage.TRANSCODER);
    }

    public static CommandExecutor.Method getFreeExecutor(Stage st) {
        while (true) {
            synchronized(executorsMap) {
                try {
                    for (Map.Entry<CommandExecutor.Method, Stage> entry : executorsMap.entrySet()) {
                        if (!entry.getKey().isInUse() && entry.getValue() == st) {
                            CommandExecutor.Method m = entry.getKey();
                            m.setInUse(true);
                            return m;
                        }
                    }

                    LOG.info("All executors for stage " + st + " in use (will wait..)");
                    executorsMap.wait();

                } catch (InterruptedException ie) {
                    return null;
                }
            }
        }

    }

    public static void notifyExecutors() {
        synchronized (executorsMap) {
            executorsMap.notifyAll();
        }
    }

    public static <T> Future<T> submit(Stage s, Callable<T> c) {
        return threadPools.get(s).submit(c);
    }
}

