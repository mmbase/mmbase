/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util.xml;

import java.util.*;
import org.w3c.dom.*;

import org.mmbase.module.core.MMBase;
import org.mmbase.module.core.MMObjectBuilder;
import org.mmbase.module.corebuilders.FieldDefs;

import org.mmbase.util.XMLBasicReader;
import org.mmbase.util.XMLEntityResolver;
import org.mmbase.util.logging.*;

/**
 * Used to parse and retrieve data from a builder configuration file.
 * The parser support builders for builder dtd 1.1.
 *
 * @since MMBase 1.7
 * @author Case Roole
 * @author Rico Jansen
 * @author Pierre van Rooden
 * @version $Id: BuilderReader.java,v 1.1 2003-04-11 11:05:52 pierre Exp $
 */
public class BuilderReader extends XMLBasicReader {

    // logger
    private static Logger log = Logging.getLoggerInstance(BuilderReader.class.getName());

    /** Public ID of the Builder DTD version 1.0 */
    public static final String PUBLIC_ID_BUILDER_1_0 = "-//MMBase//DTD builder config 1.0//EN";
    private static final String PUBLIC_ID_BUILDER_1_0_FAULT = "-//MMBase/DTD builder config 1.0//EN";
    private static final String PUBLIC_ID_BUILDER_OLD = "/MMBase - builder//";

    /** Public ID of the Builder DTD version 1.1 */
    public static final String PUBLIC_ID_BUILDER_1_1 = "-//MMBase//DTD builder config 1.1//EN";
    private static final String PUBLIC_ID_BUILDER_1_1_FAULT = "-//MMBase/DTD builder config 1.1//EN";

    /** DTD resource filename of the Builder DTD version 1.0 */
    public static final String DTD_BUILDER_1_0 = "builder_1_0.dtd";
    /** DTD resource filename of the Builder DTD version 1.1 */
    public static final String DTD_BUILDER_1_1 = "builder_1_1.dtd";

    /** Public ID of the most recent Builder DTD */
    public static final String PUBLIC_ID_BUILDER = PUBLIC_ID_BUILDER_1_1;
    /** DTD respource filename of the most recent Builder DTD */
    public static final String DTD_BUILDER = DTD_BUILDER_1_1;

    /**
     * Register the Public Ids for DTDs used by XMLBasicReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        // various builder dtd versions
        XMLEntityResolver.registerPublicID(PUBLIC_ID_BUILDER_1_0, DTD_BUILDER_1_0, BuilderReader.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_BUILDER_1_1, DTD_BUILDER_1_1, BuilderReader.class);

        // legacy public IDs (wrong, don't use these)
        XMLEntityResolver.registerPublicID(PUBLIC_ID_BUILDER_1_0_FAULT, DTD_BUILDER_1_0, BuilderReader.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_BUILDER_OLD, DTD_BUILDER_1_0, BuilderReader.class);
        XMLEntityResolver.registerPublicID(PUBLIC_ID_BUILDER_1_1_FAULT, DTD_BUILDER_1_1, BuilderReader.class);
    }

    /**
     * MMBase instance, used to load parent (extending) builders
     */
    private MMBase mmbase;

    /**
     * Parent builder.
     * If assigned, the properties of this buidler are used as 'defaults'
     * and the fields of the builder are inherited.
     * @since MMbase-1.6
     */
    private MMObjectBuilder parentBuilder;

    /**
     * If false, the parent builder could not be resolved.
     * A builder with an unresolved parent is set to 'inactive', regardless of actual status
     * The default value is false, as resolving Inheritance is mandatory when loading builders.
     * @since MMbase-1.6
     */
    private boolean inheritanceresolved= false;

    /**
     * Creates an instance by reading a builder configuration (xml) file.
     * @since MMBase-1.6
     * @param filename path to the builder configuration file to parse
     * @param mmb The MMBase instance. Used to resolve inheritance of builders
     */
    public BuilderReader(String filename, MMBase mmb) {
        super(filename, BuilderReader.class);
        mmbase=mmb;
        resolveInheritance();
    }

    /**
     * Creates an instance by reading a builder configuration (xml) file.
     * A parser created with this constructor does not resolve inheritance, but maintains
     * the activity status as it is set in the file.
     * This call should be used if only the actual information in the xml file is needed.
     * @param filename path to the builder configuration file to parse
     */
    public BuilderReader(String filename) {
        this(filename, null);
        // fake resolving inheritance
        inheritanceresolved=true;
    }

