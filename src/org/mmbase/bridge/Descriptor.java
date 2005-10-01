/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.bridge;

import java.util.*;


/**
 * @since MMBase-1.8
 */
public interface Descriptor {

    /**
     * Returns the name or 'key' of this object, or <code>null</code> if not applicable.
     * @return the name as a String
     */
    public String getName();

    /**
     * Returns the GUI name for this object.
     *
     * @return  the GUI name for this object
     */
    public String getGUIName();

   /**
     * Returns the GUI name for this object in a specified preferred language.
     *
     * @param locale the locale that determines the language for the GUI name
     * @return  the GUI name for this object
     * @since MMBase-1.7
     */
    public String getGUIName(Locale locale);

    /**
     * Returns the set of (localized) gui names of this object.
     * @return the description as a LocalizedString
     */
    public org.mmbase.util.LocalizedString getLocalizedGUIName();

    /**
     * Sets the GUI name of this object.
     * @param locale The locale for which this is valid, or <code>null</code> for the default locale.
     * @param g the description as a String
     */
    public void setGUIName(String g, Locale locale);

    /**
     * Sets the GUI name of this object for the default locale.
     * @param g the description as a String
     */
    public void setGUIName(String g);

    /**
     * Returns the set of (localized) descriptions of this object.
     * @return the description as a LocalizedString
     */
    public org.mmbase.util.LocalizedString getLocalizedDescription();

    /**
     * Returns the description of this object.
     * @param locale The locale for which this must be returned, or <code>null</code> for a default locale.
     *               If no fitting description for the given locale is available, getName() can be returned.
     * @return the description as a String
     */
    public String getDescription(Locale locale);

    /**
     * Returns the description of this object for the default locale.
     * @return the description as a String
     */
    public String getDescription();

    /**
     * Sets the description of this object.
     * @param description the description as a String
     * @param locale The locale for which this is valid, or <code>null</code> for a default locale.
     */
    public void setDescription(String description, Locale locale);

    /**
     * Sets the description of this object for the default locale.
     * @param description the description as a String
     */
    public void setDescription(String description);

    /**
     * Associates a resource-bundle with the description of this DataType. The description can be
     * looked up using this resource-bundle if no description for a Locale was specified explicitely.
     */
    public void setBundle(String bundle);

}
