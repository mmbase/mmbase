 /*

 This software is OSI Certified Open Source Software.
 OSI Certified is a certification mark of the Open Source Initiative.

 The license (Mozilla version 1.0) can be read at the MMBase site.
 See http://www.MMBase.org/license
 */

package org.mmbase.applications.media;

import java.util.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.DocumentReader;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.module.core.MMBaseContext;
import org.w3c.dom.Element;


/**
 * Makes the 'Format' constants available.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 * @since MMBase-1.7
 */
public enum Format {

    UNKNOWN(0),
    MP3(1),
    RA(2),
    WAV(3),
    PCM(4),
    MP2(5),
    RM(6),
    VOB(7),
    AVI(8),
    MPEG(9),
    MP4(10),
    MPG(11),
    ASF(12),
    MOV (13),
    WMA (14),
    OGG (15),
    OGM (16),
    RAM (17),
    WMP (18),
    HTML (19),
    SMIL (20),
    QT   (21),    /* more windows media types */
    ASX  (22),
    WAX  (23),
    WMV  (24),
    WVX  (25),
    WM   (26),
    WMX  (27),
    WMZ  (28),
    WMD  (29),
    MID  (30),
    OGV  (31),
    OGA  (32),
    WEBM (40),
    PODCAST(50),
    VODCAST(51),
    M4A(60),
    M4V(61),
    TS(62),
    GGP(70),
    FLASH(80),
    FLV(81),      /* image formats */
    JPG(100),
    JPEG(101),
    GIF(102),
    BMP(103),
    PNG(104),
    TIFF(105),    /* a few more audio formats */
    AAC(200),
    FLAC(201),    /* others */
    M3U(501),
    M3U8(502),
    ANY(10000);


    private static Logger log = Logging.getLoggerInstance(Format.class);

    public final static String RESOURCE = "org.mmbase.applications.media.resources.formats";
    public static final String PUBLIC_ID_MIMEMAPPING_1_0 = "-//MMBase//DTD mimemapping config 1.0//EN";
    public static final String DTD_MIMEMAPPING_1_0       = "mimemapping_1_0.dtd";


    // in case you want i18ed format strings.

    private static Map<String, MimeType> mimeMapping = null;
    static {

        org.mmbase.util.xml.EntityResolver.registerPublicID(PUBLIC_ID_MIMEMAPPING_1_0, DTD_MIMEMAPPING_1_0, Format.class);

        String mimeMappingFile = "media/mimemapping.xml";
        readMimeMapping(mimeMappingFile);
        ResourceWatcher watcher = new ResourceWatcher() {
                public void onChange(String file) {
                    readMimeMapping(file);
                }
            };
        watcher.add(mimeMappingFile);
        watcher.start();

    }

    static void readMimeMapping(String mimeMappingFile) {
        mimeMapping = new HashMap<String, MimeType>();


        log.service("Reading " + mimeMappingFile);
        try {
            DocumentReader reader = new DocumentReader(ResourceLoader.getConfigurationRoot().getDocument(mimeMappingFile, DocumentReader.validate(), Format.class));

            for(Element map:reader.getChildElements("mimemapping", "map")) {
                String format = reader.getElementAttributeValue(map, "format");
                String codec = reader.getElementAttributeValue(map, "codec");
                String mime = DocumentReader.getElementValue(map);

                mimeMapping.put(format + "/" + codec, new MimeType(mime));
                log.debug("Adding mime mapping " + format + "/" + codec + " -> " + mime);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private int    number; // for storage

    private Format(int n) { // private constructor!!
        number = n;
    }

    public int toInt()    {
        return number;
    }
    public int getValue() {
        return number;
    }

    public static Format get(int i) {
        for (Format f : Format.values()) {
            if (f.number == i) return f;
        }
        return UNKNOWN;
    }

    /**
     * don't know if this is nice
     */
    public static List<Format> getMediaFormats() {
        return Arrays.asList(Format.values());
    }

    private static final Map<String,String> duplicates = new HashMap<String,String>() {
        {
            put("AIF", "AIFF");
            put("JPG", "JPEG");
            put("TIF", "TIFF");
        }
    };

    public static Format get(String id) {
        id = id.toUpperCase();
        if (duplicates.containsKey(id)) id = duplicates.get(id);
        try {
            return Format.valueOf(id);
        } catch (IllegalArgumentException iae) {
            return UNKNOWN;
        }
    }

    public String getGUIIndicator(Locale locale) {
        try {
            ResourceBundle m = ResourceBundle.getBundle(RESOURCE, locale);
            return m.getString("" + number);
            // return  ConstantsBundle.get(RESOURCE, this.getClass(), number, locale);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }


    protected static final List<Format> windowsMedia = Arrays.asList(new Format[] {ASF, WMP, WMA, ASX,  WAX, WMV, WVX, WM, WMX, WMZ, WMD});
    protected static final List<Format> real         = Arrays.asList(new Format[] {RA, RM, RAM});

    public boolean isReal() {
        return real.contains(this);
    }
    public boolean isWindowsMedia() {
        return windowsMedia.contains(this);
    }

    public List<Format> getSimilar() {
        if (isReal()) {
            return real;
        } else if (isWindowsMedia()) {
            return windowsMedia;
        }
        return Arrays.asList(new Format[]{this});
    }

    public MimeType getMimeType() {
        return getMimeType(null);
    }

    public MimeType getMimeType(String codec) {
        String format = toString().toLowerCase();
        if(format == null || format.equals("unknown")) {
            format = MimeType.STAR;
        }
        if(codec == null || codec.equals("")) {
            codec = MimeType.STAR;
        }

        MimeType mimeType = mimeMapping.get(format + "/" + codec);
        if(mimeType == null && ! codec.equals("*")) {
            mimeType = mimeMapping.get(format + "/*");
        }
        if (mimeType == null && ! format.equals("*")) {
            mimeType = mimeMapping.get("*/" + codec);
        }
        if (mimeType == null) {
            mimeType = mimeMapping.get("*/*");
        }
        if (mimeType == null) {
            mimeType = new MimeType(MMBaseContext.getServletContext().getMimeType("test." + format));
        }
        if (mimeType == null) {
            mimeType = new MimeType("application", "octet-stream");
        }

        if (log.isDebugEnabled()) {
            log.debug("Finding mimetype for " + this + " (codec: " + codec + ") -> " + mimeType + " (used " + mimeMapping + ")");
        }
        return mimeType;

    }

}

