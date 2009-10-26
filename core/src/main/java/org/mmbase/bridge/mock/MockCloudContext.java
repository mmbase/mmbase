/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.mock;

import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.AbstractCloudContext;
import org.mmbase.bridge.util.NodeManagerDescription;
import org.mmbase.datatypes.DataType;
import org.mmbase.bridge.implementation.*;
import org.mmbase.storage.search.*;
import org.mmbase.security.*;
import org.mmbase.util.*;
import org.mmbase.util.logging.*;
import org.xml.sax.InputSource;


/**
 * The 'mock' cloud context is mainly meant for usage in junit test cases. It provides a
 * functional bridge implementation, without the backing of the MMBase core classes. This means on
 * one hand that it takes much less time to initialize, on the other hand that all data created is only
 * availabe in memory and is in no way persistent.
 *
 * Also, at the moment, it is only <em>partially</em> working. You will easily encounter many {@link
 * UnsupportedOperationException}s and other kind of errors. For several simple tests it is however useful already.

 * The object model must be manually set up using the several <code>addNodeManager</code>
 * methods. The 'core' model is created with {@link #addCore}. Current implementation will basicly allow all 'related' and 'posrel' relations, but this
 * part has still to be elaborated on. This may change in future versions of this.
 *
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @since   MMBase-1.9.2
 * @todo    EXPERIMENTAL
 */

public class MockCloudContext extends  AbstractCloudContext {

    private static final Logger LOG = Logging.getLoggerInstance(MockCloudContext.class);

    private static final MockCloudContext virtual = new MockCloudContext();
    public static MockCloudContext getInstance() {
        return virtual;
    }

    public static class NodeDescription {
        public final String type;
        public final Map<String, Object> values;
        public NodeDescription(String t, Map<String, Object> v) {
            type = t;
            values = v;
        }
        public String toString() {
            return type + ":" + values;
        }
    }


    private int lastNodeNumber = 0;
    private final Authentication authentication = new NoAuthentication();


    final Map<Integer, NodeDescription>  nodes                 = Collections.synchronizedMap(new LinkedHashMap<Integer, NodeDescription>());
    public final Map<String,  NodeManagerDescription> nodeManagers    = Collections.synchronizedMap(new LinkedHashMap<String, NodeManagerDescription>());


    public Map<Integer, NodeDescription>  getNodes() {
        return nodes;
    }

    public MockCloudContext() {
        clouds.add("mmbase");
    }

    public void clear() {
        nodes.clear();
        nodeManagers.clear();
        lastNodeNumber = 0;
    }

    /**
     * Adds the 'core' builders to the object model.
     * It may also add a core relation model. Like 'related' and 'posrel' roles.
     */
    public void addCore() throws java.io.IOException {
        for (String buil : new String[] {"object", "typedef", "typerel", "reldef", "insrel"}) {
            if (! nodeManagers.containsKey(buil)) {
                addNodeManager(MockBuilderReader.getBuilderLoader().getInputSource("core/" + buil + ".xml"));
            } else {
                LOG.service("Builder with name '" + buil + "' already exists");
            }
        }
    }

    protected int getTypeDefNode(String name) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        return addNode("typedef", map);
    }

    public void addNodeManager(String name, Map<String, DataType> map) {
        Map<String, Field> m = new HashMap<String, Field>();
        for (Map.Entry<String, DataType> e : map.entrySet()) {
            m.put(e.getKey(), new MockField(e.getKey(), null, e.getValue()));
        }

        nodeManagers.put(name, new NodeManagerDescription(name, m, getTypeDefNode(name)));
    }

    public void addNodeManager(InputSource source) {
        synchronized(nodeManagers) {
            MockBuilderReader reader = new MockBuilderReader(source, this);
            addNodeManager(reader);
        }
    }

    protected void addNodeManager(MockBuilderReader reader) {
        if (! nodeManagers.containsKey(reader.getName())) {
            nodeManagers.put(reader.getName(), new NodeManagerDescription(reader, getTypeDefNode(reader.getName())));
        } else {
            LOG.service("Builder with name '" + reader.getName() + "' already exists");
        }
    }

    public void addNodeManagers(ResourceLoader directory) throws java.io.IOException {
        for (String builder : directory.getResourcePaths(ResourceLoader.XML_PATTERN, true)) {
            synchronized(nodeManagers) {
                MockBuilderReader reader = new MockBuilderReader(directory.getInputSource(builder), this);
                if (reader.getRootElement().getTagName().equals("builder")) {
                    try {
                        addNodeManager(reader);
                    } catch (Exception e) {
                        System.err.println("" + reader + ": " + e.getMessage() + ": " + Logging.stackTrace(e));
                    }
                }
            }
        }
    }

    public synchronized int addNode(String type, Map<String, Object> map) {
        int number = ++lastNodeNumber;
        nodes.put(number, new NodeDescription(type, map));
        map.put("number", number);
        //System.out.println("produced " + number + " " + map);
        return number;
    }

    public Cloud getCloud(String name, org.mmbase.security.UserContext user) throws NotFoundException {
        if (clouds.contains(name)) {
            return new MockCloud(name, this, user);
        } else {
            throw new NotFoundException("No such cloud '" + name + "'");
        }
    }

    public String getUri() {
        return "mock:local";
    }

    @Override
    public String toString() {
        return getUri() + "#" + hashCode() + "(" + nodes.size() + " nodes)";
    }

    public static class MockResolver extends ContextProvider.Resolver {
        {
            description.setKey("mock");
        }
        @Override
        public CloudContext resolve(String uri) {
            if (uri.startsWith("mock:")){
                CloudContext result = MockCloudContext.getInstance();
                return result;
            } else {
                return null;
            }
        }
        @Override
        public boolean equals(Object o) {
            return o != null && o instanceof MockResolver;
        }
        @Override
        public String toString() {
            return "mock:local";
        }
    }



 }
