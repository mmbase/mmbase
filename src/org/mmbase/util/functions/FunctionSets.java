/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.*;
import org.mmbase.module.core.*;

import java.net.*;
import java.io.*;
import java.util.*;

import org.w3c.dom.*;


import org.mmbase.bridge.*;
import org.mmbase.bridge.implementation.*;

/**
 * A utility class for maintaining and querying functionsets.
 * A set function belongs to a certain namespace of functions ('sets'), and therefore is identified by
 * two strings: The name of the 'set' and the name of the function.
 * <br />
 * Function sets can be defined in the functions/functionsets.xml configuration file.
 * <br />
 * This class implements a number of static methods for maintaining {@link FunctionSet} objects,
 * and filling these with {@link SetFunction} objects that match the namespace.
 * It also implements a {@link #getFunction} method for obtaining a function from such a set.
 *
 * @author Dani&euml;l Ockeloen
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id: FunctionSets.java,v 1.6 2004-12-06 15:25:19 pierre Exp $ 
 */
public class FunctionSets {

    public static final String DTD_FUNCTIONSET_1_0  = "functionset_1_0.dtd";
    public static final String DTD_FUNCTIONSETS_1_0 = "functionsets_1_0.dtd";

    public static final String PUBLIC_ID_FUNCTIONSET_1_0  = "-//MMBase//DTD functionset config 1.0//EN";
    public static final String PUBLIC_ID_FUNCTIONSETS_1_0 = "-//MMBase//DTD functionsets config 1.0//EN";

    private static final Logger log = Logging.getLoggerInstance(FunctionSets.class);

    private static Map functionSets = new HashMap();
    private static boolean init = false;

