package commandproxy.core;

import java.util.Map;

import com.sdicons.json.model.JSONObject;

public interface Command{
	public String getName(); 
	public JSONObject execute( Map<String, String> params ) throws CommandException; 
}
