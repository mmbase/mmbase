/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util;

// general

import java.io.*;
import java.net.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.core.event.EventManager;
import org.mmbase.core.event.SystemEvent;
import org.mmbase.storage.search.CompositeConstraint;
import org.mmbase.storage.search.Constraint;
import org.mmbase.storage.search.implementation.BasicCompositeConstraint;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.transformers.*;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.InputSource;

// used for resolving in servlet-environment
// used for resolving in MMBase database
// XML stuff
// used for Unicode Escaping when editing property files


/**
 * MMBase resource loader, for loading config-files and those kind of things. It knows about MMBase config file locations.
 *
 * I read <a href="http://www.javaworld.com/javaqa/2003-08/02-qa-0822-urls.html">http://www.javaworld.com/javaqa/2003-08/02-qa-0822-urls.html</a>.
 *
 * Programmers should do something like this if they need a configuration file:
<pre>
InputStream configStream = ResourceLoader.getConfigurationRoot().getResourceAsStream("modules/myconfiguration.xml");
</pre>
or
<pre>
InputSource config = ResourceLoader.getConfigurationRoot().getInputSource("modules/myconfiguration.xml");
</pre>
of if you need a list of all resources:
<pre>
ResourceLoader builderLoader = new ResourceLoader("builders");
List list = builderLoader.getResourcePaths(ResourceLoader.XML_PATTERN, true)
</pre>

When you want to place a configuration file then you have several options, wich are in order of preference:
<ol>
  <li>Place it as on object in 'resources' builder (if such a builder is present)</li>
  <li>Place it in the directory identified by the 'mmbase.config' setting (A system property or web.xml setting).</li>
  <li>Place it in the directory WEB-INF/config. If this is a real directory (you are not in a war), then the resource will also be returned by {@link #getFiles}.</li>
  <li>
  Place it in the class-loader path of your app-server, below the 'org.mmbase.config' package.
  For tomcat this boils down to the following list (Taken from <a href="http://jakarta.apache.org/tomcat/tomcat-5.0-doc/class-loader-howto.html">tomcat 5 class-loader howto</a>)
   <ol>
    <li>Bootstrap classes of your JVM</li>
    <li>System class loader classses</li>
    <li>/WEB-INF/classes of your web application. If this is a real directory (you are not in a war), then the resource will also be returned by {@link #getFiles}.</li>
    <li>/WEB-INF/lib/*.jar of your web application</li>
    <li>$CATALINA_HOME/common/classes</li>
     <li>$CATALINA_HOME/common/endorsed/*.jar</li>
    <li>$CATALINA_HOME/common/lib/*.jar</li>
    <li>$CATALINA_BASE/shared/classes</li>
    <li>$CATALINA_BASE/shared/lib/*.jar</li>
  </ol>
  </li>
</ol>
 * <p>
 *   Resources which do not reside in the MMBase configuration repository, can also be handled. Those can be resolved relatively to the web root, using {@link #getWebRoot()}.
 * </p>
 *
 * <p>Resources can  programmaticly created or changed by the use of {@link #createResourceAsStream}, or something like {@link #getWriter}.</p>
 *
 * <p>If you want to check beforehand if a resource can be changed, then something like <code>resourceLoader.getResource().openConnection().getDoOutput()</code> can be used.</p>
 * <p>That is also valid if you want to check for existance. <code>resourceLoader.getResource().openConnection().getDoInput()</code>.</p>
 * <p>If you want to remove a resource, you must write <code>null</code> to all URL's returned by {@link #findResources} (Do for every URL:<code>url.openConnection().getOutputStream().write(null);</code>)</p>
 * <h3>Encodings</h3>
 * <p>ResourceLoader is well aware of encodings. You can open XML's as Reader, and this will be done using the encoding specified in the XML itself. When saving an XML using a Writer, this will also be done using the encoding specified in the XML.</p>
 * <p>For property-files, the java-unicode-escaping is undone on loading, and applied on saving, so
 * there is no need to think of that.</p>
 * <h3>Configuration and weights</h3>
 * <p>The Classloader itself reads the resources <code>config/utils/resourceloader.xml</code>. This
 * is mainly to attribute weights to certain resources. See {@link #getWeight}. This is used if a
 * resource is available more than once (e.g. in several distinct jars in WEB-INF/lib), to determin
 * the order they appear in {@link #getResourceList}. E.g. an mmbase application providing a
 * security implementation would give a large weight to its own
 * <code>config/security/security.xml</code>, the ratio being that if you install a security
 * implementation, you probably want to <em>use</em> it too.</p>
 * <p>Take a look e.g. at <a href="https://scm.mmbase.org/mmbase/trunk/applications/cloudcontext/src/main/config/utils/resourceloader.xml">A
 * typical resourceloader.xml</a>. In this example every other resource in the same jar as the
 * resourceloader.xml itself (the '!'
 * when used as the first character of the key means 'replace this with the URL of this jar') is
 * attributed a relatively big weight (default it is 0).</p>

 * </p>
 * @author Michiel Meeuwissen
 * @since  MMBase-1.8
 * @version $Id$
 */
public class ResourceLoader extends ClassLoader {

    private static Logger log = Logging.getLoggerInstance(ResourceLoader.class);

    public static void initLogging() {
        Logger nlog = Logging.getLoggerInstance(ResourceLoader.class);
        if (nlog != null) {
            log = nlog;
        } else {
            log.warn("Could not get new logger instance");
        }
    }

    /**
     * Protocol prefix used by URL objects in this class.
     */
    protected static final String PROTOCOL         = "mm";

    /**
     * Used for files, and servlet resources.
     */
    protected static final String RESOURCE_ROOT    = "/WEB-INF/config";

    /**
     * Used when getting resources with normal class-loader.
     */
    protected static final String CLASSLOADER_ROOT = "/org/mmbase/config";

    /**
     * Protocol prefix used by URL objects in this class.
     */
    public static final URL NODE_URL_CONTEXT;

    static {
        URL temp = null;
        try {
            temp = new URL("http", "localhost", "/node/");
        } catch (MalformedURLException mfue) {
            assert false : mfue;
        }
        NODE_URL_CONTEXT = temp;
    }


    /**
     * Used when using getResourcePaths for normal class-loaders.
     */
    protected static final String INDEX = "INDEX";

    private static  final ResourceLoader configRoot = new ResourceLoader();
    private static  boolean        configRootNeedsInit = true;

    private static  final ResourceLoader webRoot = new ResourceLoader();
    private static  boolean        webRootNeedsInit = true;

    private static  ResourceLoader systemRoot = null;

    private static ServletContext  servletContext = null;

    // these should perhaps be configurable:
    public static final String    RESOURCENAME_FIELD  = "name";
    public static final String    TYPE_FIELD          = "type";
    public static final String    FILENAME_FIELD      = "filename";
    public static final String    HANDLE_FIELD        = "handle";
    public static final String    LASTMODIFIED_FIELD  = "lastmodified";
    public static final String    DEFAULT_CONTEXT     = "admin";

    /**
     * The available ResourceLoaders wrapped into an enum, which makes them available dynamicly
     * (with <code>ResourceLoader.Type.valueOf(string).get())</code>
     * @since MMBase-1.9
     */

    public enum Type {
        CONFIG { public ResourceLoader get() { return ResourceLoader.getConfigurationRoot(); } },
        WEB {    public ResourceLoader get() { return ResourceLoader.getWebRoot(); } },
        SYSTEM { public ResourceLoader get() { return ResourceLoader.getSystemRoot(); } };
        public abstract ResourceLoader get();
    }


    // This should perhaps be a member (too) to allow for better authorisation support.
    static String resourceBuilder = null;


    /**
     * The URLStreamHandler for 'mm' URL's.
     */

    private final MMURLStreamHandler mmStreamHandler = new MMURLStreamHandler();

    /**
     * Creates a new URL object, which is used to load resources. First a normal java.net.URL is
     * instantiated, if that fails, we check for the 'mmbase' protocol. If so, a URL is instantiated
     * with a URLStreamHandler which can handle that.
     *
     * If that too fails, it should actually already be a MalformedURLException, but we try
     * supposing it is some existing file and return a file: URL. If no such file, only then a
     * MalformedURLException is thrown.
     */
    protected  URL newURL(final String url) throws MalformedURLException {
        // Try already installed protocols first:
        try {
            return new URL (url);
        } catch (MalformedURLException ignore) {
            // Ignore: try our own handler next.
        }

        final int firstColon = url.indexOf (':');
        if (firstColon <= 0) {
            if (new File(url).exists()) return new URL("file:" + url); // try it as a simply file
            throw new MalformedURLException ("No protocol specified: " + url);
        } else {

            final String protocol = url.substring (0, firstColon);
            if (protocol.equals(PROTOCOL)) {
                return new URL (null/* no context */, url, mmStreamHandler);
            } else {
                if (new File(url).exists()) return new URL("file:" + url);
                throw new MalformedURLException ("Unknown protocol: " + protocol);
            }
        }
    }


    static {
        // make sure it works a bit before servlet-startup.
        init(null);
    }



    /**
     * Initializes the Resourceloader using a servlet-context (makes resolving relatively to WEB-INF/config possible).
     * @param sc The ServletContext used for determining the mmbase configuration directory. Or <code>null</code>.
     */
    public static  synchronized void init(ServletContext sc) {
        if (servletContext != null) {
            throw new IllegalArgumentException("ResourceLoader is already initialized with a servlet context, cannot be reinitialized with another one");
        }
        servletContext = sc;
        // reset both roots, they will be redetermined using servletContext.
        configRootNeedsInit = true;
        webRootNeedsInit    = true;
        ResourceWatcher.reinitWatchers();
        if (sc != null) {
            EventManager.getInstance().propagateEvent(new SystemEvent.ResourceLoaderChange());
        }
    }

