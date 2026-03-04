/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.mmbase.util.IOUtil;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.SizeMeasurable;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.resolvers.Resolvers;
import org.xmlresolver.XMLResolver;

/**
 * This URIResolver can be used to resolve URI's, also in TransformerFactory's.
 * <p>
 * It has knowledge of a kind of path (as used by shells). Every entry
 * of this path is labeled with a 'prefix'.
 * <p>
 * This path always has at least (and on default) two entries:

 <ol>
   <li> Current working directory (prefix: none or 'file:')</li>
   <li> MMBase configuration directory (prefix: 'mm:') </li>
 </ol>

 * Optionally you can add other dirs  between these two.
 * <p>
 * When you start searching in the current working dir, and the URI
 * does not point to an existing file, it starts searching downwards in
 * this list, until it finds a file that does exist.
 *
 * @author Michiel Meeuwissen.
 * @since  MMBase-1.6
 * @version $Id$
 */

public class URIResolver implements javax.xml.transform.URIResolver, SizeMeasurable, Serializable {


    private static final long serialVersionUID = 1L; // increase this if object serialization changes (which we shouldn't do!)
    private static final Logger log = Logging.getLoggerInstance(URIResolver.class);

    private EntryList     dirs;  // prefix -> URL pairs
    private final int           hashCode;

    private final XMLResolver xmlResolver = Resolvers.getResolver();


    /**
     * This constructor does not create an actual object that can be
     * used. Only the hashCode is filled. This is because I liked it
     * possible a URIResolver to be equal to a File. But 'equals' must
     * be symmetric, and only a File can be equal to a File. It seemed
     * stupid to extend URIResolver from File, only for this. If you
     * want to compare a File to to an URIResolver (in Maps), you
     * could wrap the file in such an empty URIResolver, and avoid all
     * further overhead.
     *
     * @param c         The directory for which this URIResolver must (not) be created.
     * @param overhead  A boolean. It is ignored. It serves only to distinct this constructor from the other one.
     * @see org.mmbase.cache.xslt.FactoryCache
     */

    public URIResolver(URL c, boolean overhead) {
        hashCode = c.hashCode();
    }
    /**
     * Create an URIResolver for a certain directory.
     * @param c   The directory for which this URIResolver must be created.
     */

    public URIResolver(URL c) {
        this(c, null);
    }

