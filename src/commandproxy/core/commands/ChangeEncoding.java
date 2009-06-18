package commandproxy.core.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;

import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONValue;
import commandproxy.core.Command;
import commandproxy.core.CommandException;
import commandproxy.core.Log;
import commandproxy.core.Proxy;

/**
 * Changes the encoding of a file (optionally writes the output to a new file) 
 * Parameters: 
 * 
 * file:       The source file
 * output:     The target file (optional, default=source file) 
 * from:       The source file encoding (optional, default=java's default)
 * to:         The target file encoding (optional, default=utf-8)
 * 
 * @author hansi
 */
public class ChangeEncoding implements Command{

	public JSONObject execute( Map<String, String> params ) throws CommandException {
		String filename = params.get( "file" );
		String outputName = params.get( "output" ); 
		String fromEncodingName = params.get( "from" ); 
		String toEncodingName = params.get( "to" ); 
		
		File file = Proxy.getFile( filename, params ); 
		File output = Proxy.getFile( outputName, params ); 
		
		// Some formalities... 
		if( file == null ){
			throw new CommandException( "File parameter not provided", this ); 
		}
		
		if( !file.exists() ){
			throw new CommandException( "File doesn't exist (" + file.getAbsolutePath() + ")", this ); 
		}
		
		if( outputName == null || outputName.trim().equals( "" ) ){
			output = file; 
		}
		
		Charset fromEncoding = Charset.defaultCharset();   
		Charset toEncoding = Charset.forName( "UTF-8" );
		
		if( fromEncodingName != null ){
			if( Charset.isSupported( fromEncodingName ) ){
				fromEncoding = Charset.forName( fromEncodingName ); 
			}
			else{
				throw new CommandException( "from-encoding not supported: " + fromEncodingName, this ); 
			}
		}
		
		if( toEncodingName != null ){
			if( Charset.isSupported( toEncodingName ) ){
				toEncoding = Charset.forName( toEncodingName ); 
			}
			else{
				throw new CommandException( "to-encoding not supported: " + toEncodingName, this ); 
			}
		}
		
		// Good, do it! 
		File tempFile = null;
		try {
			tempFile = File.createTempFile( "commandproxy", ".encoding" );
		}
		catch ( IOException e ){
			throw new CommandException( "Failed to create temporary file", this ); 
		}
		
		try{
			BufferedReader in = new BufferedReader( 
				new InputStreamReader( new FileInputStream( file ), fromEncoding )
			); 
			BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter( new FileOutputStream( tempFile ), toEncoding )
			); 
			
			char buffer[] = new char[4096];
			int len = 0;
			
			while( ( len = in.read( buffer ) ) > 0 ){
				out.write( buffer, 0, len );  
			}
			
			in.close(); 
			out.close(); 
		}
		catch( IOException ex ){
			throw new CommandException( "Problem while changing encoding: " + file.getAbsolutePath(), this ); 
		}
		
		
		if( output.exists() && !output.delete() ){
			throw new CommandException( "Output file couldn't be emptied for writing: " + output.getAbsolutePath(), this ); 
		}
		
		if( !tempFile.renameTo( output ) ){
			tempFile.delete(); 
			throw new CommandException( "Output file couldn't be created: " + output.getAbsolutePath(), this ); 
		}
		
		JSONObject result = new JSONObject(); 
		result.getValue().put( "file", JSONValue.decorate( file.getAbsolutePath() ) );
		result.getValue().put( "output", JSONValue.decorate( output.getAbsolutePath() ) ); 
		result.getValue().put( "from", JSONValue.decorate( fromEncoding.displayName() ) );
		result.getValue().put( "to", JSONValue.decorate( toEncoding.displayName() ) );
		
		return result; 
	}

	public String getName() {
		return "changeEncoding"; 
	}

}
