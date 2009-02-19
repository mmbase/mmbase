package org.mmbase.util.logging.log4j;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This layout can be used in logging in order to print out a stacktrace
 * for a given logging event. THis is a very expensive call, but can be
 * very useful when trying to find application bugs. Using this layout
 * you will see exactly which line of code (or which JSP) generated the
 * exception.
 * @author Johannes Verelst
 */
public class StacktraceLoggingPatternLayout extends PatternLayout {

    /**
     * Overridden from PatternLayout; use the normal pattern to format
     * the logevent, but also return the current stacktrace.
     */
    public String format(LoggingEvent event) {
        String res = super.format(event);
        Exception e = new Exception();
        StackTraceElement[] ste = e.getStackTrace();
        StringBuffer trace = new StringBuffer();
        for (int i=0; i<ste.length; i++) {
          res += "\tat " + ste[i].toString() + "\n";
        }
        return res;
    }
}
