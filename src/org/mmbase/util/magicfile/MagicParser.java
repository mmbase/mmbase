/*
 
This software is OSI Certified Open Source Software.  OSI Certified is
a certification mark of the Open Source Initiative.
 
The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license
 
 */

package org.mmbase.util.magicfile;

import java.util.*;
import java.io.*;

import org.mmbase.util.logging.*;
/**
 * This Parser translates the configuration file of UNIX's file to a
 * list of Detectors (and to a magic.xml) Perhaps it's easier to
 * rewrite this stuff to perl or something like that.
 *
 * @version: $Id: MagicParser.java,v 1.2 2003-01-24 14:24:22 kees Exp $
 * NOT TESTED YET
 */

public class MagicParser implements DetectorProvider {
    private static Logger log = Logging.getLoggerInstance(MagicParser.class.getName());
    private List detectors;
    
    // what a mess:
    // I think all of these members must be removed:
    private boolean parsingFailure = false;
    private int offset;
    private String type;
    private String typeAND;
    private String test;
    private String message;
    private char testComparator;
    
    
    public MagicParser(){
        try {
            // BufferedReader br = new BufferedReader(new FileReader(new File("/etc/mime-magic")));
            BufferedReader br = new BufferedReader(new FileReader(new File("/usr/share/magic/magic.mime")));
            String line;
            detectors = new Vector();
            
            while((line = br.readLine()) != null){
                Detector d= createDetector(line);
                if (d != null){
                    System.out.println(d.toString());
                    detectors.add(d);
                }
            }
        } catch (Exception e ){
            System.out.println(""+ e.getMessage() +"}");
        };
    }
    
    public List getDetectors() {
        return detectors;
    }
    // --------------------------------------------------------------------------------
    // some utitily functions
    
    protected int nextWhiteSpace(String s) {
        return nextWhiteSpace(s, 0);
    }
    
    protected int nextWhiteSpace(String s, int startIndex) {
        for (int j=startIndex; j < s.length(); j++) {
            if (s.charAt(j) == ' ' || s.charAt(j) == '\t' || s.charAt(j) == '\n') {
                return j;
            }
        }
        return s.length();
    }
    
    protected int nextNonWhiteSpace(String s, int startIndex) {
        for (int j=startIndex; j < s.length(); j++) {
            if (s.charAt(j) != ' ' && s.charAt(j) != '\t') {
                return j;
            }
        }
        return -1;
    }
    
    
    /**
     * Separate command from offset
     * @exception Throws an exception when parsing failed
     */
    private int parseOffsetString(String s,int startIndex) throws Exception {
        try {
            int m = nextWhiteSpace(s,startIndex);
            
            // Bail out when encountering an indirect offset
            char c = s.charAt(startIndex);
            // '&': In sublevel we can start relatively to where the previous match ended
            // '(': Read value at first address, and add that at second to it
            if (c == '&') {
                parsingFailure = true;
                throw new UnsupportedOperationException("parseOffsetString: >& offset feature not implemented\n(Tt is used only for HP Printer Job Language type)");
            } else if (c == '(') {
                parsingFailure = true;
                throw new UnsupportedOperationException("parseOffsetString: indirect offsets not implemented");
            }
            offset = Integer.decode(s.substring(startIndex,m)).intValue();
            return nextNonWhiteSpace(s,m+1);
        } catch (NumberFormatException e) {
            // log.error("string->integer conversion failure for '"+s+"'");
            throw new Exception("parseOffetString: string->integer conversion failure for '"+s+"'");
        }
    }
    
    /**
     * Parse the type string from the magic file
     *
     *   -- nothing to be done: the found string is already atomic :-)
     */
    private int parseTypeString(String s, int startIndex) throws Exception {
        int m = nextWhiteSpace(s,startIndex);
        if (m <= startIndex) {
            throw new Exception("parseTypeString: failed to delimit type string");
        }
        int n = s.indexOf('&',startIndex);
        if (n > -1 && n < m-2) {
            type = s.substring(startIndex,n);
            typeAND = s.substring(n+1,m);
        } else {
            type = s.substring(startIndex,m);
            typeAND = "0";
        }
        return nextNonWhiteSpace(s,m+1);
    }
    
