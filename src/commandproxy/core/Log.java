package commandproxy.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Log {

	private static PrintStream blackHole = new PrintStream( new OutputStream(){
		public void write(int b) throws IOException {
		}
	});
	
	public static PrintStream debug = System.out; 
	public static PrintStream warn = System.out; 
	public static PrintStream error = System.err;
	
	
	public static void logToCommandLine( boolean verbose ){
		debug = verbose? System.out:blackHole; 
		warn = System.out; 
		error = System.err; 
	}
	
	public static void logToFile( File file, boolean verbose ) throws FileNotFoundException{
		PrintStream out = new PrintStream( file ); 
		debug = verbose? out:blackHole; 
		warn = out; 
		error = out; 
	}
	
	public static void logToNirvana(){
		debug = blackHole; 
		warn = blackHole; 
		error = blackHole; 
	}
}
