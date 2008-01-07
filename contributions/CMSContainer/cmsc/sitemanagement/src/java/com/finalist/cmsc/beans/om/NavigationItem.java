package com.finalist.cmsc.beans.om;

import java.util.Date;

import net.sf.mmapps.commons.beans.NodeBean;

@SuppressWarnings("serial")
public class NavigationItem extends NodeBean implements Comparable<NavigationItem> {

   private String title;
   private String urlfragment;
   private String description;
   private Date creationdate;
   private Date lastmodifieddate;
   private Date publishdate;
   private Date expirydate;
   private boolean use_expiry;
   private String lastmodifier;


   public int compareTo(NavigationItem o) {
      return title.compareTo(o.title);
   }


   public String getTitle() {
      return title;
   }


   public void setTitle(String title) {
      this.title = title;
   }


   public String getUrlfragment() {
      return urlfragment;
   }


   public void setUrlfragment(String urlfragment) {
      this.urlfragment = urlfragment;
   }


   public String getDescription() {
      return description;
   }


   public void setDescription(String description) {
      this.description = description;
   }


   public Date getCreationdate() {
      return creationdate;
   }


   public void setCreationdate(Date creationdate) {
      this.creationdate = creationdate;
   }


   public Date getLastmodifieddate() {
      return lastmodifieddate;
   }


   public void setLastmodifieddate(Date lastmodifieddate) {
      this.lastmodifieddate = lastmodifieddate;
   }


   public Date getPublishdate() {
      return publishdate;
   }


   public void setPublishdate(Date publishdate) {
      this.publishdate = publishdate;
   }


   public Date getExpirydate() {
      return expirydate;
   }


   public void setExpirydate(Date expirydate) {
      this.expirydate = expirydate;
   }


   public boolean isUse_expirydate() {
      return use_expiry;
   }


   public void setUse_expirydate(boolean use_expiry) {
      this.use_expiry = use_expiry;
   }


   public String getLastmodifier() {
      return lastmodifier;
   }


   public void setLastmodifier(String lastmodifier) {
      this.lastmodifier = lastmodifier;
   }
}
