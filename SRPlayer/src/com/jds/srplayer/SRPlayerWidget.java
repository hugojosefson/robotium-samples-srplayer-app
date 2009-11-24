package com.jds.srplayer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jds.srplayer.R;

import android.R.integer;
import android.R.string;
import android.app.PendingIntent;
import android.app.Service;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.os.RemoteException;
import android.sax.Element;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SRPlayerWidget extends AppWidgetProvider {
	public static final int INIT_SERVICE = 0;
	public static final int START_STREAMING = 1;
	public static final int STOP_STREAMING = 2;
	public static final int UPDATE_CONFIG = 3;
	
	
	private static String playUrl = "rtsp://lyssna-mp4.sr.se/live/mobile/SR-P3.sdp";
	private static int ChannelIndex = 3;
	private static String ChannelName;
	//private static String playUrl = "rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp";
	public static final int STOP = 0;
	public static final int BUFFER = 1;
	public static final int PLAY = 2;
	
	private static int ServerStatus=STOP;
	
	//Collected data
	private static String CurrentProgramTitle = "";
	private static String NextProgramTitle = "";
	
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
         if(intent.getAction().equals("com.jds.srplayerwidget.START")) 
         { 
              Intent ServiceIntent = new Intent(context, SRPlayerService.class);
          	  ServiceIntent.addFlags(START_STREAMING);
          	  context.startService(ServiceIntent);
         }
         if(intent.getAction().equals("com.jds.srplayerservice.UPDATE")) 
         { 
        	 Log.d(getClass().getSimpleName(), "Service update intent received");        	 
             
        	 //Log.d(getClass().getSimpleName(), "Update: Channelname = " + intent.getStringExtra("com.jds.srplayerservice.CHANNEL_NAME"));
        	 //Log.d(getClass().getSimpleName(), "Update: Current program name = " + intent.getStringExtra("com.jds.srplayerservice.CURRENT_PROGRAM_NAME"));
        	 //Log.d(getClass().getSimpleName(), "Update: Next program name = " + intent.getStringExtra("com.jds.srplayerservice.NEXT_PROGRAM_NAME"));
        	 //Log.d(getClass().getSimpleName(), "Update: Channelname = " + intent.getStringExtra("com.jds.srplayerservice.CHANNEL_NAME"));
        	 ChannelName = intent.getStringExtra("com.jds.srplayerservice.CHANNEL_NAME");
        	 CurrentProgramTitle = intent.getStringExtra("com.jds.srplayerservice.CURRENT_PROGRAM_NAME");
        	 NextProgramTitle = intent.getStringExtra("com.jds.srplayerservice.NEXT_PROGRAM_NAME");
        	 ServerStatus = intent.getIntExtra("com.jds.srplayerservice.PLAYER_STATUS", 0);
        	 ChannelIndex = intent.getIntExtra("com.jds.srplayerservice.CHANNEL_INDEX", 0);     
        	 
        	 //ComponentName thisWidget = new ComponentName(this, SRPlayerWidget.class);
 	         //AppWidgetManager manager = AppWidgetManager.getInstance(this);
        	 
        	 
        	 if (ThisappWidgetId >= 0)
        	 {
	        	 AppWidgetManager manager = AppWidgetManager.getInstance(context);
	        	 //ComponentName thisWidget = new ComponentName(context, SRPlayerWidget.class);
	        	  
	        	 //int AppIds = manager.getAppWidgetIds(thisWidget)[0]; 	               	 
	        	 //UpdateWidget(context, manager, AppIds);
	        	 UpdateWidget(context, manager, ThisappWidgetId);
        	 }
         }
         
    }
	
	
	private void UpdateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) 
	{
		//Update the graphical interface		
		
    	RemoteViews updateViews = null;
		updateViews = new RemoteViews(context.getPackageName(), R.layout.ws_widget_linear);
		
    	Intent clickintent=new Intent("com.jds.srplayerwidget.START"); 
    	PendingIntent pendingIntentClick=PendingIntent.getBroadcast(context, 0, clickintent, START_STREAMING);
        updateViews.setOnClickPendingIntent(R.id.PlayPause, pendingIntentClick); 
        
        Intent StartIntent = new Intent(context, srplayerserviceConfig.class); 
        StartIntent.putExtra("com.jds.srplayerservice.CHANNEL_INDEX", ChannelIndex);        
        StartIntent.putExtra("com.jds.srplayerservice.CHANNEL_NAME", ChannelName);
        PendingIntent pendingConfigIntentClick = PendingIntent.getActivity(context, 0, StartIntent, PendingIntent.FLAG_UPDATE_CURRENT);	        	        
        updateViews.setOnClickPendingIntent(R.id.ConfigButton, pendingConfigIntentClick);
                        
        updateViews.setTextViewText(R.id.ChannelName, ChannelName);
        updateViews.setTextViewText(R.id.CurrentProgName, CurrentProgramTitle);
        updateViews.setTextViewText(R.id.NextProgName, NextProgramTitle);
                
        if (ServerStatus == STOP)
        {
        	updateViews.setImageViewResource(R.id.PlayPause, R.drawable.play_white);
        }
        else if (ServerStatus == BUFFER)
        {
        	updateViews.setImageViewResource(R.id.PlayPause, R.drawable.buffer_white);
        }
        else if (ServerStatus == PLAY)
        {
        	updateViews.setImageViewResource(R.id.PlayPause, R.drawable.pause_white);
        }
        Resources res = context.getResources();
		updateViews.setTextViewText(R.id.ChannelName, ChannelName);
                		
        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
	
        
	}
	
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            
    	ThisappWidgetId = appWidgetIds[0];
    	
    	//Start service if not already started
    	Log.d( getClass().getSimpleName(), "Init start of new service" );
    	Intent ServiceIntentNew = new Intent(context, SRPlayerService.class);
    	ServiceIntentNew.addFlags(INIT_SERVICE);
    	context.startService(ServiceIntentNew);
    	
    	UpdateWidget(context, appWidgetManager, appWidgetIds[0]);	
    	    
    }

}

