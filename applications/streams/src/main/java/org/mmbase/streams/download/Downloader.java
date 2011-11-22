/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2011 Netherlands Institute for Sound and Vision

The Open Images Platform is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Open Images Platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with The Open Images Platform.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.openimages.api;

import java.io.*;
import java.net.*;
import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.servlet.FileServlet;
import org.mmbase.util.transformers.*;

/**
 * This is called by {@link MediaDownload} and does the actual downloading and saving 
 * of the downloaded stream on the filesystem. Returns filename of file it is
 * saved into. 
 *
 * @author Andr&eacute; van Toly
 * @author Michiel Meeuwissen
 * @version $Id$
 */

public class Downloader {
    private Logger log = Logging.getLoggerInstance(Downloader.class);
    private URL url;
    private Node node;
    private static File directory = null;
    
    public void setLogger(Logger l) {
        log = l;
    }
    public void setUrl(URL u) {
        url = u;
    }
    public void setNode(Node n) {
        node = n;
    }
    public void setDirectory(File f) {
        directory = f;
    }

    /* The following methods are copied org.mmbase.datatypes.processors.BinaryFile 
        and should probably made accessibel (public?) there. */
    private static File getDirectory() {
        if (directory != null) return directory;
        File servletDir = FileServlet.getDirectory();
        if (servletDir == null) throw new IllegalArgumentException("No FileServlet directory found (FileServlet not (yet) active)?");
        return servletDir;
    }
    private static File getFile(final Node node, final Field field, String fileName) {
        return new File(getDirectory(), getFileName(node, field, fileName).replace("/", File.separator));
    }
    private static String getFileName(final Node node, final Field field, String fileName) {
        StringBuilder buf = new StringBuilder();
        org.mmbase.storage.implementation.database.DatabaseStorageManager.appendDirectory(buf, node.getNumber(), "/");
        buf.append("/").append(node.getNumber()).append(".");
        buf.append(fileName);
        return  buf.toString();
    }
    
    private Asciifier fileNameTransformer = new Asciifier();
    {
        fileNameTransformer.setReplacer("_");
        fileNameTransformer.setMoreDisallowed("[\\s!?:/,]");
    }
    
    public String download() throws MalformedURLException, SocketException, IOException, IllegalArgumentException {
        
        HttpURLConnection huc = getURLConnection(url);
        BufferedInputStream in = new BufferedInputStream(huc.getInputStream());
        
        String urlStr = url.toString();
        String name = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
        Field field = node.getNodeManager().getField("url");
        File dir = getDirectory();
        if (log.isDebugEnabled()) {
            log.debug("filename " + name + ", directory: " + dir);
        }
        
        String existing = (String) node.getValue(field.getName());
        if (existing != null && ! "".equals(existing)) {
            File ef = new File(dir, existing);
            if (ef.exists() && ef.isFile()) {
                log.service("Removing existing file " + ef);
                ef.delete();
            } else {
                log.warn("Could not find " + ef + " so could not delete it");
            }
        }
        
        File f = getFile(node, field, fileNameTransformer.transform(name));
        Map<String, String> meta = FileServlet.getInstance().getMetaHeaders(f);
        meta.put("Content-Disposition", "attachment; " + FileServlet.getMetaValue("filename", name));
        FileServlet.getInstance().setMetaHeaders(f, meta);
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        byte[] buf = new byte[1024];
        int b = 0;
        while ((b = in.read(buf)) != -1) {
            out.write(buf, 0, b);
        }
        out.flush();
        in.close();
        out.close();
        
        String urlValue = f.toString().substring(dir.toString().length() + 1);
        node.setStringValue("url", urlValue);
        if (huc.getContentLength() > -1)  node.setIntValue("filesize", huc.getContentLength());
        if (huc.getContentType() != null) node.setValue("mimetype", huc.getContentType());
        
        if (log.isDebugEnabled()) {
            log.debug("Returning url field: " + urlValue);
            log.debug("File " + f.getName());
        }

        return urlValue;
    }

    /**
     * Opens and tests an HttpURLConnection, throws SocketException, IOException or IllegalArgumentException
     * when it failes to open the connection. Only accepts http connections.
     *
     * @param  url  the url to open
     * @return a connection or null in case of a bad response (f.e. not a 200)
     */
    private HttpURLConnection getURLConnection(URL url) throws SocketException, IOException, IllegalArgumentException {
        URLConnection uc = url.openConnection();
        if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
            HttpURLConnection huc = (HttpURLConnection)uc;
            int res = huc.getResponseCode();
            if (res == -1) {
                log.error("Server error, bad HTTP response: " + res);
                return null;
            } else if (res < 200 || res >= 400) {
                log.warn(res + " - " + huc.getResponseMessage() + " : " + url.toString());
                return null;
            } else {
                return huc;
            }
        /*   
        } else if (url.getProtocol().equals("file")) {
            InputStream is = uc.getInputStream();
            is.close();
            // If that didn't throw an exception, the file is probably OK
            return uc;
        */
        } else {
            throw new IllegalArgumentException("Not a HTTP connection: " + url.toString());
        }
    }

}
