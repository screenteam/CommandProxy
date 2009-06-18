package commandproxy.cli;

import java.io.File;

import commandproxy.core.Constants;
import commandproxy.core.Log;

/**
 * Your window to the commandproxy world! 
 *
 * @author hansi
 */
public class Main implements Constants{
	public static void main( String[] args ){
		if( args.length == 0 ){
			Tools.printUsage();
			System.exit( 0 ); 
		}
		
		
		if( args[0].equals( "debug" ) ){
			for( int i = 1; i < args.length; i++ ){
				if( args[i].equals( "-verbose" ) ){
					Log.info.println( "Enabling verbose mode" ); 
					Log.setVerbose( true ); 
				}
			}
			new DebugProxy();
			return;  
		}
		else if( args[0].equals( "export" ) ){
			// Parse options...
			if( args.length < 3 ){
				failEarly( "Wrong Parameters", E_PARAMETER_FORMAT ); 
			}
			
			String os = args[1]; 
			File outFile = null;
			File airFile = new File( args[args.length-1] );
			
			if( !os.equals( "windows" ) && !os.equals( "mac" ) ){
				failEarly( "Operating system unknown: " + os, E_UNSUPPORTED_OS ); 
			}
			
			if( !airFile.exists() ){
				failEarly( "Air-File " + args[args.length-1] + " does not exist", E_AIR_FILE_NOT_FOUND ); 
			}
			
			for( int i = 1; i < args.length; i++ ){
				if( args[i].startsWith( "-out=" ) ){
					outFile = new File( airFile.getParent(), args[i].substring( 5 ) );
					Log.info.println( "Output to " + outFile.getAbsolutePath() ); 
				}
				if( args[i].startsWith( "-verbose" ) ){
					Log.setVerbose( true ); 
				}
			}
			
			if( os.equals( "windows" ) ){
				try {
					ExportWindows.export( airFile, outFile );
				} catch (Exception e) {
					e.printStackTrace();
					Tools.fail( e.getMessage(), Constants.E_EXPORT_FAILED ); 
				}
			}
			else if( os.equals( "mac" ) ){
				try{
					ExportMac.export( airFile, null, null ); 
				}
				catch( Exception e ){
					e.printStackTrace(); 
					Tools.fail( e.getMessage(), E_EXPORT_FAILED ); 
				}
			}
			else{
				Tools.fail( "Operating system unknown: " + os, E_UNSUPPORTED_OS ); 
			}
			
		}
		else{
			Tools.printUsage(); 
			System.exit( 0 ); 
		}
	}

	
	/**
	 * Fails with an error message, 
	 * then prints information on how to use the commandproxy to the command line
	 * and finally exits
	 * 
	 * @param message The error message
	 * @param code The error code, for a list of codes see {@link Constants}
	 */
	private static void failEarly( String message, int code ){
		Tools.printUsage();
		
		System.err.println(); 
		System.err.println( "Error: " ); 
		System.err.println( message ); 
		System.exit( code ); 
	}


}