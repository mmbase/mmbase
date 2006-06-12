package com.finalist.cmsc.taglib;

import java.util.Date;
import java.util.Locale;
import javax.servlet.jsp.*;

import net.sf.mmapps.commons.util.DateUtil;

import org.mmbase.bridge.jsp.taglib.ContextReferrerTag;
import org.mmbase.bridge.jsp.taglib.Writer;

/**
 * Tag class for displaying dates in a specific format
 */
@SuppressWarnings("serial")
public class DateFormatterTag extends ContextReferrerTag {
   
   /** the begindate to format */
   protected Long begindate;
   
   /** the enddate to format */
   protected Long enddate;
   
   /** Indicates if the time should be displayed */
   protected boolean displaytime;
   
   public void setBegindate(Long begindate) {
      this.begindate = begindate;
   }
   
   public void setEnddate(Long enddate) {
      this.enddate = enddate;
   }
   
   public void setDisplaytime(String displaytime) {
      this.displaytime = (displaytime == null) ? false : (displaytime.equalsIgnoreCase("true"));
   }
   
   /**
     * @see javax.servlet.jsp.tagext.BodyTagSupport#doStartTag()
     */
    public int doStartTag() {
        return SKIP_BODY;
    }
   
    /**
     * @see org.mmbase.bridge.jsp.taglib.ContextReferrerTag#doEndTag()
     */
    public int doEndTag() throws javax.servlet.jsp.JspTagException {
        try {
            Locale locale = getLocale();
            if (enddate == null) {
                Long outputDate = null;
                if (this.begindate == null) {
                    Writer w = findWriter(false);
                    if (w != null) {
                        Object object = w.getWriterValue();
                        if (object instanceof Date) {
                            outputDate = Long.valueOf(((Date) object).getTime());
                        } else {
                            throw new JspTagException("Cannot evaluate time. No attribute given and no writer parent tag found.");
                        }
                    }
                }
                else {
                    outputDate = begindate;
                }
                
                if (displaytime) {
                    pageContext.getOut().write(
                            DateUtil.displayDateWithTime(outputDate, locale));
                }
                else {
                    pageContext.getOut().write(
                            DateUtil.displayDate(outputDate, locale));
                }
            }
            else {
                pageContext.getOut().write(
                        DateUtil.displayDate(this.begindate, this.enddate, locale));
            }
        }
        catch (java.io.IOException e) {
            throw new JspTagException("IO Error: " + e.getMessage());
        }
        return EVAL_PAGE;
    }
}