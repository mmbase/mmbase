/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.images;

import java.util.List;
import java.util.Map;

/**
 * A 'Dummy' converter for converting images when Imagemagick and JAI are not available.
 * This class simply returns an image unchanged.
 *
 * @since MMBase 1.6.3
 * @author Gerard van de Looi
 * @version $Id: DummyImageConverter.java,v 1.1 2005-05-09 09:53:07 michiel Exp $
 */
public class DummyImageConverter implements ImageConverter {

    /**
     * @see ImageConvertInterface#init(Map)
     */
    public void init(Map params) {
    }

    /**
     * Call for converting a specified image (byte array) using a list of (string) commands
     * This dummy method ignores any passed commands, and simply returns the inputed list.
     * @see ImageConvertInterface#convertImage(byte[], List)
     */
    public byte[] convertImage(byte[] input, List commands) {
        return input;
    }

}
