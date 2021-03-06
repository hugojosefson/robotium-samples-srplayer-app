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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class DownloadPodcastService extends Service {
			
	private Timer timer = new Timer();
	private boolean AlreadyRunning;
	private boolean Abort;
	private boolean Delete;
	private boolean Pause;
	private boolean Resume;
	private SRPlayerDBAdapter SRPlayerDB; 
	private Cursor PodList;
	
	String address,guid;		
	String filename;
	File root;		
	int id;
	int bytesDownloaded;
	File subdirectory;		
	String outfile;
	byte[] buffer;
	
	BufferedOutputStream out;
    HttpURLConnection conn;
    InputStream in;
    URL url;
    long rowId;
    
    public static final int PAUSE = 1;
    public static final int RESUME = 2;
    public static final int ABORT_CURRENT = 3;
			
	@Override
	public void onStart(Intent intent, int startId) {
		Delete = false;
		Abort = false;
		Pause = false;
		Resume = false;
		int CurrAction = intent.getIntExtra("Action", 0);
		switch (CurrAction)
		{
		case PAUSE:
			//TODO Set the status of the current downloading item to pauses
			//and do an abort
			Pause = true;
			Abort = true;
			break;
		case RESUME:
			//TODO Remove the paused status of the current item
			//and start downloading again
			Resume = true;
			break;
		case ABORT_CURRENT:
			//TODO Set the flag that the current item shall
			//be not only aborted but also deleted from the DB and SD card
			Abort = true;
			Delete = true;
			break;		
		default:
			 
		}
				
		if (AlreadyRunning == false)
		{
		//Due to long executiontimes the
		//service is spawn to a new thread
		timer.schedule(new TimerTask() {
        public void run() {DownloadNewPodcasts();}
      }, 0);
		};
	}	
				
	@Override
	public void onDestroy() {
		Abort = true;
		AlreadyRunning = false;
		super.onDestroy();
	}

	
	private void DownloadNewPodcasts()
	{
		AlreadyRunning = true;		
		//Get a list of all the podcasts that shall be downloaded
		SRPlayerDB = new SRPlayerDBAdapter(this);
		SRPlayerDB.open();
		boolean failure;
		
		PodList = SRPlayerDB.fetchPodcastsToDownload();
		
		for(;;)
		{			
			if (PodList.getCount() == 0)
				break;
			
			if (PodList.moveToFirst())
		    {				
				//Log.d(SRPlayer.TAG, "Found new podcast to download");
				buffer = new byte[8096];
		    	do {
		    		failure = false;
		    			    		
		    		guid = PodList.getString(SRPlayerDBAdapter.INDEX_GUID); //The GUID contains the unique ID and path to the file
		    		
		    		address = PodList.getString(SRPlayerDBAdapter.INDEX_LINK); 
		    		//bytesDownloaded = PodList.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED); //The GUID contains the unique ID and path to the file
		    		rowId = PodList.getLong(SRPlayerDBAdapter.INDEX_ROWID);
		    		if (address == null)
		    		{
		    			//Remove from the database since invalid entry
		    			SRPlayerDB.deleteFavorite(rowId);
		    			continue;
		    		}
		    		
		    		id = PodList.getInt(SRPlayerDBAdapter.INDEX_ID);
		    		if ((id == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED) && (!Resume))
		    		{
		    			//Download is paused.
		    			Abort = true;
		    		}
		    			
		    		
		    		//Extract the filename
		    		File file = new File(guid);		    		
		            String filename = file.getName();
		    			    		 	    		
		    		root = Environment.getExternalStorageDirectory();					
		    		subdirectory = new File(root, "SRPlayer");
		    		if (subdirectory.exists() == false)
		    		{
		    			Log.d(SRPlayer.TAG, "Directory does not exist. Creating it");
		    			subdirectory.mkdir();
		    		}		
		    		outfile = subdirectory.getAbsolutePath() + "/" + filename;
		    		File newfile = new File(outfile);
		    		bytesDownloaded = (int)newfile.length();
		    		
		    		if (Abort)
					{
		    			if (Delete)
		    			{
		    				Log.d(SRPlayer.TAG, "Deleting and aborting the current download");
		    				newfile.delete();
		    				SRPlayerDB.deleteFavorite(rowId);
		    				Abort = false; //Continue to the next one
		    			}
		    			
		    			if (Pause)
		    			{
		    				SRPlayerDB.podcastSetAsPaused(rowId);
		    			}
		    				
						break;
					}
		    		
		    		out = null;
		            conn = null;
		            in = null;
		            try {   
		            	Log.d(SRPlayer.TAG, "Starting download of " + address);
		                url = new URL(address);		                          
		                //conn = url.openConnection();
		                int Size = 0;		                
		                boolean ShallAppend = false;		                
		                
		                conn = 
		                	(HttpURLConnection)url.openConnection
		                	();
		                if (bytesDownloaded > 0) 
		                {
		                	conn.addRequestProperty("Range", "bytes=" + 
		                			bytesDownloaded +
		                	"-");
		                }
		                else
		                {
		                	ShallAppend = false;		                
		                }
		                
		                conn.connect();
		                int responseCode = conn.getResponseCode();
		                switch (responseCode) 
		                {
		                case HttpURLConnection.HTTP_OK:
		                	bytesDownloaded = 0;
		                	Size = conn.getContentLength();
		                	ShallAppend = false;
		                	Log.d(SRPlayer.TAG, "Restarting from the beginning");
		                	break;
		                case HttpURLConnection.HTTP_PARTIAL:
		                	// The server supports resume
		                	Size = conn.getContentLength() + 
		                	bytesDownloaded;
		                	ShallAppend = true;
		                	Log.d(SRPlayer.TAG, "Appending file");
		                	//Looper.prepare();
							//Toast.makeText(getApplicationContext(), "Appending to " + bytesDownloaded + " bytes", Toast.LENGTH_LONG).show();	 Looper.loop();
		                	break;		   
		                }
		                
		                //Size = conn.getContentLength();
		                
		                in = conn.getInputStream();
		                // Open an output stream that is a file on the SD
		                out = new BufferedOutputStream(new FileOutputStream(outfile,ShallAppend)); 
		                		              		                
		                SRPlayerDB.SetFileSize(rowId, Size);	                			                
		                SRPlayerDB.podcastSetAsCurrentDownloading(rowId);		                
		                
		                // Get the data	                
		                int numRead,totalBytesRead,diffBytesRead;
		                totalBytesRead = bytesDownloaded;
		                diffBytesRead = 0;
		                while ((numRead = in.read(buffer)) != -1)
		                {
		                    out.write(buffer, 0, numRead);
		                    totalBytesRead += numRead;
		                    if ((totalBytesRead - diffBytesRead) > 100000)
		                    {
		                    	diffBytesRead = totalBytesRead;
		                    	SRPlayerDB.SetBytesDownloaded(rowId, totalBytesRead);		                    	
		                    }
		                    if (Abort)
		                    	break;
		                }            		                
		                SRPlayerDB.SetBytesDownloaded(rowId, totalBytesRead);
		                
		                //The entire file has been written
		                if (!Abort)
		                {
		                	Log.d(SRPlayer.TAG, "Download of podcast complete");		                		              
		                	SRPlayerDB.podcastDownloadCompleteUpdate(rowId, outfile);		                	 		                
		                }
		                else
		                {
		                	Log.d(SRPlayer.TAG, "Download of podcast aborted");
		                }
		            		            
		            } catch (MalformedURLException e) {								            			            
		            	failure = true;		            	
						e.printStackTrace();
					} catch (FileNotFoundException e) {					
						failure = true;
						e.printStackTrace();
					} catch (SocketException e) {
						//Probably a problem with the data connection
						//try again when the data connection is restored						
						Abort = true; 						
						e.printStackTrace();
						//Looper.prepare();
						//Toast.makeText(getApplicationContext(), "Nedladdaning avbruten", Toast.LENGTH_LONG).show();	 Looper.loop();
					} catch (IOException e) {						
						//TODO this problem can be due to the fact that 
						//not data connection is available. How should is be handled??
						failure = true;
						e.printStackTrace();
						Looper.prepare();
						Toast.makeText(getApplicationContext(), "Nedladdaning avbruten", Toast.LENGTH_LONG).show();	 Looper.loop();
					} finally {
						if (failure)
						{
							Log.d(SRPlayer.TAG, "Failure downloading podcast");
							SRPlayerDB.deleteFavorite(rowId);									
						}
						
		                try {
		                    if (in != null) {
		                        in.close();
		                    }
		                    if (out != null) {
		                        out.close();
		                    }
		                } catch (IOException ioe) {
		                    //TODO how should be handle this. Retry? Try the next podcast, remove from DB?  
		                    
		                }
		            }
		    		
		    		
		    	} while ((!Abort) && (PodList.moveToNext()));
		    }
			
			PodList.requery();
	    
		}
		      
        
        //Done with all the podcasts. Stop the service
        Log.d(SRPlayer.TAG, "Stopping the download service");
        AlreadyRunning = false;
        
        if (SRPlayerDB != null)
        	SRPlayerDB.close();
        
        stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}	
	
}
