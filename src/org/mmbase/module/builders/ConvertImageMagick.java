/*
 
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */
package org.mmbase.module.builders;

import java.util.*;
import java.io.*;

import org.mmbase.util.ProcessWriter;

import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * Converts images using ImageMagick.
 *
 * @author Rico Jansen
 * @author Michiel Meeuwissen
 * @author Nico Klasens
 * @version $Id: ConvertImageMagick.java,v 1.50 2003-05-08 06:01:21 kees Exp $
 */
public class ConvertImageMagick implements ImageConvertInterface {
    private static Logger log =
    Logging.getLoggerInstance(ConvertImageMagick.class.getName());
    
    // Currently only ImageMagick works, this are the default value's
    private static String converterPath = "convert"; // in the path.
    
    private static int colorizeHexScale = 100;
    // The modulate scale base holds the builder property to specify the scalebase.
    // If ModulateScaleBase property is not defined, then value stays max int.
    private static int modulateScaleBase = Integer.MAX_VALUE;
    
    /**
     * The default image format.
     */
    protected String defaultImageFormat = "jpeg";
    
    /**
     * This function initalises this class
     * @param params a <code>Map</code> of <code>String</string>s containing informationn, this should contina the key's
     *               ImageConvert.ConverterRoot and ImageConvert.ConverterCommand specifing the converter root....
     */
    public void init(Map params) {
        String converterRoot = "";
        String converterCommand = "convert";
        
        /* don't think it is necessary.
        if(System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows")) {
            // on the windows system, we _can_ assume the it uses .exe as extention...
            converterCommand += ".exe";
        }
         */
        
        String tmp;
        tmp = (String) params.get("ImageConvert.ConverterRoot");
        if (tmp != null)
            converterRoot = tmp;
        
        tmp = (String) params.get("ImageConvert.ConverterCommand");
        if (tmp != null)
            converterCommand = tmp;
        
        converterPath = converterCommand; // default.
        if (!converterRoot.equals("")) { // also a root was indicated, add it..
            // now check if the specified ImageConvert.converterRoot does exist and is a directory
            File checkConvDir = new File(converterRoot).getAbsoluteFile();
            if (!checkConvDir.exists()) {
                log.error( "ImageConvert.ConverterRoot " + converterRoot + " in images.xml does not exist");
            } else if (!checkConvDir.isDirectory()) {
                log.error( "ImageConvert.ConverterRoot " + converterRoot + " in images.xml is not a directory");
            } else {
                // now check if the specified ImageConvert.Command does exist and is a file..
                File checkConvCom = new File(converterRoot, converterCommand);
                converterPath = checkConvCom.toString();
                if (!checkConvCom.exists()) {
                    log.error( converterPath + " specified by images.xml does not exist");
                } else if (!checkConvCom.isFile()) {
                    log.error( converterPath + " specified by images.xml is not a file");
                }
            }
        }
        // do a test-run, maybe slow during startup, but when it is done this way, we can also output some additional info in the log about version..
        // and when somebody has failure with converting images, it is much earlier detectable, when it wrong in settings, since it are settings of
        // the builder...
        
        // TODO: on error switch to Dummy????
        // TODO: research how we tell convert, that is should use the System.getProperty(); with respective the value's 'java.io.tmpdir', 'user.dir'
        //       this, since convert writes at this moment inside the 'user.dir'(working dir), which isnt writeable all the time.
        
        Process process = null;
        InputStream in = null;
        ByteArrayOutputStream outputstream = null;
        try {
            log.debug("Starting convert");
            process = Runtime.getRuntime().exec(converterPath);
            in = process.getInputStream();
            outputstream = new ByteArrayOutputStream();
            
            byte[] inputbuffer = new byte[1024];
            int size = 0;
            // well it should be mentioned on first line, that means no need to look much further...
            while ((size = in.read(inputbuffer)) !=  -1) {
                outputstream.write(inputbuffer, 0, size);
            }
            
            // make stringtokenizer, with nextline as new token..
            StringTokenizer tokenizer = new StringTokenizer(outputstream.toString(), "\n\r");
            if (tokenizer.hasMoreTokens()) {
                log.info("Will use: " + converterPath + ", " + tokenizer.nextToken());
            } else {
                log.error( "converter from location " + converterPath + ", gave strange result: " + outputstream.toString() + "conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
            }
        } catch (SecurityException e) {
            log.error( "Was not permitted to execute (because of SecurityManager) " + converterPath + " (" + e.toString() + ") conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
        } catch (IOException e) {
            log.error( "An I/O error occured while executing " + converterPath + " (" + e.toString() + ") conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("An I/O error occured while closing resources ");
            }
            try {
                if (outputstream != null) {
                    outputstream.close();
                }
            } catch (IOException e) {
                log.error("An I/O error occured while closing resources ");
            }
            if (process != null) {
                int errorCode = 0;
                try {
                    process.waitFor(); // error code is only certainly available after the process finished
                    errorCode = process.exitValue();
                    if (errorCode != 0) {
                        //could add here process.getErrorStream()
                        log.error( "sub process failed and exited with error code " + errorCode);
                    }
                } catch (IllegalThreadStateException ie) {
                    log.warn( "Process didn't exit yet, but should have exited already." + ie.getMessage()+ " "  + Logging.stackTrace(ie));
                } catch (InterruptedException e){
                    log.warn( "InterruptedException while waiting for proccess to die " + e.getMessage()+ " "  + Logging.stackTrace(e) );
                }
                process.destroy();
            }
        }
        // Cant do more checking then this, i think....
        tmp = (String) params.get("ImageConvert.ColorizeHexScale");
        if (tmp != null) {
            try {
                colorizeHexScale = Integer.parseInt(tmp);
            } catch (NumberFormatException e) {
                log.error( "Property ImageConvert.ColorizeHexScale should be an integer: " + e.toString() + "conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
            }
        }
        // See if the modulate scale base is defined. If not defined, it will be ignored.
        log.debug("Searching for ModulateScaleBase property.");
        tmp = (String) params.get("ImageConvert.ModulateScaleBase");
        if (tmp != null) {
            try {
                modulateScaleBase = Integer.parseInt(tmp);
            } catch (NumberFormatException nfe) {
                log.error( "Property ImageConvert.ModulateScaleBase should be an integer, instead of:'" + tmp + "'" + ", conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
                log.error("Ignoring modulateScaleBase property.");
                log.error(nfe.getMessage());
            }
        } else {
            log.debug(
            "ModulateScaleBase property not found, ignoring the modulateScaleBase.");
        }
    }
    
    private static class ParseResult {
        List args;
        String format;
        File cwd;
    }
    
    /**
     * This functions converts an image by the given parameters
     * @param input an array of <code>byte</code> which represents the original image
     * @param commands a <code>List</code> of <code>String</code>s containing commands which are operations on the image which will be returned.
     *                 ImageConvert.converterRoot and ImageConvert.converterCommand specifing the converter root....
     * @return an array of <code>byte</code>s containing the new converted image.
     *
     */
    public byte[] convertImage(byte[] input, List commands) {
        byte[] pict = null;
        if (commands != null && input != null) {
            ParseResult parsedCommands = getConvertCommands(commands);
            pict = convertImage( input, parsedCommands.args, parsedCommands.format, parsedCommands.cwd);
        }
        return pict;
    }
    
    /**
     * @deprecated Use convertImage
     */
    public byte[] ConvertImage(byte[] input, List commands) {
        return convertImage(input, commands);
    }
    
    /**
     * Translates MMBase color format (without #) to an convert color format (with or without);
     */
    protected String color(String c) {
        if (c.charAt(0) == 'X') {
            // the # was mentioned but replaced by X in ImageTag
            c = '#' + c.substring(1); // put it back.
        }
        if (c.length() == 6) {
            // obviously a little to simple now, because color names of 6 letters don't work now
            return "#" + c.toLowerCase();
        } else {
            return c.toLowerCase();
        }
    }
    
    /**
     * MMBase has some abreviations to convert commands, like 's' for 'geometry'. These are treated here.
     * @param a alias
     * @return actual convert parameter name for alias.
     */
    protected String getAlias(String a) {
        if (a.equals("s"))
            return "geometry";
        if (a.equals("r"))
            return "rotate";
        if (a.equals("c"))
            return "colors";
        if (a.equals("t"))
            return "transparent";
        if (a.equals("i"))
            return "interlace";
        if (a.equals("q"))
            return "quality";
        if (a.equals("mono"))
            return "monochrome";
        if (a.equals("highcontrast"))
            return "contrast";
        if (a.equals("flipx"))
            return "flop";
        if (a.equals("flipy"))
            return "flip";
        // I don't think that this makes any sense, I dia is not dianegative,
        // can be diapositive as well... But well, we are backwards compatible.
        if (a.equals("dia"))
            return "negate";
        return a;
        
    }
    
    /**
     * Translates the arguments for img.db to arguments for convert of ImageMagick.
     * @param params  List with arguments. First one is the image's number, which will be ignored.
     * @return        Map with three keys: 'args', 'cwd', 'format'.
     */
    private ParseResult getConvertCommands(List params) {
        StringBuffer cmdstr = new StringBuffer();
        
        ParseResult result = new ParseResult();
        List cmds = new Vector();
        result.args = cmds;
        result.cwd = null;
        result.format = defaultImageFormat;
        
        String key, type;
        String cmd;
        int pos, pos2;
        Iterator t = params.iterator();
        if (t.hasNext()) {
            t.next(); // first element is the number, ignore it.
        }
        while (t.hasNext()) {
            key = (String) t.next();
            pos = key.indexOf('(');
            pos2 = key.lastIndexOf(')');
            if (pos != -1 && pos2 != -1) {
                type = key.substring(0, pos).toLowerCase();
                cmd = key.substring(pos + 1, pos2);
                if (log.isDebugEnabled()) {
                    log.debug("getCommands(): type=" + type + " cmd=" + cmd);
                }
                // Following code translates some MMBase specific things to imagemagick's convert arguments.
                type = getAlias(type);
                // Following code will only be used when ModulateScaleBase builder property is defined.
                if (type.equals("modulate")
                && (modulateScaleBase != Integer.MAX_VALUE)) {
                    cmd = calculateModulateCmd(cmd, modulateScaleBase);
                } else if (type.equals("colorizehex")) {
                    // Incoming hex number rrggbb is converted to
                    // decimal values rr,gg,bb which are inverted on a scale from 0 to 100.
                    if (log.isDebugEnabled())
                        log.debug("colorizehex, cmd: " + cmd);
                    String hex = cmd;
                    // Check if hex length is 123456 6 chars.
                    if (hex.length() == 6) {
                        
                        // Byte.decode doesn't work correctly.
                        int r =
                        colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(0, 2), 16) / 255.0f);
                        int g =
                        colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(2, 4), 16) / 255.0f);
                        int b =
                        colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(4, 6), 16) / 255.0f);
                        if (log.isDebugEnabled()) {
                            log.debug("Hex is :" + hex);
                            log.debug( "Calling colorize with r:" + r + " g:" + g + " b:" + b);
                        }
                        type = "colorize";
                        cmd = r + "/" + g + "/" + b;
                    }
                } else if (type.equals("gamma")) {
                    StringTokenizer tok = new StringTokenizer(cmd, ",/");
                    String r = tok.nextToken();
                    String g = tok.nextToken();
                    String b = tok.nextToken();
                    cmd = r + "/" + g + "/" + b;
                } else if (
                type.equals("pen")
                || type.equals("transparent")
                || type.equals("fill")
                || type.equals("bordercolor")
                || type.equals("background")
                || type.equals("box")
                || type.equals("opaque")
                || type.equals("stroke")) {
                    // rather sucks, because we have to maintain manually which options accept a color
                    cmd = color(cmd);
                } else if (type.equals("text")) {
                    int firstcomma = cmd.indexOf(',');
                    int secondcomma = cmd.indexOf(',', firstcomma + 1);
                    type = "draw";
                    try {
                        cmd = "text " + cmd.substring(0, secondcomma) + " " + ((String) cmd).substring( secondcomma + 1).replace( '\'', '"');
                        cmd = new String(cmd.getBytes("UTF-8"), "ISO-8859-1");
                        // convert needs UTF-8, but Runtime seemingly always writes ISO-8859-1, so we
                        // are going to lie here.
                        
                        // even the value of this doesn't seem to matter
                        if (log.isDebugEnabled()) {
                            log.debug( "file.encoding: " + java.lang.System.getProperty( "file.encoding"));
                        }
                    } catch (java.io.UnsupportedEncodingException e) {
                        log.error(e.toString());
                    }
                } else if (type.equals("draw")) {
                    try {
                        cmd = new String(cmd.getBytes("UTF-8"), "ISO-8859-1");
                        // can be some text in the draw command
                    } catch (java.io.UnsupportedEncodingException e) {
                        log.error(e.toString());
                    }
                } else if (type.equals("font")) {
                    if (cmd.startsWith("mm:")) {
                        // recognize MMBase config dir, so that it is easy to put the fonts there.
                        cmd = org.mmbase.module.core.MMBaseContext.getConfigPath()+ File.separator + cmd.substring(3);
                    }
                    File fontFile = new File(cmd);
                    if (!fontFile.isFile()) {
                        // if not pointed to a normal file, then set the cwd to <config>/fonts where you can put a type.mgk
                        File fontDir =
                        new File( org.mmbase.module.core.MMBaseContext.getConfigPath(),"fonts");
                        if (fontDir.isDirectory()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Using " + fontDir + " as working dir for conversion. A 'type.mgk' (see ImageMagick documentation) can be in this dir to define fonts");
                            }
                            result.cwd = fontDir;
                        } else {
                            log.debug(
                            "Using named font without MMBase 'fonts' directory, using ImageMagick defaults only");
                        }
                    }
                    
                } else if (type.equals("circle")) {
                    type = "draw";
                    cmd = "circle " + cmd;
                } else if (type.equals("part")) {
                    StringTokenizer tok = new StringTokenizer(cmd, "x,\n\r");
                    try {
                        int x1 = Integer.parseInt(tok.nextToken());
                        int y1 = Integer.parseInt(tok.nextToken());
                        int x2 = Integer.parseInt(tok.nextToken());
                        int y2 = Integer.parseInt(tok.nextToken());
                        type = "crop";
                        cmd = (x2 - x1) + "x" + (y2 - y1) + "+" + x1 + "+" + y1;
                    } catch (Exception e) {
                        log.error(e.toString());
                    }
                } else if (type.equals("roll")) {
                    StringTokenizer tok = new StringTokenizer(cmd, "x,\n\r");
                    String str;
                    int x = Integer.parseInt(tok.nextToken());
                    int y = Integer.parseInt(tok.nextToken());
                    if (x >= 0)
                        str = "+" + x;
                    else
                        str = "" + x;
                    if (y >= 0)
                        str += "+" + y;
                    else
                        str += "" + y;
                    cmd = str;
                } else if (type.equals("f")) {
                    result.format = cmd;
                    continue; // ignore this one, don't add to cmds.
                }
                if (log.isDebugEnabled()) {
                    log.debug("adding " + type + " " + cmd);
                }
                // all other things are recognized as well..
                if (! isCommandPrefix(type.charAt(0))) { // if no prefix given, suppose '-'
                    cmds.add("-" + type);
                } else {
                    cmds.add(type);
                }
                cmds.add(cmd);
                
            } else {
                key = getAlias(key);
                if (key.equals("lowcontrast")) {
                    cmds.add("+contrast");
                } else if (key.equals("neg")) {
                    cmds.add("+negate");
                } else {
                    if (! isCommandPrefix(key.charAt(0))) { // if no prefix given, suppose '-'
                        cmds.add("-" + key);
                    } else {
                        cmds.add(key);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * @since MMBase-1.7
     */
    private boolean isCommandPrefix(char c) {
        return c == '-' || c == '+';
    }
    
    /**
     * Calculates the modulate parameter values (brightness,saturation,hue) using a scale base.
     * ImageMagick's convert command changed its modulate scale somewhere between version v4.2.9 and v5.3.8.<br />
     * In version 4.2.9 the scale ranges from -100 to 100.<br />
     * (relative, eg. 20% higher, value is 20, 10% lower, value is -10).<br />
     * In version 5.3.8 the scale ranges from 0 to 100.<br />
     * (absolute, eg. 20% higher, value is 120, 10% lower, value is 90).<br />
     * Now, for different convert versions the scale range can be corrected with the scalebase. <br />
     * The internal scale range that's used will be from -100 to 100. (eg. modulate 20,-10,0).
     * With the base you can change this, so for v4.2.9 scalebase=0 and for v5.3.9 scalebase=100.
     * @param cmd modulate command string
     * @param scaleBase the scale base value
     * @return the transposed modulate command string.
     */
    private String calculateModulateCmd(String cmd, int scaleBase) {
        log.debug( "Calculating modulate cmd using scale base " + scaleBase + " for modulate cmd: " + cmd);
        String modCmd = "";
        StringTokenizer st = new StringTokenizer(cmd, ",/");
        while (st.hasMoreTokens())
            modCmd += scaleBase + Integer.parseInt(st.nextToken()) + ",";
        if (!modCmd.equals(""))
            modCmd = modCmd.substring(0, modCmd.length() - 1);
        // remove last ',' char.
        log.debug("Modulate cmd after calculation: " + modCmd);
        return modCmd;
    }
    
    /**
     * Does the actual conversion.
     *
     * @param pict Byte array with the original picture
     * @param cmd  List with convert parameters.
     * @param format The picture format to output to (jpg, gif etc.).
     * @return      The result of the conversion (a picture).
     *
     */
    private byte[] convertImage(
    byte[] pict,
    List cmd,
    String format,
    File cwd) {
        cmd.add(0, "-");
        cmd.add(0, converterPath);
        cmd.add(format + ":-");
        
        String command = cmd.toString(); // only for debugging.
        
        if (log.isDebugEnabled()) {
            log.debug( "command:" + command + " in " + new File("").getAbsolutePath());
        }
        
        Process process = null;
        InputStream in = null;
        ByteArrayOutputStream imagestream = null;
        
        try {
            log.debug("starting program");
            
            if (cwd != null) {
                // using MAGICK_HOME for mmbase config/fonts if 'font' option used (can put type.mgk)
                String[] env = new String[1];
                env[0] = "MAGICK_HOME=" + cwd.toString();
                if (log.isDebugEnabled()) {
                    log.debug("MAGICK_HOME " + env[0]);
                }
                process =
                Runtime.getRuntime().exec(
                (String[]) cmd.toArray(new String[0]),
                env);
            } else {
                process =
                Runtime.getRuntime().exec(
                (String[]) cmd.toArray(new String[0]));
            }
            
            
            if (pict.length >0){
                ProcessWriter pw = new ProcessWriter(new ByteArrayInputStream(pict), process.getOutputStream());
                log.debug("starting process writer");
                pw.start();
            } else {
                log.warn("trying to convert empty byte[] (no picute data)");
            }
            
            // in grabs the stuff coming from stdout from program...
            in = new BufferedInputStream(process.getInputStream(), 1);
            imagestream = new ByteArrayOutputStream();
            
            {
                int size = 0;
                byte[] inputbuffer = new byte[2048];
                while ((size = in.read(inputbuffer)) != -1) {
                    log.debug("read a chunk");
                    imagestream.write(inputbuffer, 0, size);
                }
            }
            in.close();
            
            log.debug("waiting");
            process.waitFor(); // error code is only certainly available after the process finished
            
            
            log.debug("retrieved all information");
            byte[] image = imagestream.toByteArray();
            
            if (image.length < 1) {
                // No bytes in the image -
                // ImageMagick failed to create a proper image.
                // return null so this image is not by accident stored in the database
                
                // What if the _original_ image was not a proper image?
                // in that case it is perhaps better to store an invalid icache too, otherwise
                // the failure will be repeated
                
                log.error("Imagemagick conversion did not succeed. Returning null.");
                return null;
            } else {
                // print some info and return....
                if (log.isServiceEnabled()) {
                    log.service("converted image(#" + pict.length + " bytes)  to '" + format +
                    "'-image(#" + image.length + " bytes)('" + command + "')");
                }
                return image;
            }
        } catch (InterruptedIOException e) {
            log.error("converting image with command: '" + command +
            "' failed  with reason: '" + e.getMessage() + "'");
        } catch (IOException e) {
            log.error("converting image with command: '" + command +
            "' failed  with reason: '" + e.getMessage() + "'");
            log.error(Logging.stackTrace(e));
        } catch (Exception exception) {
            log.error("converting image with command: '" + command + "' failed  with reason: '" + exception + "'");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
            }
            try {
                if (imagestream != null) {
                    imagestream.close();
                }
            } catch (IOException ioe) {
            }
            
            if (process != null) {
                int errorCode = 0;
                try {
                    errorCode = process.exitValue();
                    if (errorCode != 0) {
                        log.error("sub process failed and exited with error code " + errorCode);
                        
                        InputStream err = null;
                        ByteArrayOutputStream errorStream = null;
                        try {
                            err = process.getErrorStream();
                            
                            errorStream = new ByteArrayOutputStream();
                            int size = 0;
                            byte[] inputbuffer = new byte[2048];
                            while ((size = err.read(inputbuffer)) != -1) {
                                log.debug( "copying " + size + " bytes from ERROR-stream ");
                                errorStream.write(inputbuffer, 0, size);
                            }
                            
                            byte[] errorMessage = errorStream.toByteArray();
                            
                            if (errorMessage.length > 0) {
                                log.error( "From stderr with command '" + command + "' in '" + new File("").getAbsolutePath() + "'  --> '" + new String(errorMessage) + "'");
                            } else {
                                log.debug("No information on stderr found");
                            }
                        } catch (IOException e) {
                            //don't know what to say here. Errors already in log, but not a detaild one.of the sub process
                        } finally {
                            try {
                                if (err != null) {
                                    err.close();
                                }
                            } catch (IOException e) {
                                //don't know what to say here. Errors already in log
                            }
                            try {
                                if (errorStream != null) {
                                    errorStream.close();
                                }
                            } catch (IOException e) {
                                //don't know what to say here. Errors already in log
                            }
                        }
                    }
                } catch (IllegalThreadStateException ie) {
                    log.warn("Process didn't exit yet, but should have exited already." + ie);
                    //             } catch (InterruptedException ine) {
                    // log.warn("convert process was interrupted " + ine);
                }
                
                process.destroy();
            }
        }
        
        return null;
    }
}
