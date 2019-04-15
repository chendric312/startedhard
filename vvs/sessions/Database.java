package com.vvs.sessions;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.vvs.VVS;

public class Database {

	public static final String DB_NAME = "vvs";
	
	public static final String USERS_TABLE = "users";
	public static final String USERS_TABLE_ID = "id";
	public static final String USERS_TABLE_EMAIL = "email";
	public static final String USERS_TABLE_ROLE = "role";
	public static final String USERS_TABLE_BANNED = "banned";
	public static final String USERS_TABLE_NAME = "name";
	public static final String USERS_TABLE_PASSWORD = "password";
	public static final String USERS_TABLE_SALT = "salt";
	
	public static final String VIDEOS_TABLE = "videos";
	public static final String VIDEOS_TABLE_ID = "id";
	public static final String VIDEOS_TABLE_SHORTNAME = "shortname";
	public static final int VIDEOS_TABLE_SHORTNAME_LENGTH = 10;
	public static final String VIDEOS_TABLE_TITLE = "title";
	public static final String VIDEOS_TABLE_TYPE = "type";
	public static final String VIDEOS_TABLE_DESCRIPTION = "description";
	public static final String VIDEOS_TABLE_POSTED = "posted";
	public static final String VIDEOS_TABLE_OWNER = "owner";
	public static final String VIDEOS_TABLE_VIEWS = "views";
	
	private static final String DB_URL = "jdbc:mysql://localhost:3306";
	private static final String DB_PARAMS = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&allowMultiQueries=true&autoReconnect=true&useSSL=false";
	private static final String DB_USER = "root";
	private static final String DB_PASS = "badpass";
	
	private Connection connection;

	private Database(Connection connection) throws SQLException {
		this.connection = connection;
	}
	
	private void createDatabase() throws SQLException, NoSuchAlgorithmException {
		Statement statement;
		
		String dropDatabaseSQL = "DROP DATABASE IF EXISTS " + DB_NAME + ";";
		statement = connection.createStatement();
		statement.execute(dropDatabaseSQL);
		statement.close();
		
		String createDatabaseSQL = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + ";";
		statement = connection.createStatement();
		statement.execute(createDatabaseSQL);
		statement.close();
		
		String createDatabaseUserSQL = "GRANT USAGE ON *.* TO '" + DB_USER + "'@'0.0.0.0' IDENTIFIED BY '" + DB_PASS + "' WITH MAX_QUERIES_PER_HOUR 0 MAX_CONNECTIONS_PER_HOUR 0 MAX_UPDATES_PER_HOUR 0 MAX_USER_CONNECTIONS 0;";
		statement = connection.createStatement();
		statement.execute(createDatabaseUserSQL);
		statement.close();

		String grantUserPriviligesSQL = "GRANT ALL PRIVILEGES ON " + DB_NAME + ".* TO '" + DB_USER + "'@'0.0.0.0';";
		statement = connection.createStatement();
		statement.execute(grantUserPriviligesSQL);
		statement.close();
		
		String flushUserPrivilegesSQL = "FLUSH PRIVILEGES;";
		statement = connection.createStatement();
		statement.execute(flushUserPrivilegesSQL);
		statement.close();
		
		String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS " + DB_NAME + "." + USERS_TABLE + " (" 
				+ USERS_TABLE_ID + " INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " 
				+ USERS_TABLE_NAME + " VARCHAR(50) NOT NULL, "
				+ USERS_TABLE_EMAIL + " VARCHAR(255) NOT NULL, "
				+ USERS_TABLE_ROLE + " VARCHAR(10) NOT NULL, "
				+ USERS_TABLE_BANNED + " BOOLEAN DEFAULT false, "
				+ USERS_TABLE_PASSWORD + " CHAR(128) NOT NULL, "
				+ USERS_TABLE_SALT + " CHAR(128) NOT NULL, "
				+ "UNIQUE(" + USERS_TABLE_NAME + "), "
				+ "UNIQUE(" + USERS_TABLE_EMAIL + ")"
				+ ") ENGINE = InnoDB;";
		statement = connection.createStatement();
		statement.execute(createUsersTableSQL);
		statement.close();
		
		String salt = AuthUtils.generateSalt();
		String password = AuthUtils.hash("badpass", salt);
		String insertAdminSQL = "INSERT INTO " + DB_NAME + "." + USERS_TABLE + " (" + USERS_TABLE_NAME + ", " + USERS_TABLE_EMAIL + ", " + USERS_TABLE_ROLE + ", " + USERS_TABLE_PASSWORD + ", " + USERS_TABLE_SALT + ") VALUES ('admin', 'admin@vulnerablevideoservice.com', 'ADMIN', '" + password + "', '" + salt + "');";
		statement = connection.createStatement();
		statement.execute(insertAdminSQL);
		statement.close();
		
		String createVideoTableSQL = "CREATE TABLE IF NOT EXISTS " + DB_NAME + "." + VIDEOS_TABLE + " ("
				+ VIDEOS_TABLE_ID + " INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
				+ VIDEOS_TABLE_SHORTNAME + " VARCHAR(" + VIDEOS_TABLE_SHORTNAME_LENGTH + ") NOT NULL , "
				+ VIDEOS_TABLE_TITLE + " VARCHAR(255) NOT NULL, "
				+ VIDEOS_TABLE_TYPE + " VARCHAR(5) NOT NULL, "
				+ VIDEOS_TABLE_DESCRIPTION + " TEXT NOT NULL, "
				+ VIDEOS_TABLE_POSTED + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(), "
				+ VIDEOS_TABLE_OWNER + " INT NOT NULL, "
				+ VIDEOS_TABLE_VIEWS + " INT NOT NULL DEFAULT '0', "
				+ "UNIQUE(" + VIDEOS_TABLE_SHORTNAME + ")"
				+ ") ENGINE = InnoDB;";
		statement = connection.createStatement();
		statement.execute(createVideoTableSQL);
		statement.close();
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void close() throws SQLException {
		connection.close();
	}
	
	public static Database getInstance() throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
		return getInstance(false);
	}
	
	/**
	 * Returns a connection to the database
	 * If reset is true then the database is cleared and reset to its initial empty state
	 * @param reset
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
	public static Database getInstance(boolean reset) throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		Database database = null;
		if(!reset) {
			// try to get the existing database
			try {
				database = new Database(DriverManager.getConnection(DB_URL + "/" + DB_NAME + DB_PARAMS, DB_USER, DB_PASS));
			} catch (Exception e){
				VVS.LOG.info("Database does not exist.");
			}
		}
		if(database == null){
			// database needs to be created or reset
			// if a database already exists it will be destroyed and recreated
			VVS.LOG.info("Creating database...");
			database = new Database(DriverManager.getConnection(DB_URL + "/" + DB_PARAMS, DB_USER, DB_PASS));
			database.createDatabase();
		}
		return database;
	}
	
}
