package com.samknows.measurement.storage;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.samknows.libcore.SKPorting;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.SKApplication;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.tests.HttpTest;
import com.samknows.tests.LatencyTest;
import com.samknows.tests.SKAbstractBaseTest;

//Model for the test_result table in the SQLite database 
public class StorageTestResult extends JSONObject{

  // These are DETAILED test ids.
  // They are ONLY referenced INTERNALLY.
  public enum DETAIL_TEST_ID {

    DOWNLOAD_TEST_ID(0),
    UPLOAD_TEST_ID(1),
    LATENCY_TEST_ID(2),
    PACKETLOSS_TEST_ID(3),
    JITTER_TEST_ID(4);

    private int value;

    DETAIL_TEST_ID(int value) {
      this.value = value;
    }

    public int getValueAsInt() {
      return value;
    }

    public static DETAIL_TEST_ID sGetTestIdForInt(int value) {
      if (value ==  DETAIL_TEST_ID.DOWNLOAD_TEST_ID.value) {
        return DETAIL_TEST_ID.DOWNLOAD_TEST_ID;
      } else if (value ==  DETAIL_TEST_ID.UPLOAD_TEST_ID.value) {
        return DETAIL_TEST_ID.UPLOAD_TEST_ID;
      } else if (value ==  DETAIL_TEST_ID.LATENCY_TEST_ID.value) {
        return DETAIL_TEST_ID.LATENCY_TEST_ID;
      } else if (value ==  DETAIL_TEST_ID.PACKETLOSS_TEST_ID.value) {
        return DETAIL_TEST_ID.PACKETLOSS_TEST_ID;
      } else if (value ==  DETAIL_TEST_ID.JITTER_TEST_ID.value) {
        return DETAIL_TEST_ID.JITTER_TEST_ID;
      } else {
        SKPorting.sAssert(false);
        return DETAIL_TEST_ID.DOWNLOAD_TEST_ID;
      }
    }
  }
	
	//Test Result JSONObject implementation
	public static final String JSON_TYPE_ID = "type";
	public static final String JSON_TYPE_NAME = "type_name";
	public static final String JSON_TESTNUMBER = "testnumber";
	public static final String JSON_STATUS_COMPLETE = "status_complete";
	public static final String JSON_DTIME = "dtime";
	public static final String JSON_DATETIME = "datetime";
	public static final String JSON_LOCATION = "location";
	public static final String JSON_RESULT = "result";
	public static final String JSON_SUCCESS = "success";
	public static final String JSON_HRRESULT = "hrresult";



	public static final String UPLOAD_TEST_STRING = "upload";
	public static final String DOWNLOAD_TEST_STRING = "download";
	public static final String LATENCY_TEST_STRING = "latency";
	public static final String PACKETLOSS_TEST_STRING = "packetloss";
	public static final String JITTER_TEST_STRING = "jitter";
	private enum TESTSSTRINGID {
		JHTTPGET, JHTTPGETMT, JHTTPPOST, JHTTPPOSTMT, JUDPLATENCY, JUDPJITTER
	}
	
	
	
	private DETAIL_TEST_ID _test_id;
	
	private StorageTestResult(DETAIL_TEST_ID test_id){
		_test_id = test_id;
		put(JSON_TYPE_ID, _test_id+"");
		put(JSON_TYPE_NAME, testIdToString(_test_id));
	}
	
	public String getTestIdAsString() {
		return testIdToString(_test_id);
	}
	
	public static String testIdToString(DETAIL_TEST_ID test_id) {
		switch (test_id) {
		case UPLOAD_TEST_ID:
			return UPLOAD_TEST_STRING;
		case DOWNLOAD_TEST_ID:
			return DOWNLOAD_TEST_STRING;
		case LATENCY_TEST_ID:
			return LATENCY_TEST_STRING;
		case PACKETLOSS_TEST_ID:
			return PACKETLOSS_TEST_STRING;
		case JITTER_TEST_ID:
			return JITTER_TEST_STRING;
		}
		return "";
	}
	
