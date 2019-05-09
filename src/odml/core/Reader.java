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
 * <a href="http://gnu.org/licenses">http://gnu.org/licenses</a>.
 */

import odml.util.Mapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderXSDFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;


/**
 * The {@link Reader} class reads an xml-file, applies the schema, if wanted and provides the tools to extract the
 * stored information.
 * 
 * @since 08.2009
 * 
 * @author Jan Grewe, Christine Seitz, Jakub Krauz
 *
 */
public class Reader implements Serializable {

   private static final long     serialVersionUID = 146L;
   private Section               root;
   private final URL[]           schemaLocations;
   private final Vector<Section> links            = new Vector<Section>();
   Vector<Section>               includes         = new Vector<Section>();
   private URL                   fileUrl;
   boolean                       loadIncludes     = false;
   public static int             NO_CONVERSION = 1, FULL_CONVERSION = 3, LOAD_AND_RESOLVE = 2, NO_VALIDATION = 4, VALIDATE = 5;


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
    * Reads the odML document from the given InputStream and returns the root section
    * of the odML tree.
    * This method does not load includes, does not resolve links and does not apply
    * mapping information.
    * 
    * @param stream the input stream
    * @return {@link Section} the root section of the metadata tree loaded from the input stream
    * @throws Exception
    * 
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
    */
   public Section load(InputStream stream, boolean validate) throws Exception {
       return load(stream, NO_CONVERSION, validate);
   }


   /**
    * Reads a metadata file from the location specified in file parameter and returns the root section of the odml
    * tree. Function does not load includes, resolves links or applies mapping information. Behavior of the function
    * can be specified with the option parameter. Returns the root section of the odml tree.
    * 
    * @param file  {@link String} the file url.
    * @return {@link Section} the root section of the metadata tree stored in the file.
    * 
    * @throws Exception
    */
   public Section load(String file) throws Exception {
      return load(file, NO_CONVERSION, false);
   }


   /**
    * Reads a metadata file from the specified url. Function behavior can be controlled using the option parameter: <ol><li>
    * NO_CONVERSION : 1. Does nothing else but reading the file and returning the odml tree as defined in the file.</li>
    * <li>LOAD_AND_RESOLVE: 2. Load the file and all external information (defined in the include element) and resolve
    * links.</li> <li>FULL_CONVERSION : 3. loads the file, load external information (defined in the include element)
    * and resolves links between sections and applies mappings.</li></ol>
    * 
    * @param file
    *            {@link String} the url of the metadata file.
    * @param option
    *            {@link Integer} the reading options.
    * @return Section the root section of the metadata tree stored in the file.
    * @throws Exception
    */
   public Section load(String file, int option) throws Exception {
      return load(file, option, false);
   }


   /**
    * 
    * @param file a string containing the file name.
    * @param loadOption int, specifying the actions to take upon loading
    * @return {@link Section} the root section of the file.
    * @throws Exception
    */
   public Section load(String file, int loadOption, boolean validate) throws Exception {
      URL url;
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
    * @param fileURL  The URL of the file.
    * @param option load option as described in load(String ...)
    * @return {@link Section}: the root section of the loaded file.
    * @throws Exception
    */
   public Section load(URL fileURL, int option, boolean validate) throws Exception {
       this.fileUrl = fileURL;
       try {
           InputStream stream = fileURL.openStream();
          System.out.println("Parsing the xml file: " + fileURL.toString() + "...");
           return load(stream, option, validate);
       } catch (IOException e) {
          System.out.println("Could not open file at specified url: " +
                  fileURL.toString() + ". Verify connection! " + e.getMessage());
           return null;
       }
   }


   /**
    * Load the odML document from the given input stream.
    * 
    * @param stream the input stream
    * @param option defines the behaviour during load. See load(String, int) method
    * @param validate defines whether the file should be validated against a schema.
    * @return {@link Section}: the root section of the loaded file.
    * @throws Exception
    */
   public Section load(InputStream stream, int option, boolean validate) throws Exception {
      boolean isValid = true;
      Section s;
      Document dom = parseXML(stream);
      if (dom == null) {
         this.root = null;
         return null;
      }
      if (validate && schemaLocations != null) {
         isValid = validateXML(stream);
      }
      
      if (isValid) {
         createTree(dom);
      } else {
         System.out.println("Validation failed.");
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
    * @param dom - {@link Document}: the document to parse
    */
   public void createTree(Document dom) {
      root = new Section();
      if (dom == null) {
         return;
      }
      Element rootElement = dom.getRootElement();
      String odmlVersion = rootElement.getAttribute("version").getValue();
      if (Float.parseFloat(odmlVersion) != 1.0) {
         System.out.println("Can not handle odmlVersion: " + odmlVersion
                 + " stopping further processing!");
         return;
      }

      String author = rootElement.getChildText("author");
      root.setDocumentAuthor(author);
      Date date;
      String temp = rootElement.getChildText("date");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
         date = sdf.parse(temp);
      } catch (Exception e) {
         date = null;
      }
      root.setDocumentDate(date);
      String version = rootElement.getChildText("version");
      root.setDocumentVersion(version);
      URL url = null;
      temp = rootElement.getChildText("repository");
      if (temp != null && !temp.isEmpty()) {
         try {
            url = new URL(temp);
         } catch (Exception e) {
            System.out.println("Reader.parseSection.repository: " + e);
         }
      }
      root.setRepository(url);
      root.setFileUrl(this.fileUrl);

      for (Element domSection : rootElement.getChildren("section")) {
         if (rootElement.isAncestor(domSection)) {
            root.add(parseSection(domSection));
         }
      }
      confirmLinks(root);
   }


   /**
    * Parses the xml file and creates the DOM representation of it.
    * @param stream - an {@link java.io.InputStream}
    * @return Document - returns the Document (dom) representation of the xml-file or null if an error occurred.
    */
   private Document parseXML(InputStream stream) {
      if (stream == null) {
         return null;
      }
      try {
         SAXBuilder builder = new SAXBuilder();
         return builder.build(stream);
      } catch (IOException ioe) {
         System.out.println("Parsing failed! " + ioe.getMessage());
         return null;
      } catch (Exception e) {
         System.out.println(e.getMessage());
         return null;
      }
   }


   /**
    * Validates the the metadata xml-file against a schema and returns true if the file is valid or false if validation
    * fails.
    * @param stream - the input stream.
    * @return - boolean true or false if validation succeeded or failed, respectively.
    */
   private boolean validateXML(InputStream stream) {
      for (URL schemaLocation : schemaLocations) {
         try {
            File xsdfile = new File("schema.xsd");
            XMLReaderJDOMFactory schemafac = new XMLReaderXSDFactory(xsdfile);
            SAXBuilder builder = new SAXBuilder(schemafac);
            Document validdoc = builder.build(stream);
         } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
         }
      }
      return true;
   }


   /**
    * Parses an xml section of the metadata file and returns it. Subsections are parsed in a recursive
    * manner.
    * 
    * @param domSection - {@link Element}: the section that is to parse
    * @return {@link Section}: the Section representation of the dom section
    */
   private Section parseSection(Element domSection) {
      String type = domSection.getChildText("type");
      String name = domSection.getChildText("name");
      String reference = domSection.getChildText("reference");
      String definition = domSection.getChildText("definition");
      URL mapURL = null;
      String temp = domSection.getChildText("mapping");
      if (temp != null && !temp.isEmpty()) {
         try {
            mapURL = new URL(temp);
         } catch (Exception e) {
            System.out.println("odml.core.Reader.parseSection.mappingURL handling: " + e.getMessage());
         }
      }

      URL url = null;
      temp = domSection.getChildText("repository");
      if (temp != null && !temp.isEmpty()) {
         try {
            url = new URL(domSection.getChildText("repository"));
         } catch (Exception e) {
            url = null;
            System.out.println("Reader.parseSection.repository: " + e.getMessage());
         }
      }
      String link = domSection.getChildText("link");
      String include = domSection.getChildText("include");
      Section section;
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
         System.out.println("Reader.parseSection: exception while creating section: " + e.getMessage());
         return null;
      }
      Property tempProp;
      for (Element element : domSection.getChildren("property")) {
         section.add(parseProperty(element));
      }
      for (Element element : domSection.getChildren("section")) {
         section.add(parseSection(element));
      }
      return section;
   }


