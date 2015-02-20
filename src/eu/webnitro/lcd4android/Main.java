package eu.webnitro.lcd4android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Main extends Activity {
	WebView web;
	//connection type: 1 - server, 2 - bluetooth
	private int connection_type = 1;
	private String url = "file:///android_asset/welcome.html";
	private String usb_path = "";
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothService mBluetoothService = null;
	private static final int REQUEST_ENABLE_BT = 3;
	private int mSecure = 2;
	// Name of the connected device
    private String mConnectedDeviceName = null;

	boolean isplaying = false;
	boolean USBListenServiceisrunning = false;
	CountDownTimer usbservicetimer = null;
	
	private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

	LayoutInflater inflater;
	View  alertdialog;
	EditText alert_msg;
	Dialog dialog;
	
	private ProgressDialog mProgressDlg;
	
	int menuitem = 10;
	int menusize = 0;
	
	USBService tUSBService = null;
	
//	USBService tUSBService = null;
	Thread tUSBListenService;
	int USB_PAGE = -1;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	setContentView(R.layout.main);
    }
    
    
    @Override
    protected void onStart() {
    	super.onStart();
    	shared_db.preferences = getSharedPreferences(shared_db.PREFERENCES_NAME, Activity.MODE_PRIVATE);
    	shared_db.StartDatabase();
    	connection_type = 	Integer.parseInt(shared_db.SharedLoadData("TYPE_CONNECTION"));
    	url = shared_db.SharedLoadData("SERVER_URL");
    	mSecure = Integer.parseInt(shared_db.SharedLoadData("BLUETOOTH_SECURE_CONNECTION"));
    	usb_path = shared_db.SharedLoadData("USB_STORAGE");
    	web = (WebView) findViewById(R.id.web);
    	web.setVerticalScrollbarOverlay(false);
    	web.getSettings().setJavaScriptEnabled(true);
    	web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
    	web.getSettings().setBuiltInZoomControls(false);
    	web.getSettings().setDisplayZoomControls(false);
    	web.getSettings().setSupportZoom(false);
    	web.addJavascriptInterface(new JSInterface(this), "AndroidJS");
    	web.setWebChromeClient(new WebChromeClient());
    	web.setWebViewClient(new WebViewClient(){
    		@Override
    		public boolean shouldOverrideUrlLoading(WebView view, String url) {
    			return false;
    		}
    	});    	
    	dialog = new Dialog(this);
    	dialog.setContentView(R.layout.alertdialog);
    	dialog.setTitle(getResources().getString(R.string.type_url_to_new_server));
    	alert_msg = (EditText) dialog.findViewById(R.id.alert_msg);    	
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	if(!new File(usb_path).exists()) usb_path = getFilesDir().getAbsolutePath();
    	if(connection_type == 1){
        	web.loadUrl(url);
    	}else if(connection_type == 2){
    		if (mBluetoothAdapter == null) {
    			connection_type = 1;
				shared_db.SharedSaveData("TYPE_CONNECTION","1"); 
				web.loadUrl(url); 
    		}else{
     		   if (!mBluetoothAdapter.isEnabled()) {
		            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
     		   } else if (mBluetoothService == null) {
     			   BluetoothListeningService();
     		   }
  	        } 
    	}else{
    		USBListenService();
    	}
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
    }
    

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.choose_server: {
				connection_type = 1;
				if (mBluetoothService != null)
					mBluetoothService.stop();
				try {
					if(usbservicetimer != null) {
						usbservicetimer.cancel();
						usbservicetimer = null;	
					}
				}catch(Exception e){}
				shared_db.SharedSaveData("TYPE_CONNECTION","1");
				web.loadUrl(url);
			}
			break;
			case R.id.choose_bluetooth: {
				connection_type = 2;
				try {
					if(usbservicetimer != null) {
						usbservicetimer.cancel();
						usbservicetimer = null;	
					}
				}catch(Exception e){}
				shared_db.SharedSaveData("TYPE_CONNECTION","2");
	    		if (mBluetoothAdapter == null) {
	    			connection_type = 1;
					shared_db.SharedSaveData("TYPE_CONNECTION","1"); 
					web.loadUrl(url); 
	    		}else{
	     		   if (!mBluetoothAdapter.isEnabled()) {
			            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	     		   } else if (mBluetoothService == null) {
	     			   BluetoothListeningService();
	     		   }
	  	        } 
			}		
			break;
			case R.id.change_server: {
				alert_msg.setText(url);
				Button dialogButton = (Button) dialog.findViewById(R.id.button_ok);
				dialogButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						url = alert_msg.getText().toString();
						dialog.dismiss();
						if(url.isEmpty())
						{
							url = "file:///android_asset/welcome.html";
						}
						web.loadUrl(url);
						
					}
				});
				dialog.show();
				shared_db.SharedSaveData("SERVER_URL",url);
			}			
			break;		
			
			case R.id.choose_usb: {
				connection_type = 3;
				shared_db.SharedSaveData("TYPE_CONNECTION","3");
				USBListenService();
			}			
			break;		
			
			case R.id.reload_page: 
				web.reload();
			break;		
		
			case R.id.bluetooth_conn_insecure: {
				mBluetoothService.stop();
				mBluetoothService = null;
				shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION","0");
				mSecure=0;	
				BluetoothListeningService();
			}
			break;
			
			case R.id.bluetooth_conn_secure: {
				mBluetoothService.stop();
				mBluetoothService = null;
				shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION","1");
				mSecure=1;	
				BluetoothListeningService();
			}
			break;

			case R.id.bluetooth_conn_both: {
				mBluetoothService.stop();
				mBluetoothService = null;
				shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION","2");
				mSecure=2;	
				BluetoothListeningService();
			}
			break;
			
			case R.id.discoverable: {
				ensureDiscoverable();
			}
			break;
			case R.id.pairing: {
				mBluetoothAdapter.startDiscovery();
				mProgressDlg = new ProgressDialog(this);
				mProgressDlg.setMessage(getResources().getString(R.string.scanning));
				mProgressDlg.setCancelable(false);
				mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        dialog.dismiss();
				        mBluetoothAdapter.cancelDiscovery();
				    }
				});
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				if (pairedDevices == null || pairedDevices.size() == 0) { 
					Toast.makeText(this, getResources().getString(R.string.no_paired_devices_found), Toast.LENGTH_SHORT).show();
				} else {
					ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
					list.addAll(pairedDevices);
					Intent intent = new Intent(Main.this, DeviceListActivity.class);
					intent.putParcelableArrayListExtra("device.list", list);
					startActivity(intent);		
				}
			}	
			break;			
			case R.id.close_app: {	
				finish();
			}
			break;
		}
		
		if( ( item.getItemId() >= menuitem ) && ( item.getItemId() <= menuitem+menusize  ) ) {
			usb_path = item.getTitle().toString();
			shared_db.SharedSaveData("USB_STORAGE",usb_path);
			new File(usb_path).mkdir();
			new File(usb_path+"/0").mkdir();
			new File(usb_path+"/1").mkdir();			
		 }
		
		return false;		
	}

	
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(connection_type == 1)		
		{		
			menu.setGroupVisible(R.id.internet_group, true);
			menu.setGroupVisible(R.id.bluetooth_group, false);
			menu.setGroupVisible(R.id.usb_group, false);			
		}		
		if(connection_type == 2)		
		{	
			menu.setGroupVisible(R.id.internet_group, false);	
			menu.setGroupVisible(R.id.bluetooth_group, true);
			menu.setGroupVisible(R.id.usb_group, false);
		}	
		if(connection_type == 3)		
		{	
			menu.setGroupVisible(R.id.internet_group, false);	
			menu.setGroupVisible(R.id.bluetooth_group, false);
			menu.setGroupVisible(R.id.usb_group, true);

			
	    	SubMenu m1 = menu.findItem(R.id.storage_web).getSubMenu();
	    	m1.clear();
	     	File filea = getFilesDir();
			m1.add(Menu.NONE,menuitem,100,filea.getAbsolutePath().toString());
			m1.getItem(0).setCheckable(true);
			if(usb_path.contains(filea.getAbsolutePath().toString())) {
				m1.getItem(0).setChecked(true);				
			}
			File[] file = getExternalFilesDirs(null);
			for(int i=0;i<file.length;i++) {
				m1.add(Menu.NONE,menuitem+i+1,100,file[i].getAbsolutePath().toString());
				m1.getItem(i+1).setCheckable(true);
				if(usb_path.contains(file[i].getAbsolutePath().toString())) {
					m1.getItem(i+1).setChecked(true);
				}
			}
			menusize = m1.size();
		}	
    	if (mBluetoothAdapter != null) {
    		menu.findItem(R.id.choose_bluetooth).setVisible(true);
    	}
		return super.onPrepareOptionsMenu(menu);
	}
	
	
    public void PlayAlert()
    {
		if(!isplaying)
		{	
			try{
				final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.alert);
				isplaying=true;
				mediaPlayer.setVolume(1, 1);
				mediaPlayer.start();
				mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						isplaying = false;
					}
				});
			}
			catch(Exception mediaPlayer_exception)
			{
				Log.d("MEDIAPLAYER", mediaPlayer_exception.getMessage());
			}	
		} 
    } 

    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {	
	        case REQUEST_ENABLE_BT:
	            if (resultCode == Activity.RESULT_OK) {
					BluetoothListeningService();
	            } else {
					connection_type = 1;
					shared_db.SharedSaveData("TYPE_CONNECTION","1"); 
	            }	
            break;
        }
        
    }

    
	private void BluetoothListeningService()
	{
		Log.d("BluetoothListeningService", "LETSGO");
    	web.loadData(getResources().getString(R.string.WAITING_FOR_DATA_FROM_BLUETOOTH_DEV), "text/html", "utf-8");
    	mBluetoothService = new BluetoothService(this, mHandler, mSecure);
	}
	
	private void USBListenService()
	{
		Log.d("USBListeningService", "LETSGO");
		web.loadData(getResources().getString(R.string.WAITING_FOR_DATA_FROM_USB_DEV), "text/html", "utf-8");
		try {
			getFilesDir().mkdir();
			new File(usb_path).mkdir();
			new File(usb_path+"/0").mkdir();
			new File(usb_path+"/1").mkdir();
		}catch(Exception e) {}
		
		if(usbservicetimer == null) {
			usbservicetimer = new CountDownTimer(5000, 1000) {
				@Override
				public void onTick(long millisUntilFinished) {}
				@Override
				public void onFinish() {
					tUSBService = new USBService();
					tUSBService.start();
					synchronized (tUSBService) {
						try {
							tUSBService.wait();
							if(USB_PAGE != Constants.USB_PAGE) {
								USB_PAGE = Constants.USB_PAGE;
								if(new File(usb_path + "/"+USB_PAGE+"/lcd4android.html").exists()) {
									BufferedReader usb_file = new BufferedReader(new FileReader("/storage/sdcard1/Android/data/eu.webnitro.lcd4android/files/0/lcd4android.html"));
									StringBuffer stringBuffer = new StringBuffer(); 
									String inputString;
									while ((inputString = usb_file.readLine()) != null) {
										stringBuffer.append(inputString + "\n");
									}
									usb_file.close();
									web.loadData(stringBuffer.toString(), "text/html", "utf-8");						
								}
							}							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					start();
				}
			};
			usbservicetimer.start();
		}
	}

	
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            case Constants.MESSAGE_STATE_CHANGE:
	            	Log.i("BT Service", "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
						case BluetoothService.STATE_CONNECTED:
							//
						break;
						case BluetoothService.STATE_CONNECTING:
							//
						break;
						case BluetoothService.STATE_LISTEN:
						case BluetoothService.STATE_NONE:
							//
						break;
	                }
                break;
	            case Constants.MESSAGE_WRITE:
	            	byte[] writeBuf = (byte[]) msg.obj;
	            	String writeMessage = new String(writeBuf);
                break;
	            case Constants.MESSAGE_READ:
	            	byte[] readBuf = (byte[]) msg.obj;
	            	String readMessage = new String(readBuf, 0, msg.arg1);
            		web.loadData(readMessage, "text/html", "utf-8");
            		mBluetoothService.write("ok\r\n".getBytes());
                break;
	            case Constants.MESSAGE_DEVICE_NAME:
	            	mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                break;
            }
        }
    };

    
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }	

    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
		shared_db.SharedSaveData("TYPE_CONNECTION",Integer.toString(connection_type));
		shared_db.SharedSaveData("SERVER_URL",url);		
		shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION",Integer.toString(mSecure));	
		shared_db.SharedSaveData("USB_STORAGE", usb_path);
		if (mBluetoothService != null)
			mBluetoothService.stop(); 
		try {
			if(usbservicetimer != null) {
				usbservicetimer.cancel();
				usbservicetimer = null;	
			}
			if(connection_type == 3) {
				new File("/data/data/eu.webnitro.lcd4android/files/lcd4android.page").delete();
			}
		}catch(Exception e){}		
		unregisterReceiver(mReceiver);
    }
    
    
    @Override
    public synchronized void onPause() {
        super.onPause();
    }   
    
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(connection_type == 1){
        	web.reload();
        }else{
            if (mBluetoothService != null) {
                if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                	mBluetoothService.start();
                }
            }    
        }
    }    

    
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {	    	
	        String action = intent.getAction();
	        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	        	mDeviceList = new ArrayList<BluetoothDevice>();
				mProgressDlg.show();
	        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	mProgressDlg.dismiss();
	        	Intent newIntent = new Intent(Main.this, DeviceListActivity.class);
	        	newIntent.putParcelableArrayListExtra("device.list", mDeviceList);
				startActivity(newIntent);
	        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	        	BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	mDeviceList.add(device);
	        }
	    }
	};
}
