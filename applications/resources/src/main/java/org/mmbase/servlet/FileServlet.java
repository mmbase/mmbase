/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.servlet;

import org.mmbase.module.core.*;
import org.mmbase.datatypes.processors.FormatFileSize;
import org.mmbase.util.transformers.*;
import java.util.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.util.regex.*;

import java.io.*;
import java.text.*;

import org.mmbase.util.logging.*;


/**
 * Straight-forward filter which can serve files from one directory (the directory 'files' in the
 * mmbase 'datadir') outside the web application root.
 *
 * @version $Id$
 * @author Michiel Meeuwissen
 * @since  MMBase-1.9
 * @see    AttachmentServlet
 */
public class FileServlet extends BridgeServlet {
    private static Logger log = Logging.getLoggerInstance(FileServlet.class);

    private static final UrlEscaper URL = new UrlEscaper();
    private static final String SESSION_EXTENSION  = ".SESSION";

    private static FileServlet firstInstance;
    private File files = null;

    private Pattern ignore = Pattern.compile("");

    // implementation of CERN httpd meta-file processing. (like mod_cern_meta of apache)
    private String metaDir = ".web";
    private String metaSuffix = ".meta";
    private boolean metaFiles = true;

    private Comparator<File> comparator = null;


    //private static final Properties properties= new Properties();

    @Override
    public void init() throws ServletException {
        super.init();
        if (firstInstance == null) firstInstance = this;
        String ig = getInitParameter("ignore");
        if (ig != null && ig.length() > 0) {
            ignore = Pattern.compile(ig);
        }
        String comparatorClass = getInitParameter("comparator");
        if (comparatorClass != null && comparatorClass.length() > 0) {
            try {
                Class clazz = Class.forName(comparatorClass);
                comparator = (Comparator<File>) clazz.newInstance();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public void setMMBase(MMBase mmb) {
        super.setMMBase(mmb);
        log = Logging.getLoggerInstance(FileServlet.class);
        if (files == null) {
            File dataDir = MMBase.getMMBase().getDataDir();

            files = new File(dataDir, "files");
            if (! files.exists()) {
                if (files.mkdirs()) {
                    log.info("Directory " + files + " was created");
                } else {
                    log.warn("Directory " + files + " could not be created");
                }
            }
            log.info("Using datadir " + files);
        }
    }


    @Override
    public String getServletInfo()  {
        return "Serves files from <MMBase data directory>/files";
    }

    @Override
    protected Map<String, Integer> getAssociations() {
        Map<String, Integer> a = super.getAssociations();
        a.put("files",      50);
        return a;
    }

    public static FileServlet getInstance() {
        return firstInstance;
    }
    public static File getDirectory() {
        return getInstance().files;
    }

    public static File getFile(String pathInfo, ServletResponse res) {
        if (pathInfo == null) {
            return getDirectory();
        } else {
            //final String fileName = URL.transformBack(pathInfo).replace("/", File.separator); // fucks up UTF8
            final File f = new File(getDirectory(), pathInfo.replace("/", File.separator));
            return f;
        }
    }

    protected static File getSessionFile(File f) {
        if (f.getName().endsWith(SESSION_EXTENSION)) return f;
        return new File(f.getParentFile(), f.getName() + SESSION_EXTENSION);
    }

    static final String PROTECTED_FILES = FileServlet.class.getName() + ".protectedfiles";
    /**
     * @since MMBase-1.9.2
     */
    public static void protectFile(HttpServletRequest req, File f) throws IOException {
        File sessionFile = getSessionFile(f);
        Writer w = new FileWriter(sessionFile);
        HttpSession session = req.getSession(true);
        w.write(req.getSession(true).getId());
        w.close();
        Set<File> protectedFiles = (Set<File>) session.getAttribute(PROTECTED_FILES);
        if (protectedFiles == null) {
            protectedFiles = new HashSet<File>();
            session.setAttribute(PROTECTED_FILES, protectedFiles);
        }
        protectedFiles.add(f);
    }


    /**
     * Returns whether the given file can be served out for the given request. You can use {@link
     * #protectFile} to make the file only accessible to the current http session.
     */
    protected boolean canRead(HttpServletRequest req, File f) {
        if (! f.canRead()) return false;

        // something with mmbase security ?
        File sessionFile = getSessionFile(f);
        if (sessionFile.exists()) {
            if (! sessionFile.canRead()) return true;
            try {
                BufferedReader r = new BufferedReader(new FileReader(sessionFile));
                String sessionId = r.readLine();
                r.close();
                return sessionId.equals(req.getSession(true).getId());
            } catch (IOException ioe) {
                log.warn(ioe);
                return false;
            }
        } else {
            return true;
        }
    }

    protected boolean ignores(String pi) {
        return pi != null && (ignore.matcher(pi).matches());
    }

    /**
     * FileServlet supports 'meta' files like Cern HTTPD (and apaches mod_cern_meta).
     * @since MMBase-1.9.2
     */
    public  File getMetaFile(File f) {
        File webDir = new File(f.getParentFile(), metaDir);
        File metaFile = new File(webDir, f.getName() + metaSuffix);
        return metaFile;
    }

    private static final Pattern STARTS_WITH_WHITESPACE = Pattern.compile("^\\s+.*");
    /**
     * Returns contents of {@link #getMetaFile} as a Map.
     * @since MMBase-1.9.2
     */
    public Map<String, String> getMetaHeaders(File f) {
        Map<String, String> meta = new HashMap<String, String>();
        File metaFile = getMetaFile(f);
        if (metaFile.exists() && metaFile.canRead()) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(metaFile), "ISO-8859-1"));
                String line = r.readLine();
                String prevHeader = null;
                while (line != null) {
                    if (prevHeader != null && STARTS_WITH_WHITESPACE.matcher(line).matches()) {
                        meta.put(prevHeader, meta.get(prevHeader) + "\n" + line);
                    } else {
                        line = line.trim();
                        String[] header = line.split(":\\s*", 2);
                        if (header.length == 2) {
                            meta.put(header[0], header[1]);
                            prevHeader = header[0];
                        } else {
                            log.warn("Could not parse " + line);
                        }
                    }
                    line = r.readLine();
                }
            } catch (IOException ioe) {
                log.error(ioe);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Found " + meta + " for " + f);
        }
        return meta;
    }
    /**
     * Sets contents of {@link #getMetaFile} as a Map.
     * @since MMBase-1.9.2
     */
    public void setMetaHeaders(File f, Map<String, String> meta) {
        try {
            File metaFile = getMetaFile(f);
            metaFile.getParentFile().mkdirs();
            OutputStream os = new FileOutputStream(metaFile);
            for (Map.Entry<String, String> entry : meta.entrySet()) {
                // The values my have some decomposed form, in that case the ISO-8859-1 bytes don't work so nice.
                String line = entry.getKey() + ": " + entry.getValue() + "\n";
                try {
                    Class c = Class.forName("java.text.Normalizer");
                    line = Normalizer.normalize(entry.getKey() + ": " + entry.getValue() + "\n", Normalizer.Form.NFC);
                } catch (ClassNotFoundException cnfe) {
                    log.warn("java.text.Normalizer is unsupported (JVM < 1.6) !");
                }
                os.write(line.getBytes("ISO-8859-1"));
            }
            os.close();
            log.debug("Created: " + metaFile);
        } catch (IOException ioe) {
            log.warn("IOException: " +  ioe);
        }
    }

