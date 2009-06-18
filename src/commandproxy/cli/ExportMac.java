package commandproxy.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import commandproxy.core.Constants;
import commandproxy.core.Log;

import static commandproxy.cli.Tools.*; 

public class ExportMac implements Constants{

	public static void export( File airFile, File sourceImage, File setupImage ) throws IOException, ParserConfigurationException, SAXException, InterruptedException{
		// Create a temp directory to work in
		System.out.println( "Creating temporary directory..." ); 
		File temp = File.createTempFile( "commandproxy", "" );
		temp.delete(); // Delete the file
		temp.mkdir();  // And create a directory with that name
		temp = new File( "work" ); 
		temp.mkdir(); 
		
		// Get access to the air-descriptor
		System.out.println( "Reading air descriptor..." ); 
		Hashtable<String, String> conf = getAirConfig( airFile );
		conf.put( "tempdir", temp.getAbsolutePath() ); 
		
		// Look for the input image
		if( sourceImage == null ){
			sourceImage = getCommandProxyFile( "files/mac/template.dmg" ); 
		}
		
		// Look for the output file
		if( setupImage == null ){
			setupImage = new File( conf.get( "filename" ) + "-" + conf.get( "version" ) + ".dmg" );  
		}
		
		// If the icon exists export it...
		// We don't just pass throw the error to the callee
		// of this function because we really don't the whole
		// process to fail just because of the icon. 
		try{
			// Get the new image path... 
			File target = new File( temp, conf.get( "icon/image128x128" ) );
			target.getParentFile().mkdirs(); 
			
			// Create an input stream for application.xml file
			ZipFile archive = new ZipFile( airFile );
			ZipEntry iconSrc = archive.getEntry( conf.get( "icon/image128x128" ) );
			InputStream in = archive.getInputStream( iconSrc );
			FileOutputStream out = new FileOutputStream( target );
			
			int len = 0;
			byte buffer[] = new byte[4096]; 
			while( ( len = in.read( buffer ) ) > 0 ){
				out.write( buffer, 0, len ); 
			}
			
			in.close();
			out.close();
			
			// Great! now convert it to a tiff file
			Tools.exec( temp, "sips",  "-s",  "format", "tiff", target.getAbsolutePath(), "--out", "icon.tiff" );
			Tools.exec( temp, "tiff2icns", "icon.tiff", "icon.icns" ); 
		}
		catch( Exception ex ){
			Log.warn.println( "Icon " + conf.get( "icon/image128x128" ) + " could not be extracted" ); 
		}
		
		if( true ){
			return; 
		}
		
		// Create the windows launcher
		System.out.println( "Creating launcher..." );
		File launchFile = new File( temp, "launcher.jsmooth" );
		String jSmoothExe = getCommandProxyFile( "files/windows/jsmooth/jsmoothcmd.exe" ).getAbsolutePath();  
		
		copy( getCommandProxyFile( "files/windows/launcher.jsmooth" ), launchFile, conf );
		copy( getCommandProxyFile( "jars/commandproxy-launcher.jar" ), temp );
		
		int result = Tools.exec( temp, jSmoothExe, launchFile.getAbsolutePath() );
		if( result != 0 ){
			Tools.fail( "Executable failed to build", E_EXPORT_FAILED );
		}
		
		
		// Create the window installer
		System.out.println( "Creating installer..." ); 
		File nsisFile = new File( temp, "installer.nsi" );
		String nsisExe = getCommandProxyFile( "files/windows/nsis/makensis.exe" ).getAbsolutePath(); 
		
		copy( getCommandProxyFile( "files/windows/installer.nsi" ), temp, conf );
		
		result = Tools.exec( temp, nsisExe, nsisFile.getAbsolutePath() );
		if( result != 0 ){
			Tools.fail( "Installer failed to build", E_EXPORT_FAILED ); 
		}
		
		// Done? Done! 
		System.out.println( "Success!" ); 
	}
}
