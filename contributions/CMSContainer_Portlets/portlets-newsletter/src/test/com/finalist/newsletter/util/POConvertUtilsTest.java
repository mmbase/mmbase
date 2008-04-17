package com.finalist.newsletter.util;

import com.finalist.newsletter.BaseNewsletterTest;
import org.mmbase.bridge.NodeManager;
import org.mmbase.bridge.Node;

import java.lang.reflect.InvocationTargetException;

public class POConvertUtilsTest extends BaseNewsletterTest {

   protected void setUp() throws Exception {
      super.setUp();
   }

   public void testConvert() throws IllegalAccessException, InvocationTargetException, InstantiationException {
      NodeManager manager = cloud.getNodeManager("contentelement");
      Node node = manager.createNode();
      node.setStringValue("description", "adescription");
      Model model = new Model();
      new POConvertUtils<Model>().convert(model, node);
      assertEquals("adescription", model.getDescription());
   }


}
