package odml.core;

/************************************************************************
 * odML - open metadata Markup Language -
 * Copyright (C) 2009, 2010 Jan Grewe, Jan Benda
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * odML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.*;

/**
 * The {@link Writer} class provides the tools to write
 * odML metadata files.
 *
 * @since 08.2009
 *
 * @author Jan Grewe, Christine Seitz
 *
 */
public class Writer implements Serializable {
   private static final long             serialVersionUID = 146L;
   private final boolean                 asTerminology;
   private Document                      doc;
   private final File                    file;
   private Section                       odmlTree         = null;

   private final static SimpleDateFormat dateFormat       = new SimpleDateFormat("yyyy-MM-dd");
   private final static SimpleDateFormat datetimeFormat   = new SimpleDateFormat(
           "yyyy-MM-dd hh:mm:ss");
   private final static SimpleDateFormat timeFormat       = new SimpleDateFormat("hh:mm:ss");



   /**
    * Creates a writer instance. Lets the Wirter write only those properties that have values.
    *
    * @param rootSection {@link Section} the root Section of the metadata tree.
    *
    * @author Jakub Krauz
    */
   public Writer(Section rootSection) {
      this(rootSection, false);
   }


   /**
    * Creates a writer instance. Setting asTerminology to true lets the writer
    * write also those properties that have no values as is usually the case for
    * terminologies.
    *
    * @param rootSection {@link Section}: the rootSection of the odml metadata tree.
    * @param asTerminology {@link Boolean}: if true also emtpy properties (no value) are written in the serialization,
    *        otherwise only non-emty properties are processed.
    *
    * @author Jakub Krauz
    */
   public Writer(Section rootSection, boolean asTerminology) {
      this.odmlTree = rootSection;
      this.asTerminology = asTerminology;
      this.file = null;
   }


   /**
    * Creates a writer instance. Lets the Wirter write only those properties that have values.
    *
    * @param filename {@link String} the full name of the destination file (including path).
    * @param rootSection {@link Section} the root Section of the metadata tree.
    *
    * @deprecated Use combination of {@link #Writer(Section)} and {@link #write(OutputStream)} instead.
    */
   @Deprecated
   public Writer(String filename, Section rootSection) {
      this(new File(filename), rootSection);
   }


   /**
    * Creates a Writer-instance. Writes only non-empty properties into the metadata files.
    *
    * @param file {@link File} the File into which the metadata should be written.
    * @param rootSection {@link Section}: the rootSection of the odml metadata tree.
    *
    * @deprecated Use combination of {@link #Writer(Section)} and {@link #write(OutputStream)} instead.
    */
   @Deprecated
   public Writer(File file, Section rootSection) {
      this(file, rootSection, false);
   }


   /**
    * Creates a Writer-instance. Setting asTerminology to true lets the writer
    * write also those properties that have no values as is usually the case for
    * terminologies.
    *
    * @param file {@link File} the File into which the metadata should be written.
    * @param rootSection {@link Section}: the rootSection of the odml metadata tree.
    * @param asTerminology {@link Boolean}: if true also emtpy properties (no value) are written, otherwise
    *        only non-emty properties are written to disc.
    *
    * @deprecated Use combination of {@link #Writer(Section, boolean)} and {@link #write(OutputStream)} instead.
    */
   @Deprecated
   public Writer(File file, Section rootSection, boolean asTerminology) {
      this.file = file;
      this.odmlTree = rootSection;
      this.asTerminology = asTerminology;
   }


   /**
    * Writes the odML serialization to the given output stream.
    *
    * @param stream {@link OutputStream}: output stream to which to write the document
    * @return {@link Boolean} true if operation was successful, false otherwise.
    *
    * @author Jakub Krauz
    */
   public boolean write(OutputStream stream) {
      if (odmlTree == null) {
         System.out.println("Writer.write error: there is no metadata to write!");
         return false;
      }
      createDom(odmlTree, asTerminology);
      return writeToStream(stream);
   }


