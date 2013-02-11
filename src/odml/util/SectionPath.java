package odml.util;


public class SectionPath{
   private String sectionPart = ""; 
   private String nextSection = "";
   private String restPath = "";
   private String propertyPart = "";
   private boolean valid = false;
   private boolean absolute = false;
   
   public SectionPath(String path) {
      path = sanitizePath(path);
      this.valid = validate(path);
      if(path.startsWith("/")){
         absolute = true;
         path = path.substring(1);
      }
      splitPath(path);
   }


   public boolean addressesProperty(){
      return !propertyPart.isEmpty();
   }


   public boolean hasRest(){
      return !restPath.isEmpty();
   }

   public String nextSection(){
      return nextSection;
   }

   public boolean isAbsolute(){
      return absolute;
   }

   public boolean isValid(){
      return valid;
   }

   private String sanitizePath(String path){
      if (path.endsWith("/")) {
         path = path.substring(0, path.lastIndexOf("/"));
      }
      return path;
   }

   private void splitPath(String path) {
      if(path.contains("#")){
         sectionPart = path.substring(0,path.lastIndexOf("#"));
         propertyPart = path.substring(path.indexOf("#")+1);
      }
      else 
         sectionPart = path;
      if(sectionPart.contains("/")){
         nextSection = sectionPart.substring(0,sectionPart.indexOf("/"));
         restPath = sectionPart.substring(sectionPart.indexOf("/")+1);
      }
      else
         nextSection = sectionPart;
   }

   private boolean validate(String path) {
      return path.indexOf("#") == path.lastIndexOf("#");
   }


   public String getRestPath() {
      return restPath;
   }


   public String getPropertyPart() {
      return propertyPart;
   }
}
