package eu.webnitro.lcd4android;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.webkit.JavascriptInterface;


class JSInterface {
    private Context context;
    private boolean isplaying = false;
    public JSInterface(Context c)
    {
    	this.context = c;
    }    
    @JavascriptInterface
    public void PlayAlert()
    {
		if(!isplaying)
		{	
			try{
				final MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.alert);
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
 }