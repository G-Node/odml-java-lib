package odml.util;

/************************************************************************
 * odML - open metadata Markup Language - Copyright (C) 2009, 2010 Jan Grewe, Jan Benda
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License (LGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 * 
 * odML is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this software. If not, see
 * <a href="http://gnu.org/licenses">http://gnu.org/licenses</a>.
 */

import odml.core.Reader;
import odml.core.Section;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import static java.lang.System.out;

public class TerminologyManager {

   private static TerminologyManager      instance           = null;
   private static final String            TERMINOLOGIES_FILE = System.getProperty("user.home")
                                                                   + System
                                                                         .getProperty("file.separator")
                                                                   + "odml"
                                                                   + System
                                                                         .getProperty("file.separator")
                                                                   + "terminologies.properties";
   private static final String            REDIRECTIONS       = System.getProperty("user.home")
                                                                   + System
                                                                         .getProperty("file.separator")
                                                                   + "odml"
                                                                   + System
                                                                         .getProperty("file.separator")
                                                                   + "redirections.properties";
   private static final String            COMMENT            = "locally stored odml terminologies.";
   private final HashMap<String, Section> urlSectionHash     = new HashMap<String, Section>();
   // Properties hash for user settings
   private Properties                     localTerminologies, redirections;


   /**
    * Constructor
    */
   private TerminologyManager() {
      try {
         // user configuration
         File userFile = new File(TERMINOLOGIES_FILE);
         if (!userFile.exists()) {
            File dir = new File(System.getProperty("user.home")
                  + System.getProperty("file.separator") + "odml");
            if (!dir.exists())
               dir.mkdir();
            userFile.createNewFile();
         }
         localTerminologies = new Properties();
         localTerminologies.load(new FileInputStream(userFile));
      } catch (NullPointerException e) {
         out.println(e.toString());
         e.printStackTrace();
      } catch (IOException e) {
         out.println(e.toString());
         e.printStackTrace();
      }
      try {
         // user configuration
         File redirectFile = new File(REDIRECTIONS);
         if (!redirectFile.exists()) {
            File dir = new File(System.getProperty("user.home")
                  + System.getProperty("file.separator") + "odml");
            if (!dir.exists())
               dir.mkdir();
            redirectFile.createNewFile();
         }
         redirections = new Properties();
         redirections.load(new FileInputStream(redirectFile));
      } catch (NullPointerException e) {
         out.println(e.toString());
         e.printStackTrace();
      } catch (IOException e) {
         out.println(e.toString());
         e.printStackTrace();
      }
   }


   /**
    * This method should be used instead of calling the constructor of this class.
    * 
    * @return The only instance of this {@link TerminologyManager}
    */
   public static TerminologyManager instance() {
      if (instance == null)
         instance = new TerminologyManager();
      return instance;
   }


   /**
    * See loadTerminology(url,string) for information.
    *
    * @param url {@link String} the url of the repository as String
    * @param type {@link String} the desired section type
    * @return {@link Section}
    */
   public Section loadTerminology(String url, String type) {
      Section s = null;
      try {
         URL u = new URL(url);
         s = loadTerminology(u, type);
      } catch (Exception e) {
         System.out.println("invalid url");
      }
      return s;
   }


   /**
    * Load a terminology from the given url. If the type is specified, loadTerminology returns the first type-matching
    * section found in the terminology. If the terminology does not contain that type of section null is returned. If
    * no type is specified, loadTerminology returns the root section of the terminology.
    * 
    * @param repository {@link URL} the url of the repository
    * @param sectionType {@link String} the desired section type
    * @return {@link Section}
    */
   public Section loadTerminology(URL repository, String sectionType) {
      // check Hash
      Section s = null;
      String key = repository.getProtocol() + "://" + repository.getAuthority()
            + repository.getPath() + "#"
            + sectionType;
      if (urlSectionHash.containsKey(key)) {
         s = urlSectionHash.get(key);
      } else {// if not successful, get repository
         Section rep = getRepository(repository);
         if (rep != null) {
            // try to find directly
            s = rep.findSectionByType(sectionType);
            if (s == null && sectionType.contains("/")) {
               // try with super-type first
               s = rep.findSectionByType(sectionType.substring(0, sectionType.indexOf("/")));
               if (s != null) {
                  s.loadInclude();
                  s = s.findSectionByType(sectionType);
               }
            }
         }
      }
      // finally check for section
      if (s != null) {
         s.loadInclude();
         if (!urlSectionHash.containsKey(key)) {
            urlSectionHash.put(key, s);
         }
      }
      return s;
   }


   /**
    * Returns the repository provided with the url in form of a Section. 
    * @param repository {@link URL}
    * @return {@link Section}
    */
   private Section getRepository(URL repository) {
      Section rep = null;
      String key = repository.getProtocol() + "://" + repository.getAuthority()
            + repository.getPath();
      if (urlSectionHash.containsKey(key)) {
         rep = urlSectionHash.get(key);
      } else {
         try {
            Reader r = new Reader();
            r.load(repository, Reader.NO_CONVERSION, false);
            rep = r.getRootSection();
            urlSectionHash.put(key, rep);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return rep;
   }


   /**
    * Clears the cache of terminologies.
    */
   public void clearCache() {
      this.urlSectionHash.clear();
   }


   /**
    * Add a redirection.
    * 
    * @param original
    *            the URL to for which a redirection should be performed
    * @param redirection
    *            URL the redirection
    * @deprecated not fully implemented
    */
   @Deprecated
   public void addRedirection(URL original, URL redirection) {// TODO implement
      redirections.put(original.toString(), redirection.toString());
      store();
   }


   /**
    * Store all changes made during runtime. All changes were applied to the user configuration
    */
   public void store() {
      File userFile = new File(TERMINOLOGIES_FILE);
      File redirectFile = new File(REDIRECTIONS);
      try {
         localTerminologies.store(new FileOutputStream(userFile), "");
         redirections.store(new FileOutputStream(redirectFile), "");
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }


   /**
    * Try to store the user configuration.
    */
   @Override
   public void finalize() {
      File userFile = new File(TERMINOLOGIES_FILE);
      if (userFile.exists() && userFile.canWrite()) {
         try {
            localTerminologies.store(new FileOutputStream(userFile), COMMENT);
         } catch (IOException e) {
            System.out.println(e.getMessage());
         }
      }
   }
}
