package com.vvs.handlers;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.vvs.VVS;
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

public class DeleteHandler extends PageSessionTemplateHandler {

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
			String videoShortname = Router.normalizeUri(uriParams.get("video")).toLowerCase();

			// lookup the video by its shortname
			String sql = "SELECT " 
					+ Database.VIDEOS_TABLE_TITLE + ", " 
					+ Database.VIDEOS_TABLE_OWNER + ", " 
					+ Database.VIDEOS_TABLE_TYPE 
					+ " FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE 
					+ " WHERE " + Database.VIDEOS_TABLE_SHORTNAME + "='" + videoShortname + "' LIMIT 1;";
			
			// video properties
			boolean videoExists = false;
			String videoTitle = "";
			String videoType = null;
			String videoOwner = "";
			
			Database database = null;
			Statement statement = null;
			ResultSet result = null;
			try {
				// search the database
				database = Database.getInstance();
				statement = database.getConnection().createStatement();
				result = statement.executeQuery(sql);
				
				// if there is a result then get the video properties from the database
				if(result.next()) {
					videoExists = true;
					videoTitle = result.getString(Database.VIDEOS_TABLE_TITLE);
					videoType = result.getString(Database.VIDEOS_TABLE_TYPE);
					String videoOwnerId = result.getString(Database.VIDEOS_TABLE_OWNER);
					result.close();
					statement.close();
					
					// get the username of the video's owner
					sql = "SELECT " + Database.USERS_TABLE_NAME + " FROM " + Database.DB_NAME + "." + Database.USERS_TABLE + " WHERE " + Database.USERS_TABLE_ID + "='" + videoOwnerId + "';";
					statement = database.getConnection().createStatement();
					result = statement.executeQuery(sql);
					if(result.next()) {
						videoOwner = result.getString(Database.USERS_TABLE_NAME);
					}
				}
				
				// check permissions
				boolean isCurrentUserVideoOwner = false;
				boolean isCurrentUserAdministrator = false;
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
							if(userSession.get(UserSession.NAME).equals(videoOwner)) {
								isCurrentUserVideoOwner = true;
							}
						}
					}
				}
				
				// attempt to delete the video
				if(videoExists) {
					if(isCurrentUserVideoOwner || isCurrentUserAdministrator) {
						// video file
						File videoFile = new File(VVS.VIDEOS_DIRECTORY, videoShortname + "." + videoType);
						if(videoFile.exists()) {
							videoFile.delete();
						}
						
						// delete the thumbnail
						File thumbnail = new File(VVS.VIDEOS_THUMBNAILS_DIRECTORY, (videoShortname + ".png"));
						if(thumbnail.exists()) {
							thumbnail.delete();
						}
						
						// delete SQL record
						// update the banned status
						sql = "DELETE FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE + " WHERE " 
					    		  + Database.VIDEOS_TABLE_SHORTNAME + "='" + videoShortname + "';";

						// update the database
						statement = database.getConnection().createStatement();
						statement.execute(sql);
						
						return getRedirect("/users/" + videoOwner + "?message=Deleted video '" + videoTitle + "'.");
					} else {
						return getRedirect("/unauthorized");
					}
				} else {
					return getRedirect("/users/" + videoOwner + "?message=Video with shortname '" + videoShortname + "' does not exist.");
				}
			} catch (SQLException | ClassNotFoundException | NoSuchAlgorithmException e) {
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
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
