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

import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.traversal.*;

import org.mmbase.util.*;

/**
 * @author cjr@dds.nl
 *
 * Config module
 *
 * This module analyses the MMBase XML configuration files.
 *
 * LIST functions are:
 *  - token=SHOW
 *  - type={builders,databases,modules}
 *  Returns an list of arrays:
 *   $ITEM1 = name of file
 *   $ITEM2 = DTD-based syntax check: false, nodtd, true
 *  and for builders and databases there is:
 *   $ITEM3 = whether the item is active: false, true
 *
 *
 * REPLACE functions are: SHOW, CHECK and ANNOTATE
 *    where the next argument is the directory, e.g. 'builders'
 *    and the third the name of the file without the .xml extension.
 *    SHOW: return a string with syntax colored XML file
 *    CHECK: return 1 if validates ok, -1 if not
 *    ANNOTATE: return a string with the XML file with errors listed
 *      and pointed out visually.
 *
 *    Example: $MOD-CONFIG-SHOW-builders-people  
 *
 * @version $Id: Config.java,v 1.8 2000-08-10 21:06:46 case Exp $
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.7  2000/08/06 14:56:18  case
 * cjr: Now tells if no dtd is defined - in this case no validation takes place and previously it was presented as if validation had been done successfully.
 *
 * Revision 1.6  2000/08/06 10:00:42  case
 * cjr: very minor change
 *
 *
 * TODO:
 * - Add code for examples
 * - Add code to check whether database configuration works
 * - Add code for fault oriented results, rather than directory oriented results
 */
public class Config extends ProcessorModule {

    private String classname = getClass().getName();
    private String configpath;

    private static boolean debug = false;

    class ParseResult {

        Vector warningList, errorList, fatalList,resultList;
        boolean hasDTD;
        String dtdpath;

        public ParseResult(String path) {
            hasDTD = false;
            dtdpath = null;
            try {

                DOMParser parser = new DOMParser();
                parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
                parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);

                XMLCheckErrorHandler errorHandler = new XMLCheckErrorHandler();
                parser.setErrorHandler(errorHandler);

                //use own entityResolver for converting the dtd's url to a local file
                XMLEntityResolver resolver = new XMLEntityResolver();
                parser.setEntityResolver((EntityResolver)resolver);

                parser.parse(path);
                hasDTD = resolver.hasDTD();
                dtdpath = resolver.getDTDPath();

                Document document = parser.getDocument();

                warningList = errorHandler.getWarningList();
                errorList = errorHandler.getErrorList();
                fatalList = errorHandler.getFatalList();

                resultList = errorHandler.getResultList();

            } catch (Exception e) {
                warningList = new Vector();
                errorList = new Vector();

                ErrorStruct err = new ErrorStruct("fatal error",0,0,e.getMessage());

                fatalList = new Vector();
                fatalList.addElement(err);
                resultList = new Vector();
                resultList.addElement(err);

                if (debug) {
                    debug("ParseResult error: "+e.getMessage());
                }
            }
        }

        public Vector getResultList() {
            return resultList;
        }

        public Vector getWarningList() {
            return warningList;
        }

        public Vector getErrorList() {
            return errorList;
        }

        public Vector getFatalList() {
            return fatalList;
        }

        public boolean hasDTD() {
            return hasDTD;
        }

