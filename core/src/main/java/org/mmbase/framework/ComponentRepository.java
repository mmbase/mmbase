/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import java.net.URL;
import org.w3c.dom.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.Instantiator;



import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This (singleton) class maintains all {@link Component}s which are registered in the current MMBase.
 * Components can be configured by placing their configuration in 'config/components/'.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9
 */
public class ComponentRepository {

    public static final String XSD_COMPONENT = "component.xsd";
    public static final String NAMESPACE_COMPONENT = "http://www.mmbase.org/xmlns/component";

    public static final String XSD_BLOCKTYPES = "blocktypes.xsd";
    public static final String NAMESPACE_BLOCKTYPES = "http://www.mmbase.org/xmlns/blocktypes";
    private static final Set<String> RECOGNIZED_NAMESPACES = new HashSet<String>();

    static {
        org.mmbase.util.xml.EntityResolver.registerSystemID(NAMESPACE_COMPONENT + ".xsd", XSD_COMPONENT, ComponentRepository.class);
        org.mmbase.util.xml.EntityResolver.registerSystemID(NAMESPACE_BLOCKTYPES + ".xsd", XSD_BLOCKTYPES, ComponentRepository.class);
        RECOGNIZED_NAMESPACES.addAll(Arrays.asList(NAMESPACE_COMPONENT, NAMESPACE_BLOCKTYPES));
    }

    private static final Logger log = Logging.getLoggerInstance(ComponentRepository.class);

    static final ComponentRepository repository = new ComponentRepository();


    /**
     * Returns the (ready configured) instance of the repository
     */
    public static ComponentRepository getInstance() {

        if (Thread.currentThread() == repository.configuringThread) {
            return repository;
        }
        try {
            synchronized(repository) {
                while (! repository.isConfigured()) {
                    repository.wait();
                }
            }
        } catch (InterruptedException ie) {
            log.info(ie);
        }
        return repository;
    }


    /**
     * @since MMBase-1.9.3
     */
    public static class Ready extends org.mmbase.core.event.SystemEvent {
    }

    static {
        synchronized(repository) {
            ResourceWatcher rw = new ResourceWatcher() {
                    public void onChange(String r) {
                        repository.readConfiguration(r);
                        Framework.framework = null; // invalidate also the framework configuration,
                        // because e.g. UrlConverters may have referrences to components
                    }
                };
            rw.add("components");
            rw.onChange();
            rw.setDelay(ResourceWatcher.getDefaultResourceWatcherDelay());
            rw.start();
        }
    }

    private final Map<String, Component> rep = new TreeMap<String, Component>();
    private final List<Component> failed     = new ArrayList<Component>();

    private boolean isConfigured = false;
    private Thread configuringThread = null;

    private ComponentRepository() { }

    /**
     * Converts a comma seperated list of blocks to an array of {@link Block.Type}s. Possible
     * 'weights' per block are ignored.
     */
    public Block.Type[] getBlockClassification(String id) {
        if (id == null) {
            return new Block.Type[] {Block.Type.ROOT};
        } else {
            return Block.Type.getClassification(id, false);
        }
    }

    /**
     * The available components.
     */
    public Collection<Component> getComponents() {
        return Collections.unmodifiableCollection(rep.values());
    }

    /**
     * The components which could not be instantiated or configured, due to some
     * misconfiguration.
     * @todo failed collection seems to be unused, so this wil always return an empty set.
     */
    public Collection<Component> getFailedComponents() {
        return Collections.unmodifiableCollection(failed);
    }

    /**
     * An (unmodifiable) map representing the complete repository
     */
    public Map<String, Component> toMap()  {
        return Collections.unmodifiableMap(rep);
    }

    /**
     * Acquires the component with given name, or <code>null</code> if no such component.
     */
    public Component getComponent(String name) {
        return rep.get(name);
    }
    /**
     * Returns a certain block for a certain component. Much like {@link
     * #getComponent(String)}.{@link Component#getBlock(String)}.
     * @throws IllegalArgumentException if no component with given name.
     * @return a Block or <code>null</code> if given component has no such block.
     */
    public Block getBlock(String componentName, String blockName) {
        Component component = getComponent(componentName);
        if (component == null) throw new IllegalArgumentException("No component with name '" + componentName + "'");
        return component.getBlock(blockName);
    }

    /**
     * Returns a default block for a certain component. Much like {@link
     * #getComponent(String)}.{@link Component#getDefaultBlock()}.
     * @throws IllegalArgumentException if no component with given name.
     */
    public Block getDefaultBlock(String componentName) {
        Component component = getComponent(componentName);
        if (component == null) throw new IllegalArgumentException("No component with name '" + componentName + "'");
        return component.getDefaultBlock();
    }

