package test;
import odml.core.Property;
import odml.core.Section;

/**
 * 
 * This file contains some example java code which, in most cases, can be 
 * also used in Matlab.
 * 
 */
public class Tutorial {
	public Tutorial() {
		sections1();
		sections2();
//		links();
//		includes();
//		propertiesWithBinaryContent();
	}

	

   private void propertiesWithBinaryContent() {
		// TODO Auto-generated method stub
		
	}

	private void sections1() {
		try {
		   //create a section
			Section s = new Section("myFirstSection","recording");
			//create a second section with s being the parent 
			new Section(s,"myNextSection","dataset");
			//another one with a different type but the same name as s2!!
			//NOTE: have a look at the logger Warning. Section names must be unique among siblings
			new Section(s, "myNextSection", "stimulus");
			//adding a subsection with a derived type
			new Section(s, "stimulus", "stimulus/white_noise");
			//  another section
            new Section(s, "thirdSection", "dataset");
            //show the tree
			s.displayTree();
			
			//*****************************
			//***** accessing sections
			//***************************** 
			//display all subsections
			System.out.println(s.getSections());
			//retrieve a section by name
			System.out.println(s.getSection("myNextSection"));
			//retrieve all sections by type
			System.out.println(s.getSectionByType("dataset"));
			//retrieve all sections of the same type
			System.out.println(s.getSectionsByType("dataset"));
			//retrieving subsections by type includes derived types!
			System.out.println(s.getSectionsByType("stimulus"));
            
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void sections2() {
	   //validation of a section
	   
	   //related sections....
	}
	
	private void properties1(){
	   try{
//	      create a section
	   Section s = new Section("Data01","dataset");
	     //create a Property and add it to section s
	   Property p = new Property("TrialCount", 10, "int");
       s.add(p);
	   }
	   catch (Exception e) {
	      e.printStackTrace();
      }
	}
	
	public static void main(String[] args) {
		new Tutorial();
	}
}
