// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GlossaryFactory.java

package com.finalist.cmsc.module.glossary;

import net.sf.mmapps.modules.cloudprovider.CloudProviderFactory;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeList;
import org.mmbase.bridge.NodeManager;

import java.util.Iterator;

// Referenced classes of package com.finalist.cmsc.module.glossary:
//            Glossary

public class GlossaryFactory
{

    public GlossaryFactory()
    {
    }

    public static Glossary getGlossary()
    {
        Glossary glossary = Glossary.instance();
        Cloud cloud = CloudProviderFactory.getCloudProvider().getAnonymousCloud();

        NodeManager manager = cloud.getNodeManager("glossary");
        NodeList list = manager.createQuery().getList();

        Iterator<Node> nodeListIterator = list.iterator();

        while (nodeListIterator.hasNext()) {
            Node node = nodeListIterator.next();
            Glossary.instance().addTerm(node.getStringValue("term"), node.getStringValue("definition"));
        }
      
        return glossary;
    }
}
