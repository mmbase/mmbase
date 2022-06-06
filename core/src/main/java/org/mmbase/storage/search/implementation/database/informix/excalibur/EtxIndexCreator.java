/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.storage.search.implementation.database.informix.excalibur;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.mmbase.util.xml.ModuleReader;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * The Etx index creator creates Excalibur Text Search indices,
 * when used with an Informix database and a Excalibur Text Search datablade.
 * This class is provided as a utility to supplement the
 * {@link EtxSqlHandler EtxSqlHandler}.
 * <p>
 * When run as an application, the index creator reads a list of etx-indices
 * from a configuration file, and creates the indices that are not present
 * already.
 * The configurationfile must be named <em>etxindices.xml</em> and located
 * inside the <em>databases</em> configuration directory.
 * It's DTD is located in the directory
 * <code>org.mmbase.storage.search.implementation.database.informix.excalibur.resources</code>
 * in the MMBase source tree and
 * <a href="https://www.mmbase.org/dtd/etxindices.dtd">here</a> online.
 *
 * @author Rob van Maris
 * @version $Id$
 * @since MMBase-1.7
 */
public class EtxIndexCreator {

    /** Path to the MMBase configuration directory. */
    private String configDir = null;

    /** Database connection. */
    private Connection con = null;

    /**
     * Creates a new instance of EtxIndexCreator, opens database connection.
     *
     * @param configDir Path to MMBase configuration directory.
     */
    public EtxIndexCreator(String configDir) throws Exception{
        this.configDir = configDir;

        // Get database connection:
        // 1 - read database configuration
        ModuleReader moduleReader = new ModuleReader(new InputSource(new FileInputStream(configDir + "/modules/jdbc.xml")));
        Map<String, String> properties = moduleReader.getProperties();
        String url = properties.get("url");
        String host = properties.get("host");
        String port = properties.get("port");
        String database = properties.get("database");
        String user = properties.get("user");
        String password = properties.get("password");
        String driver = properties.get("driver");
        // 2 - construct url, substituting database, host and port when needed
        int pos = url.indexOf("$DBM");
        if (pos != -1) {
            url = url.substring(0, pos) + database + url.substring(pos + 4);
        }
        pos = url.indexOf("$HOST");
        if (pos !=- 1) {
            url = url.substring(0, pos) + host + url.substring(pos + 5);
        }
        pos=url.indexOf("$PORT");
        if (pos != -1) {
            url = url.substring(0, pos) + port + url.substring(pos + 5);
        }
        // 3 - Load driver
        Class.forName(driver);
        // 4 - Create connection
        if (user.equals("url") && password.equals("url")) {
            con = DriverManager.getConnection(url);
        } else {
            con = DriverManager.getConnection(url, user, password);
        }
    }

    /**
     * Application main method.
     * <p>
     * Reads etxindices configuration file, and creates the etx indices
     * that are not already created.
     *
     * @param args The command line arguments, should be path to
     *        MMBase configuration directory.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Command line arguments not as expected,"
                + "should be path to MMBase configuration directory.");
            System.exit(1);
        }
        try {
            // Execute tasks.
            new EtxIndexCreator(args[0]).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes the tasks: reads configuration file and creates indices
     * as needed, and closes database connection.
     */
    public void execute() throws Exception {
        try {
            // Read etxindices config.
            File etxConfigFile = new File(
                configDir + "/databases/etxindices.xml");
            XmlEtxIndicesReader configReader =
                new XmlEtxIndicesReader(
                    new InputSource(
                        new BufferedReader(
                            new FileReader(etxConfigFile))));

            for (Iterator<Element> iSbspaces = configReader.getSbspaceElements(); iSbspaces.hasNext();) {
                Element sbspace = iSbspaces.next();
                String sbspaceName = configReader.getSbspaceName(sbspace);

                for (Iterator<Element> iEtxindices = configReader.getEtxindexElements(sbspace); iEtxindices.hasNext();) {
                    Element etxindex = iEtxindices.next();
                    String name = configReader.getEtxindexValue(etxindex);
                    String table = configReader.getEtxindexTable(etxindex);
                    String field = configReader.getEtxindexField(etxindex);
                    if (!etxIndexExists(name)) {
                        createEtxIndex(sbspaceName,
                            name, table, field);
                    }
              }
            }

        } finally {
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * Tests if a Etx index already exists with a specified name.
     * NOTE: Tests if the index exists, but does not verify that is is
     * indeed an Etx index.
     *
     * @param etxindexName The index name.
     * @return True if a Etx index already exists with this name,
     *         false otherwise.
     */
    private boolean etxIndexExists(String etxindexName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(
                "SELECT * FROM sysindexes WHERE idxname = ?");
            ps.setString(1, etxindexName);
            try {
                rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("Index " + etxindexName + " exists already.");
                    return true;
                } else {
                    System.out.println("Index " + etxindexName + " does not exist already.");
                    return false;
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /**
     * Creates new Etx index.
     *
     * @param sbspace The sbspace to use.
     * @param name The index name.
     * @param table The table.
     * @param field The field.
     */
    private void createEtxIndex(String sbspace,
        String name, String table, String field)
        throws SQLException {
            String operatorclass = getOperatorClass(table, field);
            String sqlCreateIndex =
                "CREATE INDEX " + name
                + " ON " + table + " (" + field + " " + operatorclass
                + ") USING etx (CHAR_SET='OVERLAP_ISO', "
                + "PHRASE_SUPPORT='MAXIMUM', "
                + "WORD_SUPPORT='PATTERN') IN " + sbspace;

            PreparedStatement st = null;
            try {
                st = con.prepareStatement(sqlCreateIndex);
                st.executeUpdate();
                System.out.println("Index " + name + " created.");
            } finally {
                if (st != null) {
                    st.close();
                }
            }
    }

    /**
     * Determines the appropriate operator class for a field to be indexed,
     * based on metadata retrieved from the database.
     *
     * @param table The table.
     * @param field The field.
     * @return The operator class.
     */
    private String getOperatorClass(String table, String field) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        ResultSet columninfo = metadata.getColumns(null, null, table, field);
        try {
            boolean hasRows = columninfo.next();
            if (!hasRows) {
                throw new IllegalArgumentException(
                    "The field " + field + " of table " + table
                    + " does not exist.");
            }
            String typeName = columninfo.getString("TYPE_NAME").toLowerCase();
            if (typeName.equals("blob")) {
                return "etx_blob_ops";
            } else if (typeName.equals("clob")) {
                return "etx_clob_ops";
            } else if (typeName.equals("char")) {
                return "etx_char_ops";
            } else if (typeName.equals("lvarchar")) {
                return "etx_lvarc_ops";
            } else if (typeName.equals("varchar")) {
                return ("etx_varc_ops");
            } else {
                throw new IllegalArgumentException(
                    "The field " + field + " of table " + table
                    + " is not of an appropriate type for an Etx index.");
            }
        } finally {
            columninfo.close();
        }
    }
}
