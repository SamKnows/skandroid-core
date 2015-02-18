package com.samknows.ska.activity;

import java.io.ByteArrayInputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import com.samknows.libcore.R;
import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKApplication;

// http://blog.blundell-apps.com/sending-a-tweet/

public class SKAPostToTwitterActivity extends Activity {


	private static final String TAG = "SKAPostToTwitterActivity";

	/** Name to store the users access token */
	private static final String PREF_ACCESS_TOKEN = "accessToken";
	/** Name to store the users access token secret */
	private static final String PREF_ACCESS_TOKEN_SECRET = "accessTokenSecret";
	/** Consumer Key generated when you registered your app at https://dev.twitter.com/apps/ */
	private static final String CONSUMER_KEY = "x23Mr06aRgLJtq2nE2wJA";
	/** Consumer Secret generated when you registered your app at https://dev.twitter.com/apps/  */
	private static final String CONSUMER_SECRET = "oZAKYoX1i4zhYJQktOLlXEw5POMild8qlYDNUi8CEI"; // XXX Encode in your app
	/** The url that Twitter will redirect to after a user log's in - this will be picked up by your app manifest and redirected into this activity */
	private static final String CALLBACK_URL = "libcore-twitter-android:///";
	/** Preferences to store a logged in users credentials */
	private SharedPreferences mPrefs;
	/** Twitter4j object */
	private Twitter mTwitter;
	/** The request token signifies the unique ID of the request you are sending to twitter  */
	private RequestToken mReqToken;

    private String mMessageToPost = "";
    private byte[] mImageToPost = null;
       
    private WebView myWebView;
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Loading TweetToTwitterActivity");
		setContentView(R.layout.ska_post_to_twitter_activity);
	
		// http://adilatwork.blogspot.co.uk/2011/12/android-twitter-login-using-twitter4j.html
		myWebView = (WebView)findViewById(R.id.myWebView);
		myWebView.getSettings().setJavaScriptEnabled(true);
		myWebView.setWebViewClient(new WebViewClient()
		{
		  @Override
		  public boolean shouldOverrideUrlLoading(WebView webView, String url)
		  {
		    if (url != null
		        && url.startsWith("myapptwittercallback:///myapp"))
		      handleTwitterCallback(url);
		    else
		      webView.loadUrl(url);
		    return true;
		  }
		});
		
		setTitle(getString(R.string.socialmedia_post_to_twitter_title));
		
        Intent intent = getIntent();
        mMessageToPost = intent.getStringExtra("messageToPost");
        mImageToPost = intent.getByteArrayExtra("imageToPost");
        
		// Should only be an image, if that is appropriate to the application configuration.
        // If not null, it contains the byte data of a png file to post.
		SKLogger.sAssert(getClass(), (mImageToPost != null) == SKApplication.getAppInstance().isSocialMediaImageExportSupported());

		// Create a new shared preference object to remember if the user has
		// already given us permission
		mPrefs = getSharedPreferences("twitterPrefs", MODE_PRIVATE);
		Log.i(TAG, "Got Preferences");
		
//        if (com.samknows.measurement.util.OtherUtils.isDebuggable(this)) {
//        	// In debug builds, for now - clear the login details in the shared preferences,
//        	// to force a fresh login each time!
//        	clearLoginDetails();
//        }
		
		// Load the twitter4j helper
		mTwitter = new TwitterFactory().getInstance();
		Log.i(TAG, "Got Twitter4j");
		
		// Tell twitter4j that we want to use it with our app
		mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		Log.i(TAG, "Inflated Twitter4j");
		
