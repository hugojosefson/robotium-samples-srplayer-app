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

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class FaqActivity extends Activity 
{
    private WebView webView;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	webView = new WebView(this);
    	
    	setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
                 
        webView.loadUrl("http://sr-player.googlecode.com/svn/htdocs/about.html"); 
        
        final Activity activity = this;
        webView.setWebViewClient(new WebViewClient() {
        	   public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        	     Toast.makeText(activity, "Det gick inte att hämta FAQ sidan! " + description, Toast.LENGTH_SHORT).show();
        	   }
        	 });
    }           
}