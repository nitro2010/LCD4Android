package eu.webnitro.lcd4android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.util.Log;

public class USBService extends Thread {
	@Override
	public void run() {
		synchronized (this) {
			Log.d("USBService","Start checking...");
			File lcd4android_page = new File("/data/data/eu.webnitro.lcd4android/files/lcd4android.page");
			if(lcd4android_page.exists()) {
				try{
					BufferedReader br = new BufferedReader(new FileReader(lcd4android_page));	
					String line = br.readLine();
					br.close();
					int page = Integer.parseInt(line);
						Constants.USB_PAGE = page;
				}catch(Exception e){}
			}
			notify();
		}
	}
}
