/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;

import javax.xml.parsers.DocumentBuilder;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.DocumentReader;
import org.xml.sax.*;

/**
 * XMLBasicReader has two goals.
 *  <ul>
 *    <li>It provides a way for parsing XML</li>
 *    <li>It provides a way for searching in this XML, without the need for an XPath implementation, and without the hassle of org.w3c.dom alone.
 *    It uses dots to lay a path in the XML (XPath uses slashes).</li>
 *  </ul>
 *
 * @deprecated use DocumentReader or DocumentWriter. Some code may need to be moved to DocumentReader
 * @author Case Roule
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @author Michiel Meeuwissen
 * @version $Id$
 */
public class XMLBasicReader extends DocumentReader {

    private static Logger log = Logging.getLoggerInstance(XMLBasicReader.class);

    public XMLBasicReader(String path) {
        super(getInputSource(path));
    }

    public XMLBasicReader(String path, boolean validating) {
        super(getInputSource(path), validating, null);
    }

    public XMLBasicReader(String path, Class resolveBase) {
        super(getInputSource(path), DocumentReader.validate(), resolveBase);
    }

    public XMLBasicReader() {
        super();
    }

    public XMLBasicReader(InputSource source, boolean validating, Class resolveBase) {
        super(source, validating, resolveBase);
    }

    public XMLBasicReader(InputSource source, boolean validating) {
        super(source, validating);
    }

    public XMLBasicReader(InputSource source, Class resolveBase) {
        super(source, resolveBase);
    }

    public XMLBasicReader(InputSource source) {
        super(source);
    }

    /**
     * Creates an input source for a document, based on a filepath
     * If the file cannot be opened, the method returns an inputsource of an error document describing the condition
     * under which this failed.
     * @param  path the path to the file containing the document
     * @return the input source to the document.
     * @deprecated
     */
    public static InputSource getInputSource(String path) {
        InputSource is;
        try {
            // remove file protocol if present to avoid errors in accessing file
            if (path.startsWith("file://")) {
                try {
                    path = new java.net.URL(path).getPath();
                } catch (java.net.MalformedURLException mfe) {
                }
            }
            is = new InputSource(new FileInputStream(path));
            try {
                is.setSystemId(new File(path).toURL().toExternalForm());
            } catch (java.net.MalformedURLException mfe) {
            }
            is.setSystemId("file://" + path);
        } catch (java.io.FileNotFoundException e) {
            log.error("Error reading " + path + ": " + e.toString());
            log.service("Using empty source");
            // try to handle more or less gracefully
            is = new InputSource();
            is.setSystemId(FILENOTFOUND + path);
            is.setCharacterStream(new StringReader("<?xml version=\"1.0\"?>\n" +
                                                   "<!DOCTYPE error PUBLIC \"" + PUBLIC_ID_ERROR + "\"" +
                                                   " \"https://www.mmbase.org/dtd/error_1_0.dtd\">\n" +
                                                   "<error>" + path + " not found</error>"));
         }
        return is;
    }

    /**
     * Obtain a DocumentBuilder
     * @deprecated use {!link DocumentReader.getDocumentBuilder(boolean, ErrorHandler, EntityResolver)}
     */
    public static DocumentBuilder getDocumentBuilder(boolean validating, ErrorHandler handler) {
        return DocumentReader.getDocumentBuilder(validating, handler, null);
    }

    /**
     * Obtain a DocumentBuilder
     * @deprecated use {!link DocumentReader.getDocumentBuilder(boolean, ErrorHandler, EntityResolver)}
     */
    public static DocumentBuilder getDocumentBuilder(boolean validating, EntityResolver resolver) {
        return DocumentReader.getDocumentBuilder(validating, null, resolver);
    }

    /**
     * Obtain a DocumentBuilder
     * @deprecated use {!link DocumentReader.getDocumentBuilder(boolean, ErrorHandler, EntityResolver)}
     */
    public static DocumentBuilder getDocumentBuilder(boolean validating, ErrorHandler handler, EntityResolver resolver) {
        return DocumentReader.getDocumentBuilder(validating, handler, resolver);
    }

    /**
     * Obtain a DocumentBuilder
     * @deprecated use {!link DocumentReader.getDocumentBuilder(boolean, ErrorHandler, EntityResolver)}
     */
    public static DocumentBuilder getDocumentBuilder(Class refer) {
        return DocumentReader.getDocumentBuilder(DocumentReader.validate(), null, new org.mmbase.util.xml.EntityResolver(DocumentReader.validate(), refer));
    }

}
