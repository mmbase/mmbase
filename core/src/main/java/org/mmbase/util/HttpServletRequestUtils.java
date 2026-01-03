package org.mmbase.util;

import javax.servlet.http.HttpServletRequest;

/**
 * @since MMBase-1.9.7
 */
public class HttpServletRequestUtils {

    public static void appendAbsolute(HttpServletRequest req, StringBuilder show) {
        String scheme = req.getHeader("x-forwarded-proto");
        if (scheme == null) {
            scheme = req.getScheme();
        }
        show.append(scheme).append("://");
        appendHostWithPort(scheme, req, show);
    }

    public static void appendHostWithPort(String scheme, HttpServletRequest req, StringBuilder show) {
        show.append(getServerName(req));
        int port = getServerPort(req);
        show.append((port == 80 && "http".equals(scheme)) ||
            (port == 443 && "https".equals(scheme))
            ? "" : ":" + port);
    }

    public static String getServerName(HttpServletRequest req) {
        String host = req.getHeader("x-forwarded-host");
        if (host == null) {
            host = req.getServerName();
        }
        return host;
    }

    public static int getServerPort(HttpServletRequest req) {
        int port = req.getIntHeader("x-forwarded-port");
        if (port == -1) {
            port = req.getServerPort();
        }
        return port;
    }

}
