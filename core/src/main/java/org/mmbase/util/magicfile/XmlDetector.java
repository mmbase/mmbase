/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.util.magicfile;
import java.util.*;
import java.io.*;
import org.w3c.dom.Element;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.regex.*;
import org.mmbase.util.xml.ErrorHandler;
import org.mmbase.util.xml.*;
import org.mmbase.util.logging.*;

/**
 * A detector which can match on XML namespaces, publicId.
 *
 * @version $Id$
 * @author Michiel Meeuwissen
 */

public class XmlDetector extends AbstractDetector {
    private static final Logger log = Logging.getLoggerInstance(XmlDetector.class);


    protected String namespace = null;
    protected Pattern publicId   = null;

    public void setXmlns(String xmlns) {
        namespace = xmlns;
    }

    public void setPublicId(String dt) {
        publicId = Pattern.compile(dt);
    }

    /**
     * @return Whether detector matches the prefix/lithmus of the file
     */
    public boolean test(byte[] lithmus) {
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            Handler handler = new Handler();
            parser.setContentHandler(handler);
            parser.setDTDHandler(handler);
            parser.setEntityResolver(handler);
            parser.setErrorHandler(new ErrorHandler(false, ErrorHandler.FATAL_ERROR));
            InputSource source = new InputSource(new ByteArrayInputStream(lithmus));
            parser.parse(source);
            return false;
        } catch (Matched m) {
            log.debug("Matched " + m.getMessage());
            return true;
        } catch (SAXException e) {
            return false;
        } catch (java.io.IOException ioe) {
            log.warn(ioe);
            return false;
        }
    }

    @Override
    public void configure(Element el) {
        super.configure(el);
        if (namespace == null && publicId == null) {
            throw new IllegalStateException("Not configured with either namespace or publicId");
        }
    }

    protected class Matched extends RuntimeException {
        public Matched(String mes) {
            super(mes);
        }
    }


    protected class Handler extends DefaultHandler {

        @Override
        public void startPrefixMapping(String prefix, String uri)  {
            if (uri.equals(XmlDetector.this.namespace)) {
                throw new Matched("Namespace " + uri);
            }
        }
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            if (XmlDetector.this.publicId != null && XmlDetector.this.publicId.matcher(publicId).matches()) {
                throw new Matched("publicId " + publicId);
            }
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }

    }

}
