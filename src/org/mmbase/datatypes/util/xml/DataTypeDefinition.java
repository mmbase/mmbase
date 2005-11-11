/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes.util.xml;

import java.util.*;
import org.w3c.dom.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.mmbase.bridge.Field;
import org.mmbase.datatypes.processors.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.datatypes.*;
import org.mmbase.core.util.Fields;
import org.mmbase.util.*;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.xml.DocumentReader;
import org.mmbase.util.xml.XMLWriter;
import org.mmbase.util.logging.*;
import org.mmbase.util.transformers.*;

/**
 * This utility class contains methods to instantiate the right DataType instance. It is used by DataTypeReader.
 *
 * XXX MM: This class does the actual datatype reading, so more correctly this one would be called
 * DatatypeReader?
 *
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id: DataTypeDefinition.java,v 1.42 2005-11-11 14:29:20 pierre Exp $
 * @since MMBase-1.8
 **/
public class DataTypeDefinition {

    private static final Logger log = Logging.getLoggerInstance(DataTypeDefinition.class);

    /**
     * The data type which will be produced
     */
    public BasicDataType dataType = null;

    /**
     * The base data type on which it was based, or <code>null</code>
     */
    private BasicDataType baseDataType = null;

    /**
     * The data type collector that contains the data datatype with this definition.
     */
    protected final DataTypeCollector collector;

    /**
     * Constructor.
     */
    public DataTypeDefinition(DataTypeCollector collector) {
        this.collector = collector;
    }


    private static int anonymousSequence = 1;

    private String getId(String id) {
        if (id.equals("")) {
            if (baseDataType == null) {
                return "ANONYMOUS" + anonymousSequence++;
            } else {
                return  baseDataType.getName() + anonymousSequence++;
            }
        } else {
            return id;
        }
    }
    /**
     * If id was empty string, then this can still be equal to baseDataType, and nothing changed. Never <code>null</code>
     * @param   dataTypeElement piece of XML used to configure. Only the 'class' subelements are explored in this method.
     * @param   id              the new id or empty string (which means that is still identical to baseDataType)
     *
     */
    private  void getImplementation(Element dataTypeElement, String id) {
        BasicDataType dt = id.equals("") ? null : collector.getDataType(id);
        if (dt != null) {
            collector.rewrite(dt);
        }

        // Check the class element only.
        NodeList childNodes = dataTypeElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                Element childElement = (Element) childNodes.item(i);
                if (childElement.getLocalName().equals("class")) {
                    String className = childElement.getAttribute("name");
                    if (dt != null) {
                        log.debug("Already defined " + id);
                        if (! className.equals(dt.getClass().getName())) {
                            log.error("Cannot change class for '" + id + "' from " + dt.getClass().getName() + " to '" + className + "'");
                        }
                    } else {
                        try {
                            Class claz = Class.forName(className);
                            log.info("Instantiating " + claz + " for " + dataType);
                            java.lang.reflect.Constructor constructor = claz.getConstructor(new Class[] { String.class});
                            dt = (BasicDataType) constructor.newInstance(new Object[] { getId(id) });
                            if (baseDataType != null) {
                                // should check class here, perhaps
                                dt.inherit((BasicDataType) baseDataType);
                            }
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                    break;
                } else {
                    continue;
                }
            }
        }
        if (dt == null) { // either it had no id, or it did not exist yet.
            if (baseDataType == null) {
                log.warn("No base datatype available and no class specified for datatype '" + id + "', using 'unknown' for know.\n" + XMLWriter.write(dataTypeElement, true, true));
                baseDataType = Constants.DATATYPE_UNKNOWN;
            }
            if (id.equals("")) {
                log.debug("No id given, for the time being this datatype will be equal to its base type " + baseDataType);
                dataType = baseDataType;
            } else {
                log.debug("Id given, cloning " + baseDataType);
                dataType = (BasicDataType) baseDataType.clone(id);
            }
        } else { // means that it existed it already
            log.debug("Existing datatype " + dt + " with base " + baseDataType);
            dataType = dt;
        }

    }

