package com.vvs.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TimeZone;

import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;
import com.vvs.sessions.UserSession.Role;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public abstract class PageSessionTemplateHandler extends PageTemplateHandler {

	protected TimeZone timezone = TimeZone.getDefault();
	
	protected void parseTimezonePreference(IHTTPSession session) {
		CookieHandler cookies = session.getCookies();
		String locale = cookies.read("locale");
		if(locale != null) {
			for(String tzid : TimeZone.getAvailableIDs()) {
				if(tzid.contains(locale)) {
					timezone = TimeZone.getTimeZone(tzid);
				}
			}
		}
	}
	
	protected String getHTML(String template, IHTTPSession session) throws IOException {
		return getHTML(template, null, session);
	}
	
	protected String getHTML(String template, String url, IHTTPSession session) throws IOException {
		CookieHandler cookies = null;
		UserSession userSession = null;
		
		if(session != null) {
			cookies = session.getCookies();
			userSession = SessionManager.find(cookies);
		}
		
		ArrayList<NavBarItem> navBarItems = new ArrayList<NavBarItem>();
		navBarItems.add(new NavBarItem("Home","/", "/".equals(url)));
		
		// dark is the default navbar style
		boolean darkNavBar = true;
		if(userSession != null) {	
			// if user is logged in as an admin then change the navbar style to light
			if(Role.getRole(userSession.get(UserSession.ROLE).toString()) == Role.ADMIN) {
				darkNavBar = false;
			}
			navBarItems.add(new NavBarItem("Post Video","/post", "/post".equals(url)));
			navBarItems.add(new NavBarItem("Logout (" + userSession.get(UserSession.NAME) + ")","/logout", false));
		} else {
			navBarItems.add(new NavBarItem("Login","/login", "/login".equals(url)));
			navBarItems.add(new NavBarItem("Register","/register", "/register".equals(url)));
		}
		
		return super.getHTML(template, darkNavBar, navBarItems);
	}
	
}
