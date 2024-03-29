/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util.images;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import org.mmbase.util.IOUtil;
import org.mmbase.util.externalprocess.CommandLauncher;
import org.mmbase.util.externalprocess.ProcessException;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * Informs about a image using the 'identify' binary of ImageMagick.
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.8
 * @version $Id$
 */
public class ImageMagickImageInformer implements ImageInformer {

    private static final Logger log = Logging.getLoggerInstance(ImageMagickImageInformer.class);

    // Currently only ImageMagick works, this are the default value's
    private static final String identifyPath = "identify"; // in the path.

    private static final Pattern IDENTIFY_PATTERN = Pattern.compile(".+?\\s.*?\\s(\\d+)x(\\d+).*");

    public void init(Map<String,String> params) {
        String identifyCommand = "identify";
        if(System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows")) {
            // on the windows system, we _can_ assume the it uses .exe as extention...
            // otherwise the check on existance of the program will fail.
            identifyCommand += ".exe";
        }

    }

    protected Dimension getDimension(File file) throws IOException {
        try {
            CommandLauncher launcher = new CommandLauncher("ImageMagick's identify");
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assert file.exists();
            launcher.execute(identifyPath, new String[] {file.getCanonicalPath()});
            launcher.waitAndRead(outputStream, errorStream);
            String result =  new String(outputStream.toByteArray()).trim();
            Matcher matcher = IDENTIFY_PATTERN.matcher(result);
            if (! matcher.matches()) {
                throw new IOException(identifyPath + " " + file.getCanonicalPath() + "'" + result + "' doesn't match " + IDENTIFY_PATTERN + " Errors: " + new String(errorStream.toByteArray()));
            }
            return  new Dimension(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } catch (UnsupportedEncodingException uee) {
            log.error(uee.toString());
        } catch (ProcessException pe) {
            log.error(pe.toString());
        } finally {
            //file.delete();
        }
        return  new Dimension(0, 0);
    }


    public Dimension getDimension(InputStream input) throws IOException {
        File file = File.createTempFile(ImageMagickImageInformer.class.getName(), null);
        try {
            FileOutputStream image = new FileOutputStream(file);
            IOUtil.copy(input, image);
            image.close();
            return getDimension(file);
        } finally {
            file.delete();
        }
    }

    public Dimension getDimension(byte[] input) throws IOException {
        File file = File.createTempFile(ImageMagickImageInformer.class.getName(), null);
        try {
            FileOutputStream image = new FileOutputStream(file);
            image.write(input);
            image.close();
            return  getDimension(file);
        } finally {
            file.delete();
        }

    }
    public static void main(String[] args) {
        try {
            File file = new File(args[0]);
            FileInputStream input = new FileInputStream(file);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            IOUtil.copy(input, bytes);
            input.close();
            byte[] ba = bytes.toByteArray();
            ImageInformer imii  = new ImageMagickImageInformer();
            ImageInformer jaiii = new JAIImageInformer();
            ImageInformer dii   = new DummyImageInformer();
            System.out.println("Image magick " + imii.getDimension(ba));
            System.out.println("JAI          " + jaiii.getDimension(ba));
            System.out.println("Dummy        " + dii.getDimension(ba));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

}
