/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package nl.eo.chat;

/**
 * A synchronized logger to be used in situations where more than one thread
 * has access to the logger.
 *
 * @author Jaco de Groot
 */
class SynchronizedLogger extends Logger {

    public SynchronizedLogger(String propertiesFilename, String propertiesPrefix) {
        super(propertiesFilename, propertiesPrefix);
    }
    
    protected synchronized void log(String message) {
        super.log(message);
    }
    
    protected synchronized void checkProperties() {
        super.checkProperties();
    }
    
}
