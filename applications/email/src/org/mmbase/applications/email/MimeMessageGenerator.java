/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/

package org.mmbase.applications.email;

import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.*;


import org.mmbase.util.logging.Logging;
import org.mmbase.util.logging.Logger;

/**
 * @javadoc
 * @author Daniel Ockeloen
 * 
 */
public class MimeMessageGenerator {

    private static final Logger log = Logging.getLoggerInstance(MimeMessageGenerator.class);



    /**
     * @javadoc
     */
    public static MimeMultipart getMimeMultipart(String text) {
        
        Hashtable<String, MimeBodyTag> nodes = new Hashtable<String, MimeBodyTag>();
        Vector<MimeBodyTag> rootnodes = new Vector<MimeBodyTag>();


	Enumeration<MimeBodyTag> tags=MimeBodyTagger.getMimeBodyParts(text);
	while (tags.hasMoreElements()) {
            try {
		MimeBodyTag tag=tags.nextElement();

		// get all the needed fields
		String type=tag.getType();
		String id=tag.getId();
		String related=tag.getRelated();
		String alt=tag.getAlt();

		// add it to the id cache
		nodes.put(id,tag);

		// is it a root node ?
		if (alt==null && related==null) {
                    rootnodes.addElement(tag);
		} else if (alt!=null) {
                    MimeBodyTag oldpart=nodes.get(alt);
                    if (oldpart!=null) {
                        oldpart.addAlt(tag);
                    }
		} else if (related!=null) {
                    MimeBodyTag oldpart=nodes.get(related);
                    if (oldpart!=null) {
                        oldpart.addRelated(tag);
                    }
		}	

            } catch(Exception e) {
		log.error("Mime mail error");
            }
	}

	if (rootnodes.size()==1) {
            MimeBodyTag t=rootnodes.elementAt(0);
            MimeMultipart mmp=t.getMimeMultipart();
            if (mmp!=null) {
                return(mmp);
            }
	} else {
        if (rootnodes.size()>1) {
            try {
                MimeMultipart root = new MimeMultipart();
                root.setSubType("mixed");
                Enumeration<MimeBodyTag> l = rootnodes.elements();
                while (l.hasMoreElements()) {
                    MimeBodyTag t = l.nextElement();
                    MimeMultipart mmp = t.getMimeMultipart();
                    if (mmp != null) {
                        log.info("setting parent info : " + t.getId());
                        MimeBodyPart wrapper = new MimeBodyPart();
                        wrapper.setContent(mmp);
                        root.addBodyPart(wrapper);
                    }
                    else {
                        log.info("adding info : " + t.getId());
                        root.addBodyPart(t.getMimeBodyPart());
                    }
                }
                return root;
            } catch (MessagingException e) {
                log.error("Root generation error" + e.getMessage());
            }
    	} else {
                log.error("Don't have a root node");
    	}
    }
	return null;
    }

}
