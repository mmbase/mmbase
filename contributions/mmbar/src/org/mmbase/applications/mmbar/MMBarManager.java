/*
 *  This software is OSI Certified Open Source Software.
 *  OSI Certified is a certification mark of the Open Source Initiative.
 *  The license (Mozilla version 1.0) can be read at the MMBase site.
 *  See http://www.MMBase.org/license
 */
package org.mmbase.applications.mmbar;

import org.mmbase.util.logging.*;
import org.mmbase.util.*;
import org.mmbase.util.xml.*;
import org.mmbase.module.core.*;
import org.mmbase.bridge.*;
import org.mmbase.storage.*;
import org.mmbase.storage.search.*;
import org.mmbase.applications.mmbar.writetests.*;
import org.mmbase.applications.mmbar.readtests.*;
import org.mmbase.applications.mmbar.mixedtests.*;
import org.mmbase.applications.mmbar.endurancetests.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * @author     Daniel Ockeloen
 * @created    February 28, 2005
 */
public class MMBarManager {

    // logger
    private static Logger log = Logging.getLoggerInstance(MMBarManager.class);

    /**
     *  DTD for the configuration file
     */
    public final static String DTD_MMBARCONFIG_1_0 = "mmbarconfig_1_0.dtd";

    /**
     *  DTD detection string
     */
    public final static String PUBLIC_ID_MMBARCONFIG_1_0 = "-//MMBase//DTD mmbarconfig 1.0//EN";

    // is this manager running
    private static boolean state = false;

    // cloud object
    private static Cloud cloud;

    // account name used for creating objects
    private static String createaccount = null;

    // password used with account to create objects
    private static String createpassword = null;

    // vital stats of the server we are running on
    private static String os, cpu, server, database, driver, java;

    // write tests we have loaded
    private static ArrayList writetests = new ArrayList();

    // read tests we have loaded
    private static ArrayList readtests = new ArrayList();

    // mixed tests we have loaded
    private static ArrayList mixedtests = new ArrayList();

    // endurance tests we have loaded
    private static ArrayList endurancetests = new ArrayList();

    // load tests (not really tests but load test data)
    private static ArrayList loadtests = new ArrayList();

    // test we are currently running, null is none
    private static BaseTest runningtest = null;

    // base testurl, http base for all the jsp tests
    private static String basetesturl;

    /**
     * Register the Public Ids for DTDs used by XMLBasicReader
     * This method is called by XMLEntityResolver.
     */
    public static void registerPublicIDs() {
        XMLEntityResolver.registerPublicID(PUBLIC_ID_MMBARCONFIG_1_0, DTD_MMBARCONFIG_1_0, MMBarManager.class);
    }


    /**
     * init, starts the manager. Its main task is to load the config.xml file
     * and load all the tests.
     */
    public static void init() {
        state = true;
	// read the config that starts the tests
        boolean result = readConfig();
        if (!result) {
            log.error("MMBarManager initialized failed");
	    state = false;
        } else {
            log.info("MMBarManager initialized");
        }
    }


    /**
     * is the MMBarManager running
     *
     * @return    true if manager is running
     */
    public static boolean isRunning() {
        return state;
    }


    /**
     *  get the loaded writetests
     *
     * @return    the loaded write tests
     */
    public static Iterator getWriteTests() {
        return writetests.iterator();
    }


    /**
     *  get the loaded read tests
     *
     * @return    the loaded read tests
     */
    public static Iterator getReadTests() {
        return readtests.iterator();
    }


    /**
     *  get the loaded mixed tests
     *
     * @return    the loaded mixed tests
     */
    public static Iterator getMixedTests() {
        return mixedtests.iterator();
    }


    /**
     *  get the endurance tests
     *
     * @return    the endurance mixed tests
     */
    public static Iterator getEnduranceTests() {
        return endurancetests.iterator();
    }


    /**
     * Get write test defined by name
     *
     * @param  name of write test we want
     * @return  write test or null if not found
     */
    public static WriteTest getWriteTest(String name) {
        for (Iterator i = getWriteTests(); i.hasNext(); ) {
            WriteTest wt = (WriteTest) i.next();
            if (wt != null && wt.getName().equals(name)) {
                return wt;
            }
        }
        return null;
    }


