package sr.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootupReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {		
		//Reschedule alarms
		SRPlayerAlarm.HandleAlarmStateChange(context);
	}

}	