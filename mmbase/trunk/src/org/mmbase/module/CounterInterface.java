/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import org.mmbase.module.sessionInfo;
import org.mmbase.util.scanpage;

public interface CounterInterface
{
	public String getTag( String part, sessionInfo session, scanpage sp );
}
