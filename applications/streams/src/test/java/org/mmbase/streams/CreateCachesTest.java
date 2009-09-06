package org.mmbase.streams;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import java.util.*;
import java.io.*;
import org.mmbase.bridge.*;
import org.mmbase.datatypes.DataType;
import static org.mmbase.datatypes.Constants.*;
import org.mmbase.datatypes.processors.*;
import org.mmbase.bridge.mock.*;
import org.mmbase.streams.transcoders.*;
import static org.mmbase.streams.transcoders.AnalyzerUtils.*;
import org.mmbase.util.logging.*;

import org.mmbase.servlet.FileServlet;


/**
 * @author Michiel Meeuwissen
 */

public class CreateCachesTest {

    private static final String REMOTE_URI = "rmi://127.0.0.1:1111/remotecontext";
    private static Cloud remoteCloud;


    private final static MockCloudContext cloudContext = new MockCloudContext();
    private final static String FILE = "basic.mp4";
    private static File  testFile;

    public CreateCachesTest() {
    }


    @AfterClass
    public static void shutdown() {
        System.out.println("Ready testing ");
        org.mmbase.util.ThreadPools.shutdown();
    }
    @BeforeClass
    public static void setUp() throws Exception {
        try {
            CloudContext c =  ContextProvider.getCloudContext(REMOTE_URI);
            remoteCloud = c.getCloud("mmbase", "class", null);
            System.out.println("Found remote cloud " + remoteCloud);
        } catch (Exception e) {
            System.err.println("Cannot get RemoteCloud. (" + e.getMessage() + "). Some tests will be skipped. (but reported as succes: see http://jira.codehaus.org/browse/SUREFIRE-542)");
            System.err.println("You can start up a test-environment for remote tests: trunk/example-webapp$ mvn jetty:run");
            remoteCloud = null;
        }

        //cloudContext.clear();
        /*
        cloudContext.addCore();
        cloudContext.addNodeManagers(DummyBuilderReader.getBuilderLoader().getChildResourceLoader("resources"));
        cloudContext.addNodeManagers(DummyBuilderReader.getBuilderLoader().getChildResourceLoader("media"));
        cloudContext.addNodeManagers(DummyBuilderReader.getBuilderLoader().getChildResourceLoader("streams"));
        */

        /* Mock stuff not yet sufficiently useable
        {
            Map<String, DataType> map = new HashMap<String, DataType>();
            map.put("number",    DATATYPE_INTEGER);
            map.put("url",       DATATYPE_STRING);
            map.put("title",     DATATYPE_STRING);
            map.put("mimetype",  DATATYPE_STRING);
            map.put("id",        DATATYPE_INTEGER);
            map.put("key",       DATATYPE_STRING);
            map.put("format",    DATATYPE_INTEGER);
            map.put("codec",     DATATYPE_INTEGER);
            map.put("state",     DATATYPE_INTEGER);
            map.put("mediafragment",     DATATYPE_NODE);
            map.put("mediaprovider",     DATATYPE_NODE);
            cloudContext.addNodeManager("dummy", map);

            cloudContext.getCloud("mmbase").getNodeManager("dummy").getProperties().put("org.mmbase.streams.cachestype", "dummy");

        }

        {
            Map<String, DataType> map = new HashMap<String, DataType>();
            map.put("number", DATATYPE_INTEGER);
            map.put("title",     DATATYPE_STRING);
            cloudContext.addNodeManager("container", map);
        }

        */
        testFile = new File("samples", FILE);

    }

    protected Cloud getCloud() {
        if (remoteCloud != null) {
            remoteCloud.setProperty(CreateCachesProcessor.NOT, "no implicit processesing please");
            //remoteCloud.setProperty(BinaryCommitProcessor.NOT, "no implicit processesing please");
            //remoteCloud.setProperty(org.mmbase.applications.media.FragmentTypeFixer.NOT, "no implicit processesing please");
        }
        return remoteCloud;
    }




    @Test
    public void node() {
        Cloud cloud = getCloud();
        assumeNotNull(cloud);
        NodeManager nm = cloud.getNodeManager("streamsources");
        assumeNotNull(nm);
        Node newSource = nm.createNode(); //
        newSource.commit();

    }

