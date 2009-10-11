package commandproxy.core;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;


public interface Command{
	public String getName(); 
	public JSONObject execute( Map<String, String> params ) throws CommandException, JSONException; 
}
