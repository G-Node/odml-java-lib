package odml.util;
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
 * <http:/s/www.gnu.org/licenses/>.
 */

public class SectionPath{
   private String sectionPart = ""; 
   private String nextSection = "";
   private String restPath = "";
   private String propertyPart = "";
   private boolean valid = false;
   private boolean absolute = false;

   public SectionPath(String path) {
      if(path != null && !path.isEmpty()){
         path = sanitizePath(path);
         this.valid = validate(path);
         if(path.startsWith("/")){
            absolute = true;
            path = path.substring(1);
         }
         splitPath(path);
      }
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
	  path = path.trim();
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
