package sr.player;

import java.io.BufferedInputStream;
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
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class DownloadPodcastService extends Service {
			
	private Timer timer = new Timer();
	private boolean AlreadyRunning;
	private boolean Abort;
	private SRPlayerDBAdapter SRPlayerDB; 
	private Cursor PodList;
	
	String address,guid;		
	String filename;
	File root;		
	int bytesDownloaded;
	File subdirectory;		
	String outfile;
	byte[] buffer;
	
	BufferedOutputStream out;
    HttpURLConnection conn;
    InputStream in;
    URL url;
    long rowId;
			
	@Override
	public void onStart(Intent intent, int startId) {
		Abort = false;
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
			if ((PodList.getCount() == 0) || (Abort))
				break;
			
			if (PodList.moveToFirst())
		    {
				//Log.d(SRPlayer.TAG, "Found new podcast to download");
				buffer = new byte[8096];
		    	do {
		    		failure = false;
		    			    		
		    		guid = PodList.getString(SRPlayerDBAdapter.INDEX_GUID); //The GUID contains the unique ID and path to the file
		    		address = PodList.getString(SRPlayerDBAdapter.INDEX_LINK); //The GUID contains the unique ID and path to the file
		    		//bytesDownloaded = PodList.getInt(SRPlayerDBAdapter.INDEX_BYTESDOWNLOADED); //The GUID contains the unique ID and path to the file
		    		rowId = PodList.getLong(SRPlayerDBAdapter.INDEX_ROWID);
		    		if (address == null)
		    		{
		    			//Remove from the database since invalid entry
		    			SRPlayerDB.deleteFavorite(rowId);
		    			continue;
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
		    		
		    		
		    	} while (PodList.moveToNext());
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
