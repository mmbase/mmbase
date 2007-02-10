/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.security.implementation.basic;

import java.util.Map;
import java.util.HashMap;

import org.mmbase.security.Rank;

/**
 * Support for authentication method 'class' for 'basic' authentication.
 *
 * @author  Michiel Meeuwissen
 * @version $Id: ClassLoginModule.java,v 1.5 2007-02-10 16:22:38 nklasens Exp $
 * @since   MMBase-1.8
 */
public class ClassLoginModule implements LoginModule {

    private Map<String, String> ranks = new HashMap();

    public void load(Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof String) {
                ranks.put(entry.getKey(), (String) entry.getValue());
            }
        }
    }

    public boolean login(NameContext user, Map loginInfo,  Object[] parameters) {
        org.mmbase.security.classsecurity.ClassAuthentication.Login li = org.mmbase.security.classsecurity.ClassAuthentication.classCheck("class");
        if (li == null) {
            throw new SecurityException("Class authentication failed (class not authorized)");
        }
        String userName = li.getMap().get("username");

        String r = ranks.get(userName);
        Rank rank = r == null ? Rank.BASICUSER : Rank.getRank(r);
        user.setIdentifier(userName);
        user.setRank(rank);
        return true;
    }
}
