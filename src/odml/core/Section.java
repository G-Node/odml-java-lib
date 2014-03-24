package odml.core;

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
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import odml.util.TerminologyManager;
import odml.util.SectionPath;
import org.slf4j.*;

/**
 * The {@link Section} class defines the odML section, one of the two core elements that can contain odMLProperties - if
 * it is not the root of the odMLTree - as well as section to built a tree structure. A Section is defined by its name
 * and may carry a description. Furthermore a section may contain one terminology which contains property definitions
 * and which can be used to validate the properties. Properties defined in the terminology will be checked for
 * consistency and missing fields like type or unit will be added from the terminology.
 * 
 * To create a valid section a name must be provided, everything else is extra.
 * 
 * @since 08.2009
 * 
 * @author Jan Grewe, Christine Seitz
 */
public class Section extends Object implements Serializable, TreeNode {

   static Logger             logger                     = LoggerFactory.getLogger(Section.class);
   private static final long serialVersionUID           = 145L;
   public static final int   MERGE_THIS_OVERRIDES_OTHER = 0, MERGE_OTHER_OVERRIDES_THIS = 1,
   MERGE_COMBINE = 2;
   private String            type                       = null, definition = null, name = null,
   reference = null;
   private Vector<Property>  properties         = new Vector<Property>();
   private URL               repositoryURL              = null, fileUrl = null;
   private String            link                       = null;
   private String            include                    = null, author = null, version = null;
   private Date              date                       = null;
   private Section           parent, terminology = null;
   private URL               mapping                    = null;
   protected int             level;
   private boolean           isTerminology              = false;
   protected Vector<Section> subsections = new Vector<Section>();


   /**
    * The default constructor. Creating the root for the odmlTree, i.e. no name is necessary and no parent must be
    * given.
    */
   public Section() {
      subsections = new Vector<Section>();
      this.level = 0;
      // rootSec = this;
   }


   /**
    * Constructor for creating a section of a certain type. The section name equals the type.
    * 
    * @param type
    *            {@link String}
    * @throws Exception
    */
   public Section(String type) throws Exception {
      this(null, type);
   }


   /**
    * Create a {@link Section} of a certain type and with a specified name.
    * 
    * @param name
    * @param type
    * @throws Exception
    */
   public Section(String name, String type) throws Exception {
      this(name, type, null);
   }


   /**
    * 
    * @param name
    * @param type
    * @param reference
    * @throws Exception
    */
   public Section(String name, String type, String reference) throws Exception {
      this(null, name, type, reference, null);
   }


   /**
    * Constructor that creates a new section with the specified name and the specified parent. Parent and name may be
    * null.
    * 
    * @param parent
    *            {@link Section}: the parent of this section.
    * @param name
    *            - {@link String}:
    * @param type
    *            - {@link String}:
    */
   public Section(Section parent, String name, String type) throws Exception {
      this(parent, name, type, null, null);
   }


   /**
    * Constructor that creates a new section of a certain type and with the specified name and definition.
    * 
    * @param parent
    *            {@link Section}: the parent of this section.
    * @param name
    *            - {@link String}:
    * @param type
    *            - {@link String}:
    * @param definition
    *            - {@link String}: the definition of this section.
    */
   public Section(Section parent, String name, String type, String reference, String definition)
   throws Exception {
      this(parent, name, type, reference, definition, null);
   }


   /**
    * 
    * @param parent
    *            {@link Section}: the parent of this section.
    * @param name
    *            {@link String}: the section name.
    * @param type
    *            {@link String}: the section type.
    * @param definition
    *            {@link String}: a description, can be null.
    * @param baseURL
    *            {@link URL}: the URL of the underlying terminology.
    * @throws Exception
    */
   public Section(Section parent, String name, String type, String reference, String definition,
                  URL baseURL)
   throws Exception {
      this(parent, name, type, reference, definition, baseURL, null);
   }


   /**
    * Constructor that creates a new section with all the possible information (name, description, terminology and
    * mappingURL. Also parentURLs (logical parent) and partenSections are included)
    * 
    * @param parent
    * @param type
    *            {@link String}: the name of the Section, must not be null or empty
    * @param definition
    * @param repository
    * @param mappingURL
    * @throws Exception
    */
   public Section(Section parent, String name, String type, String reference, String definition,
                  URL repository,
                  URL mappingURL) throws Exception {
      if (type == null) {
         logger.error("odml.core.Section.initialize causes exception");
         throw new Exception("Type must not be null!");
      }
      if (type.isEmpty()) {
         logger.error("odml.core.Section.initialize causes exception");
         throw new Exception("Type must not be empty.");
      }
      if (name == null || name.isEmpty()) {
         name = type;
      }
      setName(checkNameStyle(name));
      setType(checkTypeStyle(type));
      setReference(reference);
      setDefinition(definition);
      setRepository(repository);
      setMapping(mappingURL);
      this.subsections = new Vector<Section>();
      this.properties = new Vector<Property>();

      if (parent != null) {
         //this.setParent(parent);
         parent.add(this);
      } else {
         this.level = 0;
      }
   }


   /**
    * Checks whether the given section type is in lower case, i.e. having one word beginning with an uppercase letter.
    * If not generating CamelCase by deleting white-spaces and putting letter of formerly new word to uppercase. If
    * String is beginning with a digit adding S_ for indicating a Section-type
    * 
    * @param type
    *            {@link String}: the type that shall be checked / converted to CamelCase
    * @return {@link String}: returning the checked / converted type-String in CamelCase
    */
   public static String checkTypeStyle(String type) {
      type = type.trim();
      while (type.contains(" ")) {
         type = type.replace(" ", "_").toLowerCase();
         logger
         .info("Invalid section type: types must not contain blanks, all blanks were replaced!");
      }
      String nameRegex = "^[a-zA-Z]{1}.*";
      if (!type.matches(nameRegex)) {
         type = "s_" + type;
         logger.info("Invalid section type: types must start alphabethical! 's_' added as " +
         "leading character ");
      }
      return type;
   }


   /**
    * Checks the name style of a section. Names must not contain '/'. Is replaced by '-'
    * 
    * @param name
    *            {@link String} the name.
    * @return String the converted (if needed) name.
    */
   public static String checkNameStyle(String name) {
      name = name.trim();
      while (name.contains("/")) {
         name = name.replace("/", "-");
         logger.warn("Invalid section name:\treplacing slashes '/' by '-'");
      }
      return name;
   }


   /**
    * checking if the ordinarySection is consistent to the given terminology section in aspects of definition,
    * dependencyURLs, mappingURL, terminologyURL, synonyms If not taking the info of the ordinary section and logging a
    * warning
    * 
    */
   private void validateSection() {
      if (this.terminology == null)
         return;
      if (this.terminology.getDefinition() != null && (!this.terminology.getDefinition().isEmpty())) {
         if (this.getDefinition() == null || (this.getDefinition().isEmpty())) {
            this.setDefinition(this.terminology.getDefinition());
            logger.info("Section definintion updated with terminology information!");
         } else if (!this.terminology.getDefinition().equalsIgnoreCase(this.getDefinition())) {
            logger.warn("Section definition deviates from the definition in the terminology! "
                  + "No changes applied. Please double check to avoid conflicts!");
         }
      }
      if (this.terminology.getMapping() != null
            && (!this.terminology.getMapping().toString().isEmpty())) {
         if (this.getMapping() == null || (this.getMapping().toString().isEmpty())) {
            this.setMapping(this.terminology.getMapping());
            logger.info("mappingURL set to one used in terminology");
         } else if (!this.terminology.getMapping().toString().equalsIgnoreCase(
               this.getMapping().toString())) {
            logger.warn("Section mapping different from the one specified in terminology! "
                  + "No changes applied. Please double check to avoid conflicts!");
         }
      }
      if (this.terminology.getRepository() != null
            && (!this.terminology.getRepository().toString().isEmpty())) {
         if (this.getRepository() == null
               || (this.getRepository().toString().isEmpty())) {
            this.setRepository(this.terminology.getRepository());
            logger.info("Section repository information updated with terminology information!");
         }
      }
   }


