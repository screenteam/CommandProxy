package commandproxy.cli;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.simpleframework.transport.connect.SocketConnection;

import commandproxy.core.Constants;
import commandproxy.core.Log;
import commandproxy.core.PluginLoader;
import commandproxy.core.Proxy;

public class DebugProxy extends Thread{
	int port = 0; 

	public DebugProxy(){
		start(); 
	}
	
	public void run(){
		try{
			// the debug port... 
			port = 37148; 
			
			// Let's set up the command proxy
			// The null-parameter means disables the key authentification
			Proxy proxy = new Proxy( null ); 
			proxy.loadPlugins( new PluginLoader( new File( "plugins" ) ) ); 
			
			SocketConnection connection = new SocketConnection( proxy );
			InetSocketAddress address = new InetSocketAddress( "127.0.0.1", port );  
			connection.connect( address );
			
			// Great! 
			// We're up and running!
			System.out.println( "CommandProxy running on localhost:" + port );
		}
		catch (IOException e) {
			e.printStackTrace( Log.error );
			Tools.fail( "CommandProxy failed: ", Constants.E_DEBUGPROXY_CRASHED ); 
		}
	}
}