    /**
     * Parse the test string from the magic file
     *   -- determine: a.) the test comparator, and b.) the test value
     */
    private int parseTestString(String s, int startIndex) throws Exception {
        int start = 0;
        //int m = nextWhiteSpace(s,startIndex); // XXX need a better algorithm to account for '\' syntax
        // Can't use nextWhiteSpace here, we need harder parsing...
        boolean backslashmode = false;
        boolean octalmode = false;
        boolean hexmode = false;
        //int l = s.length();
        char c;
        StringBuffer numbuf = new StringBuffer();
        
        test = "";
        
        c = s.charAt(startIndex);
        switch (c) {
            case '>':
            case '<':
            case '&':
            case '^':
            case '=':testComparator = c; start = 1; break;
            default: testComparator = '='; break;
        }
        if (s.charAt(startIndex+start) == '~' || s.charAt(startIndex+start) == '!') {
            // XXX do nothing with these, but remove them to get rid of decode errors
            start++;
        }
        int i = startIndex+start;
        
        if (!type.equals("string")) {
            int m = nextWhiteSpace(s,i);
            String t = s.substring(i,m);
            if (t.equals("x")) {
                test = "x";
            } else if (type.equals("beshort") || type.equals("leshort")) {
                try {
                    test = "0x"+Integer.toHexString(Integer.decode(s.substring(i,m)).intValue());
                    //test.addElement(Integer.decode(s.substring(i,m)));
                } catch (NumberFormatException e) {
                    throw new Exception("decode("+s.substring(i,m)+")");
                }
            } else if (type.equals("belong") || type.equals("lelong")) {
                // Values possibly too long for Integer, while Long type won't parse :-(
                int endIndex = m;
                try {
                    //test.addElement(Long.decode(s.substring(i,m)));
                    if (s.charAt(m-1) == 'L' || s.charAt(m-1) == 'l') {
                        endIndex = m-1;
                    }
                    test = "0x"+Long.toHexString(Long.decode(s.substring(i,endIndex)).longValue());
                } catch (NumberFormatException e) {
                    log.error(e.getMessage());
                    log.error(Logging.stackTrace(e));
                    throw new Exception("parseLong("+s.substring(i,endIndex)+") ");
                }
            } else if (type.equals("byte")) {
                try {
                    test = "0x"+Integer.toHexString(Integer.decode(s.substring(i,m)).intValue());
                    //test.addElement(Integer.decode(s.substring(i,m)));
                } catch (NumberFormatException e) {
                    throw new Exception("decode("+s.substring(i,m)+")");
                }
            }
            i = m;
        } else {
            StringBuffer buf = new StringBuffer();
            
            int testIndex = 0;
            int m = s.length();
            int m1 = i;
            while (i<m) {
                c = s.charAt(i);
                if (backslashmode) {
                    switch (c) {
                        case 'n': backslashmode = false; buf.append('\n'); break;
                        case 'r': backslashmode = false; buf.append('\r'); break;
                        case 't': backslashmode = false; buf.append('\t'); break;
                        case '\\':
                            if (hexmode) {
                                try {
                                    //test.addElement(Integer.decode("0x"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0x"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0x"+numbuf.toString()+") faalde");
                                }
                                hexmode = false;
                            } else if (octalmode) {
                                try {
                                    //test.addElement(Integer.decode("0"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0"+numbuf.toString()+") faalde");
                                }
                                octalmode = false;
                            } else {
                                backslashmode = false;
                                buf.append('\\');
                            }
                            break;
                        case 'x':
                            if (octalmode && numbuf.length()==3) {
                                try {
                                    //test.addElement(Integer.decode("0"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0"+numbuf.toString()+") faalde");
                                }
                                octalmode = false;
                                backslashmode = false;
                                buf = new StringBuffer();
                                buf.append('x');
                            } else {
                                hexmode = true;
                                numbuf = new StringBuffer();
                                if (buf.length()>0) {
                                    test = test + buf.toString();
                                    buf = new StringBuffer();
                                }
                            }
                            break;
                        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                            // We should be in octalmode or hexmode here!!
                            if (!octalmode && !hexmode) {
                                if (buf.length()>0) {
                                    //test.addElement(buf.toString());
                                    test = test + buf.toString();
                                    buf = new StringBuffer();
                                }
                                octalmode = true;
                                numbuf = new StringBuffer();
                            }
                            numbuf.append( c );
                            break;
                        case ' ':
                            if (octalmode) {
                                try {
                                    //test.addElement(Integer.decode("0"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0"+numbuf.toString()+") faalde");
                                }
                                octalmode = false;
                            } else if (hexmode) {
                                try {
                                    //test.addElement(Integer.decode("0x"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0x"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0x"+numbuf.toString()+") faalde");
                                }
                                hexmode = false;
                            } else {
                                buf.append(' ');
                            }
                            backslashmode = false;
                            break;
                        default:
                            if (hexmode) {
                                if (c == 'a' || c == 'A' || c == 'b' || c == 'B' || c == 'c' || c == 'C' || c == 'd' || c == 'D' ||
                                c == 'e' || c == 'E' || c == 'f' || c == 'F') {
                                    numbuf.append(c);
                                } else {
                                    try {
                                        //test.addElement(Integer.decode("0x"+numbuf.toString()));
                                        test = test + (char)Integer.decode("0x"+numbuf.toString()).intValue();
                                    } catch (NumberFormatException e) {
                                        throw new Exception("decode(0x"+numbuf.toString()+") faalde");
                                    }
                                    hexmode = false;
                                    backslashmode = false;
                                }
                            } else if (octalmode) {
                                try {
                                    //test.addElement(Integer.decode("0"+numbuf.toString()));
                                    test = test + (char)Integer.decode("0"+numbuf.toString()).intValue();
                                } catch (NumberFormatException e) {
                                    throw new Exception("decode(0"+numbuf.toString()+") faalde");
                                }
                                octalmode = false;
                                backslashmode = false;
                            } else {
                                backslashmode = false;
                                //tmp[testIndex++] = charToByte(c);
                                buf.append(c);
                            }
                    }
                } else if (c == '\\') {
                    if (buf.length()>0) {
                        //test.addElement(buf.toString());
                        test = test + buf.toString();
                        buf = new StringBuffer();
                    }
                    backslashmode = true;
                } else if (c == ' ' || c == '\t' || c == '\n' || i==m-1) {  // Don't forget to set values on end of string
                    if (buf.length() > 0) {
                        //test.addElement(buf.toString());
                        test = test + buf.toString();
                        buf = new StringBuffer();
                    }
                    if (numbuf.length() >0) {
                        if (octalmode) {
                            try {
                                //test.addElement(Integer.decode("0"+numbuf.toString()));
                                test = test + (char)Integer.decode("0"+numbuf.toString()).intValue();
                            } catch (NumberFormatException e) {
                                throw new Exception("decode(0"+numbuf.toString()+") faalde");
                            }
                            octalmode = false;
                            backslashmode = false;
                        } else if (hexmode) {
                            try {
                                //test.addElement(Integer.decode("0x"+numbuf.toString()));
                                test = test + (char)Integer.decode("0x"+numbuf.toString()).intValue();
                            } catch (NumberFormatException e) {
                                throw new Exception("decode(0x"+numbuf.toString()+") faalde");
                            }
                            hexmode = false;
                            backslashmode = false;
                        }
                    }
                    break;
                } else {
                    buf.append(c);
                }
                i++;
            }
        }
        //log.debug("test size = "+test.size());
        //log.debug("test = "+vectorToString(test));
        return nextNonWhiteSpace(s, i+1);
    }
    
    
    