    /**
     * Get mixed test defined by name
     *
     * @param  	name of the mixed test we want
     * @return      mixed test or null if not found
     */
    public static MixedTest getMixedTest(String name) {
        for (Iterator i = getMixedTests(); i.hasNext(); ) {
            MixedTest mt = (MixedTest) i.next();
            if (mt != null && mt.getName().equals(name)) {
                return mt;
            }
        }
        return null;
    }


    /**
     * Get endurance test defined by name
     *
     * @param  	name of the endurance test we want
     * @return      endurance test or null if not found
     */
    public static EnduranceTest getEnduranceTest(String name) {
        for (Iterator i = getEnduranceTests(); i.hasNext(); ) {
            EnduranceTest et = (EnduranceTest) i.next();
            if (et != null && et.getName().equals(name)) {
                return et;
            }
        }
        return null;
    }


    /**
     * Get read test defined by name
     *
     * @param  	name of the read test we want
     * @return      read test or null if not found
     */
    public static ReadTest getReadTest(String name) {
        for (Iterator i = MMBarManager.getReadTests(); i.hasNext(); ) {
            ReadTest rt = (ReadTest) i.next();
            if (rt != null && rt.getName().equals(name)) {
                return rt;
            }
        }
        return null;
    }


    /**
     *  perform/start the test defined by the given name
     *
     * @param  name of the write test we want to execute
     * @return      true is test was started, false if it failed to start
     */
    public static boolean performWriteTest(String name) {
        WriteTest wt = getWriteTest(name);
        if (wt != null) {
            wt.perform();
            return true;
        }
        return false;
    }


    /**
     *  perform/start the test defined by the given name
     *
     * @param  name of the mixed test we want to execute
     * @return      true is test was started, false if it failed to start
     */
    public static boolean performMixedTest(String name) {
        MixedTest mt = getMixedTest(name);
        if (mt != null) {
            mt.perform();
            return true;
        }
        return false;
    }


    /**
     *  perform/start the test defined by the given name
     *
     * @param  name of the endurance test we want to execute
     * @return      true is test was started, false if it failed to start
     */
    public static boolean performEnduranceTest(String name) {
        EnduranceTest et = getEnduranceTest(name);
        if (et != null) {
            et.perform();
            return true;
        }
        return false;
    }


    /**
     *  perform/start the test defined by the given name
     *
     * @param  name of the endurance test we want to execute
     * @return      true is test was started, false if it failed to start
     */
    public static boolean performReadTest(String name) {
        ReadTest rt = getReadTest(name);
        if (rt != null) {
            rt.perform();
            return true;
        }
        return false;
    }


    public static boolean saveReadTestBenchmark(String name) {
        ReadTest rt = getReadTest(name);
        if (rt != null) {
            rt.savebenchmark();
	    saveSettings();
            return true;
        }
        return false;
    }


    public static boolean saveWriteTestBenchmark(String name) {
        WriteTest wt = getWriteTest(name);
        if (wt != null) {
            wt.savebenchmark();
	    saveSettings();
            return true;
        }
        return false;
    }


    public static boolean saveMixedTestBenchmark(String name) {
        MixedTest mt = getMixedTest(name);
        if (mt != null) {
            mt.savebenchmark();
	    saveSettings();
            return true;
        }
        return false;
    }


    public static boolean saveEnduranceTestBenchmark(String name) {
        EnduranceTest et = getEnduranceTest(name);
        if (et != null) {
            et.savebenchmark();
	    saveSettings();
            return true;
        }
        return false;
    }

    public static boolean deleteReadTestBenchmark(String name,int pos) {
        ReadTest rt = getReadTest(name);
        if (rt != null) {
            rt.deletebenchmark(pos);
	    saveSettings();
            return true;
        }
        return false;
    }


    public static boolean deleteWriteTestBenchmark(String name,int pos) {
        WriteTest wt = getWriteTest(name);
        if (wt != null) {
            wt.deletebenchmark(pos);
	    saveSettings();
            return true;
        }
        return false;
    }

    public static boolean deleteMixedTestBenchmark(String name,int pos) {
        MixedTest mt = getMixedTest(name);
        if (mt != null) {
            mt.deletebenchmark(pos);
	    saveSettings();
            return true;
        }
        return false;
    }


