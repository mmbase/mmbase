/*
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.logging;
import java.util.*;
import org.apache.log4j.spi.LocationInfo;

/**
 * A very simple implementation of Logger. It ignores everything below
 * warn (or what else if configured), and throws an exception for
 * everything higher. In junit tests this generates test-case failures
 * if a warn or error is issued (we don't want that in normal
 * situations).
 *
 * Logging can be set up like this in the setup of your test:
   <pre>
  Logging.configure(System.getProperty("mmbase.config") + File.separator + "log" + File.separator + "log.xml");
   </pre>
 *
 * @author Michiel Meeuwissen 
 * @since  MMBase-1.7
 */

public class ExceptionImpl extends AbstractSimpleImpl implements Logger {

    private String cat;
    private static   Map instances = new HashMap();
    protected static int exceptionLevel  = Level.WARN_INT;

    private ExceptionImpl(String c) {
        cat = c;
    }

    public static  ExceptionImpl getLoggerInstance(String name) {
        if (instances.containsKey(name)) {
            return (ExceptionImpl) instances.get(name);
        } else {
            ExceptionImpl i = new ExceptionImpl(name);
            instances.put(name, i);
            return i;
        }
    }

    /**
     * The configure method of this Logger implemenation.
     *
     * @param A string, which can contain the output (stdout or
     * stderr) and the priority (e.g. 'info')
     */
   
    public static  void configure(String c) {
        if (c == null) {
           return; // everything default
        } else {
            StringTokenizer t    = new StringTokenizer(c, ","); 
            if (t.hasMoreTokens()) {
                exceptionLevel = Level.toLevel(t.nextToken()).toInt();
            }
            if (t.hasMoreTokens()) {
                level = Level.toLevel(t.nextToken()).toInt();
            }
        }
    }

    protected final void log (String s, int l) {
        if (l >= level) {
            LocationInfo info = new LocationInfo(new Throwable(), AbstractSimpleImpl.class.getName());
            System.out.println(info.getFileName() + ":" + info.getMethodName() + "." + info.getLineNumber() + ": " + s);
        }
        if (l >= exceptionLevel) {
            throw new LoggingException(cat + ":" + s);
        }
    }


}
