/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.DocumentReader;
import org.mmbase.util.*;
import org.mmbase.util.xml.XMLWriter;

import java.io.*;
import java.util.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import java.net.*;



/**
 * A utility class for maintaining and querying functionsets.
 * A set function belongs to a certain namespace of functions ('sets'), and therefore is identified by
 * two strings: The name of the 'set' and the name of the function.
 * <br />
 * Function sets can be defined in the functions/functionsets.xml configuration file.
 * <br />
 * This class implements a number of static methods for maintaining {@link FunctionSet} objects,
 * and filling these with {@link FunctionSet} objects that match the namespace.
 * It also implements a {@link #getFunction} method for obtaining a function from such a set.
 *
 * @author Dani&euml;l Ockeloen
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id$
 */
public class FunctionSets {

    public static final String DTD_FUNCTIONSET_1_0  = "functionset_1_0.dtd";
    public static final String DTD_FUNCTIONSETS_1_0 = "functionsets_1_0.dtd";

    public static final String PUBLIC_ID_FUNCTIONSET_1_0  = "-//MMBase//DTD functionset config 1.0//EN";
    public static final String PUBLIC_ID_FUNCTIONSETS_1_0 = "-//MMBase//DTD functionsets config 1.0//EN";

    private static final String XSD_FUNCTIONSET       = "functionset.xsd";
    public static  final String FUNCTIONSET_NS  = "http://www.mmbase.org/xmlns/functionset";

    private static final Logger log = Logging.getLoggerInstance(FunctionSets.class);

    private static final Map<String, FunctionSet> functionSets = new TreeMap<String, FunctionSet>();

    static {
        org.mmbase.util.xml.EntityResolver.registerSystemID(FUNCTIONSET_NS + ".xsd", XSD_FUNCTIONSET, FunctionSets.class);

        org.mmbase.util.xml.EntityResolver.registerPublicID(PUBLIC_ID_FUNCTIONSET_1_0,  DTD_FUNCTIONSET_1_0,  FunctionSets.class);
        org.mmbase.util.xml.EntityResolver.registerPublicID(PUBLIC_ID_FUNCTIONSETS_1_0, DTD_FUNCTIONSETS_1_0, FunctionSets.class);
    }

    /**
     * Returns the {@link Function} with the given function name, and which exists in the set with the given set name.
     * If this is the first call, or if the set does not exist in the cache, the cache
     * is refreshed by reading the functionset.xml configuration file.
     * @param setName the name of the function set
     * @param functionName the name of the function
     * @return the {@link Function}, or <code>nulll</code> if either the fucntion or set is not defined
     */
    public static Function<?> getFunction(String setName, String functionName) {
        FunctionSet set = getFunctionSet(setName);
        if (set != null) {
            Function<?> fun = set.getFunction(functionName);
            if (fun != null) {
                return fun;
            } else {
                log.warn("No function with name : " + functionName + " in set : " + setName + ", functions available: " + set);
            }
        } else {
            log.warn("No functionset with name : " + setName);
        }
        return null;
    }

    static {
        ResourceLoader functionLoader = ResourceLoader.getConfigurationRoot().getChildResourceLoader("functions");
        // read the XML
        try {
            ResourceWatcher watcher = new ResourceWatcher(functionLoader) {
                    public void onChange(String resource) {
                        functionSets.clear();
                        clear();
                        add(resource);
                        readSets(this);
                    }
                };
            readSets(watcher);
            watcher.setDelay(ResourceWatcher.getDefaultResourceWatcherDelay());
            watcher.start();
        } catch (Throwable t) {
            log.error(t.getClass().getName(), t);
        }

    }

    /**
     * Returns the {@link FunctionSet} with the given set name.
     * If this is the first call, or if the set does not exist in the cache, the cache
     * is refreshed by reading the functionset.xml configuration file.
     * configuration file.
     * @param setName the name of the function set
     * @return the {@link FunctionSet}, or <code>null</code> if the set is not defined
     */
    public static FunctionSet getFunctionSet(String setName) {
        return functionSets.get(setName);
    }
    /**
     * @since MMBase-1.9
     */
    public static Map<String, FunctionSet> getFunctionSets() {
        return Collections.unmodifiableMap(functionSets);
    }

    /**
     * Reads the current function set from the functionsets.xml configuration file.
     * The read sets are added to the functionset cache.
     * @todo It makes FunctionSet's now using a sub-XML. It would be possible to create a complete function-set by reflection.
     */

