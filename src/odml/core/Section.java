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
import org.slf4j.*;

/*
 * TODO (CES 30.07.2010): checking all get-/set-/add-/delete- Section or Property cases with different paths! (e.g.
 * having '/sec' or '/prop' not working properly until now!) normal cases work well, purgeEmptySections working,
 * checkDatatType finally working correct still needs more testing: mapping, linkdSection stuff and terminology use
 * (only simple cases tested so far, no nested stuff yet)
 */

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
   // logging
   private static final long serialVersionUID           = 145L;
   public static final int   MERGE_THIS_OVERRIDES_OTHER = 0, MERGE_OTHER_OVERRIDES_THIS = 1,
         MERGE_COMBINE = 2;
   private String            type                       = null, definition = null, name = null,
         reference = null;
   private Vector<Property>  properties;                                                          // = new Vector<Property>();
   private URL               repositoryURL              = null, fileUrl = null;
   private String            link                       = null;
   private String            include                    = null, author = null, version = null;
   private Date              date                       = null;
   private Section           parent, terminology = null;
   private URL               mapping                    = null;
   protected int             level;
   private boolean           isTerminology              = false;
   protected Vector<Section> subsections;


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


   public Section(String name, String type, String id) throws Exception {
      this(null, name, type, id);
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
   public Section(Section parent, String name, String type, String reference) throws Exception {
      this(parent, name, type, reference, null);
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
    * @param baseURL
    * @param mappingURL
    * @throws Exception
    */
   public Section(Section parent, String name, String type, String reference, String definition,
                  URL baseURL,
                  URL mappingURL) throws Exception {
      super();
      if (type == null) {
         logger.error("odml.core.Section.initialize causes exception");
         throw new Exception("Type must not be null!");
      }
      if (type.isEmpty()) {
         logger.error("odml.core.Section.initialize causes exception");
         throw new Exception("Type must not be empty.");
      }
      if (parent != null && (parent.getClass() != Section.class)) {
         logger.error("odml.core.Section.initialize causes exception");
         throw new Exception("Invalid parent. parent must be of type odMLSection!");
      }
      this.type = checkTypeStyle(type);
      if (name == null || name.isEmpty()) {
         name = type;
      }
      this.name = checkNameStyle(name);
      this.reference = reference;
      this.definition = definition;
      this.repositoryURL = baseURL;
      this.mapping = mappingURL;
      this.subsections = new Vector<Section>();
      this.properties = new Vector<Property>();

      if (parent != null) {
         this.parent = parent;
         // parent.addSection(this);
      } else {
         this.level = 0;
      } // if no parent is given > this one is root, i.e. level = 0
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
         // type =
         // type.substring(0,type.indexOf(" "))+type.substring(type.indexOf(" ")+1,
         // type.indexOf(" ")+2).toUpperCase()+type.substring(type.indexOf(" ")+2);
         logger.warn("Invalid section type:\tmaking it lower case, replacing blanks");
      }

      String nameRegex = "^[a-zA-Z]{1}.*"; // checking beginning: normal
      // letter, than anything
      if (!type.matches(nameRegex)) {
         type = "s_" + type;
         logger.warn("Invalid section type:\t's_' added as no leading character found");
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
         logger.warn("Invalid section name:\treplacing blanks '/' by '-'");
      }

      //		String nameRegex = "^[a-zA-Z]{1}.*"; // checking beginning: normal
      //		// letter, than anything
      //		if (!name.matches(nameRegex)) {
      //			name = "s_" + name;
      //			logger.warn("Invalid section type:\t's_' added as no leading character found");
      //		}
      return name;
   }


   /**
    * checking if the ordinarySection is consistent to the given terminology section in aspects of definition,
    * dependencyURLs, mappingURL, terminologyURL, synonyms If not taking the info of the ordinary section and logging a
    * warning
    * 
    * @param termSec
    *            {@link Section}: the terminology section
    * @param ordinarySec
    *            {@link Section}: the section of the user
    */
   private void checkTerminologyConsistency(Section termSec, Section ordinarySec) {
      if (termSec == null)
         return;
      if (ordinarySec == null)
         return;

      // checking the definition
      if (termSec.getDefinition() != null && (!termSec.getDefinition().isEmpty())) {
         if (ordinarySec.getDefinition() == null || (ordinarySec.getDefinition().isEmpty())) {
            ordinarySec.setDefinition(termSec.getDefinition());
            logger.info("definintion set to one used in terminology");
         } else if (!termSec.getDefinition().equalsIgnoreCase(ordinarySec.getDefinition())) {
            logger.warn("definition of checked section and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the mappingURL
      if (termSec.getMapping() != null && (!termSec.getMapping().toString().isEmpty())) {
         if (ordinarySec.getMapping() == null || (ordinarySec.getMapping().toString().isEmpty())) {
            ordinarySec.setMapping(termSec.getMapping());
            logger.info("mappingURL set to one used in terminology");
         } else if (!termSec.getMapping().toString().equalsIgnoreCase(
               ordinarySec.getMapping().toString())) {
            logger.warn("mappingURL of checked section and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the terminologyURL
      if (termSec.getRepository() != null && (!termSec.getRepository().toString().isEmpty())) {
         if (ordinarySec.getRepository() == null
               || (ordinarySec.getRepository().toString().isEmpty())) {
            ordinarySec.setRepository(termSec.getRepository());
            logger.info("terminologyURL set to one used in terminology");
         } else if (!termSec.getRepository().toString().equalsIgnoreCase(
               ordinarySec.getRepository().toString())) {
            logger.warn("terminologyURL of checked section and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the synonyms // actually there are no synonyms in the
      // terminology as this is the original term
      // if (termSec.getSynonyms() != null &&
      // (!termSec.getSynonyms().isEmpty())){
      // if(ordinarySec.getSynonyms() == null ||
      // (ordinarySec.getSynonyms().isEmpty())){
      // ordinarySec.setSynonyms(termSec.getSynonyms());
      // logger.info("synonyms set to one used in terminology");
      // }
      // else{ // adding the ones of the terminology
      // for (String synonym : termSec.getSynonyms()){
      // ordinarySec.addSynonym(synonym);
      // }
      // logger.warn("synonyms of checked section and of one in terminology different! "
      // +
      // "Adding the ones of terminology to the ones defined by user");
      // }
      // }
   }


   /**
    * checking if the property is consistent to the given terminology of its parent-section in aspects of dependency,
    * dependency-value, nameDefinition, mappingURL, propSynonyms. If not taking the info of the ordinary property and
    * logging a warning
    * 
    * @param property
    *            {@link Property}: the property that shall be checked against the one defined in the terminology of its
    *            parent-section. if there is no terminology set or no such property in it, return is called
    */
   private void checkTerminologyConsistency(Property property) {
      if (this.terminology == null)
         return;
      if (this.terminology.propertyCount() <= 0)
         return;
      Property termProp = this.terminology.getProperty(property.getName());
      if (termProp == null)
         return;

      // checking the dependency
      if (termProp.getDependency() != null && (!termProp.getDependency().isEmpty())) {
         if (property.getDependency() == null || (property.getDependency().isEmpty())) {
            property.setDependency(termProp.getDependency());
            logger
                  .warn("dependency set to one used in terminology; user has to check correctness!");
         } else if (!termProp.getDependency().equalsIgnoreCase(property.getDependency())) {
            logger.warn("dependency of checked property and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the dependencyValue
      if (termProp.getDependencyValue() != null && (!termProp.getDependencyValue().isEmpty())) {
         if (property.getDependencyValue() == null || (property.getDependencyValue().isEmpty())) {
            property.setDependencyValue(termProp.getDependencyValue());
            logger
                  .warn("dependencyValue set to one used in terminology; user has to check correctness!");
         } else if (!termProp.getDependencyValue().equalsIgnoreCase(property.getDependencyValue())) {
            logger.warn("dependencyValue of checked property and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the nameDefinition
      if (termProp.getDefinition() != null && (!termProp.getDefinition().isEmpty())) {
         if (property.getDefinition() == null || (property.getDefinition().isEmpty())) {
            property.setDefinition(termProp.getDefinition());
            logger.info("nameDefinition set to one used in terminology");
         } else if (!termProp.getDefinition().equalsIgnoreCase(property.getDefinition())) {
            logger.warn("nameDefinition of checked property and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the mappingURL
      if (termProp.getMapping() != null && (!termProp.getMapping().toString().isEmpty())) {
         if (property.getMapping() == null || (property.getMapping().toString().isEmpty())) {
            property.setMapping(termProp.getMapping());
            logger.info("mappingURL set to one used in terminology");
         } else if (!termProp.getMapping().toString().equalsIgnoreCase(
               property.getMapping().toString())) {
            logger.warn("mappingURL of checked property and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // // checking the propSynonyms // actually there are no synonyms in the
      // terminology as this is the original term
      // if (termProp.getSynonyms() != null &&
      // (!termProp.getSynonyms().isEmpty())){
      // if(property.getSynonyms() == null ||
      // (property.getSynonyms().isEmpty())){
      // property.setSynonyms(termProp.getSynonyms());
      // logger.info("synonyms set to one used in terminology");
      // }
      // else{ // adding the ones of the terminology
      // for (String synonym : termProp.getSynonyms()){
      // property.addSynonym(synonym);
      // }
      // logger.warn("synonyms of checked property and of one in terminology different! "
      // +
      // "Adding the ones of terminology to the ones defined by user");
      // }
      // }
      // checking all the values appended to the given property
      for (int i = 0; i < property.valueCount(); i++) {
         if (termProp.getValueIndex((property.getWholeValue(i)).getContent()) >= 0) {
            Value fromTerm = termProp.getWholeValue(termProp
                  .getValueIndex((property.getWholeValue(i)).getContent()));
            checkTerminologyConsistency(fromTerm, property.getWholeValue(i));
         }
      }
   }


   /**
    * checking if the value is consistent to the given terminology-value of its parent-property in aspects of unit,
    * type, uncertainty, defaultFileName, valueComment, id If not taking the info of the ordinary Value and logging a
    * warning
    * 
    * @param fromTerm
    *            {@link Value}: the value defined in the terminology
    * @param value
    *            {@link Value}: the value of the user, that shall be checked return if one value == null
    */
   private void checkTerminologyConsistency(Value fromTerm, Value value) {
      if (fromTerm == null)
         return;
      if (value == null)
         return;

      // checking the unit
      if (fromTerm.getUnit() != null && (!fromTerm.getUnit().isEmpty())) {
         if (value.getUnit() == null || (value.getUnit().isEmpty())) {
            value.setUnit(fromTerm.getUnit());
            logger.warn("unit set to one used in terminology; user has to check correctness "
                  + "(e.g. could be mV and not V");
         } else if (!fromTerm.getUnit().equalsIgnoreCase(value.getUnit())) {
            logger.warn("unit of checked value and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the type
      if (fromTerm.getType() != null && (!fromTerm.getType().isEmpty())) {
         if (value.getType() == null || (value.getType().isEmpty())) {
            value.setType(fromTerm.getType());
            logger.warn("type set to one used in terminology; user has to check correctness, "
                  + "as type should be the same!");
         } else if (!fromTerm.getType().equalsIgnoreCase(value.getType())) {
            logger.warn("type of checked value and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the uncertainty
      if (fromTerm.getUncertainty() != null && (!fromTerm.getUncertainty().toString().isEmpty())) {
         if (value.getUncertainty() == null || (value.getUncertainty().toString().isEmpty())) {
            value.setUncertainty(fromTerm.getUncertainty());
            logger
                  .warn("uncertainty set to one used in terminology; user has to check correctness!");
         } else if (!fromTerm.getUncertainty().equals(value.getUncertainty())) {
            logger.warn("uncertainty of checked value and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the defaultFileName
      if (fromTerm.getFilename() != null && (!fromTerm.getFilename().isEmpty())) {
         if (value.getFilename() == null || (value.getFilename().isEmpty())) {
            value.setFilename(fromTerm.getFilename());
            logger
                  .warn("defaultFileName set to one used in terminology; user has to check correctness!");
         } else if (!fromTerm.getFilename().equalsIgnoreCase(value.getFilename())) {
            logger.warn("defaultFileName of checked value and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the valueComment
      if (fromTerm.getDefinition() != null && (!fromTerm.getDefinition().isEmpty())) {
         if (value.getDefinition() == null || (value.getDefinition().isEmpty())) {
            value.setDefinition(fromTerm.getDefinition());
            logger
                  .info("valueComment set to one used in terminology; user has to check correctness!");
         } else if (!fromTerm.getDefinition().equalsIgnoreCase(value.getDefinition())) {
            logger.warn("valueComment of checked value and of one in terminology different! "
                  + "Using the one defined by user");
         }
      }
      // checking the id // actually independent from terminology as id
      // dependent on e.g. local database
      if (fromTerm.getReference() != null && (!fromTerm.getReference().isEmpty())) {
         if (value.getReference() == null || (value.getReference().isEmpty())) {
            value.setReference(fromTerm.getReference());
            logger.warn("id set to one used in terminology; user has to check correctness, "
                  + "as id should be independent!");
         } else if (!fromTerm.getReference().equalsIgnoreCase(value.getReference())) {
            logger.warn("id of checked value and of one in terminology different, "
                  + "using the one defined by user, to be rechecked by user");
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
         if (this.getSections(name).size() > 0) {
            logger
                  .warn("There already exists a section with that name! Will append an index to the name!");
            section.setName(section.getName() + this.getSectionsByType(section.getType()).size());
         }
         section.setParent(this);
         section.updateLevel();
         if (this.terminology != null && this.terminology.sectionCount() > 0) {
            Section termSec = this.terminology.getSection(section.getType());
            checkTerminologyConsistency(termSec, section);
         }
         subsections.add(section);
         index = subsections.size() - 1;
      }
      return index;
   }


   // /**
   // * Adds a new subsection to this section. The new section will have the
   // specified name.
   // * @param name {@link String} The new section's name, can be a path.
   // * @param type {@link String} the section's type. If null or empty the
   // type will be the same as the name.
   // * @return {@link Integer} the index of the new section
   // * (i.e. the number of subsections as added one is last), -1 when adding
   // failed
   // */
   // public int addSection(String name, String type){
   // return this.addSection(name, type, type, null, null);
   // }
   // /**
   // * Adds a new subsection to this section. The new section will have the
   // specified name and definition.
   // * @param name (@link String} The new section's name.
   // * @param definition (@link String} The new section's definition
   // * @return - int: returns the index of the added section if successful
   // * (i.e. the number of subsections as added one is last), -1 otherwise.
   // */
   // public int addSection(String name, String type, String definition){
   // return this.addSection(name, type, definition, null, null);
   // }
   // /**
   // * Creates a new subsection and adds it to the root section.
   // * @param name - String: the name of the new subsection.
   // * @param definition - String: the definition of this subsection. Can be
   // null or empty
   // * @param baseURL
   // * @param mappingURL
   // * @return - int: returns the index of the new section if successful
   // * (i.e. the number of subsections as added one is last), otherwise -1.
   // */
   // public int addSection(String name,String type, String definition, URL
   // baseURL, URL mappingURL){
   // try{
   // // if (name.contains("/")) { // name is a path as containing "/"
   // // String path = ensureValidPathEnding(name);
   // // if(type == null || type.isEmpty()){type =
   // path.substring(path.lastIndexOf("/")+1);}
   // // sectionToAdd = new Section(this, type,
   // path.substring(path.lastIndexOf("/")+1),
   // // definition,baseURL, mappingURL);
   // // return addSection(path.substring(0,path.lastIndexOf("/")),
   // sectionToAdd);
   // // }
   // // else { // name ordinary section name
   // // if(type == null || type.isEmpty()){type = name;}
   // Section sectionToAdd = new Section(this, name, type, definition,baseURL,
   // mappingURL);
   // return this.addSection(sectionToAdd);
   // // }
   // }catch (Exception e) {
   // logger.error("",e);
   // return -1;
   // }
   // }
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


   /**
    * Returns a section specified by it's path from the root (beginning with '/') or from the current section
    * (beginning directly with the subsection-name)
    * 
    * @param pathOfSections
    *            {@link String}: path to the wanted section
    * @return {@Section} - returns the section specified by the given path
    */
   private Section getSectionViaPath(String pathOfSections) {
      String path = ensureValidPathEnding(pathOfSections);
      Section sectionSpecifiedByPath = getActualSectionSpecifiedByPath(path, false);
      return sectionSpecifiedByPath;
   }


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
   public Section getSection(String name) {
      // name containing "/" meaning path is given
      if (name.contains("/")) {
         return getSectionViaPath(name);
      }
      Vector<Section> temp = getSections(name);
      if (temp != null && temp.size() > 1) {
         logger
               .warn("Section.getSection: more than one subsection of this name exists > returning first occurence");
         return temp.get(0);
      } else if (temp != null && temp.size() == 1) {
         return temp.get(0);
      }
      return null;
   }


   /**
    * Returns a {@link Vector} of subsections that match the name. Matching is case-insensitive.<br/>
    * 
    * @param name
    *            - {@link String}: the name of subsections.
    * @return Vector<Section> the {@link Vector} of matching {@link Section}s or an empty {@link Vector}.
    */
   public Vector<Section> getSections(String name) {
      Vector<Section> temp = new Vector<Section>();
      // name containing "/" meaning path is given
      if (name != null) {
         if (name.contains("/")) {
            name = ensureValidPathEnding(name);
            logger
                  .info("name for getSections actually a path > calling getSectionViaPath to search at right place");
            // actualParentsName is at penultimate position, ultimate
            // position is name of wanted sections!
            Section actualParent = getSectionViaPath(name.substring(0, name.lastIndexOf("/")));
            return actualParent.getSections(name.substring(name.lastIndexOf("/") + 1));
         }
         // ensure validness of name (i.e. if no loading character > adding
         // S_ as also done so when adding Section
         name = checkNameStyle(name);
         for (int i = 0; i < subsections.size(); i++) {
            if (subsections.get(i).getName().equalsIgnoreCase(name)) {
               temp.add(subsections.get(i));
            }
         }
         if (temp.size() == 0) {
            logger.info("Section.getSections: no subsection of this name exists!");
         }
      }
      return temp;
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
      if (temp.size() == 1) {
         found = getSectionsByType(type).get(0);
      }
      if (temp.size() > 1) {
         // try to find exact match
         for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).getType() != null && temp.get(i).getType().equalsIgnoreCase(type)) {
               found = temp.get(i);
               break;
            }
         }
         if (found == null) {
            logger
                  .warn("Section.getSectionByType: more than one subsection of this type exists > returning first occurence");
            found = getSectionsByType(type).get(0);
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
         if (subsections.get(i).getType().equalsIgnoreCase(type)) {
            temp.add(subsections.get(i));
         } else if (subsections.get(i).getType().contains("/")
               && subsections.get(i).getType().startsWith(type)) {// TODO
            // Problem
            // with
            // sam
            // beginnings
            temp.add(subsections.get(i));
         }
      }
      if (temp.size() == 0) {
         logger.debug("Section.getSectionsByType: no subsection of this type exists!");
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
    * Finds all {@link Section}s that have the given name. Name may also be a absolute or relative path.
    * 
    * @param name
    *            {@link String} the name or path to look for.
    * @return {@link Vector}<{@link Section}> All matching sections. In case of a path the vector contains only one
    *         element. May be empty if no match found.
    */
   public Vector<Section> findSections(String name) {
      Vector<Section> temp = new Vector<Section>();
      if (name.contains("/")) {// is a path
         Section s = getSectionViaPath(name);
         if (s != null) {
            temp.add(s);
         }
         return temp;
      }
      temp.addAll(this.getSections(name));
      for (int i = 0; i < sectionCount(); i++) {// no path, look for it
         temp.addAll(subsections.get(i).getSections(name));
      }
      return temp;
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
               parent = parent.getParent();// TODO uncles!!!!
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
      Vector<Section> sections = new Vector<Section>();
      Section s = getRelatedSection(type);
      if (s != null) {
         sections = s.getParent().getSectionsByType(type);
      }
      return sections;
   }


   /**
    * Ask if this section is the root section, i.e. its level must be 0.
    * 
    * @return - boolean: true if section is root, false otherwise.
    */
   public boolean isRoot() {
      return this.level == 0;
   }


   /**
    * Returns the level of this section (i.e. the depth / position in the tree)
    * 
    * @return - int: the level of this section.
    */
   public int getLevel() {
      return level;
   }


   // TODO: copySection(fromPath, toPath) using the copy function to get a real
   // second object, not only reference!
   /**
    * Removes a subsection that is defined by its index.
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
    * @param toRemove
    *            {@link Section} the section that should be deleted.
    * @return {@link Boolean} true if operation succeeded. False otherwise.
    */
   public boolean removeSection(Section toRemove) {
      return subsections.remove(toRemove);
   }


   /**
    * Removes the first subsection with this name (accordingly at this position in the tree with the name as the last
    * section in path)
    * 
    * @param name
    *            - {@link String}: the name of the subsection accordingly the path (e.g. sec/subsec/toRemoveSec
    * @return boolean: true if removing successful, false otherwise
    */
   public boolean removeSection(String name) {
      Vector<Section> temp = new Vector<Section>();
      if (name.contains("/")) {// is path
         Section s = getSection(name);
         if (s != null) {
            temp.add(s);
         }
      } else {
         temp = this.findSections(name);
      }
      if (temp.size() == 0) {
         logger.error("no section with that name found!");
         return false;
      }
      if (temp.size() > 1) {
         logger.error("Delete not unique! Found " + temp.size() + " matches.");
         return false;
      }
      return temp.get(0).getParent().removeSection(temp.get(0));
   }


   // /**
   // * Method to remove a section specified by a path. Leading "/" means path
   // beginning from root,
   // * otherwise beginning from current section (e.g.
   // subsec/subsubsec/secToRemove)
   // * @param pathOfSections
   // * @return {@link boolean}: true if removed successfully, false if no
   // section with specified name existing
   // */
   // private boolean removeSectionViaPath(String pathOfSections){
   // pathOfSections = ensureValidPathEnding(pathOfSections);
   // //parentOfRemover is at penultimate position in the path!
   // Section parentOfRemover =
   // getActualSectionSpecifiedByPath(pathOfSections.substring(0,
   // pathOfSections.lastIndexOf("/")), false);
   // if(parentOfRemover == null) return false;
   // return
   // parentOfRemover.removeSection(pathOfSections.substring(pathOfSections.lastIndexOf("/")+1));
   // }
   // *****************************************************************
   // ************** section basics ***********
   // *****************************************************************
   /**
    * Updates the level of the according section and all it's subsections. Called when calling addSection(), as a
    * Section can become subsection of another one when merging two files, etc.
    */
   private void updateLevel() {
      if (this.getParent() == null) {
         this.level = 0;
      } else {
         this.level = (this.getParent()).getLevel() + 1;
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
      // if (type.contains("/")){
      // logger.error("Section.setType: type must not be a path");
      // return false;
      // }
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
      if (this.getProperty("name") != null
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
    * @param url
    *            - {@link URL}: the url of the new terminology.
    * @return - boolean: returns true if operation succeeded, otherwise false.
    */
   public boolean setRepository(String url) {
      try {
         URL tempUrl = new URL(url);
         this.repositoryURL = tempUrl;
         // appending the terminology-tree to the section and checking
         // consistency
         // Reader readTerm = new Reader(termURL);
         // this.terminology = readTerm.getRootSection();
         // // usually terminology xml File starts with the odml-rootSection,
         // that contains no info
         // if(this.terminology.getType() == null ||
         // this.terminology.getType().isEmpty()){
         // this.terminology = readTerm.getRootSection().getSection(0);
         // }
         // // checking the actual section
         // checkTerminologyConsistency(this.terminology, this);
         // // checking its subsections
         // for (int i = 0; i < this.sectionCount(); i++){
         // if (this.terminology != null && this.terminology.sectionCount()
         // >0){
         // Section section = this.getSection(i);
         // Section termSec = this.terminology.getSection(section.getType());
         // checkTerminologyConsistency(termSec, section);
         // }
         // }
         // // checking its properties according to the new terminology
         // for (int i = 0; i < this.propertyCount(); i++){
         // checkTerminologyConsistency(this.getProperty(i));
         // }

         return true;
      } catch (Exception e) {
         this.repositoryURL = null;
         logger.error("", e);
         return false;
      }
   }


   public boolean setRepository(URL url) {
      try {
         this.repositoryURL = url;
         return true;
      } catch (Exception e) {
         this.repositoryURL = null;
         logger.error("", e);
         return false;
      }
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
    * @return - {@link String}: the section type.
    */
   public String getType() {
      return this.type;
   }


   /**
    * Returns the section name.
    * 
    * @return {@link String} the name of the section, may be null in case of root sections.
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
    * Returns the parent Section as TreeNode.
    * 
    * @return parent section as {@link TreeNode}, may be null for root sections.
    */
   public TreeNode getParentAsTreeNode() {
      return parent;
   }


   // *****************************************************************
   // ************** section links ***********
   // *****************************************************************
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
      temp = getSectionViaPath(this.link);
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
         Section linkedSection = getSectionViaPath(this.link);
         if (linkedSection != null) {
            linkedSection.resolveLink();
         } else {
            logger.error("Section.resolveLink: could not find referenced section!");
            return false;
         }
         this.merge(getSectionViaPath(this.link), MERGE_THIS_OVERRIDES_OTHER);
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


   // *****************************************************************
   // ************** Properties ***********
   // *****************************************************************
   /**
    * Adds a property to this section and returns its index.
    * 
    * @param property
    *            - {@link Property}: the property you want to add.
    * @return {@link Integer}: the index of the added property in the properties vector or -1 if command failed.
    */
   public int add(Property property) {
      // System.out.println("\tSection.add(Property): "+property.getName());
      if (this.level == 0 && this.type == null) {
         logger
               .error("! property must not be added to the root section (level == 0 && name == null)!");
         return -1;
      }
      if (property == null) {
         logger.error("! no property to add (argument is null)");
         return -1;
      }
      // make this section the parent of the property
      property.setParent(this);
      // *** check for existing property
      int index = contains(property);
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
      }
      if (this.terminology != null) {
         this.checkTerminologyConsistency(property);
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
    * @param pathOfSections
    *            {@link String}: the path form the root, where the property shall be added; different levels separated
    *            by "/", path ending on section-name!!
    * @param property
    *            - {@link Property}: the property you want to add.
    * @return {@link Integer}: the index of the added property in the properties vector or -1 if command failed.
    */
   public int add(String pathOfSections, Property property) {
      if (property == null) {
         logger.error("!no property for adding (is null)");
         return -1;
      }
      String path = ensureValidPathEnding(pathOfSections);
      Section newParent = getSectionViaPath(pathOfSections);
      // getActualSectionSpecifiedByPath(path, true); TODO check
      if (newParent == null) {
         logger.error("!path somehow wrong, no parent for adding porperty found! (path '" + path
               + "')");
         return -1;
      }
      return newParent.add(property);
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
      Property prop = null;
      // name is a path as containing "/"
      name = ensureValidPathEnding(name);
      try {
         if (name.startsWith("/") && (name.indexOf("/", 1) > 0)) {
            String path = name;
            prop = new Property(path.substring(path.lastIndexOf("/") + 1), value);
            return this.add(path.substring(0, path.lastIndexOf("/")), prop);
         } else if (name.startsWith("/")) {
            prop = new Property(name.substring(1), value);
            return this.add(prop);
         }
         // name ordinary section name
         else {
            prop = new Property(name, value);
            return this.add(prop);
         }
      } catch (Exception e) {
         logger.error("Section.addProperty: adding Property failed!");
         return -1;
      }
   }


   /**
    * Method to remove a property specified by a path. Leading "/" means path beginning from root, otherwise beginning
    * from current section (e.g. /root/subsec/subsubsec/propToRemove)
    * 
    * @param path
    * @return {@link boolean}: true if removed successfully, false if no property with specified name existing
    */
   private boolean removePropertyViaPath(String path) {
      path = ensureValidPathEnding(path);
      // parentOfRemover is at penultimate position in the path!
      Section parentOfRemover = getActualSectionSpecifiedByPath(path.substring(0, path
            .lastIndexOf("/")), false);
      if (parentOfRemover == null)
         return false;
      return parentOfRemover.removeProperty(path.substring(path.lastIndexOf("/") + 1));
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
      if (getProperties(name) != null && getProperties(name).size() > 1)
         logger.warn("more than one property with this name existing > removing first occurence");
      // if name contains "/" not only at first place, finding right parent
      // and removing prop from there
      if (name.contains("/")) {
         name = ensureValidPathEnding(name);
         if (name.substring(1).contains("/"))
            return removePropertyViaPath(name);
         else {
            name = name.substring(1);
         } // TODO eigentlich von root entfernen! (auch wenn des meistens ned
         // gehn sollte, weil dort keine props zulaessig sind!)
      }
      int place = -1;
      for (int i = 0; i < properties.size(); i++) {
         if (properties.elementAt(i).getName().equalsIgnoreCase(name)) {
            place = i;
            break;
         }
      }
      try {
         properties.removeElementAt(place);
         return true;
      } catch (ArrayIndexOutOfBoundsException a) {
         logger.error("no property with name '" + name + "' found for removing");
         return false;
      }
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
         properties.remove(index);
         return true;
      }
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
    * Returns the property given by a path (looking like /root/sec1/subsec1/property-name). If sections of path not
    * existing returning null.
    * 
    * @param pathToProperty
    *            {@link String}: the path to the wanted property, ending with the name of the property (if ending with
    *            '/' > deleting last character!) i.e. wanted path, not only the property-name itself (as there is
    *            another method getProperty (String name)
    * @return: the found property
    */
   private Property getPropertyViaPath(String pathToProperty, boolean resolveLink) {
      pathToProperty = ensureValidPathEnding(pathToProperty);
      // splitting path in sectionPath (until last '/') & propertyName
      // ('rest')
      String pathToParentSec = pathToProperty.substring(0, pathToProperty.lastIndexOf("/"));
      String propName = pathToProperty.substring(pathToProperty.lastIndexOf("/") + 1);
      logger.debug("total path: " + pathToProperty + ", path to section: " + pathToParentSec
            + " and propName: "
            + propName);
      Section newParent = getActualSectionSpecifiedByPath(pathToParentSec, false);
      if (newParent == null) {
         logger.error("!no parent found for given path (" + pathToParentSec + ")!");
         return null;
      }
      if (newParent.getType() != null)
         logger.debug("found parentsec of property '" + propName + "' via path with name: '"
               + newParent.getType()
               + "' and total path: '" + pathToParentSec + "'");
      return newParent.getProperty(propName, resolveLink);
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
      Property p = null;
      if (name != null && !name.isEmpty()) {
         if (name.contains("/")) // name is actually a path as containing "/"
            p = getPropertyViaPath(name, true);
         else
            p = getProperty(name, true); // ordinary property name
      }
      return p;
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
      // name actually a path > calling method from section defined by path
      if (name.contains("/")) {
         p = getPropertyViaPath(name, resolveLink);
      } else {
         // try to find the property in this section
         for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).getName().equalsIgnoreCase(name)) {
               p = properties.get(i);
            }
         }
         // if(p == null){//try to find it by its synonym
         // for(int i=0;i<properties.size();i++){
         // if(properties.get(i).hasSynonyms()){
         // Vector<String> syn = properties.get(i).getSynonyms();
         // if(syn.contains(name)){
         // p = properties.get(i);
         // }
         // else{
         // for(int j=0;j<syn.size();j++){
         // if(syn.get(j).equalsIgnoreCase(name)){
         // p = properties.get(j);
         // logger.warn("Section.getProperty(): found no exact match, returning case-insensitive match.");
         // break;
         // }
         //
         // }
         // }
         // if(((Vector<String>)properties.get(i).getSynonyms()).contains(name)){
         // p = properties.get(i);
         // }
         // }
         // }
         // }
         // try to find it in linked sections
         if (p == null && resolveLink && this.resolveLink()) { // this.resolveLink()
            // inflates
            // / expands
            // the tree
            // > search
            // can be
            // done
            // again
            p = this.getProperty(name, false);
         }
         // // check the terminology if it defines a synonym
         // if( p == null && !this.isTerminology){
         // System.out.println("Section.getProperty: looking in terminology for synonym!");
         // URL url = this.getRepository();
         // if(url != null){
         // Section term = TerminologyManager.instance().loadTerminology(url,
         // this.getType());
         // if(term != null){
         // p = this.getProperty(term.getPropertyOdmlName(name));
         // }
         // else{
         // System.out.println("Section.getProperty: looking in terminology for synonym!");
         // }
         // // String propertyName = getPropertyOdmlName(name);
         // // if(propertyName != null){
         // // getProperty(propertyName, resolveLink);
         // // }
         // }
         // }
      }
      // if everything fails
      if (p == null)
         logger.warn("Could not find property with name: " + name + "!");
      return p;
   }


   /**
    * Returns all properties stored in this section that have the specified name. Name matching is case insensitive.
    * 
    * @param name
    *            - {@link String}: the property name.
    * @return - Vector<odMLProperty>: returns the matching properties or null if no properties stored.
    */
   public Vector<Property> getProperties(String name) {
      if (name.contains("/")) { // name actually a path > calling method from
         // section defined by path
         logger.info("name to look for properties actually a path");
         name = ensureValidPathEnding(name);
         Section actualSec = getActualSectionSpecifiedByPath(name.substring(0, name
               .lastIndexOf("/")), false);
         return actualSec.getProperties(name.substring(name.lastIndexOf("/") + 1));
      }
      Vector<Property> temp = new Vector<Property>();
      for (int i = 0; i < properties.size(); i++) {
         if (properties.get(i).getName().equalsIgnoreCase(name)) {
            temp.add(properties.get(i));
         }
      }
      return temp;
   }


   /**
    * Returns all properties stored in this section.
    * 
    * @return - Vector<odMLProperty>: returns the properties Vector or null if no properties stored.
    */
   public Vector<Property> getProperties() {
      if (properties.size() == 0) {
         return null;
      }
      return properties;
   }


   // *****************************************************************
   // ************** util Methods ***********
   // *****************************************************************
   /**
    * Checks whether the path ends on "/" or not. If yes deleting last char
    * 
    * @param {@link String}: the path (e.g. section/section or /section/section/property/)
    * @return {@link String}: a path ending on an alphabetic char (i.e. not on "/")
    */
   private String ensureValidPathEnding(String path) {
      if (path.endsWith("/")) {
         path = path.substring(0, path.lastIndexOf("/"));
         logger.info("given path ending on '/' > deleting last character");
      }
      return path;
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
      // if ((this.getName()!=null && otherSection.getName()!=null) &&
      // !this.getName().equalsIgnoreCase(otherSection.getName())){
      // logger.error("Section.merge error: cannot merge sections havin different link identifiers!");
      // return;
      // }
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
         int index = this.contains(temp);
         if (index == -1) { // if 'otherSection' contains a new section add
            // it.
            this.add(temp);
         } else {// recursive call to merge subsections
            this.getSection(index).merge(temp, mergeOption);
         }
      }
      // terminologyURL
      if (this.getRepository() == null) {
         this.setRepository(otherSection.getRepository());
      } else {// resolve conflict
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getRepository() != null) {
            this.setRepository(otherSection.getRepository());
         }
      }
      // Definition
      if (this.getDefinition() == null) {
         this.setDefinition(otherSection.getDefinition());
      } else {// resolve conflict
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getDefinition() != null) {
            this.setDefinition(otherSection.getDefinition());
         } else if (mergeOption == MERGE_COMBINE && otherSection.getDefinition() != null) {
            this.setDefinition(this.definition + "\n" + otherSection.getDefinition());
         }
      }
      // mappingURL
      if (this.getMapping() == null) {
         this.setMapping(otherSection.getMapping());
      } else {// resolve conflict
         if (mergeOption == MERGE_OTHER_OVERRIDES_THIS && otherSection.getRepository() != null) {
            this.setRepository(otherSection.getRepository());
         }
      }
   }


   /**
    * Checks whether or not the section already contains a property with the same name, unit, type, parentValue and
    * parent. Returns the index of the first matching property.
    * 
    * @param property
    *            {@link Property} the property that should be found.
    * @return int: the index of the property if such a property already exists, -1 if not.
    */
   public int contains(Property property) {
      if (properties != null) {
         for (int i = 0; i < properties.size(); i++) {
            if (properties.get(i).getName().equalsIgnoreCase(property.getName())) {
               // if((properties.get(i).getType()!=null &&
               // property.getType()!=null &&
               // properties.get(i).getType().equalsIgnoreCase(property.getType()))
               // ||
               // (properties.get(i).getType()==null &&
               // property.getType()==null)){
               // if((properties.get(i).getUnit()!=null &&
               // property.getUnit()!=null &&
               // properties.get(i).getUnit().equalsIgnoreCase(property.getUnit()))
               // ||
               // (properties.get(i).getUnit()==null &&
               // property.getUnit()==null)){
               if ((properties.get(i).getDependency() != null && property.getDependency() != null && properties
                     .get(i).getDependency().equalsIgnoreCase(property.getDependency()))
                     || (properties.get(i).getDependency() == null && property.getDependency() == null)) {
                  if ((properties.get(i).getDependencyValue() != null
                        && property.getDependencyValue() != null && properties
                        .get(i).getDependencyValue()
                        .equalsIgnoreCase(property.getDependencyValue()))
                        || (properties.get(i).getDependencyValue() == null && property
                              .getDependencyValue() == null)) {
                     return i;
                  } else {
                     continue;
                  }
               } else {
                  continue;
               }
            } else {
               continue;
            }
            // }
            // else{continue;}
            // }
            // else{continue;}
         }
      }
      return -1;
   }


   /**
    * Checks whether or not this section already contains a subsection with the same name and type. Method does not
    * traverse the subsections!
    * 
    * @param section
    *            {@link Section}: the section that one looks for
    * @return {@link Integer}: the index of the section if exists, -1 if not.
    */
   public int contains(Section section) {
      if (subsections != null) {
         for (int i = 0; i < subsections.size(); i++) {
            if (subsections.get(i).getType().equalsIgnoreCase(section.getType())
                  && subsections.get(i).getName().equalsIgnoreCase(section.getName())) {
               return i;
            } else {
               continue;
            }
         }
      }
      return -1;
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


   // //*****************************************************************
   // //************** synonyms ***********
   // //*****************************************************************
   // /**
   // * Returns the standard name of a property that has the specified synonym.
   // * Method returns the first match of among the properties in this section.
   // * This method does not enter subsections.
   // * @param synonym {@link String}: the synonym
   // * @return String the standard odML name of the property. Returns null if
   // not found.
   // */
   // private String getPropertyOdmlName(String synonym){
   // for (int i = 0; i< this.propertyCount(); i++){
   // if(properties.get(i).hasSynonyms()){
   // Property prop = properties.get(i);
   // for(int j = 0; j<prop.getSynonymCount();j++){
   // if(prop.getSynonym(j).equalsIgnoreCase(synonym)){
   // return prop.getName();
   // }
   // }
   // }
   // }
   // return null;
   // }
   // *****************************************************************
   // ************** mappings ***********
   // *****************************************************************
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
      // if(url == null && this.parent != null){
      // url = this.parent.getMapping();
      // }
      return url;
   }


   // *****************************************************************
   // ************** optimization the tree ***********
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


   /**
    * 
    */
   private void validateRecursively() {
      this.validate();
      for (int i = 0; i < sectionCount(); i++) {
         getSection(i).validate();
      }
   }


   /**
    * Validate this section against the related terminology. Does not cycle through its subsections!
    */
   public void validate() {
      logger.info("Validating section " + this.getPath() + "...");
      if (this.terminology == null && findRepositoryURL() == null) {
         logger.warn("No terminology information available for section " + this.getPath()
               + "... skipping!");
         return;
      }
      if (terminology == null) {
         terminology = TerminologyManager.instance().loadTerminology(getRepository(), this.type);
      }
      if (terminology == null) {
         logger
               .error("No terminology information could be located or loaded! ... validation of section: "
                     + this.getPath() + " skipped!");
         return;
      }
      if (this.definition != null && !this.definition.isEmpty()) {
         if (!this.definition.equalsIgnoreCase(terminology.getDefinition())) {
            logger
                  .warn("The definition stored in section '"
                        + this.getPath()
                        + "' does not match the terminology definition."
                        + " Are they really meant to be the same? Please check. Kept provided definition, however!");
         }
      }
      for (int i = 0; i < propertyCount(); i++) {
         Property p = getProperty(i);
         logger.info("Validating property " + p.getName());
         // try to find an similar property in the terminology
         Property termProp = terminology.getProperty(p.getName());
         if (termProp != null) {
            p.compareToTerminology(termProp);
         } else {
            logger.info("Property: " + p.getName()
                  + " could not be validated. No counterpart in the terminology.");
         }
      }
   }


   /**
    * This method scans the tree and optimizes linked sections. This means that local sections are simplified that they
    * only contain information that deviates from the global description.
    */
   public void optimizeTree() {
      Section root = getRootSection();
      for (int i = 0; i < root.sectionCount(); i++) { // going through all
         // sections
         root.getSection(i).optimize();
      }
      logger.info("optimization done");
   }


   /**
    * Optimizes this section. Optimization is only done if this section is linked and all referring sections have been
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
      for (int i = 0; i < sectionCount(); i++) {
         if (getSection(i).sectionCount() == 0 && getSection(i).propertyCount() == 0)
            removeSection(i);
         else
            getSection(i).optimize();
      }
   }


   /**
    * 
    */
   private void removeEmptyProperties() {
      // System.out.println("Section.removeEmptyProperties");
      for (int i = propertyCount() - 1; i >= 0; i--) {
         this.getProperty(i).removeEmptyValues();
         if (this.getProperty(i).isEmpty()) {
            this.removeProperty(i);
         }
      }
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


   // *****************************************************************
   // ************** overrides ***********
   // *****************************************************************
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
      // all info belonging directly to section
      info += ("definition: \t" + this.definition + "\n");
      info += ("\n\t- repository: \t" + this.repositoryURL + "\n\t- mapping: \t" + this.mapping);
      info += ("\n\t- ");
      // subsections number and names
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

      // properties number and names
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
    * Method for getting the parent = section where to add new Property / Section via a path of sections. Different
    * Sections / levels separated by "/"
    * 
    * @param pathOfSections
    *            {@link String}: The path to specify the destination
    * @param usedForInserting
    *            {@link boolean}: flag to specify whether method is used for adding or getting sec/prop via path
    * @return Section {@link Section}: the section that is end of the path. created if not existing yet (same for other
    *         not existing sections yet on path)
    */
   private Section getActualSectionSpecifiedByPath(String pathOfSections, boolean usedForAdding) {
      pathOfSections = ensureValidPathEnding(pathOfSections);
      // parse the string to find the parent of this property/section
      String[] pathPieces = pathOfSections.split("/"); // '/' is the separator
      Section newParent = this;
      // beginning from current section
      if (!pathPieces[0].isEmpty()) {
         logger.debug("path beginning from current section");
         boolean ignoreFirst = false;
         if (pathPieces[0].equalsIgnoreCase(".")) {
            logger.info("found leading point, handling as path beginning from current position");
            ignoreFirst = true;
         }
         newParent = newParent.followPath(pathPieces, ignoreFirst, usedForAdding);
         return newParent;
      }
      // else beginning from root
      int pathDepth = pathPieces.length - 1; // -1 as tree level begins at
      // zero
      logger.debug("pathPieces with length: '" + pathPieces.length
            + "'; section for inserting prop/sec has level: "
            + pathDepth);
      if (pathDepth < 0)
         return null;

      if ((this.level == pathDepth) && (this.getPath() == pathOfSections)) {
         logger.debug("addProperty/addSection to parentSec as path same as specified one");
         return newParent;
      } else { // check where to add the prop/sec; creating section(s) if not
         // existing yet
         if (this.level > 0) {
            newParent = this.parent;
            while (newParent.level != 0) { // go to root first to follow
               // path from there
               newParent = newParent.parent;
            }
         }
         // check whether root has name, if not go one level deeper to first
         // path-piece
         if (newParent.type == null) {
            if (newParent.getSection(pathPieces[1]) == null) {
               if (usedForAdding) {
                  logger.warn("subsec '" + pathPieces[1] + "' does not exist > creating it");
                  try {
                     newParent.add(new Section(pathPieces[1], ""));
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               } else {
                  // logger.error("!section according to given path ("+pathOfSections+
                  // ") not existing!");
                  return null;
               }
            }
            newParent = newParent.getSection(pathPieces[1]);
         } else if (!newParent.type.equalsIgnoreCase(pathPieces[1])) {
            // logger.error("path from root specified but names differ!: '"+newParent.type+"' & '"
            // +pathPieces[1]+"'; meaning: there can only be one root!");
            return null;
         }
         String[] pathPiecesFromRoot = new String[pathPieces.length - 2];
         for (int i = 2; i < pathPieces.length; i++) {
            pathPiecesFromRoot[i - 2] = pathPieces[i];
         } // copying all besides first two, as [0] is empty string and [1]
         // is already starting point

         newParent = newParent.followPath(pathPiecesFromRoot, false, usedForAdding);
      } // end determining parent of prop/sec to add
      logger.debug("Section specified by path has name: '" + newParent.type
            + "', and total path: '"
            + newParent.getPath() + "'");
      return newParent;
   }


   /**
    * returning the section obtained by following the given path fragmented in an array. possibility to ignore first
    * entry (important when having "." in it. distinguishing between adding and getting-case > for getting no sections
    * must be created!
    * 
    * @param pathPieces
    *            {@link String[]}: String array containing single path elements = section names
    * @param givenIgnoreFirst
    *            {@link boolean}: flag to be able to ignore first entry (could be ".")
    * @param usedForAdding
    *            {@link boolean}: flag for distinguishing between adding and getting-case; for getting-case no sections
    *            must be created (returned section null then)
    * @return {@link Section} - returning the section specified by path. usedForAdding == true: creating sections on
    *         path when not existing yet usedForAdding == false: returning null when sections on path not existing
    */
   private Section followPath(String[] pathPieces, boolean givenIgnoreFirst, boolean usedForAdding) {
      boolean ignoreFirst = givenIgnoreFirst;
      Section SectionAtEndOfPath = this;
      for (int i = 0; i < pathPieces.length; i++) {
         if (ignoreFirst) {
            ignoreFirst = false;
            continue; // "." in first array position, not interesting
         }
         if (SectionAtEndOfPath.getSection(pathPieces[i]) == null) {
            if (usedForAdding) {
               // logger.warn("subsec '"+pathPieces[i]+"' not existing yet > creating it");
               try {
                  SectionAtEndOfPath.add(new Section(pathPieces[i], ""));
               } catch (Exception e) {
                  e.printStackTrace();
               }
            } else {
               // logger.error("!section according to given path not existing!");
               return null;
            }
         }
         SectionAtEndOfPath = SectionAtEndOfPath.getSection(pathPieces[i]);
      }
      return SectionAtEndOfPath;
   }


   /**
    * This method returns the absolute path of this section beginning from the root. Paths are made up from section
    * names separated by "/". Leading '/' indicates an absolute path.
    * 
    * @return {@link String} returns the path as a String, different Sections / levels separated with '/'
    */
   public String getPath() {
      String completePath = "";
      if (!this.isRoot()) {
         completePath = parent.getPath() + "/" + this.getName();
      }
      // if ((this.getParent() != null)){ // parent existing, recursive
      // calling
      // completePath = this.name;
      // if ((((Section)this.getParent()).getName() != null) &&
      // (!((Section)this.getParent()).getName().isEmpty())){
      // completePath =
      // ((Section)this.getParent()).getPath().concat("/").concat(completePath);
      // }
      // else { // parent existing, but no name (means, is root, no other
      // option possible) > just adding '/'
      // completePath = ("/").concat(completePath);
      // }
      // }
      // else {
      // if((this.name != null) && (!this.name.isEmpty())){
      // completePath = "/".concat(this.name);
      // }
      // else {
      // completePath = "/";
      // }
      // }
      return completePath;
   }


   // *****
   // important methods to be able to create a TreeNode Enumeration (see below)
   // containing Properties and Sections
   // *****
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


   // *************************************************************
   // ************** Overrides for TreeNode ***********
   // *************************************************************
   @Override
   public Enumeration<TreeNode> children() {
      Enumeration<TreeNode> sectionsAndProperties;
      // creating an Enumeration of TreeNodes containing first the
      // subsections, then the Properties
      Vector<TreeNode> foo = this.getTreeNodeSections();
      foo.addAll(this.getTreeNodeProperties());
      sectionsAndProperties = foo.elements();
      return sectionsAndProperties;
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
         logger.info("index wanted for TreeNode of type Section");
         Section soo = (Section) arg0;
         for (int i = 0; i < this.sectionCount(); i++) {
            if (this.getSection(i).equals(soo))
               return i;
         }
         logger.error("wanted TreeNode (of type Section) not existent");
         return -1;
      } else if (arg0 instanceof Property) {
         logger.info("index wanted for TreeNode of type Property");
         Property poo = (Property) arg0;
         for (int j = 0; j < this.propertyCount(); j++) {
            // sectionCount must be added as Enumeration contains first the
            // sections, then the properties
            if (this.getProperty(j).equals(poo))
               return (j + this.sectionCount());
         }
         logger.error("wanted TreeNode (of type Property) not existent");
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
         Section temp = this.getSectionViaPath(link);
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
   public void loadInclude() {
      if (this.include == null) {
         return;
      }
      // System.out.println("TerminologyManager.loadInclude: include link" + this.include);
      Reader r = null;
      URL fileUrl = null;
      Section includeSection = null;
      Section rootSection = null;
      String includeSectionName = null;
      if (this.getInclude().contains("#")) {
         includeSectionName = this.getInclude().substring(this.getInclude().indexOf("#") + 1);
         this.setInclude(this.getInclude().substring(0, this.getInclude().indexOf("#"))); // TODO does this lead to
         // errors if load
         // include is evoked
         // more than once?
      }
      // try if the include is a valid url
      try {
         fileUrl = new URL(this.getInclude());
      } catch (NullPointerException e) {
         System.out.println("Section.loadInclude: NullPointerException");
      } catch (Exception e) {
         // try if a file can be created
         try {
            File thisFile = new File(this.getRootSection().getFileUrl().toURI());
            fileUrl = new File(new File(thisFile.getParent()), this.getInclude()).toURI().toURL();
         } catch (Exception e1) {
            try {
               logger
                     .error("Section.loadInclude: Could not create a file from the include information: "
                           + this.getRootSection().getFileUrl().toURI()
                           + " combined with "
                           + this.getInclude());
            } catch (Exception e2) {
               e2.printStackTrace();
            }
            return;
         }
      }
      try {
         r = new Reader();
         rootSection = r.load(fileUrl, Reader.NO_CONVERSION, false);
         // System.out.println("Section.loadInclude: sucessfully got include file.");
      } catch (Exception e) {
         logger.error("Section.loadInclude: Could not read file from the include location: "
               + fileUrl);
         return;
      }

      // if a name is provided find it by name or path
      Vector<Section> includeSections = null;

      if (includeSectionName != null) {
         // System.out.println("Section.loadInclude: include section name: " + includeSectionName);
         includeSections = rootSection.findSections(includeSectionName);
         // System.out.println("Section.loadInclude: found sections: " + includeSections.size());
         for (int i = includeSections.size() - 1; i >= 0; i--) {
            if (!includeSections.get(i).getType().equalsIgnoreCase(this.getType())) {
               includeSections.remove(i);
               // System.out.println("delete one");
            }
         }
      } else {
         includeSections = rootSection.findSectionsByType(this.getType());
      }
      if (includeSections.size() > 1
            && (includeSectionName == null || includeSectionName.isEmpty())) {
         // look for an exact and unique type match
         boolean unique = false;
         for (int i = 0; i < includeSections.size(); i++) {
            if (includeSections.get(i).getType() != null
                  && includeSections.get(i).getType().equals(this.getType())
                  && !unique) {
               unique = true;
               includeSection = includeSections.get(i);
            } else if (includeSections.get(i).getType() != null
                  && includeSections.get(i).getType().equals(this.getType()) && unique) {
               unique = false;
               break;
            }
         }
         if (includeSection == null || !unique)
            logger
                  .error("Section.loadInclude: Could not include from indicated location: Include statement is ambiguous!");
      } else if (includeSections.size() > 1) {
         for (int j = 0; j < includeSections.size(); j++) {
            // System.out.println(includeSections.get(j).getName()+" compare with "
            // +this.getName());
            if (includeSections.get(j).getName().equalsIgnoreCase(this.getName())) {
               includeSection = includeSections.get(j);
               // System.out.println("found");
               break;
            }
         }
      } else if (includeSections.size() == 1) {
         includeSection = includeSections.get(0);
      } else {
         logger
               .error("Could not find corresponding include section Include location does not contain section of type: "
                     + this.getType());
         return;
      }
      if (includeSection != null) {
         this.merge(includeSection, Section.MERGE_THIS_OVERRIDES_OTHER);
      } else {
         logger
               .error("Could not find corresponding include section. Inlcude location does not contain section of type: "
                     + this.getType());
         return;
      }
      this.include = null;
   }


   /**
    * Loads inlcudes from this section and cylces through all subsections. If you want to load all includes in the tree
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
    * Returns the stored file url, i.e. the url of the file this section originated from. Usually only defined for root
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
      if (this.isRoot()) {
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
}
