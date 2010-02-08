/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.util.images;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mmbase.util.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.logging.*;

/**
 * Converts images using ImageMagick.
 *
 * @author Rico Jansen
 * @author Michiel Meeuwissen
 * @author Nico Klasens
 * @author Jaco de Groot
 * @version $Id$
 */
public class ImageMagickImageConverter extends AbstractImageConverter implements ImageConverter {
    private static final Logger log = Logging.getLoggerInstance(ImageMagickImageConverter.class);

    static final Pattern IM_VERSION_PATTERN = Pattern.compile("(?is)(.*)\\s(\\d+)\\.(\\d+)\\.(\\d+)(-[0-9]+)?\\s.*");
    private static final Pattern IM_FORMAT_PATTERN  = Pattern.compile("(?is)\\s*([A-Z0-9]+)\\*?\\s+[A-Z0-9]*\\s*[r\\-]w[\\+\\-]\\s+.*");


    private String program = "ImageMagick";
    private int imVersionMajor = 5;
    private int imVersionMinor = 5;
    private int imVersionPatch = 0;

    private String converterPath = "convert"; // in the path.

    private  int colorizeHexScale = 100;
    // The modulate scale base holds the builder property to specify the scalebase.
    // If ModulateScaleBase property is not defined, then value stays max int.
    private int modulateScaleBase = Integer.MAX_VALUE;

    // private static String CONVERT_LC_ALL= "LC_ALL=en_US.UTF-8"; I don't know how to change it.

    public static final int METHOD_LAUNCHER  = 1;
    public static final int METHOD_CONNECTOR = 2;
    protected int method = METHOD_LAUNCHER;
    protected String host = "localhost";
    protected int port = 1679;

    protected Set<String> excludeFormats = new TreeSet<String>();
    final Set<String> validFormats = new TreeSet<String>();

    private OutputStream getOutput(String... args) {
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        switch (method) {
        case METHOD_LAUNCHER: {
            log.debug("Starting convert");
            List<String> cmd = new ArrayList<String>();
            for (String arg : args) {
                cmd.add(arg);
            }
            try {

                CommandLauncher launcher = new CommandLauncher("ConvertImage");
                launcher.execute(converterPath, cmd.toArray(EMPTY));
                launcher.waitAndRead(outputStream, errorStream);
            } catch (ProcessException e) {
                log.error("Convert test failed. " + converterPath + cmd + " (" + e.getMessage() + ")");
            }
            break;
        }
        case METHOD_CONNECTOR: {
            try {
                java.net.Socket socket = new java.net.Socket(host, port);
                final OutputStream os = socket.getOutputStream();
                os.write(0); // version
                final ObjectOutputStream stream = new ObjectOutputStream(os);
                List<String> cmd = new ArrayList<String>();
                cmd.add(converterPath);
                for (String arg : args) {
                    cmd.add(arg);
                }
                stream.writeObject((cmd.toArray(EMPTY)));
                stream.writeObject(EMPTY);
                Copier copier = new Copier(new ByteArrayInputStream(new byte[0]), os, ".file -> socket");
                log.debug("Executing " + copier);
                org.mmbase.util.ThreadPools.jobsExecutor.execute(copier);

                Copier copier2 = new Copier(socket.getInputStream(), outputStream, ";socket -> cout");
                org.mmbase.util.ThreadPools.jobsExecutor.execute(copier2);
                log.debug("Waitting for " + copier);
                copier.waitFor();
                log.debug("Ready 1");
                socket.shutdownInput();
                socket.shutdownOutput();
                log.debug("Now waiting for 2");
                copier2.waitFor();
                log.debug("Ready 2");
                socket.close();
                log.debug("Ready");
            } catch (Exception ioe) {
                log.error("" + host + ":" + port);
                try {
                    errorStream.write(("" + host + ":" + port).getBytes());
                    errorStream.flush();
                } catch (Exception e) {
                    log.error(e);
                }
            }
            break;
        }
        default: log.error("unknown method " + method);
        }
        return outputStream;

    }