    /**
     * Sets the MMBase builder which must be used for resource.
     * The builder must have an URL and a HANDLE field.
     * This method can be called only once.
     * @param b An String (this may be <code>null</code> if no such builder available)
     * @throws RuntimeException if builder was set already.
     */
    public static void setResourceBuilder(String b) {
        if (ResourceWatcher.resourceWatchers == null) {
            throw new RuntimeException("A resource builder was set already: " + resourceBuilder);
        }
        resourceBuilder = b;
        // must be informed to existing ResourceWatchers.
        ResourceWatcher.setResourceBuilder(); // this will also set ResourceWatcher.resourceWatchers to null.
        if (b != null) {
            log.info("The resources builder '" + b  + "' is available.");
        } else {
            log.debug("No resources builder");
        }
        EventManager.getInstance().propagateEvent(new SystemEvent.ResourceLoaderChange());
    }

    private static NodeManager resourceNodeManager;
    /**
     * @since MMBase-1.9.2
     */
    static NodeManager getResourceBuilder() {
        if (resourceBuilder == null) return null;
        if (resourceNodeManager == null) {
            Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
            resourceNodeManager = cloud.getNodeManager(ResourceLoader.resourceBuilder);
        }
        return resourceNodeManager;
    }


    /**
     * Utility method to return the name part of a resource-name (removed directory and 'extension').
     * Used e.g. when loading builders in MMBase.
     */
    public static String getName(String path) {
        //avoid NullPointerException in util method
        if (path == null) {
            return null;
        }
        int i = path.lastIndexOf('/');
        path = path.substring(i + 1);

        i = path.lastIndexOf('.');
        if (i > 0) {
            path = path.substring(0, i);
        }
        return path;
    }

    /**
     * Utility method to return the 'directory' part of a resource-name.
     * Used e.g. when loading builders in MMBase.
     */
    public static String getDirectory(String path) {
        //avoid NullPointerException in util method
        if (path == null){
            return null;
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            path = path.substring(0, i);
        } else {
            path = "";
        }
        return path;
    }

    /**
     * Utility method to return the name of the directory of a resource-name. This does not include
     * any /-chars any more.
     * @since MMBase-1.8.2
     */
    public static String getDirectoryName(String path) {
        if (path == null){
            return null;
        }
        if (path.length() > 0 && path.charAt(path.length() - 1) == '/') path = path.substring(0, path.length() - 1);

        int i = path.lastIndexOf('/');
        path = path.substring(i + 1);
        if (path.length() > 0 && path.charAt(0) == '/') path = path.substring(1);
        return path;
    }

    /**
     * Singleton that returns the ResourceLoader for loading mmbase configuration
     */
    public static synchronized ResourceLoader getConfigurationRoot() {
        if (configRootNeedsInit) {
            configRootNeedsInit = false;
            configRoot.roots.clear();

            //adds a resource that can load from nodes
            configRoot.roots.add(configRoot.new NodeURLStreamHandler(Type.CONFIG.ordinal()));

            // mmbase.config settings
            String configPath = null;
            if (servletContext != null) {
                configPath = servletContext.getInitParameter("mmbase.config");
                log.debug("found the mmbase config path parameter using the mmbase.config servlet context parameter");
            }
            if (configPath == null) {
                try {
                    configPath = System.getProperty("mmbase.config");
                } catch (SecurityException se) {
                    log.info(se.getMessage());
                }
                if (configPath != null) {
                    log.debug("found the mmbase.config path parameter using the mmbase.config system property");
                }
            } else {
                try {
                    if (System.getProperty("mmbase.config") != null){
                        //if the configPath at this point was not null and the mmbase.config system property is defined
                        //this deserves a warning message since the setting is masked
                        log.warn("mmbase.config system property is masked by mmbase.config servlet context parameter");
                    }
                } catch (SecurityException se) {
                    log.debug(se.getMessage());
                }
            }


            try {
                configRoot.roots.add(configRoot.new ApplicationContextFileURLStreamHandler());
            } catch (NoClassDefFoundError t) {
                // Never mind, we may be in RMMCI
            }

            if (configPath != null) {
                if (servletContext != null) {
                    // take into account that configpath can start at webrootdir
                    if (configPath.startsWith("$WEBROOT")) {
                        configPath = servletContext.getRealPath(configPath.substring(8));
                    }
                }
                log.debug("Adding " + configPath);
                configRoot.roots.add(configRoot.new FileURLStreamHandler(new File(configPath), true));
            }

            if (servletContext != null) {
                String s = servletContext.getRealPath(RESOURCE_ROOT);
                if (s != null) {
                    PathURLStreamHandler h = configRoot.new FileURLStreamHandler(new File(s), true);
                    if (! configRoot.roots.contains(h)) {
                        configRoot.roots.add(h);
                    }
                } else {
                    configRoot.roots.add(configRoot.new ServletResourceURLStreamHandler(RESOURCE_ROOT));
                }
            }

            if (servletContext != null) {
                String s = servletContext.getRealPath("/WEB-INF/classes" + CLASSLOADER_ROOT); // prefer opening as a files.
                if (s != null) {
                    configRoot.roots.add(configRoot.new FileURLStreamHandler(new File(s), false));
                }
            }

            configRoot.roots.add(configRoot.new ClassLoaderURLStreamHandler(CLASSLOADER_ROOT));

            //last fall back: fully qualified class-name
            configRoot.roots.add(configRoot.new ClassLoaderURLStreamHandler("/"));

        }
        return configRoot;
    }


    /**
     * Singleton that returns the ResourceLoader for loading from the file-system, relative from
     * current working directory and falling back to the file system roots. This can be used in
     * command-line tools and such. In a servlet environment you should use either
     * {@link  #getConfigurationRoot} or {@link #getWebRoot}.
     */
    public static synchronized ResourceLoader getSystemRoot() {
        if (systemRoot == null) {
            systemRoot = new ResourceLoader();
            try {
                systemRoot.roots.add(systemRoot.new FileURLStreamHandler(new File(System.getProperty("user.dir")), true));
            } catch (SecurityException se) {
                log.service(se.getMessage());
            }
            File[] roots = File.listRoots();
            for (File element : roots) {
                systemRoot.roots.add(systemRoot.new FileURLStreamHandler(element, true));
            }

        }
        return systemRoot;
    }

    /**
     * Singleton that returns the ResourceLoader for witch the base path is the web root
     */
    public static synchronized ResourceLoader getWebRoot() {
        if (webRootNeedsInit) {
            webRootNeedsInit = false;
            webRoot.roots.clear();
            //webRoot.roots.add(webRoot.new NodeURLStreamHandler(Resource.TYPE_WEB));


            String htmlRoot = null;
            if (servletContext != null) {
                htmlRoot = servletContext.getInitParameter("mmbase.htmlroot");
            }

            if (htmlRoot == null) {
                try {
                    htmlRoot = System.getProperty("mmbase.htmlroot");
                } catch (SecurityException se) {
                    log.service(se);
                }
            }
            if (htmlRoot != null) {
                webRoot.roots.add(webRoot.new FileURLStreamHandler(new File(htmlRoot), true));
            }

            if (servletContext != null) {
                String s = servletContext.getRealPath("/");
                if (s != null) {
                    webRoot.roots.add(webRoot.new FileURLStreamHandler(new File(s), true));
                }
                webRoot.roots.add(webRoot.new ServletResourceURLStreamHandler("/"));
            }
        }

        return webRoot;
    }

    /**
     * Returns the resource loader associated with the directory of the given request
     */
    public static synchronized ResourceLoader getWebDirectory(HttpServletRequest request) {
        return  ResourceLoader.getWebRoot().getChildResourceLoader(request.getServletPath()).getParentResourceLoader();
    }

    /**
     * The URL relative to which this class-loader resolves. Cannot be <code>null</code>.
     */
    private final URL context;


    /**
     * Child resourceloaders have a parent.
     */
    private final ResourceLoader parent;


    final List<PathURLStreamHandler> roots;


