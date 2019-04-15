package com.vvs.handlers.pages;

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Map;

import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.Database;
import com.vvs.sessions.TimeUtils;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class HomePage extends PageSessionTemplateHandler {
	
	private static final int NUM_HOMEPAGE_VIDEOS = 60;
	
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
			String html = this.getHTML("templates/index.html", "/", session);
			
			StringBuilder homepageContent = new StringBuilder();
			
			// get top n trending videos
	        String sql = "SELECT " + Database.VIDEOS_TABLE_TITLE + ", " 
	        		+ Database.VIDEOS_TABLE_SHORTNAME + ", " 
	        		+ "CONVERT_TZ(" + Database.VIDEOS_TABLE_POSTED + ", @@session.time_zone, '+00:00') AS " + Database.VIDEOS_TABLE_POSTED + ", "
	        		+ Database.VIDEOS_TABLE_VIEWS 
	        		+ " FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE 
	        		+ " ORDER BY " + Database.VIDEOS_TABLE_VIEWS + " DESC, " + Database.VIDEOS_TABLE_POSTED + " DESC"
    				+ " LIMIT " + NUM_HOMEPAGE_VIDEOS + ";";
			
	        Database database = null;
			Statement statement = null;
			ResultSet result = null;
			try {
				database = Database.getInstance();
				statement = database.getConnection().createStatement();
				result = statement.executeQuery(sql);
				
				// count the number of sql results
				int numResults = 0;
				try {
				    result.last();
				    numResults = result.getRow();
				    result.beforeFirst();
				}
				catch(Exception ex) {
					numResults = 0;
				}
				
				if(numResults == 0) {
					homepageContent.append("<h2>Coming soon!</h2>");
				} else {
					homepageContent.append("<br />");
					homepageContent.append("<div class=\"slidegrid\">");
					while (result.next()){
						String shortname = result.getString(Database.VIDEOS_TABLE_SHORTNAME);
						String title = result.getString(Database.VIDEOS_TABLE_TITLE);
						int views = result.getInt(Database.VIDEOS_TABLE_VIEWS);
						String posted = TimeUtils.getFormattedLocaleTime(result.getTimestamp(Database.VIDEOS_TABLE_POSTED), timezone);
						homepageContent.append("<div class=\"cell\" style=\"background: #e7e7e7;\">"
								+ "<center>"
								+ "<a style=\"color:black;\" href=\"/view?video=" + shortname + "\">"
								+ "<div style=\"margin-top:5px; margin-bottom:5px; white-space: nowrap; width: 90%; overflow: hidden; text-overflow: ellipsis;\"><b>" + title + "</b></div>"
								+ "</a>"
								+ "<a href=\"/view?video=" + shortname + "\">"
								+ "<img src=\"/thumbnails/" + shortname + ".png\" width=\"90%\" height=\"90%\"/>"
								+ "</a>"
								+ "<div style=\"margin-top:5px; margin-bottom:5px; white-space: nowrap; width: 90%; overflow: hidden; text-overflow: ellipsis;\"><i>" +  + views + " view" + (views > 1 ? "s" : "") + "</i> &middot; <i>" + posted + "</i></div>"
								+ "</center>"
								+ "</div>");
					}
					homepageContent.append("</div>");
				}
				
				html = html.replace("TEMPLATE_HOMEPAGE_CONTENT", homepageContent.toString());
		    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
			} catch (SQLException | ParseException | ClassNotFoundException | NoSuchAlgorithmException e){
				if(e instanceof SQLException) {
					return new Error500Page(e, sql).getResponse();
				} else {
					return new Error500Page(e).getResponse();
				}
			} finally {
				try { if(result != null) result.close(); } catch (Exception e) {};
				try { if(statement != null) statement.close(); } catch (Exception e) {};
				try { if(database != null) database.close(); } catch (Exception e) {};
			}
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
