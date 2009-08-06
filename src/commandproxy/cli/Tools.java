package commandproxy.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import commandproxy.core.Constants;
import commandproxy.core.Log;

public class Tools {
	/**
	 * Prints information on how to use the commandproxy to the command line
	 */
	public static void printUsage(){
		System.out.println( 
			"Usage:\n" +
			"        commandproxy debug [-verbose]\n" + 
			"        commandproxy export windows|mac [-verbose] [-out=<out-file>] <air-file>\n" + 
			"\n" + 
			"\n" + 
			"Options: \n" + 
			"        -out:       Specify output file. For windows exports the .exe extension \n" + 
			"                    will be forced (for macintosh exports it's .dmg). \n" + 
			"                    By default <air-filename>-<version>.exe/dmg will be used. \n" +
			"                    \n" +
			"        -verbose:   Get more detailed output. This should be interresting only\n" +
			"                    if the build process fails\n"
		); 
	}
	
	
	
	/**
	 * Returns the home directory of the command proxy 
	 * installation
	 * 
	 * @return the home directory
	 */
	public static File getCommandProxyHome(){
		File home = new File( Main.class.getProtectionDomain().getCodeSource().getLocation().getFile() );
		
		// On windows spaces and other characters will be urlencode
		// (e.g. space=%20) 
		// Hm... we don't really want that to happen! 
		try {
			home = new File( URLDecoder.decode( home.getAbsolutePath(), "UTF-8" ) );
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		if( home.isDirectory() ){
			return home.getParentFile(); 
		}
		else{
			return home.getParentFile().getParentFile(); 
		}
	}
	
	/**
	 * Locates a file inside the command proxy directory
	 * @param path The path relative to the commandproxy home dir
	 * @return A file with the correct absolute path set
	 */
	public static File getCommandProxyFile( String path ){
		return new File( getCommandProxyHome(), path ); 
	}
	
	/**
	 * Fails with an error message, 
	 * and then exits
	 * 
	 * @param message The error message
	 * @param code The error code, for a list of codes see {@link Constants}
	 */
	public static void fail( String message, int code ){
		Log.error.println( "Error: " ); 
		Log.error.println( message ); 
		Log.error.println(); 
		
		System.exit( code );
	}
	
	/**
	 * Reads the Adobe Air Application descriptor and places some 
	 * attributes in a properties object
	 * This will only give you access to these properties: 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws ZipException 
	 */
	public static Hashtable<String, String> getAirConfig( File airFile ) throws ZipException, IOException, ParserConfigurationException, SAXException{
		Hashtable<String, String> conf = new Hashtable<String, String>(); 
		
		AirXML air = new AirXML( airFile );
		
		String[] interresting = new String[]{
				"version", 
				"filename", 
				"vendor", 
				"id", 
				"icon/image128x128"
		}; 
		
		for( String key : interresting ){
			conf.put( key, air.get( key ) ); 
		}
		
		if( "".equals( conf.get( "filename" ) ) )
			fail( "filename specified in the Air application descriptor is empty", Constants.E_AIR_FILE_INVALD ); 
		
		if( "".equals( conf.get( "id" ) ) )
			fail( "name specified in the Air application descriptor is empty", Constants.E_AIR_FILE_INVALD ); 
		
		/*if( "".equals( conf.get( "vendor" ) ) )
			fail( "vendor not specified in the Air application descriptor\n" + 
			      "you might want to add the following lines to your " + airFile.getName() + ": \n\n" + 
			      "<!--\n" + 
			      "vendor=My company\n" + 
			      "-->\n\n" + 
			      "The new lines are important, don't put everything on a single line!",
			      Constants.E_AIR_FILE_INVALD ); 
		*/
		if( conf.get( "filename" ).matches( "[^A-Za-z0-9 \\._-]+" ) )
			Log.warn.println( "Warning: filename specified in the Air application descriptor might contain non-trivial characters (" + conf.get("filename") + ")" );
		
		if( "".equals( conf.get( "icon/image128x128" ) ) )
			Log.warn.println( "Warning: icon/image128x128 not set in Air application descriptor" );  
		
		
		return conf;
	}
	
	/**
	 * Copies a file to a different place
	 * @throws IOException 
	 */
	public static void copy( File from, File to ) throws IOException{
		if( to.isDirectory() ){
			to = new File( to, from.getName() ); 
		}
		
		byte buffer[] = new byte[4096];
		int len = 0; 
		
		FileInputStream in = new FileInputStream( from );
		FileOutputStream out = new FileOutputStream( to ); 
		
		while( ( len = in.read( buffer ) ) > 0 ){
			out.write( buffer, 0, len ); 
		}
		
		in.close(); 
		out.close(); 
		
		return; 
	}
	
	/**
	 * Copies a file with property expansion
	 * 
	 * @throws IOException 
	 */
	public static void copy( File from, File to, Hashtable<String, String> props ) throws IOException{
		if( to.isDirectory() ){
			to = new File( to, from.getName() ); 
		}
		
		BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( from ) ) ); 
		PrintWriter out = new PrintWriter( to ); 
		
