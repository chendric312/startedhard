package com.vvs.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vvs.VVS;
import com.vvs.handlers.pages.Error500Page;
import com.vvs.router.Router.UriResource;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;
import com.vvs.sessions.UserSession;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class UploadHandler extends PageSessionTemplateHandler {

	@Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }
    
    private Response getRedirectErrorResponse(String message) {
		return getRedirect("/post?message=" + message);
	}
    
    private Response getRedirectErrorResponse() {
		return getRedirectErrorResponse("Upload failed, check video file.");
	}
    
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private static String generateShortName(){
		Random rnd = new Random();
		StringBuilder token = new StringBuilder(Database.VIDEOS_TABLE_SHORTNAME_LENGTH);
		for(int i = 0; i < Database.VIDEOS_TABLE_SHORTNAME_LENGTH; i++){
			token.append(ALPHABET[rnd.nextInt(ALPHABET.length)]);
		}
		return token.toString();
	}
    
	@Override
	public Response post(UriResource uriResource, Map<String, String> uriParams, IHTTPSession session) {
		try {
			// parse out the post parameters
			// this is because of an open bug in nanohttpd
			// see https://github.com/NanoHttpd/nanohttpd/issues/427
			HashMap<String,String> files = new HashMap<String, String>();
			session.parseBody(files);

			// get uploaded video title
			String title = "";
			List<String> titleParams = session.getParameters().get("title");
			if(titleParams != null && !titleParams.isEmpty()) {
				String titleParam = titleParams.get(0);
				if(!titleParam.equals("")) {
					title = URLDecoder.decode(titleParam, "UTF-8");
				} else {
					return getRedirectErrorResponse("Upload failed, a video title is required.");
				}
			} else {
				return getRedirectErrorResponse("Upload failed, a video title is required.");
			}
			
			// get uploaded video description
			String description = "";
			List<String> descriptionParams = session.getParameters().get("description");
			if(descriptionParams != null && !descriptionParams.isEmpty()) {
				String descriptionParam = descriptionParams.get(0);
				if(!descriptionParam.equals("")) {
					description = URLDecoder.decode(descriptionParam, "UTF-8");
				} else {
					return getRedirectErrorResponse("Upload failed, a video description is required.");
				}
			} else {
				return getRedirectErrorResponse("Upload failed, a video description is required.");
			}
			
			// get uploaded video file name
			String uploadFileName = "";
			List<String> videoParams = session.getParameters().get("video");
			if(videoParams != null && !videoParams.isEmpty()) {
				String videoParam = videoParams.get(0);
				if(!videoParam.equals("")) {
					uploadFileName = URLDecoder.decode(videoParam, "UTF-8");
				} else {
					return getRedirectErrorResponse();
				}
			} else {
				return getRedirectErrorResponse();
			}
			
			// get uploaded video file
			File uploadedFile = new File(VVS.VIDEOS_DIRECTORY, uploadFileName);
			if (files.containsKey("video")) {
				File tempFile = new File(files.get("video"));
	            if(!tempFile.renameTo(uploadedFile)) {
	            	return getRedirectErrorResponse("Upload failed, error accessing file upload.");
	            }
	        }
			
			// get uploaded video filename extension
			String extension = getFileExtension(uploadFileName).toLowerCase();
			
			// validate media type by extension
			if(!(extension.equals("mp4") || extension.equals("ogg") || extension.equals("webm"))) {
				return getRedirectErrorResponse("Upload failed, file format not supported.");
			}
			
			// generate a shortname alias for the file
			String shortname = generateShortName();
			File renamedFile = new File(VVS.VIDEOS_DIRECTORY, shortname + "." + extension);
			while(renamedFile.exists()) {
				shortname = generateShortName();
				renamedFile = new File(VVS.VIDEOS_DIRECTORY, shortname + "." + extension);
			}
			
			// generate video thumbnail
			File thumbnail = generateThumbnail(uploadedFile, shortname);
			if(!thumbnail.exists()) {
				return getRedirectErrorResponse("Upload failed, error generating thumbnail.");
			}
			
			// rename the video file appropriately
			if(!uploadedFile.renameTo(renamedFile)) {
				return getRedirectErrorResponse("Upload failed, error accessing file upload.");
			}
			uploadedFile = renamedFile;
			
			// check user is properly logged in
			CookieHandler cookies = session.getCookies();
			UserSession userSession = SessionManager.find(cookies);
			if (userSession != null) {
				// insert video into clips table
				String sql = "INSERT INTO " + Database.DB_NAME + "." + Database.VIDEOS_TABLE + "(" 
			    		  + Database.VIDEOS_TABLE_SHORTNAME + ", " 
			    		  + Database.VIDEOS_TABLE_TITLE + ", " 
			    		  + Database.VIDEOS_TABLE_DESCRIPTION + ", " 
			    		  + Database.VIDEOS_TABLE_OWNER + ", " 
			    		  + Database.VIDEOS_TABLE_TYPE 
			    		  + ") VALUES ('" + shortname + "', '" + title + "', '" + description + "', '" + userSession.get(UserSession.ID) + "', '" + extension + "');";

				// update the database
				Database database = null;
				Statement statement = null;
				try {
					database = Database.getInstance();
					statement = database.getConnection().createStatement();
					statement.execute(sql);
					return getRedirect("/view?video=" + shortname);
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
				return getRedirect("/login?message=You must be logged in to post a video.");
			}
		} catch (Exception e) {
			return new Error500Page(e).getResponse();
		}
    }
	
	private File generateThumbnail(File uploadedFile, String shortname) throws IOException {
		File thumbnail = new File(VVS.VIDEOS_THUMBNAILS_DIRECTORY, (shortname + ".png"));
		
		// ffmpeg 
		// -i <uploaded file path> // input video file
		// -ss 00:00:04 -f image2 // save frame at 00:00:04
		// -vframes 1 // just save 1 thumbnail
		// -s qvga <thumbnail path> // output file
		// -y // force overwrite
		// -hide_banner -loglevel panic // quiet mode
		String ffmpegCommand = VVS.FFMPEG_PATH.getAbsolutePath() + " -i \"" + uploadedFile.getAbsolutePath() + "\" -ss 00:00:04 -f image2 -vframes 1 -s qvga \"" + thumbnail.getAbsolutePath() + "\" -y -hide_banner -loglevel panic";
		String shellCommand[] = {"/bin/bash", "-c", ffmpegCommand};

		VVS.LOG.info("Executing: " + ffmpegCommand);
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(shellCommand);

		BufferedReader stdInput = new BufferedReader(new 
		     InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new 
		     InputStreamReader(proc.getErrorStream()));

		// read the stdout from the command
		StringBuilder stdout = new StringBuilder();
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			stdout.append(s + "\n");
		}
		if(stdout.length() > 0) {
			VVS.LOG.info("Command STDOUT: " + stdout.toString());
		}
		
		// read stderr from the command
		StringBuilder stderr = new StringBuilder();
		while ((s = stdError.readLine()) != null) {
		    stderr.append(s + "\n");
		}
		if(stderr.length() > 0) {
			VVS.LOG.info("Command STDERR: " + stderr.toString());
		}
		
		return thumbnail;
	}

	private String getFileExtension(String filename) {
		String extension = "";
		File file = new File(filename);
		int i = file.getName().lastIndexOf('.');
		if (i > 0) {
		    extension = file.getName().substring(i+1);
		}
		return extension;
	}

}
