package com.finalist.cmsc.module.glossary;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glossary {
   public static final String LINKPATTERN = "<dfn class=\"glossaryWord\" title=\"%2$s - %s\" id=\"_glossary_%2$s\">%2$s</dfn>";
   public static final String GLOSSARY = "glossary";
   private final Map<String, String> terms = new HashMap<String, String>();
   private static Glossary glossary = null;

   public void addTerm(String term, String description) {
      terms.put(term, description);
   }

   public void removeTerm(String term) {
      terms.remove(term);
   }

   
   public String mark(String material) {
      Set<String> keyWords = terms.keySet();

      for (String word : keyWords) {

         Pattern pattern = Pattern.compile(String.format("\\b[%s%s]%s\\b", word.substring(0,1).toLowerCase(), word.substring(0, 1).toUpperCase(), word.substring(1)));
         Matcher matcher = pattern.matcher(material);

         while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String found = material.substring(start, end);
            if (!isInFormatedFragment(material, word, start)) {
               String highlight = String.format(LINKPATTERN, terms.get(word), found);
               material = (new StringBuilder()).append(material.substring(0, start)).append(highlight).append(
                     material.substring(end, material.length())).toString();
               break;
            }
         }
      }
      return material;

   }


   private boolean isInFormatedFragment(String material, String keyword, int keywordStartPosition) {
      String materialBefore = material.substring(0, keywordStartPosition);
      
      int openA = materialBefore.lastIndexOf("<a");
      int closeA = materialBefore.lastIndexOf("/a>");
      if(openA > closeA) {
         return true;
      }

      int openInput = materialBefore.lastIndexOf("<input");
      int closeInput = materialBefore.lastIndexOf(">");
      if(openInput > closeInput) {
         return true;
      }

      int openSpan = materialBefore.lastIndexOf("<span class=\"noGlossary\">");
      int closeSpan = materialBefore.lastIndexOf("</span>");
      if(openSpan > closeSpan) {
         return true;
      }

      for(int h = 1; h <= 6; h++) {
         int openH = materialBefore.lastIndexOf("<h"+h);
         int closeH = materialBefore.lastIndexOf("/h"+h+">");
         if(openH > closeH) {
            return true;
         }
      }

      return false;
   }
   

   public static synchronized Glossary instance() {
      if (null == glossary) {
         glossary = new Glossary();
      }
      return glossary;
   }


   public Map<String, String> getTerms() {
      return terms;
   }
}
