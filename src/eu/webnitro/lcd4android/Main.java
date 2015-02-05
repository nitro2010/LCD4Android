package eu.webnitro.lcd4android;

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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothService mBluetoothService = null;
	private static final int REQUEST_ENABLE_BT = 3;
	private boolean mSecure = true;
	// Name of the connected device
    private String mConnectedDeviceName = null;

	boolean isplaying = false;
	
	private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

	LayoutInflater inflater;
	View  alertdialog;
	EditText alert_msg;
	Dialog dialog;
	
	private ProgressDialog mProgressDlg;
	

	
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
    	mSecure = (shared_db.SharedLoadData("BLUETOOTH_SECURE_CONNECTION").contentEquals("1")) ? true : false;
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
    	if(connection_type == 1){
        	web.loadUrl(url);
    	}else{
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
				shared_db.SharedSaveData("TYPE_CONNECTION","1");
				web.loadUrl(url);
				if (mBluetoothService != null)
					mBluetoothService.stop();
			}
			break;
			case R.id.choose_bluetooth: {
				connection_type = 2;
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
			case R.id.reload_page: 
				web.reload();
			break;		
		
			case R.id.bluetooth_conn_insecure: {
				mBluetoothService.stop();
				mBluetoothService = null;
				shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION","0");
				mSecure=false;	
				BluetoothListeningService();
			}
			break;
			
			case R.id.bluetooth_conn_secure: {
				mBluetoothService.stop();
				mBluetoothService = null;
				shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION","1");
				mSecure=true;	
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
		return false;		
	}

	
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(connection_type == 1)		
		{		
			menu.setGroupVisible(R.id.internet_group, true);
			menu.setGroupVisible(R.id.bluetooth_group, false);
		}		
		if(connection_type == 2)		
		{	
			menu.setGroupVisible(R.id.internet_group, false);	
			menu.setGroupVisible(R.id.bluetooth_group, true);
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
    	mBluetoothService = new BluetoothService(this, mHandler);
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
		shared_db.SharedSaveData("BLUETOOTH_SECURE_CONNECTION",((mSecure)?"1":"0"));	
		if (mBluetoothService != null)
			mBluetoothService.stop(); 
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
