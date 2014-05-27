package com.samknows.tests;

import com.samknows.libcore.SKCommon;

public class TriggerTest extends Test {

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getNetUsage() {
		// TODO Auto-generated method stub
		return 0;
	}
//	public enum TESTTYPE {
//		BANDWIDTH, LATENCY
//	};
//
//	List<String> requestParam;
//
//	String cgiPath;
//
//	TESTTYPE testType;
//
//	public TriggerTest(String unitIP) {
//		super(unitIP);
//		requestParam = new ArrayList<String>();
//		cgiPath = "cgi-bin/inhome_cgi";
//		addParam("TYPE", "TEST");
//	}
//
//	public void addParam(String param, String value) {
//		requestParam.add(param + "=" + value);
//	}
//
//	public void bandwidth(String target) {
//		addParam("RUN", "TRIGGER_BANDWIDTH");
//		addParam("SERVER", target);
//		setType(TESTTYPE.BANDWIDTH);
//	}
//
//	@Override
//	public void execute() {
//		start();
//		HttpURLConnection conn = null;
//		URL triggerRequest = null;
//		String output = "";
//		try {
//			triggerRequest = new URL(getRequest());
//			conn = (HttpURLConnection) triggerRequest.openConnection();
//			conn.connect();
//			String line;
//			BufferedReader in = new BufferedReader(new InputStreamReader(
//					conn.getInputStream()));
//			while ((line = in.readLine()) != null) {
//				output += line + "\n";
//			}
//			in.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return;
//		}
//		setResult(output);
//		finish();
//	}
//
//	@Override
//	public String getOutputString() {
//		if (getType() == TESTTYPE.BANDWIDTH) {
//			StringBuilder sb = new StringBuilder();
//			Formatter formatter = new Formatter(sb, Locale.US);
//			double value = SKCommon.sGetDecimalStringAnyLocaleAsDouble(getResult().split(";")[7])
//					/ (1024L * 1024L);
//			formatter.format("%.2f MiB/s", value);
//			return sb.toString();
//		} else if (getType() == TESTTYPE.LATENCY) {
//			StringBuilder sb = new StringBuilder();
//			Formatter formatter = new Formatter(sb, Locale.US);
//			double value = SKCommon.sGetDecimalStringAnyLocaleAsDouble(getResult().split(";")[5]) / 1000;
//			formatter.format("%.2f ms", value);
//			return sb.toString();
//		}
//		return "Unknown test type";
//	}
//	
//	@Override
//	public String getRequest() {
//		String ret = "http://";
//		ret += getServer();
//		if (getPort() != 80) {
//			ret += ":" + getPort();
//		}
//		ret += "/" + cgiPath + "?";
//		Iterator<String> it = requestParam.iterator();
//		while (it.hasNext()) {
//			ret += it.next();
//			if (it.hasNext()) {
//				ret += "&";
//			}
//		}
//		return ret;
//	}
//	private TESTTYPE getType() {
//		return testType;
//	}
//	public void latency(String target) {
//		addParam("SERVER", target);
//		addParam("RUN", "TRIGGER_LATENCY");
//		setType(TESTTYPE.LATENCY);
//	}
//	public void latency(String target, int port, int datagrams, int interval,
//			int delay, int percentile) {
//		addParam("SERVER", target);
//		addParam("RUN", "TRIGGER_LATENCY");
//		addParam("PORT", Integer.toString(port));
//		addParam("NUM_DATAGRAMS", Integer.toString(datagrams));
//		addParam("INTER_PACKET_TIME", Integer.toString(interval));
//		addParam("DELAY_TIMEOUT", Integer.toString(delay));
//		addParam("PERCENTILE", Integer.toString(percentile));
//		setType(TESTTYPE.LATENCY);
//	}
//	private void setType(TESTTYPE testType) {
//		this.testType = testType;
//	}
//
//	@Override
//	boolean setParam(String name, String value) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	boolean isReady() {
//		// TODO Auto-generated method stub
//		return false;
//	}

	@Override
	public boolean isSuccessful() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getHumanReadableResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isProgressAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getProgress() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HumanReadable getHumanReadable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStringID() {
		// TODO Auto-generated method stub
		return null;
	}
}
