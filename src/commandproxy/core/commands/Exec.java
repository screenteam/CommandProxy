package commandproxy.core.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import commandproxy.core.Command;
import commandproxy.core.CommandException;

public class Exec implements Command{

	public JSONObject execute( Map<String, String> params ) throws CommandException, JSONException{
		String executable = params.get( "executable"  ); 
		Vector<String> args = new Vector<String>(); 
		args.add( executable ); 
		for( String key : params.keySet() ){
			if( key.startsWith( "arg" ) ){
				args.add( params.get( key ) ); 
			}
		}
		
		try{
			Process process = Runtime.getRuntime().exec( args.toArray( new String[]{} ) );
			InputReader in = new InputReader( process.getInputStream() ); 
			InputReader err = new InputReader( process.getErrorStream() ); 
			
			process.waitFor(); 
			
			JSONObject result = new JSONObject(); 
			result.put( "exitCode", process.exitValue() );
			result.put( "output", in.getValue() ); 
			result.put( "error", err.getValue() );
			
			return result; 
		}
		catch( IOException e ){
			throw new CommandException( "Execution of " + executable + " failed", this, e ); 
		}
		catch( InterruptedException e ){
			throw new CommandException( "Execution of " + executable + " was interrupted", this, e ); 
		}   
	}

	public String getName(){
		return "exec"; 
	}

	
	private class InputReader extends Thread{
		private String val; 
		private InputStream in; 
		
		public InputReader( InputStream in ){
			this.in = in; 
			this.val = "";  
			start(); 
		}
		
		public String getValue(){
			return val; 
		}
		
		public void run(){
			try{
				BufferedReader input = new BufferedReader( new InputStreamReader( in ) );
				String line;
				
				while( ( line = input.readLine() ) != null ){
					val += line; 
				}
				
				in.close(); 
			}
			catch( IOException e ){
				e.printStackTrace();
			}
		}
	}
}
