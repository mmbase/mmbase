/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.tests;
import junit.framework.*;

import java.io.File;
import java.sql.*;
import org.hsqldb.Server;
import org.mmbase.util.ResourceLoader;
import org.mmbase.util.logging.Logging;
import org.mmbase.module.Module;
import org.mmbase.module.core.*;
import org.mmbase.module.tools.MMAdmin;

/**
 * This class contains static methods for MMBase tests.
 *
 * @author Michiel Meeuwissen
 */
public abstract class MMBaseTest extends TestCase {

    static MMBase mmb;

    public MMBaseTest() {
        super();
    }
    public MMBaseTest(String name) {
        super(name);
    }

    static public void startMMBase() throws Exception {
        if (System.getProperty("nostartmmbase") == null) {
            startMMBase(System.getProperty("nostartdb") == null);
        }
    }
    /**
     * If your test needs a running MMBase. Call this.
     */
    static public void startMMBase(boolean startDatabase) throws Exception {
        if (startDatabase) {
            startDatabase();
        }
        try {
            MMBaseContext.init();
            mmb = MMBase.getMMBase();

            MMAdmin mmadmin = (MMAdmin) Module.getModule("mmadmin", true);
            int i = 0;
            while (! mmadmin.getState()) {
                Thread.sleep(1000);
                i++;
                if (i % 10 == 0) System.out.println("Waiting for the mmadmin module (" + i + ") " + mmb.getMMBaseState());
            }
        } catch (Throwable e) {
            System.out.println("Error during startMMBase" + e.getMessage() + " " + Logging.stackTrace(e));
            throw new Exception(e);
        }
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Starting test");
    }

    static public void shutdownMMBase() {
        if (System.getProperty("nostartmmbase") == null) {
            try {
                System.out.println("Shuttind down " + MMBase.getMMBase());
                MMBase.getMMBase().shutdown();
            } catch (java.lang.NoClassDefFoundError mcdfe) {
            }
        }
    }
    static public Test SHUTDOWN = new Test() {
                public int countTestCases() {
                    return 0;
                }
                public void run(TestResult tr) {
                    System.out.println("Shutting down");
                    MMBaseTest.shutdownMMBase();
                }
            };

    static public void startDatabase() {
        // first try if it is running already
        try {
            Thread.sleep(5000);
            Class.forName("org.hsqldb.jdbcDriver" );
        } catch (Exception e) {
            System.err.println("ERROR: failed to load HSQLDB JDBC driver." + e.getMessage());
            return;
        }
        while(true) {
            String database = System.getProperty("test.database");
            if (database == null) database = "test";
            try {
                DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/" + database, "sa", "");
                // ok!, already running one.
                return;
            } catch (SQLException sqe) {
                Server server = new Server();
                server.setSilent(true);
                String dbDir = System.getProperty("test.database.dir");
                if (dbDir == null) dbDir = System.getProperty("user.dir") + File.separator + "data";
                server.setDatabasePath(0, dbDir + File.separator + database);
                server.setDatabaseName(0, database);
                server.start();
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    // I hate java
                }
            }
        }
    }

    static protected void persist() {
    }

    /**
     * If no running MMBase is needed, then you probably want at least to initialize logging.
     */
    static public void startLogging() throws Exception {
        startLogging("log.xml");
    }
    /**
     * If no running MMBase is needed, then you probably want at least to initialize logging.
     */
    static public void startLogging(String configure) throws Exception {
        Logging.configure(ResourceLoader.getConfigurationRoot().getChildResourceLoader("log"), configure);
    }

    /**
     * Always useful, an mmbase running outside an app-server, you can talk to it with rmmci.
     */
    public static void main(String[] args) {
        try {
            startMMBase();
            while(!mmb.isShutdown()) {
                System.out.print(";");
                Thread.sleep(1000);
            }
            System.out.println("MMBase was shut down");
            System.exit(0);

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

}
