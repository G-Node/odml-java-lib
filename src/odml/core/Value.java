package odml.core;
/************************************************************************
 *	odML - open metadata Markup Language - 
 * Copyright (C) 2009, 2010 Jan Grewe, Jan Benda 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the  GNU Lesser General Public License (LGPL) as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * odML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;



/**
 * The {@link Value} class constitutes the further information of a property where only the value = content
 * is mandatory. It contains the following fields:
 * <ol>
 * <li>value - mandatory, its the value = content itself.</li>
 * <li>uncertainty - optional, an estimation of the value's uncertainty.</li>
 * <li>unit - optional, the vlaue's unit.</li>
 * <li>type - optional, the data type of the value.</li>
 * <li>filename - optional, the default file name which should be used when saving the object.</li>
 * <li>definition - optional, here additional comments on the value of the property can be given.</li>
 * <li>reference - optional, here additional comments on the value of the property can be given.</li>
 * <li>encoder - optional. If binary content is included in the {@link Value}, indicate the encoder used in the form.</li>
 * <li>checksum - optional. The checksum of the file included in the {@link Value}. State the checksum in the form algorithm$checksum (e.g. crc32$...).</li>
 * </ol> 
 *  Only the value = content is mandatory, the others are optional.
 *   
 * @since 06.2010
 * @author Jan Grewe, Christine Seitz
 *
 */
public class Value extends Object implements Serializable, Cloneable, TreeNode{
	static Logger logger = Logger.getLogger(Value.class.getName()); // for logging
	private static final long serialVersionUID = 147L; 
	private String unit = null, type = null, reference = null;
	private Object content, uncertainty;
	private String definition, filename, checksum, encoder;
	private Property associatedProperty;
	//*****************************************************************
	//**************				constructors			***********	
	//*****************************************************************
	/**
	 * @param content
	 * @param unit
	 * @throws Exception
	 */
	protected Value(Object content, String unit)throws Exception{
		this(content,unit,null,null);
	}
	/**
	 * 
	 * @param content
	 * @param unit
	 * @param uncertainty
	 * @throws Exception
	 */
	protected Value(Object content, String unit, Object uncertainty)throws Exception{
		this(content,unit,uncertainty,null);
	}
	/**
	 * 
	 * @param content
	 * @param unit
	 * @param uncertainty
	 * @param type
	 * @throws Exception
	 */
	protected Value(Object content, String unit, Object uncertainty, String type)throws Exception{
		this(content,unit,uncertainty,type,null,null, null);
	}
	/**
	 * Creates a Value from a Vector containing the value data in the following sequence:
	 * "content","unit","uncertainty","type","defaultFileName","valueDefinition","id"
	 * @param data {@link Vector} of Objects that contains the data in the sequence as the {@link Value}
	 * @throws Exception 
	 */
	protected Value(Vector<Object> data) throws Exception{
		this(data.get(0),(String)data.get(1),data.get(2),(String)data.get(3),(String)data.get(4),(String)data.get(5),
				(String)data.get(6));
	}
	/**
	 * Constructor for a Value containing all possible information. Any of the arguments
	 * may be null except for Object value and it's unit.
	 * @param content
	 * @param unit
	 * @param uncertainty
	 * @param type
	 * @param filename
	 * @param definition
	 * @param reference
	 * @throws Exception
	 */
	protected Value(Object content, String unit, Object uncertainty, String type, String filename, 
			String definition, String reference)throws Exception{
		this(content, unit,uncertainty,type,filename, definition, reference, "", "");
	}
	
	protected Value(Object content, String unit, Object uncertainty, String type, String filename, 
			String definition, String reference, String encoder)throws Exception{
		this(content, unit,uncertainty,type,filename, definition, reference,encoder, "");
	}
	
