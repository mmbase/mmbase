/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util;

import java.io.*;
import org.mmbase.util.logging.*;
import org.apache.commons.fileupload.FileItem;

/**
 * Sometimes you need an InputStream to be Serializable. This wraps
 * another InputStream, or some other representation of a 'binary'.
 *
 * @since MMBase-1.9
 * @author Michiel Meeuwissen
 * @version $Id: SerializableInputStream.java,v 1.9 2009-04-27 14:15:17 michiel Exp $
 * @todo IllegalStateException or so, if the inputstreas is used (already).
 */

public class SerializableInputStream  extends InputStream implements Serializable  {

    private static final long serialVersionUID = 1;

    private static final Logger log = Logging.getLoggerInstance(SerializableInputStream.class);

    private final long size;

    private boolean used = false;

    public static byte[] toByteArray(InputStream stream) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int n;
            while ((n = stream.read(buf)) > -1) {
                bos.write(buf, 0, n);
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
        return bos.toByteArray();
    }

    private void use() {
        if (! used) {
            if (log.isTraceEnabled()) {
                log.trace("Using " + this + " because ", new Exception());
            }
            used = true;
        }
    }


    private InputStream wrapped;
    private File file = null;
    private String name;
    private String contentType;

    public SerializableInputStream(InputStream wrapped, long s) {
        this.wrapped = wrapped;
        this.size = s;
        this.name = null;
    }

    public SerializableInputStream(byte[] array) {
        this.wrapped = new ByteArrayInputStream(array);
        this.size = array.length;
        this.name = null;
    }

    public SerializableInputStream(FileItem fi) throws IOException {
        this.size = fi.getSize();
        this.name = fi.getName();
        this.contentType = fi.getContentType();
        file = File.createTempFile(getClass().getName(), this.name);
        try {
            fi.write(file);
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
        this.wrapped = new FileInputStream(file);


    }


    public long getSize() {
        return size;
    }
    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }
    public byte[] get() throws IOException {
        if (wrapped.markSupported()) {
            byte[] b =  toByteArray(wrapped);
            wrapped.reset();
            return b;
        } else {
            byte[] b =  toByteArray(wrapped);
            wrapped = new ByteArrayInputStream(b);
            return b;
        }
    }

    public void moveTo(File f) {
        if (name == null) {
            name = f.getName();
        }
        if (file != null) {
            if (file.equals(f)) {
                log.debug("File is already there " + f);
                return;
            } else if (file.renameTo(f)) {
                log.debug("Renamed " + file + " to " + f);
                file = f;
                return;
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
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        wrapped.reset();
        out.writeObject(toByteArray(wrapped));
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte[] b = (byte[]) in.readObject();
        wrapped = new ByteArrayInputStream(b);
    }
    @Override
    public int available() throws IOException {
        return wrapped.available();
    }
    private void supportMark() {
        try {
            if (file == null) {
                file = File.createTempFile(getClass().getName(), this.name);
                FileOutputStream os = new FileOutputStream(file);
                IOUtil.copy(wrapped, os);
                os.close();
            }
            wrapped = new FileInputStream(file);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void mark(int readlimit) {
        if (wrapped.markSupported()) {
            wrapped.mark(readlimit);
        } else {
            supportMark();
            wrapped.mark(readlimit);
        }
    }
    @Override
    public boolean markSupported() {
        return true;
    }
    @Override
    public int read() throws IOException { use(); return wrapped.read(); }
    @Override
    public int read(byte[] b) throws IOException { use(); return wrapped.read(b); }
    @Override
    public int read(byte[] b, int off, int len) throws IOException { use(); return wrapped.read(b, off, len); }

    @Override
    public void reset() throws IOException {
        if (wrapped.markSupported()) {
            wrapped.reset() ;
        } else {
            supportMark();
        }
    }
    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }

    @Override
    public String toString() {
        return "SERIALIZABLE " + wrapped + (used ? " (used)" :  "") + " (" + size + " byte, " + ( name == null ? "[no name]" : name) + ")";
    }
}