    /**
     * This function initializes this class
     * @param params a <code>Map</code> of <code>String</code>s containing information, this should contain the key's
     *               ImageConvert.ConverterRoot and ImageConvert.ConverterCommand specifying the converter root, and it can also contain
     *               ImageConvert.DefaultImageFormat which can also be 'asis'.
     */
    @Override
    public void init(Map<String, String> params) {
        String converterRoot = "";
        String converterCommand = "convert";

        String tmp;
        tmp =  params.get("ImageConvert.ConverterRoot");
        if (tmp != null && ! tmp.equals("")) {
            converterRoot = tmp;
        }

        tmp =  params.get("ImageConvert.ConverterCommand");
        if (tmp != null && ! tmp.equals("")) {
            converterCommand = tmp;
        }

        tmp = params.get("ImageConvert.Host");
        if (tmp != null && ! tmp.equals("")) {
            host = tmp;
        }
        tmp = params.get("ImageConvert.Port");
        if (tmp != null && ! tmp.equals("")) {
            port = Integer.parseInt(tmp);
        }

        tmp = params.get("ImageConvert.ExcludeFormats");
        if (tmp != null && ! tmp.equals("")) {
            StringTokenizer tokenizer = new StringTokenizer(tmp, " ,");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                excludeFormats.add(token);
            }
        }

        tmp = params.get("ImageConvert.Method");
        if (tmp != null && ! tmp.equals("")) {
            if (tmp.equals("launcher")) {
                method = METHOD_LAUNCHER;
            } else if (tmp.equals("connector")) {
                method = METHOD_CONNECTOR;
                log.info("Will connect to " + host + ":" + port + " to convert images");
            } else {
                log.error("Unknown imageconvert method " + tmp);
            }
        }

        if(System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows")) {
            // on the windows system, we _can_ assume the it uses .exe as extention...
            // otherwise the check on existance of the program will fail.
            if (!converterCommand.endsWith(".exe")) {
                converterCommand += ".exe";
            }
        }

        String configFile = params.get("configfile");
        if (configFile == null) configFile = "images builder xml";

        converterPath = converterCommand; // default.
        if (!converterRoot.equals("")) { // also a root was indicated, add it..
            // now check if the specified ImageConvert.converterRoot does exist and is a directory
            File checkConvDir = new File(converterRoot).getAbsoluteFile();
            if (!checkConvDir.exists()) {
                log.error( "ImageConvert.ConverterRoot " + converterRoot + " in " + configFile + " does not exist (using any way)");
            } else if (!checkConvDir.isDirectory()) {
                log.error( "ImageConvert.ConverterRoot " + converterRoot + " in " + configFile + " is not a directory");
            } else {
                // now check if the specified ImageConvert.Command does exist and is a file..
                File checkConvCom = new File(converterRoot, converterCommand);
                converterPath = checkConvCom.toString();
                if (!checkConvCom.exists()) {
                    log.error( converterPath + " specified by " + configFile + "  does not exist");
                } else if (!checkConvCom.isFile()) {
                    log.error( converterPath + " specified by " + configFile + "  is not a file");
                }
            }
        }
        // do a test-run, maybe slow during startup, but when it is done this way, we can also output some additional info in the log about version..
        // and when somebody has failure with converting images, it is much earlier detectable, when it wrong in settings, since it are settings of
        // the builder...

        // TODO: on error switch to Dummy????
        // TODO: research how we tell convert, that is should use the System.getProperty(); with respective the value's 'java.io.tmpdir', 'user.dir'
        //       this, since convert writes at this moment inside the 'user.dir'(working dir), which isnt writeable all the time.


        {
            String imOutput = getOutput("-version").toString();
            Matcher m = IM_VERSION_PATTERN.matcher(imOutput);
            if (m.matches()) {
                String p = m.group(1);
                imVersionMajor = Integer.parseInt(m.group(2));
                imVersionMinor = Integer.parseInt(m.group(3));
                imVersionPatch = Integer.parseInt(m.group(4));
                if (p.indexOf("GraphicsMagick") >= 0) {
                    log.service("Found GraphicsMagick version " + imVersionMajor + "." + imVersionMinor + "." + imVersionPatch);
                    imVersionMajor += 5; // I have no freaking idea
                    log.service("Supposing that that is equivalent to ImageMagick version " + imVersionMajor + "." + imVersionMinor + "." + imVersionPatch);
                    program = "GraphicsMagick";
                } else {
                    log.service("Found ImageMagick version " + imVersionMajor + "." + imVersionMinor + "." + imVersionPatch);
                }

            } else {
                log.error( "converter from location " + converterPath + ", gave strange result: " + imOutput
                           + "conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'. (Doesn't match " + IM_VERSION_PATTERN + ")");
                log.info("Supposing ImageMagick version " + imVersionMajor + "." + imVersionMinor + "." + imVersionPatch);

            }
        }


