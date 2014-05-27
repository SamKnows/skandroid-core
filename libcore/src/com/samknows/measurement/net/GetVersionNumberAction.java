package com.samknows.measurement.net;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.net.Uri;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SKAppSettings;

public class GetVersionNumberAction extends NetAction{
	public InputStream content;
	public String version, path;
	public GetVersionNumberAction(Context c, String currentVersion) {
		super();
		String request = new Uri.Builder().scheme("https")
				.authority(SKAppSettings.getInstance().getServerBaseUrl())
				.path("/mobile/version")
				.appendQueryParameter("current", currentVersion).build().toString();

		setRequest(request);

		addHeader("X-Encryption-Desired", "false");
		addHeader("X-Unit-ID", SKAppSettings.getInstance().getUnitId());
	}

	@Override
	public boolean isSuccess() {
		return version != null && path != null;
	}

	@Override
	protected void onActionFinished() {
		try {
			content = response.getEntity().getContent();
			List<String> lines = IOUtils.readLines(content);
			version = lines.get(0);
			path = lines.get(1);
		} catch (Exception e) {
			SKLogger.e(this, "failed to parse response", e);
		}
	}
}
