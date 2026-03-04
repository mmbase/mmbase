/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;


import java.io.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMBaseContext;
import org.mmbase.util.Casting;
import org.mmbase.util.ChainedWriter;
import org.mmbase.util.DateFormats;
import org.mmbase.util.ThreadPools;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This renderer caches other renderers. If you need caching for a certain block, then you define
 * another block with this class CachedRenderer, and refer the to-be-cached block. Like so:
 <pre>
  &lt;block name="statistics_uncached"
         mimetype="text/html"&gt;
    &lt;body&gt;
       &lt;class name="org.mmbase.framework.ResourceRenderer"&gt;
         &lt;param name="resource"&gt;documentation/mmstatistics.xml&lt;/param&gt;
         &lt;param name="type"&gt;config&lt;/param&gt;
         &lt;param name="xslt"&gt;xslt/docbook2block.xslt&lt;/param&gt;
      &lt;/class&gt;
    &lt;/body&gt;
  &lt;/block&gt;

  &lt;block name="statistics"
         classification="mmbase.documentation"
         mimetype="text/html"&gt;
    &lt;body&gt;
      &lt;class name="org.mmbase.framework.CachedRenderer"&gt;
        &lt;param name="wrapsBlock"&gt;statistics_uncached&lt;/param&gt;
        &lt;param name="includeRenderTime"&gt;xml-comments&lt;/param&gt;
      &lt;/class&gt;
    &lt;/body&gt;
  &lt;/block&gt;
