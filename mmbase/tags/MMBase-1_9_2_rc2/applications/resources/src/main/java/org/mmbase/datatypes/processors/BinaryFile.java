/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes.processors;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.*;
import org.mmbase.util.*;
import org.mmbase.datatypes.processors.*;
import java.util.*;
import java.io.*;
import org.mmbase.util.logging.*;
import org.mmbase.servlet.FileServlet;


/**
 * This class constains Setter and Getter method for 'binary' file fields. In such field you can set
 * a FileItem, and it is stored as a file, using the FileServlet to produce an URL. The (string)
 * field itself only contains a file name.
 *
 * The file could (and currently is supposed to)  be served with {@link org.mmbase.servlet.FileServlet}.
 *
 * @author Michiel Meeuwissen
 */

public class BinaryFile {

    public static final String DISABLE_DELETE = BinaryFile.class.getName() + ".DISABLE";

    private static final Logger log = Logging.getLoggerInstance(BinaryFile.class);

    static File directory = null;

    private static File getDirectory() {
        if (directory != null) return directory;
        File servletDir = FileServlet.getDirectory();
        if (servletDir == null) throw new IllegalArgumentException("No FileServlet directory found (FileServlet not (yet) active)?");
        return servletDir;
    }


    private static File getFile(final Node node, final Field field, String fileName) {
        return new File(getDirectory(), getFileName(node, field, fileName).replace("/", File.separator));
    }

    public static String getFileName(final Node node, final Field field, String fileName) {
        StringBuilder buf = new StringBuilder();
        org.mmbase.storage.implementation.database.DatabaseStorageManager.appendDirectory(buf, node.getNumber(), "/");
        buf.append("/").append(node.getNumber()).append(".");
        buf.append(fileName);
        return  buf.toString();
    }

    public static class Delete implements CommitProcessor {

        protected String searchFields = "";

        public void setSetFields(String f) {
            searchFields = f;
        }
        public void commit(final Node node, final Field field) {
            if (node.getCloud().getProperty(DISABLE_DELETE) != null) {
                log.service("Disabled");
                return;
            }
            String existing = (String) node.getValue(field.getName());
            if (existing != null && ! "".equals(existing)) {
                File ef = new File(getDirectory(), existing);
                if (ef.exists() && ef.isFile()) {
                    // check whether not in use by some other node
                    boolean inUse = false;
                    if (searchFields.length() == 0) searchFields = field.getNodeManager().getName() + ":" + field.getName();
                    FIELDS:
                    for (String fd : searchFields.split(",")) {
                        String[] fdef = fd.split(":", 2);
                        NodeList otherNodes = SearchUtil.findNodeList(node.getCloud(), fdef[0], fdef[1], existing);
                        for (Node otherNode : otherNodes) {
                            if (otherNode.getNumber() != node.getNumber()) {
                                inUse = true;
                                break FIELDS;
                            }
                        }
                    }
                    if (! inUse) {
                        // no? Then delete the file.
                        ef.delete();
                        log.service("Deleted " + ef);
                        File metaFile = FileServlet.getInstance().getMetaFile(ef);
                        if (metaFile.exists()) {
                            metaFile.delete();
                        }

                    } else {
                        log.service("Not deleted " + ef + " because still in use");
                    }
                } else {
                    log.warn("Could not find " + ef + " so could not delete it");
                }
            }
        }
    }


    public static class Setter implements Processor {

        private static final long serialVersionUID = 1L;

        private String contenttypeField = "mimetype";

        public void setContenttypeField(String f) {
            contenttypeField = f;
        }

        public Object process(final Node node, final Field field, final Object value) {
            SerializableInputStream is = Casting.toSerializableInputStream(value);
            String name = is.getName();
            if (name != null) {
                File dir = getDirectory();
                String existing = (String) node.getValue(field.getName());
                if (existing != null && ! "".equals(existing)) {
                    File ef = new File(dir, existing);
                    if (ef.exists() && ef.isFile()) {
                        log.service("Removing existing field " + ef);
                        ef.delete();
                    } else {
                        log.warn("Could not find " + ef + " so could not delete it");
                    }
                }
                File f = getFile(node, field, name);
                Map<String, String> meta = FileServlet.getInstance().getMetaHeaders(f);
                meta.put("Content-Disposition", "attachment; filename=\"" + name + "\"");
                FileServlet.getInstance().setMetaHeaders(f, meta);

                if (log.isDebugEnabled()) {
                    log.debug("" + value + " -> " + is + " -> " + f + " " + Logging.applicationStacktrace());
                }
                is.moveTo(f);
                log.debug("Set a file " + f.getName());
                if (node.getNodeManager().hasField(contenttypeField)) {
                    if (! node.isChanged(contenttypeField) || node.isNull(contenttypeField)) {
                        node.setStringValue(contenttypeField, is.getContentType());
                        log.info("Found " + is.getContentType());
                    } else {
                        log.debug("Field " + contenttypeField + " is already changed " + node.getChanged() + " not setting to " + is.getContentType());
                    }
                } else {
                    log.debug("No field " + contenttypeField);
                }
                return f.toString().substring(dir.toString().length() + 1);
            } else {
                log.debug("No name given, ignoring this processor (not an upload)");
                return value;
            }
        }
    }

    public static class Getter implements Processor {
        private static final long serialVersionUID = 1L;

        public Object process(Node node, Field field, Object value) {

            return value;
        }
    }

    /**
     * A bit of a hack, used if the file was originally saved in a transaction, and hence as a
     * negative number prefix. If possible this processor corrects that.
     */
    public static class StringGetter implements Processor {
        private static final long serialVersionUID = 1L;

        public Object process(final Node node, final Field field, final Object value) {
            if (value == null) return null;
            String fileName = (String) value;
            log.debug("String processing " + fileName);
            if (fileName.startsWith("-") && node.getNumber() > 0) {
                File dir = getDirectory();
                String[] parts = fileName.split("\\.", 2);
                File file = new File(dir, fileName);
                File to = getFile(node, field, parts[1]);
                if (! to.getParentFile().exists()) {
                    if (! (to.getParentFile().mkdirs())) {
                    log.warn("Could not make directories " + to.getParentFile());
                    }
                }
                log.debug("Fixing file");
                if (file.renameTo(to)) {
                    fileName = to.toString().substring(dir.toString().length() + 1);
                    log.debug("Setting file name to " + fileName);
                    node.setValueWithoutProcess(field.getName(), fileName);
                    log.debug("Chached " + node.getChanged() + " " + node.getCloud());
                    node.commit();
                    File meta = FileServlet.getInstance().getMetaFile(file);
                    if (meta.exists()) {
                        File toMeta = FileServlet.getInstance().getMetaFile(to);
                        toMeta.getParentFile().mkdirs();
                        meta.renameTo(toMeta);
                    }
                } else {
                    log.warn("Could not rename " + file + " to " + to);
                }
            }

            return fileName;
        }
    }

}
