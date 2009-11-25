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
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlayerService extends Service implements OnPreparedListener,
OnCompletionListener, OnInfoListener, OnErrorListener, OnBufferingUpdateListener {
	private static final long _TIME_MINUTE = 60*1000L;

	private static final int NOTIFY_ID = 1;
	
	// Constants used in the start intent to show what we want to perform.
	private static final int START_STREAMING = 0;
	private static final int STOP_STREAMING = 1;
	private static final int CHANGE_CHANNEL = 2;
	
	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new PlayerBinder();
	private MediaPlayer player;
	
	private List<PlayerObserver> playerObservers;
	private boolean isstopped;
	private NotificationManager mNM;
	private Notification notification;
	private Station currentStation;
	private Timer rightNowTimer;
	private TimerTask rightNowtask;

	
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
		Log.i(SRPlayer.TAG, "PlayService onCreate!");
		if ( this.player == null) {
			this.player = new MediaPlayer();
		}
		this.playerObservers = new Vector<PlayerObserver>();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
		rightNowTimer = new Timer();
		
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);            
        PhoneStateListener phoneListnerHandler = new PhoneStateHandler(this);
		tm.listen(phoneListnerHandler, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(SRPlayer.TAG, "PlayService onStart!");
		switch (intent.getFlags())
        {
        case START_STREAMING :
        	// Start the streaming (this is called from the widget
        	break;
        case STOP_STREAMING :
        	// Start the streaming (this is called from the widget
        	break;
        case CHANGE_CHANNEL:
        	// Change the channel
        	break;
        }
	}

	@Override
	public void onDestroy() {
		Log.i(SRPlayer.TAG, "PlayService onDestroy!");
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

	public void startPlay(Station station) throws IllegalArgumentException, 
		IllegalStateException, IOException  {
		Log.d(SRPlayer.TAG, "PlayerService startPlay");

		if ( !station.equals(this.currentStation) || this.isstopped ) {
			Log.i(SRPlayer.TAG, "Media Player start " + station.getStreamUrl());
			this.currentStation = station;

			updateNotify(station.getStationName());
			restartRightNowInfo();	
			this.startStream();
		} 
		
	}
	
	private void restartRightNowInfo() {
		if ( this.rightNowtask != null) {
			this.rightNowtask.cancel();
		}
		this.rightNowtask = new RightNowTask(this);
		this.rightNowTimer.schedule(rightNowtask, 0, 2 * _TIME_MINUTE);	
	}

	private void updateNotify(String stationName) {
		notification.setLatestEventInfo(this, "SR Player",
        		"You have tuned in " + stationName, notification.contentIntent);
        mNM.notify(PlayerService.NOTIFY_ID, notification);
	}

	synchronized private void startStream() throws IllegalArgumentException, IllegalStateException, IOException  {
		Log.d(SRPlayer.TAG, "PlayerService startStream!");

		if ( this.player == null ) {
            Log.d(SRPlayer.TAG, "PlayerService Player is null creating new");
            this.player = new MediaPlayer();
		}
		Log.i(SRPlayer.TAG, "Start stream " + this.currentStation.getStreamUrl());
		
		this.player.setDataSource(this.currentStation.getStreamUrl());
		this.player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		this.player.setOnCompletionListener(this);
		this.player.setOnErrorListener(this);
		this.player.setOnInfoListener(this);
		this.player.setOnPreparedListener(this);
		this.player.setOnBufferingUpdateListener(this);
		
		this.player.prepareAsync();
		this.isstopped = false;
	}
	
	synchronized public void stopPlay() {
		Log.d(SRPlayer.TAG, "PlayerService stopPlay");
		this.player.stop();
		this.player.reset();
		this.isstopped = true;
		notification.setLatestEventInfo(this, "SR Player",
        		"Paused", notification.contentIntent);
        mNM.notify(PlayerService.NOTIFY_ID, notification);
        if ( this.rightNowtask != null) {
			this.rightNowtask.cancel();
		}
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(SRPlayer.TAG, "PlayerService onPrepared!");
		mp.start();
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onPrepared(mp);
			}
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d(SRPlayer.TAG, "PlayerService onCompletion!");
		// Since it seems that glitches in the SR stream is treated by
		// Android Media Player as if the the stream is completed we need to
		// restart if this method is triggered unless we pressed stop.
		if (!this.isstopped) {
			Log.i(SRPlayer.TAG, "Not stopped restarting !!");

			this.player.stop();
			this.player.reset();
			try {
				this.startStream();
			} catch (IOException e) {
				Log.e(SRPlayer.TAG, "Error restarting stream!", e);
			}
		} else {

		}
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onCompletion(mp);
			}
		}
	}

	@Override
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
		Log.i(SRPlayer.TAG, sb.toString());
		
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onInfo(mp, what, extra);
			}
		}
		return true;
	}

	@Override
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
		Log.e(SRPlayer.TAG, sb.toString());
		// If we get a Error we will need to restart the stream again.
		if (!this.isstopped) {
			this.player.stop();
			this.player.reset();
			try {
				this.startStream();
			} catch (IOException e) {
				Log.e(SRPlayer.TAG, "Error restarting stream!", e);
			} 
		} else {

		}
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onError(mp, what, extra);
			}
		}
		return true;

	}
	
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.d(SRPlayer.TAG, "PlayerService onBufferingUpdate : " + percent + "%");
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer : this.playerObservers) {
				observer.onBufferingUpdate(mp, percent);
			}
		}
	}
	
	/**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        
        // Set the icon, scrolling text and time stamp
        this.notification = new Notification(R.drawable.icon, "SR Player Started",
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SRPlayer.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "SR Player",
        		"Only static ... no station tuned in", contentIntent);
		notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(PlayerService.NOTIFY_ID, notification);
    }

	public void addPlayerObserver(PlayerObserver playerObserver) {
		if ( !this.playerObservers.contains(playerObserver)) {
			this.playerObservers.add(playerObserver);
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

	public void rightNowUpdate(RightNowChannelInfo info) {
		if ( this.playerObservers != null ) {
			for(PlayerObserver observer: this.playerObservers) {
				observer.onRightNowChannelInfoUpdate(info);
			}
		}
	}
}