    /**
     * Tries to resolve unsatisified dependencies, in all components, by calling {@link
     * Component#resolve(VirtualComponent, Component)} on all {@link
     * Component#getUnsatisfiedDependencies} of all components.
     * @return <code>true</code> if no unsatisfied dependencies remain.
     */
    protected boolean resolve() {
        int unsatisfied = 0;
        for (Component comp : getComponents()) {
            for (VirtualComponent virtual :  comp.getUnsatisfiedDependencies()) {
                Component proposed = getComponent(virtual.getName());
                if (proposed != null) {
                    if (proposed.getVersion() >= virtual.getVersion()) {
                        comp.resolve(virtual, proposed);
                    } else {
                        unsatisfied++;
                        log.warn("" + comp + " depends on " + virtual + " but the version of " + proposed + " is only " + proposed.getVersion());
                    }
                } else {
                    unsatisfied++;
                    log.warn("" + comp + " depends on " + virtual + " but no such component.");
                }
            }
        }

        return unsatisfied == 0;
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
        failed.clear();
    }

    protected boolean isConfigured() {
        return isConfigured;
    }

    /**
     */
    private void readBlockTypes(Element root) {
        NodeList blockTypes = root.getElementsByTagName("blocktype");
        for (int i = 0; i < blockTypes.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) blockTypes.item(i);
            String classification = element.getAttribute("name");
            int weight = Integer.parseInt(element.getAttribute("weight"));
            for (Block.Type t : Block.Type.getClassification(classification, true)) {
                t.setWeight(weight);
                t.getTitle().fillFromXml("title", element);
            }
        }
    }



    /**
     * Reads all component xmls
     */
    protected synchronized void readConfiguration(String child) {
        configuringThread = Thread.currentThread();
        try {
            clear();

            ResourceLoader loader =  ResourceLoader.getConfigurationRoot().getChildResourceLoader(child);
            Collection<String> components = loader.getResourcePaths(ResourceLoader.XML_PATTERN, true /* recursive*/);
            log.debug("In " + loader + " the following components XML's were found " + components);
            for (String resource : components) {
                for (URL url : loader.getResourceList(resource)) {
                    try {
                        if (url.openConnection().getDoInput()) {
                            String namespace = ResourceLoader.getDocument(url, false, null).getDocumentElement().getNamespaceURI();

                            if (namespace == null || ! RECOGNIZED_NAMESPACES.contains(namespace)) {
                                log.debug("Ignoring " + url  + " because namespace is not one of  " + RECOGNIZED_NAMESPACES + ", but " + namespace);
                                continue;
                            }
                            Document doc = ResourceLoader.getDocument(url, true, getClass());
                            Element documentElement = doc.getDocumentElement();

                            if (documentElement.getTagName().equals("component")) {
                                String name = documentElement.getAttribute("name");
                                String fileName = ResourceLoader.getName(resource);
                                if (! fileName.equals(name)) {
                                    log.warn("Component " + url + " is defined in resource with name " + resource + " but its name is '" + name + "'");
                                } else {
                                    log.service("Instantiating component '" + url + "' " + namespace);
                                }
                                if (rep.containsKey(name)) {
                                    Component org = rep.get(name);
                                    log.debug("There is already a component with name '" + name + "' (" + org.getUri() + "), " + doc.getDocumentURI() + " defines another one, which is now ignored");
                                } else {
                                    Component newComponent = getComponent(name, doc);
                                    rep.put(name, newComponent);
                                }
                            } else if (documentElement.getTagName().equals("blocktypes")) {
                                log.service("Reading block types from '" + url + "' " + namespace);
                                readBlockTypes(documentElement);
                            } else if (documentElement.getTagName().equals("head")
                                       || documentElement.getTagName().equals("body")) {
                                log.debug("Resource '" + url + "' " + documentElement.getTagName() + "' used for include");
                            } else {
                                log.warn("Resource '" + url + "' " + namespace + " and entry '" + documentElement.getTagName() + "' cannot be recognized");
                            }
                        } else {
                            log.debug("" + url + " does not exist");
                        }
                    } catch (ClassNotFoundException cnfe) {
                        log.error("For " + url + ": " + cnfe.getClass() + " " + cnfe.getMessage());
                    } catch (Throwable e) {
                        log.error("For " + url + ": " + e.getMessage(), e);
                    }

                }
            }

            if (! resolve()) {
                log.error("Not all components satisfied their dependencies");
            }
            log.info("Found the following components " + getComponents());

        } finally {
            isConfigured = true;
            configuringThread = null;
            notifyAll();
            org.mmbase.core.event.EventManager.getInstance().propagateEvent(new Ready(), true);
        }
    }



    /**
     * Given  an XML, creates and configures one component.
     */
    protected Component getComponent(String name, Document doc)
        throws org.xml.sax.SAXException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {

        Component component = (Component) Instantiator.getInstanceWithSubElement(doc.getDocumentElement(), name);
        if (component == null) {
            component = new BasicComponent(name);
        }
        component.configure(doc.getDocumentElement());
        return component;
    }
}