   // *****************************************************************
   // ************** formerly RootSection basics ***********
   // *****************************************************************
   /**
    * Adds a new subsection to this section.
    * 
    * @param section
    *            - {@link Section}: the new section.
    * @return - int: returns the index of the added section if successful (i.e. the number of subsections as added one
    *         is last), -1 otherwise.
    */
   public int add(Section section) {
      int index = -1;
      if (section != null) {
         if (this.containsSection(section.name, section.type)) {
            logger
            .warn("There already exists a section with that name! Will append an index to the name!");
            section.setName(section.getName() + this.getSectionsByType(section.getType()).size());
         }
         section.setParent(this);
         if (this.terminology != null) {
            validateSection();
         }
         subsections.add(section);
         index = subsections.size() - 1;
      }
      return index;
   }


   /**
    * Returns the number of subsections.
    * 
    * @return - int: the number of subsections. I.e. the size of the subsections vector.
    */
   public int sectionCount() {
      return subsections.size();
   }


   /**
    * Returns a list of all the types of first level subsections.
    * 
    * @return - {@link String}[]: an array containing the types of all fist level subsections.
    */
   public String[] subsectionsNames() {
      String[] types = new String[subsections.size()];
      for (int i = 0; i < subsections.size(); i++) {
         types[i] = subsections.get(i).getName();
      }
      return types;
   }


   //   /**
   //    * Returns a section specified by it's path from the root (beginning with '/') or from the current section
   //    * (beginning directly with the subsection-name)
   //    * 
   //    * @param pathOfSections
   //    *            {@link String}: path to the wanted section
   //    * @return {@Section} - returns the section specified by the given path
   //    */
   //   private Section getSectionViaPath(String pathOfSections) {
   //      String path = sanitizePath(pathOfSections,SECTION_PATH);
   //      Section sectionSpecifiedByPath = getActualSectionSpecifiedByPath(path, false);
   //      return sectionSpecifiedByPath;
   //   }


   /**
    * Returns a subsection as defined by the index.
    * 
    * @param index
    *            - int: the index of the requested subsection (i.e. position subsection-array)
    * @return the section of that index or null if index not valid.
    */
   public Section getSection(int index) {
      if (index < subsections.size()) {
         return subsections.get(index);
      } else {
         logger.error("Section: Index exceeds number of subsections.");
         return null;
      }
   }


   /**
    * Returns the first section with its name matching the argument. Matching is case-insensitive.
    * 
    * @param name
    *            - {@link String}: the name of the target section. May be a path.
    * @return the first section matching with its name
    */
   public  Section getSection(String name) {
      if (name == null || name.isEmpty())
         return null;
      if(isPath(name)){
         SectionPath sp = new SectionPath(name);
         if( !sp.isValid()){
            logger.error("Section.getSection: provided path is invalid!");
            return null;  
         }
         if(sp.isAbsolute()){
            return this.getRootSection().getSection(name.substring(1));
         }
         else{
            Section s = this.getSection(sp.nextSection());
            if(s != null && sp.hasRest())
               return s.getSection(sp.getRestPath());
            return s;
         }  
      }
      else{
         Iterator<Section> iter = subsections.iterator();
         while(iter.hasNext()){
            Section s = iter.next();
            if(s.getName().equalsIgnoreCase(name)){
               return s;
            }
         }
      }
      logger.error("Section.getSection(): could not locate section: " + name + " in the tree!");
      return null;
   }


   /**
    * Returns all first level subsections.
    * 
    * @return Vector<Section>: the subsections or null if no subsections.
    */
   public Vector<Section> getSections() {
      if (subsections.size() == 0) {
         return null;
      }
      return subsections;
   }


   /**
    * Return a child section that matches the requested type. Method does not crawl through the rest of the tree. Use
    * FindSectionByType, instead.
    * 
    * @param type
    *            {@link String} the type of the desired section.
    * @return {@link Section} the found subsection or null;
    */
   public Section getSectionByType(String type) {
      Section found = null;
      Vector<Section> temp = getSectionsByType(type);
      if (temp.size() > 0){
         found = getSectionsByType(type).get(0);
         if (temp.size() > 1) {
            logger.warn("Section.getSectionByType: more than one subsection " +
            "of this type exists > returning first match!");
         }
      }
      return found;
   }


   /**
    * Returns a Vector of subsections of the requested type. Matching is case-insensitive. <b>Note:</b> function
    * returns those matches in which the search type is super-type (e.g. stimulus/white_noise).
    * 
    * @param type
    *            - {@link String}: the type of subsections.
    * @return Vector<Section> the matching sections or an empty {@link Vector}.
    */
   public Vector<Section> getSectionsByType(String type) {
      Vector<Section> temp = new Vector<Section>();
      for (int i = 0; i < subsections.size(); i++) {
         String subsectionType = subsections.get(i).getType();
         if (subsectionType.equalsIgnoreCase(type) || (subsectionType.contains("/") && 
               subsectionType.substring(0, subsectionType.indexOf("/")).equalsIgnoreCase(
                     type))) {
            temp.add(subsections.get(i));
         }
      }
      return temp;
   }


   /**
    * findSection looks for a child section with the specified name. In contrast to getSection, findSection recursively
    * crawls down all subsection. Returns the first occurrence! If name is a path this method is equivalent to
    * getSection(name).
    * 
    * @param name
    *            String the section name.
    * @return The found section or null.
    */
   public Section findSection(String name) {
      Section found = getSection(name);
      if (found == null) {
         for (int i = 0; i < sectionCount(); i++) {
            Section temp = subsections.get(i).findSection(name);
            if (temp != null) {
               return temp;
            }
         }
         return null;
      } else {
         return found;
      }
   }

   /**
    * Finds the first child {@link Section} matching the requested type.
    * 
    * @param type
    *            {@link String} the type of the section.
    * @return {@link Section} the section or null;
    */
   public Section findSectionByType(String type) {
      Section found = getSectionByType(type);
      if (found == null) {
         for (int i = 0; i < sectionCount(); i++) {
            Section temp = subsections.get(i).findSectionByType(type);
            if (temp != null) {
               return temp;
            }
         }
         return null;
      } else {
         return found;
      }
   }


   /**
    * Finds all child sections of the specified type. Recursively cycles down the tree.
    * 
    * @param type
    *            {@link String} the type of sections.
    * @return {@link Vector} of {@link Section}s, may be empty
    */
   public Vector<Section> findSectionsByType(String type) {
      Vector<Section> temp = getSectionsByType(type);
      for (int i = 0; i < sectionCount(); i++) {
         temp.addAll(subsections.get(i).findSectionsByType(type));
      }
      return temp;
   }


   /**
    * Returns that Section of the specified type that has the strongest relation to this section. Section relations
    * rate from children over siblings to parents, their siblings, grandparents, their siblings and so on...
    * 
    * @param type
    *            {@link String} the type of the target section.
    * @return The first matching {@link Section};
    */
   public Section getRelatedSection(String type) {
      Vector<Section> temp = this.findSectionsByType(type);
      if (temp.size() == 0) {
         Section parent = this.getParent();
         while (parent != null && parent.level >= 0) {
            temp = parent.getSectionsByType(type);
            if (temp.size() != 0) {
               return temp.get(0);
            } else {
               parent = parent.getParent();
            }
         }
         return null;
      } else {
         return temp.get(0);
      }
   }


