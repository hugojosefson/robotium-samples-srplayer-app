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

import sr.player.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class PlayerWidget extends AppWidgetProvider {
	
	private static String ChannelName = "";
	
	private static int ServerStatus=PlayerService.STOP;
	
	private static Integer ThisappWidgetId = -1;

	private RightNowChannelInfo rightNowInfo;
	
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		ThisappWidgetId = -1;
		
		super.onDeleted(context, appWidgetIds);
	}


	@Override 
    public void onReceive(Context context, Intent intent) 
    { 
		Log.d(getClass().getSimpleName(), "PlayerWidget onReceive");
         super.onReceive(context, intent); 
         if(intent.getAction().equals("sr.playerwidget.START")) 
         { 
              Intent ServiceIntent = new Intent(context, PlayerService.class);
          	  ServiceIntent.addFlags(PlayerService.TOGGLE_STREAMING_STATUS);
          	  context.startService(ServiceIntent);
          	  
          	  //In order to get better response when pressing the 
          	  //button. Change the status of the widget.
          	if (ServerStatus == PlayerService.STOP) 
            {
          		ServerStatus = PlayerService.BUFFER;
            }
            else if (ServerStatus == PlayerService.BUFFER)
            {
            	ServerStatus = PlayerService.STOP;
            }
            else if (ServerStatus == PlayerService.PLAY)
            {
            	ServerStatus = PlayerService.STOP;
            }
          	if (ThisappWidgetId >= 0)
          	{
          		AppWidgetManager manager = AppWidgetManager.getInstance(context);
          		this.UpdateWidget(context, manager, ThisappWidgetId);
          	}
          	
         }
         if(intent.getAction().equals("sr.playerservice.UPDATE")) 
         { 
        	 //The service has sent information about
        	 //the current channel and its status
        	 Log.d(getClass().getSimpleName(), "Service update intent received");        	 
             
        	 //Store the data from the intent
        	 // sr.playerservice.RIGHT_NOW_INFO
        	 Object obj = intent.getSerializableExtra("sr.playerservice.RIGHT_NOW_INFO");
        	 if (obj instanceof RightNowChannelInfo ) {
        		 this.rightNowInfo = (RightNowChannelInfo)obj;
        	 } else {
        		 this.rightNowInfo = null;
        	 }
        	 ChannelName = intent.getStringExtra("sr.playerservice.CHANNEL_NAME");
        	 ServerStatus = intent.getIntExtra("sr.playerservice.PLAYER_STATUS", 0);
        	 
        	 if (ServerStatus == PlayerService.PAUSE)
        		 ServerStatus = PlayerService.STOP;
        	 
        	 Log.d(getClass().getSimpleName(), "Service status = " + String.valueOf(ServerStatus));        
        	 
        	 if (ThisappWidgetId >= 0)
        	 {
	        	 AppWidgetManager manager = AppWidgetManager.getInstance(context);
	        	 this.UpdateWidget(context, manager, ThisappWidgetId);
        	 }
         }
         
    }
	
	
	private void UpdateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) 
	{
		Log.d(getClass().getSimpleName(), "Updating view");           	 
		
		//Update the graphical interface		
    	RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.srplayer_widget);
		
    	
    	Intent clickintent=new Intent("sr.playerwidget.START"); 
    	PendingIntent pendingIntentClick=PendingIntent.getBroadcast(context, 0, clickintent, PlayerService.GET_INFO);
        updateViews.setOnClickPendingIntent(R.id.PlayPauseW, pendingIntentClick); 
                
        Intent StartIntent = new Intent(context, SRPlayer.class); 
        PendingIntent pendingConfigIntentClick = PendingIntent.getActivity(context, 0, StartIntent, PendingIntent.FLAG_UPDATE_CURRENT);	        	        
        updateViews.setOnClickPendingIntent(R.id.ConfigButtonW, pendingConfigIntentClick);
                              
        updateViews.setTextViewText(R.id.ChannelNameW, ChannelName);
        String str = "";
        if ( this.rightNowInfo != null ) {
			if ( ! this.rightNowInfo.getProgramTitle().trim().equals("") )
				str = this.rightNowInfo.getProgramTitle();
			else if ( ! this.rightNowInfo.getSong().trim().equals("") )
				str = this.rightNowInfo.getSong();
		}
        updateViews.setTextViewText(R.id.CurrentProgNameW, str);
        
        str = "";
        if ( this.rightNowInfo != null ) {
			if ( ! this.rightNowInfo.getNextProgramTitle().trim().equals("") )
				str = this.rightNowInfo.getNextProgramTitle();
			else if ( ! this.rightNowInfo.getNextSong().trim().equals("") )
				str = this.rightNowInfo.getNextSong();
		}
        updateViews.setTextViewText(R.id.NextProgNameW, str);
         
        if (ServerStatus == PlayerService.STOP)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseW, R.drawable.play_white);
        }
        else if (ServerStatus == PlayerService.BUFFER)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseW, R.drawable.buffer_white);
        }
        else if (ServerStatus == PlayerService.PLAY)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseW, R.drawable.pause_white);
        }
                		
		appWidgetManager.updateAppWidget(appWidgetId, updateViews);
	
        
	}
	
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            
    	ThisappWidgetId = appWidgetIds[0];
    	
    	//Start service if not already started
    	Log.d( getClass().getSimpleName(), "Init start of new service" );
    	
    	//Generate an intent to get information about the status
    	//and the current channel from the service
    	Intent ServiceIntentNew = new Intent(context, PlayerService.class);
    	ServiceIntentNew.addFlags(PlayerService.GET_INFO);
    	context.startService(ServiceIntentNew);
    	
    	    	
    	UpdateWidget(context, appWidgetManager, appWidgetIds[0]);	
    	    
    }

}

