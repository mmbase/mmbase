/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.io.*;
import java.util.*;
/**
* This is a flexible Properties version, it can handle saving of Properties with
* the comments that will stay in your file.   
* @author Jan van Oosterom
* @version 28-Oct-1996
*/
public class ExtendedProperties extends Properties
{
	/**
	* the prefix of the comment in the Properties file.
	* everithing after it will be treaded as comment
	*/
	protected String commentPrefix = "#";
	

    public ExtendedProperties() {
	}

	/**
	* Create and read an ExtendedProperty from file.
	* @param filename The file from were to read.
	*/
    public ExtendedProperties(String filename) {
		try {
			getProps(filename);  
		} catch (IOException e) {
			System.out.println("Failed to load the ExtendedProperties for: "+ filename);	
		}
    }

	/**
	* Create an ExtendedProperties with a Allready filled ExtendedProperty list.
	* @param exProp The list that will be put in this ExtendedProperty. 
	*/	
	public ExtendedProperties(ExtendedProperties exProp) {
        super((Properties) exProp);
    }


	/**
	* Read from Properties and return them.
	* @param filename The file from were to read the Properties.
	*/
    public Hashtable readProperties(String filename) {
		clear();
		try {
			getProps(filename);  
		} catch (IOException e) {
			System.out.println("Failed to load the ExtendedProperties from: "+ filename);	
		}
		ExtendedProperties propsToReturn = new ExtendedProperties();
		Enumeration e = keys();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			propsToReturn.put(s,get(s));
		}
		return (Hashtable) propsToReturn;
    }

	/**
	* save Properties to file.
	* @param filename The File were to save them
	* @param propsToSave The Properties which to save.
	*/
	public synchronized void saveProperties(String filename, Hashtable propsToSave) {
		clear();
		Enumeration e = propsToSave.keys();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			put(s,propsToSave.get(s));//ROB
		}		
		try {
			save(filename);
		} catch (IOException ioe) {	
			System.out.println("Fail to save the ExtendedProperties to: " + filename+" : "+ioe);
		}
	}

	/**
	* return a Vector of Strings which is parsed from a specified Property. 
	* @param whichProp The Property to get the list from.
	* @param delimeter The delimeter to split wichProp's value with.
	*/
    public Vector getPropertyValues(String whichProp, String delimeter) {
		Vector parsedPropsToReturn = new Vector();
		if (containsKey(whichProp)) {
			//whichProp is available in this Property list
			String value = (String) get(whichProp);
			StringTokenizer tok = new StringTokenizer(value,delimeter);
			while (tok.hasMoreTokens()) {
				parsedPropsToReturn.addElement(tok.nextToken());	
			}
			return parsedPropsToReturn;
		} else {
			//whichProp is not available in this Property list
			// ROB UIT GEZET
			//System.out.println("ExtendedProperties.getParsedProperty: " + whichProp + " not found." );
			return null;
		}	
	}	

	/**
	* Read to this Property, the Properties from a file.
	* @param filename The file were to read from
	*/
    public void getProps(String filename) throws IOException {
		try {
			FileInputStream fileInputStream = new FileInputStream(filename);
        	BufferedInputStream bufferedInputStream = new BufferedInputStream (fileInputStream);
        	load(bufferedInputStream);
        	bufferedInputStream.close();
		} catch (FileNotFoundException e ) {
			// ROB UIT GEZET
			//System.out.println("ExtendedProperties:: file " + filename + " niet gevonden");
		}
    }
	

    /**
     * Loads properties from an InputStream.
	 * Uses "=" as delimeter between key and value.
	 * Does <B> not </B> uses a ":" as delimiter!
     * @param in the input stream
     * @exception IOException Error when reading from input stream.
     */
    public synchronized void load(InputStream in) throws IOException {
		in = Runtime.getRuntime().getLocalizedInputStream(in);
	
		int ch = in.read();
		while (true) {
		    switch (ch) {
		      case -1:
				return;
	
		      case '#':
		      case '!':
				do {
				    ch = in.read();
				} while ((ch >= 0) && (ch != '\n') && (ch != '\r'));
				continue;
	
		      case '\n':
		      case '\r':
		      case ' ':
		      case '\t':
				ch = in.read();
				continue;
		    }
	
		    // Read the key
		    StringBuffer key = new StringBuffer();
		    
			while ((ch >= 0) && (ch != '=') && (ch != ' ') && (ch != '\t') && (ch != '\n') && (ch != '\r')) {
				key.append((char)ch);
				ch = in.read();
		    }
		    while ((ch == ' ') && (ch == '\t')) {
				ch = in.read();
		    }
		    if ((ch == '=') || (ch == ':')) {
				ch = in.read();
		    }
		    while ((ch == ' ') && (ch == '\t')) {
				ch = in.read();
		    }
	
		    // Read the value
		    StringBuffer val = new StringBuffer();
		    while ((ch >= 0) && (ch != '\n') && (ch != '\r')) {
				if (ch == '\\') {
				    switch (ch = in.read()) {
				      case '\r':
					if (((ch = in.read()) == '\n') ||
					    (ch == ' ') || (ch == '\t')) {
					  // fall thru to '\n' case
					} else continue;
				      case '\n': 
					while (((ch = in.read()) == ' ') || (ch == '\t'));
					continue;
				      case 't': ch = '\t'; break;
				      case 'n': ch = '\n'; break;
				      case 'r': ch = '\r'; break;
				      case 'u': {
					while ((ch = in.read()) == 'u');
					int d = 0;
				      loop:
					for (int i = 0 ; i < 4 ; i++, ch = in.read()) {
					    switch (ch) {
					      case '0': case '1': case '2': case '3': case '4':
					      case '5': case '6': case '7': case '8': case '9':
						d = (d << 4) + ch - '0';
						break;
					      case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
						d = (d << 4) + 10 + ch - 'a';
						break;
					      case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
						d = (d << 4) + 10 + ch - 'A';
						break;
					      default:
						break loop;
					    }	
					}
					ch = d;
				      }
				    }
				}
				val.append((char)ch);
				ch = in.read();
		    }
		    put(key.toString(), val.toString());
		}
    }

	/**
	* Warning this routine destroys your comments in your properties file.
	* But it save's your Properties (If that is all that U want :-)
	* @param filename The File were to save your Properties
	* @param header If you want you can speciefy a header (on top of you file)
	*/   
	 private void write (String filename, String header) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(filename);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        save(bufferedOutputStream, header);
        bufferedOutputStream.close();
    }


	/**
	* This routine does not only saves your Properties but also  
	* merges your comments if the file already exists 
	* if the file doesn't exists the will call the "normal" write routine.
	* @param filename The file were to save to. 
	*/
	public synchronized void  save(String filename) throws IOException {
		File file = new File (filename);
		boolean des = false;     // for skipping enters when destroying some info
		if (file.exists()) {
			ExtendedProperties copyOfProps = (ExtendedProperties) this.clone(); 
			//the file exists so we have to merge
			String newlines = "";
			String lines = readOldProps(file);

			String comment = "";

			StringTokenizer tok = new StringTokenizer(lines,"\n",true);
			while(tok.hasMoreTokens()) {
				String line =(String) tok.nextElement();
				// Is it a comment
				if (line.startsWith(commentPrefix)) {
					newlines = newlines + line;
				} else {
					int index = line.indexOf('=');
					if (index == -1) {	
						//assuming a empty line ....
						if (!des) {
							newlines = newlines + "\n"; 	
						} else {
							des = false;
						}
					} else {
						//we found a new property value in the props file
						String name = line.substring(0,index);
						String value = line.substring(index+1,line.length());
		
						if (containsKey(name)) {
							//this Property is in memory so get this one from memory
							newlines = newlines + name + "=" + getProperty(name);
							//remove it from the copy
							copyOfProps.remove(name);
						} else {	
							//Thats odd, this one is not in memory 
							//Well we didn't used it so leave it there and use the old one
							des = true;	
					/*		newlines = newlines + line;*/
						}
					
					}
				}
			}
			
			//everything that is left in the copy should be written also:
			Enumeration e = copyOfProps.keys();
			while (e.hasMoreElements()) {	
				String name = (String) e.nextElement();
				newlines = newlines + "\n" + name + "=" + copyOfProps.getProperty(name);
			}		
			//newlines = newlines + "\n";	
	
			file.delete();
			//save the file:
			RandomAccessFile writefile = new RandomAccessFile(file,"rw");
			writefile.writeBytes(newlines);	
			writefile.close();
		} else {
			//the file does not exists so we can use the simple write routine
			//there is nothing to merge
			write(filename, "");
		}
	}

	/**
	* Read a file and returns everything in a String. 
	* @param file The File were to read from (has to exsists).
	*/
	private String readOldProps(File file) throws IOException {
			String line = "";	
		
			String lines = "";	
			RandomAccessFile readfile = new RandomAccessFile(file,"r");
      		do {
            	try 	{	
					line = readfile.readLine();
                	if (line != null) lines= lines + line + "\n";
                } 
				catch(Exception e) {
					System.out.println("EOF!");
				}
             } while (line != null);
		
			if (!lines.equals("")) {
				lines = lines.substring(0,lines.length()-1);
			}
         	readfile.close(); 
			return lines;
	}
	/**
	* Set the Property.
	* @param name the name of the Property (the part in front of the '=' in the Property file)
	* @param value the (new) value of the Property (the  part after the '=' in the Property file)
	*/
    public Object setProperty(String name, String value) {
        return(put(name, value));
    }
	/**
	*	Dump the contents of this Property to your screen (for debugging)
	*/
	public void showContents() {
		Enumeration names = propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
			System.out.println(name + "=" + getProperty(name));
		}
        
	}

    public synchronized String save() {
		StringBuffer b=new StringBuffer();
		b.append('#');
		b.append(new Date());
		b.append('\n');
		int ch;
	
		for (Enumeration e = keys() ; e.hasMoreElements() ;) {
		    String key = (String)e.nextElement();
		    b.append(key);
		    b.append('=');
	
		    String val = (String)get(key);
		    int len = val.length();
			if (len>0) b.append(val);
		    b.append('\n');
		}
		return(b.toString());
    }

    /**
     * Convert a nibble to a hex character
     * @param	nibble	the nibble to convert.
     */
    private static char toHex(int nibble) {
	return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static char[] hexDigit = {
	'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
