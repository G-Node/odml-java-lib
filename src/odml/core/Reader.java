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
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.*;
import odml.util.Mapper;
import org.slf4j.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * The {@link Reader} class reads an xml-file, applies the schema, if wanted and provides the tools to extract the
 * stored information.
 * 
 * @since 08.2009
 * 
 * @author Jan Grewe, Christine Seitz
 * 
 */
public class Reader implements Serializable {

   public static Logger          logger           = LoggerFactory.getLogger(Reader.class);
   private static final long     serialVersionUID = 146L;
   private Section               root;
   private final URL[]           schemaLocations;
   private final Vector<Section> links            = new Vector<Section>();
   Vector<Section>               includes         = new Vector<Section>();
   private URL                   fileUrl;
   boolean                       isValid          = true, loadIncludes = false;
   public static int             NO_CONVERSION    = 1, FULL_CONVERSION = 3, LOAD_AND_RESOLVE = 2,
         NO_VALIDATION = 4, VALIDATE = 5;


   public Reader() throws Exception {
      this(null);
   }


   /**
    * Constructor
    * 
    * @param schemaLocations
    *            {@link URL}[]: the location of the validation schema.
    */
   public Reader(URL[] schemaLocations) throws Exception {
      this.schemaLocations = schemaLocations;
   }


   /**
    * Reads a metadata file from the location specified in file parameter and returns the root section of the odml
    * tree. Function does not load includes, resolves links or applies mapping information. Behavior of the function
    * can be specified with the option parameter. Returns the root section of the odml tree.
    * 
    * @param file
    *            {@link String} the file url.
    * @return {@link Section} the root section of the metadata tree stored in the file.
    * 
    * @throws Exception
    * @throws MalformedURLException
    */
   public Section load(String file) throws MalformedURLException, Exception {
      return load(file, NO_CONVERSION, false);
   }


   /**
    * Reads a metadata file from the specified url. Funciton behavior can be controlled using the option parameter: <li>
    * NO_CONVERSION : 1. Does nothing else but reading the file and returning the odml tree as defined in the file.</li>
    * <li>LOAD_AND_RESOLVE: 2. Load the file and all external information (defined in the include element) and resolve
    * links.</li> <li>FULL_CONVERSION : 3. loads the file, load external information (defined in the include element)
    * and resolves links between sections and applies mappings.</li>
    * 
    * @param file
    *            {@link String} the url of the metadata file.
    * @param option
    *            {@link Integer} the reding options.
    * @return Section the root section of the metadata tree stored in the file.
    * @throws MalformedURLException
    * @throws Exception
    */
   public Section load(String file, int option) throws MalformedURLException, Exception {
      return load(file, option, false);
   }


   /**
    * 
    * @param file
    * @param loadOption
    * @return {@link Section} the root section of the file.
    * @throws Exception
    * @throws MalformedURLException
    */
   public Section load(String file, int loadOption, boolean validate) throws MalformedURLException,
         Exception {
      URL url = null;
      try {
         url = new URL(file);
      } catch (Exception e) {
         try {
            url = new File(file).toURI().toURL();
         } catch (Exception exc) {
            throw new Exception("Could not read from specified location! " + file);
         }
      }
      return load(url, loadOption, validate);
   }


   /**
    * Load the file that is identified with the passed {@link URL}.
    * 
    * @param fileURL
    *            The URL of the file.
    * @param option
    * @return {@link Section}: the root section of the loaded file.
    * @throws Exception
    */
   public Section load(URL fileURL, int option, boolean validate) throws Exception {
      Section s = null;
      this.fileUrl = fileURL;
      Document dom = parseXML(fileURL);
      if (dom == null) {
         this.root = null;
         throw new Exception("parsing of file: " + fileURL.toString()
               + " failed! Please check file name.");
      }
      logger.info("... parsing succeeded.");
      if (validate && schemaLocations != null) {
         isValid = validateXML(dom);
         if (isValid)
            logger.info("Validation succeeded.");
      } else if (schemaLocations == null) {
         validate = false;
      } else {
         logger.info("Validation skipped on request.");
      }
      if (isValid) {
         logger.info("Creating odML tree representation...");
         createTree(dom);
         logger.info("... finished.");
      } else {
         logger.error("Validation failed.");
      }
      if (option == LOAD_AND_RESOLVE || option == FULL_CONVERSION) {
         loadIncludes();
      }
      if (option == LOAD_AND_RESOLVE || option == FULL_CONVERSION) {
         resolveLinks();
      }
      if (option == FULL_CONVERSION) {
         s = map();
      } else {
         s = this.getRootSection();
      }
      return s;
   }


