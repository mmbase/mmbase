/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework.basic;
import org.mmbase.framework.*;
import java.util.*;
import java.io.*;
import java.net.URI;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.util.*;
import org.mmbase.util.xml.Instantiator;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.Cloud;
import org.mmbase.core.util.SystemProperties;
import org.mmbase.util.functions.*;
import org.mmbase.util.transformers.Url;
import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The Basic Framework is based on a list of {@link UrlConverter}s. It is
 * configured in 'config/framework.xml'. The order in which the UrlConverters
 * are configured is the order in which they are processed.
 *
 * @author Michiel Meeuwissen
 * @version $Id: BasicFramework.java,v 1.20 2008-07-31 09:42:32 michiel Exp $
 * @since MMBase-1.9
 */
public class BasicFramework extends Framework {
    private static final Logger log = Logging.getLoggerInstance(BasicFramework.class);

    private static final CharTransformer paramEscaper = new Url(Url.ESCAPE);

    public static final String XSD = "basicframework.xsd";
    public static final String NAMESPACE = "http://www.mmbase.org/xmlns/basicframework";

    static {
        XMLEntityResolver.registerSystemID(NAMESPACE + ".xsd", XSD, Framework.class);
    }


    public static final Parameter<String> ACTION     = new Parameter<String>("action", String.class);

    protected final ChainedUrlConverter urlConverter = new ChainedUrlConverter();
    protected final UrlConverter fallbackConverter   = new BasicUrlConverter(this);

    protected final LocalizedString description      = new LocalizedString("description");


    protected final Map<Setting<?>, Object> settingValues = new HashMap<Setting<?>, Object>();

    public BasicFramework(Element el) {
        configure(el);
    }
    public BasicFramework() {

    }


    public String getUrl(String path, Map<String, Object> parameters,
                         Parameters frameworkParameters, boolean escapeAmps) throws FrameworkException {
        String url = urlConverter.getUrl(path, parameters, frameworkParameters, escapeAmps);
        if (url == null) {
            return fallbackConverter.getUrl(path, parameters, frameworkParameters, escapeAmps);
        } else {
            return url;
        }
    }


    public String getProcessUrl(String path, Map<String, Object> parameters,
                               Parameters frameworkParameters, boolean escapeAmps) throws FrameworkException {
        HttpServletRequest request = BasicUrlConverter.getUserRequest(frameworkParameters.get(Parameter.REQUEST));
        State state = State.getState(request);
        frameworkParameters.set(ACTION, state.getId());
        String url = urlConverter.getUrl(path, parameters, frameworkParameters, escapeAmps);
        if (url == null) {
            return fallbackConverter.getUrl(path, parameters, frameworkParameters, escapeAmps);
        } else {
            return url;
        }
    }

