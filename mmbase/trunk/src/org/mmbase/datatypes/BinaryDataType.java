/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.datatypes;

import org.mmbase.util.logging.*;
import java.util.regex.Pattern;
import org.apache.commons.fileupload.FileItem;

/**
 * The datatype associated with byte arrays ('blobs').
 *
 * @author Pierre van Rooden
 * @version $Id: BinaryDataType.java,v 1.9 2006-04-29 19:37:06 michiel Exp $
 * @since MMBase-1.8
 */
public class BinaryDataType extends AbstractLengthDataType {

    private static final Logger log = Logging.getLoggerInstance(BinaryDataType.class);

    private static final long serialVersionUID = 1L; // increase this if object serialization changes (which we shouldn't do!)

    protected Pattern validMimeTypes = Pattern.compile(".*");
    /**
     * Constructor for binary field.
     * @param name the name of the data type
     */
    public BinaryDataType(String name) {
        super(name, byte[].class);
    }

    protected void inheritProperties(BasicDataType origin) {
        super.inheritProperties(origin);
        if (origin instanceof BinaryDataType) {
            validMimeTypes = ((BinaryDataType) origin).validMimeTypes;
        }
    }

    //
    public long getLength(Object value) {
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (log.isDebugEnabled()) {
                StringBuffer buf = new StringBuffer("[");
                for (int i = 0 ; i < bytes.length; i++) {
                    buf.append((char) bytes[i]);
                    if (i + 1 < bytes.length) {
                        buf.append(", ");
                    }
            }
                buf.append("]");
                log.debug("Getting length of " + buf);
            }
            return bytes.length;
        } else if (value instanceof FileItem) {
            return ((FileItem) value).getSize();
        } else {
            throw new RuntimeException("Value " + value + " of " + getName() + " is not a byte array but" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    /**
     * Returns a regular expression which describes wich mime-types are valid for blobs with this
     * DataType. This is not yet available as a Restriction, only as a property.
     */
    public Pattern getValidMimeTypes() {
        return validMimeTypes;
    }

    public void setValidMimeTypes(Pattern pattern) {
        validMimeTypes = pattern;
    }

}