	protected Value(Object content, String unit, Object uncertainty, String type, String filename, 
			String definition, String reference, String encoder, String checksum)throws Exception{
		try {
			PropertyConfigurator.configure(this.getClass().getResource("/resources/loggingParams.txt"));	
		} catch (Exception e) {
			System.out.println("logger could not be properly configured.");
		}
		if(content == null){
			logger.debug("! value should not be empty except for terminologies!");
			//throw new ValueEmptyException("Could not create value! The Object 'value' is the mandatory entry and must not be null!");
		}
		else if(content instanceof String && content.toString().isEmpty()){
			logger.debug("! value should not be empty except for terminologies!");
			//throw new ValueEmptyException("Could not create value! The Object 'value' is the mandatory entry and must not be empty!");
		}
		boolean typeGiven = true;
		if(type==null || type.isEmpty()){
			type = inferType(content);
			if (type.equalsIgnoreCase("string")) typeGiven = false;
		}
		Object checkedValue = new Object();
		try {
			if (typeGiven) checkedValue = checkDatatype(content, type);
			else {
				int caseNumber = checkStringsforDatatype((String) content);
				switch(caseNumber){
				case 0: checkedValue = (String) content; break;
				case 10: checkedValue = (String) content; type = "text"; break;
				case 2: checkedValue = (String) content; type = "n-tuple"; break;
				case 3: checkedValue = (String) content; type = "date"; break; 
				case 4: checkedValue = (String) content; type = "time"; break;
				case 5: checkedValue = Integer.parseInt((String) content); type = "int"; break;
				case 55: checkedValue = Float.parseFloat((String) content); type = "float"; break;
				case 6: checkedValue = Boolean.parseBoolean((String) content); type = "boolean"; break;
				case 7: checkedValue = (String) content; type = "datetime"; break;
				}
			}
			initialize(checkedValue, unit, uncertainty, type, filename, definition, reference, encoder, checksum);
		}
		catch (Exception e){
			logger.error("! error during datacheck: ",e);
		}
	}
	/**
	 * 
	 * @param content Object
	 * @param unit String
	 * @param uncertainty Object
	 * @param type String
	 * @param filename String
	 * @param definition String
	 * @param reference String
	 * @throws Exception
	 */
	private void initialize(Object content, String unit, Object uncertainty, String type, String filename, 
			String definition, String reference, String encoder, String checksum)throws Exception{
		if(type == null){
			throw new Exception("Could not create Value! 'type' must not be null or empty!");
		}
		else if(type.isEmpty()){
			throw new Exception("Could not create Value! 'type' must not be null or empty!");
		}
		this.content 			= new Object();
		this.uncertainty 		= new Object();
		this.filename 	= new String();
		this.definition			= new String();
		this.reference					= new String();
		//*** value stuff
		if(type.equalsIgnoreCase("binary")){
			BASE64Encoder enc = new BASE64Encoder();
			//the value has to be converted to String; if it is already just take it, if it is not
			//try different things 
			if(content instanceof File){
				this.content = enc.encode(getBytesFromFile((File)content));
			}
			else if (content instanceof URI){
				try{
					File tempFile = new File((URI)content);
					this.content = enc.encode(getBytesFromFile(tempFile));
				}catch (Exception e) {
					logger.error("", e);
					throw e;
				}
			}
			else if(content instanceof URL){
				try{
					File tempFile = new File(((URL)content).toURI());
					this.content = enc.encode(getBytesFromFile(tempFile));
				}catch (Exception e) {
					logger.error("", e);
					throw e;
				}
			}
			else if(content instanceof String){
				this.content = content;						
			}
			else{
				throw new Exception("Base64 encoding of the binary value failed! If you want to add a binar " +
				"to the property it must be either an already encoded String, a File, an URI or URL.");
			}
		}
		else{
			this.content = content;	
		}
		//*** uncertainty
		if(uncertainty == null){	this.uncertainty = "";	}
		else{
			try{	this.uncertainty = uncertainty;	}
			catch (Exception e) {
				this.uncertainty = "";
				logger.error("", e);	
			}
		}
		//*** filename
		if(filename == null){
			if(type.equalsIgnoreCase("binary")){	this.filename = "";	}
			else{	this.filename = "";	}
		}
		else{
			try{	this.filename = filename;	}
			catch (Exception e) {
				this.filename = "";
				logger.error("", e);	
			}
		}
		//*** definition	
		if(definition == null){	this.definition = "";	}
		else{
			try{	this.definition = definition;	}
			catch (Exception e) {
				this.definition = "";
				logger.error("", e);	
			}
		}
		//*** reference
		if(reference == null){	this.reference ="";	}
		else{	this.reference = reference;	}
		//*** unit
		if(unit == null){	this.unit ="";	}
		else{	this.unit = unit;	}
		//*** type
		if(type == null){	this.type = "string";	}
		else{	this.type = type;	}
		//*** encoder
		if(encoder == null){	this.encoder = "";	}
		//*** checksum
		if(checksum == null){	this.checksum= "";	}
		
	}
	/**
	 * Returns whether or not a {@link Value} is empty.
	 * @return {@link Boolean}: true if value is empty, false otherwise.
	 */
	public boolean isEmpty(){
		return (content == null) || (content!= null && content instanceof String && ((String)content).isEmpty()); 
	}
	//*****************************************************************
	//**************	additional methods for constructor  ***********	
	//*****************************************************************	
	/**
	 * Checks the passed values class and returns the odML type.
	 * @param value {@link Object} the value;
	 * @return {@link String} the type under which odml refers to it.
	 */
	private String inferType(Object value){
		if(value instanceof String){
			return "string";
		}
		else if(value instanceof Integer){
			return "int";
		}
		else if(value instanceof Boolean){
			return "boolean";
		}
		else if(value instanceof Date){ // TODO date can be date, time or datetime > use SimpleDateFormat?!
			//			int caseNumber = checkStringsforDatatype((String) value);
			//			switch(caseNumber){
			//			case 3: return "date";
			//			case 4: return "time";
			//			case 7: return "datetime";
			//			}
			return "date";
		}
		else if(value instanceof Float){
			return "float";
		}
		else if(value instanceof Double){
			return "float";
		}
		else if(value instanceof URL){
			return "url";
		}
		else if(value instanceof File){
			return "binary";
		}
		else if(value instanceof Date){
			return "date";
		}
		else{
			return "string";
		}
	}
	/**
	 * Checks the datatype  of the passed values and puts them into the checkedValues Vector.
	 * @param content Object 
	 * @param type String
	 * @throws Exception
	 */
	public static Object checkDatatype(Object content, String type)
	throws Exception {
		logger.debug("entered method:\t'typeCheck'");
		if (content == null || content.toString().isEmpty()){
			logger.debug("no typecheck performed as value is null/empty > can/should only occur for terminologies!");
			return content;
		}
		// check for int
		if (type.matches("(?i)int.*"))
		{
			logger.debug("type specified:\tint");
			if (content instanceof java.lang.Integer){	
				logger.debug("value of class:\tint \t> correct");
				return content;
			}
			// integer could be masked as string
			else if (content instanceof java.lang.String){
				int checked = Integer.parseInt((String) content);		
				logger.debug("value of class String:\tparsed to int \t> correct");
				return checked;
			}
			else if (content instanceof java.lang.Double){	
				double checked = ((Double)content).intValue();
				logger.debug("value of class String:\tparsed to Double \t> correct");
				return checked;
			}
			else if (content instanceof java.lang.Float){					
				float checked = ((Float)content).intValue();
				logger.debug("value of class String:\tparsed to Float \t> correct");
				return checked;
			}
			else {
				throw new WrongTypeException("Int expected, not "+content.getClass());
			}
		}

		// check for float
		else if (type.matches("(?i)float.*"))
		{
			logger.debug("type specified:\tfloat");
			if (content instanceof java.lang.Float){
				logger.debug("value of class:\tfloat \t> correct");	
				return content;
			}
			else if (content instanceof java.lang.Double){
				double checked = ((Double)content).floatValue(); 
				logger.debug("value of class:\tdouble \t> correct");	
				return checked;
			}
			// float could be masked as string
			else if (content instanceof java.lang.String){
				float checked = Float.parseFloat((String) content);	
				logger.debug("value of class String:\tparsed to float \t> correct");
				return checked;
			}
			else {
				logger.warn("Float expected, not "+content.getClass());
				throw new WrongTypeException("Float expected, not "+content.getClass());
			}
		}			

		// check for string (string = oneWord in this case)!
		else if (type.matches("(?i)string"))
		{
			logger.debug("type specified:\tstring");

			if (!(content instanceof java.lang.String) && !(content instanceof java.lang.Character)){	
				throw new WrongTypeException("java.lang.String expected, not "+content.getClass());
			}
			// else string, but could contain everything > case0 is string in checkStringsforDatatype(..)
			else {
				logger.debug("value of class:\tString");
				int checkNumber;
				if(content instanceof java.lang.Character){
					checkNumber = checkStringsforDatatype(((Character)content).toString());
				}
				else{
					checkNumber = checkStringsforDatatype((String) content);
				}
				switch (checkNumber){
				case 0: // string > ok
					logger.debug("checked String = string\t> correct");	
					return content;
				case 10:
					logger.debug("checked String = string (text)\t> correct");	
					return content;
					// strict definition: text contains whitespaces, string not..
					// throw new WrongTypeException("'string' expected, not 'text': "+values.get(i));

					// explicit errors
				case 2:
					logger.warn("'string' expected, not 'n-tuple': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'n-tuple': "+values.get(i));
				case 3:
					logger.warn("'string' expected, not 'date': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'date': "+values.get(i));
				case 4:
					logger.warn("'string' expected, not 'time': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'time': "+values.get(i));
				case 5:
					logger.warn("'string' expected, not 'int': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'int': "+values.get(i));
				case 55:
					logger.warn("'string' expected, not 'float': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'float': "+values.get(i));
				case 6:
					logger.warn("'string' expected, not 'boolean': "+content+"; added as string (respecting user-demand)");
					return content;
					//						throw new WrongTypeException("'string' expected, not 'boolean': "+values.get(i));

				default: 
					throw new WrongTypeException("'string' expected, got '"+content+"' as input");
				}
			}
		}

		// check for text = string containing newlines, blanks...
		else if (type.matches("(?i)text"))
		{			
			logger.debug("type specified:\tstring");
			if (!(content instanceof java.lang.String)){
				throw new WrongTypeException("java.lang.String expected, not "+content.getClass());
			}
			// else string, but could contain everything > case1 is text in checkStringsforDatatype(..)
			else {
				logger.debug("value of class:\tString");

				int checkNumber = checkStringsforDatatype((String) content);
				switch (checkNumber){
				case 10:
					logger.debug("checked String = text\t> correct");	
					return content;
				case 0:
					logger.debug("checked String = text (string)\t> correct");
					return content;
					// strict definition: text contains whitespaces, stirng not..
					// throw new WrongTypeException("'text' expected, not 'string'");

					// explicit errors
				case 2:
					throw new WrongTypeException("'text' expected, not 'n-tuple': "+content);
				case 3:
					throw new WrongTypeException("'text' expected, not 'date': "+content);
				case 4:
					throw new WrongTypeException("'text' expected, not 'time': "+content);
				case 5:
					throw new WrongTypeException("'text' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'text' expected, not 'float': "+content);
				case 6:
					throw new WrongTypeException("'text' expected, not 'boolean': "+content);

				default: 
					throw new WrongTypeException("'text' expected, got '"+content+"' as input");						
				}
			}
		}

		// check for n-tuple (format DIGITSxDIGITS)
		else if (type.matches("(?i)n-tuple"))
		{
			logger.debug("type specified:\tn-tuple");
			if (!(content instanceof java.lang.String)){
				throw new WrongTypeException("java.lang.String expected, not "+content.getClass());
			}
			// else string, but could contain everything > case2 is n-tuple in checkStringsforDatatype(..)
			else {
				logger.debug("value of class:\tString");

				int checkNumber = checkStringsforDatatype((String) content);
				switch (checkNumber){
				case 2:
					logger.debug("checked String:\tparsed to n-tuple\t> correct");
					return content;

					// explicit errors
				case 0:
					throw new WrongTypeException("'n-tuple' expected, not 'string': "+content);
				case 10:
					throw new WrongTypeException("'n-tuple' expected, not 'text': "+content);
				case 3:
					throw new WrongTypeException("'n-tuple' expected, not 'date': "+content);
				case 4:
					throw new WrongTypeException("'n-tuple' expected, not 'time': "+content);
				case 5:
					throw new WrongTypeException("'n-tuple' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'n-tuple' expected, not 'float': "+content);
				case 6:
					throw new WrongTypeException("'n-tuple' expected, not 'boolean': "+content);

				default: 
					throw new WrongTypeException("'n-tuple' expected, got '"+content+"' as input");
				}				
			}
		}

		// check for date (format yyyy-mm-dd)
		else if (type.matches("(?i)date"))
		{
			logger.debug("type specified:\tdate");
			//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); //TODO

			if (content instanceof java.util.Date){
				logger.debug("value of class:\tdate\t> correct");
				return content;
			}
			// else string, but could contain everything > case3 is date in checkStringsforDatatype(..)
			else if (content instanceof java.lang.String){

				logger.debug("value of class:\tString");
				int checkNumber = checkStringsforDatatype((String) content);

				switch (checkNumber){
				case 3: 
					logger.debug("checked String:\tparsed to date\t> correct");
					return content;

					// explicit errors
				case 0:
					throw new WrongTypeException("'date' expected, not 'string': "+content);
				case 10:
					throw new WrongTypeException("'date' expected, not 'text': "+content);
				case 2:
					throw new WrongTypeException("'date' expected, not 'n-tuple': "+content);
				case 4:
					throw new WrongTypeException("'date' expected, not 'time': "+content);
				case 5:
					throw new WrongTypeException("'date' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'date' expected, not 'float': "+content);
				case 6:
					throw new WrongTypeException("'date' expected, not 'boolean': "+content);

				default: 
					throw new WrongTypeException("'date' expected, got '"+content+"' as input");
				}
			}
			else {
				throw new WrongTypeException("java.util.Date or java.lang.String expected, not "+content.getClass());
			}
		}

		// check for time (format hh:mm:ss)
		else if (type.matches("(?i)time"))
		{
			logger.debug("type specified:\ttime");
			//				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

			if (content instanceof java.util.Date){
				logger.debug("value of class:\tdate:\tfor time\t> correct");
				return content;
			}

			// else string, but could contain everything > case4 is time in checkStringsforDatatype(..)
			else if (content instanceof java.lang.String){
				logger.debug("value of class:\tString");
				int checkNumber = checkStringsforDatatype((String) content);

				switch (checkNumber){
				case 4:
					logger.debug("checked String:\tparsed to time\t> correct");
					return content;

					// explicit errors
				case 0:
					throw new WrongTypeException("'time' expected, not 'string': "+content);
				case 10:
					throw new WrongTypeException("'time' expected, not 'text': "+content);
				case 2:
					throw new WrongTypeException("'time' expected, not 'n-tuple': "+content);
				case 3:
					throw new WrongTypeException("'time' expected, not 'date': "+content);
				case 5:
					throw new WrongTypeException("'time' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'time' expected, not 'float': "+content);
				case 6:
					throw new WrongTypeException("'time' expected, not 'boolean': "+content);

				default: 
					throw new WrongTypeException("'time' expected, got '"+content+"' as input");
				}
			}
			else {
				throw new WrongTypeException("java.util.Date (for 'time') or java.lang.String expected, not "+content.getClass());
			}
		}

		// check for boolean
		else if (type.matches("(?i)bool.*"))
		{
			logger.debug("type specified:\tbool");
			if (content instanceof java.lang.Boolean){
				logger.debug("value of class:\tboolean\t> correct");
				return content;
			}
			// bool could be masked as string > case5 in checkStringsforDatatype(..) would be bool
			else if (content instanceof java.lang.String){
				logger.debug("value of class:\tString");
				//added by jan to catch empty value problems
				int checkNumber = checkStringsforDatatype((String) content);
				boolean checked;

				switch (checkNumber){
				case 6: 
					logger.debug("checked String:\tparsed to boolean\t> correct");
					if (((String) content).matches("(true)|1")){
						checked = true;
						return checked;
					}
					else {
						checked = false;
						return checked;
					}

					// explicit errors
				case 0:
					throw new WrongTypeException("'bool' expected, not 'string': "+content);
				case 10:
					throw new WrongTypeException("'bool' expected, not 'text': "+content);
				case 2:
					throw new WrongTypeException("'bool' expected, not 'n-tuple': "+content);
				case 3:
					throw new WrongTypeException("'bool' expected, not 'date': "+content);
				case 4:
					throw new WrongTypeException("'bool' expected, not 'time': "+content);
				case 5:
					throw new WrongTypeException("'bool' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'bool' expected, not 'float': "+content);

				default: 
					throw new WrongTypeException("'bool' expected, got '"+content+"' as input");

				}
			}
			else {
				throw new WrongTypeException("'bool' expected, not "+content.getClass());
			}
		}

		// check for URL
		else if (type.matches("(?i)URL"))
		{
			logger.debug("type specified:\tURL");
			if (content instanceof java.net.URL){
				logger.debug("value of class:\tURL\t> correct");
				return content;
			}
			else if (content instanceof java.lang.String){
				logger.debug("value of class:\tString");
				try{
					@SuppressWarnings("unused")
					URL parsedURL = new URL((String) content);
					logger.debug("checked String:\tparsed to URL\t> correct");
					return content;
				}
				catch(MalformedURLException e){
					logger.error("checked String"+content+": is not a valid URL!");
					return content;
				}
			}
			else {
				throw new WrongTypeException("URL expected, not "+content.getClass());
			}
		}

		else if (type.matches("(?i)binary"))
		{
			logger.debug("type specified:\tbinary");
			if (!(content instanceof java.lang.String)){
				throw new WrongTypeException("Binary (String) expected, not "+content.getClass());
			}
			else { 
				logger.debug("value of class:\tString\t> correct for binary");
				return content;
			}				
		}

		// check for persons = string
		else if (type.matches("(?i)person"))
		{
			logger.debug("type specified:\tperson");
			if (!(content instanceof java.lang.String)){	
				throw new WrongTypeException("java.lang.String expected, not "+content.getClass());
			}
			else {
				logger.debug("value of class:\tString:\tfor person\t> correct");
				return content;
			}
		}

		// check for datetime (format yyyy-MM-dd HH:mm:ss)
		else if (type.matches("(?i)datetime"))
		{
			logger.debug("type specified:\tdatetime");
			//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (content instanceof java.util.Date){
				logger.debug("value of class:\tdate:\tfor datetime\t> correct");
				return content; 
			}
			// else string, but could contain everything > case7 is datetime in checkStringsforDatatype(..)
			else if (content instanceof java.lang.String){
				logger.debug("value of class:\tString");
				int checkNumber = checkStringsforDatatype((String) content);

				switch (checkNumber){
				case 7: 
					logger.debug("checked String:\tparsed to datetime\t> correct");
					return content;

					// explicit errors
				case 0:
					throw new WrongTypeException("'datetime' expected, not 'string': "+content);
				case 10:
					throw new WrongTypeException("'datetime' expected, not 'text': "+content);
				case 2:
					throw new WrongTypeException("'datetime' expected, not 'n-tuple': "+content);
				case 3:
					throw new WrongTypeException("'datetime' expected, not 'date': "+content);
				case 4:
					throw new WrongTypeException("'datetime' expected, not 'time': "+content);
				case 5:
					throw new WrongTypeException("'datetime' expected, not 'int': "+content);
				case 55:
					throw new WrongTypeException("'datetime' expected, not 'float': "+content);
				case 6:
					throw new WrongTypeException("'datetime' expected, not 'boolean': "+content);

				default: 
					throw new WrongTypeException("'datetime' expected, got '"+content+"' as input");
				}
			}

			else {
				throw new WrongTypeException("java.util.Date (for 'datetime') or java.lang.String expected, not "+content.getClass());
			}
		}