   /**
    * Returns all Sections that have the specified type and are have the strongest relation to this section. Relation
    * strengths decrease from children over siblings to parents, their siblings, grandparents, their siblings and so
    * on... Sections with equal relation strength are returned. That means that they are siblings.
    * 
    * @param type
    *            {@link String}: The section type like 'stimulus'.
    * @return {@link Vector} of {@link Section}s which may be empty.
    */
   public Vector<Section> getRelatedSections(String type) {
      Vector<Section> sections = findSectionsByType(type);
      if (sections.size() == 0) {
         Section parent = this.getParent();
         while (parent != null && parent.level >= 0) {
            sections = parent.getSectionsByType(type);
            if (sections.size() != 0) {
               break;
            }
            parent = parent.getParent();
         }
      }
      return sections;
   }


   /**
    * Ask if this section is the root section, i.e. its level must be 0.
    * 
    * @return - boolean: true if section is root, false otherwise.
    */
   public boolean isRoot() {
      return this.level == 0 && this.propertyCount() == 0;
   }


   /**
    * Returns the level of this section (i.e. the depth / position in the tree)
    * 
    * @return - int: the level of this section.
    */
   public int getLevel() {
      return level;
   }


   /**
    * Removes the subsection at the given index.
    * 
    * @param index
    *            {@link Integer} the index of the section.
    */
   public boolean removeSection(int index) {
      try {
         subsections.removeElementAt(index);
         return true;
      } catch (ArrayIndexOutOfBoundsException a) {
         logger.error("Invalid index! Index '" + index + "' exceeds the bounds of Section array.");
         return false;
      }
   }


   /**
    * Deletes a section from the tree.
    * 
    * @param section
    *            {@link Section} the section that should be deleted.
    * @return {@link Boolean} true if operation succeeded. False otherwise.
    */
   public boolean removeSection(Section section) {
      return subsections.remove(section);
   }


   /**
    * Removes the first subsection that matches the specified name. Name can also be a path. 
    * 
    * @param name
    *            - {@link String}: the name of the subsection accordingly the path (e.g. sec/subsec/toRemoveSec
    * @return boolean: true if removing successful, false otherwise
    */
   public boolean removeSection(String name) {
      if(isPath(name)){
         Section s = getSection(name);
         s.getParent().removeSection(this);
      }
      else{
         int index = indexOfSection(name);
         if(index > 0)
            this.subsections.remove(index);
         else{
            logger.error("Section.removeSection(): Cannot remove section ("+name+")!");
            return false;
         }
      }
      return true;    
   }


   /**
    * Updates the level of the according section and all it's subsections. Called when calling addSection(), as a
    * Section can become subsection of another one when merging two files, etc.
    */
   private void updateLevel() {
      if (this.getParent() == null) {
         this.level = 0;
      } else {
         this.level = this.getParent().getLevel() + 1;
      }
      if (this.subsections != null) {
         for (int i = 0; i < this.sectionCount(); i++) {
            this.getSection(i).updateLevel();
         }
      }
   }


   /**
    * Sets the type of the section.
    * 
    * @param type
    *            - {@link String}: the new type of this section.
    * @return - boolean: returns true if successful or false if not.
    */
   public boolean setType(String type) {
      if (type == null) {
         logger.error("Section.setType: type must not be null");
         return false;
      }
      if (type.isEmpty()) {
         logger.error("Section.setType: type must not be empty");
         return false;
      }
      this.type = type;
      return true;
   }


   /**
    * Set the name of the section.
    * 
    * @param name
    *            {@link String} the new name.
    * @return {@link Boolean} whether or not the operation succeeded.
    */
   public boolean setName(String name) {
      if (name == null) {
         logger.error("Section.setName: name must not be null");
         return false;
      }
      if (name.isEmpty()) {
         logger.error("Section.setName: name must not be empty");
         return false;
      }
      if (containsProperty("name")
            && !this.getProperty("name").getName().equalsIgnoreCase(this.name)) {
         logger
         .error("Section.setName: provided name is in conflict with the one provided by the 'name' property! No change done!");
         return false;
      }

      this.name = name;
      return true;
   }


   /**
    * Set the reference element of the {@link Section}. the reference can be used to point to an entity e.g. defined in
    * a database.
    * 
    * @param reference
    */
   public void setReference(String reference) {
      this.reference = reference;
   }


   /**
    * Get the reference element of the {@link Section}.
    * 
    * @return String - the reference or null if empty.
    */
   public String getReference() {
      return reference;
   }


   /**
    * Sets the description of this section.
    * 
    * @param definition
    *            - String: the new description of this section.
    */
   public void setDefinition(String definition) {
      this.definition = definition;
   }


   /**
    * Sets the parent for this section
    * 
    * @param parent
    *            {@link Section}: the parent that shall be set for the current section
    */
   private void setParent(Section parent) {
      this.parent = parent;
      this.updateLevel();
   }


   /**
    * Sets the terminology of the section.
    * 
    * @param url {@link URL}: the url of the new terminology.
    */
   public void setRepository(String url) {
      try {
         URL tempUrl = new URL(url);
         this.repositoryURL = tempUrl;
      } catch (Exception e) {
         this.repositoryURL = null;
         logger.error("An error occurred when setting the repository: ", e);
      }
   }


   public void setRepository(URL url) {
      this.repositoryURL = url;
   }


   /**
    * Returns the description of this section.
    * 
    * @return - String: the description stored with this section or null if none stored.
    */
   public String getDefinition() {
      if (definition != null && definition.isEmpty()) {
         return null;
      }
      return definition;
   }


   /**
    * Returns the type of this section.
    * 
    * @return - {@link String}
    */
   public String getType() {
      return this.type;
   }


   /**
    * Returns the section name.
    * 
    * @return {@link String}
    */
   public String getName() {
      return this.name;
   }


   /**
    * Returns the parent section of this section.
    * 
    * @return Section: the parent section.
    */
   public Section getParent() {
      return parent;
   }


   /**
    * Returns those {@link Section}s that are referring (are linked) to this section.
    * 
    * @return {@link Vector} of {@link Section} the referring sections. May be empty!
    */
   public Vector<Section> getLinkingSections() {
      Vector<Section> temp = new Vector<Section>();
      Section root = this.getRootSection();
      Vector<Section> candidates = root.findSectionsByType(this.type);
      for (int i = 0; i < candidates.size(); i++) {
         if (candidates.get(i).isLinked()) {
            if (candidates.get(i).getLinkedSection().equals(this)) {
               temp.add(candidates.get(i));
            }
         }
      }
      return temp;
   }


   /**
    * Returns whether this section is linked to another one.
    * 
    * @return {@link Boolean}: true if this section is linked to another section
    */
   public boolean isLinked() {
      return this.link != null && !this.link.isEmpty();
   }


   /**
    * Returns the linked section of the given one. Does not follow the linked sections links!
    * 
    * @return {@link Boolean}: the linked section
    */
   public Section getLinkedSection() {
      Section temp = null;
      temp = getSection(this.link);
      return temp;
   }


   //
   /**
    * Ask this section to follow its links and to resolve it. In case this section is linked to another one it will be
    * extended by the properties found in the linked section.
    * 
    * @return {@link Boolean} true if this section is linked to another section and has been merged otherwise false.
    */
   public boolean resolveLink() {
      if (this.link != null) {
         Section linkedSection = getSection(this.link);
         if (linkedSection != null) {
            linkedSection.resolveLink();
         } else {
            logger.error("Section.resolveLink: could not find referenced section!");
            return false;
         }
         this.merge(getSection(this.link), MERGE_THIS_OVERRIDES_OTHER);
         return true;
      } else {
         return false;
      }
   }