    static {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_FUNCTIONSET_1_0,  DTD_FUNCTIONSET_1_0,  FunctionSets.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_FUNCTIONSETS_1_0, DTD_FUNCTIONSETS_1_0, FunctionSets.class);
    }

    /**
     * Returns the {@link #Function} with the given function name, and which exists in the set with the given set name.
     * If this is the first call, or if the set does not exist in the cache, the cache
     * is refreshed by reading the functionset.xml configuration file.
     * @param setName the name of the function set
     * @param functionName the name of the function
     * @return the {@link #Function}, or <code>nulll</code> if either the fucntion or set is not defined
     */
    public static Function getFunction(String setName, String functionName) {
        FunctionSet set = getFunctionSet(setName);
        if (set != null) {
            Function fun = set.getFunction(functionName);
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

    /**
     * Returns the {@link #FunctionSet} with the given set name.
     * If this is the first call, or if the set does not exist in the cache, the cache
     * is refreshed by reading the functionset.xml configuration file.
     * configuration file.
     * @param setName the name of the function set
     * @return the {@link #FunctionSet}, or <code>nulll</code> if the set is not defined
     */
    public static FunctionSet getFunctionSet(String setName) {
        if (!init) {
            readSets();
        }
        FunctionSet set = (FunctionSet)functionSets.get(setName);
        if (set == null) { // retry hack for mmpm
            readSets();
            set = (FunctionSet)functionSets.get(setName);
        }
        return set;
    }

    /**
     * Reads the current function set from the functionsets.xml configuration file.
     * The read sets are added to the functionset cache.
     * @todo It makes FunctionSet's now using a sub-XML. It would be possible to create a complete function-set by reflection.
     */
    private static void readSets() {
	init = true;
        org.xml.sax.InputSource source;
        try {
            source = ResourceLoader.getConfigurationRoot().getInputSource("functions/functionsets.xml");
        } catch (Exception e) {
            log.warn(e);
            return;
        }
        if (source == null) {
            log.info("No resource functions/functionsets.xml");
            return;
        }
        
        XMLBasicReader reader = new XMLBasicReader(source, FunctionSets.class);
        functionSets.clear();
        for(Enumeration ns = reader.getChildElements("functionsets", "functionset"); ns.hasMoreElements(); ) {
            Element n = (Element)ns.nextElement();

            NamedNodeMap nm = n.getAttributes();
            if (nm != null) {
                String name   = null;
                String setfile = null;

                // decode name
                org.w3c.dom.Node n3 = nm.getNamedItem("name");
                if (n3 != null) {
                    name = n3.getNodeValue();
                }

                // decode filename
                n3 = nm.getNamedItem("file");
                if (n3 != null) {
                    setfile = MMBaseContext.getConfigPath() + File.separator + "functions" + File.separator + n3.getNodeValue();
                    decodeFunctionSet(setfile, name);
                }
            }
        }
    }

    /**
     * Reads a 'sub' xml (a functionset XML) referred to by functionsets.xml.
     * The read set is added to the functionset cache.
     * @param
     * @param
     */
    private static void decodeFunctionSet(String fileName, String setName) {
        XMLBasicReader reader = new XMLBasicReader(fileName, FunctionSets.class);

        String status      = reader.getElementValue("functionset.status");
        String version     = reader.getElementValue("functionset.version");
        String setDescription = reader.getElementValue("functionset.description");

        FunctionSet functionSet = new FunctionSet(setName, version, status, setDescription);
        functionSets.put(setName, functionSet);

        functionSet.setFileName(fileName);

        for (Enumeration functionElements = reader.getChildElements("functionset","function"); functionElements.hasMoreElements();) {
            Element element = (Element)functionElements.nextElement();
            String functionName = reader.getElementAttributeValue(element,"name");
            if (functionName != null) {

                Element a = reader.getElementByPath(element, "function.type");
                String type = reader.getElementValue(a);

                a = reader.getElementByPath(element, "function.description");
                String description = reader.getElementValue(a);

                a = reader.getElementByPath(element, "function.class");
                String classname = reader.getElementValue(a);

                a = reader.getElementByPath(element, "function.method");
                String methodname = reader.getElementValue(a);

                // read the return types and values
                a = reader.getElementByPath(element, "function.return");

                ReturnType returnType = ReturnType.UNKNOWN;
                String returnTypeClassName = reader.getElementAttributeValue(a, "type");
                if (returnTypeClassName != null) {
                    try {
                        Class returnTypeClass = getClassFromName(returnTypeClassName);
                        returnType = new ReturnType(returnTypeClass, "");
                    } catch (Exception e) {
                        log.warn("Cannot determine return type : " + returnTypeClassName + ", using UNKNOWN");
                    }
                }

                /* obtaining field definitions for a result Node... useful ??

                for (Enumeration n2 = reader.getChildElements(a, "field"); n2.hasMoreElements();) {
                    Element return_element = (Element)n2.nextElement();
                    String returnFieldName = reader.getElementAttributeValue(return_element, "name");
                    String returnFieldValueType = reader.getElementAttributeValue(return_element, "type");
                    String returnFieldDescription = reader.getElementAttributeValue(return_element, "description");
                    // not implemented (yet) :
                    // FunctionReturnValue r=new FunctionReturnValue(returnname,returnvaluetype);
                    // fun.addReturnValue(returnname,r);
                    // r.setDescription(description);
                }
                */

                // read the parameters
                List parameterList = new ArrayList();
                for (Enumeration parameterElements = reader.getChildElements(element,"param"); parameterElements.hasMoreElements();) {
                    Element parameterElement = (Element)parameterElements.nextElement();
                    String parameterName = reader.getElementAttributeValue(parameterElement, "name");
                    String parameterType = reader.getElementAttributeValue(parameterElement, "type");
                    description = reader.getElementAttributeValue(parameterElement, "description");

                    Parameter parameter = null;

                    Class parameterClass = getClassFromName(parameterType);
                    parameter = new Parameter(parameterName, parameterClass);

                    // check for a default value
                    org.w3c.dom.Node n3 = parameterElement.getFirstChild();
                    if (n3 != null) {
                        parameter.setDefaultValue(parameter.autoCast(n3.getNodeValue()));
                    }
                    parameterList.add(parameter);

                }

                Parameter[] parameters = (Parameter[]) parameterList.toArray(new Parameter[0]);

                SetFunction fun = new SetFunction(functionName, parameters, returnType);
                fun.setDescription(description);
                fun.setClassName(classname);
                fun.setMethodName(methodname);
                fun.initialize();
                functionSet.addFunction(fun);
            }
        }
    }

    /**
     * Tries to determine the correct class from a given classname.
     * Classnames that are not fully expanded are expanded to the java.lang package.
     */
    private static Class getClassFromName(String className) {
        String fullClassName = className;
        boolean fullyQualified = className.indexOf('.') > -1;
        if (!fullyQualified) {
            if (className.equals("int")) { // needed?
                return int.class;
            }
            fullClassName = "java.lang." + fullClassName;
        }
        try {
            return Class.forName(fullClassName);
        } catch (ClassNotFoundException cne) {
            log.warn("Cannot determine parameter type : " + className + ", using Object as type instead.");
            return Object.class;
        }
    }

}
