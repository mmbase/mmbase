/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

/**
 * For an important part stolen from jakarte vfs (only one function). 
 *
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.7
 */
package org.mmbase.util;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

public class UriParser {

    private static final Logger log = Logging.getLoggerInstance(UriParser.class);

    final static char separatorChar = java.io.File.separator.charAt(0); // '\' for windows '/' for other oses.

     /**
     * Converts an absolute path into a relative path.
     *
     * @param basePath The base path.
     * @param path The path to convert.
     */
    static public String makeRelative(final String basePath, final String path ) {
        if (log.isDebugEnabled()) {
            log.debug("converting " + path + " relative to " + basePath);
        }
        // Calculate the common prefix
        final int basePathLen = basePath.length();
        final int pathLen = path.length();

        // Deal with root
        if ( basePathLen == 1 && pathLen == 1 ) {
            return ".";
        } else if ( basePathLen == 1 ) {
            return path.substring(1);
        }

        final int maxlen = Math.min( basePathLen, pathLen );
        int pos = 0;
        for ( ; pos < maxlen && basePath.charAt( pos ) == path.charAt( pos ); pos++ ) {
        }

        if ( pos == basePathLen && pos == pathLen ) {
            // Same names
            return ".";
        } else if ( pos == basePathLen && pos < pathLen && path.charAt( pos ) == separatorChar ) {
            // A descendent of the base path
            return path.substring( pos + 1 );
        }

        // Strip the common prefix off the path
        final StringBuffer buffer = new StringBuffer();
        if ( pathLen > 1 && ( pos < pathLen || basePath.charAt( pos ) != separatorChar ) ) {
            // Not a direct ancestor, need to back up
            pos = basePath.lastIndexOf( separatorChar, pos );
            buffer.append( path.substring( pos ) );
        }

        // Prepend a '../' for each element in the base path past the common
        // prefix
        buffer.insert( 0, ".." );
        pos = basePath.indexOf( separatorChar, pos + 1 );
        while ( pos != -1 ) {
            buffer.insert( 0, "../" );
            pos = basePath.indexOf( separatorChar, pos + 1 );
        }
        if (log.isDebugEnabled()) {
            log.debug("is: " + buffer);
        }

        return buffer.toString();
    }
}
