package sr.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectionStateReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

            boolean noConnectivity =
                intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (!noConnectivity) {
                //Data connection available
            	//Restart the download service
            	Intent ServiceIntent = new Intent(context, DownloadPodcastService.class);            	            	  
            	context.startService(ServiceIntent);
            }            
       }
		/*
		else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN);

            boolean enabled;
            if (state == WifiManager.WIFI_STATE_ENABLED) {
                enabled = true;
            } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                enabled = false;
            } else {
                return;
            }

            // Notify network provider of current wifi enabled state
            if (mNetworkLocationProvider != null) {
                mNetworkLocationProvider.updateWifiEnabledState(enabled);
            }
		*/
         	        
	}
}