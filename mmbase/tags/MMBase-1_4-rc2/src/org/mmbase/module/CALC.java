/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.module;

import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import org.mmbase.util.*;
import org.mmbase.util.logging.*;

/**
 * @author Daniel Ockeloen
 */
public class CALC extends ProcessorModule {
    private static Logger log = Logging.getLoggerInstance(CALC.class.getName());
 
	/**
	 * Generate a list of values from a command to the processor
	 */
	 public Vector  getList(scanpage sp,StringTagger tagger, String value) throws ParseException {
    	String line = Strip.DoubleQuote(value,Strip.BOTH);
		StringTokenizer tok = new StringTokenizer(line,"-\n\r");
		if (tok.hasMoreTokens()) {
			String cmd=tok.nextToken();	
		}
		return(null);
	}

	/**
	 * Execute the commands provided in the form values
	 */
	public boolean process(scanpage sp, Hashtable cmds,Hashtable vars) {
		log.debug("CMDS="+cmds);
		log.debug("VARS="+vars);
		return(false);
	}

	/**
	*	Handle a $MOD command
	*/
	public String replace(scanpage sp, String cmds) {
		StringTokenizer tok = new StringTokenizer(cmds,"-\n\r");
		if (tok.hasMoreTokens()) {
			String cmd=tok.nextToken();	
			while (tok.hasMoreTokens()) {
				cmd+="-"+tok.nextToken();
			}
			return(doCalc(cmd));
		}
		log.warn("Calc replace was involed with no decent command");
		return("No command defined");
	}
	
	String doCalc(String cmd) {
		log.service("Calc module calculates "+cmd);
		ExprCalc cl=new ExprCalc(cmd);
		log.warn("Calc converts number Natural number");
		return(""+(int)(cl.getResult()+0.5));
	}

	public String getModuleInfo() {
		return("Support routines simple calc, Daniel Ockeloen");
	}
}
