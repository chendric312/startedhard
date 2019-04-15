package com.vvs.handlers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import com.vvs.handlers.pages.Error404Page;
import com.vvs.router.DefaultResponder;
import com.vvs.router.Router;
import com.vvs.router.Router.UriResource;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class ThumbnailHandler extends DefaultResponder {

    @Override
    public Response get(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
        String thumbnail = Router.normalizeUri(uriParams.get("thumbnail"));
        if(thumbnail.endsWith(".png")) {
        	 File fileOrDirectory = new File(uriResource.initParameter(File.class).getAbsoluteFile() + File.separator + thumbnail);
             if (!fileOrDirectory.exists() || !fileOrDirectory.isFile()) {
                 return new Error404Page().get(uriResource, uriParams, session);
             } else {
                 try {
                 	String mimeType = NanoHTTPD.getMimeTypeForFile(fileOrDirectory.getName());
                     return NanoHTTPD.newChunkedResponse(Status.OK, mimeType, getFileInputStream(fileOrDirectory));
                 } catch (IOException ioe) {
                     return NanoHTTPD.newFixedLengthResponse(Status.REQUEST_TIMEOUT, "text/plain", (String) null);
                 }
             }
        } else {
        	return new Error404Page().get(uriResource, uriParams, session);
        }
    }

    private BufferedInputStream getFileInputStream(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }
}