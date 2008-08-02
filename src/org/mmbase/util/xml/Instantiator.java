/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import org.mmbase.util.Casting;

import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;

import org.mmbase.util.logging.*;

/**
 * Utilities to use an XML to instantiate Java objects, using
 * reflection. This is used by various configuration-read code, which
 * all perform similar tasks of instantiating and configurating
 * certain objects.
 * 
 * Supported are schemes like
 <pre><![CDATA[
  <class name="class name">
     <param name="parameter name">parameter value</param>
      ...
  </class>
  and
  <anyelement class="class name">
     <param name="parameter name">parameter value</param>
      ...
  </anyname>
  ]]>
 * Param subtags are matched with 'setters' on the created object.
 *
 *
 * @since MMBase-1.9
 * @author Michiel Meeuwissen
 * @version $Id: Instantiator.java,v 1.5 2008-08-02 15:15:00 michiel Exp $
 */
public abstract class Instantiator {

    private static final Logger log = Logging.getLoggerInstance(Instantiator.class);

    /**
     * Instantiates any object using an Dom Element and constructor arguments. Sub-param tags are
     * used on set-methods on the newly created object. 
     * @param classElement a 'class' element with a 'name' attribute,
     *        or any element with a 'class' attribute.
     * @param args Constructor arguments.
     * @throws NoSuchMethodError If not matching constructor could be found
     * @throws ClassNotFoundException If the specified class does not exist.
     * @return A newly created object. Never <code>null</code>.
     */
    public static Object getInstance(Element classElement, Object... args)
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        String className = classElement.getAttribute("name");
        if ("".equals(className)) className = classElement.getAttribute("class"); 
        Class claz = Class.forName(className);
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
        if (constructor == null) throw new NoSuchMethodError("No constructors found for " + args);
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
     * converted to from String by {@link org.mmbase.util.Casting#toType(Class, Object)}.
     *
     * @param value The value as a <code>String</code>
     */
    public static void setProperty(String name, Class claz, Object o, String value) {
        String methodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
        boolean invoked = false;
        for (Method m : claz.getMethods()) {
            try {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == 1) {
                    if (invoked) {
                        log.warn("Found multiple matches for setter " + name + " on " + claz);
                    }
                    m.invoke(o, Casting.toType(m.getParameterTypes()[0], value));
                    invoked = true;
                }
            } catch (IllegalAccessException ie) {
                log.warn(ie);
            } catch (InvocationTargetException ite) {
                log.warn(ite);
            }
        }
    }

    /**
     * Instantiates on object with the first child with name 'class'. So, this searches the child, and then
     * calls {@link #getInstance(Element, Object...)}.
     * @param element Element in which a child specifying a java object must be searched.
     * @return a new object, or <code>null</code> if no matching child found.
     */
    public static Object getInstanceWithSubElement(Element element, Object... args) throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
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
