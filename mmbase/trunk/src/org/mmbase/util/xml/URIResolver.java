/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.File;
import java.net.*;
import java.util.*;

import org.mmbase.util.SizeMeasurable;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This URIResolver can be used to resolve URI's, also in TransformerFactory's.
 *
 * It has knowledge of a kind of path (as used by shells). Every entry
 * of this path is labeled with a 'prefix'.
 *
 * This path always has at least (and on default) two entries:

 <ol>
   <li> Current working directory (prefix: none or 'file:')</li>
   <li> MMBase configuration directory (prefix: 'mm:') </li>
 </ol>

 * Optionially you can add other dirs  between these two.
 *
 * When you start searching in the current working dir, and the URI
 * does not point to an existing file, it starts searching downwards in
 * this list, until it finds a file that does exist.
 *
 * @author Michiel Meeuwissen.
 * @since  MMBase-1.6
 * @version $Id: URIResolver.java,v 1.22 2005-01-26 10:23:13 pierre Exp $
 */

public class URIResolver implements javax.xml.transform.URIResolver, SizeMeasurable {

    private static final Logger log = Logging.getLoggerInstance(URIResolver.class);

    private EntryList     dirs;  // prefix -> URL pairs
    private int           hashCode;


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
     * @param extradirs A EntryList, containing 'extra' dirs with
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
        dirs.add(new Entry("mm:", ResourceLoader.getConfigurationRoot().getResource(".")));
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
        return ((Entry) dirs.get(0)).getDir();
    }

    /**
     * Creates a 'path' string, which is a list of directories. Mainly usefull for debugging, of course.
     *
     * @return A String which could be used as a shell's path.
     */
    public String getPath() {
        StringBuffer result = new StringBuffer();
        Iterator i = dirs.iterator();
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
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
    public List getPrefixPath() {
        List result = new ArrayList();
        Iterator i = dirs.iterator();
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
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
                baseURL = resolveToURL(base, null); // resolve URIRsolver's prefixes like mm:, ew: in base.
            }

            URL path = null;
            { // check all known prefixes
                Iterator i = dirs.iterator();
                while (i.hasNext()) {
                    Entry entry = (Entry) i.next();
                    String pref = entry.getPrefix();
                    if (! "".equals(pref) && href.startsWith(pref)) { //explicitely stated!
                        path = new URL(entry.getDir(), href.substring(entry.getPrefixLength()));
                        log.trace("href matches " + entry + " returing " + path);
                        break;
                    }
                    try {
                        URL u = new URL(entry.getDir(), href);
                        log.trace("Trying " + u);
                        // getDoInput does not work for every connection.
                        if (u.openConnection().getInputStream() != null) {
                            path = u;
                            break;
                        }
                    } catch (MalformedURLException mfe) {
                        log.debug(mfe);
                        // ignore, this might be because of a prefix, which is not yet tried.
                    } catch (java.io.IOException io) {
                        log.debug(io);
                        // ignore, try next one.
                    }
                }
            }

            // still not found!
            if (path == null) {
                if (href.startsWith("file:")) { // don't know excactly why this is good.
                    path =  new URL(baseURL, href.substring(5));
                } else {
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

    public Source resolve(String href,  String base) throws TransformerException {
        try {
            URL u = resolveToURL(href, base);
            if (u == null) return null;
            Source source = new StreamSource(u.openStream());
            source.setSystemId(u.toString());
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

    static public class EntryList extends ArrayList {
        public EntryList() {
        }

        /**
         *
         * @throws ClassCastException If you don't add an Entry.
         */
        public boolean add(Object o) {
            return super.add((Entry) o);
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
    }

    /**
     * Objects of this type connect a prefix (must normally end in :)
     * with a File (which must be a Directory). A List of this type
     * (EntryList) can be fed to the constructor of URIResolver.
     *
     */

    static class Entry {
        private String prefix;
        private URL    dir;
        private int    prefixLength;

        Entry(String p, URL u) {
            prefix = p;
            dir = u;
            prefixLength = prefix.length(); // avoid calculating it again.

            /*
            if (! d.isDirectory()) {
                throw new IllegalArgumentException(d.toString() + " is not an existing directory");
            }
            */
        }

        String getPrefix() {
            return prefix;
        }
        URL getDir() {
            return dir;
        }
        int getPrefixLength() {
            return prefixLength;
        }
        public String toString() {
            return prefix + ":" + dir.toString();
        }
        public boolean equals(Object o) {
            if (o instanceof File) {
                return dir.equals(o);
            } else if (o instanceof Entry) {
                Entry e = (Entry) o;
                return dir.equals(e.dir);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return dir.hashCode();
        }

    }

    /**
     * For testing only
     * @mmbase-1.8
     */
    public static void main(String argv[]) {
        try {
            URIResolver resolver = new URIResolver(new URL("file:///tmp/"));
            System.out.println("Resolving with " + resolver);
            String href, base;

            href = "xsl/list.xsl";  base = null;
            System.out.println("href: " + href + " base: " + base + " --> " + resolver.resolveToURL(href, base));
            href = "xsl/prompts.xsl";  base = "file:///home/mmbase/mmbase17/mmbase/edit/wizard/data/xsl/base.xsl";
            System.out.println("href: " + href + " base: " + base + " --> " + resolver.resolveToURL(href, base));

        } catch (Throwable t) {
            System.err.println(t.getClass().getName() + " : " + t.getMessage());
        }

    }

}
