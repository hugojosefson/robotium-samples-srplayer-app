package com.jds.srplayer;

import com.jds.srplayer.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class srplayerserviceConfig extends Activity
{
	private int SelectedChannel = 0;
	private BroadcastReceiver onUpdateFromService = null;
	
	public static final int STOP = 0;
	public static final int BUFFER = 1;
	public static final int PLAY = 2;
	private static String CurrentProgramTitle = "";
	private static String NextProgramTitle = "";
	private static String ChannelName = "";
	private static Integer ServiceStatus = STOP;
	private static String ProgramInfo = "";
	private static String NextProgramInfo = "";
	private static String CurrentSong = "";
	private static String NextSong = "";
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
                
        //Get the latest data from the service
        Intent ServiceIntentNew = new Intent(getBaseContext(), SRPlayerService.class);
    	ServiceIntentNew.addFlags(SRPlayerWidget.INIT_SERVICE);
    	startService(ServiceIntentNew);
    	
    	Resources res = getResources();
    	final CharSequence[] items = res.getTextArray(R.array.channels);

    	
    	//AlertDialog alert = builder.create();
    	
        
        //Fill the spinner
        //setContentView(R.layout.service_config_view);
        setContentView(R.layout.sr_player_activity_layout);
        
        /*        
        Spinner s = (Spinner) findViewById(R.id.ChannelSpinner);
        
        final ArrayAdapter adapter = ArrayAdapter.createFromResource(
                this, R.array.channels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        */

        //SelectedChannel = StartIntent.getIntExtra("com.jds.srplayerwidget.CHANNEL_INDEX", 0);
        
        
        Intent StartIntent = getIntent();                
        ParseIntent(StartIntent);
        
        /*
        s.setSelection(SelectedChannel);
        
        String StartAction = StartIntent.getAction();
        Log.d(getClass().getSimpleName(), "Activity started. Action= " + StartAction);
                
        s.setOnItemSelectedListener(
                new  AdapterView.OnItemSelectedListener() {           

           @Override
           public void onItemSelected(AdapterView<?> parent, 
             View view, int position, long id) {
        	   if (position != SelectedChannel)
        	   {
        	   SelectedChannel = position;
        	   ChangeOfChannel();
        	   
        	   Intent ServiceIntent = new Intent(getBaseContext(), SRPlayerService.class);
	           ServiceIntent.putExtra("com.jds.srplayerservice.CHANNEL_INDEX", SelectedChannel);
			   ServiceIntent.addFlags(SRPlayerWidget.UPDATE_CONFIG);
	          	  startService(ServiceIntent);
        	   }
           }

           @Override
           public void onNothingSelected(AdapterView<?> parent) {

           }

       });
       */ 

        
        
        
        
        ImageButton ChangeChannelButton = (ImageButton) findViewById(R.id.SelectChannel_A);
        ChangeChannelButton.setOnClickListener(new View.OnClickListener() {
        	@Override
			public void onClick(View v) {				
        		ShowDialog();
			}
		});
		
		    	    						
        /*
        Button CancleButton = (Button) findViewById(R.id.AvbrytKonfig);
        CancleButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				finish();
			}
		}
        
        );
        */
        
        
        
        ImageButton PlayPauseButton = (ImageButton) findViewById(R.id.PlayPauseA);
        PlayPauseButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent ServiceIntentNew = new Intent(getBaseContext(), SRPlayerService.class);
		    	ServiceIntentNew.addFlags(SRPlayerWidget.START_STREAMING);
		    	startService(ServiceIntentNew);				
			}
		}
        );
        
        
        
        if (onUpdateFromService == null)
        {
	        onUpdateFromService = new BroadcastReceiver()
	        {
		        @Override
		        public void onReceive(Context context, Intent intent)
		        {
		        	if(intent.getAction().equals("com.jds.srplayerservice.UPDATE")) 
		            {
		        		ParseIntent(intent);
		        		Log.d("SR Player", "Update received");
		            }
		        }
	        };}
    
	
	}
	
	void ShowDialog()
	{
		Resources res = getResources();
		String[] items = res.getStringArray(R.array.channels);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);		
		builder.setTitle("Välj kanal");
		builder.setSingleChoiceItems(items, SelectedChannel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        dialog.dismiss();
		        if (item != SelectedChannel)
	        	   {
	        	   SelectedChannel = item;
	        	   ChangeOfChannel();
	        	   
	        	   Intent ServiceIntent = new Intent(getBaseContext(), SRPlayerService.class);
		           ServiceIntent.putExtra("com.jds.srplayerservice.CHANNEL_INDEX", SelectedChannel);
				   ServiceIntent.addFlags(SRPlayerWidget.UPDATE_CONFIG);
		          	  startService(ServiceIntent);
	        	   }
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();

	}
	
	void ParseIntent(Intent intent)
	{
		SelectedChannel = intent.getIntExtra("com.jds.srplayerservice.CHANNEL_INDEX", 0);
		ChannelName = intent.getStringExtra("com.jds.srplayerservice.CHANNEL_NAME");
      	 CurrentProgramTitle = intent.getStringExtra("com.jds.srplayerservice.CURRENT_PROGRAM_NAME");
      	 //Log.d("SR Player", "Current program title = " + CurrentProgramTitle);
      	 NextProgramTitle = intent.getStringExtra("com.jds.srplayerservice.NEXT_PROGRAM_NAME");
      	//Log.d("SR Player", "Next program title = " + NextProgramTitle);
      	 ServiceStatus = intent.getIntExtra("com.jds.srplayerservice.PLAYER_STATUS", 0);
      	 //ChannelIndex = intent.getIntExtra("com.jds.srplayerservice.CHANNEL_INDEX", 0);  
      	ProgramInfo = intent.getStringExtra("com.jds.srplayerservice.CURRENT_PROGRAM_INFO");
		NextProgramInfo = intent.getStringExtra("com.jds.srplayerservice.NEXT_PROGRAM_INFO");;
		CurrentSong = intent.getStringExtra("com.jds.srplayerservice.CURRENT_SONG");
		NextSong = intent.getStringExtra("com.jds.srplayerservice.NEXT_SONG");
		
      	 
      	 Integer NewSelectedChannel = intent.getIntExtra("com.jds.srplayerservice.CHANNEL_INDEX", 0);
      	 
      	 if (NewSelectedChannel != SelectedChannel)
      	 {
      		SelectedChannel = 	NewSelectedChannel;
      		ChangeOfChannel();
      		UpdateViews();
      	 }
      	 else
      	 {
      		UpdateViews();
      	 }
	}
	
	void UpdateViews()
	{		
		
		TextView ChannelNameV = (TextView) findViewById(R.id.ChannelName);
     	 if (ChannelNameV != null)
     		ChannelNameV.setText(ChannelName);
     	
		
		TextView CurrentProgramTitleV = (TextView) findViewById(R.id.ProgramNamn);
      	 if ((CurrentProgramTitleV != null) && (CurrentProgramTitle!=null))
      		 CurrentProgramTitleV.setText(CurrentProgramTitle+" "+ProgramInfo);
      	 
      	 
      	TextView NextProgramTitleV = (TextView) findViewById(R.id.NextProgramNamn);
      	 if ((NextProgramTitleV != null) && (NextProgramTitle != null))
      		NextProgramTitleV.setText(NextProgramTitle+" "+NextProgramInfo);
      	 
      	TextView CurrentSongV = (TextView) findViewById(R.id.SongNamn);
     	 if (CurrentSongV != null)
     		CurrentSongV.setText(CurrentSong);
     	 
     	 
     	TextView NextSongV = (TextView) findViewById(R.id.NextSongNamn);
     	 if (NextSongV != null)
     		NextSongV.setText(NextSong);
      	 
      	ImageButton PlayPauseButton = (ImageButton) findViewById(R.id.PlayPauseA);
      	
      	 
        switch (ServiceStatus)
     	 {
     	  case (BUFFER):
     		//Set the ImageButton to buffer symbol
     		PlayPauseButton.setImageResource(R.drawable.buffer_white); 
    		 break;
     	  case (PLAY):
     		//Set the ImageButton to pause symbol
     		PlayPauseButton.setImageResource(R.drawable.pause_white);
    		 break;
    	  default :	 
    		 //Set the ImageButton to play button
    		 PlayPauseButton.setImageResource(R.drawable.play_white);
    		 break;
     	 }
	}
	
	void ChangeOfChannel()
	{
		CurrentProgramTitle = "-";
		NextProgramTitle = "-";
		ProgramInfo = "";
		NextProgramInfo = "";
		CurrentSong = "";
		NextSong = "";
	}

	@Override
	protected void onPause() {
		if (onUpdateFromService != null)
			unregisterReceiver(onUpdateFromService);
		//Log.d("SR Player", "Receiver unregistered");
		super.onPause();
	}

	@Override
	protected void onResume() {
		//Log.d("SR Player", "Broadcast receiver registered");
		registerReceiver(onUpdateFromService, 
        		new IntentFilter("com.jds.srplayerservice.UPDATE"));
		super.onDestroy();
	}
	
	
		
}
