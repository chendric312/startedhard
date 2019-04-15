package com.vvs.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.vvs.router.FixedLengthResponder;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public abstract class PageTemplateHandler extends FixedLengthResponder {

	public Response getRedirect(String uri) {
		// apparently browsers get confused without the trailing slash
		// reference: https://github.com/NanoHttpd/nanohttpd/blob/master/webserver/src/main/java/org/nanohttpd/webserver/SimpleWebServer.java#L400
		int paramsIndex = uri.indexOf('?');
		if(paramsIndex != -1) {
			String base = uri.substring(0, paramsIndex);
			if(!base.endsWith("/")) {
				base += "/";
			}
			uri = base + uri.substring(paramsIndex);
		} else {
			if(!uri.endsWith("/")) {
				uri += "/";
			}
		}
		// REDIRECT_SEE_OTHER=303: Redirect for undefined reason. Typically, 'Operation has completed, continue elsewhere.' Clients making subsequent requests for this resource should not use the new URI. Clients should follow the redirect for POST/PUT/DELETE requests, but use GET for the follow-up request.
		Response response = NanoHTTPD.newFixedLengthResponse(Status.REDIRECT_SEE_OTHER, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
		response.addHeader("Location", uri);
		return response;
	}

	public String getHostIP() {
		try {
			final DatagramSocket socket = new DatagramSocket();
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			String ip = socket.getLocalAddress().getHostAddress();
			socket.close();
			return ip;
		} catch (Throwable t) {
			try {
				InetAddress localhost = InetAddress.getLocalHost();
				return localhost.getHostAddress();
			} catch (Throwable t2) {
				return "127.0.0.1";
			}
		}
	}
	
	public String getHostExternalIP() {
		String ip = "";
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
	        BufferedReader in = null;
	        try {
	            in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
	            ip = in.readLine();
	            return ip;
	        } finally {
	        	try {  if (in != null) in.close(); } catch (IOException e) {}
	        }
		} catch (Exception e) {
			ip = "127.0.0.1";
		}
		return ip;
    }
	
	public static class NavBarItem {
		private boolean active;
		private String name;
		private String link;
		
		public NavBarItem(String name, String link) {
			this.active = false;
			this.name = name;
			this.link = link;
		}
		
		public NavBarItem(String name, String link, boolean active) {
			this.active = active;
			this.name = name;
			this.link = link;
		}
		
		@Override
		public String toString() {
			return "<li" + (active ? " class=\"active\"" : "") + "><a href=\"" + link + "\">" + name + "</a></li>";
		}
	}
	
	protected String getHTML(String template) throws IOException {
		return getHTML(template, null);
	}
	
	protected String getHTML(String template, String url) throws IOException {
		ArrayList<NavBarItem> navBarItems = new ArrayList<NavBarItem>();
		navBarItems.add(new NavBarItem("Home","/", "/".equals(url)));
		navBarItems.add(new NavBarItem("Login","/login", "/login".equals(url)));
		navBarItems.add(new NavBarItem("Register","/register", "/register".equals(url)));
		return getHTML(template, true, navBarItems);
	}
	
	protected String getHTML(String template, boolean darkNavBar, ArrayList<NavBarItem> navBarItems) throws IOException {
		String html = new String(Files.readAllBytes(Paths.get(template)));
		html = html.replace("TEMPLATE_NAVBAR_STYLE", (darkNavBar ? "navbar-inverse" : "navbar-default"));
		StringBuilder navigationLinks = new StringBuilder();
		for(NavBarItem navBarItem : navBarItems) {
			navigationLinks.append(navBarItem.toString());
		}
		html = html.replace("TEMPLATE_NAVBAR_LINKS", navigationLinks.toString());
		html = html.replace("TEMPLATE_APPLICATION_HOSTNAME", getHostExternalIP());
		html = html.replace("TEMPLATE_DATE", new SimpleDateFormat("yyyy").format(new Date()).toString());
		return html;
	}

}