    /**
     * This constructor instantiates a new root resource-loader. This constructor is protected (so you may use it in an extension), but normally use:
     * {@link #getConfigurationRoot} or {@link #getWebRoot}.
     */
    protected ResourceLoader() {
        super();
        roots        = new ArrayList<PathURLStreamHandler>();
        parent       = null;
        try {
            context = newURL(PROTOCOL + ":/");
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
    }



    /**
     * Instantiates a ResourceLoader for a 'sub directory' of given ResourceLoader. Used by {@link #getChildResourceLoader}.
     */
    protected  ResourceLoader(final ResourceLoader cl, final String context)  {
        super(ResourceLoader.class.getClassLoader());
        this.context = cl.findResource(context + "/");
        roots   = new ArrayList<PathURLStreamHandler>();
        for (PathURLStreamHandler o : cl.roots) {
        // hmm, don't like this code, but don't know how else to copy the inner object.
            if (o instanceof FileURLStreamHandler) {
                roots.add(new FileURLStreamHandler((FileURLStreamHandler) o));
            } else if (o instanceof ApplicationContextFileURLStreamHandler) {
                roots.add(new ApplicationContextFileURLStreamHandler());
            } else if (o instanceof NodeURLStreamHandler) {
                roots.add(new NodeURLStreamHandler((NodeURLStreamHandler) o));
            } else if (o instanceof ServletResourceURLStreamHandler) {
                roots.add(new ServletResourceURLStreamHandler((ServletResourceURLStreamHandler) o));
            } else if (o instanceof ClassLoaderURLStreamHandler) {
                roots.add(new ClassLoaderURLStreamHandler((ClassLoaderURLStreamHandler) o));
            } else {
                assert false;
            }
        }
        parent  = cl;
    }


    @Override
    public URL getResource(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
            /*
            String p = context.getPath();
            if (p.endsWith("/")) {
                p = p.substring(0, p.length() - 1);
            }
            if (p.startsWith("/")) {
                p = p.substring(1);
            }
            name = p + name;
            */
        }
        //System.out.println("Using " + name);
        URL res = super.getResource(name);
        return res;
    }
    /**
     * If name starts with '/' or 'mm:/' the 'parent' resourceloader is used.
     *
     * Otherwise the name is resolved relatively. (For the root ResourceLoader that is the same as starting with /)
     *
     * {@inheritDoc}
     */
    @Override
    protected URL findResource(final String name) {
        try {
            if (name.startsWith("/")) {
                return newURL(PROTOCOL + ":" + name);
            } else if (name.startsWith(PROTOCOL + ":")) {
                return newURL(name);
            } else {
                return new URL(context, name);
            }
        } catch (MalformedURLException mfue) {
            log.info(mfue + Logging.stackTrace(mfue));
            return null;
        }
    }


    /**
     * {@inheritDoc}
     * @see #getResourceList
     */
    @Override
    protected Enumeration<URL> findResources(final String name) throws IOException {
        final Iterator<PathURLStreamHandler> i = roots.iterator();
        return new Enumeration<URL>() {
                private Enumeration<URL> current = null;
                private Enumeration<URL> next;
                {
                    current = getNext();
                    next = getNext();
                }

                protected Enumeration<URL> getNext() {
                    if (i.hasNext()) {
                        try {
                            PathURLStreamHandler ush = i.next();
                            Enumeration<URL> e = ush.getResources(name);
                            return e;
                        } catch (IOException io) {
                            log.warn(io);
                            return current;
                        }
                    } else {
                        return current;
                    }
                }

                public boolean hasMoreElements() {
                    return current.hasMoreElements() || next.hasMoreElements();
                }
                public URL nextElement() {
                    if (! current.hasMoreElements()) {
                        current = next;
                        next = getNext();
                    }
                    URL n = current.nextElement();
                    return n;
                }

            };
    }

    /**
     * Returns a List, containing all URLs which may represent the
     * given resource. This can be used to show what resource would be loaded and what resource
     * would be masked, or one can also simply somehow 'merge' all these resources.
     * The List is ordered, the URL's appearing earlier in the list should be considered 'more
     * important' or 'heavier'. See {@link #getWeight}.
     */
    public List<URL> getResourceList(final String name) {
        try {
            return Collections.list(getResources(name));
        } catch (IOException io) {
            log.warn(io);
            return Collections.emptyList();
        }
    }


    /**
     * Can be used as an argument for {@link #getResourcePaths(Pattern, boolean)}. MMBase works mainly
     * with xml configuration files, so this comes in handy.
     */
    public static final Pattern XML_PATTERN = Pattern.compile(".*\\.xml$");

    /**
     * Returns the 'context' for the ResourceLoader (an URL).
     */
    public URL getContext() {
        return context;
    }


    /**
     * Returns the 'parent' ResourceLoader. Or <code>null</code> if this ClassLoader has no
     * parent. You can create a ResourceLoader with a parent by {@link
     * #getChildResourceLoader(String)}.
     */
    public ResourceLoader getParentResourceLoader() {
        return parent;
    }

    /**
     *
     *
     * @param context a context relative to the current resource loader
     * @return a new 'child' ResourceLoader or the parent ResourceLoader if the context is ".."
     * @see #ResourceLoader(ResourceLoader, String)
     */
    public ResourceLoader getChildResourceLoader(final String context) {
        if (context.equals("..")) { // should be made a bit smarter, (also recognizing "../..", "/" and those kind of things).
            return getParentResourceLoader();
        }
        String [] dirs = context.split("/");
        ResourceLoader rl = this;
        for (String element : dirs) {
            rl =  new ResourceLoader(rl, element);
        }
        return rl;

    }


    /**
     * Returns a set of 'sub resources' (read: 'files in the same directory'), which can succesfully be loaded by the ResourceLoader.
     *
     * @param pattern   A Regular expression pattern to which  the file-name must match, or <code>null</code> if no restrictions apply
     * @param recursive If true, then also subdirectories are searched.
     * @return A Set of Strings which can be successfully loaded with the resourceloader.
     */
    public Set<String> getResourcePaths(final Pattern pattern, final boolean recursive) {
        return getResourcePaths(pattern, recursive, false);
    }

    /**
     * Returns a set of context strings which can be used to instantiated new ResourceLoaders (resource loaders for directories)
     * (see {@link #getChildResourceLoader(String)}).
     * @param pattern   A Regular expression pattern to which  the file-name must match, or <code>null</code> if no restrictions apply
     * @param recursive If true, then also subdirectories are searched.
     */
    public Set<String> getChildContexts(final Pattern pattern, final boolean recursive) {
        return getResourcePaths(pattern, recursive, true);
    }

    /**
     * Used by {@link #getResourcePaths(Pattern, boolean)} and {@link #getChildContexts(Pattern, boolean)}
     * @param pattern   A Regular expression pattern to which  the file-name must match, or <code>null</code> if no restrictions apply
     * @param recursive If true, then also subdirectories are searched.
     * @param directories {@link #getChildContexts(Pattern, boolean)} supplies <code>true</code> {@link #getResourcePaths(Pattern, boolean)} supplies <code>false</code>
     */
    protected Set<String> getResourcePaths(final Pattern pattern, final boolean recursive, final boolean directories) {
        Set<String> results = new TreeSet<String>(); // a set with fixed iteration order
        for (PathURLStreamHandler cf : roots) {
            cf.getPaths(results, pattern, recursive, directories);
        }
        return results;
    }



    /**
     * If you want to change a resource, or create one, then this method can be used. Specify the
     * desired resource-name and you get an OutputStream back, to which you must write.
     *
     * This is a shortcut to <code>findResource(name).openConnection().getOutputStream()</code>
     *
     * If the given resource already existed, it will be overwritten, or shadowed, if it was not
     * writeable.
     *
     * @throws IOException If the Resource for some reason could not be created.
     */
    public OutputStream createResourceAsStream(String name) throws IOException {
        if (name == null || name.equals("")) {
            throw new IOException("You cannot create a resource with an empty name");
        }
        URL resource = findResource(name);
        URLConnection connection = resource.openConnection();
        return connection.getOutputStream();
    }

    /**
     * Returns the givens resource as an InputSource (XML streams). ResourceLoader is often used for
     * XML.
     * The System ID is set, otherwise you could as wel do new InputSource(r.getResourceAsStream());
     * @param name The name of the resource to be loaded
     * @return The InputSource if succesfull, <code>null</code> otherwise.
     */
    public InputSource getInputSource(final String name)  throws IOException {
        return getInputSource(findResource(name));
    }

    /**
     * Static version of {@link #getInputSource(String)}, can e.g. be used in combination with {@link #getResourceList(String)}
     */
    public static InputSource getInputSource(final URL url) throws IOException {
        try {
            InputStream stream = url.openStream();
            if (stream == null) return null;
            InputSource is = new InputSource(stream);
            //is.setCharacterStream(new InputStreamReader(stream));
            is.setSystemId(url.toExternalForm());
            return is;
        } catch (MalformedURLException mfue) {
            log.info(mfue);
            return null;
        }
    }

    /**
     * Returns the givens resource as a Document (parsed XML). This can come in handly, because most
     * configuration is in XML.
     *
     * @param name The name of the resource to be loaded
     * @return The Document if succesful, <code>null</code> if there is no such resource.
     * @throws SAXException If the resource does not present parseable XML.
     * @throws IOException
     */
    public Document getDocument(String name) throws org.xml.sax.SAXException, IOException  {
        return getDocument(name, true, null);
    }

    /**
     * Returns the givens resource as a Document (parsed XML). This can come in handly, because most
     * configuration is in XML.
     *
     * @param name The name of the resource to be loaded
     * @param validation If <code>true</code>, validate the xml. By dtd if one of the first lines starts with &lt;!DOCTYPE, by XSD otherwise
     * @param baseClass If validation is <code>true</code>, the base class to search for the validating xsd or dtd
     * @return The Document if succesful, <code>null</code> if there is no such resource.
     * @throws SAXException If the resource does not present parseable XML.
     * @throws IOException
     */
    public Document getDocument(String name, boolean validation, Class<?> baseClass) throws org.xml.sax.SAXException, IOException  {
        return getDocument(getResource(name), validation, baseClass);
    }



