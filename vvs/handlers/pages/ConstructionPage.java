package com.vvs.handlers.pages;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import com.vvs.handlers.PageSessionTemplateHandler;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.Database;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class ConstructionPage extends PageSessionTemplateHandler {
	
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
			String html = this.getHTML("templates/construction.html", session);
			
			Database database = null;
			Statement statement = null;
			ResultSet result = null;
			try {
				database = Database.getInstance();
				String randomVideoSQL = "SELECT shortname FROM " + Database.DB_NAME + "." + Database.VIDEOS_TABLE + " ORDER BY RAND() LIMIT 1";
				statement = database.getConnection().createStatement();
				result = statement.executeQuery(randomVideoSQL);
				if (result.next()){
					String shortname = result.getString(Database.VIDEOS_TABLE_SHORTNAME);
					html = html.replace("TEMPLATE_VIDEO_SUGGESTION", "<p>Try watching this <a href=\"/view?video=" + shortname + "\">video</a> instead!</p>");
				} else {
					// failed to find a video suggestion, just show the plain 404 page instead
					html = html.replace("TEMPLATE_VIDEO_SUGGESTION", "");
				}
			} catch (Throwable t){
				// failed to find a video suggestion, just show the plain 404 page instead
				html = html.replace("TEMPLATE_VIDEO_SUGGESTION", "");
			} finally {
				try { if(result != null) result.close(); } catch (Exception e) {};
				try { if(statement != null) statement.close(); } catch (Exception e) {};
				try { if(database != null) database.close(); } catch (Exception e) {};
			}
	    	return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), html);
		} catch (Throwable t) {
			return new Error500Page(t).getResponse();
		}
    }

}
