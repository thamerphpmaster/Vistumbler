package com.eiri.wifidb_uploader;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ScanService extends Service {
	private static final String TAG = "WiFiDB_ScanService";
	private static Timer timer;
	private Context ctx;
	static WifiManager wifi;
	SharedPreferences sharedPrefs;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		ctx = this; 
		Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Log.d(TAG, "onCreate");   		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show(); 	
		// Stop Timer
		if(timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
				
		// Stop WiFi
		wifi = null;
						
		// Stop GpS
		GPS.stop(ctx);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onStart");
		timer = new Timer();
		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.startScan();		
		//Setup GpS
		GPS.start(ctx);
		//Setup Timer
		Integer RefreshInterval = Integer.parseInt(sharedPrefs.getString("wifidb_upload_interval", "10000"));
		Log.d(TAG, "RefreshInterval:" + RefreshInterval);
		timer.scheduleAtFixedRate(new mainTask(), 0, RefreshInterval);
	}
	
	private class mainTask extends TimerTask
    { 
        public void run() 
        {
        	
        	//Initiate Wifi Scan
        	wifi.startScan();
        	
        	// Get Prefs
        	String WifiDb_ApiURL = sharedPrefs.getString("wifidb_upload_api_url", "http://dev01.wifidb.net/wifidb/api/");
        	String WifiDb_Username = sharedPrefs.getString("wifidb_username", "Anonymous"); 
        	String WifiDb_ApiKey = sharedPrefs.getString("wifidb_upload_api_url", "");     	
        	String WifiDb_SID = "1";
        	Log.d(TAG, "WifiDb_ApiURL: " + WifiDb_ApiURL + " WifiDb_Username: " + WifiDb_Username + " WifiDb_ApiKey: " + WifiDb_ApiKey + " WifiDb_SID: " + WifiDb_SID);
	    		    
        	// Get Location
        	Location location = GPS.getLocation(ctx);
        	final Double latitude = location.getLatitude();
        	final Double longitude = location.getLongitude();
        	Integer sats = MyLocation.getGpsStatus(ctx);      	
        	Log.d(TAG, "LAT: " + latitude + "LONG: " + longitude + "SATS: " + sats);
    	    	    
        	// Get Wifi Info
        	List<ScanResult> results = ScanService.wifi.getScanResults();
        	for (ScanResult result : results) {    	    	    
            	 	Log.d(TAG, "onReceive() http post");
                  	WifiDB post = new WifiDB();
    	    	    String Label = "";
    	   	    	post.postLiveData(WifiDb_ApiURL, WifiDb_Username, WifiDb_ApiKey, WifiDb_SID, result.SSID, result.BSSID, result.capabilities, result.frequency, result.level, latitude, longitude, Label);
     	    }
        }
    }	
}