package com.samknows.ska.activity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.facebook.*;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.activity.BaseLogoutActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.samknows.libcore.ExportFileProvider;
import com.samknows.libcore.R;
import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKScreenShot;

public class SKAPostToSocialMedia extends BaseLogoutActivity {
	
	// Twitter, then Facebook!
	public class SocialStrings {
		String twitterString;
		String facebookString;
	}

	private ProgressDialog mProgress;

	//private static final String[] PERMISSIONS = new String[] {"publish_stream", "read_stream", "offline_access"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProgress = new ProgressDialog(this);
	}
	
 	Pair<Intent,Drawable> twitterClient = null;
 	Pair<Intent,Drawable> facebookClient = null;
	
	SocialStrings mMessageToPost;
	byte[] mImageToPost = null;
	
	private void promptUserToInstallTwitterApp() {

		Builder builder = new AlertDialog.Builder(SKAPostToSocialMedia.this);
		builder.setTitle("Twitter app required"); // TODO - change to resource string!
		builder.setMessage("To post to Twitter, you must first install the Twitter app from the Google Play Store");
		builder.setPositiveButton(getString(R.string.ok_dialog), null);
		AlertDialog alert = builder.create();
		alert.show();

	}

	// http://stackoverflow.com/questions/3920640/how-to-add-icon-in-alert-dialog-before-each-item
	private static class Item{
	    public final String text;
	    public final int icon;
	    public Item(String text, Integer icon) {
	        this.text = text;
	        this.icon = icon;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
	private byte[] takeScreenshot() {
		mImageToPost = null;
		
		if (!SKApplication.getAppInstance().isSocialMediaImageExportSupported()) {
			return null;
		}
		
        byte[] imageToPost = SKScreenShot.sScreenShotToByteArray(findViewById(android.R.id.content).getRootView());
        mImageToPost = imageToPost;
        
        return mImageToPost;
	}
		
	private void attachScreenshotToIntent(byte[] imageToPost, Intent intent) {
		
		// Only get an image, if that is appropriate to the application configuration.
		// For now, we push a screenshot together with the text.
		// TODO - I think this will change to being a menu option
       
		if (imageToPost != null) {
			File imageFile = new File(getCacheDir(), "social.png");
			
			FileOutputStream output = null;
			try {
				output = new FileOutputStream(imageFile);
				output.write(imageToPost);
				output.close();
				output = null;

				Uri uri = Uri.parse("content://" + ExportFileProvider.sGetAUTHORITY() + "/" +  imageFile.getName());
				//ArrayList<Uri> uris = new ArrayList<Uri>();
				//uris.add(uri);
				//intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//				intent.putExtra(Intent.EXTRA_STREAM, uri);
//				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				if (intent != null) {
					intent.putExtra(Intent.EXTRA_STREAM, uri);
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				}
			} catch (FileNotFoundException e) {
				SKLogger.sAssert(getClass(),  false);
			} catch (IOException e) {
				SKLogger.sAssert(getClass(),  false);
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						SKLogger.sAssert(getClass(),  false);
					}
				}
				
			}
		}
	}
	
	public void postTextAndMaybeImageToSocialMedia(String selectedItem, byte[] imageToPost) {
		
		mImageToPost = imageToPost;
		
		if (selectedItem.equals("Twitter")) { // TODO use resource string
			if (twitterClient == null || twitterClient.first == null) {
				promptUserToInstallTwitterApp();
			} else {
				// OK!
				startActivity(Intent.createChooser(twitterClient.first, "Post to Twitter"));
			}
		} else if (selectedItem.equals("Facebook")) { // TODO use resource string
			//					if (facebookClient.first == null) {
			//						// NOTHING to do!
			//					} else {
			// OK!

			// https://stackoverflow.com/questions/16893152/android-action-send-image-and-text
			// "actually in facebook you cannot send any text from app via action send or
			// actionsend multiple because this is the known bug in facebook."
			// https://stackoverflow.com/questions/11593292/posting-text-image-to-facebook-from-my-app?rq=1
			// "What by many android developer is classified as a bug in the facebook app is actually from their point of view a valid design decision."
			// https://stackoverflow.com/questions/14244998/sharing-images-and-text-with-facebook-on-android?rq=1
			// https://stackoverflow.com/questions/7545254/android-and-facebook-share-intent?rq=1
			// This has yet more information on the problem!
			// https://developers.facebook.com/bugs/332619626816423
			// This is the main discussion, where facebook reject the issue!

			//startActivity(Intent.createChooser(facebookClient.first, "Post to Facebook"));
			doPostToFacebookUsingFacebookSDKButPromptFirst(mMessageToPost.facebookString, mImageToPost);
			//}
		}
	}
	
	public void promptUserToSelectSocialMediaAndThenPost(final SocialStrings messageToPost) {
		mMessageToPost = messageToPost;

		twitterClient = findTwitterClient(messageToPost.twitterString);
		facebookClient = findFacebookClient(messageToPost.facebookString);
		
		if ((twitterClient != null) && (twitterClient.first != null)) {
			twitterClient.first.putExtra(Intent.EXTRA_TEXT, messageToPost.twitterString);
		}
					
		if ((facebookClient != null) && (facebookClient.first != null)) {
			facebookClient.first.putExtra(Intent.EXTRA_TEXT, messageToPost.facebookString);
		}
					

		//if ((twitterClient == null && facebookClient == null)) {
		//} else {
		Builder builder = new AlertDialog.Builder(SKAPostToSocialMedia.this);
		builder.setTitle("Choose Social Media"); // TODO - change to resource string!

		// http://stackoverflow.com/questions/3920640/how-to-add-icon-in-alert-dialog-before-each-item
		// Chooser with icons...

		final Item[] items = {
				new Item("Twitter", R.drawable.twitter), // TODO use resource string
				new Item("Facebook", R.drawable.facebook) // TODO use resource string
				//,new Item("...", 0),//no icon for this one
		};

		ListAdapter adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item,
				android.R.id.text1,
				items) {
			public View getView(int position, View convertView, ViewGroup parent) {
				//User super class to create the View
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView)v.findViewById(android.R.id.text1);

				//Put the image on the TextView
				tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

				//Add margin between image and text (support various screen densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				tv.setCompoundDrawablePadding(dp5);

				return v;
			}
		};


		mImageToPost = null;

		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				final String selectedSocialMediaItem = items[which].text;

				if (SKApplication.getAppInstance().isSocialMediaImageExportSupported()) {
					Builder builderImage = new AlertDialog.Builder(SKAPostToSocialMedia.this);
					builderImage.setTitle(R.string.social_media_screenshot_title);
					builderImage.setMessage(R.string.social_media_screenshot_message);
					builderImage.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							byte[] imageToPost = takeScreenshot();
							if (imageToPost != null) {
								File imageFile = new File(getCacheDir(), "social.png");

								FileOutputStream output = null;
								try {
									output = new FileOutputStream(imageFile);
									output.write(imageToPost);
									output.close();
									output = null;

									Uri uri = Uri.parse("content://" + ExportFileProvider.sGetAUTHORITY() + "/" +  imageFile.getName());
									//ArrayList<Uri> uris = new ArrayList<Uri>();
									//uris.add(uri);
									//intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
									//         							intent.putExtra(Intent.EXTRA_STREAM, uri);
									//         							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
									if ((twitterClient != null) && (twitterClient.first != null)) {
									
									    String theMessageToPost = messageToPost.twitterString.replace(
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeFromThis1),
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeToThis1));
									    theMessageToPost = theMessageToPost.replace(
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeFromThis2),
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeToThis2));
									   
									    // The replaceAll uses a regex!
									    theMessageToPost = theMessageToPost.replaceAll(
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeRegex4From),
									                        getString(R.string.SocialMedia_TwitterIfUsingImage_ChangeRegex4To));
									        
										twitterClient.first.putExtra(Intent.EXTRA_TEXT, theMessageToPost);

										attachScreenshotToIntent(imageToPost, twitterClient.first);
									}

									if ((facebookClient != null) && (facebookClient.first != null)) {
										attachScreenshotToIntent(imageToPost, facebookClient.first);
									}
								} catch (FileNotFoundException e) {
									SKLogger.sAssert(getClass(),  false);
								} catch (IOException e) {
									SKLogger.sAssert(getClass(),  false);
								} finally {
									if (output != null) {
										try {
											output.close();
										} catch (IOException e) {
											SKLogger.sAssert(getClass(),  false);
										}
									}

								}
							}

							postTextAndMaybeImageToSocialMedia(selectedSocialMediaItem, imageToPost);

							dialog.dismiss();
						}
					});
					builderImage.setNegativeButton(getString(R.string.no_dialog),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							postTextAndMaybeImageToSocialMedia(selectedSocialMediaItem, null);
							dialog.dismiss();
						}
					});
					AlertDialog alert = builderImage.create();
					alert.show();
				} else {
					// Social media image NOT supported!
					postTextAndMaybeImageToSocialMedia(selectedSocialMediaItem, null);
				}
			}
		});
		builder.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
		//}
	}
	
	
	// https://stackoverflow.com/questions/14051664/android-check-to-see-if-facebook-app-is-present-on-users-device
	// https://stackoverflow.com/questions/6711295/how-to-check-if-facebook-is-installed-android
	private Pair<Intent,Drawable> findSocialMediaClient(String appArray[], String messageToPost) {
		Intent theIntent = new Intent(Intent.ACTION_SEND);
		theIntent.putExtra(Intent.EXTRA_TEXT, messageToPost);
		theIntent.setType("text/plain");
		final PackageManager packageManager = getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(theIntent, PackageManager.MATCH_DEFAULT_ONLY);

		for (String anAppArray : appArray) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(anAppArray)) {
					theIntent.setPackage(p);
					Drawable appIcon = resolveInfo.activityInfo.loadIcon(packageManager);
					return new Pair<>(theIntent, appIcon);
				}
			}
		}
		return null;
	}

	private Pair<Intent,Drawable> findTwitterClient(String messageToPost) {
		final String[] appArray = { "com.twitter.android", "com.handmark.tweetcaster", "com.seesmic", "com.thedeck.android", "com.levelup.touiteur", "com.thedeck.android.app" };
		return findSocialMediaClient(appArray, messageToPost);
    }
	
	private Pair<Intent,Drawable> findFacebookClient(String messageToPost) {
		final String[] appArray = { "com.facebook.katana" };
		return findSocialMediaClient(appArray, messageToPost);
    }
	
	private void doPostToFacebookUsingFacebookSDKButPromptFirst(final String messageToPost, final byte[] imageToPost) {

		Builder builder = new AlertDialog.Builder(SKAPostToSocialMedia.this);
		builder.setTitle("Post to Facebook?"); // TODO - change to resource string!
		builder.setIcon(R.drawable.facebook); // facebookClient.second);
	
		
		// Add the custom layout, in which we can put data and the image!
		ViewGroup myView = null;
		if (imageToPost != null) {
			myView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.ska_social_media_edit_post, null);
		} else {
			myView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.ska_social_media_edit_post_no_image, null);
		}
			
		final EditText editText = (EditText) myView.findViewById(R.id.editText);
		
		if (imageToPost != null) {
    		ImageView imageView = (ImageView) myView.findViewById(R.id.imageView);
			ByteArrayInputStream imageStream = new ByteArrayInputStream(imageToPost);
			Bitmap theScreenBitmap = BitmapFactory.decodeStream(imageStream);
			imageView.setImageDrawable(new BitmapDrawable(getResources(), theScreenBitmap));
		}

		// ... and initialise the text.
		editText.setText(messageToPost);
		
		// Now attach the layout to the AlertDialog!
		builder.setView(myView); //  new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		builder.setPositiveButton(getString(R.string.ok_dialog),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
        		doPostToFacebookUsingFacebookSDK(editText.getText().toString(), imageToPost);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void doPostToFacebookUsingFacebookSDK(String messageToPost, byte[] imageToPost) {
		
		String shortString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_footer_short);
		String longString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_footer_long);
		messageToPost = messageToPost.replace(shortString, longString);
		// Change e.g. @operator to operator.
		messageToPost = messageToPost.replace("@", "");
		
		sendFacebookRequestUsingWebDialog(messageToPost, imageToPost);

		/*
		if (mFacebook == null) {
			// Create the singleton Facebook session.
			mFacebook       = new Facebook(getString(R.string.facebook_app_id));
		}
		
		// Facebook
		// Change the short, Twitter-specific strings... to longer, facebook specific strings.
		String shortString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_footer_short);
		String longString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_footer_long);
		messageToPost = messageToPost.replace(shortString, longString);
		// Change e.g. @operator to operator.
		messageToPost = messageToPost.replace("@", "");

		if (!mFacebook.isSessionValid()) {
			// We are not yet connected - try to log-in to facebook!
			// If this completes, we will automatically post the message.
			mFacebook.authorize(SKAPostToSocialMedia.this, PERMISSIONS, -1, new FbLoginDialogListener());
		} else {
			postToFacebook(messageToPost, mImageToPost);
		}
		*/
	}
	
	Context getActivity() {
		return this;
	}
	
	String getUriForImageToPost(byte[] imageToPost) {
		File imageFile = new File(getCacheDir(), "social.png");
		
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(imageFile);
			output.write(imageToPost);
			output.close();
			output = null;

			Uri uri = Uri.parse("content://" + ExportFileProvider.sGetAUTHORITY() + "/" +  imageFile.getName());
			//ArrayList<Uri> uris = new ArrayList<Uri>();
			//uris.add(uri);
			//intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//			intent.putExtra(Intent.EXTRA_STREAM, uri);
//			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			return uri.toString();
		} catch (FileNotFoundException e) {
			SKLogger.sAssert(getClass(),  false);
		} catch (IOException e) {
			SKLogger.sAssert(getClass(),  false);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					SKLogger.sAssert(getClass(),  false);
				}
			}
			
		}
		return null;
	}
	
	private void createFacebookSessionAndThenPost(final String messageToPost, final byte[] imageToPost) {
		// start Facebook Login
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if (session.isOpened()) {
					SKLogger.sAssert(getClass(), session == Session.getActiveSession());
					postImageToActiveFacebookSession (messageToPost, imageToPost);
				}
			}
		});
	}
	
	private void sendFacebookRequestUsingWebDialog(String messageToPost, byte[] imageToPost) {
		if ( (Session.getActiveSession() != null) &&
           	 (Session.getActiveSession().isOpened())
           )
		{
        	postImageToActiveFacebookSession (messageToPost, imageToPost);
			return;
		}
		
		createFacebookSessionAndThenPost(messageToPost, imageToPost);
	}
	
		Request.Callback uploadPhotoRequestCallback = new Request.Callback() {
		    @Override
		    public void onCompleted(Response response) {
		    	if (response.getError() != null) { 
		    		//post error
		    		Toast.makeText(getActivity().getApplicationContext(), 
		    				"Not posted!",
		    				Toast.LENGTH_SHORT).show();
		    		Toast.makeText(getActivity().getApplicationContext(), 
		    				"Please check that the Facebook app is installed, and signed-in!", 
		    				Toast.LENGTH_SHORT).show();
		    	} else{
		    		Toast.makeText(getActivity().getApplicationContext(), 
		    				"Message Posted!", 
		    				Toast.LENGTH_SHORT).show();
		    		//String idRploadResponse = (String) response.getGraphObject().getProperty("id");
		    		//if (idRploadResponse!= null) { 
		    		//   String fbPhotoAddress = "https://www.facebook.com/photo.php?fbid=" +idRploadResponse;                             
		    		//} else { 
		    		//      //error
		    		//} 

		    	}
		    }
		};

		//@SuppressWarnings("deprecation")
		// https://stackoverflow.com/questions/20908219/how-to-post-bitmap-to-facebook-using-facebook-sdk
		// https://stackoverflow.com/questions/7359173/create-bitmap-from-bytearray-in-android
		public void shareBitmapToFacebook(String messageToPost, Bitmap bmp) {
			//Bitmap bmp = BitmapFactory.decodeByteArray(imageToShare, 0, imageToShare.length);

			if (bmp != null) {

				Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), bmp,  uploadPhotoRequestCallback);
				Bundle parameters = request.getParameters(); // <-- THIS IS IMPORTANT
				parameters.putString("message", messageToPost);
				// add more params here
				request.setParameters(parameters);
				request.executeAsync();
			}
		}
	
		private void postImageToActiveFacebookSession (String messageToPost, byte[] imageToPost) {

			Bitmap bmp = null;
			
			if (imageToPost != null) {
				bmp = BitmapFactory.decodeByteArray(imageToPost, 0, imageToPost.length);
			}
			
			if (bmp == null) {
				bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
			}

			shareBitmapToFacebook(messageToPost, bmp);
			
			//bmp.recycle();

			/*
		Bundle params = new Bundle();
		//params.putString("message", messageToPost);
		//params.putString("name", messageToPost);
	    params.putString("name", "Facebook SDK for Android");
	    params.putString("caption", "Build great social apps and get more installs.");
	    params.putString("description", "The Facebook SDK for Android makes it easier and faster to develop Facebook integrated Android apps.");
	    params.putString("link", "https://developers.facebook.com/android");

		if (imageToPost != null) {
    		String uri = getUriForImageToPost(imageToPost);
	    	if (uri != null) {
        	    params.putString("picture", uri);
		    } else {
        	    params.putString("picture", "http://www.samknows.com/broadband/images/logo.png");
		    }
		} else {
        	 params.putString("picture", "http://www.samknows.com/broadband/images/logo.png");
		}

//		if (mImageToPost != null) {
//			String uri = getUriForImageToPost(imageToPost);
//			if (uri != null) {
//				params.putString("picture", uri );
//			}
//		}

		WebDialog.FeedDialogBuilder feedDialogBuilder =
				new WebDialog.FeedDialogBuilder(getActivity(),
						Session.getActiveSession(),
						params);
		feedDialogBuilder.setOnCompleteListener(new OnCompleteListener() {

			@Override
			public void onComplete(Bundle values,
					FacebookException error) {
				if (error != null) {
					if (error instanceof FacebookOperationCanceledException) {
						Toast.makeText(getActivity().getApplicationContext(), 
								"Message cancelled", 
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getActivity().getApplicationContext(), 
								"Error!", 
								Toast.LENGTH_SHORT).show();
					}
				} else {
					final String requestId = values.getString("request");
					if (requestId != null) {
						Toast.makeText(getActivity().getApplicationContext(), 
								"Message posted",  
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getActivity().getApplicationContext(), 
								"Message cancelled", 
								Toast.LENGTH_SHORT).show();
					}
				}   
			}

		});

		feedDialogBuilder.setName(messageToPost);
//		if (imageToPost != null) {
//			String uri = getUriForImageToPost(imageToPost);
//			if (uri != null) {
//				feedDialogBuilder.setPicture(uri);
//			}
//		}

		WebDialog feedDialog = feedDialogBuilder.build();
		feedDialog.show();
			 */
		}
	
	final int cRequestCodeForFacebookResult = 88888888;
	
	public void doPostToTwitterOldApproach(SocialStrings messageToPost, byte[] imageToPost) {
		
    	mMessageToPost = messageToPost;
    	mImageToPost = imageToPost;
    	
		Intent intent = new Intent(SKAPostToSocialMedia.this,
				SKAPostToTwitterActivity.class);

		// Remove the "about", to give shorter, Twitter-specific strings...
		String shortString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_header_short_nocarrier);
		String longString = SKAPostToSocialMedia.this.getString(R.string.socialmedia_header_long_nocarrier);
		messageToPost.twitterString = messageToPost.twitterString.replace(longString, shortString);

		intent.putExtra("messageToPost", messageToPost.twitterString);
		if (mImageToPost != null) {
			intent.putExtra("imageToPost", mImageToPost);
		}
		startActivityForResult(intent, cRequestCodeForFacebookResult);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (Session.getActiveSession() != null) {
			Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
		}
	}


	private Handler mRunOnUi = new Handler();

	// http://stackoverflow.com/questions/7175873/click-effect-on-button-in-android?lq=1
	private static AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F);

	public static void sSetViewOnTouchEffect(View button){

		button.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN: {
					buttonClick.setDuration(500);
					v.startAnimation(buttonClick);
					//v.getBackground().setColorFilter(0xe0f47521,android.graphics.PorterDuff.Mode.SRC_ATOP);
					v.invalidate();
					break;
				}
				//                    case MotionEvent.ACTION_UP: {
				//                        //v.getBackground().clearColorFilter();
				//                        v.invalidate();
				//                        break;
				//                    }
				}
				return false;
			}
		});
	}
}
