package com.finalist.cmsc.publish;

import java.util.*;

import org.mmbase.bridge.*;
import com.finalist.cmsc.repository.RepositoryUtil;

/**
 * @author Jeoffrey Bakker, Finalist IT Group
 */
public class ChannelPublisher extends Publisher{

   public ChannelPublisher(Cloud cloud) {
      super(cloud);
   }

   @Override
   public boolean isPublishable(Node node) {
      return RepositoryUtil.isContentChannel(node) || RepositoryUtil.isCollectionChannel(node);
   }

   @Override
   public void publish(Node channel, NodeList contentnodes) {
       List<Integer> relatedNodes = new ArrayList<Integer>();
       for (Iterator<Node> iterator = contentnodes.iterator(); iterator.hasNext();) {
           Node content = iterator.next();
           if (isPublished(content)) {
               relatedNodes.add(content.getNumber());
           }
       }
       publishNode(channel, relatedNodes);
   }

}