   /**
    * Writes the odML serialization to the given output stream. The odML tree can be optimized
    * (linked sections are simplified to reduce redundancy) and validated against terminologies before
    * the serialization.
    *
    * @param stream {@link OutputStream}: output stream to which to write the document
    * @param optimize {@link Boolean}: remove empty properties and sections, removes redundancy in linked sections.
    * @param validate {@link Boolean}: validates the metadata against the terminologies.
    * @return {@link Boolean}: true if writing succeeded, false otherwise.
    *
    * @author Jakub Krauz
    */
   public boolean write(OutputStream stream, boolean optimize, boolean validate) {
      if (optimize)
         odmlTree.optimizeTree();
      if (validate)
         odmlTree.validateTree();
      return write(stream);
   }


   /**
    * Write the metadata to disc after the tree has been optimized (linked sections are simplified to reduce
    * redundancy) and validated against the terminologies.
    *
    * @param optimize {@link Boolean}: remove empty properties and sections, removes redundancy in linked sections.
    * @param validate {@link Boolean}: validates the metadata against the terminologies.
    * @return {@link Boolean}: true if writing succeeded, false otherwise.
    *
    * @deprecated Use {@link #write(OutputStream, boolean, boolean)} instead.
    */
   @Deprecated
   public boolean write(boolean optimize, boolean validate) {
      if (optimize) {
         odmlTree.optimizeTree();
      }
      if (validate) {
         odmlTree.validateTree();
      }
      return write();
   }


   /**
    * Writes the metadata to disc.
    *
    * @return {@link Boolean} true if operation was successful, false otherwise.
    *
    * @deprecated Use {@link #write(OutputStream)} instead.
    */
   @Deprecated
   public boolean write() {
      if (odmlTree == null) {
         out.println("Writer.write error: there is no metadata to write!");
         return false;
      }
      try {
         FileOutputStream stream = new FileOutputStream(file);
         createDom(odmlTree, asTerminology);
         return (write(stream));
      } catch (Exception e) {
         System.out.println(e.getMessage());
         return false;
      }
   }

   public Map<String, Object> getMap() {
      Map<String, Object> self = new HashMap<String, Object>();
      self.put("date", odmlTree.getDocumentDate());
      self.put("author", odmlTree.getDocumentAuthor());
      self.put("version", odmlTree.getDocumentVersion());
      self.put("repository", odmlTree.getRepository());
      self.put("section", odmlTree.getMap());
      return self;
   }


   /**
    *
    * @param odMLRoot {@link Section}: the section to start the dom creation.
    * @param asTerminology {@link boolean}: flag to indicate whether Template is used or not
    * @return {@link boolean}: true if creating Dom successfully, otherwise false
    */
   private void createDom(Section odMLRoot, boolean asTerminology) {
      doc = new Document();
      ProcessingInstruction instr = null;
      ProcessingInstruction altInstr = null;
      if (asTerminology) {
         altInstr = new ProcessingInstruction("xml-stylesheet",
                 "type=\"text/xsl\" href=\"odml.xsl\"");
         instr = new ProcessingInstruction("xml-stylesheet",
                 "type=\"text/xsl\" href=\"odmlTerms.xsl\"");
      } else {
         altInstr = new ProcessingInstruction("xml-stylesheet",
                 "type=\"text/xsl\" href=\"odmlTerms.xsl\"");
         instr = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"odml.xsl\"");
      }
      doc.addContent(instr);
      doc.addContent(altInstr);
      Element rootElement = new Element("odML");
      rootElement.setAttribute("version", "1");
      doc.setRootElement(rootElement);

