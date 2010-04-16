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
	private List<PodcastInfo> PodInfo = new ArrayList<PodcastInfo>();
	private int ListAction;
	private int id;	
	private int RetryCount;
	private static boolean allreadyRunning = false;
	
	public static final int GET_CATEGORIES = 0;
	public static final int GET_ALL_PROGRAMS = 1;
	public static final int GET_PROGRAMS_BY_CATEGORY = 2;
	public static final int GET_IND_PROGRAMS = 3;
	public static final int GET_CHANNEL_LIST = 4;
	
	public static final String podcast_categories_feed = "http://api.sr.se/api/poddradio/PoddCategories.aspx";
	public static final String podcasts_by_category_feed = "http://api.sr.se/api/poddradio/poddfeed.aspx?CategoryId=";
	public static final String podcast_programs_feed = "http://api.sr.se/api/poddradio/poddfeed.aspx";
	public static final String podcast_ind_program_feed = "http://api.sr.se/api/rssfeed/rssfeed.aspx?Poddfeed=";
	public static final String channel_list_feed = "http://api.sr.se/api/channels/channels.aspx";
	private int type;
	
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
			type = SRPlayerDBAdapter.CATEGORY;
		}
		else if (ListAction == GET_ALL_PROGRAMS)
		{
			FeedUrl = podcast_programs_feed;
			type = SRPlayerDBAdapter.PROGRAM;
		}
		else if (ListAction == GET_PROGRAMS_BY_CATEGORY)
		{
			FeedUrl = podcasts_by_category_feed;
			FeedUrl = FeedUrl + String.valueOf(id);
			type = SRPlayerDBAdapter.PROGRAM;
		}
		else if (ListAction == GET_CHANNEL_LIST)
		{
			FeedUrl = channel_list_feed;
			type = SRPlayerDBAdapter.CHANNEL;
		}
		else //(ListAction == GET_IND_PROGRAMS)
		{
			FeedUrl = podcast_ind_program_feed;
			FeedUrl = FeedUrl + String.valueOf(id);			
			type = SRPlayerDBAdapter.EPISODE;
		}        
        
        for (RetryCount = 0; RetryCount < 3; RetryCount++)
        {
	        try {								
					url = new URL(FeedUrl);			
					urlStream = url.openStream();
					break;
				} catch (MalformedURLException e) {
					Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
					
				} catch (IOException e) {
					Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
					
				}
		
        }
        if (urlStream == null)
        {
        	allreadyRunning = false;
			this.activity.UpdateArray(null);			
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
	        		 String CurrName = xpp.getName();
	        		 if ( CurrName.equals("item")) 
	        		 {
	        			 parsePodFeed(xpp);
	        		 }	        		 
	        		 else if ((ListAction == GET_CHANNEL_LIST) && ( CurrName.equals("channel"))) 
	        		 {
	        			 parseChannelFeed(xpp);
	        		 }
	        	 }
	        	 eventType = xpp.next();
	         }
	         this.activity.UpdateArray(PodInfo);
		} catch (XmlPullParserException e) {
			Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
			this.activity.UpdateArray(null);
		} catch (IOException e) {
			Log.e(SRPlayer.TAG, "Error getting Podcast Feed", e);
			this.activity.UpdateArray(null);
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
	
	private void parseChannelFeed(XmlPullParser xpp) throws XmlPullParserException, IOException {		
		PodcastInfo NewInfo = new PodcastInfo();
		NewInfo.setType(type);
		int eventType = xpp.getEventType();
		String CurrentTag=""; 
		String CurrentID,CurrentName;
		boolean ValidStream = false;
		String CurrentType="";
		while (eventType != XmlPullParser.END_DOCUMENT) {          			
			if(eventType == XmlPullParser.START_TAG)
			{				          
				CurrentTag = xpp.getName();				
				if (CurrentTag.equals("channel")) {
					//Retreive the id and the name
					CurrentID = xpp.getAttributeValue(null, "id");
					CurrentName = xpp.getAttributeValue(null, "name");
					NewInfo.setTitle(CurrentName);
					NewInfo.setID(CurrentID);
				}				
				else if (CurrentTag.equals("url")) {        			        			        	
					//Check if the channel has support for 3gp
					CurrentType = xpp.getAttributeValue(null, "type");									
        		} 

			}
			else if(eventType == XmlPullParser.END_TAG) {              				         
				if (xpp.getName().equals("channel")) {
					if (ValidStream)
						PodInfo.add(NewInfo);
        			return;
        		}
				else CurrentTag = "none";
			} 
			else if(eventType == XmlPullParser.TEXT) {              				
				String CurrentText = xpp.getText();
				if (CurrentTag.equals("url")) {
					if (CurrentType.equals("3gp"))
					{
						ValidStream = true;
						NewInfo.setLink(CurrentText);
					}
				}
				
								
			}			
			eventType = xpp.next();         
		}
		//Log.d(SRPlayer.TAG, "RightNowTask parseChannel");
		return;

	} // end parseChannel

	
	private void parsePodFeed(XmlPullParser xpp) throws XmlPullParserException, IOException {		
		PodcastInfo NewInfo = new PodcastInfo();
		NewInfo.setType(type);
		int eventType = xpp.getEventType();
		String CurrentTag=""; 
		while (eventType != XmlPullParser.END_DOCUMENT) {          			
			if(eventType == XmlPullParser.START_TAG)
			{				          
				CurrentTag = xpp.getName();				
			}
			else if(eventType == XmlPullParser.END_TAG) {              				         
				if (xpp.getName().equals("item")) {        			
        			PodInfo.add(NewInfo);
        			return;
        		}
				else CurrentTag = "none";
			} 
			else if(eventType == XmlPullParser.TEXT) {              				
				String CurrentText = xpp.getText();
				
				if (CurrentTag.equals("title")) {        			        			
        			NewInfo.setTitle(CurrentText);
        		} else if (CurrentTag.equals("id")) {        			
        			NewInfo.setID(CurrentText);        			
        		} else if (CurrentTag.equals("link")) {        			
        			NewInfo.setLink(CurrentText);        			
        		}
        		else if (CurrentTag.equals("poddid")) {        			
        			NewInfo.setPoddID(CurrentText);        			
        		}
        		else if (CurrentTag.equals("description")) {        			
        			NewInfo.setDescription(CurrentText);        			
        		}
        		else if (CurrentTag.equals("guid")) {        			
        			NewInfo.setGuid(CurrentText);        			
        		}

			}			
			eventType = xpp.next();         
		}
		//Log.d(SRPlayer.TAG, "RightNowTask parseChannel");
		return;

	} // end parseChannel

	
}