   /**
    * Cycles through all subsections and resolves all links. Uses resolveLink() recursively. Call it on
    */
   public void resolveAllLinks() {
      for (int i = 0; i < sectionCount(); i++) {
         getSection(i).resolveAllLinks();
      }
      this.resolveLink();
   }


   /**
    * Adds a property to this section and returns its index.
    * 
    * @param property
    *            - {@link Property}: the property you want to add.
    * @return {@link Integer}: the index of the added property in the properties vector or -1 if command failed.
    */
   public int add(Property property) {
      if (this.isRoot() && this.type == null) {
         logger
         .error("! property must not be added to the root section (level == 0 && type == null)!");
         return -1;
      }
      if (property == null) {
         return -1;
      }
      int index = indexOfProperty(property.getName());
      if (index > -1) {
         if (properties.get(index).equals(property)) {
            logger.error("! nothing added as identical property already existing"
                  + "\n\tproperty details: "
                  + property.toString());
            return index;
         } else {
            properties.get(index).addValue(property);
         }
      } else {
         properties.add(property);
         property.setParent(this);
      }
      // override section name when "name" property is added
      if (property.getName().equalsIgnoreCase("name")) {
         this.setName(property.getValue(0).toString());
         logger
         .info("Section.addProperty: New Property overrides the section name. Section name was replaced!");
      }
      return propertyCount() - 1;
   }


   /**
    * Adds a property to the section given by the the path and returns its index. If sections in path not existing yet,
    * creating them. path beginning with '/' meaning starting from the root.
    * 
    * @param path
    *            {@link String}: the path form the root, where the property shall be added; different levels separated
    *            by "/", path ending on section-name!!
    * @param property
    *            - {@link Property}: the property you want to add.
    * @return {@link Integer}: the index of the added property in the properties vector or -1 if command failed.
    */
   public int add(String path, Property property) {
      assert property != null : "Property must not be null!";
      Section parent = getSection(path);
      if (parent == null) {
         logger.error("!path somehow wrong, no parent for adding porperty found! (path '" + path
               + "')");
         return -1;
      }
      return parent.add(property);
   }


   /**
    * Ads a new {@link Property} to this section.
    * 
    * @param name
    *            {@link String} the name of the new Property.
    * @param value
    *            {@link Object} the new Property's value.
    * @return int the property index.
    */
   public int addProperty(String name, Object value) {
      int index = -1;
      try{
         if(isPath(name)){
            SectionPath sp = new SectionPath(name);
            if(sp.isValid() && sp.addressesProperty()){
               Property prop = new Property(sp.getPropertyPart(), value);
               Section s = getSection(name);
               if(prop != null && s != null){
                  index = s.add(prop);
               }
               else{
                  logger.error("An error occurred adding property with path specification: " +name);
               }
            }
            else{
               logger.error("Section.addProperty: " + "specified path is not valid!");
            }
         }
         else{
            Property prop = new Property(name, value);
            index = this.add(prop);
         }
      }
      catch (Exception e) {
         logger.error(e.getLocalizedMessage());
      }
      return index;
   }


   /**
    * Removes a property matching with its name from the properties that are stored in this section. Removes the first
    * occurrence with this name and does not check for duplicate entries!
    * 
    * @param name
    *            {@link String} : the name of the property.
    * @return {@link Boolean} true if successful, false if not.
    */
   public boolean removeProperty(String name) {
      boolean result = false;
      if(isPath(name)){
         SectionPath sp = new SectionPath(name);
         if(sp.isValid() && sp.addressesProperty()){
            Section s = getSection(name);
            if(s != null)
               result = s.removeProperty(sp.getPropertyPart());
         }
         else{
            logger.error("Specified path is invalid or does not address a property.");
         }
      }
      else{
         result = removeProperty(this.indexOfProperty(name));
      }
      return result;
   }


   /**
    * Removes the property defined by its index the properties that are stored in this section.
    * 
    * @param index
    *            {@link Integer} : the index of the property. With -1<index<getPropertyCount().
    * @return {@link Boolean} :true if operation succeeded and false otherwise.
    */
   public boolean removeProperty(int index) {
      if (properties.size() < index || index < 0) {
         return false;
      } else {
         properties.removeElementAt(index);
      }
      return true;
   }


   /**
    * Returns a list of the property names in this section. Method does not traverse through the subsections.
    * 
    * @return String[] an array of Strings.
    */
   public String[] getPropertyNames() {
      String[] names = new String[this.propertyCount()];
      for (int i = 0; i < this.propertyCount(); i++) {
         names[i] = this.properties.get(i).getName();
      }
      return names;
   }


   /**
    * Returns the number of properties stored in this section.
    * 
    * @return int: the number of stored properties.
    */
   public int propertyCount() {
      if (properties == null) {
         return 0;
      }
      return properties.size();
   }


   /**
    * Returns the property with this index.
    * 
    * @param index
    *            - int: the index in the properties vector.
    * @return {@link Property}: the property or null if index exceeds the propertyCount.
    */
   public Property getProperty(int index) {
      if (index < properties.size()) {
         return properties.get(index);
      } else {
         logger.error("Index exceeds number of properties.");
         return null;
      }
   }


   /**
    * Returns the first property stored in this section that matches the name. Name matching is case-insensitive. The
    * name can also be a path looking like /section/section/property. Leading "/" means beginning from root, leading
    * section-name meaning path beginning from current section. In case no direct match is found method also checks the
    * synonyms. The first match is returned. Identical for search via path, just changing position in tree.
    * 
    * @param name
    *            - {@link String}: the name of the searched property.
    * @return - {@link Property}: the first matching property or null if no match found.
    */
   public Property getProperty(String name) {
      return getProperty(name, true);
   }


   /**
    * Returns the date component of the first value's content of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @return {@link Date} the date component or null if property not found or conversion fails.
    */
   public Date getDate(String propertyName) {
      return getDate(propertyName, 0);
   }


   /**
    * Returns the date component of the i-th value's content of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @param i
    *            {@link Integer} the value index.
    * @return {@link Date} the date component or null if property not found or conversion fails.
    */
   public Date getDate(String propertyName, int i) {
      Property p = getProperty(propertyName);
      if (p == null) {
         return null;
      }
      return p.getDate(i);
   }


   /**
    * Returns the time component of the first value's content of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @return {@link Date} the time component or null if property not found or conversion fails.
    */
   public Date getTime(String propertyName) {
      return getTime(propertyName, 0);
   }


   /**
    * Returns the time component of the i-th value's content of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @param index
    *            {@link Integer} the value index.
    * @return {@link Date} the time component or null if property not found or conversion fails.
    */
   public Date getTime(String propertyName, int index) {
      Property p = getProperty(propertyName);
      if (p == null) {
         return null;
      }
      return p.getTime(index);
   }


   /**
    * Returns the first value's content of the defined property as text.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @return {@link String} the text content or null if property not found or conversion fails.
    */
   public String getText(String propertyName) {
      return getText(propertyName, 0);
   }


   /**
    * Returns the i-th value's text content of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @param index
    *            {@link Integer} the value index.
    * @return {@link String} the text content or null if property not found or conversion fails.
    */
   public String getText(String propertyName, int index) {
      Property p = getProperty(propertyName);
      if (p == null) {
         return null;
      }
      return p.getText(index);
   }


   /**
    * Returns the first value's content as Float of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @return {@link Float} the content as Float, Float.NaN if conversion fails or null if property not found.
    */
   public double getNumber(String propertyName) {
      return getNumber(propertyName, 0);
   }


   /**
    * Returns the i-th value's content as Float of the defined property.
    * 
    * @param propertyName
    *            {@link String} the propertyName or path.
    * @param index
    *            {@link Integer} the value index.
    * @return {@link Float} the content as Float, Float.NaN if conversion fails or null if property not found.
    */
   public double getNumber(String propertyName, int index) {
      Property p = getProperty(propertyName);
      if (p == null) {
         return Double.NaN;
      }
      return p.getNumber(index);
   }


