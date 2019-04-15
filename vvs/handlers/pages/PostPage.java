package com.vvs.handlers.pages;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router.UriResource;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class PostPage extends PageSessionTemplateHandler {

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
			String html = this.getHTML("templates/post.html", "/post", session);
			
			List<String> messages = session.getParameters().get("message");
			StringBuilder feedback = new StringBuilder();
			if(messages != null) {
				for(String message : messages) {
					message = URLDecoder.decode(message, "UTF-8");
					feedback.append("<p style=\"color:red;\">" + message + "</p>");
				}
			}
			html = html.replace("TEMPLATE_MESSAGE", feedback.toString());
			
	    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
