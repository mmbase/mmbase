/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
sQuery
*/
package org.mmbase.module.lucene;

import java.util.*;
import java.io.File;
import org.w3c.dom.*;
import org.w3c.dom.NodeList;
import java.net.URL;
import javax.sql.DataSource;

import org.mmbase.bridge.*;
import org.mmbase.storage.search.*;
import org.mmbase.cache.*;
import org.mmbase.module.ReloadableModule;

import org.mmbase.core.event.*;
import org.mmbase.module.core.*;
import org.mmbase.bridge.util.xml.query.*;
import org.mmbase.bridge.util.BridgeCollections;
import org.mmbase.util.*;
import org.mmbase.util.xml.*;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.storage.implementation.database.DatabaseStorageManagerFactory;

import java.util.concurrent.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.queryParser.ParseException;
import org.mmbase.module.lucene.extraction.*;

/**
 * This is the implementation of a 'Lucene' module. It's main job is to bootstrap mmbase lucene
 * indexing, and provide some functions to give access to lucene functionality in an MMBase way.
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id$
 **/
public class Lucene extends ReloadableModule implements NodeEventListener, RelationEventListener, IdEventListener, AssignmentEvents.Listener {

    public static final String PUBLIC_ID_LUCENE_2_0 = "-//MMBase//DTD luceneindex config 2.0//EN";
    public static final String DTD_LUCENE_2_0 = "luceneindex_2_0.dtd";

    /** Most recent Lucene config DTD */
    public static final String PUBLIC_ID_LUCENE = PUBLIC_ID_LUCENE_2_0;
    public static final String DTD_LUCENE = DTD_LUCENE_2_0;

    /**
     * But we use XSD now!
     * @todo Support for DTD's can be dropped, it was never released.
     */
    public static final String XSD_LUCENE_1_0 = "luceneindex.xsd";
    public static final String NAMESPACE_LUCENE_1_0 = "http://www.mmbase.org/xmlns/luceneindex";

    /**
     * Most recend namespace
     */
    public static final String NAMESPACE_LUCENE = NAMESPACE_LUCENE_1_0;

    protected MMBase mmbase;

    /**
     * Parameter constants for Lucene functions.
     */
    protected final static Parameter<String> VALUE = new Parameter<String>("value", String.class);
    static { VALUE.setDescription("the search term(s)"); }

    protected final static Parameter<String> INDEX = new Parameter<String>("index", String.class);
    static { INDEX.setDescription("the name of the index to search in"); }

    protected final static Parameter/*<List<String>>*/MACHINES =
            new Parameter/*<List<String>>*/(
            "machines",
            List.class, null);
    static { MACHINES.setDescription("the machines on which to execute a full index"); }

    protected final static Parameter<Boolean> COPY = new Parameter<Boolean>("copy", Boolean.class, Boolean.FALSE);
    static { INDEX.setDescription("use the copy of the index (used for full index)"); }

    protected final static Parameter/*<Class<? extends IndexDefinition>>*/CLASS =
            new Parameter/*<Class<? extends IndexDefinition>>*/("class",
            Class.class,
            IndexDefinition.class);
    static { INDEX.setDescription("the class of indices to search in (default to all classes)"); }

    protected final static Parameter<String> SORTFIELDS = new Parameter<String>("sortfields", String.class);
    protected final static Parameter<String> FIELDS     = new Parameter<String>("fields", String.class);
    protected final static Parameter<String> ANALYZER   = new Parameter<String>("analyzer", String.class);
    protected final static Parameter<Integer> OFFSET = new Parameter<Integer>("offset", Integer.class, 0);
    protected final static Parameter<Integer> EXPLAIN = new Parameter<Integer>("explain", Integer.class, 0);
    static { OFFSET.setDescription("for creating sublists"); }

    protected final static Parameter<Integer> MAX = new Parameter<Integer>("max", Integer.class, Integer.MAX_VALUE);
    static { MAX.setDescription("for creating sublists"); }

    protected final static Parameter<String> EXTRACONSTRAINTS = new Parameter<String>("extraconstraints", String.class);
    static { EXTRACONSTRAINTS.setDescription("@see org.mmbase.module.lucene.Searcher#createQuery()"); }

    protected final static Parameter<String>  FILTER = new Parameter<String>("filter", String.class);

    /*
     * Determines what to do when a search fails (i.e. due to a Lucene TooManyClauses execption).
     * Default is throw (throw the exception).
     * You can set it to 'ignore', which will instead cause the function to return an empty list (or search size of 0).
     * Note that in general, it may be better to catch the exception in the page instead.
     */
    protected final static Parameter<String>  ONFAIL = new Parameter<String>("onfail", String.class);

    /*
    protected final static Parameter EXTRACONSTRAINTSLIST = new Parameter("constraints", List.class);
    static { EXTRACONSTRAINTSLIST.setDescription("@see org.mmbase.module.lucene.Searcher#createQuery()"); }
    */

    protected final static Parameter<String> IDENTIFIER = new Parameter<String>("identifier", String.class);
    static { IDENTIFIER.setDescription("Normally a node number, identifier (a number of) lucene document(s) in an index."); }

    static {
        EntityResolver.registerPublicID(PUBLIC_ID_LUCENE_2_0, DTD_LUCENE_2_0, Lucene.class);
        EntityResolver.registerPublicID(NAMESPACE_LUCENE_1_0, XSD_LUCENE_1_0, Lucene.class);
        EntityResolver.registerPublicID(PUBLIC_ID_LUCENE, DTD_LUCENE, Lucene.class);
    }

    // initial wait time after startup, default 2 minutes. This can be needed to give e.g. CCS time
    // to deploy

    private static final long INITIAL_WAIT_TIME = 2 * 60 * 1000;
    private static final long WAIT_TIME = 5 * 1000;

    private static final String INDEX_CONFIG_FILE = "utils/luceneindex.xml";

    private static final Logger log = Logging.getLoggerInstance(Lucene.class);

    public static Lucene getLucene() {
        //return getModule(Lucene.class); // java 1.5
        return (Lucene) getModule("lucene");
    }


    private String indexPath = null;
    private Scheduler scheduler = null;
    private String defaultIndex = null;
    private final Set<String>           disableIndexes = new HashSet<String>();
    private final Map<String, Indexer>  indexerMap    = new ConcurrentHashMap<String, Indexer>();
    private final Map<String, Searcher> searcherMap   = new ConcurrentHashMap<String, Searcher>();
    private boolean readOnly = false;
    private boolean incrementalUpdates = true;
    private String master; // If readonly, the machine name of the mmbase which is responsible for the index

    private long initialWaitTime = INITIAL_WAIT_TIME;
    private long waitTime  = WAIT_TIME;


    private List<String> configErrors = new ArrayList<String>();
    private Date configReadTime = new Date(0);
    /**
     * keeps track of startnodes to not reindex them
     * @todo If a startnode changes itself you _might_ want to reindex.
     */
    private Set<String> startNodes = new HashSet<String>();

    private ChainedReleaseStrategy defaultStrategy = new ChainedReleaseStrategy();

    /**
     * Returns whether an element has a certain attribute, either an unqualified attribute or an attribute that fits in the
     * lucene namespace
     */
    static public boolean hasAttribute(Element element, String localName) {
        return element.hasAttributeNS(NAMESPACE_LUCENE, localName) || element.hasAttribute(localName);
    }

