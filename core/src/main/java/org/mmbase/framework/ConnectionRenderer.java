/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.framework;

import java.io.*;
import java.net.*;
import java.time.Duration;

import org.mmbase.util.GenericResponseWrapper;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

import org.apache.commons.io.IOUtils;

/**
 * A {@link Renderer} implementation based on an external connection. It also supports
 * XSLT-transforming of the obtained result.
 *
 * An example of a {@link ConnectionRenderer} is the following one:
 *
 * <p>
 *    &lt;block name="mmbase_news"<br />
 *          classification="mmbase.about:100"<br />
 *          mimetype="text/html"&gt;<br />
 *     &lt;title xml:lang="nl"&gt;Nieuws&lt;/title&gt;<br />
 *     &lt;title xml:lang="en"&gt;News&lt;/title&gt;<br />
 *     &lt;description xml:lang="en"&gt;Shows latest news from the mmbase site&lt;/description&gt;<br />
 *     &lt;body&gt;<br />
 *       &lt;class name="org.mmbase.framework.ConnectionRenderer"&gt;<br />
 *         &lt;param name="url"&gt;http://www.mmbase.org/rss&lt;/param&gt;<br />
 *         &lt;param name="xslt"&gt;xslt/rss.xslt&lt;/param&gt;<br />
 *       &lt;/class&gt;<br />
 *     &lt;/body&gt;<br />
 *   &lt;/block&gt;
 * </p>
 *
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.9
 */
public class ConnectionRenderer extends AbstractRenderer {
    private static final Logger log = Logging.getLoggerInstance(ConnectionRenderer.class);


    protected URL url;
    protected int  timeout = 2000;
    protected String xsl = null;
    protected boolean decorate = true;

    public ConnectionRenderer(Type t, Block parent) {
        super(t, parent);
    }

    public void setUrl(String u) throws MalformedURLException {
        url = new URL(u);
    }

    public void setXslt(String x) throws MalformedURLException {
        xsl = x;
    }
    public void setTimeout(Duration t) {
        timeout = (int) t.toMillis();
    }

    public void setDecorate(boolean d) {
        decorate = d;
    }

    @Override public  Parameter[] getParameters() {
        return new Parameter[] {};
    }


    @Override public void render(Parameters blockParameters, Writer w, RenderHints hints) throws FrameworkException {


        if (w == null) {
            throw new NullPointerException();
        }
        InputStream inputStream = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Rendering with " + blockParameters);
            }
            if (decorate) {
                decorateIntro(hints, w, null);
            }
            URL u = getUri(blockParameters, hints).toURL();
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            inputStream = connection.getInputStream();
            if (responseCode == 200) {
                log.debug("" + xsl);
                if (xsl == null) {
                    String encoding = GenericResponseWrapper.getEncoding(contentType);
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, encoding));
                    char[] buf = new char[1000];
                    int c;
                    while ((c = r.read(buf, 0, 1000)) > 0) {
                        w.write(buf, 0, c);
                    }
                } else {
                    URL x = ResourceLoader.getConfigurationRoot().getResource(xsl);
                    Utils.xslTransform(blockParameters, u, inputStream, w, x);
                }


            } else {
                log.debug(u + ":" + responseCode);
                throw new FrameworkException(u + ":" + responseCode);
            }
        } catch (java.net.ConnectException ce) {
            throw new FrameworkException(ce.getMessage(), ce);
        } catch (java.net.SocketTimeoutException ste) {
            throw new FrameworkException(ste.getMessage(), ste);
        } catch (IOException ioe) {
            throw new FrameworkException(ioe.getMessage(), ioe);
        } catch (javax.xml.transform.TransformerException te) {
            throw new FrameworkException(te.getMessage(), te);
        } catch (RuntimeException e) {
            log.debug(e.getMessage(), e);
            throw e;
        } catch(FrameworkException fe) {
            log.debug(fe.getMessage(), fe);
            throw fe;
        } finally {
            IOUtils.closeQuietly(inputStream, null);
            if (decorate) {
                log.debug("Decorating");
                try {
                    decorateOutro(hints, w);
                } catch (Exception e) {
                }
            } else {
                log.debug("no decoration");
            }
        }

    }


    @Override public String toString() {
        return getUri(new Parameters(getParameters()), new RenderHints(this, WindowState.NORMAL, null, null, RenderHints.Mode.NORMAL)).toString();
    }

    @Override public URI getUri(Parameters blockParameter, RenderHints hints) {
        try {
            return url.toURI();
        } catch (URISyntaxException use) {
            throw new RuntimeException(use.getMessage(), use);
        }
    }
}
