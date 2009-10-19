package commandproxy.cli;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class allows you to read the application descriptor 
 * from .air files
 * 
 * Because air strictly forbids you to add "foreign" attributes 
 * to the descriptor we have to hack the properties into the air 
 * file by adding comments to it, e.g. 
 * <!--
 * vendor=My Company
 * -->
 * @author hansi
 */
public class AirXML {

	private File file;
	private Document xml;
	private XPath xPath;
	private Properties trickConfig; 
	
	public AirXML( File file ) throws ZipException, IOException, ParserConfigurationException, SAXException{
		this.file = file; 
		
		// Create an input stream for application.xml file
		ZipFile archive = new ZipFile( file );
		ZipEntry xmlFile = archive.getEntry( "META-INF/AIR/application.xml" );
		InputStream xmlStream = archive.getInputStream( xmlFile );
		
		
		// Parse the xml
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder builder = docFactory.newDocumentBuilder(); 
		xml = builder.parse( xmlStream ); 
		
		// Setup the xpath object... 
		XPathFactory xFactory = XPathFactory.newInstance(); 
		xPath = xFactory.newXPath();
		
		// Figure out all the comments ... 
		String comments = "";
		
		try {
			XPathExpression query = xPath.compile( "//comment()" );
			NodeList list = (NodeList) query.evaluate( xml, XPathConstants.NODESET );
			
			for( int i = 0; i < list.getLength(); i++ ){
				Comment comment = (Comment) list.item( i );
				String[] tokens = comment.getTextContent().split( "\n" );
				for( String line : tokens ){
					if( line.matches( "[\\s\\w]+=.*" ) ){
						comments += line + "\n"; 
					}
				}
			}
		}
		catch( XPathExpressionException e ){
			e.printStackTrace();
		}
		
		trickConfig = new Properties(); 
		trickConfig.load( new ByteArrayInputStream( comments.getBytes() ) ); 
	}
	
	
	/**
	 * Returns an air-files attributes. 
	 * 
	 * @param path The path to the attribute, e.g. "filename", or "initialWindow/width"
	 * @return The node's value, an empty string if the path wasn't found, or null if the specified path was invalid
	 */
	public String get( String path ){
		// Is that path in our trick-properties?
		if( trickConfig.getProperty( path ) != null ){
			return trickConfig.getProperty( path ); 
		}
		
		try {
			XPathExpression query = xPath.compile( "//" + path + "/text()" );
			return (String) query.evaluate( xml, XPathConstants.STRING ); 
		}
		catch( XPathExpressionException e ){
			e.printStackTrace();
			return null; 
		}
	}

	/**
	 * @return The location of the Air-file
	 */
	public File getFile() {
		return file;
	}
}