    /**
     * Returns the value of a certain attribute, either an unqualified attribute or an attribute that fits in the
     * lucene namespace
     */
    static public String getAttribute(Element element, String localName) {
        if (element.hasAttributeNS(NAMESPACE_LUCENE, localName)) {
            return element.getAttributeNS(NAMESPACE_LUCENE, localName);
        } else {
            return element.getAttribute(localName);
        }
    }

    /**
     * This function starts a full Index of Lucene.
     * This may take a while.
     * This function can be called through the function framework.
     */
    protected Function<Void> fullIndexFunction = new AbstractFunction<Void>("fullIndex", new Parameter[] {INDEX, MACHINES}, ReturnType.VOID) {
        private static final long serialVersionUID = 0L;
        public Void getFunctionValue(Parameters arguments) {
            String index = arguments.get(INDEX);
            List<String> machines = (List<String>) arguments.get(MACHINES);
            EventManager.getInstance().propagateEvent(new AssignmentEvents.Event(index, machines, AssignmentEvents.FULL, null,  null));
            return null;
        }
    };
    {
        addFunction(fullIndexFunction);
    }

    public void notify(AssignmentEvents.Event event) {
        log.info("Received " + event);
        if (event.getMachines().contains(MMBase.getMMBase().getMachineName())) {
            switch(event.getType()) {
            case AssignmentEvents.FULL: {
                    String index = event.getIndex();
                    if (scheduler == null) throw new RuntimeException("Read only");
                    if (index == null || "".equals(index)) {
                        scheduler.fullIndex();
                    } else {
                        scheduler.fullIndex(index);
                    }
                } break;
            case AssignmentEvents.UPDATE:
                throw new UnsupportedOperationException();
            case AssignmentEvents.DELETE: {
                    if (scheduler == null) throw new RuntimeException("Read only");
                    if(!readOnly){
                        String index      = event.getIndex();
                        String identifier = event.getIdentifier();
                        Class<? extends IndexDefinition>  klass      = event.getClassFilter();
                        if(index == null || "".equals(index)){
                            scheduler.deleteIndex(identifier, klass);
                        } else {
                            scheduler.deleteIndex(identifier, index);
                        }
                    }
                } break;
            case AssignmentEvents.CLEAR: {
                    if (readOnly) {
                        throw new IllegalStateException("This lucene is readonly");
                    }
                    String index = event.getIndex();
                    Indexer indexer = indexerMap.get(index);
                    boolean copy = event.getCopy();
                    indexer.clear(copy);
                } break;
            }
        } else {
            log.info("Event " + event + " ignored, because MMBase machinename '" + MMBase.getMMBase().getMachineName() + "' is not equal to one in event '" + event.getMachines() + "'.");
        }
    }

    /**
     * This function deletes an indexed entry from an index
     * if the Parameter 'index' has value null, all indexes are iterated over, otherwise
     * the right index is addressed.
     */
    protected Function<Void> deleteIndexFunction = new AbstractFunction<Void>("deleteIndex", new Parameter<?>[] {INDEX, MACHINES, IDENTIFIER, CLASS}, ReturnType.VOID) {
        private static final long serialVersionUID = 0L;
        public Void getFunctionValue(Parameters arguments) {
            String index = arguments.get(INDEX);
            String identifier = arguments.get(IDENTIFIER);

            @SuppressWarnings("unchecked")
            Class<? extends IndexDefinition> klass = (Class<? extends IndexDefinition>) arguments.get(CLASS);
            @SuppressWarnings("unchecked")
            List<String> machines = (List<String>) arguments.get(MACHINES);
            EventManager.getInstance().propagateEvent(new AssignmentEvents.Event(index, machines, AssignmentEvents.DELETE, identifier, klass));
            return null;
        }
    };


    {
        addFunction(deleteIndexFunction);
    }



    /**
     * This function can be called through the function framework.
     * It (re)loads the index for a specific item (identified by 'identifier' parameter).
     */
    //protected Function<Void> updateIndexFunction = new AbstractFunction<Void>("updateIndex", new Parameter(IDENTIFIER, true),  CLASS) {
    protected Function<Void> updateIndexFunction = new AbstractFunction<Void>("updateIndex",
            new Parameter<?>[]{new Parameter<String>(IDENTIFIER, true), CLASS},
            ReturnType.VOID) {
        private static final long serialVersionUID = 0L;
        public Void getFunctionValue(Parameters arguments) {
            if (scheduler == null) {
                throw new RuntimeException("Read only");
            }
            scheduler.updateIndex(arguments.getString(IDENTIFIER), (Class<? extends IndexDefinition>) arguments.get(CLASS));
            return null;
        }
    };


    {
        addFunction(updateIndexFunction);
    }


    /**
     * This function returns the status of the scheduler. For possible values see: Lucene.Scheduler
     */
    //protected Function<Integer> statusFunction = new AbstractFunction<Integer>("status") {
    protected Function<Integer> statusFunction = new AbstractFunction<Integer>("status", Parameter.EMPTY, ReturnType.INTEGER) {
        private static final long serialVersionUID = 0L;
        public Integer getFunctionValue(Parameters arguments) {
            return scheduler == null ? Scheduler.READONLY : scheduler.getStatus();
        }
    };
    {
        addFunction(statusFunction);
    }
    //protected Function<String> statusDescriptionFunction = new AbstractFunction<String>("statusdescription", Parameter.LOCALE) {
    protected Function<String> statusDescriptionFunction = new AbstractFunction<String>("statusdescription", new Parameter<?>[] {Parameter.LOCALE, new Parameter<Boolean>("assignment",  Boolean.class, Boolean.TRUE)}, ReturnType.STRING) {
        private static final long serialVersionUID = 0L;
        public String getFunctionValue(Parameters arguments) {
            Locale locale = arguments.get(Parameter.LOCALE);
            SortedMap map = SortedBundle.getResource("org.mmbase.module.lucene.resources.status",  locale,
                                                     getClass().getClassLoader(),
                                                     SortedBundle.getConstantsProvider(Scheduler.class), Integer.class, null);
            String desc = "" + map.get(new Integer(scheduler == null ? Scheduler.READONLY : scheduler.getStatus()));
            boolean appendAssignment = (Boolean) arguments.get("assignment");
            if (appendAssignment) {
                Object ass = (scheduler == null ? null : scheduler.getAssignment());
                return desc + (ass == null ? "" : " " + ass);
            } else {
                return desc;
            }
        }
    };
    {
        addFunction(statusDescriptionFunction);
    }

    protected Function <Scheduler.Assignment> assignmentFunction = new AbstractFunction<Scheduler.Assignment> ("assignment", Parameter.EMPTY,
            new ReturnType<Scheduler.Assignment>(Scheduler.Assignment.class, "")) {
        private static final long serialVersionUID = 0L;
        public Scheduler.Assignment getFunctionValue(Parameters arguments) {
            return scheduler == null ? null : scheduler.getAssignment();
        }
    };
    {
        addFunction(assignmentFunction);
    }


    protected Function<Collection<Scheduler.Assignment>> queueFunction = new AbstractFunction<Collection<Scheduler.Assignment>>("queue", Parameter.EMPTY) {
        private static final long serialVersionUID = 0L;
        public Collection<Scheduler.Assignment> getFunctionValue(Parameters arguments) {
            if (scheduler == null) {
                return Collections.emptyList();
            }
            return  scheduler.getQueue();
        }
    };
    {
        addFunction(queueFunction);
    }

