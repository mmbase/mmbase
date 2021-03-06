/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge.mock;

import static org.mmbase.datatypes.Constants.*;
import org.mmbase.bridge.util.*;
import org.mmbase.datatypes.DataType;
import java.util.*;
import org.mmbase.bridge.*;
import org.mmbase.util.functions.*;

import org.mmbase.util.logging.*;

/**
 * Straight-forward implementation of NodeManager based on a Map with DataType's.
 *
 * @author  Michiel Meeuwissen
 * @version $Id$
 * @since   MMBase-1.9.2
 */

public class MockNodeManager extends AbstractNodeManager  {
    private static final  Logger log = Logging.getLoggerInstance(MockNodeManager.class);

    protected final Map<String, Field> map = new LinkedHashMap<String, Field>();
    protected final String name;
    protected final String parent;
    protected final MockCloud vcloud;
    protected final int oType;
    protected final List<Function<?>> functions = new ArrayList<Function<?>>();
    protected String context = "default";

    public MockNodeManager(MockCloud cloud, NodeManagerDescription desc) {
        super(cloud);
        this.vcloud = cloud;
        this.name = desc.name;
        for (Map.Entry<String, Field> entry : desc.fields.entrySet()) {
            map.put(entry.getKey(), new MockField(this, entry.getValue()));
        }
        map.put("_number", new SystemField("_number", this, DATATYPE_INTEGER));
        map.put("_exists", new SystemField("_exists", this, DATATYPE_STRING));
        this.oType = desc.oType;
        String e = desc.reader != null ? desc.reader.getExtends() : null;
        if (e != null && e.length() == 0) e = null;
        this.parent = e;

        // it should behave as a proper node
        values.put("number", desc.oType);
        values.put("name",   desc.name);


        // It seems that any node manager needs to implement at least the following functions
        // Perhaps we need to come up with something to make this reusable.
        functions.add(new NodeFunction<Integer>("age", Parameter.emptyArray(), ReturnType.INTEGER) {
                @Override
                public Integer getFunctionValue(Node n, Parameters params) {
                    // mock nodes probably all exists less then a day..
                    return 0;
                }
            });
        functions.add(new NodeFunction<String>("gui",
                                               //GuiFunction.PARAMETERS, // TODO, that would introduce MMObjectNode dependency
                                               new Parameter<?>[] {
                                                   Parameter.FIELD,
                                                       Parameter.LANGUAGE,
                                                       new Parameter<String>("session", String.class),
                                                       Parameter.RESPONSE,
                                                       Parameter.REQUEST,
                                                       Parameter.LOCALE,
                                                       new Parameter<String>("stringvalue", String.class) },
                                               ReturnType.STRING) {
                @Override
                public String getFunctionValue(Node n, Parameters params) {
                    return n.getStringValue("number");
                }
            });
    }



    @Override
    public String getName() {
        return name;
    }


    // TODO This method is more or less copied from a method in AbstactNodeManager and should be generalized
    protected void setDefaultsWithCloud(Node node) {
        log.debug("Setting default values");
        for (Iterator i = getFields().iterator(); i.hasNext(); ) {
            Field field = (Field) i.next();
            if (field.getName().equals("number"))      continue;
            if (field.getName().equals("otype")) continue;

            if (node.isNull(field.getName()) || "".equals(node.getStringValue(field.getName()))) { // required field are set to '', which would destroy the default value...
                org.mmbase.datatypes.DataType dt = field.getDataType();
                //log.info("" + field.getName() + " " + dt);
                Object defaultValue = dt.getDefaultValue(getCloud().getLocale(), getCloud(), field);
                if (defaultValue != null) {
                    if (defaultValue instanceof Node) {
                        defaultValue = ((Node) defaultValue).getNumber();
                    }

                    node.setValueWithoutProcess(field.getName(), defaultValue);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("" + field.getName() + " is already non null, but " + node.getValue(field.getName()));
                }
            }
        }
    }


    @Override
    public Node createNode() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("otype", oType);
        map.put("owner", vcloud.getUser().getOwnerField());
        Node n = vcloud.getNode(map, this, true);
        setDefaultsWithCloud(n);
        return n;
    }


    @Override
    public Map<String, String> getProperties() {
        return vcloud.cloudContext.nodeManagers.get(getName()).properties;
    }

    @Override
    protected Map<String, Field> getFieldTypes() {
        return Collections.unmodifiableMap(map);
    }


    @Override
    public NodeManager getParent() throws NotFoundException {
        if (parent == null) {
            throw new NotFoundException();
        } else {
            return getCloud().getNodeManager(parent);
        }
    }

    @Override
    public Collection<Function<?>>  getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    @Override
    public String getContext() {
        return context;
    }
    @Override
    public void setContext(String c) {
        context = c;
    }

    @Override
    public boolean mayCreateNode() {
        return true;
    }

    protected class SystemField extends DataTypeField {
        public SystemField(String name, NodeManager nm, DataType dt) {
            super(name, nm, dt);
        }
        @Override
        public boolean isReadOnly() {
            return true;
        }
        @Override
        public int getEditPosition() {
            return -1;
        }
    }


}