    /**
     * Configures the data type definition, using data from a DOM element
     */
    DataTypeDefinition configure(Element dataTypeElement, BasicDataType requestBaseDataType) {

        String id = DataTypeXml.getAttribute(dataTypeElement, "id");

        if ("byte".equals(id)) {
            log.warn("Found for datatype id 'byte', supposing that 'binary' is meant");
            id = "binary"; // hmmmmm
        }

        String base = DataTypeXml.getAttribute(dataTypeElement, "base");
        if (log.isDebugEnabled()) {
            log.debug("Reading element id='" + id + "' base='" + base + "' req datatype " + requestBaseDataType);
        }
        if (! base.equals("")) { // also specified, let's see if it is correct

            BasicDataType definedBaseDataType = collector.getDataType(base, true);
            if (requestBaseDataType != null) {
                if (requestBaseDataType != definedBaseDataType) {
                    log.warn("Attribute 'base' ('" + base+ "') not allowed with datatype '" + id + "', because it has already an baseDataType '" + baseDataType + "'");
                }
            }
            if (definedBaseDataType == null) {
                log.warn("Attribute 'base' ('" + base + "') of datatype '" + id + "' is an unknown datatype.");
            } else {
                requestBaseDataType = definedBaseDataType;
            }
        }

        baseDataType = requestBaseDataType;
        getImplementation(dataTypeElement, id);
        LocalizedString description = dataType.getLocalizedDescription();
        DataTypeXml.getLocalizedDescription("description", dataTypeElement, description, dataType.getName());
        configureConditions(dataTypeElement);

        return this;
    }



    private static final java.util.regex.Pattern nonConditions   = java.util.regex.Pattern.compile("specialization|datatype|class|description");


    /**
     * Configures the conditions of a datatype definition, using data from a DOM element
     */
    protected void configureConditions(Element dataTypeElement) {
        log.debug("Now going to configure " + dataType);
        // add conditions
        NodeList childNodes = dataTypeElement.getChildNodes();
        for (int k = 0; k < childNodes.getLength(); k++) {
            if (childNodes.item(k) instanceof Element) {
                Element childElement = (Element) childNodes.item(k);
                if (childElement.getLocalName().equals("")) {
                    continue;
                }
                if (nonConditions.matcher(childElement.getLocalName()).matches()) {
                    continue;
                }
                if (dataType == baseDataType) {
                    log.debug("About to add/change conditions, need clone first!");
                    dataType = (BasicDataType) baseDataType.clone(getId(""));
                }
                log.debug("Considering " + childElement.getLocalName() + " for " + dataType);
                if (!addCondition(childElement)) {
                    log.error("" + XMLWriter.write(childElement, true, true) + " defines '" + childElement.getLocalName() + "', but " + dataType + " doesn't support that");
                }
            }
        }
    }

    /**
     * Uses one subelement of a datatype xml configuration element and interpret it. Possibly this
     * method is a good candidate to override.
     * @return whether successfully read the element.
     */
    protected boolean addCondition(Element childElement) {
        boolean ret = false;
        String childTag = childElement.getLocalName();
        if ("property".equals(childTag)) {
            ret = setProperty(childElement);
        } else if ("required".equals(childTag)) {
            boolean value = DataTypeXml.getBooleanValue(childElement, false);
            setRestrictionData(dataType.setRequired(value), childElement);
            ret = true;
        } else if ("unique".equals(childTag)) {
            boolean value = DataTypeXml.getBooleanValue(childElement, false);
            setRestrictionData(dataType.setUnique(value), childElement);
            ret = true;
        } else if ("getprocessor".equals(childTag)) {
            addProcessor(DataType.PROCESS_GET, childElement);
            ret = true;
        } else if ("setprocessor".equals(childTag)) {
            addProcessor(DataType.PROCESS_SET, childElement);
            ret = true;
        } else if ("commitprocessor".equals(childTag)) {
            addCommitProcessor(childElement);
            ret = true;
        } else if ("enumeration".equals(childTag)) {
            addEnumeration(childElement);
            ret = true;
        } else if ("default".equals(childTag)) {
            String value = DataTypeXml.getAttribute(childElement, "value");
            dataType.setDefaultValue(value);
            ret = true;
        } else if (addPatternCondition(childElement)) {
            ret = true;
        } else if (addPasswordProperty(childElement)) {
            ret = true;
        } else if (addLengthDataCondition(childElement)) {
            ret =  true;
        } else if (addComparableCondition(childElement)) {
            ret = true;
        }
        return ret;
    }


    private void addProcessor(int action, int processingType, Processor newProcessor) {
        Processor oldProcessor = dataType.getProcessor(action, processingType);
        newProcessor = DataTypeXml.chainProcessors(oldProcessor, newProcessor);
        log.info(dataType + " Found processor " + oldProcessor + "--> " + newProcessor);
        dataType.setProcessor(action, newProcessor, processingType);
    }


