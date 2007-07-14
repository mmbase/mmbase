/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.util.*;
import org.w3c.dom.*;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.logging.*;

/**
 * A component is a piece of pluggable functionality that typically has dependencies on other
 * components, and may be requested several blocks.
 *
 * @author Michiel Meeuwissen
 * @version $Id: BasicComponent.java,v 1.29 2007-07-14 17:19:42 michiel Exp $
 * @since MMBase-1.9
 */
public class BasicComponent implements Component {
    private static final Logger log = Logging.getLoggerInstance(BasicComponent.class);

    private final String name;
    private ResourceBundle bundle;
    private final LocalizedString description;
    private final Map<String, Block> blocks = new HashMap<String, Block>();
    private Block defaultBlock = null;
    private String uri;
    private int version = -1;


    public BasicComponent(String name) {
        this.name = name;
        this.description = new LocalizedString(name);
    }

    public String getName() {
        return name;
    }
    public String getUri() {
        return uri;
    }
    public int getVersion() {
        return version;
    }
    

    public LocalizedString getDescription() {
        return description;
    }

    public void configure(Element el) {
        uri = el.getOwnerDocument().getDocumentURI();
        log.debug("Configuring " + this);
        description.fillFromXml("description", el);

        version = Integer.parseInt(el.getAttribute("version"));

        NodeList bundleElements = el.getElementsByTagName("bundle");
        for (int i = 0; i < bundleElements.getLength(); i++) {
            Element element = (Element) bundleElements.item(i);
            bundle = ResourceBundle.getBundle(element.getAttribute("name"));
        }
        if (bundle == null) {
            log.service("No resource bundle defined for block " + name + ". Using empty one.");
            bundle = new ResourceBundle() {
                    public Object handleGetObject(String key) {
                        return null;
                    }
                    public Enumeration<String> getKeys() {
                        Set<String> empty = Collections.emptySet();
                        return Collections.enumeration(empty);
                    }
                };
        }

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
            log.service("Default block: " + defaultBlock);
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
                log.error(e);
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

    public ResourceBundle getBundle() {
        return bundle;
    }
}
