/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.security.implementation.basic;

import java.util.HashMap;

public interface LoginModule {
    public void load(HashMap properties);
    public boolean login(NameContext user, HashMap loginInfo,  Object[] parameters);
}
