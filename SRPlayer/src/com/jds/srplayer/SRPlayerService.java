package com.jds.srplayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jds.srplayer.R;

import android.app.PendingIntent;
import android.app.Service;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

public class SRPlayerService extends Service implements OnPreparedListener, OnCompletionListener, OnInfoListener, OnErrorListener{
		private Timer timer = new Timer();
		
		public static final int INIT_SERVICE = 0;
		public static final int START_STREAMING = 1;
		public static final int STOP_STREAMING = 2;
		public static final int UPDATE_CONFIG = 3;
		
		
		private static MediaPlayer player;
		private static boolean isstoped = true;
		private static boolean ispaused = false;
		private static boolean WaitingForEndOfCall = false;
		private static String playUrl = "rtsp://lyssna-mp4.sr.se/live/mobile/SR-P3.sdp";
		private static int ChannelIndex = 3;
		private static String ChannelName;		
		public static final int STOP = 0;
		public static final int BUFFER = 1;
		public static final int PLAY = 2;
		
		private static int ServerStatus=STOP;
		
		//Collected data
		private static String CurrentProgramTitle = "";
		private static String NextProgramTitle = "";
		private static String ProgramInfo = "";
		private static String NextProgramInfo = "";
		private static String CurrentSong = "";
		private static String NextSong = "";
		
		private void UpdateDataAndInformReceivers() {
			Intent updateIntent=new Intent("com.jds.srplayerservice.UPDATE");
			
			//Fill in the Intent with the data
			//Insert the current index
			updateIntent.putExtra("com.jds.srplayerservice.CHANNEL_INDEX", ChannelIndex);
			
			//Insert the current channel name
			Resources res = getResources();
			ChannelName = res.getTextArray(R.array.channels)[ChannelIndex].toString();
			playUrl = res.getTextArray(R.array.urls)[ChannelIndex].toString();
			updateIntent.putExtra("com.jds.srplayerservice.CHANNEL_NAME", ChannelName);
			
			//Insert the current program name 
			updateIntent.putExtra("com.jds.srplayerservice.CURRENT_PROGRAM_NAME", CurrentProgramTitle);
			
			//Insert the next program name
			updateIntent.putExtra("com.jds.srplayerservice.NEXT_PROGRAM_NAME", NextProgramTitle);
			
			//Insert the current program info 
			updateIntent.putExtra("com.jds.srplayerservice.CURRENT_PROGRAM_INFO", ProgramInfo);
			
			//Insert the next program info
			updateIntent.putExtra("com.jds.srplayerservice.NEXT_PROGRAM_INFO", NextProgramInfo);
						
			//Insert the status
			updateIntent.putExtra("com.jds.srplayerservice.PLAYER_STATUS", ServerStatus);
			
			//Insert the next program info
			updateIntent.putExtra("com.jds.srplayerservice.CURRENT_SONG", CurrentSong);
						
			//Insert the status
			updateIntent.putExtra("com.jds.srplayerservice.NEXT_SONG", NextSong);
						
			//Send a broadcast that the service has new data
	    	PendingIntent pendingupdateIntent=PendingIntent.getBroadcast(getBaseContext(), 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    	try {
				pendingupdateIntent.send();
			} catch (CanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		private void ClearChannelData() 
		{			
			CurrentProgramTitle = "-";
			NextProgramTitle = "-";
			ProgramInfo = "";
			NextProgramInfo = "";
			CurrentSong = "";
			NextSong = "";
		}
						
		private void updateJustNuInfo() throws ParserConfigurationException, SAXException
		{			
			//RemoteViews ViewToUpdate = updateWidget(true,false);
			UpdateDataAndInformReceivers();
			boolean ResponseFromServer = false;
			
			Resources res = getResources();
			String ChannelID = res.getTextArray(R.array.channelID)[ChannelIndex].toString(); 
			Log.d(getClass().getSimpleName(), "Trying to get channelinfo. Id = " + ChannelID);
			
			try {
				URL url = new URL(getString(R.string.just_nu_feed));
				
				URLConnection connection = url.openConnection();
				 
				HttpURLConnection httpConnection = (HttpURLConnection)connection;
				
				int responseCode = httpConnection.getResponseCode();
				
				
				if (responseCode == HttpURLConnection.HTTP_OK)
				{
					ResponseFromServer = true;
					
					Log.d(getClass().getSimpleName(), "OK Response from server");
					InputStream in = httpConnection.getInputStream();
					
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					
					DocumentBuilder db = dbf.newDocumentBuilder();
					
					Document dom = db.parse(in);
					
					org.w3c.dom.Element docEle = dom.getDocumentElement();
					
					NodeList nl = docEle.getElementsByTagName("Channel");
					
					Log.d(getClass().getSimpleName(), "Nodelist retreived - Size: " + Integer.toString(nl.getLength()));
				
					
						
					for (int i=0; i < nl.getLength(); i++)
					{
						//Search for channelname
						org.w3c.dom.Element entry = (org.w3c.dom.Element)nl.item(i);
						String CHName = entry.getAttributeNode("Name").getNodeValue();
						String CHID = entry.getAttributeNode("Id").getNodeValue();
						//if (CHName.contentEquals(ChannelName))
						if (CHID.contentEquals(ChannelID))
						{							
							Log.d(getClass().getSimpleName(), "Found channelinfo");
							
							String ProgTitle = entry.getElementsByTagName("ProgramTitle").item(0).getFirstChild().getNodeValue();
							
							//ViewToUpdate = updateWidget(true,false);
							
							if (ProgTitle != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.CurrentProgName, ProgTitle);
								CurrentProgramTitle = ProgTitle;
							}
							
							String NextProgTitle = entry.getElementsByTagName("NextProgramTitle").item(0).getFirstChild().getNodeValue();
							
							if (NextProgTitle != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.NextProgName, NextProgTitle);
								NextProgramTitle = NextProgTitle;
							}
							
							String ProgInfo = entry.getElementsByTagName("ProgramInfo").item(0).getFirstChild().getNodeValue();
							
							if (ProgInfo != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.NextProgName, NextProgTitle);
								ProgramInfo = ProgInfo;
							}
							
							String NextProgInfo = entry.getElementsByTagName("NextProgramDescription").item(0).getFirstChild().getNodeValue();
							
							if (NextProgInfo != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.NextProgName, NextProgTitle);
								NextProgramInfo = NextProgInfo;
							}
							
							String CurrSong = entry.getElementsByTagName("Song").item(0).getFirstChild().getNodeValue();
							
							if (CurrSong != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.NextProgName, NextProgTitle);
								CurrentSong = CurrSong;
							}
							
							String NSong = entry.getElementsByTagName("NextSong").item(0).getFirstChild().getNodeValue();
							
							if (NSong != null)
							{
								//Log.d(getClass().getSimpleName(), ProgTitle);
								//ViewToUpdate.setTextViewText(R.id.NextProgName, NextProgTitle);
								NextSong = NSong;
							}
																				
							break;
						}
					}
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				Log.d(getClass().getSimpleName(), "Malformed URL Exception");
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d(getClass().getSimpleName(), "IO Exception");				
				e.printStackTrace();
			}
			finally
		    {
				
		    }
			
