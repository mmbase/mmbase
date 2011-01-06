/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.images;

import org.mmbase.util.IOUtil;
import java.util.*;
import java.io.*;

/**
 * A 'Dummy' converter for converting images when Imagemagick and JAI are not available.
 * This class simply returns an image unchanged.
 *
 * @since MMBase 1.6.3
 * @author Gerard van de Looi
 * @version $Id$
 */
public class DummyImageConverter implements ImageConverter {

    /**
     * @see ImageConverter#init(Map)
     */
    @Override
    public void init(Map<String,String> params) {
    }

    /**
     * Call for converting a specified image (byte array) using a list of (string) commands
     * This dummy method ignores any passed commands, and simply returns the inputed list.
     * @see ImageConverter#convertImage(byte[], String, List)
     */
    @Override
    public byte[] convertImage(byte[] input, String sourceFormat, List<String> commands) {
        return input;
    }

    @Override
    public long convertImage(InputStream input, String sourceFormat, OutputStream out, List<String> commands) throws IOException {
        return IOUtil.copy(input, out);
    }

}
