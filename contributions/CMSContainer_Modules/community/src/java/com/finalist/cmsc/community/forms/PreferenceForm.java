package com.finalist.cmsc.community.forms;

import org.apache.struts.action.ActionForm;

public class PreferenceForm  extends ActionForm {

   private String id;
   
   private String userId;
   
   private String module;
   
   private String key;
   
   private String value;
   
   private String offset;
   
   private String order;  
   
   private String direction;  
   
   private String method;  
   
   public String getUserId() {
      return userId;
   }

   public void setUserId(String userId) {
      this.userId = userId;
   }

   public String getModule() {
      return module;
   }

   public void setModule(String module) {
      this.module = module;
   }

   public String getKey() {
      return key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getOffset() {
      return offset;
   }

   public void setOffset(String offset) {
      this.offset = offset;
   }

   public String getOrder() {
      return order;
   }

   public void setOrder(String order) {
      this.order = order;
   }

   public String getDirection() {
      return direction;
   }

   public void setDirection(String direction) {
      this.direction = direction;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(String method) {
      this.method = method;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }
  
}
