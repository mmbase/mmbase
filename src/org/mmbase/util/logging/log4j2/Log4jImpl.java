/*
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.logging.log4j2;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Level;
import org.mmbase.util.logging.Logging;

import org.mmbase.util.FileWatcher;

import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.DOMConfigurator;

import org.xml.sax.InputSource;
import java.io.FileInputStream;

import java.io.PrintStream;
import java.io.File;

/**
 * This Logger implementation extends the Logger class from the log4j
 * project (version >= 1.2). It has the following extra functionality.
 *
 * First of all it uses the LoggerLevel class for Level, and so
 * has two extra priorities, namely 'trace' and 'service'.
 *
 * Further it instantiates one object of itself, named `STDERR' to
 * which stderr will be redirected. Normally this will happen with
 * priority `info' but Exceptions will get priorty `fatal'.
 *
 * It also has a static member method `configure', which calls the
 * configure of DOMConfigurator, in this way log4j classes are used
 * only here, and the rest of MMBase can use only `Logger'.
 *
 * @author Michiel Meeuwissen 
 */

public final class Log4jImpl extends org.apache.log4j.Logger  implements Logger { 
    // class is final, perhaps then its methods can be inlined when compiled with -O?

    // It's enough to instantiate a factory once and for all.
    private final static org.apache.log4j.spi.LoggerRepository repository = new LoggerRepository(getRootLogger());
    private final static  DOMConfigurator domConfigurator = new DOMConfigurator();
    private static Logger log;
    private static File configurationFile = null;

    private static final String classname = Log4jImpl.class.getName();

    /** 
     * Constructor, like the constructor of {@link Category}.
     */

    protected Log4jImpl(String name) {
        super(name);
        // not needed.
    }


  
    /** 
     * As getLogger, but casted to MMBase Logger already. And the possible
     * ClassCastException is caught.
     */
    public static Log4jImpl getLoggerInstance(String name) {
        try {
            return (Log4jImpl) repository.getLogger(name);
        } catch (ClassCastException e) {
            Log4jImpl root =  (Log4jImpl) getRootLogger(); // make it log on root, and log a huge error, that something is wrong.
            root.error("ClassCastException, probably you've forgotten a class attribute in your configuration file. It must say class=\"" + Log4jImpl.class.getName() + "\""); 
            return root;
        }
    }


    /**
     * Calls the configure method of DOMConfigurator, and redirect standard error
     * to STDERR category.
     *
     * @param s: A string to the xml-configuration file. Can be
     * absolute, or relative to the Logging configuration file.
     **/

    public static void configure(String s) {        
        configurationFile = new File(s); 
        if (! configurationFile.isAbsolute()) { // make it absolute
            configurationFile = new File(Logging.getConfigurationFile().getParent() + File.separator + s);
        }
        System.out.println("Parsing " + configurationFile.getAbsolutePath());
        try {
            domConfigurator.doConfigure(new FileInputStream(configurationFile), repository);
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Could not find " + configurationFile  + " to reconfigure: " + e.toString());
        }
        configWatcher.add(configurationFile);
        configWatcher.setDelay(10 * 1000); // check every 10 secs if config changed
        configWatcher.start();
        log = getLoggerInstance(Log4jImpl.class.getName());
       
        Log4jImpl err = getLoggerInstance("STDERR");
        // a trick: if the level of STDERR is FATAL, then stderr will not be captured at all.
        if(err.getLevel() != Log4jLevel.FATAL) {
            System.out.println("Redirecting stdout to MMBase logging");
            System.setErr(new LoggerStream(err));
        }
       
    }

    private static FileWatcher configWatcher = new FileWatcher (true) {
            protected void onChange(File file) {
                try {     
                    System.out.println("Reading " + file);
                    domConfigurator.doConfigure(new FileInputStream(file), repository);
                } catch (java.io.FileNotFoundException e) {
                    System.out.println("Could not find " + file + " to reconfigure");
                }
                //configReader = new XMLBasicReader(file.getAbsolutePath());
                //configure(configReader);
            }
        };



    public static File getConfigurationFile() {
        return configurationFile;
    }

