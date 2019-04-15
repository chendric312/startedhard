package com.vvs.router;

import fi.iki.elonen.NanoHTTPD.Response.IStatus;

/**
 * A responder for fixed length responses
 */
public abstract class FixedLengthResponder extends DefaultResponder {

    public abstract String getMimeType();

    public abstract IStatus getStatus();

}