/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.logging.commons.impl;

import org.mmbase.util.logging.Level;
import org.mmbase.util.logging.Logger;

import org.apache.commons.logging.Log;

/**
 * Adaptor class to convert jakarta-commons Log calls to MMBase Logger calls.<br/>
 * <br/>
 *
 * @author Kees Jongenburger
 */
public class MMBaseLogger implements Log {

    protected Logger logger;

    /**
     * creates a new adaptor
     * @param logger the logger to use
     */
    MMBaseLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @return if debug is enabled in the MMBaseLogger
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * @return if service is enabled in the MMBaseLogger
     */
    public boolean isErrorEnabled() {
        return logger.isEnabledFor(Level.ERROR);
    }

    /**
     * @return is service is enabled in the MMBase logger
     */
    public boolean isFatalEnabled() {
        return logger.isEnabledFor(Level.FATAL);
    }
    /**
     * @return is service is enabled in the MMBase logger
     */
    public boolean isInfoEnabled() {
        return logger.isEnabledFor(Level.INFO);
    }

    /**
     * @return if debug is enabled in the MMBaseLogger
     */
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * @return is service is enabled in the MMBaseLogger
     */
    public boolean isWarnEnabled() {
        return logger.isEnabledFor(Level.WARN);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#trace(java.lang.Object)}
     */
    public void trace(Object object) {
        logger.trace(object);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#debug(java.lang.Object)}
     */
    public void debug(Object object) {
        logger.debug(object);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#info(java.lang.Object)}
     */
    public void info(Object object) {
        logger.info(object);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#warn(java.lang.Object)}
     */
    public void warn(Object object) {
        logger.warn(object);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#error(java.lang.Object)}
     */
    public void error(Object object) {
        logger.error(object);
    }

    /**
     * calls {@link org.mmbase.util.logging.Logger#fatal(java.lang.Object)}
     */
    public void fatal(Object object) {
        logger.fatal(object);

    }

	/**
	 * calls {@link org.mmbase.util.logging.Logger#debug(java.lang.Object, java.lang.Throwable)}
	 */
	public void debug(Object object, Throwable throwable) {
		logger.debug(object, throwable);
	}

	/**
	 * calls {@link org.mmbase.util.logging.Logger#error(java.lang.Object, java.lang.Throwable)}
	 */
	public void error(Object object, Throwable throwable) {
		logger.error(object, throwable);
	}

	/**
	 * calls {@link org.mmbase.util.logging.Logger#fatal(java.lang.Object, java.lang.Throwable)}
	 */
	public void fatal(Object object, Throwable throwable) {
		logger.fatal(object, throwable);
	}

	/**
	 * calls {@link org.mmbase.util.logging.Logger#info(java.lang.Object, java.lang.Throwable)}
	 */
	public void info(Object object, Throwable throwable) {
		logger.info(object, throwable);
	}

	/**
	 * calls {@link org.mmbase.util.logging.Logger#trace(java.lang.Object, java.lang.Throwable)}
	 */
	public void trace(Object object, Throwable throwable) {
		logger.trace(object, throwable);
	}

	/**
	 * calls {@link org.mmbase.util.logging.Logger#warn(java.lang.Object, java.lang.Throwable)}
	 */
	public void warn(Object object, Throwable throwable) {
		logger.warn(object, throwable);
	}

}
