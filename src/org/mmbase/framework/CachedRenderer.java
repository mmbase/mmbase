/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


import org.mmbase.util.functions.*;
import org.mmbase.util.*;
import org.mmbase.module.core.MMBase;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

/**
 * This render caches other renderers. If you need caching for a certain block, then you define
 * another block with this class CachedRenderer, and refer the to-be-cached block. Like so:
 <pre><![CDATA[
  <block name="statistics_uncached"
         mimetype="text/html">
    <body>
       <class name="org.mmbase.framework.ResourceRenderer">
         <param name="resource">documentation/mmstatistics.xml</param>
         <param name="type">config</param>
         <param name="xslt">xslt/docbook2block.xslt</param>
      </class>
    </body>
  </block>

  <block name="statistics"
         classification="mmbase.documentation"
         mimetype="text/html">
    <body>
      <class name="org.mmbase.framework.CachedRenderer">
        <param name="wrapsBlock">statistics_uncached</param>
        <param name="includeRenderTime">xml-comments</param>
      </class>
    </body>
  </block>
]]></pre>
 *
 * @author Michiel Meeuwissen
 * @version $Id: CachedRenderer.java,v 1.5 2009-01-13 20:28:07 michiel Exp $
 * @since MMBase-1.9.1

 */
public class CachedRenderer extends WrappedRenderer {
    private static final Logger log = Logging.getLoggerInstance(CachedRenderer.class);

    private int expires = -1; // use last modified

    private int timeout = 2000; // ms

    private String directory = "CachedRenderer";

    private String includeRenderTime = null;

    public void setExpires(int e) {
        expires = e;
    }

    public void setDirectory(String d) {
        directory = d;
    }

    public void setTimeout(int  t) {
        timeout = t;
    }

    public void setIncludeRenderTime(String type) {
        includeRenderTime = type;
    }

    public CachedRenderer(String t, Block parent) {
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
        for (Map.Entry<String, Object> entry : blockParameters.toEntryList()) {
            if (entry.getValue() == null) continue;
            if (! Casting.isStringRepresentable(entry.getValue().getClass())) continue;
            k.append(entry.getKey()).append("=");
            k.append(Casting.toString(entry.getValue()));
        }
        return k.toString();
    }

    protected File getCacheFile(Parameters blockParameters, RenderHints hints) {
        File dir = new File(MMBase.getMMBase().getDataDir(), directory);
        File componentDir = new File(dir, getBlock().getComponent().getName());
        File blockDir = new File(componentDir, getBlock().getName());
        blockDir.mkdirs();
        String key = getBlock().getName() + "_" + getKey(blockParameters) + "_" + hints.getWindowState() + "_" + hints.getId() + "_" + hints.getStyleClass() + ".cache";
        return new File(blockDir, key);

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

    protected void renderWrappedAndFile(File f, Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException, IOException  {
        writeRenderTime(new Date(), w);
        Writer fw = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        ChainedWriter chain = new ChainedWriter(w, fw);
        getWraps().render(blockParameters, chain, hints);
        chain.flush();
        fw.close();
    }

    @Override public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {
        File cacheFile = getCacheFile(blockParameters, hints);

        try {
            if (expires > 0) {
                // use expires
                if (! cacheFile.exists() || ( (cacheFile.lastModified() + expires * 1000) < System.currentTimeMillis())) {
                    renderWrappedAndFile(cacheFile, blockParameters, w, hints);
                } else {
                    renderFile(cacheFile, w);
                }
            } else {
                // user last modified
                URI uri = getWraps().getUri(blockParameters, hints);
                if (uri == null) throw new FrameworkException("" + getWraps() + " did not return an URI, and cannot be cached using getLastModified");
                URLConnection connection =  uri.toURL().openConnection();
                connection.setConnectTimeout(timeout);
                long modified = connection.getLastModified();
                if (! cacheFile.exists() || (cacheFile.lastModified() < modified)) {
                    log.service("Rendering " + uri + " because " + cacheFile + " older than " + new Date(modified));
                    renderWrappedAndFile(cacheFile, blockParameters, w, hints);
                } else {
                    renderFile(cacheFile, w);
                }
            }
        } catch (MalformedURLException mfe) {
            throw new FrameworkException(mfe);
        } catch (IOException mfe) {
            throw new FrameworkException(mfe);
        }
    }


    @Override public String toString() {
        return "cached " + wrapped;
    }

}
