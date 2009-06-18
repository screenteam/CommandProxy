package commandproxy.core.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.sdicons.json.model.JSONObject;

import commandproxy.core.Command;
import commandproxy.core.CommandException;
import commandproxy.core.Log;
import commandproxy.core.Proxy;

/**
 * Opens a file according to the application associated with it. 
 * 
 * This is implemented by using: 
 *   on windows: explorer.exe <file> 
 *   on macos x: open <file>
 *   on linux:   not implemented
 */
public class Open implements Command{
	static boolean hasDesktopApi;
	static{
		// Check for the desktop class 
		// included in java 1.6+
		try{
			Class.forName( "java.awt.Desktop" );
			hasDesktopApi = Desktop.isDesktopSupported(); 
		}
		catch( ClassNotFoundException ex ){
			hasDesktopApi = false; 
		}
	}
	
	public JSONObject execute( Map<String, String> parameters ) throws CommandException{
		String filename = parameters.get( "file" );
		File file = Proxy.getFile( filename, parameters ); 
		
		try {
			Thread.sleep( 5000 );
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		if( file == null ){
			throw new CommandException( "Paramter file not set!", this ); 
		}
		else if( !file.exists() ){
			throw new CommandException( "File does not exist: " + file.getAbsolutePath(), this ); 
		}
		else{
			try{
				Log.debug.println( "Opening " + file.getAbsolutePath() ); 
				
				// What's our os?
				if( hasDesktopApi ){
					Desktop.getDesktop().open( file );
				}
				else if( System.getProperty("os.name" ).equals(  "Mac OS X" ) ){
					launchMac( file.getAbsolutePath() ); 
				}
				else if( System.getProperties().get( "os.name" ).toString().startsWith(  "Windows" ) ){
					launchWindows( file.getAbsolutePath() ); 
				}
			}
			catch( IOException e ){
				e.printStackTrace(); 
				throw new CommandException( "File could not be opened", this ); 
			} 
		}
		
		return null;
	}

	public String getName(){
		return "open"; 
	}
	
	/**
	 * Launch on mac... 
	 * @throws IOException 
	 */
	private void launchMac( String filename ) throws IOException{
		Process p = Runtime.getRuntime().exec( new String[]{
				"/usr/bin/open", 
				filename
		} );
		
		try{
			p.waitFor();
		}
		catch( InterruptedException e ){
			e.printStackTrace();
		} 
	}
	
	/**
	 * Launch on windows
	 * @throws IOException 
	 */
	private void launchWindows( String filename ) throws IOException{
		Runtime.getRuntime().exec( new String[]{
				"cmd.exe",
				"/C",
				"start", 
				"", 
				filename
				
		} );
		
		Process p = Runtime.getRuntime().exec( "cmd.exe /C start \"\" \"" + filename + "\"" );
		
		try{
			p.waitFor();
		}
		catch( InterruptedException e ){
			e.printStackTrace();
		} 
		
	}

}
