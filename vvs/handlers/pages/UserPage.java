package com.vvs.handlers.pages;

import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.TimeUtils;
import com.vvs.sessions.UserSession;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class UserPage extends PageSessionTemplateHandler {

	private static final int NUM_USER_HOMEPAGE_VIDEOS = 60;
	
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
			this.parseTimezonePreference(session);
	
			String html = this.getHTML("templates/user.html", session);
			String user = Router.normalizeUri(uriParams.get("user")).toLowerCase();
			html = html.replace("TEMPLATE_USERNAME", user);
			
			List<String> messages = session.getParameters().get("message");
			StringBuilder feedback = new StringBuilder();
			if(messages != null) {
				for(String message : messages) {
					message = URLDecoder.decode(message, "UTF-8");
					feedback.append("<p style=\"color:red;\">" + message + "</p>");
				}
			}
			html = html.replace("TEMPLATE_MESSAGE", feedback.toString());
	
			// lookup the user
			String sql = "SELECT * FROM " + Database.DB_NAME + "." + Database.USERS_TABLE + " WHERE " + Database.USERS_TABLE_NAME + "=? LIMIT 1";
			Database database = null;
			Statement statement = null;
			ResultSet result = null;
			try {
				database = Database.getInstance();
				statement = database.getConnection().prepareStatement(sql);
				((PreparedStatement)statement).setString(1, user);
				result = ((PreparedStatement)statement).executeQuery();
				
				PreparedStatement preparedStatement = database.getConnection().prepareStatement(sql);
				preparedStatement.setString(1, user);
				result = preparedStatement.executeQuery();
				if (result.next()){
					Integer userID = result.getInt(Database.USERS_TABLE_ID);
					String userRole = result.getString(Database.USERS_TABLE_ROLE);
					Boolean userBanned = result.getBoolean(Database.USERS_TABLE_BANNED);
					result.close();
					statement.close();
					
					boolean isCurrentUserHomepage = false;
					boolean isCurrentUserAdministrator = false;
					boolean isCurrentUserBanned = userBanned;
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
									isCurrentUserHomepage = true;
								}
							}
						}
					}
					
					// set the video cell size
					html = html.replace("TEMPLATE_CELL_HEIGHT", ((isCurrentUserHomepage || isCurrentUserAdministrator) ? "300" : "264"));
					
					StringBuilder adminControls = new StringBuilder();
					if(isCurrentUserAdministrator && !isCurrentUserHomepage) {
						adminControls.append("<h1>Administrator Controls</h1>\n");
						adminControls.append("<table>\n");
						adminControls.append("<tr>\n");
						if(isCurrentUserBanned) {
							adminControls.append("<td align=\"center\"><form name=\"unban\" action=\"/unban/" + user + "\" method=\"get\" onSubmit=\"return confirm('Are you sure you want to unban this user?');\" role=\"form\"><button type=\"submit\" class=\"btn btn-sm btn-default\">Unban User</button>&nbsp;</form></td>");
						} else {
							adminControls.append("<td align=\"center\"><form name=\"ban\" action=\"/ban/" + user + "\" method=\"get\" onSubmit=\"return confirm('Are you sure you want to ban this user?');\" role=\"form\"><button type=\"submit\" class=\"btn btn-sm btn-default\">Ban User</button>&nbsp;</form></td>");
						}
						if(userRole.equals(UserSession.Role.ADMIN.toString())) {
							adminControls.append("<td align=\"center\"><form name=\"demote\" action=\"/demote/" + user + "\" method=\"get\" onSubmit=\"return confirm('Are you sure you want to demote this user to a standard user account?');\" role=\"form\">&nbsp;<button type=\"submit\" class=\"btn btn-sm btn-default\">Demote User</button></form></td>");
						} else {
							adminControls.append("<td align=\"center\"><form name=\"promote\" action=\"/promote/" + user + "\" method=\"get\" onSubmit=\"return confirm('Are you sure you want to promote this user to an administrator account?');\" role=\"form\">&nbsp;<button type=\"submit\" class=\"btn btn-sm btn-default\">Promote User</button></form></td>");
						}
						adminControls.append("</tr>\n");
						adminControls.append("</table><br/>\n");
					}
					html = html.replace("TEMPLATE_ADMIN_CONTROLS", adminControls.toString());
					
					html = html.replace("TEMPLATE_HEADER", isCurrentUserHomepage ? "Your Recent Video Uploads" : ("Recent Video Uploads by " + user));
					
					// lookup top n videos by the user that posted them
			        sql = "SELECT " + Database.VIDEOS_TABLE_TITLE + ", " 
			        		+ Database.VIDEOS_TABLE_SHORTNAME + ", " 
			        		+ "CONVERT_TZ(" + Database.VIDEOS_TABLE_POSTED + ", @@session.time_zone, '+00:00') AS " + Database.VIDEOS_TABLE_POSTED + ", "
			        		+ Database.VIDEOS_TABLE_VIEWS 
			        		+ " FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE 
			        		+ " WHERE " + Database.VIDEOS_TABLE_OWNER + "='" + userID + "'" 
			        		+ " ORDER BY " + Database.VIDEOS_TABLE_POSTED + " DESC"
		    				+ " LIMIT " + NUM_USER_HOMEPAGE_VIDEOS + ";";
			        
			        // execute query
			        statement = database.getConnection().createStatement();
					result = statement.executeQuery(sql);
			        
					// count the number of sql results
					int numResults = 0;
					try {
						result.last();
						numResults = result.getRow();
						result.beforeFirst();
					} catch (Exception ex) {
						numResults = 0;
					}

					StringBuilder userVideos = new StringBuilder();
					userVideos.append("<h2>Videos</h2>");
					if (numResults == 0) {
						if(isCurrentUserHomepage) {
							userVideos.append("<p>You haven't posted any videos yet. Get busy!</p>");
						} else {
							userVideos.append("<p>Sadly, this user hasn't posted any videos yet...</p>");
						}
					} else {
						userVideos.append("<br />");
						userVideos.append("<div class=\"slidegrid\">");
						while (result.next()){
							
							String shortname = result.getString(Database.VIDEOS_TABLE_SHORTNAME);
							String title = result.getString(Database.VIDEOS_TABLE_TITLE);
							int views = result.getInt(Database.VIDEOS_TABLE_VIEWS);
							String posted = TimeUtils.getFormattedLocaleTime(
									result.getTimestamp(Database.VIDEOS_TABLE_POSTED), timezone);

							userVideos.append("<div class=\"cell\" style=\"background: #e7e7e7;\">"
									+ "<center>" 
									+ "<a href=\"/view?video=" + shortname + "\">" 
									+ "<div style=\"margin-top:5px; margin-bottom:5px; white-space: nowrap; width: 90%; overflow: hidden; text-overflow: ellipsis;\"><b>" + title + "</b></div>"
									+ "</a>" 
									+ "<a href=\"/view?video=" + shortname + "\">" + "<img src=\"/thumbnails/" + shortname + ".png\" width=\"90%\" height=\"90%\"/>" + "</a>" 
									+ "<div style=\"margin-top:5px; margin-bottom:5px; white-space: nowrap; width: 90%; overflow: hidden; text-overflow: ellipsis;\"><i>" +  + views + " view" + (views > 1 ? "s" : "") + "</i> &middot; <i>" + posted + "</i></div>"
									+ ((isCurrentUserHomepage || isCurrentUserAdministrator) ? "<form name=\"delete-video\" action=\"/videos/" + shortname + "\" method=\"get\" onSubmit=\"return confirm('Are you sure you want to delete the video [" + title + "] with " + views + " views?');\" role=\"form\"><button type=\"submit\" class=\"btn btn-sm btn-default\">Delete Video</button>&nbsp;</form>" : "")
									+ "</center>"
									+ "</div>");
						}
						userVideos.append("</div>");
					}
					html = html.replace("TEMPLATE_USER_HOMEPAGE_CONTENT", userVideos.toString());
				} else {
					html = html.replace("TEMPLATE_USERNAME", "User Not Found");
					html = html.replace("TEMPLATE_CELL_HEIGHT", "264");
					html = html.replace("TEMPLATE_ADMIN_CONTROLS", "");
					html = html.replace("TEMPLATE_HEADER", "User Not Found");
					html = html.replace("TEMPLATE_USER_HOMEPAGE_CONTENT", "<p>Sorry, the user '" + user + "' does not exist.</p>");
				}
			} catch (SQLException | ClassNotFoundException | NoSuchAlgorithmException | ParseException e) {
				if(e instanceof SQLException) {
					return new Error500Page(e, sql).getResponse();
				} else {
					return new Error500Page(e).getResponse();
				}
			} finally {
				try { if(result != null) result.close(); } catch (Exception e) {}
				try { if(statement != null) statement.close(); } catch (Exception e) {}
				try { if(database != null) database.close(); } catch (Exception e) {}
			}
			
	    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