    public static boolean deleteEnduranceTestBenchmark(String name,int pos) {
        EnduranceTest et = getEnduranceTest(name);
        if (et != null) {
            et.deletebenchmark(pos);
	    saveSettings();
            return true;
        }
        return false;
    }

    /**
     * read the config files, at the moment overdone in that it uses almost
     * the same code for things that now are almost the same this is done by
     * design since i expect all to become different over time.
     *
     * @return      true if config was loaded, false if it failed to load
     */
    public static boolean readConfig() {
        try {
            InputSource is = ResourceLoader.getConfigurationRoot().getInputSource("mmbar/config.xml");

            DocumentReader reader = new DocumentReader(is,MMBarManager.class);
            if (reader != null) {
                org.w3c.dom.Node n = reader.getElementByPath("mmbarconfig.machinespecs");
                if (n != null) {
                    NamedNodeMap nm = n.getAttributes();
                    if (nm != null) {
                        org.w3c.dom.Node n2 = nm.getNamedItem("os");
                        if (n2 != null) {
                            os = n2.getNodeValue();
                        }
                        n2 = nm.getNamedItem("cpu");
                        if (n2 != null) {
                            cpu = n2.getNodeValue();
                        }
                        n2 = nm.getNamedItem("server");
                        if (n2 != null) {
                            server = n2.getNodeValue();
                        }
                        n2 = nm.getNamedItem("database");
                        if (n2 != null) {
                            database = n2.getNodeValue();
                        }
                        n2 = nm.getNamedItem("driver");
                        if (n2 != null) {
                            driver = n2.getNodeValue();
                        }
                        n2 = nm.getNamedItem("java");
                        if (n2 != null) {
                            java = n2.getNodeValue();
                        }
                    }
                }

                n = reader.getElementByPath("mmbarconfig.basetesturl");
                if (n != null) {
		    basetesturl = n.getFirstChild().getNodeValue();
		}


                Iterator e = reader.getChildElements("mmbarconfig", "writetests").iterator();
                while (e.hasNext()) {
                    WriteTest wt = null;
                    org.w3c.dom.Element n2 = (org.w3c.dom.Element) e.next();
                    org.w3c.dom.Node n3 = n2.getFirstChild();
                    while (n3 != null) {
                        NamedNodeMap nm = n3.getAttributes();
                        if (nm != null) {
                            String name = null;
                            String action = null;
                            String classname = null;
                            int count = 1;
                            int threads = 1;
                            org.w3c.dom.Node n4 = nm.getNamedItem("name");
                            if (n4 != null) {
                                name = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("action");
                            if (n4 != null) {
                                action = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("class");
                            if (n4 != null) {
                                classname = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("count");
                            if (n4 != null) {
                                count = Integer.parseInt(n4.getNodeValue());
                            }
                            n4 = nm.getNamedItem("threads");
                            if (n4 != null) {
                                threads = Integer.parseInt(n4.getNodeValue());
                            }
                            if (classname != null) {
                                try {
                                    Class newclass = Class.forName(classname);
                                    wt = (WriteTest) newclass.newInstance();
                                    wt.setName(name);
                                    wt.setAction(action);
                                    wt.setCount(count);
                                    wt.setThreads(threads);
                                    writetests.add(wt);
                                } catch (Exception f) {
                                    log.error("error can't create writetest : " + name);
                                }
                            }
                        }
                        // decode the benchtests
                        org.w3c.dom.Node n4 = n3.getFirstChild();
                        while (n4 != null) {
                            String name = n4.getNodeName();
                            if (name.equals("description")) {
                                String des = n4.getFirstChild().getNodeValue();
				if (des.equals("null")) {
                                	wt.setDescription(des);
				}
                            } else if (name.equals("benchmark")) {
                                nm = n4.getAttributes();
                                if (nm != null) {
                                    Benchmark bm = wt.getNewBenchmark();
                                    org.w3c.dom.Node n5 = nm.getNamedItem("result");
                                    if (n5 != null) {
                                        String tmp = n5.getNodeValue();
                                        try {
                                            bm.setResult(Float.parseFloat(tmp));
                                        } catch (Exception f) {}
                                    }
                                    n5 = nm.getNamedItem("os");
                                    if (n5 != null) {
                                        bm.setOS(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("cpu");
                                    if (n5 != null) {
                                        bm.setCPU(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("server");
                                    if (n5 != null) {
                                        bm.setServer(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("database");
                                    if (n5 != null) {
                                        bm.setDatabase(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("driver");
                                    if (n5 != null) {
                                        bm.setDriver(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("java");
                                    if (n5 != null) {
                                        bm.setJava(n5.getNodeValue());
                                    }
                                }
                            }
                            n4 = n4.getNextSibling();
                        }

                        n3 = n3.getNextSibling();
                    }
                }

                e = reader.getChildElements("mmbarconfig", "readtests").iterator();
                while (e.hasNext()) {
                    ReadTest rt = null;
                    org.w3c.dom.Element n2 = (org.w3c.dom.Element) e.next();
                    org.w3c.dom.Node n3 = n2.getFirstChild();
                    while (n3 != null) {
                        NamedNodeMap nm = n3.getAttributes();
                        if (nm != null) {
                            String name = null;
                            String action = null;
                            String classname = null;
                            int count = 1;
                            int threads = 1;
                            org.w3c.dom.Node n4 = nm.getNamedItem("name");
                            if (n4 != null) {
                                name = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("action");
                            if (n4 != null) {
                                action = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("class");
                            if (n4 != null) {
                                classname = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("count");
                            if (n4 != null) {
                                count = Integer.parseInt(n4.getNodeValue());
                            }
                            n4 = nm.getNamedItem("threads");
                            if (n4 != null) {
                                threads = Integer.parseInt(n4.getNodeValue());
                            }
                            if (classname != null) {
                                try {
                                    Class newclass = Class.forName(classname);
                                    rt = (ReadTest) newclass.newInstance();
                                    rt.setName(name);
                                    rt.setAction(action);
                                    rt.setCount(count);
                                    rt.setThreads(threads);
                                    readtests.add(rt);
                                } catch (Exception f) {
                                    log.error("error can't create writetest : " + name);
                                }
                            }
                        }
                        // decode the benchtests
                        org.w3c.dom.Node n4 = n3.getFirstChild();
                        while (n4 != null) {
                            String name = n4.getNodeName();
                            if (name.equals("description")) {
                                rt.setDescription(n4.getFirstChild().getNodeValue());
                            } else if (name.equals("benchmark")) {
                                nm = n4.getAttributes();
                                if (nm != null) {
                                    Benchmark bm = rt.getNewBenchmark();
                                    org.w3c.dom.Node n5 = nm.getNamedItem("result");
                                    if (n5 != null) {
                                        String tmp = n5.getNodeValue();
                                        try {
                                            bm.setResult(Float.parseFloat(tmp));
                                        } catch (Exception f) {}
                                    }
                                    n5 = nm.getNamedItem("os");
                                    if (n5 != null) {
                                        bm.setOS(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("cpu");
                                    if (n5 != null) {
                                        bm.setCPU(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("server");
                                    if (n5 != null) {
                                        bm.setServer(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("database");
                                    if (n5 != null) {
                                        bm.setDatabase(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("driver");
                                    if (n5 != null) {
                                        bm.setDriver(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("java");
                                    if (n5 != null) {
                                        bm.setJava(n5.getNodeValue());
                                    }
                                }
                            } else if (name.equals("property")) {
                                nm = n4.getAttributes();
                                if (nm != null) {
                                    Benchmark bm = rt.getNewBenchmark();
                                    org.w3c.dom.Node n5 = nm.getNamedItem("name");
                                    if (n5 != null) {
					name = n5.getNodeValue();
                                    	n5 = nm.getNamedItem("value");
					String value = n5.getNodeValue();
					rt.setProperty(name,value);
				    }
				}
                            }
                            n4 = n4.getNextSibling();
                        }

                        n3 = n3.getNextSibling();
                    }
                }

                e = reader.getChildElements("mmbarconfig", "mixedtests").iterator();
                while (e.hasNext()) {
                    MixedTest mt = null;
                    org.w3c.dom.Element n2 = (org.w3c.dom.Element) e.next();
                    org.w3c.dom.Node n3 = n2.getFirstChild();
                    while (n3 != null) {
                        NamedNodeMap nm = n3.getAttributes();
                        if (nm != null) {
                            String name = null;
                            String action = null;
                            String classname = null;
                            int count = 1;
                            int threads = 1;
                            org.w3c.dom.Node n4 = nm.getNamedItem("name");
                            if (n4 != null) {
                                name = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("action");
                            if (n4 != null) {
                                action = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("class");
                            if (n4 != null) {
                                classname = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("count");
                            if (n4 != null) {
                                count = Integer.parseInt(n4.getNodeValue());
                            }
                            n4 = nm.getNamedItem("threads");
                            if (n4 != null) {
                                threads = Integer.parseInt(n4.getNodeValue());
                            }
                            if (classname != null) {
                                try {
                                    Class newclass = Class.forName(classname);
                                    mt = (MixedTest) newclass.newInstance();
                                    mt.setName(name);
                                    mt.setAction(action);
                                    mt.setCount(count);
                                    mt.setThreads(threads);
                                    mixedtests.add(mt);
                                } catch (Exception f) {
                                    log.error("error can't create mixedtest : " + name);
                                }
                            }
                        }
                        // decode the benchtests
                        org.w3c.dom.Node n4 = n3.getFirstChild();
                        while (n4 != null) {
                            String name = n4.getNodeName();
                            if (name.equals("description")) {
                                mt.setDescription(n4.getFirstChild().getNodeValue());
                            } else if (name.equals("benchmark")) {
                                nm = n4.getAttributes();
                                if (nm != null) {
                                    Benchmark bm = mt.getNewBenchmark();
                                    org.w3c.dom.Node n5 = nm.getNamedItem("result");
                                    if (n5 != null) {
                                        String tmp = n5.getNodeValue();
                                        try {
                                            bm.setResult(Float.parseFloat(tmp));
                                        } catch (Exception f) {}
                                    }
                                    n5 = nm.getNamedItem("os");
                                    if (n5 != null) {
                                        bm.setOS(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("cpu");
                                    if (n5 != null) {
                                        bm.setCPU(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("server");
                                    if (n5 != null) {
                                        bm.setServer(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("database");
                                    if (n5 != null) {
                                        bm.setDatabase(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("driver");
                                    if (n5 != null) {
                                        bm.setDriver(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("java");
                                    if (n5 != null) {
                                        bm.setJava(n5.getNodeValue());
                                    }
                                }
                            }
                            n4 = n4.getNextSibling();
                        }

                        n3 = n3.getNextSibling();
                    }
                }

                e = reader.getChildElements("mmbarconfig", "endurancetests").iterator();
                while (e.hasNext()) {
                    EnduranceTest et = null;
                    org.w3c.dom.Element n2 = (org.w3c.dom.Element) e.next();
                    org.w3c.dom.Node n3 = n2.getFirstChild();
                    while (n3 != null) {
                        NamedNodeMap nm = n3.getAttributes();
                        if (nm != null) {
                            String name = null;
                            String action = null;
                            String classname = null;
                            int count = 1;
                            int threads = 1;
                            org.w3c.dom.Node n4 = nm.getNamedItem("name");
                            if (n4 != null) {
                                name = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("action");
                            if (n4 != null) {
                                action = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("class");
                            if (n4 != null) {
                                classname = n4.getNodeValue();
                            }
                            n4 = nm.getNamedItem("count");
                            if (n4 != null) {
                                count = Integer.parseInt(n4.getNodeValue());
                            }
                            n4 = nm.getNamedItem("threads");
                            if (n4 != null) {
                                threads = Integer.parseInt(n4.getNodeValue());
                            }
                            if (classname != null) {
                                try {
                                    Class newclass = Class.forName(classname);
                                    et = (EnduranceTest) newclass.newInstance();
                                    et.setName(name);
                                    et.setAction(action);
                                    et.setCount(count);
                                    et.setThreads(threads);
                                    endurancetests.add(et);
                                } catch (Exception f) {
                                    log.error("error can't create endurancetest : " + name);
                                }
                            }
                        }
                        // decode the benchtests
                        org.w3c.dom.Node n4 = n3.getFirstChild();
                        while (n4 != null) {
                            String name = n4.getNodeName();
                            if (name.equals("description")) {
                                et.setDescription(n4.getFirstChild().getNodeValue());
                            } else if (name.equals("benchmark")) {
                                nm = n4.getAttributes();
                                if (nm != null) {
                                    Benchmark bm = et.getNewBenchmark();
                                    org.w3c.dom.Node n5 = nm.getNamedItem("result");
                                    if (n5 != null) {
                                        String tmp = n5.getNodeValue();
                                        try {
                                            bm.setResult(Float.parseFloat(tmp));
                                        } catch (Exception f) {}
                                    }
                                    n5 = nm.getNamedItem("os");
                                    if (n5 != null) {
                                        bm.setOS(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("cpu");
                                    if (n5 != null) {
                                        bm.setCPU(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("server");
                                    if (n5 != null) {
                                        bm.setServer(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("database");
                                    if (n5 != null) {
                                        bm.setDatabase(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("driver");
                                    if (n5 != null) {
                                        bm.setDriver(n5.getNodeValue());
                                    }
                                    n5 = nm.getNamedItem("java");
                                    if (n5 != null) {
                                        bm.setJava(n5.getNodeValue());
                                    }
                                }
                            }
                            n4 = n4.getNextSibling();
                        }

                        n3 = n3.getNextSibling();
                    }
                }
            }
        } catch(Exception e) {
            log.error("missing config resource : mmbar/config.xml");
            return false;
        }
        return true;
    }


    /**
     * get the state of the mananger, for example if a test is running
     *
     * @return   state of the manager as a string (gui feedback)
     */
    public static String getState() {
        if (runningtest == null) {
            return "waiting";
        }
        return "running";
    }


    /**
     * get the name of the test that is running
     *
     * @return   name of test, null if none is running
     */
    public static String getRunningName() {
        if (runningtest != null) {
            return runningtest.getName();
        }
        return null;
    }


    /**
     * get the current progress (within the test) of the test that is running
     *
     * @return   current progress counter, -1 if none is running
     */
    public static int getRunningPos() {
        if (runningtest != null) {
            return runningtest.getCurrentPos();
        }
        return -1;
    }


    /**
     * get the current progress (within the test) of the test that is running
     *
     * @return   current progress counter, -1 if none is running
     */
    public static int getRunningCount() {
        if (runningtest != null) {
            return runningtest.getCount();
        }
        return -1;
    }


    /**
     * clear the running test value, called by a test once its finished
     */
    public static void cleanRunningTest() {
        runningtest = null;
    }


    /**
     *  get the create account that is used for the creation of all the test data
     *
     * @return  The in config defined name or 'admin' if none was defined
     */
    public static String getCreateAccount() {
        if (createaccount == null) {
            return "admin";
        }
        return createaccount;
    }


    /**
     *  get the create password that is used for the creation of all the test data
     *
     * @return  The in config defined password or 'admin2k' if none was defined
     */
    public static String getCreatePassword() {
        if (createpassword == null) {
            return "admin2k";
        }
        return createpassword;
    }


    /**
     * get the cloud object needed for the testing
     *
     * @return   cloud object or null if it failed to create one
     */
    public static Cloud getCloud() {
        if (cloud == null) {
            cloud = ContextProvider.getDefaultCloudContext().getCloud("mmbase", "name/password", getNamePassword("default"));
        }
        return cloud;
    }


    /**
     * get the name/password map for logging into a cloud
     *
     * @param  id of the cloud
     * @return  user map with username and password
     */
    protected static Map getNamePassword(String id) {
        Map user = new HashMap();
        if (id.equals("default")) {
            user.put("username", getCreateAccount());
            user.put("password", getCreatePassword());
        }
        return user;
    }


    /**
     * set the running test reference, used by the manager to provide gui feedback
     * while a test is running.
     *
     * @param  test we just started
     */
    public static void setRunningTest(BaseTest test) {
        runningtest = test;
    }


    /**
     *  Gets the runningTest attribute of the MMBarManager class
     *
     * @return    The runningTest value
     */
    public static BaseTest getRunningTest() {
        return runningtest;
    }


    /**
     * get the os of the machine we are on (or what is defined)
     *
     * @return    os ident
     */
    public static String getOS() {
	if (os!=null) {
            return os;
	} else {
	    return System.getProperty("os.name") + "/" + System.getProperty("os.version");
	}
    }

    public static void setOS(String newos) {
	os = newos;
    }

    /**
     * get the cpu of the machine we are on (or what is defined)
     *
     * @return    cpu ident
     */
    public static String getCPU() {
        return cpu;
    }

    public static void setCPU(String newcpu) {
	cpu = newcpu;
    }

    /**
     * get the app server of the machine we are on (or what is defined)
     *
     * @return    app server ident
     */
    public static String getServer() {
        return server;
    }

    public static void setServer(String newserver) {
	server = newserver;
    }

    /**
     * get the database server of the machine we are on (or what is defined)
     *
     * @return    database server ident
     */
    public static String getDatabase() {
        return database;
    }

    public static void setDatabase(String newdatabase) {
	database = newdatabase;
    }



    /**
     * get the database driver of the machine we are on (or what is defined)
     *
     * @return    database driver ident
     */
    public static String getDriver() {
        return driver;
    }

    public static void setDriver(String newdriver) {
	driver = newdriver;
    }

    /**
     * get the java version of the machine we are on (or what is defined)
     *
     * @return    java ident
     */
    public static String getJava() {
	if (java!=null) {
        	return java;
	} else {
		return System.getProperty("java.version");
	}
    }

    public static void setJava(String newjava) {
	java = newjava;
    }

    public static String getBaseTestUrl() {
	return basetesturl;
    }

    public static void setBaseTestUrl(String newbasetesturl) {
	basetesturl = newbasetesturl;
    }

    public static Iterator getOSList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("osx");
	result.add("xp");
	result.add("linux");
	result.add("unknown");
        return result.iterator();
    }


    public static Iterator getCPUList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("G4/768Mhz");
	result.add("P4/3.2Ghz");
	result.add("P3/1.26Ghz");
	result.add("unknown");
        return result.iterator();
    }


    public static Iterator getServerList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("Tomcat 5.0.x");
	result.add("Tomcat 5.5.x");
	result.add("Orion");
	result.add("Webphere");
	result.add("Resin");
	result.add("unknown");
        return result.iterator();
    }


    public static Iterator getDatabaseList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("MySQL");
	result.add("Hypersonic");
	result.add("Postgress");
	result.add("Oracle");
	result.add("unknown");
        return result.iterator();
    }


    public static Iterator getDriverList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("mysqlconnector 3.x");
	result.add("Hypersonic jdbc");
	result.add("pg7xjdbc3");
	result.add("pg8xjdbc3");
	result.add("unknown");
        return result.iterator();
    }


    public static Iterator getJavaList() {
	// temp until from config
	ArrayList result = new ArrayList();
	result.add("1.3");
	result.add("1.4");
	result.add("1.5");
	result.add("unknown");
        return result.iterator();
    }

    public static void saveSettings() {
	String body="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	body+="<!DOCTYPE mmbarconfig PUBLIC \"-//MMBase/DTD mmbar 1.0//EN\" \"https://www.mmbase.org/dtd/mmbarconfig_1_0.dtd\">\n";
	body+="<mmbarconfig>\n";
	body+="\t<basetesturl>"+getBaseTestUrl()+"</basetesturl>\n";
	body+="\t<machinespecs os=\""+getOS()+"\" cpu=\""+getCPU()+"\" server=\""+getServer()+"\" database=\""+getDatabase()+"\" driver=\""+getDriver()+"\" java=\""+getJava()+"\" />\n";
	body+="\t<readtests>\n";
        for (Iterator i = getReadTests(); i.hasNext(); ) {
            ReadTest rt = (ReadTest) i.next();
	    body+="\t\t<readtest name=\""+rt.getName()+"\" action=\""+rt.getAction()+"\" class=\""+rt.getClass().getName()+"\" count=\""+rt.getCount()+"\" threads=\""+rt.getThreads()+"\">\n";
	   if (rt.getDescription()!=null && !rt.getDescription().equals("null")) {
	   	body+="\t\t\t<description>"+rt.getDescription()+"</description>\n";
	   }
           for (Iterator i2 = rt.getBenchmarks(); i2.hasNext(); ) {
            	Benchmark bm = (Benchmark) i2.next();
		body+="\t\t\t<benchmark result=\""+bm.getResult()+"\" os=\""+bm.getOS()+"\" cpu=\""+bm.getCPU()+"\" server=\""+bm.getServer()+"\" database=\""+bm.getDatabase()+"\" driver=\""+bm.getDriver()+"\" java=\""+bm.getJava()+"\" />\n";
	   }
	   body+="\t\t</readtest>\n";
	}
	body+="\t</readtests>\n";
	body+="\t<writetests>\n";
        for (Iterator i = getWriteTests(); i.hasNext(); ) {
            WriteTest wt = (WriteTest) i.next();
	    body+="\t\t<writetest name=\""+wt.getName()+"\" action=\""+wt.getAction()+"\" class=\""+wt.getClass().getName()+"\" count=\""+wt.getCount()+"\" threads=\""+wt.getThreads()+"\">\n";
	   if (wt.getDescription()!=null && !wt.getDescription().equals("null")) {
	   	body+="\t\t\t<description>"+wt.getDescription()+"</description>\n";
	   }
           for (Iterator i2 = wt.getBenchmarks(); i2.hasNext(); ) {
            	Benchmark bm = (Benchmark) i2.next();
		body+="\t\t\t<benchmark result=\""+bm.getResult()+"\" os=\""+bm.getOS()+"\" cpu=\""+bm.getCPU()+"\" server=\""+bm.getServer()+"\" database=\""+bm.getDatabase()+"\" driver=\""+bm.getDriver()+"\" java=\""+bm.getJava()+"\" />\n";
	   }
	   body+="\t\t</writetest>\n";
	}
	body+="\t</writetests>\n";
	body+="\t<mixedtests>\n";
        for (Iterator i = getMixedTests(); i.hasNext(); ) {
            MixedTest mt = (MixedTest) i.next();
	    body+="\t\t<mixedtest name=\""+mt.getName()+"\" action=\""+mt.getAction()+"\" class=\""+mt.getClass().getName()+"\" count=\""+mt.getCount()+"\" threads=\""+mt.getThreads()+"\">\n";
	   if (mt.getDescription()!=null && !mt.getDescription().equals("null")) {
	   	body+="\t\t\t<description>"+mt.getDescription()+"</description>\n";
	   }
           for (Iterator i2 = mt.getBenchmarks(); i2.hasNext(); ) {
            	Benchmark bm = (Benchmark) i2.next();
		body+="\t\t\t<benchmark result=\""+bm.getResult()+"\" os=\""+bm.getOS()+"\" cpu=\""+bm.getCPU()+"\" server=\""+bm.getServer()+"\" database=\""+bm.getDatabase()+"\" driver=\""+bm.getDriver()+"\" java=\""+bm.getJava()+"\" />\n";
	   }
	   body+="\t\t</mixedtest>\n";
	}
	body+="\t</mixedtests>\n";

	body+="\t<endurancetests>\n";
        for (Iterator i = getEnduranceTests(); i.hasNext(); ) {
            EnduranceTest et = (EnduranceTest) i.next();
	    body+="\t\t<endurancetest name=\""+et.getName()+"\" action=\""+et.getAction()+"\" class=\""+et.getClass().getName()+"\" count=\""+et.getCount()+"\" threads=\""+et.getThreads()+"\">\n";
	   if (et.getDescription()!=null && !et.getDescription().equals("null")) {
	   	body+="\t\t\t<description>"+et.getDescription()+"</description>\n";
	   }
           for (Iterator i2 = et.getBenchmarks(); i2.hasNext(); ) {
            	Benchmark bm = (Benchmark) i2.next();
		body+="\t\t\t<benchmark result=\""+bm.getResult()+"\" os=\""+bm.getOS()+"\" cpu=\""+bm.getCPU()+"\" server=\""+bm.getServer()+"\" database=\""+bm.getDatabase()+"\" driver=\""+bm.getDriver()+"\" java=\""+bm.getJava()+"\" />\n";
	   }
	   body+="\t\t</endurancetest>\n";
	}
	body+="\t</endurancetests>\n";
	body+="</mmbarconfig>\n";
	/* need fix daniel
        String filename = MMBaseContext.getConfigPath() + File.separator + "mmbar" + File.separator + "config.xml";
	saveFile(filename,body);
	*/
    }

    static boolean saveFile(String filename,String value) {
        File sfile = new File(filename);
        try {
            DataOutputStream scan = new DataOutputStream(new FileOutputStream(sfile));
            scan.writeBytes(value);
            scan.flush();
            scan.close();
        } catch(Exception e) {
            log.error(Logging.stackTrace(e));
        }
        return true;
    }

}