   /**
    * Internal method to retrieve a property. If the property is not found in this section itself the a possible link
    * will be resolved and the property search starts again.
    * 
    * @param name
    *            {@link String} the name of the searched property.
    * @param resolveLink
    *            {@link Boolean} indicates whether or not a link should be resolved.
    * @return {@link Property} the found property or null.
    */
   private Property getProperty(String name, boolean resolveLink) {
      Property p = null;
      if(name == null || name.isEmpty()){
         return p;
      }
      if(isPath(name)){
         SectionPath sp = new SectionPath(name);
         if(sp.isValid() && sp.addressesProperty()){
            p = getSection(name).getProperty(sp.getPropertyPart());
         }
         else{
            logger.error("Section.getProperty: specified path is not valid or does not specify a property!");
            return p;
         }
      }
      else{
         Iterator<Property> iter = properties.iterator();
         while(iter.hasNext()){
            Property temp = iter.next();
            if(temp.getName().equalsIgnoreCase(name)){
               p = temp;
            }
         }
      }
      if(p == null){
         if (this.resolveLink())  // search again if the link was resolved
             this.getProperty(name, true);
      }
      return p;
   }


   /**
    * Returns all properties stored in this section.
    * 
    * @return - Vector<odMLProperty>: returns the properties Vector which may be empty.
    */
   public Vector<Property> getProperties() {
      return properties;
   }


   /**
    * indicates whether a terminology is set or not
    * 
    * @return {@link Boolean}: true if this.terminology != null, false otherwise
    */
   public boolean hasTerminology() {
      if (this.terminology == null)
         return false;
      else {
         return true;
      }
   }


   /**
    * Get the URL of the linked terminology. Recursively crawls up the tree and returns the first one found.
    * 
    * @return {@link URL}: the URL of the terminology may be null.
    */
   public URL getRepository() {
      URL url = null;
      if (this.repositoryURL == null && this.parent != null) {
         url = parent.getRepository();
      } else
         url = this.repositoryURL;
      return url;
   }


