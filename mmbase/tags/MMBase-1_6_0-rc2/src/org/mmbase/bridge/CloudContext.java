/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.bridge;
import java.util.Map;

/**
 * The collection of clouds and modules within a Java Virtual Machine.
 *
 * @author Rob Vermeulen
 * @author Pierre van Rooden
 * @author Jaco de Groot
 * @version $Id: CloudContext.java,v 1.19 2002-11-05 14:17:02 pierre Exp $
 */
public interface CloudContext {

    /**
     * Returns all modules available in this context.
     *
     * @return all available modules
     */
    public ModuleList getModules();

    /**
     * Returns the module with the specified name.
     *
     * @param name                the name of the module to be returned
     * @return                    the requested module
     * @throws NotFoundException  if the specified module could not be found
     */
    public Module getModule(String name) throws NotFoundException;

    /**
     * Returns whether the module with the specified name is available.
     *
     * @param name                the name of the module
     * @return                    <code>true</code> if the module is available
     */
    public boolean hasModule(String name);

    /**
     * Returns the cloud with the specified name.
     *
     * @param name                     the name of the cloud to be returned
     * @return                         the requested cloud
     * @throws CloudNotFoundException  if the specified cloud could not be found
     */
    public Cloud getCloud(String name);

    /**
     * Returns the cloud with the specified name, with authentication
     *
     * @param name                the name of the cloud to be returned
     * @param authenticationtype  the type of authentication, which should be
     *                            used by the authentication implementation.
     * @param loginInfo           the user related login information.
     * @return                    the requested cloud
     * @throws NotFoundException  if the specified cloud could not be found
     */
    public Cloud getCloud(String name, String authenticationtype, Map loginInfo) throws NotFoundException;

    /**
     * Returns the names of all the clouds known to the system
     *
     * @return  A StringList of all clouds names known to our Context
     */
    public StringList getCloudNames();

    /**
     * Returns the default character encoding, which can be used as a default.
     *
     * @return  A string with the character encoding
     * @since   MMBase-1.6
     *
     */
    public String getDefaultCharacterEncoding();


    /**
     * Returns the default locale setting.
     *
     * @return  A Locale object
     * @since   MMBase-1.6
     */
    public java.util.Locale getDefaultLocale();
    
    /**
     * Returns a new, empty field list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public FieldList createFieldList();
    
    /**
     * Returns a new, empty node list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public NodeList createNodeList();
    
    /**
     * Returns a new, empty relation list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public RelationList createRelationList();
    
    /**
     * Returns a new, empty node manager list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public NodeManagerList createNodeManagerList();
    
    /**
     * Returns a new, empty relation manager list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public RelationManagerList createRelationManagerList();
    
    /**
     * Returns a new, empty module list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public ModuleList createModuleList();
    
    /**
     * Returns a new, empty string list
     *
     * @return  The empty list
     * @since   MMBase-1.6
     */
    public StringList createStringList();
    
 }
