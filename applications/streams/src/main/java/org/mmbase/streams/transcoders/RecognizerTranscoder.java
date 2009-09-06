/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.streams.transcoders;

import org.mmbase.applications.media.*;
import java.net.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import org.mmbase.util.externalprocess.*;
import org.mmbase.util.WriterOutputStream;

import org.mmbase.util.logging.*;


/**
 * This thin wrapper just represents a 'Recognizer' as a Transcoder. This makes administration easier.
 *
 * @author Michiel Meeuwissen
 * @version $Id: AbstractTranscoder.java 36425 2009-06-25 18:26:28Z michiel $
 */
public class RecognizerTranscoder implements Transcoder {

    final Recognizer recognizer;
    public RecognizerTranscoder(Recognizer rec) {
        recognizer = rec;
    }

    protected boolean clone = false;

    protected URI in;
    protected URI out;

    protected Format format;

    protected Codec  codec = Codec.UNKNOWN;

    protected MimeType  mimeType = MimeType.ANY;

    public void setFormat(String f) {
        throw new UnsupportedOperationException();
    }

    public Format getFormat() {
        return null;
    }

    public void setCodec(String c) {
        throw new UnsupportedOperationException();
    }

    public Codec getCodec() {
        return null;
    }

    public MimeType getMimeType() {
        return recognizer.getMimeType();
    }
    public void setMimeType(String m) {
        recognizer.setMimeType(m);
    }
    public String getInId() {
        return null;
    }
    public void setInId(String i) {
        if (i != null) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The key of recognizes must be null, that avoid that any subsequent caches are created
     */
    public  final String getKey() {
        return null;

    }

    public final void transcode(final URI in, final URI out, final Logger log) throws Exception {
        if (in == null) throw new IllegalArgumentException();
        this.in = in;
        recognizer.analyze(in, log);
    }

    public URI getIn() {
        return in;
    }
    public URI getOut() {
        return null;
    }

    public RecognizerTranscoder clone() {
        try {
            RecognizerTranscoder c =  (RecognizerTranscoder) super.clone();
            c.clone = true;
            return c;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    public String toString() {
        return recognizer.toString();
    }




}
