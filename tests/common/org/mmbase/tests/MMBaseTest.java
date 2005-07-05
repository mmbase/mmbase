/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.tests;
import junit.framework.TestCase;

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

    public MMBaseTest() {
        super();
    }
    public MMBaseTest(String name) {
        super(name);
    }

    /**
     * If your test needs a running MMBase. Call this.
     */
    static public void startMMBase() throws Exception {
        startDatabase();
        MMBaseContext.init();
        MMBase.getMMBase();
        
        MMAdmin mmadmin = (MMAdmin) Module.getModule("mmadmin", true);
        while (! mmadmin.getState()) {
            Thread.sleep(1000);
        }
        System.out.println("Starting test");        
    }

    static public void startDatabase() {
        // first try if it is running already
        try {
            Class.forName("org.hsqldb.jdbcDriver" );
        } catch (Exception e) {
            System.err.println("ERROR: failed to load HSQLDB JDBC driver." + e.getMessage());
            return;
        }
        while(true) {
            try {
                Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/test", "sa", "");
                // ok!, elaredy running one.
                return;
            } catch (SQLException sqe) {
                Server server = new Server();
                server.setSilent(true);
                server.setDatabasePath(0, System.getProperty("user.dir") + File.separator + "database" + File.separator + "test");
                server.setDatabaseName(0, "test");
                server.start();
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    // I hate java
                }
            }
        }
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
            while(true) {
                
            }
        } catch (Exception e) {
        }
    }

}
