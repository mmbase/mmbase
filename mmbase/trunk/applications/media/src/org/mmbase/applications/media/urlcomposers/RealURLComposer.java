/*
  
This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.
  
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
  
*/

package org.mmbase.applications.media.urlcomposers;
import org.mmbase.applications.media.builders.MediaFragments;
import org.mmbase.applications.media.Format;
import org.mmbase.module.core.MMObjectNode;
import java.util.*;

/**
 * A RealURLComposer is an URLComposer which can produce URL's to RM/RA streams.
 *
 * @author Michiel Meeuwissen
 * @author Rob Vermeulen (VPRO)
 */

public class RealURLComposer extends FragmentURLComposer  {

    protected StringBuffer getURLBuffer() {
        StringBuffer buff = super.getURLBuffer();
        if (getFormat().equals(Format.RM)) {
            return getRMArgs(buff, fragment, info);
        } else {
            return buff;
        }
    }

    public static StringBuffer getRMArgs(StringBuffer args, MMObjectNode fragment, Map info) {
        if ("true".equals(info.get("nude"))) return args;
        if (fragment != null) { // can add this for RM-sources
            long start = fragment.getLongValue("start");
            long end   = fragment.getLongValue("stop");
            char sep = '?';
            if (start > -1) {            
                appendTime(start, args.append(sep).append("start="));
                sep = '&';
            }
            if (end > -1) {            
                appendTime(end, args.append(sep).append("end="));
                sep = '&';
            }
            
            // real...
            String title = fragment.getStringValue("title");
            args.append(sep).append("title=").append(title);
        } 
        return args;
    }
    

    /**
     * Script accept times that look like dd:hh:mm:ss.th, where t is tenths of seconds.
     * @param time the time in milliseconds
     * @return the time in real format
     */
    public static StringBuffer appendTime(long time, StringBuffer buf) {
        time /= 10; // in centis

        long centis = -1;
        long s     = 0;
        long min   = 0;
        long h     = 0;
        long d     = 0;

        if (time != 0) {
            centis = time % 100;
            time /= 100;  // in s
            if (time != 0) {
                s = time % 60;
                time /= 60;  // in min
                if (time != 0) {
                    min = time % 60;
                    time /= 60; // in hour
                    if (time != 0) {
                        h = time % 24;
                        d = time / 24;   // in day
                    }
                }
            }
        }
        boolean append = false;

        if (d > 0) append = true;
        if (append) buf.append(d).append(':');
        if (h > 0) append = true;
        if (append) buf.append(h).append(':');
        if (min > 0) append = true;
        if (append) buf.append(min).append(':');
        buf.append(s);
        if (centis > 0) {
            buf.append('.');
            if (centis < 10) buf.append('0');
            buf.append(centis);
        }
        
        return buf;
    }
    
    /**
     * Removes RealPlayer incompatible characters from the string.
     * '#' characters are replaced by space characters.
     * Characters that are allowed are every letter or digit and ' ', '.', '-' and '_' chars.

     *
     * @param s the String that needs to be fixed.
     * @return a realPlayer compatible String.
     */
    public static String makeRealCompatible(String s) {
        if (s != null) {
            char[] sArray = s.replace('#',' ').toCharArray();
            char[] dArray = new char[sArray.length];
            
            int j = 0;
            for (int i=0;i<sArray.length;i++) {
                if (Character.isLetterOrDigit(sArray[i]) ||(sArray[i]==' ')||(sArray[i]=='.')||(sArray[i]=='-')||(sArray[i]=='_')) {
                    dArray[j] = sArray[i];
                    j++;
                }
            }
            return (new String(dArray)).substring(0,j);
        }
        return "";
        
    }
    

}
