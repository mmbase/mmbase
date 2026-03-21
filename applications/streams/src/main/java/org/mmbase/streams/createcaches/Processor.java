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

import java.io.*;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.core.event.*;
import org.mmbase.datatypes.processors.CommitProcessor;
import org.mmbase.security.ActionRepository;
import org.mmbase.security.UserContext;
import org.mmbase.servlet.FileServlet;
import org.mmbase.streams.transcoders.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.xml.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
 * This commit-processor is used on nodes of type 'streamsources' and is used to initiate the
 * conversions to other formats which are saved in 'streamsourcescaches'. Its analogy is derived
 * from the conversion of 'images' in MMBase to their resulting 'icaches' nodes.
 * Several transcodings of media files can be configured with
 * {@link org.mmbase.streams.transcoders.Recognizer}s and
 * {@link org.mmbase.streams.transcoders.Transcoder}s.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public class Processor implements CommitProcessor, java.io.Externalizable {
    private static final long serialVersionUID = 0L;

    public static String NOT = Processor.class.getName() + ".DONOT";
    private static final Logger LOG = Logging.getLoggerInstance(Processor.class);

    public static final String XSD_CREATECACHES       = "createcaches.xsd";
    public static final String NAMESPACE_CREATECACHES = "http://www.mmbase.org/xmlns/createcaches";

    static {
        EntityResolver.registerSystemID(NAMESPACE_CREATECACHES + ".xsd", XSD_CREATECACHES, Processor.class);
    }

    /**
     * List with the configured {@link JobDefinition}s.
     */
    protected final Map<String, JobDefinition> list = Collections.synchronizedMap(new LinkedHashMap<String, JobDefinition>());

    String[] cacheManagers = new String[] {"streamsourcescaches", "videostreamsourcescaches", "audiostreamsourcescaches"};

    private File fileServletDirectory;

    public void setDirectory(File dir) {
        fileServletDirectory = dir;
    }
    public File getDirectory() {
        if (fileServletDirectory == null) {
            return FileServlet.getDirectory();
        } else {
            return fileServletDirectory;
        }
    }

    /**
     * The MMBase NodeManagers that contain 'caches', i.e. trancoded media objects.
     */
    public void setCacheManagers(String... cm) {
        cacheManagers = cm;
    }

    private String configFile = "streams/createcaches.xml";
    protected final ResourceWatcher watcher = new ResourceWatcher() {
            @Override
            public void onChange(String resource) {
                try {
                    LOG.debug("Reading " + resource);
                    Map<String, JobDefinition> newList = new LinkedHashMap<String, JobDefinition>();
                    Document document = getResourceLoader().getDocument(resource);

                    if (document != null) {
                        org.w3c.dom.NodeList ellist = document.getDocumentElement().getChildNodes();

                        Stage prevStage = Stage.RECOGNIZER;
                        for (int i = 0; i <= ellist.getLength(); i++) {
                            if (ellist.item(i) instanceof Element) {
                                Element el = (Element) ellist.item(i);
                                if (el.getTagName().equals("transcoder") || el.getTagName().equals("recognizer")) {
                                    String id = el.getAttribute("id");
                                    Transcoder transcoder;
                                    if (el.getTagName().equals("transcoder")) {
                                        transcoder = (Transcoder) Instantiator.getInstanceWithSubElement(el);
                                    } else {
                                        Recognizer recognizer = (Recognizer) Instantiator.getInstanceWithSubElement(el);
                                        transcoder = new RecognizerTranscoder(recognizer);
                                    }
                                    String in = el.getAttribute("in");
                                    String label = el.getAttribute("label");
                                    MimeType mimeType = new MimeType(el.getAttribute("mimetype"));
                                    LOG.debug("Created " + transcoder);
                                    Stage stage = Stage.valueOf(el.getTagName().toUpperCase());
                                    if (stage.ordinal() < prevStage.ordinal()) {
                                        LOG.warn("Wrong ordering (config err) " + stage + " < " + prevStage);
                                    }
                                    prevStage = stage;
                                    JobDefinition def = new JobDefinition(id, in.length() > 0 ? in : null, label.length() > 0 ? label : null, transcoder, mimeType, stage);
                                    org.w3c.dom.NodeList childs = el.getChildNodes();
                                    for (int j = 0; j <= childs.getLength(); j++) {
                                        if (childs.item(j) instanceof Element) {
                                            Element child = (Element) childs.item(j);
                                            if (child.getTagName().equals("loganalyzer")) {
                                                try {
                                                    Analyzer analyzer = (Analyzer) Instantiator.getInstance(child);
                                                    def.addAnalyzer(analyzer);
                                                } catch (Exception e) {
                                                    LOG.error(XMLWriter.write(child) + ": " + e.getMessage(), e);
                                                }

                                            }
                                        }
                                    }
                                    if (newList.containsKey(id)) {
                                        LOG.warn("" + newList + " already contains an entry with id " + id);
                                    }
                                    newList.put(id, def);

                                } else if (el.getTagName().equals("localhost") || el.getTagName().equals("server")) {
                                    if (! "streams/createcaches.xml".equals(resource)) {
                                        LOG.warn("Ignored " + XMLWriter.write(el));
                                    }
                                }
                            }
                        }
                    } else {
                        LOG.warn("No " + resource);
                    }
                    list.clear();
                    list.putAll(newList);
                    LOG.service("Reading of configuration file " + resource + " successfull. JobDefinitions now " + list);
                } catch (Exception e)  {
                    LOG.error(e.getClass() + " " + e.getMessage() + " In " + resource + " Transcoders now " + list + " (not changed)", e);
                }
            }
        };

    protected final void initWatcher() {
        LOG.service("Adding for " + this + " " + configFile);
        watcher.exit();
        watcher.add(configFile);
        watcher.onChange();
        watcher.start();
    }
    public Processor() {
        initWatcher();
    }
    Processor(final String configFile) {
        setConfigFile(configFile);
    }


    public final void setConfigFile(final String configFile) {
        LOG.service("Adding " + configFile);
        this.configFile = configFile;
        initWatcher();
    }

    protected static final Map<Integer, Job> runningJobs = Collections.synchronizedMap(new LinkedHashMap<Integer, Job>());

    /* Jobs user with this context has started */
    public static Set<Job> myJobs(UserContext u) {
        Set<Job> myjobs = new LinkedHashSet<Job>();
        synchronized(runningJobs) {
            for (Job j : runningJobs.values()) {
                if (j.getUser().equals(u.getIdentifier())) {
                    myjobs.add(j);
                }
            }
        }
        return myjobs;
    }
    public static Collection<Job> runningJobs() {
        return Collections.unmodifiableCollection(runningJobs.values());
    }

    public static Job getJob(Node node) {
        return runningJobs.get(node.getNumber());
    }

    protected static boolean removeJob(Node node) {
        Job job = runningJobs.get(node.getNumber());
        if (job.future != null && job.future.isDone()) {
            runningJobs.remove(node.getNumber());
            return true;
        } else if (job.future == null ) {
            LOG.warn("Job " + job + " removed, it did not run/start correctly?");
            runningJobs.remove(node.getNumber());
            return true;
        } else {
            job.logger.info("This job has not completed yet.");
            return false;
        }
    }

    public static String cancelJob(Node node) {
        if (node.getCloud().may(ActionRepository.getInstance().get("streams", "cancel_jobs"), null)) {
            Job job = runningJobs.get(node.getNumber());
            if (job == null) {
                return "No job for node #" + node.getNumber();
            } else {
                job.interrupt();
                String msg = "";
                if (job.future != null && job.future.isDone()) {
                    msg = "This job has already completed. ";
                    job.logger.info(msg);
                }
                if (job.future != null && job.future.cancel(true)) {
                    String message = msg + "Canceled job for node #" + node.getNumber() + " (" + job.future + ")";
                    job.logger.info(message);
                    removeJob(node);
                    return message;
                } else {
                    removeJob(node);
                    return msg + "Could not cancel " + job + " (did not run/start correctly?), but removed it anyway.";
                }
            }
        } else {
            return "You may not cancel jobs";
        }
    }

    public Map<String, JobDefinition> getConfiguration() {
        return Collections.unmodifiableMap(list);
    }


    /**
     * Creates and submits a job transcoding everything as configured for one source object, this
     * produces all new 'streamsourcescaches' as configured in createcaches.xml.
     * @param ntCloud   a non transactional cloud
     * @param node      node number of a source stream
     * @param logger    a logger that keeps track
     * @return job trans coding a source stream in (an)other stream(s)
     */
    private Job createJob(final Cloud ntCloud, final int node, final Map<String, JobDefinition> jdlist, final ChainedLogger logger) {
        synchronized(runningJobs) {
            Job job = runningJobs.get(node);
            if (job != null) {  // already running?
                LOG.warn("There is already a job running for node #" + node);
                return null;
            }
            assert node > 0;
            final Job thisJob = new Job(this, jdlist, ntCloud, logger);
            runningJobs.put(node, thisJob);
            thisJob.submit(ntCloud, node, logger);

            return thisJob;
        }
    }

    public Job createCaches(final Cloud ntCloud, final int node) {
        return createCaches(ntCloud, node, this.list);
    }

    /**
     * Creates caches nodes when not existing (or recreate) by making a transcoding Job
     * @param ntCloud   a non transactional cloud
     * @param node      node number of a source node
     * @param jdlist    job definitions
     * @return Job recognizing and/or transcoding the source stream
     */
    public Job createCaches(final Cloud ntCloud, final int node, final Map<String, JobDefinition> jdlist) {
        final ChainedLogger logger = new ChainedLogger(LOG);
        final Job thisJob = createJob(ntCloud, node, jdlist, logger);

        LOG.info("Triggering caches for #" + node + ":" + jdlist + "  -> " + thisJob);
        if (thisJob != null) {
            // If the node happens to be deleted before the future with cache creations is ready, cancel the future
            EventManager.getInstance().addEventListener(new WeakNodeEventListener() {
                    public void notify(NodeEvent event) {
                        if (event.getNodeNumber() == node && event.getType() == Event.TYPE_DELETE) {
                            LOG.info("Delete node event received for #" + node);
                            /* if (thisJob.future.cancel(true)) {
                                logger.info("Job canceled " + thisJob.future + " for " + event.getBuilderName() + " " + event.getNodeNumber());
                            } */
                        }
                    }
                    @Override
                    public String toString() {
                        return "Job canceled for " + node;
                    }
                });
        }
        return thisJob;
    }

    /**
     * Commits the mediastreamsources node, calls createCaches to create resulting streamscaches nodes
     * and start transcoding.
     */
    public void commit(final Node node, final Field field) {
        if (node.getCloud().getProperty(NOT) != null) {
            LOG.service("Not doing because of property");
            return;
        }
        if (node.getNumber() > 0) {
            LOG.info("url: " + node.getValueWithoutProcess("url"));
            if (node.isChanged(field.getName())) {
                LOG.service("For node " + node.getNumber() + ", the field '" + field.getName() + ":" + node.getStringValue(field.getName()) + "' is changed " + node.getChanged() + ". That means that we must schedule create caches");

                final Cloud ntCloud = node.getCloud().getNonTransactionalCloud();
                final int nodeNumber = node.getNumber();
                createCaches(ntCloud, nodeNumber);
            }
        } else {
            LOG.info("Cannot execute processor, because node has not yet a real number " + node);
        }
    }

    @Override
    protected Processor clone() throws CloneNotSupportedException {
        Processor clone = (Processor) super.clone();
        LOG.info("Cloned");
        clone.initWatcher();
        return clone;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }

    public void writeExternal(ObjectOutput stream) throws IOException {
    }

}