    public String getInternalUrl(String page, Map<String, Object> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("calling urlConverter " + urlConverter);
        }
        return urlConverter.getInternalUrl(page, params, frameworkParameters);
    }

    public String getName() {
        return "BASIC";
    }



    /**
     * Configures the framework by reading its config file 'config/framework.xml'
     * containing a list with UrlConverters.
     */
    protected void configure(Element el) {
        log.info("Configuring " + getClass() + " with " + el.getOwnerDocument().getDocumentURI());
        try {
            description.fillFromXml("description", el);

            NodeList urlconverters = el.getElementsByTagName("urlconverter");
            for (int i = 0; i < urlconverters.getLength(); i++) {
                Element element = (Element) urlconverters.item(i);
                UrlConverter uc;
                try {
                    uc = (UrlConverter) Instantiator.getInstance(element, (Framework) this);
                } catch (NoSuchMethodException nsme) {
                    uc = (UrlConverter) Instantiator.getInstance(element);
                } catch (ClassNotFoundException cnfe) {
                    log.warn(cnfe);
                    continue;
                }
                urlConverter.add(uc);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        /*
        BasicUrlConverter buc = new BasicUrlConverter(this);
        if (! urlConverter.contains(buc)) {
            urlConverter.add(buc);
        }
        */
        log.info("Configured BasicFramework: " + this);

    }
    public Block getRenderingBlock(Parameters frameworkParameters) {
        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);
        State state = State.getState(request);
        return state.getBlock();
    }


    public Block getBlock(Parameters frameworkParameters) {
        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);
        State state = State.getState(request);
        log.debug("Getting block for " + frameworkParameters + " -> " + state);

        // BasicFramework always shows only one component
        Component component  = ComponentRepository.getInstance().getComponent(frameworkParameters.get(MMBaseUrlConverter.COMPONENT));
        boolean explicitComponent = component != null;
        if (state != null && state.isRendering()) {
            component = state.getBlock().getComponent();
        } else {
            log.debug("No state object found");
        }

        if (component == null || !component.getName().equals("mynews")) {
            log.debug("Not currently rendering mynews component");
            return null;
        } else {
            // can explicitely state new block by either 'path' (of mm:url) or framework parameter  'block'.
            boolean filteredMode =
                (state == null && explicitComponent) ||
                request.getServletPath().startsWith("/magazine");

            log.debug("Using " + component);

            Block block = null;
            String blockParam = frameworkParameters.get(MMBaseUrlConverter.BLOCK);
            if (blockParam != null) {
                block = component.getBlock(blockParam);
            }
            /*
            else {
                block = component.getBlock(path);
                if (block == null && path != null && ! "".equals(path)) {
                    log.debug("No block '" + path + "' found");
                    return null;
                }

            }
            */
            if (block == null && state != null) {
                block = state.getRenderer().getBlock();
            }

            if (block == null) {
                log.debug("Cannot determin a block for '" + state + "' suppose it a normal link");
                if (filteredMode) {
                    return null;
                } else {
                    // throw new IllegalArgumentException("not such block '" + + " for component " + block);
                }
            }
            return block;
        }
        //return null;
    }


    /**
     */
    public Parameter[] getParameterDefinition() {
        return new Parameter[] { ACTION, Parameter.REQUEST, new Parameter.Wrapper(urlConverter.getParameterDefinition())};
    }

    public Parameters createParameters() {
        return new Parameters(getParameterDefinition());
    }

    public boolean makeRelativeUrl() {
        return false;
    }


    protected void setBlockParametersForRender(State state, Parameters blockParameters) {
        for (Map.Entry<String, ?> entry : blockParameters.toMap().entrySet()) {
            if (entry.getValue() == null) {
                log.debug("Using " + entry + " and parameter " + getPrefix(state) + entry.getKey());
                blockParameters.set(entry.getKey(), state.getRequest().getParameter(getPrefix(state) + entry.getKey()));
            }
        }
    }

     protected void setBlockParametersForProcess(State state, Parameters blockParameters) {
        ServletRequest request = state.getRequest();
        for (Map.Entry<String, ?> entry : blockParameters.toMap().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Basic Framework implicitely also processes, i'm not sure if we should require any framework
     * to do that (perhaps we could say, that the render method must process, if that is necessary,
     * and not yet done).
     */
    public void render(Renderer renderer, Parameters blockParameters, Parameters frameworkParameters, Writer w, Renderer.WindowState windowState) throws FrameworkException {
        ServletRequest request = frameworkParameters.get(Parameter.REQUEST);
        if (request == null) throw new IllegalArgumentException("No request object given");
        State state = State.getState(request);

        try {

            request.setAttribute(COMPONENT_CLASS_KEY, "mm_fw_basic");

            Renderer actualRenderer = state.startBlock(frameworkParameters, renderer);
            if (! actualRenderer.equals(renderer)) {
                Parameters newBlockParameters = actualRenderer.getBlock().createParameters();
                newBlockParameters.setAllIfDefinied(blockParameters);
                blockParameters = newBlockParameters;

            }
            state.setAction(request.getParameter(ACTION.getName()));
            if (state.needsProcess()) {
                log.service("Performing action on " + actualRenderer.getBlock());
                Processor processor = actualRenderer.getBlock().getProcessor();
                state.process(processor);
                log.service("Processing " + actualRenderer.getBlock() + " " + processor);
                setBlockParametersForProcess(state, blockParameters);
                processor.process(blockParameters, frameworkParameters);
            }

            state.render(actualRenderer);
            setBlockParametersForRender(state, blockParameters);
            actualRenderer.render(blockParameters, frameworkParameters, w, windowState);
        } catch (FrameworkException fe) {
            URI uri = renderer.getUri();
            Renderer error = new ErrorRenderer(renderer.getType(), renderer.getBlock(), (uri != null) ? uri.toString() : null, 500, fe);
            error.render(blockParameters, frameworkParameters, w, windowState);
        } finally {
            state.endBlock();
        }
    }

    /**
     * Think in the basic framework this method is never called explicitely, because processing is
     * done implicitely by the render
     */
    public void process(Processor processor, Parameters blockParameters, Parameters frameworkParameters) throws FrameworkException {
        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);
        State state = State.getState(request);
        state.startBlock(frameworkParameters, null);
        setBlockParametersForProcess(state, blockParameters);
        processor.process(blockParameters, frameworkParameters);
    }

    public Node getUserNode(Parameters frameworkParameters) {
        Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
        return cloud.getCloudContext().getAuthentication().getNode(cloud.getUser());
    }

    public String getUserBuilder() {
        return org.mmbase.module.core.MMBase.getMMBase().getMMBaseCop().getAuthentication().getUserBuilder();
    }

    public Map<String, Object> prefix(State state, Map<String, Object> params) {
        return getMap(state, params);
    }

    protected String getPrefix(final State state) {
        //return "_" + renderer.getBlock().getComponent().getName() + "_" +
        //renderer.getBlock().getName() + "_" + count + "_";
        return state.getId() + ":";
    }
    protected Map<String, Object> getMap(final State state, final Map<String, Object> params) {
        return new AbstractMap<String, Object>() {
            public Set<Map.Entry<String, Object>> entrySet() {
                return new AbstractSet<Map.Entry<String, Object>>() {
                    public int size() { return params.size(); }
                    public Iterator<Map.Entry<String, Object>> iterator() {
                        return new Iterator<Map.Entry<String, Object>>() {
                            private Iterator<Map.Entry<String, Object>> i = params.entrySet().iterator();
                            public boolean hasNext() { return i.hasNext(); };
                            public Map.Entry<String, Object> next() {
                                Map.Entry<String, Object> e = i.next();
                                return new org.mmbase.util.Entry<String, Object>(getPrefix(state) + e.getKey(), e.getValue());
                                }
                            public void remove() { throw new UnsupportedOperationException(); }

                        };
                    }
                };
            }
        };
    }

    public String toString() {
        return getName() + ": " + description + ": " + urlConverter.toString();
    }

    private static final Parameter<Boolean> USE_REQ = new Parameter<Boolean>("usesession", Boolean.class, Boolean.TRUE);
    public Parameters createSettingValueParameters() {
        return new Parameters(Parameter.REQUEST, Parameter.CLOUD, USE_REQ);
    }

    protected String getKey(Setting<?> setting) {
        return "org.mmbase.framework." + setting.getComponent().getName() + "." + setting.getName();
    }

    public <C> C getSettingValue(Setting<C> setting, Parameters parameters) {
        boolean useSession = parameters != null && parameters.get(USE_REQ);
        if (useSession) {
            HttpServletRequest req = parameters.get(Parameter.REQUEST);
            Object v = req.getSession(true).getAttribute(getKey(setting));
            if (v != null) {
                return setting.getDataType().cast(v, null, null);
            }
        }
        if (settingValues.containsKey(setting)) {
            return (C) settingValues.get(setting);
        } else {
            C settingValue = loadSettingValue(setting);
            if (settingValue != null) {
                settingValues.put(setting, settingValue);
                return settingValue;
            }
            return setting.getDataType().getDefaultValue();
        }
    }

    public <C> C setSettingValue(Setting<C> setting, Parameters parameters, C value) {
        if (parameters == null) throw new SecurityException("You should provide Cloud and request parameters");
        boolean useSession = parameters.get(USE_REQ);
        if (useSession) {
            C ret = getSettingValue(setting, parameters);
            HttpServletRequest req = parameters.get(Parameter.REQUEST);
            req.getSession(true).setAttribute(getKey(setting), value);
            return ret;
        } else {
            Cloud cloud = parameters.get(Parameter.CLOUD);
            if (cloud.getUser().getRank() == org.mmbase.security.Rank.ADMIN) {
                saveSettingValue(setting, value);
                return (C) settingValues.put(setting, value);
            } else {
                throw new SecurityException("Permission denied");
            }
        }
    }

    public <C> C loadSettingValue(Setting<C> setting) {
        String v = SystemProperties.getComponentProperty(setting.getComponent().getName(), setting.getName());
        if (v != null) {
            return setting.getDataType().cast(v, null, null);
        }
        return null;
    }

    public <C> void saveSettingValue(Setting<C> setting, C value) {
        SystemProperties.setComponentProperty(setting.getComponent().getName(), setting.getName(), value.toString());
    }
}
