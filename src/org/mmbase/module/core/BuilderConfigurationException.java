/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module.core;

/**
 * Builder configuration exception.
 * This exception is thrown when there is a (unrecoverable) foault in teh configuration
 * of the builder file, i.e. a required builder file does not exist, a core builder is
 * inactive, circularity is detected between two builders, etc.
 *
 * @since MMBase-1.6
 * @author Pierre van Rooden
 * @version $Id: BuilderConfigurationException.java,v 1.1 2002-03-21 17:17:11 pierre Exp $
 */
public class BuilderConfigurationException extends RuntimeException {

    public BuilderConfigurationException(String s) {
        super(s);
    }
}

