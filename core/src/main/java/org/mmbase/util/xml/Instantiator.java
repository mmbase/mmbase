/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import org.mmbase.util.Casting;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import org.w3c.dom.*;

/**
 * Utilities to use an XML to instantiate Java objects, using
 * reflection. This is used by various configuration-read code, which
 * all perform similar tasks of instantiating and configurating
 * certain objects.
 *
 * Supported are schemes like
 <pre>
  &lt;class name="class name"&gt;
     &lt;param name="parameter name"&gt;parameter value&lt;/param&gt;
      ...
  &lt;/class&gt;
  and
  &lt;anyelement class="class name"&gt;
     &lt;param name="parameter name"&gt;parameter value&lt;/param&gt;
      ...
  &lt;/anyname&gt;
  </pre>
 * Param subtags are matched with 'setters' on the created object.
 *
 *
 * @since MMBase-1.9
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public abstract class Instantiator {

    private static final Logger log = Logging.getLoggerInstance(Instantiator.class);


    /**
     * @see {@link #getInstance(Class, Element, Object...)}
     * @since MMBase-1.9.6
     */
    public static Class getClass(Element classElement) throws ClassNotFoundException {
        String className;
        if (classElement.getTagName().equals("class")) {
            className = classElement.getAttribute("name");
        } else {
            className = classElement.getAttribute("class");
            if ("".equals(className)) {
                className = classElement.getAttribute("name");
            }
        }
        if ("".equals(className)) { // try body
            className = BuilderReader.getNodeTextValue(classElement);
        }
        return Class.forName(className);
    }
    /**
     * @see {@link #getInstance(Class, Element, Object...)}
     */
    public static Object getInstance(Element classElement, Object... args)
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        return getInstance(getClass(classElement), classElement, args);
    }
    /**
     * Instantiates any object using an Dom Element and constructor arguments. Sub-param tags are
     * used on set-methods on the newly created object.
     * @param classElement a 'class' element with a 'name' attribute,
     *        or any element with a 'class' attribute. Finally if this any element has no 'class' attribute, it will try
     *        'name' too. If it hasn't either, it will try the body.
     * @param args Constructor arguments.
     * @throws NoSuchMethodError If not matching constructor could be found
     * @throws ClassNotFoundException If the specified class does not exist.
     * @return A newly created object. Never <code>null</code>.
     * @since MMBase-1.9.6
     */
    public static Object getInstance(final Class claz, Element classElement, Object... args)
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        List<Class> argTypes = new ArrayList<Class>(args.length);
        for (Object arg : args) {
            argTypes.add(arg.getClass());
        }
        Class[] argTypesArray = argTypes.toArray(new Class[] {});
        Constructor constructor = null;
        for (Constructor c : claz.getConstructors()) {
            Class[] parameterTypes = c.getParameterTypes();
            if (parameterTypes.length != argTypesArray.length) continue;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (! parameterTypes[i].isAssignableFrom(argTypesArray[i])) continue;
            }
            constructor = c;
            break;
        }
        if (constructor == null) {
            throw new NoSuchMethodError("No constructors found for " + args);
        }
        log.debug("Found constructor " + constructor);

        Object o = constructor.newInstance(args);

        NodeList params = classElement.getChildNodes();
        for (int i = 0 ; i < params.getLength(); i++) {
            try {
                Node node = params.item(i);
                if (node instanceof Element && node.getNodeName().equals("param")) {
                    Element param = (Element)node;
                    String name = param.getAttribute("name");
                    String value = org.mmbase.util.xml.DocumentReader.getNodeTextValue(param);
                    setProperty(name, claz, o, value);

                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return o;
    }

    /**
     * Given the name of a property, a clazz, an object and a value, set the property on the
     * object. The point of this method is that it converts the name of the property to an actual
     * method name (set&lt;Name of the property&gt;). The methods needs to have precisely
     * <em>one</em> parameter. The type of this parameter may be anything as long as it can be
     * converted to and from String by {@link org.mmbase.util.Casting#toType(Class, Object)}.
     *
     * @param value The value as a <code>String</code>
     */
    public static void setProperty(final String name, final Class claz, final Object o, final String value) {
        String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        boolean invoked = false;
        Method setProperty = null;
        for (Method m : claz.getMethods()) {
            try {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == 1) {
                    if (invoked) {
                        log.warn("Found multiple matches for setter " + name + " on " + claz);
                    }
                    m.invoke(o, Casting.toType(m.getParameterTypes()[0], value));
                    invoked = true;
                }
                if (m.getName().equals("setProperty") && m.getParameterTypes().length == 2 && String.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    setProperty = m;
                }
            } catch (IllegalAccessException ie) {
                log.warn(ie);
            } catch (InvocationTargetException ite) {
                invoked = true;
                log.warn(ite.getMessage(), ite);
            }
        }
        if (! invoked && setProperty != null) {
            try {
                invoked = true;
                setProperty.invoke(o, name, Casting.toType(setProperty.getParameterTypes()[1], value));
            } catch (IllegalAccessException ie) {
                log.warn(ie);
            } catch (InvocationTargetException ite) {
                log.warn(ite);
            }
        }
        if (! invoked) {
            log.warn("Could not set property '" + name + "' on " + o.getClass().getName() + " " + o);
        }
    }

    /**
     * Instantiates on object with the first child with name 'class'. So, this searches the child, and then
     * calls {@link #getInstance(Element, Object...)}.
     * @param element Element in which a child specifying a java object must be searched.
     * @return a new object, or <code>null</code> if no matching child found.
     */
    public static Object getInstanceWithSubElement(Element element, Object... args)
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        NodeList childs =  element.getChildNodes();
        Object instance = null;
        for (int i = 0; i < childs.getLength(); i++) {
            Node node = childs.item(i);
            if (node instanceof Element && node.getNodeName().equals("class")) {
                instance =  getInstance((Element) node, args);
                break;
            }
        }
        return instance;
    }



}
