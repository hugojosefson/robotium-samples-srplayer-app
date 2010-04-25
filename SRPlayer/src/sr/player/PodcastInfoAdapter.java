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

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PodcastInfoAdapter extends ArrayAdapter<PodcastInfo> {

        private ArrayList<PodcastInfo> items;
        private Context CurrentContext;
        private PodcastInfo CurrentItem;

        public PodcastInfoAdapter(Context context, int textViewResourceId, ArrayList<PodcastInfo> items) {
                super(context, textViewResourceId, items);
                this.items = items;
                CurrentContext = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;                
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)CurrentContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.podlistitem, null);
                }
                
                RelativeLayout MainView = (RelativeLayout) v.findViewById(R.id.podlistview);
                if ((position % 2) != 0)
                {
                	
                	MainView.setBackgroundResource(R.drawable.list_selector_bg_odd);
                }
                else
                {                	
                	MainView.setBackgroundResource(R.drawable.list_selector_bg);
                }
                	
                
                CurrentItem = items.get(position);                
                String Title = CurrentItem.getTitle();
                String Desciption = CurrentItem.getDescription();
                TextView tt = (TextView) v.findViewById(R.id.text1);
                String TagLine = null;
                if (tt != null) 
                {                    
                    int GBDrawID = 0;
                    
                    if (CurrentItem.getType() == SRPlayerDBAdapter.CHANNEL)
                    {
	                    switch (Integer.valueOf(CurrentItem.getID()))
	                    {
		    			case SRPlayer.P1_CHANNELID : //P1
		    				GBDrawID = R.drawable.p1_gradient;
		    				//TagLine = "den talade kanalen";
		    				TagLine = CurrentItem.getTagline();
		    				break;
		    			case SRPlayer.P2_CHANNELID : //P2
		    				GBDrawID = R.drawable.p2_gradient;
		    				//TagLine = "musik och språk";
		    				TagLine = CurrentItem.getTagline();
		    				break;
		    			case SRPlayer.P3_CHANNELID : //P3
		    				GBDrawID = R.drawable.p3_gradient;
		    				//TagLine = "den unga kanalen";
		    				TagLine = CurrentItem.getTagline();
		    				break;
		    			case SRPlayer.P4_CHANNELID : //P4
		    				GBDrawID = R.drawable.p4_gradient;
		    				//TagLine = "den vuxna kanalen";
		    				TagLine = CurrentItem.getTagline();
		    				break;
		    			/*
		    			case SRPlayer.P4_SPORT_CHANNELID :
		    				TagLine = "radiosporten";
		    				Title = "P4";
		    				GBDrawID = R.drawable.p4_gradient;
		    				break;
		    				*/
		    			default:    
		    				//Instead of using category, check if
		    				//the name starts with P4
		    				String[] SplitStr;		    				
		    				if (Title.indexOf("P4 ") >= 0)
		    				{		    				
		    					SplitStr = Title.split(" ");
		    					if (SplitStr.length > 1)
		    					{
		    						GBDrawID = R.drawable.p4_gradient;
		    						Title = SplitStr[0];
		    						TagLine = SplitStr[1];
		    					}
		    				}
		    				break;
		    			}
                    }
                    
                    tt.setText(Title);
                    
                    if (GBDrawID != 0)
                    {
                    	tt.setBackgroundResource(GBDrawID);
                    }
                    else
                    {
                    	tt.setBackgroundColor(android.R.color.transparent);
                    }
                                        
                }
                
                TextView taglineview = (TextView) v.findViewById(R.id.tagline);
                if (TagLine != null)
                {                	                	
                	taglineview.setText(TagLine);
                	taglineview.setVisibility(View.VISIBLE);
                	tt.setTextColor(Color.WHITE);
                }
                else                
                {
                	taglineview.setVisibility(View.GONE);
                	tt.setTextColor(Color.BLACK);
                }                                       
                
                TextView bt = (TextView) v.findViewById(R.id.text2);
                ProgressBar pb = (ProgressBar) v.findViewById(R.id.progress);
                pb.setVisibility(View.GONE);
                if (CurrentItem.getType() == SRPlayerDBAdapter.EPISODE_TO_DOWNLOAD)
                {       
                	int CurrID = (Integer.parseInt(CurrentItem.getID())); 
                	if ((CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD) ||
                		(CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED))
                	{
                		
                		
                		//Calculate the progress
                		int BytesDownloaded = CurrentItem.getBytesdownloaded();
                		int FileSize = CurrentItem.getFilesize();
                		double FileSizeMB = ((double)FileSize)/1000000;
                		double BytesDownloadedMB = ((double)BytesDownloaded)/1000000;
                		
                		String CurrDownloadText = CurrentContext.getResources().getString(R.string.CurrentDownloadDesc);
                		if (CurrID == SRPlayerDBAdapter.ACTIVE_DOWNLOAD_PAUSED)
                		{
                			CurrDownloadText = "Pausad! ";
                		}
                		                		
                		if ((FileSize > 0) && (BytesDownloaded > 0))
                		{                			
                			CurrDownloadText = String.format("%s (%.1fMB/%.1fMB)", CurrDownloadText,BytesDownloadedMB,FileSizeMB);
                			//CurrDownloadText = CurrDownloadText + " (" + BytesDownloadedMB + "MB/" + FileSizeMB + "MB)";
                			pb.setMax(FileSize);
                			pb.setProgress(BytesDownloaded);
                		}
                		else
                		{
                			Log.d(SRPlayer.TAG,"Incorrect size/bytes downloaded");
                			pb.setProgress(0);
                		}
                		
                		
                		
                		bt.setText(CurrDownloadText);
                		pb.setVisibility(View.VISIBLE);
                	}
                	else
                	{	                		
                		bt.setText(R.string.QueueDownloadDesc);
                	}
                	
            		bt.setVisibility(View.VISIBLE);
            		bt.setTextColor(Color.RED);
                }
                else if (bt != null) 
                {
                	if (Desciption != null)
                	{
                		bt.setText(Desciption);
                		bt.setVisibility(View.VISIBLE);
                		bt.setTextColor(Color.BLACK);
                	}                	
                }
                	
                return v;
        }
}