package com.vvs.handlers;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.vvs.handlers.pages.Error500Page;
import com.vvs.router.Router;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class BanUserHandler extends PageTemplateHandler {

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
			
			String user = Router.normalizeUri(uriParams.get("user")).toLowerCase();
			
			@SuppressWarnings("unused")
			boolean isCurrentUser = false;
			boolean isCurrentUserAdministrator = false;
			boolean isCurrentUserBanned = false;
			UserSession userSession = null;
			if(session != null) {
				CookieHandler cookies = session.getCookies();
				cookies = session.getCookies();
				userSession = SessionManager.find(cookies);
				if(userSession != null) {
					if(userSession.get(UserSession.ROLE) != null) {
						if(userSession.get(UserSession.ROLE).equals(UserSession.Role.ADMIN.toString())) {
							isCurrentUserAdministrator = true;
						}
					}
					if(userSession.get(UserSession.NAME) != null) {
						if(userSession.get(UserSession.NAME).equals(user)) {
							isCurrentUser = true;
						}
					}
				}
			}
			
			// if the user is not already banned, then ban them
			if(!isCurrentUserBanned) {
				// must be an administrator to perform this action
				if(isCurrentUserAdministrator) {
					// update the banned status
					String sql = "UPDATE " + Database.DB_NAME + "." + Database.USERS_TABLE + " SET " 
				    		  + Database.USERS_TABLE_BANNED + "='1';";

					// update the database
					Database database = null;
					Statement statement = null;
					try {
						database = Database.getInstance();
						statement = database.getConnection().createStatement();
						statement.execute(sql);
						return getRedirect("/users/" + user + "?message=User '" + URLEncoder.encode(user, "UTF-8") + "' has been banned.");
					} catch (SQLException e) {
						if (e instanceof SQLException) {
							return new Error500Page(e, sql).getResponse();
						} else {
							return new Error500Page(e).getResponse();
						}
					} finally {
						try { if (statement != null) statement.close(); } catch (SQLException e) {}
						try { if (database != null) database.close(); } catch (SQLException e) {}
					}
				} else {
					return getRedirect("/unauthorized");
				}
			} else {
				return getRedirect("/users/" + user + "?message=User '" + URLEncoder.encode(user, "UTF-8") + "' has been banned.");
			}
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
