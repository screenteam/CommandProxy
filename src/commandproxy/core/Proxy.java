package commandproxy.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;

import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONString;

import commandproxy.core.commands.ChangeEncoding;
import commandproxy.core.commands.Exec;
import commandproxy.core.commands.Open;
import commandproxy.core.commands.Restart;

public class Proxy implements Container {
	// All the commands we know
	private Hashtable<String, Command> commands = new Hashtable<String, Command>(); 
	
	
	/**
	 * Create a new command proxy
	 */
	public Proxy(){
		// Register the built-in commands
		registerCommand( new Open() ); 
		registerCommand( new Exec() ); 
		registerCommand( new ChangeEncoding() ); 
		registerCommand( new Restart() ); 
	}
	
	
	public void handle( Request req, Response res ){
		Log.debug.println( "==============================" );

		String path = req.getPath().getName(); 
		Command command = commands.get( path );  
		PrintStream out;
		Map<String, String> parameters;
		
		// Do we even have a handler for this?
		if( command == null ){
			Log.debug.println( "Invalid command requested: " + path ); 
			abort( res, 404, "Command not found" );  
			return; 
		}
		
		try{
			// Connect to the client
			out = res.getPrintStream();
			
			// Get the request parameters
			parameters = req.getForm();
		}
		catch( IOException e ){
			Log.debug.println( "Invalid post parameters" ); 
			abort( res, 500, "Can't read post parameters" );
			e.printStackTrace(); 
			return; 
		}
		
		Log.debug.println( "Request: " + path );
		for( String key : parameters.keySet() ){
			Log.debug.printf( "  %10s: %s\n", key, parameters.get( key ) ); 
		}
		
		// Awsome, we have everything we need. 
		// Now let's execute that thing!
		JSONObject result; 
		try{
			result = command.execute( parameters );
		}
		catch( CommandException e ){
			Log.error.println( e.getMessage() ); 
			e.printStackTrace( Log.error );
			result = new JSONObject();
			result.getValue().put( "error", new JSONString( "Execution failed: " + e.getMessage() ) ); 
		} 
		
		Log.debug.println( "------------------------------" ); 
		Log.debug.println( "Response: " );
		if( result == null ){
			result = new JSONObject(); 
		}
		
		Log.debug.println( result.render( true ) );
		res.add( "Content-type", "text/html; charset=UTF-8" ); 
		out.println( result.render( true ) );
		
		out.flush(); 
		out.close(); 
	}
	
	
	/**
	 * Registers a command
	 * @param command The command object
	 */
	public void registerCommand( Command command ){
		commands.put( command.getName(), command ); 
	}
	
	/**
	 * Fails with a certain message and response code...
	 * 
	 * No data can be written to the response after this call
	 * 
	 * @param res The response object
	 * @param code The http status code
	 * @param message An error message, to be a little debug-friendlier
	 */
	private void abort( Response res, int code, String message ){
		try{
			res.setCode(  code ); 
			res.commit(); 
			res.getPrintStream().print(  message ); 
			res.getPrintStream().flush(); 
			res.close();
		}
		catch( Exception ex ){
			ex.printStackTrace(); 
		}
	}
	
	
	/**
	 * Finds a file for you
	 * @return 
	 */
	public static File getFile( String filename, Map<String, String> params ){
		if( filename == null ){
			return null; 
		}
		else{
			return new File( filename ); 
		}
	}
}