   // *********************************************************
   // *********** handling the dom *************
   // *********************************************************
   /**
    * Converts the DOM representation of the metadata-file to the tree like odML structure.
    * 
    * @param dom
    *            - {@link Document}: the document to parse
    */
   public void createTree(Document dom) {
      root = new Section();
      if (dom == null) {
         return;
      }
      Element rootElement = dom.getDocumentElement();
      String odmlVersion = rootElement.getAttribute("version");
      if (Float.parseFloat(odmlVersion) != 1.0) {
         logger.error("can not handle odmlVersion: " + odmlVersion + " ending processing!");
         return;
      }

      String author = getDirectChildContent(rootElement, "author");
      root.setDocumentAuthor(author);
      Date date = null;
      String temp = getDirectChildContent(rootElement, "date");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {// convert the date to yyyy-MM-dd format
         date = sdf.parse(temp);
      } catch (Exception e) {
         date = null;
      }
      root.setDocumentDate(date);
      String version = getDirectChildContent(rootElement, "version");
      root.setDocumentVersion(version);
      URL url = null;
      temp = getDirectChildContent(rootElement, "repository");
      if (temp != null && !temp.isEmpty()) {
         try {
            url = new URL(temp);
         } catch (Exception e) {
            logger.error("Reader.parseSection.repository: ", e);
         }
      }
      root.setRepository(url);
      root.setFileUrl(this.fileUrl);
      if (rootElement.getElementsByTagName("section").getLength() > 0) {
         for (int i = 0; i < rootElement.getElementsByTagName("section").getLength(); i++) {
            Element domSection = (Element) rootElement.getElementsByTagName("section").item(i);
            if (domSection.getParentNode().isSameNode(rootElement)) {
               root.add(parseSection(domSection, root));
            }
         }
      }
      confirmLinks(root);
      if (includes.size() > 0 && !loadIncludes) {
         logger
               .info("The document includes external files which have not yet been loaded. Call loadIncludes() to load them.");
      }
   }


   // *********************************************************
   // *********** handling of the xml - file **************
   // *********************************************************
   /**
    * Parses the xml file and creates the DOM representation of it.
    * 
    * @return Document - returns the Document (dom) representation of the xml-file or null if an error occured.
    * @throws IOException
    */
   private Document parseXML(URL url) throws IOException {
      if (url == null) {
         return null;
      }
      // test the connection to the url
      // URLConnection testCon;
      // testCon = url.openConnection();
      // testCon.connect();
      this.fileUrl = url;
      logger.info("Parsing the xml file: " + url.toString() + "...");
      try {
         InputStream stream = null;
         try {
            stream = url.openStream();
         } catch (Exception e) {
            logger.error("Could not open url. ", e);
            return null;
         }
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         // Using factory get an instance of document builder
         DocumentBuilder dbuilder = dbf.newDocumentBuilder();
         // parse using builder to get DOM representation of the XML file
         Document dom = dbuilder.parse(stream);
         logger.info("... parsing succeeded!");
         return dom;
      } catch (ParserConfigurationException pce) {
         logger.error("... parsing failed! ", pce);
         return null;
      } catch (IOException ioe) {
         logger.error("... parsing failed! ", ioe);
         return null;
      } catch (Exception e) {
         logger.error("... parsing failed! ", e);
         return null;
      }
   }