      Section dummyRoot;
      if (odMLRoot.propertyCount() != 0) {
         dummyRoot = new Section();
         dummyRoot.add(odMLRoot);
         dummyRoot.setDocumentAuthor(odMLRoot.getDocumentAuthor());
         dummyRoot.setDocumentDate(odMLRoot.getDocumentDate());
         dummyRoot.setDocumentVersion(odMLRoot.getDocumentVersion());
         dummyRoot.setRepository(odMLRoot.getRepository());
      } else {
         dummyRoot = odMLRoot;
      }
      String author = dummyRoot.getDocumentAuthor();
      if (author != null) {
         Element authorElement = new Element("author");
         authorElement.setText(author);
         rootElement.addContent(authorElement);
      }
      String version = dummyRoot.getDocumentVersion();
      if (version != null) {
         Element versionElement = new Element("version");
         versionElement.setText(version);
         rootElement.addContent(versionElement);
      }
      String dateString = null;
      Date date = dummyRoot.getDocumentDate();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      if (date != null) {
         dateString = sdf.format(date);
      } else {
         date = new Date(Calendar.getInstance().getTimeInMillis());
         dateString = sdf.format(date);
      }
      Element dateElement = new Element("date");
      dateElement.setText(dateString);
      rootElement.addContent(dateElement);
      URL repository = dummyRoot.getRepository();
      if (repository != null) {
         Element repElement = new Element("repository");
         repElement.setText(repository.toString());
         rootElement.addContent(repElement);
      }
      for (int i = 0; i < dummyRoot.sectionCount(); i++) {
         appendSection(rootElement, dummyRoot.getSection(i), asTerminology);
      }
   }

   /**
    * Method to append a section-element to the dom-tree.
    * @param parent {@link Element}: the parent where the section shall be appended
    * @param section {@link Section}: the section to append to the parent-element
    * @param asTemplate {@link boolean}: flag to indicate whether template or not; if template then also writing 
    * value-information (e.g. unit or type) without having actual value-content
    */
   private void appendSection(Element parent, Section section, boolean asTemplate) {
      Element sectionElement = new Element("section");

      Element type = new Element("type");
      type.setText(section.getType());
      sectionElement.addContent(type);

      Element name = new Element("name");
      name.setText(section.getName());
      sectionElement.addContent(name);

      Element nameDefinition = new Element("definition");
      nameDefinition.setText(section.getDefinition());
      sectionElement.addContent(nameDefinition);

      Element repository = new Element("repository");
      URL termUrl = section.getRepository();
      if (termUrl != null) {
         repository.setText(termUrl.toString());
         sectionElement.addContent(repository);
      }

      Element mapping = new Element("mapping");
      URL mapUrl = section.getMapping();
      if (mapUrl != null) {
         mapping.setText(mapUrl.toString());
         sectionElement.addContent(mapping);
      }
      Element link = new Element("link");
      String sectionLink = section.getLink();
      if (sectionLink != null) {
         link.setText(sectionLink);
         sectionElement.addContent(link);
      }
      Element include = new Element("include");
      String sectionInclude = section.getInclude();
      if (sectionInclude != null) {
         include.setText(sectionInclude);
         sectionElement.addContent(include);
      }
      Element reference = new Element("reference");
      String sectionReference = section.getReference();
      if (sectionReference != null) {
         reference.setText(sectionReference);
         sectionElement.addContent(reference);
      }
      for (int i = 0; i < section.propertyCount(); i++) {
         appendProperty(sectionElement, section.getProperty(i), asTemplate);
      }
      for (int i = 0; i < section.sectionCount(); i++) {
         appendSection(sectionElement, section.getSection(i), asTemplate);
      }
      parent.addContent(sectionElement);
   }



   /**
    * Appends a property elements to the dom tree. Empty properties (those with no values) 
    * will only be written to file if the file is to become a terminology.
    *
    * @param parent {@link Element}: the parent Element to which the properties belong.
    * @param prop {@link Property}: the property to append.
    * @param asTerminology boolean: defines whether the file will be a terminology.
    */
   private void appendProperty(Element parent, Property prop, boolean asTerminology) {
      if (!asTerminology) {
         prop.removeEmptyValues();
         if (prop.isEmpty()) {
            out.println("Writer.appendProperty: Property " + prop.getName()
                    + "is empty and will not be written to file!");
            return;
         }
      }
      Element propertyElement = new Element("property");

      Element name = new Element("name");
      name.setText(prop.getName());
      propertyElement.addContent(name);

      Element nameDefinition = new Element("definition");
      String nameDef = prop.getDefinition();
      if (nameDef != null && !nameDef.isEmpty()) {
         nameDefinition.setText(nameDef);
         propertyElement.addContent(nameDefinition);
      }
      Element dependency = new Element("dependency");
      String dep = prop.getDependency();
      if (dep != null && !dep.isEmpty()) {
         dependency.setText(dep);
         propertyElement.addContent(dependency);
      }

      Element dependencyValue = new Element("dependencyValue");
      String depVal = prop.getDependencyValue();
      if (depVal != null && !depVal.isEmpty()) {
         dependencyValue.setText(depVal);
         propertyElement.addContent(dependencyValue);
      }

      Element mapping = new Element("mapping");
      URL mapURL = prop.getMapping();
      if (mapURL != null) {
         mapping.setText(mapURL.toString());
         propertyElement.addContent(mapping);
      }

      for (int i = 0; i < prop.valueCount(); i++) {
         appendValue(propertyElement, prop.getWholeValue(i), asTerminology);
      }

      parent.addContent(propertyElement);
   }


   /**
    * Appends a value element to the dom tree.
    *
    * @param parent {@link Element}: the parent Element to which the values belong.
    * @param val {@link Value}: the value to append.
    * @param asTemplate defines whether to save as template, i.e. empty values are accepted.
    */
   private void appendValue(Element parent, Value val, boolean asTemplate) {
      if (!asTemplate) {
         if (val.getContent() == null || val.getContent().toString().isEmpty()) { return; }
      }

      Element valueElement = new Element("value");
      if (val.getContent() != null && (!val.getContent().toString().isEmpty())) {
         if (val.getContent() instanceof Date) {
            Date d = (Date) val.getContent();
            if (val.getType().equalsIgnoreCase("date")) {
               valueElement.setText(dateFormat.format(d));
            } else if (val.getType().equalsIgnoreCase("datetime")) {
               valueElement.setText(datetimeFormat.format(d));
            } else if (val.getType().equalsIgnoreCase("time")) {
               valueElement.setText(timeFormat.format(d));
            } else {
               valueElement.setText(val.getContent().toString());
            }
         } else {
            valueElement.setText(val.getContent().toString());
         }
      }

      Element typeElement = new Element("type");
      String type = val.getType();
      if (type != null && (!type.isEmpty())) {
         typeElement.setText(type);
         valueElement.addContent(typeElement);
      }
      Element unitElement = new Element("unit");
      String unit = val.getUnit();
      if (unit != null && (!unit.isEmpty())) {
         unitElement.setText(unit);
         valueElement.addContent(unitElement);
      }
      Element errorElement = new Element("uncertainty");
      Object uncertainty = val.getUncertainty();
      if (uncertainty != null && (!uncertainty.toString().isEmpty())) {
         errorElement.setText(uncertainty.toString());
         valueElement.addContent(errorElement);
      }
      Element filenameElement = new Element("filename");
      String filename = val.getFilename();
      if (filename != null && (!filename.isEmpty())) {
         filenameElement.setText(filename);
         valueElement.addContent(filenameElement);
      }
      Element defElement = new Element("definition");
      String valueDefinition = val.getDefinition();
      if (valueDefinition != null && (!valueDefinition.isEmpty())) {
         defElement.setText(valueDefinition);
         valueElement.addContent(defElement);
      }
      Element idElement = new Element("reference");
      String id = val.getReference();
      if (id != null && (!id.isEmpty())) {
         idElement.setText(id);
         valueElement.addContent(idElement);
      }
      Element encoderElement = new Element("encoder");
      String encoder = val.getEncoder();
      if (encoder != null && (!encoder.isEmpty())) {
         encoderElement.setText(encoder);
         valueElement.addContent(encoderElement);
      }
      Element checksumElement = new Element("checksum");
      String checksum = val.getChecksum();
      if (checksum != null && (!checksum.isEmpty())) {
         checksumElement.setText(checksum);
         valueElement.addContent(checksumElement);
      }
      parent.addContent(valueElement);
   }


   /**
    * Writes the dom tree to the given output stream.
    *
    * @param stream the output stream
    * @return true if the dom tree was successfully written to the stream, false otherwise
    *
    * @author Jakub Krauz
    */
   private boolean writeToStream(OutputStream stream) {
      if (doc == null) {
         System.out.println("doc empty");
         return false;
      }
      try {
         System.out.println("in streamToFile");
         org.jdom2.output.Format frmt = org.jdom2.output.Format.getPrettyFormat().setIndent("    ");
         XMLOutputter outp = new XMLOutputter();
         outp.setFormat(Format.getPrettyFormat());
         outp.output(doc, stream);
      } catch (IOException ie) {
         System.out.println("StreamToFile failed: " + ie.getMessage());
         return false;
      }
      System.out.println("StreamToFile successful");
      return true;
   }

}
