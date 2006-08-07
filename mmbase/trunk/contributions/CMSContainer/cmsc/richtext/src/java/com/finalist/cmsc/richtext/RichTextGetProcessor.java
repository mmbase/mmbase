/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package com.finalist.cmsc.richtext;

import java.util.*;

import net.sf.mmapps.commons.util.StringUtil;

import org.mmbase.bridge.*;
import org.mmbase.bridge.Node;
import org.mmbase.datatypes.processors.Processor;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.w3c.dom.*;
import org.w3c.dom.NodeList;

import com.finalist.cmsc.mmbase.ResourcesUtil;


@SuppressWarnings("serial")
public class RichTextGetProcessor implements Processor {

    /** MMbase logging system */
    private static Logger log = Logging.getLoggerInstance(RichTextGetProcessor.class.getName());
    
    public Object process(Node node, Field field, Object value) {
       String out = "";
       String in = (String) value;
       if (!StringUtil.isEmpty(in) && RichText.hasRichtextItems(in)) {
           try {
               Document doc = RichText.getRichTextDocument(in);
               if (doc == null) {
                   out = in;
               }
               else {
                   resolve(node, doc);
                   out = RichText.getRichTextString(doc);
               }
           } catch (Exception e) {
               log.error("resolve failed " +node.getNumber() + " " + field.getName(), e);
               throw new RuntimeException(e);
           }
       }
       else {
          out = in;
       }
       return out;
    }

    /**
     * Transform given richtext field data links to frontend links using given
     * navigation bean.
     * 
     * @param node MMbase node
     * @param doc richtext field data
     * @return transformed field data DOM object
     */
    public Document resolve(Node node, Document doc) {
        Cloud cloud = node.getCloud();
        
        Map<String,String> inlineLinks = new HashMap<String,String>();
        RelationList links = node.getRelations(RichText.INLINEREL_NM, null, "DESTINATION");
        for (Iterator iter = links.iterator(); iter.hasNext();) {
            Relation inlineRel = (Relation) iter.next();
            inlineLinks.put(inlineRel.getStringValue(RichText.REFERID_FIELD), inlineRel.getStringValue("dnumber"));
        }
        
        // transform links
        NodeList linklist = doc.getElementsByTagName(RichText.LINK_TAGNAME);
        if (linklist.getLength() > 0) {
            resolveLinks(cloud, linklist, inlineLinks);
        }

        Map<String,Relation> inlineImages = new HashMap<String,Relation>();
        RelationList images = node.getRelations(RichText.IMAGEINLINEREL_NM, cloud.getNodeManager("images"), "DESTINATION");
        for (Iterator iter = images.iterator(); iter.hasNext();) {
            Relation inlineRel = (Relation) iter.next();
            inlineImages.put(inlineRel.getStringValue(RichText.REFERID_FIELD), inlineRel);
        }

        // transform all images
        NodeList imglist = doc.getElementsByTagName(RichText.IMG_TAGNAME);
        log.debug("" + imglist.getLength() + " images found in richtext.");
        if (imglist.getLength() > 0) {
            resolveImages(cloud, imglist, inlineImages);
        }

        return doc;
    }

    /**
     * Find image tags in the text and replace them with marked up blocks
     * including the corrected image tag.
     */
    private void resolveImages(Cloud cloud, NodeList nl, Map<String, Relation> inlineImages) {
       for (int j = nl.getLength() - 1; j >= 0; --j) {
          Element image = (Element) nl.item(j);

          if (image.hasAttribute(RichText.DESCRIPTION_ATTR)
                && !(image.hasAttribute(RichText.TITLE_ATTR) && image.hasAttribute(RichText.ALT_ATTR))) {
             String desc = image.getAttribute(RichText.DESCRIPTION_ATTR);
             image.removeAttribute(RichText.DESCRIPTION_ATTR);
             image.setAttribute(RichText.TITLE_ATTR, desc);
             image.setAttribute(RichText.ALT_ATTR, desc);
          }

          if (image.hasAttribute(RichText.RELATIONID_ATTR)) {
             String imgidrel = image.getAttribute(RichText.RELATIONID_ATTR);
             log.debug("Creating image by relation " + imgidrel);

             if (!inlineImages.containsKey(imgidrel)) {
                log.error("Relation "
                            + imgidrel
                            + " for inline image not found. Ignoring this image!!! It will NOT be displayed.");
                continue;
             }
             Relation inlineRel = inlineImages.get(String.valueOf(imgidrel));
             String imageId = inlineRel.getStringValue("dnumber");

             Node imageNode = cloud.getNode(imageId);

             int height = inlineRel.getIntValue("height");
             int width = inlineRel.getIntValue("width");
            
             imageNode = ResourcesUtil.getImageNode(imageNode, height, width);
             String servletPath = ResourcesUtil.getServletPath(imageNode, imageNode.getStringValue("number"));

             image.setAttribute(RichText.SRC_ATTR, servletPath);
             if (width > 0) {
                image.setAttribute(RichText.WIDTH_ATTR, String.valueOf(width));
             }
             if (height > 0) {
                image.setAttribute(RichText.HEIGHT_ATTR, String.valueOf(height));
             }
          } else {
             log.debug("ImageTag does not contain relationId attribute. Skipping this image.");
          }
       }
    }

    /**
     * Find a tags in the text and replace them with valid links
     */
    private void resolveLinks(Cloud cloud, NodeList nl, Map<String, String> inlineLinks) {
       for (int j = nl.getLength() - 1; 0 <= j; --j) {
          Element aElement = (Element) nl.item(j);

          if (aElement.hasAttribute(RichText.RELATIONID_ATTR)) {
             log.debug("Creating link to attachment/article by inlinerel");
             String idrel = aElement.getAttribute(RichText.RELATIONID_ATTR);

             if (!inlineLinks.containsKey(String.valueOf(idrel))) {
                log.error("Relation "
                            + idrel
                            + " for inline link not found. Ignoring this link!!! It will NOT be displayed.");
                continue;
             }
             String aElementId = inlineLinks.get(String.valueOf(idrel));
             
             Node destinationNode = cloud.getNode(aElementId);

             String name = null;
             String url = null;
             String builderName = destinationNode.getNodeManager().getName();
             if ("attachments".equals(builderName)) {
                 name = destinationNode.getStringValue(RichText.TITLE_ATTR);
                 url = ResourcesUtil.getServletPath(destinationNode, destinationNode.getStringValue("number"));
             } else {
                 if ("urls".equals(builderName)) {
                     name = destinationNode.getStringValue("name");
                     url = destinationNode.getStringValue("url");
                 }
                 else {
                     name = destinationNode.getStringValue(RichText.TITLE_ATTR);
                     url = getContentUrl(destinationNode);
                 }
             }
             
             if (aElement.hasAttribute(RichText.HREF_ATTR)) {
                 aElement.removeAttribute(RichText.HREF_ATTR);
             }
             aElement.setAttribute(RichText.HREF_ATTR,url);
             
             if (!aElement.hasAttribute(RichText.TITLE_ATTR)) {
                 aElement.setAttribute(RichText.TITLE_ATTR, name);
             }
          }
       }
    }

    private String getContentUrl(Node node) {
        String servletpath = ResourcesUtil.getServletPathWithAssociation("content", "/content/*");
        return servletpath + node.getStringValue("number") + "/" + node.getStringValue("title");
    }

}
