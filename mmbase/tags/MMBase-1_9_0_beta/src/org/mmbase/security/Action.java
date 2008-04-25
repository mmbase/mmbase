/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security;

import org.mmbase.util.functions.Parameters;
import org.mmbase.util.LocalizedString;

/**
 * An action is something which an authenticated user may want to do, but which is not directly
 * associated with MMBase nodes. Actions are e.g. provided by components (and can be added to
 * component XML's).
 *
 * @author Michiel Meeuwissen
 * @version $Id: Action.java,v 1.6 2008-01-21 17:28:15 michiel Exp $
 * @since MMBase-1.9
 */
public class Action implements java.io.Serializable {
    protected final String name;
    protected final LocalizedString description;
    protected final ActionChecker defaultChecker;
    protected final String nameSpace;

    public Action(String ns, String n, ActionChecker c) {
        nameSpace = ns;
        name = n;
        defaultChecker = c;
        description = new LocalizedString(name);
    }
    /**
     * Every action needs to do a proposal on how to check it. The security implementation may
     * override this. But since components can freely define new actions, which may not be
     * anticipated by  the authorization implementation, the action itself must provide some basic
     * checker (e.g. an instance of {@link ActionChecker.Rank}.
     */
    public ActionChecker getDefault() {
        return defaultChecker;
    }
    /**
     * Every action has a non-null name. Together with the {@link #getNameSpace} it uniquely
     * identifies the action.
     */
    public String getName() {
        return name;
    }

    /**
     * Most 'actions' have a namespace. This is normally identical to thye name of the component
     * with wich there are associated. It can be <code>null</code> though.
     */
    public String getNameSpace() {
        return nameSpace;
    }

    public LocalizedString getDescription() {
        return description;
    }

    public String toString() {
        return nameSpace + ":" + name + ":" + defaultChecker;
    }
    public Parameters createParameters() {
        return new Parameters();
    }
}
