package commandproxy.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginLoader {
	File pluginDir; 
	
	public PluginLoader( File pluginDir ){
		this.pluginDir = pluginDir; 
	}
	
	public void loadPlugins( Hashtable<String, Command> commands ){
		
		// First see if we have something in the main classloader
		// In fact - this is only interresting when developing plugins. 
		try{
			InputStream in = getClass().getResourceAsStream( "/plugin.txt" );
			BufferedReader bin = new BufferedReader( new InputStreamReader( in ) );
			String line = ""; 
			String info = ""; 
			while( ( line = bin.readLine() ) != null ){
				info += line; 
			}
			
			Log.debug.println( "Loading plugins from the main classloader..." ); 
			loadPlugins( commands, info, getClass().getClassLoader() ); 
		}
		catch( Exception e ){
			Log.debug.println( "No plugins found in the system classloader" );
		}
		
		Log.debug.println( "Loading plugins from " + pluginDir.getAbsolutePath() + "..." ); 
		if( !pluginDir.exists() ){
			Log.warn.println( "Plugin dir not found!" ); 
			return; 
		}
		
		// Now load stuff from the pluginDir
		Vector<File> jars = findJars( pluginDir );
		for( File jar : jars ){
			// See if there's a "plugin.txt" in the jar file
			Log.debug.print( "> Found " + jar.getName() + "..." );
			String info = getPluginTxt( jar ); 

			if( info == null ){
				Log.debug.println( "doesn't have plugin.txt" ); 
			}
			else{
				try{
					URL[] urls = new URL[]{ jar.toURI().toURL() };
					ClassLoader loader = new URLClassLoader( urls, getClass().getClassLoader() );
					Log.debug.println();
					loadPlugins( commands, info, loader );
				}
				catch( Exception e ){
					Log.debug.println( "failed: " + e.getMessage() );
					e.printStackTrace( Log.error ); 
					continue; 
				}
			}
		}
	}
	
	/**
	 * Recursively finds all jar files in a directory
	 * @param baseDir
	 * @return
	 */
	private Vector<File> findJars( File baseDir ){
		Vector<File> result = new Vector<File>(); 
		
		for( File file : baseDir.listFiles() ){
			if( file.isDirectory() ){
				result.addAll( findJars( file ) );
			}
			else if( file.getName().endsWith( ".jar" ) ){
				result.add( file ); 
			}
		}
		
		return result; 
	}
	
	/**
	 * Loads all the classes found in the plugins string, 
	 * using the provided classloader. 
	 */
	private void loadPlugins( Hashtable<String, Command> commands, String plugins, ClassLoader loader ){
		for( String className : plugins.split( "\n" ) ){
			className = className.trim(); 
			if( !"".equals( className ) ){
				Log.debug.print( "  > Loading " + className + "..." );
				try {
					Command command = (Command) loader.loadClass( className ).newInstance();
					if( commands.get( command.getName() ) != null ){
						throw new Exception( "Command " + command.getName() + " already provided by another class" );
					}
					commands.put( command.getName(), command );
					Log.debug.println( "ok" ); 
				}
				catch( Exception e ){
					Log.debug.println( "failed: " + e.getMessage() );
					e.printStackTrace( Log.error ); 
				}
			}
		}
	}
	
	
	/**
	 * Returns the contents of the file "plugin.txt" inside a jar 
	 * file, return null if the file doesn't exist. 
	 */
	private String getPluginTxt( File jar ){
		try{
			ZipFile zip = new ZipFile( jar );
			ZipEntry entry = zip.getEntry( "plugin.txt" );
			InputStream in = zip.getInputStream( entry );
			
			byte[] content = new byte[(int) entry.getSize()];
			in.read( content );
			
			return new String( content ); 
		}
		catch( Exception e ){
			return null; 
		}
	}
}