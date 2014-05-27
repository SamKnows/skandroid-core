package com.samknows.measurement.net;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.net.Uri;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.environment.PhoneIdentityData;

public class DCSActivateAction extends NetAction{
	public String unitId;
	
	public DCSActivateAction(Context c, PhoneIdentityData data) {
		super();
		
		Uri.Builder uriActivate = new Uri.Builder();
		uriActivate.
		scheme("https")
    	.authority(SK2AppSettings.getInstance().getServerBaseUrl())
    	.path("mobile/activate")
    	.appendQueryParameter("IMEI", data.imei);
		String brand = SK2AppSettings.getInstance().brand;
		if(brand != null && ! brand.equals("")){
			uriActivate.appendQueryParameter("brand", brand);
		}
		String request = uriActivate.build().toString();
		SKLogger.d(DCSActivateAction.class, "request: "+request);
		setRequest(request);
		
		addHeader("X-Mobile-IMSI", data.imsi);
		addHeader("X-Mobile-Manufacturer", data.manufacturer);
		addHeader("X-Mobile-Model", data.model);
		addHeader("X-Mobile-OSType", data.osType);
		addHeader("X-Mobile-OSVersion", data.osVersion+"");
		
//		RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
//		String full = String.format("%s.%s", key.getModulus().toString(), key.getPublicExponent().toString());
//		String result = new String(Base64.encode(full.getBytes(), Base64.NO_WRAP));
//		addHeader("X-Unit-PublicKey", result);
	}

	@Override
	protected void onActionFinished() {
		try {
			unitId = IOUtils.toString(response.getEntity().getContent()).trim();
		} catch (Exception e) {
			SKLogger.e(this, "failed to parse response", e);
		}
	}

	@Override
	public boolean isSuccess() {
		return unitId != null;
	}
}