    Node getNode(File dir) throws Exception {
        Cloud cloud = getCloud();
        assumeNotNull(cloud);


        Node container = cloud.getNodeManager("videofragments").createNode();
        container.commit();

        NodeManager nm = cloud.getNodeManager("streamsources");
        File tempFile = new File(dir, getClass().getName() + "." + FILE);
        if (! tempFile.exists()) {
            org.mmbase.util.IOUtil.copy(new FileInputStream(testFile), new FileOutputStream(tempFile));
        }

        assertEquals(513965, tempFile.length());

        System.out.println("DIR " + dir);

        Node newSource = nm.createNode();
        newSource.setNodeValue("mediafragment", container);
        newSource.setNodeValue("mediaprovider", cloud.getNode("default.provider"));
        newSource.commit();

        assertTrue(testFile.exists());


        newSource.setValueWithoutProcess("url", tempFile.getName());
        newSource.commit();
        return newSource;
    }

    CreateCachesProcessor get(String config) {
        Cloud cloud = getCloud();
        assumeNotNull(cloud);

        NodeManager nm = cloud.getNodeManager("streamsources");
        CreateCachesProcessor proc = new CreateCachesProcessor(config);
        File dir = new File(nm.getFunctionValue("fileServletDirectory", null).toString());
        proc.setDirectory(dir);
        return proc;
    }

    Node refresh(Node source) throws InterruptedException {
        //Thread.sleep(100);
        return source.getCloud().getNode(source.getNumber());

    }
    void checkSource(Node source, int sourceCount) {
        checkSource(source, sourceCount, "");
    }
    void checkSource(Node source, int sourceCount, String message) {
        assertEquals(source.getNumber() + " " + message, "videostreamsources", source.getNodeManager().getName());
        assertTrue(source.getNumber() + " " + message, source.getStringValue("mimetype").startsWith("video/"));
        assertNotNull(source.getNumber() + " " + message, source.getValue("width"));
        assertNotNull(source.getValue("height"));
        assertEquals(352, source.getIntValue("width"));
        assertEquals(288, source.getIntValue("height"));
        assertEquals(1, source.getRelatedNodes("mediaproviders").size());
        assertEquals(1, source.getRelatedNodes("mediafragments").size());

        Node mediafragment = source.getNodeValue("mediafragment");
        assertEquals("videofragments", mediafragment.getNodeManager().getName());
        //assertEquals("" + mediafragment.getNumber() + " is supposed to have " + sourceCount + " source", sourceCount, mediafragment.getRelatedNodes("mediasources").size()); // why does this not work?
        assertEquals("" + mediafragment.getNumber() + " is supposed to have " + sourceCount + " source", sourceCount, mediafragment.getRelatedNodes("object", "related", "destination").size());
    }


    @Test
    public void recognizerOnly() throws Exception  {
        for (int i = 0; i < 1; i++) {
            CreateCachesProcessor proc = get("dummycreatecaches_0.xml");
            Node source = getNode(proc.getDirectory());
            CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());


            job.waitUntilReady();
            source = refresh(source);
            checkSource(source, 1, "" + i);
        }
    }

    @Test
    public  void simple() throws Exception {
        CreateCachesProcessor proc = get("dummycreatecaches_1.xml");
        Node source = getNode(proc.getDirectory());
        CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());
        source.commit();


        job.waitUntilReady();
        source = refresh(source);
        checkSource(source, 2);
    }


    @Test
    public  void twoSteps() throws Exception {
        CreateCachesProcessor proc = get("dummycreatecaches_2.xml");
        Node source = getNode(proc.getDirectory());
        CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());
        source.commit();
        job.waitUntilReady();
        source = refresh(source);
        checkSource(source, 3);
    }

    @Test
    public  void twoStepsTwoResults() throws Exception {
        CreateCachesProcessor proc = get("dummycreatecaches_3.xml");
        Node source = getNode(proc.getDirectory());
        CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());
        source.commit();
        job.waitUntilReady();
        source = refresh(source);
        checkSource(source, 4);
    }

    @Test
    public  void ignoreByMimeType() throws Exception {
        CreateCachesProcessor proc = get("dummycreatecaches_4.xml");
        Node source = getNode(proc.getDirectory());
        CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());
        source.commit();
        job.waitUntilReady();
        source = refresh(source);
        checkSource(source, 1);
    }

    @Test
    public  void twoStepsTwoResultsIgnoreAudio() throws Exception {
        CreateCachesProcessor proc = get("dummycreatecaches_5.xml");
        Node source = getNode(proc.getDirectory());
        CreateCachesProcessor.Job job = proc.createCaches(source.getCloud(), source.getNumber());
        source.commit();
        job.waitUntilReady();
        source = refresh(source);
        checkSource(source, 4);
    }
}


