/**
  * This file is part of SR Player for Android
  *
  * SR Player for Android is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License version 2 as published by
  * the Free Software Foundation.
  *
  * SR Player for Android is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with SR Player for Android.  If not, see <http://www.gnu.org/licenses/>.
  */
package sr.player;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SRPlayerAlarm extends BroadcastReceiver {
	
	public static void HandleAlarmStateChange(Context context)
    {
    	//SharedPreferences settings = getPreferences(0);		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	int Hour = settings.getInt("AlarmHour", 6);
    	int Minute = settings.getInt("AlarmMinute", 0);
    	int Rep = settings.getInt("AlarmRep", 255);
    	String AlarmStationName = settings.getString("AlarmStationName", "P1");
    	String AlarmStationURL = settings.getString("AlarmStationURL", "rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp");
    	
    	int AlarmStationID = settings.getInt("AlarmStationID", 132);
    	boolean AlarmEnable = settings.getBoolean("AlarmEnable", false);
    	AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    	
    	
    	    	
    	if (AlarmEnable)
    	{
    	Log.d(SRPlayer.TAG,"Alarm enabled");
    	//Calculate when the next alarm should go off
    	
    	//First check which day it is
    	Calendar CurrDateTime = new GregorianCalendar();
    	Calendar NextAlarmTime = new GregorianCalendar();
    	
    	NextAlarmTime.set(Calendar.HOUR_OF_DAY, Hour);
    	NextAlarmTime.set(Calendar.MINUTE, Minute);
    	NextAlarmTime.set(Calendar.SECOND, 0);
    	
    	//Check which day of the week it is
    	int WeekDay = CurrDateTime.get(Calendar.DAY_OF_WEEK);
    	int WeekDayAdjust = ((WeekDay+5) % 7); //Adjust so that monday is 0, tuesday 1 and so on    	
    	boolean FoundAlarm = false;
    	//Check if the alarm should be active today
    	if (((Rep & (1<<WeekDayAdjust)) != 0) &&
    	   (NextAlarmTime.compareTo(CurrDateTime) > 0))
    	{
    		//The alarm should be activated this
    		//day at the time set by NextAlarmTime    		    		
    		FoundAlarm = true;
    	}
    	else
    	{
    		//The next alarmtime is the first day after today that 
    		//the alarm is active. Find that day
    		int DayCount = 1;
    		NextAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
	    		while(true)
	    		{    		
		    		WeekDay = NextAlarmTime.get(Calendar.DAY_OF_WEEK);
		        	WeekDayAdjust = ((WeekDay+5) % 7); //Adjust so monday is 0, tuesday 1 and so on
		        	if ((Rep & (1<<WeekDayAdjust)) != 0)
		        	{
		        		//Found the day
		        		FoundAlarm = true;
		        		break;
		        	}
		        	
		    		if (DayCount >= 7)
		    			break;
		    		NextAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
	    		}
    	}
    	    	
    		Intent intent        = new Intent(context, SRPlayerAlarm.class);
    		intent.putExtra("AlarmStationID", AlarmStationID);
    		intent.putExtra("AlarmStationName", AlarmStationName);
    		intent.putExtra("AlarmStationURL", AlarmStationURL);
    		
    		PendingIntent sender = PendingIntent.getBroadcast(context, 001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    		
    		//Cancel the current alarm since it may have been changed
    		am.cancel(sender);
    		
	    	if (FoundAlarm)
	    	{
	    		//Log.d(TAG,"Next alarm will be " + NextAlarmTime.getTime());	    		
	    		//Resources res = context.getResources();
	    		//String[] items = res.getStringArray(R.array.week_days);
	    		//Log.d(SRPlayer.TAG, "New alarm set on :" + items[WeekDayAdjust]);
	    		am.set(AlarmManager.RTC_WAKEUP, NextAlarmTime.getTimeInMillis(), sender); // to be alerted 30 seconds from now
	    	}	    	
	    		    
    	}
    	else
    	{
    		Log.d(SRPlayer.TAG,"Alarm diabled");
        	Intent intent        = new Intent(context, SRPlayerAlarm.class);
        	PendingIntent sender = PendingIntent.getBroadcast(context, 001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
        	am.cancel(sender);
        	
    	}
    }
	
	@Override
	public void onReceive(Context con, Intent in) {
			Log.d(SRPlayer.TAG,"Alarm received");					
			
			//Aquire the wakelock
			AlarmAlertWakeLock.acquireCpuWakeLock(con);
			
			Intent ServiceIntent = new Intent(con, PlayerService.class);
        	ServiceIntent.addFlags(PlayerService.ALARM_START);    
        	
        	//Log.d(SRPlayer.TAG,"Alarm receivde. " + in.getIntExtra("AlarmStationID",132) + " " + in.getStringExtra("AlarmStationName") + " " + in.getStringExtra("AlarmStationURL"));
        	
        	ServiceIntent.putExtra("AlarmStationID",  in.getIntExtra("AlarmStationID",132));
        	ServiceIntent.putExtra("AlarmStationName", in.getStringExtra("AlarmStationName"));
        	ServiceIntent.putExtra("AlarmStationURL", in.getStringExtra("AlarmStationURL"));
        	con.startService(ServiceIntent);
        	
        	HandleAlarmStateChange(con);
	}
	
}