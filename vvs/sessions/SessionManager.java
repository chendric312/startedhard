package com.vvs.sessions;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Random;

import com.vvs.VVS;

import fi.iki.elonen.NanoHTTPD.Cookie;
import fi.iki.elonen.NanoHTTPD.CookieHandler;

public class SessionManager {
	
	private static final long SESSION_SEED = 0;
	private static final Random RANDOM = new Random(SESSION_SEED);
	private static final int TOKEN_SIZE = 35;
	private static final char[] HEX_ALPHA = new char[]{'A', 'B', 'C', 'D', 'E', 'F'};
	public static final String SESSION_COOKIE_NAME = "VVS_SESSION_ID";
	private static HashMap<String, UserSession> sessions = new HashMap<>();
	
	private static String genSessionToken(String user){
		StringBuilder token = new StringBuilder(TOKEN_SIZE);
		for(int i = 32; i < TOKEN_SIZE; i++){
			token.append(HEX_ALPHA[RANDOM.nextInt(HEX_ALPHA.length)]);
		}
		token.append(md5(user));
		return token.toString();
	}
	
	private static String newSessionToken(String user){
		String token;
		do{
			token = genSessionToken(user);
		} while(sessions.containsKey(token));
		return token;
	}
	
	public static synchronized UserSession find(CookieHandler cookies){
		String token = cookies.read(SESSION_COOKIE_NAME);
		return sessions.get(token);
	}
	
	public static synchronized UserSession findOrCreate(String username, CookieHandler cookies){
		String token = cookies.read(SESSION_COOKIE_NAME);
		if(token == null){
			token = newSessionToken(username);
			int daysTillExpiration = 30;
			Cookie cookie = new Cookie(SESSION_COOKIE_NAME, token, Cookie.getHTTPTime(daysTillExpiration));
			cookies.set(cookie);
		}
		if(!sessions.containsKey(token)){
			sessions.put(token, new UserSession(token));
		}
		return sessions.get(token);
	}
	
	public static void destroy(String token, CookieHandler cookies){
		sessions.remove(token);
		cookies.delete(SESSION_COOKIE_NAME);
	}
	
	@SuppressWarnings("unchecked")
	public static void load() throws Exception {
		if (!VVS.SESSIONS_DATABASE.exists()) {
			return;
		}
		FileInputStream input = new FileInputStream(VVS.SESSIONS_DATABASE);
		sessions = (HashMap<String, UserSession>) new ObjectInputStream(input).readObject();
		input.close();
	}

	public static void save() throws Exception {
		FileOutputStream output = new FileOutputStream(VVS.SESSIONS_DATABASE);
		new ObjectOutputStream(output).writeObject(sessions);
		output.close();
	}
	
	private static String md5(String data){
		String result  = "";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data.getBytes());
			byte[] digest = md.digest();
			for (int i=0; i<digest.length; i++) {
				result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
			}
			return result;
		} catch (Exception e) {
			return result;
		}
	}
}