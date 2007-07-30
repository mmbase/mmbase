/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import org.w3c.dom.*;
import java.net.URI;
import org.mmbase.security.*;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.logging.*;

/**
 * A component is a piece of pluggable functionality that typically has dependencies on other
 * components, and may be requested several blocks.
 *
 * @author Michiel Meeuwissen
 * @version $Id: BasicComponent.java,v 1.36 2007-07-30 23:01:42 michiel Exp $
 * @since MMBase-1.9
 */
public class BasicComponent implements Component {
    private static final Logger log = Logging.getLoggerInstance(BasicComponent.class);


    private final String name;
    private String bundle;
    private final LocalizedString description;
    private final Map<String, Block> blocks = new HashMap<String, Block>();
    private final Map<String, Setting<?>> settings = new HashMap<String, Setting<?>>();
    private Block defaultBlock = null;
    private URI uri;
    private int version = -1;

    protected final Collection<Component> dependencies = new HashSet<Component>();
    protected final Collection<VirtualComponent> unsatisfied  = new HashSet<VirtualComponent>();


    public BasicComponent(String name) {
        this.name = name;
        this.description = new LocalizedString(name);
    }

    public String getName() {
        return name;
    }
    public URI getUri() {
        return uri;
    }
    public int getVersion() {
        return version;
    }


    public LocalizedString getDescription() {
        return description;
    }

    public void configure(Element el) {
        try {
            uri = new URI(el.getOwnerDocument().getDocumentURI());
        } catch (Exception e) {
            log.error(e);
        }
        log.debug("Configuring " + this);
        description.fillFromXml("description", el);

        version = Integer.parseInt(el.getAttribute("version"));

        {
            NodeList depElements = el.getElementsByTagName("dependency");
            for (int i = 0; i < depElements.getLength(); i++) {
                Element element = (Element) depElements.item(i);
                String name = element.getAttribute("component");
                int version = Integer.parseInt(element.getAttribute("version"));
                Component comp = ComponentRepository.getInstance().getComponent(name);
                if (comp != null && comp.getVersion() >= version) {
                    dependencies.add(comp);
                } else {
                    unsatisfied.add(new VirtualComponent(name, version));

                }
            }
        }
        {
            NodeList bundleElements = el.getElementsByTagName("bundle");
            if(bundleElements.getLength() > 0) {
                bundle = ((Element) bundleElements.item(0)).getAttribute("name");
            }
        }

        {
            NodeList settingElements = el.getElementsByTagName("setting");
            for (int i = 0; i < settingElements.getLength(); i++) {
                Element element = (Element) settingElements.item(i);
                Setting s = new Setting(this, element);
                settings.put(s.getName(), s);
            }
        }
        {
            NodeList actionElements = el.getElementsByTagName("action");
            for (int i = 0; i < actionElements.getLength(); i++) {
                try {
                    Element element = (Element) actionElements.item(i);
                    String name = element.getAttribute("name");
                    String rank = element.getAttribute("rank");
                    Object c = ComponentRepository.getInstanceWithSubElement(element);
                    Action a;
                    if (c != null) {
                        if (! "".equals(rank)) {
                            log.warn("Rank attribute ignored");
                        }
                        a = new Action(name, (ActionChecker) c);
                    } else {
                        if ("".equals(rank)) { rank = "basic user"; }
                        a = new Action(name, new ActionChecker.Rank(Rank.getRank(rank)));
                    }
                    a.getDescription().fillFromXml("description", element);
                    log.service("Registering action " + a);
                    ActionRepository.getInstance().add(a);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        {
            NodeList blockElements = el.getElementsByTagName("block");
            if (log.isDebugEnabled()) {
                log.debug("Found description: " + description);
                log.debug("Found number of blocks: " + blockElements.getLength());
            }
            for (int i = 0 ; i < blockElements.getLength(); i++) {
                Element element = (Element) blockElements.item(i);
                String name = element.getAttribute("name");
                String mimetype = element.getAttribute("mimetype");
                Block.Type[] classification = Block.Type.getClassification(element.getAttribute("classification"), true);
                Block b = new Block(name, mimetype, this, classification);
                b.getDescription().fillFromXml("description", element);
                log.trace("Found block: " + name);
                b.getRenderers().put(Renderer.Type.HEAD, getRenderer("head", element, b));
                b.getRenderers().put(Renderer.Type.BODY, getRenderer("body", element, b));
                b.processor = getProcessor("process", element, b);
                if (defaultBlock == null) defaultBlock = b;
                blocks.put(name, b);
            }
        }

        String defaultBlockName = el.getAttribute("defaultblock");
        if (defaultBlockName != null && ! defaultBlockName.equals("")) {
            Block b = blocks.get(defaultBlockName);
            if (b == null) {
                log.error("There is no block '" + defaultBlockName + "' so, cannot take it as default. Taking " + defaultBlock + " in stead");
            } else {
                defaultBlock = b;
            }
        }
        if (defaultBlock == null) {
            log.warn("No blocks found.");
        } else {
            log.debug("Default block: " + defaultBlock);
        }
    }

    private Renderer getRenderer(String name, Element block, Block b) {
        NodeList renderElements = block.getElementsByTagName(name);
        log.debug("Number of [" + name + "] elements: " + renderElements.getLength());
        if (renderElements.getLength() < 1) return null;
        Element renderElement = (Element) renderElements.item(0);
        String jsp = renderElement.getAttribute("jsp");
        Renderer renderer;
        if (!"".equals(jsp)) {
            renderer = new JspRenderer(name.toUpperCase(), jsp, b);
        } else {
            try {
                renderer = (Renderer) ComponentRepository.getInstanceWithSubElement(renderElement, name.toUpperCase(), b);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
        Parameter[] params = Parameter.readArrayFromXml(renderElement);
        if (params.length > 0) { // a bit to simple, how can you explicitely make a renderer parameter-less now?
            b.addParameters(params);
        }
        return renderer;
    }

    private Processor getProcessor(String name, Element block, Block b) {
        NodeList processorElements = block.getElementsByTagName(name);
        if (processorElements.getLength() < 1) return null;
        Element processorElement = (Element) processorElements.item(0);
        String jsp = processorElement.getAttribute("jsp");
        Processor processor;
        if (!"".equals(jsp)) {
            processor = new JspProcessor(jsp, b);
        } else {
            try {
                processor = (Processor) ComponentRepository.getInstanceWithSubElement(processorElement, name.toUpperCase(), b);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        Parameter[] params = Parameter.readArrayFromXml(processorElement);
        if (params.length > 0) { // a bit to simple, how can you explicitely make a processor parameter-less now?
            b.addParameters(params);
        }
        return processor;

    }

    public Collection<Block> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }
    public Block getBlock(String name) {
        if (name == null) return getDefaultBlock();
        return blocks.get(name);
    }
    public Block getDefaultBlock() {
        return defaultBlock;
    }

    public String toString() {
        return getName();
    }

    public String getBundle() {
        return bundle;
    }

    public Collection<Setting<?>> getSettings() {
        return settings.values();
    }

    public Setting<?> getSetting(String name) {
        return settings.get(name);
    }

    public Collection<Component> getDependencies() {
        return Collections.unmodifiableCollection(dependencies);
    }

    public Collection<VirtualComponent> getUnsatisfiedDependencies() {
        return Collections.unmodifiableCollection(unsatisfied);
    }

    public void resolve(VirtualComponent unsat, Component comp) {
        unsatisfied.remove(unsat);
        dependencies.add(comp);
    }
}
