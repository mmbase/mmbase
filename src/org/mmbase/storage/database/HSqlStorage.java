/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.database;

import java.util.*;
import org.mmbase.storage.*;
import org.mmbase.module.core.*;
import org.mmbase.module.corebuilders.FieldDefs;
import org.mmbase.util.logging.*;

/**
 * Implements Storage in an <a href="http://hsqldb.sourceforge.net/">HSql database</a>
 *
 * Typical for HSql is that 'constraints' must be defined after the
 * field definitions, and that the fieldNames must be lowercased
 * before they are returned to MMBase.
 *
 * @author Michiel Meeuwissen
 * @since MMBase-1.7
 * @version $Id: HSqlStorage.java,v 1.2 2003-05-02 14:44:00 michiel Exp $
 */
public class HSqlStorage extends RelationalDatabaseStorage {
    /**
     * Logging instance
     */
    private static Logger log = Logging.getLoggerInstance(HSqlStorage.class.getName());

    public HSqlStorage() {
        super();
    }

    public String mapToMMBaseFieldName(String fieldName) {
        return super.mapToMMBaseFieldName(fieldName).toLowerCase();
    }

    // hsql wanst the table constraint after the column definiation, here are they spared up.
    private Map tableConstraints = new HashMap();
    private Map indices          = new HashMap();


    /**
     * Hsql wants the contraint definition (uniuqe, primary key etc) _after_ the collumn definition, so not mixed.
     */
    
    protected String constructFieldDefinition(String tablename, String fieldname, int type, int size, int keyType) {
        log.debug("createing field definition for " + tablename);
        String name   = mapToTableFieldName(fieldname);
        String result = matchType(type,size);
        // cannot determine database type. log the error, but continue -
        // but note that the sql will likely fail!
        if (result == null) {
            log.error("Cannot determine database type for : " + FieldDefs.getDBTypeDescription(type)+"(" + size + ")");
        }
        StringBuffer constraints = (StringBuffer) tableConstraints.get(getFullTableName(tablename));      
        if (constraints == null) {
            constraints = new StringBuffer(); 
        } 
        
        if (keyType == KEY_PRIMARY) {            
            constraints.append(", ").append(applyPrimaryKeyScheme(name, result));
        } else if (keyType == KEY_SECONDARY) {
            constraints.append(", ").append(applyKeyScheme(name, result, name));
        } else if (keyType == KEY_FOREIGN) {
            constraints.append(", ").append(applyForeignKeyScheme(name, result, getFullTableName("object")));
        } else if (keyType == KEY_NOTNULL) {
            return  applyNotNullScheme(name, result);
        }
        
        tableConstraints.put(getFullTableName(tablename), constraints);

        return name + " " + result;
    }



    /*
create table documenation for hsql:

CREATE [ MEMORY | CACHED | TEMP | TEXT ] TABLE name
( columnDefinition [, ...] [, constraintDefinition...]) ;

Creates a tables in the memory (default) or on disk and only cached in memory. Identity columns are autoincrement columns. They must be integer columns and are automatically primary key columns. The last inserted value into an identity column for a connection is available using the function IDENTITY(), for example (where Id is the identity column):
INSERT INTO Test (Id, Name) VALUES (NULL,'Test'); CALL IDENTITY();

columnDefinition:
columnname Datatype [(columnSize[,precision])] [DEFAULT 'defaultValue'] [[NOT] NULL] [IDENTITY] [PRIMARY KEY]

the default value must be enclosed in singlequotes

constraintDefinition:
[ CONSTRAINT name ]
UNIQUE ( column [,column...] ) |
PRIMARY KEY ( column [,column...] ) |
FOREIGN KEY ( column [,column...] ) REFERENCES refTable ( column [,column...]) [ON DELETE CASCADE]
*/

    // javadoc inherited
    protected String createSQL(String tableName, String fields, String parentTableName, String parentFields) {
        log.debug("Creating SQL for " + tableName);
        if (!supportsExtendedTables() && (parentFields != null) && (! "".equals(parentFields))) {
            if (fields == null || "".equals(fields)) {
                fields = parentFields;
            } else {
                fields = parentFields + ", " + fields;
            }
        }       
        StringBuffer constraints = (StringBuffer) tableConstraints.get(tableName);
        if (constraints != null) {
            log.debug("using constraints " + constraints);
            fields = fields + constraints.toString();
        }
        return applyCreateScheme(tableName, fields, parentTableName)+";";
    }


}
