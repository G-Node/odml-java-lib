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
 * <http://www.gnu.org/licenses/>.
 */
import static java.lang.System.out;
import java.io.*;
import java.net.URL;
import java.util.*;
import odml.core.*;
import odml.core.Reader;
import org.slf4j.LoggerFactory;

public class TerminologyManager {

   public static org.slf4j.Logger         logger             = LoggerFactory
                                                                   .getLogger(TerminologyManager.class);
   private static TerminologyManager      instance           = null;                                    // The one and only
   // instance of this
   // manager
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
   // private HashMap<String,URL> typeUrlHash = new HashMap<String, URL>();
   private final HashMap<String, Section> urlSectionHash     = new HashMap<String, Section>();
   /** Properties hash for user settings */
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
    */
   public Section loadTerminology(String url, String type) {
      Section s = null;
      try {
         URL u = new URL(url);
         s = loadTerminology(u, type);
      } catch (Exception e) {
         logger.error("invalid url");
      }
      return s;
   }


   /**
    * Load a terminology from the given url. If the type is specified, loadTerminology returns the first type-matching
    * section found in the terminology. If the terminology does not contain that type of section null is returned. If
    * no type is specified, loadTerminology returns the root section of the terminology.
    * 
    * @param URL {@link URL}
    * @return {@link Section}
    */
   public Section loadTerminology(URL repository, String sectionType) {
      // check Hash
      Section s = null;
      String key = repository.getProtocol() + "://" + repository.getAuthority()
            + repository.getPath() + "#"
            + sectionType;
      System.out.println("TerminologyManager.loadTerminology: looking for type " + sectionType);
      if (urlSectionHash.containsKey(key)) {
         s = urlSectionHash.get(key);
         System.out.println("TerminologyManager.loadTerminology: got from hash: " + key);
      } else {// if not successful, get repository
         Section rep = getRepository(repository);
         if (rep != null) {
            // System.out.println("TerminologyManager.loadTerminology: got repository");
            // try to find directly
            s = rep.findSectionByType(sectionType);
            if (s == null && sectionType.contains("/")) {
               // try with super-type first
               s = rep.findSectionByType(sectionType.substring(0, sectionType.indexOf("/")));
               if (s != null) {
                  s.loadInclude();
                  s = s.findSectionByType(sectionType);
               }
               // else
               // System.out.println("TerminologyManager.loadTerminology: did not even find supertype");
            }
            // else
            // System.out.println("TerminologyManager.loadTerminology: not found and no supertype");
         }
         // else
         // System.out.println("TerminologyManager.loadTerminology: could not find repository");
      }
      // finally check for section
      if (s != null) {
         s.loadInclude();
         if (!urlSectionHash.containsKey(key)) {
            urlSectionHash.put(key, s);
            System.out.println("TerminologyManager.loadTerminology: put to hash: " + key);
         }
      }
      return s;
   }


