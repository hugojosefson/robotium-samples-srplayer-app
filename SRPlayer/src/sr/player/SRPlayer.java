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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SRPlayer extends Activity implements PlayerObserver,
		OnItemSelectedListener {
	
	private static final String _SR_RIGHTNOWINFO_URL = 
		"http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=false";
	private static Station currentStation = new Station("P1", 
			"rtsp://lyssna-mp4.sr.se/live/mobile/SR-P1.sdp",
			"http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=true",
			132);
	public static final String TAG = "SRPlayer";
	
	private static final int MENU_EXIT = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_CONFIG = 2;
	
	protected static final int MSGUPDATECHANNELINFO = 0;
	
	private static final int PLAY_STATE_STOPPED = 0;
	private static final int PLAY_STATE_BUFFER = 1;
	private static final int PLAY_STATE_PLAY = 2;
	
	private ImageButton startStopButton;
	private int playState = PLAY_STATE_STOPPED;
	
	public PlayerService boundService;
	
	private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	boundService = ((PlayerService.PlayerBinder)service).getService();
        	boundService.addPlayerObserver(SRPlayer.this);
        }

        public void onServiceDisconnected(ComponentName className) {
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
		if ( savedInstanceState != null ) {
			this.playState = savedInstanceState.getInt("playState");
			Log.d(TAG, "playstate restored to " + this.playState);
		} else {
			this.playState = PLAY_STATE_STOPPED;
		}
		startService();
		setContentView(R.layout.main);

		startStopButton = (ImageButton) findViewById(R.id.BtnStartStop);
		Spinner spin = (Spinner) findViewById(R.id.channelSelect);
		spin.setOnItemSelectedListener(this);

		startStopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (SRPlayer.this.playState == PLAY_STATE_STOPPED) {
						setBufferText();
						startStopButton.setImageResource(R.drawable.loading);
						startPlaying();
					} else {
						stopPlaying();
						startStopButton.setImageResource(R.drawable.play);
						clearAllText();
					}
				} catch (IllegalStateException e) {
					Log.e(SRPlayer.TAG, "Could not " +(SRPlayer.this.playState == PLAY_STATE_STOPPED?"start":"stop") +" to stream play.", e);
				} catch (IOException e) {
					Log.e(SRPlayer.TAG, "Could not " +(SRPlayer.this.playState == PLAY_STATE_STOPPED?"start":"stop") +" to stream play.", e);
				}
			}
		});

		if (this.playState == PLAY_STATE_BUFFER) {
			startStopButton.setImageResource(R.drawable.loading);
		} if (this.playState == PLAY_STATE_STOPPED) {
			startStopButton.setImageResource(R.drawable.play);
		} else {
			startStopButton.setImageResource(R.drawable.stop);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	  savedInstanceState.putInt("playState", this.playState);
	  super.onSaveInstanceState(savedInstanceState);
	}

	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		this.boundService.removePlayerObserver(this);
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
					boundService.startPlay(SRPlayer.currentStation);
					startStopButton.setImageResource(R.drawable.loading);
					setBufferText();
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
				this.playState = PLAY_STATE_BUFFER;
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
	
		return true;
	}
	
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
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void handleMenuAbout() {
		new AlertDialog.Builder(this)
			.setTitle(getResources().getText(R.string.about_title))
			.setMessage(R.string.about_message)
			.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Do nothing...
						}
					}).show();
	}

	private void handleMenuExit() {
		stopService(new Intent(SRPlayer.this,
                    PlayerService.class));
		this.finish();
	}
	
	private void clearAllText() {
		TextView tv = (TextView) findViewById(R.id.TextProgramTitle);
		tv.setText("");
		tv = (TextView) findViewById(R.id.TextProgramInfo);
		tv.setText("");
	}
	
	private void setBufferText() {
		clearAllText();
		TextView tv = (TextView) findViewById(R.id.TextProgramTitle);
		tv.setText("Buffrar...");
	}

	private void stopPlaying() {
		Log.d(TAG, "stopPlaying");
		Log.i(SRPlayer.TAG, "Media Player stop!");
		this.boundService.stopPlay();
		clearAllText();
		this.playState = PLAY_STATE_STOPPED;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		startStopButton.setImageResource(R.drawable.stop);
		this.playState = PLAY_STATE_PLAY;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		startStopButton.setImageResource(R.drawable.loading);
		setBufferText();
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		startStopButton.setImageResource(R.drawable.loading);
		setBufferText();
		return true;

	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int pos,
			long id) {
		Log.d(TAG, "onItemSelected");
		Resources res = getResources();
		CharSequence[] channelInfo = (CharSequence[]) res
				.getTextArray(R.array.channels);
		CharSequence[] urls = (CharSequence[]) res.getTextArray(R.array.urls);
		// Only restart the channel if it is a new channel that is selected.
		if ( !SRPlayer.currentStation.getStationName().equals(channelInfo[pos].toString()) ) {
			SRPlayer.currentStation.setStreamUrl(urls[pos].toString());
			SRPlayer.currentStation.setStationName(channelInfo[pos].toString());
			SRPlayer.currentStation.setChannelId(res.getIntArray(R.array.channelid)[pos]);
			SRPlayer.currentStation.setRightNowUrl(_SR_RIGHTNOWINFO_URL);
			if ( this.playState != PLAY_STATE_STOPPED) {
				this.stopPlaying();
				try {
					this.startPlaying();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		// If nothing is selected reset to P1
		Resources res = getResources();
		CharSequence[] channelInfo = (CharSequence[]) res
				.getTextArray(R.array.channels);
		CharSequence[] urls = (CharSequence[]) res.getTextArray(R.array.urls);
		SRPlayer.currentStation.setStreamUrl(urls[0].toString());
		SRPlayer.currentStation.setStationName(channelInfo[3].toString());
	}

	Handler viewUpdateHandler = new Handler(){
        // @Override
        public void handleMessage(Message msg) {
             switch (msg.what) {
                  case SRPlayer.MSGUPDATECHANNELINFO:
                	  	RightNowChannelInfo info = (RightNowChannelInfo) msg.getData().getSerializable("data");
	                	TextView tv = (TextView) findViewById(R.id.TextProgramTitle);
	              		try {
	              			tv.setText(info.getProgramTitle());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting info", e);
	              		}
	              		tv = (TextView) findViewById(R.id.TextProgramInfo);
	              		try {
	              			tv.setText(info.getProgramInfo());
	              		} catch (Exception e) {
	              			Log.e(SRPlayer.TAG, "Problem setting info", e);
	              		}
                       break;
             }
             super.handleMessage(msg);
        }
   };

   
	@Override
	public void onRightNowChannelInfoUpdate(RightNowChannelInfo info) {
		Message m = new Message();
        m.what = SRPlayer.MSGUPDATECHANNELINFO;
        m.getData().putSerializable("data", info);
        SRPlayer.this.viewUpdateHandler.sendMessage(m); 
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		
	}
}