   /**
    * Parses property and creates the odMLProperty representation of it.
    * 
    * @param domProperty - {@link Element}: the Element to parse. (should be a property element)
    * @return {@link Property} the {@link Property} representation of this domElement
    */
   private Property parseProperty(Element domProperty) {
      String name;
      name = domProperty.getChildTextTrim("name");
      String dependency;
      String dependencyValue;
      String definition;
      URL mapURL = null;

      String temp = domProperty.getChildText("mapping");

      if (temp != null && !temp.isEmpty() && !temp.endsWith("?")) {
         try {
            mapURL = new URL(temp);
         } catch (Exception e) {
            System.out.println("odml.core.Reader.parseProperty.mappingURL handling: \n"
                    + " \t> tried to form URL out of: '"
                    + domProperty.getChildText("mapping")
                    + "'\n\t= mapURL of Property named: " + name + e.getMessage());
         }
      }
      definition = domProperty.getChildText("definition");
      dependency = domProperty.getChildText("dependency");
      dependencyValue = domProperty.getChildText("dependencyValue");
      Vector<Value> tmpValues = new Vector<Value>();
      for (Element element : domProperty.getChildren("value")) {
         tmpValues.add(parseValue(element));
      }

      Property property;
      try {
         property = new Property(name, tmpValues, definition, dependency, dependencyValue, mapURL);
      } catch (Exception e){
         System.out.println("odml.core.Reader.parseProperty: create new prop failed. " + e.getMessage());
         property = null;
      }
      return property;
   }


   /**
    * Parses value and creates the odMLValue representation of it.
    * 
    * @param domValue
    *            - {@link Element}: the Element to parse. (should be a value element)
    * @return {@link Value} the {@link Value} representation of this domElement
    */
   private Value parseValue(Element domValue) {
      Value value;

      String content;
      String unit;
      Object uncertainty;
      String type;
      String filename;
      String definition;
      String reference;
      String encoder;
      String checksum;
      content = domValue.getTextTrim();
      if (content == null) {
         content = "";
      }
      unit = domValue.getChildText("unit");
      uncertainty = domValue.getChildText("uncertainty");
      type = domValue.getChildText("type");
      filename = domValue.getChildText("filename");
      definition = domValue.getChildText("definition");
      reference = domValue.getChildText("reference");
      checksum = domValue.getChildText("checksum");
      encoder = domValue.getChildText("encoder");
      try {
         value = new Value(content, unit, uncertainty, type, filename, definition, reference,
               encoder, checksum);
      } catch (Exception e) {
         System.out.println("odml.core.Reader.parseValue: create Value failed. " + e.getMessage());
         return null;
      }
      return value;
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
      for (Section link : links) {
         if (link.getSection(link.getLink()) == null
                 && link.getType().equals(link.getSection(link.getLink()).getType())) {
            System.out.println("Reader.confirmLinks: The link stored in section '"
                    + link.toString()
                    + "' could not be confirmed and was removed!");
            link.setLink(null, true);
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
      for (Section include : includes) {
         include.loadInclude();
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
