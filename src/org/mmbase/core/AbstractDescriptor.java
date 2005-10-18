/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.core;

import java.util.*;
import org.mmbase.bridge.Descriptor;
import org.mmbase.util.LocalizedString;
import org.mmbase.util.logging.*;

/**
 * @javadoc
 *
 * @author Pierre van Rooden
 * @since  MMBase-1.8
 * @version $Id: AbstractDescriptor.java,v 1.6 2005-10-18 21:05:23 michiel Exp $
 */

abstract public class AbstractDescriptor implements Descriptor, Cloneable {

    private static final Logger log = Logging.getLoggerInstance(AbstractDescriptor.class);

    protected String key;
    protected LocalizedString description;
    protected LocalizedString guiName;

    protected AbstractDescriptor() {}

    /**
     * Create a data type object
     * @param name the name of the data type
     */
    protected AbstractDescriptor(String name) {
        key = name;
        setDescription(name);
        setGUIName(name);
    }

    /**
     * Create a data type object
     * @param name the name of the data type
     * @param descriptor
     */
    protected AbstractDescriptor(String name, Descriptor descriptor) {
        key = name;
        description = (LocalizedString)descriptor.getLocalizedDescription().clone();
        guiName = (LocalizedString)descriptor.getLocalizedGUIName().clone();
    }

    /**
     * Returns the name or 'key' of this data type.
     * @return the name as a String
     */
    public String getName() {
        return key;
    }

    public String getDescription(Locale locale) {
        if (description == null) description = new LocalizedString(key);
        return description.get(locale);
    }

    public String getDescription() {
        return getDescription(null);
    }

    public LocalizedString getLocalizedDescription() {
        return description;
    }

    protected void setLocalizedDescription(LocalizedString description) {
        this.description = description;
    }

    public void setDescription(String desc, Locale locale) {
        if (description == null) description = new LocalizedString(key);
        description.set(desc, locale);
    }

    public void setDescription(String desc) {
        setDescription(desc,null);
    }

    public void setBundle(String b) {
        if (description == null) description = new LocalizedString(key);
        description.setBundle(b);
    }

    /**
     * Retrieve the GUI name of the field depending on specified langauge.
     * If the language is not available, the "en" value is returned instead.
     * If that one is unavailable the internal fieldname is returned.
     * @return the GUI Name
     */
    public String getGUIName(Locale locale) {
        if (guiName == null) guiName = new LocalizedString(key);
        return guiName.get(locale);
    }

    /**
     * Retrieve the GUI name of the field.
     * If possible, the "en" value is returned.
     * If that one is unavailable the internal fieldname is returned.
     * @return the GUI Name
     */
    public String getGUIName() {
        return getGUIName(null);
    }

    public void setGUIName(String g, Locale locale) {
        if (guiName == null) guiName = new LocalizedString(key);
        guiName.set(g, locale);
    }

    public void setGUIName(String g) {
        setGUIName(g, null);
    }

    public LocalizedString getLocalizedGUIName() {
        return guiName;
    }

    protected void setLocalizedGUIName(LocalizedString value) {
        guiName = value;
    }

    public String toString() {
        return key;
    }

    public Object clone() throws CloneNotSupportedException {
        return clone(getName() + ".clone");
    }

    public Object clone(String name) throws CloneNotSupportedException {
        AbstractDescriptor clone = (AbstractDescriptor)super.clone();
        clone.description = (LocalizedString)description.clone();
        clone.guiName = (LocalizedString)guiName.clone();
        if (name != null) {
            clone.key = name;
            clone.description.setKey(name);
            clone.guiName.setKey(name);
        }
        return clone;
    }

}
