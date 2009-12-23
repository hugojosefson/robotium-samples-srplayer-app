package sr.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

public class MediabuttonReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(SRPlayer.TAG, "MediabuttonReciever recived event.");
		
		String intentAction = intent.getAction();
		
		if(!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)){			
			return;
		}
		
		KeyEvent MediaButtonEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		
		if (MediaButtonEvent == null) {			
            return;
        }
		
        int keycode = MediaButtonEvent.getKeyCode();
        int action = MediaButtonEvent.getAction();
        
        if (action == KeyEvent.ACTION_DOWN) {        	
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    		Boolean MediaButtonEnable = prefs.getBoolean("MediaButtonEnable", false);
    					
    		if (MediaButtonEnable)
    		{		
                switch (keycode) {

                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                	Log.v(SRPlayer.TAG, "MediabuttonReciever supported event.");
                	Intent ServiceIntent = new Intent(context, PlayerService.class);
    		    	ServiceIntent.addFlags(PlayerService.TOGGLE_STREAMING_STATUS);
    		    	context.startService(ServiceIntent);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                default:
                	Log.v(SRPlayer.TAG, "MediabuttonReciever got not yet supported media key enent.");
                	return;
                }
			
    	    	//Do not send the broadcast to the receivers with 
    	    	//lower priority
    	    	abortBroadcast();			

    		}
        }        
	}
}