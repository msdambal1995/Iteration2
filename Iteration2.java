import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.JavaCore;

/**
 * The counter program implements an application that:
 * 1) takes a pathname to indicate a directory or JAR file of interest,
 * 2) finds the declarations of all types within that directory (recursively) or JAR file, and
 * 3) finds the references to all types within that directory (recursively) or JAR file
 * Extra Notes:
 *    Input directory or JAR file should be provided as a command line argument
 *    This program only works with directories and JAR files
 * @author Robert Fiker and Hamzah Umar.
 * @version 3.0
 */
public class Iteration2 {
	
	static String inputPathname;
	static HashMap<String, Integer> decDictionary = new HashMap<String, Integer>();
	static HashMap<String, Integer> refDictionary = new HashMap<String, Integer>();
	
	/**
	 * This is the main method
	 * @param args
	 * @return Nothing
	 * @throws IOException
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("You must enter exactly one command line argument.");
			System.exit(0);
		}
		inputPathname = args[0];
		try {
			findFiles(inputPathname);
		}
		catch(FileNotFoundException fnfe) {
			System.out.println("Sorry, pathname must indicate either an existing directory or an existing JAR file");
		}
		catch (IOException ioe){
			System.out.println("Error occred while retreiving directory/jar file. Please check your input");
		}
		System.out.println("Declarations: "+decDictionary.toString());
		System.out.println("References: "+refDictionary.toString());
	}
	
	/**
	 * Method which parses all Java files within a directory (recursively) or a JAR file
	 * @param pathname The directory or JAR file containing the .java files that need to be parsed
	 * @return Nothing
	 * @throws IOException
	 */
	public static void findFiles(String pathname) throws IOException{
		//check if input pathname is a directory
		File root = new File(pathname);
		if (root.isDirectory()) {
	        File[] list = root.listFiles();
	
	        if (list == null) {
	        	return;
	        }
	        
	        for (File f : list) {
	        	//if a directory is found within the original directory, recursively search the new directory
	            if (f.isDirectory()) {
	                findFiles(f.getAbsolutePath());
	            }
	            else {
	            	//only parse .java files
	            	if (f.getName().endsWith(".java")) {
	            		parse(readFileToString(f.getAbsolutePath()));
	            	}
	            }
	        } 
		}
		//if not, check if it is a JAR or ZIP file
		else if (pathname.endsWith(".jar") || pathname.endsWith(".zip")){
			ZipFile zipFile = new ZipFile(pathname);

		    Enumeration<? extends ZipEntry> entries = zipFile.entries();
		    
		    //make an ArrayList to store all .java zip entries
		    ArrayList<ZipEntry> myArray = new ArrayList<ZipEntry>();
		    while(entries.hasMoreElements()){
		        ZipEntry entry = entries.nextElement();
		        if (entry.getName().endsWith(".java")){
		        	myArray.add(entry);
		        }
		    }
		    
		    for (ZipEntry entry: myArray) {
				parse(readZipEntryToString(entry, zipFile));
		    }
		}
		//if not, print error message
		else {
			System.out.println("Sorry, pathname must indicate either an existing directory or an existing JAR file");
		}
	}
	
