package commandproxy.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.simpleframework.transport.connect.SocketConnection;

import commandproxy.core.Constants;
import commandproxy.core.Log;
import commandproxy.core.PluginLoader;
import commandproxy.core.Proxy;

/**
 * The main adobe air file launcher. 
 * Yep, this is the first file to be executed when you double-click the installed app. 
 * @author hansi
 */
public class Main implements Constants{
	public final static ResourceBundle BUNDLE = ResourceBundle.getBundle( "commandproxy.launcher.i18n.lang" );
	
	private static Process process; 
	private static boolean restart; 
	
	/**
	 * All the arguments are passed along to the air-app
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException 
	 */
	public static void main( String args[] ) throws InterruptedException, IOException{
		int port = 0; 
		Vector<String> execArgs = new Vector<String>(); 
		
		if( args.length > 0 ){
			String c = args[0]; 
			if( c.startsWith( "--help" ) || c.startsWith( "/?" ) || c.startsWith( "-h" ) ){
				Log.debug.println( "Usage: [--help] [<air-arg1> [<air-arg2> ...]]" ); 
				System.exit( 0 ); 
			}
		}
		
		// Logging auf kommandozeile ausgeben 
		Log.logToCommandLine( true ); 
		
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
			String key = generateKey( 40 ); 
			Proxy proxy = new Proxy( key );
			SocketConnection connection = new SocketConnection( proxy );
			InetSocketAddress address = new InetSocketAddress( "127.0.0.1", port );  
			connection.connect( address );
			
			// Load plugins... 
			if( System.getProperty( "os.name" ).startsWith(  "Windows" ) ){
				PluginLoader pluginLoader = new PluginLoader( new File( "plugins" ) );
				proxy.loadPlugins( pluginLoader );
			}
			else if( System.getProperties().get( "os.name" ).toString().equals(  "Mac OS X" ) ){
				PluginLoader pluginLoader = new PluginLoader( LauncherMac.PLUGINS );
				proxy.loadPlugins( pluginLoader );
			}
			
			
			// Great! 
			// We're up and running!
			Log.debug.println( "CommandProxy running on localhost:" + port );

			// Add final params
			execArgs.add( "--port=" + port );
			execArgs.add( "--key=" + key ); 
			
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
				process = exec( execArgs ); 
				new Forwarder( process.getErrorStream(), Log.error );
				new Forwarder( process.getInputStream(), Log.debug );
				
				process.waitFor(); 
			}
			
			// Bye! 
			connection.close();
			System.exit( 0 );
		}
		catch( IOException e ){
			e.printStackTrace();
			fail( "An IO-Exception occured while trying to run the application", e, E_LAUNCHER_FAILED ); 
		}
	}
	
	
	/**
	 * Finds the executable file
	 * @throws IOException 
	 */
	private final static Process exec( Vector<String> args ) throws IOException{
		Vector<String> argsCopy = new Vector<String>(); 
		argsCopy.addAll( args ); 
		
		if( System.getProperty( "os.name" ).startsWith(  "Windows" ) ){
			return LauncherWindows.exec( argsCopy );  
		}
		else if( System.getProperties().get( "os.name" ).toString().equals(  "Mac OS X" ) ){
			return LauncherMac.exec( argsCopy ); 
		}
		
		return null; 
	}
	
	/**
	 * Fails, properly! 
	 */
	public static void fail( String message, int exitCode ){
		fail( message, null, exitCode );
	}
	
	/**
	 * Fails, super-properly! 
	 */
	public static void fail( String message, Exception exception, int exitCode ){
		// We love native LAF, even for errors! 
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch( Exception e){
			// don't give a ****
		}
		
		String options[] = {
				BUNDLE.getString( "button.ok" ), 
				BUNDLE.getString( "button.details" )
		}; 
		
		int selection = JOptionPane.showOptionDialog(
			null, // parent 
			BUNDLE.getString( "app.launch_failed.text" ), // message 
			BUNDLE.getString( "app.launch_failed.title" ), // title
			JOptionPane.YES_NO_OPTION, // message type 
			JOptionPane.ERROR_MESSAGE, // option type
			null, // icon
			options, // options
			options[0] // default option
		);

		if( selection == 1 ){
			// Show details! 
			if( exception != null ){
				ByteArrayOutputStream exOut = new ByteArrayOutputStream(); 
				exception.printStackTrace( new PrintStream( exOut ) );
				message += "\n\n" + exOut.toString();
			}
			
			JTextArea messageText = new JTextArea( message );
			// somehow, jtextarea usually always has a f*cking nasty font! 
			messageText.setFont( new JLabel().getFont() );
			messageText.setEditable( false );
			
			JOptionPane.showMessageDialog( null, new JScrollPane( messageText ), BUNDLE.getString( "app.launch_failed.title" ), JOptionPane.ERROR_MESSAGE );
		}
		
		// bye! 
		System.exit( exitCode ); 
	}
	
	/**
	 * Restart... 
	 */
	public static void restart(){
		restart = true; 
	}
	
	/**
	 * Forward process output to an output stream
	 */
	private static class Forwarder extends Thread{
		private InputStream in; 
		private OutputStream out; 
		
		public Forwarder( InputStream in, OutputStream out ){
			this.in = in; 
			start(); 
		}
		
		public void run(){
			try{
				int len = 0; 
				byte buffer[] = new byte[1024]; 
				
				while( ( len = in.read() ) > 0 ){
					out.write( buffer, 0, len ); 
				}
			}
			catch( Exception e ){
				// Whatever...
				// e.printStackTrace( Log.error ); 
			}
		}
	}
	
	/**
	 * Generates a random key of a certain length
	 */
	public static String generateKey( int length ){
		char chars[] = {
			'A','B','C','D','E','F','G','H','I','J','K','L','M',
			'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
			'a','b','c','d','e','f','g','h','i','j','k','l','m',
			'n','o','p','q','r','s','t','u','v','w','x','y','z',
			'0','1','2','3','4','5','6','7','8','9','0'
		};
		
		String key = ""; 
		for( int i = 0; i < length; i++ ){
			key += chars[ (int)( Math.random() * chars.length) ];
		}
		
		return key; 
	}
	
	/**
	 * Writes the publisher-id file that air requires to start
	 * @throws IOException 
	 */
	public static void generatePubID( File pubFile ) throws IOException{
		String pubid = "";

		char chars[] = { 
			'A','B','C','D','E','F','G','H','I','J','K','L',
			'M','N','O','P','Q','R','S','T','U','V','W','X',
			'Y','Z','0','1','2','3','4','5','6','7','8','9'
		};
		
		for( int i = 0; i < 40; i++ ){
			pubid += chars[(int)(Math.random()*chars.length)];
		}
		pubid += ".1";
		
		FileOutputStream fos = new FileOutputStream( pubFile );
		fos.write( pubid.getBytes() );
		fos.close(); 
	}
}