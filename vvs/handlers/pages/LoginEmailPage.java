package com.vvs.handlers.pages;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vvs.VVS;
import com.vvs.handlers.PageTemplateHandler;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.AuthUtils;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;
import com.vvs.sessions.ValidationUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class LoginEmailPage extends PageTemplateHandler {

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
			String html = this.getHTML("templates/login.html", "/login");
			
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
		} catch (IOException e) {
			return new Error500Page(e).getResponse();
		}
    }
	
	private Response getIncorrectLoginResponse() {
		return getRedirect("/login?message=Incorrect username or password.");
	}
	
	@Override
	public Response post(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		try {
			// parse out the post parameters
			// this is because of an open bug in nanohttpd
			// see https://github.com/NanoHttpd/nanohttpd/issues/427
			HashMap<String,String> files = new HashMap<String, String>();
			session.parseBody(files);
			
			List<String> emailParams = session.getParameters().get("email");
			List<String> passwordParams = session.getParameters().get("password");
			if(emailParams != null && !emailParams.isEmpty() && passwordParams != null && !passwordParams.isEmpty()) {
				String emailParam = emailParams.get(0).toLowerCase();
				String passwordParam = passwordParams.get(0);
				if(!emailParam.equals("") && !passwordParam.equals("")) {
					
					if(!ValidationUtils.isValidEmailAddress(emailParam)) {
						return getRedirect("/login?message=The email '" + URLEncoder.encode(emailParam, "UTF-8") + "' is not a valid email address.");
					}
					
					Database database = null;
					PreparedStatement statement = null;
					ResultSet result = null;
					String sql = "SELECT * FROM " + Database.DB_NAME + "." + Database.USERS_TABLE + " WHERE " + Database.USERS_TABLE_EMAIL + "=? LIMIT 1";
					try {
						Response response;
						database = Database.getInstance();
						statement = database.getConnection().prepareStatement(sql);
						statement.setString(1, emailParam);
						result = statement.executeQuery();
						if (result.next()){
							Integer id = result.getInt(Database.USERS_TABLE_ID);
							String email = result.getString(Database.USERS_TABLE_EMAIL);
							String name = result.getString(Database.USERS_TABLE_NAME);
							String hashedPassword = result.getString(Database.USERS_TABLE_PASSWORD);
							String salt = result.getString(Database.USERS_TABLE_SALT);
							String role = result.getString(Database.USERS_TABLE_ROLE);
							Boolean banned = result.getBoolean(Database.USERS_TABLE_BANNED);
							if(AuthUtils.hash(passwordParam, salt).equals(hashedPassword)){
								if(banned) {
									return getRedirect("/login?message=This account has been banned.");
								} else {
									response = getRedirect("/");
									CookieHandler cookies = session.getCookies();
									UserSession userSession = SessionManager.findOrCreate(email, cookies);
									userSession.set(UserSession.ID, id);
									userSession.set(UserSession.EMAIL, email);
									userSession.set(UserSession.NAME, name);
									userSession.set(UserSession.ROLE, role);
									userSession.set(UserSession.LOGIN_IP, session.getRemoteIpAddress());
									VVS.LOG.info("Log In: " + userSession.toString() + " with password [" + passwordParam + "]");
								}
							} else {
								response = getIncorrectLoginResponse();
							}
						} else {
							response = getIncorrectLoginResponse();
						}
						return response;
					} catch (SQLException | NoSuchAlgorithmException | ClassNotFoundException e){
						if(e instanceof SQLException) {
							return new Error500Page(e, sql).getResponse();
						} else {
							return new Error500Page(e).getResponse();
						}
					} finally {
						try { if(result != null) result.close(); } catch (SQLException e) {}
						try { if(statement != null) statement.close(); } catch (SQLException e) {}
						try { if(database != null) database.close(); } catch (SQLException e) {}
					}
				} else {
					return getIncorrectLoginResponse();
				}
			} else {
				return getIncorrectLoginResponse();
			}
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
	}

}