   /**
    * Validates the the metadata xml-file against a schema and returns true if the file is valid or false if validation
    * fails.
    * 
    * @return - boolean true or false if validation succeeded or failed, respectively.
    */
   private boolean validateXML(Document dom) {
      // create a SchemaFactory capable of understanding WXS schemas
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = null;
      // load a WXS schema, represented by a Schema instance
      for (int i = 0; i < schemaLocations.length; i++) {
         try {
            schema = factory.newSchema(schemaLocations[i]);
         } catch (Exception e) {
            if (i < schemaLocations.length - 1) {
               continue;
            } else {
               logger.error("... validation failed. Could not open the schema definitions.", e);
               return false;
            }
         }
      }
      try {
         // create a Validator instance, which can be used to validate an instance document
         Validator validator = schema.newValidator();
         // validate the dom
         validator.validate(new DOMSource(dom));
      } catch (SAXException se) {
         logger.error("... validation failed! ", se);
         return false;
      } catch (IOException ioe) {
         logger.error("... validation failed! ", ioe);
         return false;
      } catch (Exception e) {
         logger.error("... validation failed! ", e);
         return false;
      }
      return true;
   }


   /**
    * Parses a section of the metadata file and adds a new {@link PropertyTreeNode} to the parentNode. Properties are
    * stored within the node's userObject as a {@link PropertyTableModel}. Subsections are parsed in a recursive
    * manner.
    * 
    * @param domSection
    *            - {@link Element}: the section that is to parse
    * @param parent
    *            - {@link Object}: the treeNode to which the section should be appended. Only {@link RootSection} or
    *            {@link Section} parents allowed.
    * @return odMLSection: the odMLSection representation of the dom section
    */
   private Section parseSection(Element domSection, Section parent) {
      String type = getDirectChildContent(domSection, "type");
      String name = getDirectChildContent(domSection, "name");
      String reference = getDirectChildContent(domSection, "reference");
      String definition = getDirectChildContent(domSection, "definition");
      URL mapURL = null;
      String temp = getDirectChildContent(domSection, "mapping");
      if (temp != null && !temp.isEmpty()) {
         try {
            mapURL = new URL(temp);
         } catch (Exception e) {
            logger.error("odMLReader.parseSection.mappingURL handling: ", e);
         }
      }

      URL url = null;
      temp = getDirectChildContent(domSection, "repository");
      if (temp != null && !temp.isEmpty()) {
         try {
            url = new URL(getDirectChildContent(domSection, "repository"));
         } catch (Exception e) {
            url = null;
            logger.error("Reader.parseSection.repository: ", e);
         }
      }
      String link = getDirectChildContent(domSection, "link");
      String include = getDirectChildContent(domSection, "include");
      Section section = null;
      try {

         section = new Section(parent, name, type, reference, definition, url, mapURL);
         section.setLink(link, true);
         if (link != null) {
            links.add(section);
         }
         section.setInclude(include);
         if (include != null) {
            includes.add(section);
         }
      } catch (Exception e) {
         logger.error("Reader.parseSection: exception while creating section: ", e);
         return null;
      }

      NodeList properties = domSection.getElementsByTagName("property");
      NodeList kids = domSection.getChildNodes();
      Property tempProp;
      if (properties.getLength() < kids.getLength()) {
         if (properties.getLength() > 0) {
            for (int i = 0; i < properties.getLength(); i++) {
               if (properties.item(i).getParentNode().isSameNode(domSection)) {
                  tempProp = parseProperty((Element) properties.item(i));
                  section.add(tempProp);
                  logger.debug("Property added");
               }
            }
         }
      } else {
         for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i).getNodeName().equals("property")) {
               tempProp = parseProperty((Element) kids.item(i));
               section.add(tempProp);
               logger.debug("Property added");
            }
         }
      }
      // append subsections
      NodeList sections = domSection.getElementsByTagName("section");
      if (domSection.getElementsByTagName("section").getLength() > 0) {
         for (int i = 0; i < sections.getLength(); i++) {
            if (!sections.item(i).getParentNode().isSameNode(domSection)) {
               continue;
            }
            section.add(parseSection((Element) sections.item(i), section));
            logger.debug("Subsection added");
         }
      }
      return section;
   }


   /**
    * Parses property and creates the odMLProperty representation of it.
    * 
    * @param domProperty
    *            - {@link Element}: the Element to parse. (should be a property element)
    * @return {@link Property} the {@link Property} representation of this domElement
    */
   private Property parseProperty(Element domProperty) {
      String name = null;
      name = getTextValue(domProperty, "name");
      // Vector<String> synonyms = new Vector<String>();
      String dependency = "";
      String dependencyValue = "";
      String definition = "";
      URL mapURL = null;

      // ***the mappingURL
      String temp = getDirectChildContent(domProperty, "mapping");

      if (temp != null && !temp.isEmpty() && !temp.endsWith("?")) {
         try {
            mapURL = new URL(temp);
         } catch (Exception e) {
            logger.error("odMLReader.parseProperty.mappingURL handling: \n"
                  + " \t> tried to form URL out of: '"
                  + domProperty.getElementsByTagName("mapping").item(0).getTextContent()
                  + "'\n\t= mapURL of Property named: " + name, e);
         }
      }
      // *** the name definition (element definition)
      definition = getDirectChildContent(domProperty, "definition");
      // //*** the synonyms (element synonym)
      // synonyms = getDirectChildContents(domProperty, "synonym");
      // *** the dependency
      dependency = getDirectChildContent(domProperty, "dependency");
      // *** the parent value
      dependencyValue = getDirectChildContent(domProperty, "dependencyValue");
      // get a Vector containing all the values of type Value
      NodeList values = domProperty.getElementsByTagName("value");
      NodeList kids = domProperty.getChildNodes();
      Vector<Value> tmpValues = new Vector<Value>();
      if (values.getLength() < kids.getLength()) {
         if (values.getLength() > 0) {
            for (int i = 0; i < values.getLength(); i++) {
               if (values.item(i).getParentNode().isSameNode(domProperty)) {
                  tmpValues.add(parseValue((Element) values.item(i)));
               }
            }
         }
      } else {
         for (int i = 0; i < kids.getLength(); i++) {
            if (kids.item(i).getNodeName().equals("value")) {
               tmpValues.add(parseValue((Element) kids.item(i)));
            }
         }
      }

      // *** create the new property
      Property property = null;
      try {
         property = new Property(name, tmpValues, definition, dependency, dependencyValue, mapURL);
         // for(int i=0;i<synonyms.size();i++){
         // property.addSynonym(synonyms.get(i));
         // }
         return property;
      } catch (Exception e) {
         logger.error("odMLReader.parseProperty: create new prop failed. ", e);
         return null;
      }
   }


   /**
    * Parses value and creates the odMLValue representation of it.
    * 
    * @param domValue
    *            - {@link Element}: the Element to parse. (should be a value element)
    * @return {@link Value} the {@link Value} representation of this domElement
    */
   private Value parseValue(Element domValue) {
      Value toReturn = null;

      String content = "";
      String unit = "";
      Object uncertainty = null;
      String type = "";
      String filename = "";
      String definition = "";
      String reference = "";
      String encoder = "";
      String checksum = "";
      // *** the value (as TextContent / NodeValue in xml-file)
      // actually wanting getTextContent(), but this iterates recursively through everything coming below
      content = domValue.getFirstChild().getNodeValue();
      if (content == null) {
         content = "";
      }
      content = content.trim();
      if (content == null)
         content = "";
      logger.debug(">>>>>>the value: " + content);
      // *** the unit
      unit = getDirectChildContent(domValue, "unit");
      // if(domValue.getElementsByTagName("unit").getLength()>0){
      // unit = domValue.getElementsByTagName("unit").item(0).getTextContent();
      // }
      // *** the uncertainty
      if (domValue.getElementsByTagName("uncertainty").getLength() > 0) {
         uncertainty = ((Element) domValue.getElementsByTagName("uncertainty").item(0))
               .getTextContent();
      }
      // *** the type
      if (domValue.getElementsByTagName("type").getLength() > 0) {
         type = ((Element) domValue.getElementsByTagName("type").item(0)).getTextContent();
      } else {
         type = "";
      }
      // *** the filename
      if (domValue.getElementsByTagName("filename").getLength() > 0) {
         filename = ((Element) domValue.getElementsByTagName("filename").item(0))
               .getTextContent();
      }
      // *** the value definitions
      if (domValue.getElementsByTagName("definition").getLength() > 0) {
         definition = ((Element) domValue.getElementsByTagName("definition").item(0))
               .getTextContent();
      }
      // *** the reference entry
      if (domValue.getElementsByTagName("reference").getLength() > 0) {
         reference = ((Element) domValue.getElementsByTagName("reference").item(0))
               .getTextContent();
      }
      // *** the encoder
      if (domValue.getElementsByTagName("encoder").getLength() > 0) {
         encoder = ((Element) domValue.getElementsByTagName("encoder").item(0)).getTextContent();
      }
      // *** the checksum entry
      if (domValue.getElementsByTagName("checksum").getLength() > 0) {
         checksum = ((Element) domValue.getElementsByTagName("checksum").item(0)).getTextContent();
      }
      try {
         toReturn = new Value(content, unit, uncertainty, type, filename, definition, reference,
               encoder, checksum);
      } catch (Exception e) {
         logger.error("odMLReader.parseValue: create Value failed. ", e);
         return null;
      }
      return toReturn;
   }


   /**
    * Returns the value of an element.
    * 
    * @param ele
    *            - {@link Element}: the element of which the TextValue should be read.
    * @param tagName
    *            - {@link String}: The tagName.
    * @return String: the textValue if the tag exists otherwise null.
    */
   private String getTextValue(Element ele, String tagName) {
      if (ele == null) {
         return null;
      }
      String textVal = null;
      NodeList nl = ele.getElementsByTagName(tagName);
      if (nl != null && nl.getLength() > 0) {
         Element el = (Element) nl.item(0);
         if (el == null) {
            return null;
         }
         if (el.getFirstChild() == null) {
            return null;
         }
         if (el.getFirstChild().getNodeValue() == null) {
            return null;
         }
         textVal = el.getFirstChild().getNodeValue();
      }
      return textVal;
   }


   /**
    * Returns the text content of the first direct element that is named according to the elementName argument. Direct
    * means that only 1st-level child elements are considered, i.e. those of which the parentNode equals element.
    * 
    * @param element
    *            {@link Element} the parent node.
    * @param elementName
    *            {@link String} the name of the searched child element.
    * @return {@link String} the text content or null.
    */
   private String getDirectChildContent(Element element, String elementName) {
      String content = null;
      NodeList l = element.getElementsByTagName(elementName);
      for (int i = 0; i < l.getLength(); i++) {
         if (l.item(i).getParentNode().isSameNode(element)) {
            content = l.item(i).getTextContent();
            return content;
         }
      }
      return content;
   }


   /**
    * Returns all the text contents of the direct element that is named according to the elementName argument. Direct
    * means that only 1st-level child elements are considered, i.e. those of which the parentNode equals element.
    * 
    * @param element
    * @param elementName
    * @return {@link Vector} of {@link String} that my be empty if no match is found.
    */
   @SuppressWarnings("unused")
   private Vector<String> getDirectChildContents(Element element, String elementName) {
      Vector<String> content = new Vector<String>();
      NodeList l = element.getElementsByTagName(elementName);
      for (int i = 0; i < l.getLength(); i++) {
         if (l.item(i).getParentNode().isEqualNode(element)) {
            content.add(l.item(i).getTextContent());
         }
      }
      return content;
   }


   /**
    * Returns the rootSection of the odMLTree, i.e. the root of type Section
    * 
    * @return - {@link Section} the root section of the odML Tree.
    */
   public Section getRootSection() {
      return root;
   }


   /**
    * Tries to confirm the links stored in the tree by trying to retreive the linkes sections. Links that could not be
    * validated are removed and an error message is generated.
    * 
    * @param root
    *            {@link Section} the root section of the odml - tree.
    */
   private void confirmLinks(Section root) {
      for (int i = 0; i < links.size(); i++) {
         if (links.get(i).getSection(links.get(i).getLink()) == null
               && links.get(i).getType().equals(links.get(i).getSection(links.get(i).getLink()))) {
            logger.error("Reader.confirmLinks: The link stored in section '"
                  + links.get(i).toString()
                  + "' could not be confirmed and was removed!");
            links.get(i).setLink(null, true);
         }
      }
   }


   /**
    * Loads all included Files into the document.
    */
   public void loadIncludes() {
      for (int i = 0; i < includes.size(); i++) {
         includes.get(i).loadInclude();
      }
   }


   /**
    * Tries to apply the mappings, if there are any.
    * 
    * @throws Exception
    */
   public Section map() throws Exception {
      Mapper m = new Mapper(root);
      return m.map();
   }


   /**
    * Resolves all links that are stored in the tree.
    */
   public void resolveLinks() {
      root.resolveAllLinks();
   }

}