        for (String imLine : getOutput("-list", "format").toString().split("\n")) {
            Matcher m = IM_FORMAT_PATTERN.matcher(imLine);
            if (m.matches()) {
                String format = m.group(1);
                validFormats.add(format.toUpperCase());
            }
        }


        // Cant do more checking then this, i think....
        tmp = params.get("ImageConvert.ColorizeHexScale");
        if (tmp != null) {
            try {
                colorizeHexScale = Integer.parseInt(tmp);
            } catch (NumberFormatException e) {
                log.error( "Property ImageConvert.ColorizeHexScale should be an integer: " + e.toString() + "conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
            }
        }
        // See if the modulate scale base is defined. If not defined, it will be ignored.
        log.debug("Searching for ModulateScaleBase property.");
        tmp = params.get("ImageConvert.ModulateScaleBase");
        if (tmp != null) {
            try {
                modulateScaleBase = Integer.parseInt(tmp);
            } catch (NumberFormatException nfe) {
                log.error( "Property ImageConvert.ModulateScaleBase should be an integer, instead of:'" + tmp + "'" + ", conv.root='" + converterRoot + "' conv.command='" + converterCommand + "'");
                log.error("Ignoring modulateScaleBase property.");
                log.error(nfe.getMessage());
            }
        } else {
            log.debug("ModulateScaleBase property not found, ignoring the modulateScaleBase.");
        }
        log.info("Found " + program + " supported formats " + validFormats + ". Using " + this);
    }

    private static class ParseResult {
        final List<String> args = new ArrayList<String>();
        String format;
        File cwd;
        final Set<File> temporaryFiles = new HashSet<File>();
    }

    /**
     *  Test whether the current version of ImageMagick is at least a certain version.
     *  @param major minimum major version number
     *  @param minor minimum minor version number within the major version
     *  @param patch minimum patch version number within the minor version
     *  @return <code>true</code> if the version is equal to or later than major.minor.patch
     *  @since MMBase-1.9.2
     */
    public boolean isMinimumVersion(int major, int minor, int patch) {
        return (imVersionMajor > major) ||
               ((imVersionMajor == major) &&
                ((imVersionMinor > minor) ||
                  ((imVersionMinor == minor) && (imVersionPatch >= patch))
                )
               );
    }

    /**
     * This functions converts an image by the given parameters
     * @param input an array of <code>byte</code> which represents the original image
     * @param sourceFormat original image format
     * @param commands a <code>List</code> of <code>String</code>s containing commands which are operations on the image which will be returned.
     *                 ImageConvert.converterRoot and ImageConvert.converterCommand specifying the converter root....
     * @return The number of bytes produces
     */
    @Override
    public long convertImage(InputStream input, String sourceFormat, OutputStream out, List<String> commands) throws IOException {
        if (input == null) {
            log.error("Converting an empty image does not make sense.");
            return -1;
        }
        if (sourceFormat != null && excludeFormats.contains(sourceFormat)) {
            log.debug("Conversion is excluded for image format: " + sourceFormat);
            return -1;
        }

        SerializableInputStream in = Casting.toSerializableInputStream(input);

        if (in.getSize() <= 0) {
            log.debug("Nothing to convert");
            return -1;
        }

        long result;
        if (commands != null && !commands.isEmpty()) {
            ParseResult parsedCommands = getConvertCommands(commands);
            if (parsedCommands.format.equals("asis") && sourceFormat != null) {
                parsedCommands.format = sourceFormat;
            }
            if (log.isDebugEnabled()) {
                log.debug("Converting image (" + in.getSize() + " bytes)  to '" + parsedCommands.format + "' ('" + parsedCommands.args + "') with cwd = " + parsedCommands.cwd);
            }
            if ("gif".equals(parsedCommands.format)) {
                if (isAnimated(in)) {
                    parsedCommands.args.add(0, "-coalesce");
                }
                in.reset();
            }
            log.service("Now converting " + commands + " " + in + " to " + out.getClass() + " " + out);
            result = convertImage(in, out, sourceFormat, parsedCommands.args, parsedCommands.format, parsedCommands.cwd);
            for (File tempFile : parsedCommands.temporaryFiles) {
                try {
                    tempFile.delete();
                } catch (Exception e) {
                }
            }
        } else {
            log.error("Not Converting with empty commands.");
            log.error(Logging.stackTrace());
            result = -1;
        }
        return result;
    }

