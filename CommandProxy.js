CommandProxy = {
	url : "", 
	port : "37148", 
	key : "secret!", 
	
	/**
	 * Called by AIR when the application is started.
	 * This is used to determine the port number of the 
	 * command proxy. 
	 */
	onInvoke : function( event ){
		for( arg in event.arguments ){
			if( event.arguments[arg].indexOf( "--port=" ) == 0 ){
				CommandProxy.port = parseInt( event.arguments[arg].substring( 7 ) ); 
			}
			if( event.arguments[arg].indexOf( "--key=" ) == 0 ){
				CommandProxy.key = event.arguments[arg].substring( 6 ); 
			}
		}
		
		CommandProxy.url = "http://localhost:" + CommandProxy.port + "/"; 
	}, 
	
	/**
	 * Use this to call a command on the command proxy server. 
	 *
	 * @param command The name of the command
	 * @param parameters A map of parameters, e.g. { "file": "/myfile.pdf" }
	 * @param (optional) callback, a function with one parameter, that will be given a json object when the result is ready.  
	 */
	call : function( command, parameters, callback ){
		if( !parameters )
			parameters = {}; 
		
		
		parameters.cpAppDir = air.File.applicationDirectory.nativePath; 
		parameters.cpKey = CommandProxy.key; 
		
		if( CommandProxy.url == "" ){
			if( callback ){
				callback( { "error" : "no connection to the command proxy" } );
			}
			else{
				return { "error" : "no connection to the command proxy" }; 
			}
		}
		else{
			// Let's set everything up.. 
			var paramString = ""; 
			for( var param in parameters ){
				paramString += encodeURI( param ) + "=" + encodeURI( parameters[param] ) + "&"; 
			}
			
			var async = (typeof (callback) == "function");
			
			var req = new XMLHttpRequest();
			req.open( "POST", CommandProxy.url + command, async ); 
			
			req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
			req.setRequestHeader("Content-length", paramString.length);
			req.setRequestHeader("Connection", "close");
			
			if( async ){
				req.onreadystatechange = function(){
					
					if( req.readystate == 4 && callback ) {
						if( req.status == 200 ){
							callback( JSON.parse( req.responseText ) ); 
						}
						else{
							callback( { "error" : req.responseText } ); 
						}
					}
				};
				req.send( paramString ); 
			}
			else{
				req.send( paramString ); 
				var ret = JSON.parse( req.responseText );
				
				return ret; 
			}
		}
	}
};

air.NativeApplication.nativeApplication.addEventListener( air.InvokeEvent.INVOKE, CommandProxy.onInvoke );