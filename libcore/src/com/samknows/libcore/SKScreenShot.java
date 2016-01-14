package com.samknows.libcore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.view.View;

// Based on https://stackoverflow.com/questions/2661536/how-to-programatically-take-a-screenshot-on-android

public class SKScreenShot {
	
	static public Bitmap sScreenShotAsBitmap(View view) {
		    Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
		    Canvas canvas = new Canvas(bitmap);
		    view.draw(canvas);
		    return bitmap;
	}

	static public boolean sScreenShotToOutputStream (View view, OutputStream toStream) {

		Bitmap bitmap = sScreenShotAsBitmap(view);
		if (bitmap == null) {
			SKLogger.sAssert(SKScreenShot.class, false);
			return false; 
		}

		if (!bitmap.compress(Bitmap.CompressFormat.PNG, 0, toStream)) {
			SKLogger.sAssert(SKScreenShot.class, false);
			return false;
		}

		try {
			toStream.flush();
		} catch (IOException e) {
			SKLogger.sAssert(SKScreenShot.class, false);
			return false;
		}

		// NB: the client would probably want to close the stream as soon as this is done...
		
		return true;
	}

	// Capture the view, to a (byte array) input stream...
	// which can then be passed-on to e.g. Twitter or Facebook.
	static public byte[] sScreenShotToByteArray (View view) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

     	if (sScreenShotToOutputStream (view, stream) == false) {
			SKLogger.sAssert(SKScreenShot.class, false);
     		return null;
     	}
     	
		//InputStream imageStream = new ByteArrayInputStream(((ByteArrayOutputStream) stream).toByteArray());
		byte[] byteArray = stream.toByteArray();
		
		try {
			stream.close();
		} catch (IOException e) {
			SKLogger.sAssert(SKScreenShot.class, false);
		}
		stream = null;

		return byteArray;
	}
	
	// Capture the view, to a (byte array) input stream...
	// which can then be passed-on to e.g. Twitter or Facebook.
	static public InputStream sScreenShotToByteArrayInputStream (View view) {
     	
		byte[] byteArray = sScreenShotToByteArray(view);
		if (byteArray == null) {
			return null;
		}
		
		InputStream imageStream = new ByteArrayInputStream(byteArray);

		return imageStream;
	}
}
