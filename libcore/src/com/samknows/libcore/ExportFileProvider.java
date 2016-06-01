package com.samknows.libcore;

import java.io.File;
import java.io.FileNotFoundException;
import com.samknows.measurement.SKApplication;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

// http://stephendnicholas.com/archives/974

public class ExportFileProvider extends ContentProvider {
	
	private static final String CLASS_NAME = "ExportFileProvider";

	// The authority is the symbolic name for the provider class
	private static String sAUTHORITY = null;
	
	synchronized public static String sGetAUTHORITY() {
		if (sAUTHORITY == null) {
		  sAUTHORITY = SKApplication.getAppInstance().getExportFileProviderAuthority();
		}
		return sAUTHORITY;
	}

	// UriMatcher used to match against incoming requests
	private UriMatcher uriMatcher;
	
	final private int cMatchCode = 1;

	@Override
	public boolean onCreate() {
		Log.d("ExportFileProvider", "ExportFileProvider - onCreate()");
		
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		// Add a URI to the matcher which will match against the form
		// 'content://com.samknows.myapppackage.ExportFileProvider.provider/*'
		// and return 1 in the case that the incoming Uri matches this pattern
		uriMatcher.addURI(sGetAUTHORITY(), "*", cMatchCode);

		return true;
	}

	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
			throws FileNotFoundException {
		Log.d("ExportFileProvider", "ExportFileProvider - openFile, uri=" + uri.toString());
		
		// Check incoming Uri against the matcher
		switch (uriMatcher.match(uri)) {

      // If it returns 1 - then it matches the Uri defined in onCreate
      case cMatchCode: {

        // https://stackoverflow.com/questions/12170386/create-and-share-a-file-from-internal-storage
        String fileLocation = getContext().getCacheDir() + "/" + uri.getLastPathSegment();
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(fileLocation), ParcelFileDescriptor.MODE_READ_ONLY);
        return pfd;
      }

      default:
        break;
    }

		// Otherwise unrecognised Uri
		Log.v("ExportFileProvider", "ExportFileProvider - Unsupported uri: '" + uri + "'.");
		throw new FileNotFoundException("Unsupported uri: " + uri.toString());
	}


	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
											String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		Log.d("ExportFileProvider", "ExportFileProvider - getType, uri=" + uri.toString());
		
		if (uri.getLastPathSegment().endsWith(".png")) {
    		return "image/png";
		}
		
		if (uri.getLastPathSegment().endsWith(".jpg")) {
    		return "image/jpg";
		}
		
		if (uri.getLastPathSegment().endsWith(".zip")) {
    		return "application/zip";
		}
	
		// Unexpected - but assume it is zip, given no other information!
		SKLogger.sAssert(getClass(),  false);
		
		return "application/zip";
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
										String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
