/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

/**
 *  This class is somekinda enumeration of the operations possible within 
 *  the security context
 */
public final class Operation {
    /** int value for the read Operation*/
    public final static int READ_INT = 0;
    
    /** int value for the write Operation*/    
    public final static int WRITE_INT = 1;
    
    /** int value for the create Operation*/    
    public final static int CREATE_INT = 2;
    
    /** int value for the link Operation */    
    public final static int LINK_INT = 3;
       
    /** int value for the remove Operation */    
    public final static int DELETE_INT = 4;

    /** int value for the remove Operation */    
    public final static int CHANGECONTEXT_INT = 6;

    /** Identifier for read operation, which is used for reading information*/    
    public final static Operation READ = new Operation(READ_INT, "read");
    
    /** Identifier for write operation, which is used for writing information*/        
    public final static Operation WRITE = new Operation(WRITE_INT, "write");
    
    /** 
     *	Identifier for create operation, which is used for creating a new node.
     *	This only applies on NodeManagers (builders)
     */            
    public final static Operation CREATE = new Operation(CREATE_INT, "create");
    
    /** 
     *	Identifier for link operation, which is used when creating a relation 
     *	between 2 nodes.
     */                
    public final static Operation LINK = new Operation(LINK_INT, "link");
    
    /** Identifier for remove operation, which is used when removing a node */
    public final static Operation DELETE = new Operation(DELETE_INT, "delete");

    /** Identifier for change context operation, which is used when changing the context of a node */                    
    public final static Operation CHANGECONTEXT = new Operation(CHANGECONTEXT_INT, "change context");

    /**
     *	Private constructor, to prevent creation of new Operations
     */
    private Operation(int level, String description) {
    	this.level = level;
	this.description = description;
    }

    /**
     *	This method gives back the internal int value of the Operation,
     *	which can be used in switch statements
     *	@return the internal int value
     */
    public int getInt(){
    	return level;
    }
  
    /**
     *	@return a string containing the description of the operation
     */
    public String toString(){
    	return description;
    }
  
    /** the int value of the instance */
    private int level;
    
    /** the description of this operation */    
    private String description;
}
