package com.vvs.handlers.pages;

import java.util.Map;

import com.vvs.handlers.PageTemplateHandler;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class LogoutPage extends PageTemplateHandler {

	@Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }
    
	@Override
    public Response get(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		try {
			String html = this.getHTML("templates/login.html");
			StringBuilder feedback = new StringBuilder();
			feedback.append("<p style=\"color:red;\">You have been logged out.</p>");
			html = html.replace("TEMPLATE_MESSAGE", feedback.toString());
	    	Response response = NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
	    	
			CookieHandler cookies = session.getCookies();
			UserSession userSession = SessionManager.find(cookies);
			if(userSession != null) {
				userSession.destroy(cookies);
				cookies.unloadQueue(response);
			}
			
			return response;
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