    protected Function <Void> waitFunction = new AbstractFunction<Void> ("wait", Parameter.EMPTY, ReturnType.VOID) {
        private static final long serialVersionUID = 0L;
            public Void getFunctionValue(Parameters arguments) {
                scheduler.waitForReady();
                return null;

            }
        };
    {
        addFunction(waitFunction);
    }



    //protected Function<Boolean> readOnlyFunction = new AbstractFunction<Boolean>("readOnly"){
    protected Function<Boolean> readOnlyFunction = new AbstractFunction <Boolean>("readOnly", Parameter.EMPTY, ReturnType.BOOLEAN) {
        private static final long serialVersionUID = 0L;
        public Boolean getFunctionValue(Parameters arguments) {
            return readOnly;
        }
    };
    {
        addFunction(readOnlyFunction);

        addFunction(new AbstractFunction<String>("master", Parameter.EMPTY, ReturnType.STRING) {
            private static final long serialVersionUID = 0L;
                public String getFunctionValue(Parameters argumnets) {
                    return master;
                }
            });
    }

    /**
     * This function returns Set with the names of all configured indexes (ordered alphabeticly)
     */
    protected Function<Set<String>>  listFunction = new AbstractFunction<Set<String>> ("list", Parameter.EMPTY, new ReturnType<Set<String>>(Set.class, "")) {
        private static final long serialVersionUID = 0L;
            public Set<String> getFunctionValue(Parameters arguments) {
                return new TreeSet<String>(indexerMap.keySet());
            }

        };
    /**
     * This function returns Set with the names of all configured indexes (ordered alphabeticly)
     */
    protected Function<Set<Indexer>>  indexerListFunction = new AbstractFunction<Set<Indexer>> ("indexerList", Parameter.EMPTY, new ReturnType<Set<Indexer>>(Set.class, "")) {
        private static final long serialVersionUID = 0L;
            public Set<Indexer> getFunctionValue(Parameters arguments) {
                return new TreeSet<Indexer>(indexerMap.values());
            }

        };
    {
        addFunction(listFunction);
        addFunction(indexerListFunction);
    }

    /**
     *This function returns the description as configured for a specific index and a specific locale.
     */
    protected Function<String>  descriptionFunction = new AbstractFunction<String>("description", new Parameter<?>[] {INDEX, Parameter.LOCALE}, ReturnType.STRING) {
        private static final long serialVersionUID = 0L;
        public String getFunctionValue(Parameters arguments) {
            String key = arguments.getString(INDEX);
            Locale locale = arguments.get(Parameter.LOCALE);
            Indexer index = indexerMap.get(key);
            return index.getDescription().get(locale);
        }

    };
    {
        addFunction(descriptionFunction);
    }

