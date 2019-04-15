package com.vvs.handlers.pages;

import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
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

public class ViewPage extends PageSessionTemplateHandler {

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
			// parse out the post parameters
			// this is because of an open bug in nanohttpd
			// see https://github.com/NanoHttpd/nanohttpd/issues/427
			Map<String,String> files = new HashMap<String,String>();
			session.parseBody(files);
			
			this.parseTimezonePreference(session);
			
			// video properties
			boolean videoExists = false;
			String videoShortname = null;
			String videoTitle = null;
			String videoType = null;
			String videoOwner = "";
			String videoDescription = "";
			String videoPosted = "";
			int videoViews = 0;
			
			// URL GET parameter ?video=<shortname>
			List<String> shortnameParams = session.getParameters().get("video");
			if(shortnameParams != null && !shortnameParams.isEmpty()) {
				String shortnameParam = shortnameParams.get(0);
				if(!shortnameParam.equals("")) {
					videoShortname = URLDecoder.decode(shortnameParam, "UTF-8");
					
					// lookup the video by its shortname
					String sql = "SELECT " 
							+ Database.VIDEOS_TABLE_TITLE + ", " 
							+ Database.VIDEOS_TABLE_DESCRIPTION + ", " 
							+ "CONVERT_TZ(" + Database.VIDEOS_TABLE_POSTED + ", @@session.time_zone, '+00:00') AS " + Database.VIDEOS_TABLE_POSTED + ", " 
							+ Database.VIDEOS_TABLE_OWNER + ", " 
							+ Database.VIDEOS_TABLE_VIEWS + ", " 
							+ Database.VIDEOS_TABLE_TYPE 
							+ " FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE 
							+ " WHERE " + Database.VIDEOS_TABLE_SHORTNAME + "='" + videoShortname + "' LIMIT 1;";
					
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
							videoDescription = result.getString(Database.VIDEOS_TABLE_DESCRIPTION);
							videoViews = result.getInt(Database.VIDEOS_TABLE_VIEWS);
							videoPosted = TimeUtils.getFormattedLocaleTime(result.getTimestamp(Database.VIDEOS_TABLE_POSTED), timezone);
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
						
						// increment the video view count by one
						if(videoExists) {
							// update the database
							sql = "UPDATE " + Database.DB_NAME + "." + Database.VIDEOS_TABLE + " SET views=views+1 WHERE shortname='" + videoShortname +  "';";
							statement = database.getConnection().createStatement();
							statement.execute(sql);
						}
					} catch (SQLException | ClassNotFoundException | NoSuchAlgorithmException | ParseException e) {
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
				}
			}
			
			// build the html response
			String html = this.getHTML("templates/view.html", session);
			StringBuilder content = new StringBuilder();
			
			if(videoExists) {
				html = html.replace("TEMPLATE_VIDEO_TITLE", videoTitle);
				String videoFile = videoShortname + "." + videoType;
				content.append("<h1 style=\"white-space: nowrap; width: 90%; overflow: hidden; text-overflow: ellipsis;\">" + videoTitle + "</h1>");
				content.append("<video src=\"/stream/" + videoFile + "\" width=\"640\" height=\"390\" class=\"mejs-player\" data-mejsoptions='{\"alwaysShowControls\": true}'></video>");
				content.append("<br/>");
				content.append("<div style=\"max-width: 640px; height: 140px;\">\n" + 
						"       <div style=\"float: left; max-width: 400px; width: 100%; height: 100%;\">\n" + 
						"         <pre style=\"text-align: left; height: 100%;\">" + videoDescription + "</pre>\n" + 
						"       </div>\n" + 
						"       <div style=\"float: left; margin-left: 20px; max-width: 220px; width: 100%;\">\n" + 
						"         <pre><b>Views: " + videoViews + "</b></pre>\n" + 
						"         <pre><b>Posted by: <a href=\"/users/" + videoOwner + "\">" +  videoOwner + "</a></b></pre>\n" + 
						"         <pre><b>" +  videoPosted + "</b></pre>\n" +
						"       </div>\n" + 
						"     </div>\n");
				content.append("<script type=\"text/javascript\">\n" + 
						"	    $(document).ready(function() {\n" + 
						"				var v = document.getElementsByTagName(\"video\")[0];\n" + 
						"				new MediaElement(v, {success: function(media) {\n");
				if(session.getParameters().containsKey("t")) {
					try {
						long seconds = Math.abs(Long.parseLong(session.getParameters().get("t").get(0)));
						content.append("				media.setCurrentTime(" + seconds + ");\n");
					} catch (Exception e) {
						return new Error500Page(e).getResponse();
					}
				}
				content.append("	    		media.play();\n" + 
						"				}});\n" + 
						"	    });\n" + 
						"</script>\n");
			} else {
				html = html.replace("TEMPLATE_VIDEO_TITLE", "Video Not Found");
				content.append("<h1 style=\"position: absolute; left:50%; top:50%; -webkit-transform:translateY(-50%,-50%); -ms-transform:translateY(-50%,-50%); transform:translate(-50%,-50%);\">Sorry, we couldn't find that video :(</h1>");
			}
			
			html = html.replace("TEMPLATE_VIDEO_CONTENT", content.toString());
			
	    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
