/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.images;

import java.util.Map;
import java.io.*;
import javax.media.jai.*;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;

/**
 * Informs about a image using JAI.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.8
 */
public class JAIImageInformer implements ImageInformer {

    @Override
    public void init(Map<String,String> params) {
    }

    @Override
    public Dimension getDimension(byte[] input) throws IOException { 
        ByteArraySeekableStream bin = new ByteArraySeekableStream(input);
        PlanarImage img = JAI.create("stream", bin);
        return new Dimension(img.getWidth(), img.getHeight());
    }
    @Override
    public Dimension getDimension(InputStream input) {
        MemoryCacheSeekableStream bin = new MemoryCacheSeekableStream(input);
        PlanarImage img = JAI.create("stream", bin);
        return new Dimension(img.getWidth(), img.getHeight());
    }
}
