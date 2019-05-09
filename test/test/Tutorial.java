package test;
import java.io.IOException;
import odml.core.Section;

/**
 * 
 * This file contains some example java code which, in most cases, can be 
 * also used in Matlab.
 * 
 */
public class Tutorial {
   public Tutorial() {
      Section tree = sections1();
      askForInput();
      sections2();
      askForInput();
      properties1(tree);
      askForInput();
      propertiesWithBinaryContent();
      askForInput();
      linksAndIncludes();
      askForInput();
      mappings();
   }

  
   private Section sections1() {
      Section s = null;
      try {
         System.out.println("odml javalib Tutorial: section01");
         System.out.println();
         System.out.println("Create a simple tree...");
         System.out.println("Create a section: Section s = new Section(\"myFirstSection\",\"recording\");...");
         s = new Section("myFirstSection","recording");


         System.out.println("Append a second to s: new Section(s,\"myNextSection\",\"dataset\");...");
         new Section(s,"myNextSection","dataset");

         System.out.println("Append another one with a different type but the same name as before \n " +
         "(Note the warning! Section names must be unique): new Section(s, \"myNextSection\", \"stimulus\");...");
         new Section(s, "myNextSection", "stimulus");

         System.out.println("Append a section that has a derived type: new Section(s, \"stimulus\", \"stimulus/white_noise\"); ...");
         new Section(s, "stimulus", "stimulus/white_noise");

         System.out.println("Let's add a few more sections: ");
         System.out.println("\tnew Section(s, \"thirdSection\", \"dataset\");");
         System.out.println("\tSection s2 = new Section(s, \"Subject01\", \"subject\");");
         System.out.println("\tnew Section(s2, \"cell01\", \"cell\");");
         System.out.println("\tnew Section(s2, \"cell02\", \"cell\");");
         new Section(s, "thirdSection", "dataset");
         Section s2 = new Section(s,"Subject01", "subject");
         new Section(s2,"Cell01", "cell");
         new Section(s2,"Cell02", "cell");
         s2.removeSection("/subject01/cell02");
         s2.addProperty("cell01#cellType", "CA-1");
         //show the tree
         System.out.println("Display the tree in tree view dialog, just for illustration! Not suited to really work with it! Also works in Matlab...");
         System.out.println("\ts.displayTree();");
         s.displayTree();

         askForInput();
         
         System.out.println();
         System.out.println("List all child sections of the root node: s.getSections()...");
         System.out.println(s.getSections());
         //retrieve a section by name
         System.out.println();
         System.out.println(s.getSection("myNextSection"));
         //retrieve all sections by type
         System.out.println();
         System.out.println(s.getSectionByType("dataset"));
         //retrieve all sections of the same type
         System.out.println();
         System.out.println(s.getSectionsByType("dataset"));
         // retrieving subsections by type includes derived types!
         System.out.println();
         System.out.println(s.getSectionsByType("stimulus"));

         System.out.println();
         System.out.println("Retrieving 'cell01' from the root section using path notation. getSection(\"Subject01/cell01\")...");
         System.out.println(s.getSection("Subject01/cell01/"));

         System.out.println();
         System.out.println("Retrieving 'cell01' from section 'Subject01' using absolute path notation: s2.getSection(\"/Subject01/cell01/\")...");
         System.out.println(s2.getSection("/Subject01/cell01/"));

      } catch (Exception e) {
         e.printStackTrace();
      }
      return s;
   }

   private void sections2() {
      //validation of a section

      //related sections....
   }

   private void properties1(Section s){
      try{
         System.out.println();
         System.out.println("odml Javalib tutorial properties1:");
         System.out.println();

         Section s2 = s.getSection("/subject01");
         System.out.println();
         System.out.println("Adding a new property to section 'Subject01' using absolute path notation. s2.addProperty(\"/subject01/cell01#cellType\", \"CA-1\");...");
         s2.addProperty("/subject01/cell01#cellType", "CA-1");
         System.out.println("Cell01 propertyList: " + s.getSection("/Subject01/cell01").getProperties());

         System.out.println();
         System.out.println("Retrieving the property 'cellType' from 'cell01' in section 'Subject01' using absolute path notation. s2.getProperty(\"/Subject01/cell01#cellType\")...");
         System.out.println("Cell type: " + s2.getProperty("/Subject01/cell01#cellType").getValue());

         System.out.println();
         System.out.println("Removing property 'cellType' from the 'cell01' section 'Subject01' using path notation. s2.removeProperty(\"/Subject01/cell01#cellType\")...");
         s2.removeProperty("/Subject01/cell01#cellType");

         System.out.println("Cell01 propertyList: " + s.getSection("/Subject01/cell01").getProperties());
         //	      create a section
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void propertiesWithBinaryContent() {
      // TODO Auto-generated method stub
   }

   private void linksAndIncludes(){
      // TODO Auto-generated method stub
   }

   private void mappings(){
      // TODO Auto-generated method stub
   }
   
   private void askForInput(){
      System.out.println("Press any key to continue ...");
      try {
         System.in.read();
      } catch (IOException e) {

      }
   }
   public static void main(String[] args) {
      new Tutorial();
   }
}
