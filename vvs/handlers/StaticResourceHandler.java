package com.vvs.handlers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.vvs.handlers.pages.Error404Page;
import com.vvs.router.DefaultResponder;
import com.vvs.router.Router;
import com.vvs.router.Router.UriResource;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class StaticResourceHandler extends DefaultResponder {

    @Override
    public Response get(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
        String baseUri = uriResource.getUri();
        String realUri = Router.normalizeUri(session.getUri());
        for (int index = 0; index < Math.min(baseUri.length(), realUri.length()); index++) {
            if (baseUri.charAt(index) != realUri.charAt(index)) {
                realUri = Router.normalizeUri(realUri.substring(index));
                break;
            }
        }
        File fileOrDirectory = uriResource.initParameter(File.class);
        for (String pathPart : getPathArray(realUri)) {
            fileOrDirectory = new File(fileOrDirectory, pathPart);
        }
        if (fileOrDirectory.isDirectory()) {
            fileOrDirectory = new File(fileOrDirectory, "index.html");
            if (!fileOrDirectory.exists()) {
                fileOrDirectory = new File(fileOrDirectory.getParentFile(), "index.htm");
            }
        }
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
    }
    
    private static String[] getPathArray(String uri) {
        String array[] = uri.split("/");
        ArrayList<String> pathArray = new ArrayList<String>();

        for (String s : array) {
            if (s.length() > 0) {
                pathArray.add(s);
            }
        }

        return pathArray.toArray(new String[]{});
    }

    private BufferedInputStream getFileInputStream(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }
}