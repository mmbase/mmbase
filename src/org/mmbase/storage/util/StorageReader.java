/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.util;

import java.util.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import org.mmbase.storage.*;
import org.mmbase.util.xml.DocumentReader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * @author Pierre van Rooden
 * @version $Id: StorageReader.java,v 1.3 2003-09-05 13:41:28 pierre Exp $
 */
public class StorageReader extends DocumentReader  {

    // logger
    private static Logger log = Logging.getLoggerInstance(StorageReader.class);

    /** Public ID of the Storage DTD version 1.0 */
    public static final String PUBLIC_ID_STORAGE_1_0 = "-//MMBase//DTD storage config 1.0//EN";
    /** DTD resource filename of the Database DTD version 1.0 */
    public static final String DTD_STORAGE_1_0 = "storage_1_0.dtd";

    /** Public ID of the most recent Database DTD */
    public static final String PUBLIC_ID_STORAGE = PUBLIC_ID_STORAGE_1_0;
    /** DTD resource filename of the most Database DTD */
    public static final String DTD_STORAGE = DTD_STORAGE_1_0;

    /**
     * Register the Public Ids for DTDs used by StorageReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        org.mmbase.util.XMLEntityResolver.registerPublicID(PUBLIC_ID_STORAGE_1_0, DTD_STORAGE_1_0, StorageReader.class);
    }

    /**
     * The factory for which the reader reads the document.
     * The factory is used to verify whether the document is compatible, and is used to instantiate objects
     * that depend on factory information (such as schemes)
     */
    protected StorageManagerFactory factory;

    /**
     * Constructor.
     *
     * @param factory the factory for which to read the storage configuration
     * @param Inputsource to the xml document.
     * @since MMBase-1.7
     */
    public StorageReader(StorageManagerFactory factory, InputSource source) {
        super(source, DocumentReader.validate(), StorageReader.class);
        this.factory = factory;
    }

    /**
     * Attempt to load a StorageManager class, using the classname as given in the configuration.
     * The method verifies whether the instantiated class is of the correct version.
     * @return the storage manager Class, or null if none was configured
     * @throws StorageConfigurationException if the factory version did not match, or the class configured is invalid
     */
    public Class getStorageManagerClass() throws StorageConfigurationException {
        Element root = document.getDocumentElement();
        if (factory != null) {
            // verify if the storagemanagerfactory is of the correct class, and
            // of the correct version
            NodeList factoryTagList = root.getElementsByTagName("storagemanagerfactory");
            if (factoryTagList.getLength()>0) {
                Element factoryTag = (Element)factoryTagList.item(0);
                try {
                    // obtain and check class
                    String factoryClassName = factoryTag.getAttribute("classname");
                    Class factoryClass = Class.forName(factoryClassName);
                    if (!factoryClass.isInstance(factory)) {
                        throw new StorageConfigurationException("StorageManager Configuration requires factory class '"+factoryClassName+"'.");
                    }
                    // obtain and check version
                    String storageManagerFactoryVersion = factoryTag.getAttribute("version");
                    if (storageManagerFactoryVersion != null) {
                        double version = Double.parseDouble(storageManagerFactoryVersion);
                        if (version > factory.getVersion()) {
                            throw new StorageConfigurationException("StorageManager Configuration requires factory version '"+version+", found "+factory.getVersion()+".");
                        }
                    }
                } catch (NumberFormatException pe) {
                    throw new StorageConfigurationException(pe);   // version is not an integer
                } catch (ClassNotFoundException cnfe) {
                    throw new StorageConfigurationException(cnfe); // factory class not found
                }
            }
        }
        NodeList managerTagList = root.getElementsByTagName("storagemanager");
        if (managerTagList.getLength()>0) {
            Element managerTag = (Element)managerTagList.item(0);
            String managerClassName = managerTag.getAttribute("classname");
            // intantiate storage manager and check version
            try {
                Class managerClass = Class.forName(managerClassName);
                StorageManager manager = (StorageManager)managerClass.newInstance();
                // obtain and check version
                String storageManagerVersion = managerTag.getAttribute("version");
                if (storageManagerVersion != null) {
                    double version = Double.parseDouble(storageManagerVersion);
                    if (version > manager.getVersion()) {
                        throw new StorageConfigurationException("StorageManager Configuration requires storage manager version '"+version+", found "+manager.getVersion()+".");
                    }
                }
                return managerClass;
            } catch (NumberFormatException pe) {
                throw new StorageConfigurationException(pe);   // version is not an integer
            } catch (ClassNotFoundException cnfe) {
                throw new StorageConfigurationException(cnfe);
            } catch (IllegalAccessException iae) {
                throw new StorageConfigurationException(iae);
            } catch (InstantiationException ie) {
                throw new StorageConfigurationException(ie);
            }
        } else {
            return null;
        }
    }

    /**
     * Attempt to load a SearchQueryHandler class, using the classname as given in the configuration.
     * @return the SearchQueryHandler class, or null if none was configured
     * @throws StorageConfigurationException if the class configured is invalid
     */
    public Class getSearchQueryHandlerClass() throws StorageConfigurationException {
        // override if otherwise specified
        Element root = document.getDocumentElement();
        NodeList handlerTagList = root.getElementsByTagName("searchqueryhandler");
        if (handlerTagList.getLength()>0) {
            Element handlerTag = (Element)handlerTagList.item(0);
            String queryHandlerClassName = handlerTag.getAttribute("classname");
            //  get class
            try {
                return Class.forName(queryHandlerClassName);
            } catch (ClassNotFoundException cnfe) {
                throw new StorageConfigurationException(cnfe);
            }
        } else {
            return null;
        }
    }

