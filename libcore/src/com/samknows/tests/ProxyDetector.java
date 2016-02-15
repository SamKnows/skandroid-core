package com.samknows.tests;

import com.samknows.libcore.SKLogger;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

public class ProxyDetector extends SKAbstractBaseTest implements Runnable {

	private static final String X_REMOTE_ADDR = "X-Remote-Addr";
	private static final String HTTP_VIA = "Via";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_PROXY_DETECTOR = "X-Proxy-Detector";

  private int port = 80;
  private String targetIpAddress = "" ;
  private String seenIp = "NONE";
  private String target = "";
  private boolean success = false;
  private String forwardedForIp = "NONE";
  private String httpViaIp = "NONE";
  private String file = "";

	//OUTPUTFORMAT
	//PROXYDETECTOR;<unix timestamp>;<OK|FAIL>;<target>;<target ip>;<x-remote-addr>;<http-via-ip>;<x-forwarded-ip>
	private static final String TESTSTRING = "PROXYDETECTOR";

  private ProxyDetector() {
  }

  public static ProxyDetector sCreateProxyDetector(List<Param> params) {
    ProxyDetector ret = new ProxyDetector();
    try {
      for (Param param : params) {
        String value = param.getValue();
        if (param.contains(TestFactory.TARGET)) {
          ret.setTarget(value);
        } else if (param.contains( TestFactory.PORT)) {
          ret.setPort(Integer.parseInt(value));
        } else if (param.contains( TestFactory.FILE)) {
          ret.setFile(value);
        } else {
          ret = null;
          break;
        }
      }
    } catch (NumberFormatException nfe) {
      ret = null;
    }
    return ret;
  }

	private static final String checkHeader(String line, String header){
		String ret = null;
		if(line.toLowerCase().startsWith(header.toLowerCase())){
			int start = line.indexOf(' ') + 1;
			if(start !=-1){
				ret = line.substring(start);
			}
		}
		return ret;
	}

  private Long mTimestamp = SKAbstractBaseTest.sGetUnixTimeStamp();
	@Override
	public synchronized void finish() {
		mTimestamp = SKAbstractBaseTest.sGetUnixTimeStamp();
		status = STATUS.DONE;
	}

  @Override
  public long getTimestamp() {
    return mTimestamp;
  }

  @Override
  public JSONObject getJSONResult() {
    SKLogger.sAssert(false);
    return new JSONObject();
	}

	@Override
	public void runBlockingTestToFinishInThisThread() {
		try{
			InetAddress addr = InetAddress.getByName(target);
			targetIpAddress = addr.getHostAddress();
			Socket conn = new Socket(addr, port);
			PrintWriter writerOut = new PrintWriter(conn.getOutputStream(), false);
			writerOut.print(getHeaderRequest());
			writerOut.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			int returnCode = 0;
			String line = reader.readLine();

			if( line != null && line.length() != 0 ){
				int s = line.indexOf(' ');
				int f = line.indexOf(' ', ++s);
				returnCode = Integer.parseInt(line.substring(s, f));
			}

			if(returnCode != 200){
				success = false;
				conn.close();
				return;
			}

			while ((line = reader.readLine()) != null){
				String headerValue = checkHeader(line, X_PROXY_DETECTOR);
				if(headerValue!= null && headerValue.equals("true")){
					success = true;
					continue;
				}
				headerValue = checkHeader(line, X_REMOTE_ADDR);
				if(headerValue != null){
					seenIp = headerValue;
					continue;
				}
				headerValue = checkHeader(line,X_FORWARDED_FOR);
				if(headerValue != null){
					forwardedForIp = headerValue;
					continue;
				}
				headerValue = checkHeader(line, HTTP_VIA);
				if(headerValue != null){
					httpViaIp = headerValue;
					continue;
				}
				if(line.length() == 0 ){
					break;
				}
			}
			conn.close();
		}catch(Exception e){
			success = false;
      SKLogger.sAssert(false);
		}
	}

	private String getHeaderRequest(){
		String request = "GET /%s HTTP/1.1\r\nHost: %s \r\nACCEPT: */*\r\n\r\n";
		return String.format(request, file, target);
	}

	@Override
	public boolean isSuccessful() {
		return success;
	}

	@Override
	public void run() {
		setStateToRunning();
		runBlockingTestToFinishInThisThread();
		finish();
	}

	@Override
	public int getProgress0To100() {
		return 0;
	}

	@Override
	public boolean isReady() {
		if(target.equals("")){
			return false;
		}
		if(file.equals("")){
			return false;
		}
		return true;
	}

	@Override
	public int getNetUsage() {
		return 0;
	}

  private void setPort(int p){
		port = p;
	}

	private void setTarget(String t){
		target = t;
	}

	private void setFile(String f){
		file = f;
	}


	@Override
	public String getStringID() {
		// TODO Auto-generated method stub
		return null;
	}
}
