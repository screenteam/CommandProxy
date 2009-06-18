package commandproxy.core;

public class CommandException extends Exception{
	// whatever... 
	private static final long serialVersionUID = 1L;

	// who's causing trouble? 
	private Command command; 
	
	// what's the big deal? 
	String message; 
	
	// nested exception
	Exception nested; 
	
	/**
	 * Create a new commandexception object
	 * 
	 * @param message The error message
	 * @param command The command that failed
	 * @param nested The original cause
	 */
	public CommandException( String message, Command command, Exception nested ){
		this.command = command; 
		this.message = message;
		this.nested = nested; 
	}
	
	/**
	 * Create a new commandexception object
	 * 
	 * @param message The error message
	 * @param command The error object
	 */
	public CommandException( String message, Command command ){
		this( message, command, null ); 
	}
	
	/**
	 * @return The error message
	 */
	public String getMessage(){
		return message; 
	}
	
	/**
	 * @return The command that failed
	 */
	public Command getCommand(){
		return command; 
	}

	
	/**
	 * @return The cause of the problem
	 */
	public Exception getNested(){
		return nested; 
	}
}
