package com.finalist.cmsc.rating.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.finalist.cmsc.rating.util.RatingUtil;

public class SetTag extends SimpleTagSupport {

   private int number;
   private int value;
   private String user;

   public void doTag() throws JspException, IOException {
      
      RatingUtil.setUserRating(number, user, value);
   }

   public void setNumber(int number) {
      this.number = number;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public void setValue(int value) {
      this.value = value;
   }

   
}
