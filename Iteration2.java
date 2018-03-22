import java.io.BufferedReader;
import java.io.File;
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
 * @author Robert Fiker and Hamzah Umar.
 * @version 2.0
 */
public class Iteration2 {
	
	static String inputType;
	static String inputDirectory;
	static int declarations = 0;
	static int references = 0;
	static HashMap<String, Integer> decDictionary = new HashMap<String, Integer>();
	static HashMap<String, Integer> refDictionary = new HashMap<String, Integer>();
	
	/**
	 * This is the main method
	 * @param args
	 * @return Nothing
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		inputDirectory = args[0];
		parseFilesInDir(inputDirectory);
		System.out.println("Declarations: "+decDictionary.toString());
		System.out.println("References: "+refDictionary.toString());
	}
	
	/**
	 * Method which parses all Java files within a directory (recursively)
	 * @param directory The directory containing the files that need to be parsed
	 * @return Nothing
	 * @throws IOException
	 */
	public static void parseFilesInDir(String directory) throws IOException{
		File root = new File(directory);
		InputStream stream = null;
		
		if (root.isDirectory()) {
	        File[] list = root.listFiles();
	
	        if (list == null) return;
	        
	        String filepath = null;
	
	        for (File f : list) {
	            if (f.isDirectory()) {
	                parseFilesInDir(f.getAbsolutePath());
	            }
	            else {
	            	filepath = f.getAbsolutePath();
	                parse(readFileToString(filepath));
	            }
	        }
		}
		else {
			System.out.println("Here");
			System.out.println(directory);
			ZipFile zipFile = new ZipFile(directory);

		    Enumeration<? extends ZipEntry> entries = zipFile.entries();
		    
		    ArrayList<ZipEntry> myArray = new ArrayList<ZipEntry>();
		    while(entries.hasMoreElements()){
		        ZipEntry entry = entries.nextElement();
		        String entryName = entry.getName();
		        if (entryName.endsWith(".java")){
		        	myArray.add(entry);
		        }
		    }
		    
		    for (ZipEntry entry: myArray) {
		    	
		    	System.out.println(entry.toString());
		        stream = zipFile.getInputStream(entry);
		        
		    	BufferedReader br = null;
				StringBuilder sb = new StringBuilder();

				String line;
				try {

					br = new BufferedReader(new InputStreamReader(stream));
					while ((line = br.readLine()) != null) {
						sb.append(line);
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

				parse(sb.toString());
		    }
		}

	}
	
	/**
	 * Method which reads a file, and converts it to a string
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
				return false;
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
	
	public static String getInputType() {
		return inputType;
	}

	public static void setInputType(String inputType) {
		Iteration2.inputType = inputType;
	}

	public static int getDeclarations() {
		return declarations;
	}

	public static void setDeclarations(int declarations) {
		Iteration2.declarations = declarations;
	}

	public static int getReferences() {
		return references;
	}

	public static void setReferences(int references) {
		Iteration2.references = references;
	}

	public static String getInputDirectory() {
		return inputDirectory;
	}
	
	public static void setInputDirectory(String inputDirectory) {
		Iteration2.inputDirectory = inputDirectory;
	}
	
}
