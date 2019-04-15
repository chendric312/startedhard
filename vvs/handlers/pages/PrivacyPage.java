package com.vvs.handlers.pages;

import java.util.Map;

import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router.UriResource;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class PrivacyPage extends PageSessionTemplateHandler {

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
			String html = this.getHTML("templates/privacy.html", session);
	    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
