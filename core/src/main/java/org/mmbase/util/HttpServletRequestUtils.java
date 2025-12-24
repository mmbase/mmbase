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
        String host = req.getHeader("x-forwarded-host");
        if (host == null) {
            host = req.getServerName();
        }
        show.append(host);
        int port = req.getIntHeader("x-forwarded-port");
        if (port == -1) {
            port = req.getServerPort();
        }
        show.append((port == 80 && "http".equals(scheme)) ||
            (port == 443 && "https".equals(scheme))
            ? "" : ":" + port);
    }
}