   /**
    * Merges this section with another section. Generally all properties in the other section will be copied. The way
    * the merging is done in case of conflict can be set by the mergeOption parameter which can assume the following
    * values:
    * <ol>
    * <li>MERGE_THIS_OVERRIDES_OTHER: the local information overrides the information passed in otherSection .</li>
    * <li>MERGE_OTHER_OVERRIDES_THIS: the global information passed with otherSection overrides local information.</li>
    * <li>MERGE_COMBINE: the local information will be combined with the passed section. properties with the same name
    * will be fused.</li>
    * </ol>
    * Merging works recursively. Only those subsections are merged that share the same name and type! Otherwise they
    * are simply added.
    * 
    * @param otherSection
    *            {@link Section} the other section which shall be merged with this section.
    * @param mergeOption
    *            {@link Integer} the way merging is done.
    * 
    */
   public void merge(Section otherSection, int mergeOption) {
      if (otherSection == null) {
         return;
      }
      if (mergeOption < 0 || mergeOption > MERGE_COMBINE) {
         logger.error("Section.merge error: invalid mergeOption!");
         return;
      }
      if (!this.type.equalsIgnoreCase(otherSection.getType())) {
         logger.error("Section.merge error: cannot merge sections of differnt types!");
         return;
      }
      if ((this.getRepository() != null && otherSection.getRepository() != null)
            && !this.getRepository().sameFile(otherSection.getRepository())) {
         logger
         .error("Section.merge error: cannot merge sections based on different terminologies!");
         return;
      }
      if ((this.getMapping() != null && otherSection.getMapping() != null)
            && !this.mapping.sameFile(otherSection.getMapping())) {
         logger.error("Section.merge error: cannot merge sections mapping to different sections!");
         return;
      }
      for (int i = 0; i < otherSection.propertyCount(); i++) {
         Property temp = null;
         try {
            temp = otherSection.getProperty(i).copy();
         } catch (Exception e) {
            logger.error("", e);
         }
         if (temp == null) {
            logger.error("Section.merge error: cloning Property failed.");
            continue;
         }
         int index = this.contains(temp);
         if (index != -1) {
            this.getProperty(index).merge(temp, mergeOption);
         } else {
            this.add(temp);
         }
      }
      for (int i = 0; i < otherSection.sectionCount(); i++) {
         Section temp = null;
         try {
            temp = otherSection.getSection(i).copy();
         } catch (Exception e) {
            logger.error("Copying 'otherSection' failed!", e);
         }
         if (temp == null) {
            logger.error("Section.merge error: cloning section failed.");
            continue;
         }
         int index = this.indexOfSection(temp);
         if (index == -1) {
            this.add(temp);
         } else {
            this.getSection(index).merge(temp, mergeOption);
         }
      }
      // terminologyURL
      if (this.getRepository() == null) {
         this.setRepository(otherSection.getRepository());
      } else {
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getRepository() != null) {
            this.setRepository(otherSection.getRepository());
         }
      }
      // Definition
      if (this.getDefinition() == null) {
         this.setDefinition(otherSection.getDefinition());
      } else {
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getDefinition() != null) {
            this.setDefinition(otherSection.getDefinition());
         } else if (mergeOption == MERGE_COMBINE && otherSection.getDefinition() != null) {
            this.setDefinition(this.definition + "\n" + otherSection.getDefinition());
         }
      }
      // mappingURL
      if (this.getMapping() == null) {
         this.setMapping(otherSection.getMapping());
      } else {
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getRepository() != null) {
            this.setRepository(otherSection.getRepository());
         }
      }
   }


   /**
    * Returns whether this {@link Section} contains a {@link Property} of the specified name or not. 
    * @param propertyName {@link String}: The property name;
    * @return boolean true, if such a {@link Property} exists, false otherwise.
    */
   public boolean containsProperty(String propertyName) {
      return indexOfProperty(propertyName) != -1;
   }


   /**
    * Returns whether this {@link Section} contains a subsection with the specified 
    * name and type.
    * 
    * @param sectionName {@link String}: The name searched for
    * @param sectionType {@link String}: The requested section type.
    * @return boolean: true if such a subsection exists, false otherwise
    */
   public boolean containsSection(String sectionName, String sectionType) {
      return indexOfSection(sectionName, sectionType) != -1;
   }


   /**
    * Returns whether this section contains a section of the specified name.
    * 
    * @param name String
    * @return boolean 
    */
   public boolean containsSection(String name){
      Iterator<Section> iter = subsections.iterator();
      while (iter.hasNext()){
         if(iter.next().getName().equalsIgnoreCase(name)){
            return true;
         }
      }
      return false;
   }


   /**
    * Returns the index of the first matching {@link Property}. Method compares only the name. 
    * the method is marked @deprecated use indexOfProperty instead.
    * 
    * @param property
    *            {@link Property} the property that should be found.
    * @return int: the index of the property if such a property already exists, -1 if not.
    */
   @Deprecated
   public int contains(Property property) {
      return indexOfProperty(property.getName());
   }


   /**
    * Returns the index of the first matching {@link Property}. Method compares the name. 
    * 
    * @param propertyName {@link String} the property name that should be found.
    * @return integer: the index of the property if such a property already exists, -1 if not.
    */
   public int indexOfProperty(String propertyName) {
      int index = -1;
      if (properties != null) {
         for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).getName().equalsIgnoreCase(propertyName)) {
               index = i;
               break;
            }
         }
      }
      return index;
   }


   /**
    * Returns the index of the first matching subsection that matches in its name 
    * and type. Returns -1 if no match found.
    * 
    * @param sectionName {@link String}: the requested section name.
    * @param sectionType {@link String}: the requested section type.
    * 
    * @return boolean: true if such a subsection exists, false otherwise.
    */
   public int indexOfSection(String sectionName, String sectionType) {
      int index = -1;
      if (subsections != null) {
         for (int i = 0; i < subsections.size(); i++) {
            if (subsections.get(i).getType().equalsIgnoreCase(sectionType)
                  && subsections.get(i).getName().equalsIgnoreCase(sectionName)) {
               index = i;
               break;
            }
         }
      }
      return index;
   }


   /**
    * Find the index of a subsection that first matches regarding its name.
    * @param sectionName
    * @return int the index
    */
   public int indexOfSection(String sectionName) {
      int index = -1;
      if (subsections != null) {
         for (int i = 0; i < subsections.size(); i++) {
            if (subsections.get(i).getName().equalsIgnoreCase(sectionName)) {
               index = i;
               break;
            }
         }
      }
      return index;
   }


   /**
    * Returns the index of the first matching subsection that matches in name 
    * and type to the one passed as argument. Returns -1 if no match found.
    * 
    * @param section {@link Section}: Name and type of a subsection must match to the 
    * name and type of this argument.
    * @return int: the index of the requested subsection. -1 if no matrch found.
    */
   public int indexOfSection(Section section) {
      return indexOfSection(section.getName(), section.getType());
   }


   /**
    * Checks whether or not this section already contains a subsection with the same name and type. Method does not
    * traverse the subsections! 
    * Method is marked @deprecated ! Use containsSection(String name, String type) instead.
    * 
    * @param section
    *            {@link Section}: the section that one looks for
    * @return {@link Integer}: the index of the section if exists, -1 if not.
    */
   @Deprecated
   public int contains(Section section) {
      return indexOfSection(section);
   }


   /**
    * Creates a copy of this section via serialization. This copy is an exact doublet
    * 
    * @return Section: returns an exact copy of the section
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public Section copy() throws IOException, ClassNotFoundException {
      File tempFile = File.createTempFile("section", ".ser");
      tempFile.deleteOnExit();
      ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(
            new FileOutputStream(tempFile)));
      objOut.writeObject(this);
      objOut.close();

      ObjectInputStream objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
            tempFile)));
      Section copy = (Section) objIn.readObject();
      objIn.close();
      copy.setParent(null);
      return copy;
   }


   /**
    * Sets the mapping to the given one.
    * 
    * @param mapping
    *            {@link URL}: the url to which this section maps. Can (should) contain references to indicate the type
    *            of section to include.
    */
   public void setMapping(URL mapping) {
      this.mapping = mapping;
   }


   /**
    * Tries to convert the given mappingURL String to a valid URL and sets the internal mappingURL to it.
    * 
    * @param mapping
    *            {@link String}: the url to which this property maps. Must not contain a refernce part.
    */
   public void setMapping(String mapping) {
      URL url = null;
      try {
         url = new URL(mapping);
      } catch (MalformedURLException m) {
         logger.error("getSectionMapping: ", m);
      }
      this.setMapping(url);
   }


   /**
    * Removes the mappingURL of this section.
    */
   public void removeMapping() {
      this.mapping = null;
   }


   /**
    * Returns the mappingURL of this section.
    * 
    * @return {@link URL} the mappingURL
    */
   public URL getMapping() {
      URL url = this.mapping;
      return url;
   }


   /**
    * Returns the repository url stored in this section or in one of its ancestors.
    * 
    * @return {@link URL} the repository url if set locally or by this section's parent or null if none found.
    */
   public URL findRepositoryURL() {
      if (this.repositoryURL != null) {
         return this.repositoryURL;
      } else {
         if (this.parent != null) {
            return this.getParent().findRepositoryURL();
         } else {
            return null;
         }
      }
   }


   /**
    * Validate the whole metadata tree against the terminologies.
    */
   public void validateTree() {
      logger.info("Starting validation ...");
      getRootSection().validateRecursively();
   }


   private void validateRecursively() {
      this.validate();
      for (int i = 0; i < sectionCount(); i++) {
         getSection(i).validate();
      }
   }


   /**
    * Validates this section against the related terminology. Tests all Properties but 
    * does not cycle through its subsections!
    * 
    */
   public void validate() {
      logger.info("Validating section " + this.getPath()
            + " against terminology located in repository "
            + findRepositoryURL().toString());
      findTerminology();
      if (terminology != null) {
         validateSection();
         for (int i = 0; i < propertyCount(); i++) {
            Property termProp = terminology.getProperty(getProperty(i).getName());
            if (termProp != null) {
               getProperty(i).validate(termProp);
            }
         }
      }
   }


   /**
    * Finds a terminology for this section to perform validity checks. 
    * If there is no terminology already stored, it tries to load it from 
    * the repository.
    * 
    * @return {@link Boolean} true, if a terminology is present, false otherwise.
    */
   private boolean findTerminology() {
      boolean success = true;
      if (terminology == null && findRepositoryURL() != null) {
         terminology = TerminologyManager.instance().loadTerminology(getRepository(), this.type);
      }
      if (this.terminology == null) {
         logger.warn("Validation of section: " + this.getPath()
               + " aborted! Could not locate a terminology equivalent!");
         success = false;
      }
      return success;
   }


   /**
    * This method scans the tree and optimizes the tree.
    * Empty Properties are removed. It optimizes linked sections in the sense 
    * that local sections are simplified that they only contain information that deviates from the 
    * global description. 
    * 
    * This method recursively cycles through the whole tree starting at the RootSection. 
    */
   public void optimizeTree() {
      Section root = getRootSection();
      for (int i = 0; i < root.sectionCount(); i++) {
         root.getSection(i).optimize();
      }
      logger.info("optimization done");
   }


   /**
    * Optimizes only this section. Method removes empty properties Optimization is only done if this section is linked and all referring sections have been
    * optimized before.
    */
   private void optimize() {
      Vector<Section> ref = this.getLinkingSections();
      for (int i = 0; i < ref.size(); i++) {
         ref.get(i).optimize();
      }
      if (this.isLinked()) {
         compareToLink();
      }
      removeEmptyProperties();
      removeEmptySections();
   }


   /**
    * Removes those Properties that have not value in them. If this section isTerminology 
    * nothing will be removed.
    * 
    */
   private void removeEmptyProperties() {
      if (this.isTerminology) {
         return;
      }
      for (int i = propertyCount() - 1; i >= 0; i--) {
         this.getProperty(i).removeEmptyValues();
         if (this.getProperty(i).isEmpty()) {
            this.removeProperty(i);
         }
      }
   }


   /**
    * Removes empty subsections from this {@link Section}. Does not do anything if this section is 
    * set to be a terminology.
    */
   private void removeEmptySections() {
      if (this.isTerminology) {
         return;
      }
      for (int i = sectionCount() - 1; i >= 0; i--) {
         getSection(i).removeEmptyProperties();
         if (getSection(i).isEmpty())
            this.removeSection(i);
      }
   }


   /**
    * Returns whether a {@link Section} is empty in the sense that it does not contain any properties or sections.
    * @return boolean true if there are no subsections and no properties, false otherwise.
    */
   public boolean isEmpty() {
      return this.propertyCount() == 0 && this.sectionCount() == 0;
   }


   /**
    * Compares this section to the linked section, removes redundancy and clears empty sections...
    */
   private void compareToLink() {
      Section linkSection = this.getLinkedSection();
      for (int i = propertyCount() - 1; i >= 0; i--) {
         Property mine = this.getProperty(i);
         Property his = linkSection.getProperty(mine.getName());
         if (his != null) {
            for (int j = mine.valueCount() - 1; j >= 0; j--) {
               for (int k = 0; k < his.valueCount(); k++) {
                  if (!mine.getWholeValue(j).isEmpty() && !his.getWholeValue(k).isEmpty()) {
                     if (mine.getValue(j).toString().equalsIgnoreCase(his.getValue(k).toString())) {
                        mine.removeValue(j);
                     }
                  }
               }
            }
         }
         if (mine.valueCount() == 0) {
            this.removeProperty(i);
         }
      }
   }


   /**
    * Returns a String representation of this section. I.e the fact that it is a section and it's name.
    */
   @Override
   public String toString() {
      if (this.isRoot()) {
         return "root section";
      }
      return (this.name + " - [" + this.type + "]");
   }


   /**
    * Returns an extended String representation of this section. I.e it's name, level, completePath, number and names
    * of subsections and number and names of appended properties
    */
   public String toStringExtended() {
      String info = (this.type + "-section named '" + this.name + "', id (" + this.reference
            + ") on level: " + level
            + "; complete path: " + this.getPath() + "\n\t- ");

      if (this.subsections != null && this.sectionCount() != 0 && this.getSections() != null) {
         info += (this.sectionCount() + " subsection(s) named: ");
         for (String name : this.subsectionsNames()) {
            info += name + ", ";
         }
         info = info.substring(0, info.lastIndexOf(", "));
      } else {
         info += ("no subsections");
      }
      info += ("\n\t- ");

      if (this.properties != null && this.propertyCount() != 0 && this.getProperties() != null) {
         info += (this.propertyCount() + " propertie(s) named: ");
         for (String name : this.getPropertyNames()) {
            info += name + ", ";
         }
         info = info.substring(0, info.lastIndexOf(", "));
      } else {
         info += ("no properties appended");
      }
      return info;
   }


   /**
    * Returns a detailed String representation of this section. I.e it's name, level, completePath + definition,
    * termURL, mappingURL + number and names of subsections and number and names of appended properties
    */
   public String toStringAllDetails() {
      String info = (this.type + "-section named '" + this.name + "', id (" + this.reference
            + ") on level: " + level
            + "; full path: " + this.getPath() + "\n\t- ");
      info += ("definition: \t" + this.definition + "\n");
      info += ("\n\t- repository: \t" + this.repositoryURL + "\n\t- mapping: \t" + this.mapping);
      info += ("\n\t- ");
      if (this.subsections != null && this.sectionCount() != 0 && this.getSections() != null) {
         info += (this.sectionCount() + " subsection(s) named: ");
         for (String name : this.subsectionsNames()) {
            info += name + ", ";
         }
         info = info.substring(0, info.lastIndexOf(", "));
      } else {
         info += ("no subsections");
      }
      info += ("\n\t- ");

      if (this.properties != null && this.propertyCount() != 0 && this.getProperties() != null) {
         info += (this.propertyCount() + " propertie(s) named: ");
         for (String name : this.getPropertyNames()) {
            info += name + ", ";
         }
         info = info.substring(0, info.lastIndexOf(", "));
      } else {
         info += ("no properties appended");
      }
      return info;
   }


   /**
    * This method returns the absolute path of this section beginning from the root. Paths are made up from section
    * names separated by "/". Leading '/' indicates an absolute path.
    * 
    * @return {@link String} returns the path as a String, different Sections / levels separated with '/'
    */
   public String getPath() {
      String completePath = "";
      if (this.getParent() != null) {
         completePath = parent.getPath() + "/" + this.getName();
      }
      return completePath;
   }


   private Vector<TreeNode> getTreeNodeSections() {
      Vector<TreeNode> tnSections = new Vector<TreeNode>();
      for (int i = 0; i < (this.subsections).size(); i++) {
         tnSections.add(this.subsections.elementAt(i));
      }
      return tnSections;
   }


   private Vector<TreeNode> getTreeNodeProperties() {
      Vector<TreeNode> tnProperties = new Vector<TreeNode>();
      for (int i = 0; i < this.propertyCount(); i++) {
         tnProperties.add(this.getProperty(i));
      }
      return tnProperties;
   }


   @Override
   public Enumeration<TreeNode> children() {
      Vector<TreeNode> foo = this.getTreeNodeSections();
      foo.addAll(this.getTreeNodeProperties());
      return foo.elements();
   }


   @Override
   public boolean getAllowsChildren() {
      return true;
   }


   @Override
   public TreeNode getChildAt(int arg0) {
      if (arg0 < 0 || arg0 > this.getChildCount()) {
         logger.error("index out of range! must be within 0 and " + this.getChildCount());
         return null;
      }
      if (arg0 < this.sectionCount()) {
         return getSection(arg0);
      } else {
         return getProperty(arg0 - this.sectionCount());
      }
   }


   @Override
   public int getChildCount() {
      int secAndPropCnt = this.sectionCount() + this.propertyCount();
      return secAndPropCnt;
   }


   @Override
   public int getIndex(TreeNode arg0) {
      if (arg0 instanceof Section) {
         Section soo = (Section) arg0;
         for (int i = 0; i < this.sectionCount(); i++) {
            if (this.getSection(i).equals(soo))
               return i;
         }
         return -1;
      } else if (arg0 instanceof Property) {
         Property poo = (Property) arg0;
         for (int j = 0; j < this.propertyCount(); j++) {
            if (this.getProperty(j).equals(poo))
               return (j + this.sectionCount());
         }
         return -1;
      } else {
         logger.error("!should not happen as TreeNode can only be Section or Property! "
               + "Here we have: "
               + arg0.getClass());
         return -1;
      }
   }


   @Override
   public boolean isLeaf() {
      return false;
   }

   /**
    * Displays the odml tree in a dialog window. This view has no further 
    * function but to give an impression of the stored information.
    */
   public void displayTree() {
      JDialog d = new JDialog();
      d.add(new JScrollPane(new JTree(new DefaultTreeModel(this))));
      d.setSize(300, 300);
      d.setVisible(true);
   }


   /**
    * Set a link. This indicates that this section is linked to another one and thus inherits its properties. Links can
    * only be established between sections of the same type and are defined by the absolute paths in the odml tree.
    * Paths are the '/' separated names of sections, e.g. /dataset 1/myStimulus/.
    * 
    * @param link
    *            {@link String} the link.
    */
   public void setLink(String link) {
      setLink(link, false);
   }


   /**
    * Set a link. A link indicates that this section is linked to another one and thus inherits its properties. Links
    * can only be established between sections of the same type and are defined by the absolute paths in the odml tree.
    * Paths are the '/' separated names of sections, e.g. /dataset 1/myStimulus/. Using this function a link can be set
    * that is untested.
    * 
    * @param link
    *            String the link as an absolute path (beginning at the root section and starting with / e.g. '/dataset
    *            1/myStimulus/')
    * @param ignore
    *            {@link Boolean} setting this true does not test the link for validity!
    */
   public void setLink(String link, boolean ignore) {
      if (link == null) {
         this.link = null;
         return;
      }
      if (ignore) {
         this.link = link;
      } else {

         if (!link.startsWith("/")) {
            logger
            .warn("Section.setLink: A link must be given as an abolute path in the tree, i.e. start with '/'.");
            link = "/" + link;
         }
         Section temp = this.getSection(link);
         if (temp == null) {
            logger
            .error("Section.setLink: The link is invalid. Referenced section does not exist. Link: "
                  + link);
            return;
         }
         if (!temp.getType().equalsIgnoreCase(this.type)) {
            logger.error("Section.setLink: The link (" + link
                  + ")is invalid! Section types (this: " + this.type
                  + "," + " link: " + temp.getType() + ")do not match!");
            return;
         }
         this.link = link;
      }
   }


   /**
    * Returns the link stored for this section.
    * 
    * @return {@link String} the stored link or null.
    */
   public String getLink() {
      return link;
   }


   /**
    * Sets the include information of this section. The include string can represent an url or a relative path. Setting
    * the include information does not automatically load the resource. To do so call loadInclude() method.
    * 
    * @param include
    *            A {@link String} representing a valid url an absolute or relative path of the resource to included.
    */
   public void setInclude(String include) {
      this.include = include;
   }


   /**
    * Returns the include information stored in this section.
    * 
    * @return {@link String} the inlcude information, or null if none stored.
    */
   public String getInclude() {
      return include;
   }


   /**
    * Sets the author of this document. Author elements are only valid in root sections. They are otherwise ignored.
    * 
    * @param author
    *            {@link String} the author of the document.
    */
   public void setDocumentAuthor(String author) {
      if (!this.isRoot()) {
         logger
         .warn("Author information is only allowed in Root sections. Otherwise it will be ignored.");
      }
      this.author = author;
   }


   /**
    * Returns the author of this document. Usually this information is only available in root sections.
    * 
    * @return {@link String} the Author of this document.
    */
   public String getDocumentAuthor() {
      return author;
   }


   /**
    * Set the Version of the document. Version elements are only valid in root sections. They are otherwise ignored.
    * 
    * @param version
    *            {@link String} the version of the document.
    */
   public void setDocumentVersion(String version) {
      if (!this.isRoot()) {
         logger
         .warn("Version information is only allowed in Root sections. Otherwise it will be ignored.");
      }
      this.version = version;
   }


   /**
    * Returns the version of this document, if set. Usually this information is only available in root sections.
    * 
    * @return {@link String} the Version of the document, if available, null otherwise.
    */
   public String getDocumentVersion() {
      return version;
   }


   /**
    * Set the date of the document. Usually this information is only allowed in root sections. It will be ignored if
    * stored in normal sections.
    * 
    * @param date
    *            {@link Date} the date.
    */
   public void setDocumentDate(Date date) {
      if (!this.isRoot()) {
         logger
         .warn("Version information is only allowed in Root sections. Otherwise it will be ignored.");
      }
      this.date = date;
   }


   /**
    * Returns the date this document was created. This information is only available in root sections.
    * 
    * @return {@link Date} the create date of the document, or null.
    */
   public Date getDocumentDate() {
      if (this.isRoot()) {
         return this.date;
      } else {
         return null;
      }
   }


   /**
    * Loads an included file into this section and merges them while the locally defined information (properties of
    * this section) overwrites the imported one.
    */
   //TODO TEST me!!!
   public void loadInclude() {
      if (this.include == null) {
         return;
      }
      Section includeSection = null;
      String includePath = getIncludeSectionPath();
      URL fileUrl = getIncludeFileURL();

      if(fileUrl != null){
         Section temp = loadIncludeFile(fileUrl);
         assert temp != null : "loadIncludeFile returns empty section! Something is wrong with the include element.";
         includeSection = locateIncludeSection(temp, includePath);
      }
      if (includeSection != null) {
         this.merge(includeSection, Section.MERGE_THIS_OVERRIDES_OTHER);
      }
      this.include = null;
   }

   private Section locateIncludeSection(Section temp, String includePath) {
      Section s = temp.getSection(includePath);
      if(s == null){
         Vector<Section> typeMatches = temp.findSectionsByType(this.getType());
         if(typeMatches.size() > 1){
            logger.error("Section.locateIncludeSection: Include statement is ambiguous!");
         }
         else{
            s = typeMatches.firstElement();
         }
      }
      return s;
   }


   private Section loadIncludeFile(URL fileUrl) {
      Section includeRoot = null;
      try {
         Reader r = new Reader();
         includeRoot = r.load(fileUrl, Reader.NO_CONVERSION, false);
      } catch (Exception e) {
         logger.error("Section.loadIncludeFile: Could not read file from the include location: "
               + fileUrl);
      }
      return includeRoot;
   }


   private String getIncludeSectionPath(){
      String sectionPath= "";
      if(this.getInclude() != null && this.getInclude().contains("#")){
         sectionPath = this.include.substring(this.getInclude().indexOf("#")+1);
      }
      return sectionPath;
   }

   private URL getIncludeFileURL(){
      URL url = null;
      if(this.getInclude() != null && this.getInclude().contains("#")){
         String urlPart = this.include.substring(0,this.getInclude().indexOf("#"));
         try{
            File thisFile = new File(this.getRootSection().getFileUrl().toURI());
            url = new File(new File(thisFile.getParent()), urlPart).toURI().toURL();
         }
         catch (Exception e) {
            logger.error("Section.getIncludeURL: Could not locate the file referenced with the include information!");
         }
      }
      return url;
   }

   /**
    * Loads includes from this section and cycles through all subsections. If you want to load all includes in the tree
    * call this function on the rootSection.
    */
   public void loadAllIncludes() {
      this.loadInclude();
      for (int i = 0; i < sectionCount(); i++) {
         this.getSection(i).loadAllIncludes();
      }
   }


   /**
    * Sets the fileURL of the file this section originated from. This information is used to resolve includes if they
    * are relative. Usually only present in the root section.
    * 
    * @param fileUrl
    *            {@link URL} the URL of the original file.
    */
   public void setFileUrl(URL fileUrl) {
      this.fileUrl = fileUrl;
   }


   /**
    * Returns the stored file url, i.e. the url of the file this section originated from. Usually only set while reading 
    * from file and only present in the root section. 
    * sections.
    * 
    * @return {@link URL} the URL, if present, null otherwise.
    */
   public URL getFileUrl() {
      return fileUrl;
   }


   /**
    * Returns the root of the odml-tree.
    * 
    * @return {@link Section} the root section.
    */
   public Section getRootSection() {
      Section root = null;
      if (this.getParent()== null) {
         root = this;
      } else {
         root = parent.getRootSection();
      }
      return root;
   }


   /**
    * Returns whether the tree starting at this section contains Includes (i.e. links to external resources). *
    * 
    * @return {@link Boolean} true if there are includes, false otherwise.
    */
   public boolean containsIncludes() {
      if (this.include != null && !this.include.isEmpty()) {
         return true;
      }
      for (int i = 0; i < sectionCount(); i++) {
         if (getSection(i).containsIncludes()) {
            return true;
         }
      }
      return false;
   }


   /**
    * Returns whether the tree starting at this section contains links.
    * 
    * @return {@link Boolean} true if there are links.
    */
   public boolean containsLinks() {
      if (this.link != null && !this.link.isEmpty()) {
         return true;
      }
      for (int i = 0; i < sectionCount(); i++) {
         if (getSection(i).containsLinks()) {
            return true;
         }
      }
      return false;
   }


   /**
    * Returns whether the tree starting at this section contains mapping information that might need to be resolved.
    * 
    * @return {@link Boolean} true if mapping information is present, false otherwise.
    */
   public boolean containsMappings() {
      if (this.mapping != null) {
         return true;
      }
      for (int i = 0; i < propertyCount(); i++) {
         if (getProperty(i).getMapping() != null) {
            return true;
         }
      }
      for (int i = 0; i < sectionCount(); i++) {
         if (getSection(i).containsMappings()) {
            return true;
         }
      }
      return false;
   }


   /**
    * Returns whether this section was set to be a terminology. The default is false.
    * 
    * @return {@link Boolean}
    */
   public boolean isTerminology() {
      return isTerminology;
   }


   /**
    * Defines that this section is a terminology.
    * 
    * @param isTerminology
    */
   public void setAsTerminology(boolean isTerminology) {
      this.isTerminology = isTerminology;
   }

   /**
    * Returns whether or not the string is a path 
    * @param name String 
    * @return boolean
    */
   private boolean isPath(String name){
      return name.contains("#") || (name.contains("/") && !(name.indexOf("/") == name.lastIndexOf("/") && name.endsWith("/")));
   }
}