    /**
     * sice MMBase-1.9
     */
    protected static boolean validateable(URL url) throws IOException {
       // determin whether this XML perhaps must be validated by DTD (specified 'DOCTYPE')
        boolean xsd = true;
        Reader r = new InputStreamReader(url.openStream());
        BufferedReader reader = new BufferedReader(r);
        String line = reader.readLine();
        int lineNumber = 0;
        while (lineNumber < 2 && line != null) {
            if (line.startsWith("<!DOCTYPE")) {
                log.debug("Using DTD to validate '" + url + "'");
                xsd = false;
            }
            line = reader.readLine();
            lineNumber++;
        }
        reader.close();
        return xsd;
    }
    /**
     * Static version of {@link #getDocument(String, boolean, Class)}, can e.g. be used in combination with {@link #getResourceList(String)}
     */
    public static Document getDocument(URL url, boolean validation, Class<?> baseClass) throws org.xml.sax.SAXException, IOException {

        InputSource source = getInputSource(url);
        if (source == null) return null;

        boolean xsd = validation;
        if (validation) {
            xsd = validateable(url);
        }

        org.xml.sax.EntityResolver resolver = new org.mmbase.util.xml.EntityResolver(validation, baseClass);
        DocumentBuilder dbuilder = org.mmbase.util.xml.DocumentReader.getDocumentBuilder(validation, xsd,
                                                                                         null/* default error handler */, resolver);
        if(dbuilder == null) throw new RuntimeException("failure retrieving document builder");
        if (log.isDebugEnabled()) log.debug("Reading " + source.getSystemId());
        try {
            Document doc = dbuilder.parse(source);
            return  doc;
        } catch (IOException ioe) { // dtd or so not found?
            if (validation) {
                log.error(ioe);
                // try without validating too.
                return getDocument(url, false, baseClass);
            } else {
                throw ioe;
            }
        }

    }

    /**
     * Creates a resource with given name for given Source.
     *
     * @see #createResourceAsStream(String)
     * @param docType Document which must be used, or <code>null</code> if none.
     */
    public void storeSource(String name, Source source, DocumentType docType) throws IOException {
        try {
            log.service("Storing source " + name + " for " + this);
            OutputStream stream = createResourceAsStream(name);
            StreamResult streamResult = new StreamResult(stream);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (docType != null) {
                serializer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
                serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
            }
            // Indenting not very nice in all xslt-engines, but well, its better then depending
            // on a real xslt or lots of code.
            serializer.transform(source, streamResult);
            stream.close();
        } catch (final TransformerException te) {
            IOException io = new IOException(te.getMessage());
            io.initCause(te);
            throw io;
        }
    }

    /**
     * Creates a resource for a given Document.
     * @param name Name of the resource.
     * @param doc  The xml document which must be stored.
     * @see #createResourceAsStream(String)
     */
    public void  storeDocument(String name, Document doc) throws IOException {
        storeSource(name, new DOMSource(doc), doc.getDoctype());
    }

    /**
     * A version of {@link #getReader(String)} which accepts the result of {@link #getResourceAsStream}.
     * @since MMBase-1.9
     */
    public  Reader getReader(InputStream is, String name) throws IOException {
        try {
            if (is == null) return null;
            if (name != null && name.endsWith(".properties")) {
                // todo \ u escapes must be escaped to decent Character's.
                return new TransformingReader(new InputStreamReader(is, "UTF-8"), new InverseCharTransformer(new UnicodeEscaper()));
            }
            byte b[] = new byte[100];
            if (is.markSupported()) {
                is.mark(101);
            }
            try {
                is.read(b, 0, 100);
                if (is.markSupported()) {
                    is.reset();
                } else {
                    is = getResourceAsStream(name);
                }
            } catch (IOException ioe) {
                is = getResourceAsStream(name);
            }


            String encoding = GenericResponseWrapper.getXMLEncoding(b);
            if (encoding != null) {
                return new InputStreamReader(is, encoding);
            }

            // all other things, default to UTF-8
            return new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            // could not happen
            return null;
        }
    }

    /**
     * Returns a reader for a given resource. This performs the tricky task of finding the encoding.
     * Resources are actually InputStreams (byte arrays), but often they are quite text-oriented
     * (like e.g. XML's or property-files), so this method may be useful.
     * A resource is supposed to be a property-file if it's name ends in ".properties", it is
     * supposed to be XML if it's content starts with &lt;?xml.
     * @see #getResourceAsStream(String)
     */
    public Reader getReader(String name) throws IOException {
        return getReader(getResourceAsStream(name), name);
    }

    /**
     * Returns a writer for a given resource, so that you can overwrite or create it. This performs
     * the tricky task of serializing to the right encoding. It supports the same tricks as {@link
     * #getReader}, but then inversed.
     * @see #getReader(String)
     * @see #createResourceAsStream(String)
     */
    public Writer getWriter(String name) throws IOException {
        OutputStream os = createResourceAsStream(name);
        try {
            if (os == null) return null;
            if (name.endsWith(".properties")) {
                // performs \ u escaping.
                return new TransformingWriter(new OutputStreamWriter(os, "UTF-8"), new UnicodeEscaper());
            }
        } catch (UnsupportedEncodingException uee) {
            log.error("uee " + uee);
        }
        return new EncodingDetectingOutputStreamWriter(os);
    }

    /**
     * Returns an abstract URL for a resource with given name, <code>findResource(name).toString()</code> would give an 'external' form.
     */
    public String toInternalForm(String name) {
        return toInternalForm(findResource(name));

    }

    public static String toInternalForm(URL u) {
        return u.getProtocol() + ":" + u.getPath();
    }

    /**
     * Used by {@link ResourceWatcher}. And by some deprecated code that wants to produce File objects.
     * @return A List of all files associated with the resource.
     */
    public List<File> getFiles(String name) {
        List<File> result = new ArrayList<File>();
        if (name.startsWith("file://")) {
            try {
                result.add(new File(new URL(name).getFile()));
                return result;
            } catch (MalformedURLException mfue) {
                log.warn(mfue);
            }
        }
        for (PathURLStreamHandler o : roots) {
            if (o instanceof AbstractFileURLStreamHandler) {
                File f = ((AbstractFileURLStreamHandler) o).getFile(name);
                if (f != null) {
                    result.add(((AbstractFileURLStreamHandler) o).getFile(name));
                }
            }
        }
        return result;

    }


    /**
     * @return A Node associated with the resource.
     *         Used by {@link ResourceWatcher}.
     */
    Node getResourceNode(String name) {
        for (PathURLStreamHandler o : roots) {
            if (o instanceof NodeURLStreamHandler) {
                return ((NodeConnection) o.openConnection(name)).getResourceNode();
            }
        }
        return null;
    }

    /**
     * Logs warning if 'newer' resources are shadowed by older ones.
     */
    void checkShadowedNewerResources(String name) {
        long lastModified = -1;
        URL  usedUrl = null;

        for (PathURLStreamHandler cf : roots) {
            URLConnection con = cf.openConnection(name);
            if (con.getDoInput()) {
                long lm = con.getLastModified();
                if (lm  > 0 && usedUrl != null  && lastModified > 0 && lm > lastModified) {
                    log.warn("File " + con.getURL() + " is newer (" + new Date(lm) + ") then " + usedUrl + "(" + new Date(lastModified) + ") but shadowed by it");
                    log.debug("Checked because " + Logging.stackTrace(15));
                }
                if (usedUrl == null && lm > 0) {
                    usedUrl = con.getURL();
                    lastModified = lm;
                }
            }
        }
    }

    /**
     * Determine wether File f is shadowed.
     * @param name Check for resource with this name
     * @param f The file to check for this resource.
     * @return The URL for the shadowing resource, or <code>null</code> if not shadowed.
     * @throws IllegalArgumentException if <code>file</code> is not a file associated with the resource with given name.
     */
    URL shadowed(File f, String name) {
        for (PathURLStreamHandler cf : roots) {
            if (cf instanceof NodeURLStreamHandler) {
                URLConnection con = cf.openConnection(name);
                if (con.getDoInput()) {
                    return con.getURL();
                }
            } else if (cf instanceof AbstractFileURLStreamHandler) {
                URLConnection con = cf.openConnection(name);
                if (con instanceof FileConnection) { // could be NOT AVAILABLE
                    FileConnection fc = (FileConnection) con;
                    File file = fc.getFile();
                    if (file.equals(f)) {
                        return null; // ok, not shadowed.
                    } else {
                        if (file.exists()) {
                            try {
                                return file.toURI().toURL(); // f is shadowed!
                            } catch (MalformedURLException mfue) {
                                assert false : mfue;
                            }
                        }
                    }
                }
            }
        }
        for (File file : getFiles(name)) {
            if (file.equals(f)) {
                return null;
            } else {
                if (file.exists()) {
                    try {
                        return file.toURI().toURL(); // f is shadowed!
                    } catch (MalformedURLException mfue) {
                        assert false : mfue;
                    }
                }
            }
        }
        // did not find f as a file for this resource
        throw new IllegalArgumentException("File " + f + " is not a file for resource "  + name);
    }



    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + context.getPath() + " resolving in "  + roots;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        // if this is a 'root' loader, then the only equal object should be the object itself!
        if (parent == null) return  false;
        if (o instanceof ResourceLoader) {
            ResourceLoader rl = (ResourceLoader) o;
            return rl.parent == parent && rl.context.sameFile(context);
        } else {
            return false;
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = HashCodeUtil.hashCode(result, parent);
        result = HashCodeUtil.hashCode(result, context);
        return result;
    }

    /**
     * ================================================================================
     * INNER CLASSES, all private, protected
     * ================================================================================
     */


    /**
     * Extension URLStreamHandler, used for the 'sub' Handlers, entries of 'roots' in ResourceLoader are of this type.
     */
    protected abstract class PathURLStreamHandler extends URLStreamHandler {
        /**
         * We need an openConnection by name only, and public.
         */
        abstract public URLConnection openConnection(String name);

