package com.vvs.handlers.pages;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.vvs.VVS;
import com.vvs.handlers.PageTemplateHandler;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Handling error 500 - error
 */
public class Error500Page extends PageTemplateHandler {

	private static boolean DEBUG = true;
	
	private Throwable t;
	private String sql = null;
	
	public Error500Page(Throwable t) {
		this.t = t;
	}
	
	public Error500Page(Throwable t, String sql) {
		this.t = t;
		this.sql = sql;
	}

	public String getText() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
		VVS.LOG.warning(stackTrace);
		
		String html;
		String sqlQuery = (sql != null ? ("<pre>" + sql + "</pre>") : "");
		try {
			html = getHTML("templates/500.html");
			html = html.replace("TEMPLATE_STACK_TRACE", "<pre>" + stackTrace + "</pre>" + sqlQuery);
		} catch (Throwable t) {
			// something went wrong building the 500 error page, fall back to simple page
			StringBuilder simpleHTML = new StringBuilder();
			simpleHTML.append("<html><body><h1>Error 500: Server Error</h1>");
			if(DEBUG) {
				simpleHTML.append("<h2>Stack Trace</h2><pre>" + stackTrace + "</pre>" + sqlQuery);
			}
			simpleHTML.append("</body></html>");
			html = simpleHTML.toString();
		}
		
		return html;
	}

	public String getMimeType() {
		return "text/html";
	}

	public IStatus getStatus() {
		return Status.INTERNAL_ERROR;
	}

	public Response getResponse() {
		return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
	}

}
