package com.samknows.measurement;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;

import android.content.Context;

/**
 * not used anymore
 * @author ymyronovych
 *
 */
public class Security {
	public static final String TAG = Security.class.getName();
	
	private static KeyPair generateRSAKeys() {
		KeyPair keypair = null;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			keypair = keyGen.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return keypair;
	}
	
	private static void saveKeys(Context c, KeyPair keyPair) {
		ObjectOutputStream oos = null;
		try {
			OutputStream os = c.openFileOutput(SKConstants.KEYS_FILE_NAME, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(os);
			oos.writeObject(keyPair);
		} catch (Exception e) {
			SKLogger.e(TAG, "failed to save RSA keys. What should I do Master???", e);
		} finally {
			IOUtils.closeQuietly(oos);
		}
	}
	
	private static KeyPair readSaved(Context c) {
		ObjectInputStream ois = null;
		KeyPair keyPair = null;
		try {
			InputStream is = c.openFileInput(SKConstants.KEYS_FILE_NAME);
			ois = new ObjectInputStream(is);
			keyPair = (KeyPair) ois.readObject();
		} catch (FileNotFoundException e) {
			//ignore, not keys yet, so generate new
		} catch (Exception e) {
			SKLogger.e(TAG, "failed to read RSA keys. What should I do Master???", e);
			return null;
		} finally {
			IOUtils.closeQuietly(ois);
		}
		
		return keyPair;
	}
	
	public static KeyPair getKeys(Context c) {
		KeyPair keyPair = readSaved(c);
		if (keyPair == null) {
			keyPair = generateRSAKeys();
			saveKeys(c, keyPair);
		}
		return keyPair;
	}
}