   private Section getRepository(URL repository) {
      Section rep = null;
      String key = repository.getProtocol() + "://" + repository.getAuthority()
            + repository.getPath();
      if (urlSectionHash.containsKey(key)) {
         rep = urlSectionHash.get(key);
         System.out.println("TerminologyManager.getRepository: got from hash: " + key);
      } else {
         try {
            Reader r = new Reader();
            r.load(repository, Reader.NO_CONVERSION, false);
            rep = r.getRootSection();
            // System.out.println("TerminologyManager.getRepository: put to hash: " + key);
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


   // public void addLocalTerminology(){
   // localTerminologies.put(repUrl.toString(), children[i].getAbsolutePath());
   // }
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


   // public void addLocalRepository(String repository){
   // File file = new File(repository);
   // if(!file.exists() || file.isDirectory() || isOdml(file)){
   // return;
   // }
   // try {
   // Reader r = new Reader(repository);
   // Section root = r.getRootSection();
   // scanSection(root);
   // store();
   // } catch (Exception e) {
   // e.printStackTrace();
   // }
   //	

   // /**
   // *
   // * @param s
   // */
   // private void scanSection(Section s){
   // Vector<Section> sections = s.getSections();
   // for(int i=0;i < sections.size(); i++){
   //			
   // }
   // }
   // /**
   // * Function recursively scans the given path and its sub-directories for
   // odml files.
   // * Found files are added to the 'localTerminologies.properties' file
   // located in a folder
   // * called 'odml' in your home directory.
   // * @param path {@link String} the path to the locally stored
   // terminologies.
   // */
   // public void addLocalTerminologies(String path){
   // File file = new File(path);
   // if(!file.exists() && !file.isDirectory()){
   // return;
   // }
   // scanFolder(file);
   // store();
   // }
   // /**
   // * Scans a folder and cycles recursively through all sub-folders looking
   // for odml files;
   // * @param folder {@link File} the folder to scan
   // */
   // private void scanFolder(File folder){
   // File[] children = folder.listFiles();
   // for(int i=0; i<children.length;i++){
   // if(children[i].isDirectory() && !children[i].isHidden()){
   // scanFolder(children[i]);
   // }
   // else if(children[i].isFile() && children[i].getName().endsWith(".xml")){
   // System.out.print("testing candidate: "+ children[i].getName()+"...\t");
   // if (isOdml(children[i])){
   // System.out.println(" success!");
   // //what to do here? scan for sections? no!, try to find a repository ,yes
   // otherwise use the path
   // URL repUrl = getRepository(children[i]);
   // if(repUrl != null)
   // localTerminologies.put(repUrl.toString(), children[i].getAbsolutePath());
   // }
   // else{
   // System.out.println("...is no odml!");
   // }
   // }
   // }
   // }
   // /**
   // * Returns whether or not a file is an odml file.
   // * @param file {@link File} the file to test.
   // * @return {@link Boolean} true if the file represents an odml file, false
   // otherwise.
   // */
   // private boolean isOdml(File file){
   // boolean result = false;
   // InputStream stream = null;
   // try{
   // stream = file.toURI().toURL().openStream();
   // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   // //Using factory get an instance of document builder
   // DocumentBuilder dbuilder = dbf.newDocumentBuilder();
   // //parse using builder to get DOM representation of the XML file
   // Document dom = dbuilder.parse(stream);
   // if(dom == null){
   // result = false;
   // }
   // else{
   // NodeList l = dom.getElementsByTagName("odML");
   // if(l.getLength()==0){
   // result = false;
   // }
   // else{
   // result = true;
   // }
   // }
   // }catch (Exception e) {
   // e.printStackTrace();
   // result = false;
   // }
   // return result;
   // }
   // /**
   // * Retrieves the repository url stored in the root section if present.
   // Otherwise an URL representation
   // * of the current absolute path on the file system is returned.
   // * @param file {@link File} the odml - file.
   // * @return {@link URL} the repository url or null, if not possible.
   // */
   // private URL getRepository(File file) {
   // URL result = null;
   // InputStream stream = null;
   // try{
   // stream = file.toURI().toURL().openStream();
   // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   // //Using factory get an instance of document builder
   // DocumentBuilder dbuilder = dbf.newDocumentBuilder();
   // //parse using builder to get DOM representation of the XML file
   // Document dom = dbuilder.parse(stream);
   // if(dom == null){
   // result = null;
   // }
   // else{
   // Element rootElement = dom.getDocumentElement();
   // NodeList l = rootElement.getElementsByTagName("repository");
   // for(int i=0;i<l.getLength();i++){
   // if(l.item(i).getParentNode().isEqualNode(rootElement)){
   // result = new URL(l.item(0).getTextContent());
   // System.out.print(result);
   // break;
   // }
   // }
   // }
   // if(result == null){
   // System.out.print("no repository found");
   // result = file.toURI().toURL();
   // }
   // }catch (Exception e) {
   // e.printStackTrace();
   // result = null;
   // }
   // return result;
   // }

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
         out.println(e.toString());
         e.printStackTrace();
      } catch (IOException e) {
         out.println(e.toString());
         e.printStackTrace();
      }
   }


   /**
    * Try to store the user configuration.
    */
   @Override
   public void finalize() {
      out.println("finalize");
      File userFile = new File(TERMINOLOGIES_FILE);
      if (userFile.exists() && userFile.canWrite()) {
         try {
            localTerminologies.store(new FileOutputStream(userFile), COMMENT);
         } catch (IOException e) {
         }
      }
   }
}
