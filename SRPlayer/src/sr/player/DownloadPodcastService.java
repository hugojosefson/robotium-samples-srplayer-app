package sr.player;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import android.util.Log;
import android.widget.Toast;

public class DownloadPodcastService extends Service {
			
	private Timer timer = new Timer();
	private boolean AlreadyRunning;
	private SRPlayerDBAdapter SRPlayerDB; 
	private Cursor PodList;
	
	String address;		
	String filename;
	File root;					
	File subdirectory;		
	String outfile;
	byte[] buffer;
	
	BufferedOutputStream out;
    URLConnection conn;
    InputStream in;
    URL url;
    long rowId;
			
	@Override
	public void onStart(Intent intent, int startId) {
		if (AlreadyRunning == false)
		{
		//Due to long executiontimes the
		//service is spawn to a new thread
		timer.schedule(new TimerTask() {
        public void run() {DownloadNewPodcasts();}
      }, 0);
		};
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
				buffer = new byte[1024];
		    	do {
		    		failure = false;
		    			    		
		    		address = PodList.getString(SRPlayerDBAdapter.INDEX_GUID); //The GUID contains the unique ID and path to the file
		    		rowId = PodList.getLong(SRPlayerDBAdapter.INDEX_ROWID);
		    		if (address == null)
		    		{
		    			//Remove from the database since invalid entry
		    			SRPlayerDB.deleteFavorite(rowId);
		    			continue;
		    		}
		    		
		    		//Extract the filename
		    		File file = new File(address);  
		            String filename = file.getName();
		    			    		 	    		
		    		root = Environment.getExternalStorageDirectory();					
		    		subdirectory = new File(root, "SRPlayer");
		    		if (subdirectory.exists() == false)
		    		{
		    			Log.d(SRPlayer.TAG, "Directory does not exist. Creating it");
		    			subdirectory.mkdir();
		    		}		
		    		outfile = subdirectory.getAbsolutePath() + "/" + filename;
		    		
		    		out = null;
		            conn = null;
		            in = null;
		            try {   
		            	Log.d(SRPlayer.TAG, "Starting download of " + address);
		                url = new URL(address);
		                // Open an output stream that is a file on the SD
		                out = new BufferedOutputStream(new FileOutputStream(outfile));            
		                conn = url.openConnection();
		                in = conn.getInputStream();     
		                		                
		                SRPlayerDB.podcastSetAsCurrentDownloading(rowId);
		                
		                // Get the data	                
		                int numRead;
		                while ((numRead = in.read(buffer)) != -1) {
		                    out.write(buffer, 0, numRead);
		                }            
		                out.flush();
		                
		                //The entire file has been written
		                Log.d(SRPlayer.TAG, "Download of podcast complete");
		                		               
		                SRPlayerDB.podcastDownloadCompleteUpdate(rowId, outfile);
		            		            
		            } catch (MalformedURLException e) {								            			            
		            	failure = true;		            	
						e.printStackTrace();
					} catch (FileNotFoundException e) {					
						failure = true;
						e.printStackTrace();
					} catch (IOException e) {						
						//TODO this problem can be due to the fact that 
						//not data connection is available. How should is be handled??
						failure = true;
						e.printStackTrace();
					} finally {
						if (failure)
						{
							Log.d(SRPlayer.TAG, "Failure downloading podcast");
							SRPlayerDB.deleteFavorite(rowId);	
								
							Context context = getApplicationContext();
							CharSequence text = "Fel uppstod då Offline avsnitt skulle laddas ner. Avsnitter tas bort ifrån listan.";
							int duration = Toast.LENGTH_LONG;
	
							Toast toast = Toast.makeText(context, text, duration);
							toast.show();
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
