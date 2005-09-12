/*
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.logging;

/**
 * Base class for simple Logger implementations (no patterns and so
 * on). A static protected level is provided, so even all categories has to be the same.
 *
 *
 * @author Michiel Meeuwissen
 * @since  MMBase-1.7
 */

abstract public class AbstractSimpleImpl  {

    // could of cause be generalized to an abstract 'getLevel()', but this would
    // nog be too good for performance I think.
    protected static int  level = Level.INFO_INT;

    public void setLevel(Level p) {
        level = p.toInt();
    }

    // override one of these two
    protected void log (String s) {
    }

    protected void log(String s, Level level) {
        log(s);
    }

    /**
     * @since MMBase-1.8
     */
    protected void log (String s, Throwable t) {
        log(s + "\n"  + Logging.stackTrace(t));
    }

    /**
     * @since MMBase-1.8
     */
    protected void log(String s, Level level, Throwable t) {
        log(s, t);
    }

    public void trace (Object m) {
        if (level <= Level.TRACE_INT) {
            log("TRACE " + m,  Level.TRACE);
        }
    }

    public void trace (Object m, Throwable t) {
        if (level <= Level.TRACE_INT) {
            log("TRACE " + m,  Level.TRACE, t);
        }
    }

    public void debug (Object m) {
        if (level <= Level.DEBUG_INT) {
            log("DEBUG " + m, Level.DEBUG);
        }
    }
    public void debug (Object m, Throwable t) {
        if (level <= Level.DEBUG_INT) {
            log("DEBUG " + m, Level.DEBUG, t);
        }
    }

    public void service (Object m) {
        if (level <= Level.SERVICE_INT) {
            log("SERVICE " + m, Level.SERVICE);
        }
    }

    public void service (Object m, Throwable t) {
        if (level <= Level.SERVICE_INT) {
            log("SERVICE " + m, Level.SERVICE, t);
        }
    }

    public void info    (Object m) {
        if (level <= Level.INFO_INT) {
            log("INFO " + m, Level.INFO);
        }
    }

    public void info    (Object m, Throwable t) {
        if (level <= Level.INFO_INT) {
            log("INFO " + m, Level.INFO, t);
        }
    }

    public void warn    (Object m) {
        if (level <= Level.WARN_INT) {
            log("WARN " + m, Level.WARN);
        }
    }

    public void warn    (Object m, Throwable t) {
        if (level <= Level.WARN_INT) {
            log("WARN " + m, Level.WARN, t);
        }
    }

    public void error   (Object m) {
        if (level <= Level.ERROR_INT) {
            log("ERROR " + m, Level.ERROR);
        }
    }

    public void error   (Object m, Throwable t) {
        if (level <= Level.ERROR_INT) {
            log("ERROR " + m, Level.ERROR, t);
        }
    }

    public void fatal   (Object m) {
        if (level <= Level.FATAL_INT) {
            log("FATAL " + m, Level.FATAL);
        }
    }

    public void fatal   (Object m, Throwable t) {
        if (level <= Level.FATAL_INT) {
            log("FATAL " + m, Level.FATAL, t);
        }
    }

    public boolean isDebugEnabled() {
        return level <= Level.DEBUG_INT;
    }

    public boolean isServiceEnabled() {
        return level <= Level.SERVICE_INT;
    }

}
