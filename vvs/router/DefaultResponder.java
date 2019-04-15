package com.vvs.router;

import java.util.Map;

import com.vvs.handlers.pages.Error500Page;
import com.vvs.router.Router.UriResource;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * A responder with nothing implemented
 */
public abstract class DefaultResponder implements UriResponder {

	@Override
    public Response get(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		return new Error500Page(new IllegalArgumentException("GET method not implemented.")).getResponse();
    }
    
    @Override
	public Response put(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
    	return new Error500Page(new IllegalArgumentException("PUT method not implemented.")).getResponse();
	}

	@Override
	public Response post(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		return new Error500Page(new IllegalArgumentException("POST method not implemented.")).getResponse();
	}

	@Override
	public Response delete(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		return new Error500Page(new IllegalArgumentException("DELETE method not implemented.")).getResponse();
	}

	@Override
	public Response other(String method, UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		return new Error500Page(new IllegalArgumentException("HTTP method not implemented.")).getResponse();
	}
	
}