package org.mmbase.bridge.tests;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import java.io.*;
import java.util.*;
import org.mmbase.util.images.*;
import org.mmbase.util.transformers.AbstractCommandStringTransformer;
import org.mmbase.util.transformers.CharTransformer;

import org.mmbase.util.functions.Function;
import org.mmbase.util.functions.Parameters;

/**
 * JUnit tests for convertimage-interface implementation.
 *
 * @author Michiel Meeuwissen
 * @version $Id$
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConvertImageTest extends org.mmbase.tests.BridgeTest {

    private final static String JPG_IMAGE_NAME = "testimage.jpg";

    protected int countIcaches() {
        Cloud cloud = getCloud();
        NodeManager nodeManager = cloud.getNodeManager("icaches");
        return Queries.count(nodeManager.createQuery());
    }

    public void test001ImportedJpegImage() {
        Cloud cloud = getCloud();
        Node node = cloud.getNode("jpeg.test.image");
        node.getByteValue("handle");
        assertTrue("MMBase failed to determine mime-type properly (magicfile problem?)", node.getStringValue("itype").equals("jpeg"));
        //node.delete();
    }

    //test if an image can be converted using the getIntValue
    public void test002GetInvalueCachedImage() {
        Cloud cloud = getCloud();
        Node node = cloud.getNode("jpeg.test.image");
        int icacheNodeNumber = node.getIntValue("cache(s(30x30))");

        assertEquals(1, countIcaches());
        Node icache = cloud.getNode(icacheNodeNumber);
        icache.getFunctionValue("wait", null); // triggers actual conversion
        assertEquals(1, countIcaches());
    }

    // test if an image can be converted using getFunctionValue
    public void test003FunctionValueCachedImage() {
        Cloud cloud = getCloud();
        Node node = cloud.getNode("jpeg.test.image");
        Function f = node.getFunction("cache");
        Parameters p = f.createParameters();
        p.set("template","s(31x31)");
        int nn = (Integer) f.getFunctionValue(p);
        assertEquals(2, countIcaches());
        Node icache = cloud.getNode(nn);
        icache.getFunctionValue("wait", null);
        assertEquals(2, countIcaches());
    }

    protected Map<String, String> breakImaging() {
        Map<String, String> originalParameters = Factory.getParameters();
        Factory.shutdown();
        Map<String, String> brokenParameters = new HashMap<String, String>();
        brokenParameters.putAll(originalParameters);
        brokenParameters.put("ImageConvert.ConverterCommand", "nonexistingbinary");
        Factory.init(brokenParameters);
        return originalParameters;
    }

    protected void restoreImaging(Map<String, String> parameters) {
        Factory.shutdown();
        Factory.init(parameters);
    }


    public void test004FailAnImage() {
        // MMB-495
        Map<String, String> originalParameters = breakImaging();

        Cloud cloud = getCloud();
        Node node = cloud.getNode("jpeg.test.image");
        Function f = node.getFunction("cache");
        Parameters p = f.createParameters();
        p.set("template","s(32x32)");
        int icacheNumber = (Integer) f.getFunctionValue(p);
        assertEquals(3, countIcaches());
        assertTrue(icacheNumber > 0);
        Node icache = cloud.getNode(icacheNumber);
        icache.getFunctionValue("wait", null); // this should fail because imaging is broken now

        //hence:
        assertTrue("Found icache: " + icache.getNumber() + " " + icache.getSize("handle") + " bytes", icache.isNull("handle"));
        assertEquals(3, countIcaches());

        restoreImaging(originalParameters);

        assertEquals(new Integer(icacheNumber), (Integer) f.getFunctionValue(p));
        assertEquals(3, countIcaches());

        icache.getFunctionValue("wait", null); // should succeed now.
        assertEquals(3, countIcaches());
        assertFalse(icache.isNull("handle"));

        assertTrue(icache.getSize("handle") > 0);


    }

    // Java api to get access times is only available in java 7. This is a quick and dirty hack, at
    // least working in linux. It does not actually try to determin the access times of individual
    // files, it simply returns a string containing the access times. For test cases 'equals' on the
    //string should suffice.

    public String getImagesAccessTimes() {
        CharTransformer accessTimes = new AbstractCommandStringTransformer() {
                public String[] getCommand() {
                    return new String[] {"xargs", "ls", "-lu"};
                }
            };
        return accessTimes.transform(System.getProperty("user.dir") + "/data/test/mmbasetests_images");
    }

    public void test006AccessTime() throws InterruptedException {
        if(System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows")) {
            System.err.println("Pretty sure this test won't work in Windows. Skippig it.");
        } else {
            // MMB-1386
            String accessTimesBefore = getImagesAccessTimes();
            // make sure it is a minute later.
            Thread.sleep(61000);
            org.mmbase.cache.CacheManager.getCache("Nodes").clear();
            Node node = cloud.getNode("jpeg.test.image"); // just gettign the node, should not access the file
            assertEquals(accessTimesBefore, getImagesAccessTimes());
            assertFalse(node.isNull("handle")); // checking for file existance only, should not access the file
            assertEquals(accessTimesBefore, getImagesAccessTimes());
            node.setStringValue("title", "the images title"); // setting another field, should not access the file
            assertEquals(1, node.getChanged().size());
            node.commit();
            assertEquals(accessTimesBefore, getImagesAccessTimes());
        }
    }


    //test what happens if template is errorneous
    public void test007ErrorneousTemplate() {
        Cloud cloud = getCloud();
        Query q = cloud.getNodeManager("icaches").createQuery();

        int count = Queries.count(q);
        Node node = cloud.getNode("jpeg.test.image");
        Function f = node.getFunction("cache");
        Parameters p = f.createParameters();
        p.set("template","s(30x30");
        f.getFunctionValue(p);
        // TODO, add some aaserts here, on about what it means to have used an errorneaous template?
        // -Should the icache have been created?
        // -if it should, it should of course not been created again if we ask it again
        // etc.
    }

    private static Cloud cloud;


    ///Sets up before each test.
    public void setUp() throws Exception {
        if (cloud == null) {
            startMMBase();
            startLogging();
            cloud = getCloud();
        }
        if (! cloud.hasNode("jpeg.test.image")) {

            NodeManager nodeManager = cloud.getNodeManager("images");
            Node jpegNode = nodeManager.createNode();
            jpegNode.setStringValue("title", JPG_IMAGE_NAME);
            byte[] bytes = getTextImageBytes(JPG_IMAGE_NAME);
            jpegNode.setByteValue("handle", bytes);
            jpegNode.commit();
            jpegNode.createAlias("jpeg.test.image");
        }
    }


    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        Cloud cloud = getCloud();
        //Node node = cloud.getNode("jpeg.test.image");
        //node.delete();
    }
    /**
     * read the test image from the file system and return it a byte[]
     *
     * @return the byte[] containing the image
     */
    private byte[] getTextImageBytes(String name) {
        try {
            InputStream in = this.getClass().getResourceAsStream(name);

            if (in == null) {
                throw new Error("No such resource '" + name + "'");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[200];
            int readSize = 0;
            while ((readSize = in.read(buffer)) > 0) {
                baos.write(buffer, 0, readSize);
            }
            return baos.toByteArray();
        } catch (IOException ioe) {
            fail("did not find image to load " + this.getClass().getResource(name));
        }
        return null;
    }
}
