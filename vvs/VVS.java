package com.vvs;

import java.io.File;
import java.util.logging.Logger;

import com.vvs.handlers.BanUserHandler;
import com.vvs.handlers.DeleteHandler;
import com.vvs.handlers.DemoteUserHandler;
import com.vvs.handlers.PromoteUserHandler;
import com.vvs.handlers.StaticResourceHandler;
import com.vvs.handlers.ThumbnailHandler;
import com.vvs.handlers.UnbanUserHandler;
import com.vvs.handlers.UploadHandler;
import com.vvs.handlers.VideoStreamHandler;
import com.vvs.handlers.pages.ConstructionPage;
import com.vvs.handlers.pages.Error401Page;
import com.vvs.handlers.pages.Error404Page;
import com.vvs.handlers.pages.HomePage;
import com.vvs.handlers.pages.LoginEmailPage;
import com.vvs.handlers.pages.LoginUsernamePage;
import com.vvs.handlers.pages.LogoutPage;
import com.vvs.handlers.pages.PostPage;
import com.vvs.handlers.pages.PrivacyPage;
import com.vvs.handlers.pages.RegisterPage;
import com.vvs.handlers.pages.TermsPage;
import com.vvs.handlers.pages.UserPage;
import com.vvs.handlers.pages.ViewPage;
import com.vvs.router.Router;
import com.vvs.sessions.Database;
import com.vvs.sessions.SessionManager;

import fi.iki.elonen.util.ServerRunner;

public class VVS extends Router {
	
	/**
	 * If this is true, the database and user sessions are reset when vvs starts
	 * Note: Setting this to true will purge all application data and reset vvs
	 * DO NOT SET TO TRUE FOR USE IN PRODUCTION!
	 */
	public static final boolean DEVELOPMENT_MODE = false;
	
	/**
	 * Port to listen on
	 */
	private static final int DEPLOYMENT_PORT = 80;
	private static final int DEVELOPMENT_PORT = 8080;
	private static final int PORT = DEVELOPMENT_MODE ? DEVELOPMENT_PORT : DEPLOYMENT_PORT;
	
	/**
	 * The video resources directory
	 */
	public static final File VIDEOS_DIRECTORY = new File("videos");
	
	/**
	 * The video thumbnail resources directory
	 */
	public static final File VIDEOS_THUMBNAILS_DIRECTORY = new File("thumbnails");
	
	/**
	 * The video thumbnail resources directory
	 */
	public static final File SESSIONS_DATABASE = new File(".sessions.db");
	
	/**
	 * The timestamp format for video metadata
	 */
	public static final String TIME_FORMAT = "MMM dd, yyyy @ HH:mm z"; // US Format: MMM dd, yyyy @ hh:mm a z
	
	/**
	 * Path to ffmpeg binary
	 */
	public static final File FFMPEG_PATH = new File("binaries" + File.separator + "ffmpeg").getAbsoluteFile();
	
	/**
     * Logger to log to.
     */
    public static final Logger LOG = Logger.getLogger(VVS.class.getName());
    
	public static void main(String[] args) {
		// create the video resources directory
		VIDEOS_DIRECTORY.mkdirs();
		VIDEOS_THUMBNAILS_DIRECTORY.mkdirs();
		
		// cleanup previous debug sessions if needed
		if(DEVELOPMENT_MODE) {
			LOG.info("Deleting application data...");
			SESSIONS_DATABASE.delete();
			for(File file : VIDEOS_DIRECTORY.listFiles()) {
				if(file.isFile() && (file.getName().endsWith(".mp4") || file.getName().endsWith(".ogg") || file.getName().endsWith(".webm"))){
					file.delete();
				}
			}
			for(File file : VIDEOS_THUMBNAILS_DIRECTORY.listFiles()) {
				if(file.isFile() && file.getName().endsWith(".png")){
					file.delete();
				}
			}
		}
		
		// load and initialize database
		try {
			LOG.info("Connecting to database...");
			Database.getInstance(DEVELOPMENT_MODE);
		} catch (Exception e) {
			LOG.warning("Unable to connect to database - " + e.getMessage());
			return;
		}
		// load previous user sessions
		try {
			LOG.info("Loading saved user sessions...");
			SessionManager.load();
		} catch (Exception e) {
			LOG.warning("Unable to load serialized sessions, all sessions have been reset.");
		}
		// on shutdown save the active user sessions
		Runtime.getRuntime().addShutdownHook(new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					SessionManager.save();
				} catch (Exception e) {
					LOG.warning("Unable to save sessions, all sessions will be reset on restart.");
				}
			}
		}));
		// start the server
		ServerRunner.run(VVS.class);
	}

	public VVS() {
		super(PORT);
		addMappings();
		LOG.info("\nVVS is running! Point your browers to http://localhost:" + PORT + "/ \n");
	}

    /**
     * Add the routes Every route is an absolute path Parameters starts with ":"
     * Handler class should implement @UriResponder interface If the handler not
     * implement UriResponder interface - toString() is used
     */
    @Override
    public void addMappings() {
    	// error handlers
    	setNotImplementedHandler(ConstructionPage.class);
		setNotFoundHandler(Error404Page.class);
		
		// page handlers
    	addRoute("/", HomePage.class);
    	addRoute("/unauthorized", Error401Page.class);
    	addRoute("/login", LoginEmailPage.class);
    	addRoute("/loginalt", LoginUsernamePage.class);
    	addRoute("/logout", LogoutPage.class);
    	addRoute("/privacy", PrivacyPage.class);
    	addRoute("/terms", TermsPage.class);
    	addRoute("/register", RegisterPage.class);
    	addRoute("/view", ViewPage.class);
    	addRoute("/post", PostPage.class);
    	addRoute("/users/:user", UserPage.class);
    	
    	// resource handlers
    	addRoute("/ban/:user", BanUserHandler.class);
    	addRoute("/unban/:user", UnbanUserHandler.class);
    	addRoute("/promote/:user", PromoteUserHandler.class);
    	addRoute("/demote/:user", DemoteUserHandler.class);
    	addRoute("/upload", UploadHandler.class);
    	addRoute("/videos/:video", DeleteHandler.class);
        addRoute("/static/(.)+", StaticResourceHandler.class, new File("static").getAbsoluteFile());
        addRoute("/lib/(.)+", StaticResourceHandler.class, new File("static" + File.separator + "lib").getAbsoluteFile());
        addRoute("/thumbnails/:thumbnail", ThumbnailHandler.class, VIDEOS_THUMBNAILS_DIRECTORY.getAbsoluteFile());
        addRoute("/stream/:resource", VideoStreamHandler.class, VIDEOS_DIRECTORY.getAbsoluteFile());
    }
    
}