</pre> *
 * Caching can be based on an 'expires' property. If that is not specified, caching will happen according the HTTP specifications on the URI returned by {@link Renderer#getUri}.
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9.1

 */
public class CachedRenderer extends WrappedRenderer {
    private static final Logger log = Logging.getLoggerInstance(CachedRenderer.class);

    private int expires = -1; // use last modified

    private int timeout = 2000; // ms
    private int wait    = Integer.MAX_VALUE; // ms

    private String directory = "CachedRenderer";

    private String includeRenderTime = null;

    public void setExpires(int e) {
        expires = e;
    }

    public void setDirectory(String d) {
        directory = d;
    }

    /**
     * If using an HttpURLConnection, then use the given timeout. Defaults to 2 seconds.
     * @param t Timeout in milliseconds
     */
    public void setTimeout(int  t) {
        timeout = t;
    }

    /**
     * If rendering of the cached renderer takes very long, you may choose to not wait for the
     * result. But serve an message or the old version. The job can be joined later.
     *
     * @param t Wait-time in milliseconds
     */
    public void setWait(int  t) {
        wait = t;
    }

    public void setIncludeRenderTime(String type) {
        includeRenderTime = type;
    }

    public CachedRenderer(Type t, Block parent) {
        super(t, parent);
    }


    protected void writeRenderTime(Date time, Writer w) throws FrameworkException, IOException  {
        if (includeRenderTime == null || "".equals(includeRenderTime)) {
            return;
        } else if ("xml-comments".equals(includeRenderTime)) {
            w.write("<!-- ");
            w.write(DateFormats.getInstance("yyyy-MM-dd HH:mm:ss", null, Locale.US).format(time));
            w.write(" -->");
        } else if ("html".equals(includeRenderTime)) {
            w.write("<span class=\"mm_rendertime\">");
            w.write(DateFormats.getInstance("yyyy-MM-dd HH:mm:ss", null, Locale.US).format(time));
            w.write("</span>");
        } else {
            throw new FrameworkException("Did not recognize the value for includeRenderTime: " + includeRenderTime + " (should be empty, 'xml-comments' or 'html')");
        }
    }

    protected String getKey(Parameters blockParameters) {
        StringBuilder k = new StringBuilder();
        for (Map.Entry<String, Object> entry : blockParameters.toUndefaultEntryList()) {
            if (entry.getValue() == null) continue;
            if (! Casting.isStringRepresentable(entry.getValue().getClass())) continue;
            if (k.length() > 0) k.append(':');
            k.append(entry.getKey()).append("=");
            k.append(Casting.toString(entry.getValue()));
        }
        return k.toString();
    }

    private static final String CACHE_EXTENSION = ".cache";
    private static final String ETAG_EXTENSION = ".etag";
    private static final String EXPIRES_EXTENSION = ".expires";
    private static final Pattern INVALID_IN_FILENAME = Pattern.compile("[\\/\\\\\\s]");

    protected File getCacheFile(Parameters blockParameters, RenderHints hints) {
        File cachedDir = new File(MMBase.getMMBase().getDataDir(), directory );
        File dir = new File(cachedDir, MMBaseContext.getHtmlRootUrlPath());
        File componentDir = new File(dir, getBlock().getComponent().getName());
        File blockDir = new File(componentDir, getBlock().getName());
        blockDir.mkdirs();
        String key = getBlock().getName() + "+" + getKey(blockParameters) + "+" + hints.getWindowState() + "_" + hints.getId() + "_" + hints.getStyleClass() + CACHE_EXTENSION;

        String escaped = INVALID_IN_FILENAME.matcher(key).replaceAll("_");
        return new File(blockDir, escaped);

    }

    protected File getETagFile(File file) {
        String name = file.getName();
        File dir = file.getParentFile();
        String tagName = name.substring(0, name.length() - CACHE_EXTENSION.length());
        return new File(dir, tagName + ETAG_EXTENSION);
    }

    protected void writeETag(File f, String etag) throws IOException {
        Writer fw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        fw.write(etag);
        fw.close();
    }

    protected String readETag(File f) throws IOException {
        BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        String t = fr.readLine();
        fr.close();
        return t;
    }

    protected File getExpiresFile(File file) {
        String name = file.getName();
        File dir = file.getParentFile();
        String tagName = name.substring(0, name.length() - CACHE_EXTENSION.length());
        return new File(dir, tagName + EXPIRES_EXTENSION);
    }

    protected void writeExpires(File f, long expires) throws IOException {
        Writer fw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        fw.write("" + expires);
        fw.close();
    }

    protected long readExpires(File f) throws IOException {
        BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        long e = Long.parseLong(fr.readLine());
        fr.close();
        return e;
    }

    protected void renderFile(File f , Writer w) throws FrameworkException, IOException {
        writeRenderTime(new Date(f.lastModified()), w);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        char[] buf = new char[1024];
        int read = reader.read(buf, 0, 1024);
        while (read > 0) {
            w.write(buf, 0, read);
            read = reader.read(buf, 0, 1024);
        }
    }

    private static final Map<File, Future<Throwable>> rendering = new ConcurrentHashMap<>();

    /**
     * Renders the wrapped renderer, and writes the result to both a file, and to the writer.
     */
    protected void renderWrappedAndFile(final File f, final Parameters blockParameters, final Writer w, final RenderHints hints, final Runnable ready) throws FrameworkException, IOException  {
        writeRenderTime(new Date(), w);

        Future<Throwable> future = rendering.get(f);
        if (future == null)  {
            final Parameters myBlockParameters = Utils.fixateParameters(blockParameters);
            future = ThreadPools.jobsExecutor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    File tempFile = new File(f + ".busy");
                    Writer fw = new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8");
                    Writer writer;
                    if (wait == Integer.MAX_VALUE) {
                        writer = new ChainedWriter(w, fw);
                    } else {
                        writer = fw;
                    }
                    getWraps().render(myBlockParameters, writer, hints);
                    writer.flush();
                    fw.close();
                    tempFile.renameTo(f);
                    if (ready != null) {
                        ready.run();
                    }
                    if ((System.currentTimeMillis() - startTime) > (wait / 2)) {
                        log.service("Created " + f);
                    } else {
                        log.debug("Created " + f);
                    }
                    return null;
                } catch (Throwable e) {
                    log.error(e.getMessage());
                    return e;
                }
            });
            ThreadPools.identify(future, "Rendering " + f);
            rendering.put(f, future);
            if (log.isDebugEnabled()) {
                log.debug("Now rendering " + rendering);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Joined " + f + future);
            }
        }
        Throwable e;
        try {
            e = future.get(wait, TimeUnit.MILLISECONDS);
            if (e == null) {
                if (wait < Integer.MAX_VALUE) {
                    if (f.exists()) {
                        renderFile(f, w);
                    } else {
                        log.error("No " + f);
                        w.write("<h1>No " + f + "</h1>");
                    }
                }
            }
            if (rendering.remove(f) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Now rendering " + rendering);
                }
            }
        } catch (TimeoutException to) {
            log.debug(to);
            w.write("<div class='mm_c stale'>");
            if (f.exists()) {
                w.write("<div class=\"msg\">What you see is stale. A new version is being rendered. Please try again later.</div>");
                renderFile(f, w);
            } else {
                w.write("<div class=\"msg\">Rendering took too long. This job is still running. Please try again later.</div>");
            }
            w.write("</div>");
            e = null;
        } catch (InterruptedException | ExecutionException ioe) {
            throw new FrameworkException(ioe);
        }

        if (e != null) {
            if (e instanceof FrameworkException) {
                throw (FrameworkException) e;
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new FrameworkException(e);
        }


    }

    @Override
    public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        File cacheFile = getCacheFile(blockParameters, hints);

        try {
            if (expires > 0) {
                // use expires
                if (! cacheFile.exists() || ( (cacheFile.lastModified() + expires * 1000) < System.currentTimeMillis())) {
                    renderWrappedAndFile(cacheFile, blockParameters, w, hints, null);
                } else {
                    renderFile(cacheFile, w);
                }
            } else {
                // use last modified or etag.
                URI uri = getWraps().getUri(blockParameters, hints);
                if (uri == null) {
                    throw new FrameworkException("" + getWraps() + " did not return an URI, and cannot be cached using getLastModified");
                }
                URLConnection connection =  uri.toURL().openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    int statusCode = httpConnection.getResponseCode();
                    if (statusCode != HttpURLConnection.HTTP_OK) {
                        throw new FrameworkException("The response for " + uri + " returned status code " + statusCode + " (" + httpConnection.getResponseMessage() + ")");
                    }
                }
                connection.setConnectTimeout(timeout);
                String cacheControlHeader = connection.getHeaderField("Cache-Control");
                if (cacheControlHeader == null) {
                    cacheControlHeader = "";
                }
                List<String> cacheControl = Arrays.asList(cacheControlHeader.toLowerCase().split("\\s*,\\s*"));
                if (cacheControl.contains("no-cache") || cacheControl.contains("no-store")) {
                    log.warn("The response for " + uri + " cannot be implicitely cached (Because of Cache-Control: " + cacheControl + ") Use the 'expires' parameter on " + this + " to override this, because it will _not_ be cached now.");
                    getWraps().render(blockParameters, w, hints);
                    return;
                }
                    if (cacheControl.contains("must-revalidate")) {
                        if (cacheFile.exists()) {
                            log.debug("Server indicated that the cache must be revalidated");
                            cacheFile.delete();
                        }
                    }
                final String etag = connection.getHeaderField("ETag");
                final long expiration = connection.getExpiration();
                if (etag != null) {
                    log.debug("Found an etag header on " + uri + " " + etag);
                    final File etagFile = getETagFile(cacheFile);
                    if (!cacheFile.exists() || !etagFile.exists() || !etag.equals(readETag(etagFile))) {
                        renderWrappedAndFile(cacheFile, blockParameters, w, hints, new Runnable() {
                            public void run() {
                                try {
                                    writeETag(etagFile, etag);
                                } catch (IOException ioe) {
                                    throw new RuntimeException(ioe);
                                }
                            }
                        });

                    } else {
                        log.debug("" + cacheFile + " up to date");
                        renderFile(cacheFile, w);
                    }
                } else if (expiration > 0) {
                    log.debug("Found an expires header on " + uri + " " + etag);
                    final File expiresFile = getExpiresFile(cacheFile);
                    if (!cacheFile.exists() || !expiresFile.exists() || System.currentTimeMillis() > readExpires(expiresFile)) {
                        log.service("Rendering " + uri + " because " + cacheFile + " not existing expired");
                        renderWrappedAndFile(cacheFile, blockParameters, w, hints, new Runnable() {
                            public void run() {
                                try {
                                    writeExpires(expiresFile, expiration);
                                } catch (IOException ioe) {
                                    throw new RuntimeException(ioe);
                                }
                            }
                        });
                    } else {
                        log.debug("Serving cached file because not yet expired (it's before " + new Date(expiration) + ")");
                        renderFile(cacheFile, w);
                    }
                } else {

                    long modified = connection.getLastModified();
                    if (modified == 0) {
                        log.warn("No last-modified or expiration returned by " + uri + " taking it 5 minutes after last rendering. Consider using 'expires'. Cache control " + cacheControl);
                        if (cacheFile.exists()) {
                            modified = cacheFile.lastModified();
                            long delay = 5 * 60 * 1000;
                            if (modified + delay < System.currentTimeMillis()) {
                                modified += delay;
                            }
                        }
                    }
                    if (!cacheFile.exists() || (cacheFile.lastModified() < modified)) {
                        log.service("Rendering " + uri + " because " + cacheFile + " older (" + new Date(cacheFile.lastModified()) + ") than " + new Date(modified));
                        renderWrappedAndFile(cacheFile, blockParameters, w, hints, null);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Serving cached file because modification time of " + uri + " (" + new Date(modified) + ") after modification time of " + cacheFile + " (" + cacheFile.lastModified() + ")");
                        }
                        renderFile(cacheFile, w);
                    }
                }
            }

        } catch (IOException mfe) {
            throw new FrameworkException(mfe);
        }
    }


    @Override
    public String toString() {
        return "cached " + wrapped;
    }

}
