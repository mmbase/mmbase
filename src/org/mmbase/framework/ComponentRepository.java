/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.mmbase.util.*;


import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * The class maintains all compoments which are registered in the current MMBase.
 *
 * @author Michiel Meeuwissen
 * @version $Id: ComponentRepository.java,v 1.11 2006-11-11 21:32:10 michiel Exp $
 * @since MMBase-1.9
 */
public class ComponentRepository {

    public static final String XSD_COMPONENT = "component.xsd";
    public static final String NAMESPACE = "http://www.mmbase.org/xmlns/component";
    static {
        XMLEntityResolver.registerSystemID(NAMESPACE + ".xsd", XSD_COMPONENT, ComponentRepository.class);
    }

    private static final Logger log = Logging.getLoggerInstance(ComponentRepository.class);

    private static final ComponentRepository repository = new ComponentRepository();

    public static ComponentRepository getInstance() {
        return repository;
    }

    private Map<String, Component> rep = new HashMap<String, Component>();

    private ComponentRepository() {
        ResourceWatcher rw = new ResourceWatcher() {
                public void onChange(String r) {
                    readConfiguration(r);
                }
            };
        rw.add("components");
        rw.onChange();
        rw.setDelay(2 * 1000); // 2 s
        rw.start();

    }

    public Block.Type[] getBlockClassification(String id) {
        if (id == null) {
            return new Block.Type[] {Block.Type.ROOT};
        } else {
            return Block.Type.getClassification(id, false);
        }
    }

    public Collection<Component> getComponents() {
        return Collections.unmodifiableCollection(rep.values());
    }

    public Component getComponent(String name) {
        return rep.get(name);
    }

    public void shutdown() {
        clear();
    }
    protected void clear() {
        Block.Type.ROOT.subs.clear();
        Block.Type.ROOT.blocks.clear();
        Block.Type.NO.subs.clear();
        Block.Type.NO.blocks.clear();
        rep.clear();
    }

    protected void readConfiguration(String child) {
        clear();
        ResourceLoader loader =  ResourceLoader.getConfigurationRoot().getChildResourceLoader(child);
        Collection<String> components = loader.getResourcePaths(ResourceLoader.XML_PATTERN, true /* recursive*/);
        log.info("In " + loader + " the following components XML's were found " + components);
        for (String file : components) {
            try {
                Document doc = loader.getDocument(file, true, getClass());
                String name = doc.getDocumentElement().getAttribute("name");
                String fileName = ResourceLoader.getName(file);
                if (! fileName.equals(name)) {
                    log.warn("Component " + name + " is defined in resource with name " + file);
                } else {
                    log.service("Instantatiating component '" + name + "'");
                }
                rep.put(name, getComponent(name, doc));
            } catch (Exception e) {
                log.error("For " + loader.getResource(file) + ": " + e.getMessage(), e);
            }
        }
        log.info("Found the following components " + getComponents());

    }

    /**
     * Instantaties any object using an Dom Element and constructor arguments. Sub-param tags are
     * used on set-methods on the newly created object.
     */
    public static Object getInstance(Element classElement, Object... args) 
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        String className = classElement.getAttribute("name"); 
        Class claz = Class.forName(className);
        List<Class> argTypes = new ArrayList<Class>(args.length);
        for (Object arg : args) {
            argTypes.add(arg.getClass());
        }
        Class[] argTypesArray = argTypes.toArray(new Class[] {});
        Constructor constructor = claz.getConstructor(argTypesArray);
        Object o = constructor.newInstance(args);

        NodeList params = classElement.getElementsByTagName("param");
        for (int i = 0 ; i < params.getLength(); i++) {
            Element param = (Element) params.item(i);
            String name = param.getAttribute("name");
            String value = org.mmbase.util.xml.DocumentReader.getNodeTextValue(param);
            Method method = claz.getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), String.class);
            method.invoke(o, value);
        }
        return o;
    }


    protected Component getComponent(String name, Document doc) throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        Element classElement = (Element) doc.getDocumentElement().getElementsByTagName("class").item(0);
        Component component;
        if (classElement == null) {
            component = new BasicComponent(name);
        } else {
            component = (Component) getInstance(classElement, name);
        }
        component.configure(doc.getDocumentElement()); 
        return component;
    }

}