			if (!ResponseFromServer)
				Log.e(getClass().getSimpleName(), "No response from server");			
			
			//updateWidget(false,false);
			Log.d(getClass().getSimpleName(), "Sending update data");			
			UpdateDataAndInformReceivers();
		}
		
		
		private void startPlaying() throws IllegalArgumentException, IllegalStateException, IOException  {
	    	if ( player == null ) {
	    		Log.d(getClass().getSimpleName(), "Player is null");
	    		player = new MediaPlayer();
	    	}
	    	if (ispaused)
	    	{
	    		player.start();
	    		ispaused = false;
	    	}
	    	else
	    	{
	    		player.setDataSource(playUrl);
	    		player.setOnCompletionListener(this);
	    		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
	    	
	    		player.setOnErrorListener(this);
	    		player.setOnInfoListener(this);
	    		player.setOnPreparedListener(this);
	    		player.prepareAsync();
	    		isstoped = false;
	    		ispaused = false;
	    	}
	    	
	    		    	
		}
		
		private void stopPlaying() {
	    	Log.d(getClass().getSimpleName(), "Media Player stop!");			
	    	player.stop();
	    	player.reset();			
	    	isstoped = true;
	    }
		
		private void pausePlaying() {
	    	Log.d(getClass().getSimpleName(), "Media Player pause!");			
	    	player.pause();
	    	//isstoped = true;
	    	ispaused = true;
	    }
		
		
		
		private PhoneStateListener mPhoneListener = new PhoneStateListener()
		{
		        public void onCallStateChanged(int state, String incomingNumber)
		        {
		                switch (state)
						{
						case TelephonyManager.CALL_STATE_OFFHOOK:
							Log.d(getClass().getSimpleName(), "Offhook state detected");
						    break;
						case TelephonyManager.CALL_STATE_RINGING:		
							Log.d(getClass().getSimpleName(), "Ringing detected");
							if (ServerStatus > STOP)
							{
								WaitingForEndOfCall = true;
								stopPlaying();
								//pausePlaying();
								
								ServerStatus = STOP;
								//updateWidget(false, false);
								UpdateDataAndInformReceivers();
								//updateWidget(false);
							}
						    break;
						case TelephonyManager.CALL_STATE_IDLE:		
							Log.d(getClass().getSimpleName(), "Idle state detected");
							if (WaitingForEndOfCall)
							{
								try {
									startPlaying();
									ServerStatus = BUFFER;
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
								
								
								//updateWidget(false, false);
								UpdateDataAndInformReceivers();
								//updateWidget(false);
								WaitingForEndOfCall = false;
							}
						    break;
						default:
						    Log.d(getClass().getSimpleName(), "Unknown phone state=" + state);
						}
		        }
		};
		
		
	    @Override
	    public void onStart(Intent intent, int startId) {
	        TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);	        
	        tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	        
	        	        
	    	switch (intent.getFlags())
	        {
	    		
	        case START_STREAMING :
	        	Log.d(getClass().getSimpleName(), "Start streaming request");
	        	if (ServerStatus == STOP)
	        	{
	        		//updateViews.setImageViewResource(R.id.PlayPause, R.drawable.pause_white);
	    	        //manager.updateAppWidget(thisWidget, updateViews);
	        		
	        		        	
	        		//updateWidget(false);
	        		
	        		try {
	        			ServerStatus = BUFFER;
	        			//updateWidget(false, false);
	        			UpdateDataAndInformReceivers();
						startPlaying();
						
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
	        		//updateViews.setImageViewResource(R.id.PlayPause, R.drawable.play_white);
	        		ServerStatus = STOP;
	        		//updateWidget(false, false);
	        		UpdateDataAndInformReceivers();
	        		//updateWidget(false);
	        		
	        		stopPlaying();
	        		//pausePlaying();
	        	}
	        	
	        	break;
	        case UPDATE_CONFIG :
	    		int NewChannelIndex = intent.getIntExtra("com.jds.srplayerservice.CHANNEL_INDEX", 0);
	    		
	    		if (NewChannelIndex != ChannelIndex)
	    		{	
	    			Log.d(getClass().getSimpleName(), "Change of channel");	
	    			ChannelIndex = NewChannelIndex;
	    			ClearChannelData();
	    				    			
	        		if (ServerStatus > STOP)
	        		{	        		        		
		        		stopPlaying();
		        		ServerStatus = STOP;
		        		try {
		        			ServerStatus = BUFFER;
							//updateWidget(false, true);
							UpdateDataAndInformReceivers();
		        			startPlaying();							
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
	        			//updateWidget(false, true);
	        			UpdateDataAndInformReceivers();
	        		}
	        		
	        		
	    			
	    		}
	    		else
	    		{	
	    		break;
	    		}
	    		
	    		
	        default :
	        	
	        	//Due to possible long executing time 
	        	//to retreived XML file the update
	        	//of the extra info is handled my a timerthread
	        	//Current just executed once after 100ms 
	        	timer.schedule(new TimerTask() {
    		        public void run() {
    		        	try {
							updateJustNuInfo();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    		        }
    		      }, 100);	        		        		        
	        	
	        	break;
	        }

	    	Log.d( getClass().getSimpleName(), "Service started. Flags = " + String.valueOf(intent.getFlags()));
	    }
	    
	    
	
	    @Override
	    public IBinder onBind(Intent intent) {
	        return null;
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
			sb.append(" ");
			sb.append(extra);
			Log.i(getClass().getSimpleName(),  sb.toString());
			
			return true;
		}
	    
	    @Override
		public void onPrepared(MediaPlayer mp) {
			Log.d(getClass().getSimpleName(), "Media Player prepared!");
			mp.start();
			ServerStatus = PLAY;
			//updateWidget(false,false);
			UpdateDataAndInformReceivers();
		}
		
		@Override
		public void onCompletion(MediaPlayer mp) {
			Log.i(getClass().getSimpleName(), "Media Player completed");
			// Since it seems that glitches in the SR stream is treated by 
			// Android Media Player as if the the stream is completed we need to 
			// restart if this method is triggered unless we pressed stop.
			if ( !isstoped ) {
				Log.i(getClass().getSimpleName(), "Not stoped restarting !!");
				
				player.stop();
				player.reset();
				try {
					this.startPlaying();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
			sb.append(" ");
			sb.append(extra);
			Log.e(getClass().getSimpleName(),  sb.toString());
			// If we get a Error we will need to restart the stream again.
			if ( !isstoped ) {
				player.stop();
				player.reset();
				try {
					this.startPlaying();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return true;
		
		}
	}