		// all cases checked > unknown type specified!
		else {
			// type not supported > handled as String
			type = "string";
			logger.warn("type unknown:\thandling as 'string':\tcorrect");
			return content;
			// more strict: there are no more types!
			// throw new WrongTypeException("Type '"+type+"' specified doesn't exist!");
		}			
	}
	/**
	 * Checks one String Object more in detail, could be
	 * string (oneWord): 		caseNumber 0
	 * text (more words&lines): caseNumber 10
	 * n-tuple (DIGIT;DIGIT): 	caseNumber 2
	 * date (yyyy-mm-dd):		caseNumber 3
	 * time (HH:mm:ss):			caseNumber 4
	 * integer:					caseNumber 5
	 * float:					caseNumber 55
	 * boolean:					caseNumber 6
	 * datetime (yyyy-mm-dd HH:mm:ss): caseNumber 7 (3+4 ;)
	 * @param content {@link String}
	 * @return {@link Integer}: the caseNumber for the String
	 */
	protected static int checkStringsforDatatype(String content)
	{
		content = content.trim();
		int caseNumber = 0; // by default string = oneWord
		// case 10: theContent has whitespaces in it > 'text'

		// pattern matchers for strings

		// case 2: for n-tuple: format DIGITSxDIGITS
		String regExNTuple = "(?i)[0-9]{1,};[0-9]{1,}";

		/* case 3: for date: 
		 * max. 12 for months allowed
		 * max. 31 for days allowed
		 * ensuring that February has max 29 days (not checking for leap years...)
		 */
		String regExDate = "[0-9]{4}-(((([0][13-9])|([1][0-2]))-(([0-2][0-9])|([3][01])))|(([0][2]-[0-2][0-9])))";
		String regExDateGeneral = "[0-9]{4}-[0-9]{2}-[0-9]{2}";

		/* case 4: for time:
		 * max 24 hours
		 * max 60 min
		 * max 60 seconds
		 */
		String regExTime = "(([01][0-9])|([2][0-4])):(([0-5][0-9])|([6][0])):(([0-5][0-9])|([6][0]))"; // for time
		String regExTimeGeneral = "[0-9]{2}:[0-9]{2}:[0-9]{2}";

		// case 5: for int:
		// possibility for signs +- followed by at least one digit. nothing else can be in the pattern (i.e.  !
		String regExInt = "^[+-]?[0-9]+$";
		// case 55: for float:
		// possibility for signs +- maybe followed by digits, then must have a '.', 
		// then must be followed by at least one digit. nothing else can be in the pattern!
		String regExFloat = "^[+-]?[0-9]*\\.[0-9]+$";

		// case 6: for bool:
		String regExBool = "(true)|(false)|1|0";

		// String regExURL = "";	// for URL

		//case 7: for datetime
		String regExDatetimeGeneral ="[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}";


		if (content.matches(regExNTuple)){
			caseNumber = 2;
			logger.debug("checkStringsForDatatype:\tfound 'n-tuplet'");
		}
		else if (content.matches(regExDateGeneral)){
			if (content.matches(regExDate)){
				caseNumber = 3;	
				logger.debug("checkStringsForDatatype:\tfound 'date'");
			}
			else { // (6)66 eeevvvill! instead of first 6 > 3 for Date
				caseNumber = 366;
				logger.info("checkStringsForDatatype:\tfound 'date'-like thing: "+content);
			}
			if (content.matches(regExDatetimeGeneral)){
				caseNumber = 7;
				logger.debug("checkStringsForDatatype:\tfound 'datetime'");
			}
		}
		else if (content.matches(regExTimeGeneral)){
			if (content.matches(regExTime)){
				caseNumber = 4;	
				logger.debug("checkStringsForDatatype:\tfound 'time'");
			}
			else { // (6)66 eeevvvill! instead of first 6 > 4 for Time
				caseNumber = 466;
				logger.info("checkStringsForDatatype:\tfound 'time'-like thing: "+content);
			}
			if (content.matches(regExDatetimeGeneral)){
				caseNumber = 7;
				logger.debug("checkStringsForDatatype:\tfound 'datetime'");
			}
		}
		else if (content.matches(regExInt)){
			caseNumber = 5;
			logger.debug("checkStringsForDatatype:\tfound 'int'");
		}
		else if (content.matches(regExFloat)){
			caseNumber = 55;
			logger.debug("checkStringsForDatatype:\tfound 'float'");
		}
		else if (content.matches(regExBool)){
			caseNumber = 6;
			logger.debug("checkStringsForDatatype:\tfound 'bool'");
		}
		else if (content.matches(regExDatetimeGeneral)){
			caseNumber = 7;
			logger.debug("checkStringsForDatatype:\tfound 'datetime'");
		}
		else if (content.contains(" ")){
			caseNumber = 10;
			logger.debug("checkStringsForDatatype:\tfound 'text'");
		} 		
		return caseNumber;
	}
	//***************************************************************************************
	//*****					methods to handle binary content					**********
	//***************************************************************************************
	/**
	 * Function to convert the content of the indicated file to an array of bytes.
	 * Is primarily for internal use to Base64 encode binary data. 
	 * @param file {@link File}: the file to convert.
	 * @return byte[]: the array of bytes contained in the file.
	 * @throws IOException
	 */
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		//Get the size of the file
		long length = file.length();
		//ensure that the file not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			throw new IOException("File exceeds max value: "+ Integer.MAX_VALUE);
		}
		//Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];
		//Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && 
				(numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		//Ensure all the bytes have been read
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file"+ file.getName());
		}
		//Close stream
		in.close();
		return bytes;
	}
	
	/**
	 * Writes the value content which is Base64 encoded to disc.
	 * @param content {@link String}: the value content.
	 * @param outFile {@link File}: the File into which the decoded content should be written
	 */
	public static void writeBinaryToDisc(String content, File outFile) throws Exception{
		if(outFile==null){
			throw new Exception("Argument outFile not specified!");
		}
		//create the outputStream
		FileOutputStream os = null;
		try{
			os = new FileOutputStream(outFile);
		}
		catch (Exception e) {
			logger.error("",e);
			throw e;
		}
		//create the decoder
		BASE64Decoder decoder = new BASE64Decoder();
		//decode the content
		byte[] bytes = decoder.decodeBuffer(content);
		//write bytes to disc
		os.write(bytes);
		os.flush();
		os.close();
	}
	//****************************************************************
	//*****					getter & setter					**********
	//****************************************************************
	// associated property (so to say dad in tree)
	protected void setAssociatedProperty(Property property){
		this.associatedProperty = property;
	}
	protected Property getAssociatedProperty(){
		return this.associatedProperty;
	}
	// value
	protected void setContent(Object content){
		this.content = content;
	}
	protected Object getContent(){
		return this.content;
	}
	// unit
	protected void setUnit(String unit){
		this.unit = unit;
	}
	protected String getUnit(){
		return this.unit;
	}
	// uncertainty
	protected void setUncertainty(Object uncertainty){
		this.uncertainty = uncertainty;
	}
	protected Object getUncertainty(){
		return this.uncertainty;
	}
	// type
	protected void setType(String type){
		this.type = type;
	}
	protected String getType(){
		return this.type;
	}
	// Filename
	protected void setFilename(String filename){
		this.filename = filename;
	}
	protected String getFilename(){
		return this.filename;
	}
	// definition
	protected void setDefinition(String comment){
		this.definition = comment;
	}
	protected String getDefinition(){
		return this.definition;
	}
	// reference
	protected void setReference(String reference){
		this.reference = reference;
	}
	protected String getReference(){
		return this.reference;
	}
	// encoder
	protected void setEncoder(String encoder){
		this.encoder = encoder;
	}
	protected String getEncoder(){
		return this.encoder;
	}
	// checksum
	protected void setChecksum(String checksum){
		this.checksum = checksum;
	}
	protected String getChecksum(){
		return this.checksum;
	}
	
	public void compareToTerminology(Property termProp){
		if(this.type != null && !this.type.isEmpty()){
			if(!this.type.equalsIgnoreCase(termProp.getType())){
				logger.warn("Value type ("+this.type+") does not match the one given in the terminology("+termProp.getType()
						+")! To guarantee interoperability please ckeck. However, kept provided type.");
			}
		}
		else{
			try {
				checkDatatype(this.content, termProp.getType());
				this.setType(termProp.getType());
				logger.info("Added type information to value.");
			} catch (Exception e) {
				logger.warn("Value is not compatible with the type information the terminology suggests ("+termProp.getType()+"). Did nothing but please check");
			}
		}
		if(this.unit != null && !this.unit.isEmpty()){
			if(!this.unit.equalsIgnoreCase(termProp.getUnit(0))){
				logger.warn("Value unit ("+this.unit+") does not match the one given in the terminology("+termProp.getUnit()
						+")! To guarantee interoperability please ckeck. However, kept provided unit.");
			}
		}
		else{
			if(termProp.getUnit() != null && !termProp.getUnit(0).isEmpty()){
				this.setUnit(termProp.getUnit(0));
				logger.info("Added unit "+termProp.getUnit()+" information to value.");
			}
		}
	}
	/**
	 * TODO
	 * Compares the content of two values and returns whether they are equal. So far this
	 * concerns only the value content. Not type,definition etc.
	 * @param other
	 * @return {@link Boolean} <b>true</b> if the content of two values matches, <b>false</b> otherwise.
	 */
	public boolean isEqual(Value other){
		if(this.content.toString()!= other.content.toString()){
			return false;
		}
		return true;
	}
	//****************************************************************
	//*****					Overrides for TreeNode			**********
	//****************************************************************
	@Override
	public Enumeration<TreeNode> children() {
		return null;
	}
	@Override
	public boolean getAllowsChildren() {
		return false;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		return null;
	}
	@Override
	public int getChildCount() {
		return 0;
	}
	@Override
	public int getIndex(TreeNode node) {
		return 0;
	}
	@Override
	public TreeNode getParent() {
		return this.getAssociatedProperty();
	}
	@Override
	public boolean isLeaf() {
		return true;
	}
	@Override
	public String toString() {
		return this.getContent().toString();
	}
}
