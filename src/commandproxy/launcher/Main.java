package commandproxy.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.simpleframework.transport.connect.SocketConnection;

import commandproxy.core.Constants;
import commandproxy.core.Log;
import commandproxy.core.Proxy;

public class Main implements Constants{
	private static Process process; 
	private static boolean restart; 
	
	/**
	 * All the arguments are passed along to the air-app
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main( String args[] ) throws InterruptedException{
		int port = 0; 
		Vector<String> execArgs = new Vector<String>(); 
		
		if( args.length > 0 ){
			String c = args[0]; 
			if( c.startsWith( "--help" ) || c.startsWith( "/?" ) || c.startsWith( "-h" ) ){
				Log.debug.println( "Usage: [--help] [<air-arg1> [<air-arg2> ...]]" ); 
				System.exit( 0 ); 
			}
		}
		
		// See if we can find the air-file
		File executable = findAirExecutable();
		if( executable == null ){
			fail( "Air-App could not be found inside " + new File( "air" ).getAbsolutePath() + "\n" + 
			      "Please consider reinstalling this application", E_AIR_APP_NOT_FOUND ); 
		}
		else{
			execArgs.add( executable.getAbsolutePath() ); 
		}
		
		// Logging in Dateien schrieben.... 
		try {
			Log.logToFile( new File( "log.txt" ), true ); 
		}
		catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
		
		// Append the user arguments
		for( int i = 0; i < args.length; i++ ){
			execArgs.add( args[i] );
		}
		
		try{
			// Let's get a free port number
			ServerSocket socket = new ServerSocket( 0 ); 
			port = socket.getLocalPort();
			socket.close();
			
			// Let's set up the command proxy
			Proxy proxy = new Proxy(); 
			SocketConnection connection = new SocketConnection( proxy );
			InetSocketAddress address = new InetSocketAddress( "127.0.0.1", port );  
			connection.connect( address );
			
			// Great! 
			// We're up and running!
			Log.debug.println( "CommandProxy running on localhost:" + port );

			// Add final params
			execArgs.add( "--port=" + port );
			
			// Kill Air if this server is killed
			Runtime.getRuntime().addShutdownHook( new Thread(){
				public void run(){
					process.destroy(); 
				}
			}); 
			
			// Wait until it dies
			restart = true; 
			while( restart ){
				restart = false; 
				process = Runtime.getRuntime().exec( execArgs.toArray( new String[]{} ) ); 
				process.waitFor(); 
			}
			
			// Bye! 
			connection.close();
			System.exit( 0 );
		}
		catch( IOException e ){
			e.printStackTrace();
			fail( "An IO-Exception while trying to run " + executable.getAbsolutePath(), E_LAUNCHER_FAILED ); 
		}
	}
	
	
	/**
	 * Finds the executable file
	 */
	private final static File findAirExecutable(){
		if( !new File( "air" ).exists() ){
			fail( "Air-Directory doesn't exist", E_AIR_APP_NOT_FOUND ); 
		}
		
		if( System.getProperty( "os.name" ).startsWith(  "Windows" ) ){
			File files[] = new File( "air" ).listFiles(); 
			for( File dir : files ){
				File executable = new File( dir, dir.getName() + ".exe" ); 
				if( dir.isDirectory() && executable.exists() ){
					return executable; 
				}
			}
		}
		else if( System.getProperties().get( "os.name" ).toString().equals(  "Mac OS X" ) ){
			fail( "Mac OS X is not supported yet", E_UNSUPPORTED_OS ); 
		}
		
		return null; 
	}
	
	/**
	 * Fails, properly! 
	 */
	private static void fail( String message, int exitCode ){
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch( Exception ex ){
			ex.printStackTrace();
		}
		
		Log.error.println( message ); 
		JOptionPane.showMessageDialog( null, message, "Error", JOptionPane.ERROR_MESSAGE );
		System.exit( exitCode ); 
	}
	
	/**
	 * Restart... 
	 */
	public static void restart(){
		restart = true; 
	}
}