	public StorageTestResult(String type, long dtime, String location, long success, double result){
		_test_id = testStringToId(type);
		setTime(dtime);
		setLocation(location);
		putLong(JSON_SUCCESS, success);

    // This writes the test result via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
    // by ManualTestRunner:progressMessage
		setResult(result);
	}
	
	public static DETAIL_TEST_ID testStringToId(String testString) {
		if (UPLOAD_TEST_STRING.equals(testString)) {
			return DETAIL_TEST_ID.UPLOAD_TEST_ID;
		} else if (DOWNLOAD_TEST_STRING.equals(testString)) {
			return DETAIL_TEST_ID.DOWNLOAD_TEST_ID;
		} else if (LATENCY_TEST_STRING.equals(testString)) {
			return DETAIL_TEST_ID.LATENCY_TEST_ID;
		} else if (PACKETLOSS_TEST_STRING.equals(testString)) {
			return DETAIL_TEST_ID.PACKETLOSS_TEST_ID;
		} else if (JITTER_TEST_STRING.equals(testString)) {
			return DETAIL_TEST_ID.JITTER_TEST_ID;
		}

    SKPorting.sAssert(false);
    return DETAIL_TEST_ID.DOWNLOAD_TEST_ID;
	}
	
//	public static String hrResult(String test_type, double value){
//		return hrResult(testStringToId(test_type),value);
//	}
	
	public static String hrResult(DETAIL_TEST_ID test_type_id, double value){
		String ret = value +"";
		switch (test_type_id) {
		case UPLOAD_TEST_ID:
		case DOWNLOAD_TEST_ID:
			ret = throughputToString(value);
			break;
		case LATENCY_TEST_ID:
		case JITTER_TEST_ID:
			ret = timeMicrosecondsToString(value);
			break;
		case PACKETLOSS_TEST_ID:
			ret = String.format("%.2f %%", value);
			break;
		}
		return ret;
	}

  // This writes the test result via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
  // by ManualTestRunner:progressMessage
	private void setResult(double value) {
		String hrresult = "";
		switch (_test_id) {
		case UPLOAD_TEST_ID:
			Log.d("*******", "setResult for Upload test");
			hrresult = throughputToString(value);
			break;
		case DOWNLOAD_TEST_ID:
			Log.d("*******", "setResult for Download test");
			hrresult = throughputToString(value);
			break;
		case LATENCY_TEST_ID:
			hrresult = timeMicrosecondsToString(value);
			break;
		case JITTER_TEST_ID:
			hrresult = timeMicrosecondsToString(value);
			break;
		case PACKETLOSS_TEST_ID:
			hrresult = String.format("%.2f %%", value);
			break;
		}
		putDouble(JSON_RESULT, value);

    // The FOLLOWING FIELD is the value extracted from the Message instance...
		put(JSON_HRRESULT, hrresult);
	}

	public static String throughputToString(double value) {
		
		if (SKApplication.getAppInstance().getForceUploadDownloadSpeedToReportInMbps()) {
			return String.format("%.2f Mbps", (double) (value / 1000000.0));
		}
		
		String ret = "";
		if (value < 1000) {
			ret = String.format("%.0f bps", value);
		} else if (value < 1000000) {
			ret = String.format("%.2f Kbps", (double) (value / 1000.0));
		} else {
			ret = String.format("%.2f Mbps", (double) (value / 1000000.0));
		}
		return ret;
	}

  // Use this method from the Summary screen; it is like timeMicrosecondsToString,
  // but without the units; and ALWAYS in milliseconds.
  public static String timeMicrosecondsToMillisecondsStringNoUnits(double valueMicroseconds) {
    String ret = String.format("%.0f", valueMicroseconds/1000);
    return ret;
  }

