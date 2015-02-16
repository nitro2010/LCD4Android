package eu.webnitro.lcd4android;

import android.content.SharedPreferences;
import android.util.Log;

public class shared_db {
	private static int DB_VERSION = 3;
	static String PREFERENCES_NAME = "settings";
	static SharedPreferences preferences;
	
	public static void SharedSaveData(String name,String value)	
	{
		SharedPreferences.Editor se = preferences.edit();
		se.putString(name, value);
		se.commit();	
	}		
	
	public static String SharedLoadData(String name)
	{
		return preferences.getString(name, "");	
	}
	
	static void StartDatabase() {
		//check DB version
		String db_version = SharedLoadData("DB_VERSION");
		// user install app first time
		if(db_version.isEmpty())
		{
			//DATA IN VERSION 1
			String dbv = Integer.toString(DB_VERSION);
			SharedSaveData("DB_VERSION", dbv); 
			SharedSaveData("TYPE_CONNECTION","1");
			SharedSaveData("SERVER_URL","file:///android_asset/welcome.html");
			SharedSaveData("BLUETOOTH_SECURE_CONNECTION","2");
			SharedSaveData("USB_STORAGE", "/data/data/eu.webnitro.lcd4android/files");
			
			//DATA IN VERSION 2
			SharedSaveData("USB_STORAGE", "/data/data/eu.webnitro.lcd4android/files");
			
			
		}
	
		//updates and deletes from 2-x versions
		if(DB_VERSION > Integer.parseInt(SharedLoadData("DB_VERSION")))
		{
			Log.d("DATABASE", "UPDATING..");
			SharedSaveData("DB_VERSION", Integer.toString(DB_VERSION)); 
			//UPDATES IN VERSION 2

		}
	}
}