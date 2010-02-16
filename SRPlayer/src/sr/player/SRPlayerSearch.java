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
import java.util.List;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SRPlayerSearch extends ListActivity {

	private List<PodcastInfo> SearchList = new ArrayList<PodcastInfo>();
	private PodcastInfoAdapter SearchListAdapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SearchListAdapter = new PodcastInfoAdapter(this,
                R.layout.podlistitem, (ArrayList<PodcastInfo>) SearchList);
        
        PodcastInfo SearchMatch = new PodcastInfo();
        SearchMatch.setTitle("Titel");
        SearchMatch.setDescription("Beskrivning");
        SearchList.add(SearchMatch);
        
        this.setListAdapter(SearchListAdapter);
        
        
        Log.d(SRPlayer.TAG,"New search");        

        Intent intent = getIntent();        
        String query = null;

        // locate a query string; prefer a fresh search Intent over saved
        // state
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        } else if (savedInstanceState != null) {
            query = savedInstanceState.getString(SearchManager.QUERY);
        }
        if (query != null && query.length() > 0) {
            
        }

        /*
        // Do the query
        Cursor c = managedQuery(uri, PROJECTION, null, null,
                                WikiNote.Notes.DEFAULT_SORT_ORDER);
        mCursor = c;

        mHelper = new WikiActivityHelper(this);

        // Bind the results of the search into the list
        ListAdapter adapter = new SimpleCursorAdapter(
                                                      this,
                                                      android.R.layout.simple_list_item_1,
                                                      mCursor,
                                                      new String[] { WikiNote.Notes.TITLE },
                                                      new int[] { android.R.id.text1 });
        setListAdapter(adapter);

        // use the menu shortcut keys as default key bindings for the entire
        // activity
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        */
        
        
    }

    /**
     * Override the onListItemClick to open the wiki note to view when it is
     * selected from the list.
     */
    @Override
    protected void onListItemClick(ListView list, View view, int position,
                                   long id) {
        /*
        Cursor c = mCursor;
        c.moveToPosition(position);
        String title = c.getString(c
            .getColumnIndexOrThrow(WikiNote.Notes.TITLE));

        // Create the URI of the note we want to view based on the title
        Uri uri = Uri.withAppendedPath(WikiNote.Notes.ALL_NOTES_URI, title);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
        */
    	
    }

 }
