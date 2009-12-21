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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class SRPlayer extends ListActivity implements PlayerObserver {
	
	private static final String _SR_RIGHTNOWINFO_URL = 
		"http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=false";
	private static Station currentStation = new Station("P1", 
			"rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp",
			"http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=true",
			132,0);
	public static final String TAG = "SRPlayer";
	
	private static final int MENU_EXIT = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_CONFIG = 2;
	private static final int MENU_UPDATE_INFO = 3;
	private static final int MENU_SLEEPTIMER = 4;
	
	protected static final int MSGUPDATECHANNELINFO = 0;
	protected static final int MSGPLAYERSTOP = 1;
	protected static final int MSGNEWPODINFO = 2;	
	
	private ImageButton startStopButton;
	private int playState = PlayerService.STOP;
	boolean isFirstCall = true;
	boolean isExitCalled = false;
	private int ChannelIndex = 0;
	public PlayerService boundService;
	private static int SleepTimerDelay;
	
	private List<String> MainListArray = new ArrayList<String>();
    private List<PodcastInfo> PodInfo = new ArrayList<PodcastInfo>();    	
    private int currentPosition = 0;
    private ArrayAdapter<String> PodList; 
    
    public static final int CATEGORIES = 0;	
	public static final int PROGRAMS = 1;
	public static final int PROGRAMS_IN_A_CATEGORY = 2;    	
	public static final int GET_IND_PROGRAMS = 3;
	public static final int CHANNELS = 4;
	
	
	private int PlayerMode;
	private static final int LIVE_MODE = 0;
	private static final int PODCAST_MODE = 1;
	
	
	public static final String ACTION = "ACTION";
	private int CurrentAction;
	private int SelectedCategory = -1;
	PodcastInfoThread podcastinfothread;
	private ProgressDialog waitingfordata;
	    	
	private String PoddIDLabel;
	
	private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	Log.d(TAG, "onServiceConnected");

        	// This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	boundService = ((PlayerService.PlayerBinder)service).getService();
        	boundService.addPlayerObserver(SRPlayer.this);
        	// Set StationName
        	TextView tv = (TextView) findViewById(R.id.PageLabel);
  			tv.setText(boundService.getCurrentStation().getStationName());
  			// Set channel in spinner
        	Station station = boundService.getCurrentStation();
        	CharSequence[] channelInfo = (CharSequence[]) getResources().getTextArray(R.array.channels);
        	int channelPos = 0;
        	// Why does binarySearch(CharSequence[], String) not work ?
    		// = Arrays.binarySearch(channelInfo, station.getStationName());
        	for(CharSequence cs : channelInfo) {
        		if ( cs.toString().equals(station.getStationName()) ) {
        			break;
        		}
        		channelPos++;
        	}
        }

        public void onServiceDisconnected(ComponentName className) {
    		Log.d(TAG, "onServiceDisconnected");

            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	boundService = null;
        }
    };

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		this.isExitCalled = false;
		if ( savedInstanceState != null ) {
			this.playState = savedInstanceState.getInt("playState");
			Log.d(TAG, "playstate restored to " + this.playState);
		} else {
			this.playState = PlayerService.STOP;
		}
		startService();
		requestWindowFeature  (Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		PlayerMode = this.LIVE_MODE;
		UpdateBottomButton(PlayerMode);
		
		MainListArray.add("");
        
        Intent intent = this.getIntent();
        CurrentAction = intent.getIntExtra(ACTION, 0);
        
        PodList = new ArrayAdapter<String>(this,
                R.layout.podlistitem, MainListArray);                
        UpdateList();
 
        startStopButton = (ImageButton) findViewById(R.id.BtnStartStop);		
		
        Button ChangeListButton = (Button) findViewById(R.id.ProgChannelButton);
        ChangeListButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {				
        		//Check if the mode is Live och Podcast
        		if (PlayerMode == LIVE_MODE)
        		{
        		//Live mode
        		Resources res = getResources();        		
        		List<String> items= Arrays.asList(res.getStringArray(R.array.channels));
        		MainListArray.clear();
        		MainListArray.addAll(items);        	
        		setListAdapter(PodList);        	   	
        	   	UpdatePlayerVisibility(true);
        	   	TextView tv = (TextView) findViewById(R.id.PageLabel);
        	   	tv.setText("Kanaler");
        	   	CurrentAction = SRPlayer.CHANNELS;
        		}
        		else
        		{
        		//Podcast mode	
        		podcastinfothread = new PodcastInfoThread(SRPlayer.this, PodcastInfoThread.GET_ALL_PROGRAMS, 0);
                podcastinfothread.start();  
                waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player","Hämtar lista");
                CurrentAction = SRPlayer.PROGRAMS;	
        		}
        		
			}
		});
                
        Button PlayerButton = (Button) findViewById(R.id.PlayerButton);
        PlayerButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {				
        		UpdatePlayerVisibility(false);
			}
		});

		startStopButton.setOnClickListener(new OnClickListener() {        
			public void onClick(View v) {
				try {
					if (SRPlayer.this.playState == PlayerService.STOP) {
						setBufferText(-1);
						startStopButton.setImageResource(R.drawable.buffer_black);
						startPlaying();
					} else {
						stopPlaying();
						startStopButton.setImageResource(R.drawable.play_black);
					}
				} catch (IllegalStateException e) {
					Log.e(SRPlayer.TAG, "Could not " +(SRPlayer.this.playState == PlayerService.STOP?"start":"stop") +" to stream play.", e);
				} catch (IOException e) {
					Log.e(SRPlayer.TAG, "Could not " +(SRPlayer.this.playState == PlayerService.STOP?"start":"stop") +" to stream play.", e);
				}
			}
		});
		
		Button CategoriesButton = (Button) findViewById(R.id.PodCatButton);
        CategoriesButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	
        		
        		podcastinfothread = new PodcastInfoThread(SRPlayer.this, 0, 0);
                podcastinfothread.start();  
                waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player","Hämtar lista");
                CurrentAction = SRPlayer.CATEGORIES;
        	}
		});
        
        ImageButton ModeButton = (ImageButton) findViewById(R.id.ModeButton);
        ModeButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	        		
        		//Change the mode and update the view
        		if (PlayerMode == LIVE_MODE)
        		{
        			PlayerMode = PODCAST_MODE;        			
        		}
        		else
        		{
        			PlayerMode = LIVE_MODE;        			
        		}
        		
        		UpdateBottomButton(PlayerMode);
        	}
		});

		if (this.playState == PlayerService.BUFFER) {
			startStopButton.setImageResource(R.drawable.buffer_black);
		} if (this.playState == PlayerService.STOP) {
			startStopButton.setImageResource(R.drawable.play_black);
		} else {
			startStopButton.setImageResource(R.drawable.pause_black);
		}
		
		// Restore save text strings 
		if ( savedInstanceState != null ) {
			try {
	  			TextView tv = (TextView) findViewById(R.id.PageLabel);
	  			tv.setText(savedInstanceState.getString("stationNamn"));
	  			tv = (TextView) findViewById(R.id.ProgramNamn);
	  			tv.setText(savedInstanceState.getString("programNamn"));
	  			tv = (TextView) findViewById(R.id.NextProgramNamn);
	  			tv.setText(savedInstanceState.getString("nextProgramNamn"));
	  			tv = (TextView) findViewById(R.id.SongNamn);
	  			tv.setText(savedInstanceState.getString("songName"));
	  			tv = (TextView) findViewById(R.id.NextSongNamn);
	  			tv.setText(savedInstanceState.getString("nextSongName"));
	  			SleepTimerDelay = savedInstanceState.getInt("SleepTimerDelay");
	  		} catch (Exception e) {
	  			Log.e(SRPlayer.TAG, "Problem setting next song name", e);
	  		}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt("playState", this.playState);
		TextView tv = (TextView) findViewById(R.id.PageLabel);
		savedInstanceState.putString("stationName", tv.getText().toString());
		tv = (TextView) findViewById(R.id.ProgramNamn);
		savedInstanceState.putString("programNamn", tv.getText().toString());
		tv = (TextView) findViewById(R.id.NextProgramNamn);
		savedInstanceState.putString("nextProgramNamn", tv.getText().toString());
		tv = (TextView) findViewById(R.id.SongNamn);
		savedInstanceState.putString("songName", tv.getText().toString());
		tv = (TextView) findViewById(R.id.NextSongNamn);
		savedInstanceState.putString("nextSongName", tv.getText().toString());
		savedInstanceState.putInt("SleepTimerDelay", SleepTimerDelay);
		super.onSaveInstanceState(savedInstanceState);
	}

	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		if ( this.boundService != null ) {
			this.boundService.removePlayerObserver(this);
			unbindService(connection);
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean killService = prefs.getBoolean("KillServiceEnable", false);
		if ( killService && this.isExitCalled ) {
			// 
			Log.d(TAG, "Killing service");
			stopService(new Intent(SRPlayer.this,
                    PlayerService.class));
		}
		super.onDestroy();
	}

	private void startService() {
		Log.d(TAG, "startService");
		
		startService(new Intent(SRPlayer.this, 
                PlayerService.class));
		if ( this.boundService == null ) {
			bindService(new Intent(SRPlayer.this, 
					PlayerService.class), connection, 0);
		}
	}

	private void startPlaying() throws IllegalArgumentException,
			IllegalStateException, IOException {
		Log.d(TAG, "startPlaying");
		if ( this.boundService != null ) {
				try {
					boundService.startPlay();
					//startStopButton.setImageResource(R.drawable.loading);
					startStopButton.setImageResource(R.drawable.buffer_black);
					setBufferText(-1);
					this.playState = PlayerService.BUFFER;
				} catch (IllegalArgumentException e) {
					Log.e(SRPlayer.TAG, "Could not start to stream play.", e);
					Toast.makeText(SRPlayer.this, "Failed to start stream! See log for more details.", 
							Toast.LENGTH_LONG).show();
				} catch (IllegalStateException e) {
					Log.e(SRPlayer.TAG, "Could not start to stream play.", e);
					Toast.makeText(SRPlayer.this, "Failed to start stream! See log for more details.", 
							Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					Log.e(SRPlayer.TAG, "Could not start to stream play.", e);
					Toast.makeText(SRPlayer.this, "Failed to start stream! See log for more details.", 
							Toast.LENGTH_LONG).show();
				}
		} else {
			Toast.makeText(this, "Failed to start service", Toast.LENGTH_LONG).show();
		}
	}
	
	// Menu handling.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		super.onCreateOptionsMenu(menu);
		menu.add(0, SRPlayer.MENU_EXIT, 0, R.string.menu_exit).
			setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, SRPlayer.MENU_ABOUT, 0, R.string.menu_about).
			setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, SRPlayer.MENU_CONFIG, 0, R.string.menu_config).
			setIcon(android.R.drawable.ic_menu_save);
		menu.add(0, SRPlayer.MENU_UPDATE_INFO, 0, R.string.menu_update_info).
			setIcon(android.R.drawable.ic_menu_info_details);
		if (this.boundService.SleeptimerIsRunning())
		{
			menu.add(0, SRPlayer.MENU_SLEEPTIMER, 0, R.string.menu_sleeptimer_cancel).
			setIcon(R.drawable.ic_menu_sleeptimer_cancel);
		}
		else
		{
			menu.add(0, SRPlayer.MENU_SLEEPTIMER, 0, R.string.menu_sleeptimer).
			setIcon(R.drawable.ic_menu_sleeptimer);
		}
		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (this.boundService.SleeptimerIsRunning())
		{	
			menu.findItem(MENU_SLEEPTIMER).setIcon(R.drawable.ic_menu_sleeptimer_cancel);
			menu.findItem(MENU_SLEEPTIMER).setTitle(R.string.menu_sleeptimer_cancel);
		}
		else
		{			
			menu.findItem(MENU_SLEEPTIMER).setIcon(R.drawable.ic_menu_sleeptimer);
			menu.findItem(MENU_SLEEPTIMER).setTitle(R.string.menu_sleeptimer);
		}
		return true;
	}
	
	private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            	SleepTimerDelay = 60*hourOfDay+minute;
            	boundService.StartSleeptimer(SleepTimerDelay);
            	
            }
        };
           
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.d(TAG, "onMenuItemSelected");
		switch (item.getItemId()) {
		case SRPlayer.MENU_EXIT:
			handleMenuExit();
			return true;
		case SRPlayer.MENU_ABOUT:
			handleMenuAbout();
			return true;
		case SRPlayer.MENU_CONFIG:
			handleMenuConfig();
			return true;
		case SRPlayer.MENU_UPDATE_INFO:
			boundService.restartRightNowInfo();
			return true;
		case SRPlayer.MENU_SLEEPTIMER:
			if (this.boundService.SleeptimerIsRunning())
			{
				this.boundService.StopSleeptimer();
			}
			else
			{
			TimePickerDialog SelectSleepTimeDialog = new TimePickerDialog(this,
                    mTimeSetListener, 
                    SleepTimerDelay/60, 
                    SleepTimerDelay%60, 
                    true);
			SelectSleepTimeDialog.setTitle("Ange tid HH:MM");
			SelectSleepTimeDialog.show();
			}
			return true;
			
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void handleMenuAbout() {
		new AlertDialog.Builder(this)
			.setTitle(getResources().getText(R.string.about_title))
			.setMessage(R.string.about_message)
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// Do nothing...
						}
					}).show();
	}
		
	private void handleMenuExit() {
		this.isExitCalled = true;
		this.stopPlaying();
		this.finish();
	}
	
	private void handleMenuConfig() {		
		Intent launchIntent = new Intent(SRPlayer.this, SRPlayerPreferences.class);
		SRPlayer.this.startActivity(launchIntent);
	}
	
	private void setBufferText(int percent) {
		// clearAllText();
		TextView tv = (TextView) findViewById(R.id.PageLabel);
		if ( percent == -1) {
			tv.setText("Buffrar...");
		} else {
			tv.setText("Buffrar... " + percent + "%");
		}
	}

	private void stopPlaying() {
		Log.d(TAG, "stopPlaying");
		Log.i(SRPlayer.TAG, "Media Player stop!");
		this.boundService.stopPlay();
	}

	Handler viewUpdateHandler = new Handler(){
        public void handleMessage(Message msg) {
             switch (msg.what) {
                  case SRPlayer.MSGUPDATECHANNELINFO:
                	  	RightNowChannelInfo info = (RightNowChannelInfo) msg.getData().getSerializable("data");
                	  	if ( info == null ) {
                	  		return;
                	  	}
	                	TextView tv = (TextView) findViewById(R.id.ProgramNamn);
	              		try {
	              			tv.setText(info.getProgramTitle() + " " + info.getProgramInfo());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting program title and info", e);
	              		}
	              		tv = (TextView) findViewById(R.id.NextProgramNamn);
	              		try {
	              			tv.setText(info.getNextProgramTitle());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting next program title", e);
	              		}
	              		tv = (TextView) findViewById(R.id.SongNamn);
	              		try {
	              			tv.setText(info.getSong());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting song name", e);
	              		}
	              		tv = (TextView) findViewById(R.id.NextSongNamn);
	              		try {
	              			tv.setText(info.getNextSong());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting next song name", e);
	              		}
                       break;
                  case MSGPLAYERSTOP:
                	  	playState = PlayerService.STOP;
              			tv = (TextView) findViewById(R.id.PageLabel);
              			tv.setText(SRPlayer.currentStation.getStationName());
              			startStopButton.setImageResource(R.drawable.play_black);
                	  break;
                  case MSGNEWPODINFO :
                	  waitingfordata.dismiss();                      
                      UpdatePlayerVisibility(true);
                      UpdateList();
                      break;
             }
             super.handleMessage(msg);
        }
   };

   
	public void onRightNowChannelInfoUpdate(RightNowChannelInfo info) {
		Message m = new Message();
        m.what = SRPlayer.MSGUPDATECHANNELINFO;
        m.getData().putSerializable("data", info);
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
	}

	public void onPlayerBuffer(int percent) {
		//startStopButton.setImageResource(R.drawable.loading);
		startStopButton.setImageResource(R.drawable.buffer_black);
		setBufferText(percent);
	}

	public void onPlayerStarted() {
		//startStopButton.setImageResource(R.drawable.stop);
		startStopButton.setImageResource(R.drawable.pause_black);
		this.playState = PlayerService.PLAY;
		TextView tv = (TextView) findViewById(R.id.PageLabel);
	    tv.setText(SRPlayer.currentStation.getStationName());
	}

	public void onPlayerStoped() {		
		Message m = new Message();
        m.what = SRPlayer.MSGPLAYERSTOP;
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
	}
	
   private void clearAllText() {
       TextView tv = (TextView) findViewById(R.id.PageLabel);
       tv.setText(SRPlayer.currentStation.getStationName());
       tv = (TextView) findViewById(R.id.ProgramNamn);
       tv.setText("-");
       tv = (TextView) findViewById(R.id.NextProgramNamn);
       tv.setText("-");
       tv = (TextView) findViewById(R.id.SongNamn);
       tv.setText("-");
       tv = (TextView) findViewById(R.id.NextSongNamn);
       tv.setText("-");
   }
   
   private void UpdateList()
   {
   	setListAdapter(PodList);
   	TextView tv = (TextView) findViewById(R.id.PageLabel);
   	if (CurrentAction == SRPlayer.PROGRAMS)
   	{
   		tv.setText("Program A-Ö");
   	}
   	else if (CurrentAction == SRPlayer.CATEGORIES)
   	{
   		tv.setText("Kategorier");
   	}
   	else
   	{        		                        	
 		tv.setText(PoddIDLabel);
   	}
   	
   }

   public void UpdateArray(List<String> PodStringArray, Object PodObject)
    {        
    	if (PodStringArray == null)
    	{
    	MainListArray.clear();
    	PodInfo.clear();
    	}
    	else
    	{
    	MainListArray.clear();        		
    	MainListArray.addAll(PodStringArray); 
    	PodInfo.clear();
    	List<PodcastInfo> NewPodInfo = (List<PodcastInfo>)PodObject;
    	PodInfo.addAll(NewPodInfo);
    	Log.d(SRPlayer.TAG, "New PodInfo receviced. Size = " + String.valueOf(PodInfo.size()));		
		
    	}
    	
    	Message m = new Message();
        m.what = SRPlayer.MSGNEWPODINFO;
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
    };
    
    private void UpdatePlayerVisibility(boolean Hide)
    {
    	View LayoutToHide = null;
    	View LayoutToShow = null;
    	TextView tv = (TextView) findViewById(R.id.PageLabel);
		tv.setText(SRPlayer.currentStation.getStationName());
    	if (Hide)
    	{
    		//Hide the player
    		LayoutToHide = (View)findViewById(R.id.PlayerLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		LayoutToHide = (View)findViewById(R.id.PlayerControlsLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		//Show the listview
    		LayoutToShow = (View)findViewById(R.id.ListViewLayout);
    		LayoutToShow.setVisibility(View.VISIBLE);
    	}
    	else
    	{
    		//Show the player
    		LayoutToShow = (View)findViewById(R.id.PlayerLayout);
    		LayoutToShow.setVisibility(View.VISIBLE);
    		
    		LayoutToShow = (View)findViewById(R.id.PlayerControlsLayout);
    		LayoutToShow.setVisibility(View.VISIBLE);
    		
    		//Hise the listview
    		LayoutToHide = (View)findViewById(R.id.ListViewLayout);
    		LayoutToHide.setVisibility(View.GONE);    	
    	}
    }
    
    private void UpdateBottomButton(int Mode)
    {
     Button ChannelProgramButton = (Button) findViewById(R.id.ProgChannelButton);
     Button CategoryButton = (Button) findViewById(R.id.PodCatButton);
     ImageButton ModeButton = (ImageButton) findViewById(R.id.ModeButton);
     
     
     if (Mode == LIVE_MODE)
     {
    	 ModeButton.setImageResource(R.drawable.mode_live);
         
    	 //Remove the categories button
    	 CategoryButton.setVisibility(View.GONE);
    	 
    	 //Set the text of the list button to "Kanal"    	
    	 ChannelProgramButton.setText("Kanal");  
    	 
    	 //The player is currently always visible during live
    	 //UpdatePlayerVisibility(false);
     }
     else
     {
    	 ModeButton.setImageResource(R.drawable.mode_pod);
         
    	 //Show the categories button
    	 CategoryButton.setVisibility(View.VISIBLE);
    	 
    	//Set the text of the list button to "Program A-Ö"    	
    	 ChannelProgramButton.setText("Program A-Ö");                  
     }
    }
    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            currentPosition = position;        
            
        	switch (CurrentAction)
        	{
        	case SRPlayer.CATEGORIES:
        		//A specific category has been selected. 
            	//Retreive a list of all programs in the category
            	String CategoryId = PodInfo.get(currentPosition).getID();
            	PoddIDLabel = PodInfo.get(currentPosition).getTitle();
        		
        		podcastinfothread = new PodcastInfoThread(SRPlayer.this, SRPlayer.PROGRAMS_IN_A_CATEGORY, Integer.valueOf(CategoryId));
                podcastinfothread.start();  
                waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player","Hämtar lista");
                CurrentAction = SRPlayer.PROGRAMS_IN_A_CATEGORY;
        		break;	
        	case SRPlayer.PROGRAMS:            	
        	case SRPlayer.PROGRAMS_IN_A_CATEGORY:
        		//A specific program has been selected
            	//Retreive a list of all stored podcasts for
            	//that channel
            	String PoddId = PodInfo.get(currentPosition).getPoddID();
            	PoddIDLabel = PodInfo.get(currentPosition).getTitle();
        		
        		podcastinfothread = new PodcastInfoThread(SRPlayer.this, SRPlayer.GET_IND_PROGRAMS, Integer.valueOf(PoddId));
                podcastinfothread.start();  
                waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player","Hämtar lista");
                CurrentAction = SRPlayer.GET_IND_PROGRAMS;
        		break;
        	case SRPlayer.GET_IND_PROGRAMS:
        		SRPlayer.currentStation.setStreamUrl(PodInfo.get(currentPosition).getLink());
				SRPlayer.currentStation.setStationName(PoddIDLabel);
				SRPlayer.currentStation.setChannelId(0);
				SRPlayer.currentStation.setStreamType(Station.POD_STREAM);
				// TODO remove rightnow info updates during podcasts
				SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_URL);
				boundService.selectChannel(SRPlayer.currentStation);					
				clearAllText();
				UpdatePlayerVisibility(false);
				RightNowChannelInfo info = new RightNowChannelInfo();
				info.setProgramTitle(PodInfo.get(currentPosition).getTitle());
				info.setProgramInfo(PodInfo.get(currentPosition).getDescription());
				boundService.rightNowUpdate(info);    
        		break;
        	case SRPlayer.CHANNELS:
        		ChannelIndex = this.boundService.getStationIndex();
        		if (position != ChannelIndex)
	        	{
		        	Resources res = getResources();
		    		CharSequence[] channelInfo = (CharSequence[]) res
		    				.getTextArray(R.array.channels);
		    		CharSequence[] urls = (CharSequence[]) res.getTextArray(R.array.urls);
		    		
		        	SRPlayer.currentStation.setStreamUrl(urls[position].toString());
					SRPlayer.currentStation.setStationName(channelInfo[position].toString());
					SRPlayer.currentStation.setChannelId(res.getIntArray(R.array.channelid)[position]);
					SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_URL);
					SRPlayer.currentStation.setStreamType(Station.NORMAL_STREAM);					
					boundService.selectChannel(SRPlayer.currentStation);					
					clearAllText();
					UpdatePlayerVisibility(false);
	        	}
        	    break;
            default:
            	break;
        	}
            
   }
}