    /**
     * @since MMBase-1.8
     */
    private static URL toURL(File f) {
        try {
            return f.toURL();
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * @deprecated
     */
    public URIResolver(File f) {
        this(toURL(f), null);
    }

    /**
     * Create an URIResolver without support for a certain directory. (Will be taken the first root).
     */
    public URIResolver() {
        this((URL) null, null);
    }

    /**
     * @deprecated
     */
    public URIResolver(File f, EntryList extraDirs) {
        this(toURL(f), extraDirs);
    }

    /**
     * Besides the current working directory you can also supply an
     * ordered list of URIResolver.Entry's. First in this list are the
     * directories which must be checked first, in case no prefix is
     * given.
     * @param c 'Current working dir'
     * @param extraDirs A EntryList, containing 'extra' dirs with
     * prefixes.  If not specified or null, there will still be one
     * 'extra dir' available, namely the MMBase configuration
     * directory (with prefix mm:)
     */
    public URIResolver(URL c, EntryList extraDirs) {
        if (log.isDebugEnabled()) log.debug("Creating URI Resolver for " + c);
        URL cwd;
        if (c == null) {
            File[] roots = File.listRoots();
            if (roots != null && roots.length > 0) {
                try {
                    cwd = roots[0].toURL();
                } catch (Exception e) {
                    cwd = null;
                }
            } else {
                log.warn("No filesystem root available, trying with 'null'");
                cwd = null;
                // will this result in anything useful?
                // well, I don't think we will use mmbase on root-less systems anyway?
            }
        } else {
            cwd = c;
        }
        dirs = new EntryList();
        dirs.add(new Entry("", cwd));
        if (extraDirs != null) {
            dirs.addAll(extraDirs);
        }
        dirs.add(new Entry("mm:", ResourceLoader.getConfigurationRoot()));
        // URIResolvers  cannot be changed, the hashCode can already be calculated and stored.

        if (extraDirs == null || extraDirs.size() == 0) { // only mmbase config, and root cannot change
            if (log.isDebugEnabled()) log.debug("getting hashCode " + cwd.hashCode());
            hashCode = cwd.hashCode();
            // if only the cwd is set, then you alternatively use the cwd has hashCode is this way.
            // it this way in these case it is easy to avoid constructing an URIResolver at all.
        } else {
            hashCode = dirs.hashCode(); // see also javadoc of List
        }
    }

    /**
     * Returns the working directory which was supplied in the
     * constructor.
     *
     */
    public URL getCwd() {
        return dirs.get(0).getDir();
    }

    /**
     * Creates a 'path' string, which is a list of directories. Mainly useful for debugging, of course.
     *
     * @return A String which could be used as a shell's path.
     */
    public String getPath() {
        StringBuilder result = new StringBuilder();
        for (Entry entry : dirs) {
            result.append(File.pathSeparatorChar);
            result.append(entry.getDir().toString());
        }
        return result.toString();
    }

    /**
     * Creates a List of strings, every entry is a directory prefixed with its 'prefix'. Handy during debugging.
     *
     * @return A List with prefix:path Strings.
     */
    public List<String> getPrefixPath() {
        List<String> result = new ArrayList<String>();
        for (Entry entry : dirs) {
            result.add(entry.getPrefix() + entry.getDir().toString());
        }
        return result;
    }

    /**
     * @deprecated
     */
    public File resolveToFile(String href) {
        return resolveToFile(href, null);
    }
    /**
     * @deprecated
     */
    public File resolveToFile(String href,  String base)  {
        try {
            return new File(resolveToURL(href, base).getFile());
        } catch (Exception e) {
            return null;
        }
    }


    public URL resolveToURL(final String href,  final String base) throws TransformerException {
        if (log.isDebugEnabled()) {
            log.debug("Using resolver  " + this + " to resolve href: " + href + "   base: " + base);
        }
        try {
            URL baseURL;
            if (base == null  // 'base' is often 'null', but happily, this object knows about cwd itself.
                || base.endsWith("javax.xml.transform.stream.StreamSource"))  {
                baseURL = getCwd();
            } else {
                baseURL = resolveToURL(base, null); // resolve URIResolver's prefixes like mm:, ew: in base.
                log.debug("Resolved '" + base + "' to " + baseURL);
            }

            URL path = null;
            { // check all known prefixes
                for (Entry entry : dirs) {
                    String pref = entry.getPrefix();
                    if (! "".equals(pref) && href.startsWith(pref)) { //explicitely stated!
                        path = entry.getPath(href.substring(entry.getPrefixLength()));
                        if (log.isTraceEnabled()) {
                            log.trace("href matches " + entry + " returning " + path);
                        }
                        break;
                    }
                    try {
                        URL u = entry.getPath(href);
                        if (log.isTraceEnabled()) {
                            log.trace("Trying " + u + " " + u.getClass());
                        }
                        // getDoInput does not work for every connection.
                        if (u.openConnection().getInputStream() != null) {
                            log.trace("Ok, breaking");
                            path = u;
                            break;
                        }
                    } catch (MalformedURLException mfe) {
                        log.debug("For " + entry + " " + mfe);
                        // ignore, this might be because of a prefix, which is not yet tried.
                    } catch (java.io.IOException io) {
                        log.debug("For " + entry + " " + io);
                        // ignore, try next one.
                    }
                }
            }

            // still not found!
            if (path == null) {
                if (href.startsWith("file:")) { // don't know excactly why this is good.
                    path =  new URL(baseURL, href.substring(5));
                } else {
                    log.debug("" + baseURL + " " + href);
                    path =  new URL(baseURL, href);
                }
                try {
                    if (path.openConnection().getInputStream() == null) {
                        path = null;
                    }
                } catch (Exception e) {
                    path = null;
                }

            }
            if (log.isDebugEnabled()) {
                log.debug("Returning " + path);
            }
            return path;

        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    /**
     * Implementation of the resolve method of javax.xml.transform.URIResolver.
     *
     * @see javax.xml.transform.URIResolver
     **/

    @Override
    public Source resolve(String href, String base) throws TransformerException {

        try {
            URL u = resolveToURL(href, base);
            if (u == null) {
                Source s = xmlResolver.getURIResolver().resolve(href, base);
                if (s != null) {
                    log.debug("Resolved " + href + " to " + s.getSystemId() + " using XMLResolver");
                    return s;
                }
                log.debug("Didn't resolve " + href);
                return null;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            IOUtil.copy(u.openStream(), bytes);
            Source source = new StreamSource(new ByteArrayInputStream(bytes.toByteArray()));
            //source.setSystemId(u.toString());
            return source;
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }


    /**
     *  URIResolver can be used as a key in Maps (Caches).
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     *  URIResolver can be used as a key in Maps (Caches).
     */
    public boolean equals(Object o) {
        if (o != null && (o instanceof URIResolver)) {
            URIResolver res = (URIResolver) o;
            return (dirs == null ? (res.dirs == null || res.dirs.size() == 1) : dirs.equals(res.dirs));
            // See java javadoc, lists compare every element, files equal if  point to same file
            // extraDirs == null?
            // -> created with first constructor.
        }
        return false;
    }


    public int getByteSize() {
        return getByteSize(new org.mmbase.util.SizeOf());
    }

    public int getByteSize(org.mmbase.util.SizeOf sizeof) {
        return sizeof.sizeof(dirs);
    }
    public String toString() {
        return getPrefixPath().toString();
    }

    /**
     * This is a list of prefix/directory pairs which is used in the constructor of URIResolver.
     */

    static public class EntryList extends ArrayList<Entry> implements Serializable {
        private static final long serialVersionUID = 1L;
        public EntryList() {
        }

        /**
         * Adds an prefix/dir entry to the List.
         * @return The list again, so you can easily 'chain' a few.
         * @throws IllegalArgumentException if d is not a directory.
         * @deprecated
         */
        public EntryList add(String p, File d) {
            try {
                add(new Entry(p, d.toURI().toURL()));
                return this;
            } catch (Exception e) {
                return this;
            }
        }
        public EntryList add(String p, URL u) {
            try {
                add(new Entry(p, u));
                return this;
            } catch (Exception e) {
                return this;
            }
        }
        /**
         * @since MMBase-1.8.2
         */
        public EntryList add(String p, ClassLoader cl) {
            try {
                add(new Entry(p, cl));
                return this;
            } catch (Exception e) {
                return this;
            }
        }

    }

    /**
     * Objects of this type connect a prefix (must normally end in :)
     * with a File (which must be a Directory). A List of this type
     * (EntryList) can be fed to the constructor of URIResolver.
     *
     */

    static class Entry implements java.io.Serializable {
        private static final long serialVersionUID = 2L;
        private String prefix;
        private URL    dir;
        private ClassLoader classLoader;
        private int    prefixLength;

        Entry(String p, URL u) {
            prefix = p;
            dir = u;
            classLoader = null;
            prefixLength = prefix.length(); // avoid calculating it again.
        }
        Entry(String p, ClassLoader cl) {
            prefix = p;
            dir = null;
            classLoader = cl;
            prefixLength = prefix.length(); // avoid calculating it again.
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            try {
                out.writeUTF(prefix);
                if (dir != null && dir.getProtocol().equals("mm")) {
                    out.writeObject("mm");
                } else {
                    out.writeObject(dir);
                }
            } catch (Throwable t) {
                log.warn(t.getMessage(), t);
            }
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            try {
                prefix = in.readUTF();
                Object o = in.readObject();
                if ("mm:".equals(prefix)) {
                    classLoader = ResourceLoader.getConfigurationRoot();
                    dir = null;
                } else {
                    if ("mm".equals(o)) {
                        classLoader = ResourceLoader.getConfigurationRoot();
                        dir = null;
                    } else {
                        dir = (URL) o;
                        classLoader = null;
                    }
                }
            } catch (Throwable t) {
                log.warn(t.getMessage(), t);
            }
            prefixLength = prefix.length();
        }

        String getPrefix() {
            return prefix;
        }
        URL getDir() {
            if (dir != null) {
                return dir;
            } else {
                return classLoader.getResource("");
            }
        }

        /**
         * Uses this entry to resolve a href
         * @since MMBase-1.8.2
         */
        URL getPath(String href) throws MalformedURLException {
            if (dir != null) {
                return new URL(dir, href);
            } else {
                return classLoader.getResource(href);
            }
        }
        int getPrefixLength() {
            return prefixLength;
        }
        public String toString() {
            return prefix + ":" + (dir != null ? (dir.getClass() + " " + dir.toString()) : (classLoader.getClass() + " " + classLoader.toString()));
        }
        public boolean equals(Object o) {
            if (o instanceof File) {
                return dir != null && dir.equals(o);
            } else if (o instanceof Entry) {
                Entry e = (Entry) o;
                return dir != null ?
                    dir.equals(e.dir) :
                    classLoader.equals(e.classLoader);
            } else {
                return false;
            }
        }

        public int hashCode() {
            if (dir != null) {
                return dir.hashCode();
            } else {
                return classLoader.hashCode();
            }
        }

    }

    /**
     * For testing only
     * @since MMBase-1.8
     */
    public static void main(String argv[]) throws Exception {

        URIResolver resolver = new URIResolver(new URL("file:///home/mmbase/head/mmbase/edit/wizard/data"));
        System.out.println("Resolving with " + resolver);
        String href, base;

        href = "xsl/list.xsl";  base = null;
        System.out.println("href: " + href + " base: " + base + " --> " + resolver.resolveToURL(href, base));
        href = "prompts.xsl";  base = "file:///home/mmbase/head/mmbase/edit/wizard/data/xsl/base.xsl";
        System.out.println("href: " + href + " base: " + base + " --> " + resolver.resolveToURL(href, base));

        FileOutputStream fos = new FileOutputStream("/tmp/uriresolver.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(resolver);
        oos.close();

        FileInputStream fis = new FileInputStream("/tmp/uriresolver.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        URIResolver resolver2 = (URIResolver) ois.readObject();
        ois.close();

        System.out.println("r " + resolver2.resolveToURL("mm:hoi", null).getProtocol());

        href = "xsl/list.xsl";  base = null;
        System.out.println("href: " + href + " base: " + base + " --> " + resolver2.resolveToURL(href, base));
        href = "prompts.xsl";  base = "file:///home/mmbase/head/mmbase/edit/wizard/data/xsl/base.xsl";
        System.out.println("href: " + href + " base: " + base + " --> " + resolver2.resolveToURL(href, base));



    }

}