    protected boolean isAnimated(InputStream inp) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setDetermineImageNumber(true);
        imageInfo.setInput(inp);
        imageInfo.check();
        return (imageInfo.getNumberOfImages() > 1);
    }

    /**
     * Translates MMBase color format (without #) to an convert color format (with or without);
     * @param c color to convert
     * @return converted color
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
     * Translates the arguments for img.db to arguments for convert of ImageMagick.
     * @param params  List with arguments. First one is the image's number, which will be ignored.
     * @return        Map with three keys: 'args', 'cwd', 'format'.
     */
    private ParseResult getConvertCommands(List<String> params) {
        if (log.isDebugEnabled()) {
            log.debug("getting convert commands from " + params);
        }
        ParseResult result = new ParseResult();
        List<String> cmds = result.args;
        result.cwd = null;

        for (String key : params) {
            if (log.isDebugEnabled()) log.debug("parsing '" + key + "'");
            if (key == null) continue;
            int pos = key.indexOf('(');
            int pos2 = key.lastIndexOf(')');
            if (pos != -1 && pos2 != -1) {
                String type = key.substring(0, pos).toLowerCase();
                String cmd = key.substring(pos + 1, pos2);
                if (log.isDebugEnabled()) {
                    log.debug("getCommands(): type=" + type + " cmd=" + cmd);
                }
                // Following code translates some MMBase specific things to imagemagick's convert arguments.
                type = Imaging.getAlias(type);
                // Following code will only be used when ModulateScaleBase builder property is defined.
                if (type.equals("modulate") && (modulateScaleBase != Integer.MAX_VALUE)) {
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
                        int r = colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(0, 2), 16) / 255.0f);
                        int g = colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(2, 4), 16) / 255.0f);
                        int b = colorizeHexScale - Math.round( colorizeHexScale * Integer.parseInt( hex.substring(4, 6), 16) / 255.0f);
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
                    if (isMinimumVersion(6,0,0)) {
                        cmds.add("-encoding");
                        cmds.add("unicode");
                        cmds.add("-annotate");
                        try {
                            File tempFile = File.createTempFile("mmbase_image_text_", null);
                            tempFile.deleteOnExit();
                            FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile);
                            Encode encoder = new Encode("ESCAPE_SINGLE_QUOTE");
                            String text =  cmd.substring(secondcomma + 1);
                            log.debug("Using '" + text + "'");
                            tempFileOutputStream.write(encoder.decode(text.substring(1, text.length() - 1)).getBytes("UTF-8"));
                            tempFileOutputStream.close();
                            cmds.add("+" + cmd.substring(0, firstcomma) + "+" + cmd.substring(firstcomma + 1, secondcomma));
                            cmds.add("@" + tempFile.getPath());
                            result.temporaryFiles.add(tempFile);
                        } catch (IOException e) {
                            log.error("Could not create temporary file for text: " + e.toString());
                            cmd =  cmd.substring(0, secondcomma) + " 'Could not create temporary file for text.'";
                        }
                        continue;
                    } else {
                        type = "draw";
                        try {
                            File tempFile = File.createTempFile("mmbase_image_text_", null);
                            tempFile.deleteOnExit();
                            Encode encoder = new Encode("ESCAPE_SINGLE_QUOTE");
                            String text = cmd.substring(secondcomma + 1);
                            FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile);
                            tempFileOutputStream.write(encoder.decode(text.substring(1, text.length() - 1)).getBytes("UTF-8"));
                            tempFileOutputStream.close();
                            cmd = "text " + cmd.substring(0, secondcomma) + " '@" + tempFile.getPath() + "'";
                            result.temporaryFiles.add(tempFile);
                        } catch (IOException e) {
                            log.error("Could not create temporary file for text: " + e.toString());
                            cmd = "text " + cmd.substring(0, secondcomma) + " 'Could not create temporary file for text.'";
                        }
                    }
                } else if (type.equals("draw")) {
                    //try {
                        //cmd = new String(cmd.getBytes("UTF-8"), "ISO-8859-1");
                        // can be some text in the draw command
                    //} catch (java.io.UnsupportedEncodingException e) {
                    //   log.error(e.toString());
                    //}
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
                                log.debug("Using " + fontDir + " as working dir for conversion. A 'type.mgk' (see " + program + " documentation) can be in this dir to define fonts");
                            }
                            result.cwd = fontDir;
                        } else {
                            log.debug("Using named font without MMBase 'fonts' directory, using " + program + " defaults only");
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
                    if (! (cmd.equals("asis") && result.format != null)) {
                        result.format = cmd;
                    }
                    continue; // ignore this one, don't add to cmds.
                }
                if (log.isDebugEnabled()) {
                    log.debug("adding " + type + " " + cmd);
                }
                // all other things are recognized as well..
                if (! isCommandPrefixed(type)) { // if no prefix given, suppose '-'
                    cmds.add("-" + type);
                } else {
                    cmds.add(type);
                }
                cmds.add(cmd);
                if (type.equals("crop") && (isMinimumVersion(6,0,5))) {
                    // +repage is shortcut for -page +0+0
                    // (http://www.mail-archive.com/lilypond-devel@gnu.org/msg24065.html),
                    // but works in graphicsmagick too.
                    cmds.add("-page");
                    cmds.add("+0+0");
                }
            } else {
                key = Imaging.getAlias(key);
                if (key.equals("lowcontrast")) {
                    cmds.add("+contrast");
                } else if (key.equals("neg")) {
                    cmds.add("+negate");
                } else {
                    if (! isCommandPrefixed(key)) { // if no prefix given, suppose '-'
                        cmds.add("-" + key);
                    } else {
                        cmds.add(key);
                    }
                }
            }
        }
        if (result.format == null)  result.format = Factory.getDefaultImageFormat();
        return result;
    }

    /**
     * Is command prefixed with '-' or '+'
     * @param s command
     * @return <code>true</code> when prefixed
     * @since MMBase-1.7
     */
    private boolean isCommandPrefixed(String s) {
        if (s == null || s.length() == 0) return false;
        char c = s.charAt(0);
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
        while (st.hasMoreTokens()) {
            modCmd += scaleBase + Integer.parseInt(st.nextToken()) + ",";
        }
        if (!modCmd.equals("")) {
            modCmd = modCmd.substring(0, modCmd.length() - 1);
        }
        // remove last ',' char.
        log.debug("Modulate cmd after calculation: " + modCmd);
        return modCmd;
    }

    /**
     * Does the actual conversion.
     *
     * @param originalStream Byte stream with the original picture
     * @param imageStream output stream with converted image bytes
     * @param cmd  List with convert parameters.
     * @param format The picture format to output to (jpg, gif etc.).
     * @param cwd Directory for fonts
     */
    private long convertImage(InputStream originalStream, OutputStream imageStream, String sourceFormat, List<String> cmd, String format, File cwd) {

        cmd.add(0, "-");
        cmd.add(0, converterPath);
        if (! validFormats.contains(format.toUpperCase())) {
            String fallBackFormat = Factory.getDefaultImageFormat();
            if ("asis".equals(fallBackFormat)) {
                if (validFormats.contains(sourceFormat.toUpperCase())) {
                    fallBackFormat = sourceFormat;
                } else {
                    fallBackFormat = "jpg";
                }
            }
            log.warn("format '" + format + "' is not supported (" + validFormats + ") falling back to " + fallBackFormat);
            format = fallBackFormat;
        }
        cmd.add(format+ ":-");
        String command = cmd.toString(); // only for debugging.

        log.service("" + this + " executing " + command + " on " + originalStream);



        Map<String, String> envMap;
        if (cwd != null) {
            // using MAGICK_HOME for mmbase config/fonts if 'font' option used (can put type.mgk)
            envMap = new HashMap<String, String>();
            envMap.putAll(System.getenv());

            envMap.put("MAGICK_HOME", cwd.toString());
            if (log.isDebugEnabled()) {
                log.debug("MAGICK_HOME " + cwd.toString());
            }
        } else {
            envMap = System.getenv();
        }

        String[] env = new String[envMap.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            env[i++] = entry.getKey() + "=" + entry.getValue();
        }

        LoggerWriter writer = new LoggerWriter(log, Level.ERROR, "'" + command + "'");
        OutputStream errorStream = new WriterOutputStream(writer, "ISO-8859-1");

        long result;
        try {
            switch(method) {
            case METHOD_LAUNCHER:
                result = launcherConvertImage(cmd, env, originalStream, imageStream, errorStream);
                break;
            case METHOD_CONNECTOR:
                result = connectorConvertImage(cmd, env, originalStream, imageStream, errorStream);
                break;
            default: log.error("unknown method " + method);
                result = 0;
            }
            writer.close();

            log.debug("retrieved all information");
            if (writer.getCount() == 0) {
                // print some info and return....
                if (log.isServiceEnabled()) {
                    log.service("converted ('" + command + "') using " + this);
                }
            }
        } catch (Exception e) {
            log.error("converting image with command: '" + command + "' failed  with reason: '" + e.getMessage() + "'"  + errorStream.toString());
            result = 0;
        } finally {
            try {
                if (originalStream != null) {
                    originalStream.close();
                }
            } catch (IOException ioe) {
            }
            try {
                if (imageStream != null) {
                    imageStream.close();
                }
            } catch (IOException ioe) {
            }
        }
        return result;

    }

    protected long launcherConvertImage(List<String> cmd, String[] env, InputStream originalStream, OutputStream imageStream, OutputStream errorStream) throws ProcessException {
        CommandLauncher launcher = new CommandLauncher("ConvertImage");
        launcher.execute(cmd.toArray(new String[0]), env);
        ProcessClosure reader = launcher.waitAndWrite(originalStream, imageStream, errorStream);
        return reader.getCount();
    }

    // copy job
    public static class Copier implements Runnable {
        private volatile boolean ready;
        private long count = 0;
        private final InputStream in;
        private final OutputStream out;
        private final String name;
        public  boolean debug = false;

        public Copier(InputStream i, OutputStream o, String n) {
            in = i; out = o; name = n;
        }
        public void run() {
            log.debug("Executing " + this);
            try {
                count = IOUtil.copy(in, out);
            } catch (Throwable t) {
                System.err.println("Connector " + toString() +  ": " + t.getClass() + " " + t.getMessage());
            }
            log.debug("Ready" + this);
            synchronized(this) {
                ready = true;
                notifyAll();
            }
        }
        public  boolean ready() {
            return ready;
        }
        public void  waitFor() throws InterruptedException {
            synchronized(this) {
                while (! ready) {
                    wait();
                }
            }
        }
        public String toString() {
            return name;
        }
        public long getCount() {
            return count;
        }

    }


    private final String[] EMPTY = new String[] {};
    protected long connectorConvertImage(List<String> cmd, String[] env, InputStream originalStream, OutputStream imageStream, OutputStream errorStream) throws java.net.UnknownHostException, IOException, InterruptedException   {
        try {
            java.net.Socket socket = new java.net.Socket(host, port);
            final OutputStream os = socket.getOutputStream();
            os.write(0); // version
            final ObjectOutputStream stream = new ObjectOutputStream(os);
            stream.writeObject((cmd.toArray(EMPTY)));
            stream.writeObject(env);

            Copier copier = new Copier(originalStream, os, ".file -> socket");
            org.mmbase.util.ThreadPools.jobsExecutor.execute(copier);

            Copier copier2 = new Copier(socket.getInputStream(), imageStream, ";socket -> cout");
            org.mmbase.util.ThreadPools.jobsExecutor.execute(copier2);

            copier.waitFor();
            log.debug("Ready copying stuff to socket");
            originalStream.close();
            socket.shutdownOutput();
            log.debug("Waiting for response");
            copier2.waitFor();
            socket.close();
            return copier2.getCount();
        } catch (IOException ioe) {
            log.error("" + host + ":" + port);
            errorStream.write(("" + host + ":" + port).getBytes());
            errorStream.flush();
            throw ioe;
        }

    }

    public String toString() {
        return super.toString() + " " + converterPath + " (version " + imVersionMajor + "." + imVersionMinor + "." + imVersionPatch + ")";
    }

    public static void main(String[] args) throws Exception {
        String s = new BufferedReader(new InputStreamReader((System.in))).readLine();
        Matcher m = IM_VERSION_PATTERN.matcher(s);
        if (m.matches()) {
            System.out.println("Imagemagick version " + m.group(1) + " " + m.group(2) + " " + m.group(3));
        } else {
            System.out.println("Could not find");
        }
    }

}