    /**
     * @deprecated use setLevel
     **/

    public void setPriority(Level p) {
        setLevel(p);
    }
    public void setLevel(Level p) {
        switch (p.toInt()) {
        case Level.TRACE_INT:   setLevel(Level.TRACE);   break;
        case Level.DEBUG_INT:   setLevel(Level.DEBUG);   break;
        case Level.SERVICE_INT: setLevel(Log4jLevel.SERVICE); break;
        case Level.INFO_INT:    setLevel(Level.INFO);    break;
        case Level.WARN_INT:    setLevel(Level.WARN);    break;
        case Level.ERROR_INT:   setLevel(Level.ERROR);   break;
        case Level.FATAL_INT:   setLevel(Level.FATAL);
        }
  
    }

    /**
     *  This method overrides {@link Category#getInstance} by supplying
     *  its own factory type as a parameter.    
     */
    public static org.apache.log4j.Category getInstance(String name) {
        return getLogger(name); 
    }
    public static org.apache.log4j.Logger  getLogger(String name) {
        return repository.getLogger(name);
    }

    /**
     * A new logging method that takes the TRACE priority.
     */
    public final void trace(Object message) {       
        // disable is defined in Category
        if (repository.isDisabled(Log4jLevel.TRACE_INT)) {
            return;
        }
        if (Log4jLevel.TRACE.isGreaterOrEqual(this.getEffectiveLevel()))
            //callAppenders(new LoggingEvent(classname, this, Log4jLevel.TRACE, message, null));
            forcedLog(classname, Log4jLevel.TRACE, message, null);
    }

    /**
     *  A new logging method that takes the SERVICE priority.
     */
    public final void service(Object message) {  
        // disable is defined in Category
        if (repository.isDisabled(Log4jLevel.SERVICE_INT)) {
            return;
        }
        if (Log4jLevel.SERVICE.isGreaterOrEqual(this.getEffectiveLevel()))
            //callAppenders(new LoggingEvent(classname, this, Log4jLevel.SERVICE, message, null));
            forcedLog(classname, Log4jLevel.SERVICE, message, null);
    }

    public final boolean isServiceEnabled() {
        if(repository.isDisabled( Log4jLevel.SERVICE_INT))
            return false;   
        return Log4jLevel.SERVICE.isGreaterOrEqual(this.getEffectiveLevel());
    }


    /**
     * Catches stderr and sends it also to the log file (with category `stderr').
     *  
     * In this way, things producing standard output, such as uncatch
     * exceptions, will at least appear in the log-file.
     *
     **/

    private static class LoggerStream extends PrintStream {

        private Logger log;

        private int checkCount = 0;         // needed to avoid infinite
        // recursion in some errorneos situations.

        LoggerStream (Log4jImpl l) throws IllegalArgumentException {
            super(System.out);
            if (l == null) {
                throw new IllegalArgumentException("logger == null");
            }
            log = l;
        }

        private LoggerStream () {
            // do not use.
            super(System.out);
        }
        // simply overriding all methods that possibly could be used (forgotten some still)
        public void print   (char[] s) { log.warn(new String(s)); } 
        public void print   (String s) { log.warn(s); }  
        public void print   (Object s) { log.warn(s.toString()); }
        public void println (char[] s) { log.warn(new String(s)); }
        public void println (String s) { 
            // if something goes wrong log4j write to standard error
            // we don't want to go in an infinite loop then, if LoggerStream is stderr too.            
            if (checkCount > 0) { 
                System.out.println(s); 
            } else {
                checkCount++;
                log.trace("6"); log.warn(s); 
                checkCount--;
            }
        }  
        public void println (Object s) { 
            // it seems that exception are written to log in this way, so we can check 
            // if s is an exception, in which case we want to log with FATAL.
            if (Exception.class.isAssignableFrom(s.getClass())) {
                log.fatal(s.toString()); // uncaught exception, that's a fatal error
            } else {
                log.warn(s.toString());
            }
        }  
        //public void write(byte[] buf) { }  
        //public void write(byte[] b, int off, int len) { }
 
    }

}

