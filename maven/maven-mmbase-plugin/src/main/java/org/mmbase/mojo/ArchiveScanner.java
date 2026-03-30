/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.mojo;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.plexus.util.DirectoryScanner;



/**
 * @javadoc
 */
public class ArchiveScanner extends DirectoryScanner {

   public static void main(String[] args) {
      try {
         ArchiveScanner scanner = new ArchiveScanner();
         scanner.jarfile = new JarFile("C:\\projects\\.m2\\org\\mmbase\\mmbase\\1.9-SNAPSHOT\\mmbase-1.9-SNAPSHOT.jar");
         scanner.setExcludes(new String[] {"**/*.xml"});
         scanner.scan();

         String[] filesIncluded = scanner.getIncludedFiles();
         for (int i = 0; i < filesIncluded.length; i++) {
            String string = filesIncluded[i];
            System.out.println(string);
         }
      }
      catch (IllegalStateException e) {
         e.printStackTrace();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }

   JarFile jarfile;

   public void scan() throws IllegalStateException {
      if ( includes == null ) {
          // No includes supplied, so set it to 'matches all'
          includes = new String[1];
          includes[0] = "**";
      }
      if ( excludes == null ) {
          excludes = new String[0];
      }

       setupDefaultFilters();
       setupMatchPatterns();

       filesIncluded = new ArrayList<String>();
       filesNotIncluded = new ArrayList<String>();
       filesExcluded = new ArrayList<String>();
       filesDeselected = new ArrayList<String>();
       dirsIncluded = new ArrayList<String>();
       dirsNotIncluded = new ArrayList<String>();
       dirsExcluded = new ArrayList<String>();
       dirsDeselected = new ArrayList<String>();



       List<String> jarEntries = new ArrayList<String>();
      Enumeration<JarEntry> en = jarfile.entries();
      while (en.hasMoreElements()) {
         JarEntry entry = en.nextElement();
         String name = entry.getName().trim().replace('/', File.separatorChar).replace('\\',
               File.separatorChar);
         jarEntries.add(name);
      }
      String[] newfiles = jarEntries.toArray(new String[jarEntries.size()]);

      String fileSeparator = String.valueOf(File.separatorChar);

      for (int i = 0; i < newfiles.length; i++) {
         String name = newfiles[i];
         if (name.endsWith(fileSeparator)) {
            if (isIncluded(name)) {
               if (!isExcluded(name)) {
                  dirsIncluded.add(name);
               }
               else {
                  everythingIncluded = false;
                  dirsExcluded.add(name);
               }
            }
            else {
               everythingIncluded = false;
               dirsNotIncluded.add(name);
            }
         } else {
            if (isIncluded(name)) {
               if (!isExcluded(name)) {
                  filesIncluded.add(name);
               }
               else {
                  everythingIncluded = false;
                  filesExcluded.add(name);
               }
            }
            else {
               everythingIncluded = false;
               filesNotIncluded.add(name);
            }
         }
      }

   }

   protected void slowScan() {
      scan();
   }

   public String[] getIncludedFiles() {
      return convertScanEntries(super.getIncludedFiles());
   }

   public String[] getExcludedFiles() {
      return convertScanEntries(super.getExcludedFiles());
   }

   public String[] getIncludedDirectories() {
      return convertScanEntries(super.getIncludedDirectories());
   }

   public String[] getExcludedDirectories() {
      return convertScanEntries(super.getExcludedDirectories());
   }

   private String[] convertScanEntries(String[] entries) {
      for (int i = 0; i < entries.length; i++) {
         String name = entries[i];
         entries[i] = name.replace(File.separatorChar, '/');
      }
      return entries;
   }

   public void setJarfile(JarFile jarfile) {
      this.jarfile = jarfile;
   }

}