	public static String timeMicrosecondsToString(double valueMicroseconds) {
		String ret = "";
		if (valueMicroseconds < 1000) {
			ret = String.format("%.0f microseconds", valueMicroseconds );
		} else if (valueMicroseconds < 1000000) {
			ret = String.format("%.0f ms", valueMicroseconds/1000);
		} else {
			ret = String.format("%.2f s", valueMicroseconds/1000000);
		}
		return ret;
	}
	
	
	private void put(String key, String value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKPorting.sAssertE(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	private void putLong(String key, long value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKPorting.sAssertE(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	private void setTime(long dtime_mills){
		putLong(JSON_DTIME, dtime_mills);
		put(JSON_DATETIME, SKDateFormat.sGetDateAsIso8601String(new java.util.Date(dtime_mills)));
	}
	
	private void putDouble(String key, double value){
		try{
			super.put(key, value);
		}catch(JSONException je){
			SKPorting.sAssertE(StorageTestResult.class, "JSONException "+ key +" "+ value);
		}
	}
	
	//Empty constructor
	public StorageTestResult(){}
	
	public void setSuccess(int success){
		put(JSON_SUCCESS, success+"");
	}

  // This writes the test results§ via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
  // by ManualTestRunner:progressMessage
	public static List<JSONObject> testOutput(SKAbstractBaseTest theTest, String testType) {
		List<JSONObject> ret = new ArrayList<>();
//		if (data[0].equals("NETACTIVITY")) {
//    		return ret;
//		}
//		if (data[0].equals("CPUACTIVITY")) {
//    		return ret;
//		}
//		if (data[0].equals("CLOSESTTARGET")) {
//			return null;
//		}
    TESTSSTRINGID tsid;
		//SKLogger.sAssert(data[0].equals(testType));

		switch (testType) {
			case SKConstants.TEST_TYPE_DOWNLOAD:
				tsid = TESTSSTRINGID.JHTTPGETMT;
				// Continue!
				break;
			case SKConstants.TEST_TYPE_UPLOAD:
				tsid = TESTSSTRINGID.JHTTPPOSTMT;
				// Continue!
				break;
			case SKConstants.TEST_TYPE_LATENCY:
				tsid = TESTSSTRINGID.JUDPLATENCY;
				// Continue!
				break;
			case SKConstants.TEST_TYPE_CLOSEST_TARGET:
				return null;
			case "NETACTIVITY":
				SKPorting.sAssert(false);
				return null;
//		} else if (testType.equals("CPUACTIVITY")) {
//			SKLogger.sAssert(false);
//			return null;
			default:
				SKPorting.sAssert(false);
				// Nothing to report!
				return null;
		}

		switch (tsid) {
      case JHTTPGET:
      case JHTTPGETMT: {
        // This writes the test result via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
        // by ManualTestRunner:progressMessage
        StorageTestResult testResult = createStorageTestResultForUI_For_HttpTest(DETAIL_TEST_ID.DOWNLOAD_TEST_ID, (HttpTest) theTest);

        ret.add(testResult);
      }
      break;

      case JHTTPPOST:
      case JHTTPPOSTMT: {
        // This writes the test result via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
        // by ManualTestRunner:progressMessage
        StorageTestResult testResult = createStorageTestResultForUI_For_HttpTest(DETAIL_TEST_ID.UPLOAD_TEST_ID, (HttpTest) theTest);
        ret.add(testResult);
      }
      break;

      case JUDPLATENCY: {
        // This writes the test result via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
        // by ManualTestRunner:progressMessage
        StorageTestResult latencyTestResult = createStorageTestResultForUI_For_LatencyTest_Latency((LatencyTest) theTest);
        ret.add(latencyTestResult);

        StorageTestResult lossTestResult = createStorageTestResultForUI_For_LatencyTest_Loss((LatencyTest) theTest);
        ret.add(lossTestResult);

        // And send as a JITTER test, as well!
        StorageTestResult jitterTestResult = createStorageTestResultForUI_For_LatencyTest_Jitter((LatencyTest) theTest);
        ret.add(jitterTestResult);
      }
      break;
      case JUDPJITTER:
        SKPorting.sAssert(false);
        break;
    }

		return ret;
	}

	private static StorageTestResult createStorageTestResultForUI_For_HttpTest(DETAIL_TEST_ID test_id, HttpTest theTest) {
		StorageTestResult ret = new StorageTestResult(test_id);
    long dtimeNano = theTest.getTimestamp()*1000;
		ret.setTime(dtimeNano);
    String target = theTest.getTarget();
		ret.setLocation(target);

    // This writes the test results via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
    // by ManualTestRunner:progressMessage
    Double bytesPerSecond = Math.max(0, theTest.getTransferBytesPerSecond());
    Double bitsPerSecond = bytesPerSecond * 8.0;
		ret.setResult(bitsPerSecond);

    long success = theTest.isSuccessful() ? 1 : 0;
		ret.putLong(JSON_SUCCESS, success);
		ret.setTest(test_id);
		ret.setComplete();
		return ret;
	}

  // This writes the test results via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
  // by ManualTestRunner:progressMessage
  private static StorageTestResult createStorageTestResultForUI_For_LatencyTest_Latency(LatencyTest theTest) {
    StorageTestResult lat = new StorageTestResult(DETAIL_TEST_ID.LATENCY_TEST_ID);
    lat.setTest(DETAIL_TEST_ID.LATENCY_TEST_ID);

    long dtimeNano = theTest.getTimestamp()*1000;
    lat.setTime(dtimeNano);

    String target = theTest.getTarget();
    lat.setLocation(target);

    Double latencyResultMicroseconds = (double) theTest.getAverageMicroseconds();
    lat.setResult(latencyResultMicroseconds);

    long success = theTest.isSuccessful() ? 1 : 0;
    lat.putLong(JSON_SUCCESS, success);
    lat.setComplete();
    return lat;
  }

  // This writes the test results via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
  // by ManualTestRunner:progressMessage
	private static StorageTestResult createStorageTestResultForUI_For_LatencyTest_Loss(LatencyTest theTest) {
		StorageTestResult pl = new StorageTestResult(DETAIL_TEST_ID.PACKETLOSS_TEST_ID);
    pl.setTest(DETAIL_TEST_ID.PACKETLOSS_TEST_ID);

    int lost = theTest.getLostPackets();
    int sent = theTest.getSentPackets();
    double packetLoss = 0.0;
    if(sent != 0){
      packetLoss = 100d * ((double) lost)/sent;
    }
    pl.setResult(packetLoss);

    long dtimeNano = theTest.getTimestamp()*1000;
		pl.setTime(dtimeNano);

    String target = theTest.getTarget();
		pl.setLocation(target);
    long success = theTest.isSuccessful() ? 1 : 0;
		pl.putLong(JSON_SUCCESS, success);
		pl.setComplete();
		return pl;
	}

  // This writes the test results via JSON_HRESULT, which is sent (a short while later) to the UI as Message instances...
  // by ManualTestRunner:progressMessage
  private static StorageTestResult createStorageTestResultForUI_For_LatencyTest_Jitter(LatencyTest theTest){
    StorageTestResult ret = new StorageTestResult(DETAIL_TEST_ID.JITTER_TEST_ID);
    ret.setTest(StorageTestResult.DETAIL_TEST_ID.JITTER_TEST_ID);

    long dtimeNano = theTest.getTimestamp()*1000;
    ret.setTime(dtimeNano);

		String target = theTest.getTarget();
    ret.setLocation(target);

    Double jitterMilliseconds = (double) theTest.getResultJitterMilliseconds();
    Double jitterMicroseconds = jitterMilliseconds * 1000.0;
    ret.setResult(jitterMicroseconds);

    long success = theTest.isSuccessful() ? 1 : 0;
    ret.putLong(JSON_SUCCESS, success);

    ret.setComplete();
    return ret;
  }


  private void setTest(DETAIL_TEST_ID test_number){
		put(JSON_TYPE_ID, "test");
		put(JSON_TESTNUMBER, ""+test_number.getValueAsInt());
	}
	
	private void setComplete(){
		put(JSON_STATUS_COMPLETE, "100");
	}
	
	private void setPassiveMetric(){
		put(JSON_TYPE_ID, "passivemetric");
	}
	
	private void setLocation(String target){
		put(JSON_LOCATION, targetToLocation(target));
	}
	
	private static String targetToLocation(String target){
		String ret = target;
		ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
		
		if(config != null && config.hosts.containsKey(target)){
			ret = config.hosts.get(target);
		}
		return ret;
	}
	
}
