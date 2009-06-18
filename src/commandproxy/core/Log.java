package commandproxy.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class Log {

	private static PrintStream nullStream = new PrintStream( new OutputStream(){
		public void write(int b) throws IOException {
		}
	});
	
	public static PrintStream debug = nullStream; 
	public static PrintStream info = System.out; 
	public static PrintStream warn = System.out; 
	public static PrintStream error = System.out;
	
	
	public static void setVerbose( boolean verbose ){
		if( verbose ){
			// redirect to system.out
			debug = System.out; 
		}
		else{
			// redirect to nowhere
			debug = new PrintStream( new OutputStream(){
				@Override
				public void write(int b) throws IOException {
				}
			} ); 
		}
	}
}