    /**
     * @since MMBase-1.9.6
     */
    public static String parseMetaValue(String fieldName, String cd) {
        String[] fields = cd.split(";");
        String value = null;
        for (String field : fields) {
            String[] expr = field.split("=", 2);

            if (expr.length == 2) {
                String foundFieldName = expr[0].trim();
                if (foundFieldName.equals(fieldName) && value == null) {
                    value = expr[1].trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                if (foundFieldName.equals(fieldName + "*")) {
                    value = expr[1].trim();
                    int q = value.indexOf("'");
                    if (q > 0) {
                        q = value.indexOf("'", q + 1);
                        if (q > -1) {
                            String found = value.substring(q + 1);
                            //System.out.println("found " + found);
                            value = UrlEscaper.INSTANCE.transformBack(found);
                        }
                    }
                }

            }
        }
        return value;
    }

    /**
     * @since MMBase-1.9.6
     */
    public static String getMetaValue(String field, String value) {
        //http://greenbytes.de/tech/webdav/rfc5987.html
        //http://code.google.com/p/chromium/issues/detail?id=57830
        return field + "=\"" + value + "\"; " + field + "*=UTF-8''" + UrlEscaper.INSTANCE.transform(value);
    }

    protected File checkFile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pi = req.getPathInfo();
        log.debug(pi);
        if (ignores(pi)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The file '" + pi + "' is explicitely ignored by the file servlet.");
            return null;
        } else {
            log.debug("Not matching " + ignore);
        }

        File file = getFile(pi, resp);
        log.debug(" " + file);
        if (! file.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "The file '" + pi + "' does not exist");
            log.debug("" + pi + " "  + file + " does not exist");
            return null;
        }
        if (! canRead(req, file)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The file '" + pi + "' may not be read");
            log.debug("" + file + " may not be read");
            return null;
        }
        if (file.isDirectory()) {
            if (! "true".equals(getInitParameter("listings"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Directory listings are not enabled. (see web.xml)");
                return null;
            } else {
                if (pi == null || ! pi.endsWith("/")) {
                    resp.sendRedirect(req.getContextPath() + req.getServletPath() + (pi == null ? "" : pi) + "/");
                    return null;
                }
            }
        }
        return file;
    }
    protected void setHeaders(HttpServletRequest req, HttpServletResponse resp, File file) throws ServletException, IOException {
        if (file.isDirectory()) {
            resp.setContentType("application/xhtml+xml"); // We hate IE anyways.
        } else {
            resp.addHeader("Accept-Ranges", "bytes");
            resp.setContentType(getServletContext().getMimeType(file.getName()));
            final ChainedRange range = getRange(req, file);
            if (range != null) {
                long length = range.getLength();
                resp.setContentLength((int) length);
                if (length < file.length()) {
                    resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                }
            } else {
                resp.setContentLength((int) file.length());
            }
            if (metaFiles) {
                for (Map.Entry<String, String> e : getMetaHeaders(file).entrySet()) {
                    resp.setHeader(e.getKey(), e.getValue());
                }
            }
        }
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        File file = checkFile(req, resp);
        if (file == null) {
           return;
        }
        setHeaders(req, resp, file);
        if (file.isDirectory()) {
            byte [] bytes = getListingBytes(req, resp, file);
            resp.setContentLength(bytes.length);
        }
        return;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        File file = checkFile(req, resp);
        if (file == null) {
           return;
        }
        OutputStream out = resp.getOutputStream();
        setHeaders(req, resp, file);

        if (file.isDirectory()) {
            listing(req, resp, out, file);
        } else {
            stream(req, resp, out, file);
        }
    }


    /**
     * @TODO Ranges stuff can be generalized to also work e.g. with images and attachments.
     *
     * Tomcat has an implementation of these headers: Content-Range, Accep-Ranges, Range and If-Range.
     * Use that? See org.apache.catalina.servlets.DefaultServlet
     * http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/catalina/servlets/DefaultServlet.html
     *
     * @since MMBase-2.0
     */
    protected static interface Range {
        /**
         * If we are at byte number i, how many are available from here until we encounter one which isn't?
         * @return A number of bytes which are available, <code>0</code> if there are not bytes available. A large number near <code>Long.MAX_VALUE</code> if there is no limit any more.
         */
        long available(long i);

        /**
         * If we are at byte number i, how many are not available from here until we encounter one which is?
         * @return Some number of bytes or <code>0</code> if the next character is availabe. A large number near <code>Long.MAX_VALUE</code> if all subsequent byes are unavailable.
         */
        long notavailable(long i);
    }

    /**
     * Implementation of Range simply stating the first and last chars which are available, perhaps with a maximum too.
     * This only deals with <start>-<stop> entries in the Range specificiation.
     * @since MMBase-2.0
     */
    protected static class FirstLastRange implements Range {
        private final long first;
        private final long last;
        private final long max;
        FirstLastRange(long f, long l, long m) {
            first = f; last = Math.min(m, l);
            max = m;
        }
        FirstLastRange(String parse, long max) {
            String[] fl = parse.split("-", 2);
            String firstString = fl[0].trim();
            String lastString = fl[1].trim();
            if (firstString.length() == 0) {
                first  = max - Long.parseLong(lastString);
                last   = max - 1;
            } else {
                first = Long.parseLong(firstString);
                last  = Math.min(max - 1, lastString.length() > 0 ? Long.parseLong(lastString) : Long.MAX_VALUE);
            }
            this.max = max;
        }
        public long available(long i) {
            if (i < first) return 0;
            if (i > last) return 0;
            return last - i + 1;
        }
        public long notavailable(long i) {
            if (i < first) return first - i;
            if (i > last)  return max - last;
            return 0;
        }
        @Override
        public String toString() {
            return "" + first + "-" + (last < Long.MAX_VALUE ? last : "");
        }
    }
    /**
     * This implementation of Range parses and combines a number of {@link FirstLastRange}s.
     * So, this deals with the entire Range specification then.
     * @since MMBase-2.0
     */
    protected static class ChainedRange implements Range {
        final List<Range> ranges = new ArrayList<Range>();
        final long max;
        ChainedRange(String s, long max) {
            String[] array = s.split(",");
            for (String r : array) {
                ranges.add(new FirstLastRange(r, max));
            }
            this.max = max;
        }

        public long available(long i) {
            long available = 0;
            for (Range r : ranges) {
                long a = r.available(i);
                available += a;
                i         += a;
            }
            return available;
        }
        public long notavailable(long i) {
            long notavailable = max;
            for (Range r : ranges) {
                long na = r.notavailable(i);
                if (na < notavailable) notavailable = na;
            }
            return notavailable;
        }

        public long getLength() {
            long pos = 0;
            long length = 0;
            while (pos < max) {
                long available = available(pos);
                if (available > 0) {
                    length += available;
                }
                //System.out.println(pos + "/" + max + " available " + available);
                pos += available;
                long notavailable = notavailable(pos);
                pos += notavailable;

            }
            if (length > max) length = max;
            return length;
        }
        @Override
        public String toString() {
            StringBuilder bul = new StringBuilder();
            for (Range r : ranges) {
                if (bul.length() > 0) bul.append(",");
                bul.append(r.toString());
            }
            return bul.append("/").append(max).toString();
        }
    }

    /**
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     * @return A ChainedRange object if Range header was present and If-Range didn't provide useage. <code>null</code> otherwise.
     * @since MMBase-2.0
     */
    protected ChainedRange getRange(HttpServletRequest req, File file) {
        try {
            // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.27
            long ifRange = req.getDateHeader("If-Range");
            if (ifRange != -1) {
                if (ifRange < file.lastModified()) {
                    log.debug("cannot use partial content, because the file was changed in the mean time " + new Date(ifRange) + " < " + new Date(file.lastModified()));
                    return null;
                }
            }
        } catch (IllegalArgumentException ie) {
            // never mind, it may be entity tag, which we don't support
            log.warn("Could not parse " + req.getHeader("If-Range"));
        }

        String range = req.getHeader("Range");

        if (range != null) {
            String r[] = range.split("=");
            if (r.length == 2 && r[0].trim().toLowerCase().equals("bytes")) {
                ChainedRange parsed = new ChainedRange(r[1], file.length());
                if (log.isDebugEnabled()) {
                    log.debug("Range: " + range + " -> " + r[1] + " -> " + parsed);
                }
                return parsed;
            }
        }
        return null;

    }

    /**
     * @todo Generalize this stuff  with Ranges to HandleServlet, so that it also could work for images and attachments.
     */
    protected static void stream(ChainedRange range, InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        if (range != null) {

            long pos = 0;
            while (pos < range.max) {
                long available = range.available(pos);
                if (log.isTraceEnabled()) {
                    log.trace("streaming " + available);
                }
                while(available > 0L) {
                    int b = in.read(buf, 0, (int) Math.min(available, 8192L));
                    out.write(buf, 0, b);
                    pos += b;
                    available -= b;
                }
                long notavailable = range.notavailable(pos);
                if (notavailable > 0L) {
                    in.skip(notavailable);
                    pos += notavailable;
                }
            }
        } else {
            int b = 0;
            while ((b = in.read(buf)) != -1) {
                out.write(buf, 0, b);
            }
        }
        out.flush();
        in.close();
        out.close();
    }


    protected void stream(HttpServletRequest req, HttpServletResponse resp, OutputStream o, File file) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(o);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        final ChainedRange range = getRange(req, file);
        if (range != null) {
            if (log.isDebugEnabled()) {
                log.debug("using range " + range);
            }
            resp.addHeader("Content-Range", "bytes " + range.toString());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No range in request found " + Collections.list(req.getHeaderNames()));
            }
        }
        stream(range, in, out);
    }

    private static final FormatFileSize formatFileSize = new FormatFileSize();

    private static final Xml XML = new Xml();

    protected byte[] getListingBytes(HttpServletRequest req, HttpServletResponse resp, File directory) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append("<?xml version='1.0'?>\n");
        result.append("<html xmlns='http://www.w3.org/1999/xhtml'>");
        result.append("<head>");
        String pathInfo = req.getPathInfo();
        result.append("<title>Directory Listing For " + XML.transform(URL.transformBack(pathInfo)) + "</title>");
        result.append("<link rel='stylesheet' href='" + req.getContextPath() + "/mmbase/style/css/mmbase.css' type='text/css' />");
        result.append("</head>");
        result.append("<body class='filelisting'>");
        result.append("<h1>Directory Listing For " + XML.transform(URL.transformBack(pathInfo)) + "</h1>");
        String header = getInitParameter("header");
        if (header != null && ! "".equals(header)) {
            File headerFile = new File(directory, header);
            if (headerFile.canRead()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(headerFile), "UTF-8"));
                String line = in.readLine();
                while (line != null) {
                    result.append(line);
                    line = in.readLine();
                }
                in.close();
            }
        }
        result.append("<table>");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        result.append("<tr><td class='lastmodified'>" + df.format(new Date(directory.lastModified())) + "</td><td class='filesize'> </td><td class='filename'><a href='.'>./</a></td></tr>");
        if (! pathInfo.equals("/")) {
            result.append("<tr><td class='lastmodified'>" + df.format(new Date(directory.getParentFile().lastModified()))  +"</td><td class='filesize'> </td><td class='filename'><a href='..'>../</a></td></tr>");
        }
        List<File> list = Arrays.asList(directory.listFiles());
        if (comparator != null) {
            Collections.sort(list, comparator);
        }
        for (File file : list) {
            String name = file.getName() + (file.isDirectory() ? "/" : "");
            if (ignores(pathInfo + name)) continue;

            result.append("<tr><td class='lastmodified'>");
            result.append(df.format(new Date(file.lastModified())));
            result.append("</td>");
            result.append("<td class='filesize'>");
            result.append(formatFileSize.process(null, null, file.length()));
            result.append("</td>");
            result.append("<td class='filename'>");
            if (canRead(req, file)) {
                String url = URL.transform(file.getName()) + (file.isDirectory() ? "/" : "");
                result.append("<a href='" + url + "'>" + XML.transform(name) + "</a>");
            } else {
                result.append(XML.transform(name));
            }
            result.append("</td></tr>");
        }
        result.append("</table>");
        result.append("<h3>" + org.mmbase.Version.get() + "</h3>");
        result.append("</body>");
        result.append("</html>");
        try {
            byte [] bytes = result.toString().getBytes("UTF-8");
            return bytes;
        } catch (java.io.UnsupportedEncodingException uue) {
            // cannot happen UTF-8 is supported.
            log.fatal(uue);
            return result.toString().getBytes();
        }
    }
    protected void listing(HttpServletRequest req, HttpServletResponse resp, OutputStream o, File directory) throws IOException {
        byte [] bytes = getListingBytes(req, resp, directory);
        resp.setContentLength(bytes.length);
        BufferedOutputStream out = new BufferedOutputStream(o);
        out.write(bytes);
        out.flush();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        File file = getFile(req.getPathInfo(), resp);
        if (file.exists()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "The file '" + req.getPathInfo() + "' already exists");
            return;
        }
        org.mmbase.bridge.Cloud cloud = getCloud(readQuery(req.getQueryString()));
        if (cloud.getUser().getRank() == org.mmbase.security.Rank.ANONYMOUS) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Anonymous may not put files");
            return;
        }
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        BufferedInputStream in = new BufferedInputStream(req.getInputStream());
        byte[] buf = new byte[1024];
        int b = 0;
        while ((b = in.read(buf)) != -1) {
            out.write(buf, 0, b);
        }
        out.flush();
        in.close();
        out.close();
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        return getFile(req.getPathInfo(), null).lastModified();
    }

    /**
     * The directory as a function. This can makes the information available via RMMCI.
     */
    public static class DirectoryFunction  extends org.mmbase.util.functions.AbstractFunction<String> {
        public DirectoryFunction() {
            super("fileServletDirectory");
        }
        String dir = null;
        public String getFunctionValue(org.mmbase.util.functions.Parameters parameters) {
            if (dir != null) {
                return dir;
            } else {
                return FileServlet.getDirectory().toString();
            }

        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            dir = in.readUTF();
        }
        private void writeObject(ObjectOutputStream out) throws IOException {
            if (dir != null) {
                out.writeUTF(dir);
            } else {
                out.writeUTF(FileServlet.getDirectory().toString());
            }
        }
    }

}


