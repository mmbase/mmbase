/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;


import java.net.URL;
import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;
/**
 * Test the working of the ResourceLoader.
 *
 * <ul>
 * <li>tests if the resource loader can run when mmbase is not started</li>
 * </ul>
 *
 * @author Kees Jongenburger
 * @verion $Id$
 */
public class ResourceLoaderTest {

    /**
     * perform lookup of non existing resource
     */
    @Test
    public void testNonExistingResource() throws java.io.IOException {
        URL url = ResourceLoader.getConfigurationRoot().getResource("nonExisting/test.xml");
        assertTrue("non existing resource should not be openable for input", !url.openConnection().getDoInput());
    }

    /**
     * perform lookup of mmbaseroot.xml using getConfigurationroot
     */
    @Test
    public void testGetMMBaseRootModule() throws java.io.IOException {
        URL url = ResourceLoader.getConfigurationRoot().getResource("modules/mmbaseroot.xml");
        assertNotNull("did not find mmbaseroot.xml", url);
        assertTrue("non existing resource should openable for input", url.openConnection().getDoInput());
    }

    @Test
    public void testGetPropertiesBuilder() throws java.io.IOException {
        URL url = ResourceLoader.getConfigurationRoot().getResource("builders/properties.xml");
        assertNotNull("did not find properties.xml", url);
        assertTrue("non existing resource should openable for input", url.openConnection().getDoInput());
    }
    @Test
    public void testGetPropertiesBuilder2() throws java.io.IOException {
        URL url = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResource("properties.xml");
        assertNotNull("did not find properties.xml", url);
        assertTrue("non existing resource should openable for input", url.openConnection().getDoInput());
    }

    @Test
    public void testGetPropertiesBuilder3() throws java.io.IOException {
        URL url = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResource("/properties.xml");
        assertNotNull("did not find /properties.xml", url);
        assertTrue("non existing resource should openable for input", url.openConnection().getDoInput());
        System.out.println(url);
    }

    @Test
    public void testBuilders() throws java.io.IOException {
        Set<String> xmls = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResourcePaths(ResourceLoader.XML_PATTERN, true);
        assertTrue("" + xmls + " did not contain  properties.xml", xmls.contains("properties.xml"));
        assertTrue("" + xmls + " did not contain  core/object.xml", xmls.contains("core/object.xml"));

    }

    @Test
    public void testBuilders2() throws java.io.IOException {
        List<URL> xmls1 = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResourceList("/properties.xml");
        List<URL> xmls2 = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResourceList("properties.xml");
        assertEquals(xmls1, xmls2);

    }
    // @Test TODO, this test is broken now.
    public void testWeightConfiguration() throws java.io.IOException {
        URL u  = ResourceLoader.getConfigurationRoot().getChildResourceLoader("builders").getResource("core/object.xml");
        assertTrue(u.toString(), u.toString().endsWith("/mmbase-tests-1.jar!/org/mmbase/config/builders/core/object.xml"));
    }

}
