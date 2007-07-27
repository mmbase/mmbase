/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.functions;

import java.util.Arrays;
import java.lang.reflect.*;

import org.mmbase.util.logging.*;

/**
 * A SetFunction is a {@link Function} which wraps precisely one method of a class. It is used as one function of a 'set' of functions.
 *
 * @author Michiel Meeuwissen
 * @author Daniel Ockeloen
 * @author Pierre van Rooden
 * @version $Id: SetFunction.java,v 1.13.2.3 2007-07-27 13:59:35 michiel Exp $
 * @since MMBase-1.8
 * @see   FunctionSets
 */
public class SetFunction extends AbstractFunction {
    private static final Logger log = Logging.getLoggerInstance(SetFunction.class);

    /**
     * If type is 'class' the method must be static, or if it is not static, there will ie instantiated <em>one</em> object.
     */
    static final int TYPE_CLASS = 1;

    /**
     * If type is 'class' the method must not be static, and on every call to getFunctionValue, a new object is instantiated.
     */
    static final int TYPE_INSTANCE = 2;

    private Method functionMethod   = null;
    private Object functionInstance = null;
    private int type = TYPE_CLASS;
    private final int defLength;

    public SetFunction(String name, Parameter[] def, ReturnType returnType, String className, String methodName) {
        super(name, def, returnType);
        defLength = def.length;
        Class functionClass;
        try {
            functionClass = Class.forName(className);
        } catch(Exception e) {
            throw new RuntimeException("Can't create an application function class : " + className + " " + e.getMessage(), e);
        }
        initialize(functionClass, methodName);
    }
    public SetFunction(String name, Parameter[] def, String className, String methodName) {
        this(name, def, null, className, methodName);
    }
    /**
     * @since MMBase-1.8.5
     */
    public SetFunction(String name, Parameter[] def, Class clazz) {
        super(name, def, null);
        defLength = def.length;
        initialize(clazz, name);
    }

    /**
     */
    public Object getFunctionValue(Parameters parameters) {
        parameters.checkRequiredParameters();
        log.info("Calling " + functionMethod + " with " + parameters);
        try {
            if (defLength < parameters.size()) {
                // when wrapping this fucntion, it can happen that the number of parameters increases.
                return functionMethod.invoke(getInstance(), parameters.subList(0, defLength).toArray());
            } else {
                return functionMethod.invoke(getInstance(), parameters.toArray());
            }
        } catch (IllegalAccessException iae) {
            log.error("Function call failed (method not available) : " + name +", method: " + functionMethod +
                       ", instance: " + getInstance() +", parameters: " + parameters);
            return null;
        } catch (InvocationTargetException ite) {
            Throwable te = ite.getTargetException();
            if (te instanceof RuntimeException) {
                throw (RuntimeException) te;
            } else {
                throw new RuntimeException(te); // throw the actual exception that occurred
            }
        } catch (IllegalArgumentException iae) {
            String mes = 
                "Function call failed (method not available) : " + name +", method: " + functionMethod +
                ", instance: " + getInstance() +", parameters: " + parameters;
            throw new RuntimeException(mes, iae); 

        }
    }

    public void setType(String t) {
        if (t.equalsIgnoreCase("instance")) {
            type = TYPE_INSTANCE;
        } else {
            type = TYPE_CLASS;
        }
    }


    protected Object getInstance() {
        if (functionInstance != null || type == TYPE_CLASS) return functionInstance;
        try {
            return functionMethod.getDeclaringClass().newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Can't create an function instance : " + functionMethod.getDeclaringClass().getName(), e);
        }
    }

    private void checkReturnType() {
        if (returnType == null) {
            returnType = new ReturnType(functionMethod.getReturnType(), functionMethod.getReturnType().getClass().getName());
        }

	Class returni = functionMethod.getReturnType();
        if (returni.isPrimitive()) {
            if (returni.equals(Boolean.TYPE)) {
                returni = Boolean.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Character.class;
            } else if (returni.equals(Byte.TYPE)) {
                returni = Byte.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Short.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Integer.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Long.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Float.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Double.class;
            } else if (returni.equals(Character.TYPE)) {
                returni = Void.class;
            }
        }
	Class returnx = returnType.getDataType().getTypeAsClass();
        if (! returnx.isAssignableFrom(returni)) {
            log.warn("Return value of method " + functionMethod + " (" + returni + ") does not match method return type as specified in XML: (" + returnx + ")");
        }
    }

    /**
     * Initializes the function by creating an instance of the function class, and
     * locating the method to call.
     */
    private void initialize(Class functionClass, String methodName) {

        try {
            functionMethod = functionClass.getMethod(methodName, createParameters().toClassArray());
        } catch(NoSuchMethodException e) {
            throw new RuntimeException("Function method not found : " + functionClass + "." + methodName + "(" +  Arrays.asList(getParameterDefinition()) +")", e);
        }
        if (Modifier.isStatic(functionMethod.getModifiers())) {
            functionInstance = null;
        } else {
            if (type != TYPE_INSTANCE) {
                try {
                    functionInstance =  functionMethod.getDeclaringClass().newInstance();
                } catch(Exception e) {
                    throw new RuntimeException("Can't create an function instance : " + functionMethod.getDeclaringClass().getName(), e);
                }
            }
        }
        checkReturnType();

    }
}
