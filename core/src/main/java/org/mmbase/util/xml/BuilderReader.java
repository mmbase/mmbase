/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.util.*;
import java.util.regex.Pattern;
import org.mmbase.bridge.ContextProvider;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.mmbase.bridge.Field;
import org.mmbase.bridge.NodeManager;
import org.mmbase.core.CoreField;
import org.mmbase.core.util.Fields;
import org.mmbase.core.util.DataTypeSetter;
import org.mmbase.datatypes.*;
import org.mmbase.datatypes.DataTypes.FieldNotFoundException;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.storage.util.Index;

import org.mmbase.util.*;

import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;

/**
 * Used to parse and retrieve data from a builder configuration file.
 * The parser support builders for builder dtd 1.1.
 *
 * @since MMBase 1.7
 * @author Case Roole
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class BuilderReader extends AbstractBuilderReader<CoreField> {


    private static final Logger log = Logging.getLoggerInstance(BuilderReader.class);

    /**
     * MMBase instance, used to load parent (extending) builders
     */
    private MMBase mmbase;

    /**
     * Parent builder.
     * If assigned, the properties of this builder are used as 'defaults'
     * and the fields of the builder are inherited.
     * @since MMbase-1.6
     */
    private MMObjectBuilder parentBuilder;


    /**
     * @since MMBase-1.7
     */
    public BuilderReader(InputSource source, MMBase mmb) {
        super(source);
        mmbase = mmb;
        if (getRootElement().getTagName().equals("builder")) {
            resolveInheritance();
        }
    }

    /**
     * @since MMBase-1.8
     */
    public BuilderReader(Document doc, MMBase mmb) {
        this(doc, mmb, Integer.MAX_VALUE);
    }
    /**
     * @since MMBase-1.9
     */
    private BuilderReader(Document doc, MMBase mmb, int maxVersion) {
        super(doc);
        mmbase = mmb;
        if (this.getVersion() <= maxVersion) {
            if (getRootElement().getTagName().equals("builder")) {
                resolveInheritance();
            }
        }
    }


    @Override
    protected boolean resolveInheritance() {
        String buildername = getExtends();
        if (buildername.equals("")) {
            parentBuilder = null;
            inheritanceResolved = true;
        } else if (buildername.equals(getName())) {
            int thisVersion = getVersion();
            BuilderReader parent = null;
            int foundVersion = -1;
            ResourceLoader loader = mmbase.getBuilderLoader();
            for (String s :  loader.getResourcePaths(Pattern.compile(getName()+ "\\.xml"), true)) {
                for (java.net.URL url : loader.getResourceList(s)) {
                    try {
                        if (! url.openConnection().getDoInput()) continue;
                        org.w3c.dom.Document doc = ResourceLoader.getDocument(url, true, BuilderReader.class);
                        if (! doc.getDocumentElement().getTagName().equals("builder")) {
                            continue;
                        }
                        BuilderReader prop = new BuilderReader(doc, mmbase, thisVersion - 1);
                        int v = prop.getVersion();
                        if (v < thisVersion && v > foundVersion) {
                            parent = prop;
                            foundVersion = v;
                        }
                    } catch (Exception ioe) {
                        log.warn(ioe);
                    }
                }

            }
            if (parent == null) {
                log.warn("Tried to extend builder " + getSystemId() + " from itself, but no other builder resource found (with smaller version)");
                inheritanceResolved = false;
            } else {
                log.info("Inheriting " + document.getDocumentURI() + " from " + parent.document.getDocumentURI());
                Document inherit = this.document;
                this.document = (Document) parent.document.cloneNode(true);
                inherit.getDocumentElement().removeAttribute("extends");
                resolveInheritanceByXML(document, inherit);
                inheritanceResolved = false;
                return resolveInheritance();

            }


        } else {
            inheritanceResolved = false;
            if (mmbase != null) {
                parentBuilder = mmbase.getBuilder(buildername);
                inheritanceResolved = (parentBuilder != null);
                if (inheritanceResolved) { // fill inputPositions, searchPositions
                    Iterator<CoreField> fields = parentBuilder.getFields(NodeManager.ORDER_EDIT).iterator();
                    while (fields.hasNext()) {
                        CoreField def = fields.next();
                        inputPositions.add(def.getEditPosition());
                    }
                    fields = parentBuilder.getFields(NodeManager.ORDER_SEARCH).iterator();
                    while (fields.hasNext()) {
                        CoreField def = fields.next();
                        searchPositions.add(def.getSearchPosition());
                    }
                }
            }
        }
        return inheritanceResolved;
    }


    @Override
    protected int getParentSearchAge() {
        return Integer.parseInt(parentBuilder.getSearchAge());
    }


    @Override
    protected String getParentClassName() {
        return parentBuilder.getClass().getName();
    }


    @Override
    public List<CoreField> getFields() {
        return getFields(null, DataTypes.getSystemCollector());
    }

    /**
     * Get the field definitions of this builder.
     * If applicable, this includes the fields inherited from a parent builder.
     *
     * @param builder the MMObjectBuilder to which the fields will be added
     * @param collector the datatype collector used to access the datatypes available for the fields to read.
     * @return a List of all Fields as CoreField
     * @since MMBase-1.8
     */
    public List<CoreField> getFields(final MMObjectBuilder builder, final DataTypeCollector collector) {
        List<CoreField> results = new ArrayList<CoreField>();
        Map<String, CoreField> oldset = new HashMap<String, CoreField>();
        int pos = 1;
        if (parentBuilder != null) {
            Collection<CoreField> parentfields = parentBuilder.getFields();
            if (parentfields != null) {
                // have to clone the parent fields
                // need clone()!
                for (CoreField f : parentfields) {
                    CoreField newField = (CoreField)f.clone(f.getName());
                    newField.setParent(builder);
                    while(newField.getStoragePosition() >= pos) pos++;
                    newField.finish();
                    results.add(newField);
                    oldset.put(newField.getName(), newField);
                }
            }
        }

        for(Element fieldList : getChildElements("builder", "fieldlist")) {
            for (final Element fieldElement : getChildElements(fieldList,"field")) {
                String fieldName = getElementAttributeValue(fieldElement, "name");
                if ("".equals(fieldName)) {
                    fieldName = getElementValue(getElementByPath(fieldElement,"field.db.name"));
                }
                final CoreField def = oldset.get(fieldName);
                try {
                    if (def != null) {
                        def.rewrite();

                        DataTypeSetter setter = new DataTypeSetter(def) {
                                @Override
                                public void set(DataType dt) {
                                    if (dt != null) {
                                        field.setDataType(dt); // replace datatype
                                    }
                                    decodeFieldDef(fieldElement, def, collector);
                                    decodeFieldAttributes(fieldElement, def);
                                    def.finish();
                                }

                            };
                        decodeDataType(setter,
                                       (builder != null ? builder.getTableName() : null), collector, def.getName(), fieldElement, false);
                    } else {
                        CoreField newDef = decodeFieldDef(builder, collector, fieldElement);
                        newDef.setStoragePosition(pos++);
                        decodeFieldAttributes(fieldElement, newDef);
                        results.add(newDef);
                    }
                } catch (Exception e) {
                    log.error("During parsing of " + XMLWriter.write(fieldElement, true, true) + " " + e.getMessage(), e);
                }
            }
        }
        return results;
    }

    /**
     * Get the named indices of this builder.
     * Note that the 'default' index (set with the 'key' attribute) is also included
     * in this list (with the name {@link Index#MAIN}).
     *
     * @param builder the MMObjectBuilder to which the fields will be added
     * @return a List of all Indices
     */
    public List<Index> getIndices(MMObjectBuilder builder) {
        List<Index> results = new ArrayList<Index>();
        Index mainIndex = null;
        if (parentBuilder != null) {
            // create the
            Index parentIndex = parentBuilder.getStorageConnector().getIndex(Index.MAIN);
            if (parentIndex != null) {
                mainIndex = new Index(builder, Index.MAIN);
                mainIndex.setUnique(true);
                for (Field field : parentIndex) {
                    mainIndex.add(builder.getField(field.getName()));
                }
            }
        }

        for (Element field : getChildElements("builder.fieldlist","field")) {
            Element dbtype = getElementByPath(field,"field.db.type");
            if (dbtype != null) {
                String key = getElementAttributeValue(dbtype,"key");
                if (key != null && key.equalsIgnoreCase("true")) {
                    String fieldName = getElementAttributeValue(field, "name");
                    if ("".equals(fieldName)) {
                        fieldName = getElementValue(getElementByPath(field,"field.db.name"));
                    }
                    if (mainIndex == null ) mainIndex = new Index(builder, Index.MAIN);
                    mainIndex.add(builder.getField(fieldName));
                }
            }
        }
        if (mainIndex != null) {
           results.add(mainIndex);
        }

        if (parentBuilder != null) {
            Collection<Index> parentIndices = parentBuilder.getStorageConnector().getIndices().values();
            if (parentIndices != null) {
                for (Index parentIndex : parentIndices) {
                    Index newIndex = new Index(builder, parentIndex.getName());;
                    newIndex.setUnique(parentIndex.isUnique());
                    for (Field field : parentIndex) {
                        newIndex.add(builder.getField(field.getName()));
                    }
                    results.add(newIndex);
                }
            }
        }

        for (Element indexList : getChildElements("builder", "indexlist")) {
            for (Element indexElement : getChildElements(indexList, "index")) {
                String indexName = indexElement.getAttribute("name");
                if (indexName != null && !indexName.equals("")) {
                    String unique = indexElement.getAttribute("unique");
                    Index index = new Index(builder, indexName);
                    index.setUnique(unique != null && unique.equals("true"));
                    for (Element fieldElement : getChildElements(indexElement,"indexfield")) {
                        String fieldName = fieldElement.getAttribute("name");
                        Field field = builder.getField(fieldName);
                        if (field == null) {
                            log.error("field '" + fieldName +"' in index '" + indexName + "' in builder " + builder.getTableName() + " does not exist");
                        } else {
                            index.add(field);
                        }
                    }
                    results.add(index);
                } else {
                    log.error("index in builder " + builder.getTableName() + " has no name");
                }
            }
        }
        return results;
    }

    /**
     * @since MMBase-1.8
     */
    public Set<Function> getFunctions(final MMObjectBuilder builder) {
        Map<String, Function> results = new HashMap<String, Function>();
        for (Element functionList : getChildElements("builder","functionlist")) {
            for (Element functionElement : getChildElements(functionList,"function")) {
                final String functionName = functionElement.getAttribute("name");
                String providerKey        = functionElement.getAttribute("key");
                Element classElement      = getElementByPath(functionElement, "function.class");

                try {
                    Function function;
                    final Class claz = Instantiator.getClass(classElement);
                    log.debug("Using function class '" + claz + "'");
                    if (Function.class.isAssignableFrom(claz)) {
                        if (!providerKey.equals("")) {
                            log.warn("Specified a key attribute for a Function " + claz + " in " + getSystemId() + ", this makes only sense for FunctionProviders.");
                        }
                        function = (Function) Instantiator.getInstance(claz, functionElement);
                    } else if (FunctionProvider.class.isAssignableFrom(claz)) {
                        if ("".equals(providerKey)) providerKey = functionName;
                        if ("".equals(providerKey)) {
                            log.error("FunctionProvider " + claz + " specified in " + getSystemId() + " without key or name");
                            continue;
                        }
                        FunctionProvider provider = (FunctionProvider) Instantiator.getInstance(claz, functionElement);
                        function = provider.getFunction(providerKey);
                        if (function == null) {
                            log.error("Function provider " + provider + "has no function '" + providerKey + "'");
                            continue;
                        }
                    } else {
                        if ("".equals(providerKey)) {
                            providerKey = functionName;
                        }
                        if ("".equals(providerKey)) {
                            log.error("Speficied class " + claz + " in " + getSystemId() + "/functionslist/function is not a Function or FunctionProvider and can not be wrapped in a BeanFunction, because neither key nor name attribute were specified.");
                            continue;
                        }
                        java.lang.reflect.Method method = MethodFunction.getMethod(claz, providerKey);
                        if (method == null) {
                            log.error("Could not find  method '" + providerKey + "' in " + claz);
                            continue;
                        } else {
                            if (method.getParameterTypes().length == 0) {
                                function = BeanFunction.getFunction(claz, providerKey, new BeanFunction.Producer() {
                                        public Object getInstance() {
                                            try {
                                                return BeanFunction.getInstance(claz, builder);
                                            } catch (Exception e) {
                                                log.error(e.getMessage(), e);
                                                return null;
                                            }
                                        }
                                        @Override
                                        public String toString() {
                                            return "" + claz.getName() + "." + builder.getTableName();
                                        }
                                    });
                            } else {
                                if (method.getClass().isInstance(builder)) {
                                    function = MethodFunction.getFunction(method, providerKey, builder);
                                } else {
                                    function = MethodFunction.getFunction(method, providerKey);
                                }
                            }
                        }
                    }
                    if (! functionName.equals("") && ! function.getName().equals(functionName)) {
                        log.debug("Wrapping " + function.getName() + " to " + functionName);
                        function = new WrappedFunction(function) {
                                public String getName() {
                                    return functionName;
                                }
                            };
                    }

                    String key = function.getName();
                    Function existing = results.get(key);

                    if (existing != null) {
                        log.info("Function " + key + " already defined, will combine it");
                        CombinedFunction cf;
                        if (existing instanceof CombinedFunction) {
                            cf = (CombinedFunction) existing;
                        } else {
                            cf = new CombinedFunction(key);
                            cf.addFunction(existing);
                        }
                        cf.addFunction(function);
                        function = cf;
                    }

                    NodeFunction nf = NodeFunction.wrap(function);
                    if (nf != null) function = nf;

                    results.put(key, function);
                    log.debug("functions are now: " + results);
                } catch (ClassNotFoundException cnfe) {
                    log.warn(XMLWriter.write(classElement) + " " + cnfe.getClass() + " " + getSystemId() + " '" + cnfe.getMessage() + "'");
                } catch (Throwable e) {
                    log.error(e.getClass() + " " + getSystemId() + " " + e.getMessage(), e);
                }
            }
        }
        Set<Function> r = new HashSet<Function>();
        for(Function fun : results.values()) {
            r.add(fun);
        }
        log.debug("Found functions " + r);
        return r;

    }

    /**
     * Construct a FieldDef object using a field Element using information
     * obtained from the builder configuration.
     * @since MMBase-1.8
     */
    private CoreField decodeFieldDef(final MMObjectBuilder builder, final DataTypeCollector collector,
                                     final Element fieldElement) {
        // create a new CoreField we need to fill

        final String fieldName;
        {
            // obtain field name.
            // if both the field name attribute and the <db><name> tag are specified, the attribute takes precedence.
            String fn = getElementAttributeValue(fieldElement, "name");
            String fieldDBName = getElementValue(getElementByPath(fieldElement, "field.db.name"));
            if ("".equals(fn)) {
                if ("".equals(fieldDBName)) {
                    throw new IllegalArgumentException("Field name was not specified for builder " + builder.getTableName() + ".");
                }
                if (log.isDebugEnabled()) {
                    log.debug("<db><name> tag for field '" + fieldDBName + "' is deprecated. Use the name attribute.");
                }
                fn = fieldDBName;
            } else if (!"".equals(fieldDBName)) {
                log.warn("Specified field name twice: once in the name attribute ('" + fn + "') and once in the <name> tag ('" + fieldDBName + "'). Ignoring name tag.");
            }
            fieldName = fn;
        }


        // implied by datatype
        // use db/type to override for legacy database issues
        // (mostly to prevent warnings in the log, as mmbase fixes this anyway)
        final String fieldType;
        final String fieldSize;
        final String fieldNotNull;

        // defined in datatype
        final String fieldRequired;
        final String fieldUnique;


        // type - default unknown (derived from datatype)
        // deprecated db type tag - only use if no other data is given!
        Element dbtype = getElementByPath(fieldElement, "field.db.type");
        if (dbtype != null) {
            if (log.isDebugEnabled()) {
                log.debug("<db><type> tag for field '" + fieldName + "' is deprecated.");
            }
            fieldType = getElementValue(dbtype);
            fieldNotNull = getElementAttributeValue(dbtype, "notnull");
            fieldRequired = getElementAttributeValue(dbtype, "required");
            fieldUnique = getElementAttributeValue(dbtype, "unique");
            fieldSize = getElementAttributeValue(dbtype, "size");
        } else {
            fieldType = "";
            fieldNotNull = "";
            fieldRequired = "";
            fieldUnique = "";
            fieldSize = "";
        }

        final int type;
        final int listItemType;
        {
            int _type         = Field.TYPE_UNKNOWN;
            int _listItemType = Field.TYPE_UNKNOWN;
            if (!"".equals(fieldType)) {
                _type = Fields.getType(fieldType);
                if (_type == Field.TYPE_LIST) {
                    if (fieldType.length() > 5) {
                        _listItemType = Fields.getType(fieldType.substring(5, fieldType.length()));
                    }
                }
            }
            type = _type;
            listItemType = _listItemType;
        }

        final CoreField def = Fields.createField(fieldName, type, listItemType,
                                                 Field.STATE_VIRTUAL,/*temp default, will set by decodeFieldAttributes*/
                                                 Constants.DATATYPE_UNKNOWN);

        DataTypeSetter setter = new DataTypeSetter(def) {
                @Override
                public void set(DataType dataType) {
                    def.setDataType(dataType);
                    // determine type from datatype, if possible)
                    int _type = getType();
                    if (_type == Field.TYPE_UNKNOWN) {
                        setType(dataType.getBaseType());
                        if (_type == Field.TYPE_LIST) {
                            setListItemType(((ListDataType)dataType).getItemDataType().getBaseType());
                        }
                    }

                    dataType = def.getDataType();

                    decodeFieldAttributes(fieldElement, def);

                    def.setParent(builder);

                    if (!fieldSize.equals("")) {
                        try {
                            def.setMaxLength(Integer.parseInt(fieldSize));
                        } catch (NumberFormatException e) {
                            log.warn("invalid value for size : " + fieldSize);
                        }
                    }


                    // set required property, but only if given
                    if (!"".equals(fieldRequired)) {
                        dataType.setRequired("true".equalsIgnoreCase(fieldRequired));
                    }

                    // default for notnull is value of required
                    def.setNotNull("true".equals(fieldNotNull) ||
                                   ("".equals(fieldNotNull) && dataType.isRequired()));

                    if (def.isNotNull() && ! "false".equalsIgnoreCase(fieldRequired)) { // If not null, it _must_ be required, unless explicitely not so (MMB-1504)
                        dataType.setRequired(true);
                    }

                    // set unique property, but only if given
                    if ("implied".equalsIgnoreCase(fieldUnique)) {
                        dataType.setUnique(true);
                        dataType.getUniqueRestriction().setEnforceStrength(DataType.ENFORCE_NEVER);
                    } else if ("true".equalsIgnoreCase(fieldUnique)) {
                        dataType.setUnique(true);
                    }
                    decodeFieldDef(fieldElement, def, collector);
                    def.finish();

                }
            };
        // datatype
        decodeDataType(setter,
                       (builder != null ? builder.getTableName() : null), collector, fieldName, fieldElement, true);


        return def;
    }

    /**
     * Get the builder that this builder extends
     *
     * @since MMBase-1.6
     * @return the parent as an MMObjectBuilder, or null if not specified or unresolved
     */
    public MMObjectBuilder getParentBuilder() {
        return parentBuilder;
    }

    @Override
    protected boolean hasParent() {
        return parentBuilder != null;
    }

    @Override
    protected Map<String, String> getParentProperties() {
        return parentBuilder.getInitParameters();
    }


    @Override
    protected int getParentVersion() {
        return parentBuilder.getVersion();
    }

    @Override
    protected String getParentMaintainer() {
        return parentBuilder.getMaintainer();
    }


    /**
     * Whether this builderreader object is equal to another for storage purposes (so, ignoring gui and documentation fields)
     * @since MMBase-1.7
     */
    public boolean storageEquals(BuilderReader f) {
        List<CoreField> otherFields = f.getFields();
        List<CoreField> thisFields  = getFields();
        if (otherFields.size() != thisFields.size()) return false;
        for (int i = 0; i < thisFields.size(); i++) {
            CoreField thisField = thisFields.get(i);
            CoreField otherField = otherFields.get(i);
            if (! thisField.storageEquals(otherField)) return false;
        }
        return true;
    }
    /**
     * @since MMBase-1.9.6
     * @param fieldAttribute
     * @return
     * @throws FieldNotFoundException
     */
    @Override
    protected BasicDataType getDataTypeForFieldAttribute(String fieldAttribute) throws FieldNotFoundException {
        List<String> elements = Arrays.asList(fieldAttribute.split("\\|", 4));
        Collections.reverse(elements);
        String uri                = elements.size() == 4 ? elements.get(3) : ContextProvider.getDefaultCloudContext().getUri();
        if (uri.equals("local")) {
            MMObjectBuilder ob = mmbase.getBuilder(elements.get(1));
            if (ob == null) throw new FieldNotFoundException("Builder " + elements.get(1) + " not yet available");
            CoreField field = ob.getField(elements.get(0));
            if (field == null) throw new FieldNotFoundException("Builder " + elements.get(1) + " has no field " + elements.get(0));
            return (BasicDataType) field.getDataType();
        } else {
            return super.getDataTypeForFieldAttribute(fieldAttribute);
        }
    }
     
    /**
     * For testing only
     */
    public static void main(String[] argv) throws Exception {
        org.mmbase.util.ResourceLoader rl = org.mmbase.util.ResourceLoader.getSystemRoot();
        Document doc = rl.getDocument(argv[0], true, BuilderReader.class);
        new BuilderReader(doc, null);
    }

}