    /**
     * This function starts a search fro a given string.
     * This function can be called through the function framework.
     */
    protected Function<org.mmbase.bridge.NodeList> searchFunction = new AbstractFunction<org.mmbase.bridge.NodeList>("search", new Parameter<?>[] {VALUE, INDEX, FIELDS, SORTFIELDS, OFFSET, MAX, EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER, ONFAIL, EXPLAIN}, ReturnType.NODELIST) {
        private static final long serialVersionUID = 0L;
            public org.mmbase.bridge.NodeList getFunctionValue(Parameters arguments) {
                String value = arguments.getString(VALUE);
                String index = arguments.getString(INDEX);
                String sortFields  = arguments.get(SORTFIELDS);
                String[] sortFieldArray = sortFields == null ? null : StringSplitter.split(sortFields).toArray(new String[]{});
                String fields = arguments.get(FIELDS);
                String[] fieldArray = fields == null || "".equals(fields) ? getSearcher(index).allIndexedFields : StringSplitter.split(fields).toArray(new String[]{});
                String onFail = arguments.getString(ONFAIL);
                if (onFail != null) onFail = onFail.toLowerCase();
                Analyzer analyzer = null;
                String an = arguments.get(ANALYZER);
                if (an != null && ! "".equals(an)) {
                    try {
                        Class clazz = Class.forName(an);
                        analyzer = (Analyzer) clazz.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                int offset         = arguments.get(OFFSET);
                int max            = arguments.get(MAX);
                int explain        = arguments.get(EXPLAIN);
                String extraConstraints = arguments.getString(EXTRACONSTRAINTS);
                String filter     = arguments.getString(FILTER);
                if (log.isDebugEnabled()) {
                    log.debug("using analyzer " + analyzer);
                }
                /*
                List moreConstraints = (List) arguments.get(EXTRACONSTRAINTSLIST);
                if (moreConstraints != null && moreConstraints.size() > 0) {
                    StringBuilder ec = new StringBuilder(extraConstraints == null ? "" : extraConstraints + " ");
                    Iterator i = moreConstraints.iterator();
                    while (i.hasNext()) {
                        ec.append(i.next().toString());
                        ec.append(" ");
                    }
                    extraConstraints = ec.toString().trim();
                }
                */
                Cloud cloud         = arguments.get(Parameter.CLOUD);
                cloud.setProperty(Cloud.PROP_XMLMODE, "flat"); //
                try {
                    return getSearcher(index).search(cloud, value,
                                                     Searcher.createFilter(filter),
                                                     Searcher.getSort(sortFieldArray),
                                                     analyzer,
                                                     Searcher.createQuery(extraConstraints),
                                                     fieldArray,
                                                     offset, max,
                                                     explain);
                } catch (BooleanQuery.TooManyClauses tmc) {
                    if ("ignore".equals(onFail)) {
                        if (log.isDebugEnabled()) {
                            log.debug(tmc);
                        }
                        return org.mmbase.bridge.util.BridgeCollections.EMPTY_NODELIST;
                    } else {
                        throw tmc;
                    }
                } catch (ParseException pe) {
                    // search function is typically used in a JSP and the 'value' parameter filled by web-site users.
                    // They may not fill the log with errors!
                    if (log.isDebugEnabled()) {
                        log.debug(pe);
                    }
                    return BridgeCollections.EMPTY_NODELIST;
                }
            }
        };
    {
        addFunction(searchFunction);
    }


    /**
     * This function returns the size of a query on an index.
     */
    protected Function<Integer> searchSizeFunction = new AbstractFunction<Integer>("searchsize", new Parameter<?>[] {VALUE, INDEX, COPY, FIELDS, EXTRACONSTRAINTS, FILTER, Parameter.CLOUD, ANALYZER, ONFAIL}, ReturnType.INTEGER) {
        private static final long serialVersionUID = 0L;
        public Integer getFunctionValue(Parameters arguments) {
            String value = arguments.getString(VALUE);
            String index = arguments.getString(INDEX);
            String extraConstraints = arguments.getString(EXTRACONSTRAINTS);
            String filter = arguments.getString(FILTER);
            String fields = arguments.get(FIELDS);
            String[] fieldArray = fields == null || "".equals(fields) ? getSearcher(index).allIndexedFields : StringSplitter.split(fields).toArray(new String[]{});
            String onFail = arguments.getString(ONFAIL);
            if (onFail != null) onFail = onFail.toLowerCase();
            Cloud cloud  =  arguments.get(Parameter.CLOUD);
            Analyzer analyzer = null;
            String an = arguments.get(ANALYZER);
            if (an != null && ! "".equals(an)) {
                try {
                    Class clazz = Class.forName(an);
                    analyzer = (Analyzer) clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                return getSearcher(index).searchSize(cloud, value,  Searcher.createFilter(filter), analyzer, Searcher.createQuery(extraConstraints), fieldArray, Boolean.TRUE.equals(arguments.get(COPY)));
            } catch (BooleanQuery.TooManyClauses tmc) {
                if ("ignore".equals(onFail)) {
                    if (log.isDebugEnabled()) {
                        log.debug(tmc);
                    }
                    return 0;
                } else {
                    throw tmc;
                }
            }
        }
    };
    {
        addFunction(searchSizeFunction);
    }


    protected Function<Void> clearDirectory = new AbstractFunction<Void>("clearDirectory", new Parameter<?>[] {INDEX, MACHINES, COPY}, ReturnType.VOID) {
        private static final long serialVersionUID = 0L;
        public Void getFunctionValue(Parameters arguments) {
            String index = arguments.get(INDEX);
            List<String> machines = (List<String>) arguments.get(MACHINES);
            AssignmentEvents.Event event = new AssignmentEvents.Event(index, machines, AssignmentEvents.CLEAR, null, null);
            event.setCopy(arguments.get(COPY));
            EventManager.getInstance().propagateEvent(event);
            return null;
        }
    };
    {
        addFunction(clearDirectory);
    }
    protected Function<Integer> fullIndexing = new AbstractFunction<Integer>("fullIndexing", new Parameter<?>[] {INDEX, COPY}, ReturnType.INTEGER) {
        private static final long serialVersionUID = 0L;
            public Integer  getFunctionValue(Parameters arguments) {
                String index = arguments.getString(INDEX);
                Searcher searcher = getSearcher(index);
                return searcher.getFullIndexSize();
            }
        };
    {
        addFunction(fullIndexing);
    }

    protected Function<Integer> unAssignFunction = new AbstractFunction<Integer>("unassign", new Parameter<?>[] {new Parameter<Integer>("id", Integer.class, true)}, ReturnType.INTEGER) {
        private static final long serialVersionUID = 0L;
            public Integer getFunctionValue(Parameters arguments) {
                int id = (Integer) arguments.get("id");
                if (scheduler != null) {
                    return scheduler.unAssign(id);
                } else {
                    return 0;
                }
            }
        };
    {
        addFunction(unAssignFunction);
    }
    protected Function<String> interruptFunction = new AbstractFunction<String>("interrupt", Parameter.EMPTY, ReturnType.STRING) {
        private static final long serialVersionUID = 0L;
            public String getFunctionValue(Parameters arguments) {
                if (scheduler != null) {
                    if (scheduler.getStatus() > Scheduler.IDLE) {
                        log.info("Sending interrupt to " + scheduler);
                        scheduler.interrupt();
                        return "Interrupted";
                    } else {
                        scheduler.interrupt();
                        return "Interrupted (though idle)";
                    }
                } else  {
                    return "not yet running";
                }

            }
        };
    {
        addFunction(interruptFunction);
    }



    //protected Function<Date> lastFullIndexFunction = new AbstractFunction<Date>("last", INDEX) {
    protected Function<Date> lastFullIndexFunction = new AbstractFunction<Date>("last", new Parameter<?>[] {INDEX}, new ReturnType<Date>(Date.class, "")) {
        private static final long serialVersionUID = 0L;
        public Date getFunctionValue(Parameters arguments) {
            String key = arguments.get(INDEX);
            Indexer index = indexerMap.get(key);
            if (index != null) {
                return index.getLastFullIndex();
            } else {
                return null;
            }
        }
    };
    protected Function<Long> lastFullIndexDurationFunction = new AbstractFunction<Long>("lastduration", new Parameter[] {INDEX}, ReturnType.LONG) {
        private static final long serialVersionUID = 0L;
        public Long getFunctionValue(Parameters arguments) {
            String key = arguments.get(INDEX);
            Indexer index = indexerMap.get(key);
            if (index != null) {
                return index.getLastFullIndexDuration();
            } else {
                return null;
            }
        }
    };
    {
        addFunction(lastFullIndexFunction);
        addFunction(lastFullIndexDurationFunction);

        //addFunction(new AbstractFunction<Indexer>("default") {
        addFunction(new AbstractFunction<Indexer>("default", Parameter.EMPTY, new ReturnType<Indexer>(Indexer.class, "")) {
            private static final long serialVersionUID = 0L;
            public Indexer getFunctionValue(Parameters arguments) {
                return indexerMap.get(defaultIndex);
            }
        });
        addFunction(new AbstractFunction<String>("path", Parameter.EMPTY, ReturnType.STRING){
            private static final long serialVersionUID = 0L;

            public String getFunctionValue(Parameters arguments) {
                return Lucene.this.indexPath;
            }
        });

        addFunction(new AbstractFunction<List<String>>("errors", new Parameter[] {INDEX, OFFSET, MAX}, new ReturnType<List<String>>(List.class, "")) {
            private static final long serialVersionUID = 0L;
            public List<String> getFunctionValue(Parameters arguments) {
                String index = arguments.getString(INDEX);
                List<String> errors = indexerMap.get(index).getErrors();
                int offset = arguments.get(OFFSET);
                int toIndex = offset + arguments.get(MAX);
                if (toIndex > errors.size()) {
                    toIndex = errors.size();
                }
                return indexerMap.get(index).getErrors().subList(offset, toIndex);
            }
        });
        addFunction(new AbstractFunction<Long>("nodes", new Parameter[] {INDEX}, ReturnType.LONG) {
            private static final long serialVersionUID = 0L;
            public Long getFunctionValue(Parameters arguments) {
                String index = arguments.getString(INDEX);
                return searcherMap.get(index).getNumberOfProducedNodes();
            }
            });
        addFunction(new AbstractFunction<Date>("config",Parameter.EMPTY, new ReturnType<Date>(Date.class, "")) {
            private static final long serialVersionUID = 0L;
            public Date getFunctionValue(Parameters arguments) {
                return configReadTime;
            }
        });

    }

    private ContentExtractor factory;

    @Override
    public void init() {
        init(true);
    }

    protected void init(final boolean initialWait) {
        super.init();

        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {

                    // Make sure that everthing is well initalized, otherise the binary file base
                    // path may still be empty.
                    org.mmbase.bridge.ContextProvider.getDefaultCloudContext().assertUp();

                    mmbase = MMBase.getMMBase();
                    String databaseName = "";
                    String binaryFileBasePath = "";
                    //try to get the index path from the strorage configuration
                    try {
                        DatabaseStorageManagerFactory dsmf = (DatabaseStorageManagerFactory)mmbase.getStorageManagerFactory();
                        String binaries = dsmf.getBinaryFileBasePath().toString();
                        if (binaries != null) {  // this test is needed for compatibility betwen 1.8 and 1.9
                            if (! binaries.endsWith(File.separator)) {
                                binaries += File.separator;
                            }
                            binaryFileBasePath = binaries;
                            databaseName = dsmf.getDatabaseName();
                        }
                    } catch(Exception e){}

                    String path = getInitParameter("indexpath");
                    if (path != null) {
                        indexPath = path;
                        indexPath = indexPath.replace("$BINARYFILEBASEPATH", binaryFileBasePath);
                        indexPath = indexPath.replace("$DATABASE", databaseName);

                        //hack, to get backslashes working on windows
                        indexPath = indexPath.replaceAll("/+", "/");
                        indexPath = indexPath.replace('/', File.separatorChar);

                        log.service("found module parameter for lucene index path : " + indexPath);
                    } else {
                        indexPath = binaryFileBasePath + "lucene" + File.separator + databaseName;
                    }

                    if(indexPath != null) {
                        log.service("found storage configuration for lucene index path : " + indexPath);
                    } else {
                        // expand the default path (which is relative to the web-application)
                        indexPath = MMBaseContext.getServletContext().getRealPath(indexPath);
                        log.service("fall back to default for lucene index path : " + indexPath);
                    }


                    if (initialWait) {
                        // initial wait time?
                        String time = getInitParameter("initialwaittime");
                       if (time != null) {
                           try {
                               initialWaitTime = Long.parseLong(time);
                               if (log.isDebugEnabled()) {
                                   log.debug("Set initial wait time to " + time + " milliseconds");
                               }
                           } catch (NumberFormatException nfe) {
                               log.warn("Invalid value '" + time + "' for property 'initialwaittime'");
                           }
                       }
                       try {
                           if (initialWaitTime > 0) {
                               log.service("Sleeping " + (initialWaitTime / 1000) + " seconds for initialisation");
                               Thread.sleep(initialWaitTime);
                           }
                       } catch (InterruptedException ie) {
                           //return;
                       }
                    }


                    factory = ContentExtractor.getInstance();

                    String incrementalUpdatesSetting = getInitParameter("incrementalupdates");
                    incrementalUpdates = "true".equals(incrementalUpdatesSetting);
                    log.info("Setting incremental index updates to " + incrementalUpdates);

                    String readOnlySetting = getInitParameter("readonly");
                    while (readOnlySetting != null && readOnlySetting.startsWith("system:")) {
                        try {
                            readOnlySetting = System.getProperty(readOnlySetting.substring(7));
                        } catch (SecurityException se) {
                            log.info(se);
                            break;
                        }
                    }
                    if (readOnlySetting != null) {
                        if (readOnlySetting.startsWith("host:")) {
                            String host = readOnlySetting.substring(5);
                            try {
                                boolean write =
                                    java.net.InetAddress.getLocalHost().getHostName().equals(host) ||
                                    (System.getProperty("catalina.base") + "@" + java.net.InetAddress.getLocalHost().getHostName()).equals(host) ||
                                    mmbase.getMachineName().equals(host);
                                readOnly = ! write;
                                if (readOnly) {
                                    master = getInitParameter("master");
                                    if (master == null) {
                                        master = host;
                                    }
                                }
                            } catch (java.net.UnknownHostException uhe) {
                                log.error(uhe);
                            }
                        } else {
                            readOnly = "true".equals(readOnlySetting);
                            if (readOnly) {
                                master = getInitParameter("master");
                            }
                        }
                    }
                    if (readOnly) {
                        log.info("Lucene module of this MMBase will be READONLY. Responsible for the index is " + (master != null ? master : "UKNOWN"));
                    }

                    String time = getInitParameter("waittime");
                    if (time != null) {
                        try {
                            waitTime = Long.parseLong(time);
                            if (log.isDebugEnabled()) {
                                log.debug("Set wait time to " + time + " milliseconds. This long assigments remain scheduled, and can still be canceled.");
                            }
                        } catch (NumberFormatException nfe) {
                            log.warn("Invalid value '" + time +" ' for property 'waittime'");
                        }
                    }
                    if (!readOnly) {
                        scheduler = new Scheduler();
                    }

                    ResourceWatcher watcher = new ResourceWatcher() {
                            public void onChange(String resource) {
                                readConfiguration(resource);
                            }
                        };
                    watcher.add(INDEX_CONFIG_FILE);
                    watcher.onChange();
                    watcher.start();

                    if (!readOnly) {
                        log.service("Module Lucene started");
                        // full index ?
                        String fias = getInitParameter("fullindexatstartup");
                        scheduler.start();
                        if ("true".equals(fias)) {
                            log.info("Configured to run a full index at startup, so doing that now");
                            scheduler.fullIndex();
                        }
                    } else {
                        log.service("No scheduler started, because read-only");
                    }
                }
            });

    }



    private Cloud cloud = null;
    protected Cloud getCloud() {
        if (cloud != null && !cloud.getUser().isValid()) cloud = null;
        while (cloud == null) {
            try {
                cloud = LocalContext.getCloudContext().getCloud("mmbase", "class", null);
                cloud.setProperty(Cloud.PROP_XMLMODE, "flat");
                log.info("Using cloud of " + cloud.getUser().getIdentifier() + "(" + cloud.getUser().getRank() + ") to lucene index.");
            } catch (Throwable t) {
                log.error(t.getClass().getName() + " " + t.getMessage());
            }
            if (cloud == null) {
                try {
                    log.info("No cloud found, waiting for 5 seconds");
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    return null;
                }
            }
        }
        return cloud;
    }


    @Override
    public void shutdown() {
        EventManager.getInstance().removeEventListener(assignmentsListener);
        EventManager.getInstance().removeEventListener(idListener);
        if (scheduler != null) {
            log.service("Stopping Lucene Scheduler");
            scheduler.interrupt();
        }
        indexerMap.clear();
        for (Searcher searcher : searcherMap.values()) {
            searcher.shutdown();
        }
        searcherMap.clear();
        scheduler = null;

    }

    public void reload() {
        shutdown();
        init(false);
    }

    @Override
    public String getDescription() {
        return "This module performs lucene searches and maintains indices";
    }

    /**
     * MMBase Queries and sub-queries
     */
    MMBaseIndexDefinition createIndexDefinition (Element queryElement, Set<String> allIndexedFieldsSet, boolean storeText, boolean mergeText, String relateFrom, Analyzer analyzer) {
        try {
            if (Lucene.hasAttribute(queryElement, "optimize")) {
                String optimize = Lucene.getAttribute(queryElement, "optimize");
                storeText = optimize.equals("none");
                mergeText = optimize.equals("full");
            }

            QueryConfigurer configurer = new IndexConfigurer(allIndexedFieldsSet, storeText, mergeText);

            MMBaseIndexDefinition queryDefinition = (MMBaseIndexDefinition) QueryReader.parseQuery(queryElement, configurer, getCloud(), relateFrom);
            queryDefinition.setAnalyzer(analyzer);
            // do not cache these queries
            queryDefinition.query.setCachePolicy(CachePolicy.NEVER);

            queryDefinition.getReleaseStrategy().removeAllStrategies();
            queryDefinition.getReleaseStrategy().fillFromXml(queryElement.getOwnerDocument().getDocumentElement());
            if (queryDefinition.getReleaseStrategy().size() == 0) {
                Iterator i = defaultStrategy.iterator();
                while (i.hasNext()) {
                    ReleaseStrategy rs = (ReleaseStrategy) i.next();
                    queryDefinition.getReleaseStrategy().addReleaseStrategy(rs);
                }
            }


            // MM: I think the follwing functionality should be present on MMBaseIndexDefinition itself. and not on Lucene.
            // And of course, the new event-mechanism must be used.
            if (!readOnly) {
                MMBase mmb = MMBase.getMMBase();
                // register. Unfortunately this can currently only be done through the core
                //for (Step step : queryDefinition.query.getSteps() ) {
                for (Iterator i = queryDefinition.query.getSteps().iterator(); i.hasNext(); ) {
                    Step step = (Step) i.next();
                    MMObjectBuilder builder = mmb.getBuilder(step.getTableName());
                    if (log.isDebugEnabled()) {
                        log.debug("Observing for builder " + builder.getTableName() + " for  '" + queryDefinition + "'");
                    }
                    builder.addEventListener(this);
                }
            }


            String elementName = queryDefinition.elementManager.getName();
            NodeList childNodes = queryElement.getChildNodes();
            for (int k = 0; k < childNodes.getLength(); k++) {
                if (childNodes.item(k) instanceof Element) {
                    Element childElement = (Element) childNodes.item(k);
                    if ("related".equals(childElement.getLocalName()) || "relatednodes".equals(childElement.getLocalName())) {
                        MMBaseIndexDefinition subIndex = createIndexDefinition(childElement, allIndexedFieldsSet, storeText, mergeText, elementName, analyzer);
                        queryDefinition.subQueries.add(subIndex);
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Configured builder " + elementName + " with query:" + queryDefinition.query);
            }
            return queryDefinition;
        } catch (org.mmbase.bridge.NotFoundException notFoundException) {
            configErrors.add(notFoundException.getMessage());
            log.info("NotFoundException in " + queryElement.getOwnerDocument().getDocumentURI() + " for index " + XMLWriter.write(queryElement, true, true) + " " + notFoundException.getMessage());
        } catch (Exception e) {
            configErrors.add(e.getMessage());
            log.warn("Invalid query in " + queryElement.getOwnerDocument().getDocumentURI() + " for index " + XMLWriter.write(queryElement, true, true), e);
        }
        return null;
    }

    protected final IdEventListener idListener = new IdEventListener() {
            // wrapping to avoid also registring it as a NodeEventListener
            public void notify(IdEvent idEvent) {
                Lucene.this.notify(idEvent);
            }
        @Override
            public String toString() {
                return Lucene.this.toString();
            }
        };
    protected final AssignmentEvents.Listener assignmentsListener = new AssignmentEvents.Listener() {
            // wrapping to avoid also registring it as a NodeEventListener
            public void notify(AssignmentEvents.Event event) {
                Lucene.this.notify(event);
            }
        @Override
            public String toString() {
                return Lucene.this.toString();
            }
        };


    protected void readConfiguration(String resource) {


        defaultStrategy.removeAllStrategies();
        List<URL> configList = ResourceLoader.getConfigurationRoot().getResourceList(resource);
        log.service("Reading " + configList);
        for(URL url : configList) {
            try {
                if (! url.openConnection().getDoInput()) continue;
                Document config = ResourceLoader.getDocument(url, true, Lucene.class);
                log.service("Reading lucene strategy configuration from " + url);
                Element root = config.getDocumentElement();
                if (defaultStrategy.size() == 0) {
                    defaultStrategy.fillFromXml(root);
                }
            } catch (Exception e) {
                log.warn(e);
            }
        }
        if (defaultStrategy.size() == 0) {
            defaultStrategy.addReleaseStrategy(new BasicReleaseStrategy());
        }
        log.service("Default release strategy " + defaultStrategy);

        indexerMap.clear();
        searcherMap.clear();
        disableIndexes.clear();
        defaultIndex = null;
        factory.clear();

        for(URL url : configList) {
            try {
                if (! url.openConnection().getDoInput()) continue;
                Document config = ResourceLoader.getDocument(url, true, Lucene.class);
                log.service("Reading lucene search configuration from " + url);
                Element root = config.getDocumentElement();
                disableIndexes.addAll(StringSplitter.split(root.getAttribute("disableIndexes")));


                if (defaultStrategy.size() == 0) {
                    defaultStrategy.fillFromXml(root);
                }

                NodeList extractorElements = root.getElementsByTagName("extractor");
                for (int i = 0; i < extractorElements.getLength(); i++) {
                    Element extractorElement = (Element) extractorElements.item(i);
                    String className = extractorElement.getAttribute("class");
                    String mimeType = extractorElement.getAttribute("mimetype");
                    if ("".equals(mimeType)) mimeType = null;
                    factory.addExtractor(className, mimeType);
                }

                NodeList indexElements = root.getElementsByTagName("index");
                for (int i = 0; i < indexElements.getLength(); i++) {
                    Element indexElement = (Element) indexElements.item(i);
                    String indexName = "default";
                    if (Lucene.hasAttribute(indexElement, "name")) {
                        indexName = Lucene.getAttribute(indexElement, "name");
                    }
                    if (disableIndexes.contains(indexName)) {
                        log.service("Index with name '" + indexName + "' was disabled");
                        continue;
                    }
                    if (indexerMap.containsKey(indexName)) {
                        log.warn("Index with name " + indexName + " already exists");
                    } else {
                        boolean storeText = false; // default: no text fields are stored in the index unless noted otherwise
                        boolean mergeText = true; // default: all text fields have the "fulltext" alias unless noted otherwise
                        if (Lucene.hasAttribute(indexElement, "optimize")) {
                            String optimize = Lucene.getAttribute(indexElement, "optimize");
                            storeText = optimize.equals("none");
                            mergeText = optimize.equals("full");
                        }
                        if (defaultIndex == null) {
                            defaultIndex = indexName;
                            log.service("Default index: " + defaultIndex);
                        }
                        Set<String> allIndexedFieldsSet = new HashSet<String>();
                        List<IndexDefinition> queries = new ArrayList<IndexDefinition>();
                        // lists
                        NodeList childNodes = indexElement.getChildNodes();
                        Analyzer analyzer = null;
                        int lists = 0;
                        for (int k = 0; k < childNodes.getLength(); k++) {
                            if (childNodes.item(k) instanceof Element) {
                                Element childElement = (Element) childNodes.item(k);
                                String childName = childElement.getLocalName();
                                if ("list".equals(childName)||
                                    "builder".equals(childName) || // backward comp. old finalist lucene
                                    "table".equals(childName)) { // comp. finalist lucene
                                    IndexDefinition id = createIndexDefinition(childElement, allIndexedFieldsSet, storeText, mergeText, null, analyzer);
                                    if (id == null) {
                                        continue;
                                    }
                                    id.setId(indexName + "_" + (++lists));
                                    queries.add(id);
                                    log.service("Added mmbase index definition " + id);
                                    if ("list".equals(childName)) {
                                        String snodes = childElement.getAttribute("startnodes");
                                        String[] sn = snodes.split(",");
                                        if (snodes != null && !"".equals(snodes)) {
                                            if (log.isDebugEnabled()) {
                                                log.debug("Found startnodes '" + snodes + "' of list in index: " + indexName);
                                            }
                                            for (int l = 0; l < sn.length; l++) {
                                                String snr = cloud.getNodeByAlias(sn[l]).getStringValue("number");
                                                if (log.isDebugEnabled()) {
                                                    log.debug("checking for: " + snr);
                                                }
                                                if (!startNodes.contains(snr)) startNodes.add(snr);
                                            }
                                        }
                                    }
                                } else if ("jdbc".equals(childName)) {
                                    MMBase mmb = MMBase.getMMBase();

                                    DataSource ds =  ((DatabaseStorageManagerFactory) mmb.getStorageManagerFactory()).getDataSource();
                                    IndexDefinition id = new JdbcIndexDefinition(ds, childElement,
                                                                                 allIndexedFieldsSet, storeText, mergeText, analyzer, false);
                                    if (id == null) continue;
                                    id.setId(indexName + "_" + (++lists));
                                    queries.add(id);
                                    EventManager.getInstance().addEventListener(idListener);
                                    log.service("Added mmbase jdbc definition " + id);
                                } else if ("analyzer".equals(childName)) {
                                    analyzer = (Analyzer) Instantiator.getInstance(childElement);
                                }
                            }
                        }

                        boolean updates = incrementalUpdates;
                        String updatesAttribute = Lucene.getAttribute(indexElement, "incrementalUpdates");
                        if (updatesAttribute.length() > 0) {
                            updates = Casting.toBoolean(updatesAttribute);
                        }
                        Indexer indexer = new Indexer(indexPath, indexName, queries, analyzer, readOnly, updates, scheduler);
                        for (String s : configErrors) {
                            indexer.addError(url.toString() + ": " + s);
                        }
                        configErrors.clear();
                        indexer.getDescription().fillFromXml("description", indexElement);
                        log.service("Add lucene index with name " + indexName);
                        indexerMap.put(indexName, indexer);
                        String[]  allIndexedFields = allIndexedFieldsSet.toArray(new String[0]);
                        Searcher searcher = new Searcher(indexer, allIndexedFields);
                        searcherMap.put(indexName, searcher);
                    }
                }
            } catch (Exception e) {
                log.warn("Can't read Lucene configuration: "+ e.getMessage(), e);
            }
        }
        EventManager.getInstance().addEventListener(assignmentsListener);
        EventManager.getInstance().propagateEvent(new ConfigurationRead());
        configReadTime = new Date();
    }

    public Searcher getSearcher(String indexName) {
        if (indexName == null || indexName.equals("")) indexName = defaultIndex;
        Searcher searcher = indexName == null ? null : searcherMap.get(indexName);
        if (searcher == null) {
            throw new IllegalArgumentException("Index with name " + indexName + " does not exist. Existing are " + searcherMap.keySet());
        }
        return searcher;
    }


    public void notify(NodeEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Received node event: " + event +  Logging.stackTrace(6));
        }
        if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_NEW:
                if (! event.isRelation()) {
                    scheduler.newIndex("" + event.getNodeNumber(), MMBaseIndexDefinition.class);
                }
                break;
            case Event.TYPE_CHANGE:
                if (event.getChangedFields().size() > 0) {
                    scheduler.updateIndex("" + event.getNodeNumber(), MMBaseIndexDefinition.class);
                } else {
                    // I don't know why the event was issued in the first place, but don't make it
                    // worse.
                }
                break;
            case Event.TYPE_DELETE:
                scheduler.deleteIndex("" + event.getNodeNumber(), MMBaseIndexDefinition.class);
                break;
            }
        }
    }
    public void notify(RelationEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Received relation event: " + event + Logging.stackTrace(6));
        }
        // LUCENE-25: I fail to see why related nodes need to be scheduled for an index update,
        // this causes unnecessary incremental updates of nodes further up- or downstream.
        /* if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_NEW:
                //scheduler.newIndex("" + event.getRelationSourceNumber(), MMBaseIndexDefinition.class);
                if (!startNodes.contains("" + event.getRelationDestinationNumber())) {
                    scheduler.updateIndex("" + event.getRelationDestinationNumber(), MMBaseIndexDefinition.class);
                }
                break;
            case Event.TYPE_CHANGE:
            case Event.TYPE_DELETE:
                if (!startNodes.contains("" + event.getRelationSourceNumber())) {
                    scheduler.updateIndex("" + event.getRelationSourceNumber(), MMBaseIndexDefinition.class);
                }
                if (!startNodes.contains("" + event.getRelationDestinationNumber())) {
                    scheduler.updateIndex("" + event.getRelationDestinationNumber(), MMBaseIndexDefinition.class);
                }
                break;
            }
        } */
    }

    public void notify(IdEvent event) {
        if (scheduler != null) {
            switch(event.getType()) {
            case Event.TYPE_DELETE:
                scheduler.deleteIndex(event.getId(), JdbcIndexDefinition.class);
                break;
            default:
                scheduler.updateIndex(event.getId(), JdbcIndexDefinition.class);
                break;
            }
        }
    }

    private static int assignmentIds = 0;

    /**
     * Queue for index operations.
     */
    // public because the constants need to be visible for the SortedBundle
    public class Scheduler extends Thread {

        public static final int READONLY         = -100;
        public static final int IDLE             = 0;
        public static final int IDLE_AFTER_ERROR = -1;
        public static final int BUSY_INDEX       = 1;
        public static final int BUSY_FULL_INDEX  = 2;

        // status of the scheduler
        private int status = IDLE;
        private Assignment assignment = null;

        // assignments: tasks to run
        private BlockingQueue<Scheduler.Assignment> indexAssignments = new DelayQueue<Scheduler.Assignment>();

        Scheduler() {
            super(MMBaseContext.getThreadGroup(), null, MMBaseContext.getMachineName() + ":Lucene.Scheduler");
            setDaemon(true);
        }

        public int getStatus() {
            return status;
        }
        public Assignment getAssignment() {
            return assignment;
        }
        public Collection<Assignment> getQueue() {
            return Collections.unmodifiableCollection(indexAssignments);
        }

        @Override
        public void run() {
            MMBase mmbase = MMBase.getMMBase();
            log.service("Start Lucene Scheduler");
            while (mmbase != null && !mmbase.isShutdown()) {
                if (log.isTraceEnabled()) {
                    log.trace("Obtain Assignment from " + indexAssignments);
                }
                try {
                    assignment = indexAssignments.take();
                    if (log.isDebugEnabled()) log.debug("Running " + assignment);
                    // do operation...
                    assignment.run();
                    status = IDLE;

                } catch (InterruptedException e) {
                    log.service(Thread.currentThread().getName() +" was interruped.");
                    status = IDLE;
                    continue;
                } catch (RuntimeException rte) {
                    log.error(rte.getMessage(), rte);
                    status = IDLE_AFTER_ERROR;
                } finally {
                    assignment = null;
                }

            }
        }
        public int unAssign(int id) {
            int tot = 0;
            Assignment[] assigns = indexAssignments.toArray(new Assignment[]{}); // makes a copy, to avoid ConcurrentModification exceptions
            for (Assignment assign : assigns) {
                if (assign.getId() == id) { if (indexAssignments.remove(assign)) { tot++;} }
            }
            return tot;
        }

        public abstract class Assignment implements Runnable, Delayed {


            private final Throwable cause = new Throwable(); // store a stack trace
            private final int id = assignmentIds++;

            private final long endTime = System.currentTimeMillis() + Lucene.this.waitTime;

            public int getId() {
                return id;
            }
            @Override
            public int hashCode() {
                return idString().hashCode();
            }

            @Override
            public boolean equals(Object o) {
                if (o == null) return false;
                if (! o.getClass().equals(getClass())) return false;
                Assignment a = (Assignment) o;
                return id == a.getId() || idString().equals(a.idString());
            }
            public long  getDelay(TimeUnit unit) {
                return unit.convert(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
            public int compareTo(Delayed o) {
                return (int) (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
            }
            public Date getDate() {
                return new Date(endTime - Lucene.this.waitTime);
            }
            abstract String idString();

            public Throwable getCause() {
                return cause;
            }
        }
        void assign(Assignment a) {
            if (assigned(a)) {
                log.service("Canceling " + a + ", because already queued");
                return;
            }
            if (a.equals(assignment)) {
                log.service("Canceling " + a + ", because it is currently running already");
                return;
            }
            indexAssignments.offer(a);

        }
        /**
         * Checks whether the given assignment is scheduled already
         */
        boolean assigned(Assignment a) {
            Assignment[] assigns = indexAssignments.toArray(new Assignment[]{}); // makes a copy, to avoid ConcurrentModification exceptions
            for (Assignment assign : assigns) {
                if (assign.equals(a)) return true;
            }
            return false;
        }


        void newIndex(final String number, final Class<? extends IndexDefinition> klass) {
            assert klass != null;
            assign(new Assignment() {
                    Indexer current;
                    public void run() {
                        log.service("New index for " + number);
                        status = BUSY_INDEX;
                        for (Indexer indexer : indexerMap.values()) {
                            current = indexer;
                            int updated = indexer.newIndex(number, klass);
                            if (updated > 0) {
                                log.service(indexer.getName() + ": " + updated + " new index entr" + (updated > 1 ? "ies" : "y"));
                            }
                        }
                        current = null;
                    }
                    public String idString() {
                        return klass.getName() + number;
                    }
                @Override
                    public String toString() {
                        return "NEW for " + number + " " + klass + (current != null ? " (" + current.getName() + ")" : "");
                    }

                });
        }

        void updateIndex(final String number, final Class<? extends IndexDefinition> klass) {
            assert klass != null;
            assign(new Assignment() {
                    Indexer current;
                    public void run() {
                        if (log.isDebugEnabled()) log.debug("Update index for " + number);
                        status = BUSY_INDEX;
                        for (Indexer indexer : indexerMap.values()) {
                            current = indexer;
                            if (current.isIncrementalUpdating()) {
                                int updated = indexer.updateIndex(number, klass);
                                if (updated > 0) {
                                    if (log.isDebugEnabled()) {
                                        log.debug(indexer.getName() + ": Updated " + updated + " index entr" + (updated > 1 ? "ies" : "y"), getCause());
                                    } else if (log.isServiceEnabled()) {
                                        log.service(indexer.getName() + ": Updated " + updated + " index entr" + (updated > 1 ? "ies" : "y"));
                                    }
                                }
                            }
                            current = null;
                        }
                    }
                    public String idString() {
                        return klass.getName() + number;
                    }
                @Override
                    public String toString() {
                        return "UPDATE for " + number + " " + klass + (current != null ? " (" + current.getName() + ")" : "");
                    }

                });
        }

        void deleteIndex(final String number, final Class<? extends IndexDefinition> klass) {
            assert klass != null;
            assign(new Assignment() {
                    Indexer current;
                    public void run() {
                        if (log.isDebugEnabled()) log.debug("delete index for " + number); // already logged in indexer.deleteIndex
                        status = BUSY_INDEX;
                        for (Indexer indexer : indexerMap.values()) {
                            current = indexer;
                            indexer.deleteIndex(number, klass);
                        }
                        current = null;
                    }
                    public String idString() {
                        return klass.getName() + number;
                    }
                @Override
                    public String toString() {
                        return "DELETE for " + number + " " + klass + (current != null ? " (" + current.getName() + ")" : "");
                    }
                });
        }

        void deleteIndex(final String number, final String indexName) {
            assign(new Assignment() {
                    public void run() {
                        log.service("delete index for " + number);
                        status = BUSY_INDEX;
                        Indexer indexer = indexerMap.get(indexName);
                        if (indexer == null) {
                            log.error("No such index '" + indexName + "'");
                        } else {
                            indexer.deleteIndex(number, IndexDefinition.class);
                        }
                    }
                    public String idString() {
                        return indexName + " " + number;
                    }
                @Override
                    public String toString() {
                        return "DELETE for " + number + " " + indexName;
                    }
                });
        }

        class Marker {
            boolean reached = false;
        }
        void waitForReady() {
            final Marker marker = new Marker();
            assign(new Assignment() {
                    public void run() {
                        synchronized(marker) {
                            marker.reached = true;
                            marker.notifyAll();
                        }
                    }
                    public String idString() {
                        return "END_MARKER " + marker;
                    }
                }
                );
            try {
                synchronized(marker) {
                    while (! marker.reached) {
                        marker.wait();
                    }
                }
            } catch (InterruptedException ie) {
            }
        }

        private final Assignment ALL_FULL_INDEX = new Assignment() {
                Indexer indexer = null;
                public void run() {
                    status = BUSY_FULL_INDEX;
                    log.service("start full index");
                    SortedSet<String> keys = new TreeSet<String>(indexerMap.keySet());
                    for (String i : keys) {
                        indexer = indexerMap.get(i);
                        if (indexer != null) {
                            indexer.fullIndex();
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("Interrupted");
                            return;
                        }
                    }
                    indexer = null;
                }
                public String idString() {
                    return "";
                }
            @Override
                public String toString() {
                    return "FULLINDEX(" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(getDate()) + (indexer == null ? "" : (", " + indexer.getName())) + ")";
                }
            @Override
                public long getDelay(TimeUnit unit) {
                    return 0;
                }
            };

        void fullIndex() {
            if (status != BUSY_FULL_INDEX) {
                assign(ALL_FULL_INDEX);
                log.service("Scheduled full index");
                // only schedule a full index if none is currently busy.
            } else {
                log.service("Cannot schedule full index because it is busy with " + getAssignment());
            }
        }
        void fullIndex(final String index) {
            if (status != BUSY_FULL_INDEX || ! ALL_FULL_INDEX.equals(assignment)) {
                if (! assigned(ALL_FULL_INDEX)) {
                    // only schedule a full index if no complete full index is currently busy or scheduled already.
                    Assignment a = new Assignment() {
                            public void run() {
                                status = BUSY_FULL_INDEX;
                                log.service("start full index for index '" + index + "'");
                                Indexer indexer = indexerMap.get(index);
                                if (indexer == null) {
                                    log.error("No such index '" + index + "'");
                                } else {
                                    indexer.fullIndex();
                                }
                            }
                            public String idString() {
                                return index;
                            }
                        @Override
                            public String toString() {
                                return "FULLINDEX for " + index + " (" + getDate() + ")";
                            }
                        @Override
                            public long getDelay(TimeUnit unit) {
                                return 0;
                            }
                        };
                    assign(a);
                    log.service("Scheduled full index for '" + index + "'");
                } else {
                    log.service("Scheduled full index for '" + index + "' because full index on every index is scheduled already");
                }
            } else {
                log.service("Cannot schedule full index for '" + index + "' because it is busy with " + getAssignment());
            }
        }

    }

    /**
     * Main for testing
     */
    public static void main(String[] args) {
        final BlockingQueue<String> q = new LinkedBlockingQueue<String>();
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000000; i++) {
                        q.offer("" + i);
                        Thread.yield();
                    }
                    q.offer("z");
                }
            });
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    while(true) {
                        try {
                            String t = q.take();
                            if ("z".equals(t)) return;
                        } catch (InterruptedException ie) { System.err.println("interrupted take"); return ; }
                    }
                }
            });
        ThreadPools.jobsExecutor.execute(new Runnable() {
                public void run() {
                    while(true) {
                        String[] t = q.toArray(new String[] {});
                        System.out.print("." + q.contains("123"));
                        Thread.yield();
                    }
                }
            });
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ie) {
            System.err.println("interrupted sleep");
            return;
        }

    }

    public static class ConfigurationRead extends SystemEvent.Collectable {
    }

}
