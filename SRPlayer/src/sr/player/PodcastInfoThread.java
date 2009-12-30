package sr.player;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.util.Log;

public class PodcastInfoThread extends Thread {
	private SRPlayer activity;
	private List<String> PodList = new ArrayList<String>();
	private List<PodcastInfo> PodInfo = new ArrayList<PodcastInfo>();
	private int ListAction;
	private int id;	
	private int RetryCount;
	private static boolean allreadyRunning = false;
	
	public static final int GET_CATEGORIES = 0;	
	public static final int GET_ALL_PROGRAMS = 1;
	public static final int GET_PROGRAMS_BY_CATEGORY = 2;
	public static final int GET_IND_PROGRAMS = 3;
	
	public static final String podcast_categories_feed = "http://api.sr.se/poddradio/PoddCategories.aspx";
	public static final String podcasts_by_category_feed = "http://api.sr.se/poddradio/poddfeed.aspx?CategoryId=";
	public static final String podcast_programs_feed = "http://api.sr.se/poddradio/poddfeed.aspx";
	public static final String podcast_ind_program_feed = "http://api.sr.se/rssfeed/rssfeed.aspx?Poddfeed=";

	
	public PodcastInfoThread(Activity activity, int action, int ID) {
		this.activity = (SRPlayer) activity;
		ListAction = action;
		id = ID;
	}
	
	@Override
	public void run() {
		if ( allreadyRunning ) {
			//this.activity.UpdateArray(null,null);			
			return;
		}
		allreadyRunning = true;
		URL url;		
        InputStream urlStream = null;
        String FeedUrl = "";
        if (ListAction == GET_CATEGORIES)
		{
			FeedUrl = podcast_categories_feed;
		}
		else if (ListAction == GET_ALL_PROGRAMS)
		{
			FeedUrl = podcast_programs_feed;
		}
		else if (ListAction == GET_PROGRAMS_BY_CATEGORY)
		{
			FeedUrl = podcasts_by_category_feed;
			FeedUrl = FeedUrl + String.valueOf(id);
		}
		else //(ListAction == GET_IND_PROGRAMS)
		{
			FeedUrl = podcast_ind_program_feed;
			FeedUrl = FeedUrl + String.valueOf(id);			
		}        
        
        for (RetryCount = 0; RetryCount < 3; RetryCount++)
        {
	        try {								
					url = new URL(FeedUrl);			
					urlStream = url.openStream();
					break;
				} catch (MalformedURLException e) {
					Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
					//allreadyRunning = false;
					//this.activity.UpdateArray(null,null);			
					//return;
				} catch (IOException e) {
					Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
					//allreadyRunning = false;
					//String ExtraInfo = "urlStream is null";
					//if (urlStream != null)
					//	ExtraInfo = "urlStream is NOT null";
						
					//PodList.add("Error getting list. " + ExtraInfo);
					//this.activity.UpdateArray(PodList,null);
					//this.activity.UpdateArray(null,null);
					//return;
				}
		
        }
        if (urlStream == null)
        {
        	allreadyRunning = false;
			this.activity.UpdateArray(null,null);			
			return;
        }
        try {
        	
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	         factory.setNamespaceAware(false);
	         XmlPullParser xpp = factory.newPullParser();
	         xpp.setInput(urlStream, null);
	         int eventType = xpp.getEventType();
	         while( eventType != XmlPullParser.END_DOCUMENT) {
	        	 if (eventType == XmlPullParser.START_TAG) {
	        		 if ( xpp.getName().equals("item")) 
	        		 {
	        			 parsePodFeed(xpp);
	        		 }
	        	 }
	        	 eventType = xpp.next();
	         }
	         this.activity.UpdateArray(PodList,PodInfo);
		} catch (XmlPullParserException e) {
			Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
			this.activity.UpdateArray(null,null);
		} catch (IOException e) {
			Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
			this.activity.UpdateArray(null,null);
		} finally {
			try {
				if ( urlStream != null ) {					
					urlStream.close();		
					Log.d(SRPlayer.TAG, "Podinfo stream closed");
				}
			} catch (IOException e) { }
		}
		
		
		allreadyRunning = false;			
	}
	
	private void parsePodFeed(XmlPullParser xpp) throws XmlPullParserException, IOException {
		//Log.d(SRPlayer.TAG, "PodcastInfoThread parseChannel");		
		int eventType = xpp.next();
		PodcastInfo NewInfo = new PodcastInfo();
		
        while( eventType != XmlPullParser.END_DOCUMENT) {
        	if (eventType == XmlPullParser.END_TAG ) {
        		if (xpp.getName().equals("item")) {
        			//Log.d(SRPlayer.TAG, "PodcasInfoThred parseChannel end item");
        			PodInfo.add(NewInfo);
        			return;
        		}
        	} else if (eventType == XmlPullParser.START_TAG ) {
        		if (xpp.getName().equals("title")) {        			
        			xpp.next();
        			PodList.add(xpp.getText());
        			NewInfo.setTitle(xpp.getText());
        		} else if (xpp.getName().equals("id")) {
        			xpp.next();
        			NewInfo.setID(xpp.getText());
        			
        		} else if (xpp.getName().equals("link")) {
        			xpp.next();
        			NewInfo.setLink(xpp.getText());        			
        		}
        		else if (xpp.getName().equals("poddid")) {
        			xpp.next();
        			NewInfo.setPoddID(xpp.getText());        			
        		}
        		else if (xpp.getName().equals("description")) {
        			xpp.next();        			
        			NewInfo.setDescription(xpp.getText());        			
        		}
        	}
        	eventType = xpp.next();
        }
        
	} // end parseChannel

	
}