    /**
     * Resolves inheritance.
     * If a builder 'extends' another builder, the parser attempts to
     * retrieve a reference to this builder (using getParentBuilder).
     * Note that if inheritance cannot be resolved, the builder cannot be activated.
     * This method returns false if the builder to extend from is inactive.
     * It throws a RuntimeException is the builder to extend from is not allowed as
     * an parent builder.
     *
     * @since MMBase-1.6
     * @return true if inheritance could be resolved, false if the .
     * @see #isInheritanceResolved()
     * @throws RuntimeException when the builder to extend from is not allowed as parent
     */
    protected boolean resolveInheritance() {
        String buildername=getBuilderExtends();
        if (buildername.equals("")) {
            parentBuilder=null;
            inheritanceresolved=true;
        } else {
            inheritanceresolved=false;
            if (mmbase!=null) {
                parentBuilder = mmbase.getBuilder(buildername);
                inheritanceresolved=(parentBuilder!=null);
            }
        }
        return inheritanceresolved;
    }

    /**
     * Detremines if inheritance is resolved.
     * This method returns true if a call to resolveInheritance succeeded.
     * it returns false if resolveInheritance failed (returned false or threw an exception)
     *
     * @since MMBase-1.6
     * @return true if inheritance could be resolved
     * @see #resolveInheritance()
     */
    public boolean isInheritanceResolved() {
        return inheritanceresolved;
    }

    /**
     * Get the status of this builder.
     * Note that if inheritance cannot be resolved, this method always returns "inactive".
     * @return a String decribing the status ("active" or "inactive")
     */
    public String getStatus() {
        if (!inheritanceresolved) return "inactive";
        String val=getElementValue("builder.status").toLowerCase();
        if (val.equals("")) {
           if (parentBuilder!=null) {
               return "active";
           } else {
               return "";
           }
        }
        // fix invalid values
        if (!val.equals("active")) {
           val="inactive";
        }
        return val;
    }

    /**
     * Retrieves the Search Age.
     * The search age may be used by editors or search forms to determine
     * the maximum age in days of an object to be searched (limiting the resultset
     * of a search)
     * @return the search age in days
     */
    public int getSearchAge() {
        int val=30;
        String sval = getElementValue("builder.searchage");
        if (sval.equals("") && (parentBuilder!=null)) {
            sval=parentBuilder.getSearchAge();
        }
        try {
            val=Integer.parseInt(sval);
        } catch(Exception f) {}
        return val;
    }

    /**
     * Get the class name to use for instantiating this builder.
     * Note that it is possible to specify a short-hand format in
     * the builder configuration file.
     * If only the classname (withoput package name) is given, the classname
     * is expanded to fall into the <code>org.mmbase.module.builders</code> package.
     * @return the classname to use.
     */
    public String getClassFile() {
        String val=getElementValue("builder.classfile");
        if (val.equals("")) {
            if (parentBuilder!=null) {
                return parentBuilder.getClass().getName();
            } else {
                return "";
            }
        }
        // is it a full name or inside the org.mmbase.module.builders.* path
        int pos=val.indexOf('.');
        if	(pos==-1) {
            val="org.mmbase.module.builders."+val;
        }
        return val;
    }

    /**
     * Get the field definitions of this builder.
     * If applicable, this includes the fields inherited from a parent builder.
     *
     * @code-conventions return type should be List
     * @return a Vector of all Fields as FieldDefs
     */
    public Vector getFieldDefs() {
        Vector results=new Vector();
        HashMap oldset=new HashMap();
        int pos=1;
        if (parentBuilder!=null) {
            List parentfields = parentBuilder.getFields(FieldDefs.ORDER_CREATE);
            if (parentfields!=null) {
                // have to clone the parent fields...
                for (Iterator i=parentfields.iterator();i.hasNext();) {
                    FieldDefs f=(FieldDefs)i.next();
                    FieldDefs newfield=
                      new FieldDefs(f.getGUIName(),f.getGUIType(),
                                    f.getGUISearch(), f.getGUIList(),
                                    f.getDBName(), f.getDBType(), f.getGUIPos(),
                                    f.getDBState());
                    newfield.setDBNotNull(f.getDBNotNull());
                    newfield.setDBKey(f.isKey());
                    newfield.setDBSize(f.getDBSize());
                    newfield.setDBDocType(f.getDBDocType());
                    newfield.setDBPos(pos++);
                    // also inherit the gui names
                    Iterator guinames = f.getGUINames().entrySet().iterator();
                    while(guinames.hasNext()) {
                        Map.Entry p = (Map.Entry) guinames.next();
                        newfield.setGUIName((String)p.getKey(), (String)p.getValue());
                    }
                    Iterator descriptions = f.getDescriptions().entrySet().iterator();
                    while(descriptions.hasNext()) {
                        Map.Entry p = (Map.Entry) descriptions.next();
                        newfield.setDescription((String)p.getKey(), (String)p.getValue());
                    }
                    results.add(newfield);
                    oldset.put(newfield.getDBName(),newfield);
                }
            }
        }
        for(Enumeration ns= getChildElements("builder.fieldlist","field"); ns.hasMoreElements(); ) {
            Element field = (Element)ns.nextElement();
            FieldDefs def=(FieldDefs)oldset.get(getElementValue(getElementByPath(field,"field.db.name")));
            if (def!=null) {
                decodeFieldDef(field,def);
            } else {
                def=decodeFieldDef(field);
                def.setDBPos(pos++);
                results.add(def);
            }
        }
        return results;
    }

