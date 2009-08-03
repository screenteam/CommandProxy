package commandproxy.core.commands;

import java.util.Map;

import com.sdicons.json.model.JSONObject;
import com.sdicons.json.model.JSONValue;

import commandproxy.core.Command;
import commandproxy.core.CommandException;
import commandproxy.launcher.Main;

public class Restart implements Command{

	public JSONObject execute( Map<String, String> params ) throws CommandException {
		Main.restart(); 
		
		JSONObject result = new JSONObject(); 
		result.getValue().put( "success", JSONValue.decorate( true ) );
		
		return result; 
	}

	public String getName() {
		return "restart"; 
	}

	
}