        /**
         * When a URL has been created, in {@link #openConnection(String)}, this method can make a 'name' of it again.
         */
        abstract protected String getName(URL u);

        /**
         * Returns a List of URL's with the same name. Defaults to one URL.
         */
        Enumeration<URL> getResources(final String name) throws IOException {
            return new Enumeration<URL>() {
                    private boolean hasMore = true;
                    public boolean hasMoreElements() {
                        return hasMore;
                    };
                    public URL nextElement() {
                        hasMore = false;
                        return openConnection(name).getURL();
                    }

                };
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            String name = getName(u);
            if (name != null) {
                return openConnection(name);
            } else {
                log.warn("" + this + " could not find name for " + u);
                return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(u.getPath());
            }
        }

        abstract Set<String> getPaths(Set<String> results, Pattern pattern,  boolean recursive,  boolean directories);
    }


    // ================================================================================
    // Files

    protected abstract class  AbstractFileURLStreamHandler extends PathURLStreamHandler {
        protected final boolean writeable;
        AbstractFileURLStreamHandler(boolean w) {
            writeable = w;
        }

        @Override
        public URLConnection openConnection(final String name)  {
            URL u;
            try {
                if (name.startsWith("file:")) {
                    u = new URL(null, new URI(name).toURL().toString(), this);
                } else {
                    File file = getFile(name);
                    if (file == null) return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
                    String fileUrl = file.toURI().toURL().toString();
                    u = new URL(null, fileUrl, this);
                }
            } catch (java.net.URISyntaxException use) {
                throw new AssertionError(use.getMessage());
            } catch (MalformedURLException mfue) {
                throw new AssertionError(mfue.getMessage());
            }
            return new FileConnection(u, getFile(name), writeable);
        }
        @Override
        public Set<String> getPaths(final Set<String> results, final Pattern pattern,  final boolean recursive, final boolean directories) {
            return getPaths(results, pattern, recursive ? "" : null, directories);
        }
        private  Set<String> getPaths(final Set<String> results, final Pattern pattern,  final String recursive, final boolean directories) {
            FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        File f = new File(dir, name);
                        return pattern == null || (f.isDirectory() && recursive != null) || pattern.matcher(f.toString()).matches();
                    }
                };
            File f = getFile(recursive);

            if (f != null && f.isDirectory()) { // should always be true
                File [] files = f.listFiles(filter);
                if (files == null) return results;
                for (File element : files) {
                    if (element.getName().equals("")) {
                        continue;
                    }
                    if (recursive != null && element.isDirectory()) {
                        getPaths(results, pattern, recursive + element.getName() + "/", directories);
                    }
                    if (element.canRead() && (directories == element.isDirectory())) {
                        results.add((recursive == null ? "" : recursive) + element.getName());
                    }

                }
            }