    protected  void addProcessor(int action, Element processorElement) {
        Processor newProcessor = DataTypeXml.createProcessor(processorElement);
        if (newProcessor != null) {
            String type = processorElement.getAttribute("type");
            if (!type.equals("") && !type.equals("*")) { // "" was not equal to "*" !
                int processingType = Field.TYPE_UNKNOWN;
                BasicDataType basicDataType = DataTypes.getDataType(type); // this makes NO sense, processors type are assocated with bridge methods (field types) not with datatypes
                if (basicDataType != null) {
                    processingType = Fields.classToType(basicDataType.getTypeAsClass());
                } else {
                    log.warn("Datatype " + type + " is unknown, create processor as a default processor");
                }
                addProcessor(action, processingType, newProcessor);
            } else {
                // todo: iterate through all types?
                addProcessor(action, Field.TYPE_UNKNOWN, newProcessor);
            }
        }
    }

    protected void addCommitProcessor(Element processorElement) {
        CommitProcessor newProcessor = DataTypeXml.createCommitProcessor(processorElement);
        CommitProcessor oldProcessor = dataType.getCommitProcessor();
        newProcessor = DataTypeXml.chainProcessors(oldProcessor, newProcessor);
        dataType.setCommitProcessor(newProcessor);
    }

    protected void setRestrictionData(DataType.Restriction restriction, Element element) {
        if (DataTypeXml.hasAttribute(element, "fixed")) {
            boolean isFixed = Boolean.valueOf(DataTypeXml.getAttribute(element, "fixed")).booleanValue();
            restriction.setFixed(isFixed);
        }
        if (DataTypeXml.hasAttribute(element, "enforce")) {
            String enforce = DataTypeXml.getAttribute(element, "enforce").toLowerCase();
            if (enforce.equals("always")) {
                restriction.setEnforceStrength(DataType.ENFORCE_ALWAYS);
            } else if (enforce.equals("onchange")) {
                restriction.setEnforceStrength(DataType.ENFORCE_ONCHANGE);
            } else if (enforce.equals("oncreate")) {
                restriction.setEnforceStrength(DataType.ENFORCE_ONCREATE);
            } else if (enforce.equals("never")) {
                restriction.setEnforceStrength(DataType.ENFORCE_NEVER);
            } else {
                log.warn("Unrecognised value for 'enforce' attribute '" + enforce + "' in " + XMLWriter.write(element, true, true));
            }
        }
        LocalizedString descriptions = restriction.getErrorDescription();
        restriction.setErrorDescription(DataTypeXml.getLocalizedDescription("description", element, descriptions, null));
    }

