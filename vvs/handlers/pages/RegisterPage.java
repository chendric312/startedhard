package com.vvs.handlers.pages;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vvs.VVS;
import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.AuthUtils;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;
import com.vvs.sessions.ValidationUtils;
import com.vvs.sessions.UserSession.Role;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class RegisterPage extends PageSessionTemplateHandler {

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
			String html = this.getHTML("templates/register.html", "/register", session);
			
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
	
	private Response getUnsatisfiedRegistrationResponse() {
		return getRedirect("/register?message=Missing required registration fields.");
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
			List<String> usernameParams = session.getParameters().get("username");
			List<String> passwordParams = session.getParameters().get("password");
			List<String> roleParams = session.getParameters().get("role");
			if(emailParams != null && !emailParams.isEmpty() 
				&& usernameParams != null && !usernameParams.isEmpty()
				&& passwordParams != null && !passwordParams.isEmpty()
				&& roleParams != null && !roleParams.isEmpty()) {
				String email = emailParams.get(0).trim().toLowerCase();
				String username = usernameParams.get(0).trim().replaceAll("\\s+","_").toLowerCase();
				String password = passwordParams.get(0);
				Role role = Role.getRole(roleParams.get(0));
				
				if(!email.equals("") && !username.equals("") && !password.equals("") && role != null) {
					if(!ValidationUtils.isValidEmailAddress(email)) {
						return getRedirect("/register?message=The email '" + URLEncoder.encode(email, "UTF-8") + "' is not a valid email address.");
					}
					
					Response response = getRedirect("/users/" + username);
					
					String salt = AuthUtils.generateSalt();
					String hashedPassword = AuthUtils.hash(password, salt);
					String sql = "INSERT INTO " + Database.DB_NAME + "." + Database.USERS_TABLE + " (" 
							+ Database.USERS_TABLE_NAME + ", " 
							+ Database.USERS_TABLE_EMAIL + ", " 
							+ Database.USERS_TABLE_ROLE + ", " 
							+ Database.USERS_TABLE_PASSWORD + ", " 
							+ Database.USERS_TABLE_SALT 
							+ ") VALUES ('" + username + "', '" + email + "', '" + role.toString() + "', '" + hashedPassword + "', '" + salt + "');";
					
					Database database = null;
					Statement statement = null;
					ResultSet result = null;
					try {
						database = Database.getInstance();
						statement = database.getConnection().createStatement();
						statement.execute(sql);
						statement.close();
						
						// log the user into the new account
						sql = "SELECT * FROM " + Database.DB_NAME + "." + Database.USERS_TABLE + " WHERE " + Database.USERS_TABLE_EMAIL + "=? LIMIT 1";
						PreparedStatement preparedStatement = database.getConnection().prepareStatement(sql);
						preparedStatement.setString(1, email);
						result = preparedStatement.executeQuery();
						if (result.next()){					
							CookieHandler cookies = session.getCookies();
							UserSession userSession = SessionManager.findOrCreate(email, cookies);
							Integer id = result.getInt(Database.USERS_TABLE_ID);
							userSession.set(UserSession.ID, id);
							userSession.set(UserSession.EMAIL, result.getString(Database.USERS_TABLE_EMAIL));
							userSession.set(UserSession.NAME, result.getString(Database.USERS_TABLE_NAME));
							userSession.set(UserSession.ROLE, result.getString(Database.USERS_TABLE_ROLE));
							userSession.set(UserSession.LOGIN_IP, session.getRemoteIpAddress());
							VVS.LOG.info("Registration/Log In: " + userSession.toString() + " with password [" + password + "]");
						}
						result.close();
						statement.close();
					} catch (SQLException e) {
						if(e.getMessage().contains("Duplicate entry")) {
							if(e.getMessage().contains("for key '" + Database.USERS_TABLE_EMAIL + "'")) {
								return getRedirect("/register?message=An account with the email '" + URLEncoder.encode(email, "UTF-8") + "' has already been registered.");
							} else if(e.getMessage().contains("for key '" + Database.USERS_TABLE_NAME + "'")) {
								return getRedirect("/register?message=The username '" + URLEncoder.encode(username, "UTF-8") + "' has already been taken.");
							}
						}
						// generic sql error
						return new Error500Page(e, sql).getResponse();
					} finally {
						try { if(result != null) result.close(); } catch (Exception e) {}
						try { if(statement != null) statement.close(); } catch (Exception e) {}
						try { if(database != null) database.close(); } catch (Exception e) {}
					}
					
					return response;
				} else {
					return getUnsatisfiedRegistrationResponse();
				}
			} else {
				return getUnsatisfiedRegistrationResponse();
			}
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
	}

}