    /**
     * Parse the message string from the magic file
     *
     *   -- nothing to be done: the found string is already atomic :-)
     */
    private int parseMessageString(String s, int startIndex) throws Exception {
        if (false) throw new Exception("dummy exception to stop jikes from complaining");
        message = s.substring(startIndex);
        return s.length()-1;
        
    }
    
    
    
    private Detector createDetector(String line) {
        String offsetString, typeString, testString, messageString;
        
        Detector detector = new Detector();
        // rawinput = line;
        
        // hasX = false;
        //xInt = -99;
        //xString = "default";
        //xChar = 'x';
        
        // parse line
        log.debug("parse: "+line);
        int n;
        String level = "start";
        try {
            level = "parseOffsetString";
            n = parseOffsetString(line,0);
            level = "parseTypeString";
            n = parseTypeString(line,n);
            level = "parseTestString";
            n = parseTestString(line,n);
            // If there are multiple test level, an upper one doesn't have to have a message string
            if (n > 0) {
                level = "parseMessageString";
                n = parseMessageString(line,n);
            } else {
                message = "";
            }
            level = "end";
        } catch (UnsupportedOperationException e) {
            log.warn(e.getMessage());
        } catch (Exception e) {
            log.error("parse failure at "+level+": "+e.getMessage()+" for ["+line+"]");
            parsingFailure = true;
        }
        detector.setType(type);
        detector.setOffset("" + offset);
        detector.setTest(test);
        detector.setComparator(testComparator);
        detector.setMimeType(message);
        detector.setDesignation(message);
        return detector;
    }
    
    public boolean toXML(String path) throws IOException {
        File f = new File(path);
        return toXML(f);
    }
    
    /**
     * Write the current datastructure to an XML file
     * XXX Ugly and hardcoded paths
     */
    public boolean toXML(File f) throws IOException {
        FileWriter writer = new FileWriter(f);
        
        writer.write("<!DOCTYPE magic PUBLIC \"-//MMBase/Magic XML config 1.0//EN\" \"http://www.mmbase.org/dtd/magic_1_0.dtd\">\n<magic>\n<info>\n<version>0.1</version>\n<author>cjr@dds.nl</author>\n<description>Conversion of the UNIX 'magic' file with added mime types and extensions.</description>\n</info>\n<detectorlist>\n");
        Iterator i = getDetectors().iterator();
        while (i.hasNext()) {
            ((Detector)i.next()).toXML(writer);
        }
        writer.write("</detectorlist>\n</magic>\n");
        writer.close();
        return true;
    }
    
}
