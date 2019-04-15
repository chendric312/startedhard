package com.vvs.sessions;

import java.io.Serializable;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD.CookieHandler;

/**
 * Stores server side session date
 */
public class UserSession implements Serializable {
	
	public static enum Role {
		ADMIN("ADMIN"),
		USER("USER");
		
		private String name;
		
		private Role(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		public static Role getRole(String role) {
			Role result = null;
			if(role.equalsIgnoreCase(Role.ADMIN.toString())){
				result = Role.ADMIN;
			} else if(role.equalsIgnoreCase(Role.USER.toString())){
				result = Role.USER;
			}
			return result;
		}
	}
	
	public static final String ID = "ID";
	public static final String NAME = "NAME";
	public static final String EMAIL = "EMAIL";
	public static final String ROLE = "ROLE";
	public static final String LOGIN_IP = "LOGIN_IP";
	
	private static final long serialVersionUID = 1L;
	private HashMap<String, Serializable> data = new HashMap<String, Serializable>();
	private final String sessionID;
	
	public UserSession(String sessionID){
		this.sessionID = sessionID;
	}
	
	public Serializable get(String key){
		return data.get(key);
	}

	public void set(String key, Serializable value){
		data.put(key, value);
	}

	public void destroy(CookieHandler cookies) {
		SessionManager.destroy(sessionID, cookies);
	}
	
	@Override
	public String toString() {
		return "[UserSession=" + data.toString() + "]";
	}
}