    private static synchronized void readSets(ResourceWatcher watcher) {

        List<URL> resources = watcher.getResourceLoader().getResourceList("functionsets.xml");
        watcher.clear();

        watcher.add("functionsets.xml");

        log.service("Using " + resources);
        ListIterator<URL> i = resources.listIterator();
        while (i.hasNext()) {
            i.next();
        }
        while (i.hasPrevious()) {
            try {
                URL u = i.previous();
                log.service("Reading " + u);
                URLConnection con = u.openConnection();
                if (con.getDoInput()) {
                    InputSource source = new InputSource(con.getInputStream());
                    DocumentReader reader = new DocumentReader(source, FunctionSets.class);
                    reader.setSystemId(u.toString());
                    for (Element n: reader.getChildElements("functionsets", "functionset")) {
                        try {
                            String setName     = n.getAttribute("name");
                            if (functionSets.containsKey(setName)) {
                                log.debug("The function-set '" + setName + "' did exist already, while reading " + u);
                            }
                            String setResource = n.getAttribute("resource");
                            if (setResource.equals("")) setResource = n.getAttribute("file"); // deprecated, it's not necessarily a file
                            if (! watcher.getResources().contains(setResource)) {
                                watcher.add(setResource);
                            }
                            decodeFunctionSet(watcher.getResourceLoader(), setResource, setName);
                        } catch (Throwable t) {
                            log.error(t.getClass() + " " +  t.getMessage(), t);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * Reads a 'sub' xml (a functionset XML) referred to by functionsets.xml.
     * The read set is added to the functionset cache.
     * @param
     * @param
     */
    private static void decodeFunctionSet(ResourceLoader loader, String setResource, String setName) throws IOException, org.xml.sax.SAXException {

        org.w3c.dom.Document doc = loader.getDocument(setResource, true, FunctionSets.class);
        DocumentReader reader = new DocumentReader(doc);

        log.service("Parsing " + reader.getSystemId());
        String setDescription = reader.getElementValue("functionset.description");

        FunctionSet functionSet = new FunctionSet(setName, setDescription);
        functionSets.put(setName, functionSet);

        for (Element element: reader.getChildElements("functionset", "function")) {
            final String functionName = reader.getElementAttributeValue(element, "name");
            if (functionName != null) {

                Element a = DocumentReader.getElementByPath(element, "function.type");

                String type = DocumentReader.getElementValue(a); // 'class' or 'instance'

                a = DocumentReader.getElementByPath(element, "function.description");
                String description = DocumentReader.getElementValue(a);

                a = DocumentReader.getElementByPath(element, "function.class");
                String className = DocumentReader.getElementValue(a);

                a = DocumentReader.getElementByPath(element, "function.method");
                String methodName = DocumentReader.getElementValue(a);

                // read the return types and values
                a = DocumentReader.getElementByPath(element, "function.return");
                ReturnType returnType = null;
                if (a != null) {
                    String returnTypeClassName = reader.getElementAttributeValue(a, "type");
                    if (returnTypeClassName != null) {
                        try {
                            returnType = new ReturnType(Parameter.getClassForName(returnTypeClassName), "");
                        } catch (Exception e) {
                            log.warn("Cannot determine return type : " + returnTypeClassName + ", will auto-detect");
                        }
                    }
                }


                /* obtaining field definitions for a result Node... useful ??

                for (Element return_element: reader.getChildElements(a, "field")) {
                    String returnFieldName = reader.getElementAttributeValue(return_element, "name");
                    String returnFieldValueType = reader.getElementAttributeValue(return_element, "type");
                    String returnFieldDescription = reader.getElementAttributeValue(return_element, "description");
                    // not implemented (yet) :
                    // FunctionReturnValue r=new FunctionReturnValue(returnname,returnvaluetype);
                    // fun.addReturnValue(returnname,r);
                    // r.setDescription(description);
                }
                */

                SetFunction.Type functionType = SetFunction.Type.valueOf(type.toUpperCase());

                // read the parameters

                Parameter<?>[] parameters = Parameter.readArrayFromXml(element);

                if (functionType != SetFunction.Type.BEAN) {
                    for (Parameter param : parameters) {
                        if (param.getClass().isPrimitive() && param.getDefaultValue() == null) {
                            // that would give enigmatic IllegalArgumentExceptions, so fix that.
                            param.setDefaultValue(Casting.toType(param.getClass(), -1));
                            log.debug("Primitive parameter '" + param + "' had default value null, which is impossible for primitive types. Setting to " + param.getDefaultValue());
                        }
                    }
                } else {
                    if (parameters.length != 0) {
                        throw new RuntimeException("Cannot define explicit parmeters for BEAN functions");
                    }
                }

                try {
                    Class functionClass;
                    try {
                        functionClass = Class.forName(className);
                    } catch(Exception e) {
                        throw new RuntimeException("Can't create an application function class : " + className + " " + e.getMessage(), e);
                    }
                    Function fun;
                    if (functionType == SetFunction.Type.BEAN) {
                        fun = new WrappedFunction(BeanFunction.getFunction(functionClass, methodName)) {
                                @Override
                                public String getName() {
                                    return functionName;
                                }
                            };
                    } else {
                        fun = new SetFunction(functionName, parameters, returnType, functionClass, methodName, functionType);
                    }
                    fun.setDescription(description);
                    Function prev = functionSet.addFunction(fun);
                    if (prev != null && ! (prev.equals(fun))) {
                        log.warn("Replaced " + prev + " with " + fun + " in function set " + functionSet);
                    } else {
                        log.debug("Found " + fun);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.warn("No function name specified on " + XMLWriter.write(element));
            }
        }
    }


}
