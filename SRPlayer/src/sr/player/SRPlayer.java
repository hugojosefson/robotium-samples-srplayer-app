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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SRPlayer extends ListActivity implements PlayerObserver, SeekBar.OnSeekBarChangeListener{
	
	private static final String _SR_RIGHTNOWINFO_URL = 
		"http://api.sr.se/api/rightnowinfo/rightnowinfo.aspx?filterinfo=all";
	public static final String _SR_RIGHTNOWINFO_IND_URL =
		"http://api.sr.se/api/rightnowinfo/Rightnowinfo.aspx?unit=";
	private static Station currentStation = new Station("P1", 
			"rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp",
			"http://api.sr.se/api/rightnowinfo/rightnowinfo.aspx?filterinfo=all",
			132,0);
	public static final String TAG = "SRPlayer";
	
	private static final int MENU_EXIT = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_CONFIG = 2;
	private static final int MENU_UPDATE_INFO = 3;
	private static final int MENU_SEARCH = 3;
	private static final int MENU_SLEEPTIMER = 4;
	private static final int MENU_HELP = 5;
	private static final int MENU_ALARM = 6;
	
	public static final int MENU_CONTEXT_ADD_TO_FAVORITES = 20;			
	public static final int MENU_CONTEXT_DELETE_FAVORITE = 21;
	public static final int MENU_CONTEXT_DOWNLOAD = 22;
	public static final int MENU_CONTEXT_ABORT_AND_DELETE = 23;
	public static final int MENU_CONTEXT_PAUSE = 24;
	public static final int MENU_CONTEXT_RESUME = 25;
	public static final int MENU_CONTEXT_SET_AS_ALARM = 26;
	
	protected static final int MSGUPDATECHANNELINFO = 0;
	protected static final int MSGPLAYERSTOP = 1;
	protected static final int MSGNEWPODINFO = 2;	
	protected static final int MSGUPDATESEEK = 3;
	protected static final int MSGDOWNLOADTIMERELAPS = 4;
		
	private ImageButton startStopButton;
	private int playState = PlayerService.STOP;
	boolean isFirstCall = true;
	boolean isExitCalled = false;
	public PlayerService boundService;
	private static int SleepTimerDelay;
	
	private List<PodcastInfo> PodInfo = new ArrayList<PodcastInfo>();
    private List<PodcastInfo> ProgramArray = new ArrayList<PodcastInfo>();
    private List<PodcastInfo> CategoryArray = new ArrayList<PodcastInfo>();
    private List<PodcastInfo> ChannelArray = new ArrayList<PodcastInfo>();
    private static List<History> HistoryList = new ArrayList<History>();
    private int currentPosition = 0;
    private PodcastInfoAdapter PodList;
        
    public static final int PLAYER = -1;
    public static final int CATEGORIES = 0;	
	public static final int PROGRAMS = 1;
	public static final int PROGRAMS_IN_A_CATEGORY = 2;    	
	public static final int GET_IND_PROGRAMS = 3;
	public static final int CHANNELS = 4;
	public static final int FAVORITES = 5;
	public static final int FAVORITES_CHANNELS = 6;
	public static final int FAVORITES_PROGRAMS = 7;
	public static final int FAVORITES_IND_PROGRAMS = 8;
	public static final int FAVORITES_CATEGORIES = 9;	
	public static final int FAVORITES_OFFLINE_PROGRAMS = 10;
	public static final int DOWNLOAD_QUEUE = 11;
	public static final int SEARCH_RESULT = 12;
	public static final int ALARM = 13;
	       	
	public static final String ACTION = "ACTION";
	private static int CurrentAction;
	PodcastInfoThread podcastinfothread;
	private ProgressDialog waitingfordata;
	    	
	private String PoddIDLabel;
	private String LastQuery;
	
	private Timer SeekTimer;
	private Timer downloadTimer;
	private TimerTask SeekTimerTask;
	private TimerTask downloadTimerTask;
	private int Rep;
	
	//Database variables
	SRPlayerDBAdapter SRPlayerDB;
	    
	@Override
	 public boolean onSearchRequested() {
		if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
    	{        		
    		//Generate a toast that you can't search from
			//the player view
			Context context = getApplicationContext();
			CharSequence text = getResources().getString(R.string.no_search_from_player);				
			int duration = Toast.LENGTH_LONG;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
    		return false;
    	}
		super.onSearchRequested();
		return true;		
	 }
				
	@Override
	protected void onNewIntent(Intent intent) {						       
        String query = null;

        // locate a query string; prefer a fresh search Intent over saved
        // state
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        } 
        if (query != null && query.length() > 0) {
        	Log.d(TAG,"Query with the string " + query);
        	        	
        	LastQuery = query.toLowerCase();        	
        	GenerateNewList(SRPlayer.SEARCH_RESULT, 0, "", "", false,null);
        }
	
		super.onNewIntent(intent);
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {
		
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		//Stop the automatic update of the seekbar
		if ( this.SeekTimerTask != null) {
			this.SeekTimerTask.cancel();
		} 
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		if (this.boundService != null)
		{
			int NewPosition = seekBar.getProgress();
						
			String SDK = Build.VERSION.SDK; 
			int SDKVal = 0;
			try {
					SDKVal = Integer.valueOf(SDK);						
			}
			catch (NumberFormatException e)
			{
				SDKVal = 0;
			}
						
			if ((currentStation.getStreamType() != Station.OFFLINE_STREAM) && ( SDKVal >= 4))
			{
				Context context = getApplicationContext();
				CharSequence text = getResources().getString(R.string.WindErorText);				
				int duration = Toast.LENGTH_LONG;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
			else
			{
				this.boundService.SetPosition(NewPosition);				
			}
			
			if ( this.SeekTimerTask != null) {
				this.SeekTimerTask.cancel();
			}
		    SeekTimerTask = new seekTimerTask(this);
		    SeekTimer.schedule(SeekTimerTask, 500, 1000);
		}
	}
	
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
        	// Set StationName, but only if the historlist is empty
        	if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
        	{        		
        		TextView tv = (TextView) findViewById(R.id.PageLabel);
        		tv.setText(boundService.getCurrentStation().getStationName());        		
        	}
        	
  			// Set channel in spinner
        	//Station station = boundService.getCurrentStation();        	
        	SRPlayer.currentStation = boundService.getCurrentStation().clone();
        	UpdateSeekBar();
        	
        	//TODO If the current stream is a podcast/offline. Retreive the text from saved data
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
		
		super.onCreate(savedInstanceState);
		// Make sure volume button changes Media Volume if SR Player in focus.
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		Log.d(TAG, "onCreate");	
		
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
		
		//setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		SeekTimer = new Timer();
		downloadTimer = new Timer();
		SeekBar mSeekBar = (SeekBar) findViewById(R.id.PlayerSeekBar);
		mSeekBar.setOnSeekBarChangeListener(this);
                              
        PodList = new PodcastInfoAdapter(this,
                R.layout.podlistitem, (ArrayList<PodcastInfo>) PodInfo);
                                       
        startStopButton = (ImageButton) findViewById(R.id.BtnStartStop);
        
        Button ChannelButton = (Button) findViewById(R.id.Channels);
        ChannelButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {				
        		//Check if the mode is Live och Podcast        		
        		GenerateNewList(SRPlayer.CHANNELS, 0, "", "", false,
        				(ChannelArray.size() > 0) ? ChannelArray : null);        		        		
			}
		});
		
        Button ChangeListButton = (Button) findViewById(R.id.ProgButton);
        ChangeListButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {				
        		//Check if the mode is Live och Podcast        		
        		//Podcast mode	
        		GenerateNewList(SRPlayer.PROGRAMS, 0, "", "", false,
        				(ProgramArray.size() > 0) ? ProgramArray : null);        		
			}
		});
        
        Button FavoritesButton = (Button) findViewById(R.id.Favorites);
        FavoritesButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {				
        		//Check if the mode is Live och Podcast        		
        		GenerateNewList(SRPlayer.FAVORITES, 0, "", "", false,null);        		        	
			}
		});               
                
        Button PlayerButton = (Button) findViewById(R.id.PlayerButton);
        PlayerButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		//HistoryList.clear(); //Reset the history
        		UpdatePlayerVisibility(false);
			}
		});

		startStopButton.setOnClickListener(new OnClickListener() {        
			public void onClick(View v) {
				try {
					if (SRPlayer.this.playState == PlayerService.STOP) {
						setBufferText(-1);
						startStopButton.setImageResource(R.drawable.buffer_white);
						startPlaying();
					} else {
						stopPlaying();
						startStopButton.setImageResource(R.drawable.play_white);
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
        		GenerateNewList(SRPlayer.CATEGORIES, 0, "", "", false,
        				(CategoryArray.size() > 0) ? CategoryArray : null);
        	}
		});
        
        RelativeLayout AlarmChannelLayout = (RelativeLayout) findViewById(R.id.AlarmChannelLayout);
        AlarmChannelLayout.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		if (ChannelArray.size() == 0)
        		{
        			podcastinfothread = new PodcastInfoThread(SRPlayer.this, PodcastInfoThread.GET_CHANNEL_LIST, 0);
        			podcastinfothread.start();  
        			waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
        		}
        		else
        		{
        			OpenAlarmChannelDialog();
        		}
        		
        	}
		});
        
        RelativeLayout AlarmTimeLayout = (RelativeLayout) findViewById(R.id.AlarmTimeLayout);
        AlarmTimeLayout.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	        		        		
        		OpenAlarmTimeDialog();
        	}
		});
        
        RelativeLayout AlarmRepLayout = (RelativeLayout) findViewById(R.id.AlarmRepLayout);
        AlarmRepLayout.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	        		        		
        		OpenAlarmRepeatDialog();
        	}
		});
        
        CheckBox AlarmEnable = (CheckBox) findViewById(R.id.AlarmEnable);
        AlarmEnable.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	        		        		
        		CheckBox checkBox = (CheckBox) findViewById(R.id.AlarmEnable);
        		//SharedPreferences settings = getPreferences(0);
        		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        		SharedPreferences.Editor editor = settings.edit();
        		editor.putBoolean("AlarmEnable", checkBox.isChecked());
        		editor.commit();                
                SRPlayerAlarm.HandleAlarmStateChange(getApplicationContext());
        	}
		});
                
		if (this.playState == PlayerService.BUFFER) {
			startStopButton.setImageResource(R.drawable.buffer_white);
		} if (this.playState == PlayerService.STOP) {
			startStopButton.setImageResource(R.drawable.play_white);
		} else {
			startStopButton.setImageResource(R.drawable.pause_white);
		}
		
		SRPlayerDB = new SRPlayerDBAdapter(this);
		SRPlayerDB.open();		
		
		// Restore save text strings
		if ( savedInstanceState != null ) {
			try {
				Log.d(TAG,"savedInstanceState is not null");
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
	  				  				  		
	  			ArrayList<History> HistoryList = (ArrayList<History>) savedInstanceState.getSerializable("historylist");
	  			if ((HistoryList != null) && (!HistoryList.isEmpty()))
	  			{
	  				//Log.d(TAG,"Current action is " +  CurrentAction);	  				
	  				int HistoryIndex = HistoryList.size();        		        	
		        	History CurrentHistory = HistoryList.get(HistoryIndex-1);    
		        	CurrentAction = CurrentHistory.ReadAction();		        	
		        	String CurrID = CurrentHistory.ReadID();
		        	PoddIDLabel = CurrentHistory.ReadLabel();
		        	Object CurrObject = CurrentHistory.ReadStreamdata();	        	
		        	GenerateNewList(CurrentAction, 0, CurrID, PoddIDLabel, true,CurrObject);		        	
	  			}
	  			
	  			SleepTimerDelay = savedInstanceState.getInt("SleepTimerDelay");
	  		} catch (Exception e) {
	  			Log.e(SRPlayer.TAG, "Problem setting next song name", e);
	  		}
		}		
								
		UpdateList();
		if (HistoryList.isEmpty())
		{
			UpdatePlayerVisibility(false);
		}
					
		//Add a contextmenu for the listview
		registerForContextMenu(getListView());
		
		//Start the download service so it can check if there are
		//any podcasts to download
		startService(new Intent(SRPlayer.this, DownloadPodcastService.class));			

	}
	
	
		
	@Override
	protected void onPause() {
		Log.d("TAG","OnPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.d(TAG,"OnStop");
		if ( this.downloadTimerTask != null) {
			this.downloadTimerTask.cancel();
		}
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {		
		savedInstanceState.putInt("playState", this.playState);
		TextView tv = (TextView) findViewById(R.id.PageLabel);
		savedInstanceState.putString("stationNamn", tv.getText().toString());
		tv = (TextView) findViewById(R.id.ProgramNamn);
		savedInstanceState.putString("programNamn", tv.getText().toString());
		tv = (TextView) findViewById(R.id.NextProgramNamn);
		savedInstanceState.putString("nextProgramNamn", tv.getText().toString());
		tv = (TextView) findViewById(R.id.SongNamn);
		savedInstanceState.putString("songName", tv.getText().toString());
		tv = (TextView) findViewById(R.id.NextSongNamn);
		savedInstanceState.putString("nextSongName", tv.getText().toString());
		savedInstanceState.putInt("SleepTimerDelay", SleepTimerDelay);		
		//savedInstanceState.putParcelableArrayList("historylist", (ArrayList<? extends Parcelable>) HistoryList);
		savedInstanceState.putSerializable("historylist", (Serializable) HistoryList);		
		Log.d(TAG,"onSaveInstanceState");
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
			stopService(new Intent(SRPlayer.this,
                    DownloadPodcastService.class));
			//Since the service is also killed, 
			//clear the history
			HistoryList.clear();
						
			currentStation = new Station("P1", 
					"rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp",
					"http://api.sr.se/api/rightnowinfo/rightnowinfo.aspx?filterinfo=all",
					132,0);
		}
		else
		{			
			Log.d(TAG, "Keeping the service alive.");
		}
		if ( this.SeekTimerTask != null) {
			this.SeekTimerTask.cancel();
		}
		
		if ( this.downloadTimerTask != null) {
			this.downloadTimerTask.cancel();
		}
		
		SRPlayerDB.close();
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
					if (SRPlayer.currentStation.getStreamType() == Station.NORMAL_STREAM)			
					{
						boundService.startPlay();
						startStopButton.setImageResource(R.drawable.buffer_white);
						setBufferText(-1);
						this.playState = PlayerService.BUFFER;
					}
					else
					{
						//Check if the curren status is paused
						if (boundService.getPlayerStatus() == PlayerService.PAUSE)
						{
							boundService.resumePlay();
							startStopButton.setImageResource(R.drawable.pause_white);							
							this.playState = PlayerService.PLAY;
						}
						else
						{
							boundService.startPlay();
							startStopButton.setImageResource(R.drawable.buffer_white);
							setBufferText(-1);
							this.playState = PlayerService.BUFFER;
						}
						
					}
						
					//startStopButton.setImageResource(R.drawable.loading);
					
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

		if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
    	{
			menu.add(0, SRPlayer.MENU_UPDATE_INFO, 0, R.string.menu_update_info).
			setIcon(android.R.drawable.ic_menu_info_details);
    	}
		else
		{
			menu.add(0, SRPlayer.MENU_SEARCH, 0, R.string.menu_search).
			setIcon(android.R.drawable.ic_search_category_default);
		}
								
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
		menu.add(0, SRPlayer.MENU_HELP, 0, R.string.menu_help).
		setIcon(android.R.drawable.ic_menu_help);
		
		menu.add(0, SRPlayer.MENU_ALARM, 0, R.string.menu_alarm);
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
		
		if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
    	{
			menu.findItem(MENU_UPDATE_INFO).setTitle(R.string.menu_update_info);
			menu.findItem(MENU_UPDATE_INFO).setIcon(android.R.drawable.ic_menu_info_details);
    	}
		else
		{
			menu.findItem(MENU_SEARCH).setTitle(R.string.menu_search);
			menu.findItem(MENU_SEARCH).setIcon(android.R.drawable.ic_search_category_default);
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
			if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
			{
				boundService.restartRightNowInfo(true);
			}
			else
			{
				 onSearchRequested();
			}
			return true;
		case SRPlayer.MENU_HELP:			
			Intent FaqIntent = new Intent(SRPlayer.this, FaqActivity.class);
			SRPlayer.this.startActivity(FaqIntent);
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
		case MENU_ALARM:								
			GenerateNewList(SRPlayer.ALARM, 0, "", "Alarm", false,null);
			return true;
			
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void handleMenuAbout() {		
		View view = View.inflate(this, R.layout.about, null);
		TextView textView = (TextView) view.findViewById(R.id.message);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText(R.string.about_message);
		new AlertDialog.Builder(this)
				.setTitle(getResources().getText(R.string.about_title))		
		        .setView(view)
				.setPositiveButton(android.R.string.ok, null)
		        .setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// Do nothing...
							}
						})
				.show();
			
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
		
		//If the stream is a Pod stream, pause it
		//instead
		if (SRPlayer.currentStation.getStreamType() == Station.NORMAL_STREAM)			
			this.boundService.stopPlay();
		else
			this.boundService.pausePlay();
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
              			if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
              			{
                	  	tv = (TextView) findViewById(R.id.PageLabel);
              			tv.setText(SRPlayer.currentStation.getStationName());
              			}
              			startStopButton.setImageResource(R.drawable.play_white);
                	  break;
                  case MSGNEWPODINFO :
                	  if (waitingfordata != null)
                		  waitingfordata.dismiss();                      
                      UpdatePlayerVisibility(true);
                      UpdateList();
                      if (CurrentAction == SRPlayer.ALARM)
                      {
                    	  OpenAlarmChannelDialog();
                      }                      
                      break;
                  case MSGUPDATESEEK :
                	  UpdateSeekBar();
                	  if (( SeekTimerTask != null) && (playState == PlayerService.STOP)) {
                		 SeekTimerTask.cancel(); //Cancel after update if player is stopped
              			} 
                	  break;
                  case MSGDOWNLOADTIMERELAPS :
                	  UpdateDownloadList();
                	  break;
             }
             super.handleMessage(msg);
        }
   };

   public void onSeekReqUpdate() {
	   Message m = new Message();
       m.what = SRPlayer.MSGUPDATESEEK;       
       SRPlayer.this.viewUpdateHandler.sendMessage(m); 
	}
   
	public void onRightNowChannelInfoUpdate(RightNowChannelInfo info) {
		Message m = new Message();
        m.what = SRPlayer.MSGUPDATECHANNELINFO;
        m.getData().putSerializable("data", info);
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
	}

	public void onPlayerBuffer(int percent) {
		//startStopButton.setImageResource(R.drawable.loading);
		startStopButton.setImageResource(R.drawable.buffer_white);
		setBufferText(percent);
	}

	public void onPlayerStarted() {
		//startStopButton.setImageResource(R.drawable.stop);
		startStopButton.setImageResource(R.drawable.pause_white);
		this.playState = PlayerService.PLAY;
		if ((HistoryList.isEmpty()) || (CurrentAction == SRPlayer.PLAYER))
		{
		TextView tv = (TextView) findViewById(R.id.PageLabel);
	    tv.setText(SRPlayer.currentStation.getStationName());
		}
		
	    if ( this.SeekTimerTask != null) {
			this.SeekTimerTask.cancel();
		}
	    SeekTimerTask = new seekTimerTask(this);
	    SeekTimer.schedule(SeekTimerTask, 0, 1000);
	}

	public void onPlayerStoped() {		
		Message m = new Message();
        m.what = SRPlayer.MSGPLAYERSTOP;
        SRPlayer.this.viewUpdateHandler.sendMessage(m);
        
        if ( this.SeekTimerTask != null) {
			this.SeekTimerTask.cancel();
		}        
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
   		tv.setText(R.string.ProgramListLabel);   		
   	}
   	else if (CurrentAction == SRPlayer.CATEGORIES)
   	{
   		tv.setText("Kategorier");
   	}
   	else if (CurrentAction == SRPlayer.ALARM)
   	{
   		tv.setText("Alarm");
   	}
   	else
   	{        		                        	
 		tv.setText(PoddIDLabel);
   	}
   	
   }

   public void UpdateArray(Object PodObject)
    {        
    	if (PodObject == null)
    	{    	
    		PodInfo.clear();
    	}
    	else
    	{
	    	PodInfo.clear();	    	
		    List<PodcastInfo> NewPodInfo = (List<PodcastInfo>)PodObject;
		    PodInfo.addAll(NewPodInfo);
		    	    
		    if ((CurrentAction == CATEGORIES) && (CategoryArray.size() == 0))
		    {
		    	//store the categories list
		    	CategoryArray.clear();
		    	CategoryArray.addAll(NewPodInfo);
		    }
		    else if ((CurrentAction == PROGRAMS) && (ProgramArray.size() == 0))
		    {
		    	//store the program list
		    	ProgramArray.clear();
		    	ProgramArray.addAll(NewPodInfo);
		    }
		    else if (((CurrentAction == CHANNELS) || (CurrentAction == ALARM)) 
		    		&& (ChannelArray.size() == 0))
		    {
		    	//store the program list
		    	ChannelArray.clear();
		    	ChannelArray.addAll(NewPodInfo);
		    }
	    
    	}
    	
    	int HistSize = HistoryList.size();
    	if (HistSize > 0)
    	{
    		HistoryList.get(HistSize-1).SetStreamdata(PodObject);
    	}
    	
    	Message m = new Message();
        m.what = SRPlayer.MSGNEWPODINFO;
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
    };
    
    private void UpdateSeekBar()
    {
    	TextView tv;
    	SeekBar sb = (SeekBar) findViewById(R.id.PlayerSeekBar);
    	int CurrentTime = 0; 
		int TimeLeft = 0;    		
		if ( this.boundService != null )
		{
			CurrentTime = this.boundService.GetPosition();
			TimeLeft = this.boundService.GetDuration();			
		}
		tv = (TextView) findViewById(R.id.SeekStartTime);
		int Minutes, Seconds;
		if (CurrentTime >= 0)
		{			
			Minutes = CurrentTime / (1000 * 60);
			Seconds = (CurrentTime / 1000) % 60;
			tv.setText(String.format("%d:%02d",Minutes,Seconds));
		}
		else
		{
			tv.setText("0:00");
		}
		
		tv = (TextView) findViewById(R.id.SeekEndTime);
		if (TimeLeft >= 0)
		{
			Seconds = (TimeLeft / 1000);
			sb.setMax(Seconds);
			sb.setSecondaryProgress(sb.getMax());
			Seconds = (CurrentTime / 1000);
			sb.setProgress(Seconds);			
			TimeLeft = Math.abs(CurrentTime -  TimeLeft);
			Minutes = TimeLeft / (1000 * 60);
			Seconds = (TimeLeft / 1000) % 60;
			tv.setText(String.format("-%d:%02d",Minutes,Seconds));
			tv.setVisibility(View.VISIBLE);
		}
		else
		{
			sb.setProgress(0);
			sb.setSecondaryProgress(sb.getMax());
			tv.setVisibility(View.GONE);
		}
    }
    
    private void UpdatePlayerVisibility(boolean Hide)
    {
    	View LayoutToHide = null;
    	View LayoutToShow = null;
    	Animation FadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fadein);
    	TextView tv = (TextView) findViewById(R.id.PageLabel);
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		boolean DisableAnim = prefs.getBoolean("AnimationDisable", false);
    	
    	if (!DisableAnim)
    		tv.startAnimation(FadeInAnimation);
    	
		tv.setText(SRPlayer.currentStation.getStationName());
		Button PlayerButton = (Button) findViewById(R.id.PlayerButton);
		ScrollView sv = (ScrollView) findViewById(R.id.PlayerLayout);
		ViewGroup.MarginLayoutParams Layout = (MarginLayoutParams) sv.getLayoutParams();
				
		final float scale = getBaseContext().getResources().getDisplayMetrics().density;		
		
		if (Hide)
    	{
    		//Hide the player
    		LayoutToHide = (View)findViewById(R.id.PlayerLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		LayoutToHide = (View)findViewById(R.id.PlayerControlsLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		Animation SliedFromBottomAnimation = AnimationUtils.loadAnimation(this, R.anim.slidefrombottom);
    		//Animation SliedFromBottomAnimation = AnimationUtils.loadAnimation(this, R.anim.slidefromright);
    		//Show the listview
    		
    		if (CurrentAction != SRPlayer.ALARM)
    		{	
    			LayoutToHide = (View)findViewById(R.id.AlarmLayout);
        		LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToShow = (View)findViewById(R.id.ListViewLayout);
    			LayoutToShow.setVisibility(View.VISIBLE);
    		}
    		else
    		{    		
    			tv = (TextView) findViewById(R.id.PageLabel);
    	    	tv.setText("Alarm");
    	    	
    			UpdateAlarmView();
    			LayoutToShow = (View)findViewById(R.id.AlarmLayout);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToHide = (View)findViewById(R.id.ListViewLayout);
        		LayoutToHide.setVisibility(View.GONE);
    		}
    		    		
    		if (!DisableAnim)
    			LayoutToShow.startAnimation(SliedFromBottomAnimation);
    		
    		PlayerButton.setBackgroundResource(R.drawable.player);
    		
    		LayoutToHide = (View)findViewById(R.id.SeekLayout);
    		LayoutToHide.setVisibility(View.GONE);
    	}
    	else
    	{    	
    		PlayerButton.setBackgroundResource(R.drawable.player_pressed);
    		
    		Button ProgramButton = (Button) findViewById(R.id.ProgButton);
			Button CategoryButton = (Button) findViewById(R.id.PodCatButton);			    	
			Button FavoriteButton = (Button) findViewById(R.id.Favorites);			    	
			Button ChannelButton = (Button) findViewById(R.id.Channels);			    		
			
			ProgramButton.setBackgroundResource(R.drawable.channel_prog_select);
    		CategoryButton.setBackgroundResource(R.drawable.category);
    		FavoriteButton.setBackgroundResource(R.drawable.favorite);
    		ChannelButton.setBackgroundResource(R.drawable.channel);
    		
    		//Show the player
    		LayoutToShow = (View)findViewById(R.id.PlayerLayout);
    		LayoutToShow.setVisibility(View.VISIBLE);
    		if (!DisableAnim)
    			LayoutToShow.startAnimation(FadeInAnimation);
    		
    		LayoutToShow = (View)findViewById(R.id.PlayerControlsLayout);
    		LayoutToShow.setVisibility(View.VISIBLE);
    		if (!DisableAnim)
    			LayoutToShow.startAnimation(FadeInAnimation);
    		    		    		
    		//Hide the listview
    		LayoutToHide = (View)findViewById(R.id.ListViewLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		//Hide the AlarmLayout
    		LayoutToHide = (View)findViewById(R.id.AlarmLayout);
    		LayoutToHide.setVisibility(View.GONE);
    		
    		//Check the mode 
    		if (SRPlayer.currentStation.getStreamType() == Station.NORMAL_STREAM)
    		{
    			//All text should be visible
    			LayoutToShow = (View)findViewById(R.id.NextProgramNamnLabel);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToShow = (View)findViewById(R.id.NextProgramNamn);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToShow = (View)findViewById(R.id.SongNamnLabel);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToShow = (View)findViewById(R.id.SongNamn);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToShow = (View)findViewById(R.id.NextSongNamnLabel);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			
    			LayoutToShow = (View)findViewById(R.id.NextSongNamn);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			    			 
    			Layout.bottomMargin = (int) (65 * scale);
    			sv.setLayoutParams(Layout);
    			
    			LayoutToHide = (View)findViewById(R.id.SeekLayout);
        		LayoutToHide.setVisibility(View.GONE);    			
    		}
    		else
    		{
    			//All text should be visible
    			LayoutToHide = (View)findViewById(R.id.NextProgramNamnLabel);
    			LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToHide = (View)findViewById(R.id.NextProgramNamn);
    			LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToHide = (View)findViewById(R.id.SongNamnLabel);
    			LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToHide = (View)findViewById(R.id.SongNamn);
    			LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToHide = (View)findViewById(R.id.NextSongNamnLabel);
    			LayoutToHide.setVisibility(View.GONE);
    			
    			LayoutToHide = (View)findViewById(R.id.NextSongNamn);
    			LayoutToHide.setVisibility(View.GONE);
    			    			
    			Layout.bottomMargin = (int) (100 * scale);
    			sv.setLayoutParams(Layout);
    			
    			sv.setLayoutParams(Layout);
    			
    			LayoutToShow = (View)findViewById(R.id.SeekLayout);
    			LayoutToShow.setVisibility(View.VISIBLE);
    			if (!DisableAnim)
    				LayoutToShow.startAnimation(FadeInAnimation);
    			    			
    			UpdateSeekBar();
    		}
    	}
    }
            
    protected void GenerateNewList(int Action, int position, String ID, String Label, boolean NoNewHist, Object SavedAdapter)
    {
    	if (Action != SRPlayer.SEARCH_RESULT)
    		CurrentAction = Action;
    	
    	TextView tv;
    	Cursor FavCursor;
    	PodcastInfo FavoritesInfo;
    	boolean DifferentLabelsForEachEpisode = false;
    	
       	int HighlightedButton = 0;
       	
       	if ( this.downloadTimerTask != null) {
			this.downloadTimerTask.cancel();
		}
       	       	       	
    	switch (Action)
    	{
    	case SRPlayer.ALARM :
    		UpdatePlayerVisibility(true);
    		
    		//Init the history
            HistoryList.clear();
            HistoryList.add(new History(SRPlayer.ALARM,"","Alarm",null));
			break;
    	case SRPlayer.PLAYER:
    		//Just show the player    		    	   	
    		UpdatePlayerVisibility(false);
    		break;
    	case SRPlayer.PROGRAMS:
    		if (SavedAdapter == null)
    		{
    		podcastinfothread = new PodcastInfoThread(SRPlayer.this, PodcastInfoThread.GET_ALL_PROGRAMS, 0);
            podcastinfothread.start();  
            waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
    		}
    		
            //Init the history
            HistoryList.clear();
            HistoryList.add(new History(SRPlayer.PROGRAMS,"",getResources().getString(R.string.ProgramListLabel),SavedAdapter));
    		break;
    	case SRPlayer.CHANNELS:
    		//Live mode    		
    		HighlightedButton = 3;
    		PoddIDLabel = "Kanaler";
    	   	if (SavedAdapter == null)
    		{
	    		podcastinfothread = new PodcastInfoThread(SRPlayer.this, PodcastInfoThread.GET_CHANNEL_LIST, 0);
	            podcastinfothread.start();  
	            waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
    		}
    	   	
    	   	//Init the history
            HistoryList.clear();
            HistoryList.add(new History(SRPlayer.CHANNELS,"","Kanaler",SavedAdapter));
            break;
    	case SRPlayer.CATEGORIES:
    		//Init the history
    		HighlightedButton = 1;
    		HistoryList.clear();
	        HistoryList.add(new History(SRPlayer.CATEGORIES,"","",SavedAdapter));
	        
	        if (SavedAdapter == null)
    		{    		
	        podcastinfothread = new PodcastInfoThread(SRPlayer.this, PodcastInfoThread.GET_CATEGORIES, 0);
	        podcastinfothread.start();  
	        waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
    		}
	        
	        break;
	        
    	case SRPlayer.PROGRAMS_IN_A_CATEGORY:
    		//A specific category has been selected. 
        	//Retreive a list of all programs in the category
        	PoddIDLabel = Label;
        	HighlightedButton = 1;
    		
        	if (SavedAdapter == null)
    		{    		
        	podcastinfothread = new PodcastInfoThread(SRPlayer.this, SRPlayer.PROGRAMS_IN_A_CATEGORY, Integer.valueOf(ID));
            podcastinfothread.start();  
            waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
    		}
            //Add a new level to the history
            if (!NoNewHist)
            	HistoryList.add(new History(SRPlayer.PROGRAMS_IN_A_CATEGORY,ID,Label,SavedAdapter));
    		break;
    		
    	case GET_IND_PROGRAMS:        		
    		//A specific program has been selected
        	//Retreive a list of all stored podcasts for
        	//that channel
        	String PoddId = ID;
        	PoddIDLabel = Label;
        	HighlightedButton = -1; //Remain the button that was highlighted before
    		
        	if (SavedAdapter == null)
    		{    		
        	podcastinfothread = new PodcastInfoThread(SRPlayer.this, SRPlayer.GET_IND_PROGRAMS, Integer.valueOf(PoddId));
            podcastinfothread.start();  
            waitingfordata = ProgressDialog.show(SRPlayer.this,"SR Player",getResources().getString(R.string.PodlistProgressText));
    		}
        	
            if (!NoNewHist)                
            	HistoryList.add(new History(SRPlayer.GET_IND_PROGRAMS,PoddId,Label,SavedAdapter));
    		
    		break;
    	case SEARCH_RESULT:
    		    		
    		int PodInfoSize = PodInfo.size();
    		int LoopVar = 0;
    		int Count = 0;
    		String CurrTitle,CurrDesc;
    		List<PodcastInfo> TempPodInfo = new ArrayList<PodcastInfo>();
    		if (SavedAdapter == null)
    		{    			    			    	
        		for (LoopVar = 0; LoopVar < PodInfoSize; LoopVar++)
        		{
        			//Loop over all the items and check which of them
        			//that do not match
        			CurrTitle = PodInfo.get(LoopVar).getTitle();
        			CurrDesc = PodInfo.get(LoopVar).getDescription();
        			if ((CurrTitle.toLowerCase().indexOf(LastQuery) >= 0) ||
        				((CurrDesc != null) && (CurrDesc.toLowerCase().indexOf(LastQuery) >= 0)))
        			{
        				//Match        				
        				TempPodInfo.add(PodInfo.get(LoopVar));
        				Count++;
        			}
        		}
    		}
    		    		
    		if ((Count > 0) || (SavedAdapter != null))
    		{
    			UpdatePlayerVisibility(true);
    			tv = (TextView) findViewById(R.id.PageLabel);
    			tv.setText(R.string.search_result_label);
    			PoddIDLabel = getResources().getString(R.string.search_result_label);
    		}
    		    		    		
        	if (Count > 0)
        	{        			        		
        			PodInfo.clear();
        			PodInfo.addAll(TempPodInfo);
        			setListAdapter(PodList);
        			if (!NoNewHist)
        			{
        				
            	   		HistoryList.add(new History(SRPlayer.SEARCH_RESULT,"",
            	   				PoddIDLabel,
            	   				TempPodInfo));            	   		
        			}
        	}
        	else if (SavedAdapter == null)
        	{
        		//TODO No match. Generate a toast
       			Context context = getApplicationContext();		
       			int duration = Toast.LENGTH_LONG;
        		Toast toast = Toast.makeText(context, R.string.no_match, duration);
        		toast.show();
        	}
        	
        	if (SavedAdapter != null)
    		{
    			CurrentAction = HistoryList.get(HistoryList.size()-2).ReadAction();    		
    			PodInfo.clear();
    			TempPodInfo.addAll((Collection<? extends PodcastInfo>) SavedAdapter);    			
    		    PodInfo.addAll((Collection<? extends PodcastInfo>) TempPodInfo);
    		    SavedAdapter = null;    	
    		    setListAdapter(PodList);
    		}
        	        	
    		break;
    	case FAVORITES:
    		HighlightedButton = 2;
    		
    		PodInfo.clear();	    	    	        	   
    	    int RowCount;    	        	    
    	    String RowCountStr = "";
    	    String SinglerFooter = getResources().getString(R.string.SingleFavoriteAdded);
    	    String MultipleFooter = getResources().getString(R.string.MultipleFavoriteAdded);    	       	    
    	    
    	    RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.CHANNEL));
    	    RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);    	    
    	    
    	    FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle("Kanaler");
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.CHANNEL));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    	
	    	RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.PROGRAM));
	    	RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);    	        	   
    	    
	    	FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle("Program");
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.PROGRAM));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    	
	    	RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.EPISODE));
	    	RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);    	        	   
    	    
	    	FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle("Avsnitt");
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.EPISODE));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    	
	    	RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.CATEGORY));
	    	RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);    	        	   
    	    
	    	FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle("Kategorier");
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.CATEGORY));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    		    	
	    	RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.EPISODE_OFFLINE));	    	
    	    RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);    	        	   
    	      	    	 
	    	FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle("Offline avsnitt");
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.EPISODE_OFFLINE));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    	
	    	RowCount = SRPlayerDB.fetchCountByType(String.valueOf(SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD));
	    	RowCountStr = String.format("%d %s", RowCount,(RowCount == 1) ? SinglerFooter : MultipleFooter);
	    	
	    	FavoritesInfo = new PodcastInfo();
	    	FavoritesInfo.setTitle(getResources().getString(R.string.queue_list_label));
	    	FavoritesInfo.setID(String.valueOf(SRPlayerDBAdapter.DOWNLOAD_QUEUE));
	    	FavoritesInfo.setDescription(RowCountStr);
	    	PodInfo.add(FavoritesInfo);
	    	
    		setListAdapter(PodList);
    	   	UpdatePlayerVisibility(true);
    	   	tv = (TextView) findViewById(R.id.PageLabel);
    	   	tv.setText("Favoriter");
    	   	
    	   	//Init the history
            HistoryList.clear();
            HistoryList.add(new History(SRPlayer.FAVORITES,"","Favoriter",SavedAdapter));
            
    		break;
    	case DOWNLOAD_QUEUE:
    		DifferentLabelsForEachEpisode = true;
    		downloadTimerTask = new downloadTimerTask(this);
    	    downloadTimer.schedule(downloadTimerTask, 0, 1000);
    	    ID = String.valueOf(SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD);
    	    startService(new Intent(SRPlayer.this, DownloadPodcastService.class));
    	case FAVORITES_OFFLINE_PROGRAMS :    		
    	case FAVORITES_IND_PROGRAMS :
    		DifferentLabelsForEachEpisode = true;
    	case FAVORITES_CHANNELS : 
    	case FAVORITES_PROGRAMS :     	
    	case FAVORITES_CATEGORIES : 	
    	
    		HighlightedButton = -1;
    		    	    		    	
    		PodInfo.clear();
    		String CurrentID = ID;
    		    		
    		if (CurrentAction == DOWNLOAD_QUEUE)
    		{
    			FavCursor = SRPlayerDB.fetchPodcastsToDownload();
    		}
    		else
    		{
    			FavCursor = SRPlayerDB.fetchFavoritesByType(CurrentID);
    		}
    	    int FavId = 0;
    	    String FavLabel = ""; 
    	    String FavLink, FavDesc, FavGuid;
    	    //Insert the records in the array
    	    if (FavCursor.moveToFirst())
    	    {
    	    	do {
    	    		FavId = FavCursor.getInt(SRPlayerDBAdapter.INDEX_ID);
    	    		FavLabel = FavCursor.getString(SRPlayerDBAdapter.INDEX_LABEL);
    	    		FavLink = FavCursor.getString(SRPlayerDBAdapter.INDEX_LINK);
    	    		FavDesc = FavCursor.getString(SRPlayerDBAdapter.INDEX_DESC);
    	    		FavGuid = FavCursor.getString(SRPlayerDBAdapter.INDEX_GUID);
    	    		
    	    		FavoritesInfo = new PodcastInfo();
        	    	FavoritesInfo.setTitle(FavLabel);
        	    	FavoritesInfo.setID(String.valueOf(FavId));
        	    	
        	    		
        	    	FavoritesInfo.setLink(FavLink);
        	    	FavoritesInfo.setDescription(FavDesc);
        	    	FavoritesInfo.setDBIndex(FavCursor.getInt(SRPlayerDBAdapter.INDEX_ROWID));
        	    	FavoritesInfo.setType(Integer.parseInt(CurrentID));
        	    	FavoritesInfo.setGuid(FavGuid);
        	    	FavoritesInfo.setBytesdownloaded(FavCursor.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED));
        	    	FavoritesInfo.setFilesize(FavCursor.getInt(SRPlayerDBAdapter.INDEX_FILESIZE));
        	    	PoddIDLabel = FavCursor.getString(SRPlayerDBAdapter.INDEX_NAME);
        	    	if (DifferentLabelsForEachEpisode)
        	    		//The episodes in the list is not from the same program
        	    		//Save the name of the parent i.e. the program in the PoddIDLabel
        	    		FavoritesInfo.setPoddID(PoddIDLabel);
        	    	else        	    		
        	    		FavoritesInfo.setPoddID(String.valueOf(FavId));
        	    	
        	    	PodInfo.add(FavoritesInfo);        	    	        	    	
    	    	} while (FavCursor.moveToNext());
    	    }
    	    FavCursor.close();
    		
    		
    		
    		setListAdapter(PodList);    		
    		
    	   	UpdatePlayerVisibility(true);
    	   	tv = (TextView) findViewById(R.id.PageLabel);    	   	    	   	
    	   	String FavoriteLabel = "Favoriter - ";
    	   	    	   	
    	   	switch (CurrentAction)
    	   	{
	    	case DOWNLOAD_QUEUE:
	    		FavoriteLabel += getResources().getString(R.string.download_queue_label);
	    		break;
	    	case FAVORITES_OFFLINE_PROGRAMS :    		
	    		FavoriteLabel += getResources().getString(R.string.offline_epdisode_label);
	    		break;
	    	case FAVORITES_IND_PROGRAMS :
	    		FavoriteLabel += getResources().getString(R.string.epdisode_label);
	    		break;
	    	case FAVORITES_CHANNELS : 
	    		FavoriteLabel += getResources().getString(R.string.channel_label);
	    		break;
	    	case FAVORITES_PROGRAMS :     	
	    		FavoriteLabel += getResources().getString(R.string.program_label);
	    		break;
	    	case FAVORITES_CATEGORIES :
	    		FavoriteLabel += getResources().getString(R.string.category_label);
	    		break;
    	   	}
    	   	tv.setText(FavoriteLabel);
    	   	//Add to the history
    	   	if (!NoNewHist)
    	   		HistoryList.add(new History(Action,ID,FavoriteLabel,SavedAdapter));
            
    		break;
    	
        default:
        	break;
    	}
    	
    	if (HighlightedButton >= 0)
    	{
    		Button ProgramButton = (Button) findViewById(R.id.ProgButton);
			Button CategoryButton = (Button) findViewById(R.id.PodCatButton);
			Button PlayerButton = (Button) findViewById(R.id.PlayerButton);
			Button FavoritesButton = (Button) findViewById(R.id.Favorites);
			Button ChannelButton = (Button) findViewById(R.id.Channels);
			PlayerButton.setBackgroundResource(R.drawable.player);
			
    		switch (HighlightedButton)
    		{
    		case 0:
    			ProgramButton.setBackgroundResource(R.drawable.channel_prog_select_pressed);
    			CategoryButton.setBackgroundResource(R.drawable.category);
    			FavoritesButton.setBackgroundResource(R.drawable.favorite);
    			ChannelButton.setBackgroundResource(R.drawable.channel);
    			break;
    		case 1:    		    		
    			ProgramButton.setBackgroundResource(R.drawable.channel_prog_select);
    			CategoryButton.setBackgroundResource(R.drawable.category_pressed);
    			FavoritesButton.setBackgroundResource(R.drawable.favorite);
    			ChannelButton.setBackgroundResource(R.drawable.channel);
    			break;
    		case 2:
    			ProgramButton.setBackgroundResource(R.drawable.channel_prog_select);
    			CategoryButton.setBackgroundResource(R.drawable.category);
    			FavoritesButton.setBackgroundResource(R.drawable.favorite_pressed);
    			ChannelButton.setBackgroundResource(R.drawable.channel);
    			break;
    		case 3:
    			ProgramButton.setBackgroundResource(R.drawable.channel_prog_select);
    			CategoryButton.setBackgroundResource(R.drawable.category);
    			FavoritesButton.setBackgroundResource(R.drawable.favorite);
    			ChannelButton.setBackgroundResource(R.drawable.channel_pressed);
    			break;
    		default:
    			break;
    		}
    		
    	}
    	
    	if (SavedAdapter != null)
       	{
       		UpdateArray(SavedAdapter);           
       	}
		
    }
    
    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            currentPosition = position;        
            
            switch (CurrentAction)
        	{
            case SRPlayer.FAVORITES:
            	String FavoriteType = PodInfo.get(currentPosition).getID();
            	//PoddIDLabel = PodInfo.get(currentPosition).getTitle();        	            	
            	
                GenerateNewList(SRPlayer.FAVORITES_CHANNELS+Integer.parseInt(FavoriteType), currentPosition, FavoriteType, PoddIDLabel, false,null);
            	break;
            case SRPlayer.FAVORITES_CATEGORIES:
        	case SRPlayer.CATEGORIES:
        		//A specific category has been selected. 
            	//Retreive a list of all programs in the category
            	String CategoryId = PodInfo.get(currentPosition).getID();
            	PoddIDLabel = PodInfo.get(currentPosition).getTitle();
        		
                GenerateNewList(SRPlayer.PROGRAMS_IN_A_CATEGORY, currentPosition, CategoryId, PoddIDLabel, false,null);
                break;	
        	case SRPlayer.DOWNLOAD_QUEUE:
        		break;
        	case SRPlayer.FAVORITES_PROGRAMS:
        	case SRPlayer.PROGRAMS:        		
        	case SRPlayer.PROGRAMS_IN_A_CATEGORY:
        		//A specific program has been selected
            	//Retreive a list of all stored podcasts for
            	//that channel
            	
        		String PoddId = PodInfo.get(currentPosition).getPoddID();
            	PoddIDLabel = PodInfo.get(currentPosition).getTitle();
        		GenerateNewList(SRPlayer.GET_IND_PROGRAMS, currentPosition, PoddId, PoddIDLabel, false,null);
        		break;
        	
        	case SRPlayer.FAVORITES_OFFLINE_PROGRAMS:
        		//Check that the type is not an episode that is being downloaded
        		if (PodInfo.get(currentPosition).getType() == SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD)
        			break;
        	case SRPlayer.FAVORITES_IND_PROGRAMS:
        		//The name of the parent is stored in the PoddID
        		PoddIDLabel = PodInfo.get(currentPosition).getPoddID();
        	case SRPlayer.GET_IND_PROGRAMS:    

        		SRPlayer.currentStation.setStreamUrl(PodInfo.get(currentPosition).getLink());
    			SRPlayer.currentStation.setStationName(PoddIDLabel);
    			SRPlayer.currentStation.setChannelId(0);
    			SRPlayer.currentStation.setStreamType((CurrentAction != FAVORITES_OFFLINE_PROGRAMS) ? Station.POD_STREAM : Station.OFFLINE_STREAM);    			
    			SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_URL);    			
    			boundService.selectChannel(SRPlayer.currentStation);		
    			clearAllText();
    			UpdatePlayerVisibility(false);
    			RightNowChannelInfo info = new RightNowChannelInfo();
    			info.setProgramTitle(PodInfo.get(currentPosition).getTitle());
    			info.setProgramInfo(PodInfo.get(currentPosition).getDescription());
    			boundService.rightNowUpdate(info);    
    			//HistoryList.clear(); Dont clear. Add to player to history instead
    			HistoryList.add(new History(SRPlayer.PLAYER,"",PoddIDLabel,null));
    			CurrentAction = SRPlayer.PLAYER;
        		break;
        	case SRPlayer.FAVORITES_CHANNELS:
        	case SRPlayer.CHANNELS:
        		//ChannelIndex = this.boundService.getStationIndex();
        		//int NewChannelIndex = (CurrentAction == SRPlayer.CHANNELS) ? position : Integer.parseInt(PodInfo.get(currentPosition).getID());        		
        		if (this.boundService.getChannelID() != Integer.parseInt(PodInfo.get(currentPosition).getID()))
            	{    	        	
        			SRPlayer.currentStation.setStreamUrl(PodInfo.get(currentPosition).getLink());
    				SRPlayer.currentStation.setStationName(PodInfo.get(currentPosition).getTitle());
    				SRPlayer.currentStation.setChannelId(Integer.parseInt(PodInfo.get(currentPosition).getID()));
    				//SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_URL);
    				SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_IND_URL+PodInfo.get(currentPosition).getID());
    				SRPlayer.currentStation.setStreamType(Station.NORMAL_STREAM);					
    				boundService.selectChannel(SRPlayer.currentStation);					
    				clearAllText();
    				
            	}
        		
        		UpdatePlayerVisibility(false); //Show the player
        		CurrentAction = SRPlayer.PLAYER;
        		//HistoryList.clear();
        		HistoryList.add(new History(SRPlayer.PLAYER,"",SRPlayer.currentStation.getStationName(),null));
        		break;
            default:
            	break;
        	}
   }
    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (HistoryList.size() > 0)) {
        	//Remove the last entry in the history list and execute
        	//the previous one
        	
        	int HistoryIndex = HistoryList.size()-1;        	
        	HistoryList.remove(HistoryIndex);
        	if (HistoryIndex == 0)
        	{
        	//Return to the player screen
        	HistoryList.clear();
        	UpdatePlayerVisibility(false); //Show the player    		
        	}
        	else
        	{
        	History PrevHistory = HistoryList.get(HistoryIndex-1);    
        	int PrevAction = PrevHistory.ReadAction();
        	String PrevID = PrevHistory.ReadID();
        	String PrevLabel = PrevHistory.ReadLabel();
        	Object PrevObject = PrevHistory.ReadStreamdata();
        	
        	GenerateNewList(PrevAction, 0, PrevID, PrevLabel, true,PrevObject);
        	
        	}
        		
        	return true;

        }
        return super.onKeyDown(keyCode, event);

    } 
    
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
    	int SelectedIndex = (int)((AdapterContextMenuInfo)menuInfo).id;
		super.onCreateContextMenu(menu, v, menuInfo);
		
		if (CurrentAction <= SRPlayer.CHANNELS)
		{			
			menu.add(0, MENU_CONTEXT_ADD_TO_FAVORITES, 0, R.string.menu_add_to_favorite);
			switch (CurrentAction)
        	{
			case SRPlayer.GET_IND_PROGRAMS:
				menu.add(0, MENU_CONTEXT_DOWNLOAD, 0, R.string.menu_listen_offline);
				break;
			default:
				break;
        	}
		}
		else if (CurrentAction != SRPlayer.FAVORITES)
		{
			if (CurrentAction == SRPlayer.FAVORITES_OFFLINE_PROGRAMS)
			{
				menu.add(0, MENU_CONTEXT_DELETE_FAVORITE, 0, R.string.menu_delete_episode);
			}
			else if (CurrentAction == SRPlayer.DOWNLOAD_QUEUE)
			{
				int CurrID = (Integer.parseInt(PodInfo.get(SelectedIndex).getID())); 
				if (CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD)                	
				{
					menu.add(0, MENU_CONTEXT_ABORT_AND_DELETE, 0, R.string.menu_delete_from_queue_and_delete);
					menu.add(0, MENU_CONTEXT_PAUSE, 0, R.string.menu_pause);
				}
				else if (CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED)
				{
					menu.add(0, MENU_CONTEXT_ABORT_AND_DELETE, 0, R.string.menu_delete_from_queue_and_delete);
					menu.add(0, MENU_CONTEXT_RESUME, 0, R.string.menu_resume);				
				}
				else
				{
				menu.add(0, MENU_CONTEXT_DELETE_FAVORITE, 0, R.string.menu_delete_from_queue);
				}
			}
			else
			{
				menu.add(0, MENU_CONTEXT_DELETE_FAVORITE, 0, R.string.menu_remove_from_favorite);
			}
			
			if (CurrentAction == FAVORITES_IND_PROGRAMS)
			{
				menu.add(0, MENU_CONTEXT_DOWNLOAD, 0, R.string.menu_listen_offline);
			}
			
		}
		
    }
    
    

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();		
		long SelectedIndex = info.id;		
		String Selectedtitle = PodInfo.get((int)SelectedIndex).getTitle();
		String SelectedidStr;
		Intent AbortAndDeletIntent;
		
		switch (item.getItemId()) {
		case MENU_CONTEXT_ADD_TO_FAVORITES:
			//L�gg till bland favoriter
			Log.d(TAG, "Add to favorites selected. ID = " + String.valueOf(SelectedIndex));						
			int Selectedid = -1;
			int FavType = 0;
			
			switch (CurrentAction)
        	{
        	case SRPlayer.CATEGORIES:
        		FavType = SRPlayerDBAdapter.CATEGORY;
        		break;    			        	
        	case SRPlayer.CHANNELS:        		        		
        		Log.d(TAG, "Channel favorite added. ID set to " + String.valueOf(SelectedIndex));
        		Selectedid = Integer.parseInt(PodInfo.get((int)SelectedIndex).getID()); 
        		FavType = SRPlayerDBAdapter.CHANNEL;
        		break;
        	case SRPlayer.PROGRAMS:            	
        	case SRPlayer.PROGRAMS_IN_A_CATEGORY:            	
        		FavType = SRPlayerDBAdapter.PROGRAM;
        		break;        		
        	case SRPlayer.GET_IND_PROGRAMS:
        		FavType = SRPlayerDBAdapter.EPISODE;
        		break;
        	}
			
			if (Selectedid <0)
			{
				SelectedidStr = PodInfo.get((int)SelectedIndex).getPoddID();
				if (SelectedidStr == null)
				{				
					SelectedidStr = PodInfo.get((int)SelectedIndex).getID();
		    		if (SelectedidStr == null)
						Selectedid = 0;
					else
						Selectedid = Integer.parseInt(SelectedidStr);
				}
				else
					Selectedid = Integer.parseInt(SelectedidStr);
			}
			
			SRPlayerDB.createFavorite(FavType, 
					Selectedid, 
					Selectedtitle, 
					PodInfo.get((int)SelectedIndex).getLink(),
					PodInfo.get((int)SelectedIndex).getDescription(),
					PoddIDLabel,
					PodInfo.get((int)SelectedIndex).getGuid());						
						
			return true;				
		case MENU_CONTEXT_DELETE_FAVORITE:
			Selectedid = PodInfo.get((int)SelectedIndex).getDBIndex();
    			    							
			//If the current action is an offline program then 
			//the file should be deleted
			if (CurrentAction == SRPlayer.FAVORITES_OFFLINE_PROGRAMS)
			{
				File file = new File(PodInfo.get((int)SelectedIndex).getLink());
				file.delete();				
			}
			
			//Delete the favorite and update the array
			SRPlayerDB.deleteFavorite((long)Selectedid);
			PodInfo.remove((int)SelectedIndex);
			setListAdapter(PodList);
    		
			return true;
		case MENU_CONTEXT_ABORT_AND_DELETE:
			//Inform service that the current download should be aborted
			AbortAndDeletIntent = new Intent(SRPlayer.this, DownloadPodcastService.class);
			AbortAndDeletIntent.putExtra("Action", DownloadPodcastService.ABORT_CURRENT);
			startService(AbortAndDeletIntent);
			PodInfo.remove((int)SelectedIndex);
			setListAdapter(PodList);
			return true;
		case MENU_CONTEXT_PAUSE:
			//Inform service that the current download should be aborted
			AbortAndDeletIntent = new Intent(SRPlayer.this, DownloadPodcastService.class);
			AbortAndDeletIntent.putExtra("Action", DownloadPodcastService.PAUSE);
			startService(AbortAndDeletIntent);			
			return true;
		case MENU_CONTEXT_RESUME:
			//Inform service that the current download should be aborted
			AbortAndDeletIntent = new Intent(SRPlayer.this, DownloadPodcastService.class);
			AbortAndDeletIntent.putExtra("Action", DownloadPodcastService.RESUME);
			startService(AbortAndDeletIntent);			
			return true;
		case MENU_CONTEXT_DOWNLOAD:						
			//Episode selected to be downloaded
			//Add to database
			SRPlayerDB.createFavorite(SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD, 
					SRPlayerDBAdapter.QUEUE_FOR_DOWNLOAD, 
					Selectedtitle, 
					PodInfo.get((int)SelectedIndex).getLink(),
					PodInfo.get((int)SelectedIndex).getDescription(),
					PoddIDLabel,
					PodInfo.get((int)SelectedIndex).getGuid());
			
			//Start the service to download the new podcast. 
			StartDownloadService();
			
			return true;		
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	private void StartDownloadService()
	{
		startService(new Intent(SRPlayer.this, DownloadPodcastService.class));
		
		Context context = getApplicationContext();		
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, R.string.OfflineAddedText, duration);
		toast.show();
	}
	
	public void onDownloadTimerElapse()
	{
		Message m = new Message();
        m.what = SRPlayer.MSGDOWNLOADTIMERELAPS;
        SRPlayer.this.viewUpdateHandler.sendMessage(m);
	}
	
	public void UpdateDownloadList()
	{
		// PodcastInfo FavoritesInfo = new PodcastInfo();
		
		int PodInfoSize = PodInfo.size();
		int LoopVar = 0;
		int DBIndex = 0;
		int DeleteIndex = -1;
		boolean DataChanged = false;
		Cursor FavCursor = null;
		for (LoopVar = 0; LoopVar < PodInfoSize; LoopVar++)
		{
			DBIndex = PodInfo.get(LoopVar).getDBIndex();
			FavCursor = SRPlayerDB.fetchFavorite((long)DBIndex);		
			if (FavCursor.moveToFirst())
		    {
				//Check if either the database or the PodInfo says that it is an
				//active download
				if (PodInfo.get(LoopVar).getType() == SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD)
				{
					int CurrID = Integer.parseInt(PodInfo.get(LoopVar).getID()); 
					int CurrDBID = FavCursor.getInt(SRPlayerDBAdapter.INDEX_ID);
					int CurrDBType = FavCursor.getInt(SRPlayerDBAdapter.INDEX_TYPE);
					if ((CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD) ||
						(CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED))
                	{
						//The podinfo says that this program is current being downloaded. Verify that this is
						//still the case
						if (CurrDBType == SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD)
						{
							//Still being downloaded
							//Check if the item is paused or was paued and has resumed
							
							if ( (CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED) &&
								 (CurrDBID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD)	
								)
							{
								//No longer paused
								PodInfo.get(LoopVar).setID(String.valueOf(SRPlayerDBAdapter.ACTIVE_DOWNLOAD));
								DataChanged = true;
							}
							else if ( (CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD) &&
									 (CurrDBID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED)	
							)
							{
								//Pausad
								PodInfo.get(LoopVar).setID(String.valueOf(SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED));
								DataChanged = true;
							}
							
							if (CurrDBID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD)
							{
								//Only need to change data if it is activly downloading
								PodInfo.get(LoopVar).setBytesdownloaded(FavCursor.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED));
								PodInfo.get(LoopVar).setFilesize(FavCursor.getInt(SRPlayerDBAdapter.INDEX_FILESIZE));
								DataChanged = true;
							}
						}
						else
						{
							//Download is complete. Update link and type
							PodInfo.get(LoopVar).setLink(FavCursor.getString(SRPlayerDBAdapter.INDEX_LINK));
							PodInfo.get(LoopVar).setType(SRPlayerDBAdapter.EPISODE_OFFLINE);
							PodInfo.get(LoopVar).setDescription(FavCursor.getString(SRPlayerDBAdapter.INDEX_DESC));
							DataChanged = true;
							DeleteIndex = LoopVar; 
						}
                	}
					else if ((CurrDBType == SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD) &&
							(CurrDBID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD))
					{
							//Next in queue ready to be downloaded. Update the type
							PodInfo.get(LoopVar).setBytesdownloaded(FavCursor.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED));
							PodInfo.get(LoopVar).setFilesize(FavCursor.getInt(SRPlayerDBAdapter.INDEX_FILESIZE));
							PodInfo.get(LoopVar).setID(String.valueOf(SRPlayerDBAdapter.ACTIVE_DOWNLOAD));
							DataChanged = true;
					}					
				}	
				
		    }
			FavCursor.close();
		}
		
		if (DataChanged)
		{
			PodList.notifyDataSetChanged();
			if (DeleteIndex >= 0)
				PodInfo.remove(DeleteIndex);
		}
		
		if (FavCursor != null)
			FavCursor.close();
	}
	
	void OpenAlarmChannelDialog()
	{
		if (!ChannelArray.isEmpty())
		{
			//Resources res = getResources();
			String[] items = new String[ChannelArray.size()];
			//SharedPreferences settings = getPreferences(0);
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			// String AlarmStationName = settings.getString("AlarmStationName", "P1");
			int AlarmStationID = settings.getInt("AlarmStationID", 132);
			// String AlarmStationURL = settings.getString("AlarmStationURL", "rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp");
			int SelectedID = 0;
			
			for (int i = 0; i<ChannelArray.size(); i++)
			{
				items[i] = ChannelArray.get(i).getTitle();
				if (ChannelArray.get(i).getID().equals(String.valueOf(AlarmStationID)))
				{
					SelectedID = i;
				}
			}
	
			//Find the selected channel		
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);		
			builder.setTitle(R.string.pick_channel);
			builder.setSingleChoiceItems(items, SelectedID, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			        dialog.dismiss();
			           //Save the new channel, id and name
			        PodcastInfo NewChannel = ChannelArray.get(item);
			        //SharedPreferences settings = getPreferences(0);
			        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = settings.edit();
					editor.putInt("AlarmStationID", Integer.parseInt(NewChannel.getID()));
					editor.putString("AlarmStationName", NewChannel.getTitle());
					editor.putString("AlarmStationURL", NewChannel.getLink());
	        		editor.commit();
	        		SRPlayerAlarm.HandleAlarmStateChange(getApplicationContext());
	        		UpdateAlarmView();
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}
	
	void OpenAlarmRepeatDialog()
	{
		Resources res = getResources();
		String[] items = res.getStringArray(R.array.week_days);
		
		//SharedPreferences settings = getPreferences(0);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Rep = settings.getInt("AlarmRep", 255);
		boolean Selected[];
		Selected = new boolean[7];
		for (int i=0; i<7; i++)
		{
			if ( (Rep & (1<<i)) != 0)
			{
				Selected[i] = true;
			}
			else
			{
				Selected[i] = false;
			}
		}
    	    		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);		
		builder.setTitle("Ange dagar");
		builder.setMultiChoiceItems(items, Selected, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {				
				if (isChecked)
				{
					Rep |= (1<<which);
				}
				else
				{
					Rep &= ~(1<<which);
				}								
			}
		});
									    
		AlertDialog alert = builder.create();
		alert.setButton("Spara", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				//SharedPreferences settings = getPreferences(0);
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("AlarmRep", Rep);
        		editor.commit();
        		SRPlayerAlarm.HandleAlarmStateChange(getApplicationContext());
        		UpdateAlarmView();        		
			}
		});
		alert.show();
	}
	
	private TimePickerDialog.OnTimeSetListener mAlarmTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            	  //Store the selected Hour and Minute
            		//SharedPreferences settings = getPreferences(0);
            		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            		SharedPreferences.Editor editor = settings.edit();
            		editor.putInt("AlarmHour", hourOfDay);
            		editor.putInt("AlarmMinute", minute);               
            		editor.commit();  
            		SRPlayerAlarm.HandleAlarmStateChange(getApplicationContext());
            		UpdateAlarmView();
            		
            		
            }
        };
        
    private void OpenAlarmTimeDialog()
    {
    	//SharedPreferences settings = getPreferences(0);
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	int Hour = settings.getInt("AlarmHour", 6);
    	int Minute = settings.getInt("AlarmMinute", 0);
    	
    	TimePickerDialog SelectAlarmTimeDialog = new TimePickerDialog(this,
				mAlarmTimeSetListener, 
				Hour, 
                Minute, 
                true);
		SelectAlarmTimeDialog.setTitle("Ange tid HH:MM");
		SelectAlarmTimeDialog.show();
    }
    
    private void UpdateAlarmView()
    {
    	//SharedPreferences settings = getPreferences(0);    	
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	int Hour = settings.getInt("AlarmHour", 6);
    	int Minute = settings.getInt("AlarmMinute", 0);
    	boolean AlarmEnable = settings.getBoolean("AlarmEnable", false);
    	String AlarmStationName = settings.getString("AlarmStationName", "P1");
    	Rep = settings.getInt("AlarmRep", 255);
    	
		String RepStr = "";
		Resources res = getResources();
		String[] items = res.getStringArray(R.array.week_days_prefix);
		
		
		for (int i=0; i<7; i++)
		{
			if(((1<<i) & Rep) != 0)
			{
				RepStr += items[i] + " ";
			}
		}
			
    	
    	TextView tv = (TextView) findViewById(R.id.AlarmTime);
		tv.setText(String.format("%02d", Hour) + ":" + String.format("%02d", Minute));
		
		tv = (TextView) findViewById(R.id.AlarmRep);
		tv.setText(RepStr);
		
		tv = (TextView) findViewById(R.id.AlarmChannel);
		tv.setText(AlarmStationName);
				
		CheckBox AlarmEnableCheckBox = (CheckBox) findViewById(R.id.AlarmEnable);
		AlarmEnableCheckBox.setChecked(AlarmEnable);
    }
        
}