    /**
     * Used the enumeration element.
     */
    protected void addEnumeration(Element enumerationElement) {
        setRestrictionData(dataType.getEnumerationRestriction(), enumerationElement);
        LocalizedEntryListFactory fact = dataType.getEnumerationFactory();
        NodeList childNodes = enumerationElement.getElementsByTagName("query");
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element queryElement = (Element) childNodes.item(i);
            Locale locale = DataTypeXml.getLocale(queryElement);
            fact.addQuery(locale, DocumentReader.toDocument(queryElement));
        }
        childNodes = enumerationElement.getElementsByTagName("entry");
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element entryElement = (Element) childNodes.item(i);
            String value = entryElement.getAttribute("value");
            if (!value.equals("")) {
                Locale locale = DataTypeXml.getLocale(entryElement);
                String display = entryElement.getAttribute("display");
                if (display.equals("")) display = value;
                Object key = Casting.toType(dataType.getTypeAsClass(), value);
                if (key instanceof java.io.Serializable) {
                    log.info("Adding " + key + "/" + display + " for " + locale);
                    fact.add(locale, (java.io.Serializable) key, display);
                } else {
                    log.error("key " + key + " for " + dataType + " is not serializable, cannot be added to entrylist factory.");
                }
            } else {
                String resource = entryElement.getAttribute("basename");
                if (! resource.equals("")) {
                    Comparator comparator = null;
                    Class wrapper    = dataType.getTypeAsClass();
                    if (! Comparable.class.isAssignableFrom(wrapper)) {
                        wrapper = null;
                    }

                    {
                        String sorterClass = entryElement.getAttribute("sorterclass");
                        if (!sorterClass.equals("")) {
                            try {
                                Class sorter = Class.forName(sorterClass);
                                if (Comparator.class.isAssignableFrom(sorter)) {
                                    comparator = (Comparator) sorter.newInstance();
                                } else {
                                    wrapper = sorter;
                                }
                            } catch (Exception e) {
                            log.error(e);
                            }
                        }
                    }
                    Class constantsClass = null;
                    {
                        String javaConstants = entryElement.getAttribute("javaconstants");
                        if (!javaConstants.equals("")) {
                            try {
                                constantsClass = Class.forName(javaConstants);
                            } catch (Exception e) {
                                log.error(e);
                            }
                        }
                    }
                    try {
                        fact.addBundle(resource, getClass().getClassLoader(), constantsClass,
                                       wrapper, comparator);
                    } catch (MissingResourceException mre) {
                        log.error(mre);
                    }
                } else {
                    throw new IllegalArgumentException("no 'value' or 'basename' attribute on enumeration entry element");
                }
            }
            log.info("Found enumeration values now " + fact);
        }
        log.info("--" );
    }

    protected boolean setProperty(Element element) {
        try {
            String name = DataTypeXml.getAttribute(element, "name");
            String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
            String value = DataTypeXml.getAttribute(element, "value");
            Class claz = dataType.getClass();
            Method method = claz.getMethod(methodName, new Class[] {String.class});
            method.invoke(dataType, new Object[] { value });
        } catch (NoSuchMethodException nsme) {
            log.warn(nsme);
            return false;
        } catch (SecurityException se) {
            log.warn(se);
            return false;
        } catch (IllegalAccessException iae) {
            log.warn(iae);
            return false;
        } catch (InvocationTargetException ite) {
            log.warn(ite);
            return false;
        }
        return true;
    }

    /**
     * Considers length related condition elements ('minLength', 'maxLength' , 'length')
     * @return <code>true</code> if element was used
     */
    protected boolean addLengthDataCondition(Element conditionElement) {
        if (dataType instanceof LengthDataType) {
            String localName = conditionElement.getLocalName();
            LengthDataType bDataType = (LengthDataType) dataType;
            if ("minLength".equals(localName)) {
                long value = DataTypeXml.getLongValue(conditionElement);
                setRestrictionData(bDataType.setMinLength(value), conditionElement);
                return true;
            } else if ("maxLength".equals(localName)) {
                long value = DataTypeXml.getLongValue(conditionElement);
                setRestrictionData(bDataType.setMaxLength(value), conditionElement);
                return true;
            } else if ("length".equals(localName)) {
                long value = DataTypeXml.getLongValue(conditionElement);
                setRestrictionData(bDataType.setMinLength(value), conditionElement);
                setRestrictionData(bDataType.setMaxLength(value), conditionElement);
                return true;
            }
        }
        return false;
    }



    protected boolean addPasswordProperty(Element propertyElement) {
        String localName = propertyElement.getLocalName();
        if ("password".equals(localName) && (dataType instanceof StringDataType)) {
            StringDataType stringDataType = (StringDataType) dataType;
            boolean value = Casting.toBoolean(DataTypeXml.getAttribute(propertyElement, "value"));
            stringDataType.setPassword(value);
            return true;
        }
        return false;
    }

    /**
     * Considers the 'pattern' condition element.
     * @return <code>true</code> if element was used
     */

    protected boolean addPatternCondition(Element conditionElement) {
        String localName = conditionElement.getLocalName();
        if (dataType instanceof StringDataType) {
            StringDataType sDataType = (StringDataType) dataType;
            if ("pattern".equals(localName)) {
                String value = DataTypeXml.getAttribute(conditionElement, "value");
                log.debug("Setting pattern on " + sDataType);
                setRestrictionData(sDataType.setPattern(java.util.regex.Pattern.compile(value)), conditionElement);
                return true;
            }
        } else if (dataType instanceof DateTimeDataType) {
            DateTimeDataType sDataType = (DateTimeDataType) dataType;
            if ("pattern".equals(localName)) {
                String value = DataTypeXml.getAttribute(conditionElement, "value");
                Locale locale = DataTypeXml.getLocale(conditionElement);
                sDataType.setPattern(value, locale);
                return true;
            }
       }
        return false;
    }

    /**
     * Considers the condition elements associated with 'Comparables' (minInclusive, maxInclusive,
     * minExclusive, maxExclusive).
     * @return <code>true</code> if element was used
     */

    protected boolean addComparableCondition(Element conditionElement) {
        if (dataType instanceof ComparableDataType) {
            String localName = conditionElement.getLocalName();
            ComparableDataType dDataType = (ComparableDataType) dataType;
            if ("minExclusive".equals(localName) || "minInclusive".equals(localName)) {
                Comparable value = (Comparable) dDataType.cast(DataTypeXml.getValue(conditionElement), null, null);
                setRestrictionData(dDataType.setMin(value, "minInclusive".equals(localName)), conditionElement);
                return true;
            } else if ("maxExclusive".equals(localName) || "maxInclusive".equals(localName)) {
                Comparable value = (Comparable) dDataType.cast(DataTypeXml.getValue(conditionElement), null, null);
                setRestrictionData(dDataType.setMax(value, "maxInclusive".equals(localName)), conditionElement);
                return true;
            }
        }
        return false;
    }


    public String toString() {
        return "definition(" + (dataType == null ? "NONE" : dataType.toString()) + ")";
    }

}
