/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.applications.editwizard;

import org.w3c.dom.*;

/**
 * EditWizard
 * @javadoc
 * @author Kars Veling
 * @since   MMBase-1.6
 * @version $Id: ConnectorCommandGetList.java,v 1.5 2003-03-04 13:27:08 nico Exp $
 */

public class ConnectorCommandGetList extends ConnectorCommand {

    /**
     * @javadoc
     */
    public ConnectorCommandGetList(Node query) throws WizardException {
        super("getlist");
        addQuery(query);
    }

    /**
     * @javadoc
     */
    public void addQuery(Node query) {
        addCommandNode(query.cloneNode(true));
    }
}
