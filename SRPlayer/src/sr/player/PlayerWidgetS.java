package sr.player;
import sr.player.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class PlayerWidgetS extends AppWidgetProvider {
	
	private static String ChannelName = "";
	
	private static int ServerStatus=PlayerService.STOP;
	
	private static Integer ThisappWidgetId = -1;
	
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		ThisappWidgetId = -1;
		
		super.onDeleted(context, appWidgetIds);
	}


	@Override 
    public void onReceive(Context context, Intent intent) 
    { 
         super.onReceive(context, intent); 
         if(intent.getAction().equals("sr.playerwidgets.START")) 
         { 
              Intent ServiceIntent = new Intent(context, PlayerService.class);
          	  ServiceIntent.addFlags(PlayerService.TOGGLE_STREAMING_STATUS);
          	  context.startService(ServiceIntent);
         }
         if(intent.getAction().equals("sr.playerservice.UPDATE")) 
         { 
        	 //The service has sent information about
        	 //the current channel and its status
        	 Log.d(getClass().getSimpleName(), "Service update intent received");        	 
             
        	 //Store the data from the intent
        	 ChannelName = intent.getStringExtra("sr.playerservice.CHANNEL_NAME");
        	 ServerStatus = intent.getIntExtra("sr.playerservice.PLAYER_STATUS", 0);
        	 Log.d(getClass().getSimpleName(), "Service status = " + String.valueOf(ServerStatus));        
        	 
        	 if (ThisappWidgetId >= 0)
        	 {
	        	 AppWidgetManager manager = AppWidgetManager.getInstance(context);
	        	 UpdateWidget(context, manager, ThisappWidgetId);
        	 }
         }
         
    }
	
	
	private void UpdateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) 
	{
		//Update the graphical interface		
    	RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.srplayer_widgets);
		
    	
    	Intent clickintent=new Intent("sr.playerwidgets.START"); 
    	PendingIntent pendingIntentClick=PendingIntent.getBroadcast(context, 0, clickintent, PlayerService.GET_INFO);
        updateViews.setOnClickPendingIntent(R.id.PlayPauseWS, pendingIntentClick); 
               
        Intent StartIntent = new Intent(context, SRPlayer.class); 
        PendingIntent pendingConfigIntentClick = PendingIntent.getActivity(context, 0, StartIntent, PendingIntent.FLAG_UPDATE_CURRENT);	        	        
        updateViews.setOnClickPendingIntent(R.id.ConfigButtonWS, pendingConfigIntentClick);
                       
        updateViews.setTextViewText(R.id.ChannelNameWS, ChannelName);
         
        if (ServerStatus == PlayerService.STOP)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseWS, R.drawable.play_white);
        }
        else if (ServerStatus == PlayerService.BUFFER)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseWS, R.drawable.buffer_white);
        }
        else if (ServerStatus == PlayerService.PLAY)
        {
        	updateViews.setImageViewResource(R.id.PlayPauseWS, R.drawable.pause_white);
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