	/**
	 * Method which reads a file from normal directories, and converts it to a string
	 * @param filePath This indicates the file path of the file to be read
	 * @return String This returns the string that has been created
	 * @throws IOException
	 */
	public static String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
 
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return  fileData.toString();	
	}
	
	/**
	 * Method which reads a file from JARS and ZIPS, and converts it to a string
	 * @param entry This indicates the file that is to be read
	 * @param zFile This indicates the JAR or ZIP file that is being used
	 * @return String This returns the string that has been created
	 * @throws IOException
	 */
	public static String readZipEntryToString(ZipEntry entry, ZipFile zFile) throws IOException {
		InputStream stream = null;
		stream = zFile.getInputStream(entry);
        
    	BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(stream));
			while ((line = br.readLine()) != null) {
				sb.append(line+"\n");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * Main parsing method which creates an AST out of the input string
	 * Uses an ASTVisitor to visit nodes and count declarations and references
	 * @param str String which will be parsed
	 * @return Nothing
	 */
	public static void parse(String str){
		// create a new parser
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		
		// parser setup
		parser.setSource(str.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		
		parser.setCompilerOptions(options);
		parser.setEnvironment(null, null, null, true);
		
		String unitName = "";
		parser.setUnitName(unitName);
		
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
 
		// make the CompilationUnit accept a new AST visitor and overwrites relevant
		// visit methods
		cu.accept(new ASTVisitor() {
			
			// COUNT ANNOTATION DECLARATIONS
			public boolean visit(AnnotationTypeDeclaration node) {			
				String qualifiedName = node.resolveBinding().getQualifiedName();
				if (decDictionary.containsKey(qualifiedName)) {
					decDictionary.put(qualifiedName, decDictionary.get(qualifiedName)+1);	
				}	
				else {
					decDictionary.put(qualifiedName, 1);
				}
				return true;
			}
			
			// COUNT ENUMERATION DECLARATIONS
			public boolean visit(EnumDeclaration node) {												
				String qualifiedName = node.resolveBinding().getQualifiedName();
				if (decDictionary.containsKey(qualifiedName)) {
					decDictionary.put(qualifiedName, decDictionary.get(qualifiedName)+1);	
				}	
				else {
					decDictionary.put(qualifiedName, 1);
				}
				return true;
			}
		
			// COUNT CLASS AND INTERFACE DECLARATIONS
			public boolean visit(TypeDeclaration node) {
				String qualifiedName = node.resolveBinding().getQualifiedName();
				if (decDictionary.containsKey(qualifiedName)) {
					decDictionary.put(qualifiedName, decDictionary.get(qualifiedName)+1);	
				}	
				else {
					decDictionary.put(qualifiedName, 1);
				}
				return true;
			}
			
			// COUNT NORMAL ANNOTATION TYPE REFERENCES 
			public boolean visit (NormalAnnotation node) {
				String qualifiedName = node.resolveTypeBinding().getQualifiedName();
				if (refDictionary.containsKey(qualifiedName)) {
					refDictionary.put(qualifiedName, refDictionary.get(qualifiedName)+1);	
				}	
				else {
					refDictionary.put(qualifiedName, 1);
				}
				return true;
			}
			
			// COUNT MARKER ANNOTATION TYPE REFERENCES 
			public boolean visit (MarkerAnnotation node) {
				String qualifiedName = node.resolveTypeBinding().getQualifiedName();
				if (refDictionary.containsKey(qualifiedName)) {
					refDictionary.put(qualifiedName, refDictionary.get(qualifiedName)+1);	
				}	
				else {
					refDictionary.put(qualifiedName, 1);
				}
				return true;
			}

			// COUNT PRIMITIVE TYPE REFERENCES 
			public boolean visit(PrimitiveType node) {	
				String qualifiedName = node.resolveBinding().getQualifiedName();
				if (refDictionary.containsKey(qualifiedName)) {
					refDictionary.put(qualifiedName, refDictionary.get(qualifiedName)+1);	
				}	
				else {
					refDictionary.put(qualifiedName, 1);
				}
				return true;
			}
			
			// COUNT ALL OTHER TYPE REFERENCES 
			public boolean visit(SimpleType node) { 
				String qualifiedName = node.resolveBinding().getQualifiedName();
				if (refDictionary.containsKey(qualifiedName)) {
					refDictionary.put(qualifiedName, refDictionary.get(qualifiedName)+1);	
				}	
				else {
					refDictionary.put(qualifiedName, 1);
				}
				return true;
			}
		
		});
		
	}
	
	// Encapsulation methods
	public static String getInputDirectory() {
		return inputPathname;
	}
	
	public static void setInputDirectory(String inputDirectory) {
		Iteration2.inputPathname = inputDirectory;
	}
	
	public static HashMap<String, Integer> getRefDictionary(){
		return refDictionary;
	}
	
	public static HashMap<String, Integer> getDecDictionary(){
		return decDictionary;
	}
	
}