    /**
     * Determine an integer value from an elements body.
     * Used for the List, Search, and Edit position values.
     * @param elm The element containing the value.
     * @return the parsed integer
     */
    private int getEditorPos(Element elm) {
        try {
            int val=Integer.parseInt(getElementValue(elm));
            return val;
        } catch(Exception e) {
            return -1;
        }
    }

    /**
     * Alter a specified, named FieldDef object using information obtained from the buidler configuration.
     * Only GUI information is retrieved and stored (name and type of the field sg=hould already be specified).
     * @since MMBase-1.6
     * @param elm The element containing the field information acc. to the buidler xml format
     * @param def The field definition to alter
     */
    private void decodeFieldDef(Element field, FieldDefs def) {
        Element tmp;
        String lang;
        // Gui
        Enumeration enum;

        Element descriptions = getElementByPath(field,"field.descriptions");
        if (descriptions!=null) {
            for (enum = getChildElements(descriptions,"description"); enum.hasMoreElements(); ) {
                tmp = (Element)enum.nextElement();
                lang = getElementAttributeValue(tmp,"xml:lang");
                def.setDescription(lang,getElementValue(tmp));
            }
        }

        Element gui = getElementByPath(field,"field.gui");
        if (gui!=null) {
            for (enum = getChildElements(gui,"guiname"); enum.hasMoreElements(); ) {
                tmp = (Element)enum.nextElement();
                lang = getElementAttributeValue(tmp,"xml:lang");
                def.setGUIName(lang,getElementValue(tmp));
            }
            // XXX: deprecated tag 'name'
            for(enum = getChildElements(gui,"name"); enum.hasMoreElements(); ) {
                tmp = (Element)enum.nextElement();
                lang = getElementAttributeValue(tmp,"xml:lang");
                def.setGUIName(lang,getElementValue(tmp));
            }
        }

        tmp = getElementByPath(gui,"gui.guitype");
        // XXX: deprecated tag 'type'
        if (tmp == null) {
            tmp = getElementByPath(gui,"gui.type");
        }
        if (tmp != null) {
            def.setGUIType(getElementValue(tmp));
        }
        // Editor
        Element editorpos = getElementByPath(field,"field.editor.input");
        if (editorpos!=null) {
            def.setGUIPos( getEditorPos(editorpos));
        }
        editorpos = getElementByPath(field,"field.editor.list");
        if (editorpos!=null) {
            def.setGUIList( getEditorPos(editorpos));
        }
        editorpos = getElementByPath(field,"field.editor.search");
        if (editorpos!=null) {
            def.setGUISearch( getEditorPos(editorpos));
        }
    }

    /**
     * Construct a FieldDef object using a field Element using information
     * obtained from the buidler configuration.
     * @param elm The element containing the field information acc. to the buidler xml format
     * @return def The field definition to alter
     */
    private FieldDefs decodeFieldDef(Element field) {
        // create a new FieldDefs we need to fill
        FieldDefs def=new FieldDefs();

        // DB
        Element db = getElementByPath(field,"field.db");
        def.setDBName(getElementValue(getElementByPath(db,"db.name")));
        def.setDBType(getDBType(getElementByPath(db,"db.type"),def));

        decodeFieldDef(field,def);

        return def;
    }

    /**
     * Set database type information for a specified, named FieldDef object
     * using information obtained from the buidder configuration.
     * @param elm The element containing the field type information acc. to the buidler xml format
     * @param def The field definition to set the type for
     */
    private int getDBType(Element dbtype,FieldDefs def) {
        String val = getElementValue(dbtype);
        def.setDBType(val);
        String size = getElementAttributeValue(dbtype,"size");
        try {
            def.setDBSize(Integer.parseInt(size));
        } catch (Exception e) {}
        String notnull = getElementAttributeValue(dbtype,"notnull");
        def.setDBNotNull(notnull != null && notnull.equalsIgnoreCase("true"));
        String key = getElementAttributeValue(dbtype,"key");
        def.setDBKey(key != null && key.equalsIgnoreCase("true"));
        String state = getElementAttributeValue(dbtype,"state");
        def.setDBState(state);
        String doctype = null;
        Attr doctypeattr= dbtype.getAttributeNode("doctype");
        if (doctypeattr!=null) {
            doctype = doctypeattr.getValue();
        }
        def.setDBDocType(doctype);
        return def.getDBType();
    }


