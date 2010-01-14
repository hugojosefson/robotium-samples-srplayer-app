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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlayerService extends Service implements OnPreparedListener, OnSeekCompleteListener,
OnCompletionListener, OnInfoListener, OnErrorListener, OnBufferingUpdateListener {
	private static final long _TIME_MINUTE = 60*1000L;

	private static final int NOTIFY_ID = 1;
	
	// Constants used in the start intent to show what we want to perform.
	public static final int GET_INFO = 0;	
	public static final int TOGGLE_STREAMING_STATUS = 1;
	
	// Constants used in the broadcast intent to send status information
	public static final int STOP = 0;	
	public static final int BUFFER = 1;
	public static final int PLAY = 2;
	public static final int PAUSE = 3;
	
	
	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new PlayerBinder();
	private MediaPlayer player;
	
	private List<PlayerObserver> playerObservers;
	private int playerStatus = STOP;
	private NotificationManager mNM;
	private Notification notification;
	private Station currentStation = new Station("P1", 
			"rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp",
			"http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=true",
			132,0);	
	private Timer rightNowTimer;
	private Timer Sleeptimer;
	private TimerTask rightNowtask;
	private TimerTask sleepTimertask;
	private boolean sleepTimerIsRunning = false;
	private RightNowChannelInfo LastRetreivedInfo;

	
	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class PlayerBinder extends Binder {
		PlayerService getService() {
			return PlayerService.this;
		}
	}

	@Override
	public void onCreate() {
		// super.onCreate();
		Log.i(getClass().getSimpleName(), "PlayService onCreate!");
		if ( this.player == null) {
			this.player = new MediaPlayer();
		}
		this.playerObservers = new Vector<PlayerObserver>();
		this.mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		rightNowTimer = new Timer();
		Sleeptimer = new Timer();
		
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);            
        PhoneStateListener phoneListnerHandler = new PhoneStateHandler(this);
		tm.listen(phoneListnerHandler, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(getClass().getSimpleName(), "PlayService onStart!");
		switch (intent.getFlags())
        {
        case GET_INFO :
        	//Request from widget to get updated information
        	if (LastRetreivedInfo == null)
        		restartRightNowInfo(); //No info retreived. Request to retreive it
        	UpdateDataAndInformReceivers();
        	break;
        case TOGGLE_STREAMING_STATUS :
        	//Request from widget to toggle the current status
        	if ( this.playerStatus == STOP )
        	{
				try {
					Log.i(getClass().getSimpleName(), "Widget request to start");
					startPlay();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	else if (this.playerStatus == PAUSE)
        	{
        		try {
					resumePlay();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
			else
			{
				Log.i(getClass().getSimpleName(), "Widget request to stop");
        		if (this.currentStation.getStreamType() == Station.NORMAL_STREAM)
        			stopPlay();
        		else
        			pausePlay();
			}
        	break;
        }
	}

	@Override
	public void onDestroy() {
		Log.i(getClass().getSimpleName(), "PlayService onDestroy!");
		// Shutdown the player
		this.stopPlay();
		// Cancel the persistent notification.
        mNM.cancel(PlayerService.NOTIFY_ID);
        // Cancel the rightNowTasks
		rightNowTimer.cancel();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void selectChannel(Station station) {
		Log.d(getClass().getSimpleName(), "PlayerService selectChannel");

		if ( !station.equals(this.currentStation) ) {
			Log.d(getClass().getSimpleName(), "PlayerService not equal");
			this.currentStation = station.clone();
			if ( this.playerStatus == PLAY ) {
				stopPlay();
				try {
					startPlay();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
			else if ( this.playerStatus == PAUSE ) {
				//If the channel changes and the stream is paused.
				//Stop so that the stream is reset
				stopPlay();
			}
			updateNotify(this.currentStation.getStationName(), null);
			restartRightNowInfo();
			this.LastRetreivedInfo = null;
			UpdateDataAndInformReceivers(); //Inform widgets
		} 		
	}
	
	public void startPlay() throws IllegalArgumentException, 
		IllegalStateException, IOException  {
		Log.d(getClass().getSimpleName(), "PlayerService startPlay");

		if ( this.playerStatus == STOP ) {
			Log.i(getClass().getSimpleName(), "Media Player start " + this.currentStation.getStreamUrl());	
			updateNotify(this.currentStation.getStationName(), null);
			// Display a notification about us starting.  We put an icon in the status bar.
	        showNotification();
			this.startStream();
			restartRightNowInfo();
		} 
		
	}
	
	public void restartRightNowInfo() {
		if ( this.rightNowtask != null) {
			this.rightNowtask.cancel();
		}
		
		//If the stream is a NORMAL_STREAM, info about
		//the strem can be collected in intervals, if 
		//not, the information is static
		if (this.currentStation.getStreamType() == Station.NORMAL_STREAM)
		{
			this.rightNowtask = new RightNowTask(this);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			String rateStr;
			if ( this.playerStatus == STOP ) {
				rateStr = prefs.getString("rightNowInfoRetrievalPausedRate", "2");
			} else {
				rateStr = prefs.getString("rightNowInfoRetrievalRate", "2");
			}
			int rate = 0;
			try {
				rate = Integer.parseInt(rateStr);
			} catch (NumberFormatException e) {
				rate = 2;
			}
			Log.d(getClass().getSimpleName(), "Starting rightnow task with " + rate);
			this.rightNowTimer.schedule(rightNowtask, 0, rate * _TIME_MINUTE);	
		}
		
	}

	private void updateNotify(String stationName, RightNowChannelInfo info) {
		if ( this.notification != null ) {
			String str = stationName;;
			if ( info != null ) {
				if ( ! info.getProgramTitle().trim().equals("") )
					str += " - " + info.getProgramTitle();
				else if ( ! info.getSong().trim().equals("") )
					str += " - " + info.getSong();
			}
			notification.setLatestEventInfo(this, "SR Player",
	        		str, notification.contentIntent);
	        mNM.notify(PlayerService.NOTIFY_ID, notification);
		}
	}

	synchronized private void startStream() throws IllegalArgumentException, IllegalStateException, IOException  {
		Log.d(getClass().getSimpleName(), "PlayerService startStream!");

		if ( this.player == null ) {
            Log.d(getClass().getSimpleName(), "PlayerService Player is null creating new");
            this.player = new MediaPlayer();
		}
		Log.i(getClass().getSimpleName(), "Start stream " + this.currentStation.getStreamUrl());
		
		this.player.setDataSource(this.currentStation.getStreamUrl());
		this.player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		this.player.setOnCompletionListener(this);
		this.player.setOnErrorListener(this);
		this.player.setOnInfoListener(this);
		this.player.setOnPreparedListener(this);
		this.player.setOnBufferingUpdateListener(this);
		this.player.setOnSeekCompleteListener(this);
		
		this.player.prepareAsync();
		this.playerStatus = BUFFER;
		UpdateDataAndInformReceivers(); //Inform widgets
	}
	
	synchronized public void stopPlay() {
		Log.d(getClass().getSimpleName(), "PlayerService stopPlay");
		this.player.stop();
		this.player.reset();
		this.playerStatus = STOP;
			
		UpdateDataAndInformReceivers(); //Inform widgets
		if ( notification != null ) {
			notification.setLatestEventInfo(this, "SR Player",
	        		"Paused", notification.contentIntent);
			this.mNM.cancel(NOTIFY_ID);
		}
		this.notification = null;
        this.restartRightNowInfo();
        
        if ( this.sleepTimertask != null) {
			this.sleepTimertask.cancel();
			sleepTimerIsRunning = false;
		}
        
    	for(PlayerObserver observer : this.playerObservers) {
        	observer.onPlayerStoped();	// Call observers to let them
										// know that the stream has stopped.
		}
	
	}
	
	synchronized public void pausePlay() {
		Log.d(getClass().getSimpleName(), "PlayerService stopPlay");
		this.player.pause();
		this.playerStatus = PAUSE;
			
		UpdateDataAndInformReceivers(); //Inform widgets
		if ( notification != null ) {
			notification.setLatestEventInfo(this, "SR Player",
	        		"Paused", notification.contentIntent);
			this.mNM.cancel(NOTIFY_ID);
		}
		this.notification = null;
        this.restartRightNowInfo();
        
        if ( this.sleepTimertask != null) {
			this.sleepTimertask.cancel();
			sleepTimerIsRunning = false;
		}
        
    	for(PlayerObserver observer : this.playerObservers) {
        	observer.onPlayerStoped();	// Call observers to let them
										// know that the stream has stopped.
		}	
	}
	
	public void resumePlay() throws IllegalArgumentException, 
		IllegalStateException, IOException  {
		Log.d(getClass().getSimpleName(), "PlayerService startPlay");
	
		if ( this.playerStatus == PAUSE ) {
			Log.i(getClass().getSimpleName(), "Media Player start " + this.currentStation.getStreamUrl());	
			updateNotify(this.currentStation.getStationName(), null);
			// Display a notification about us starting.  We put an icon in the status bar.
	        showNotification();
			this.player.start();			
			this.playerStatus = PLAY;
			UpdateDataAndInformReceivers();
			//restartRightNowInfo();
		
			for(PlayerObserver observer : this.playerObservers) {
	        	observer.onPlayerStarted();	// Call observers to let them
											// know that the stream has stopped.
			}	
		} 
		
	}
	
	public void onPrepared(MediaPlayer mp) {
		Log.d(getClass().getSimpleName(), "PlayerService onPrepared!");
		mp.start();
		this.playerStatus = PLAY; //No longer buffering
		if ( this.playerObservers != null ) {
			UpdateDataAndInformReceivers(); //Inform widgets
			for(PlayerObserver observer : this.playerObservers) {
				observer.onPlayerStarted();
			}
		}
	}

	public void onCompletion(MediaPlayer mp) {
		Log.d(getClass().getSimpleName(), "PlayerService onCompletion!");
		// Since it seems that glitches in the SR stream is treated by
		// Android Media Player as if the the stream is completed we need to
		// restart if this method is triggered unless we pressed stop.
		if ( this.playerStatus == PLAY ) {			
			if (this.currentStation.getStreamType() == Station.NORMAL_STREAM)
			{
			Log.i(getClass().getSimpleName(), "Not stopped restarting !!");
			this.player.stop();
			this.player.reset();
			
				try {
					this.startStream();
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "Error restarting stream!", e);
				}
			}
			else
			{
				//If the stream is a podcast the stream is completed				
				stopPlay();
			}
		} else {

		}
		/*
		if ( this.playerObservers != null ) {			
			for(PlayerObserver observer : this.playerObservers) {
				observer.onPlayerBuffer(-1); // Calling to set buffer icon
			}
		}
		*/
	}

	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		StringBuilder sb = new StringBuilder();
		sb.append("Media Player Info: ");
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
			sb.append("Bad Interleaving");
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			sb.append("Not Seekable");
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			sb.append("Video Track Lagging");
			break;
		case MediaPlayer.MEDIA_INFO_UNKNOWN:
			sb.append("Unkown");
			break;
		}
		sb.append(" (");
		sb.append(what);
		sb.append(") ");
		sb.append(extra);
		Log.i(getClass().getSimpleName(), sb.toString());
		
		return true;
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		StringBuilder sb = new StringBuilder();
		sb.append("Media Player Error: ");
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			sb.append("Not Valid for Progressive Playback");
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			sb.append("Server Died");
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			sb.append("Unkown");
			break;
		default:
			sb.append(" Non standard (");
			sb.append(what);
			sb.append(")");
		}
		sb.append(" (");
		sb.append(what);
		sb.append(") ");
		sb.append(extra);
		Log.e(getClass().getSimpleName(), sb.toString());
		// If we get a Error we will need to restart the stream again.
		if ( this.playerStatus == PLAY ) {
			this.player.stop();
			this.player.reset();
			try {
				this.startStream();
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "Error restarting stream!", e);
			} 
		} else {

		}
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onPlayerBuffer(-1);
			}
		}
		return true;

	}
	
	public void onBufferingUpdate(MediaPlayer mp, int percent) {		
		Log.d(getClass().getSimpleName(), "PlayerService onBufferingUpdate : " + percent + "%");
		if (this.currentStation.getStreamType() == Station.NORMAL_STREAM)
		{
			if ( this.playerObservers != null ) {
				for(PlayerObserver observer : this.playerObservers) {
					observer.onPlayerBuffer(percent);
				}
			}
		}
		else
		{
			//Podcast stream. Since it will always buffer the buffer icon
			//is just shown until prepared
		}
	}
	
	/**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        String str = this.currentStation.getStationName();
        
        // Set the icon, scrolling text and time stamp
        this.notification = new Notification(R.drawable.icon, "Now playing : " + str, 0);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SRPlayer.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "SR Player",
        		str, contentIntent);
		notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(PlayerService.NOTIFY_ID, notification);
    }

	public void addPlayerObserver(PlayerObserver playerObserver) {		
		if ( !this.playerObservers.contains(playerObserver)) {
			this.playerObservers.add(playerObserver);
			if ( this.playerStatus == BUFFER ) {
				// Send indication to the observer that we are buffering
				playerObserver.onPlayerBuffer(-1);
			} else if ( this.playerStatus == STOP ) {
				// Send indication to the observer that we are stopped
				playerObserver.onPlayerStoped();
			} else if ( this.playerStatus == PAUSE ) {
					// Send indication to the observer that we are stopped
				playerObserver.onPlayerStoped();
			} else {
				// Send indication to the observer that we are playing.
				playerObserver.onPlayerStarted();
			}
			// Also send the last RightNowInfo
			playerObserver.onRightNowChannelInfoUpdate(this.LastRetreivedInfo);
		}
	}

	public void removePlayerObserver(PlayerObserver playerObserver) {
		this.playerObservers.remove(playerObserver);
	}
	/**
	 * @return the currentStation
	 */
	public Station getCurrentStation() {
		return currentStation;
	}
	
	private void UpdateDataAndInformReceivers() {
		Log.d(getClass().getSimpleName(), "PlayerService UpdateDataAndInformReceivers");

		Intent updateIntent=new Intent("sr.playerservice.UPDATE");
		
		//Fill in the Intent with the data
		//Insert the current index
		//updateIntent.putExtra("sr.playerservice.CHANNEL_INDEX", currentStation.);
		
		//Insert the current channel name		
		if (currentStation != null)
		{
		updateIntent.putExtra("sr.playerservice.CHANNEL_NAME", currentStation.getStationName());
		}		
		
		if (LastRetreivedInfo != null)
		{		
			// Insert RightNow Info
			updateIntent.putExtra("sr.playerservice.RIGHT_NOW_INFO", LastRetreivedInfo);
		} else {
			// Insert RightNow Info
			updateIntent.putExtra("sr.playerservice.RIGHT_NOW_INFO", "No Info");
		}
		
		//Insert the status
		updateIntent.putExtra("sr.playerservice.PLAYER_STATUS", this.playerStatus);
					
		//Send a broadcast that the service has new data
    	PendingIntent pendingupdateIntent=PendingIntent.getBroadcast(getBaseContext(), 0, 
    									updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	try {
			pendingupdateIntent.send();
		} catch (CanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void rightNowUpdate(RightNowChannelInfo info) {
		if ( info == null ) {
			return;
		}
		LastRetreivedInfo = info; // Save the last retreived info
		UpdateDataAndInformReceivers();
		updateNotify(this.currentStation.getStationName(), info);
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer: this.playerObservers) {
				observer.onRightNowChannelInfoUpdate(info);
			}
		}
	}

	/**
	 * @return the playerStatus
	 */
	public int getPlayerStatus() {
		return playerStatus;
	}
	
	public int getStationIndex() {
		Station station = getCurrentStation();
		CharSequence[] channelInfo = (CharSequence[]) getResources().getTextArray(R.array.channels);
		int channelPos = 0;
		for(CharSequence cs : channelInfo) {
			if ( cs.toString().equals(station.getStationName()) ) {
				break;
			}
			channelPos++;
		}
		return channelPos;
	}
			
	public void StartSleeptimer(int Delay) {
		sleepTimerIsRunning = true;
		
		if ( this.sleepTimertask != null) {
			this.sleepTimertask.cancel();
		}
		sleepTimertask = new sleepTimertask(this);
		Sleeptimer.schedule(sleepTimertask, Delay * _TIME_MINUTE); //Delay is in minutes		
	}
	
	public void StopSleeptimer() {
		sleepTimertask.cancel();
		sleepTimerIsRunning = false;
	}
	
	public boolean SleeptimerIsRunning() {
		return sleepTimerIsRunning;
	}
	
	public int GetPosition() {
		if (this.playerStatus != STOP)
			return this.player.getCurrentPosition();
		else
			return -1;
	}
	
	public int GetDuration() {
		if (this.playerStatus != STOP)
			return this.player.getDuration();
		else
			return -1;
	}
	
	public void SetPosition(int Seconds) {
		int NewPos = Seconds*1000;		
		this.player.seekTo(NewPos);
		Log.d(getClass().getSimpleName(), "New position in msec: " + String.valueOf(NewPos));
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.d(getClass().getSimpleName(), "Seek complete");
	}
}

