package commandproxy.launcher;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import commandproxy.core.Constants;

public class LauncherMac implements Constants{
	private static File RESOURCES = new File( System.getProperty( "APP_PACKAGE" ) + "/Contents/Resources" );
	private static File MACOS =  new File( System.getProperty( "APP_PACKAGE" ) + "/Contents/MacOS" );
	private static File AIR_TEMPLATE = new File( "/Library/Frameworks/Adobe AIR.framework/Versions/Current/Resources/Template.app/Contents/MacOS/Template" );
	private final static ResourceBundle BUNDLE = Main.BUNDLE; 
	
	public static Process exec( Vector<String> args ) throws IOException{
		while( !isAIRInstalled() ){
			showAirManualInstallDialog();
		}
		
		if( !isAirAppInitialized() ){
			initializeAirApp(); 
		}
		
		args.add( 0, new File( MACOS, "Air" ).getAbsolutePath() );
		args.add( 1, "--" );
		
		return Runtime.getRuntime().exec(
			args.toArray( new String[]{} ),
			new String[]{}, 
			MACOS
		);
	}
	
	private static boolean isAIRInstalled(){
		return AIR_TEMPLATE.exists(); 
	}
	
	private static void showAirManualInstallDialog(){
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch( Exception e ){
			e.printStackTrace();
		}
		
		Object options[] = { BUNDLE.getString( "button.continue" ), BUNDLE.getString( "button.cancel" ) };
		JPanel message = new JPanel( new BorderLayout() ); 
		JLabel text = new JLabel( BUNDLE.getString( "air.manualinstall.text" ) ); 
		JLabel link = new JLabel( "<html><font color='blue'><u>http://get.adobe.com/air/</u></font></html>" );
		link.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		link.addMouseListener( new MouseAdapter(){
			@Override
			public void mouseClicked( MouseEvent event){
				try{
					Runtime.getRuntime().exec( "open http://get.adobe.com/air/" );
				}
				catch (IOException e ){
					e.printStackTrace();
				}
			}
		});
		message.add( text, BorderLayout.NORTH );
		message.add( link, BorderLayout.CENTER );
		message.add( new JLabel(""), BorderLayout.SOUTH );
		
		int selection = JOptionPane.showOptionDialog(
			null, // parent 
			message, // message 
			BUNDLE.getString( "air.manualinstall.title" ), // title
			JOptionPane.YES_NO_OPTION, // message type 
			JOptionPane.WARNING_MESSAGE, // option type
			null, // icon
			options, // options
			options[0] // default option
		);
		
		if( selection != 0 ){
			System.exit( E_LAUNCHER_FAILED );
		}
	}

	private static boolean isAirAppInitialized(){
		return 
			new File( MACOS, "Air" ).exists() &&
			new File( RESOURCES.getAbsolutePath() + "/META-INF/AIR/publisherid" ).exists();
	}
	
	private static void initializeAirApp() throws IOException{
		Runtime.getRuntime().exec(
				new String[]{ "/bin/cp", AIR_TEMPLATE.getAbsolutePath(), "Air" }, 
				new String[]{}, 
				MACOS
		); 

		// Create a random publisher id
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
		
		FileOutputStream fos = new FileOutputStream( new File( RESOURCES.getAbsolutePath() + "/META-INF/AIR/publisherid" ) );
		fos.write( pubid.getBytes() );
		fos.close(); 
	}
}