    /**
     * Get the properties of this builder
     * @code-conventions return type should be Map
     * @return the properties in a Hashtable (as name-value pairs)
     */
    public Hashtable getProperties() {
        Hashtable results=new Hashtable();
        if (parentBuilder!=null) {
            Map parentparams= parentBuilder.getInitParameters();
            if (parentparams!=null) {
                results.putAll(parentparams);
            }
        }
        Element p;
        String name, value;
        for(Enumeration enum = getChildElements("builder.properties","property");
                        enum.hasMoreElements(); ) {
            p = (Element)enum.nextElement();
            name = getElementAttributeValue(p,"name");
            value = getElementValue(p);
            results.put(name,value);
        }
        return results;
    }


    /**
     * Get the descriptions of this builder
     * @code-conventions return type should be Map
     * @return the descriptions in a Hashtable, accessible by language
     */
    public Hashtable getDescriptions() {
        Hashtable results=new Hashtable();
        Element tmp;
        String lang;
        for (Enumeration enum = getChildElements("builder.descriptions","description");
             enum.hasMoreElements(); ) {
            tmp = (Element)enum.nextElement();
            lang = getElementAttributeValue(tmp,"xml:lang");
            results.put(lang,getElementValue(tmp));
        }
        if (results.isEmpty()) {
            if (parentBuilder!=null) {
                Map parentdescs= parentBuilder.getDescriptions();
                if (parentdescs!=null) {
                    results.putAll(parentdescs);
                }
            }
        }
        return results;
    }

    /**
     * Get the plural names of this builder
     * @code-conventions return type should be Map
     * @return the plural names in a Hashtable, accessible by language
     */
    public Hashtable getPluralNames() {
        Hashtable results=new Hashtable();
        Element tmp;
        String lang;
        for (Enumeration enum = getChildElements("builder.names","plural");
             enum.hasMoreElements(); ) {
            tmp = (Element)enum.nextElement();
            lang = getElementAttributeValue(tmp,"xml:lang");
            results.put(lang,getElementValue(tmp));
        }
        if (results.isEmpty()) { // nothing, thats little. Try parent.
            if (parentBuilder!=null) {
                Map parentnames= parentBuilder.getPluralNames();
                if (parentnames!=null) {
                    results.putAll(parentnames);
                }
            }
        }


        return results;
    }

    /**
     * Get the singular (GUI) names of this builder
     * @code-conventions return type should be Map
     * @return the singular names in a Hashtable, accessible by language
     */
    public Hashtable getSingularNames() {
        Hashtable results=new Hashtable();
        Element tmp;
        String lang;
        for (Enumeration enum = getChildElements("builder.names","singular");
             enum.hasMoreElements(); ) {
            tmp = (Element)enum.nextElement();
            lang = getElementAttributeValue(tmp,"xml:lang");
            results.put(lang,getElementValue(tmp));
        }
        if (results.isEmpty()) { // nothing, try parent, to have something at least..
            if (parentBuilder!=null) {
                Map parentnames= parentBuilder.getSingularNames();
                if (parentnames!=null) {
                    results.putAll(parentnames);
                }
                results.putAll(parentnames);
            }
        }
        return results;
    }


    /**
     * Get the builder that this builder extends
     *
     * @since MMBase-1.6
     * @return the parent as an MMObjectBuilder, or null if not specified or unresolved
     */
    public MMObjectBuilder getParentBuilder() {
        return parentBuilder;
    }

    /**
     * Get the name of the builder that this builder extends
     *
     * @since MMBase-1.6
     * @return the name of the parent builder
     */
    public String getBuilderExtends() {
        return getElementAttributeValue("builder","extends");
    }

    /**
     * Retrieve the (major) version number of this builder
     * @return the version as an integer.
     */
    public int getBuilderVersion() {
        String version = getElementAttributeValue("builder","version");
        if (version.equals("") && parentBuilder!=null) {
           return parentBuilder.getVersion();
        } else {
            int n = 0;
            if (!version.equals("")) {
                try {
                    n = Integer.parseInt(version);
                } catch (Exception f) {}
            }
            return n;
        }
    }

    /**
     * Retrieve the name of the maintainer of this builder
     * @return the name fo the maintainer as a String
     */
    public String getBuilderMaintainer() {
        String maintainer=getElementAttributeValue("builder","maintainer");
        if (maintainer.equals("")) {
            if (parentBuilder!=null) {
                maintainer=parentBuilder.getMaintainer();
            } else {
                maintainer="mmbase.org";
            }
        }
        return maintainer;
    }
}

