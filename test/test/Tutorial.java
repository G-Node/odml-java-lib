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
			Section s = new Section("myFirstSection","dataset");
			//create a second section with s being parent of s2
			new Section(s,"myNextSection","dataset");
			//another one with a different type but the same name as s2
			new Section(s, "myNextSection", "stimulus");
			//create a Property and add it to section s
			Property p = new Property("TrialCount", 10, "int");
			s.add(p);
			//show the tree
			s.displayTree();
			//show the tree starting at s2
			//display all subsections
			System.out.println(s.getSections());
			//retrieve a single section
			System.out.println(s.getSection("myNextSection"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void sections2() {
	   
	}
	
	public static void main(String[] args) {
		new Tutorial();
	}
}
