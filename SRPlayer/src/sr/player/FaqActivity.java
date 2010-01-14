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