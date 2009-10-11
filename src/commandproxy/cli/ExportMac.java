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

	public static void export( File airFile, File targetImage, File templateImage ) throws IOException, ParserConfigurationException, SAXException, InterruptedException{
		// Create a temp directory to work in
		System.out.println( "Creating temporary directory..." ); 
		File temp = File.createTempFile( "commandproxy", "" );
		temp.delete(); // Delete the file
		temp.mkdir();  // And create a directory with that name
		
		// Get access to the air-descriptor
		System.out.println( "Reading air descriptor..." ); 
		Hashtable<String, String> conf = getAirConfig( airFile );
		conf.put( "tempdir", temp.getAbsolutePath() ); 
		
		// Look for the input image
		if( templateImage == null ){
			templateImage = getCommandProxyFile( "files/mac/template.sparseimage" ); 
		}
		

		// We LOOOVE absolute paths!
		templateImage = templateImage.getAbsoluteFile(); 
		File tempImage = new File( temp, "image.sparseimage" ); 
		
		copy( templateImage, tempImage ); 
		
		// Mount image... 
		System.out.println( "Mounting copy of the template image..." );
		File mountedImageFile = new File( temp, "temp_image" ); 
		mountedImageFile.mkdir(); 
		String mountedImage = mountedImageFile.getAbsolutePath(); 
		
		ProcessHelper mount = new ProcessHelper( tempImage.getParentFile(), "hdiutil", "attach", "-mountPoint", mountedImage, tempImage.getName() );
		if( mount.getReturnCode() != 0 ){
			mount.getException().printStackTrace( Log.error );
			fail( "Disk image couldn't be mounted", E_EXPORT_FAILED );
		}
		
		// Find the path to the application
		File app = null; 
		for( File file : new File( mountedImage ).listFiles() ){
			if( file.getName().endsWith( ".app" ) ){
				app = file.getAbsoluteFile(); 
				break; 
			}
		}
		
		if( app == null ){
			fail( "No empty .app folder was found on the template image!", E_EXPORT_FAILED );
		}
		else{
			System.out.println( "Using " + app.getAbsolutePath() + " as application directory" );
		}
		String appPath = app.getAbsolutePath();
		
		System.out.println( "Copying application skeleton" );
		File src = getCommandProxyFile( "files/mac/skeleton/Contents" ); 
		ProcessHelper cp = new ProcessHelper( new File( "/bin" ), "cp", "-R", src.getAbsolutePath(), appPath );
		if( cp.getReturnCode() != 0 ){
			cp.getException().printStackTrace( Log.error ); 
			fail( "Application skeleton couldn't be copied", E_EXPORT_FAILED );
		}
		
		// put the correct properties into info.plist
		copy( getCommandProxyFile( "files/mac/skeleton/Contents/Info.plist" ), new File( appPath + "/Contents/Info.plist" ), conf );
		
		// insert application stub+adapt permission 
		File javaStubSrc = new File( "/System/Library/Frameworks/JavaVM.framework/Versions/A/Resources/MacOS/JavaApplicationStub" );
		File javaStubDest = new File( appPath + "/Contents/MacOS/JavaApplicationStub" );
		copy( javaStubSrc, javaStubDest );
		ProcessHelper chmod = new ProcessHelper( new File( "/bin" ), "chmod", "a+rx", javaStubDest.getAbsolutePath() );
		if( chmod.getReturnCode() != 0 ){
			cp.getException().printStackTrace( Log.error ); 
			fail( "File permission couldn't be set for " + javaStubDest.getAbsolutePath(), E_EXPORT_FAILED );
		}
		
		// copy the jar file
		copy( getCommandProxyFile( "jars/commandproxy-launcher.jar" ), new File( appPath + "/Contents/MacOS/" ) );
		
		// If the icon exists export it...
		// We don't just throw the error to the callee
		// of this function because we really don't want the whole
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
			exec( temp, "sips",  "-s",  "format", "tiff", target.getAbsolutePath(), "--out", "icon.tiff" );
			exec( temp, "tiff2icns", "icon.tiff", "icon.icns" );
			
			copy( new File( temp, "icon.icns" ), new File( appPath + "/Contents/Resources" ) );
		}
		catch( Exception ex ){
			Log.warn.println( "Icon " + conf.get( "icon/image128x128" ) + " could not be extracted" ); 
		}
		
		// Allmost done, 
		// copy the air file
		File resources = new File( appPath + "/Contents/Resources" ); 
		ProcessHelper unzip = new ProcessHelper( resources, "unzip", airFile.getAbsolutePath() );
		if( unzip.getReturnCode() != 0 ){
			unzip.getException().printStackTrace( Log.error ); 
			fail( "AIR-App couldn't be extracted from " + airFile.getAbsoluteFile() + " to " + resources.getAbsolutePath(), E_EXPORT_FAILED );
		}
		//copy( new File( resources.getAbsolutePath() + "/META-INF/AIR/application.xml" ), resources );

		// unmount
		ProcessHelper umount = new ProcessHelper( new File( "/" ), "hdiutil", "detach", mountedImage );
		if( umount.getReturnCode() != 0 ){
			Log.warn.println( "Couldn't unmount " + mountedImage );
			Log.warn.println( "Please unmount manually before re-exporting!" ); 
		}
		
		ProcessHelper compact = new ProcessHelper( new File( "/" ), "hdiutil", "compact", tempImage.getAbsolutePath() );
		if( compact.getReturnCode() != 0 ){
			Log.warn.println( "Couldn't compact " + tempImage.getAbsolutePath() );
			Log.warn.println( "The resulting dmg might be bigger than necessary" );
		}
		
		// Compact and then convert sparse image to dmg, all done then! 
		if( targetImage == null ){
			targetImage = new File( conf.get( "filename" ) + "-" + conf.get( "version" ) + ".dmg" );
		}
		
		if( targetImage.exists() ){
			targetImage.delete(); 
		}
		
		ProcessHelper convert = new ProcessHelper( new File( "/"), "hdiutil", "convert", tempImage.getAbsolutePath(), "-format", "UDZO", "-imagekey", "zlib-level=9", "-o", targetImage.getAbsolutePath() );
		if( convert.getReturnCode() != 0 ){
			convert.getException().printStackTrace( Log.error ); 
			fail( "Couldn't convert sparseimage to dmg", E_EXPORT_FAILED );
		}
		
		// Done? Done! 
		deleteDirectory( temp ); 
		System.out.println( "Success!" ); 
	}
}