		// Start by trying to login!
		doLoginToTwitter();
	}

	private void doLoginToTwitter() {
		Log.i(TAG, "doLoginToTwitter");
		if (mPrefs.contains(PREF_ACCESS_TOKEN)) {
			Log.i(TAG, "Repeat User");
			loginAuthorisedUser();
		} else {
			Log.i(TAG, "New User");
     		new LoginNewUser_AsyncCommand().execute();
		}
	}

	private void handleTwitterCallback (String url) {

		myWebView.clearHistory();
		myWebView.setVisibility(View.GONE);
		
		Uri uri = Uri.parse(url);

		String oauthVerifier = uri.getQueryParameter("oauth_verifier");
        new HandleTwitterCallback_AsyncCommand(oauthVerifier).execute();
	}
	
	@Override
	public void onBackPressed()
	{
	  if (myWebView.getVisibility() == View.VISIBLE)
	  {
	    if (myWebView.canGoBack())
	    {
	      myWebView.goBack();
	      return;
	    }
	    else
	    {
	      myWebView.setVisibility(View.GONE);
	      return;
	    }
	  }
	  super.onBackPressed();
	}

	/**
	 * The user had previously given our app permission to use Twitter</br> 
	 * Therefore we retrieve these credentials and fill out the Twitter4j helper
	 */
	private void loginAuthorisedUser() {
		String token = mPrefs.getString(PREF_ACCESS_TOKEN, null);
		String secret = mPrefs.getString(PREF_ACCESS_TOKEN_SECRET, null);

		// Create the twitter access token from the credentials we got previously
		AccessToken at = new AccessToken(token, secret);

		mTwitter.setOAuthAccessToken(at);
		
		//Toast.makeText(this, "Welcome back", Toast.LENGTH_SHORT).show();
		
		userLoggedIn();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Arrived at onResume");
	}
	
	/**
	 * Allow the user to Tweet
	 */
	private void userLoggedIn() {
		Log.i(TAG, "User logged in - allowing to tweet");
		
		// When this happens, we can now post the tweet automatically!
		new PostTweet_AsyncCommand().execute();
	}


	private void saveAccessToken(AccessToken at) {
		String token = at.getToken();
		String secret = at.getTokenSecret();
		Editor editor = mPrefs.edit();
		editor.putString(PREF_ACCESS_TOKEN, token);
		editor.putString(PREF_ACCESS_TOKEN_SECRET, secret);
		editor.commit();
	}
	
	Handler mHandler = new Handler();
	
	private void clearLoginDetails() {
		// If (say) the user has blocked the app on the Twitter site, we should clear-out cache login
		// details, to enable the user to resolve this the next time they enter the screen.
		Editor editor = mPrefs.edit();
		editor.remove(PREF_ACCESS_TOKEN);
		editor.remove(PREF_ACCESS_TOKEN_SECRET);
		editor.commit();
	}

	/**
	 * Send a tweet on your timeline, with a Toast msg for success or failure
	 */
	// Params, Progress, Result
	public class PostTweet_AsyncCommand extends AsyncTask<Void, String, Void> {
	
		WebView twitterSite = new WebView(SKAPostToTwitterActivity.this);
		  
		@Override
		protected Void doInBackground(Void... params) {
			try {
	
				// Post a simple text message.
				// This must not run on the main UI thread...
				
				if (mImageToPost == null) {
					// No image to post...
         			mTwitter.updateStatus(mMessageToPost);
				} else {
					// We have an image to post, as a png file in a byte array!
					StatusUpdate status = new StatusUpdate(mMessageToPost);
					status.setMedia("Results", new ByteArrayInputStream(mImageToPost));
					mTwitter.updateStatus(mMessageToPost);
				}

				mHandler.post(new Runnable(){

					@Override
					public void run() {
						// This must run on the main UI thread...
						String message = getString(R.string.socialmedia_twitter_tweet_successful); // Tweet Successful!
						Toast.makeText(SKAPostToTwitterActivity.this, message, Toast.LENGTH_SHORT).show();
						// Set the content view back after we changed to a webview
						setContentView(R.layout.ska_post_to_twitter_activity);
						SKAPostToTwitterActivity.this.finish();
					}});

			} catch (TwitterException e) {
				
				int errorCode = e.getErrorCode();
				
				if (errorCode == 187) {
					// Status is a duplicate!
					mHandler.post(new Runnable(){

						@Override
						public void run() {
							String message = getString(R.string.socialmedia_twitter_tweet_duplicate_detected); // Twitter won't let you post duplicate messages!
							Toast.makeText(SKAPostToTwitterActivity.this, message, Toast.LENGTH_SHORT).show();
							// Set the content view back after we changed to a webview
							setContentView(R.layout.ska_post_to_twitter_activity);
							SKAPostToTwitterActivity.this.finish();
						}});
				} else {
					mHandler.post(new Runnable(){

						@Override
						public void run() {
							String message = getString(R.string.socialmedia_twitter_tweet_error); // Tweet error, please try again later
							Toast.makeText(SKAPostToTwitterActivity.this, message, Toast.LENGTH_SHORT).show();
							// Set the content view back after we changed to a webview
							setContentView(R.layout.ska_post_to_twitter_activity);
                         	clearLoginDetails();
							SKAPostToTwitterActivity.this.finish();
						}});
				}
				
			} catch (Exception e) {
				SKLogger.sAssert(getClass(), false);
			}
			return null;
		}

	}
	
	/**
	 * Create a request that is sent to Twitter asking 'can our app have permission to use Twitter for this user'</br> 
	 * We are given back the {@link mReqToken}
	 * that is a unique indetifier to this request</br> 
	 * The browser then pops up on the twitter website and the user logins in ( we never see this informaton
	 * )</br> Twitter then redirects us to {@link CALLBACK_URL} if the login was a success</br>
	 * 
	 */
	// Params, Progress, Result
	public class LoginNewUser_AsyncCommand extends AsyncTask<Void, String, Void> {
	
	//	WebView twitterSite = new WebView(SKAPostToTwitterActivity.this);
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				// This must not run on the main UI thread...
				mReqToken = mTwitter.getOAuthRequestToken("myapptwittercallback:///myapp");

				mHandler.post(new Runnable(){
					@Override
					public void run() {
         				// This must run on the main UI thread...
						myWebView.setVisibility(View.VISIBLE);
						myWebView.requestFocus(View.FOCUS_DOWN);
						myWebView.loadUrl(mReqToken.getAuthenticationURL());
					}
				});

			} catch (TwitterException e) {
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						String message = getString(R.string.socialmedia_twitter_login_error); // Twitter Login error, please try again later
						Toast.makeText(SKAPostToTwitterActivity.this, message, Toast.LENGTH_SHORT).show();
					}});

			} catch (Exception e) {
				SKLogger.sAssert(getClass(), false);
			}
			return null;
		}

	}

	public class HandleTwitterCallback_AsyncCommand extends AsyncTask<Void, String, Void> {
		

		private String mOauthVerifier = "";
		
		public HandleTwitterCallback_AsyncCommand(String oauthVerifier) {
			super();
			
			mOauthVerifier = oauthVerifier;
		}

		
	//	WebView twitterSite = new WebView(SKAPostToTwitterActivity.this);
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				// This must not run on the main UI thread...
				AccessToken accessToken = mTwitter.getOAuthAccessToken(mReqToken, mOauthVerifier);
				mTwitter.setOAuthAccessToken(accessToken);		
				saveAccessToken(accessToken);

				mHandler.post(new Runnable(){
					@Override
					public void run() {
						// We're authenticated - we can now tweet!
         				// This must run on the main UI thread...
						userLoggedIn();
					}
				});

			} catch (TwitterException e) {
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						String message = getString(R.string.socialmedia_twitter_login_error); // Twitter Login error, please try again later
						Toast.makeText(SKAPostToTwitterActivity.this, message, Toast.LENGTH_SHORT).show();
					}});

			} catch (Exception e) {
				SKLogger.sAssert(getClass(), false);
			}
			return null;
		}

	}
	
}