            return results;
        }
        abstract public File getFile(String name);
    }

    protected  class FileURLStreamHandler extends AbstractFileURLStreamHandler {
        private final File fileRoot;
        FileURLStreamHandler(File root, boolean w) {
            super(w);
            fileRoot = root;

        }
        FileURLStreamHandler(FileURLStreamHandler f) {
            super(f.writeable);
            fileRoot  = f.fileRoot;
        }

        @Override
        public File getFile(String name) {
            if (name != null && name.startsWith("file:")) {
                try {
                    return new File(new URI(name)); // hff, how cumbersome, to translate an URL to a File
                } catch (URISyntaxException use) {
                    log.warn(use);
                }
            }
            //String fileName = fileRoot  + (name == null ? "" : name);
            String fileName = fileRoot + ResourceLoader.this.context.getPath() + (name == null ? "" : name);
            //System.out.println("" + fileName);
            if (! File.separator.equals("/")) { // windows compatibility
                fileName = fileName.replace('/', File.separator.charAt(0)); // er
            }
            return new File(fileName);
        }
        @Override
        public String getName(final URL u) {
            int l = (fileRoot + ResourceLoader.this.context.getPath()).length();
            String path;
            try {
                path = new File(u.toURI()).getPath(); // toURI decently unescapes %20 and so on (solves MMB-1894)
            } catch (URISyntaxException use) {
                log.error(use);
                path = u.getPath();
            }
            String r = l < path.length() ? path.substring(l) : path;
            return r;

        }

        @Override
        public String toString() {
            return fileRoot.toString();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 71 * hash + (this.fileRoot != null ? this.fileRoot.hashCode() : 0);
            return hash;
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof FileURLStreamHandler) {
                FileURLStreamHandler of = (FileURLStreamHandler) o;
                return of.fileRoot.equals(fileRoot);
            } else {
                return false;
            }
        }


    }


    /**
     * A URLConnection for connecting to a File.  Of course SUN ships an implementation as well
     * (File.getURL), but Sun's implementation sucks. You can't use it for writing a file, and
     * getDoInput always gives true, even if the file does not even exist.  This version supports
     * checking by <code>getDoInput()</code> (read rights) and <code>getDoOutput()</code> (write
     * rights) and deleting by <code>getOutputStream().write(null)</code>
     */
    private class FileConnection extends URLConnection {
        private final File file;
        private final boolean writeable;
        FileConnection(URL u, File f, boolean w) {
            super(u);
            this.file = f;
            this.writeable = w;
        }
        @Override
        public void connect() throws IOException {
            connected = true;
        }

        public File getFile() {
            return file;
        }

        @Override
        public boolean getDoInput() {
            return file.canRead();
        }
        @Override
        public boolean getDoOutput() {
            if (! writeable) return false;
            File f = file;
            while (f != null) {
                if (f.exists()) {
                    return f.canWrite();
                } else {
                    f = f.getParentFile();
                }
            }
            return false;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (! connected) connect();
            return new FileInputStream(file);
        }
        @Override
        public OutputStream getOutputStream() throws IOException {
            if (! connected) connect();
            if (! writeable) {
                throw new UnknownServiceException("This file-connection does not allow writing.");
            }
            // create directory if necessary.
            File parent = file.getParentFile();
            if (parent != null) {
                if (! parent.exists()) {
                    log.info("Creating subdirs for " + file);
                }
                parent.mkdirs();
                if (! parent.exists()) {
                    log.warn("Could not create directory for " + file + ": " + parent);
                }
            } else {
                log.warn("Parent of " + file + " is null ?!");
            }
            if (file.isDirectory()) {
                final File directory = file;
                return new OutputStream() {
                    @Override
                    public void write(byte[] b) throws IOException {
                        if (b == null) {
                            directory.delete();
                        } else {
                            super.write(b);
                        }
                    }
                    @Override
                    public void write(int b) throws IOException {
                        throw new UnsupportedOperationException("Cannot write bytes to a directory outputstream");
                    }
                };

            } else {
                return new FileOutputStream(file) {
                    @Override
                    public void write(byte[] b) throws IOException {
                        if (b == null) {
                            file.delete();
                        } else {
                            super.write(b);
                        }
                    }
                };
            }
        }
        @Override
        public long getLastModified() {
            return file.lastModified();
        }

        @Override
        public String toString() {
            return "FileConnection " + file.toString();
        }

    }

    // ================================================================================
    // ApplicationContext

    /**
     * @since MMBase-1.9
     */
    protected class ApplicationContextFileURLStreamHandler extends AbstractFileURLStreamHandler {
        private Map<String, String> FILES;
        ApplicationContextFileURLStreamHandler() {
            super(true);
            try {
                FILES = ApplicationContextReader.getProperties("mmbase-config"  + ResourceLoader.this.context.getPath());
            } catch (javax.naming.NameNotFoundException nnfe) {
                // never mind
                log.debug(nnfe);
                FILES = new HashMap<String, String>();
            } catch (javax.naming.NoInitialContextException nice) {
                // never mind
                log.debug(nice);
                FILES = new HashMap<String, String>();
            } catch (javax.naming.NamingException ne) {
                log.error(ne);
                FILES = new HashMap<String, String>();
            } catch (NoClassDefFoundError ncdfe) {
                // via rmmci, applcationcontextreader not available. Never mind
                log.debug(ncdfe);
                FILES = new HashMap<String, String>();
            }
        }

        protected File getFileFromString(String s) {
            if (s == null) {
                return null;
            }
            try {

                if (s.startsWith("file:")) {
                    return new File(new URI(s)); // hff, how cumbersome, to translate an URL to a File
                } else {
                    ResourceLoader wr = getWebRoot();
                    if (wr != null) {
                        URI file = wr.getResource("/" + s).toURI();
                        if (file.getScheme().equals("file")) {
                            return new File(file);
                        } else {
                            log.debug("The web root is no real directory");
                            return new File(s);
                        }
                    } else {
                        return new File(s);
                    }
                }
            } catch (URISyntaxException use) {
                log.warn("" + s + " : " + use.getMessage() , use);
                return new File(s);
            }
        }

        @Override
        public File getFile(final String in) {
            return getFileFromString(FILES.get(in));
        }
        @Override
        public String getName(URL u) {
            for (Map.Entry<String, String> entry : FILES.entrySet()) {
                try {
                    File file = getFileFromString(entry.getValue());
                    if (file != null) {
                        if (file.toURI().toURL().sameFile(u)) {
                            return entry.getKey();
                        }
                    }
                } catch (MalformedURLException mfue) {
                    log.warn(mfue);
                }
            }
            return null;
        }
        @Override
        public String toString() {
            StringBuilder bul = new StringBuilder("{");
            for (Map.Entry<String, String> e : FILES.entrySet()) {
                if (bul.length() > 1) bul.append(", ");
                bul.append(e.getKey()).append('=');
                File f = getFileFromString(e.getValue());
                bul.append(e.getValue());
                if (! f.exists()) {
                    bul.append("(").append(f.toURI()).append(" does not exist)");
                } else if (! f.canRead()) {
                    bul.append("(").append(f).append(" cannot be read)");

                }
            }
            bul.append("}");
            return bul.toString();
        }
    }


    // ================================================================================
    // Nodes


    /**
     * URLStreamHandler for NodeConnections.
     */
    protected class NodeURLStreamHandler extends PathURLStreamHandler {
        private final int type;
        NodeURLStreamHandler(int type) {
            this.type    = type;
        }
        NodeURLStreamHandler(NodeURLStreamHandler nf) {
            this.type = nf.type;
        }

        @Override
        protected String getName(URL u) {
            return u.getPath().substring(NODE_URL_CONTEXT.getPath().length());
        }
        @Override
        public URLConnection openConnection(String name) {
            URL u;
            while (name.startsWith("/")) {
                name = name.substring(1);
            }
            try {
                u = new URL(NODE_URL_CONTEXT, name, this);
            } catch (MalformedURLException mfue) {
                throw new AssertionError(mfue.getMessage());
            }
            return new NodeConnection(u, name, type);
        }
        @Override
        public Set<String> getPaths(final Set<String> results, final Pattern pattern,  final boolean recursive, final boolean directories) {
            if (ResourceLoader.getResourceBuilder() != null) {
                try {
                    NodeManager nm = ResourceLoader.getResourceBuilder();
                    NodeQuery query = nm.createQuery();
                    Constraint typeConstraint = Queries.createConstraint(query, TYPE_FIELD, Queries.getOperator("="),  type);
                    Constraint nameConstraint = Queries.createConstraint(query, RESOURCENAME_FIELD, Queries.getOperator("LIKE"),  ResourceLoader.this.context.getPath().substring(1) + "%");

                    BasicCompositeConstraint constraint = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);

                    constraint.addChild(typeConstraint).addChild(nameConstraint);

                    query.setConstraint(constraint);
                    for (Node node :  nm.getList(query)) {
                        String url = node.getStringValue(RESOURCENAME_FIELD);
                        String subUrl = url.substring(ResourceLoader.this.context.getPath().length() - 1);
                        int pos = subUrl.indexOf('/');

                        if (directories) {
                            if (pos < 0) continue; // not a directory
                            do {
                                String u = subUrl.substring(0, pos);
                                if (pattern != null && ! pattern.matcher(u).matches()) {
                                    continue;
                                }
                                results.add(u);
                                pos = subUrl.indexOf('/', pos + 1);
                            } while (pos > 0 && recursive);
                        } else {
                            if (pos > 0 && ! recursive) continue;
                            if (pattern != null && ! pattern.matcher(subUrl).matches()) {
                                continue;
                            }
                            results.add(subUrl);
                        }

                    }
                } catch (BridgeException sqe) {
                    log.warn(sqe);
                }
            }
            return results;
        }
        @Override
        public String toString() {
            return "nodes of type " + type;
        }

    }

    /**
     * A URLConnection based on an MMBase node.
     * @see FileConnection
     */
    private class NodeConnection extends URLConnection {
        Node node;
        final String name;
        final int type;
        NodeConnection(URL url, String name, int t) {
            super(url);
            this.name = name;
            this.type = t;
        }
        @Override
        public void connect() throws IOException {
            if (ResourceLoader.getResourceBuilder() == null) {
                throw new IOException("No resources builder available.");
            }
            connected = true;
        }
        /**
         * Gets the Node associated with this URL if there is one.
         * @return MMObjectNode or <code>null</code>
         */
        public  Node getResourceNode() {
            if (node != null) return node;
            if (name.equals("")) return null;
            String realName = (ResourceLoader.this.context.getPath() + name).substring(1);
            if (ResourceLoader.getResourceBuilder() != null) {
                try {
                    NodeManager nm = ResourceLoader.getResourceBuilder();

                    NodeQuery query = nm.createQuery();
                    Constraint constraint1 = Queries.createConstraint(query, RESOURCENAME_FIELD, Queries.getOperator("="), realName);
                    Constraint constraint2 = Queries.createConstraint(query, TYPE_FIELD, Queries.getOperator("="), type);

                    BasicCompositeConstraint  constraint  = new BasicCompositeConstraint(CompositeConstraint.LOGICAL_AND);
                    constraint.addChild(constraint1);
                    constraint.addChild(constraint2);

                    query.setConstraint(constraint);

                    Iterator<Node> i = nm.getList(query).iterator();
                    if (i.hasNext()) {
                        node = i.next();
                        return node;
                    }
                } catch (BridgeException sqe) {
                    log.warn(sqe);
                }
            }
            return null;
        }

        @Override
        public boolean getDoInput() {
            return getResourceNode() != null;
        }

        @Override
        public boolean getDoOutput() {
            getResourceNode();
            return
                (node != null && node.mayWrite()) ||
                (ResourceLoader.resourceBuilder != null && ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null).getNodeManager(ResourceLoader.resourceBuilder).mayCreateNode());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            getResourceNode();
            if (node != null) {
                return node.getInputStreamValue(HANDLE_FIELD);
            } else {
                throw new IOException("No such (node) resource for " + name);
            }
        }
        @Override
        public OutputStream getOutputStream() throws IOException {
            if (getResourceNode() == null) {
                if (ResourceLoader.resourceBuilder == null) return null;

                Cloud cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "class", null);
                NodeManager nm = cloud.getNodeManager(resourceBuilder);
                node = nm.createNode();
                node.setContext(DEFAULT_CONTEXT);
                String resourceName = (ResourceLoader.this.context.getPath() + name).substring(1);
                node.setStringValue(RESOURCENAME_FIELD, resourceName);
                node.setIntValue(TYPE_FIELD, type);
                log.info("Creating node " + resourceName + " " + name + " " + type);
                node.commit();
            }
            return new OutputStream() {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                @Override
                public void close() throws IOException {
                    byte[] b = bytes.toByteArray();
                    node.setValue(HANDLE_FIELD, b);
                    String mimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(b));
                    if (mimeType == null) {
                        URLConnection.guessContentTypeFromName(name);
                    }
                    node.setValue("mimetype", mimeType);
                    node.commit();
                }
                @Override
                public void write(int b) {
                    bytes.write(b);
                }
                @Override
                public void write(byte[] b) throws IOException {
                    if (b == null) {
                        node.delete();
                        node = null;
                    } else {
                        super.write(b);
                    }
                }
            };
        }
        @Override
        public long getLastModified() {
            getResourceNode();
            if (node != null) {
                Date lm = node.getDateValue(LASTMODIFIED_FIELD);
                if (lm != null) {
                    return lm.getTime();
                }
            }
            return -1;
        }

        @Override
        public String toString() {
            return "NodeConnection " + node;
        }

    }



    // ================================================================================
    // ServletContext

    /**
     * If running in a servlet 2.3 environment the ServletResourceURLStreamHandler is not fully
     * functional. A warning about that is logged, but only once.
     */
    private static boolean warned23 = false;

    /**
     * URLStreamHandler based on the servletContext object of ResourceLoader
     */
    protected  class ServletResourceURLStreamHandler extends PathURLStreamHandler {
        private String root;
        ServletResourceURLStreamHandler(String r) {
            root = r;
        }
        ServletResourceURLStreamHandler(ServletResourceURLStreamHandler f) {
            root = f.root;
        }


        @Override
        protected String getName(URL u) {
            return u.getPath().substring(root.length());
        }
        @Override
        public URLConnection openConnection(String name) {
            try {
                String rn = root + name;
                if (rn.startsWith("//")) {
                    // Doesn't seem to work in Jetty otherwise.
                    // On the other hand, it's a bit odd that it can happen, but let simply work around for now.
                    rn = rn.substring(1);
                }
                URL u = ResourceLoader.servletContext.getResource(rn);
                if (u == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Not found " + rn + " in " + ResourceLoader.servletContext);
                    }
                    return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
                } else {
                    log.debug("Found " + u);
                }
                return u.openConnection();
            } catch (IOException ioe) {
                log.debug(ioe.getMessage());
                return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
            }
        }
        @Override
        public Set<String> getPaths(final Set<String> results, final Pattern pattern,  final boolean recursive, final boolean directories) {
            if (log.isDebugEnabled()) {
                log.debug("Getting " + (directories ? "directories" : "files") + " matching '" + pattern + "' in '" + root + "'");
            }
            return getPaths(results, pattern, recursive ? "" : null, directories);
        }

        private  Set<String> getPaths(final Set<String> results, final Pattern pattern,  final String recursive, final boolean directories) {
            if (servletContext != null) {
                try {
                    String contextPath = ResourceLoader.this.context.getPath();
                    // It seems that it should be possible without this if/else stuff.
                    if (contextPath.startsWith("/")) contextPath = contextPath.substring(1); // MMB-1994
                    final String currentRoot  = root + (root.equals("/") ? "" : "/") + contextPath;
                    final String resourcePath = currentRoot + (recursive == null ? "" : recursive);
                    final Collection<String> c = servletContext.getResourcePaths(resourcePath);


                    if (log.isDebugEnabled()) {
                        log.debug("CurrentRoot '" + currentRoot + "' resourcePath '" + resourcePath + "' c: " + c);
                    }

                    if (c == null) return results;

                    for (String res : c) {
                        log.trace(res);
                        if (res.equals(resourcePath + "/")) {
                            // I think this is a bug in Jetty (according to javadoc this should not happen, but it does!)
                            continue;
                        }

                        String newResourcePath = res.substring(currentRoot.length());
                        final boolean isDir = newResourcePath.endsWith("/");
                        if (isDir) {
                            newResourcePath = newResourcePath.substring(0, newResourcePath.length() - 1);
                            // subdirs
                            if (recursive != null) {
                                getPaths(results, pattern, newResourcePath, directories);
                            }
                            if (newResourcePath.equals("")) continue;
                        }
                        if ((pattern == null || pattern.matcher("/" + newResourcePath).matches()) && (directories == isDir)) {
                            log.debug("Adding " + newResourcePath);
                            results.add(newResourcePath);
                        } else {
                            log.debug("/" + newResourcePath + " does not match " + pattern);
                        }
                    }
                } catch (NoSuchMethodError nsme) {
                    if (! warned23) {
                        log.warn("Servet 2.3 feature not supported! " +  nsme.getMessage());
                        warned23 = true;
                    }
                    // servletContext.getResourcePaths is only a servlet 2.3 feature.

                    // old app-server (orion 1.5.4: java.lang.NoSuchMethodError: javax.servlet.ServletContext.getResourcePaths(Ljava/lang/String;)Ljava/util/Set;)
                    // simply ignore, running on war will not work in such app-servers
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                    // ignore
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Returning " + results);
            }
            return results;
        }

        @Override
        public String toString() {
            return "ServletResource " + root;
        }
    }



    // ================================================================================
    // ClassLoader

    private static final String RESOURCELOADER_XML = "resourceloader.xml";
    private static org.mmbase.util.xml.UtilReader.PropertiesMap<Collection<Map.Entry<String, String>>> classWeightProperties =
        new org.mmbase.util.xml.UtilReader(RESOURCELOADER_XML, new Runnable() {
                public void run() {
                    ResourceLoader.readClassWeights();
                }
            }
            ) {
            @Override
            protected Map.Entry<String, String> getEntry(final org.mmbase.util.xml.DocumentReader reader, String key, final String value) {
                if (key.startsWith("!")) {
                    String u = reader.getDocument().getDocumentURI();
                    String[] parts = u.split("!", 2);
                    log.debug(u + "-> " + Arrays.asList(parts));
                    if (parts.length == 2) {
                        key = "\\A" + ReplacingLocalizedString.makeLiteral(parts[0]) + key + "\\z"; // should escape '.' and so one.
                    } else {
                        // jetty:run may opt to no wrap it in a jar (though requested by maven).
                        // anyhow you also configure maven to not archive the classes
                        int classes = u.lastIndexOf("/classes/");
                        if (classes >  0) {
                            String prefix = u.substring(0, classes + 1);
                            key = "\\A" + ReplacingLocalizedString.makeLiteral(prefix) + key.substring(1) + "\\z"; // should escape '.' and so one.
                            log.debug("Using key " + key + " in " + u);
                        } else {
                            log.debug("No /classes/ found in " + u);
                        }

                    }
                }
                return new Entry<String, String>(key, value);
            }
        }.getMaps();

    private static final Map<Pattern, Integer> classWeights = new ConcurrentHashMap<Pattern, Integer>();

    private static void readClassWeights() {
        classWeights.clear();
        Collection<Map.Entry<String, String>> col = classWeightProperties.get("classloaderpatterns");
        if (col != null) {
            for (Map.Entry<String, String> entry : col) {
                String k = entry.getKey();
                classWeights.put(Pattern.compile(entry.getKey()), Integer.parseInt(entry.getValue()));
            }
        }
        log.service("Found classWeights " + classWeights);
    }
    static {
        readClassWeights();
    }

    /**
     * Given an {@link URL}, which can be an entry of the result of {@link #getResourceList} or
     * {@link #getResources}, returns its 'weight'. This is the weight that is used for the <em>order</em>
     * in the resource list (heavier ones appear earlier).
     * @since MMBase-1.9.1
     */
    public static int getWeight(final URL u) {
        int w = 0;
        log.debug("Getting weight for " + u + " with " + classWeights);
        if (classWeights != null) {
            for (Map.Entry<Pattern, Integer> e : classWeights.entrySet()) {
                if (e.getKey().matcher(u.toExternalForm()).matches()) {
                    log.debug(" " + e.getKey() + " matched " + u.toExternalForm() + ")");
                    w = e.getValue();
                    break;
                }
            }
        }
        return w;
    }


    private static Comparator<URL> urlComparator;
    private static Comparator<URL> getUrlComparator() {
        if (urlComparator == null) {
            urlComparator = new Comparator<URL>() {
                public int compare(final URL u1, final URL u2)  {
                    int w1 = 0;
                    int w2 = 0;
                    boolean foundw1 = false;
                    boolean foundw2 = false;
                    if (classWeights != null) {
                        for (Map.Entry<Pattern, Integer> e : classWeights.entrySet()) {
                            Pattern p = e.getKey();
                            if (! foundw1 && p.matcher(u1.toExternalForm()).matches()) {
                                w1 = e.getValue();
                                log.trace("Matched " + u1 + " " + p + " -> " + w1);
                                foundw1 = true;
                            }
                            if (! foundw2 && p.matcher(u2.toExternalForm()).matches()) {
                                w2 = e.getValue();
                                log.trace("Matched " + u2 + " " + p + " -> " + w2);
                                foundw2 = true;
                            }
                            if (foundw1 && foundw2) break;
                        }
                    }
                    int r = w2 - w1;
                    return r == 0 ? u1.toString().compareTo(u2.toString()) : r;

                }
                @Override
                @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
                public boolean equals(Object o) {
                    return o == this;
                }

                @Override
                public int hashCode() {
                    int hash = 7;
                    return hash;
                }
            };
        }
        return urlComparator;
    }



    protected class ClassLoaderURLStreamHandler extends PathURLStreamHandler {
        private final String root;


        // Some arrangment to remember wich subdirs were possible
        //private Set subDirs = new HashSet();

        ClassLoaderURLStreamHandler(String r) {
            root = r;
        }
        ClassLoaderURLStreamHandler(ClassLoaderURLStreamHandler f) {
            root = f.root;
        }


        private ClassLoader getClassLoader() {
            ClassLoader cl = ResourceLoader.class.getClassLoader();
            if (cl == null) {
                // A system class.
                return ClassLoader.getSystemClassLoader();
            } else {
                return cl;
            }
        }

        @Override
        protected String getName(URL u) {
            return u.getPath().substring((root +  ResourceLoader.this.context.getPath()).length());
        }
        private String getClassResourceName(String name) throws MalformedURLException {
            String res = root + new URL(ResourceLoader.this.context, name).getPath();
            while (res.startsWith("/")) {
                res = res.substring(1);
            }
            if (log != null && log.isDebugEnabled()) {
                log.debug("Name  " + name + " is resource " + res);
            }
            return res;
        }

        /**
         * @since MMBase-1.9.1
         */
        protected SortedSet<URL> getSortedResources(String name) throws IOException {
            SortedSet<URL> result = new TreeSet<URL>(getUrlComparator());
            String crn = getClassResourceName(name);
            Enumeration<URL> e = getClassLoader().getResources(crn);
            while (e.hasMoreElements()) {
                URL n = e.nextElement();
                result.add(n);
            }
            return result;
        }

        @Override
        Enumeration<URL> getResources(String name) throws IOException {
            try {
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                log.debug("Getting the resource " + name + " from " + this);
                return Collections.enumeration(getSortedResources(name));

            } catch (IOException ioe) {
                throw ioe;
            } catch (Throwable t) {
                log.warn(t.getMessage(), t);
                return Collections.enumeration(Collections.EMPTY_LIST);
            }
        }
        @Override
        final public URLConnection openConnection(String name) {
            try {
                URLConnection u = null;
                Enumeration<URL> resources = getResources(name);
                while (resources.hasMoreElements()) {
                    URLConnection p = resources.nextElement().openConnection();
                    if (p.getDoInput()) {
                        u = p;
                        break;
                    }
                }
                if (u == null) {
                    return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
                }
                //subDirs.add(ResourceLoader.getDirectory(name));
                return u;
            } catch (IOException ioe) {
                return NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
            }
        }

        @Override
        public Set<String> getPaths(final Set<String> results, final Pattern pattern,  final boolean recursive, final boolean directories) {
            return getPaths(results, pattern, recursive, directories, "", null);
        }

        private Set<String> getPaths(final Set<String> results, final Pattern pattern, final boolean recursive, final boolean directories, String resourceDir, String searchUp) {
            try {
                List<String> subDirs = new ArrayList<String>();
                SortedSet<URL> resources = getSortedResources("".equals(resourceDir) ? INDEX : resourceDir + INDEX);
                if (searchUp != null && resourceDir.startsWith("..")) resourceDir = "";
                for (URL u : resources) {
                    InputStream inputStream = u.openStream();
                    if (inputStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        try {
                            while (true) {
                                String line = reader.readLine();
                                if (line == null) break;
                                if (line.startsWith("#")) continue; // support for comments
                                line = line.trim();

                                if (line.startsWith("./")) line = line.substring(2);

                                if (searchUp != null) {
                                    if (line.startsWith(searchUp)) {
                                        line = line.substring(searchUp.length());
                                    } else {
                                        continue;
                                    }
                                }

                                if (directories) {
                                    line = getDirectory(line);
                                }
                                if (line.equals("")) continue;     // support for empty lines

                                int firstSlash = line.indexOf('/');
                                if (firstSlash > 0 && firstSlash < line.length()  && ! recursive) continue;

                                if (pattern == null || pattern.matcher(line).matches()) {
                                    results.add("".equals(resourceDir) ? line : resourceDir + line);
                                }
                                if (line.endsWith("/")) {
                                    subDirs.add("".equals(resourceDir) ? line : resourceDir + line);
                                }
                            }
                        } catch (IOException ioe) {
                        } finally {
                            reader.close();
                        }
                    }
                }
                if (recursive) {
                    for (String dir: subDirs) {
                        String newDir = "".equals(resourceDir) ? dir : resourceDir + dir;
                        getPaths(results, pattern, recursive, directories, newDir, null);
                    }
                }
                if (searchUp == null) {
                    searchUp = ResourceLoader.getDirectoryName(ResourceLoader.this.context.getFile()) + '/';
                    ResourceLoader p = ResourceLoader.this.parent;
                    String rd = "../";
                    while (p != null) {
                        getPaths(results, pattern, recursive, directories, rd, searchUp);
                        searchUp = ResourceLoader.getDirectoryName(p.context.getFile()) + '/' + searchUp;
                        p = p.getParentResourceLoader();
                        rd = "../" + rd;

                    }
                }
            } catch (IOException ioe) {
            }
            /*
            if (directories) {
                Iterator i = subDirs.iterator();
                while (i.hasNext()) {
                    String dir = (String) i.next();
                    if (pattern == null || pattern.matcher(dir).matches()) {
                        results.add(dir);
                    }
                }
            }
            */
            if (log != null && log.isDebugEnabled()) {
                log.debug("Returning  " + results);
            }
            return results;
        }

        @Override public String toString() {
            return "ClassLoader " + root;
        }
    }



    // ================================================================================
    // 'NOT AVAILABLE'


    private static String NOT_FOUND = "/localhost/NOTFOUND/";


    /**
     * URLStreamHandler for URL's which can do neither input, nor output. Such an URL can be
     * returned by other PathURLStreamHandlers too.
     */
    private  final PathURLStreamHandler NOT_AVAILABLE_URLSTREAM_HANDLER = new PathURLStreamHandler() {

            @Override
            protected String getName(URL u) {
                String path = u.getPath();
                return path.substring("/NOTFOUND/".length());
            }

            @Override
            public URLConnection openConnection(String name) {
                URL u;
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                try {
                    u = new URL(null, "http:/" + NOT_FOUND + name, this);
                } catch (MalformedURLException mfue) {
                    throw new AssertionError(mfue.getMessage());
                }
                return new NotAvailableConnection(u, name);
            }

            @Override
            public Set<String> getPaths(final Set<String> results, final Pattern pattern,  final boolean recursive, final boolean directories) {
                return new HashSet<String>();
            }
        };



    /**
     * A connection which can neither do input, nor output.
     */
    private class NotAvailableConnection extends URLConnection {

        private final String name;

        private NotAvailableConnection(URL u, String n) {
            super(u);
            while (n.startsWith("/")) {
                n = n.substring(1);
            }
            name = n;
        }
        @Override
        public void connect() throws IOException {
            throw new IOException("No such resource " + name);
        };
        @Override
        public boolean getDoInput() { return false; }
        @Override
        public boolean getDoOutput() { return false; }
        @Override
        public InputStream getInputStream() throws IOException {
            return null;
        }
        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }
        @Override
        public String toString() { return "NOTAVAILABLECONNECTION " + name; }
    };



    // ================================================================================
    // mm:

    /**
     * The MMURLStreamHandler is a StreamHandler for the protocol 'mm' (which is only for internal
     * use). It combines the Connection types implented here above.
     */

    private class MMURLStreamHandler extends URLStreamHandler implements java.io.Serializable {
        private static final long serialVersionUID = 0L;
        MMURLStreamHandler() {
            super();
        }
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new MMURLConnection(u);
        }
        /**
         * ExternalForms are mainly used in entity-resolving and URL.toString()
         * {@inheritDoc}
         */
        @Override
        protected String toExternalForm(URL u) {
            return new MMURLConnection(u).getInputConnection().getURL().toExternalForm();
        }
    }


    /**
     * Implements the logic for our MM protocol. This logic consists of iterating in <code>ResourceLoader.this.roots</code>.
     */
    private class MMURLConnection extends URLConnection {

        URLConnection inputConnection  = null;
        URLConnection outputConnection = null;
        final String name;


        MMURLConnection(URL u) {
            super(u);
            name = url.getPath().substring(1);
            //log.debug("Connection to " + url + Logging.stackTrace(new Throwable()));
            if (! url.getProtocol().equals(PROTOCOL)) {
                throw new RuntimeException("Only supporting URL's with protocol " + PROTOCOL);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect() {
            connected = true;
        }

        /**
         * Returns first possible connection which can be read.
         */
        protected URLConnection getInputConnection() {
            if (inputConnection != null) {
                return inputConnection;
            }
            for(PathURLStreamHandler cf : ResourceLoader.this.roots) {
                URLConnection c = cf.openConnection(name);
                if (c.getDoInput()) {
                    inputConnection = c;
                    break;
                }
            }
            if (inputConnection == null) {
                setDoInput(false);
                inputConnection = NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
            } else {
                setDoInput(true);
            }
            connect();
            return inputConnection;
        }



        /**
         * Returns <code>true</true> if you can successfully use getInputStream();
         */
        @Override
        public boolean getDoInput() {
            return getInputConnection().getDoInput();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() throws IOException  {
            return getInputConnection().getInputStream();
        }

        /**
         * Returns last URL which can be written, and which is still earlier than the first URL which can be read (or the same URL).
         * This ensures that when used for writing, it will then be the prefered one for reading.
         */
        protected URLConnection getOutputConnection() {
            if (outputConnection != null) {
                return outputConnection;
            }

            // search connection which will be used for reading, and check if it can be used for writing
            ListIterator<PathURLStreamHandler> i = ResourceLoader.this.roots.listIterator();
            while (i.hasNext()) {
                PathURLStreamHandler cf = i.next();
                URLConnection c = cf.openConnection(name);
                if (c.getDoInput()) {
                    if(c.getDoOutput()) { // prefer the currently read one.
                        outputConnection = c;
                    }
                    break;
                }
            }
            if (outputConnection == null) {
                // the URL used for reading, could not be written.
                // Now iterate backwards, and search one which can be.
                while (i.hasPrevious()) {
                    PathURLStreamHandler cf = i.previous();
                    URLConnection c = cf.openConnection(name);
                    if (c.getDoOutput()) {
                        outputConnection = c;
                        break;
                    }
                }
            }

            if (outputConnection == null) {
                setDoOutput(false);
                outputConnection =  NOT_AVAILABLE_URLSTREAM_HANDLER.openConnection(name);
            } else {
                setDoOutput(true);
            }
            connect();
            return outputConnection;
        }

        /**
         * Returns <code>true</true> if you can successfully use getOutputStream();
         */
        @Override
        public boolean getDoOutput() {
            return getOutputConnection().getDoOutput();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream() throws IOException  {
            try {
                OutputStream os = getOutputConnection().getOutputStream();
                if (os == null) {
                    // Can find no place to store this resource. Giving up.
                    throw new IOException("Cannot create an OutputStream for " + url + ", it can no way written, and no resource-node could be created)");
                } else {
                    return os;
                }
            } catch (Exception ioe) {
                throw new IOException("Cannot create an OutputStream for " + url + " " + ioe.getMessage(), ioe);
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public long getLastModified() {
            return getInputConnection().getLastModified();
        }

    }

    // ================================================================================
    /**
     * For testing purposes only
     */
    public static void main(String[] argv) {
        ResourceLoader resourceLoader;

        if (System.getProperty("mmbase.htmlroot") != null) {
            resourceLoader = getWebRoot();
        } else if (System.getProperty("mmbase.config") != null) {
            resourceLoader = getConfigurationRoot();
        } else {
            resourceLoader = getSystemRoot();
        }

        try {
            if (argv.length == 0) {
                System.err.println("useage: java [-Dmmbase.config=<config dir>|-Dmmbase.htmlroot=<some other dir>] " + ResourceLoader.class.getName() + " [<sub directory>] [<resource-name>|*|**]");
                return;
            }
            String arg = argv[0];
            if (argv.length > 1) {
                resourceLoader = getConfigurationRoot().getChildResourceLoader(argv[0]);
                arg = argv[1];
            }
            if (arg.equals("*") || arg.equals("**")) {
                for (String s : resourceLoader.getResourcePaths(Pattern.compile(".*"), arg.equals("**"))) {
                    System.out.println("" + s);
                }
            } else {
                InputStream resource = resourceLoader.getResourceAsStream(arg);
                if (resource == null) {
                    System.out.println("No such resource " + arg + " for " + resourceLoader.getResource(arg) + ". Creating now.");
                    PrintWriter writer = new PrintWriter(resourceLoader.getWriter(arg));
                    writer.println("TEST");
                    writer.close();
                    return;
                }
                System.out.println("-------------------- resolved " + resourceLoader.getResourceList(arg) + " with " + resourceLoader + ": ");

                BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

                while(true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    System.out.println(line);
                }
            }

        } catch (Exception mfeu) {
            System.err.println(mfeu.getMessage() + Logging.stackTrace(mfeu));
        }
    }


}

