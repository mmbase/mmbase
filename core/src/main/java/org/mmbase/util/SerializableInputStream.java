/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.*;
import org.mmbase.util.logging.*;
import org.mmbase.util.magicfile.MagicFile;
import org.apache.commons.fileupload.FileItem;

/**
 * Sometimes you need an InputStream to be Serializable. This wraps
 * another InputStream, or some other representation of a 'binary'.
 *
 * @since MMBase-1.9
 * @author Michiel Meeuwissen
 * @version $Id$
 * @todo IllegalStateException or so, if the inputstreas is used (already).
 */

public class SerializableInputStream  extends InputStream implements Serializable  {

    private static final long serialVersionUID = 2L;


    private static final Logger log = Logging.getLoggerInstance(SerializableInputStream.class);

    private long size;


    public static byte[] toByteArray(InputStream stream) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) > -1) {
                bos.write(buf, 0, n);
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
        return bos.toByteArray();
    }



    private File file = null;
    private long fileMark = 0;
    private boolean tempFile = true;
    private String name;
    private MimeType contentType = null;
    private transient InputStream wrapped;
    private boolean used = false;

    public SerializableInputStream(InputStream wrapped, long s) {
        this.wrapped = wrapped;
        this.size = s;
        this.name = null;
        if (wrapped == null) {
            throw new NullPointerException();
        }
        if (wrapped.markSupported()) {
            wrapped.mark(Integer.MAX_VALUE);
        }
    }

    /**
     * @since MMBase-1.9.2
     */
    public SerializableInputStream(File tempFile, String name) throws IOException {
        this.file  = tempFile;
        this.wrapped = new FileInputStream(tempFile);
        this.size = tempFile.length();
        this.name = name;
        if (tempFile.length() > 0) {
            String ct = MagicFile.getInstance().getMimeType(tempFile);
            if (MagicFile.FAILED.equals(ct)) {
                log.debug("Failed to determin type of " + tempFile);
                this.contentType = MimeType.UNDETERMINED;
            } else {
                this.contentType = new MimeType(ct);

            }
        }
    }
    /**
     * @since MMBase-1.9.2
     */
    public SerializableInputStream(File tempFile) throws IOException {
        this(tempFile, tempFile.getName());
    }

    public SerializableInputStream(byte[] array) {
        wrapped = new ByteArrayInputStream(array);
        this.size = array.length;
        this.name = null;
        if (array.length > 0) {
            try {
                String ct = MagicFile.getInstance().getMimeType(array);

                if (MagicFile.FAILED.equals(ct)) {
                    log.debug("Failed to determin type of byte array");
                    this.contentType = MimeType.UNDETERMINED;
                } else {
                    this.contentType = new MimeType(ct);
                }
            } catch (Exception e) {
            }
        }
    }


    /**
     * Removed directory information that may be present in some browsers.
     * @since MMBase-1.9.5
     */
    private static String getFileName(FileItem fi) throws IOException {
        String fileName = fi.getName();
        if (fileName == null) {
            return "NULL";
        }
        int pos = fileName.lastIndexOf("\\");
        if (pos > 0) {
            fileName = fileName.substring(pos + 1);
        }
        pos = fileName.lastIndexOf("/");
        if (pos > 0) {
            fileName = fileName.substring(pos + 1);
        }
        return fileName;
    }


    public SerializableInputStream(FileItem fi) throws IOException {
        this.size = fi.getSize();
        this.name = getFileName(fi);
        this.contentType = new MimeType(fi.getContentType());
        try {
            file = File.createTempFile(getClass().getName(), this.name);
        } catch (IOException ioe) {  //MMB-1987 ?
            log.error(ioe.getMessage(), ioe);
            file = File.createTempFile(getClass().getName(), "nofilename");
        }
        file.deleteOnExit();
        try {
            fi.write(file);
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
        this.wrapped = new FileInputStream(file);
    }

    SerializableInputStream(SerializableInputStream is) throws IOException {
        if (is.file == null) {
            is.supportMark();
        }
        this.size = is.size;
        this.tempFile = is.tempFile;
        this.name = is.name;
        this.contentType =  is.contentType;
        this.used = is.used;

        if (this.tempFile) {
            this.file = File.createTempFile(getClass().getName(), name);
            this.file.deleteOnExit();
            FileOutputStream os = new FileOutputStream(this.file);
            FileInputStream  in = new FileInputStream(is.file);
            IOUtil.copy(in, os);
            os.close();
            is.close();
	    this.wrapped = in;
	    reset();

        } else {
            this.file = is.file;
	    this.wrapped = is.wrapped;
        }


    }

    private synchronized void use() {
        if (! used) {
            if (log.isTraceEnabled()) {
                log.trace("Using " + this + " because ", new Exception());
            }
            used = true;
            if (! wrapped.markSupported() && file == null) {
                supportMark();
            }
        }
    }


    public long getSize() {
        return size;
    }
    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType == null || MimeType.UNDETERMINED.equals(contentType) ? null : contentType.toString();
    }
    /**
     * @since MMBase-1.9.3
     */
    public void setContentType(MimeType ct) {
        contentType = ct;
    }
    public synchronized byte[] get() throws IOException {
        if (wrapped == null) {
            throw new IllegalStateException();
        }
        if (file != null || wrapped.markSupported()) {
            log.debug("Making byte array of " + wrapped);
            reset();
            byte[] b =  toByteArray(wrapped);
            log.debug("Resetting");
            reset();
            return b;
        } else {
            log.debug("Making byte array of " + wrapped);
            byte[] b =  toByteArray(wrapped);
            wrapped = new ByteArrayInputStream(b);
            log.debug("Converted to bytearray" + wrapped);
            return b;
        }
    }

    public synchronized void moveTo(File f) {
        if (name == null) {
            name = f.getName();
        }
        log.debug("Moving " + (file == null ? "" + this : "" + file) + " to " + f);
        if (file != null) {
            if (file.equals(f)) {
                log.debug("File is already there " + f);
                return;
            } else if (file.renameTo(f)) {
                try {
                    log.debug("Renamed " + file + " to " + f);
                    file = f;
                    wrapped = new FileInputStream(file);
                    tempFile = false;
                    return;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            } else {
                log.debug("Could not rename " + file + " to " + f + " will copy/delete in stead");
            }
        }
        try {
            FileOutputStream os = new FileOutputStream(f);
            IOUtil.copy(wrapped, os);
            os.close();
            wrapped = new FileInputStream(f);
            if (file != null) {
                file.delete();
            }
            file = f;
            tempFile = false;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    protected void _writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Serializing " + this);
        }
        if (file == null) {
            supportMark();
        }
        reset();
        out.writeObject(get());
        out.writeObject(name);
        out.writeObject(contentType);
        reset();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        _writeObject(out);
    }


    protected void _readObject(java.io.ObjectInputStream oin) throws IOException, ClassNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("DeSerializing " + this);
        }
        byte[] b = (byte[]) oin.readObject();
        wrapped = new ByteArrayInputStream(b);
        size = b.length;
        name = (String) oin.readObject();
        contentType = (MimeType) oin.readObject();
    }
    private void readObject(java.io.ObjectInputStream oin) throws IOException, ClassNotFoundException {
        _readObject(oin);
    }

    private synchronized FileInputStream supportMark() {
        try {
            assert file == null;
            file = File.createTempFile(getClass().getName(), this.name);
            file.deleteOnExit();
            FileOutputStream os = new FileOutputStream(file);
            IOUtil.copy(wrapped, os);
            os.close();
            FileInputStream fis = new FileInputStream(file);
            wrapped = fis;
            if (log.isDebugEnabled()) {
                log.debug("Created " + fis + "" + file.length());
            }
            return fis;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }


    @Override
    public void finalize() {
        if (file != null && tempFile) {
            log.debug("Deleting " + file);
            file.delete();
        }
    }
    @Override
    public void close() throws IOException {
        use();
        wrapped.close();
    }


    @Override
    public void mark(int readlimit) {
        log.debug("Marking" + wrapped, new Exception());

        if (wrapped.markSupported()) {
            wrapped.mark(readlimit);
            return;
        }
        try {
            FileInputStream fis =
                file != null ?  (FileInputStream) wrapped : supportMark();


            fileMark = fis.getChannel().position();

        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
    @Override
    public boolean markSupported() {
        return true;
    }
    @Override
    public int read() throws IOException {
        use();
        return wrapped.read();
    }
    @Override
    public int read(byte[] b) throws IOException {
        use();
        return wrapped.read(b);
    }
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        use();
        int res =  wrapped.read(b, off, len);
        if (log.isTraceEnabled()) {
            log.trace("Reading  " + res + "/" + len + " offset " + off+ " from " + wrapped, new Exception());
        }
        return res;

    }


    @Override
    public long skip(long n) throws IOException {
        use();
        log.trace("Skipping " + n + " from " + wrapped);
        return wrapped.skip(n);
    }



    @Override
    public void reset() throws IOException {
        if (wrapped.markSupported()) {
            log.debug("" + wrapped + " supports mark, using it");
            wrapped.reset() ;
        } else if (file != null) {
            if (log.isDebugEnabled()) {
                log.debug("Resetting " + this + " to " + fileMark + " (" + file + ")");
            }
            wrapped = new FileInputStream(file);
            if (fileMark > 0) {
                wrapped.skip(fileMark);
            }
        } else {
            log.debug("No file yet");
            supportMark();
        }
    }




    @Override
    public String toString() {
        String filePos;
        try {
	    if (wrapped instanceof FileInputStream) {
		java.nio.channels.FileChannel chan = ((FileInputStream) wrapped).getChannel();
		if (chan.isOpen()) {
		    filePos = "" + chan.position();
		} else {
		    filePos = "close";
		}

	    } else {
		filePos = "?";
	    }
        } catch (IOException ioe) {
	    filePos = ioe.getMessage();
        }
        return "SERIALIZABLE " + wrapped +
            (tempFile ? (" (tempfile: " + file + ") ") : (file != null ? ("(" + file + ")") : "")) +
            " (" + size + " byte, " +
            (name == null ? "[no name]" : name) +
            ", " +
            (contentType == null ? "[no contenttype]" : contentType) +
            (fileMark > 0 ? (" mark: " + fileMark) : "") +
            (filePos.equals("0") ? "" :  (" position: " + filePos)) +
            ")";
    }

    protected static boolean inputStreamEquals(SerializableInputStream in1, SerializableInputStream in2) throws IOException {
        in1.mark(Integer.MAX_VALUE);
        in2.mark(Integer.MAX_VALUE);
        try {
            final byte[] buffer1 = new byte[1024];
            final byte[] buffer2 = new byte[1024];
            while (true) {
                int n1 = in1.read(buffer1);
                int n2 = in2.read(buffer2);
                if (n1 != n2) return false;
                if (n1 == -1) break;
                if ( ! java.util.Arrays.equals(buffer1, buffer2)) {
                    return false;
                }
            }
            return true;
        } finally {
            in1.reset();
            in2.reset();
        }

    }


    @Override
    public boolean equals(Object o) {
        if (o instanceof SerializableInputStream) {
            SerializableInputStream s = (SerializableInputStream) o;
            try {
                return
                    (getSize() == s.getSize()) &&
                    (getName() == null ? s.getName() == null : getName().equals(s.getName())) &&
                    (getContentType() == null ? s.getContentType() == null : getContentType().equals(s.getContentType())) &&
                    (fileMark == s.fileMark) &&
                    ((file != null && file.equals(s.file)) || inputStreamEquals(this, s));

            } catch (IOException ioe) {
                log.error(ioe);
                return false;
            }
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (int) (this.size ^ (this.size >>> 32));
        hash = 43 * hash + (this.wrapped != null ? this.wrapped.hashCode() : 0);
        hash = 43 * hash + (this.file != null ? this.file.hashCode() : 0);
        hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 43 * hash + (this.contentType != null ? this.contentType.hashCode() : 0);
        return hash;
    }
    File getFile() {
        return file;
    }

    /**
     * @since MMBase-1.9.2
     */
    public String getFileName() {
        if (file != null) {
            return file.getName();
        } else {
            return name;
        }
    }

}
