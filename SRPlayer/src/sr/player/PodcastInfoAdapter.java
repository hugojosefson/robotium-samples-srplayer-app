package sr.player;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
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
                CurrentItem = items.get(position);                
                String Title = CurrentItem.getTitle();
                String Desciption = CurrentItem.getDescription();
                TextView tt = (TextView) v.findViewById(R.id.text1);                
                if (tt != null) 
                {
                    tt.setText(Title);                            		
                }
                                                               
                TextView bt = (TextView) v.findViewById(R.id.text2);
                ProgressBar pb = (ProgressBar) v.findViewById(R.id.progress);
                pb.setVisibility(View.GONE);
                if (CurrentItem.getType() == SRPlayerDBAdapter.AVSNITT_ATT_LADDA_NER)
                {                	
                	if ((Integer.parseInt(CurrentItem.getID())) == SRPlayerDBAdapter.ACTIVE_DOWNLOAD)
                	{
                		
                		
                		//Calculate the progress
                		int BytesDownloaded = CurrentItem.getBytesdownloaded();
                		int FileSize = CurrentItem.getFilesize();
                		double FileSizeMB = ((double)FileSize)/1000000;
                		double BytesDownloadedMB = ((double)BytesDownloaded)/1000000;
                		
                		String CurrDownloadText = CurrentContext.getResources().getString(R.string.CurrentDownloadDesc);
                		                		
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
                else
                {
                	bt.setVisibility(View.GONE);
                }	
                return v;
        }
}