        public String getDTDPath() {
            return dtdpath;
        }
    }

    public boolean builderIsActive(String path) {
        XMLBuilderReader reader = new XMLBuilderReader(path);
        return reader.getStatus().equalsIgnoreCase("active");
    }

    public boolean databaseIsActive(String path) {
        XMLProperties xmlReader = new XMLProperties();
        SAXParser parser = new SAXParser();
        // Telling the parser it must not use some features
        // we're not using right now as dtd-validation and namespaces
        try {
            parser.setFeature("http://xml.org/sax/features/validation",false);
            parser.setFeature("http://xml.org/sax/features/namespaces",false);
        } catch (SAXNotSupportedException ex) {
            debug("Config::databaseIsActive: failed because parser doesn't support feature");
            ex.printStackTrace();
        }
        catch (SAXNotRecognizedException ex) {
            debug("Config::databaseIsActive(): failed because parser didn't recognized feature");
            ex.printStackTrace();
        }
        // create new ContentHandler and let the parser use it
        xmlReader = new XMLProperties();
        parser.setContentHandler(xmlReader);

        // get us a (normal) propertie reader
        Hashtable mods = null;

        // load the
        String filename=mmbaseconfig+"/modules/mmbaseroot.xml";
        // filename=filename.replace('/',(System.getProperty("file.separator")).charAt(0));
        // filename=filename.replace('\\',(System.getProperty("file.separator")).charAt(0));

        // check if there's a xml-configuration file
        try {
            parser.parse(new InputSource(filename));
            mods = xmlReader.getProperties();
        } catch (Exception e) {}



        String curdb = (String)mods.get("DATABASE");
        return path.indexOf(curdb) > 0;
    }

    /**
     * Implement a FilenameFilter for xml files
     */
    public class XMLFilenameFilter implements FilenameFilter {
        public boolean accept(File directory, String name) {
            if (name.endsWith(".xml")) {
                return true;
            } else {
                return false;
            }
        }
    }

    public void init() {
        configpath = System.getProperty("mmbase.config");
        if (configpath.endsWith(File.separator)) {
            configpath = configpath.substring(0,configpath.length()-1);
        }
    }


    public void reload() {}



    public void onload() {}



    public void unload() {}



    public void shutdown() {}


    /**
     * Config, a support module for servscan
     */
    public Config() {}

    /**
     * Generate a list of values from a command to the processor
     */
    public Vector  getList(scanpage sp,StringTagger tagger, String value) throws ParseException {
        Vector v = new Vector();

        String line = Strip.DoubleQuote(value,Strip.BOTH);
        StringTokenizer tok = new StringTokenizer(line,"-\n\r");

        String[] argv = new String[tok.countTokens()];
        for (int i=0; i<tok.countTokens(); i++) {
            argv[i] = tok.nextToken();
        }

        String category = tagger.Value("category");

        try {
            if (argv[0].equalsIgnoreCase("show")) {
                if (argv.length == 1) {
                    if (category == null || category.equals("")) {
                        // Show main configuration file categories
                        return listConfigDirectories(configpath);
                    } else {
                        Vector item1List = listDirectory(configpath+File.separator+category);
                        Vector item2List = new Vector();
                        Vector item3List = new Vector();
                        Enumeration enum = item1List.elements();
                        String path;
                        while (enum.hasMoreElements()) {
                            path = configpath+File.separator+category+File.separator+(String)enum.nextElement()+".xml";
                            item2List.addElement(path);
                            if (category.equalsIgnoreCase("builders")) {
                                item3List.addElement(builderIsActive(path) ? "true" : "false");
                            } else if (category.equalsIgnoreCase("databases")) {
                                item3List.addElement(databaseIsActive(path) ? "true" : "false");
                            } else if (category.equalsIgnoreCase("applications")) {
                                // bla
                            }


                        }
                        int n = item1List.size();
                        int rescheck;
                        String dtdpath;
                        for (int i=0;i<n;i++) {
                            v.addElement(item1List.elementAt(i));
                            //v.addElement(checkXMLOk((String)item2List.elementAt(i)) ? "true" : "false");
                            rescheck = checkXMLOk((String)item2List.elementAt(i));

                            switch (rescheck) {
                            case -1:
                                v.addElement("false");
                                break;
                            case 0:
                                v.addElement("nodtd");
                                break;
                            case 1:
                                v.addElement("true");
                            }
                            if (category.equalsIgnoreCase("builders") || category.equalsIgnoreCase("databases")) {
                                v.addElement(item3List.elementAt(i));
                            }
                        }
                        if (category.equalsIgnoreCase("builders") || category.equalsIgnoreCase("databases")) {
                            tagger.setValue("ITEMS","3");
                        } else {
                            tagger.setValue("ITEMS","2");
                        }
                        return v;
                    }
                } else if (argv.length == 2) {
                    v.addElement("arg = "+argv[1]);
                } else if (argv.length == 3) {
                    v.addElement("arg = "+argv[1]+","+argv[2]);
                }
            }
            return v;
        } catch (IOException e) {
            return null;
        }
    }


    /**
     * @param path Path to root of configuration files
     * @return Vector containing the names of the main configuration directories
     */
    public Vector listConfigDirectories(String path) {
        Vector v = new Vector();
        File dir = new File(path);
        String[] list = dir.list();
        for (int i=0;i<list.length;i++) {
            File f = new File(configpath+File.separator+list[i]);
            if (!list[i].equalsIgnoreCase("CVS") && f.isDirectory()) {
                v.addElement(list[i]);
            }
        }
        return v;
    }

    /**
     * Retrieve all xml files in a directory
     * 
     * @param path Directory path
     * @return String array containing the names of the xml files in the directory
     *
     */
    protected Vector listDirectory(String path) throws IOException {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            throw new IOException("Path "+path+" is not a directory.\n");
        } else {
            String[] dirlist = dir.list(new XMLFilenameFilter());
            Vector v = new Vector();
            for (int i=0;i<dirlist.length;i++) {
                v.addElement(dirlist[i].substring(0,dirlist[i].length()-4));
            }
            return v;
        }
    }

    /**
     * Execute the commands provided in the form values
     */
    public boolean process(scanpage sp, Hashtable cmds,Hashtable vars) {
        debug("CMDS="+cmds);
        debug("VARS="+vars);
        return(false);
    }


    /**
     *	Handle a $MOD command
     */
    public String replace(scanpage sp, String cmds) {
        String[] dirlist;

        int level = 1;
        StringTokenizer tok = new StringTokenizer(cmds,"-\n\r");
        int count = tok.countTokens();
        String[] argv = new String[count];
        for (int i=0; i<count; i++) {
            argv[i] = tok.nextToken();
        }

        if (argv.length != 3) {
            return "$MOD-CONFIG should have three arguments, e.g. $MOD-CONFIG-show-builders-people";
        } else {
            String dir = argv[1];
            String filename = argv[2]+".xml";
            String path = configpath+File.separator+dir+File.separator+filename;

            if (argv[0].equalsIgnoreCase("SHOW")) {
                return prettyPrintXML(path);
            } else if (argv[0].equalsIgnoreCase("CHECK")) {
                return checkXML(path);
            } else if (argv[0].equalsIgnoreCase("ANNOTATE")) {
                return annotateXML(path);
            }
        }
        return "dummy";
    }

    public String getModuleInfo() {
        return("Analysis of mmbase configuration, cjr@dds.nl");
    }


    private void debug( String msg ) {
        System.out.println( classname +":"+msg );
    }

    /*
     * @param out PrintWriter to http result
     * @param path Path to the builder file
     * @return Prettified version of the XML file as a string
     */
    protected String prettyPrintXML(String path) {
        XMLScreenWriter screen = new XMLScreenWriter(path);
        StringWriter out = new StringWriter();
        try {
            screen.write(out);
            return out.toString();
        } catch (IOException e) {
            return "Config::prettyPrintXML("+path+"), IOException: "+e.getMessage();
        }
    }


    /**
     * Do validity check on XML file
     *
     * @return <ul>
     * <li>0: No DTD defined
     * <li>1: ok
     * <li>-1: false
     */
    protected int checkXMLOk(String path) {
        ParseResult pr = new ParseResult(path);
        if (!pr.hasDTD()) {
            //System.out.println("checkXMLOk[nodtd] - 0");
            return 0;
        } else {
            for (int i=0; i<pr.getResultList().size();i++) {
                ErrorStruct err = (ErrorStruct)pr.getResultList().elementAt(i);
            }
            return (pr.getResultList().size() == 0) ? 1 : -1;
        }
    }

    /**
     * @return String with '<' and '>' converted to respectively &lt; and &gt;
     */
    protected String htmlEntities(String s) {
        StringBuffer res = new StringBuffer();
        char c;
        for (int i=0;i<s.length();i++) {
            c = s.charAt(i);
            switch (c) {
            case '>':
                res.append("&gt;");
                break;
            case '<':
                res.append("&lt;");
                break;
            default:
                res.append(c);
            }
        }
        return res.toString();
    }


    protected String annotateXML(String path) {
        if (checkXMLOk(path)>=0) {
            return checkXML(path);
        } else {
            // XXX Stupid, now I'm parsing the darned file again!
            ParseResult pr = new ParseResult(path);
            Vector v = pr.getResultList();
            StringBuffer res = new StringBuffer();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(path));
                String line;
                int lineno = 0;
                int j = 0;
                ErrorStruct err = (ErrorStruct)pr.getResultList().elementAt(j++);

                int nextLine = err.getLineNumber();
                StringBuffer marker;
                res.append("<PRE>");
                if (nextLine == 0) {
                    res.append("<font color=red>"+err.getMessage()+"</font>\n");
                    if (j < pr.getResultList().size()-1) {
                        err = (ErrorStruct)pr.getResultList().elementAt(j++);
                        nextLine = err.getLineNumber();
                    } else {
                        nextLine = -1;
                    }
                }
                while (reader.ready()) {
                    lineno++;
                    line = reader.readLine();
                    if (lineno == nextLine) {
                        marker = new StringBuffer();
                        for (int i=0;i<err.getColumnNumber();i++) {
                            marker.append(' ');
                        }
                        marker.append("<font color=red>^</font>\n");
                        if (err != null) {
                            res.append(htmlEntities(line)+"\n"+marker+"<font color=red>line: "+nextLine+"  column: "+err.getColumnNumber()+"\n"+err.getMessage()+"</font>\n");
                            if (j < pr.getResultList().size()-1) {
                                err = (ErrorStruct)pr.getResultList().elementAt(j++);
                                nextLine = err.getLineNumber();
                            } else {
                                nextLine = -1;
                            }
                        }

                    } else {
                        res.append(htmlEntities(line));
                    }
                    res.append("\n");
                }
                res.append("</PRE>");
            } catch (IOException e) {
                res.append("IOException while annotating file: "+e.getMessage());
            }
            return res.toString();
        }
    }


    protected String checkXML(String path) {
        Document document;
        DOMParser parser;

        ParseResult pr = new ParseResult(path);
        if (pr.getResultList().size() == 0) {
            return "Checked ok";
        } else {
            int warnings, errors, fatals;
            warnings = pr.getWarningList().size();
            errors = pr.getErrorList().size();
            fatals = pr.getFatalList().size();

            StringBuffer s = new StringBuffer();
            s.append("warnings = "+warnings+"  errors = "+errors+"   fatal errors = "+fatals+"<br>\n");
            if (warnings > 0) {
                for (int i=0;i<warnings;i++) {
                    ErrorStruct es = (ErrorStruct)(pr.getWarningList().elementAt(i));
                    s.append("warning at line "+es.getLineNumber()+" column "+es.getColumnNumber()+": "+es.getMessage()+"<br>");
                }
            }

            if (errors > 0) {
                for (int i=0;i<errors;i++) {
                    ErrorStruct es = (ErrorStruct)(pr.getFatalList().elementAt(i));
                    s.append("error at line "+es.getLineNumber()+" column "+es.getColumnNumber()+": "+es.getMessage()+"<br>");
                }
            }

            if (fatals > 0) {
                for (int i=0;i<fatals;i++) {
                    ErrorStruct es = (ErrorStruct)(pr.getFatalList().elementAt(i));
                    s.append("fatal error at line "+es.getLineNumber()+" column "+es.getColumnNumber()+": "+es.getMessage()+"<br>");
                }
            }

            return s.toString();
        }
    }
}













