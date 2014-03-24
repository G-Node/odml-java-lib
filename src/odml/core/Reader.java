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


   public Reader() {
      this(null);
   }


   /**
    * Constructor
    * 
    * @param schemaLocations
    *            {@link URL}[]: the location of the validation schema.
    */
   public Reader(URL[] schemaLocations) {
      this.schemaLocations = schemaLocations;
   }
   
   
   /**
    * Reads the odML document from the given InputStream and returns the root section of the odML tree.
    * This method does not load includes, does not resolve links and does not apply mapping information.
    * 
    * @param stream the input stream
    * @return {@link Section} the root section of the metadata tree loaded from the input stream
    * @throws Exception
    * 
    * @author Jakub Krauz
    */
   public Section load(InputStream stream) throws Exception {
       return load(stream, false);
   }
   
   
   /**
    * Reads the odML document from the given InputStream and returns the root section of the odML tree.
    * This method does not load includes, does not resolve links and does not apply mapping information.
    * It validates the tree if schemaLocations is given in the constructor {@link #Reader(URL[])}
    * 
    * @param stream the input stream
    * @param validate if true validates the tree using schema, else no validation is applied
    * @return {@link Section} the root section of the metadata tree loaded from the input stream
    * @throws Exception
    * 
    * @author Jakub Krauz
    */
   public Section load(InputStream stream, boolean validate) throws Exception {
       return load(stream, NO_CONVERSION, validate);
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
       this.fileUrl = fileURL;
       try {
           InputStream stream = fileURL.openStream();
           logger.info("Parsing the xml file: " + fileURL.toString() + "...");
           return load(stream, option, validate);
       } catch (IOException e) {
           logger.error("Could not open file at specified url: " +
                   fileURL.toString() + ". Verify connection!", e);
           return null;
       }
   }


   /**
    * Load the odML document from the given input stream.
    * 
    * @param stream the input stream
    * @param option
    * @param validate
    * @return {@link Section}: the root section of the loaded file.
    * @throws Exception
    */
   public Section load(InputStream stream, int option, boolean validate) throws Exception {
      Section s = null;

      Document dom = parseXML(stream);
      if (dom == null) {
         this.root = null;
         return s;
      }
      logger.info("Parsing succeeded.");
      
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
         logger.error("Can not handle odmlVersion: " + odmlVersion
               + " stopping further processing!");
         return;
      }

      String author = getDirectChildContent(rootElement, "author");
      root.setDocumentAuthor(author);
      Date date = null;
      String temp = getDirectChildContent(rootElement, "date");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
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
               root.add(parseSection(domSection));
            }
         }
      }
      confirmLinks(root);
      if (includes.size() > 0 && !loadIncludes) {
         logger
               .info("The document includes external files which have not yet been loaded. Call loadIncludes() to load them.");
      }
   }


   /**
    * Parses the xml file and creates the DOM representation of it.
    * 
    * @return Document - returns the Document (dom) representation of the xml-file or null if an error occured.
    * @throws IOException
    */
   private Document parseXML(InputStream stream) {
      if (stream == null) {
         return null;
      }

      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder dbuilder = dbf.newDocumentBuilder();
         Document dom = dbuilder.parse(stream);
         return dom;
      } catch (ParserConfigurationException pce) {
         logger.error("Parsing failed! ", pce);
         return null;
      } catch (IOException ioe) {
         logger.error("Parsing failed! ", ioe);
         return null;
      } catch (Exception e) {
         logger.error("Parsing failed! ", e);
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
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = null;
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
         Validator validator = schema.newValidator();
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
    * @return {@link Section}: the Section representation of the dom section
    */
   private Section parseSection(Element domSection) {
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
         section = new Section(name, type, reference);
         section.setDefinition(definition);
         section.setRepository(url);
         section.setMapping(mapURL);
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
            section.add(parseSection((Element) sections.item(i)));
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
      String dependency = "";
      String dependencyValue = "";
      String definition = "";
      URL mapURL = null;

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
      definition = getDirectChildContent(domProperty, "definition");
      dependency = getDirectChildContent(domProperty, "dependency");
      dependencyValue = getDirectChildContent(domProperty, "dependencyValue");
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

      Property property = null;
      try {
         property = new Property(name, tmpValues, definition, dependency, dependencyValue, mapURL);
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
      content = domValue.getFirstChild().getNodeValue();
      if (content == null) {
         content = "";
      }
      content = content.trim();
      if (content == null)
         content = "";
      unit = getDirectChildContent(domValue, "unit");
      if (domValue.getElementsByTagName("uncertainty").getLength() > 0) {
         uncertainty = ((Element) domValue.getElementsByTagName("uncertainty").item(0))
               .getTextContent();
      }
      if (domValue.getElementsByTagName("type").getLength() > 0) {
         type = ((Element) domValue.getElementsByTagName("type").item(0)).getTextContent();
      } else {
         type = "";
      }
      if (domValue.getElementsByTagName("filename").getLength() > 0) {
         filename = ((Element) domValue.getElementsByTagName("filename").item(0))
               .getTextContent();
      }
      if (domValue.getElementsByTagName("definition").getLength() > 0) {
         definition = ((Element) domValue.getElementsByTagName("definition").item(0))
               .getTextContent();
      }
      if (domValue.getElementsByTagName("reference").getLength() > 0) {
         reference = ((Element) domValue.getElementsByTagName("reference").item(0))
               .getTextContent();
      }
      if (domValue.getElementsByTagName("encoder").getLength() > 0) {
         encoder = ((Element) domValue.getElementsByTagName("encoder").item(0)).getTextContent();
      }
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
    * Loads all included files into the document. Sections can contain includes 
    * which are links to sections located in a separate file. Includes are specified
    * by an URL pointing to an odml file. This url may include a hash (#)followed by
    * the absolute path of the target section. 
    * When loading an included section, the section is extended by the content of target
    * section (including subsections and their properties). 
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
    * Resolves all links that are stored in the tree. This means that the
    * linking section are extended with the content of the section that is
    * referenced in the linking section. Links can only be established
    *  within the same file.
    */
   public void resolveLinks() {
      root.resolveAllLinks();
   }

}