		String line; 
		while( ( line = in.readLine() ) != null ){
			for( String key : props.keySet() ){
				line = line.replace( "${" + key + "}", props.get( key ) ); 
			}
			out.println( line ); 
		}
		
		in.close(); 
		out.close(); 
	}
	
	/**
	 * Runs a command, waits for it to finish and returns the error code
	 * @param baseDir The app's working directory
	 * @param args The program arguments. The first argument is the name of the executable
	 * @return 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static int exec( File baseDir, String ... args ) throws IOException, InterruptedException{
		Log.debug.println( "---------------------------------------" ); 
		Log.debug.print( "Exec: " ); 
		for( String arg : args )
			Log.debug.print( arg + " " ); 
		
		Log.debug.println(); 
		Log.debug.println( "- - - - - - - - - - - - - - - - - - - - " ); 
		
		String path = "PATH=" + System.getenv( "PATH" ) + File.pathSeparator + '"' + baseDir.getAbsolutePath() + '"'; 
		Process p = Runtime.getRuntime().exec( args, new String[]{ path }, baseDir );
		new StreamConnector( p.getInputStream(), Log.debug ); 
		new StreamConnector( p.getErrorStream(), Log.debug ); 
		
		p.waitFor();
		
		Log.debug.println( "- - - - - - - - - - - - - - - - - - - - " ); 
		Log.debug.println( "Result: " + p.exitValue() ); 
		Log.debug.println( "---------------------------------------" ); 
		return p.exitValue(); 
	}
	
	
	/**
	 * Changes the extension of a file name, 
	 * if no extension is present it will be appended.
	 *   
	 * @param file
	 * @param extension (without the ".")
	 * @return A new file with the extension changed
	 */
	public static File changeExtension( File file, String extension ){
		String name = file.getName(); 
		
		// Remove extension
		int pos = name.lastIndexOf( '.' ); 
		if( pos > 0 ){
			return new File( name.substring( 0, pos) + "." + extension ); 
		}
		else{
			return new File( name + "." + extension ); 
		}
	}
	
	
	/**
	 * Finds the basename of a file (without the extension)
	 *   
	 * @param file The file
	 * @return The base name of the file
	 */
	public static String getBaseName( File file ){
		String name = file.getName(); 
		
		// Remove extension
		int pos = name.lastIndexOf( '.' ); 
		if( pos > 0 ){
			return name.substring( 0, pos );  
		}
		else{
			return name;  
		}
	}
	
	
	/**
	 * Passes an input stream to an outputstream
	 * 
	 * @author hansi
	 */
	public static class StreamConnector extends Thread{
		private InputStream in;
		private OutputStream out; 
		
		public StreamConnector( InputStream in, OutputStream out ){
			this.in = in;
			this.out = out; 
			start(); 
		}
		
		public void run(){
			try{
				byte buffer[] = new byte[4096]; 
				int len; 
				while( ( len = in.read( buffer ) ) > 0 ){
					out.write( buffer, 0, len ); 
				}
				
				in.close(); 
			}
			catch( IOException e ){
				e.printStackTrace();
			}
		}
	}	
	
}
