package commandproxy.core.commands;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import commandproxy.core.Command;
import commandproxy.core.CommandException;
import commandproxy.launcher.Main;

public class Restart implements Command{

	public JSONObject execute( Map<String, String> params ) throws CommandException, JSONException {
		Main.restart(); 
		
		JSONObject result = new JSONObject(); 
		result.put( "success", true );
		
		return result; 
	}

	public String getName() {
		return "restart"; 
	}

	
}
