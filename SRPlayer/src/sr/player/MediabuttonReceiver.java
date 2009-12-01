package sr.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class MediabuttonReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		//Check if the button was pressed or release
		KeyEvent MediaButtonEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Boolean MediaButtonEnable = prefs.getBoolean("MediaButtonEnable", false);
					
		if (MediaButtonEnable)
		{		
			//The mediabutton should be used for play/stop
			if ((MediaButtonEvent != null) && (MediaButtonEvent.getAction() == KeyEvent.ACTION_DOWN))
			{	
				
				
				//Mediabutton is pressed. Toggle the status of
				//the SR Player
				Intent ServiceIntent = new Intent(context, PlayerService.class);
		    	ServiceIntent.addFlags(PlayerService.TOGGLE_STREAMING_STATUS);
		    	context.startService(ServiceIntent);
				
			}
			
	    	//Do not send the broadcast to the receivers with 
	    	//lower priority
	    	abortBroadcast();			

		}
	}
}