    /**
     * Reads all attributes from the reader and returns them as a map.
     * This include options, as well as the following special attributes:
     * <ul>
     *  <li>option-disallowed-fields-case-sensitive : has the Boolean value TRUE if disallowed fields are case sensitive (default FALSE)</li>
     * </ul>
     * @return attributes as a map
     */
    public Map getAttributes() {
        Map attributes = new HashMap();
        Element root = document.getDocumentElement();
        NodeList attributesTagList = root.getElementsByTagName("attributes");
        if (attributesTagList.getLength()>0) {
            Element attributesTag = (Element)attributesTagList.item(0);
            NodeList attributeTagList = attributesTag.getElementsByTagName("attribute");
            for (int i=0; i<attributeTagList.getLength(); i++) {
                Element attributeTag = (Element)attributeTagList.item(i);
                String attributeName = attributeTag.getAttribute("name");
                // require an attribute name.
                // if not given, skip the option.
                if (attributeName != null) {
                    attributes.put(attributeName,getNodeTextValue(attributeTag));
                }
            }
            NodeList optionTagList = attributesTag.getElementsByTagName("option");
            for (int i=0; i<optionTagList.getLength(); i++) {
                Element optionTag = (Element)optionTagList.item(i);
                // require an option name.
                // if not given, skip the option.
                String optionName = optionTag.getAttribute("name");
                if (optionName != null) {
                    String optionValue = optionTag.getAttribute("value");
                    Boolean value = Boolean.TRUE;
                    if (optionValue != null) {
                        value = new Boolean(optionValue);
                    }
                    attributes.put(optionName,value);
                }
            }
            NodeList schemeTagList = attributesTag.getElementsByTagName("scheme");
            for (int i=0; i<schemeTagList.getLength(); i++) {
                Element schemeTag = (Element)schemeTagList.item(i);
                String schemeName = schemeTag.getAttribute("name");
                // require a scheme name
                // if not given, skip the option
                if (schemeName != null) {
                    String pattern = getNodeTextValue(schemeTag);
                    if (pattern==null || pattern.equals("")) {
                        attributes.put(schemeName, null);
                    } else {
                        attributes.put(schemeName, new Scheme(factory,getNodeTextValue(schemeTag)));
                    }
                }
            }
        }
        // system attributes
        NodeList disallowedFieldsList = root.getElementsByTagName("disallowed-fields");
        if (disallowedFieldsList.getLength()>0) {
            Element disallowedFieldsTag = (Element)disallowedFieldsList.item(0);
            attributes.put(Attributes.DISALLOWED_FIELD_CASE_SENSITIVE, Boolean.valueOf(disallowedFieldsTag.getAttribute("case-sensitive")));
            attributes.put(Attributes.ENFORCE_DISALLOWED_FIELDS, Boolean.valueOf(disallowedFieldsTag.getAttribute("enforce")));
        }
        return attributes;
    }

    /**
     * Returns all disallowed fields and their possible alternate values.
     * The fields are returned as name-value pairs, where the disallowedfieldname is the key, and
     * the alternate name is the value (null if no name is given).
     * @return disallowed fields as a map
     */
    public Map getDisallowedFields() {
        Map disallowedFields = new HashMap();
        Element root = document.getDocumentElement();
        NodeList disallowedFieldsList = root.getElementsByTagName("disallowed-fields");
        if (disallowedFieldsList.getLength() > 0) {
            Element disallowedFieldsTag = (Element)disallowedFieldsList.item(0);
            boolean casesensitive = Boolean.valueOf(disallowedFieldsTag.getAttribute("case-sensitive")).booleanValue();
            NodeList fieldTagList = disallowedFieldsTag.getElementsByTagName("disallowed-field");
            for (int i = 0; i < fieldTagList.getLength(); i++) {
                Element fieldTag = (Element)fieldTagList.item(i);
                String fieldName = fieldTag.getAttribute("name");
                // require a field name.
                // if not given, skip the option.
                if (fieldName != null) {
                    if (!casesensitive) fieldName = fieldName.toLowerCase();
                    String replacement = fieldTag.getAttribute("replacement");
                    disallowedFields.put(fieldName,replacement);
                }
            }
        }
        return disallowedFields;
    }


    /**
     * Returns all type mappings.
     * The mappings are returned in the order that they were given in the reader.
     * Calling code should sort this list if they want to use TypoMapping fuzzy matching.
     * @return a List of TypeMapping objects
     */
    public List getTypeMappings() {
        List typeMappings = new ArrayList();
        Element root = document.getDocumentElement();
        NodeList typeMappingsTagList = root.getElementsByTagName("type-mappings");
        if (typeMappingsTagList.getLength()>0) {
            Element typeMappingsTag = (Element)typeMappingsTagList.item(0);
            NodeList typeMappingTagList = typeMappingsTag.getElementsByTagName("type-mapping");
            for (int i=0; i<typeMappingTagList.getLength(); i++) {
                Element typeMappingTag = (Element)typeMappingTagList.item(i);
                TypeMapping typeMapping = new TypeMapping();
                typeMapping.name = typeMappingTag.getAttribute("name");
                // require a type-mapping name (a MMBase type)
                // if not given, skip the option.
                if (typeMapping.name != null) {
                    // obtain min/max values for sizes
                    try {
                        typeMapping.minSize = Integer.parseInt(typeMappingTag.getAttribute("min-size"));
                    } catch (NumberFormatException nfe) {}
                    try {
                        typeMapping.maxSize = Integer.parseInt(typeMappingTag.getAttribute("max-size"));
                    } catch (NumberFormatException nfe) {}
                    // get the type to convert to
                    typeMapping.type = typeMappingTag.getAttribute("type");
                    typeMappings.add(typeMapping);
                }
            }
        }
        return typeMappings;
    }
}
