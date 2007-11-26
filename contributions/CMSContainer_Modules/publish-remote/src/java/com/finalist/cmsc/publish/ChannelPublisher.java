package com.finalist.cmsc.publish;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.remotepublishing.PublishManager;
import org.mmbase.remotepublishing.util.PublishUtil;
import com.finalist.cmsc.repository.RepositoryUtil;

/**
 * @author Jeoffrey Bakker, Finalist IT Group
 */
public class ChannelPublisher extends Publisher {

   public ChannelPublisher(Cloud cloud) {
      super(cloud);
   }


   @Override
   public boolean isPublishable(Node node) {
      return RepositoryUtil.isContentChannel(node) || RepositoryUtil.isCollectionChannel(node);
   }


   @Override
   public void publish(Node node) {
      PublishUtil.publishOrUpdateNode(cloud, node.getNumber());
   }


   @Override
   public void publish(Node channel, NodeList contentnodes) {
      List<Integer> relatedNodes = new ArrayList<Integer>();
      for (Iterator<Node> iterator = contentnodes.iterator(); iterator.hasNext();) {
         Node content = iterator.next();
         if (PublishManager.isPublished(content)) {
            relatedNodes.add(content.getNumber());
         }
      }
      PublishUtil.publishOrUpdateRelations(cloud, channel.getNumber(), relatedNodes);
   }


   @Override
   public void remove(Node node) {
      PublishUtil.removeFromQueue(node);
   }


   @Override
   public void unpublish(Node node) {
      PublishUtil.removeNode(cloud, node.getNumber());
   }
}
