package com.finalist.newsletter.cao;

import com.finalist.newsletter.domain.Newsletter;
import com.finalist.newsletter.domain.Term;

import java.util.List;
import java.util.Collection;
import java.util.Set;

import org.mmbase.bridge.Node;

public interface NewsletterCAO {
   public Newsletter getNewsletterById(int id);

   public List<Term> getALLTerm();

   public List<Newsletter> getNewsletterByConstraint(String property, String constraintType, String value);

   public int getNewsletterIdBySubscription(int id);
   
   public Set<Term> getNewsletterTermsByName(int newsltterId, String name ,int pagesize, int offset);
}
