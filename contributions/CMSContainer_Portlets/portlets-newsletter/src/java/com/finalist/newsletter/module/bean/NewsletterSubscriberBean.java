package com.finalist.newsletter.module.bean;

public class NewsletterSubscriberBean {

   private String userName;
   private int numberOfThemes;

   public NewsletterSubscriberBean() {

   }

   /**
    * @return the userName
    */
   public String getUserName() {
      return userName;
   }

   /**
    * @param userName
    *           the userName to set
    */
   public void setUserName(String userName) {
      this.userName = userName;
   }

   /**
    * @return the numberOfThemes
    */
   public int getNumberOfThemes() {
      return numberOfThemes;
   }

   /**
    * @param numberOfThemes
    *           the numberOfThemes to set
    */
   public void setNumberOfThemes(int numberOfThemes) {
      this.numberOfThemes = numberOfThemes;
   }

}