package org.mmbase.security.implementation.context;

import org.mmbase.security.Rank;
import java.util.HashMap;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;

public class PasswordLogin extends ContextLoginModule {
    private static Logger log=Logging.getLoggerInstance(PasswordLogin.class.getName());
    
    public ContextUserContext login(HashMap userLoginInfo, Object[] userParameters) throws org.mmbase.security.SecurityException {

	// get username
	String username = (String)userLoginInfo.get("username");    		
	if(username == null) throw new org.mmbase.security.SecurityException("expected the property 'username' with login");
	
	// get password
	String password = (String)userLoginInfo.get("password");
	if(password == null) throw new org.mmbase.security.SecurityException("expected the property 'password' with login");	
	
	log.debug("request for user: '"+username+"' with pass: '"+password+"'");
	
	String configValue = getModuleValue(username);
	if(configValue == null) {
	    log.info("user with name:" + username + " doesnt have a value for this module");
	    return null;
	}
	if(!configValue.equals(password)) {
	    log.debug("user with name:" + username + " used pass:" + password+ " but needed :" + configValue);	
	    log.info("user with name:" + username + " didnt give the right password");
	    return null;	
	}
	
	Rank rank= getRank(username);	
	if(rank == null) {
	    log.warn( "expected a rank for user with the name:" + username + ", canceling a valid login due to the fact that the rank attribute wasnt set");
	    return null;
    	    
	}
        return getValidUserContext(username, rank);	
    }
}
